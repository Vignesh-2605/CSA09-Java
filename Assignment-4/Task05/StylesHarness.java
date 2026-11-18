/* Assignment 4 (CO4) Task 5 - runs all four styles, captures a valid and an
 * invalid conversion from each, and measures how much code the wiring costs.
 * Name: Vignesh S   Register Number: 192325002 */
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

public class StylesHarness {
    record Style(String cls, String label) { }
    static final Style[] STYLES = {
        new Style("ConverterImplements", "frame implements ActionListener"),
        new Style("ConverterNamedInner", "named inner class"),
        new Style("ConverterAnonymous",  "anonymous inner class"),
        new Style("ConverterLambda",     "lambda expression"),
    };

    public static void main(String[] args) {
        try { body(); } catch (Throwable t) {
            t.printStackTrace();
            System.exit(2);          // never leave the EDT holding the JVM open
        }
        System.exit(0);
    }

    static void body() throws Exception {
        System.out.println("TASK 5  -  SAME SCREEN, FOUR EVENT STYLES");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.println();
        System.out.println("All four produce an identical window and identical results.  Only the");
        System.out.println("wiring between the component and the handler differs, so the code");
        System.out.println("measured below is exactly the cost of each style.");
        System.out.println();

        int i = 0;
        System.out.printf("%-24s | %-34s | %-16s | %s%n",
                "STYLE", "WIRING", "VALID 100 INR->USD", "INVALID \"12x\"");
        System.out.println("-".repeat(112));
        for (Style s : STYLES) {
            i++;
            Class<?> c = Class.forName(s.cls());
            final JFrame[] f = new JFrame[1];
            SwingUtilities.invokeAndWait(() -> {
                try { f[0] = (JFrame) c.getDeclaredConstructor().newInstance();
                      f[0].setLocation(60, 60); f[0].setVisible(true); }
                catch (Exception e) { throw new RuntimeException(e); }
            });
            Shot.pause(650);
            String valid   = fire(c, f[0], "100");
            Shot.of(f[0], "../shots/task05-" + i + "a-" + s.cls() + "-valid.png");
            String invalid = fire(c, f[0], "12x");
            Shot.of(f[0], "../shots/task05-" + i + "b-" + s.cls() + "-invalid.png");
            System.out.printf("%-24s | %-34s | %-16s | %s%n", s.cls(), s.label(),
                    valid.startsWith("100 INR") ? "OK" : "FAIL", invalid.contains("not a valid") ? "OK" : "FAIL");
            SwingUtilities.invokeAndWait(f[0]::dispose);
            Shot.pause(200);
        }

        System.out.println();
        System.out.println("HOW MUCH CODE EACH STYLE COSTS");
        System.out.println("-".repeat(112));
        System.out.printf("%-24s | %-12s | %-14s | %-16s | %s%n",
                "STYLE", "TOTAL LINES", "WIRING LINES", "EXTRA CLASSES", "TO ADD A THIRD TRIGGER");
        System.out.println("-".repeat(112));
        String[][] extra = {
            {"ConverterImplements", "another branch in the one handler"},
            {"ConverterNamedInner", "one more new ConvertHandler(...)"},
            {"ConverterAnonymous",  "another 5-line anonymous class"},
            {"ConverterLambda",     "one more line"},
        };
        for (String[] e : extra) {
            Path p = Path.of(e[0] + ".java");
            List<String> lines = Files.readAllLines(p);
            long total = lines.stream().filter(l -> !l.isBlank() && !l.strip().startsWith("*")
                    && !l.strip().startsWith("/*") && !l.strip().startsWith("//")).count();
            long wiring = lines.stream().filter(l -> l.contains("addActionListener")
                    || l.contains("actionPerformed") || l.contains("implements ActionListener")
                    || l.contains("class ConvertHandler") || l.contains("new ActionListener")).count();
            long classes = lines.stream().filter(l -> l.contains("class ConvertHandler")
                    || l.contains("new ActionListener")).count();
            System.out.printf("%-24s | %-12d | %-14d | %-16d | %s%n", e[0], total, wiring, classes, e[1]);
        }
        System.out.println();
        System.out.println("Counts exclude blank lines and comments.  \"Wiring lines\" counts the");
        System.out.println("lines that register or declare a handler; \"extra classes\" counts the");
        System.out.println("named or anonymous listener types each style creates.");
    }

    static Object grab(Class<?> c, JFrame f, String name) throws Exception {
        java.lang.reflect.Field fld = c.getDeclaredField(name);
        fld.setAccessible(true);
        return fld.get(f);
    }

    static String fire(Class<?> c, JFrame f, String amount) throws Exception {
        // getDeclaredField, not getField: these are package-private fields of
        // the demo classes, and getField only returns public ones.
        JTextField amt = (JTextField) grab(c, f, "amount");
        JButton btn    = (JButton)    grab(c, f, "convert");
        JLabel res     = (JLabel)     grab(c, f, "result");
        SwingUtilities.invokeAndWait(() -> amt.setText(amount));
        SwingUtilities.invokeAndWait(() -> {
            for (ActionListener l : btn.getActionListeners())
                l.actionPerformed(new ActionEvent(btn, ActionEvent.ACTION_PERFORMED, "Convert"));
        });
        Shot.pause(250);
        return res.getText();
    }
}
