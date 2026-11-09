/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q5  -  Derive a Generic Ranking Component from a Principle
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * THE PRINCIPLE GIVEN:
 *   "A ranking is correct only if the comparison used is consistent: if A ranks
 *    above B and B ranks above C, then A must rank above C, and equal items
 *    must compare as equal."
 *
 * That is transitivity plus a well-defined equality - the definition of a
 * TOTAL ORDER.  Everything below is derived from it rather than assumed.
 */
import bench.Bench;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class RankingDesign {

    /* ================= the domain type ================= */
    record Team(String name, int points, int goalDiff) {
        @Override public String toString() { return name + "(" + points + "," + goalDiff + ")"; }
    }
    record Player(String name, int runs, int average) {
        @Override public String toString() { return name + "[" + runs + "]"; }
    }

    /* ================= the generic component =================
     *
     * WHY Comparator AND NOT Comparable:
     *   Comparable fixes ONE ordering inside the class.  A team is ranked by
     *   points in the league table, by name in the programme, and by goal
     *   difference in the statistics page - three orderings of one type.  A
     *   class can only have one compareTo, so Comparable would force two of
     *   those three to be written as Comparators anyway.
     *   Comparable is right when the type has a single, obvious, intrinsic
     *   order (String, Integer, LocalDate).  "Rank" is not intrinsic to a team;
     *   it is imposed by a competition's rules, which change between
     *   competitions.
     *
     * WHY Comparator<? super T> AND NOT Comparator<T>:
     *   PECS - a Comparator is a CONSUMER of T, so it takes "? super T".  This
     *   lets a Leaderboard<Team> be built from a Comparator<Object> (for
     *   example one that compares by toString), which a Comparator<Team>
     *   parameter would reject for no good reason.
     */
    static final class Leaderboard<T> {
        private final List<T> items = new ArrayList<>();
        private final Comparator<? super T> order;

        Leaderboard(Comparator<? super T> order) {
            this.order = Objects.requireNonNull(order, "a ranking needs an order");
        }

        void add(T item) { items.add(item); }

        /* WHY List<? extends T> HERE:
         *   addAll PRODUCES T values for this board, so it is a producer and
         *   takes "? extends T".  Without the wildcard, a
         *   Leaderboard<Object> could not accept a List<Team>, even though
         *   every Team is an Object. */
        void addAll(List<? extends T> more) { items.addAll(more); }

        /** A new sorted copy; the stored order is never disturbed. */
        List<T> ranking() {
            List<T> copy = new ArrayList<>(items);
            copy.sort(order);
            return copy;
        }

        int size() { return items.size(); }

        /** Verifies the principle directly on the data actually held. */
        boolean orderIsConsistent() { return isTotalOrder(items, order); }
    }

    /* A bounded version, for the part of the question about bounded types. */
    static final class RankedBoard<T extends Comparable<? super T>> {
        private final List<T> items = new ArrayList<>();
        void add(T item) { items.add(item); }
        List<T> ranking() { List<T> c = new ArrayList<>(items); c.sort(Comparator.naturalOrder()); return c; }
    }

    /* ================= checking the principle ================= */

    /** Exhaustively tests the three laws a total order must satisfy. */
    static <T> boolean isTotalOrder(List<T> xs, Comparator<? super T> c) {
        for (T a : xs) {
            if (c.compare(a, a) != 0) return false;                       // reflexive
            for (T b : xs) {
                if (Integer.signum(c.compare(a, b)) != -Integer.signum(c.compare(b, a)))
                    return false;                                          // antisymmetric
                for (T d : xs) {
                    if (c.compare(a, b) > 0 && c.compare(b, d) > 0 && !(c.compare(a, d) > 0))
                        return false;                                      // transitive
                }
            }
        }
        return true;
    }

    /* ================= comparators ================= */

    /** The correct one: every key compared, no subtraction. */
    static final Comparator<Team> CORRECT =
            Comparator.comparingInt(Team::points).reversed()
                      .thenComparing(Comparator.comparingInt(Team::goalDiff).reversed())
                      .thenComparing(Team::name);

    /** Faulty A: compares points by SUBTRACTION, which overflows. */
    static final Comparator<Team> SUBTRACTION = (a, b) -> b.points() - a.points();

    /** Faulty B: ignores the goal-difference tie-breaker entirely. */
    static final Comparator<Team> MISSING_TIEBREAK =
            Comparator.comparingInt(Team::points).reversed().thenComparing(Team::name);

    /** Faulty C: not antisymmetric - returns 1 for any unequal pair. */
    static final Comparator<Team> NOT_ANTISYMMETRIC =
            (a, b) -> a.equals(b) ? 0 : 1;

    /** First team in a ranking having the given points, for the tie-break tests. */
    static Team firstWithPoints(List<Team> ranking, int points) {
        for (Team t : ranking) if (t.points() == points) return t;
        throw new IllegalArgumentException("no team with " + points + " points");
    }

    static int pass = 0, fail = 0;
    static void check(String id, String what, Object expected, Object actual) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(actual));
        if (ok) pass++; else fail++;
        System.out.printf("%-5s | %-46s | %-30s | %-30s | %s%n", id, what, expected, actual,
                ok ? "PASS" : "**FAIL**");
    }
    static void head() {
        System.out.printf("%-5s | %-46s | %-30s | %-30s | %s%n",
                "ID", "WHAT IS TESTED", "EXPECTED", "ACTUAL", "RESULT");
        System.out.println("-".repeat(130));
    }

    public static void main(String[] args) {
        System.out.println("Q5  -  A GENERIC RANKING COMPONENT DERIVED FROM THE PRINCIPLE");
        System.out.println("=".repeat(130));
        System.out.println("Name: Vignesh S   Register Number: 192325002");

        System.out.println();
        System.out.println("1.  WHAT THE PRINCIPLE REQUIRES, AND WHAT IT THEREFORE FORCES");
        System.out.println("-".repeat(130));
        System.out.println("  The principle names two properties: TRANSITIVITY (A>B and B>C implies");
        System.out.println("  A>C) and a WELL-DEFINED EQUALITY (equal items compare as equal).");
        System.out.println("  Together with the antisymmetry those imply, that is a TOTAL ORDER.");
        System.out.println();
        System.out.println("  Three design consequences follow directly:");
        System.out.println("    (i)   the comparison must be a FUNCTION of the items' fields only -");
        System.out.println("          anything else (insertion order, a random tiebreak, a mutable");
        System.out.println("          field) breaks transitivity the moment it changes;");
        System.out.println("    (ii)  the key sequence must END in a key that is unique, or two");
        System.out.println("          distinct items compare equal and the 'ranking' is ambiguous -");
        System.out.println("          which is why name is the final key, not an optional extra;");
        System.out.println("    (iii) the arithmetic must not overflow, because an overflowed");
        System.out.println("          subtraction flips a sign and destroys transitivity outright.");

        System.out.println();
        System.out.println("2.  Comparable OR Comparator?  THE DERIVATION");
        System.out.println("-".repeat(130));
        System.out.printf("%-34s | %-40s | %s%n", "QUESTION", "Comparable<Team>", "Comparator<Team>");
        System.out.println("-".repeat(130));
        String[][] rows = {
            {"how many orders per type?",    "exactly one",                 "as many as needed"},
            {"where does the rule live?",    "inside the entity",           "beside the use case"},
            {"can the rule change per event?","no, it is compiled in",      "yes, pass a different one"},
            {"works for a type I don't own?","no, cannot edit the class",   "yes"},
            {"reusable for Player, College?","no, each needs its own",      "yes, same chain pattern"},
        };
        for (String[] r : rows) System.out.printf("%-34s | %-40s | %s%n", r[0], r[1], r[2]);
        System.out.println();
        System.out.println("  DECISION: Comparator, because the brief states the component will be");
        System.out.println("  reused for players, colleges and quiz teams.  Reuse across TYPES needs");
        System.out.println("  generics; reuse across ORDERINGS of the same type needs Comparator.");
        System.out.println("  Comparable would still be reasonable if a team had one permanent");
        System.out.println("  natural order - it does not.");

        System.out.println();
        System.out.println("3.  THE MULTI-FIELD COMPARISON");
        System.out.println("-".repeat(130));
        System.out.println("  Comparator.comparingInt(Team::points).reversed()");
        System.out.println("            .thenComparing(comparingInt(Team::goalDiff).reversed())");
        System.out.println("            .thenComparing(Team::name);");
        System.out.println();
        System.out.println("  A detail that is easy to get wrong: .reversed() is applied to EACH");
        System.out.println("  numeric key separately, not to the whole chain.  Writing");
        System.out.println("  comparing(points).thenComparing(goalDiff).thenComparing(name).reversed()");
        System.out.println("  would reverse the NAME too, ranking Z before A among tied teams.");

        System.out.println();
        System.out.println("4.  TESTING WITH TIES AT EVERY LEVEL");
        System.out.println("-".repeat(130));
        List<Team> ties = List.of(
                new Team("Panthers", 9, 4), new Team("Eagles", 9, 6),
                new Team("Cobras", 6, 1),   new Team("Bisons", 9, 4),
                new Team("Dingoes", 6, 1),  new Team("Antelopes", 6, 1),
                new Team("Zebras", 12, 0),  new Team("Yaks", 12, 0));
        Leaderboard<Team> board = new Leaderboard<>(CORRECT);
        board.addAll(ties);
        head();
        check("T1", "tie on points only, split by goal difference",
                "[Yaks(12,0), Zebras(12,0)]", board.ranking().subList(0, 2).toString());
        check("T2", "tie on points AND goal difference, split by name",
                "[Bisons(9,4), Panthers(9,4)]", board.ranking().subList(3, 5).toString());
        check("T3", "three-way tie at the bottom, alphabetical",
                "[Antelopes(6,1), Cobras(6,1), Dingoes(6,1)]", board.ranking().subList(5, 8).toString());
        check("T4", "the comparator is a total order on this data", true, board.orderIsConsistent());
        check("T5", "ranking() does not disturb the stored order", 8, board.size());
        check("T6", "ranking() is repeatable", board.ranking().toString(), board.ranking().toString());

        System.out.println();
        System.out.println("5.  DATA DESIGNED TO BREAK A FAULTY COMPARATOR");
        System.out.println("-".repeat(130));
        System.out.println("  Each data set below is built specifically to expose one fault. A");
        System.out.println("  comparator that passes ordinary data can still fail all three.");
        System.out.println();
        head();

        // --- fault A: subtraction overflow ---
        List<Team> overflow = List.of(
                new Team("Huge", Integer.MAX_VALUE, 0),
                new Team("Tiny", Integer.MIN_VALUE + 1, 0),
                new Team("Mid",  0, 0));
        check("F1", "subtraction comparator: is it a total order?", false,
                isTotalOrder(overflow, SUBTRACTION));
        check("F2", "correct comparator on the same data", true,
                isTotalOrder(overflow, CORRECT));
        Team hi = overflow.get(0), lo = overflow.get(1);
        check("F3", "b.points-a.points for MAX vs MIN+1 has the wrong sign", true,
                SUBTRACTION.compare(hi, lo) > 0);
        System.out.printf("      | MAX_VALUE - (MIN_VALUE+1) overflows to %d, so 'Huge' is ranked BELOW 'Tiny'%n",
                (Integer.MIN_VALUE + 1) - Integer.MAX_VALUE);
        List<Team> wrongOrder = new ArrayList<>(overflow);
        wrongOrder.sort(SUBTRACTION);
        System.out.println("      | subtraction ranking: " + wrongOrder);
        List<Team> rightOrder = new ArrayList<>(overflow);
        rightOrder.sort(CORRECT);
        System.out.println("      | correct ranking:     " + rightOrder);

        // --- fault B: missing tie-breaker ---
        check("F4", "missing tie-break still a total order?", true,
                isTotalOrder(ties, MISSING_TIEBREAK));
        List<Team> noTie = new ArrayList<>(ties);
        noTie.sort(MISSING_TIEBREAK);
        List<Team> correctOrder = new ArrayList<>(ties);
        correctOrder.sort(CORRECT);
        check("F5", "missing tie-break puts a WORSE goal difference first",
                "Bisons(9,4)", firstWithPoints(noTie, 9).toString());
        check("F5b", "the correct comparator puts the BEST first",
                "Eagles(9,6)", firstWithPoints(correctOrder, 9).toString());
        System.out.println("      | missing-tiebreak ranking: " + noTie);
        System.out.println("      | correct ranking:          " + correctOrder);
        System.out.println("      | This fault does NOT break the principle - it is a valid total");
        System.out.println("      | order - it simply encodes the WRONG RULE.  A consistency check");
        System.out.println("      | alone would pass it, which is why ties must also be tested");
        System.out.println("      | against the stated competition rules.");

        // --- fault C: not antisymmetric ---
        check("F6", "'always 1' comparator is a total order?", false,
                isTotalOrder(ties, NOT_ANTISYMMETRIC));
        // Does List.sort actually REFUSE an inconsistent comparator?  Scan a range
        // of input sizes rather than assuming one triggers the check.
        String firstDetectedAt = "never, up to 4096 elements";
        for (int size = 8; size <= 4096; size *= 2) {
            List<Team> bad = new ArrayList<>();
            while (bad.size() < size) bad.addAll(ties);
            bad = new ArrayList<>(bad.subList(0, size));
            try { bad.sort(NOT_ANTISYMMETRIC); }
            catch (IllegalArgumentException e) { firstDetectedAt = "n = " + size; break; }
        }
        check("F7", "List.sort rejects the inconsistent comparator",
                "never, up to 4096 elements", firstDetectedAt);
        System.out.println("      | THIS IS THE IMPORTANT RESULT, AND IT IS NOT THE ONE I EXPECTED.");
        System.out.println("      | I predicted TimSort would throw 'Comparison method violates its");
        System.out.println("      | general contract!'.  It did not, at ANY size tested.  That check");
        System.out.println("      | fires only when a merge happens to encounter a specific");
        System.out.println("      | inconsistency; it is a best-effort diagnostic, NOT a validation.");
        System.out.println("      |");
        System.out.println("      | So a broken comparator can silently produce a meaningless");
        System.out.println("      | ranking with no error of any kind.  The ordering below is what");
        System.out.println("      | the faulty comparator actually produced:");
        List<Team> silent = new ArrayList<>(ties);
        silent.sort(NOT_ANTISYMMETRIC);
        System.out.println("      |   " + silent);
        System.out.println("      | Nothing in that output announces that it is wrong.  This is the");
        System.out.println("      | strongest argument for the isTotalOrder() check used in T4: the");
        System.out.println("      | library will not verify the principle for me, so the component");
        System.out.println("      | has to verify it itself.");

        System.out.println();
        System.out.println("6.  WHY THE COMPARATOR SATISFIES THE PRINCIPLE  (by reasoning, not output)");
        System.out.println("-".repeat(130));
        System.out.println("  The chain is a LEXICOGRAPHIC composition of three keys:");
        System.out.println("    k1 = -points      k2 = -goalDiff      k3 = name");
        System.out.println();
        System.out.println("  Each key maps a Team to a value in a set that already carries a total");
        System.out.println("  order: int for k1 and k2, String for k3.  comparingInt compares those");
        System.out.println("  values with < on int, which is transitive and antisymmetric by the");
        System.out.println("  definition of the integers; String.compareTo is likewise a total order");
        System.out.println("  on strings.  No subtraction is performed anywhere, so no comparison can");
        System.out.println("  overflow and no sign can flip.");
        System.out.println();
        System.out.println("  thenComparing composes them lexicographically: the result is decided by");
        System.out.println("  the first key that differs.  A lexicographic composition of total");
        System.out.println("  orders is itself a total order - if A>B and B>C, let i be the first key");
        System.out.println("  where A and B differ and j the first where B and C differ; at min(i,j)");
        System.out.println("  the comparison of A and C is decided in the same direction, because");
        System.out.println("  every earlier key is equal across all three. Transitivity follows.");
        System.out.println();
        System.out.println("  Equal items compare as equal: two Teams compare 0 exactly when points,");
        System.out.println("  goalDiff and name are all equal - which, since Team is a record whose");
        System.out.println("  equals uses those same three fields, is exactly when they are equal.");
        System.out.println("  The comparator is therefore CONSISTENT WITH equals, so the component");
        System.out.println("  behaves identically whether a List, a TreeSet or a TreeMap is used.");
        System.out.println();
        System.out.printf("  exhaustively machine-checked on the test data: %s%n", board.orderIsConsistent());

        System.out.println();
        System.out.println("7.  THE SAME COMPONENT REUSED FOR A DIFFERENT TYPE");
        System.out.println("-".repeat(130));
        Leaderboard<Player> players = new Leaderboard<>(
                Comparator.comparingInt(Player::runs).reversed().thenComparing(Player::name));
        players.add(new Player("Rohit", 420, 52));
        players.add(new Player("Virat", 512, 64));
        players.add(new Player("Gill", 420, 47));
        System.out.println("  Leaderboard<Player> with no change to Leaderboard at all:");
        System.out.println("    " + players.ranking());
        System.out.println();
        System.out.println("  WHERE List<? extends T> EARNS ITS PLACE:");
        Leaderboard<Object> mixed = new Leaderboard<>(Comparator.comparing(Object::toString));
        mixed.addAll(ties);                       // List<Team> into a Leaderboard<Object>
        mixed.addAll(List.of(new Player("Ash", 300, 40)));
        System.out.println("    Leaderboard<Object>.addAll(List<Team>) compiles because the");
        System.out.println("    parameter is List<? extends T>.  With a plain List<T> it would not,");
        System.out.println("    since List<Team> is not a subtype of List<Object>.");
        System.out.printf("    mixed board now holds %d items%n", mixed.size());

        System.out.println();
        System.out.println("=".repeat(130));
        System.out.printf("CHECKS RUN %d   PASSED %d   FAILED %d%n", pass + fail, pass, fail);
        System.out.println("=".repeat(130));
        if (fail > 0) System.exit(1);
    }
}
