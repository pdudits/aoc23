///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.System.*;

/**
 * Inside each box, there are several lens slots that will keep a lens correctly positioned to focus light passing through the box. The side of each box has a panel that opens to allow you to insert or remove lenses as necessary.
 *
 * Along the wall running parallel to the boxes is a large library containing lenses organized by focal length ranging from 1 through 9. The reindeer also brings you a small handheld label printer.
 *
 * The book goes on to explain how to perform each step in the initialization sequence, a process it calls the Holiday ASCII String Helper Manual Arrangement Procedure, or HASHMAP for short.
 *
 * Each step begins with a sequence of letters that indicate the label of the lens on which the step operates. The result of running the HASH algorithm on the label indicates the correct box for that step.
 *
 * The label will be immediately followed by a character that indicates the operation to perform: either an equals sign (=) or a dash (-).
 *
 * If the operation character is a dash (-), go to the relevant box and remove the lens with the given label if it is present in the box. Then, move any remaining lenses as far forward in the box as they can go without changing their order, filling any space made by removing the indicated lens. (If no lens in that box has the given label, nothing happens.)
 *
 * If the operation character is an equals sign (=), it will be followed by a number indicating the focal length of the lens that needs to go into the relevant box; be sure to use the label maker to mark the lens with the label given in the beginning of the step so you can find it later. There are two possible situations:
 *
 *     If there is already a lens in the box with the same label, replace the old lens with the new lens: remove the old lens and put the new lens in its place, not moving any other lenses in the box.
 *     If there is not already a lens in the box with the same label, add the lens to the box immediately behind any lenses already in the box. Don't move any of the other lenses when you do this. If there aren't any lenses in the box, the new lens goes all the way to the front of the box.
 *
 * Here is the contents of every box after each step in the example initialization sequence above:
 *
 * All 256 boxes are always present; only the boxes that contain any lenses are shown here. Within each box, lenses are listed from front to back; each lens is shown as its label and focal length in square brackets.
 *
 * To confirm that all of the lenses are installed correctly, add up the focusing power of all of the lenses. The focusing power of a single lens is the result of multiplying together:
 *
 *     One plus the box number of the lens in question.
 *     The slot number of the lens within the box: 1 for the first lens, 2 for the second lens, and so on.
 *     The focal length of the lens.
 *
 * At the end of the above example, the focusing power of each lens is as follows:
 *
 *     rn: 1 (box 0) * 1 (first slot) * 1 (focal length) = 1
 *     cm: 1 (box 0) * 2 (second slot) * 2 (focal length) = 4
 *     ot: 4 (box 3) * 1 (first slot) * 7 (focal length) = 28
 *     ab: 4 (box 3) * 2 (second slot) * 5 (focal length) = 40
 *     pc: 4 (box 3) * 3 (third slot) * 6 (focal length) = 72
 *
 * So, the above example ends up with a total focusing power of 145.
 */
public class hash {


    public static void main(String... args) throws IOException {
        var input = Files.lines(Path.of("input2.txt")).reduce("", String::concat);
        var items = input.split(",");
        var result = focusingPower(items);
        out.println(result);
    }

    static long focusingPower(String[] items) {
        var boxes = IntStream.range(0,256).mapToObj(i -> new Box()).toArray(Box[]::new);
        for (var item : items) {
            if (item.endsWith("-")) {
                var label = item.substring(0, item.length() - 1);
                boxes[hash(label)].remove(label);
            } else {
                var parts = item.split("=");
                var label = parts[0];
                var focalLength = Integer.parseInt(parts[1]);
                var box = boxes[hash(label)];
                box.add(new Lens(label, focalLength));
            }
        }
        var sum = 0L;
        for(var i = 0; i < boxes.length; i++) {
            var box = boxes[i];
            var boxMultiplier = i + 1;
            sum += box.power(boxMultiplier);
        }
        return sum;
    }

    record Lens(String label, int focalLength) {}

    static class Box {
        private List<Lens> lenses = new ArrayList<>();

        void add(Lens lens) {
            for (int i=0; i<lenses.size(); i++) {
                var existingLens = lenses.get(i);
                if (existingLens.label().equals(lens.label())) {
                    lenses.set(i, lens);
                    return;
                }
            }
            lenses.add(lens);
        }

        void remove(String label) {
            for (var it = lenses.iterator(); it.hasNext();) {
                var lens = it.next();
                if (lens.label().equals(label)) {
                    it.remove();
                    return;
                }
            }
        }

        void clear() {
            lenses.clear();
        }

        long power(long boxMultiplier) {
            var sum = 0L;
            for(var i = 0; i < lenses.size(); i++) {
                var lens = lenses.get(i);
                var slotMultiplier = i + 1;
                sum  += boxMultiplier * slotMultiplier * lens.focalLength();
            }
            return sum;
        }
    }

    static int hash(String s) {
        return s.chars().reduce(0, hash::hash);
    }
    static int hash(int prev, int curr) {
        return ((prev + curr)) * 17 % 256;
    }
}
