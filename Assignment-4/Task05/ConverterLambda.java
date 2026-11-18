/*
 * Assignment 4 (CO4) Task 5 - STYLE 4: lambda expressions.
 *
 * This is the "Complete the Code" program for Task 5.  The deck supplied to
 * me is the summary version without the skeleton, so the TODO markers are
 * placed where the task describes them: the Convert action, and the Enter-key
 * action that must also show the result in a dialog.  Both are completed.
 *
 * Name: Vignesh S   Register Number: 192325002
 */
import javax.swing.*;

public class ConverterLambda extends JFrame {
    final JTextField amount = new JTextField(10);
    final JComboBox<String> from = new JComboBox<>(Converter.codes());
    final JComboBox<String> to   = new JComboBox<>(Converter.codes());
    final JButton convert = new JButton("Convert");
    final JLabel result = new JLabel(" ");

    public ConverterLambda() {
        super("Currency Converter - lambda");
        Ui.build(this, amount, from, to, convert, result);

        /* TODO 1 COMPLETED: convert when the button is pressed. */
        convert.addActionListener(e -> Ui.run(this, amount, from, to, result, false));

        /* TODO 2 COMPLETED: Enter in the amount field converts AND shows the
         * result in a dialog. */
        amount.addActionListener(e -> Ui.run(this, amount, from, to, result, true));

        addWindowListener(Ui.closer(this));
    }
    public static void main(String[] a) { SwingUtilities.invokeLater(() -> new ConverterLambda().setVisible(true)); }
}
