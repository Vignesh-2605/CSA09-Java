/* Deliberately does NOT compile.  A broader catch placed before a narrower
 * one. Kept in the submission as evidence of what javac says. */
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BadOrder {
    static void read(Path p) {
        try {
            Files.readString(p);
        } catch (IOException e) {               // BROADER - placed first
            System.out.println("io");
        } catch (NoSuchFileException e) {       // NARROWER - unreachable
            System.out.println("missing");
        }
    }
}
