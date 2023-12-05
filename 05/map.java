///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.System.*;

/**
 * The almanac (your puzzle input) lists all of the seeds that need to be planted. It also lists what type of soil to use with each kind of seed, what type of fertilizer to use with each kind of soil, what type of water to use with each kind of fertilizer, and so on. Every type of seed, soil, fertilizer and so on is identified with a number, but numbers are reused by each category - that is, soil 123 and fertilizer 123 aren't necessarily related to each other.
 *
 * For example:
 *
 * seeds: 79 14 55 13
 *
 * seed-to-soil map:
 * 50 98 2
 * 52 50 48
 *
 * soil-to-fertilizer map:
 * 0 15 37
 * 37 52 2
 * 39 0 15
 *
 * fertilizer-to-water map:
 * 49 53 8
 * 0 11 42
 * 42 0 7
 * 57 7 4
 *
 * water-to-light map:
 * 88 18 7
 * 18 25 70
 *
 * light-to-temperature map:
 * 45 77 23
 * 81 45 19
 * 68 64 13
 *
 * temperature-to-humidity map:
 * 0 69 1
 * 1 0 69
 *
 * humidity-to-location map:
 * 60 56 37
 * 56 93 4
 *
 * The almanac starts by listing which seeds need to be planted: seeds 79, 14, 55, and 13.
 *
 * The rest of the almanac contains a list of maps which describe how to convert numbers from a source category Longo numbers in a destination category. That is, the section that starts with seed-to-soil map: describes how to convert a seed number (the source) to a soil number (the destination). This lets the gardener and his team know which soil to use with which seeds, which water to use with which fertilizer, and so on.
 *
 * Rather than list every source number and its corresponding destination number one by one, the maps describe entire ranges of numbers that can be converted. Each line within a map contains three numbers: the destination range start, the source range start, and the range length.
 *
 * Consider again the example seed-to-soil map:
 *
 * 50 98 2
 * 52 50 48
 *
 * The first line has a destination range start of 50, a source range start of 98, and a range length of 2. This line means that the source range starts at 98 and contains two values: 98 and 99. The destination range is the same length, but it starts at 50, so its two values are 50 and 51. With this information, you know that seed number 98 corresponds to soil number 50 and that seed number 99 corresponds to soil number 51.
 *
 * The second line means that the source range starts at 50 and contains 48 values: 50, 51, ..., 96, 97. This corresponds to a destination range starting at 52 and also containing 48 values: 52, 53, ..., 98, 99. So, seed number 53 corresponds to soil number 55.
 *
 * Any source numbers that aren't mapped correspond to the same destination number. So, seed number 10 corresponds to soil number 10.
 *
 * So, the entire list of seed numbers and their corresponding soil numbers looks like this:
 *
 * seed  soil
 * 0     0
 * 1     1
 * ...   ...
 * 48    48
 * 49    49
 * 50    52
 * 51    53
 * ...   ...
 * 96    98
 * 97    99
 * 98    50
 * 99    51
 *
 * With this map, you can look up the soil number required for each initial seed number:
 *
 *     Seed number 79 corresponds to soil number 81.
 *     Seed number 14 corresponds to soil number 14.
 *     Seed number 55 corresponds to soil number 57.
 *     Seed number 13 corresponds to soil number 13.
 *
 * The gardener and his team want to get started as soon as possible, so they'd like to know the closest location that needs a seed. Using these maps, find the lowest location number that corresponds to any of the initial seeds. To do this, you'll need to convert each seed number through other categories until you can find its corresponding location number. In this example, the corresponding types are:
 *
 *     Seed 79, soil 81, fertilizer 81, water 81, light 74, temperature 78, humidity 78, location 82.
 *     Seed 14, soil 14, fertilizer 53, water 49, light 42, temperature 42, humidity 43, location 43.
 *     Seed 55, soil 57, fertilizer 57, water 53, light 46, temperature 82, humidity 82, location 86.
 *     Seed 13, soil 13, fertilizer 52, water 41, light 34, temperature 34, humidity 35, location 35.
 *
 * So, the lowest location number in this example is 35.
 */
public class map {

    public static void main(String... args) throws IOException {
        var parser = new Parser();
        parser.state = ParserState.SEED_INTERVALS;
        Files.lines(Path.of("input2.txt")).forEach(parser::parse);
        parser.parse("");
        var result = minimalLocationRanges(parser);
        out.println(result);
    }

    private static long minimalLocation(Parser parser) {
        return parser.seeds.stream()
                .mapToLong(parser.mapping("seed-to-soil")::convert)
                .map(parser.mapping("soil-to-fertilizer")::convert)
                .map(parser.mapping("fertilizer-to-water")::convert)
                .map(parser.mapping("water-to-light")::convert)
                .map(parser.mapping("light-to-temperature")::convert)
                .map(parser.mapping("temperature-to-humidity")::convert)
                .map(parser.mapping("humidity-to-location")::convert)
                .min().orElseThrow(() -> new IllegalStateException("No solution found"));
    }

    private static long minimalLocationRanges(Parser parser) {
        parser.flatMapIntervals("seed-to-soil");
        parser.flatMapIntervals("soil-to-fertilizer");
        parser.flatMapIntervals("fertilizer-to-water");
        parser.flatMapIntervals("water-to-light");
        parser.flatMapIntervals("light-to-temperature");
        parser.flatMapIntervals("temperature-to-humidity");
        parser.flatMapIntervals("humidity-to-location");
        return parser.mappedIntervals.get(0).start();
    }


    enum ParserState { SEEDS, SEED_INTERVALS, MAP_TITLE, RANGE }

    static class Parser {
        ParserState state = ParserState.SEEDS;
        List<Mapping> mappings = new ArrayList<>();

        Map<String,Mapping> mappingByName = new HashMap<>();

        List<MapRange> ranges = new ArrayList<>();


        List<Long> seeds;

        List<Interval> seedIntervals;

        List<Interval> mappedIntervals;

        String currentMap;

        int lineNo;

        Mapping mapping(String name) {
            return mappingByName.get(name);
        }

        void flatMapIntervals(String mapping) {
            if (mappedIntervals == null) {
                mappedIntervals = seedIntervals;
            }
            mappedIntervals = mappedIntervals.stream().flatMap(i -> mapping(mapping).map(i).stream()).sorted(Comparator.comparingLong(Interval::start)).toList();
        }

        void parse(String line) {
            lineNo++;
            switch(state) {
                case SEEDS:
                    if (line.startsWith("seeds:")) {
                        parseSeeds(line);
                        switchState(ParserState.MAP_TITLE);
                        return;
                    } else if (line.isBlank()) {
                        return;
                    }
                    break;
                case SEED_INTERVALS:
                    if (line.startsWith("seeds:")) {
                        parseSeedIntervals(line);
                        switchState(ParserState.MAP_TITLE);
                        return;
                    } else if (line.isBlank()) {
                        return;
                    }
                    break;
                case MAP_TITLE:
                    if (line.isBlank()) {
                        return;
                    }
                    var matcher = Pattern.compile("^(\\w+-to-\\w+)\\s+map:$").matcher(line);
                    if (!matcher.find()) {
                        throw new IllegalArgumentException("Unexpected line %d in state %s: %s".formatted(lineNo, state, line));
                    }
                    currentMap = matcher.group(1);

                    out.println("Parsing map %s".formatted(currentMap));
                    state = ParserState.RANGE;
                    return;
                case RANGE:
                    if (line.isBlank()) {
                        switchState(ParserState.MAP_TITLE);
                        return;
                    }
                    var parts = line.split("\\s+");
                    ranges.add(new MapRange(Long.parseLong(parts[0]), Long.parseLong(parts[1]), Long.parseLong(parts[2])));
                    return;
            }
            throw new IllegalArgumentException("Unexpected line %d in state %s: %s".formatted(lineNo, state, line));
        }

        private void parseSeedIntervals(String line) {
            var parts = Stream.of(line.split("(seeds:\\s+)|\\s+")).filter(s -> !s.isBlank()).toArray(String[]::new);
            seedIntervals = new ArrayList<>();
            for (int i = 0; i < parts.length; i += 2) {
                seedIntervals.add(new Interval(Long.parseLong(parts[i]), Long.parseLong(parts[i + 1])));
            }
        }

        private void switchState(ParserState targetState) {
            switch(targetState) {
                case SEEDS -> throw new IllegalStateException("Cannot switch to SEEDS state");
                case MAP_TITLE -> {
                    if (state == ParserState.RANGE) {
                        Collections.sort(ranges, Comparator.comparingLong(r -> r.sourceStart));
                        Mapping mapping = new Mapping(currentMap, List.copyOf(ranges));
                        mappings.add(mapping);
                        mappingByName.put(mapping.name(), mapping);
                        ranges.clear();
                    }
                }
            }
            state = targetState;
        }

        private void parseSeeds(String line) {
            seeds = Stream.of(line.split("(seeds:\\s+)|\\s+"))
                .filter(s -> !s.isBlank())
                .map(s -> Long.parseLong(s))
                .toList();
        }
    }

    record Mapping(String name, List<MapRange> ranges) {

        public long convert(long source) {
            return ranges.stream()
                .filter(range -> range.contains(source))
                .findFirst()
                .stream().mapToLong(range -> range.convert(source))
                .findAny().orElse(source);
        }

        public long reverse(Long dest) {
            return ranges.stream()
                .filter(range -> range.containsReverse(dest))
                .findFirst()
                .stream().mapToLong(range -> range.reverse(dest))
                .findAny().orElse(dest);
        }

        public List<Interval> map(Interval source) {
            var result = new ArrayList<Interval>();
            var currentStart = source.start;
            var currentEnd = source.end();
            while (currentStart < source.end()) {
                // find first range that overlaps with current interval
                var start = currentStart;
                var range = ranges.stream()
                    .filter(r -> r.overlaps(start, currentEnd))
                    .findFirst();
                if (range.isEmpty()) {
                    // no range found, therefore entire region maps with identity
                    result.add(new Interval(currentStart, currentEnd - currentStart));
                    break;
                }
                var r = range.get();
                // map the part of the interval that is before the range
                if (currentStart < r.sourceStart) {
                    result.add(new Interval(currentStart, r.sourceStart - currentStart));
                    currentStart = r.sourceStart;
                }
                if (source.end() <= r.sourceEnd()) {
                    // entire interval is mapped by the range
                    result.add(new Interval(r.convert(currentStart), source.end() - currentStart));
                    break;
                }
                // map the part of the interval that is mapped by the range
                var mappedLength = r.sourceEnd() - currentStart;
                result.add(new Interval(r.convert(currentStart), mappedLength));
                currentStart += mappedLength;
            }
            return result;
        }
    }

    record Interval(long start, long length) {
        Interval(long start, long length) {
            this.start = start;
            this.length = Math.min(length, Long.MAX_VALUE - start);
        }

        long end() {
            return start + length;
        }
    }

    record MapRange(long destStart, long sourceStart, long length) {
        public boolean contains(long source) {
            return source >= sourceStart && source < sourceStart + length;
        }

        long sourceEnd() {
            return sourceStart + length;
        }

        public long convert(long source) {
            return destStart + (source - sourceStart);
        }

        /**
         * @param start
         * @param end
         * @return true if range maps any part of the source interval
         */
        boolean overlaps(long start, long end) {
            return start < sourceStart + length && end > sourceStart;
        }

        boolean containsReverse(long dest) {
            return dest >= destStart && dest < destStart + length;
        }

        public long reverse(long dest) {
            return sourceStart + (dest - destStart);
        }
    }
}
