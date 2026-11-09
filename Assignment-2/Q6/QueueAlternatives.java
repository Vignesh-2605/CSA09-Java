/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q6  -  Alternative Concept and Complexity Analysis
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * "If the triage queue were implemented as a LinkedList kept in sorted order,
 *  or as an array of five FIFO queues - one for each severity level - what
 *  would the solution be?"
 */
import bench.Bench;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;

public class QueueAlternatives {

    record Case(String name, int severity, long arrival) {
        @Override public String toString() { return name + "(S" + severity + ")"; }
    }
    static final Comparator<Case> ORDER =
            Comparator.comparingInt(Case::severity).thenComparingLong(Case::arrival);

    /* ---- A: the submitted design ---- */
    static final class HeapQueue {
        private final PriorityQueue<Case> q = new PriorityQueue<>(ORDER);
        private long seq = 0;
        void admit(String n, int s) { q.add(new Case(n, s, seq++)); }   // O(log n)
        Case next() { return q.poll(); }                                 // O(log n)
        int size() { return q.size(); }
    }

    /* ---- B: a LinkedList kept in sorted order ---- */
    static final class SortedListQueue {
        private final LinkedList<Case> q = new LinkedList<>();
        private long seq = 0;
        /** O(n): walk from the head until the insertion point is found. */
        void admit(String n, int s) {
            Case c = new Case(n, s, seq++);
            ListIterator<Case> it = q.listIterator();
            while (it.hasNext()) {
                if (ORDER.compare(it.next(), c) > 0) { it.previous(); break; }
            }
            it.add(c);
        }
        Case next() { return q.pollFirst(); }                            // O(1)
        int size() { return q.size(); }
    }

    /* ---- C: five FIFO queues, one per severity level ---- */
    static final class BucketQueue {
        @SuppressWarnings("unchecked")
        private final Deque<Case>[] level = new ArrayDeque[6];           // 1..5 used
        private int count = 0;
        private long seq = 0;
        BucketQueue() { for (int i = 1; i <= 5; i++) level[i] = new ArrayDeque<>(); }
        /** O(1): append to the bucket for this severity. */
        void admit(String n, int s) { level[s].addLast(new Case(n, s, seq++)); count++; }
        /** O(1): at most 5 bucket checks, independent of n. */
        Case next() {
            for (int s = 1; s <= 5; s++)
                if (!level[s].isEmpty()) { count--; return level[s].pollFirst(); }
            return null;
        }
        int size() { return count; }
    }

    public static void main(String[] args) {
        System.out.println("Q6  -  ALTERNATIVE QUEUE IMPLEMENTATIONS");
        System.out.println("=".repeat(118));
        System.out.println("Name: Vignesh S   Register Number: 192325002");

        System.out.println();
        System.out.println("1.  DO ALL THREE PRODUCE THE SAME TREATMENT ORDER?");
        System.out.println("-".repeat(118));
        String[][] admissions = {
            {"Ravi","3"},{"Meena","1"},{"John","3"},{"Asha","5"},
            {"Kumar","1"},{"Lata","2"},{"Sita","5"},{"Dev","2"}};
        HeapQueue a = new HeapQueue(); SortedListQueue b = new SortedListQueue(); BucketQueue c = new BucketQueue();
        for (String[] ad : admissions) {
            int s = Integer.parseInt(ad[1]);
            a.admit(ad[0], s); b.admit(ad[0], s); c.admit(ad[0], s);
        }
        String oa = drain(a::next), ob = drain(b::next), oc = drain(c::next);
        System.out.println("  A PriorityQueue    : " + oa);
        System.out.println("  B sorted LinkedList: " + ob);
        System.out.println("  C five FIFO buckets: " + oc);
        System.out.printf("%n  all three agree: %s%n", oa.equals(ob) && ob.equals(oc));
        System.out.println();
        System.out.println("  C is interesting: it needs no comparator and no arrival counter at");
        System.out.println("  all.  FIFO within a bucket preserves arrival order automatically, and");
        System.out.println("  scanning buckets 1..5 in order enforces severity.  The two rules the");
        System.out.println("  comparator encoded are expressed by the STRUCTURE instead.");

        System.out.println();
        System.out.println("2.  MEASURED  (admit n patients, then serve all of them)");
        System.out.println("-".repeat(118));
        int[] sizes = { 1_000, 10_000, 100_000 };
        List<Bench.Result> all = Bench.list();
        Bench.tableHead();
        for (int n : sizes) {
            final int size = n;
            all.add(show(Bench.measure("A PriorityQueue", "admit+serve all", n, 2L * n, () -> {
                HeapQueue q = new HeapQueue();
                for (int i = 0; i < size; i++) q.admit("P" + i, 1 + (i * 7) % 5);
                long s = 0; Case x; while ((x = q.next()) != null) s += x.severity(); sink(s);
            })));
            if (n <= 10_000)
                all.add(show(Bench.measure("B sorted LinkedList", "admit+serve all", n, 2L * n, () -> {
                    SortedListQueue q = new SortedListQueue();
                    for (int i = 0; i < size; i++) q.admit("P" + i, 1 + (i * 7) % 5);
                    long s = 0; Case x; while ((x = q.next()) != null) s += x.severity(); sink(s);
                })));
            all.add(show(Bench.measure("C five FIFO buckets", "admit+serve all", n, 2L * n, () -> {
                BucketQueue q = new BucketQueue();
                for (int i = 0; i < size; i++) q.admit("P" + i, 1 + (i * 7) % 5);
                long s = 0; Case x; while ((x = q.next()) != null) s += x.severity(); sink(s);
            })));
        }
        System.out.println();
        System.out.println("  B is omitted above n = 10,000 because its O(n) insertion makes the");
        System.out.println("  whole run O(n^2) - at n = 100,000 it would take minutes.  Leaving it");
        System.out.println("  out is itself the result, not an evasion.");

        System.out.println();
        System.out.println("3.  COMPLEXITY AND SPACE");
        System.out.println("-".repeat(118));
        System.out.printf("%-24s | %-14s | %-14s | %-16s | %s%n",
                "DESIGN", "admit", "next", "waiting list", "SPACE");
        System.out.println("-".repeat(118));
        String[][] rows = {
            {"A PriorityQueue",     "O(log n)", "O(log n)", "O(n log n) sort", "1 array slot/case"},
            {"B sorted LinkedList", "O(n)",     "O(1)",     "O(n), free",      "24 B node/case"},
            {"C five FIFO buckets", "O(1)",     "O(1)*",    "O(n), free",      "1 slot + 5 deques"},
        };
        for (String[] r : rows) System.out.printf("%-24s | %-14s | %-14s | %-16s | %s%n", r[0], r[1], r[2], r[3], r[4]);
        System.out.println();
        System.out.println("  * C's next() scans at most 5 buckets.  That is O(k) where k is the");
        System.out.println("    number of severity LEVELS, not the number of patients - and k is");
        System.out.println("    fixed at 5 by the triage scale.  A constant bound of 5 is O(1).");
        System.out.println();
        System.out.println("  C IS THE BEST OF THE THREE ON EVERY OPERATION, and the reason is worth");
        System.out.println("  stating precisely: it exploits a property of the PROBLEM that the other");
        System.out.println("  two ignore.  Severity is drawn from a small, fixed, known range.  A");
        System.out.println("  PriorityQueue is a general-purpose comparison structure that would work");
        System.out.println("  for any key whatsoever, and pays log n for that generality.  When the");
        System.out.println("  key range is bounded, the comparisons can be replaced by indexing -");
        System.out.println("  the same insight that makes counting sort beat comparison sorts on");
        System.out.println("  small integer keys.");
        System.out.println();
        System.out.println("  WHERE C WOULD STOP BEING THE RIGHT ANSWER: if severity became a");
        System.out.println("  continuous clinical score rather than 5 discrete levels, the bucket");
        System.out.println("  array would need one entry per distinct score and the approach would");
        System.out.println("  collapse.  A PriorityQueue does not care how many distinct keys exist.");
        System.out.println();
        System.out.println("  This finding is the starting point for Q7.");

        Bench.writeCsv("Q6-alternatives", all);
    }

    interface Source { Case next(); }
    static String drain(Source s) {
        StringBuilder sb = new StringBuilder(); Case c;
        while ((c = s.next()) != null) sb.append(c.name()).append(' ');
        return sb.toString().trim();
    }
    static Bench.Result show(Bench.Result r) { Bench.row(r); return r; }
    static long SINK;
    static void sink(long v) { SINK += v; }
}
