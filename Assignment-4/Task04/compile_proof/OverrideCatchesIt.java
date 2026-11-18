/* Deliberately does NOT compile.  The ONLY difference from the faulty
 * handler in DrawingPadBroken.java is the @Override annotation.
 * Name: Vignesh S   Register Number: 192325002 */
import java.awt.event.*;

public class OverrideCatchesIt {
    static MouseAdapter listener = new MouseAdapter() {
        @Override                               // <- this is the whole difference
        public void mouseClick(MouseEvent e) {  // misspelled: should be mouseClicked
            System.out.println("clicked");
        }
    };
}
