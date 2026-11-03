/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 2  -  Choose the Data Representation
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Shows the chosen Java data type for every field, the jagged array used for the
 * floors, and a worked example where integer division and a careless cast each
 * produce a WRONG fee - together with the arithmetic that avoids both.
 */
public class DataRepresentationDemo {

    /* ---- the chosen representation ---------------------------------------- */
    private final String[][] slots;        // jagged: slots[floor][slot], null = free
    private final int[] slotsPerFloor;

    DataRepresentationDemo(int[] slotsPerFloor) {
        this.slotsPerFloor = slotsPerFloor;
        this.slots = new String[slotsPerFloor.length][];
        for (int f = 0; f < slotsPerFloor.length; f++)
            slots[f] = new String[slotsPerFloor[f]];
    }

    /* ---- the three ways of turning minutes into billable hours ------------ */

    /** WRONG: integer division truncates, so a part-hour is given away free. */
    static long feeByIntegerDivision(int minutes, int ratePerHour) {
        int hours = minutes / 60;                       // 130 / 60 == 2, the 10 min vanish
        return (long) hours * ratePerHour;
    }

    /** WRONG: double division then a cast to int truncates in exactly the same way. */
    static long feeByCarelessCast(int minutes, int ratePerHour) {
        double hours = minutes / 60.0;                  // 2.1666...
        return (long) ((int) hours * ratePerHour);      // (int) 2.1666 == 2
    }

    /** CORRECT: integer ceiling, no floating point anywhere in the money path. */
    static long feeCorrect(int minutes, int ratePerHour) {
        if (minutes < 0) throw new IllegalArgumentException("duration cannot be negative");
        int billableHours = (minutes + 59) / 60;        // ceiling without a cast
        return (long) billableHours * ratePerHour;
    }

    public static void main(String[] args) {
        System.out.println("TASK 2  -  DATA REPRESENTATION");
        System.out.println("=".repeat(80));
        System.out.printf("%-14s | %-18s | %s%n", "FIELD", "JAVA TYPE", "WHY THIS TYPE");
        System.out.println("-".repeat(80));
        String[][] fields = {
            {"slot number",  "int",      "small whole count, 0..S-1; no fractions exist"},
            {"floor number", "int",      "small whole count, 0..F-1; used as an array index"},
            {"vehicle number","String",  "letters and digits, compared with equals()"},
            {"entry time",   "LocalTime","calendar-correct; minutes-from-midnight as int"},
            {"duration",     "int",      "whole minutes; cannot overflow for a day's stay"},
            {"fee",          "long",     "whole rupees; money is never held in double"},
            {"occupancy",    "boolean",  "a slot is free or not; null String also encodes it"},
        };
        for (String[] r : fields) System.out.printf("%-14s | %-18s | %s%n", r[0], r[1], r[2]);

        System.out.println();
        System.out.println("WHY A JAGGED ARRAY FOR THE FLOORS");
        System.out.println("-".repeat(80));
        DataRepresentationDemo f = new DataRepresentationDemo(new int[]{20, 50, 50, 100, 100});
        System.out.println("  declared as  String[][] slots = new String[floors][];");
        System.out.println("  each row is then sized on its own, so floors may differ in size:");
        for (int i = 0; i < f.slots.length; i++)
            System.out.println("    floor " + i + "  slots[" + i + "].length = " + f.slots[i].length);
        int total = 0;
        for (int n : f.slotsPerFloor) total += n;
        System.out.println("  total slots = " + total + ", allocated cells = " + total
                           + "  (no waste)");
        System.out.println();
        System.out.println("  A rectangular String[5][100] would allocate 500 cells for the same");
        System.out.println("  facility and waste " + (500 - total) + " of them, and every loop would");
        System.out.println("  then need a separate validSlots[floor] array to know where a row ends -");
        System.out.println("  the length is already carried by slots[floor].length in the jagged form.");

        System.out.println();
        System.out.println("CASTING AND INTEGER-DIVISION RISK IN THE FEE PATH");
        System.out.println("=".repeat(80));
        System.out.println("  Rule: every started hour is charged in full.  Car rate = Rs 30/hour.");
        System.out.println();
        System.out.printf("%-10s | %-14s | %-14s | %-12s | %s%n",
                          "MINUTES", "int division", "careless cast", "CORRECT", "LOSS TO CAMPUS");
        System.out.println("-".repeat(80));
        int rate = 30;
        int[] samples = { 1, 59, 60, 61, 119, 130, 180, 185, 1440 };
        long totalLoss = 0;
        for (int m : samples) {
            long bad1 = feeByIntegerDivision(m, rate);
            long bad2 = feeByCarelessCast(m, rate);
            long good = feeCorrect(m, rate);
            long loss = good - bad1;
            totalLoss += loss;
            System.out.printf("%-10d | %-14s | %-14s | %-12s | %s%n",
                              m, "Rs " + bad1, "Rs " + bad2, "Rs " + good,
                              loss == 0 ? "-" : "Rs " + loss + "  WRONG");
        }
        System.out.println("-".repeat(80));
        System.out.println("  Over just these " + samples.length + " stays the truncating versions"
                           + " under-charge by Rs " + totalLoss + ".");
        System.out.println("  A 1-minute stay is billed Rs 0 instead of Rs 30 - the facility would");
        System.out.println("  earn nothing from any vehicle that stays under an hour.");

        System.out.println();
        System.out.println("HOW THE CORRECT VERSION AVOIDS IT");
        System.out.println("-".repeat(80));
        System.out.println("  billableHours = (minutes + 59) / 60;");
        System.out.println("  The + 59 pushes any non-zero remainder up into the next whole hour,");
        System.out.println("  so the division rounds UP while still being pure integer arithmetic.");
        System.out.println("  No double is created, so no rounding error can enter the money path,");
        System.out.println("  and the result is held in long because int rupees could overflow for a");
        System.out.println("  long-stay or season-ticket total.");
        System.out.println();
        System.out.println("  Verification of the boundary cases:");
        System.out.printf("    %-26s -> %s%n", "0 min  (drive-through)", "Rs " + feeCorrect(0, rate));
        System.out.printf("    %-26s -> %s%n", "1 min  (started hour 1)", "Rs " + feeCorrect(1, rate));
        System.out.printf("    %-26s -> %s%n", "60 min (exactly 1 hour)", "Rs " + feeCorrect(60, rate));
        System.out.printf("    %-26s -> %s%n", "61 min (started hour 2)", "Rs " + feeCorrect(61, rate));
        try { feeCorrect(-5, rate); }
        catch (IllegalArgumentException e) {
            System.out.printf("    %-26s -> REJECTED: %s%n", "-5 min (invalid)", e.getMessage());
        }

        System.out.println();
        System.out.println("HOW INPUT IS READ");
        System.out.println("-".repeat(80));
        System.out.println("  Scanner is used for the interactive gate console, because the operator");
        System.out.println("  types one vehicle at a time and the program must re-prompt on bad input.");
        System.out.println("  Command-line arguments are used for the batch/test runs, because they");
        System.out.println("  are fixed before the program starts and make a run reproducible.");
        System.out.println("  See ScannerInputDemo.java in this folder for the interactive version,");
        System.out.println("  driven from the file input.txt so the run can be repeated exactly.");
    }
}
