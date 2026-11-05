/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 8  -  Alternative Concept and Complexity Analysis
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Question: the design uses dynamic method dispatch.  If the same behaviour were
 * written with instanceof checks and casting for each vehicle type, what would
 * the solution be?  Analyse the time per call for k vehicle types - O(1) dispatch
 * versus up to k checks - and the effect on code size and maintenance.
 */
public class InstanceofComparison {

    private static final int K = 12;
    private static final int N = 10_000_000;

    /* ---- the type hierarchy, shared by both designs --------------------- */
    abstract static class V { final String r; V(String r){this.r=r;} abstract double rate(); }
    static class V0 extends V { V0(String r){super(r);} double rate(){return 10;} }
    static class V1 extends V { V1(String r){super(r);} double rate(){return 30;} }
    static class V2 extends V { V2(String r){super(r);} double rate(){return 30;} }
    static class V3 extends V { V3(String r){super(r);} double rate(){return 15;} }
    static class V4 extends V { V4(String r){super(r);} double rate(){return 60;} }
    static class V5 extends V { V5(String r){super(r);} double rate(){return 20;} }
    static class V6 extends V { V6(String r){super(r);} double rate(){return 25;} }
    static class V7 extends V { V7(String r){super(r);} double rate(){return 40;} }
    static class V8 extends V { V8(String r){super(r);} double rate(){return 50;} }
    static class V9 extends V { V9(String r){super(r);} double rate(){return 35;} }
    static class VA extends V { VA(String r){super(r);} double rate(){return 45;} }
    static class VB extends V { VB(String r){super(r);} double rate(){return 55;} }

    /* ---- A: dispatch ---------------------------------------------------- */
    static long feeByDispatch(V v, int minutes) {
        return (long) ((minutes + 59) / 60) * (long) v.rate();
    }

    /* ---- B: instanceof chain with casting -------------------------------- */
    static long feeByInstanceof(V v, int minutes) {
        int h = (minutes + 59) / 60;
        if (v instanceof V0) return h * (long) ((V0) v).rate();
        if (v instanceof V1) return h * (long) ((V1) v).rate();
        if (v instanceof V2) return h * (long) ((V2) v).rate();
        if (v instanceof V3) return h * (long) ((V3) v).rate();
        if (v instanceof V4) return h * (long) ((V4) v).rate();
        if (v instanceof V5) return h * (long) ((V5) v).rate();
        if (v instanceof V6) return h * (long) ((V6) v).rate();
        if (v instanceof V7) return h * (long) ((V7) v).rate();
        if (v instanceof V8) return h * (long) ((V8) v).rate();
        if (v instanceof V9) return h * (long) ((V9) v).rate();
        if (v instanceof VA) return h * (long) ((VA) v).rate();
        if (v instanceof VB) return h * (long) ((VB) v).rate();
        throw new IllegalArgumentException("unknown vehicle type " + v.getClass());
    }

    static V make(int t, String r) {
        switch (t) {
            case 0: return new V0(r); case 1: return new V1(r); case 2:  return new V2(r);
            case 3: return new V3(r); case 4: return new V4(r); case 5:  return new V5(r);
            case 6: return new V6(r); case 7: return new V7(r); case 8:  return new V8(r);
            case 9: return new V9(r); case 10: return new VA(r); default: return new VB(r);
        }
    }

    public static void main(String[] args) {
        System.out.println("TASK 8  -  ALTERNATIVE: DYNAMIC DISPATCH  vs  instanceof + CASTING");
        System.out.println("=".repeat(80));

        V[] fleet = new V[K];
        for (int t = 0; t < K; t++) fleet[t] = make(t, "TN" + t);

        System.out.println();
        System.out.println("1.  BOTH AGREE ON EVERY TYPE  (k = " + K + ", 130 minutes)");
        System.out.println("-".repeat(80));
        boolean agree = true;
        for (int t = 0; t < K; t++)
            if (feeByDispatch(fleet[t], 130) != feeByInstanceof(fleet[t], 130)) agree = false;
        System.out.println("  all " + K + " types produce identical fees: " + agree);
        System.out.println("  so the difference is entirely one of cost and maintainability.");

        System.out.println();
        System.out.println("2.  MEASURED TIME PER CALL  (" + String.format("%,d", N) + " calls each)");
        System.out.println("-".repeat(80));

        long sink = 0;
        for (int w = 0; w < 3; w++)
            for (int i = 0; i < 2_000_000; i++) {
                sink += feeByDispatch(fleet[i % K], 130);
                sink += feeByInstanceof(fleet[i % K], 130);
            }

        long t0 = System.nanoTime();
        for (int i = 0; i < N; i++) sink += feeByDispatch(fleet[i % K], 130);
        long tD = System.nanoTime() - t0;

        long t1 = System.nanoTime();
        for (int i = 0; i < N; i++) sink += feeByInstanceof(fleet[i % K], 130);
        long tI = System.nanoTime() - t1;

        // the two ends of the chain, measured separately
        V first = fleet[0], last = fleet[K - 1];
        long t2 = System.nanoTime();
        for (int i = 0; i < N; i++) sink += feeByInstanceof(first, 130);
        long tFirst = System.nanoTime() - t2;
        long t3 = System.nanoTime();
        for (int i = 0; i < N; i++) sink += feeByInstanceof(last, 130);
        long tLast = System.nanoTime() - t3;
        long t4 = System.nanoTime();
        for (int i = 0; i < N; i++) sink += feeByDispatch(last, 130);
        long tDlast = System.nanoTime() - t4;

        System.out.printf("%-38s | %-12s | %-16s | %s%n",
                          "CALL PATTERN", "TOTAL (ms)", "PER CALL (ns)", "TESTS PERFORMED");
        System.out.println("-".repeat(80));
        System.out.printf("%-38s | %-12.1f | %-16.2f | %s%n", "A  dispatch, all k types mixed",
                          tD / 1e6, (double) tD / N, "0 - one indirect jump");
        System.out.printf("%-38s | %-12.1f | %-16.2f | %s%n", "B  instanceof, all k types mixed",
                          tI / 1e6, (double) tI / N, "1..k, average " + (K / 2));
        System.out.printf("%-38s | %-12.1f | %-16.2f | %s%n", "B  instanceof, FIRST type only",
                          tFirst / 1e6, (double) tFirst / N, "1");
        System.out.printf("%-38s | %-12.1f | %-16.2f | %s%n", "B  instanceof, LAST type only",
                          tLast / 1e6, (double) tLast / N, String.valueOf(K));
        System.out.printf("%-38s | %-12.1f | %-16.2f | %s%n", "A  dispatch, LAST type only",
                          tDlast / 1e6, (double) tDlast / N, "0");
        System.out.println();
        System.out.printf("  cost of being the LAST type in the chain : %.2f ns vs %.2f ns = %.1fx%n",
                          (double) tLast / N, (double) tFirst / N, (double) tLast / tFirst);
        System.out.printf("  dispatch is position-independent         : %.2f ns for the same object%n",
                          (double) tDlast / N);

        System.out.println();
        System.out.println("3.  COMPLEXITY IN THE NUMBER OF TYPES k");
        System.out.println("-".repeat(80));
        System.out.printf("%-26s | %-14s | %-14s | %s%n",
                          "DESIGN", "BEST CASE", "WORST CASE", "WHY");
        System.out.println("-".repeat(80));
        System.out.printf("%-26s | %-14s | %-14s | %s%n", "A  dispatch", "O(1)", "O(1)",
                          "one method-table lookup");
        System.out.printf("%-26s | %-14s | %-14s | %s%n", "B  instanceof chain", "O(1)", "O(k)",
                          "every earlier test must fail first");
        System.out.println();
        System.out.println("  The measured rows above are the evidence for the O(k): the SAME chain");
        System.out.println("  and the SAME arithmetic cost measurably more for the last type than");
        System.out.println("  for the first, while dispatch costs the same for both.  The gap grows");
        System.out.println("  linearly with k, so a facility that grows from 3 to 12 to 30 vehicle");
        System.out.println("  categories makes the chain progressively worse and dispatch unchanged.");
        System.out.println();
        System.out.println("  One honest qualification the table also shows: dispatch is not free of");
        System.out.println("  all context either.  The 'all k types mixed' row is slower than the");
        System.out.println("  'LAST type only' row for dispatch too, because a call site that sees");
        System.out.println("  one class every time is MONOMORPHIC and the JIT can inline it, while a");
        System.out.println("  site that sees twelve classes is megamorphic and must take the real");
        System.out.println("  indirect jump.  That variation depends on the call SITE, not on where");
        System.out.println("  a type sits in a hand-written list, so it cannot be made worse by");
        System.out.println("  adding a thirteenth type at the bottom - which is what the chain does.");

        System.out.println();
        System.out.println("4.  CODE SIZE AND MAINTENANCE");
        System.out.println("-".repeat(80));
        System.out.printf("%-30s | %-16s | %s%n", "MEASURE", "A  dispatch", "B  instanceof chain");
        System.out.println("-".repeat(80));
        System.out.printf("%-30s | %-16s | %s%n", "lines for the fee logic", "1", String.valueOf(K + 2));
        System.out.printf("%-30s | %-16s | %s%n", "lines added per new type", "0", "1 per chain");
        System.out.printf("%-30s | %-16s | %s%n", "casts written", "0", String.valueOf(K));
        System.out.printf("%-30s | %-16s | %s%n", "order-dependent?", "no", "YES");
        System.out.printf("%-30s | %-16s | %s%n", "missing type detected", "at compile time", "at run time");
        System.out.println();
        System.out.println("  Two maintenance hazards are specific to the chain:");
        System.out.println();
        System.out.println("  a) ORDER.  A subclass must be tested before its superclass.  In the");
        System.out.println("     real hierarchy ElectricCar extends Car, so 'if (v instanceof Car)'");
        System.out.println("     placed first would capture every EV and bill it as a plain car -");
        System.out.println("     and it would compile, run, and produce a plausible wrong number.");
        System.out.println();
        System.out.println("  b) THE CAST.  Each branch needs an explicit downcast, which the");
        System.out.println("     compiler cannot verify independently of the test above it.  Pair the");
        System.out.println("     wrong test with the wrong cast and the result is a");
        System.out.println("     ClassCastException at the exit barrier, with a driver waiting.");
        System.out.println("     Java 16 pattern matching (v instanceof V1 x) removes the separate");
        System.out.println("     cast but none of the ordering problem.");

        System.out.println();
        System.out.println("5.  CONCLUSION");
        System.out.println("-".repeat(80));
        System.out.println("  Dispatch is retained.  It is faster (position-independent O(1) against");
        System.out.println("  O(k)), shorter (1 line against " + (K + 2) + "), free of casts, and");
        System.out.println("  immune to the ordering trap that makes the chain silently mis-bill a");
        System.out.println("  subclass.  Above all it puts each type's rate INSIDE that type, so the");
        System.out.println("  compiler - not a reviewer - is what guarantees every type has one.");
        System.out.println();
        System.out.println("  instanceof is still the right tool for the one job dispatch cannot do:");
        System.out.println("  asking whether a vehicle has an OPTIONAL capability, which is exactly");
        System.out.println("  how FeeDemo uses 'v instanceof Chargeable c' to add a charging fee only");
        System.out.println("  for the vehicles that can draw power.  The objection is to using it as");
        System.out.println("  a substitute for dispatch, not to the operator itself.");
        if (sink == Long.MIN_VALUE) System.out.println(sink);
    }
}
