///usr/bin/env jbang "$0" "$@" ; exit $?


import static java.lang.System.*;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.io.*;

public class calibration {

    public static void main(String... args) throws Exception {
        // read every line of input txt, combine first and last number into one string,
        // sum all resulting numbers.
        var input = new Scanner(new File("input.txt"));
        var sum = 0;
        while (input.hasNextLine()) {
            var line = input.nextLine();
            try {
                var first = firstNumber(line);
                var last = lastNumber(line);
                var sumOfNumbers = Integer.parseInt(first + last);
                sum += sumOfNumbers;
            } catch (Exception e) {
                throw new IllegalArgumentException("For input"+line, e);
            }
        }
        out.println(sum);
    }

    private static String firstNumber(String input) {  
        var pattern = "(\\d)";
        var matcher = Pattern.compile(pattern).matcher(input);
        var result = matcher.find();
        return matcher.group(1);
    }

    private static String lastNumber(String input) {
        var pattern = ".*(\\d)";
        var matcher = Pattern.compile(pattern).matcher(input);
        var result = matcher.find();
        return matcher.group(1);
    }
}
