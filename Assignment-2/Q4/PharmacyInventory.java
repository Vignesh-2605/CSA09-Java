/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q4  -  Choosing a Map for a Pharmacy Inventory
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * The pharmacy needs THREE things from one structure:
 *   1. lookup stock by medicine code, hundreds of times a day;
 *   2. an evening report of all medicines in CODE ORDER;
 *   3. a list of medicines in the order they were ADDED.
 *
 * No single map gives all three, which is what makes this a design question
 * rather than a lookup of a complexity table.
 */
import bench.Bench;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PharmacyInventory {

    record Medicine(String code, String name, int stock) { }

    static final int[] SIZES = { 1_000, 20_000, 200_000 };
    static final int LOOKUPS = 100_000;

    public static void main(String[] args) {
        System.out.println("Q4  -  HashMap, TreeMap AND LinkedHashMap FOR A PHARMACY INVENTORY");
        System.out.println("=".repeat(118));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.printf("Harness: %d warm-up runs discarded, %d measured repetitions, MEDIAN reported.%n",
                Bench.WARMUP, Bench.REPS);

        /* ---------- prediction ---------- */
        System.out.println();
        System.out.println("1.  PREDICTION, DETERMINED BEFORE IMPLEMENTATION");
        System.out.println("-".repeat(118));
        System.out.printf("%-18s | %-14s | %-14s | %-22s | %s%n",
                "MAP", "put", "get", "ITERATION ORDER", "WHICH REPORT IT GIVES FREE");
        System.out.println("-".repeat(118));
        String[][] p = {
            {"HashMap",       "O(1) avg", "O(1) avg", "unspecified",       "neither"},
            {"TreeMap",       "O(log n)", "O(log n)", "sorted by key",     "the CODE-ORDER report"},
            {"LinkedHashMap", "O(1) avg", "O(1) avg", "insertion order",   "the ARRIVAL-ORDER report"},
        };
        for (String[] r : p) System.out.printf("%-18s | %-14s | %-14s | %-22s | %s%n", r[0], r[1], r[2], r[3], r[4]);
        System.out.println();
        System.out.println("  EXTRA WORK IF THE WRONG MAP IS CHOSEN FOR A REPORT:");
        System.out.println("    HashMap       -> code-order report needs a sort of all keys: O(n log n)");
        System.out.println("                     EVERY evening, plus an O(n) copy of the key list.");
        System.out.println("    HashMap       -> arrival-order report is IMPOSSIBLE to recover at all:");
        System.out.println("                     the information was never stored.  This is the case");
        System.out.println("                     worth noticing - the other mismatches cost time, this");
        System.out.println("                     one costs correctness.");
        System.out.println("    TreeMap       -> arrival order likewise unrecoverable.");
        System.out.println("    LinkedHashMap -> code-order report needs the same O(n log n) sort.");
        System.out.println();
        System.out.println("  PREDICTED LOOKUP COST at n = 200,000: TreeMap must compare keys down");
        System.out.println("  about log2(200,000) = 17-18 levels, each a String comparison, against");
        System.out.println("  one hash and one bucket probe for the other two.  I expect TreeMap to be");
        System.out.println("  several times slower for lookups, and the gap to widen with n.");

        List<Bench.Result> all = Bench.list();

        for (int n : SIZES) {
            final Medicine[] meds = generate(n);
            final String[] probes = probes(meds, LOOKUPS);

            System.out.println();
            System.out.printf("2.  n = %s MEDICINES%n", Bench.fmt(n));
            System.out.println("-".repeat(118));
            Bench.tableHead();

            // ---- build ----
            all.add(show(Bench.measure("HashMap", "build", n, n, () -> {
                Map<String, Medicine> m = new HashMap<>();
                for (Medicine x : meds) m.put(x.code(), x);
                sink(m.size());
            })));
            all.add(show(Bench.measure("TreeMap", "build", n, n, () -> {
                Map<String, Medicine> m = new TreeMap<>();
                for (Medicine x : meds) m.put(x.code(), x);
                sink(m.size());
            })));
            all.add(show(Bench.measure("LinkedHashMap", "build", n, n, () -> {
                Map<String, Medicine> m = new LinkedHashMap<>();
                for (Medicine x : meds) m.put(x.code(), x);
                sink(m.size());
            })));

            final Map<String, Medicine> hash = new HashMap<>();
            final Map<String, Medicine> tree = new TreeMap<>();
            final Map<String, Medicine> link = new LinkedHashMap<>();
            for (Medicine x : meds) { hash.put(x.code(), x); tree.put(x.code(), x); link.put(x.code(), x); }

            // ---- 100,000 lookups ----
            all.add(show(Bench.measure("HashMap", LOOKUPS + " lookups", n, LOOKUPS, () -> {
                long s = 0; for (String c : probes) { Medicine m = hash.get(c); if (m != null) s += m.stock(); } sink(s);
            })));
            all.add(show(Bench.measure("TreeMap", LOOKUPS + " lookups", n, LOOKUPS, () -> {
                long s = 0; for (String c : probes) { Medicine m = tree.get(c); if (m != null) s += m.stock(); } sink(s);
            })));
            all.add(show(Bench.measure("LinkedHashMap", LOOKUPS + " lookups", n, LOOKUPS, () -> {
                long s = 0; for (String c : probes) { Medicine m = link.get(c); if (m != null) s += m.stock(); } sink(s);
            })));

            // ---- report A: code order ----
            all.add(show(Bench.measure("HashMap", "report: code order", n, 1, () -> {
                List<String> keys = new ArrayList<>(hash.keySet());   // O(n) copy
                Collections.sort(keys);                                // O(n log n) EVERY evening
                sink(keys.size());
            })));
            all.add(show(Bench.measure("TreeMap", "report: code order", n, 1, () -> {
                long s = 0; for (String k : tree.keySet()) s += k.length(); sink(s);   // already sorted
            })));
            all.add(show(Bench.measure("LinkedHashMap", "report: code order", n, 1, () -> {
                List<String> keys = new ArrayList<>(link.keySet());
                Collections.sort(keys);
                sink(keys.size());
            })));

            // ---- report B: arrival order ----
            all.add(show(Bench.measure("LinkedHashMap", "report: arrival order", n, 1, () -> {
                long s = 0; for (String k : link.keySet()) s += k.length(); sink(s);
            })));

            /* ---- does the output ORDER actually match the requirement? ---- */
            System.out.println();
            System.out.printf("  %-18s | %-24s | %-24s | %s%n",
                    "MAP", "CODE-ORDER REPORT", "ARRIVAL-ORDER REPORT", "FIRST 3 KEYS AS ITERATED");
            System.out.println("  " + "-".repeat(114));
            System.out.printf("  %-18s | %-24s | %-24s | %s%n", "HashMap",
                    sortedCorrect(hash) ? "correct by luck" : "WRONG without a sort",
                    "IMPOSSIBLE - not stored", first3(hash));
            System.out.printf("  %-18s | %-24s | %-24s | %s%n", "TreeMap",
                    sortedCorrect(tree) ? "correct, free" : "wrong",
                    "IMPOSSIBLE - not stored", first3(tree));
            System.out.printf("  %-18s | %-24s | %-24s | %s%n", "LinkedHashMap",
                    sortedCorrect(link) ? "correct by luck" : "WRONG without a sort",
                    arrivalCorrect(link, meds) ? "correct, free" : "wrong", first3(link));
        }

        Bench.writeCsv("Q4", all);
        System.out.println();
        System.out.println("Run Q4/ScalabilityDecision.java for the acceptability criterion, the");
        System.out.println("Hashtable question and the sorted-array alternative.");
    }

    static Medicine[] generate(int n) {
        Medicine[] m = new Medicine[n];
        // codes are generated in a SHUFFLED order so that arrival order and code
        // order genuinely differ - otherwise the experiment would be rigged.
        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;
        java.util.Random r = new java.util.Random(42);
        for (int i = n - 1; i > 0; i--) { int j = r.nextInt(i + 1); int t = order[i]; order[i] = order[j]; order[j] = t; }
        for (int i = 0; i < n; i++)
            m[i] = new Medicine(String.format("MED-%07d", order[i]), "drug" + order[i], 10 + order[i] % 500);
        return m;
    }

    static String[] probes(Medicine[] meds, int count) {
        String[] p = new String[count];
        java.util.Random r = new java.util.Random(7);
        for (int i = 0; i < count; i++) {
            // 10% deliberate misses, so the lookup path includes failures too
            p[i] = (i % 10 == 0) ? "MED-9999999" : meds[r.nextInt(meds.length)].code();
        }
        return p;
    }

    static boolean sortedCorrect(Map<String, Medicine> m) {
        String prev = null;
        for (String k : m.keySet()) { if (prev != null && prev.compareTo(k) > 0) return false; prev = k; }
        return true;
    }
    static boolean arrivalCorrect(Map<String, Medicine> m, Medicine[] meds) {
        int i = 0;
        for (String k : m.keySet()) { if (!k.equals(meds[i++].code())) return false; }
        return true;
    }
    static String first3(Map<String, Medicine> m) {
        StringBuilder sb = new StringBuilder(); int i = 0;
        for (String k : m.keySet()) { if (i++ == 3) break; sb.append(k).append(' '); }
        return sb.toString().trim();
    }

    static Bench.Result show(Bench.Result r) { Bench.row(r); return r; }
    static long SINK;
    static void sink(long v) { SINK += v; }
}
