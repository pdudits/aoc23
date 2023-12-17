///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.System.*;
import static java.util.stream.Collectors.toMap;

/**
 * Fortunately, the Elves here have a map (your puzzle input) that uses traffic patterns, ambient temperature, and hundreds of other parameters to calculate exactly how much heat loss can be expected for a crucible entering any particular city block.
 * <p>
 * For example:
 * <p>
 * 2413432311323
 * 3215453535623
 * 3255245654254
 * 3446585845452
 * 4546657867536
 * 1438598798454
 * 4457876987766
 * 3637877979653
 * 4654967986887
 * 4564679986453
 * 1224686865563
 * 2546548887735
 * 4322674655533
 * <p>
 * Each city block is marked by a single digit that represents the amount of heat loss if the crucible enters that block. The starting point, the lava pool, is the top-left city block; the destination, the machine parts factory, is the bottom-right city block. (Because you already start in the top-left block, you don't incur that block's heat loss unless you leave that block and then return to it.)
 * <p>
 * Because it is difficult to keep the top-heavy crucible going in a straight line for very long, it can move at most three blocks in a single direction before it must turn 90 degrees left or right. The crucible also can't reverse direction; after entering each city block, it may only turn left, continue straight, or turn right.
 */
public class map1 {

    public static void main(String... args) throws IOException {
        var puzzle = new Puzzle(parse(Files.readAllLines(Path.of("input2.txt"))));
        out.println(puzzle.ultraCrubicle());
    }

    record Goal(Position position, int cost) {  }
    static class Puzzle {
        CostMap costs;
        Map<State, CostMap> limits = new HashMap<>();

        PriorityQueue<Cursor> searchSpace = new PriorityQueue<>(Comparator.reverseOrder());

        Puzzle(CostMap costs) {
            this.costs = costs;
        }

        long normalCrubicle() {
            var start = new Cursor(new Position(0, 0), null, 0, null);
            searchSpace.add(start.apply(new State(Direction.RIGHT, 3, 1), costs));
            searchSpace.add(start.apply(new State(Direction.DOWN, 3, 1), costs));
            return minimumCost(State::next, new Goal(new Position(costs.width() - 1, costs.height() - 1), 0));
        }

        long ultraCrubicle() {
            var start = new Cursor(new Position(0, 0), null, 0, null);
            searchSpace.add(start.apply(new State(Direction.RIGHT, 10, 4), costs));
            searchSpace.add(start.apply(new State(Direction.DOWN, 10, 4), costs));
            return minimumCost(State::nextUltra, new Goal(new Position(costs.width() - 1, costs.height() - 1), 0));
        }

        long minimumCost(Function<State, Stream<State>> transitionFunction, Goal goal) {
            Cursor solution = null;
            Cursor bestCursor = null;
            while (!searchSpace.isEmpty()) {
                var cursor = searchSpace.remove();
                if (solution != null && cursor.cost() >= solution.cost()) {
                    // we already have a better solution
                    continue;
                }
                if (bestCursor == null || bestCursor.compareTo(cursor) < 0) {
                    bestCursor = cursor;
                    out.println("%d: Best position %s %d".formatted(searchSpace.size(), cursor.position, cursor.cost));
                }
                if (cursor.position.equals(goal.position)) {
                    searchSpace.removeIf(c -> c.cost() >= cursor.cost());
                    if (solution != null && solution.cost() <= cursor.cost()) {
                        // we already have a better solution
                        continue;
                    }
                    // we hit the target, remove any other paths that are more expensive
                    solution = cursor;
                    out.println("Found solution %s".formatted(solution));
                    //solution.print(costs);
                    continue;
                }

                transitionFunction.apply(cursor.state()).forEach(s -> {
                    var next = cursor.apply(s, costs);
                    if (next != null) {
                        var limit = limits.computeIfAbsent(next.state, st -> CostMap.max(costs));
                        if (limit.update(next.position, next.cost)) {
                            searchSpace.add(next);
                        }
                    }
                });
            }
            return solution.cost;
        }
    }

    static CostMap parse(List<String> input) {
        var costs = new int[input.size()][input.get(0).length()];
        for (int y = 0; y < input.size(); y++) {
            var line = input.get(y);
            for (int x = 0; x < line.length(); x++) {
                costs[y][x] = line.charAt(x) - '0';
            }
        }
        return new CostMap(costs);
    }

    record CostMap(int[][] costs) {
        boolean isValid(Position position) {
            return position.x() >= 0 && position.x() < width() && position.y() >= 0 && position.y() < height();
        }

        int cost(Position position) {
            return costs[position.y()][position.x()];
        }

        int width() {
            return costs[0].length;
        }

        int height() {
            return costs.length;
        }

        boolean update(Position position, int cost) {
            if (costs[position.y()][position.x()] > cost) {
                costs[position.y()][position.x()] = cost;
                return true;
            }
            return false;
        }

        static CostMap max(CostMap dimensions) {
            var costs = new int[dimensions.height()][dimensions.width()];
            for (int y = 0; y < dimensions.height(); y++) {
                Arrays.fill(costs[y], Integer.MAX_VALUE);
            }
            return new CostMap(costs);
        }
    }

    enum Direction {
        UP, DOWN, LEFT, RIGHT;

        Direction opposite() {
            return switch (this) {
                case UP -> DOWN;
                case DOWN -> UP;
                case LEFT -> RIGHT;
                case RIGHT -> LEFT;
            };
        }
    }

    record Position(int x, int y) {
        Position next(Direction direction) {
            return switch (direction) {
                case UP -> new Position(x, y - 1);
                case DOWN -> new Position(x, y + 1);
                case LEFT -> new Position(x - 1, y);
                case RIGHT -> new Position(x + 1, y);
            };
        }
    }

    record Cursor(Position position, State state, int cost, Cursor previous) implements Comparable<Cursor>{

        Cursor apply(State state, CostMap costs) {
            var c = this;
            for (int i = 0; i < state.minimum; i++) {
                var p = c.position.next(state.direction);
                if (!costs.isValid(p)) {
                    return null;
                }
                c = new Cursor(p, state, c.cost + costs.cost(p), c);
            }
            return c;
        }

        @Override
        public int compareTo(Cursor cursor) {
            var distance = Integer.compare(position().x()+ position().y(), cursor.position().x()+ cursor.position().y());
            if (distance != 0) {
                return distance;
            }
            // if we are at the same distance, prefer the one with the lowest cost
            return Integer.compare(cursor.cost(), cost());
        }

        public void print(CostMap costs) {
            var cursor = this;
            var map = new int[costs.height()][costs.width()];
            while (cursor != null) {
                map[cursor.position.y()][cursor.position.x()] = cursor.cost;
                cursor = cursor.previous;
            }
            for (int y = 0; y < costs.height(); y++) {
                for (int x = 0; x < costs.width(); x++) {
                    out.print("%3d ".formatted(map[y][x]));
                }
                out.print("|");
                for (int x = 0; x < costs.width(); x++) {
                    out.print("%3d ".formatted(costs.cost(new Position(x, y))));
                }
                out.println();
            }
        }
    }

    record State(Direction direction, int limit, int minimum) {
        Stream<State> next() {
            return Arrays.stream(Direction.values())
                    .filter(d -> d != direction.opposite())
                    .filter(d -> d != direction || limit > 1)
                    .map(d -> new State(d, d == direction ? limit - 1 : 3, 1));
        }

        /**
         * Once an ultra crucible starts moving in a direction, it needs to move a minimum of four blocks in that direction before it can turn (or even before it can stop at the end). However, it will eventually start to get wobbly: an ultra crucible can move a maximum of ten consecutive blocks without turning.
         * @return
         */
        Stream<State> nextUltra() {
            return Arrays.stream(Direction.values())
                    .filter(d -> d != direction.opposite())
                    .filter(d -> d != direction || limit > 1)
                    .map(d -> d == direction ? new State(d, limit-minimum, 1) : new State(d, 10, 4));
        }
    }

}
