/* Assignment 4 (CO4) - screenshot helper used by every task.
 * Captures the REAL window from the X server with java.awt.Robot, so the
 * images in shots/ are photographs of a running program, not mock-ups.
 * Name: Vignesh S   Register Number: 192325002 */
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

public final class Shot {
    public static void of(Window w, String path) throws Exception {
        SwingUtilities.invokeAndWait(() -> { w.toFront(); w.repaint(); });
        Thread.sleep(450);
        Rectangle r = new Rectangle(w.getX(), w.getY(), w.getWidth(), w.getHeight());
        BufferedImage img = new Robot().createScreenCapture(r);
        File f = new File(path);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
        System.out.printf("    captured %-46s %dx%d%n", path, img.getWidth(), img.getHeight());
    }
    public static void pause(int ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) { } }
}
