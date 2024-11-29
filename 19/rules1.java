///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.System.*;
import static java.util.stream.Collectors.toMap;

/**
 * each part is rated in each of four categories:
 *
 *     x: Extremely cool looking
 *     m: Musical (it makes a noise when you hit it)
 *     a: Aerodynamic
 *     s: Shiny
 *
 * Then, each part is sent through a series of workflows that will ultimately accept or reject the part. Each workflow has a name and contains a list of rules; each rule specifies a condition and where to send the part if the condition is true. The first rule that matches the part being considered is applied immediately, and the part moves on to the destination described by the rule. (The last rule in each workflow has no condition and always applies if reached.)
 *
 * Consider the workflow ex{x>10:one,m<20:two,a>30:R,A}. This workflow is named ex and contains four rules. If workflow ex were considering a specific part, it would perform the following steps in order:
 *
 *     Rule "x>10:one": If the part's x is more than 10, send the part to the workflow named one.
 *     Rule "m<20:two": Otherwise, if the part's m is less than 20, send the part to the workflow named two.
 *     Rule "a>30:R": Otherwise, if the part's a is more than 30, the part is immediately rejected (R).
 *     Rule "A": Otherwise, because no other rules matched the part, the part is immediately accepted (A).
 *
 * If a part is sent to another workflow, it immediately switches to the start of that workflow instead and never returns. If a part is accepted (sent to A) or rejected (sent to R), the part immediately stops any further processing.
 *
 * The system works, but it's not keeping up with the torrent of weird metal shapes. The Elves ask if you can help sort a few parts and give you the list of workflows and some part ratings (your puzzle input). For example:
 *
 * px{a<2006:qkq,m>2090:A,rfg}
 * pv{a>1716:R,A}
 * lnx{m>1548:A,A}
 * rfg{s<537:gd,x>2440:R,A}
 * qs{s>3448:A,lnx}
 * qkq{x<1416:A,crn}
 * crn{x>2662:A,R}
 * in{s<1351:px,qqz}
 * qqz{s>2770:qs,m<1801:hdj,R}
 * gd{a>3333:R,R}
 * hdj{m>838:A,pv}
 *
 * {x=787,m=2655,a=1222,s=2876}
 * {x=1679,m=44,a=2067,s=496}
 * {x=2036,m=264,a=79,s=2244}
 * {x=2461,m=1339,a=466,s=291}
 * {x=2127,m=1623,a=2188,s=1013}
 *
 * The workflows are listed first, followed by a blank line, then the ratings of the parts the Elves would like you to sort. All parts begin in the workflow named in.
 */
public class rules1 {

    public static void main(String... args) throws IOException {
        var input = Files.readAllLines(Path.of("input2.txt"));
        var workflows = input.stream().takeWhile(s -> !s.isBlank()).map(Workflow::parse).collect(toMap(w -> w.name, w -> w));
        var parts = input.stream().dropWhile(s -> !s.isBlank()).skip(1).map(Part::parse).toList();
        var result = parts.stream().filter(p -> new rules1().test(workflows, p)).mapToLong(Part::rating).sum();
        out.println(result);
        out.println(combinations(workflows));
    }

    boolean test(Map<String,Workflow> rules, Part part) {
        var workflow = rules.get("in");
        while (true) {
            var outcome = workflow.evaluate(part);
            switch (outcome) {
                case Outcome.Goto g: workflow = rules.get(g.workflow); break;
                case Outcome.Reject r: return false;
                case Outcome.Accept a: return true;
                case Outcome.Next n: break;
            }
        }

    }

    static BigDecimal combinations(Map<String, Workflow> rules) {
        return rules.get("in").combinations(Constraint.FULL, rules::get);
    }

    record Workflow(String name, List<Rule> rules) {
        Outcome evaluate(Part part) {
            for (var rule : rules) {
                var outcome = rule.apply(part);
                if (outcome != Outcome.NEXT) {
                    return outcome;
                }
            }
            throw new IllegalStateException("No matching rule found");
        }

        public static Workflow parse(String line) {
            var i = line.indexOf("{");
            var name = line.substring(0, i);
            var rules = List.of(line.substring(i + 1, line.length() - 1).split(",")).stream().map(Rule::parse).toList();
            return new Workflow(name, rules);
        }

        public BigDecimal combinations(Constraint input, Function<String, Workflow> follow) {
            var result = BigDecimal.ZERO;
            for (var rule : rules) {
                var positiveConstraint = rule.apply(input);
                var negativeConstraint = rule.invert(input);
                switch (rule.outcome) {
                    case Outcome.Reject r:
                        break;
                    case Outcome.Goto g:
                        result = result.add(follow.apply(g.workflow).combinations(positiveConstraint, follow));
                        break;
                    case Outcome.Accept a:
                        result = result.add(positiveConstraint.combinations());
                        break;
                    case Outcome.Next n:
                        break;
                }
                input = negativeConstraint;
            }
            return result;
        }
    }

    sealed interface Outcome {
        record Accept() implements Outcome {}
        record Reject() implements Outcome {}
        record Goto(String workflow) implements Outcome {}
        record Next() implements Outcome {}

        Next NEXT = new Next();
        Accept ACCEPT = new Accept();
        Reject REJECT = new Reject();
    }

    record Rule(Condition condition, Outcome outcome) {
        public Outcome apply(Part part) {
            if (condition == null) {
                return outcome;
            }
            return condition.test(part) ? outcome : Outcome.NEXT;
        }

        public static Rule parse(String s) {
            var i = s.indexOf(":");
            var condition = i == -1 ? null : Condition.parse(s.substring(0, i));
            var outcome = switch (s.substring(i + 1)) {
                case "A" -> Outcome.ACCEPT;
                case "R" -> Outcome.REJECT;
                default -> new Outcome.Goto(s.substring(i + 1));
            };
            return new Rule(condition, outcome);
        }

        Constraint apply(Constraint input) {
            return condition != null ? condition.apply(input) : input;
        }

        Constraint invert(Constraint input) {
            return condition != null ? condition.invert(input) : Constraint.NULL;
        }
    }

    record Condition(String field, String op, int value) {
        public boolean test(Part part) {
            var v = switch (field) {
                case "x" -> part.x();
                case "m" -> part.m();
                case "a" -> part.a();
                case "s" -> part.s();
                default -> throw new IllegalArgumentException("Unknown field: " + field);
            };
            return switch (op) {
                case "<" -> v < value;
                case ">" -> v > value;
                case "=" -> v == value;
                default -> throw new IllegalArgumentException("Unknown op: " + op);
            };
        }

        Constraint apply(Constraint input) {
            var range = input.get(field);
            var newRange = switch (op) {
                case "<" -> Range.of(range.min, Math.min(range.max, value));
                case ">" -> Range.of(Math.max(range.min, value), range.max);
                case "=" -> Range.of(value-1, value + 1);
                default -> throw new IllegalArgumentException("Unknown op: " + op);
            };
            return input.with(field, newRange);
        }

        Constraint invert(Constraint input) {
            var range = input.get(field);
            var newRange = switch (op) {
                case "<" -> Range.of(Math.max(range.min, value-1), range.max);
                case ">" -> Range.of(range.min, Math.min(range.max, value+1));
                case "=" -> Range.of(range.min-1, range.max+1);
                default -> throw new IllegalArgumentException("Unknown op: " + op);
            };
            return input.with(field, newRange);
        }

        public static Condition parse(String s) {
            var field = s.substring(0, 1);
            var op = s.substring(1, 2);
            var value = Integer.parseInt(s.substring(2));
            return new Condition(field, op, value);
        }
    }

    record Part(int x, int m, int a, int s) {
        long rating() {
            return x + m + a + s;
        }
        public static Part parse(String in) {
            var x = in.indexOf("x=");
            var m = in.indexOf("m=");
            var a = in.indexOf("a=");
            var s = in.indexOf("s=");
            return new Part(
                Integer.parseInt(in.substring(x + 2, m - 1)),
                Integer.parseInt(in.substring(m + 2, a - 1)),
                Integer.parseInt(in.substring(a + 2, s - 1)),
                Integer.parseInt(in.substring(s + 2, in.length()-1)));
        }
    }

    /** exclusive range of valid values */
    record Range(int min, int max) {
        static Range full() {
            return new Range(0, 4001);
        }

        static final Range NULL = new Range(0, 1);
        static Range of(int min, int max) {
            if (min + 1 > max) {
                return NULL;
            } else {
                return new Range(min, max);
            }
        }
    }

    /**
     * maybe you can figure out in advance which combinations of ratings will be accepted or rejected.
     *
     * Each of the four ratings (x, m, a, s) can have an integer value ranging from a minimum of 1 to a maximum of 4000. Of all possible distinct combinations of ratings, your job is to figure out which ones will be accepted.
     * @param x
     * @param m
     * @param a
     * @param s
     */
    record Constraint(Range x, Range m, Range a, Range s) {
        public static final Constraint NULL = new Constraint(Range.NULL, Range.NULL, Range.NULL, Range.NULL);

        public static final Constraint FULL = new Constraint(Range.full(), Range.full(), Range.full(), Range.full());

        Range get(String field) {
            return switch (field) {
                case "x" -> x;
                case "m" -> m;
                case "a" -> a;
                case "s" -> s;
                default -> throw new IllegalArgumentException("Unknown field: " + field);
            };
        }

        Constraint with(String field, Range newRange) {
            return switch (field) {
                case "x" -> new Constraint(newRange, m, a, s);
                case "m" -> new Constraint(x, newRange, a, s);
                case "a" -> new Constraint(x, m, newRange, s);
                case "s" -> new Constraint(x, m, a, newRange);
                default -> throw new IllegalArgumentException("Unknown field: " + field);
            };
        }

        BigDecimal combinations() {
            return BigDecimal.valueOf(x.max - x.min - 1)
                    .multiply(BigDecimal.valueOf(m.max - m.min - 1))
                    .multiply(BigDecimal.valueOf(a.max - a.min - 1))
                    .multiply(BigDecimal.valueOf(s.max - s.min - 1));
        }
    }
}
