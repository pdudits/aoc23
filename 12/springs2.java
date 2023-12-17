///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.System.out;

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
public class springs2 {

    public static void main(String... args) throws IOException {
        var input = Files.lines(Path.of("input2.txt")).map(springs2::parse).toList();
        out.println(input.stream().mapToLong(r -> r.count()).sum());
        var expandedInput = input.stream().map(Row::unfold).toList();
        out.println(expandedInput.stream().mapToLong(r -> r.count()).sum());
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
            StringBuilder expandedCondition = new StringBuilder();
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

        long count() {
            var map = new HashMap<Branch, Long>();
            return new Branch(condition, groups).count(map);
        }


        record Branch(String pattern, List<Integer> groups) {
            long count(Map<Branch,Long> cache) {
                if (pattern.isEmpty() && groups.isEmpty()) {
                    return 1;
                }

                if (pattern.isEmpty()) {
                    return 0;
                }

                if (cache.containsKey(this)) {
                    return cache.get(this);
                }

                if (pattern.charAt(0) == GOOD) {
                    // good doesn't change the outcome much, continue further
                    var res = new Branch(pattern.substring(1), groups).count(cache);
                    cache.put(this, res);
                    return res;
                }

                // can we yet satisfy the constraints?
                var remainingGroupSum = groups.stream().mapToInt(i -> i).sum();
                if (pattern.length() < remainingGroupSum+groups.size()-1) {
                    var res = 0L;
                    cache.put(this, res);
                    return res;
                }

                if (pattern.charAt(0) == UNKNOWN) {
                    // unknown can be either good or bad
                    var res = new Branch(pattern.substring(1), groups).count(cache)
                            + new Branch("#" + pattern.substring(1), groups).count(cache);
                    cache.put(this, res);
                    return res;
                }
                if (pattern.charAt(0) == BAD) {
                    // no more groupings to satisfy
                    if (groups.isEmpty()) {
                        var res = 0L;
                        cache.put(this, res);
                        return res;
                    }
                }

                var groupLength = groups.get(0);
                var nextDot = pattern.indexOf(GOOD);
                if (nextDot == -1) {
                    nextDot = pattern.length();
                }

                // does the group fit?
                if (nextDot < groupLength) {
                    var res = 0L;
                    cache.put(this, res);
                    return res;
                }

                // cut the group out
                var remaining = pattern.substring(groupLength);
                if (remaining.length() == 0) {
                    // ok if group is also empty, back there at top.
                    var res = new Branch(remaining, groups.subList(1, groups.size())).count(cache);
                    cache.put(this, res);
                    return res;
                }

                if (remaining.charAt(0) == BAD) {
                    // the group would go on, so that's not a match
                    var res = 0L;
                    cache.put(this, res);
                    return res;
                }

                var res = new Branch(remaining.substring(1), groups.subList(1, groups.size())).count(cache);
                cache.put(this, res);
                return res;
            }
        }

    }
}
