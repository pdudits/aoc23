///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.System.*;

/**
 * However, they aren't sure the lagoon will be big enough; they've asked you to take a look at the dig plan (your puzzle input). For example:
 *
 * R 6 (#70c710)
 * D 5 (#0dc571)
 * L 2 (#5713f0)
 * D 2 (#d2c081)
 * R 2 (#59c680)
 * D 2 (#411b91)
 * L 5 (#8ceee2)
 * U 2 (#caa173)
 * L 1 (#1b58a2)
 * U 2 (#caa171)
 * R 2 (#7807d2)
 * U 3 (#a77fa3)
 * L 2 (#015232)
 * U 2 (#7a21e3)
 *
 * The digger starts in a 1 meter cube hole in the ground. They then dig the specified number of meters up (U), down (D), left (L), or right (R), clearing full 1 meter cubes as they go. The directions are given as seen from above, so if "up" were north, then "right" would be east, and so on. Each trench is also listed with the color that the edge of the trench should be painted as an RGB hexadecimal color code.
 *
 * When viewed from above, the above example dig plan would result in the following loop of trench (#) having been dug out from otherwise ground-level terrain (.):
 *
 * #######
 * #.....#
 * ###...#
 * ..#...#
 * ..#...#
 * ###.###
 * #...#..
 * ##..###
 * .#....#
 * .######
 *
 * At this point, the trench could contain 38 cubic meters of lava. However, this is just the edge of the lagoon; the next step is to dig out the interior so that it is one meter deep as well:
 *
 * #######
 * #######
 * #######
 * ..#####
 * ..#####
 * #######
 * #####..
 * #######
 * .######
 * .######
 *
 * Now, the lagoon can contain a much more respectable 62 cubic meters of lava. While the interior is dug out, the edges are also painted according to the color codes in the dig plan.
 *
 * The Elves are concerned the lagoon won't be large enough; if they follow their dig plan, how many cubic meters of lava could it hold?
 */
public class lagoon1 {

    public static void main(String... args) throws IOException {
        var lines = otherParse(Files.readAllLines(Path.of("input2.txt")));
        var trench = new Trench(lines);
        out.println("topLeft: " + trench.topLeft());
        out.println("bottomRight: " + trench.bottomRight());
        out.println("area: " + trench.area());
    }

    static final class Trench {
        private final List<Line> outline;
        int[] verticals;
        int[] horizontals;

        Trench(List<Line> outline) {
            this.outline = outline;
            verticals = outline.stream().map(Line::start).map(p -> p.x).distinct().sorted().mapToInt(Integer::intValue).toArray();
            horizontals = outline.stream().map(Line::start).map(p -> p.y).distinct().sorted().mapToInt(Integer::intValue).toArray();
        }

        Point topLeft() {
                return outline.stream().map(Line::start).reduce((a, b) -> new Point(Math.min(a.x, b.x), Math.max(a.y, b.y))).get();
            }

            Point bottomRight() {
                return outline.stream().map(Line::start).reduce((a, b) -> new Point(Math.max(a.x, b.x), Math.min(a.y, b.y))).get();
            }

            BigDecimal area() {
                // pick's theorem, says reddit.
                // https://en.wikipedia.org/wiki/Pick%27s_theorem
                // shoelace area + half of perimeter + 1
                var shoelace = shoelaceArea();
                var perimeter = perimeter();
                return shoelace.add(BigDecimal.valueOf(perimeter / 2 + 1));
            }

            private long perimeter() {
                var result = 0L;
                for (var line : outline) {
                    result += line.length();
                }
                return result;
            }

        private BigDecimal shoelaceArea() {
            Point previous = null;
            var result = BigDecimal.ZERO;
            for (var line : outline) {
                var point = line.start;
                if (previous != null) {
                    result = result.add(BigDecimal.valueOf(previous.y + point.y).multiply(BigDecimal.valueOf(previous.x - point.x)));
                }
                previous = point;
            }
            return result.divide(BigDecimal.TWO).abs();
        }

        public List<Line> outline() {
            return outline;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Trench) obj;
            return Objects.equals(this.outline, that.outline);
        }

        @Override
        public int hashCode() {
            return Objects.hash(outline);
        }

        @Override
        public String toString() {
            return "Trench[" +
                    "outline=" + outline + ']';
        }

    }

    static List<Line> parse(List<String> input) {
      var start = new Point(0, 0);
      var result = new ArrayList<Line>();
      for (var line : input) {
          var parts = line.split("[\\s\\()]+");
          var direction = parts[0];
          var length = Integer.parseInt(parts[1]);
          var color = new Color(parts[2]);
            var end = switch (direction) {
                case "R" -> new Point(start.x + length, start.y);
                case "L" -> new Point(start.x - length, start.y);
                case "U" -> new Point(start.x, start.y + length);
                case "D" -> new Point(start.x, start.y - length);
                default -> throw new IllegalArgumentException("Unknown direction: " + direction);
            };
          result.add(new Line(start, end, color));
          start = end;
      }
      return result;
    }

    /**
     * After a few minutes, someone realizes what happened; someone swapped the color and instruction parameters when producing the dig plan. They don't have time to fix the bug; one of them asks if you can extract the correct instructions from the hexadecimal codes.
     *
     * Each hexadecimal code is six hexadecimal digits long. The first five hexadecimal digits encode the distance in meters as a five-digit hexadecimal number. The last hexadecimal digit encodes the direction to dig: 0 means R, 1 means D, 2 means L, and 3 means U.
     * @param input
     * @return
     */
    static List<Line> otherParse(List<String> input) {
        var start = new Point(0, 0);
        var result = new ArrayList<Line>();
        for (var line : input) {
            var parts = line.split("[\\s\\()]+");
            var direction = parts[2].charAt(6);
            var length = Integer.parseInt(parts[2].substring(1,6), 16);

            var end = switch (direction) {
                case '0' -> new Point(start.x + length, start.y);
                case '2' -> new Point(start.x - length, start.y);
                case '3' -> new Point(start.x, start.y + length);
                case '1' -> new Point(start.x, start.y - length);
                default -> throw new IllegalArgumentException("Unknown direction: " + direction);
            };
            result.add(new Line(start, end, null));
            start = end;
        }
        return result;
    }

    record Point(int x, int y) {}
    record Line(Point start, Point end, Color color) {

        public boolean onHorizontal(Point point) {
            if (start.y != end.y) {
                return false;
            }
            if (point.x == start.x) {
                return point.y >= Math.min(start.y, end.y) && point.y <= Math.max(start.y, end.y);
            } else if (point.y == start.y) {
                return point.x >= Math.min(start.x, end.x) && point.x <= Math.max(start.x, end.x);
            } else {
                return false;
            }
        }

        public boolean intersectsHorizontal(Point point) {
            if (start.x == end.x && start.x == point.x) {
                return Math.min(start.y,end.y) < point.y && point.y < Math.max(start.y,end.y);
            } else {
                return false;
            }
        }

        public int length() {
            return Math.abs(start.x - end.x) + Math.abs(start.y - end.y);
        }

        public Line extend() {
            if (start.x == end.x) {
                return new Line(start, new Point(end.x, end.y + (end.y > start.y ? 1 : -1)), color);
            } else {
                return new Line(start, new Point(end.x + (end.x > start.x ? 1 : -1), end.y), color);
            }
        }

        public Line shortenVertical() {
            return new Line(start, new Point(end.x, start.y < end.y ? end.y - 1 : end.y + 1), color);
        }

        public Line shortenHorizontal() {
            return new Line(start, new Point(start.x < end.x ? end.x - 1 : end.x + 1, end.y), color);
        }
    }
    record Color(String color) {}
}
