/*
 * Programming in Java  -  Assignment 4 (CO4)
 * Task 3  -  From Sketch to Layout: the campus library search screen
 *
 * Nested panel structure and the layout manager chosen for each container:
 *
 *   JFrame  (BorderLayout)
 *     NORTH  -> titlePanel      FlowLayout(CENTER)   title only, keeps its height
 *     CENTER -> centrePanel     BorderLayout
 *                 NORTH -> searchRow   FlowLayout(LEFT)  field, Choice, Search
 *                 CENTER-> JScrollPane(JList)            THE PART THAT GROWS
 *     SOUTH  -> southPanel      BorderLayout
 *                 CENTER-> buttonRow   FlowLayout(CENTER) Prev, Next, Reserve
 *                 SOUTH -> statusLabel                    one line, fixed
 *
 * The requirement is that the results area grows on resize while the search
 * row and the bottom buttons keep their intended size.  BorderLayout gives
 * exactly that: it hands ALL spare space to its CENTER and leaves NORTH and
 * SOUTH at their preferred height.  That is why BorderLayout appears three
 * times and FlowLayout is used only for rows that must not stretch.
 *
 * Name: Vignesh S   Register Number: 192325002
 */
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class LibrarySearch extends JFrame {

    private static final String[] CATEGORIES = {"All", "Title", "Author", "Subject", "ISBN"};
    private final JTextField searchField = new JTextField(22);
    private final JComboBox<String> category = new JComboBox<>(CATEGORIES);
    private final JButton searchBtn = new JButton("Search");
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> results = new JList<>(model);
    private final JButton prevBtn = new JButton("Prev");
    private final JButton nextBtn = new JButton("Next");
    private final JButton reserveBtn = new JButton("Reserve");
    private final JLabel statusLabel = new JLabel(" ");

    /* a small catalogue so the screen has something real to show */
    private static final List<String[]> CATALOGUE = List.of(
        new String[]{"Java: The Complete Reference",   "Herbert Schildt",  "Programming"},
        new String[]{"Effective Java",                 "Joshua Bloch",     "Programming"},
        new String[]{"Head First Design Patterns",     "Eric Freeman",     "Programming"},
        new String[]{"Operating System Concepts",      "Silberschatz",     "Systems"},
        new String[]{"Computer Networks",              "Tanenbaum",        "Systems"},
        new String[]{"Introduction to Algorithms",     "Cormen",           "Algorithms"},
        new String[]{"Algorithm Design",               "Kleinberg",        "Algorithms"},
        new String[]{"Database System Concepts",       "Korth",            "Databases"},
        new String[]{"Clean Code",                     "Robert Martin",    "Programming"},
        new String[]{"Discrete Mathematics",           "Rosen",            "Mathematics"},
        new String[]{"Theory of Computation",          "Michael Sipser",   "Theory"},
        new String[]{"Artificial Intelligence",        "Russell and Norvig","AI"});

    private static final int PAGE = 5;
    private int page = 0;
    private List<String[]> matches = new ArrayList<>(CATALOGUE);

    public LibrarySearch() {
        super("Campus Library - Search");

        /* ---- NORTH: the title.  FlowLayout, because a title must stay its
         * own size and stay centred; it must NOT stretch to fill the width. */
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel title = new JLabel("CAMPUS LIBRARY CATALOGUE");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        titlePanel.add(title);

        /* ---- the search row.  FlowLayout(LEFT): the three controls keep
         * their preferred widths and stay packed to the left.  A GridLayout
         * here would stretch the Search button to a third of the window. */
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        searchRow.add(new JLabel("Search"));
        searchRow.add(searchField);
        searchRow.add(category);
        searchRow.add(searchBtn);

        /* ---- CENTRE: BorderLayout so the scroll pane in CENTER absorbs all
         * the spare height AND width when the window grows. */
        JPanel centrePanel = new JPanel(new BorderLayout());
        results.setVisibleRowCount(PAGE);
        results.setBorder(BorderFactory.createTitledBorder("Results"));
        centrePanel.add(searchRow, BorderLayout.NORTH);
        centrePanel.add(new JScrollPane(results), BorderLayout.CENTER);

        /* ---- SOUTH: the buttons keep their size (FlowLayout) and the status
         * line sits under them, one line high. */
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        buttonRow.add(prevBtn); buttonRow.add(nextBtn); buttonRow.add(reserveBtn);
        JPanel southPanel = new JPanel(new BorderLayout());
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 6, 10));
        southPanel.add(buttonRow, BorderLayout.CENTER);
        southPanel.add(statusLabel, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(titlePanel, BorderLayout.NORTH);
        add(centrePanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> doSearch());
        searchField.addActionListener(e -> doSearch());
        prevBtn.addActionListener(e -> { if (page > 0) { page--; showPage(); } });
        nextBtn.addActionListener(e -> { if ((page + 1) * PAGE < matches.size()) { page++; showPage(); } });
        reserveBtn.addActionListener(e -> {
            String sel = results.getSelectedValue();
            if (sel == null) { statusLabel.setForeground(new Color(170,0,0)); statusLabel.setText("Select a title to reserve"); }
            else { statusLabel.setForeground(new Color(0,110,0)); statusLabel.setText("Reserved: " + sel.split(" {2,}")[0].trim()); }
        });

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        showPage();
        setMinimumSize(new Dimension(520, 300));
        pack();
    }

    private void doSearch() {
        String q = searchField.getText().trim().toLowerCase();
        String cat = (String) category.getSelectedItem();
        matches = new ArrayList<>();
        for (String[] b : CATALOGUE) {
            boolean hit = switch (cat) {
                case "Title"   -> b[0].toLowerCase().contains(q);
                case "Author"  -> b[1].toLowerCase().contains(q);
                case "Subject" -> b[2].toLowerCase().contains(q);
                case "ISBN"    -> false;
                default        -> (b[0] + b[1] + b[2]).toLowerCase().contains(q);
            };
            if (q.isEmpty() || hit) matches.add(b);
        }
        page = 0;
        showPage();
    }

    private void showPage() {
        model.clear();
        int from = page * PAGE, to = Math.min(from + PAGE, matches.size());
        for (int i = from; i < to; i++) {
            String[] b = matches.get(i);
            model.addElement(String.format("%-38s  %-22s  %s", b[0], b[1], b[2]));
        }
        prevBtn.setEnabled(page > 0);
        nextBtn.setEnabled(to < matches.size());
        statusLabel.setForeground(Color.DARK_GRAY);
        statusLabel.setText(matches.isEmpty() ? "No titles matched"
                : String.format("Showing %d-%d of %d", from + 1, to, matches.size()));
        results.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LibrarySearch().setVisible(true));
    }
}
