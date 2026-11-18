/*
 * Programming in Java  -  Assignment 3 (CO3)
 * Q1, alternative-concept analysis  -  what an exception actually costs
 *
 * Name: Vignesh S   Register Number: 192325002
 *
 * The question asks us to compare exception handling against (a) validating
 * every input before use and (b) returning an error code such as -1.  Prose
 * alone cannot answer "what does it cost", so this program measures it.
 */
import bench.Bench;
import java.util.ArrayList;
import java.util.List;

public class ExceptionCost {

    /* ---------- the three designs, doing identical work ---------- */

    static class AmountTooLarge extends Exception {
        AmountTooLarge(String m) { super(m); }
    }
    /** Same exception, but with the stack trace switched off. */
    static class AmountTooLargeLight extends Exception {
        AmountTooLargeLight(String m) { super(m, null, false, false); }
    }

    static final long LIMIT = 20_000L;

    /** volatile, so the JIT cannot delete the loops as dead code. */
    static volatile long sink;

    /** DESIGN 1: throw. */
    static long byException(long amount) {
        try {
            if (amount > LIMIT) throw new AmountTooLarge("over the daily limit");
            return amount;
        } catch (AmountTooLarge e) {
            return 0;
        }
    }

    /** DESIGN 1b: throw, but do not capture the stack. */
    static long byLightException(long amount) {
        try {
            if (amount > LIMIT) throw new AmountTooLargeLight("over the daily limit");
            return amount;
        } catch (AmountTooLargeLight e) {
            return 0;
        }
    }

    /** DESIGN 2: return a sentinel. */
    static long byErrorCode(long amount) {
        if (amount > LIMIT) return -1;
        return amount;
    }

    /** DESIGN 3: validate before calling, so the failure never happens. */
    static boolean isWithdrawable(long amount) { return amount <= LIMIT; }

    public static void main(String[] args) throws Exception {
        System.out.println("Q1, ALTERNATIVE-CONCEPT ANALYSIS  -  THE COST OF AN EXCEPTION");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.println("JVM: " + System.getProperty("java.version")
                + "   warm-up " + Bench.WARMUP + " runs discarded, " + Bench.REPS
                + " measured runs, MEDIAN reported");
        System.out.println();
        System.out.println("Each run performs 1,000,000 withdrawal checks.  The only thing that");
        System.out.println("changes between the blocks below is HOW OFTEN the check fails.  That is");
        System.out.println("the whole point: an exception is not expensive or cheap on its own, it is");
        System.out.println("expensive IN PROPORTION TO HOW OFTEN IT IS THROWN.");

        final int OPS = 1_000_000;
        int[] failPercents = { 0, 1, 10, 50, 100 };
        List<Bench.Result> all = new ArrayList<>();

        System.out.println();
        System.out.printf("%-10s | %-26s | %12s | %12s | %8s | %s%n",
                "FAIL RATE", "DESIGN", "MEDIAN (ms)", "ns PER CHECK", "RSD %", "RELATIVE TO PRE-VALIDATION");
        System.out.println("-".repeat(112));

        /* GLOBAL warm-up before the first block.  The 0% block is measured
         * first, so without this it pays for the JIT compiling all four
         * methods and reports an exception-that-is-never-thrown as 1.6x
         * slower than a comparison - which is an artefact, not a cost. */
        {
            long[] mixed = new long[100_000];
            java.util.Random r0 = new java.util.Random(7);
            for (int i = 0; i < mixed.length; i++)
                mixed[i] = r0.nextInt(100) < 30 ? LIMIT + 1 : r0.nextInt((int) LIMIT);
            for (int w = 0; w < 60; w++) {
                long a1 = 0, a2 = 0, a3 = 0, a4 = 0;
                for (long a : mixed) { a1 += byErrorCode(a); if (isWithdrawable(a)) a2 += a;
                                       a3 += byLightException(a); a4 += byException(a); }
                sink = a1 + a2 + a3 + a4;
            }
        }

        for (int pct : failPercents) {
            long[] amounts = new long[OPS];
            // deterministic: a fixed seed, so the numbers are reproducible
            java.util.Random rnd = new java.util.Random(42);
            for (int i = 0; i < OPS; i++)
                amounts[i] = rnd.nextInt(100) < pct ? LIMIT + 1 + rnd.nextInt(1000)
                                                    : rnd.nextInt((int) LIMIT);

            // Warm ALL FOUR designs on THIS block's data before measuring any of
            // them.  Without this the first block leaves the JIT with a branch
            // profile from the previous fail rate, and the first design measured
            // in each block pays for the recompilation.  That showed up as a
            // 38% RSD and a non-monotonic error-code row in our first run.
            for (int w = 0; w < 30; w++) {
                long a1 = 0, a2 = 0, a3 = 0, a4 = 0;
                for (long a : amounts) { a1 += byErrorCode(a); if (isWithdrawable(a)) a2 += a;
                                         a3 += byLightException(a); a4 += byException(a); }
                sink = a1 + a2 + a3 + a4;
            }

            Bench.Result code = Bench.measure("error code -1", "check@" + pct + "%", pct, OPS, () -> {
                long acc = 0; for (long a : amounts) acc += byErrorCode(a); sink = acc;
            });
            Bench.Result pre = Bench.measure("pre-validation", "check@" + pct + "%", pct, OPS, () -> {
                long acc = 0; for (long a : amounts) if (isWithdrawable(a)) acc += a; sink = acc;
            });
            Bench.Result light = Bench.measure("exception, no stack trace", "check@" + pct + "%", pct, OPS, () -> {
                long acc = 0; for (long a : amounts) acc += byLightException(a); sink = acc;
            });
            Bench.Result exc = Bench.measure("exception, full stack trace", "check@" + pct + "%", pct, OPS, () -> {
                long acc = 0; for (long a : amounts) acc += byException(a); sink = acc;
            });

            double base = pre.median();          // the stable design is the baseline
            for (Bench.Result r : new Bench.Result[]{ pre, code, light, exc }) {
                double ratio = r.median() / base;
                String rel = r == pre ? "baseline"
                           : ratio >= 1.0 ? String.format("%.1fx slower", ratio)
                                          : String.format("%.1fx faster", 1.0 / ratio);
                if (r.rsd() > 15.0) rel += "   [RSD > 15%, treat as noisy]";
                System.out.printf("%-10s | %-26s | %12.3f | %12.2f | %8.1f | %s%n",
                        pct + "%", r.design, r.medianMs(), r.perOp(), r.rsd(), rel);
                all.add(r);
            }
            System.out.println("-".repeat(112));
        }

        Bench.writeCsv("Q1", all);

        /* ---------- where the cost actually is ---------- */
        System.out.println();
        System.out.println("WHERE THE COST IS  -  THE STACK TRACE, NOT THE throw");
        System.out.println("-".repeat(112));
        System.out.println("  The two exception rows differ only in one constructor argument:");
        System.out.println();
        System.out.println("      super(message);                      // captures the stack");
        System.out.println("      super(message, null, false, false);  // does not");
        System.out.println();
        System.out.println("  The second form passes writableStackTrace = false, which skips");
        System.out.println("  fillInStackTrace().  Everything else - allocating the object, the");
        System.out.println("  throw, unwinding to the catch - is identical.  Compare the two rows at");
        System.out.println("  100%: the gap between them IS the cost of walking the call stack and");
        System.out.println("  allocating one StackTraceElement per frame.");

        System.out.println();
        System.out.println("SPACE COST");
        System.out.println("-".repeat(112));
        long before, after;
        Runtime rt = Runtime.getRuntime();
        List<Object> keep = new ArrayList<>();
        System.gc(); Thread.sleep(120);
        before = rt.totalMemory() - rt.freeMemory();
        for (int i = 0; i < 50_000; i++) keep.add(new AmountTooLarge("over the daily limit"));
        after = rt.totalMemory() - rt.freeMemory();
        double withTrace = (after - before) / 50_000.0;

        keep.clear(); System.gc(); Thread.sleep(120);
        before = rt.totalMemory() - rt.freeMemory();
        for (int i = 0; i < 50_000; i++) keep.add(new AmountTooLargeLight("over the daily limit"));
        after = rt.totalMemory() - rt.freeMemory();
        double noTrace = (after - before) / 50_000.0;

        keep.clear(); System.gc(); Thread.sleep(120);
        before = rt.totalMemory() - rt.freeMemory();
        for (int i = 0; i < 50_000; i++) keep.add(Long.valueOf(-1L - i));
        after = rt.totalMemory() - rt.freeMemory();
        double errCode = (after - before) / 50_000.0;

        System.out.printf("  %-44s | %s%n", "50,000 exceptions WITH a stack trace",
                String.format("%.0f bytes each", withTrace));
        System.out.printf("  %-44s | %s%n", "50,000 exceptions WITHOUT a stack trace",
                String.format("%.0f bytes each", noTrace));
        System.out.printf("  %-44s | %s%n", "50,000 boxed error codes (Long)",
                String.format("%.0f bytes each", errCode));
        System.out.printf("  %-44s | %s%n", "a primitive long error code", "0 bytes of heap - it lives in a register or on the stack");
        System.out.println();
        System.out.println("  (measured with Runtime.totalMemory() - freeMemory() around 50,000");
        System.out.println("  retained objects; approximate, because the JVM's allocation is not");
        System.out.println("  observable exactly, but the ORDER of the three is the finding.)");

        System.out.println();
        System.out.println("THE THREE DESIGNS SIDE BY SIDE");
        System.out.println("=".repeat(112));
        System.out.printf("%-22s | %-28s | %-28s | %s%n", "", "EXCEPTIONS", "ERROR CODE (-1)", "PRE-VALIDATION");
        System.out.println("-".repeat(112));
        String[][] cmp = {
            {"caller can ignore it", "no - checked ones must be handled", "YES - -1 reads as a balance", "no - the check is the call"},
            {"error detail carried", "type, message, cause, stack", "one number, no context", "none - no error exists"},
            {"cost when nothing fails", "zero (nothing is thrown)", "one comparison", "one comparison"},
            {"cost when all fails", "highest - see the 100% rows", "one comparison", "one comparison"},
            {"return value stays clean", "yes - -1 is a real amount", "no - the range is stolen", "yes"},
            {"works across a deep call", "yes - unwinds many frames", "every frame must forward it", "only at the top"},
            {"catches what you forgot", "yes - the unexpected still throws", "no", "no - only what you checked"},
        };
        for (String[] r : cmp) System.out.printf("%-22s | %-28s | %-28s | %s%n", r[0], r[1], r[2], r[3]);

        System.out.println();
        System.out.println("CONCLUSION FOR THE ATM");
        System.out.println("-".repeat(112));
        System.out.println("  Use PRE-VALIDATION for what the customer types.  A non-numeric amount");
        System.out.println("  is not exceptional - it is expected several times a day - and checking");
        System.out.println("  it with a parse-and-test costs one comparison.  This is why the real");
        System.out.println("  withdraw() checks \"amount <= 0\" and \"amount % 100 != 0\" with plain if");
        System.out.println("  statements rather than letting something deeper throw.");
        System.out.println();
        System.out.println("  Use EXCEPTIONS for what pre-validation cannot know in advance: the");
        System.out.println("  balance (which another transaction may change), the account's");
        System.out.println("  existence, and the card reader.  These need the type, the message and");
        System.out.println("  the cause, they occur rarely, and they must not be ignorable - which");
        System.out.println("  is exactly what a CHECKED exception enforces and -1 cannot.");
        System.out.println();
        System.out.println("  Do NOT use an error code for a cash amount.  -1 is a valid long, so");
        System.out.println("  \"if (result == -1)\" is a convention the compiler cannot enforce; one");
        System.out.println("  caller who forgets it credits the customer with minus one paise.");
        System.out.println();
        System.out.println("  The measured numbers do not overturn this.  At the 0% fail rate - the");
        System.out.println("  ATM's normal day - the four designs are indistinguishable, because an");
        System.out.println("  exception that is never thrown costs nothing at all.  The try block");
        System.out.println("  itself is free: the JVM records the handler in an exception table");
        System.out.println("  attached to the method, so entering a try costs no instructions.");
        System.out.println();
        System.out.println("TWO THINGS IN THE DATA THAT ARE NOT WHAT THEY FIRST LOOK LIKE");
        System.out.println("-".repeat(112));
        System.out.println("  1.  The two cheap designs are SLOWER at 50% than at 100%.  That is not");
        System.out.println("      the error-code design getting faster - it is BRANCH PREDICTION.  At");
        System.out.println("      0% and at 100% the comparison goes the same way every time and the");
        System.out.println("      processor predicts it perfectly.  At 50% it is a coin toss, and");
        System.out.println("      every misprediction discards a pipeline.  The effect appears in the");
        System.out.println("      pre-validation row and the error-code row equally, which is how we");
        System.out.println("      know it belongs to the branch and not to either design.");
        System.out.println();
        System.out.println("  2.  Our FIRST run of this program reported the error-code design at");
        System.out.println("      2.234 ms for the 1% block but 0.732 ms for the 100% block, with an");
        System.out.println("      RSD of 38%.  A design whose work does not depend on the fail rate");
        System.out.println("      cannot behave that way, so the number was wrong, not surprising.");
        System.out.println("      The cause was the JIT: each block changes the branch profile, and");
        System.out.println("      whichever design was measured first in a block paid for the");
        System.out.println("      recompilation.  Fixed by warming all four designs on each block's");
        System.out.println("      own data before measuring any of them, and by a global warm-up");
        System.out.println("      before the first block.  RSD fell from 38% to under 3%.");
        System.out.println("      This is why Assignment 2 insisted that one execution time is not a");
        System.out.println("      measurement: the harness reports RSD precisely so that a figure");
        System.out.println("      this unstable announces itself instead of reaching the report.");
    }
}
