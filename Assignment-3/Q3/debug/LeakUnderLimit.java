/* Runs the LEAKING version repeatedly under a low descriptor limit, to
 * reproduce the retail chain's "Too many open files".
 *
 * NOTE on the harness: the per-run failure caused by the corrupt row ALSO
 * arrives as an IOException, so an earlier version of this program caught
 * both with one clause and silently hid the descriptor exhaustion it was
 * written to demonstrate.  The two are now told apart by their cause.
 *
 * Name: Vignesh S   Register Number: 192325002 */
import java.nio.file.*; import java.util.*; import java.io.IOException;
public class LeakUnderLimit {
    static boolean isExhaustion(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause())
            if (c.getMessage() != null && c.getMessage().contains("Too many open files")) return true;
        return false;
    }
    public static void main(String[] a) throws Exception {
        List<Path> files = new ArrayList<>();
        for (String n : new String[]{"store01.csv","store02.csv","store03.csv","store04.csv"})
            files.add(Path.of("stores", n));
        int runs = 0, expected = 0;
        System.out.printf("  descriptor limit visible to this JVM: %s%n", readLimit());
        for (int i = 0; i < 10_000; i++) {
            try {
                SalesReport.manual(files, Path.of("report_limit.csv"), false);
            } catch (IOException e) {
                if (isExhaustion(e)) {
                    System.out.printf("  STOPPED after %d report runs%n", runs);
                    System.out.printf("  leaked store handles held at that point: %d%n",
                            SalesReport.OPENED.size());
                    System.out.printf("  what stopped it: %s%n", rootMessage(e));
                    return;
                }
                expected++;                       // the corrupt-row failure, as designed
            } catch (Throwable t) {
                System.out.printf("  STOPPED after %d report runs: %s%n", runs, t);
                return;
            }
            runs++;
            if (runs % 20 == 0)
                System.out.printf("  after %4d report runs: %d store handles leaked and still held%n",
                        runs, SalesReport.OPENED.size());
        }
        System.out.printf("  completed all %d runs without exhaustion (%d corrupt-row failures)%n",
                runs, expected);
    }
    static String rootMessage(Throwable t) {
        Throwable c = t; while (c.getCause() != null) c = c.getCause();
        return c.getClass().getName() + ": " + c.getMessage();
    }
    static String readLimit() {
        try { for (String l : Files.readAllLines(Path.of("/proc/self/limits")))
                  if (l.startsWith("Max open files")) return l.replaceAll("\\s+", " ").trim();
        } catch (IOException ignored) { }
        return "unknown";
    }
}
