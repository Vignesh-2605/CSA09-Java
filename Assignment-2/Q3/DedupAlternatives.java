/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q3  -  Alternative Concept and Complexity Analysis
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * "If a TreeSet<Student> with a Comparator on regNo, or a HashMap<String,
 * Student> keyed by regNo, were used instead of HashSet<Student>, what would
 * the de-duplication solution be?"
 */
import bench.Bench;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class DedupAlternatives {

    record Student(String regNo, String name) {
        @Override public boolean equals(Object o) {
            return o instanceof Student s && regNo.equals(s.regNo);
        }
        @Override public int hashCode() { return regNo.hashCode(); }
    }

    static final int[] SIZES = { 1_000, 20_000, 200_000 };
    static final int DUPLICATE_FACTOR = 3;          // each student scanned 3 times

    public static void main(String[] args) {
        System.out.println("Q3  -  ALTERNATIVE DE-DUPLICATION STRUCTURES");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");
        System.out.println();
        System.out.println("WHAT EACH SOLUTION LOOKS LIKE");
        System.out.println("-".repeat(112));
        System.out.println("  HashSet<Student>                 present.add(student)");
        System.out.println("     de-duplication comes from hashCode()+equals() on Student itself.");
        System.out.println();
        System.out.println("  TreeSet<Student> with Comparator  new TreeSet<>(comparing(Student::regNo))");
        System.out.println("     de-duplication comes from compare()==0.  equals() is NEVER called,");
        System.out.println("     so the class does not even need hashCode() to be correct - which is");
        System.out.println("     a trap, because the same class in a HashSet would then misbehave.");
        System.out.println();
        System.out.println("  HashMap<String,Student>           byReg.putIfAbsent(s.regNo(), s)");
        System.out.println("     de-duplication comes from the KEY, so Student needs neither equals()");
        System.out.println("     nor hashCode() at all - String already has correct ones.  This is");
        System.out.println("     the most robust of the three against a badly written entity class,");
        System.out.println("     and it keeps the first scan, which a Set does too but less visibly.");
        System.out.println();
        System.out.println("  PREDICTION: HashSet and HashMap should be indistinguishable - HashSet");
        System.out.println("  IS a HashMap with a constant value - and both O(1) average per scan.");
        System.out.println("  TreeSet should be measurably slower and should grow with n, because");
        System.out.println("  every insert walks log2(n) levels of a red-black tree comparing Strings.");
        System.out.println("  At n = 200,000 that is about 18 String comparisons per scan.");
        System.out.println();

        List<Bench.Result> all = Bench.list();
        for (int n : SIZES) {
            final int scans = n * DUPLICATE_FACTOR;
            final Student[] data = new Student[scans];
            for (int i = 0; i < scans; i++) {
                int id = i % n;
                data[i] = new Student(String.format("1923%06d", id), "S" + id);
            }
            System.out.printf("n = %s unique students, %s scans%n", Bench.fmt(n), Bench.fmt(scans));
            System.out.println("-".repeat(112));
            Bench.tableHead();

            Bench.Result hs = Bench.measure("HashSet<Student>", "de-duplicate", n, scans, () -> {
                Set<Student> set = new HashSet<>();
                for (Student s : data) set.add(s);
                sink(set.size());
            });
            Bench.Result ts = Bench.measure("TreeSet<Student>", "de-duplicate", n, scans, () -> {
                Set<Student> set = new TreeSet<>(Comparator.comparing(Student::regNo));
                for (Student s : data) set.add(s);
                sink(set.size());
            });
            Bench.Result hm = Bench.measure("HashMap<String,Student>", "de-duplicate", n, scans, () -> {
                Map<String, Student> m = new HashMap<>();
                for (Student s : data) m.putIfAbsent(s.regNo(), s);
                sink(m.size());
            });
            Bench.Result lm = Bench.measure("LinkedHashMap<String,..>", "de-duplicate", n, scans, () -> {
                Map<String, Student> m = new LinkedHashMap<>();
                for (Student s : data) m.putIfAbsent(s.regNo(), s);
                sink(m.size());
            });
            for (Bench.Result r : List.of(hs, ts, hm, lm)) { Bench.row(r); all.add(r); }

            // every structure must agree on the answer
            Set<Student> a = new HashSet<>(List.of(data));
            Set<Student> b = new TreeSet<>(Comparator.comparing(Student::regNo)); b.addAll(List.of(data));
            Map<String, Student> c = new HashMap<>();
            for (Student s : data) c.putIfAbsent(s.regNo(), s);
            System.out.printf("  unique students found: HashSet %s, TreeSet %s, HashMap %s  -  agree: %s%n%n",
                    Bench.fmt(a.size()), Bench.fmt(b.size()), Bench.fmt(c.size()),
                    a.size() == b.size() && b.size() == c.size() && a.size() == n);
        }

        System.out.println("COMPLEXITY AND SPACE FOR n UNIQUE STUDENTS");
        System.out.println("-".repeat(112));
        System.out.printf("%-28s | %-16s | %-16s | %-18s | %s%n",
                "STRUCTURE", "PER SCAN", "ORDERED OUTPUT?", "SPACE PER ENTRY", "NEEDS Student.hashCode?");
        System.out.println("-".repeat(112));
        String[][] rows = {
            {"HashSet<Student>",        "O(1) average",  "no",            "~36 B (HashMap.Node)", "YES - this is the trap"},
            {"TreeSet<Student>+Cmp",    "O(log n)",      "YES, by regNo", "~40 B (TreeMap.Entry)", "no - never calls equals"},
            {"HashMap<String,Student>", "O(1) average",  "no",            "~36 B + key reference", "no - String's is used"},
            {"LinkedHashMap<String,.>", "O(1) average",  "YES, by arrival","~44 B (+2 pointers)",  "no"},
        };
        for (String[] r : rows) System.out.printf("%-28s | %-16s | %-16s | %-18s | %s%n", r[0], r[1], r[2], r[3], r[4]);
        System.out.println();
        System.out.println("  O(1) average, not O(1) worst case: a HashMap bucket degrades to a list");
        System.out.println("  under collisions, and Java 8+ converts a chain of 8 to a red-black tree,");
        System.out.println("  so the true worst case is O(log n) rather than O(n) - but only because");
        System.out.println("  the key's compareTo is used, which is another place a badly written");
        System.out.println("  entity class causes trouble.");

        System.out.println();
        System.out.println("RECOMMENDATION FOR THIS PROBLEM");
        System.out.println("-".repeat(112));
        System.out.println("  The fest needs a COUNT of unique students and, in practice, a register");
        System.out.println("  to print.  On that basis:");
        System.out.println();
        System.out.println("    - if only the count matters: HashSet with the corrected Student. It");
        System.out.println("      is the fastest and the most direct expression of 'a set of students'.");
        System.out.println("    - if the register must print in roll-number order: TreeSet with a");
        System.out.println("      comparator on regNo, paying O(log n) per scan to avoid an O(n log n)");
        System.out.println("      sort at the end - worth it only if the register is printed often.");
        System.out.println("    - if the entity class is outside my control, or may be changed by");
        System.out.println("      someone else later: HashMap<String,Student> keyed by regNo. It");
        System.out.println("      de-duplicates on a String, whose equals/hashCode are correct by");
        System.out.println("      definition, so the de-duplication cannot be broken by an edit to");
        System.out.println("      Student.  Experiment 1 showed how silently that edit can happen.");

        Bench.writeCsv("Q3", all);
    }

    static long SINK;
    static void sink(long v) { SINK += v; }
}
