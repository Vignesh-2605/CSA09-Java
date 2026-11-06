/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 11  -  Alternative Concept and Complexity Analysis
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Question: active tickets are currently held in an array (a List).  If they
 * were held in an ArrayList<ParkingTicket> or a HashMap<String,ParkingTicket>
 * keyed by registration number, what would EXIT PROCESSING become?  Measure and
 * analyse the time to find a ticket on exit and the space used for n parked
 * vehicles in each structure.
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TicketStoreComparison {

    /** A minimal ticket: enough to be looked up and billed. */
    static final class Ticket {
        final int no; final String regNo; final int entryMinute;
        Ticket(int no, String regNo, int entryMinute) {
            this.no = no; this.regNo = regNo; this.entryMinute = entryMinute;
        }
    }

    /* ---- A: plain array, linear scan (the submitted design) ------------- */
    static final class ArrayStore {
        private Ticket[] a; private int size = 0;
        ArrayStore(int cap) { a = new Ticket[cap]; }
        void add(Ticket t) {
            if (size == a.length) a = Arrays.copyOf(a, a.length * 2);
            a[size++] = t;
        }
        /** EXIT PROCESSING: scan until the plate matches. */
        Ticket findAndRemove(String regNo) {
            for (int i = 0; i < size; i++) {
                if (a[i].regNo.equals(regNo)) {
                    Ticket found = a[i];
                    a[i] = a[--size];            // swap the last element down: O(1)
                    a[size] = null;
                    return found;
                }
            }
            return null;
        }
        int size() { return size; }
    }

    /* ---- B: ArrayList, linear scan -------------------------------------- */
    static final class ListStore {
        private final List<Ticket> list = new ArrayList<>();
        void add(Ticket t) { list.add(t); }
        Ticket findAndRemove(String regNo) {
            for (int i = 0; i < list.size(); i++)
                if (list.get(i).regNo.equals(regNo)) return list.remove(i);   // O(n) shift
            return null;
        }
        int size() { return list.size(); }
    }

    /* ---- C: HashMap keyed by registration number ------------------------ */
    static final class MapStore {
        private final Map<String, Ticket> map = new HashMap<>();
        void add(Ticket t) {
            if (map.putIfAbsent(t.regNo, t) != null)
                throw new IllegalStateException(t.regNo + " is already inside");  // free rule check
        }
        /** EXIT PROCESSING: one hash, one bucket probe. */
        Ticket findAndRemove(String regNo) { return map.remove(regNo); }
        int size() { return map.size(); }
    }

    static String plate(int i) { return String.format("TN%02dZZ%04d", i % 100, i); }

    public static void main(String[] args) {
        System.out.println("TASK 11  -  ALTERNATIVE TICKET STORES FOR EXIT PROCESSING");
        System.out.println("=".repeat(84));

        System.out.println();
        System.out.println("1.  WHAT EXIT PROCESSING BECOMES IN EACH DESIGN");
        System.out.println("-".repeat(84));
        System.out.println("  A  array       for each slot, compare the plate; swap the last entry");
        System.out.println("                 into the hole.  Find O(n), remove O(1).");
        System.out.println("  B  ArrayList   the same scan, but remove(i) shifts every later");
        System.out.println("                 element.  Find O(n), remove O(n).");
        System.out.println("  C  HashMap     map.remove(regNo).  Find and remove O(1) expected,");
        System.out.println("                 and the duplicate-entry rule becomes putIfAbsent -");
        System.out.println("                 the O(n) scan at ENTRY disappears as well.");

        /* ---- correctness ---- */
        System.out.println();
        System.out.println("2.  ALL THREE RETURN THE SAME TICKET");
        System.out.println("-".repeat(84));
        ArrayStore ca = new ArrayStore(8); ListStore cl = new ListStore(); MapStore cm = new MapStore();
        for (int i = 0; i < 5; i++) {
            Ticket t = new Ticket(1000 + i, plate(i), 540 + i);
            ca.add(t); cl.add(t); cm.add(t);
        }
        System.out.printf("%-16s | %-10s | %-10s | %-10s | %s%n",
                          "PLATE SOUGHT", "A array", "B list", "C map", "AGREE?");
        System.out.println("-".repeat(84));
        for (String q : new String[]{ plate(0), plate(3), plate(4), "ABSENT0000" }) {
            Ticket a = ca.findAndRemove(q), b = cl.findAndRemove(q), c = cm.findAndRemove(q);
            String sa = a == null ? "null" : "#" + a.no;
            String sb = b == null ? "null" : "#" + b.no;
            String sc = c == null ? "null" : "#" + c.no;
            System.out.printf("%-16s | %-10s | %-10s | %-10s | %s%n", q, sa, sb, sc,
                              sa.equals(sb) && sb.equals(sc) ? "yes" : "NO");
        }

        /* ---- measurement ---- */
        System.out.println();
        System.out.println("3.  MEASURED EXIT PROCESSING");
        System.out.println("-".repeat(84));
        System.out.println("  Workload: fill the store with n tickets, then process EVERY vehicle's");
        System.out.println("  exit in a shuffled order - that is, n find-and-remove operations.");
        System.out.println();
        System.out.printf("%-10s | %-14s | %-14s | %-14s | %s%n",
                          "n", "A array (ms)", "B list (ms)", "C map (ms)", "C IS FASTER BY");
        System.out.println("-".repeat(84));

        int[] sizes = { 320, 1_000, 5_000, 20_000, 50_000 };
        double[][] results = new double[sizes.length][3];

        // warm up
        for (int w = 0; w < 3; w++) runAll(2_000, 12345L);

        for (int si = 0; si < sizes.length; si++) {
            int n = sizes[si];
            long[] r = runAll(n, 42L);
            results[si][0] = r[0] / 1e6; results[si][1] = r[1] / 1e6; results[si][2] = r[2] / 1e6;
            System.out.printf("%-10s | %-14.2f | %-14.2f | %-14.2f | %.0fx (vs array)%n",
                              String.format("%,d", n), results[si][0], results[si][1],
                              results[si][2], results[si][0] / Math.max(results[si][2], 1e-6));
        }

        System.out.println();
        System.out.println("  per-operation cost, microseconds:");
        System.out.printf("%-10s | %-14s | %-14s | %s%n", "n", "A array", "B list", "C map");
        System.out.println("-".repeat(84));
        for (int si = 0; si < sizes.length; si++) {
            int n = sizes[si];
            System.out.printf("%-10s | %-14.4f | %-14.4f | %.4f%n", String.format("%,d", n),
                              results[si][0] * 1000 / n, results[si][1] * 1000 / n,
                              results[si][2] * 1000 / n);
        }
        System.out.println();
        System.out.println("  The shape of those columns is the whole answer.  A and B grow roughly");
        System.out.println("  in proportion to n per operation, so the TOTAL grows as n squared; C's");
        System.out.println("  per-operation column stays flat as n grows by a factor of 150.");

        System.out.println();
        System.out.println("4.  COMPLEXITY");
        System.out.println("-".repeat(84));
        System.out.printf("%-22s | %-16s | %-16s | %-16s | %s%n",
                          "OPERATION", "A array", "B ArrayList", "C HashMap", "NOTE");
        System.out.println("-".repeat(84));
        System.out.printf("%-22s | %-16s | %-16s | %-16s | %s%n", "find on exit",
                          "O(n)", "O(n)", "O(1) expected", "C hashes the plate");
        System.out.printf("%-22s | %-16s | %-16s | %-16s | %s%n", "remove after find",
                          "O(1) swap", "O(n) shift", "O(1)", "B must shift the tail");
        System.out.printf("%-22s | %-16s | %-16s | %-16s | %s%n", "duplicate check at entry",
                          "O(n)", "O(n)", "O(1)", "C gets it free");
        System.out.printf("%-22s | %-16s | %-16s | %-16s | %s%n", "one full day, n exits",
                          "O(n^2)", "O(n^2)", "O(n)", "the figure that matters");
        System.out.printf("%-22s | %-16s | %-16s | %-16s | %s%n", "list all inside",
                          "O(n)", "O(n)", "O(n)", "C loses slot ORDER");

        System.out.println();
        System.out.println("5.  SPACE FOR n PARKED VEHICLES  (4-byte references)");
        System.out.println("-".repeat(84));
        System.out.printf("%-22s | %-30s | %s%n", "STRUCTURE", "OVERHEAD PER VEHICLE", "n = 320");
        System.out.println("-".repeat(84));
        System.out.printf("%-22s | %-30s | %s%n", "A array",
                          "4 B reference, 0 extra objects", (320 * 4) + " B");
        System.out.printf("%-22s | %-30s | %s%n", "B ArrayList",
                          "4 B + up to 50% spare capacity", "~" + (320 * 4 * 3 / 2) + " B");
        System.out.printf("%-22s | %-30s | %s%n", "C HashMap",
                          "4 B + Node object ~32 B + table", "~" + (320 * 40) + " B");
        System.out.println();
        System.out.println("  C costs roughly 10x the per-vehicle overhead of A: each entry needs a");
        System.out.println("  Node object holding the hash, the key, the value and a next pointer,");
        System.out.println("  plus a backing table kept below 75% full.  For a 320-slot facility");
        System.out.println("  that is about " + (320 * 40 / 1024) + " KB against "
                           + (320 * 4) + " bytes - a difference of a few kilobytes,");
        System.out.println("  which is nothing, while the time difference is a factor of "
                           + String.format("%.0f", results[0][0] / Math.max(results[0][2], 1e-6))
                           + " even at");
        System.out.println("  n = 320 and grows without limit.");

        System.out.println();
        System.out.println("6.  CONCLUSION AND THE RECOMMENDED CHANGE");
        System.out.println("-".repeat(84));
        System.out.println("  The measurements do not support keeping the linear store, and this is");
        System.out.println("  the one place in the whole assignment where the alternative is simply");
        System.out.println("  BETTER rather than a trade-off:");
        System.out.println();
        System.out.println("    - exit lookup goes from O(n) to O(1);");
        System.out.println("    - the duplicate-plate rule, which currently costs an O(n) scan on");
        System.out.println("      every entry, becomes a single putIfAbsent;");
        System.out.println("    - a day's processing goes from O(n^2) to O(n);");
        System.out.println("    - the cost is a few kilobytes of Node overhead.");
        System.out.println();
        System.out.println("  RECOMMENDATION: replace the active-ticket List in ParkingFacility with");
        System.out.println("  a LinkedHashMap<String,ParkingTicket> keyed by registration number.");
        System.out.println("  LinkedHashMap rather than HashMap because the facility also has to");
        System.out.println("  LIST the vehicles inside - for the occupancy report and the end-of-day");
        System.out.println("  reconciliation - and a plain HashMap would return them in an arbitrary");
        System.out.println("  order that changes between runs, which makes a report impossible to");
        System.out.println("  check by hand.  LinkedHashMap keeps O(1) lookup AND entry order, for");
        System.out.println("  one extra pair of pointers per entry.");
        System.out.println();

        // demonstrate the recommended structure doing both jobs
        Map<String, Ticket> recommended = new LinkedHashMap<>();
        for (int i = 0; i < 5; i++) recommended.put(plate(i), new Ticket(2000 + i, plate(i), 540 + i));
        recommended.remove(plate(2));
        System.out.println("  demonstration - LinkedHashMap after admitting 5 and releasing 1:");
        System.out.print("    vehicles inside, in entry order: ");
        for (String k : recommended.keySet()) System.out.print(k + " ");
        System.out.println();
        System.out.println("    lookup of " + plate(3) + " -> ticket #"
                           + recommended.get(plate(3)).no + "  (one probe, no scan)");
        System.out.println();
        System.out.println("  Why the submitted code still uses a List: the change touches only the");
        System.out.println("  PRIVATE field 'active' inside ParkingFacility.  Because that field is");
        System.out.println("  private and every caller goes through admit() and release(), the");
        System.out.println("  substitution is invisible outside the class - which is the practical");
        System.out.println("  proof that the Task 10 encapsulation was worth having.  It is listed");
        System.out.println("  as the first item of planned work in the Task 12 defence.");
    }

    /** Runs the identical workload against all three stores; returns their nanos. */
    static long[] runAll(int n, long seed) {
        ArrayStore a = new ArrayStore(16);
        ListStore  b = new ListStore();
        MapStore   c = new MapStore();
        String[] plates = new String[n];
        for (int i = 0; i < n; i++) {
            plates[i] = plate(i);
            Ticket t = new Ticket(1000 + i, plates[i], 540 + (i % 600));
            a.add(t); b.add(t); c.add(t);
        }
        // a fixed shuffle, so every store faces the same exit order
        java.util.Random rnd = new java.util.Random(seed);
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            String tmp = plates[i]; plates[i] = plates[j]; plates[j] = tmp;
        }

        long hits = 0;
        long t0 = System.nanoTime();
        for (String p : plates) if (a.findAndRemove(p) != null) hits++;
        long tA = System.nanoTime() - t0;

        long t1 = System.nanoTime();
        for (String p : plates) if (b.findAndRemove(p) != null) hits++;
        long tB = System.nanoTime() - t1;

        long t2 = System.nanoTime();
        for (String p : plates) if (c.findAndRemove(p) != null) hits++;
        long tC = System.nanoTime() - t2;

        if (hits != 3L * n) throw new AssertionError("stores disagreed: " + hits + " != " + 3 * n);
        if (a.size() != 0 || b.size() != 0 || c.size() != 0)
            throw new AssertionError("a store was left non-empty");
        return new long[]{ tA, tB, tC };
    }
}
