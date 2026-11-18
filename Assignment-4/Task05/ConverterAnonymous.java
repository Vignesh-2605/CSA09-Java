/* Assignment 4 (CO4) Task 5 - STYLE 3: anonymous inner classes.
 * Name: Vignesh S   Register Number: 192325002 */
import java.awt.event.*;
import javax.swing.*;

public class ConverterAnonymous extends JFrame {
    final JTextField amount = new JTextField(10);
    final JComboBox<String> from = new JComboBox<>(Converter.codes());
    final JComboBox<String> to   = new JComboBox<>(Converter.codes());
    final JButton convert = new JButton("Convert");
    final JLabel result = new JLabel(" ");

    public ConverterAnonymous() {
        super("Currency Converter - anonymous inner class");
        Ui.build(this, amount, from, to, convert, result);
        convert.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                Ui.run(ConverterAnonymous.this, amount, from, to, result, false);
            }
        });
        amount.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                Ui.run(ConverterAnonymous.this, amount, from, to, result, true);
            }
        });
        addWindowListener(Ui.closer(this));
    }
    public static void main(String[] a) { SwingUtilities.invokeLater(() -> new ConverterAnonymous().setVisible(true)); }
}
