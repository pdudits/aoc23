///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.System.*;

public class map1 {

    public static void main(String... args) throws IOException {
        var input = new PipeMap(Files.lines(Path.of("input2.txt")));
        var distances = DistanceMap.forMap(input);


        findLoop(input, distances);
        printLoop(input, distances);
        countEnclosed(input, distances);
        printLoop(input, distances);
    }

    private static void countEnclosed(PipeMap input, DistanceMap distances) {
        var enclosed = 0;
        for (int y=0; y<input.height(); y++) {
            boolean enclosing = false;
            for(int x=0; x <input.width(); x++) {
                var cell = input.cell(new Coord(x,y));
                if (distances.isLoop(x,y)) {
                    if (cell.connectsTo(Direction.S)) {
                        enclosing = !enclosing;
                    }
                } else if (enclosing) {
                    distances.enclosed(x,y);
                    enclosed++;
                }
            }
        }
        out.println(enclosed);
    }

    private static void printLoop(PipeMap input, DistanceMap distances) {
        for(int y=0; y < input.height(); y++) {
            var line = new StringBuffer();
            input.chars(y).chars().map(Cell::prettyChar).forEach(c -> line.append((char)c));
            for(int x=0; x < line.length(); x++) {
                if (!distances.isLoop(x, y)) {
                    line.setCharAt(x, '.');
                }
                if (distances.isEnclosed(x, y)) {
                    line.setCharAt(x, '░');
                }
            }
            out.println(line);
        }
    }

    private static void findLoop(PipeMap input, DistanceMap distances) {
        var start = input.start();
        distances.startAt(start);
        var cursors = Direction.stream().map(d -> input.connects(start, d)).filter(c -> c != null).toArray(Cursor[]::new);
        if (cursors.length != 2) {
            throw new AssertionError("Expected 2 starting directions");
        }
        for(;;) {
            for(int i=0; i<cursors.length; i++) {
                var cursor = cursors[i];
                if (distances.closedLoop(cursors[i])) {
                    out.println(cursors[i].distance);
                    return;
                }
                var options = Direction.stream().map(d -> input.connects(cursor, d)).filter(c -> c != null).toArray(Cursor[]::new);
                if (options.length != 1) {
                    throw new AssertionError("There should be only one way to go: %s -> %s".formatted(cursors[i], Arrays.toString(options)));
                }
                cursors[i] = options[0];
            }
        }
    }

    record DistanceMap(int[][] distances) {
        static DistanceMap forMap(PipeMap map) {
            int[][] distances = new int[map.height()][map.width()];
            return new DistanceMap(distances);
        }

        int value(Coord c) {
            return distances[c.y][c.x];
        }

        void set(Coord c, int d) {
            distances[c.y][c.x] = d;
        }

        boolean isLoop(int x, int y) {
            return distances[y][x] > 0 || distances[y][x] == -1;
        }

        boolean closedLoop(Cursor c) {
            int d = value(c.coord);
            if (d == 0) {
                set(c.coord, c.distance);
                return false;
            } else {
                return true;
            }
        }

        public void startAt(Cursor start) {
            set(start.coord, -1);
        }

        public void enclosed(int x, int y) {
            set(new Coord(x,y), -2);
        }

        public boolean isEnclosed(int x, int y) {
            return distances[y][x] == -2;
        }
    }

    record PipeMap(List<String> map) {

        PipeMap(Stream<String> streamedInput) {
            this(streamedInput.toList());

        }
        int width() {
            return map.getFirst().length();
        }

        int height() {
            return map.size();
        }

        Cell cell(Coord c) {
            return Cell.from(map.get(c.y).charAt(c.x));
        }

        boolean validCoord(Coord c) {
            return c.x >= 0 && c.x < width() && c.y >= 0 && c.y < height();
        }

        Cursor start() {
            // index of line with "S"
            for(int y = 0; y < height(); y++) {
                var line = map.get(y);
                if (line.contains("S")) {
                    return new Cursor(new Coord(line.indexOf('S'),y),0, null, Cell.START);
                }
            }
            throw new IllegalArgumentException("No start position found");
        }

        Cursor connects(Cursor c, Direction d) {
            if (d.reverse() == c.previous()) {
                return null;
            }
            var currentCell = cell(c.coord);
            if (!currentCell.connectsTo(d)) {
                return null;
            }
            var newCoord = d.next(c.coord);
            if (!validCoord(newCoord)) {
                return null;
            }
            var newCell = cell(newCoord);
            if (!newCell.connectsTo(d.reverse())) {
                return null;
            }
            return new Cursor(newCoord, c.distance+1, d, newCell);
        }

        public CharSequence chars(int y) {
            return map.get(y);
        }
    }

    record Coord(int x, int y) {}

    enum Cell {
        NS,EW, NE, NW, SE, SW, EMPTY, START;

        static Cell from(char mapSymbol) {
            return switch(mapSymbol) {
                case '|' -> NS;
                case '-' -> EW;
                case 'L' -> NE;
                case 'J' -> NW;
                case '7' -> SW;
                case 'F' -> SE;
                case '.' -> EMPTY;
                case 'S' -> START;
                default -> throw new IllegalArgumentException("WTF is "+mapSymbol);
            };
        }

        static char prettyChar(int mapSymbol) {
            return switch(mapSymbol) {
                case '|' -> '│';
                case '-' -> '─';
                case 'L' -> '└';
                case 'J' -> '┘';
                case '7' -> '┐';
                case 'F' -> '┌';
                case '.' -> '.';
                case 'S' -> '◇';
                default -> throw new IllegalArgumentException("WTF is "+mapSymbol);
            };
        }

        boolean connectsTo(Direction d) {
            return switch (this) {
                case NS -> d == Direction.N || d == Direction.S;
                case EW -> d == Direction.E || d == Direction.W;
                case NE -> d == Direction.N || d == Direction.E;
                case NW -> d == Direction.N || d == Direction.W;
                case SE -> d == Direction.S || d == Direction.E;
                case SW -> d == Direction.S || d == Direction.W;
                case EMPTY -> false;
                case START -> true;
            };
        }
    }

    enum Direction {
        N, W, E, S;

        Coord next(Coord current) {
            return switch(this) {
                case N -> new Coord(current.x, current.y-1);
                case W -> new Coord(current.x-1, current.y);
                case E -> new Coord(current.x+1, current.y);
                case S -> new Coord(current.x, current.y+1);
            };
        }

        Direction reverse() {
            return switch(this) {
                case N -> S;
                case E -> W;
                case W -> E;
                case S -> N;
            };
        }

        static Stream<Direction> stream() {
            return Arrays.stream(values());
        }
    }

    record Cursor(Coord coord, int distance, Direction previous, Cell c) {

    }
}
