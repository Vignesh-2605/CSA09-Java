/* Assignment 4 (CO4) Task 1 - the evidence that the redesign worked.
 *
 * This runs HEADLESS.  No JFrame is constructed, no DISPLAY is needed, and
 * every rule that used to live inside actionPerformed() is exercised here.
 * Against BadFeePaymentWindow none of these checks could be written at all.
 *
 * Name: Vignesh S   Register Number: 192325002 */
import java.util.*;

public class FeeServiceTest {
    static int checks = 0, passed = 0;

    static void expectOk(String id, FeeService s, String reg, String type, String amt, long expectPaise) {
        checks++;
        try {
            Payment p = s.validate(reg, type, amt);
            boolean ok = p.amountPaise() == expectPaise;
            if (ok) passed++;
            System.out.printf("  %-5s | %-34s | %-28s | %s%n", id,
                    "accepted, " + p.amountPaise() + " paise",
                    "expected " + expectPaise + " paise", ok ? "PASS" : "FAIL");
        } catch (ValidationException e) {
            System.out.printf("  %-5s | %-34s | %-28s | FAIL%n", id,
                    "REJECTED: " + e.getMessage(), "expected accepted");
        }
    }

    static void expectReject(String id, FeeService s, String reg, String type, String amt, String field) {
        checks++;
        try {
            Payment p = s.validate(reg, type, amt);
            System.out.printf("  %-5s | %-34s | %-28s | FAIL%n", id,
                    "ACCEPTED " + p.amountPaise() + " paise", "expected rejection (" + field + ")");
        } catch (ValidationException e) {
            boolean ok = e.field().equals(field);
            if (ok) passed++;
            System.out.printf("  %-5s | %-34s | %-28s | %s%n", id,
                    "rejected on " + e.field(), e.getMessage(), ok ? "PASS" : "FAIL");
        }
    }

    public static void main(String[] args) {
        System.out.println("TASK 1  -  THE REDESIGNED FeeService, TESTED WITHOUT OPENING A WINDOW");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.printf("java.awt.headless = %s   (no display is used by this test)%n",
                System.getProperty("java.awt.headless"));
        System.out.println();
        System.out.println("The same rules, when they lived inside BadFeePaymentWindow.actionPerformed(),");
        System.out.println("could not be reached by any test: they required a visible window, a mouse");
        System.out.println("click and a live database. That is what separating the layers bought.");
        System.out.println();

        // The repository is never touched by validate(), so the test needs no database.
        FeeService s = new FeeService(null);

        System.out.printf("  %-5s | %-34s | %-28s | %s%n", "ID", "OUTCOME", "EXPECTED / MESSAGE", "VERDICT");
        System.out.println("  " + "-".repeat(104));
        expectOk    ("V1", s, "192325002", "Tuition",      "1500",      150000L);
        expectOk    ("V2", s, "192325002", "Hostel",       "1500.50",   150050L);
        expectOk    ("V3", s, " 192325002 ", "Exam",       "0.01",           1L);
        expectOk    ("V4", s, "192325002", "Library fine", "500000",  50000000L);
        expectReject("V5", s, "",          "Tuition",      "1500",   "registerNumber");
        expectReject("V6", s, "1923",      "Tuition",      "1500",   "registerNumber");
        expectReject("V7", s, "19232500X", "Tuition",      "1500",   "registerNumber");
        expectReject("V8", s, "192325002", "Donation",     "1500",   "feeType");
        expectReject("V9", s, "192325002", null,           "1500",   "feeType");
        expectReject("VA", s, "192325002", "Tuition",      "",       "amount");
        expectReject("VB", s, "192325002", "Tuition",      "abc",    "amount");
        expectReject("VC", s, "192325002", "Tuition",      "-100",   "amount");
        expectReject("VD", s, "192325002", "Tuition",      "0",      "amount");
        expectReject("VE", s, "192325002", "Tuition",      "1.005",  "amount");
        expectReject("VF", s, "192325002", "Tuition",      "500000.01", "amount");
        expectReject("VG", s, "192325002", "Tuition",      "1e5",    "amount");

        System.out.println();
        System.out.printf("  %d of %d checks passed.%n", passed, checks);

        System.out.println();
        System.out.println("WHAT EACH CHECK WOULD HAVE DONE TO THE ORIGINAL WINDOW");
        System.out.println("-".repeat(112));
        System.out.printf("  %-6s | %-30s | %s%n", "INPUT", "BadFeePaymentWindow", "FeeService");
        System.out.println("  " + "-".repeat(104));
        String[][] cmp = {
            {"V5", "NumberFormatException, swallowed", "rejected, named field"},
            {"VB", "NumberFormatException, swallowed", "rejected, named field"},
            {"VC", "stored as a NEGATIVE payment", "rejected"},
            {"V2", "stored as a double - 1500.5", "stored as 150050 paise exactly"},
            {"V8", "stored - the list is never checked", "rejected"},
        };
        for (String[] r : cmp) System.out.printf("  %-6s | %-30s | %s%n", r[0], r[1], r[2]);

        System.out.println();
        System.out.println("THE MONEY POINT, DEMONSTRATED");
        System.out.println("-".repeat(112));
        double d = 0.1 + 0.2;
        System.out.printf("  as double : 0.1 + 0.2 = %.20f%n", d);
        System.out.printf("  equals 0.3? %s%n", d == 0.3);
        System.out.printf("  as paise  : %d + %d = %d  (exact)%n", 10L, 20L, 30L);
        System.out.println("  The old window parsed the amount with Double.parseDouble and stored a");
        System.out.println("  double. A fee ledger that cannot represent its own values exactly is");
        System.out.println("  a correctness fault, not a rounding preference.");
        System.exit(passed == checks ? 0 : 1);
    }
}
