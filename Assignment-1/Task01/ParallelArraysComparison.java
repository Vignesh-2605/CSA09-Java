/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 1  -  Alternative Concept and Complexity Analysis
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Question: in the chosen design each vehicle is an object.  If the vehicles were
 * instead held as plain Strings in parallel arrays regNo[], type[], entryTime[],
 * what would the solution look like?
 *
 * Both designs are implemented and measured here for
 *   (a) finding a vehicle by registration number, and
 *   (b) adding a new vehicle attribute.
 */
public class ParallelArraysComparison {

    /* ---------- DESIGN A: one object per vehicle (the chosen design) ---------- */
    static class ParkedVehicle {
        final String regNo;
        final String type;
        final int entryMinute;
        ParkedVehicle(String regNo, String type, int entryMinute) {
            this.regNo = regNo; this.type = type; this.entryMinute = entryMinute;
        }
    }

    static ParkedVehicle findObject(ParkedVehicle[] a, int n, String regNo) {
        for (int i = 0; i < n; i++) if (a[i].regNo.equals(regNo)) return a[i];
        return null;
    }

    /* ---------- DESIGN B: three parallel String arrays ---------- */
    static int findParallel(String[] regNo, int n, String target) {
        for (int i = 0; i < n; i++) if (regNo[i].equals(target)) return i;
        return -1;                       // the caller must then index every other array
    }

    /* ---------- measurement helpers ---------- */
    private static final int N = 200_000;
    private static final int PROBES = 2_000;

    public static void main(String[] args) {
        System.out.println("TASK 1  -  ALTERNATIVE CONCEPT: OBJECTS  vs  PARALLEL STRING ARRAYS");
        System.out.println("=".repeat(78));

        // build both structures with identical data
        ParkedVehicle[] objects = new ParkedVehicle[N];
        String[] regNo = new String[N], type = new String[N], entryTime = new String[N];
        String[] types = { "TWO_WHEELER", "CAR", "EV" };
        for (int i = 0; i < N; i++) {
            String r = String.format("TN%02dZZ%04d", i % 100, i);
            objects[i]   = new ParkedVehicle(r, types[i % 3], 540 + i % 600);
            regNo[i]     = r;
            type[i]      = types[i % 3];
            entryTime[i] = String.valueOf(540 + i % 600);
        }

        // probe keys spread across the whole range, including misses
        String[] keys = new String[PROBES];
        for (int p = 0; p < PROBES; p++) {
            keys[p] = (p % 10 == 0) ? "ABSENT" + p
                                    : String.format("TN%02dZZ%04d", (p * 97) % 100, (p * 97) % N);
        }

        // warm the JIT so the measurement reflects steady-state code
        long sink = 0;
        for (int w = 0; w < 5; w++) {
            for (String k : keys) {
                if (findObject(objects, N, k) != null) sink++;
                if (findParallel(regNo, N, k) >= 0)    sink++;
            }
        }

        long t0 = System.nanoTime();
        int hitsA = 0;
        for (String k : keys) if (findObject(objects, N, k) != null) hitsA++;
        long tA = System.nanoTime() - t0;

        long t1 = System.nanoTime();
        int hitsB = 0;
        for (String k : keys) {
            int i = findParallel(regNo, N, k);
            if (i >= 0) { sink += type[i].length() + entryTime[i].length(); hitsB++; }
        }
        long tB = System.nanoTime() - t1;

        System.out.println();
        System.out.println("(a) FINDING A VEHICLE BY REGISTRATION NUMBER");
        System.out.println("    records n = " + N + ", probes = " + PROBES
                           + "  (hits A=" + hitsA + ", B=" + hitsB + ")");
        System.out.println("-".repeat(78));
        System.out.printf("%-34s | %-14s | %-14s | %s%n",
                          "DESIGN", "TOTAL (ms)", "PER PROBE (us)", "COMPLEXITY");
        System.out.printf("%-34s | %-14.2f | %-14.2f | %s%n",
                          "A  array of objects", tA / 1e6, tA / 1e3 / PROBES, "O(n)");
        System.out.printf("%-34s | %-14.2f | %-14.2f | %s%n",
                          "B  parallel String arrays", tB / 1e6, tB / 1e3 / PROBES, "O(n)");
        System.out.println();
        System.out.println("    Both scan linearly, so both are O(n); neither design makes the");
        System.out.println("    search faster by itself.  The difference is what happens after the");
        System.out.println("    index is found: design A returns one reference that already carries");
        System.out.println("    every field, while design B returns a bare index that the caller");
        System.out.println("    must use to read each parallel array separately.");

        System.out.println();
        System.out.println("(b) ADDING A NEW VEHICLE ATTRIBUTE (for example: ownerCategory)");
        System.out.println("-".repeat(78));
        System.out.printf("%-34s | %-26s | %s%n", "DESIGN", "CHANGE REQUIRED", "RISK");
        System.out.printf("%-34s | %-26s | %s%n",
                          "A  array of objects", "one field in one class", "none, compiler checks it");
        System.out.printf("%-34s | %-26s | %s%n",
                          "B  parallel String arrays", "a 4th array + every loop", "arrays silently desynchronise");

        System.out.println();
        System.out.println("SPACE PER VEHICLE");
        System.out.println("-".repeat(78));
        System.out.printf("%-34s | %s%n", "A  array of objects",
                          "1 reference (4-8 B) + 1 header (16 B) + 2 refs + 1 int");
        System.out.printf("%-34s | %s%n", "B  parallel String arrays",
                          "3 references (12-24 B), no per-vehicle header");
        System.out.println();
        System.out.println("    Design B saves roughly one object header (about 16 bytes) per vehicle,");
        System.out.println("    which for n = " + N + " is about "
                           + (N * 16L / 1024) + " KB.  Design A spends that memory to buy a single");
        System.out.println("    unit of identity: the three facts about one vehicle cannot drift apart,");
        System.out.println("    and a new attribute is one edit in one place instead of a fourth array");
        System.out.println("    that every existing loop has to be taught about.");

        System.out.println();
        System.out.println("CONCLUSION");
        System.out.println("-".repeat(78));
        System.out.println("  Search cost is identical, so the choice is decided by correctness and");
        System.out.println("  maintenance, not speed.  The object design is retained: it keeps the");
        System.out.println("  invariant that one vehicle is one thing, it is the only design that can");
        System.out.println("  later carry polymorphic behaviour such as calculateFee() (Tasks 7-8),");
        System.out.println("  and parallel arrays cannot express a type hierarchy at all.");
        if (sink == Long.MIN_VALUE) System.out.println(sink);   // keep the JIT honest
    }
}
