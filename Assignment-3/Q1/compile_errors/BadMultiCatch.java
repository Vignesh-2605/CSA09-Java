/* Deliberately does NOT compile.  Multi-catch alternatives related by
 * subclassing.  This was our own first draft of AtmSimulation.withdraw(). */
public class BadMultiCatch {
    static void parse(String s) {
        try {
            Long.parseLong(s);
            if (s.isEmpty()) throw new IllegalArgumentException("empty");
        } catch (NumberFormatException | IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }
}
