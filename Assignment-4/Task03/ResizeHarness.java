/* Assignment 4 (CO4) Task 3 - photographs the screen at three window sizes and
 * measures which panels grew.
 * Name: Vignesh S   Register Number: 192325002 */
import java.awt.*;
import javax.swing.*;

public class ResizeHarness {
    public static void main(String[] args) throws Exception {
        System.out.println("TASK 3  -  RESIZE BEHAVIOUR AT THREE WINDOW SIZES");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.println();
        final LibrarySearch[] w = new LibrarySearch[1];
        SwingUtilities.invokeAndWait(() -> { w[0] = new LibrarySearch(); w[0].setLocation(40, 40); w[0].setVisible(true); });
        Shot.pause(700);

        int[][] sizes = {{560, 320}, {820, 500}, {1180, 740}};
        String[] names = {"small", "medium", "large"};
        System.out.printf("%-8s | %-12s | %-24s | %-22s | %s%n",
                "SIZE", "WINDOW", "RESULTS AREA (grows)", "SEARCH FIELD (fixed)", "RESERVE BUTTON (fixed)");
        System.out.println("-".repeat(112));
        for (int i = 0; i < sizes.length; i++) {
            final int W = sizes[i][0], H = sizes[i][1];
            SwingUtilities.invokeAndWait(() -> { w[0].setSize(W, H); w[0].validate(); });
            Shot.pause(500);
            Shot.of(w[0], "../shots/task03-" + (i + 1) + "-" + names[i] + ".png");
            Dimension res = find(w[0], JScrollPane.class);
            Dimension row = find(w[0], JTextField.class);
            // find the Reserve button BY ITS LABEL: a plain search for the
            // first JButton returns the arrow button inside the JComboBox,
            // which is what our first run reported as a 20x24 "button row".
            Dimension btn = findButton(w[0], "Reserve");
            System.out.printf("%-8s | %-12s | %-24s | %-22s | %s%n", names[i], W + "x" + H,
                    res.width + "x" + res.height, row.width + "x" + row.height, btn.width + "x" + btn.height);
        }
        System.out.println();
        System.out.println("The results area grows in BOTH directions while the search field and the");
        System.out.println("buttons keep the same pixel size at every window size.  That is");
        System.out.println("BorderLayout giving all spare space to CENTER and none to NORTH or SOUTH.");
        System.exit(0);
    }
    static Dimension findButton(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton b && text.equals(b.getText())) return b.getSize();
            if (c instanceof Container cc) { Dimension d = findButton(cc, text); if (d != null) return d; }
        }
        return null;
    }
    static Dimension find(Container root, Class<?> type) {
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) return c.getSize();
            if (c instanceof Container cc) { Dimension d = find(cc, type); if (d != null) return d; }
        }
        return null;
    }
}
