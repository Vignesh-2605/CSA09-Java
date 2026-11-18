/* Assignment 4 (CO4) Task 1 - drives the redesigned window and photographs it.
 * Name: Vignesh S   Register Number: 192325002 */
import javax.swing.*;

public class ShotHarness {
    public static void main(String[] args) throws Exception {
        FeeService s = new FeeService(new FeeRepository(
                Db.URL, Db.USER, Db.PASS));
        final FeePaymentWindow[] w = new FeePaymentWindow[1];
        SwingUtilities.invokeAndWait(() -> {
            w[0] = new FeePaymentWindow(s);
            w[0].setLocation(60, 60);
            w[0].setVisible(true);
        });
        Shot.pause(700);
        Shot.of(w[0], "../../shots/task01-1-empty.png");

        SwingUtilities.invokeAndWait(() -> { w[0].setReg("1923"); w[0].setAmount("abc"); });
        SwingUtilities.invokeAndWait(w[0]::clickPay);
        Shot.pause(300);
        Shot.of(w[0], "../../shots/task01-2-invalid-register.png");

        SwingUtilities.invokeAndWait(() -> { w[0].setReg("192325002"); w[0].setAmount("-50"); });
        SwingUtilities.invokeAndWait(w[0]::clickPay);
        Shot.pause(300);
        Shot.of(w[0], "../../shots/task01-3-invalid-amount.png");

        SwingUtilities.invokeAndWait(() -> {
            w[0].setReg("192325002"); w[0].setFee("Tuition"); w[0].setAmount("1500.50"); });
        SwingUtilities.invokeAndWait(w[0]::clickPay);
        Shot.pause(1400);
        Shot.of(w[0], "../../shots/task01-4-paid.png");
        System.out.println("    rows now in fee_payment: " + count());
        System.exit(0);
    }
    static int count() throws Exception {
        try (var c = java.sql.DriverManager.getConnection(
                Db.URL, Db.USER, Db.PASS);
             var st = c.createStatement();
             var rs = st.executeQuery("SELECT COUNT(*) FROM fee_payment")) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }
}
