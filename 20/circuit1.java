///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.lang.System.*;

/**
 * Modules communicate using pulses. Each pulse is either a high pulse or a low pulse. When a module sends a pulse, it sends that type of pulse to each module in its list of destination modules.
 *
 * There are several different types of modules:
 *
 * Flip-flop modules (prefix %) are either on or off; they are initially off. If a flip-flop module receives a high pulse, it is ignored and nothing happens. However, if a flip-flop module receives a low pulse, it flips between on and off. If it was off, it turns on and sends a high pulse. If it was on, it turns off and sends a low pulse.
 *
 * Conjunction modules (prefix &) remember the type of the most recent pulse received from each of their connected input modules; they initially default to remembering a low pulse for each input. When a pulse is received, the conjunction module first updates its memory for that input. Then, if it remembers high pulses for all inputs, it sends a low pulse; otherwise, it sends a high pulse.
 *
 * There is a single broadcast module (named broadcaster). When it receives a pulse, it sends the same pulse to all of its destination modules.
 *
 * Here at Desert Machine Headquarters, there is a module with a single button on it called, aptly, the button module. When you push the button, a single low pulse is sent directly to the broadcaster module.
 *
 * After pushing the button, you must wait until all pulses have been delivered and fully handled before pushing it again. Never push the button if modules are still processing pulses.
 *
 * Pulses are always processed in the order they are sent. So, if a pulse is sent to modules a, b, and c, and then module a processes its pulse and sends more pulses, the pulses sent to modules b and c would have to be handled first.
 *
 * The module configuration (your puzzle input) lists each module. The name of the module is preceded by a symbol identifying its type, if any. The name is then followed by an arrow and a list of its destination modules. For example:
 *
 * broadcaster -> a, b, c
 * %a -> b
 * %b -> c
 * %c -> inv
 * &inv -> a
 *
 * In this module configuration, the broadcaster has three destination modules named a, b, and c. Each of these modules is a flip-flop module (as indicated by the % prefix). a outputs to b which outputs to c which outputs to another module named inv. inv is a conjunction module (as indicated by the & prefix) which, because it has only one input, acts like an inverter (it sends the opposite of the pulse type it receives); it outputs to a.
 *
 * By pushing the button once, the following pulses are sent:
 *
 * button -low-> broadcaster
 * broadcaster -low-> a
 * broadcaster -low-> b
 * broadcaster -low-> c
 * a -high-> b
 * b -high-> c
 * c -high-> inv
 * inv -low-> a
 * a -low-> b
 * b -low-> c
 * c -low-> inv
 * inv -high-> a
 *
 * After this sequence, the flip-flop modules all end up off, so pushing the button again repeats the same sequence.
 *
 * Here's a more interesting example:
 *
 * broadcaster -> a
 * %a -> inv, con
 * &inv -> b
 * %b -> con
 * &con -> output
 *
 * This module configuration includes the broadcaster, two flip-flops (named a and b), a single-input conjunction module (inv), a multi-input conjunction module (con), and an untyped module named output (for testing purposes). The multi-input conjunction module con watches the two flip-flop modules and, if they're both on, sends a low pulse to the output module.
 *
 * Here's what happens if you push the button once:
 *
 * button -low-> broadcaster
 * broadcaster -low-> a
 * a -high-> inv
 * a -high-> con
 * inv -low-> b
 * con -high-> output
 * b -high-> con
 * con -low-> output
 *
 * Both flip-flops turn on and a low pulse is sent to output! However, now that both flip-flops are on and con remembers a high pulse from each of its two inputs, pushing the button a second time does something different:
 *
 * button -low-> broadcaster
 * broadcaster -low-> a
 * a -low-> inv
 * a -low-> con
 * inv -high-> b
 * con -high-> output
 *
 * Flip-flop a turns off! Now, con remembers a low pulse from module a, and so it sends only a high pulse to output.
 *
 * Push the button a third time:
 *
 * button -low-> broadcaster
 * broadcaster -low-> a
 * a -high-> inv
 * a -high-> con
 * inv -low-> b
 * con -low-> output
 * b -low-> con
 * con -high-> output
 *
 * This time, flip-flop a turns on, then flip-flop b turns off. However, before b can turn off, the pulse sent to con is handled first, so it briefly remembers all high pulses for its inputs and sends a low pulse to output. After that, flip-flop b turns off, which causes con to update its state and send a high pulse to output.
 *
 * Finally, with a on and b off, push the button a fourth time:
 *
 * button -low-> broadcaster
 * broadcaster -low-> a
 * a -low-> inv
 * a -low-> con
 * inv -high-> b
 * con -high-> output
 *
 * This completes the cycle: a turns off, causing con to remember only low pulses and restoring all modules to their original states.
 *
 * To get the cables warmed up, the Elves have pushed the button 1000 times. How many pulses got sent as a result (including the pulses sent by the button itself)?
 */
public class circuit1 {

    public static void main(String... args) throws IOException {
        var circuit = new Circuit(Files.lines(Path.of("input3.txt")).map(Segment::parse).toList());
        //circuit.toMermaid(out);
        out.println(circuit.pushButton(4100));
    }

    record Signal(String source, String destination, Pulse pulse) {}
    record Segment(String name, Module module, List<String> destinations) {
        static Segment parse(String line) {
            String[] parts = line.split("\\s*->\\s*");
            String[] dests = parts[1].split("\\s*,\\s*");
            var module = parseModule(parts[0]);
            var name = parts[0].equals("broadcaster") ? "broadcaster" : parts[0].substring(1);
            return new Segment(name, module, List.of(dests));
        }

        static Module parseModule(String firstPart) {
            if ("broadcaster".equals(firstPart)) {
                return new Module.Broadcaster();
            } else if (firstPart.startsWith("%")) {
                return new Module.FlipFlop();
            } else if (firstPart.startsWith("&")) {
                return new Module.Conjuction();
            } else {
                throw new IllegalArgumentException("Unknown module type: " + firstPart.charAt(0));
            }
        }
    }
    static class Circuit {
        private Deque<Signal> inTransit = new LinkedList<>();

        private Map<String,Segment> modules = new HashMap<>();

        int lows;
        int highs;

        int counter;

        Circuit(List<Segment> segments) {
            for (Segment segment : segments) {
                modules.put(segment.name, segment);
            }
            // connect up all conjuction inputs
            for (Segment segment : segments) {
                for (var dest : segment.destinations) {
                    var other = modules.get(dest);
                    if (other == null) {
                        continue;
                    }
                    if (other.module instanceof Module.Conjuction c) {
                        c.add(segment.name);
                    }
                }
            }
        }

        public long pushButton(int times) {
            lows = 0;
            highs = 0;
            for(int i = 0; i < times; i++) {
                counter = i+1;
                send("broadcaster", Pulse.LOW);
            }
            return score();
        }

        private void send(String dest, Pulse pulse) {
            inTransit.add(new Signal("button", dest, pulse));
            while(!inTransit.isEmpty()) {
                Signal signal = inTransit.removeFirst();
                switch(signal.pulse) {
                    case HIGH -> highs++;
                    case LOW -> lows++;
                }
                Segment segment = modules.get(signal.destination);
                debug(segment, signal);
                if (segment != null) {
                    segment.module.receive(signal.source, signal.pulse,
                            p -> segment.destinations.stream()
                                    .map(d -> new Signal(signal.destination, d, p))
                                    .forEach(inTransit::add));
                }
            }
        }

        private void debug(Segment segment, Signal signal) {
            if (segment == null || segment.module == null) {
                return;
            }
            if ("df".equals(signal.destination) && signal.pulse == Pulse.HIGH) {
                out.printf("%d: %s %s -> %s%n", counter, signal.pulse, signal.source, segment.name);
            }
        }

        long score() {
            return lows*highs;
        }

        public void toMermaid(PrintStream out) {
            out.println("graph LR");
            var introduced = new HashSet<String>();
            for (Segment segment : modules.values()) {
                out.print("    ");
                if (introduced.add(segment.name)) {
                    introduceModule(segment.name, segment.module, out);
                } else {
                    out.print(segment.name);
                }
                out.print(" --> ");
                boolean first = true;
                for(var dest : segment.destinations) {
                    if (first) {
                        first = false;
                    } else {
                        out.printf("    %s --> ", segment.name);
                    }
                    if (introduced.add(dest)) {
                        var mod = modules.get(dest);
                        introduceModule(dest, mod == null ? null : mod.module, out);
                    } else {
                        out.print(dest);
                    }
                    out.println();
                }
            }
        }

        private void introduceModule(String name, Module module, PrintStream out) {

            if (module instanceof Module.Conjuction) {
                out.printf("%s[%s%s]", name, name, " &");
            } else if (module instanceof Module.FlipFlop) {
                out.printf("%s[%s%s]", name, name, " %");
            } else {
                out.printf("%s[%s]", name, name);
            }
        }
    }

    enum Pulse {
        HIGH,
        LOW;

        static Pulse of(boolean state) {
            return state ? HIGH : LOW;
        }

        public boolean booleanValue() {
            return this == HIGH;
        }
    }

    interface Wiring {
        void send(Pulse pulse);
    }



    sealed interface Module {
        void receive(String source, Pulse pulse, Wiring wiring);

        final class FlipFlop implements Module {
            private boolean on = false;
            public void receive(String source, Pulse pulse, Wiring wiring) {
                if (pulse == Pulse.LOW) {
                    on = !on;
                    wiring.send(Pulse.of(on));
                }
            }
        }

        final class Conjuction implements Module {
            private final Map<String,Pulse> states = new HashMap<>();
            public void add(String receiver) {
                states.put(receiver, Pulse.LOW);
            }

            public void receive(String source, Pulse pulse, Wiring wiring) {
                states.put(source, pulse);
                if (states.values().stream().allMatch(Pulse.HIGH::equals)) {
                    wiring.send(Pulse.LOW);
                } else {
                    wiring.send(Pulse.HIGH);
                }
            }
        }

        final class Broadcaster implements Module {
            public Broadcaster() {
            }
            public void receive(String source, Pulse pulse, Wiring wiring) {
                wiring.send(pulse);
            }
        }
    }
}
