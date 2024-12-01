///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.*;

/**
 * The Elves resume water filtering operations! Clean water starts flowing over the edge of Island Island.
 *
 * They offer to help you go over the edge of Island Island, too! Just hold on tight to one end of this impossibly long rope and they'll lower you down a safe distance from the massive waterfall you just created.
 *
 * As you finally reach Snow Island, you see that the water isn't really reaching the ground: it's being absorbed by the air itself. It looks like you'll finally have a little downtime while the moisture builds up to snow-producing levels. Snow Island is pretty scenic, even without any snow; why not take a walk?
 *
 * There's a map of nearby hiking trails (your puzzle input) that indicates paths (.), forest (#), and steep slopes (^, >, v, and <).
 *
 * For example:
 *
 * #.#####################
 * #.......#########...###
 * #######.#########.#.###
 * ###.....#.>.>.###.#.###
 * ###v#####.#v#.###.#.###
 * ###.>...#.#.#.....#...#
 * ###v###.#.#.#########.#
 * ###...#.#.#.......#...#
 * #####.#.#.#######.#.###
 * #.....#.#.#.......#...#
 * #.#####.#.#.#########v#
 * #.#...#...#...###...>.#
 * #.#.#v#######v###.###v#
 * #...#.>.#...>.>.#.###.#
 * #####v#.#.###v#.#.###.#
 * #.....#...#...#.#.#...#
 * #.#########.###.#.#.###
 * #...###...#...#...#.###
 * ###.###.#.###v#####v###
 * #...#...#.#.>.>.#.>.###
 * #.###.###.#.###.#.#v###
 * #.....###...###...#...#
 * #####################.#
 *
 * You're currently on the single path tile in the top row; your goal is to reach the single path tile in the bottom row. Because of all the mist from the waterfall, the slopes are probably quite icy; if you step onto a slope tile, your next step must be downhill (in the direction the arrow is pointing). To make sure you have the most scenic hike possible, never step onto the same tile twice. What is the longest hike you can take?
 *
 * In the example above, the longest hike you can take is marked with O, and your starting position is marked S:
 *
 * #S#####################
 * #OOOOOOO#########...###
 * #######O#########.#.###
 * ###OOOOO#OOO>.###.#.###
 * ###O#####O#O#.###.#.###
 * ###OOOOO#O#O#.....#...#
 * ###v###O#O#O#########.#
 * ###...#O#O#OOOOOOO#...#
 * #####.#O#O#######O#.###
 * #.....#O#O#OOOOOOO#...#
 * #.#####O#O#O#########v#
 * #.#...#OOO#OOO###OOOOO#
 * #.#.#v#######O###O###O#
 * #...#.>.#...>OOO#O###O#
 * #####v#.#.###v#O#O###O#
 * #.....#...#...#O#O#OOO#
 * #.#########.###O#O#O###
 * #...###...#...#OOO#O###
 * ###.###.#.###v#####O###
 * #...#...#.#.>.>.#.>O###
 * #.###.###.#.###.#.#O###
 * #.....###...###...#OOO#
 * #####################O#
 *
 * This hike contains 94 steps. (The other possible hikes you could have taken were 90, 86, 82, 82, and 74 steps long.)
 *
 * Find the longest hike you can take through the hiking trails listed on your map. How many steps long is the longest hike?
 */
public class longwalk {

    public static void main(String... args) throws IOException {
        var monitor = Executors.newSingleThreadScheduledExecutor();
        try {
            var maze = Maze.load("input2.txt", false);
            var walk = new Walk(maze);
            var future = monitor.scheduleAtFixedRate(walk::monitor, 5, 5, java.util.concurrent.TimeUnit.SECONDS);
            out.println(walk.walk());
            walk.monitor();
            future.cancel(true);
        } finally {
            monitor.shutdownNow();
        }
    }
}

record CachedWalk(Position start, WalkResult result) {}
record WalkResult(int length, Bits visited, Bits activeConstraints) {}
class Bits {
    private final int width;
    private final int height;
    private final BitSet bitset;

    Bits(Maze m) {
        this(m.rows()[0].row().length(), m.rows().length);
    }

    Bits(int width, int height) {
        this.width = width;
        this.height = height;
        this.bitset = new BitSet(width * height);
    }

    public void add(Position p) {
        bitset.set(p.y() * width + p.x());
    }

    public boolean contains(Position p) {
        return bitset.get(p.y() * width + p.x());
    }

    public Bits copy() {
        var copy = new Bits(width, height);
        copy.bitset.or(bitset);
        return copy;
    }

    public boolean noneOf(Bits visited) {
        var x = (BitSet)bitset.clone();
        x.and(visited.bitset);
        return x.isEmpty();
    }

    public Bits without(Bits original) {
        var x = (BitSet)bitset.clone();
        x.andNot(original.bitset);
        var copy = new Bits(width, height);
        copy.bitset.or(x);
        return copy;
    }

    public void addAll(Bits visited) {
        bitset.or(visited.bitset);
    }

    public Bits with(Bits visited) {
        var x = (BitSet)bitset.clone();
        x.or(visited.bitset);
        var copy = new Bits(width, height);
        copy.bitset.or(x);
        return copy;
    }

    public boolean allOf(Bits constraints) {
        var x = (BitSet)bitset.clone();
        x.and(constraints.bitset);
        return x.equals(constraints.bitset);
    }
}

class Walk {
    Maze maze;
    Position start;
    Position end;
    Map<Position, List<CachedWalk>> memoizedPath = new HashMap<>();

    AtomicInteger branches = new AtomicInteger();
    AtomicInteger deadEnds = new AtomicInteger();
    AtomicInteger memoHits = new AtomicInteger();
    AtomicInteger memoMisses = new AtomicInteger();
    AtomicInteger endings = new AtomicInteger();

    public Walk(Maze maze) {
        this.maze = maze;
        start = maze.start();
        end = maze.end();
    }

    void monitor() {
        System.out.println("Branches: " + branches.get() + ", Dead ends: " + deadEnds.get() + ", Memo hits: " + memoHits.get()
                + ", Memo misses: " + memoMisses.get() + ", Endings: " + endings.get());
    }

    public int walk() {
        return walk(start, new Bits(maze)).length()-1;
    }

    private WalkResult memo(Position branchPoint, int length, Bits walked, Bits constraints) {
        var memoList = memoizedPath.computeIfAbsent(branchPoint, (k) -> new ArrayList<>());
        var result = new WalkResult(length, walked, constraints);
        memoList.add(new CachedWalk(branchPoint, result));
        return result;
    }

    private WalkResult walk(Position start, Bits visited) {
        var p = start;
        var walked = visited.copy();
        var activeConstraints = new Bits(maze);
        int steps = 0;

        if (memoizedPath.containsKey(p)) {
            var memoized = memoizedPath.get(p).stream()
                    .filter(walk -> walked.allOf(walk.result().activeConstraints()))
                    .filter(walk -> walked.noneOf(walk.result().visited()))
                    .max(Comparator.comparingInt(walk -> walk.result().length()));

            if (memoized.isPresent()) {
                memoHits.incrementAndGet();
                return memoized.map(m -> m.result()).get();
            } else {
                memoMisses.incrementAndGet();
            }
        }
        while (true) {
            walked.add(p);
            steps++;
            if (p.equals(end)) {
                endings.incrementAndGet();
                return memo(start, steps, walked.without(visited), activeConstraints);
            }

            var validDirections = new ArrayList<Step>(3);
            for (Step step : maze.validDirections(p)) {
                if (visited.contains(step.position())) {
                    activeConstraints.add(step.position());
                }
                if (walked.contains(step.position())) {
                    continue;
                }
                validDirections.add(step);
            }
            if (validDirections.isEmpty()) {
                deadEnds.incrementAndGet();
                return memo(start, 0, walked.without(visited), activeConstraints);
            } else if (validDirections.size() == 1) {
                p = validDirections.get(0).position();
                // loop will take us further.
            } else {
                WalkResult longest = null;
                var allActiveContraints = activeConstraints.copy();
                var allVisited = walked.copy();
                // we need to recurse and take the longest path
                for (Step step : validDirections) {
                    branches.incrementAndGet();
                    var walk = walk(step.position(), walked);

                    if (walk.length() == 0) {
                        allActiveContraints.addAll(walk.activeConstraints());
                        allVisited.addAll(walk.visited());
                        continue; // dead end
                    }
                    if (longest == null || walk.length() > longest.length()) {
                        longest = walk;
                    }
                }
                if (longest == null) {
                    deadEnds.incrementAndGet();
                    return memo(start,0, allVisited.without(visited), activeConstraints);
                }
                return memo(start, steps + longest.length(),
                        walked.with(longest.visited()).without(visited),
                        activeConstraints.with(longest.activeConstraints()));
            }
        }
    }
}

enum Direction {
    UP, DOWN, LEFT, RIGHT;
}

record Position(int x, int y) {
    public Position move(Direction direction) {
        return switch (direction) {
            case UP -> new Position(x, y - 1);
            case DOWN -> new Position(x, y + 1);
            case LEFT -> new Position(x - 1, y);
            case RIGHT -> new Position(x + 1, y);
        };
    }
}

record MazeRow(String row) {
    public static MazeRow fromString(String row) {
        return new MazeRow(row);
    }

    int find(char c) {
        return row.indexOf(c);
    }
}

record Step(Position position, Direction direction) {
}

record Maze(MazeRow[] rows, boolean slippery) {
    static final char PATH = '.';
    static final char FOREST = '#';
    static final char SLOPE_UP = '^';
    static final char SLOPE_DOWN = 'v';
    static final char SLOPE_LEFT = '<';
    static final char SLOPE_RIGHT = '>';

    Position start() {
        return new Position(rows[0].find(PATH), 0);
    }

    Position end() {
        return new Position(rows[rows.length - 1].find(PATH), rows.length - 1);
    }

    char charAt(Position p) {
        return rows[p.y()].row().charAt(p.x());
    }

    List<Step> validDirections(Position p) {
        if (slippery) {
            char standingAt = charAt(p);
            switch (standingAt) {
                case SLOPE_DOWN:
                    return List.of(new Step(p.move(Direction.DOWN), Direction.DOWN));
                case SLOPE_UP:
                    return List.of(new Step(p.move(Direction.UP), Direction.UP));
                case SLOPE_LEFT:
                    return List.of(new Step(p.move(Direction.LEFT), Direction.LEFT));
                case SLOPE_RIGHT:
                    return List.of(new Step(p.move(Direction.RIGHT), Direction.RIGHT));
                case FOREST:
                    throw new IllegalArgumentException("Can't walk on forest at " + p);
            }
        }
        var directions = new ArrayList<Step>(3);
        for (Direction d : Direction.values()) {
            Position next = p.move(d);
            if (isValid(next)) {
                char c = charAt(next);
                if (c != FOREST) {
                    directions.add(new Step(next, d));
                }
            }
        }
        return directions;
    }

    boolean isValid(Position p) {
        return p.y() >= 0 && p.y() < rows.length && p.x() >= 0
                && p.x() < rows[p.y()].row().length();
    }

    public static Maze fromString(String[] rows, boolean slippery) {
        return new Maze(Arrays.stream(rows).map(MazeRow::fromString).toArray(MazeRow[]::new), slippery);
    }

    public static Maze load(String file, boolean slippery) throws IOException {
        return fromString(Files.readAllLines(Path.of(file)).toArray(String[]::new), slippery);
    }
}