///usr/bin/env jbang "$0" "$@" ; exit $?


import static java.lang.System.*;
import java.util.*;
import java.util.stream.*;
import java.util.regex.*;
import java.nio.file.*;
import java.io.*;
/**
 * The engine schematic (your puzzle input) consists of a visual representation of the engine. There are lots of numbers and symbols you don't really understand, but apparently any number adjacent to a symbol, even diagonally, is a "part number" and should be included in your sum. (Periods (.) do not count as a symbol.)

Here is an example engine schematic:

467..114..
...*......
..35..633.
......#...
617*......
.....+.58.
..592.....
......755.
...$.*....
.664.598..

In this schematic, two numbers are not part numbers because they are not adjacent to a symbol: 114 (top right) and 58 (middle right). Every other number is adjacent to a symbol and so is a part number
 */
public class schematic {

    public static void main(String... args) throws Exception {
        var lines = readLines();
        var schematic = new Schematic(lines);
        out.println("Part 1: " + schematic.sumOfPartNumbers());
        out.println("Part 2: " + schematic.sumOfGears());
    }

    static List<Line> readLines() throws IOException {
        return Files.lines(Paths.get("schematic2.txt")).map(Line::new).collect(Collectors.toList());
    }

    record Schematic(List<Line> lines) {
        public int sumOfPartNumbers() {
            var sum = 0;
            for (int i=0; i<lines.size(); i++) {
                var line = lines.get(i);
                var numbers = line.numbers();
                for (var number : numbers) {
                    if (isAdjacentToSymbol(i, number)) {
                        sum += number.value;
                    } else {
                        out.printf("%d %s invalid\n", i, number);
                    }
                }
            }
            return sum;
        }

        /**
         * This time, you need to find the gear ratio of every gear and add them all up so that the engineer can figure out which gear needs to be replaced.

Consider the same engine schematic again:

467..114..
...*......
..35..633.
......#...
617*......
.....+.58.
..592.....
......755.
...$.*....
.664.598..

In this schematic, there are two gears. The first is in the top left; it has part numbers 467 and 35, so its gear ratio is 16345. The second gear is in the lower right; its gear ratio is 451490. (The * adjacent to 617 is not a gear because it is only adjacent to one part number.) Adding up all of the gear ratios produces 467835.
         */
        public int sumOfGears() {
            var sum = 0;
            for (int i=0; i<lines.size(); i++) {
                var line = lines.get(i);
                var lineIndex = i;
                sum += line.gearCandidates().map(index -> {
                    var numbers = Stream.concat(gearCandidates(lineIndex-1, index),
                         Stream.concat(gearCandidates(lineIndex, index), gearCandidates(lineIndex+1, index)))
                         .collect(Collectors.toList());
                    if (numbers.size() == 2) {
                        return numbers.get(0).value() * numbers.get(1).value();
                    } else {
                        return 0;
                    }
                }).sum();
            }
            return sum;
        }

        Stream<Number> gearCandidates(int lineIndex, int gearIndex) {
            if (lineIndex < 0 || lineIndex >= lines.size()) {
                return Stream.of();
            }
            return lines.get(lineIndex).adjacentNumbers(gearIndex);
        }


        boolean isAdjacentToSymbol(int line, Number number) {
            return hasSymbol(line, number.begin()-1, number.begin()) ||
                   hasSymbol(line, number.end(), number.end()+1) ||
                   hasSymbol(line-1, number.begin()-1, number.end()+1) ||
                   hasSymbol(line+1, number.begin()-1, number.end()+1); 
        }

        boolean hasSymbol(int line, int begin, int end) {
            if (line < 0) {
                return false;
            }
            if (line >= lines.size()) {
                return false;
            }
            var aLine = lines.get(line);
            return aLine.hasSymbol(begin, end);
        }
    }

    static Pattern SYMBOLS = Pattern.compile("[!@#$%^&*()_+=_\\\\/,:;'\"?|{}<>\\-]");
    
    record Line(String line) {
        public List<Number> numbers() {
            List<Number> result = new ArrayList<>();
            var matcher = Pattern.compile("\\d+").matcher(line);
            while (matcher.find()) {
                result.add(new Number(matcher.start(), matcher.end(), Integer.parseInt(matcher.group())));
            }
            return result;
        }

        public IntStream gearCandidates() {
            var matcher = Pattern.compile("\\*").matcher(line);
            var result = IntStream.builder();
            while (matcher.find()) {
                result.add(matcher.start());
            }
            return result.build();
        }

        public Stream<Number> adjacentNumbers(int index) {
            return numbers().stream().filter(n -> n.adjacentTo(index));
        }

        public boolean hasSymbol(int begin, int end) {
            if (begin < 0) {
                begin = 0;
            }
            if (end > line.length()) {
                end = line.length();
            }
            return SYMBOLS.matcher(line.substring(begin, end)).find();
        }
    }

    record Number(int begin, int end, int value) {
        boolean adjacentTo(int index) {
            return index >= begin-1 && index <= end;
        }
    }
}
