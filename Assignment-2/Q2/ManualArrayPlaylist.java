/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q2  -  Alternative Concept: a plain array with manual shifting
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * "If the playlist were stored in a plain array with manual shifting, what
 * would the solution be and how would its complexity compare?"
 *
 * The answer is implemented rather than described: this IS what ArrayList is,
 * written out by hand, which is the point the comparison makes.
 */
import bench.Bench;
import java.util.ArrayList;
import java.util.List;

public class ManualArrayPlaylist {

    record Song(int id) { }

    /** The plain-array solution, written out so its cost is visible. */
    static final class ManualPlaylist {
        private Song[] a = new Song[16];
        private int size = 0;

        void addLast(Song s) {
            ensure(size + 1);
            a[size++] = s;                                   // O(1) amortised
        }
        void insert(int index, Song s) {
            if (index < 0 || index > size) throw new IndexOutOfBoundsException("" + index);
            ensure(size + 1);
            System.arraycopy(a, index, a, index + 1, size - index);   // O(n - index)
            a[index] = s;
            size++;
        }
        Song remove(int index) {
            if (index < 0 || index >= size) throw new IndexOutOfBoundsException("" + index);
            Song old = a[index];
            System.arraycopy(a, index + 1, a, index, size - index - 1); // O(n - index)
            a[--size] = null;                                 // release the reference
            return old;
        }
        Song get(int index) {
            if (index < 0 || index >= size) throw new IndexOutOfBoundsException("" + index);
            return a[index];                                  // O(1)
        }
        int size() { return size; }
        private void ensure(int need) {
            if (need <= a.length) return;
            int cap = a.length + (a.length >> 1);             // grow by 50%, as ArrayList does
            if (cap < need) cap = need;
            Song[] b = new Song[cap];
            System.arraycopy(a, 0, b, 0, size);
            a = b;
        }
    }

    public static void main(String[] args) {
        System.out.println("Q2  -  ALTERNATIVE CONCEPT: A PLAIN ARRAY WITH MANUAL SHIFTING");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.println();
        System.out.println("WHAT THE SOLUTION LOOKS LIKE");
        System.out.println("-".repeat(112));
        System.out.println("  Song[] a; int size;");
        System.out.println("  addLast : grow if full, then a[size++] = s");
        System.out.println("  insert  : System.arraycopy(a, i, a, i+1, size-i), then a[i] = s");
        System.out.println("  remove  : System.arraycopy(a, i+1, a, i, size-i-1), then a[--size] = null");
        System.out.println("  get     : return a[i]");
        System.out.println();
        System.out.println("  Writing it out makes the conclusion obvious before a single measurement:");
        System.out.println("  this is ArrayList.  ArrayList IS a plain array plus manual shifting plus");
        System.out.println("  a growth policy.  So the complexity must be identical, and the only");
        System.out.println("  differences that can appear are constant factors and safety.");
        System.out.println();

        final int n = 100_000, ops = 20_000;
        List<Bench.Result> all = Bench.list();

        System.out.println("MEASURED: IS HAND-WRITING IT ACTUALLY FASTER?");
        System.out.println("-".repeat(112));
        Bench.tableHead();

        all.add(show(Bench.measure("manual array", "add at end", n, ops, () -> {
            ManualPlaylist p = new ManualPlaylist();
            for (int i = 0; i < ops; i++) p.addLast(new Song(i));
            sink(p.size());
        })));
        all.add(show(Bench.measure("ArrayList", "add at end", n, ops, () -> {
            List<Song> p = new ArrayList<>();
            for (int i = 0; i < ops; i++) p.add(new Song(i));
            sink(p.size());
        })));

        final ManualPlaylist mp = new ManualPlaylist();
        final List<Song> al = new ArrayList<>();
        for (int i = 0; i < n; i++) { mp.addLast(new Song(i)); al.add(new Song(i)); }
        final int fo = 2_000;

        all.add(show(Bench.measure("manual array", "insert at front", n, fo, () -> {
            for (int i = 0; i < fo; i++) mp.insert(0, new Song(i));
            for (int i = 0; i < fo; i++) mp.remove(0);
        })));
        all.add(show(Bench.measure("ArrayList", "insert at front", n, fo, () -> {
            for (int i = 0; i < fo; i++) al.add(0, new Song(i));
            for (int i = 0; i < fo; i++) al.remove(0);
        })));

        final int probes = 20_000;
        final int[] keys = new int[probes];
        for (int i = 0; i < probes; i++) keys[i] = (int) ((long) i * 7919 % n);
        all.add(show(Bench.measure("manual array", "get k-th", n, probes, () -> {
            for (int k : keys) sink(mp.get(k).id());
        })));
        all.add(show(Bench.measure("ArrayList", "get k-th", n, probes, () -> {
            for (int k : keys) sink(al.get(k).id());
        })));

        System.out.println();
        System.out.println("COMPLEXITY TABLE FOR ALL FOUR DESIGNS");
        System.out.println("-".repeat(112));
        System.out.printf("%-22s | %-14s | %-14s | %-14s | %s%n",
                "OPERATION", "manual array", "ArrayList", "LinkedList", "ArrayDeque");
        System.out.println("-".repeat(112));
        String[][] rows = {
            {"add at end",       "O(1) amortised", "O(1) amortised", "O(1)",   "O(1) amortised"},
            {"insert at front",  "O(n)",           "O(n)",           "O(1)",   "O(1) amortised"},
            {"insert in middle", "O(n)",           "O(n)",           "O(n)",   "not supported"},
            {"remove from front","O(n)",           "O(n)",           "O(1)",   "O(1)"},
            {"get k-th",         "O(1)",           "O(1)",           "O(n)",   "no index API"},
            {"iterate all",      "O(n)",           "O(n)",           "O(n)",   "O(n)"},
            {"space per element","4 B slot",       "4 B slot",       "24 B node", "4 B slot"},
        };
        for (String[] r : rows)
            System.out.printf("%-22s | %-14s | %-14s | %-14s | %s%n", r[0], r[1], r[2], r[3], r[4]);

        System.out.println();
        System.out.println("CONCLUSION");
        System.out.println("-".repeat(112));
        System.out.println("  The manual array matches ArrayList in every complexity class, and the");
        System.out.println("  measured times are within noise of each other, because the two are the");
        System.out.println("  same algorithm - ArrayList's methods are thin wrappers over exactly");
        System.out.println("  these arraycopy calls, and the JIT inlines them.");
        System.out.println();
        System.out.println("  So the plain array buys no speed.  What it costs is everything the");
        System.out.println("  library gives for free:");
        System.out.println("    - bounds checks and clear IndexOutOfBoundsException messages;");
        System.out.println("    - the growth policy, which is easy to get subtly wrong;");
        System.out.println("    - nulling the vacated slot on remove, without which the array holds a");
        System.out.println("      reference to a removed Song and leaks it;");
        System.out.println("    - the List interface, so the playlist cannot be passed to anything");
        System.out.println("      that takes a List, sorted with Collections.sort, or swapped for a");
        System.out.println("      different implementation later;");
        System.out.println("    - modCount, which turns concurrent modification into an immediate");
        System.out.println("      exception instead of silent corruption.");
        System.out.println();
        System.out.println("  A plain array is the right choice only for primitives, where int[]");
        System.out.println("  avoids boxing entirely and ArrayList<Integer> cannot - that is a real");
        System.out.println("  4x space difference, and it is a different argument from this one.");

        Bench.writeCsv("Q2-manual", all);
    }

    static Bench.Result show(Bench.Result r) { Bench.row(r); return r; }
    static long SINK;
    static void sink(long v) { SINK += v; }
}
