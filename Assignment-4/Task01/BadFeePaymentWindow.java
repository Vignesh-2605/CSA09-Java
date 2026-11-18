/*
 * Programming in Java  -  Assignment 4 (CO4)
 * Task 1  -  GUI Design Review: the window AS SUPPLIED, with its faults intact.
 *
 * Name: Vignesh S   Register Number: 192325002
 *
 * This file is kept in the submission ON PURPOSE.  It is the "before" of the
 * design review.  Every numbered fault below is diagnosed in REPORT.md and
 * corrected in the redesigned version in this same folder.
 *
 * It compiles and runs.  That is the point: nothing here is a syntax error,
 * which is why a reviewer has to find these faults by reading rather than by
 * compiling.
 */
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;

public class BadFeePaymentWindow extends JFrame implements ActionListener {

    private final JTextField regField   = new JTextField(12);
    private final JTextField amountField = new JTextField(8);
    private final JComboBox<String> feeType =
            new JComboBox<>(new String[]{"Tuition", "Hostel", "Exam", "Library fine"});
    private final JButton payBtn     = new JButton("Pay");
    private final JButton receiptBtn = new JButton("Receipt");
    private final JButton clearBtn   = new JButton("Clear");
    private final JLabel  status     = new JLabel(" ");

    public BadFeePaymentWindow() {
        super("Fee Payment");
        JPanel form = new JPanel(new GridLayout(4, 2, 6, 6));
        form.add(new JLabel("Register number")); form.add(regField);
        form.add(new JLabel("Fee type"));        form.add(feeType);
        form.add(new JLabel("Amount"));          form.add(amountField);
        form.add(new JLabel("Status"));          form.add(status);

        JPanel buttons = new JPanel();
        buttons.add(payBtn); buttons.add(receiptBtn); buttons.add(clearBtn);

        // FAULT 1: ONE listener object is registered for every button, and the
        // handler then has to work out which button was pressed.
        payBtn.addActionListener(this);
        receiptBtn.addActionListener(this);
        clearBtn.addActionListener(this);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        setSize(420, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // FAULT 2: the command is compared with == instead of equals().  This
        // happens to work for a button's own label because the compiler
        // interns string literals, and stops working the moment the label is
        // built at run time.  It is a latent fault, not a visible one.
        String cmd = e.getActionCommand();

        if (cmd == "Pay") {
            // FAULT 3: ALL the logic lives inside the event handler - reading
            // the fields, validating, talking to the database, formatting the
            // message.  None of it can be unit tested without opening a window.
            String reg = regField.getText();
            String amt = amountField.getText();

            // FAULT 4: no validation at all.  A blank register number, letters
            // in the amount, or a negative amount all reach the database.
            double amount = Double.parseDouble(amt);

            // FAULT 5: JDBC on the Event Dispatch Thread.  While this runs,
            // the window cannot repaint, cannot be moved and cannot be closed.
            try {
                Connection con = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/csa09_test", "root", "");

                // FAULT 6: the SQL is built by string concatenation, so a
                // register number of   x'); DROP TABLE fee_payment; --
                // is executed as SQL.
                Statement st = con.createStatement();
                st.executeUpdate("INSERT INTO fee_payment(reg_no, fee_type, amount) VALUES('"
                        + reg + "','" + feeType.getSelectedItem() + "'," + amount + ")");

                // FAULT 7: neither the Statement nor the Connection is closed,
                // and there is no finally and no try-with-resources.  Every
                // payment leaks a connection.

                // FAULT 8: Thread.sleep() on the EDT to "show" progress.  This
                // freezes the entire user interface for two seconds.
                Thread.sleep(2000);

                status.setText("Paid");
            } catch (Exception ex) {
                // FAULT 9: the exception is swallowed.  The user is told
                // nothing, and nothing is logged.  A failed payment looks
                // exactly like a successful one that was slow.
            }

        } else if (cmd == "Clear") {
            regField.setText("");
            amountField.setText("");
            // FAULT 10: status is not cleared, so a stale "Paid" stays on
            // screen next to an empty form.
        }
        // FAULT 11: the Receipt button is registered as a listener but has no
        // branch here, so pressing it does nothing at all - silently.
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BadFeePaymentWindow().setVisible(true));
    }
}
