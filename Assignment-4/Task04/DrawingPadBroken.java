/*
 * Programming in Java  -  Assignment 4 (CO4)
 * Task 4  -  Event-Handling Forensics: the drawing pad AS SUPPLIED.
 *
 * NOTE ON PROVENANCE: the assignment deck supplied to me is the summary
 * version and does not contain the skeleton source.  This file reconstructs
 * the faulty program exactly as the task describes it - a pad that should
 * draw a dot at every mouse click and update a click counter, whose click
 * handler does not run, whose label does not update, and which loses its
 * drawing when the window is minimised and restored.  The two TODO markers
 * are placed where the task says they are, and both are completed in
 * DrawingPadFixed.java.
 *
 * Name: Vignesh S   Register Number: 192325002
 */
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class DrawingPadBroken extends JFrame {

    private int clicks = 0;
    private final JLabel counter = new JLabel("Clicks: 0");
    private final Pad pad = new Pad();

    class Pad extends JPanel {
        Pad() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(460, 300));

            addMouseListener(new MouseAdapter() {
                /*
                 * FAULT 1 - THE HANDLER THAT NEVER RUNS.
                 *
                 * The method is called mouseClick.  The interface method is
                 * mouseClicked.  MouseAdapter has no abstract methods, so
                 * this is not an override at all - it is a brand new method
                 * that nothing ever calls.  The file compiles cleanly and the
                 * pad is simply dead to the mouse.
                 *
                 * TODO 1: make this handler actually run, record the clicked
                 *         point, update the counter label, and ask the pad to
                 *         repaint itself.
                 */
                public void mouseClick(MouseEvent e) {
                    clicks++;
                    // FAULT 2 - drawing straight onto the live Graphics.
                    // Anything drawn this way exists only in the pixels
                    // currently on screen.  The moment the window is
                    // minimised and restored - or covered and uncovered, or
                    // resized - Swing repaints the panel from paintComponent
                    // and every dot is gone, because nothing recorded them.
                    Graphics g = getGraphics();
                    g.setColor(Color.BLUE);
                    g.fillOval(e.getX() - 4, e.getY() - 4, 8, 8);

                    // FAULT 3 - the label is never updated, so even if the
                    // handler did run the counter would stay at zero.
                }
            });
        }

        /*
         * TODO 2: draw every stored point here, so that the picture survives
         *         a repaint.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // nothing is drawn: the pad has no memory of what was clicked
        }
    }

    public DrawingPadBroken() {
        super("Drawing Pad - as supplied (faulty)");
        counter.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        add(counter, BorderLayout.NORTH);
        add(pad, BorderLayout.CENTER);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
    }

    /* used by the harness to deliver real MouseEvents */
    public void clickAt(int x, int y) {
        MouseEvent e = new MouseEvent(pad, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, x, y, 1, false);
        for (MouseListener l : pad.getMouseListeners()) l.mouseClicked(e);
    }
    public String counterText() { return counter.getText(); }
    public JPanel pad() { return pad; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DrawingPadBroken().setVisible(true));
    }
}
