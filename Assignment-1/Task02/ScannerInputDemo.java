/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 2  -  reading input with Scanner (interactive gate console)
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Run interactively :  java ScannerInputDemo
 * Run reproducibly  :  java ScannerInputDemo < input.txt
 *
 * Shows Scanner used with hasNextInt() validation so that a non-numeric or
 * out-of-range duration is rejected instead of throwing InputMismatchException.
 */
import java.util.Scanner;

public class ScannerInputDemo {

    static long feeCorrect(int minutes, int ratePerHour) {
        return (long) ((minutes + 59) / 60) * ratePerHour;
    }

    static int rateFor(String type) {
        switch (type.toUpperCase()) {
            case "TWO_WHEELER": return 10;
            case "CAR":         return 30;
            case "EV":          return 30;
            default:            return -1;          // unknown type
        }
    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println("TASK 2  -  SCANNER INPUT WITH VALIDATION");
        System.out.println("=".repeat(70));
        System.out.println("Reading entries until the vehicle number 'END' is given.");
        System.out.println();
        System.out.printf("%-14s | %-12s | %-9s | %s%n",
                          "VEHICLE NO", "TYPE", "MINUTES", "FEE / ERROR");
        System.out.println("-".repeat(70));

        int accepted = 0, rejected = 0;
        long collected = 0;

        while (in.hasNext()) {
            String regNo = in.next();
            if ("END".equalsIgnoreCase(regNo)) break;

            if (!in.hasNext()) {
                System.out.printf("%-14s | %-12s | %-9s | %s%n", regNo, "?", "?",
                                  "REJECTED: type is missing");
                rejected++;
                break;
            }
            String type = in.next();

            // hasNextInt() is checked BEFORE nextInt(), so bad text cannot crash the run
            if (!in.hasNextInt()) {
                String bad = in.hasNext() ? in.next() : "<end of input>";
                System.out.printf("%-14s | %-12s | %-9s | %s%n", regNo, type, bad,
                                  "REJECTED: duration is not a whole number");
                rejected++;
                continue;
            }
            int minutes = in.nextInt();

            int rate = rateFor(type);
            if (rate < 0) {
                System.out.printf("%-14s | %-12s | %-9d | %s%n", regNo, type, minutes,
                                  "REJECTED: unknown vehicle type");
                rejected++;
            } else if (minutes < 0) {
                System.out.printf("%-14s | %-12s | %-9d | %s%n", regNo, type, minutes,
                                  "REJECTED: negative duration");
                rejected++;
            } else {
                long fee = feeCorrect(minutes, rate);
                collected += fee;
                accepted++;
                System.out.printf("%-14s | %-12s | %-9d | Rs %d%n", regNo, type, minutes, fee);
            }
        }

        System.out.println("-".repeat(70));
        System.out.println("accepted = " + accepted + ", rejected = " + rejected
                           + ", total collected = Rs " + collected);
        in.close();
    }
}
