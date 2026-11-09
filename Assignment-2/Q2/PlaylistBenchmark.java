/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q2  -  When Is ArrayList an Appropriate Engineering Choice?
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Five operations x three structures x four playlist sizes, every measurement
 * repeated and the median reported.  The prediction is printed before any
 * measurement is taken.
 */
import bench.Bench;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class PlaylistBenchmark {

    record Song(int id, String title) {
        @Override public String toString() { return "#" + id; }
    }

    static final int[] SIZES = { 1_000, 10_000, 100_000, 200_000 };

    public static void main(String[] args) {
        System.out.println("Q2  -  ArrayList, LinkedList AND ArrayDeque FOR A PLAYLIST EDITOR");
        System.out.println("=".repeat(118));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.printf("Harness: %d warm-up runs discarded, %d measured repetitions, MEDIAN reported.%n",
                Bench.WARMUP, Bench.REPS);

        System.out.println();
        System.out.println("1.  PREDICTION  (recorded before any measurement)");
        System.out.println("-".repeat(118));
        System.out.printf("%-24s | %-16s | %-16s | %-16s | %s%n",
                "OPERATION", "ArrayList", "LinkedList", "ArrayDeque", "WHY");
        System.out.println("-".repeat(118));
        String[][] pred = {
            {"add at end",        "O(1) amortised", "O(1)",          "O(1) amortised", "array doubling is rare; node alloc is constant"},
            {"insert at front",   "O(n)",           "O(1)",          "O(1)",           "array must shift every element right"},
            {"insert in middle",  "O(n) shift",     "O(n) walk",     "not supported",  "both O(n), but a shift is far cheaper than a walk"},
            {"get k-th",          "O(1)",           "O(n)",          "no index API",   "one array index vs following k references"},
            {"iterate all",       "O(n), cache-friendly", "O(n), pointer chase", "O(n)", "array is contiguous; nodes are scattered"},
            {"remove from front", "O(n)",           "O(1)",          "O(1)",           "same shifting cost as inserting at the front"},
        };
        for (String[] p : pred)
            System.out.printf("%-24s | %-16s | %-16s | %-16s | %s%n", p[0], p[1], p[2], p[3], p[4]);
        System.out.println();
        System.out.println("  The prediction worth testing is NOT the Big-O - that is textbook - but");
        System.out.println("  the claim hidden inside it: that LinkedList's O(1) front insert actually");
        System.out.println("  beats ArrayList's O(n) shift in practice.  System.arraycopy moves a");
        System.out.println("  contiguous block at memory-bandwidth speed, while allocating a node");
        System.out.println("  costs a heap allocation and a cache miss.  I expect ArrayList to win on");
        System.out.println("  small playlists despite being asymptotically worse, with LinkedList");
        System.out.println("  overtaking it only once n is large.  Finding that crossover is the");
        System.out.println("  point of the experiment.");

        List<Bench.Result> all = Bench.list();

        /* ---------------- the five operations ---------------- */
        for (int n : SIZES) {
            System.out.println();
            System.out.printf("2.  PLAYLIST SIZE n = %s%n", Bench.fmt(n));
            System.out.println("-".repeat(118));
            Bench.tableHead();

            final int adds = 20_000;
            final int frontOps = Math.min(5_000, Math.max(200, 200_000 / Math.max(1, n / 1_000)));
            final int probes = 20_000;

            // ---- add at end ----
            all.add(show(Bench.measure("ArrayList", "add at end", n, adds, () -> {
                List<Song> l = new ArrayList<>();
                for (int i = 0; i < adds; i++) l.add(new Song(i, "t"));
                sink(l.size());
            })));
            all.add(show(Bench.measure("LinkedList", "add at end", n, adds, () -> {
                List<Song> l = new LinkedList<>();
                for (int i = 0; i < adds; i++) l.add(new Song(i, "t"));
                sink(l.size());
            })));
            all.add(show(Bench.measure("ArrayDeque", "add at end", n, adds, () -> {
                Deque<Song> d = new ArrayDeque<>();
                for (int i = 0; i < adds; i++) d.addLast(new Song(i, "t"));
                sink(d.size());
            })));

            // ---- insert at front ----
            final List<Song> alFront = seedList(new ArrayList<>(), n);
            final List<Song> llFront = seedList(new LinkedList<>(), n);
            final Deque<Song> adFront = seedDeque(n);
            final int fo = frontOps;
            all.add(show(Bench.measure("ArrayList", "insert at front", n, fo, () -> {
                for (int i = 0; i < fo; i++) alFront.add(0, new Song(i, "f"));
                for (int i = 0; i < fo; i++) alFront.remove(0);
            })));
            all.add(show(Bench.measure("LinkedList", "insert at front", n, fo, () -> {
                for (int i = 0; i < fo; i++) llFront.add(0, new Song(i, "f"));
                for (int i = 0; i < fo; i++) llFront.remove(0);
            })));
            all.add(show(Bench.measure("ArrayDeque", "insert at front", n, fo, () -> {
                for (int i = 0; i < fo; i++) adFront.addFirst(new Song(i, "f"));
                for (int i = 0; i < fo; i++) adFront.pollFirst();
            })));

            // ---- insert in the middle ----
            final List<Song> alMid = seedList(new ArrayList<>(), n);
            final List<Song> llMid = seedList(new LinkedList<>(), n);
            all.add(show(Bench.measure("ArrayList", "insert in middle", n, fo, () -> {
                for (int i = 0; i < fo; i++) alMid.add(alMid.size() / 2, new Song(i, "m"));
                for (int i = 0; i < fo; i++) alMid.remove(alMid.size() / 2);
            })));
            all.add(show(Bench.measure("LinkedList", "insert in middle", n, fo, () -> {
                for (int i = 0; i < fo; i++) llMid.add(llMid.size() / 2, new Song(i, "m"));
                for (int i = 0; i < fo; i++) llMid.remove(llMid.size() / 2);
            })));

            // ---- get k-th ----
            final List<Song> alGet = seedList(new ArrayList<>(), n);
            final List<Song> llGet = seedList(new LinkedList<>(), n);
            final int[] keys = new int[probes];
            for (int i = 0; i < probes; i++) keys[i] = (int) ((long) i * 7919 % n);
            all.add(show(Bench.measure("ArrayList", "get k-th", n, probes, () -> {
                for (int k : keys) sink(alGet.get(k).id());
            })));
            // LinkedList get() is O(n); at the largest sizes this is deliberately
            // left to run so the cost is visible rather than quietly skipped.
            final int llProbes = n <= 10_000 ? probes : 200;
            all.add(show(Bench.measure("LinkedList", "get k-th", n, llProbes, () -> {
                for (int i = 0; i < llProbes; i++) sink(llGet.get(keys[i]).id());
            })));

            // ---- iterate ----
            all.add(show(Bench.measure("ArrayList", "iterate all", n, n, () -> {
                long s = 0; for (Song x : alGet) s += x.id(); sink(s);
            })));
            all.add(show(Bench.measure("LinkedList", "iterate all", n, n, () -> {
                long s = 0; for (Song x : llGet) s += x.id(); sink(s);
            })));
            all.add(show(Bench.measure("ArrayDeque", "iterate all", n, n, () -> {
                long s = 0; for (Song x : adFront) s += x.id(); sink(s);
            })));
        }

        Bench.writeCsv("Q2", all);
        System.out.println();
        System.out.println("Run Q2/UpNextQueue.java for the changed condition (the Up Next queue),");
        System.out.println("and Q2/ManualArrayList.java for the plain-array alternative.");
    }

    static List<Song> seedList(List<Song> l, int n) {
        for (int i = 0; i < n; i++) l.add(new Song(i, "s" + i));
        return l;
    }
    static Deque<Song> seedDeque(int n) {
        Deque<Song> d = new ArrayDeque<>();
        for (int i = 0; i < n; i++) d.addLast(new Song(i, "s" + i));
        return d;
    }
    static Bench.Result show(Bench.Result r) { Bench.row(r); return r; }
    static long SINK;
    static void sink(long v) { SINK += v; }
}
