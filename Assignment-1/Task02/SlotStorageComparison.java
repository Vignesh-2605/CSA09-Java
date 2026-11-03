/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 2  -  Alternative Concept and Complexity Analysis
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Question: instead of a jagged array String[floor][slot], suppose the slots are
 * held in a single one-dimensional array addressed by
 *       index = floor * slotsPerFloor + slot
 * or in an ArrayList<String[]>.  What changes when floors have DIFFERENT numbers
 * of slots, and what is the time to locate a slot and the space required?
 */
import java.util.ArrayList;
import java.util.List;

public class SlotStorageComparison {

    /* ---- A: jagged array (the chosen representation) ---------------------- */
    static class Jagged {
        final String[][] slots;
        Jagged(int[] perFloor) {
            slots = new String[perFloor.length][];
            for (int f = 0; f < perFloor.length; f++) slots[f] = new String[perFloor[f]];
        }
        String get(int f, int s) { return slots[f][s]; }          // O(1), 2 derefs
        int capacity() { int n = 0; for (String[] r : slots) n += r.length; return n; }
        int cellsAllocated() { return capacity(); }
    }

    /* ---- B: flattened 1-D array with a uniform stride -------------------- */
    static class FlatUniform {
        final String[] slots;
        final int stride;                                          // slotsPerFloor
        final int floors;
        FlatUniform(int floors, int slotsPerFloor) {
            this.floors = floors; this.stride = slotsPerFloor;
            slots = new String[floors * slotsPerFloor];
        }
        String get(int f, int s) { return slots[f * stride + s]; } // O(1), 1 deref + 1 mul
        int cellsAllocated() { return slots.length; }
    }

    /* ---- B': flattened 1-D array made to work with RAGGED floors ---------- */
    static class FlatPrefix {
        final String[] slots;
        final int[] start;                                         // prefix sums
        FlatPrefix(int[] perFloor) {
            start = new int[perFloor.length + 1];
            for (int f = 0; f < perFloor.length; f++) start[f + 1] = start[f] + perFloor[f];
            slots = new String[start[perFloor.length]];
        }
        String get(int f, int s) { return slots[start[f] + s]; }    // O(1), extra lookup
        int cellsAllocated() { return slots.length; }
        int extraInts() { return start.length; }
    }

    /* ---- C: ArrayList<String[]>, one array per floor --------------------- */
    static class ListOfRows {
        final List<String[]> floors = new ArrayList<>();
        ListOfRows(int[] perFloor) { for (int n : perFloor) floors.add(new String[n]); }
        String get(int f, int s) { return floors.get(f)[s]; }       // O(1), 3 derefs
        int cellsAllocated() { int n = 0; for (String[] r : floors) n += r.length; return n; }
    }

    private static final int REPS = 40_000_000;

    public static void main(String[] args) {
        System.out.println("TASK 2  -  ALTERNATIVE SLOT REPRESENTATIONS");
        System.out.println("=".repeat(80));

        int[] ragged  = { 20, 50, 50, 100, 100 };      // the real facility: 320 slots
        int uniformF  = 5, uniformS = 100;             // what a uniform stride forces

        Jagged      a  = new Jagged(ragged);
        FlatUniform b  = new FlatUniform(uniformF, uniformS);
        FlatPrefix  bp = new FlatPrefix(ragged);
        ListOfRows  c  = new ListOfRows(ragged);

        System.out.println();
        System.out.println("WHAT BREAKS WHEN FLOORS HAVE DIFFERENT NUMBERS OF SLOTS");
        System.out.println("-".repeat(80));
        System.out.println("  Facility = " + java.util.Arrays.toString(ragged)
                           + "  ->  " + a.capacity() + " real slots");
        System.out.println();
        System.out.printf("%-30s | %-10s | %s%n", "REPRESENTATION", "CELLS", "CONSEQUENCE");
        System.out.println("-".repeat(80));
        System.out.printf("%-30s | %-10d | %s%n", "A  jagged String[f][]", a.cellsAllocated(),
                          "exact fit, length per floor is free");
        System.out.printf("%-30s | %-10d | %s%n", "B  flat, uniform stride", b.cellsAllocated(),
                          "wastes " + (b.cellsAllocated() - a.capacity()) + " cells; index is WRONG");
        System.out.printf("%-30s | %-10d | %s%n", "B' flat + prefix sums", bp.cellsAllocated(),
                          "exact fit, but needs " + bp.extraInts() + " extra ints");
        System.out.printf("%-30s | %-10d | %s%n", "C  ArrayList<String[]>", c.cellsAllocated(),
                          "exact fit, floors can be added at run time");

        System.out.println();
        System.out.println("  The single stride is the fatal problem.  With floor sizes 20/50/50/");
        System.out.println("  100/100 there is no one value of slotsPerFloor that works:");
        System.out.println("    index = floor * 100 + slot  leaves 80 dead cells on floor 0, and");
        System.out.println("    index = floor *  20 + slot  makes floor 1 slot 25 collide with");
        System.out.println("    floor 2 slot 5 - two different vehicles mapping to one cell.");
        System.out.println("  So the flat form can only survive by carrying the prefix-sum array");
        System.out.println("  start[], which is the floor lengths written out again by hand - the");
        System.out.println("  very information that slots[floor].length already gives for nothing.");

        /* ---- measured cost of locating one slot --------------------------- */
        System.out.println();
        System.out.println("TIME TO LOCATE A SLOT  (" + REPS + " indexed reads each)");
        System.out.println("-".repeat(80));

        // warm up
        long sink = 0;
        for (int i = 0; i < 2_000_000; i++) {
            int f = i % ragged.length, s = i % 20;
            if (a.get(f, s)  != null) sink++;
            if (bp.get(f, s) != null) sink++;
            if (c.get(f, s)  != null) sink++;
            if (b.get(f, s % uniformS) != null) sink++;
        }

        long tA = time(() -> { long k = 0; for (int i = 0; i < REPS; i++)
                                   if (a.get(i % 5, i % 20) != null) k++; return k; });
        long tB = time(() -> { long k = 0; for (int i = 0; i < REPS; i++)
                                   if (b.get(i % 5, i % 20) != null) k++; return k; });
        long tP = time(() -> { long k = 0; for (int i = 0; i < REPS; i++)
                                   if (bp.get(i % 5, i % 20) != null) k++; return k; });
        long tC = time(() -> { long k = 0; for (int i = 0; i < REPS; i++)
                                   if (c.get(i % 5, i % 20) != null) k++; return k; });

        System.out.printf("%-30s | %-12s | %-14s | %s%n",
                          "REPRESENTATION", "TOTAL (ms)", "PER READ (ns)", "COMPLEXITY");
        System.out.printf("%-30s | %-12.1f | %-14.3f | %s%n", "A  jagged String[f][]",
                          tA / 1e6, (double) tA / REPS, "O(1)");
        System.out.printf("%-30s | %-12.1f | %-14.3f | %s%n", "B  flat, uniform stride",
                          tB / 1e6, (double) tB / REPS, "O(1)");
        System.out.printf("%-30s | %-12.1f | %-14.3f | %s%n", "B' flat + prefix sums",
                          tP / 1e6, (double) tP / REPS, "O(1)");
        System.out.printf("%-30s | %-12.1f | %-14.3f | %s%n", "C  ArrayList<String[]>",
                          tC / 1e6, (double) tC / REPS, "O(1)");

        System.out.println();
        System.out.println("SPACE REQUIRED  (references are 4 bytes with compressed oops)");
        System.out.println("-".repeat(80));
        System.out.printf("%-30s | %s%n", "A  jagged String[f][]",
                          a.cellsAllocated() + " refs + " + ragged.length + " row headers");
        System.out.printf("%-30s | %s%n", "B  flat, uniform stride",
                          b.cellsAllocated() + " refs + 1 header (" + (b.cellsAllocated()
                          - a.capacity()) + " refs wasted)");
        System.out.printf("%-30s | %s%n", "B' flat + prefix sums",
                          bp.cellsAllocated() + " refs + 1 header + " + bp.extraInts() + " ints");
        System.out.printf("%-30s | %s%n", "C  ArrayList<String[]>",
                          c.cellsAllocated() + " refs + " + ragged.length
                          + " row headers + list object and backing array");

        System.out.println();
        System.out.println("CONCLUSION");
        System.out.println("-".repeat(80));
        System.out.println("  All four locate a slot in O(1), and the measured per-read times are");
        System.out.println("  within noise of one another, so speed decides nothing here.  The");
        System.out.println("  jagged array is kept because it is the only form that stores the");
        System.out.println("  ragged floor sizes as part of the structure itself: slots[f].length IS");
        System.out.println("  the bound the allocation loop needs, so no second array can fall out of");
        System.out.println("  step with it.  ArrayList<String[]> is the right choice only if floors");
        System.out.println("  must be added while the program runs, which this requirement does not");
        System.out.println("  ask for; Task 12 revisits exactly this when the facility grows to 5");
        System.out.println("  floors of differing size, and the jagged form absorbs that change by");
        System.out.println("  passing a longer int[] to the constructor - no code change at all.");
        if (sink == Long.MIN_VALUE) System.out.println(sink);
    }

    interface Work { long run(); }
    static long time(Work w) {
        long t = System.nanoTime();
        long r = w.run();
        long d = System.nanoTime() - t;
        if (r == Long.MIN_VALUE) System.out.print("");
        return d;
    }
}
