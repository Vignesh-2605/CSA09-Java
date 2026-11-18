/* Assignment 4 (CO4) Task 5 - STYLE 2: a named inner class.
 * Name: Vignesh S   Register Number: 192325002 */
import java.awt.event.*;
import javax.swing.*;

public class ConverterNamedInner extends JFrame {
    final JTextField amount = new JTextField(10);
    final JComboBox<String> from = new JComboBox<>(Converter.codes());
    final JComboBox<String> to   = new JComboBox<>(Converter.codes());
    final JButton convert = new JButton("Convert");
    final JLabel result = new JLabel(" ");

    /* A named inner class: it has a name, so it can be reused and tested,
     * and it still sees the enclosing frame's fields. */
    class ConvertHandler implements ActionListener {
        private final boolean dialog;
        ConvertHandler(boolean dialog) { this.dialog = dialog; }
        @Override public void actionPerformed(ActionEvent e) {
            Ui.run(ConverterNamedInner.this, amount, from, to, result, dialog);
        }
    }

    public ConverterNamedInner() {
        super("Currency Converter - named inner class");
        Ui.build(this, amount, from, to, convert, result);
        convert.addActionListener(new ConvertHandler(false));
        amount.addActionListener(new ConvertHandler(true));   // same class, different argument
        addWindowListener(Ui.closer(this));
    }
    public static void main(String[] a) { SwingUtilities.invokeLater(() -> new ConverterNamedInner().setVisible(true)); }
}
