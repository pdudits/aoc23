///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.System.*;

/**
 * You note the layout of the contraption (your puzzle input). For example:
 *
 * .|...\....
 * |.-.\.....
 * .....|-...
 * ........|.
 * ..........
 * .........\
 * ..../.\\..
 * .-.-/..|..
 * .|....-|.\
 * ..//.|....
 *
 * The beam enters in the top-left corner from the left and heading to the right. Then, its behavior depends on what it encounters as it moves:
 *
 *     If the beam encounters empty space (.), it continues in the same direction.
 *     If the beam encounters a mirror (/ or \), the beam is reflected 90 degrees depending on the angle of the mirror. For instance, a rightward-moving beam that encounters a / mirror would continue upward in the mirror's column, while a rightward-moving beam that encounters a \ mirror would continue downward from the mirror's column.
 *     If the beam encounters the pointy end of a splitter (| or -), the beam passes through the splitter as if the splitter were empty space. For instance, a rightward-moving beam that encounters a - splitter would continue in the same direction.
 *     If the beam encounters the flat side of a splitter (| or -), the beam is split into two beams going in each of the two directions the splitter's pointy ends are pointing. For instance, a rightward-moving beam that encounters a | splitter would split into two beams: one that continues upward from the splitter's column and one that continues downward from the splitter's column.
 */
public class mirror1 {

    public static void main(String... args) throws IOException {
        var mirror = new Mirror(Files.readAllLines(Path.of("input2.txt")));
        var maximum = Arrays.stream(Direction.values())
                .flatMap(d -> d.initials(mirror))
                .mapToLong(r -> energizedTiles(mirror, r))
                .max().getAsLong();
        out.println(maximum);
    }

    static long energizedTiles(Mirror mirror, Ray initial) {
        var rays = new ArrayList<Ray>();
        rays.add(initial);
        EnergyMap map = new EnergyMap(mirror);
        while (!rays.isEmpty()) {
            for(var it = rays.listIterator();it.hasNext();) {
                var ray = it.next();
                switch (ray.propagate(mirror)) {
                    case Propagation.Continue c -> {
                        map.register(ray);
                        it.set(c.ray());
                    }
                    case Propagation.Split s -> {
                        map.register(ray);
                        it.remove();
                        it.add(s.ray1());
                        it.add(s.ray2());
                    }
                    case Propagation.Exit e -> {
                        it.remove();
                    }
                    case Propagation.Loop l -> {
                        it.remove();
                    }
                }
            }
        }
        return map.total();
    }


    record Point(int x, int y) {};
    enum Direction {
        UP, DOWN, LEFT, RIGHT;
        Point next(Point p) {
            return switch (this) {
                case UP -> new Point(p.x, p.y - 1);
                case DOWN -> new Point(p.x, p.y + 1);
                case LEFT -> new Point(p.x - 1, p.y);
                case RIGHT -> new Point(p.x + 1, p.y);
            };
        }

        Stream<Ray> initials(Mirror mirror) {
            return switch (this) {
                case UP -> IntStream.range(0, mirror.width()).mapToObj(x -> new Ray(new Point(x, mirror.height() - 1), this));
                case DOWN -> IntStream.range(0, mirror.width()).mapToObj(x -> new Ray(new Point(x, 0), this));
                case RIGHT -> IntStream.range(0, mirror.height()).mapToObj(y -> new Ray(new Point(0, y), this));
                case LEFT -> IntStream.range(0, mirror.height()).mapToObj(y -> new Ray(new Point(mirror.width() - 1, y), this));
            };
        }
    }

    record Ray(Point position, Direction direction, Set<Ray> previous) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Ray ray = (Ray) o;
            return Objects.equals(position, ray.position) && direction == ray.direction && previous == ray.previous;
        }

        @Override
        public int hashCode() {
            return Objects.hash(position, direction);
        }

        @Override
        public String toString() {
            return "Ray{" +
                    "position=" + position +
                    ", direction=" + direction +
                    '}';
        }

        Propagation propagate(Mirror mirror) {
            if (!mirror.isValid(position)) {
                return new Propagation.Exit();
            }

            char c = mirror.charAt(position);
            return switch (c) {
                case '.' -> follow(direction);
                case '/' -> follow(switch (direction) {
                    case UP -> Direction.RIGHT;
                    case DOWN -> Direction.LEFT;
                    case LEFT -> Direction.DOWN;
                    case RIGHT -> Direction.UP;
                });
                case '\\' -> follow(switch (direction) {
                    case UP -> Direction.LEFT;
                    case DOWN -> Direction.RIGHT;
                    case LEFT -> Direction.UP;
                    case RIGHT -> Direction.DOWN;
                });
                case '|' -> switch(direction) {
                    case UP, DOWN -> follow(direction);
                    case LEFT, RIGHT -> split(Direction.UP, Direction.DOWN);
                };
                case '-' -> switch(direction) {
                    case UP, DOWN -> split(Direction.LEFT, Direction.RIGHT);
                    case LEFT, RIGHT -> follow(direction);
                };
                default -> throw new IllegalArgumentException("Unknown char: " + c);
            };
        }

        Propagation split(Direction direction1, Direction direction2) {
            Propagation follow1 = follow(direction1);
            Propagation follow2 = follow(direction2);
            if (follow1 instanceof Propagation.Continue c1 && follow2 instanceof Propagation.Continue c2) {
                return new Propagation.Split(c1.ray(), c2.ray());
            } else if (follow1 instanceof Propagation.Continue c1 && follow2 instanceof Propagation.Loop) {
                return new Propagation.Continue(c1.ray());
            } else if (follow1 instanceof Propagation.Loop && follow2 instanceof Propagation.Continue c2) {
                return new Propagation.Continue(c2.ray());
            } else if (follow1 instanceof Propagation.Loop && follow2 instanceof Propagation.Loop) {
                return new Propagation.Loop();
            }
            throw new IllegalStateException("Unexpected combination: " + follow1 + " and " + follow2);
        }

        Propagation follow(Direction direction) {
            previous.add(this);
            var next = new Ray(direction.next(position), direction, previous);
            if (previous.contains(next)) {
                return new Propagation.Loop();
            } else {
                return new Propagation.Continue(next);
            }
        }

        Ray(Point position, Direction direction) {
            this(position, direction, new HashSet<>());
        }
    }

    sealed interface Propagation {
        record Continue(Ray ray) implements Propagation {}
        record Split(Ray ray1, Ray ray2) implements Propagation {}

        record Exit() implements Propagation {}

        record Loop() implements Propagation {}
    }

    record Mirror(List<String> lines) {

        boolean isValid(Point p) {
            return p.x >= 0 && p.x < width() && p.y >= 0 && p.y < height();
        }
        char charAt(int x, int y) {
            return lines.get(y).charAt(x);
        }

        int width() {
            return lines.get(0).length();
        }

        int height() {
            return lines.size();
        }

        public char charAt(Point position) {
            return charAt(position.x, position.y);
        }
    }

    record EnergyMap(int[][] map) {
        EnergyMap(Mirror mirror) {
            this(new int[mirror.height()][mirror.width()]);
        }
        void register(Ray ray) {
            map[ray.position.y()][ray.position.x()]=1;
        }

        long total() {
            return Arrays.stream(map).flatMapToInt(Arrays::stream).sum();
        }
    }
}
