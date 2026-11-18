/* A minimal main() so the leak can be stepped through under jdb.
 * Name: Vignesh S   Register Number: 192325002 */
import java.nio.file.*; import java.util.*; import java.io.IOException;
public class LeakDebugTarget {
    public static void main(String[] a) throws Exception {
        List<Path> files = new ArrayList<>();
        for (String n : new String[]{"store01.csv","store02.csv","store03.csv","store04.csv"})
            files.add(Path.of("stores", n));
        try {
            SalesReport.manual(files, Path.of("report_debug.csv"), false);
        } catch (IOException e) {
            System.out.println("caught: " + e.getMessage());
            System.out.println("store fds still open: " + SalesReport.openStoreFileCount());
        }
    }
}
