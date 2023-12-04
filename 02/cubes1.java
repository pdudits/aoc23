///usr/bin/env jbang "$0" "$@" ; exit $?


import static java.lang.System.*;
import java.util.*;
import java.util.stream.*;
import java.util.regex.*;
import java.io.*;
import java.nio.file.*;

/**
 * As you walk, the Elf shows you a small bag and some cubes which are either red, green, or blue. Each time you play this game, he will hide a secret number of cubes of each color in the bag, and your goal is to figure out information about the number of cubes.

To get information, once a bag has been loaded with cubes, the Elf will reach into the bag, grab a handful of random cubes, show them to you, and then put them back in the bag. He'll do this a few times per game.

You play several games and record the information from each game (your puzzle input). Each game is listed with its ID number (like the 11 in Game 11: ...) followed by a semicolon-separated list of subsets of cubes that were revealed from the bag (like 3 red, 5 green, 4 blue).

For example, the record of a few games might look like this:

Game 1: 3 blue, 4 red; 1 red, 2 green, 6 blue; 2 green
Game 2: 1 blue, 2 green; 3 green, 4 blue, 1 red; 1 green, 1 blue
Game 3: 8 green, 6 blue, 20 red; 5 blue, 4 red, 13 green; 5 green, 1 red
Game 4: 1 green, 3 red, 6 blue; 3 green, 6 red; 3 green, 15 blue, 14 red
Game 5: 6 red, 1 blue, 3 green; 2 blue, 1 red, 2 green

In game 1, three sets of cubes are revealed from the bag (and then put back again). The first set is 3 blue cubes and 4 red cubes; the second set is 1 red cube, 2 green cubes, and 6 blue cubes; the third set is only 2 green cubes.

The Elf would first like to know which games would have been possible if the bag contained only 12 red cubes, 13 green cubes, and 14 blue cubes?

In the example above, games 1, 2, and 5 would have been possible if the bag had been loaded with that configuration. However, game 3 would have been impossible because at one point the Elf showed you 20 red cubes at once; similarly, game 4 would also have been impossible because the Elf showed you 15 blue cubes at once. If you add up the IDs of the games that would have been possible, you get 8.

Determine which games would have been possible if the bag had been loaded with only 12 red cubes, 13 green cubes, and 14 blue cubes. What is the sum of the IDs of those games?
 */
public class cubes1 {

    static Map<String,Integer> limits = Map.of("red", 12, "green", 13, "blue", 14);

    public static void main(String... args) throws Exception {
        var result = Files.lines(Paths.get("cubes2.txt")).map(cubes1::parseGame)
        //.filter(Game::isPossible)
        //.mapToInt(Game::id)
        //.sum();
            .mapToInt(Game::power)
            .sum();
        out.println(result);
    }

    private static Game parseGame(String line) {
        var parts = line.split(":");
        var id = Integer.parseInt(parts[0].replace("Game ", ""));
        var rounds = Stream.of(parts[1].split(";")).map(cubes1::parseRound);
        return new Game(id, rounds.collect(Collectors.toList()));
    }

    private static Round parseRound(String round) {
        var parts = Stream.of(round.split(","));
        var shown = parts.map(cubes1::parseCube).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new Round(shown);
    }

    private static Map.Entry<String,Integer> parseCube(String cube) {
        var parts = Pattern.compile("([0-9]+) ([a-z]+)").matcher(cube);
        parts.find();
        try {
            return new AbstractMap.SimpleEntry<>(parts.group(2), Integer.parseInt(parts.group(1)));
        } catch (Exception e) {
            throw new IllegalArgumentException("[%s]".formatted(cube), e);
        }
    }

    record Game(int id, List<Round> rounds) {
        boolean isPossible() {
            return rounds.stream().allMatch(Round::isPossible);
        }

        /**
         * 
    In game 1, the game could have been played with as few as 4 red, 2 green, and 6 blue cubes. If any color had even one fewer cube, the game would have been impossible.
    Game 2 could have been played with a minimum of 1 red, 3 green, and 4 blue cubes.
    Game 3 must have been played with at least 20 red, 13 green, and 6 blue cubes.
    Game 4 required at least 14 red, 3 green, and 15 blue cubes.
    Game 5 needed no fewer than 6 red, 3 green, and 2 blue cubes in the bag.

The power of a set of cubes is equal to the numbers of red, green, and blue cubes multiplied together. The power of the minimum set of cubes in game 1 is 48. In games 2-5 it was 12, 1560, 630, and 36, respectively. Adding up these five powers produces the sum 2286.
         */
        int power() {
            var components = rounds.stream().flatMap(r -> r.shown().keySet().stream()).collect(Collectors.toSet());
            var minimums = components.stream().mapToInt(this::maximum).toArray();
            return IntStream.of(minimums).reduce(1, (a,b) -> a*b);
        }

        int maximum(String key) {
            return rounds.stream().mapToInt(r -> r.value(key)).max().orElse(0);
        }
    }
    record Round(Map<String,Integer> shown) {
        boolean isPossible() {
            return shown.entrySet().stream().allMatch(e -> limits.get(e.getKey()) >= e.getValue());
        }

        int value(String key) {
            return shown().getOrDefault(key, 0);
        }
    }
}
