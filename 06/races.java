///usr/bin/env jbang "$0" "$@" ; exit $?


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.System.*;

public class races {

    public static void main(String... args) throws Exception {
        var races = parse(Files.lines(Path.of("input4.txt")));
        out.println(races.stream().mapToLong(Race::numberOfOptions).reduce(1, (a, b) -> a * b));
    }


    record Race(double time, double distance) {
        // valid solution satisfies the following equation:
        // x^2 - x*time + d < 0
        // we'll be looking for integer solutions of this
        long numberOfOptions() {
            double dis = Math.sqrt(time * time - 4 * distance);
            double root1 = (time - dis)/2;
            double root2 = (time + dis)/2;
            double low = Math.ceil(root1);
            double high = Math.floor(root2);
            if (low - root1 < 0.0001) {
                low++;
            }
            if (root2 - high < 0.0001) {
                high--;
            }
            return (long)(high - low + 1);
        }
    }

    /**
     * For example:
     *
     * Time:      7  15   30
     * Distance:  9  40  200
     *
     * This document describes three races:
     *
     *     The first race lasts 7 milliseconds. The record distance in this race is 9 millimeters.
     *     The second race lasts 15 milliseconds. The record distance in this race is 40 millimeters.
     *     The third race lasts 30 milliseconds. The record distance in this race is 200 millimeters.
     */
    static List<Race> parse(Stream<String> input) {
        var lines = input.limit(O2).toArray(String[]::new);
        var times = Stream.of(lines[0].split("(Time:)?\\s+")).filter(s -> !s.isBlank()).mapToDouble(Double::parseDouble).toArray();
        var distances = Stream.of(lines[1].split("(Distance:)?\\s+")).filter(s -> !s.isBlank()).mapToDouble(Double::parseDouble).toArray();
        var result = new ArrayList<Race>(times.length);
        for(int i=0; i<times.length; i++) {
            result.add(new Race(times[i], distances[i]));
        }
        return result;
    }
}
