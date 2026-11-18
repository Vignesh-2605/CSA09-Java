/* Assignment 4 (CO4) Task 5 - the window layout and the result display, shared
 * by all four styles so that only the EVENT WIRING differs between them.
 * Name: Vignesh S   Register Number: 192325002 */
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public final class Ui {
    static void build(JFrame f, JTextField amount, JComboBox<String> from,
                      JComboBox<String> to, JButton convert, JLabel result) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 8, 5, 8); c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy = 0; p.add(new JLabel("Amount"), c);
        c.gridx = 1; p.add(amount, c);
        c.gridx = 0; c.gridy = 1; p.add(new JLabel("From"), c);
        c.gridx = 1; p.add(from, c);
        c.gridx = 0; c.gridy = 2; p.add(new JLabel("To"), c);
        c.gridx = 1; p.add(to, c);
        to.setSelectedItem("USD");
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(convert);
        result.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        f.add(result, BorderLayout.NORTH);
        f.add(p, BorderLayout.CENTER);
        f.add(south, BorderLayout.SOUTH);
        f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        f.pack();
        f.setSize(Math.max(f.getWidth(), 380), f.getHeight());
    }

    /** The shared body of every handler, so the styles differ only in wiring. */
    static void run(JFrame owner, JTextField amount, JComboBox<String> from,
                    JComboBox<String> to, JLabel result, boolean dialog) {
        try {
            String text = Converter.convert(amount.getText(),
                    (String) from.getSelectedItem(), (String) to.getSelectedItem());
            result.setForeground(new Color(0, 110, 0));
            result.setText(text);
            if (dialog) JOptionPane.showMessageDialog(owner, text, "Converted",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Converter.BadAmount ex) {
            result.setForeground(new Color(170, 0, 0));
            result.setText(ex.getMessage());
            if (dialog) JOptionPane.showMessageDialog(owner, ex.getMessage(), "Invalid amount",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Window closing handled with a WindowAdapter, as the task requires. */
    static WindowAdapter closer(JFrame f) {
        return new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                int r = JOptionPane.showConfirmDialog(f, "Close the converter?",
                        "Confirm", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) f.dispose();
            }
        };
    }
    private Ui() { }
}
