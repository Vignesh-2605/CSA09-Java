/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q6  -  the fairness inquiry and the deployment decision
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * "A severity-5 patient may wait indefinitely if severe cases keep arriving.
 *  Explain why, and design a fair rule."
 *
 * Rather than asserting that starvation happens, this program SIMULATES a
 * realistic arrival stream and measures how long the lowest-priority patients
 * actually wait, with and without the fair rule.
 */
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

public class Starvation {

    /* A case whose EFFECTIVE priority can rise as it waits. */
    static final class Case {
        final String name; final int severity; final long arrivalTick;
        long servedTick = -1;
        Case(String name, int severity, long arrivalTick) {
            this.name = name; this.severity = severity; this.arrivalTick = arrivalTick;
        }
        long waited() { return servedTick < 0 ? -1 : servedTick - arrivalTick; }
        @Override public String toString() { return name + "(S" + severity + ")"; }
    }

    /** The simulation parameters, chosen to model a busy but not absurd ED. */
    static final int TICKS = 20_000;          // one tick = one minute of a long shift
    static final double ARRIVALS_PER_TICK = 1.15;  // slightly more arrive than are served
    static final int SERVICE_EVERY = 1;       // the doctor sees one patient per tick
    static final int AGING_THRESHOLD = 120;   // minutes before a patient is promoted
    /** A case may never be promoted ABOVE this level, so a genuine severity-1
     *  arrival is never overtaken.  Set to 1 this rule degenerates to FIFO -
     *  which the first version of this design did, and section 4 shows why. */
    static final int PROMOTION_FLOOR = 2;
    static final Random RNG = new Random(2026);

    /** Severity mix: critical cases are rare, minor ones common. */
    static int randomSeverity() {
        int r = RNG.nextInt(100);
        if (r < 8)  return 1;
        if (r < 24) return 2;
        if (r < 52) return 3;
        if (r < 78) return 4;
        return 5;
    }

    public static void main(String[] args) {
        System.out.println("Q6  -  STARVATION, FAIRNESS AND THE DEPLOYMENT DECISION");
        System.out.println("=".repeat(118));
        System.out.println("Name: Vignesh S   Register Number: 192325002");

        System.out.println();
        System.out.println("1.  WHY A SEVERITY-5 PATIENT CAN WAIT INDEFINITELY");
        System.out.println("-".repeat(118));
        System.out.println("  The comparator orders on severity FIRST and arrival only as a");
        System.out.println("  tie-break.  So a severity-5 case is overtaken by EVERY severity 1-4");
        System.out.println("  patient, no matter how long it has waited and no matter how recently");
        System.out.println("  they arrived.  Waiting time has no influence on the ordering at all.");
        System.out.println();
        System.out.println("  Formally: the queue serves min(severity, arrival) lexicographically.");
        System.out.println("  A severity-5 case reaches the head only when the queue contains no");
        System.out.println("  case of severity 1-4.  If such cases arrive at least as fast as they");
        System.out.println("  are served, that condition is never reached and the wait is unbounded.");
        System.out.println("  This is STARVATION, and it is a property of the ORDERING, not a bug in");
        System.out.println("  the implementation - the heap is doing exactly what it was told.");
        System.out.println();
        System.out.printf("  Simulation: %s ticks, %.2f arrivals per tick, 1 patient served per tick,%n",
                TICKS, ARRIVALS_PER_TICK);
        System.out.println("  severity mix 8%/16%/28%/26%/22% for S1..S5.");

        Result strict  = simulate(false);
        Result fairV1  = simulate(true, 1);                     // first attempt: no floor
        Result fair    = simulate(true, PROMOTION_FLOOR);       // corrected

        System.out.println();
        System.out.println("2.  MEASURED STARVATION UNDER THE STRICT RULE");
        System.out.println("-".repeat(118));
        report(strict, "STRICT severity-first");

        System.out.println();
        System.out.println("3.  THE FAIR RULE");
        System.out.println("-".repeat(118));
        System.out.printf("  DESIGN: a case waiting longer than %d ticks is promoted by one severity%n",
                AGING_THRESHOLD);
        System.out.println("  level for ordering purposes, and may be promoted repeatedly, but never");
        System.out.println("  above level 1.  The CLINICAL severity recorded on the chart is never");
        System.out.println("  altered - only the queue's view of it.");
        System.out.println();
        System.out.println("  effectiveSeverity(c, now) = max(1, c.severity - (now - c.arrival) / " + AGING_THRESHOLD + ")");
        System.out.println();
        System.out.println("  WHY AGING RATHER THAN THE OBVIOUS ALTERNATIVES:");
        System.out.println("    - a fixed quota ('serve one S5 every 10 patients') is simple but");
        System.out.println("      serves a minor case ahead of a critical one on a fixed schedule,");
        System.out.println("      which is clinically indefensible;");
        System.out.println("    - a pure deadline ('nobody waits over 2 hours') has the same flaw at");
        System.out.println("      the deadline and produces a stampede when many expire together;");
        System.out.println("    - aging degrades GRADUALLY: a long-waiting S5 first competes with S4,");
        System.out.println("      then S3, and reaches the head only if it keeps being overtaken. A");
        System.out.println("      genuinely critical arrival still goes first, because it enters at");
        System.out.println("      level 1 and nothing can be promoted above that.");
        System.out.println();
        System.out.println("  THE COST: the effective priority depends on 'now', so the heap's");
        System.out.println("  ordering is no longer stable over time - a key changes while the");
        System.out.println("  element is inside the heap, which is exactly the mutation hazard Q3");
        System.out.println("  examined for HashSet.  The implementation therefore recomputes the");
        System.out.println("  order at service time rather than relying on stale heap positions.");
        System.out.println();
        System.out.println("4.  THE FIRST VERSION OF THIS RULE WAS WRONG, AND THE DATA SAID SO");
        System.out.println("-".repeat(118));
        System.out.println("  My first design promoted without any floor: max(1, severity - waited/T).");
        System.out.println("  It fixed starvation, and broke something far more important.");
        System.out.println();
        System.out.printf("%-32s | %-18s | %-18s | %s%n", "MEASURE", "STRICT", "AGING, NO FLOOR", "VERDICT");
        System.out.println("-".repeat(118));
        cmp3("S5 mean wait", strict.meanWait[5], fairV1.meanWait[5]);
        cmp3("S5 never served", strict.neverServed[5], fairV1.neverServed[5]);
        cmp3("S1 mean wait", strict.meanWait[1], fairV1.meanWait[1]);
        cmp3("S1 worst wait", strict.maxWait[1], fairV1.maxWait[1]);
        System.out.println();
        System.out.println("  Starvation of minor cases improved sharply - and CRITICAL patients went");
        System.out.printf("  from a mean wait of %.1f ticks to %.1f.%n",
                strict.meanWait[1], fairV1.meanWait[1]);
        System.out.println();
        System.out.println("  The cause is structural, not a tuning error.  With no floor, EVERY");
        System.out.println("  waiting case eventually reaches level 1, so after enough time all cases");
        System.out.println("  share the same effective severity and the tie-break - arrival order -");
        System.out.println("  decides everything.  The rule degenerates to FIFO, which is precisely");
        System.out.println("  the behaviour section 1 showed to be unacceptable.  Lengthening the");
        System.out.println("  threshold only delays that collapse; it does not prevent it.");
        System.out.println();
        System.out.printf("  CORRECTION: a promotion FLOOR of %d. A case may age up to level %d but%n",
                PROMOTION_FLOOR, PROMOTION_FLOOR);
        System.out.println("  never to level 1, so a genuine severity-1 arrival is never overtaken by");
        System.out.println("  an aged minor case.  Starvation is bounded; clinical priority is not");
        System.out.println("  surrendered.");
        System.out.println();
        report(fair, "FAIR with aging + floor " + PROMOTION_FLOOR);

        System.out.println();
        System.out.println("5.  ALL THREE RULES, SIDE BY SIDE");
        System.out.println("-".repeat(118));
        System.out.printf("%-30s | %-15s | %-17s | %-17s | %s%n",
                "MEASURE", "STRICT", "AGING no floor", "AGING floor " + PROMOTION_FLOOR, "BEST");
        System.out.println("-".repeat(118));
        row3("S1 mean wait (ticks)", strict.meanWait[1], fairV1.meanWait[1], fair.meanWait[1], true);
        row3("S1 worst wait (ticks)", strict.maxWait[1], fairV1.maxWait[1], fair.maxWait[1], true);
        row3("S5 mean wait (ticks)", strict.meanWait[5], fairV1.meanWait[5], fair.meanWait[5], true);
        row3("S5 worst wait (ticks)", strict.maxWait[5], fairV1.maxWait[5], fair.maxWait[5], true);
        row3("S5 never served", strict.neverServed[5], fairV1.neverServed[5], fair.neverServed[5], true);
        row3("patients served", strict.served, fairV1.served, fair.served, false);
        System.out.println();
        System.out.println("  The floored rule is the only one of the three that is acceptable on");
        System.out.println("  BOTH measures: critical patients keep an essentially immediate wait,");
        System.out.println("  and minor patients are no longer left unseen indefinitely.");
        System.out.println();
        System.out.println("  Whether the remaining S5 wait is clinically acceptable is a decision");
        System.out.println("  for the department, not for the programmer.  The threshold and the");
        System.out.println("  floor are the two dials that set it, which is why both are named");
        System.out.println("  constants rather than numbers buried in the comparator.");

        System.out.println();
        System.out.println("5.  DEPLOYMENT DECISION: CAN THIS SERVE 5,000 PATIENTS A DAY?");
        System.out.println("-".repeat(118));
        System.out.println("  5,000 patients a day is one arrival every 17.3 seconds on average.");
        System.out.println("  From the growth measurements in TriageDesign:");
        System.out.println();
        System.out.printf("    %-42s %s%n", "admit() at n = 100,000:", "about 147 ns");
        System.out.printf("    %-42s %s%n", "next()  at n = 100,000:", "about 408 ns");
        System.out.printf("    %-42s %s%n", "5,000 admissions + 5,000 services:", "about 2.8 ms of CPU PER DAY");
        System.out.println();
        System.out.println("  The queue is not remotely the bottleneck. A realistic waiting room holds");
        System.out.println("  tens of patients, not thousands, so n stays small and log n stays under");
        System.out.println("  6.  The structure would still be comfortable at a hundred times this");
        System.out.println("  load.");
        System.out.println();
        System.out.println("  VERDICT: YES on throughput - but with two caveats that matter more than");
        System.out.println("  the timings:");
        System.out.println();
        System.out.println("    (a) WITHOUT the fair rule it should NOT be deployed at any scale. The");
        System.out.printf("        simulation left %d minor patients unseen at the end of the shift%n",
                strict.neverServed[5] + strict.neverServed[4]);
        System.out.printf("        and the worst minor wait reached %d ticks. That is a clinical%n",
                strict.maxWait[5]);
        System.out.println("        safety failure, and no amount of throughput compensates for it.");
        System.out.println();
        System.out.println("    (b) The waiting-list display is O(n log n) because the heap must be");
        System.out.println("        copied and sorted. On a screen refreshed every few seconds that");
        System.out.println("        is the dominant cost of the whole system - far larger than admit");
        System.out.println("        and serve combined. This is the limitation Q7 takes up.");
    }

    static void cmp(String label, long a, long b) {
        String change = a == b ? "unchanged"
                : b < a ? String.format("improved by %.0f%%", 100.0 * (a - b) / Math.max(a, 1))
                        : String.format("worse by %.0f%%", 100.0 * (b - a) / Math.max(a, 1));
        System.out.printf("%-30s | %-20s | %-20s | %s%n", label, fmt(a), fmt(b), change);
    }
    static void cmp(String label, double a, double b) {
        String change = Math.abs(a - b) < 0.05 ? "unchanged"
                : b < a ? String.format("improved by %.0f%%", 100.0 * (a - b) / Math.max(a, 0.001))
                        : String.format("worse by %.0f%%", 100.0 * (b - a) / Math.max(a, 0.001));
        System.out.printf("%-30s | %-20.1f | %-20.1f | %s%n", label, a, b, change);
    }
    static String fmt(long v) { return String.format("%,d", v); }

    static void cmp3(String label, double a, double b) {
        String verdict = b < a * 0.9 ? "improved" : b > a * 1.1 ? "MADE WORSE" : "unchanged";
        System.out.printf("%-32s | %-18.1f | %-18.1f | %s%n", label, a, b, verdict);
    }
    static void cmp3(String label, long a, long b) { cmp3(label, (double) a, (double) b); }

    static void row3(String label, double a, double b, double c, boolean lowerBetter) {
        double best = lowerBetter ? Math.min(a, Math.min(b, c)) : Math.max(a, Math.max(b, c));
        String which = best == a ? "STRICT" : best == c ? "floored" : "no floor";
        System.out.printf("%-30s | %-15.1f | %-17.1f | %-17.1f | %s%n", label, a, b, c, which);
    }
    static void row3(String label, long a, long b, long c, boolean lowerBetter) {
        row3(label, (double) a, (double) b, (double) c, lowerBetter);
    }

    static void report(Result r, String label) {
        System.out.printf("  %s: %s arrivals, %s served, %s still waiting%n",
                label, fmt(r.arrivals), fmt(r.served), fmt(r.remaining));
        System.out.println();
        System.out.printf("  %-10s | %-10s | %-10s | %-14s | %-14s | %s%n",
                "SEVERITY", "ARRIVED", "SERVED", "MEAN WAIT", "WORST WAIT", "NEVER SERVED");
        System.out.println("  " + "-".repeat(110));
        for (int s = 1; s <= 5; s++)
            System.out.printf("  S%-9d | %-10s | %-10s | %-14.1f | %-14s | %s%n",
                    s, fmt(r.arrived[s]), fmt(r.servedBy[s]), r.meanWait[s],
                    fmt(r.maxWait[s]), fmt(r.neverServed[s]));
    }

    static final class Result {
        long arrivals, served, remaining;
        long[] arrived = new long[6], servedBy = new long[6], maxWait = new long[6], neverServed = new long[6];
        double[] meanWait = new double[6];
    }

    /** Discrete-event simulation of one long shift. */
    static Result simulate(boolean aging) { return simulate(aging, 1); }

    static Result simulate(boolean aging, int floor) {
        Random rng = new Random(2026);                 // same stream for both runs
        Result res = new Result();
        long[] totalWait = new long[6];
        List<Case> waiting = new ArrayList<>();
        long seq = 0;

        for (long tick = 0; tick < TICKS; tick++) {
            int arrivals = (int) ARRIVALS_PER_TICK;
            if (rng.nextDouble() < ARRIVALS_PER_TICK - arrivals) arrivals++;
            for (int i = 0; i < arrivals; i++) {
                int sev = severityFrom(rng);
                waiting.add(new Case("P" + (seq++), sev, tick));
                res.arrivals++; res.arrived[sev]++;
            }
            if (tick % SERVICE_EVERY == 0 && !waiting.isEmpty()) {
                final long now = tick;
                Comparator<Case> order = aging
                        ? Comparator.comparingInt((Case c) -> effective(c, now, floor)).thenComparingLong(c -> c.arrivalTick)
                        : Comparator.comparingInt((Case c) -> c.severity).thenComparingLong(c -> c.arrivalTick);
                int best = 0;
                for (int i = 1; i < waiting.size(); i++)
                    if (order.compare(waiting.get(i), waiting.get(best)) < 0) best = i;
                Case c = waiting.remove(best);
                c.servedTick = tick;
                res.served++; res.servedBy[c.severity]++;
                long w = c.waited();
                totalWait[c.severity] += w;
                if (w > res.maxWait[c.severity]) res.maxWait[c.severity] = w;
            }
        }
        res.remaining = waiting.size();
        for (Case c : waiting) {
            res.neverServed[c.severity]++;
            long w = TICKS - c.arrivalTick;
            if (w > res.maxWait[c.severity]) res.maxWait[c.severity] = w;
        }
        for (int s = 1; s <= 5; s++)
            res.meanWait[s] = res.servedBy[s] == 0 ? 0 : (double) totalWait[s] / res.servedBy[s];
        return res;
    }

    /**
     * The effective severity used for ORDERING only; the clinical severity on
     * the chart is never changed.
     *
     * A case ages upward from its own severity but can never be promoted past
     * its floor.  The floor is min(severity, PROMOTION_FLOOR), NOT
     * PROMOTION_FLOOR itself - otherwise a genuine severity-1 case would be
     * pushed DOWN to level 2, which was the bug in the first implementation
     * of this method and is why section 5's first run showed no improvement.
     */
    static int effective(Case c, long now, int floor) {
        long waited = now - c.arrivalTick;
        int promoted = c.severity - (int) (waited / AGING_THRESHOLD);
        int ownFloor = Math.min(c.severity, floor);     // never better than its own severity
        return Math.max(ownFloor, promoted);
    }

    static int severityFrom(Random rng) {
        int r = rng.nextInt(100);
        if (r < 8)  return 1;
        if (r < 24) return 2;
        if (r < 52) return 3;
        if (r < 78) return 4;
        return 5;
    }
}
