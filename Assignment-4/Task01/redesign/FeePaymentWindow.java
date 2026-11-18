/* Assignment 4 (CO4) Task 1 - the redesigned window.
 *
 * This class COLLECTS INPUT and DISPLAYS RESULTS.  It contains no validation
 * rule, no SQL and no business decision; every one of those now lives in
 * FeeService or FeeRepository.  Compare its actionPerformed-equivalents with
 * the single 60-line handler in BadFeePaymentWindow.
 *
 * Name: Vignesh S   Register Number: 192325002 */
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class FeePaymentWindow extends JFrame {

    private final JTextField regField    = new JTextField(12);
    private final JTextField amountField = new JTextField(8);
    private final JComboBox<String> feeType =
            new JComboBox<>(new String[]{"Tuition", "Hostel", "Exam", "Library fine"});
    private final JLabel status = new JLabel(" ");
    private final FeeService service;

    /* Each button gets its OWN Action.  An Action carries its name, its
     * enabled state and its behaviour in one object, so the window never has
     * to ask "which button was this?" - the question that forced the string
     * comparison in the original. */
    private final Action payAction = new AbstractAction("Pay") {
        @Override public void actionPerformed(ActionEvent e) { onPay(); }
    };
    private final Action receiptAction = new AbstractAction("Receipt") {
        @Override public void actionPerformed(ActionEvent e) { onReceipt(); }
    };
    private final Action clearAction = new AbstractAction("Clear") {
        @Override public void actionPerformed(ActionEvent e) { onClear(); }
    };

    public FeePaymentWindow(FeeService service) {
        super("Fee Payment");
        this.service = service;

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 8, 5, 8); c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Register number"), c);
        c.gridx = 1; form.add(regField, c);
        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Fee type"), c);
        c.gridx = 1; form.add(feeType, c);
        c.gridx = 0; c.gridy = 2; form.add(new JLabel("Amount (Rs)"), c);
        c.gridx = 1; form.add(amountField, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(new JButton(clearAction));
        buttons.add(new JButton(receiptAction));
        JButton pay = new JButton(payAction);
        buttons.add(pay);
        getRootPane().setDefaultButton(pay);

        status.setBorder(BorderFactory.createEmptyBorder(4, 10, 8, 10));
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        add(status, BorderLayout.NORTH);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                int r = JOptionPane.showConfirmDialog(FeePaymentWindow.this,
                        "Close the fee counter?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) dispose();
            }
        });
        pack();
        setSize(Math.max(getWidth(), 420), getHeight());
    }

    private void info(String msg)  { status.setForeground(new Color(0, 110, 0)); status.setText(msg); }
    private void error(String msg) { status.setForeground(new Color(170, 0, 0)); status.setText(msg); }

    /* The handler is now four lines of UI work.  It validates by ASKING the
     * service, and it does the slow part off the Event Dispatch Thread. */
    private void onPay() {
        final Payment p;
        try {
            p = service.validate(regField.getText(),
                    (String) feeType.getSelectedItem(), amountField.getText());
        } catch (ValidationException ex) {
            error(ex.getMessage());
            focusField(ex.field());
            return;                                  // nothing was sent anywhere
        }
        payAction.setEnabled(false);
        info("Processing " + p.amountRupees() + " ...");

        // SwingWorker: doInBackground() runs on a worker thread, done() runs
        // back on the EDT.  The window stays repaintable and movable while
        // the database call is in flight - the fault Thread.sleep(2000) and
        // the inline JDBC caused in the original.
        new SwingWorker<Long, Void>() {
            @Override protected Long doInBackground() throws Exception {
                return service.pay(p.registerNumber(), p.feeType(),
                        String.valueOf(p.amountPaise() / 100.0));
            }
            @Override protected void done() {
                payAction.setEnabled(true);
                try {
                    info("Paid " + p.amountRupees() + ", receipt " + get());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    error("Payment failed: " + cause.getMessage());
                }
            }
        }.execute();
    }

    private void onReceipt() {
        String reg = regField.getText().trim();
        if (reg.isEmpty()) { error("Enter the register number to fetch a receipt"); return; }
        new SwingWorker<Payment, Void>() {
            @Override protected Payment doInBackground() throws Exception {
                return service.receiptFor(reg);
            }
            @Override protected void done() {
                try {
                    Payment p = get();
                    if (p == null) error("No payment found for " + reg);
                    else JOptionPane.showMessageDialog(FeePaymentWindow.this,
                            "Register number : " + p.registerNumber()
                          + "\nFee type        : " + p.feeType()
                          + "\nAmount          : " + p.amountRupees(),
                            "Receipt", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    error("Could not fetch the receipt: " + cause.getMessage());
                }
            }
        }.execute();
    }

    private void onClear() {
        regField.setText(""); amountField.setText("");
        feeType.setSelectedIndex(0);
        status.setText(" ");                        // the stale-status fault, fixed
        regField.requestFocusInWindow();
    }

    private void focusField(String field) {
        switch (field) {
            case "registerNumber" -> regField.requestFocusInWindow();
            case "amount"         -> amountField.requestFocusInWindow();
            default               -> feeType.requestFocusInWindow();
        }
    }

    /* --- accessors used only by the screenshot harness --- */
    public void setReg(String s)    { regField.setText(s); }
    public void setAmount(String s) { amountField.setText(s); }
    public void setFee(String s)    { feeType.setSelectedItem(s); }
    public void clickPay()          { payAction.actionPerformed(null); }
    public void clickClear()        { clearAction.actionPerformed(null); }

    public static void main(String[] args) {
        FeeService s = new FeeService(new FeeRepository(
                Db.URL, Db.USER, Db.PASS));
        SwingUtilities.invokeLater(() -> new FeePaymentWindow(s).setVisible(true));
    }
}
