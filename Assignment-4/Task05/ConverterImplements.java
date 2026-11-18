/* Assignment 4 (CO4) Task 5 - STYLE 1: the frame itself implements ActionListener.
 * Name: Vignesh S   Register Number: 192325002 */
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ConverterImplements extends JFrame implements ActionListener {
    final JTextField amount = new JTextField(10);
    final JComboBox<String> from = new JComboBox<>(Converter.codes());
    final JComboBox<String> to   = new JComboBox<>(Converter.codes());
    final JButton convert = new JButton("Convert");
    final JLabel result = new JLabel(" ");

    public ConverterImplements() {
        super("Currency Converter - implements ActionListener");
        Ui.build(this, amount, from, to, convert, result);
        convert.addActionListener(this);        // the frame IS the listener
        amount.addActionListener(this);         // Enter in the field, same listener
        addWindowListener(Ui.closer(this));
    }
    /* ONE handler for both sources; it must ask what happened. */
    @Override public void actionPerformed(ActionEvent e) {
        Ui.run(this, amount, from, to, result, e.getSource() == amount);
    }
    public static void main(String[] a) { SwingUtilities.invokeLater(() -> new ConverterImplements().setVisible(true)); }
}
