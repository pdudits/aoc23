///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.*;

import static java.lang.System.*;

/**
 * The Elf leads you over to the pile of colorful cards. There, you discover dozens of scratchcards, all with their opaque covering already scratched off. Picking one up, it looks like each card has two lists of numbers separated by a vertical bar (|): a list of winning numbers and then a list of numbers you have. You organize the information into a table (your puzzle input).
 *
 * As far as the Elf has been able to figure out, you have to figure out which of the numbers you have appear in the list of winning numbers. The first match makes the card worth one point and each match after the first doubles the point value of that card.
 *
 * For example:
 *
 * Card 1: 41 48 83 86 17 | 83 86  6 31 17  9 48 53
 * Card 2: 13 32 20 16 61 | 61 30 68 82 17 32 24 19
 * Card 3:  1 21 53 59 44 | 69 82 63 72 16 21 14  1
 * Card 4: 41 92 73 84 69 | 59 84 76 51 58  5 54 83
 * Card 5: 87 83 26 28 32 | 88 30 70 12 93 22 82 36
 * Card 6: 31 18 13 56 72 | 74 77 10 23 35 67 36 11
 *
 * In the above example, card 1 has five winning numbers (41, 48, 83, 86, and 17) and eight numbers you have (83, 86, 6, 31, 17, 9, 48, and 53). Of the numbers you have, four of them (48, 83, 17, and 86) are winning numbers! That means card 1 is worth 8 points (1 for the first match, then doubled three times for each of the three matches after the first).
 */
public class lottery {

    public static void main(String... args) throws IOException {
        var cards = Files.lines(Path.of("cards2.txt")).map(lottery::parse).collect(Collectors.toList());
        var score = cards.stream().mapToInt(Card::score).sum();
        out.println(score);
        out.println(withRepeats(cards));
    }

    static int withRepeats(List<Card> cards) {
        var boosts = new int[cards.size()];
        Arrays.fill(boosts,1);
        for(int i=0; i<cards.size(); i++) {
          for(int j=i+1; j <= i+cards.get(i).wins(); j++) {
             boosts[j]+=boosts[i];
          }
        }
        return IntStream.of(boosts).sum();
    }

    record Card(Set<Integer> winningNumbers, Set<Integer> numbers) {
        int wins() {
            return (int)winningNumbers.stream().filter(numbers::contains).count();
        }
        int score() {
            return 1 << (winningNumbers.stream().filter(numbers::contains).count()-1);
        }
    }

    static Card parse(String input) {
        var parts = Pattern.compile("Card\\s+\\d+: ([\\d ]+)\\s+\\|\\s+([\\d ]+)").matcher(input).results().findFirst().orElseThrow(() -> new IllegalArgumentException("No match for line "+input));
        var winningNumbers = Stream.of(parts.group(1).split("\\s+")).filter(r->!r.isBlank()).map(r -> Integer.parseInt(r)).collect(Collectors.toSet());
        var numbers = Stream.of(parts.group(2).split("\\s+")).map(r -> Integer.parseInt(r)).collect(Collectors.toSet());
        return new Card(winningNumbers, numbers);
    }


}
