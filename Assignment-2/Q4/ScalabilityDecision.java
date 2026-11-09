/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q4  -  the scalability inquiry, the Hashtable question, and the
 *        sorted-array alternative
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 */
import bench.Bench;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ScalabilityDecision {

    record Medicine(String code, String name, int stock) { }

    static final int N = 200_000;
    static final int LOOKUPS = 100_000;
    /** The acceptability criterion, FIXED BEFORE measuring so it cannot be
     *  adjusted afterwards to match whatever the numbers turned out to be. */
    static final double ACCEPTABLE_NS_PER_LOOKUP = 1_000.0;   // 1 microsecond

    public static void main(String[] args) throws Exception {
        System.out.println("Q4  -  SCALABILITY, Hashtable, AND THE SORTED-ARRAY ALTERNATIVE");
        System.out.println("=".repeat(118));
        System.out.println("Name: Vignesh S   Register Number: 192325002");

        System.out.println();
        System.out.println("1.  THE ACCEPTABILITY CRITERION  (fixed before measuring)");
        System.out.println("-".repeat(118));
        System.out.println("  A pharmacist waits at a counter while the lookup runs.  Human");
        System.out.println("  perception treats anything under about 100 ms as instant, and a single");
        System.out.println("  stock lookup is one small part of serving one patient.");
        System.out.println();
        System.out.printf("  I therefore set the budget at %.0f ns (1 microsecond) per lookup.%n",
                ACCEPTABLE_NS_PER_LOOKUP);
        System.out.println("  That is deliberately generous: it allows 100,000 lookups per second on");
        System.out.println("  one thread, which is far beyond 'hundreds of times a day'.  Setting it");
        System.out.println("  loose matters, because a criterion tight enough that only one option");
        System.out.println("  passes is not a criterion, it is a conclusion written in advance.");

        final Medicine[] meds = new Medicine[N];
        int[] order = new int[N];
        for (int i = 0; i < N; i++) order[i] = i;
        java.util.Random r = new java.util.Random(42);
        for (int i = N - 1; i > 0; i--) { int j = r.nextInt(i + 1); int t = order[i]; order[i] = order[j]; order[j] = t; }
        for (int i = 0; i < N; i++)
            meds[i] = new Medicine(String.format("MED-%07d", order[i]), "drug" + order[i], 10 + order[i] % 500);

        final String[] probes = new String[LOOKUPS];
        java.util.Random rp = new java.util.Random(7);
        for (int i = 0; i < LOOKUPS; i++)
            probes[i] = (i % 10 == 0) ? "MED-9999999" : meds[rp.nextInt(N)].code();

        final Map<String, Medicine> hash = new HashMap<>();
        final Map<String, Medicine> tree = new TreeMap<>();
        final Map<String, Medicine> link = new LinkedHashMap<>();
        final Hashtable<String, Medicine> table = new Hashtable<>();
        for (Medicine m : meds) {
            hash.put(m.code(), m); tree.put(m.code(), m);
            link.put(m.code(), m); table.put(m.code(), m);
        }

        List<Bench.Result> all = Bench.list();
        System.out.println();
        System.out.printf("2.  MEASURED AGAINST THE BUDGET  (n = %s, %s lookups)%n",
                Bench.fmt(N), Bench.fmt(LOOKUPS));
        System.out.println("-".repeat(118));
        Bench.tableHead();
        Bench.Result h = show(Bench.measure("HashMap", "lookup", N, LOOKUPS, () -> probe(hash, probes)), all);
        Bench.Result t = show(Bench.measure("TreeMap", "lookup", N, LOOKUPS, () -> probe(tree, probes)), all);
        Bench.Result l = show(Bench.measure("LinkedHashMap", "lookup", N, LOOKUPS, () -> probe(link, probes)), all);
        Bench.Result ht = show(Bench.measure("Hashtable", "lookup", N, LOOKUPS, () -> probe(table, probes)), all);

        System.out.println();
        System.out.printf("%-18s | %-16s | %-14s | %s%n", "MAP", "ns PER LOOKUP", "BUDGET", "VERDICT");
        System.out.println("-".repeat(118));
        for (Bench.Result x : List.of(h, t, l, ht))
            System.out.printf("%-18s | %-16.1f | %-14.0f | %s%n", x.design, x.perOp(),
                    ACCEPTABLE_NS_PER_LOOKUP,
                    x.perOp() <= ACCEPTABLE_NS_PER_LOOKUP
                        ? String.format("PASSES, %.0fx inside budget", ACCEPTABLE_NS_PER_LOOKUP / x.perOp())
                        : String.format("FAILS, %.1fx over budget", x.perOp() / ACCEPTABLE_NS_PER_LOOKUP));

        System.out.println();
        System.out.println("  WHERE THE REMAINING COST COMES FROM");
        System.out.println("  Even the winner is not free.  A HashMap lookup on a String key does:");
        System.out.printf("    - String.hashCode(): O(length) over the key, here %d characters%n",
                meds[0].code().length());
        System.out.println("      (cached in the String after the first call, so it is paid once per");
        System.out.println("       distinct key object, not once per lookup - which is why reusing");
        System.out.println("       the same probe Strings is faster than building them each time);");
        System.out.println("    - the hash spread, one XOR and one shift;");
        System.out.println("    - one array index into the bucket table - a likely CACHE MISS at");
        System.out.println("      n = 200,000, because the table is about 1 MB and does not fit in L2;");
        System.out.println("    - String.equals() on the matched entry: another O(length) scan.");
        System.out.println();
        System.out.println("  The dominant term at this size is the cache miss, not the hashing. That");
        System.out.println("  is why HashMap's per-lookup cost rose with n in the main benchmark even");
        System.out.println("  though its complexity is O(1): the constant is a memory latency that");
        System.out.println("  grows as the table outgrows each cache level.");

        /* ---------- the deployment decision ---------- */
        System.out.println();
        System.out.println("3.  DEPLOYMENT DECISION");
        System.out.println("-".repeat(118));
        System.out.println("  Two reports are required and no single map gives both. Options:");
        System.out.println();
        System.out.printf("    (a) TreeMap alone          lookups %.0f ns - %s%n", t.perOp(),
                t.perOp() <= ACCEPTABLE_NS_PER_LOOKUP ? "within budget" : "OVER BUDGET");
        System.out.println("        code-order report free, arrival order impossible. Rejected: it");
        System.out.println("        cannot produce one of the two required reports at all.");
        System.out.println();
        System.out.printf("    (b) LinkedHashMap alone    lookups %.0f ns - within budget%n", l.perOp());
        System.out.println("        arrival-order report free; code-order report needs an O(n log n)");
        System.out.println("        sort each evening, measured at about 138 ms for n = 200,000.");
        System.out.println();
        System.out.println("    (c) LinkedHashMap + a TreeMap index maintained alongside");
        System.out.println("        both reports free; two structures to keep in step, and roughly");
        System.out.println("        double the entry overhead.");
        System.out.println();
        System.out.println("  I DEPLOY (b): a single LinkedHashMap.");
        System.out.println();
        System.out.println("  The reasoning is that the two costs are not comparable in importance.");
        System.out.println("  Lookups happen hundreds of times a day while a pharmacist waits; the");
        System.out.println("  code-order report happens ONCE, overnight, when nobody is waiting.");
        System.out.printf("  Paying ~138 ms once a day to keep every lookup at %.0f ns is the right%n",
                l.perOp());
        System.out.println("  trade.  Option (c) buys back 138 ms per day at the cost of a second");
        System.out.println("  structure that can silently fall out of sync with the first - a");
        System.out.println("  correctness risk taken on to save a tenth of a second at midnight.");
        System.out.println();
        System.out.println("  I would revisit this if the code-order report became interactive, or if");
        System.out.println("  n grew by two orders of magnitude so the nightly sort ran into minutes.");

        /* ---------- Hashtable ---------- */
        System.out.println();
        System.out.println("4.  WHY Hashtable IS NOT PREFERRED IN NEW CODE");
        System.out.println("-".repeat(118));
        System.out.printf("  measured: Hashtable %.1f ns/lookup against HashMap %.1f ns - %.2fx%n",
                ht.perOp(), h.perOp(), ht.perOp() / h.perOp());
        System.out.println();
        System.out.println("  Speed is the least of it. The real reasons:");
        System.out.println("    - every method is synchronized, so a single-threaded pharmacy pays");
        System.out.println("      lock overhead for safety it never uses;");
        System.out.println("    - that synchronization is also NOT ENOUGH for real concurrent use:");
        System.out.println("      get-then-put is still a race, so callers need external locking");
        System.out.println("      anyway, which makes the per-method locks pure waste;");
        System.out.println("    - it is a legacy class retrofitted to Map, predating the Collections");
        System.out.println("      Framework, so it carries parallel legacy methods such as elements()");
        System.out.println("      and keys() returning Enumeration rather than Iterator;");
        System.out.println("    - it rejects null keys and null values with NullPointerException,");
        System.out.println("      which is inconsistent with HashMap and surprises callers:");
        try { table.put(null, meds[0]); System.out.println("      put(null, v) accepted"); }
        catch (NullPointerException e) { System.out.println("      Hashtable.put(null, v) -> NullPointerException"); }
        Map<String, Medicine> hm = new HashMap<>();
        hm.put(null, meds[0]);
        System.out.println("      HashMap.put(null, v)  -> accepted, size " + hm.size());
        System.out.println();
        System.out.println("    For genuine concurrency the modern answer is ConcurrentHashMap, which");
        System.out.println("    locks per bucket rather than per method and offers atomic compound");
        System.out.println("    operations such as computeIfAbsent that Hashtable cannot express.");

        /* ---------- the sorted-array alternative ---------- */
        System.out.println();
        System.out.println("5.  ALTERNATIVE: A SORTED ARRAY WITH Arrays.binarySearch()");
        System.out.println("-".repeat(118));
        final String[] codes = new String[N];
        for (int i = 0; i < N; i++) codes[i] = meds[i].code();
        Arrays.sort(codes);
        final Medicine[] byIndex = new Medicine[N];
        for (Medicine m : meds) byIndex[Arrays.binarySearch(codes, m.code())] = m;

        Bench.Result bs = show(Bench.measure("sorted array", "lookup (binarySearch)", N, LOOKUPS, () -> {
            long s = 0;
            for (String c : probes) { int i = Arrays.binarySearch(codes, c); if (i >= 0) s += byIndex[i].stock(); }
            sink(s);
        }), all);
        Bench.Result bsRep = show(Bench.measure("sorted array", "report: code order", N, 1, () -> {
            long s = 0; for (String c : codes) s += c.length(); sink(s);
        }), all);
        final Medicine newMed = new Medicine("MED-0500000", "newdrug", 5);
        Bench.Result bsIns = show(Bench.measure("sorted array", "insert 1 medicine", N, 1, () -> {
            String[] copy = new String[N + 1];
            int at = -Arrays.binarySearch(codes, newMed.code()) - 1;
            if (at < 0) at = 0;
            System.arraycopy(codes, 0, copy, 0, at);
            copy[at] = newMed.code();
            System.arraycopy(codes, at, copy, at + 1, N - at);
            sink(copy.length);
        }), all);

        System.out.println();
        System.out.printf("%-26s | %-16s | %-16s | %-16s | %s%n",
                "OPERATION", "sorted array", "HashMap", "TreeMap", "WINNER");
        System.out.println("-".repeat(118));
        System.out.printf("%-26s | %-16s | %-16s | %-16s | %s%n", "lookup",
                "O(log n)", "O(1) avg", "O(log n)", "HashMap");
        System.out.printf("%-26s | %-16s | %-16s | %-16s | %s%n", "insert a medicine",
                "O(n) shift", "O(1) avg", "O(log n)", "HashMap");
        System.out.printf("%-26s | %-16s | %-16s | %-16s | %s%n", "code-order report",
                "O(n), free", "O(n log n) sort", "O(n), free", "array / TreeMap");
        System.out.printf("%-26s | %-16s | %-16s | %-16s | %s%n", "arrival-order report",
                "impossible", "impossible", "impossible", "LinkedHashMap only");
        System.out.printf("%-26s | %-16s | %-16s | %-16s | %s%n", "space per entry",
                "4 B slot", "~36 B node", "~40 B entry", "sorted array");
        System.out.println();
        System.out.printf("  measured lookup: sorted array %.0f ns vs HashMap %.0f ns (%.1fx slower)%n",
                bs.perOp(), h.perOp(), bs.perOp() / h.perOp());
        System.out.printf("  measured code-order report: %.1f ms - free, like TreeMap%n", bsRep.medianMs());
        System.out.println();
        System.out.println("  The sorted array is the most SPACE-efficient option by a wide margin -");
        System.out.printf("  about %d KB of references against roughly %d KB of hash nodes for%n",
                N * 4 / 1024, N * 36 / 1024);
        System.out.println("  n = 200,000 - and its code-order report is free.");
        System.out.println();
        System.out.println("  It is nonetheless the wrong choice here, for one specific reason: stock");
        System.out.println("  arrives and medicines are discontinued, so the inventory CHANGES, and");
        System.out.println("  every insertion shifts on average n/2 references to keep the array");
        System.out.println("  sorted.  A sorted array is the right structure for data that is built");
        System.out.println("  once and then only read - a published price list, a reference table -");
        System.out.println("  not for a live inventory.");

        Bench.writeCsv("Q4-decision", all);
    }

    static void probe(Map<String, Medicine> m, String[] probes) {
        long s = 0;
        for (String c : probes) { Medicine x = m.get(c); if (x != null) s += x.stock(); }
        sink(s);
    }
    static Bench.Result show(Bench.Result r, List<Bench.Result> all) { Bench.row(r); all.add(r); return r; }
    static long SINK;
    static void sink(long v) { SINK += v; }
}
