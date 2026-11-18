/* Deliberately does NOT compile.  A CHECKED exception that is neither caught
 * nor declared.  Shows that the compiler, not the test suite, finds this. */
import java.nio.file.Files;
import java.nio.file.Path;

public class BadUnhandledChecked {
    static String read(Path p) {
        return Files.readString(p);   // throws IOException - checked
    }
}
