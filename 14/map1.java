///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.System.*;

/**
 * In short: if you move the rocks, you can focus the dish. The platform even has a control panel on the side that lets you tilt it in one of four directions! The rounded rocks (O) will roll when the platform is tilted, while the cube-shaped rocks (#) will stay in place. You note the positions of all of the empty spaces (.) and rocks (your puzzle input). For example:
 * <p>
 * O....#....
 * O.OO#....#
 * .....##...
 * OO.#O....O
 * .O.....O#.
 * O.#..O.#.#
 * ..O..#O..O
 * .......O..
 * #....###..
 * #OO..#....
 * <p>
 * Start by tilting the lever so all of the rocks will slide north as far as they will go:
 * <p>
 * OOOO.#.O..
 * OO..#....#
 * OO..O##..O
 * O..#.OO...
 * ........#.
 * ..#....#.#
 * ..O..#.O.O
 * ..O.......
 * #....###..
 * #....#....
 * <p>
 * You notice that the support beams along the north side of the platform are damaged; to ensure the platform doesn't collapse, you should calculate the total load on the north support beams.
 * <p>
 * The amount of load caused by a single rounded rock (O) is equal to the number of rows from the rock to the south edge of the platform, including the row the rock is on. (Cube-shaped rocks (#) don't contribute to load.) So, the amount of load caused by each rock in each row is as follows:
 * <p>
 * OOOO.#.O.. 10
 * OO..#....#  9
 * OO..O##..O  8
 * O..#.OO...  7
 * ........#.  6
 * ..#....#.#  5
 * ..O..#.O.O  4
 * ..O.......  3
 * #....###..  2
 * #....#....  1
 * <p>
 * The total load is the sum of the load caused by all of the rounded rocks. In this example, the total load is 136.
 * <p>
 * Tilt the platform so that the rounded rocks all roll north. Afterward, what is the total load on the north support beams?
 * <p>
 * The parabolic reflector dish deforms, but not in a way that focuses the beam. To do that, you'll need to move the rocks to the edges of the platform. Fortunately, a button on the side of the control panel labeled "spin cycle" attempts to do just that!
 * <p>
 * Each cycle tilts the platform four times so that the rounded rocks roll north, then west, then south, then east. After each tilt, the rounded rocks roll as far as they can before the platform tilts in the next direction. After one cycle, the platform will have finished rolling the rounded rocks in those four directions in that order.
 * <p>
 * Here's what happens in the example above after each of the first few cycles:
 * <p>
 * After 1 cycle:
 * .....#....
 * ....#...O#
 * ...OO##...
 * .OO#......
 * .....OOO#.
 * .O#...O#.#
 * ....O#....
 * ......OOOO
 * #...O###..
 * #..OO#....
 * <p>
 * After 2 cycles:
 * .....#....
 * ....#...O#
 * .....##...
 * ..O#......
 * .....OOO#.
 * .O#...O#.#
 * ....O#...O
 * .......OOO
 * #..OO###..
 * #.OOO#...O
 * <p>
 * After 3 cycles:
 * .....#....
 * ....#...O#
 * .....##...
 * ..O#......
 * .....OOO#.
 * .O#...O#.#
 * ....O#...O
 * .......OOO
 * #...O###.O
 * #.OOO#...O
 * <p>
 * This process should work if you leave it running long enough, but you're still worried about the north support beams. To make sure they'll survive for a while, you need to calculate the total load on the north support beams after 1000000000 cycles
 */
public class map1 {

    public static void main(String... args) throws IOException {
        var map = new RockMap(Files.readAllLines(Path.of("input2.txt")));
        var m1 = map.tilt(Direction.NORTH);
        out.println(m1.northSupportBeamLoad());
        out.println(map.tiltedNorthSupportBeamLoad());
        out.println(billionCycles(map));
    }

    static long billionCycles(RockMap map) {
        var history = new HashSet<Cached>();
        var target = 4000000000L;
        boolean caching = true;
        for (long i = 0; i < target; i++) {
            var direction = Direction.values()[(int) (i % 4)];
            map = map.tilt(direction);
            var key = new Cached(map, direction, i);
            if (caching && history.contains(key)) {
                var cached = history.stream().filter(c -> c.equals(key)).findFirst().get();
                out.println("Found cycle at " + i + " with " + cached.iteration + " " + direction);
                caching = false;
                var modulo = i - cached.iteration;
                var remainingSteps = target - cached.iteration - 1;
                target = i + (remainingSteps) % modulo;
            }
            history.add(key);
        }
        return map.northSupportBeamLoad();
    }

    record ComputationKey(RockMap map, Direction direction) {
    }

    static class Cached {
        final ComputationKey key;
        final long iteration;

        Cached(ComputationKey key, long iteration) {
            this.key = key;
            this.iteration = iteration;
        }

        public Cached(RockMap map, Direction direction, long i) {
            this(new ComputationKey(map, direction), i);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Cached cached = (Cached) o;
            return Objects.equals(key, cached.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }

    record Point(int x, int y) {

        static Stream<Point> initials(RockMap map, Direction direction) {
            return switch (direction) {
                case NORTH -> IntStream.range(0, map.width()).mapToObj(x -> new Point(x, 0));
                case SOUTH -> IntStream.range(0, map.width()).mapToObj(x -> new Point(x, map.height() - 1));
                case EAST -> IntStream.range(0, map.height()).mapToObj(y -> new Point(map.width() - 1, y));
                case WEST -> IntStream.range(0, map.height()).mapToObj(y -> new Point(0, y));
            };
        }

        Point next(Direction direction) {
            return switch (direction) {
                case SOUTH -> new Point(x, y - 1);
                case NORTH -> new Point(x, y + 1);
                case WEST -> new Point(x + 1, y);
                case EAST -> new Point(x - 1, y);
            };
        }
    }

    enum Direction {
        NORTH, WEST, SOUTH, EAST;
    }

    record RockMap(List<String> lines) {
        RockMap cycle() {
            var map = this;
            for (var direction : Direction.values()) {
                map = map.tilt(direction);
            }
            return map;
        }

        void print() {
            lines.forEach(out::println);
        }

        int width() {
            return lines.get(0).length();
        }

        int height() {
            return lines.size();
        }

        void move(Point from, Point to) {
            if (from.equals(to)) {
                return;
            }
            var fromLine = lines.get(from.y);
            var toLine = lines.get(to.y);
            if (fromLine.charAt(from.x) != 'O') {
                throw new IllegalArgumentException("No rock at " + from);
            }
            if (toLine.charAt(to.x) != '.') {
                throw new IllegalArgumentException("Can't move rock to " + to);
            }
            lines.set(from.y, fromLine.substring(0, from.x) + '.' + fromLine.substring(from.x + 1));
            if (from.y == to.y) {
                // we just overwritten the source line, so we need to get it again
                toLine = lines.get(to.y);
            }
            lines.set(to.y, toLine.substring(0, to.x) + 'O' + toLine.substring(to.x + 1));
        }

        char charAt(Point point) {
            return lines.get(point.y).charAt(point.x);
        }

        RockMap tilt(Direction direction) {
            var newMap = new RockMap(new ArrayList<>(lines));

            Point.initials(this, direction).forEach(initial -> {
                var current = initial;
                var moveTo = initial;
                while (true) {
                    var next = current.next(direction);
                    if (newMap.charAt(current) == '#') {
                        moveTo = next;
                    }
                    if (newMap.charAt(current) == 'O') {
                        newMap.move(current, moveTo);
                        moveTo = moveTo.next(direction);
                    }
                    current = next;
                    if (next.x < 0 || next.x >= width() || next.y < 0 || next.y >= height()) {
                        break;
                    }
                }
            });
            return newMap;
        }

        long tiltedNorthSupportBeamLoad() {
            long sum = 0;
            for (int i = 0; i < width(); i++) {
                sum += tiltedNorthSupportBeamLoad(i);
            }
            return sum;
        }

        private long tiltedNorthSupportBeamLoad(int x) {
            long sum = 0;
            int weight = height();
            for (int y = 0; y < height(); y++) {
                if (lines.get(y).charAt(x) == 'O') {
                    sum += (weight--);
                }
                if (lines.get(y).charAt(x) == '#') {
                    weight = height() - y - 1;
                }
            }
            return sum;
        }

        long northSupportBeamLoad() {
            var sum = 0;
            for (int i = height(); i > 0; i--) {
                var line = lines.get(height() - i);
                for (int j = 0; j < width(); j++) {
                    if (line.charAt(j) == 'O') {
                        sum += i;
                    }
                }
            }
            return sum;
        }
    }
}
