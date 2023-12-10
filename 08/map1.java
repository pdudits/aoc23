///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.System.*;

/**
 * At least, you're pretty sure that's what they are; one of the documents contains a list of left/right instructions, and the rest of the documents seem to describe some kind of network of labeled nodes.
 *
 * It seems like you're meant to use the left/right instructions to navigate the network. Perhaps if you have the camel follow the same instructions, you can escape the haunted wasteland!
 *
 * After examining the maps for a bit, two nodes stick out: AAA and ZZZ. You feel like AAA is where you are now, and you have to follow the left/right instructions until you reach ZZZ.
 *
 * This format defines each node of the network individually. For example:
 *
 * RL
 *
 * AAA = (BBB, CCC)
 * BBB = (DDD, EEE)
 * CCC = (ZZZ, GGG)
 * DDD = (DDD, DDD)
 * EEE = (EEE, EEE)
 * GGG = (GGG, GGG)
 * ZZZ = (ZZZ, ZZZ)
 *
 * Starting with AAA, you need to look up the next element based on the next left/right instruction in your input. In this example, start with AAA and go right (R) by choosing the right element of AAA, CCC. Then, L means to choose the left element of CCC, ZZZ. By following the left/right instructions, you reach ZZZ in 2 steps.
 */
public class map1 {

    public static void main(String... args) throws IOException {
        var parser = new Parser();
        Files.lines(Path.of("input2.txt")).forEach(parser::parse);
        var cycles = parser.routeToAllZ();
        // offsets happen to be 0, which is good.
        var steps = lcm(Arrays.stream(cycles).mapToInt(Parser.WalkPath.Cycle::period).toArray());
        out.println(steps);
    }

    // find least common multiplier of the parameter
    private static BigInteger lcm(int[] nums) {
        BigInteger lcm = BigInteger.ONE;
        for (int i = 0; i < nums.length; i++) {
            BigInteger num = BigInteger.valueOf(nums[i]);
            BigInteger gcd = lcm.gcd(num);
            BigInteger div = num.divide(gcd);
            lcm = lcm.multiply(div);
        }
        return lcm;
    }


    static class Parser {
        String instructions;
        Map<String,Node> nodes = new HashMap<>();
        void parse(String line) {
            if (line.isBlank() && instructions != null) {
                return;
            }
            if (instructions == null && line.matches("[LR]+")) {
                instructions = line;
                return;
            }
            Node node = Node.parse(line);
            nodes.put(node.self, node);
        }

        int routeToZzz() {
            var node = nodes.get("AAA");
            for(int i=0;;i++) {
                String instruction = String.valueOf(instructions.charAt(i%instructions.length()));
                if (instruction.equals("L")) {
                    node = nodes.get(node.left);
                } else if (instruction.equals("R")) {
                    node = nodes.get(node.right);
                }
                if (node.self.equals("ZZZ")) {
                    return i+1;
                }
            }
        }

        WalkPath.Cycle[] routeToAllZ() {
            var nodes = this.nodes.values().stream().filter(Node::isStart).map(this::connect).toArray(ConnectedNode[]::new);
            WalkPath[] paths = Stream.of(nodes).map(n -> new WalkPath()).toArray(WalkPath[]::new);


            for (int i=0;i>=0 && i < Integer.MAX_VALUE;i++) {
                var instruction = instructions.charAt(i%instructions.length());
                Function<ConnectedNode,ConnectedNode> turn =  instruction == 'L' ? ConnectedNode::left : ConnectedNode::right;
                boolean finished = true;
                for (int j=0; j<nodes.length; j++) {
                    paths[j] = paths[j].visit(j, i, i%instructions.length(), nodes[j]);
                    if (instruction == 'L') {
                        nodes[j] = nodes[j].left();
                    } else {
                        nodes[j] = nodes[j].right();
                    }
                    finished &=  paths[j].cycle() != null;
                }
                if (finished) {
                    return Arrays.stream(paths).map(WalkPath::cycle).toArray(WalkPath.Cycle[]::new);
                }
                if (i % 1_000_000 == 0) {
                    out.println("Still at it %d".formatted(i));
                }
            }
            throw new AssertionError("Out of int range");
        }

        record WalkPath(SortedSet<Integer> exits, Set<Visit> visits, Cycle cycle) {
            WalkPath() {
                this(new TreeSet<>(), new HashSet<>(), null);
            }

            WalkPath visit(int path, int routeStep, int instructionStep, ConnectedNode node) {
                if (cycle != null) {
                    return this;
                }
                if (node.isEnd()) {
                    exits.add(routeStep);
                }
                var visit = new Visit(routeStep, node, instructionStep);
                if (!visits.add(visit)) {
                    var previous = visits.stream().filter(visit::equals).findAny().get();
                    var exit = exits.first();
                    var cycleHead = previous.routeStep;
                    var period = routeStep - previous.routeStep;
                    var exitDistance = routeStep - exit;
                    var offset = cycleHead - exitDistance;
                    out.println("%d: Cycle at %d+n*%d".formatted(path, cycleHead-exitDistance,period));
                    return new WalkPath(exits, visits, new Cycle(offset, period));
                }
                return this;
            }

            record Cycle(int offset, int period) {}
        }
        record VisitKey(ConnectedNode n, int instruction) {}

        static class Visit {
            final VisitKey key;
            final int routeStep;

            Visit(int step, ConnectedNode n, int instruction) {
                this.key = new VisitKey(n, instruction);
                this.routeStep = step;
            }

            void printCycle(int path, int currentStep) {
                out.println("%d: Cycle at %d+n*%d".formatted(path, routeStep,currentStep-routeStep));
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Visit visit = (Visit) o;
                return Objects.equals(key, visit.key);
            }

            @Override
            public int hashCode() {
                return Objects.hash(key);
            }
        }

        Map<String, ConnectedNode> connectedNodes = new HashMap<>();
        class ConnectedNode {
            Node self;
            ConnectedNode left;
            ConnectedNode right;

            ConnectedNode(Node self) {
                this.self = self;
            }

            ConnectedNode left() {
                if (left == null) {
                    left = connect(self.left);
                }
                return left;
            }

            ConnectedNode right() {
                if (right == null) {
                    right = connect(self.right);
                }
                return right;
            }

            boolean isEnd() {
                return self.isEnd();
            }

        }

        ConnectedNode connect(Node n) {
            return connect(n.self);
        }
        ConnectedNode connect(String node) {
            return connectedNodes.computeIfAbsent(node, n -> new ConnectedNode(nodes.get(n)));
        }
    }

    record Node(String self, String left, String right) {
        static Node parse(String line) {
            var matcher = Pattern.compile("(\\w+)\\s*=\\s*\\((\\w+),\\s*(\\w+)\\s*\\)").matcher(line);
            if (!matcher.find()) {
                throw new IllegalArgumentException("Input doesn't match: %s".formatted(line));
            }
            return new Node(matcher.group(1), matcher.group(2), matcher.group(3));
        }

        boolean isStart() {
            return self.endsWith("A");
        }

        boolean isEnd() {
            return self.endsWith("Z");
        }
    }


}
