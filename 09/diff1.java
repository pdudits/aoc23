///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.System.*;

public class diff1 {

    public static void main(String... args) throws IOException {
        var input = Files.lines(Path.of("input2.txt")).map(Diff::parse).toList();
        var answer1 = input.stream().mapToInt(Diff::next).sum();
        out.println(answer1);
        var answer2 = input.stream().mapToInt(Diff::previous).sum();
        out.println(answer2);
    }

    static class Diff {
        private final ArrayList<int[]> diffs;

        static Diff parse(String input) {
            return new Diff(Arrays.stream(input.split("\\s+")).mapToInt(s -> Integer.parseInt(s)).toArray());
        }

        Diff(int[] input) {
            this.diffs = new ArrayList<int[]>();
            diffs.add(input);
            calculateDiffs();
        }

        int next() {
            int d = 0;
            for(int i=diffs.size()-2;i>=0;i--) {
                var cur = diffs.get(i);
               d += cur[cur.length-1];
            }
            return d;
        }

        int previous() {
            int d = 0;
            for(int i=diffs.size()-2;i>=0;i--) {
                var cur = diffs.get(i);
                d = cur[0] - d;
            }
            return d;
        }

        private void calculateDiffs() {
            for(int i=0;!allZeros(i);i++) {
                var current = diffs.get(i);
                var next = new int[current.length-1];
                for (int j=0; j<next.length; j++) {
                    next[j] = current[j+1] - current[j];
                }
                diffs.add(next);
            }
        }

        private boolean allZeros(int index) {
            return Arrays.stream(diffs.get(index)).allMatch(i -> i==0);
        }
    }
}
