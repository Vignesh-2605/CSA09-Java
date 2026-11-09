/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q5  -  Alternative Concept and Complexity Analysis
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * "If Team implemented Comparable<Team> instead, or if the list were sorted
 *  three times by stable sorts (name, then goal difference, then points), what
 *  would the solution be?"
 */
import bench.Bench;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class SortingAlternatives {

    /* ---- B: Team implements Comparable, rank baked into the type ---- */
    record ComparableTeam(String name, int points, int goalDiff)
            implements Comparable<ComparableTeam> {
        @Override public int compareTo(ComparableTeam o) {
            int c = Integer.compare(o.points, points);          // high first
            if (c != 0) return c;
            c = Integer.compare(o.goalDiff, goalDiff);          // high first
            if (c != 0) return c;
            return name.compareTo(o.name);                      // A-Z
        }
        @Override public String toString() { return name + "(" + points + "," + goalDiff + ")"; }
    }

    record Team(String name, int points, int goalDiff) {
        @Override public String toString() { return name + "(" + points + "," + goalDiff + ")"; }
    }

    static final Comparator<Team> CHAIN =
            Comparator.comparingInt(Team::points).reversed()
                      .thenComparing(Comparator.comparingInt(Team::goalDiff).reversed())
                      .thenComparing(Team::name);

    /* ---- C: three successive STABLE sorts, least significant key first ---- */
    static List<Team> byThreeStableSorts(List<Team> input) {
        List<Team> l = new ArrayList<>(input);
        l.sort(Comparator.comparing(Team::name));                          // 1. least significant
        l.sort(Comparator.comparingInt(Team::goalDiff).reversed());        // 2.
        l.sort(Comparator.comparingInt(Team::points).reversed());          // 3. most significant
        return l;
    }

    public static void main(String[] args) {
        System.out.println("Q5  -  ALTERNATIVE RANKING IMPLEMENTATIONS");
        System.out.println("=".repeat(118));
        System.out.println("Name: Vignesh S   Register Number: 192325002");

        System.out.println();
        System.out.println("1.  DO ALL THREE PRODUCE THE SAME RANKING?");
        System.out.println("-".repeat(118));
        List<Team> teams = List.of(
                new Team("Panthers", 9, 4), new Team("Eagles", 9, 6), new Team("Cobras", 6, 1),
                new Team("Bisons", 9, 4),   new Team("Dingoes", 6, 1), new Team("Zebras", 12, 0),
                new Team("Yaks", 12, 0),    new Team("Antelopes", 6, 1));

        List<Team> a = new ArrayList<>(teams); a.sort(CHAIN);
        List<Team> c = byThreeStableSorts(teams);
        List<ComparableTeam> bs = new ArrayList<>();
        for (Team t : teams) bs.add(new ComparableTeam(t.name(), t.points(), t.goalDiff()));
        bs.sort(null);                                   // uses compareTo

        System.out.println("  A  Comparator chain     : " + a);
        System.out.println("  B  Comparable           : " + bs);
        System.out.println("  C  three stable sorts   : " + c);
        System.out.printf("%n  A == C : %s%n", a.toString().equals(c.toString()));
        System.out.printf("  A == B : %s%n", a.toString().equals(bs.toString()));
        System.out.println();
        System.out.println("  WHY C WORKS AT ALL: because List.sort is STABLE - equal elements keep");
        System.out.println("  their relative order.  Sorting by the LEAST significant key first and");
        System.out.println("  the MOST significant last means each later sort preserves the ordering");
        System.out.println("  the earlier ones established among its own ties.  Reverse the order of");
        System.out.println("  the three calls and the result is simply wrong - the technique depends");
        System.out.println("  entirely on a property of the sort, which makes it fragile: a future");
        System.out.println("  change to an unstable sort would break it silently.");

        System.out.println();
        System.out.println("2.  MEASURED COST  (the same data, three implementations)");
        System.out.println("-".repeat(118));
        int[] sizes = { 1_000, 50_000, 500_000 };
        List<Bench.Result> all = Bench.list();
        Bench.tableHead();
        for (int n : sizes) {
            Random r = new Random(11);
            final List<Team> data = new ArrayList<>(n);
            final List<ComparableTeam> cdata = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                String nm = "T" + i;
                int p = r.nextInt(40), g = r.nextInt(60) - 30;
                data.add(new Team(nm, p, g));
                cdata.add(new ComparableTeam(nm, p, g));
            }
            all.add(show(Bench.measure("A Comparator chain", "rank", n, 1, () -> {
                List<Team> l = new ArrayList<>(data); l.sort(CHAIN); sink(l.size());
            })));
            all.add(show(Bench.measure("B Comparable", "rank", n, 1, () -> {
                List<ComparableTeam> l = new ArrayList<>(cdata); l.sort(null); sink(l.size());
            })));
            all.add(show(Bench.measure("C three stable sorts", "rank", n, 1, () -> {
                sink(byThreeStableSorts(data).size());
            })));
        }

        System.out.println();
        System.out.println("3.  COMPLEXITY AND SPACE");
        System.out.println("-".repeat(118));
        System.out.printf("%-26s | %-20s | %-24s | %s%n",
                "APPROACH", "SORTS PERFORMED", "TIME", "EXTRA SPACE");
        System.out.println("-".repeat(118));
        System.out.printf("%-26s | %-20s | %-24s | %s%n", "A Comparator chain",
                "1", "O(n log n)", "O(n) for the copy + TimSort run stack");
        System.out.printf("%-26s | %-20s | %-24s | %s%n", "B Comparable",
                "1", "O(n log n)", "same as A");
        System.out.printf("%-26s | %-20s | %-24s | %s%n", "C three stable sorts",
                "3", "3 x O(n log n)", "same, but allocated 3 times");
        System.out.println();
        System.out.println("  C is the same complexity class as A and B - a constant multiple of");
        System.out.println("  three does not change O(n log n) - but it does three times the work,");
        System.out.println("  and the measurements above show that constant clearly.  A and B do the");
        System.out.println("  SAME number of comparisons; they differ only in where the comparison");
        System.out.println("  lives.");

        System.out.println();
        System.out.println("4.  WHICH IS EASIEST TO REUSE FOR PLAYERS AND COLLEGES?");
        System.out.println("-".repeat(118));
        System.out.printf("%-26s | %-30s | %-26s | %s%n",
                "APPROACH", "SECOND ORDERING OF Team", "NEW TYPE (Player)", "VERDICT");
        System.out.println("-".repeat(118));
        System.out.printf("%-26s | %-30s | %-26s | %s%n", "A Comparator chain",
                "pass a different Comparator", "same Leaderboard<Player>", "REUSABLE");
        System.out.printf("%-26s | %-30s | %-26s | %s%n", "B Comparable",
                "impossible without a Comparator", "edit Player to implement it", "LEAST reusable");
        System.out.printf("%-26s | %-30s | %-26s | %s%n", "C three stable sorts",
                "rewrite the sequence of sorts", "rewrite again per type", "reusable, but verbose");
        System.out.println();
        System.out.println("  A is the answer.  B bakes one ordering into the type, so the programme");
        System.out.println("  listing (by name) and the statistics page (by goal difference) each");
        System.out.println("  need a Comparator anyway - at which point the Comparable adds a fourth");
        System.out.println("  way of ordering the same type and invites the two to disagree.");
        System.out.println();
        System.out.println("  B remains worth having for ONE purpose: a natural order lets Team be");
        System.out.println("  put in a TreeSet or sorted with sort(null) without supplying anything.");
        System.out.println("  The usual compromise is to implement Comparable with the single most");
        System.out.println("  obvious order and express every other ordering as a Comparator - and");
        System.out.println("  for Team the 'most obvious order' is the competition ranking, which is");
        System.out.println("  exactly the one that changes between competitions. So for THIS type,");
        System.out.println("  Comparator alone is the cleaner answer.");

        Bench.writeCsv("Q5", all);
    }

    static Bench.Result show(Bench.Result r) { Bench.row(r); return r; }
    static long SINK;
    static void sink(long v) { SINK += v; }
}
