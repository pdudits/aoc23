///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.System.*;

/**
 * While you wait, one of the Elves that works with the gardener heard how good you are at solving problems and would like your help. He needs to get his steps in for the day, and so he'd like to know which garden plots he can reach with exactly his remaining 64 steps.
 *
 * He gives you an up-to-date map (your puzzle input) of his starting position (S), garden plots (.), and rocks (#). For example:
 *
 * ...........
 * .....###.#.
 * .###.##..#.
 * ..#.#...#..
 * ....#.#....
 * .##..S####.
 * .##..#...#.
 * .......##..
 * .##.#.####.
 * .##..##.##.
 * ...........
 *
 * The Elf starts at the starting position (S) which also counts as a garden plot. Then, he can take one step north, south, east, or west, but only onto tiles that are garden plots. This would allow him to reach any of the tiles marked O:
 *
 * ...........
 * .....###.#.
 * .###.##..#.
 * ..#.#...#..
 * ....#O#....
 * .##.OS####.
 * .##..#...#.
 * .......##..
 * .##.#.####.
 * .##..##.##.
 * ...........
 *
 * Then, he takes a second step. Since at this point he could be at either tile marked O, his second step would allow him to reach any garden plot that is one step north, south, east, or west of any tile that he could have reached after the first step:
 *
 * ...........
 * .....###.#.
 * .###.##..#.
 * ..#.#O..#..
 * ....#.#....
 * .##O.O####.
 * .##.O#...#.
 * .......##..
 * .##.#.####.
 * .##..##.##.
 * ...........
 */
public class map1 {

    public static void main(String... args) throws IOException {
        var map = new GardenMap(Files.readAllLines(Path.of("input2.txt")));
        out.println(walk(map,64));// 3649
        out.println(altWalk(map,64));// 3649
        out.println(infiniteWalk(map));// 26501365
    }

    static int walk(GardenMap map, int steps) {
        var positions = new HashSet<Position>();
        positions.add(Position.startingPosition(map));
        for(int i =0; i<steps; i++) {
            var next = new HashSet<Position>();
            for(var position : positions) {
                position.next(map).forEach(next::add);
            }
            positions = next;
            out.println(next.size());
        }
        return positions.size();
    }

    static int altWalk(GardenMap map, int steps) {
        var distances = getDistances(map);
        return (int) distances.values().stream().filter(v -> v<=steps && v%2 == steps%2).count();
    }

    record Cursor(Position position, int distance) {}

    static BigInteger infiniteWalk(GardenMap map) {
        // https://github.com/villuna/aoc23/wiki/A-Geometric-solution-to-advent-of-code-2023,-day-21

        var distances = getDistances(map);
        int evenParityPositions = (int) distances.values().stream().filter(v -> v % 2 == 0).count();
        int evenParityCorners = (int) distances.values().stream().filter(v -> v % 2 == 0 && v > 65).count();
        int oddParityPositions = (int) distances.values().stream().filter(v -> v % 2 == 1).count();
        int oddParityCorners = (int) distances.values().stream().filter(v -> v % 2 == 1 && v > 65).count();

        int repetitions = (26501365 - map.width() / 2) / map.height();
        if (repetitions != 202300) {
            throw new IllegalStateException("Repetitions should be 202300 but was " + repetitions);
        }
        BigInteger even = BigInteger.valueOf(repetitions).pow(2);
        BigInteger odd = BigInteger.valueOf(repetitions+1).pow(2);

        BigInteger wonderfulSolution = odd.multiply(BigInteger.valueOf(oddParityPositions))
                .add(even.multiply(BigInteger.valueOf(evenParityPositions)))
                .subtract(BigInteger.valueOf(repetitions+1).multiply(BigInteger.valueOf(oddParityCorners)))
                .add(BigInteger.valueOf(repetitions).multiply(BigInteger.valueOf(evenParityCorners)));
        return wonderfulSolution;
    }

    private static HashMap<Position, Integer> getDistances(GardenMap map) {
        var distances = new HashMap<Position, Integer>();
        var positions = new ArrayDeque<Cursor>();
        positions.add(new Cursor(Position.startingPosition(map),0));
        while(!positions.isEmpty()) {
            var cursor = positions.remove();
            if (distances.containsKey(cursor.position)) {
                continue;
            }
            distances.put(cursor.position, cursor.distance);
            cursor.position.next(map)
                    .map(p -> new Cursor(p, cursor.distance+1))
                    .forEach(positions::add);
        }
        return distances;
    }

    record Position(int x, int y) {
        public static Position startingPosition(GardenMap map) {
            for (var y = 0; y < map.height(); y++) {
                var line = map.input.get(y);
                var x = line.indexOf('S');
                if (x >= 0) {
                    return new Position(x, y);
                }
            }
            throw new IllegalArgumentException("No starting position found");
        }

        Stream<Position> next(GardenMap map) {
            return Stream.of(
                new Position(x + 1, y),
                new Position(x - 1, y),
                new Position(x, y + 1),
                new Position(x, y - 1)
            ).filter(map::isGarden);
        }

        Stream<Position> nextWarped(GardenMap map) {
            return Stream.of(
                new Position(x + 1, y),
                new Position(x - 1, y),
                new Position(x, y + 1),
                new Position(x, y - 1)
            ).filter(p -> !map.isValid(p))
             .map(p -> new Position(Math.floorMod(p.x,map.width()), Math.floorMod(p.y, map.height())));
        }
    }

    record GardenMap(List<String> input) {
        int width() {
            return input.get(0).length();
        }

        int height() {
            return input.size();
        }

        char get(int x, int y) {
            return input.get(y).charAt(x);
        }

        boolean isValid(Position position) {
            return position.x >= 0 && position.x < width() && position.y >= 0 && position.y < height();
        }

        boolean isGarden(Position position) {
            return isValid(position) && isPlot(position);
        }

        private boolean isPlot(Position position) {
            return get(position.x, position.y) != '#';
        }
    }
}
