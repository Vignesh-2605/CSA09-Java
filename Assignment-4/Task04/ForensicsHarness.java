/* Assignment 4 (CO4) Task 4 - drives all three versions through the same
 * sequence: click five times, minimise, restore, photograph each stage.
 * Name: Vignesh S   Register Number: 192325002 */
import java.awt.*;
import javax.swing.*;

public class ForensicsHarness {
    static final int[][] PTS = {{80,70},{160,120},{240,80},{320,170},{400,120}};

    public static void main(String[] args) throws Exception {
        System.out.println("TASK 4  -  EVENT-HANDLING FORENSICS");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.println();
        System.out.println("The same five clicks are delivered to all three versions as real");
        System.out.println("MouseEvents dispatched to the registered listeners, then each window is");
        System.out.println("minimised and restored.");
        System.out.println();
        System.out.printf("%-34s | %-18s | %-20s | %s%n",
                "VERSION", "LABEL AFTER 5", "DOTS BEFORE", "DOTS AFTER REPAINT");
        System.out.println("-".repeat(112));

        run("DrawingPadBroken",   "as supplied", 1);
        run("DrawingPadHalfFixed","TODO 1 only", 2);
        run("DrawingPadFixed",    "TODO 1 + 2",  3);

        System.out.println();
        System.out.println("HOW TO READ THIS");
        System.out.println("  as supplied : the label never leaves 0 and nothing is ever drawn,");
        System.out.println("                because mouseClick is not mouseClicked and no listener");
        System.out.println("                method is ever invoked.");
        System.out.println("  TODO 1 only : the label counts and the dots appear - and the repaint");
        System.out.println("                erases every one of them, because getGraphics() paints");
        System.out.println("                pixels that nothing remembers.  A real restore, a window");
        System.out.println("                being uncovered, or a resize all do exactly this.");
        System.out.println("  TODO 1 + 2  : the label counts and the dots survive, because the");
        System.out.println("                points are stored and paintComponent redraws them.");
        System.exit(0);
    }

    static void run(String cls, String label, int idx) throws Exception {
        Class<?> c = Class.forName(cls);
        final JFrame[] f = new JFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            try { f[0] = (JFrame) c.getDeclaredConstructor().newInstance();
                  f[0].setLocation(50, 50); f[0].setVisible(true); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        Shot.pause(700);
        for (int[] p : PTS) {
            SwingUtilities.invokeAndWait(() -> {
                try { c.getMethod("clickAt", int.class, int.class).invoke(f[0], p[0], p[1]); }
                catch (Exception e) { throw new RuntimeException(e); }
            });
            Shot.pause(90);
        }
        Shot.pause(400);
        Shot.of(f[0], "../shots/task04-" + idx + "a-" + cls + "-before-minimise.png");
        int before = countBlue(f[0]);

        // Minimise and restore.  These screenshots are captured on a headless
        // X server (Xvfb) with NO window manager, and without a window manager
        // an iconify request is not honoured - so the restore that would
        // normally force a repaint never happens.  We record whether the
        // iconify took effect, and then force the SAME full repaint that a
        // real restore triggers, so the fault is exercised either way.
        SwingUtilities.invokeAndWait(() -> f[0].setExtendedState(Frame.ICONIFIED));
        Shot.pause(700);
        final boolean iconified = f[0].getExtendedState() == Frame.ICONIFIED;
        SwingUtilities.invokeAndWait(() -> { f[0].setExtendedState(Frame.NORMAL); f[0].toFront(); });
        Shot.pause(500);
        SwingUtilities.invokeAndWait(() -> f[0].getContentPane().repaint());
        Shot.pause(900);
        if (idx == 1) System.out.printf("  (window manager honoured iconify: %s)%n", iconified);
        Shot.of(f[0], "../shots/task04-" + idx + "b-" + cls + "-after-restore.png");
        int after = countBlue(f[0]);

        String lbl = (String) c.getMethod("counterText").invoke(f[0]);
        System.out.printf("%-34s | %-18s | %-20s | %s%n", cls + " (" + label + ")", lbl,
                before + " blue regions", after + " blue regions");
        SwingUtilities.invokeAndWait(f[0]::dispose);
        Shot.pause(250);
    }

    /** Counts distinct blue blobs by sampling the captured window pixels. */
    static int countBlue(JFrame f) throws Exception {
        Rectangle r = new Rectangle(f.getX(), f.getY(), f.getWidth(), f.getHeight());
        java.awt.image.BufferedImage img = new Robot().createScreenCapture(r);
        int found = 0;
        for (int[] p : PTS) {
            // the pad sits below the label; sample the dot's own centre
            int x = p[0], y = p[1] + (f.getHeight() - f.getContentPane().getHeight())
                               + f.getContentPane().getComponent(0).getHeight();
            if (x < img.getWidth() && y < img.getHeight()) {
                Color col = new Color(img.getRGB(x, y));
                if (col.getBlue() > 120 && col.getRed() < 100) found++;
            }
        }
        return found;
    }
}
