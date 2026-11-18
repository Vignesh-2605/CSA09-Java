/*
 * Programming in Java  -  Assignment 4 (CO4)
 * Task 4  -  the REPAIRED drawing pad.  TODO 1 and TODO 2 completed.
 *
 * Name: Vignesh S   Register Number: 192325002
 */
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class DrawingPadFixed extends JFrame {

    private int clicks = 0;
    private final JLabel counter = new JLabel("Clicks: 0");
    private final Pad pad = new Pad();

    class Pad extends JPanel {
        /* The pad now REMEMBERS what was drawn.  This single field is what
         * makes the picture survive a repaint. */
        private final List<Point> points = new ArrayList<>();

        Pad() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(460, 300));

            addMouseListener(new MouseAdapter() {
                /* ---------- TODO 1 COMPLETED ---------- */
                @Override                       // <- the compiler now checks the name
                public void mouseClicked(MouseEvent e) {
                    points.add(e.getPoint());   // 1. store the clicked point
                    clicks++;
                    counter.setText("Clicks: " + clicks);   // 2. update the label
                    repaint();                  // 3. ask Swing to repaint; do NOT draw here
                }
            });
        }

        /* ---------- TODO 2 COMPLETED ---------- */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);            // clears to the background colour
            g.setColor(Color.BLUE);
            // Every dot is redrawn from the stored list on EVERY repaint, so
            // minimising, restoring, covering, uncovering and resizing all
            // leave the picture intact.
            for (Point p : points) g.fillOval(p.x - 4, p.y - 4, 8, 8);
        }

        int storedPoints() { return points.size(); }
    }

    public DrawingPadFixed() {
        super("Drawing Pad - repaired");
        counter.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        add(counter, BorderLayout.NORTH);
        add(pad, BorderLayout.CENTER);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
    }

    public void clickAt(int x, int y) {
        MouseEvent e = new MouseEvent(pad, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, x, y, 1, false);
        for (MouseListener l : pad.getMouseListeners()) l.mouseClicked(e);
    }
    public String counterText() { return counter.getText(); }
    public int storedPoints()   { return pad.storedPoints(); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DrawingPadFixed().setVisible(true));
    }
}
