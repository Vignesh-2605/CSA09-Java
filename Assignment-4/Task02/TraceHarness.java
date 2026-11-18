/* Assignment 4 (CO4) Task 2 - opens the window, drives a real booking, closes
 * it, and prints the event log the listeners recorded.
 * Name: Vignesh S   Register Number: 192325002 */
import java.awt.event.WindowEvent;
import java.util.*;
import javax.swing.*;

public class TraceHarness {
    public static void main(String[] args) throws Exception {
        System.out.println("TASK 2  -  METHOD-CALL SEQUENCE, RECORDED BY THE RUNNING PROGRAM");
        System.out.println("=".repeat(118));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.println();
        AppointmentDesk.resetClock();
        final AppointmentDesk[] w = new AppointmentDesk[1];
        SwingUtilities.invokeAndWait(() -> { w[0] = new AppointmentDesk(); w[0].setLocation(70, 70); w[0].setVisible(true); });
        Shot.pause(800);
        Shot.of(w[0], "../shots/task02-1-opened.png");

        SwingUtilities.invokeAndWait(() -> w[0].selectDepartment("Paediatrics"));
        Shot.pause(300);
        Shot.of(w[0], "../shots/task02-2-department-changed.png");

        SwingUtilities.invokeAndWait(() -> w[0].setPatient(""));
        SwingUtilities.invokeAndWait(w[0]::clickBook);
        Shot.pause(250);
        Shot.of(w[0], "../shots/task02-3-validation-failed.png");

        SwingUtilities.invokeAndWait(() -> w[0].setPatient("Ravi Kumar"));
        SwingUtilities.invokeAndWait(w[0]::clickBook);
        Shot.pause(250);
        Shot.of(w[0], "../shots/task02-4-booked.png");

        SwingUtilities.invokeAndWait(w[0]::clickBook);     // same slot again
        Shot.pause(250);
        Shot.of(w[0], "../shots/task02-5-duplicate-slot.png");

        SwingUtilities.invokeAndWait(() ->
            w[0].dispatchEvent(new WindowEvent(w[0], WindowEvent.WINDOW_CLOSING)));
        Shot.pause(500);

        System.out.printf("%-9s | %-22s | %-22s | %-24s | %s%n",
                "AT", "EVENT SOURCE", "EVENT OBJECT", "LISTENER INTERFACE", "ACTION");
        System.out.println("-".repeat(118));
        for (String l : AppointmentDesk.LOG) System.out.println(l);
        System.out.println();
        System.out.printf("%d events recorded.%n", AppointmentDesk.LOG.size());
        System.exit(0);
    }
}
