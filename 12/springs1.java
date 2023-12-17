///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.System.*;

/**
 * In the giant field just outside, the springs are arranged into rows. For each row, the condition records show every spring and whether it is operational (.) or damaged (#). This is the part of the condition records that is itself damaged; for some springs, it is simply unknown (?) whether the spring is operational or damaged.
 *
 * However, the engineer that produced the condition records also duplicated some of this information in a different format! After the list of springs for a given row, the size of each contiguous group of damaged springs is listed in the order those groups appear in the row. This list always accounts for every damaged spring, and each number is the entire size of its contiguous group (that is, groups are always separated by at least one operational spring: #### would always be 4, never 2,2).
 *
 * So, condition records with no unknown spring conditions might look like this:
 *
 * #.#.### 1,1,3
 * .#...#....###. 1,1,3
 * .#.###.#.###### 1,3,1,6
 * ####.#...#... 4,1,1
 * #....######..#####. 1,6,5
 * .###.##....# 3,2,1
 *
 * However, the condition records are partially damaged; some of the springs' conditions are actually unknown (?). For example:
 *
 * ???.### 1,1,3
 * .??..??...?##. 1,1,3
 * ?#?#?#?#?#?#?#? 1,3,1,6
 * ????.#...#... 4,1,1
 * ????.######..#####. 1,6,5
 * ?###???????? 3,2,1
 *
 * Equipped with this information, it is your job to figure out how many different arrangements of operational and broken springs fit the given criteria in each row.
 *
 * In the first line (???.### 1,1,3), there is exactly one way separate groups of one, one, and three broken springs (in that order) can appear in that row: the first three unknown springs must be broken, then operational, then broken (#.#), making the whole row #.#.###.
 *
 * The second line is more interesting: .??..??...?##. 1,1,3 could be a total of four different arrangements. The last ? must always be broken (to satisfy the final contiguous group of three broken springs), and each ?? must hide exactly one of the two broken springs. (Neither ?? could be both broken springs or they would form a single contiguous group of two; if that were true, the numbers afterward would have been 2,3 instead.) Since each ?? can either be #. or .#, there are four possible arrangements of springs.
 *
 * The last line is actually consistent with ten different arrangements! Because the first number is 3, the first and second ? must both be . (if either were #, the first number would have to be 4 or higher). However, the remaining run of unknown spring conditions have many different ways they could hold groups of two and one broken springs:
 *
 * ?###???????? 3,2,1
 * .###.##.#...
 * .###.##..#..
 * .###.##...#.
 * .###.##....#
 * .###..##.#..
 * .###..##..#.
 * .###..##...#
 * .###...##.#.
 * .###...##..#
 * .###....##.#
 *
 * In this example, the number of possible arrangements for each row is:
 *
 *     ???.### 1,1,3 - 1 arrangement
 *     .??..??...?##. 1,1,3 - 4 arrangements
 *     ?#?#?#?#?#?#?#? 1,3,1,6 - 1 arrangement
 *     ????.#...#... 4,1,1 - 1 arrangement
 *     ????.######..#####. 1,6,5 - 4 arrangements
 *     ?###???????? 3,2,1 - 10 arrangements
 *
 * Adding all of the possible arrangement counts together produces a total of 21 arrangements.
 *
 * For each row, count all of the different arrangements of operational and broken springs that meet the given criteria. What is the sum of those counts?
 */
public class springs1 {

    public static void main(String... args) throws IOException {
        var input = Files.lines(Path.of("input2.txt")).map(springs1::parse).toList();
        out.println(input.stream().mapToLong(r -> r.tester().countOptions()).sum());
        var expandedInput = input.stream().map(Row::unfold).toList();
        out.println(expandedInput.stream().mapToLong(r -> r.tester().countOptions()).sum());
    }

    static Row parse(String input) {
        var parts = input.split("\\s+");
        var condition = parts[0];
        var groups = parts[1].split(",\\s*");
        var result = new ArrayList<Integer>();
        for (var group : groups) {
            result.add(Integer.parseInt(group));
        }
        return new Row(condition, result);
    }

    record Row(String condition, List<Integer> groups) {

        public static final char GOOD = '.';
        public static final char BAD = '#';

        public static final char UNKNOWN = '?';

        public static final char REJECT = 'X';

        /**
         * To unfold the records, on each row, replace the list of spring conditions with five copies of itself (separated by ?) and replace the list of contiguous groups of damaged springs with five copies of itself (separated by ,).
         * @return
         */
        Row unfold() {
            StringBuffer expandedCondition = new StringBuffer();
            for (int i = 0; i < 5; i++) {
                expandedCondition.append(condition);
                expandedCondition.append('?');
            }
            expandedCondition.deleteCharAt(expandedCondition.length() - 1);
            var expandedGroups = new ArrayList<Integer>();
            for (int i = 0; i < 5; i++) {
                expandedGroups.addAll(groups);
            }
            return new Row(expandedCondition.toString(), expandedGroups);
        }

        Pattern verifier() {
            var pattern = new StringBuilder();
            pattern.append("\\.*");
            for (var group : groups) {
                pattern.append("#{" + group + "}");
                pattern.append("\\.+");
            }
            pattern.setCharAt(pattern.length() - 1, '*');
            return Pattern.compile(pattern.toString());
        }

        Tester tester() {
            return new Tester(new StringBuilder(condition));
        }

        class Tester {
            private final List<Integer> placeholders;
            private final int good;
            private final int bad;
            private final StringBuilder buffer;
            private final Pattern verifier;

            boolean doClipping = true;

            boolean doMemo = true;
            private int cacheHits;

            record MemoKey(int good, int bad) {}
            Map<MemoKey, Long> memo = new HashMap<>();

            Tester(StringBuilder buffer) {
                placeholders = new ArrayList<>();
                for(int i = 0; i < buffer.length(); i++) {
                    if (buffer.charAt(i) == UNKNOWN) {
                        placeholders.add(i);
                    }
                }
                bad = groups().stream().mapToInt(i -> i).sum() - count(BAD);
                good = placeholders.size() - bad;
                this.buffer = buffer;
                this.verifier = verifier();
            }

            long countOptions() {
                if (Math.min(good, bad) >= 18) {
                    out.println("Quite some options for " + Row.this);
                    out.println("good: " + good);
                    out.println("bad: " + bad);
                }
                memo.clear();
                doMemo = true;
                var memoed = countOptions(good, bad);
                out.printf("%s : %d, cache hits %d\n", Row.this, memoed, cacheHits);
//                doMemo = false;
//                var unmemoed = countOptions(good, bad);
//                if (memoed != unmemoed) {
//                    throw new IllegalArgumentException("memoed (%d) != unmemoed (%d)".formatted(memoed, unmemoed));
//                }
                return memoed;
            }

            long countOptions(int remainingGood, int remainingBad) {
                if (remainingGood < 0 || remainingBad < 0) {
                    return 0; // we clipped our way into a dead end
                }
                if (remainingGood == 0 && remainingBad == 0) {
                    if (doClipping) {
                        if (clip(buffer.length()-1) == REJECT) {
                            return 0;
                        } else {
                            //out.println(buffer);
                            return 1;
                        }
                    } else {
                        return verifier.matcher(buffer).matches() ? 1 : 0;
                    }
                }
                var index = placeholders.size() - remainingGood - remainingBad;
                int placeholderIndex = placeholders.get(index);

                var key = new MemoKey(remainingGood, remainingBad);
                boolean canMemo = doMemo && placeholderIndex> 0 && buffer.charAt(placeholderIndex-1) == GOOD;
                if (canMemo && memo.containsKey(key)) {
                    cacheHits ++;
                    return memo.get(key);
                }

                long result = 0;

                buffer.setCharAt(placeholderIndex, UNKNOWN);
                var clip = doClipping ? clip(placeholderIndex) : UNKNOWN;
                if (clip == REJECT) {
                    return 0;
                } else if (clip == UNKNOWN) {
                    if (remainingGood > 0) {
                        buffer.setCharAt(placeholderIndex, GOOD);
                        result += countOptions(remainingGood - 1, remainingBad);
                    }
                    if (remainingBad > 0) {
                        buffer.setCharAt(placeholderIndex, BAD);
                        result += countOptions(remainingGood, remainingBad - 1);
                    }
                } else if (clip == GOOD) {
                    buffer.setCharAt(placeholderIndex, GOOD);
                    result += countOptions(remainingGood -1 , remainingBad);
                } else if (clip == BAD) {
                    buffer.setCharAt(placeholderIndex, BAD);
                    result += countOptions(remainingGood, remainingBad - 1);
                }
                if (result > 0 && canMemo) {
                    memo.put(key, result);
                }
                return result;
            }

            /**
             * decide whether at index we are forced by groups to be a # or a .
             */
            char clip(int index) {
                char requirement = UNKNOWN;
                int groupIndex = -1;
                int requiredBad = 0;
                int requiredGood = 0;
                for (int i = 0; i <= index; i++) {
                    if (requiredGood > 0) {
                        requiredGood--;
                        requirement = GOOD;
                    } else if (requiredBad > 1) {
                        requiredBad--;
                        requirement = BAD;
                    } else if (requiredBad == 1) {
                        requiredGood = 1;
                        requiredBad = 0;
                        requirement = BAD;
                    } else if (buffer.charAt(i) == BAD) {
                        groupIndex++;
                        if (groupIndex >= groups().size()) {
                            // already this one is too much
                            requiredGood = buffer.length()-i;
                            requirement = GOOD;
                        } else {
                            requiredBad = groups().get(groupIndex)-1;
                            // in case this is 1-character group, next needs to be bad.
                            if (requiredBad == 0) {
                                requiredGood = 1;
                            }
                            requirement = BAD;
                        }
                    } else {
                        requirement = UNKNOWN;
                    }
                    if (requirement != UNKNOWN) {
                        var c = buffer.charAt(i);
                        if (c != requirement && c != UNKNOWN) {
                            return REJECT;
                        }
                    }
                }
                return requirement;
            }
        }

        int count(char c) {
            return (int) condition.chars().filter(i -> i == c).count();
        }
    }
}
