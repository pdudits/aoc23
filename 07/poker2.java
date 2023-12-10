///usr/bin/env jbang "$0" "$@" ; exit $?

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.out;

public class poker2 {

    /**
     * To play Camel Cards, you are given a list of hands and their corresponding bid (your puzzle input). For example:
     * <p>
     * 32T3K 765
     * T55J5 684
     * KK677 28
     * KTJJT 220
     * QQQJA 483
     * <p>
     * This example shows five hands; each hand is followed by its bid amount. Each hand wins an amount equal to its bid multiplied by its rank, where the weakest hand gets rank 1, the second-weakest hand gets rank 2, and so on up to the strongest hand. Because there are five hands in this example, the strongest hand will have rank 5 and its bid will be multiplied by 5.
     * <p>
     * So, the first step is to put the hands in order of strength:
     * <p>
     * 32T3K is the only one pair and the other hands are all a stronger type, so it gets rank 1.
     * KK677 and KTJJT are both two pair. Their first cards both have the same label, but the second card of KK677 is stronger (K vs T), so KTJJT gets rank 2 and KK677 gets rank 3.
     * T55J5 and QQQJA are both three of a kind. QQQJA has a stronger first card, so it gets rank 5 and T55J5 gets rank 4.
     * <p>
     * Now, you can determine the total winnings of this set of hands by adding up the result of multiplying each hand's bid with its rank (765 * 1 + 220 * 2 + 28 * 3 + 684 * 4 + 483 * 5). So the total winnings in this example are 6440.
     *
     * @param args
     */
    public static void main(String... args) throws IOException {
        Comparator<Hand> comparator = Comparator.comparing(Hand::score).thenComparing(Hand::hand, poker2::compareLabel);
        var hands = Files.lines(Paths.get("input2.txt")).map(poker2::parse).sorted(comparator).toList();

        long result = 0;
        for (int i = 1; i <= hands.size(); i++) {
            result += (long) i * hands.get(i - 1).bid;
        }
        out.println(result);
    }

    static final String LABELS = "AKQT98765432J";

    static int compareLabel(String a, String b) {
        for(int i=0;i<a.length();i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return compareLabel(a.charAt(i), b.charAt(i));
            }
        }
        return 0;
    }
    static int compareLabel(char a, char b) {
        return LABELS.indexOf(b) - LABELS.indexOf(a);
    }

    static int labelWeight(char a) {
        return LABELS.length() - LABELS.indexOf(a);
    }

    static Hand parse(String line) {
        String[] parts = line.split("\\s+");
        return new Hand(parts[0], Integer.parseInt(parts[1]));
    }

    record Hand(String hand, int bid, Score score) {
        Hand(String hand, int bid) {
            this(hand, bid, scoreHandAccordingToActualRules(hand));
        }
    }

    static Score IDENTITY = new Score(0, 0);

    static Score scoreHand(String hand) {

        Map<Character, Integer> labelCounts = new HashMap<>();
        for (char c : hand.toCharArray()) {
            labelCounts.put(c, labelCounts.getOrDefault(c, 0) + 1);
        }

        var score = labelCounts.entrySet().stream().map((e) -> new Score(e.getValue(), labelWeight(e.getKey())))
                .sorted().reduce(IDENTITY, Score::combine);

        return score;
    }

    /**
     * Hands are primarily ordered based on type; for example, every full house is stronger than any three of a kind.
     * <p>
     * If two hands have the same type, a second ordering rule takes effect. Start by comparing the first card in each hand. If these cards are different, the hand with the stronger first card is considered stronger. If the first card in each hand have the same label, however, then move on to considering the second card in each hand. If they differ, the hand with the higher second card wins; otherwise, continue with the third card in each hand, then the fourth, then the fifth.
     *
     * @param hand
     * @return
     */
    static Score scoreHandAccordingToActualRules(String hand) {
        if (hand.contains("J")) {
            return scoreHandWithJoker(hand);
        }
        Map<Character, Integer> labelCounts = new HashMap<>();
        for (char c : hand.toCharArray()) {
            labelCounts.put(c, labelCounts.getOrDefault(c, 0) + 1);
        }

        var counts = labelCounts.values();
        if (counts.contains(5)) {
            return new Score(1,5);
        } else if (counts.contains(4)) {
            return new Score(1, 4);
        } else if (counts.contains(3)) {
            if (counts.contains(2)) {
                return new Score(1,3, new Score(1, 2));
            } else {
                return new Score(1, 3);
            }
        } else if (counts.contains(2)) {
            var numberOfPairs = counts.stream().filter(c -> c == 2).count();
            if (numberOfPairs == 2) {
                return new Score(1, 2, new Score(1, 2));
            } else {
                return new Score(1, 2);
            }
        } else {
            return new Score(1,1);
        }
    }

    static Score scoreHandWithJoker(String hand) {

        Map<Character, Integer> labelCounts = new HashMap<>();
        for (char c : hand.toCharArray()) {
            labelCounts.put(c, labelCounts.getOrDefault(c, 0) + 1);
        }

        var counts = labelCounts.values();
        if (counts.contains(5)) { // 5 jokers, cannot get better
            return new Score(1,5);
        } else if (counts.contains(4)) { // 4 of same kind, becomes 5
            // XXXXJ -> JJJJJ, JJJJX -> XXXXX
            return new Score(1, 5);
        } else if (counts.contains(3)) {
            if (labelCounts.get('J') == 3) {
                // do we have a pair?
                if (counts.contains(2)) {
                    // JJJXX -> XXXXX
                    return new Score(1, 5);
                } else {
                    // JJJXY -> XXXXY
                    return new Score(1, 4);
                }
            } else if (labelCounts.get('J') == 2) {
                // JJXXX -> XXXXX
                return new Score(1, 5);
            }
            // JXXXY -> XXXXY
            return new Score(1, 4);
        } else if (counts.contains(2)) {
            var numberOfPairs = counts.stream().filter(c -> c == 2).count();
            if (labelCounts.get('J') == 2) {
                if (numberOfPairs == 2) {
                    // JJXXY -> XXXXY
                    return new Score(1, 4);
                } else {
                    // JJXYZ -> XXXYZ
                    return new Score(1, 3);
                }
            } else {
                if (numberOfPairs == 2) {
                    // JXXYY -> XXXYY
                    return new Score(1, 3, new Score(1, 2));
                } else {
                    // JXXAB -> XXXAB
                    return new Score(1, 3);
                }
            }
        } else {
            // JABCD -> AABCD
            return new Score(1,2);
        }
    }

    /**
     * In Camel Cards, you get a list of hands, and your goal is to order them based on the strength of each hand. A hand consists of five cards labeled one of A, K, Q, J, T, 9, 8, 7, 6, 5, 4, 3, or 2. The relative strength of each card follows this order, where A is the highest and 2 is the lowest.
     * <p>
     * Every hand is exactly one type. From strongest to weakest, they are:
     * <p>
     * (6) Five of a kind, where all five cards have the same label: AAAAA
     * (5) Four of a kind, where four cards have the same label and one card has a different label: AA8AA
     * (4) Full house, where three cards have the same label, and the remaining two cards share a different label: 23332
     * (3) Three of a kind, where three cards have the same label, and the remaining two cards are each different from any other card in the hand: TTT98
     * (2) Two pair, where two cards share one label, two other cards share a second label, and the remaining card has a third label: 23432
     * (1) One pair, where two cards share one label, and the other three cards have a different label from the pair and each other: A23A4
     * (0) High card, where all cards' labels are distinct: 23456
     *
     * @param hand
     */
    record Score(int count, int weight, Score downstream) implements Comparable<Score> {
        static Comparator<Score> COMPARATOR = Comparator.comparing(Score::count).thenComparing(Score::weight).thenComparing(Score::downstream);

        Score(int count, int weight) {
            this(count, weight, IDENTITY);
        }

        @Override
        public int compareTo(Score o) {
            if (o == null) {
                return 1;
            }
            if (equals(o)) {
                return 0;
            }
            return COMPARATOR.compare(this, o);
        }

        Score combine(Score other) {
            if (other == null) {
                return this;
            } else if (this.compareTo(other) > 0) {
                return new Score(count, weight, other);
            } else if (this.compareTo(other) < 0) {
                return new Score(other.count, other.weight, this);
            } else {
                return this;
            }
        }
    }


}
