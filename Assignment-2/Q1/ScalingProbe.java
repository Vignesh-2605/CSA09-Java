/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q1  -  testing the O(n^2) claim directly
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * A single ratio does not demonstrate a complexity class.  This probe doubles n
 * repeatedly and reports the ratio of each time to the previous one.  The
 * prediction is specific and falsifiable:
 *
 *   if + is O(n^2), doubling n should multiply its time by about 4
 *   if StringBuilder is O(n), doubling n should multiply its time by about 2
 *
 * A measurement that showed + doubling, or StringBuilder quadrupling, would
 * refute the analysis.
 */
import bench.Bench;
import java.util.List;

public class ScalingProbe {

    static final int[] SIZES = { 250, 500, 1_000, 2_000, 4_000, 8_000 };

    public static void main(String[] args) {
        System.out.println("Q1  -  SCALING PROBE: DOES THE + LOOP REALLY GROW AS n^2?");
        System.out.println("=".repeat(104));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.println();
        System.out.println("PREDICTION (stated before measuring): each doubling of n should multiply");
        System.out.println("  String +        time by about 4.0   (quadratic)");
        System.out.println("  StringBuilder   time by about 2.0   (linear)");
        System.out.println();

        List<Bench.Result> all = Bench.list();
        double prevConcat = 0, prevBuilder = 0;

        System.out.printf("%-10s | %-14s | %-10s | %-14s | %-10s | %s%n",
                "n (fields)", "String + (ms)", "x prev", "Builder (ms)", "x prev", "+ / Builder");
        System.out.println("-".repeat(104));

        for (int n : SIZES) {
            final String[] fields = TicketBuilder.ticketFields(n, 7);
            Bench.Result c = Bench.measure("String +", "scaling", n, 1,
                    () -> sink(TicketBuilder.withConcat(fields)));
            Bench.Result b = Bench.measure("StringBuilder", "scaling", n, 1,
                    () -> sink(TicketBuilder.withStringBuilder(fields)));
            all.add(c); all.add(b);

            String rc = prevConcat  == 0 ? "-" : String.format("%.2f", c.median() / prevConcat);
            String rb = prevBuilder == 0 ? "-" : String.format("%.2f", b.median() / prevBuilder);
            System.out.printf("%-10s | %-14.3f | %-10s | %-14.4f | %-10s | %.0fx%n",
                    Bench.fmt(n), c.medianMs(), rc, b.medianMs(), rb, c.median() / b.median());
            prevConcat = c.median(); prevBuilder = b.median();
        }

        System.out.println();
        System.out.println("READING THE 'x prev' COLUMNS");
        System.out.println("-".repeat(104));
        System.out.println("  The String + column should sit near 4.0 and the StringBuilder column");
        System.out.println("  near 2.0.  Two caveats worth stating rather than hiding:");
        System.out.println();
        System.out.println("  1. At the smallest sizes the absolute times are only microseconds, so");
        System.out.println("     timer resolution and JIT effects dominate and the early ratios are");
        System.out.println("     unreliable.  The large-n rows carry the evidence.");
        System.out.println("  2. StringBuilder's ratio can exceed 2.0 because its internal array");
        System.out.println("     doubles as it grows, which copies the buffer a handful of times -");
        System.out.println("     that is still O(n) amortised, but it is not perfectly smooth.");
        System.out.println();
        System.out.println("  The decisive observation is the final column: the ratio between the two");
        System.out.println("  designs is not constant.  It GROWS with n.  A constant-factor difference");
        System.out.println("  would keep that column flat; a difference in complexity class makes it");
        System.out.println("  climb, and that is what distinguishes 'slower' from 'asymptotically");
        System.out.println("  worse'.");

        Bench.writeCsv("Q1-scaling", all);
    }

    static long SINK;
    static void sink(String s) { SINK += s.length(); }
}
