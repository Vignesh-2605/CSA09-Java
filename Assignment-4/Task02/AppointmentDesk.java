/*
 * Programming in Java  -  Assignment 4 (CO4)
 * Task 2  -  Requirement to Event-Driven Design
 *
 * Hospital outpatient appointment desk.
 *
 * Every listener in this window also APPENDS TO AN EVENT LOG, so the
 * method-call sequence demanded by the task is recorded by the running
 * program rather than asserted from memory.  The captured log is in
 * event_trace.txt.
 *
 * Name: Vignesh S   Register Number: 192325002
 */
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class AppointmentDesk extends JFrame {

    /* ---------- the "database", loaded ONCE into a Map (see the comparison) ---------- */
    static final Map<String, List<String>> DOCTORS = new LinkedHashMap<>();
    static {
        DOCTORS.put("General Medicine", List.of("Dr Anand", "Dr Beena", "Dr Chandra"));
        DOCTORS.put("Orthopaedics",     List.of("Dr Devi", "Dr Eswar"));
        DOCTORS.put("Paediatrics",      List.of("Dr Farida", "Dr Ganesh", "Dr Hari"));
        DOCTORS.put("Cardiology",       List.of("Dr Iqbal"));
    }
    static final Set<String> SLOTS = new LinkedHashSet<>(
            List.of("09:00", "09:30", "10:00", "10:30", "11:00"));
    static final Set<String> booked = new HashSet<>();

    /* ---------- the event log ---------- */
    public static final List<String> LOG = Collections.synchronizedList(new ArrayList<>());
    private static long t0 = System.nanoTime();
    static void log(String source, String eventObject, String listener, String action) {
        LOG.add(String.format("%7.1f ms | %-22s | %-22s | %-24s | %s",
                (System.nanoTime() - t0) / 1e6, source, eventObject, listener, action));
    }

    /* ---------- components ---------- */
    private final JTextField patientField = new JTextField(14);
    private final JComboBox<String> department = new JComboBox<>(DOCTORS.keySet().toArray(new String[0]));
    private final JComboBox<String> doctor = new JComboBox<>();
    private final JComboBox<String> slot = new JComboBox<>(SLOTS.toArray(new String[0]));
    private final JButton bookBtn = new JButton("Book");
    private final JButton cancelBtn = new JButton("Cancel");
    private final JLabel status = new JLabel(" ");

    public AppointmentDesk() {
        super("Outpatient Appointment Desk");
        log("JFrame", "-", "-", "constructor runs, components created");

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 8, 4, 8); c.anchor = GridBagConstraints.WEST;
        int y = 0;
        c.gridx = 0; c.gridy = y;   form.add(new JLabel("Patient name"), c);
        c.gridx = 1; form.add(patientField, c);
        c.gridx = 0; c.gridy = ++y; form.add(new JLabel("Department"), c);
        c.gridx = 1; form.add(department, c);
        c.gridx = 0; c.gridy = ++y; form.add(new JLabel("Doctor"), c);
        c.gridx = 1; form.add(doctor, c);
        c.gridx = 0; c.gridy = ++y; form.add(new JLabel("Slot"), c);
        c.gridx = 1; form.add(slot, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelBtn); buttons.add(bookBtn);

        add(status, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        /* ---------- the four listener registrations ---------- */

        // 1. department changes -> refresh the doctor list
        department.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) return;   // ignore the DESELECTED half
            log("JComboBox department", "ItemEvent(SELECTED)", "ItemListener", "refreshDoctors()");
            refreshDoctors();
        });

        // 2. Book
        bookBtn.addActionListener(e -> {
            log("JButton Book", "ActionEvent", "ActionListener", "validateAndBook()");
            validateAndBook();
        });

        // 3. Cancel
        cancelBtn.addActionListener(e -> {
            log("JButton Cancel", "ActionEvent", "ActionListener", "clearForm()");
            clearForm();
        });

        // 4. Enter in the patient field also books
        patientField.addActionListener(e -> {
            log("JTextField patient", "ActionEvent", "ActionListener", "validateAndBook()");
            validateAndBook();
        });

        // 5. window events - the whole life cycle, logged
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e)  { log("JFrame", "WindowEvent", "WindowListener", "windowOpened()"); }
            @Override public void windowActivated(WindowEvent e){ log("JFrame", "WindowEvent", "WindowListener", "windowActivated()"); }
            @Override public void windowClosing(WindowEvent e)  { log("JFrame", "WindowEvent", "WindowListener", "windowClosing() - user asked to close"); dispose(); }
            @Override public void windowDeactivated(WindowEvent e){ log("JFrame", "WindowEvent", "WindowListener", "windowDeactivated()"); }
            @Override public void windowClosed(WindowEvent e)   { log("JFrame", "WindowEvent", "WindowListener", "windowClosed() - resources released"); }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        refreshDoctors();
        pack();
        setSize(Math.max(getWidth(), 400), getHeight());
    }

    /** The action behind "refresh the doctor list when the department changes". */
    private void refreshDoctors() {
        String dept = (String) department.getSelectedItem();
        doctor.removeAllItems();
        for (String d : DOCTORS.getOrDefault(dept, List.of())) doctor.addItem(d);
        status.setForeground(Color.DARK_GRAY);
        status.setText(doctor.getItemCount() + " doctor(s) in " + dept);
    }

    /** The action behind "validate and book an appointment". */
    private void validateAndBook() {
        String name = patientField.getText().trim();
        if (name.isEmpty())                 { fail("Patient name is required"); return; }
        if (!name.matches("[A-Za-z .]{2,40}")) { fail("Patient name may contain letters, spaces and dots only"); return; }
        if (doctor.getSelectedItem() == null) { fail("No doctor available in this department"); return; }
        String key = doctor.getSelectedItem() + "@" + slot.getSelectedItem();
        if (booked.contains(key))           { fail("That slot is already taken"); return; }
        booked.add(key);
        status.setForeground(new Color(0, 110, 0));
        status.setText("Booked " + name + " with " + doctor.getSelectedItem() + " at " + slot.getSelectedItem());
    }

    private void fail(String msg) { status.setForeground(new Color(170, 0, 0)); status.setText(msg); }
    private void clearForm() { patientField.setText(""); department.setSelectedIndex(0); status.setText(" "); }

    /* accessors for the harness */
    public void setPatient(String s) { patientField.setText(s); }
    public void selectDepartment(String s) { department.setSelectedItem(s); }
    public void clickBook() { bookBtn.doClick(); }
    public void clickCancel() { cancelBtn.doClick(); }
    public static void resetClock() { t0 = System.nanoTime(); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AppointmentDesk().setVisible(true));
    }
}
