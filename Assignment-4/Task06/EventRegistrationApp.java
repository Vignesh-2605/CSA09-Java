/*
 * Programming in Java  -  Assignment 4 (CO4)
 * Task 6  -  Campus Event Registration: THE UI LAYER.
 *
 * Contains no SQL and no business rule.  It collects input, calls
 * RegistrationService, and displays what comes back.  Every database call is
 * made inside a SwingWorker so the Event Dispatch Thread is never blocked.
 *
 * Name: Vignesh S   Register Number: 192325002
 */
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

public class EventRegistrationApp extends JFrame {

    static final int PAGE_SIZE = 25;

    private final RegistrationService service;
    private final JTabbedPane tabs = new JTabbedPane();

    /* --- register screen --- */
    private final JComboBox<Model.CampusEvent> eventBox = new JComboBox<>();
    private final JTextField regNoField = new JTextField(12);
    private final JTextField nameField  = new JTextField(18);
    private final JTextField emailField = new JTextField(20);
    private final JButton registerBtn = new JButton("Register");
    private final JLabel registerStatus = new JLabel(" ");

    /* --- view screen --- */
    private final JComboBox<Model.CampusEvent> filterBox = new JComboBox<>();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Reg ID", "Register No", "Student", "Email", "Registered at"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final JButton prevBtn = new JButton("Prev");
    private final JButton nextBtn = new JButton("Next");
    private final JButton exportBtn = new JButton("Export CSV");
    private final JLabel viewStatus = new JLabel(" ");
    private final JProgressBar busy = new JProgressBar();
    private int page = 0, total = 0;

    public EventRegistrationApp(RegistrationService service) {
        super("Campus Event Registration");
        this.service = service;
        setJMenuBar(buildMenuBar());
        tabs.addTab("Register", buildRegisterPanel());
        tabs.addTab("Registrations", buildViewPanel());
        add(tabs, BorderLayout.CENTER);
        busy.setVisible(false);
        add(busy, BorderLayout.SOUTH);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (JOptionPane.showConfirmDialog(EventRegistrationApp.this,
                        "Close the registration desk?", "Confirm",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) dispose();
            }
        });
        setSize(900, 520);
        loadEvents();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem refresh = new JMenuItem("Refresh events");
        refresh.addActionListener(e -> loadEvents());
        JMenuItem export = new JMenuItem("Export current filter to CSV");
        export.addActionListener(e -> doExport());
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        file.add(refresh); file.add(export); file.addSeparator(); file.add(exit);
        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Campus Event Registration\nVignesh S - 192325002\nCSA09 Assignment 4 (CO4)",
                "About", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);
        bar.add(file); bar.add(help);
        return bar;
    }

    private JPanel buildRegisterPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 10, 6, 10); c.anchor = GridBagConstraints.WEST;
        int y = 0;
        c.gridx = 0; c.gridy = y;   form.add(new JLabel("Event"), c);         c.gridx = 1; form.add(eventBox, c);
        c.gridx = 0; c.gridy = ++y; form.add(new JLabel("Register number"), c); c.gridx = 1; form.add(regNoField, c);
        c.gridx = 0; c.gridy = ++y; form.add(new JLabel("Student name"), c);  c.gridx = 1; form.add(nameField, c);
        c.gridx = 0; c.gridy = ++y; form.add(new JLabel("Email"), c);         c.gridx = 1; form.add(emailField, c);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(registerBtn);
        registerBtn.addActionListener(e -> doRegister());
        regNoField.addActionListener(e -> doRegister());

        JPanel p = new JPanel(new BorderLayout());
        registerStatus.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        p.add(registerStatus, BorderLayout.NORTH);
        p.add(form, BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildViewPanel() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        top.add(new JLabel("Filter by event"));
        top.add(filterBox);
        filterBox.addActionListener(e -> { page = 0; loadPage(); });
        top.add(exportBtn);
        exportBtn.addActionListener(e -> doExport());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        bottom.add(prevBtn); bottom.add(nextBtn);
        prevBtn.addActionListener(e -> { if (page > 0) { page--; loadPage(); } });
        nextBtn.addActionListener(e -> { if ((page + 1) * PAGE_SIZE < total) { page++; loadPage(); } });

        JPanel south = new JPanel(new BorderLayout());
        viewStatus.setBorder(BorderFactory.createEmptyBorder(0, 12, 6, 12));
        south.add(bottom, BorderLayout.CENTER);
        south.add(viewStatus, BorderLayout.SOUTH);

        JPanel p = new JPanel(new BorderLayout());
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    /* ---------------- actions, every one off the EDT ---------------- */

    private void loadEvents() {
        new SwingWorker<List<Model.CampusEvent>, Void>() {
            @Override protected List<Model.CampusEvent> doInBackground() throws Exception { return service.events(); }
            @Override protected void done() {
                try {
                    eventBox.removeAllItems(); filterBox.removeAllItems();
                    for (Model.CampusEvent ev : get()) { eventBox.addItem(ev); filterBox.addItem(ev); }
                    loadPage();
                } catch (Exception ex) { registerStatus.setText("Could not load events: " + root(ex)); }
            }
        }.execute();
    }

    private void doRegister() {
        final Model.CampusEvent ev = (Model.CampusEvent) eventBox.getSelectedItem();
        final String reg = regNoField.getText(), name = nameField.getText(), mail = emailField.getText();
        registerBtn.setEnabled(false); busy.setVisible(true); busy.setIndeterminate(true);
        new SwingWorker<Long, Void>() {
            @Override protected Long doInBackground() throws Exception {
                return service.register(ev, reg, name, mail);
            }
            @Override protected void done() {
                registerBtn.setEnabled(true); busy.setVisible(false); busy.setIndeterminate(false);
                try {
                    long id = get();
                    ok(registerStatus, "Registered " + reg.trim() + " for " + ev.name() + "  (id " + id + ")");
                    regNoField.setText(""); nameField.setText(""); emailField.setText("");
                    if (filterBox.getSelectedItem() == ev) loadPage();
                } catch (Exception ex) {
                    Throwable t = ex.getCause() == null ? ex : ex.getCause();
                    bad(registerStatus, t.getMessage());
                    if (t instanceof RegistrationException re) focus(re.field());
                }
            }
        }.execute();
    }

    private void loadPage() {
        final Model.CampusEvent ev = (Model.CampusEvent) filterBox.getSelectedItem();
        if (ev == null) return;
        busy.setVisible(true); busy.setIndeterminate(true);
        new SwingWorker<List<Model.Registration>, Void>() {
            int count;
            @Override protected List<Model.Registration> doInBackground() throws Exception {
                count = service.count(ev.id());
                return service.page(ev.id(), page, PAGE_SIZE);   // ONE page, not the table
            }
            @Override protected void done() {
                busy.setVisible(false); busy.setIndeterminate(false);
                try {
                    total = count;
                    tableModel.setRowCount(0);
                    for (Model.Registration r : get())
                        tableModel.addRow(new Object[]{r.regId(), r.regNo(), r.studentName(), r.email(), r.registeredAt()});
                    int from = page * PAGE_SIZE;
                    viewStatus.setForeground(Color.DARK_GRAY);
                    viewStatus.setText(total == 0 ? "No registrations for " + ev.name()
                            : String.format("Showing %d-%d of %,d for %s",
                                    from + 1, Math.min(from + PAGE_SIZE, total), total, ev.name()));
                    prevBtn.setEnabled(page > 0);
                    nextBtn.setEnabled((page + 1) * PAGE_SIZE < total);
                } catch (Exception ex) { bad(viewStatus, "Could not load: " + root(ex)); }
            }
        }.execute();
    }

    private void doExport() {
        final Model.CampusEvent ev = (Model.CampusEvent) filterBox.getSelectedItem();
        if (ev == null) return;
        busy.setVisible(true); busy.setIndeterminate(true);
        new SwingWorker<Integer, Void>() {
            File out = new File("registrations-event-" + ev.id() + ".csv");
            @Override protected Integer doInBackground() throws Exception {
                int n = 0;
                try (PrintWriter w = new PrintWriter(out)) {
                    w.println("reg_id,register_no,student_name,email,registered_at");
                    int p = 0; List<Model.Registration> batch;
                    // exported a page at a time, so exporting 100,000 rows
                    // never holds 100,000 rows in memory
                    while (!(batch = service.page(ev.id(), p++, 1000)).isEmpty())
                        for (Model.Registration r : batch) {
                            w.printf("%d,%s,%s,%s,%s%n", r.regId(), r.regNo(),
                                    r.studentName().replace(",", " "), r.email(), r.registeredAt());
                            n++;
                        }
                }
                return n;
            }
            @Override protected void done() {
                busy.setVisible(false); busy.setIndeterminate(false);
                try { ok(viewStatus, "Exported " + String.format("%,d", get()) + " rows to " + out.getName()); }
                catch (Exception ex) { bad(viewStatus, "Export failed: " + root(ex)); }
            }
        }.execute();
    }

    private void focus(String field) {
        switch (field) {
            case "regNo" -> regNoField.requestFocusInWindow();
            case "name"  -> nameField.requestFocusInWindow();
            case "email" -> emailField.requestFocusInWindow();
            default -> eventBox.requestFocusInWindow();
        }
    }
    private static void ok(JLabel l, String m)  { l.setForeground(new Color(0, 110, 0)); l.setText(m); }
    private static void bad(JLabel l, String m) { l.setForeground(new Color(170, 0, 0)); l.setText(m); }
    private static String root(Exception e) {
        Throwable t = e; while (t.getCause() != null) t = t.getCause(); return String.valueOf(t.getMessage());
    }

    /* accessors for the screenshot harness */
    public void fill(String reg, String name, String mail) { regNoField.setText(reg); nameField.setText(name); emailField.setText(mail); }
    public void clickRegister() { registerBtn.doClick(); }
    public void showTab(int i)  { tabs.setSelectedIndex(i); }
    public void selectFilter(int idx) { filterBox.setSelectedIndex(idx); }
    public void nextPage() { nextBtn.doClick(); }
    public String registerStatusText() { return registerStatus.getText(); }
    public String viewStatusText() { return viewStatus.getText(); }

    public static void main(String[] args) {
        RegistrationService s = new RegistrationService(new RegistrationDao());
        SwingUtilities.invokeLater(() -> new EventRegistrationApp(s).setVisible(true));
    }
}
