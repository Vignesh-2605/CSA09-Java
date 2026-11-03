/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 3  -  Alternative Concept and Complexity Analysis
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Question: allocation currently SCANS every floor and slot until a free one is
 * found.  If instead a count of free slots per floor and a STACK of free slot
 * numbers per floor were maintained, what would allocate() and release() become,
 * and what is the time complexity and extra space for F floors and S slots?
 */
public class FastAllocator {

    /* ---- A: the scanning allocator (Task 3 as submitted) ----------------- */
    static class Scanning {
        final String[][] slots;
        long cellsRead = 0;
        Scanning(int f, int s) { slots = new String[f][s]; }

        int[] allocate(String v) {
            for (int f = 0; f < slots.length; f++)
                for (int s = 0; s < slots[f].length; s++) {
                    cellsRead++;
                    if (slots[f][s] == null) { slots[f][s] = v; return new int[]{f, s}; }
                }
            return null;
        }
        boolean release(int f, int s) {
            if (f < 0 || f >= slots.length || s < 0 || s >= slots[f].length) return false;
            if (slots[f][s] == null) return false;
            slots[f][s] = null; return true;
        }
    }

    /* ---- B: free-count per floor + a stack of free slot numbers ---------- */
    static class Stacked {
        final String[][] slots;
        final int[][] freeStack;          // freeStack[f][0..freeCount[f]-1] = free slot numbers
        final int[] freeCount;            // how many of them are valid
        long cellsRead = 0;

        Stacked(int f, int s) {
            slots     = new String[f][s];
            freeStack = new int[f][s];
            freeCount = new int[f];
            for (int i = 0; i < f; i++) {            // every slot starts free
                for (int j = 0; j < s; j++) freeStack[i][j] = s - 1 - j;  // pushed in order
                freeCount[i] = s;
            }
        }

        /** O(F) worst case, O(1) when the first non-empty floor is hit at once. */
        int[] allocate(String v) {
            for (int f = 0; f < slots.length; f++) {
                cellsRead++;                                   // one freeCount read
                if (freeCount[f] > 0) {
                    int s = freeStack[f][--freeCount[f]];       // POP: O(1)
                    slots[f][s] = v;
                    return new int[]{f, s};
                }
            }
            return null;
        }

        /** O(1): validate, clear the cell, PUSH the slot number back. */
        boolean release(int f, int s) {
            if (f < 0 || f >= slots.length || s < 0 || s >= slots[f].length) return false;
            if (slots[f][s] == null) return false;
            slots[f][s] = null;
            freeStack[f][freeCount[f]++] = s;                   // PUSH: O(1)
            return true;
        }
    }

    public static void main(String[] args) {
        System.out.println("TASK 3  -  ALTERNATIVE: SCANNING  vs  FREE-COUNT + FREE-SLOT STACK");
        System.out.println("=".repeat(80));

        final int F = 5, S = 100;          // the 5 x 100 facility from the problem statement
        final int ROUNDS = 4_000;

        System.out.println();
        System.out.println("FACILITY  F = " + F + " floors, S = " + S
                           + " slots per floor, capacity = " + (F * S));
        System.out.println("WORKLOAD  fill the facility, then " + ROUNDS
                           + " release+allocate pairs on the LAST slot");
        System.out.println("          (the worst case for scanning: the only free cell is last)");

        /* ---- warm up ---- */
        for (int w = 0; w < 3; w++) {
            Scanning ws = new Scanning(F, S); Stacked wk = new Stacked(F, S);
            for (int i = 0; i < F * S; i++) { ws.allocate("x"); wk.allocate("x"); }
            for (int i = 0; i < 500; i++) {
                ws.release(F - 1, S - 1); ws.allocate("x");
                wk.release(F - 1, S - 1); wk.allocate("x");
            }
        }

        /* ---- measure A ---- */
        Scanning a = new Scanning(F, S);
        for (int i = 0; i < F * S; i++) a.allocate("V" + i);
        a.cellsRead = 0;
        long t0 = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) { a.release(F - 1, S - 1); a.allocate("V" + i); }
        long tA = System.nanoTime() - t0;

        /* ---- measure B ---- */
        Stacked b = new Stacked(F, S);
        for (int i = 0; i < F * S; i++) b.allocate("V" + i);
        b.cellsRead = 0;
        long t1 = System.nanoTime();
        for (int i = 0; i < ROUNDS; i++) { b.release(F - 1, S - 1); b.allocate("V" + i); }
        long tB = System.nanoTime() - t1;

        System.out.println();
        System.out.println("MEASURED RESULT");
        System.out.println("-".repeat(80));
        System.out.printf("%-30s | %-12s | %-16s | %s%n",
                          "DESIGN", "TOTAL (ms)", "PER PAIR (us)", "CELLS READ");
        System.out.printf("%-30s | %-12.2f | %-16.3f | %,d%n",
                          "A  scanning", tA / 1e6, tA / 1e3 / ROUNDS, a.cellsRead);
        System.out.printf("%-30s | %-12.2f | %-16.3f | %,d%n",
                          "B  free-count + stack", tB / 1e6, tB / 1e3 / ROUNDS, b.cellsRead);
        System.out.printf("%-30s | %-12s | %-16s | %s%n", "B is faster by", "",
                          String.format("%.1fx", (double) tA / tB),
                          String.format("%.0fx fewer reads", (double) a.cellsRead / b.cellsRead));

        System.out.println();
        System.out.println("COMPLEXITY");
        System.out.println("-".repeat(80));
        System.out.printf("%-14s | %-26s | %-26s | %s%n",
                          "OPERATION", "A  SCANNING", "B  COUNT + STACK", "WHY");
        System.out.println("-".repeat(80));
        System.out.printf("%-14s | %-26s | %-26s | %s%n", "allocate()", "O(F x S)", "O(F)",
                          "B tests a count, not a cell");
        System.out.printf("%-14s | %-26s | %-26s | %s%n", "allocate() best", "O(1)", "O(1)",
                          "both stop on the first floor");
        System.out.printf("%-14s | %-26s | %-26s | %s%n", "release()", "O(1)", "O(1)",
                          "both index straight to the cell");
        System.out.println();
        System.out.println("  With F = " + F + " and S = " + S + ", the worst case falls from "
                           + (F * S) + " reads to " + F + " -");
        System.out.println("  a factor of S.  As the facility grows the gap widens: for 5 x 1000 it");
        System.out.println("  would be 5000 reads against 5.");

        System.out.println();
        System.out.println("EXTRA SPACE");
        System.out.println("-".repeat(80));
        System.out.printf("%-30s | %-22s | %s%n", "DESIGN", "EXTRA STRUCTURES", "EXTRA BYTES (4 B/int)");
        System.out.printf("%-30s | %-22s | %s%n", "A  scanning", "none", "0");
        System.out.printf("%-30s | %-22s | %s%n", "B  free-count + stack",
                          "int[F][S] + int[F]", (F * S * 4 + F * 4) + " B ("
                          + ((F * S * 4 + F * 4) / 1024.0 > 1
                             ? String.format("%.1f KB", (F * S * 4 + F * 4) / 1024.0) : "under 1 KB") + ")");
        System.out.println();
        System.out.println("  B spends one extra int per slot - the same order as the slot array");
        System.out.println("  itself - plus F ints for the counts, so the space grows as O(F x S).");

        System.out.println();
        System.out.println("CONCLUSION");
        System.out.println("-".repeat(80));
        System.out.println("  B is strictly faster and is the correct choice for a large, busy");
        System.out.println("  facility.  A is kept for THIS submission for two reasons: the required");
        System.out.println("  main() in ParkingFacility.java fixes the allocation ORDER (the first");
        System.out.println("  free slot scanning floor by floor), and a LIFO stack hands back the");
        System.out.println("  most recently freed slot instead - after release(0,1) the expected");
        System.out.println("  output demands [0, 1], which a stack over floor 0 would also give here");
        System.out.println("  only by coincidence, and would not in general.  The stack is therefore");
        System.out.println("  the right optimisation to adopt once the allocation order is free to");
        System.out.println("  change, and Task 12 notes it as the extension point.");
    }
}
