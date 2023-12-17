///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * As you move through the valley of mirrors, you find that several of them have fallen from the large metal frames keeping them in place. The mirrors are extremely flat and shiny, and many of the fallen mirrors have lodged into the ash at strange angles. Because the terrain is all one color, it's hard to tell where it's safe to walk or where you're about to run into a mirror.
 *
 * You note down the patterns of ash (.) and rocks (#) that you see as you walk (your puzzle input); perhaps by carefully analyzing these patterns, you can figure out where the mirrors are!
 *
 * For example:
 *
 * #.##..##.
 * ..#.##.#.
 * ##......#
 * ##......#
 * ..#.##.#.
 * ..##..##.
 * #.#.##.#.
 *
 * #...##..#
 * #....#..#
 * ..##..###
 * #####.##.
 * #####.##.
 * ..##..###
 * #....#..#
 *
 * To find the reflection in each pattern, you need to find a perfect reflection across either a horizontal line between two rows or across a vertical line between two columns.
 *
 * In the first pattern, the reflection is across a vertical line between two columns; arrows on each of the two columns point at the line between the columns:
 *
 * 123456789
 *     ><
 * #.##..##.
 * ..#.##.#.
 * ##......#
 * ##......#
 * ..#.##.#.
 * ..##..##.
 * #.#.##.#.
 *     ><
 * 123456789
 *
 * In this pattern, the line of reflection is the vertical line between columns 5 and 6. Because the vertical line is not perfectly in the middle of the pattern, part of the pattern (column 1) has nowhere to reflect onto and can be ignored; every other column has a reflected column within the pattern and must match exactly: column 2 matches column 9, column 3 matches 8, 4 matches 7, and 5 matches 6.
 *
 * The second pattern reflects across a horizontal line instead:
 *
 * 1 #...##..# 1
 * 2 #....#..# 2
 * 3 ..##..### 3
 * 4v#####.##.v4
 * 5^#####.##.^5
 * 6 ..##..### 6
 * 7 #....#..# 7
 *
 * This pattern reflects across the horizontal line between rows 4 and 5. Row 1 would reflect with a hypothetical row 8, but since that's not in the pattern, row 1 doesn't need to match anything. The remaining rows match: row 2 matches row 7, row 3 matches row 6, and row 4 matches row 5.
 */
public class reflection1 {

    public static void main(String... args) throws IOException {
        var input = Files.readAllLines(Path.of("input2.txt"));

        // separate to chunks separated by a newline, create a bitmap
        // as well as transposed bitmap of each chunk
        var bitmaps = input.stream().reduce(new ArrayList<List<String>>(), (acc, line) -> {
            if (line.isBlank()) {
                acc.add(new ArrayList<>());
                return acc;
            } else {
                if (acc.isEmpty()) {
                    acc.add(new ArrayList<>());
                }
                var last = acc.getLast();
                var updated = last.add(line);
                return acc;
            }
        }, (a, b) -> a).stream().map(Mirrors::fromString).toList();
        var result1 = bitmaps.stream().mapToInt(Mirrors::symmetryScore).sum();
        System.out.println(result1);
        var result2 = bitmaps.stream().mapToInt(Mirrors::adjustedScore).sum();
        System.out.println(result2);
    }

    record Mirrors(Bitmap bitmap, Bitmap transposed) {
        public static Mirrors fromString(List<String> input) {
            var bitmap = Bitmap.fromString(input.stream());
            var transposed = Bitmap.fromString(transpose(input));
            return new Mirrors(bitmap, transposed);
        }

        private static Stream<String> transpose(List<String> input) {
            int size = input.getFirst().length();
            List<StringBuffer> buffers = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                buffers.add(new StringBuffer());
            }
            for (String line : input) {
                for (int i = 0; i < size; i++) {
                    buffers.get(i).append(line.charAt(i));
                }
            }
            return buffers.stream().map(StringBuffer::toString);
        }

        int symmetryScore() {
            var vertical = bitmap.verticalSymmetry();
            var horizontal = transposed.verticalSymmetry();
            if (horizontal.size > vertical.size && horizontal.size > 0) {
                return horizontal.line;
            } else if (vertical.size > 0){
                return vertical.line*100;
            }
            return 0;
        }

        int adjustedScore() {
            var vertical = bitmap.fixSmudge();
            var horizontal = transposed.fixSmudge();
            if (horizontal.size > vertical.size && horizontal.size > 0) {
                return horizontal.line;
            } else if (vertical.size > 0){
                return vertical.line*100;
            }
            return 0;
        }
    }

    record Symmetry(int line, int size) {}

    record Bitmap(List<BigInteger> representation) {
        public static Bitmap fromString(Stream<String> input) {
            var converted = input.map(s -> s.replace(".", "0").replace("#", "1"))
                    .map(s -> new BigInteger(s, 2));
            return new Bitmap(converted.toList());
        }

        Symmetry verticalSymmetry() {
            int max = 0;
            int symmetryLine = 0;
            for (int i = representation.size() / 2 + 1; i > 0; i--) {
                var sym1 = symmetryAroundLine(i);
                var sym2 = symmetryAroundLine(representation.size() - i);
                if (sym1 > max) {
                    max = sym1;
                    symmetryLine = i;
                }
                if (sym2 > max) {
                    max = sym2;
                    symmetryLine = representation().size() - i;
                }
                if (max >= i) {
                    break;
                }
            }
            return new Symmetry(symmetryLine, max * 2);
        }

        // returns the number of elements that are symmetric around the centerline between
        // two elements only if symmetry goes to either end of the bitmapm 0 otherwise
        int symmetryAroundLine(int centerline) {
            var limit = Math.min(centerline, representation.size() - centerline);
            for (int i = 0; i < limit; i++) {
                if (!representation.get(centerline - i - 1).equals(representation.get(centerline + i))) {
                    return 0;
                }
            }
            return limit;
        }

        /**
         * In each pattern, you'll need to locate and fix the smudge that causes a different reflection line to be valid. (The old reflection line won't necessarily continue being valid after the smudge is fixed.)
         * <p>
         * Here's the above example again:
         * <p>
         * #.##..##.
         * ..#.##.#.
         * ##......#
         * ##......#
         * ..#.##.#.
         * ..##..##.
         * #.#.##.#.
         * <p>
         * #...##..#
         * #....#..#
         * ..##..###
         * #####.##.
         * #####.##.
         * ..##..###
         * #....#..#
         * <p>
         * The first pattern's smudge is in the top-left corner. If the top-left # were instead ., it would have a different, horizontal line of reflection:
         * <p>
         * 1 ..##..##. 1
         * 2 ..#.##.#. 2
         * 3v##......#v3
         * 4^##......#^4
         * 5 ..#.##.#. 5
         * 6 ..##..##. 6
         * 7 #.#.##.#. 7
         * <p>
         * With the smudge in the top-left corner repaired, a new horizontal line of reflection between rows 3 and 4 now exists. Row 7 has no corresponding reflected row and can be ignored, but every other row matches exactly: row 1 matches row 6, row 2 matches row 5, and row 3 matches row 4.
         * <p>
         * In the second pattern, the smudge can be fixed by changing the fifth symbol on row 2 from . to #:
         * <p>
         * 1v#...##..#v1
         * 2^#...##..#^2
         * 3 ..##..### 3
         * 4 #####.##. 4
         * 5 #####.##. 5
         * 6 ..##..### 6
         * 7 #....#..# 7
         * <p>
         * Now, the pattern has a different horizontal line of reflection between rows 1 and 2.
         * <p>
         * Summarize your notes as before, but instead use the new different reflection lines. In this example, the first pattern's new horizontal line has 3 rows above it and the second pattern's new horizontal line has 1 row above it, summarizing to the value 400.
         *
         * @return
         */
        public Symmetry fixSmudge() {
            int max = 0;
            int symmetryLine = 0;
            for (int i = representation.size() / 2 + 1; i > 0; i--) {
                var sym2 = symmetryWithSmudge(representation.size() - i);
                var sym1 = symmetryWithSmudge(i);
                if (sym1 > max) {
                    max = sym1;
                    symmetryLine = i;
                }
                if (sym2 > max) {
                    max = sym2;
                    symmetryLine = representation().size() - i;
                }
                if (max >= i) {
                    break;
                }
            }
            return new Symmetry(symmetryLine, max * 2);
        }


        // returns the number of elements that are symmetric around the centerline between
        // two elements only if symmetry goes to either end of the bitmapm 0 otherwise
        int symmetryWithSmudge(int centerline) {
            var limit = Math.min(centerline, representation.size()-centerline);
            Correction result = null;
            for (int i = 0; i < limit; i++) {

                BigInteger line1 = representation.get(centerline - i - 1);
                BigInteger line2 = representation.get(centerline + i);
                var possibleSmudge = line1.xor(line2);
                if (possibleSmudge.bitCount() == 1) {
                    var bitIndex = possibleSmudge.getLowestSetBit();
                    // the smudge is fixed by flipping 0 to 1
                    if (!line1.testBit(bitIndex)) {
                        return swapLine(centerline - i - 1, line1.setBit(bitIndex)).symmetryAroundLine(centerline);
                    } else {
                        return swapLine(centerline + i, line2.setBit(bitIndex)).symmetryAroundLine(centerline);
                    }
                } else if (!BigInteger.ZERO.equals(possibleSmudge)) {
                    return 0;
                }

            }
            return 0;
        }

        Bitmap swapLine(int line, BigInteger newLine) {
            var newRepresentation = new ArrayList<>(representation);
            newRepresentation.set(line, newLine);
            return new Bitmap(newRepresentation);
        }

        record Correction(int line, int bit) {}
    }

}
