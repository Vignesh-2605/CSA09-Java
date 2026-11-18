/* Assignment 4 (CO4) Task 6 - demonstrates normal, invalid and duplicate
 * registrations against the live database, and photographs each.
 * Name: Vignesh S   Register Number: 192325002 */
import javax.swing.*;

public class AppHarness {
    public static void main(String[] args) {
        try { body(); } catch (Throwable t) { t.printStackTrace(); System.exit(2); }
        System.exit(0);
    }
    static void body() throws Exception {
        System.out.println("TASK 6  -  CAMPUS EVENT REGISTRATION: NORMAL, INVALID AND DUPLICATE");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.println();
        RegistrationService s = new RegistrationService(new RegistrationDao());
        final EventRegistrationApp[] w = new EventRegistrationApp[1];
        SwingUtilities.invokeAndWait(() -> { w[0] = new EventRegistrationApp(s); w[0].setLocation(30, 30); w[0].setVisible(true); });
        Shot.pause(1600);
        Shot.of(w[0], "../shots/task06-1-register-screen.png");

        System.out.printf("%-26s | %-46s | %s%n", "CASE", "STATUS SHOWN TO THE USER", "OUTCOME");
        System.out.println("-".repeat(112));

        // 1. normal
        step(w[0], "192325002", "Vignesh S", "vignesh@campus.example", "normal registration",
             "../shots/task06-2-registered.png");
        // 2. invalid register number
        step(w[0], "1923", "Vignesh S", "vignesh@campus.example", "invalid register number",
             "../shots/task06-3-invalid-regno.png");
        // 3. invalid email
        step(w[0], "192325003", "Arun K", "not-an-email", "invalid email",
             "../shots/task06-4-invalid-email.png");
        // 4. invalid name
        step(w[0], "192325004", "R2D2", "arun@campus.example", "invalid name",
             "../shots/task06-5-invalid-name.png");
        // 5. duplicate
        step(w[0], "192325002", "Vignesh S", "vignesh@campus.example", "duplicate registration",
             "../shots/task06-6-duplicate.png");

        // the registrations tab
        SwingUtilities.invokeAndWait(() -> { w[0].showTab(1); w[0].selectFilter(0); });
        Shot.pause(1400);
        Shot.of(w[0], "../shots/task06-7-registrations-table.png");
        System.out.println();
        System.out.println("  registrations tab: " + w[0].viewStatusText());
    }

    static void step(EventRegistrationApp w, String reg, String name, String mail,
                     String label, String shot) throws Exception {
        SwingUtilities.invokeAndWait(() -> { w.showTab(0); w.fill(reg, name, mail); });
        SwingUtilities.invokeAndWait(w::clickRegister);
        Shot.pause(1100);
        Shot.of(w, shot);
        String st = w.registerStatusText();
        System.out.printf("%-26s | %-46s | %s%n", label, st.length() > 46 ? st.substring(0, 45) + "." : st,
                st.startsWith("Registered") ? "ACCEPTED" : "REJECTED");
    }
}
