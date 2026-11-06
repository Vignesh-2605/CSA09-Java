package parking.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Programming in Java - Assignment 1 (CO1), Task 10
 * Name: Vignesh S   Register Number: 192325002
 *
 * WHERE WRAPPER CLASSES AND AUTOBOXING ARE USED IN THIS DESIGN, AND THE RISK
 * THEY INTRODUCE - in particular comparing Integer objects with ==, which is
 * correct for small numbers and WRONG for large ones.
 */
public class AutoboxingRiskDemo {

    public static void main(String[] args) {
        System.out.println("TASK 10  -  WRAPPER CLASSES, AUTOBOXING AND THEIR RISKS");
        System.out.println("=".repeat(80));

        System.out.println();
        System.out.println("1.  WHERE WRAPPERS ARE USED, AND WHY A PRIMITIVE WOULD BE WRONG");
        System.out.println("-".repeat(80));
        System.out.printf("%-30s | %-12s | %s%n", "SITE", "TYPE", "WHY NOT A PRIMITIVE");
        System.out.println("-".repeat(80));
        String[][] uses = {
            {"ParkingTicket.exitMinute", "Integer", "null means 'still inside'; 0 is midnight"},
            {"Map<String,Integer> fees", "Integer", "a generic type cannot hold int"},
            {"List<Integer> durations",  "Integer", "collections store references only"},
            {"Vehicle.regNo",            "String",  "already a reference type"},
            {"slot number, floor",       "int",     "always present; primitive is correct"},
            {"fee",                      "long",    "always present; primitive is correct"},
        };
        for (String[] r : uses) System.out.printf("%-30s | %-12s | %s%n", r[0], r[1], r[2]);
        System.out.println();
        System.out.println("  The rule applied: a wrapper ONLY where absence is meaningful or a");
        System.out.println("  generic type demands it.  Everywhere else the primitive is used, which");
        System.out.println("  avoids both the boxing cost and every hazard below.");

        System.out.println();
        System.out.println("2.  THE == TRAP: THE SAME COMPARISON, TWO DIFFERENT ANSWERS");
        System.out.println("-".repeat(80));
        Integer smallA = 90,  smallB = 90;          // autoboxed via Integer.valueOf
        Integer bigA   = 900, bigB   = 900;

        System.out.printf("%-42s | %-8s | %s%n", "COMPARISON", "RESULT", "WHY");
        System.out.println("-".repeat(80));
        System.out.printf("%-42s | %-8s | %s%n", "Integer 90  == Integer 90", smallA == smallB,
                          "both from the -128..127 cache");
        System.out.printf("%-42s | %-8s | %s%n", "Integer 900 == Integer 900", bigA == bigB,
                          "two distinct objects on the heap");
        System.out.printf("%-42s | %-8s | %s%n", "Integer 900 .equals(Integer 900)", bigA.equals(bigB),
                          "compares the VALUE - always right");
        System.out.printf("%-42s | %-8s | %s%n", "900 == 900  (both int)", 900 == 900,
                          "primitives compare by value");
        System.out.printf("%-42s | %-8s | %s%n", "Integer 900 == 900  (mixed)", bigA == 900,
                          "the Integer is UNBOXED first");
        System.out.println();
        System.out.println("  This is the worst possible failure mode: == on Integer is correct for");
        System.out.println("  every value from -128 to 127 and wrong above it.  A fee comparison");
        System.out.println("  tested with Rs 90 would pass every test and fail in production the");
        System.out.println("  first time a vehicle was billed Rs 200.");

        System.out.println();
        System.out.println("3.  THE SAME BUG IN A PARKING CONTEXT");
        System.out.println("-".repeat(80));
        Map<String, Integer> feeCharged = new HashMap<>();
        feeCharged.put("TN01AB1111", 90);           // small: inside the cache
        feeCharged.put("TN02CD2222", 900);          // large: outside the cache
        Integer expectedSmall = 90, expectedBig = 900;

        System.out.printf("%-20s | %-8s | %-18s | %-18s | %s%n",
                          "VEHICLE", "FEE", "== COMPARISON", ".equals()", "AGREE?");
        System.out.println("-".repeat(80));
        for (Map.Entry<String, Integer> e : feeCharged.entrySet()) {
            Integer expected = e.getValue() == 90 ? expectedSmall : expectedBig;
            boolean byRef = e.getValue() == expected;
            boolean byVal = e.getValue().equals(expected);
            System.out.printf("%-20s | %-8d | %-18s | %-18s | %s%n",
                              e.getKey(), e.getValue(), byRef, byVal,
                              byRef == byVal ? "yes" : "NO - the bug is here");
        }
        System.out.println();
        System.out.println("  The fix used in this design: fees are held as primitive long, so the");
        System.out.println("  comparison is on values and the question never arises.  Where a");
        System.out.println("  wrapper is unavoidable (a Map value), .equals() is used, never ==.");

        System.out.println();
        System.out.println("4.  THE OTHER TWO AUTOBOXING HAZARDS IN THIS DESIGN");
        System.out.println("-".repeat(80));
        System.out.println("  a) NullPointerException on unboxing");
        Integer exitMinute = null;                  // a vehicle that has not left
        System.out.println("     Integer exitMinute = null;   // vehicle still inside");
        try {
            int duration = exitMinute - 540;        // silent unboxing of null
            System.out.println("     duration = " + duration);
        } catch (NullPointerException e) {
            System.out.println("     int duration = exitMinute - 540;  -> NullPointerException");
        }
        System.out.println("     This is why ParkingTicket.durationMinutes() tests");
        System.out.println("     'if (exitMinute == null) return -1;' BEFORE any arithmetic: the");
        System.out.println("     subtraction looks harmless but hides an unboxing call.");
        System.out.println();
        System.out.println("  b) remove(int) versus remove(Object)");
        List<Integer> ticketNos = new ArrayList<>();
        ticketNos.add(1001); ticketNos.add(1002); ticketNos.add(1003);
        List<Integer> copy = new ArrayList<>(ticketNos);
        System.out.println("     list = " + ticketNos);
        ticketNos.remove(1);                        // removes INDEX 1
        System.out.println("     list.remove(1)            -> " + ticketNos
                           + "   (removed INDEX 1)");
        copy.remove(Integer.valueOf(1002));         // removes the VALUE
        System.out.println("     list.remove(Integer(1002)) -> " + copy
                           + "   (removed the VALUE)");
        System.out.println("     Two overloads, one looks like the other, and only one is usually");
        System.out.println("     meant.  Ticket numbers are therefore held as primitive int in this");
        System.out.println("     design, and the active register is a List<ParkingTicket> rather");
        System.out.println("     than a List<Integer> of numbers.");

        System.out.println();
        System.out.println("5.  MEASURED COST OF BOXING IN A HOT LOOP");
        System.out.println("-".repeat(80));
        final int N = 20_000_000;
        long sink = 0;
        for (int w = 0; w < 3; w++) {
            for (int i = 0; i < 2_000_000; i++) { sink += i; Integer b = i; sink += b; }
        }
        long t0 = System.nanoTime();
        long primitiveTotal = 0;
        for (int i = 0; i < N; i++) primitiveTotal += (i % 1000);
        long tP = System.nanoTime() - t0;

        long t1 = System.nanoTime();
        Long boxedTotal = 0L;                       // deliberately the wrong choice
        for (int i = 0; i < N; i++) boxedTotal += (i % 1000);   // unbox, add, re-box, every time
        long tB = System.nanoTime() - t1;

        System.out.printf("%-34s | %-14s | %-16s | %s%n",
                          "ACCUMULATOR", "TOTAL (ms)", "PER ADD (ns)", "OBJECTS CREATED");
        System.out.printf("%-34s | %-14.1f | %-16.3f | %s%n", "long  (primitive)",
                          tP / 1e6, (double) tP / N, "0");
        System.out.printf("%-34s | %-14.1f | %-16.3f | %s%n", "Long  (wrapper, autoboxed)",
                          tB / 1e6, (double) tB / N, String.format("%,d", N));
        System.out.printf("%-34s | %-14s | %-16s | %s%n", "boxing penalty", "",
                          String.format("%.1fx", (double) tB / tP), "one object per addition");
        System.out.println("  (both totals agree: " + (primitiveTotal == boxedTotal) + ")");
        System.out.println();
        System.out.println("  Every += on a Long allocates a new object, because wrappers are");
        System.out.println("  immutable.  In a fee loop over a day's tickets this is the difference");
        System.out.println("  between no garbage and one object per vehicle - which is the third");
        System.out.println("  reason the money fields in this design are primitive long.");
        if (sink == Long.MIN_VALUE) System.out.println(sink);
    }
}
