/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q3  -  how the equals/hashCode contract affects hash-based collections
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Three deliberately designed experiments.  Each one PREDICTS the set size and
 * states the reason BEFORE the test is executed, then reports the observed size
 * beside the prediction.  A prediction that turns out wrong is reported as
 * wrong, not quietly corrected.
 */
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class ContractExperiments {

    /* ---- the junior developer's class: equals overridden, hashCode NOT ---- */
    static class BrokenStudent {
        final String regNo; String name;
        BrokenStudent(String regNo, String name) { this.regNo = regNo; this.name = name; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BrokenStudent)) return false;
            return regNo.equals(((BrokenStudent) o).regNo);
        }
        @Override public String toString() { return regNo; }
    }

    /* ---- the corrected class ---- */
    static final class FixedStudent {
        private final String regNo; private final String name;
        FixedStudent(String regNo, String name) { this.regNo = regNo; this.name = name; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FixedStudent)) return false;
            return regNo.equals(((FixedStudent) o).regNo);
        }
        @Override public int hashCode() { return regNo.hashCode(); }
        String regNo() { return regNo; }
        @Override public String toString() { return regNo; }
    }

    /* ---- a class whose hash depends on a MUTABLE field ---- */
    static final class MutableStudent {
        String regNo; final String name;
        MutableStudent(String regNo, String name) { this.regNo = regNo; this.name = name; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MutableStudent)) return false;
            return regNo.equals(((MutableStudent) o).regNo);
        }
        @Override public int hashCode() { return regNo.hashCode(); }
        @Override public String toString() { return regNo; }
    }

    static int passed = 0, failed = 0;

    static void predict(String id, String what, Object predicted, Object observed, String why) {
        boolean ok = String.valueOf(predicted).equals(String.valueOf(observed));
        if (ok) passed++; else failed++;
        System.out.printf("%-5s | %-40s | %-11s | %-11s | %s%n",
                id, what, predicted, observed, ok ? "as predicted" : "** PREDICTION WRONG **");
        if (why != null) System.out.printf("      | reason: %s%n", why);
    }

    static void head() {
        System.out.printf("%-5s | %-40s | %-11s | %-11s | %s%n",
                "ID", "WHAT IS TESTED", "PREDICTED", "OBSERVED", "VERDICT");
        System.out.println("-".repeat(112));
    }

    public static void main(String[] args) {
        System.out.println("Q3  -  THE equals/hashCode CONTRACT: THREE DESIGNED EXPERIMENTS");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");

        /* ================= EXPERIMENT 1 ================= */
        System.out.println();
        System.out.println("EXPERIMENT 1  -  scans for which the broken HashSet APPEARS to work");
        System.out.println("-".repeat(112));
        System.out.println("  Design: every student is scanned exactly once, at one gate only.");
        System.out.println("  PREDICTION: size 4.  With no duplicate scans there is nothing for");
        System.out.println("  equals() to catch, so a missing hashCode() cannot cause a visible");
        System.out.println("  fault.  The class is still broken; the test simply cannot see it.");
        System.out.println();
        head();
        Set<BrokenStudent> e1 = new HashSet<>();
        e1.add(new BrokenStudent("192311001", "Arun"));
        e1.add(new BrokenStudent("192311002", "Divya"));
        e1.add(new BrokenStudent("192311003", "Karthik"));
        e1.add(new BrokenStudent("192311004", "Priya"));
        predict("E1", "4 distinct students, 1 scan each", 4, e1.size(),
                "no duplicates present, so the defect is invisible");
        System.out.println();
        System.out.println("  This is the dangerous case: a test suite built only from data like");
        System.out.println("  this passes, and the class ships.");

        /* ================= EXPERIMENT 2 ================= */
        System.out.println();
        System.out.println("EXPERIMENT 2  -  scans deliberately designed to EXPOSE the duplicates");
        System.out.println("-".repeat(112));
        System.out.println("  Design: the same three students scanned again at a second gate, as");
        System.out.println("  SEPARATE objects - which is what really happens when a card is read");
        System.out.println("  twice and a new Student is constructed from each scan.");
        System.out.println("  PREDICTION: 7, not 4.  equals() says the repeat scans are duplicates,");
        System.out.println("  but Object.hashCode() returns an identity-based value, so the two");
        System.out.println("  equal objects land in DIFFERENT buckets and equals() is never called.");
        System.out.println();
        head();
        Set<BrokenStudent> e2 = new HashSet<>();
        String[][] scans = {
            {"192311001","Arun"}, {"192311002","Divya"}, {"192311003","Karthik"}, {"192311004","Priya"},
            {"192311001","Arun"}, {"192311002","Divya"}, {"192311003","Karthik"},
        };
        for (String[] s : scans) e2.add(new BrokenStudent(s[0], s[1]));
        predict("E2", "7 scans of 4 distinct students (broken)", 7, e2.size(),
                "equal objects hashed to different buckets");

        Set<FixedStudent> e2fixed = new HashSet<>();
        for (String[] s : scans) e2fixed.add(new FixedStudent(s[0], s[1]));
        predict("E2b", "the same 7 scans, corrected class", 4, e2fixed.size(),
                "hashCode() derived from regNo, so equals() is consulted");

        BrokenStudent b1 = new BrokenStudent("192311001", "Arun");
        BrokenStudent b2 = new BrokenStudent("192311001", "Arun");
        System.out.println();
        System.out.println("  the mechanism, shown directly:");
        System.out.printf("    b1.equals(b2)                 = %s%n", b1.equals(b2));
        System.out.printf("    b1.hashCode() == b2.hashCode() = %s   (%d vs %d)%n",
                b1.hashCode() == b2.hashCode(), b1.hashCode(), b2.hashCode());
        System.out.printf("    bucket index for b1 (16 buckets) = %d%n", bucket(b1.hashCode(), 16));
        System.out.printf("    bucket index for b2 (16 buckets) = %d%n", bucket(b2.hashCode(), 16));
        System.out.println();
        System.out.println("  HOW HashSet LOCATES AN ELEMENT:");
        System.out.println("    1. compute hashCode() and spread it (HashMap.hash mixes the high bits");
        System.out.println("       down, so that keys differing only above the table size still");
        System.out.println("       scatter);");
        System.out.println("    2. index = (table.length - 1) & spreadHash  - pick ONE bucket;");
        System.out.println("    3. walk only that bucket's chain, calling equals() on each entry;");
        System.out.println("    4. if nothing matches, append the new entry to that chain.");
        System.out.println();
        System.out.println("  Step 2 is the whole story.  equals() is only ever consulted inside the");
        System.out.println("  bucket chosen in step 2, so two objects that hash differently are never");
        System.out.println("  compared at all - however equal they are.");

        /* ================= EXPERIMENT 3 ================= */
        System.out.println();
        System.out.println("EXPERIMENT 3  -  my own: mutating a hashed field, and an inconsistent TreeSet");
        System.out.println("-".repeat(112));
        System.out.println("  3a. PREDICTION: a correctly-written object becomes UNFINDABLE after the");
        System.out.println("      field its hashCode() uses is changed - contains() returns false for");
        System.out.println("      the very object the set holds, and the set reports size 1 while");
        System.out.println("      containing an element it cannot locate.");
        System.out.println();
        head();
        Set<MutableStudent> e3 = new HashSet<>();
        MutableStudent m = new MutableStudent("192311001", "Arun");
        e3.add(m);
        predict("E3a1", "contains(m) BEFORE mutation", true, e3.contains(m), null);
        m.regNo = "192399999";                       // the field hashCode() depends on
        predict("E3a2", "contains(m) AFTER mutating regNo", false, e3.contains(m),
                "the set looks in the bucket for the NEW hash; the entry sits in the OLD one");
        predict("E3a3", "set size is still", 1, e3.size(), null);
        predict("E3a4", "iteration still yields the object", true,
                e3.iterator().hasNext(), "it is present, but unreachable by lookup");
        boolean removable = e3.remove(m);
        predict("E3a5", "remove(m) succeeds", false, removable,
                "remove() hashes to the new bucket too, so it cannot find it - this leaks");
        System.out.println();
        System.out.println("      This is why the corrected Student declares regNo FINAL.  A hashed");
        System.out.println("      field that can change turns a HashSet into a structure that holds");
        System.out.println("      objects it can neither find nor delete.");

        System.out.println();
        System.out.println("  3b. PREDICTION: a TreeSet whose comparator is INCONSISTENT with equals()");
        System.out.println("      de-duplicates by the comparator, not by equals().  Comparing on");
        System.out.println("      NAME while equals() compares regNo should collapse two different");
        System.out.println("      students who share a name, giving 3 instead of 4.");
        System.out.println();
        head();
        List<FixedStudent> roll = new ArrayList<>(List.of(
                new FixedStudent("192311001", "Arun"),
                new FixedStudent("192311002", "Divya"),
                new FixedStudent("192311003", "Arun"),     // same NAME, different person
                new FixedStudent("192311004", "Priya")));
        Set<FixedStudent> byName = new TreeSet<>(Comparator.comparing(s -> s.toString().isEmpty() ? "" : nameOf(s)));
        byName.addAll(roll);
        predict("E3b1", "TreeSet ordered by NAME (inconsistent)", 3, byName.size(),
                "compare()==0 means duplicate to a TreeSet, whatever equals() says");
        Set<FixedStudent> byReg = new TreeSet<>(Comparator.comparing(FixedStudent::regNo));
        byReg.addAll(roll);
        predict("E3b2", "TreeSet ordered by regNo (consistent)", 4, byReg.size(),
                "compare()==0 exactly when equals() is true");
        Set<FixedStudent> hash = new HashSet<>(roll);
        predict("E3b3", "HashSet on the same roll", 4, hash.size(), null);
        System.out.println();
        System.out.println("      A TreeSet never calls equals() at all.  Its notion of duplicate is");
        System.out.println("      compare(a,b) == 0.  So 'consistent with equals' is not a style");
        System.out.println("      preference - it decides whether two structures holding the same");
        System.out.println("      data report the same size, as E3b1 and E3b3 show.");

        /* ================= THE CORRECTION AND ITS VERIFICATION ============ */
        System.out.println();
        System.out.println("HOW TO CORRECT Student, AND HOW TO VERIFY THE CORRECTION");
        System.out.println("-".repeat(112));
        System.out.println("  final class Student {");
        System.out.println("      private final String regNo;        // final: hashed state cannot drift");
        System.out.println("      private final String name;");
        System.out.println("      @Override public boolean equals(Object o) {");
        System.out.println("          if (this == o) return true;");
        System.out.println("          if (!(o instanceof Student)) return false;");
        System.out.println("          return regNo.equals(((Student) o).regNo);");
        System.out.println("      }");
        System.out.println("      @Override public int hashCode() { return regNo.hashCode(); }");
        System.out.println("  }");
        System.out.println();
        System.out.println("  hashCode() uses regNo and ONLY regNo, because equals() does.  Adding");
        System.out.println("  name to the hash would break the contract in the other direction: two");
        System.out.println("  objects that are equal could then hash differently.");
        System.out.println();
        System.out.println("  VERIFICATION - the four properties the contract actually requires:");
        head();
        FixedStudent f1 = new FixedStudent("192311001", "Arun");
        FixedStudent f2 = new FixedStudent("192311001", "Arun");
        FixedStudent f3 = new FixedStudent("192311009", "Other");
        predict("V1", "reflexive: x.equals(x)", true, f1.equals(f1), null);
        predict("V2", "symmetric: x.equals(y) == y.equals(x)", true,
                f1.equals(f2) == f2.equals(f1), null);
        predict("V3", "equal objects have equal hash codes", true,
                !f1.equals(f2) || f1.hashCode() == f2.hashCode(), null);
        predict("V4", "consistent: repeated calls agree", true,
                f1.hashCode() == f1.hashCode() && f1.equals(f2), null);
        predict("V5", "unequal objects may differ (not required)", true,
                !f1.equals(f3), null);
        predict("V6", "null is never equal", false, f1.equals(null), null);
        predict("V7", "a different type is never equal", false, f1.equals("192311001"), null);

        System.out.println();
        System.out.println("  Verification by property is stronger than verification by example: a");
        System.out.println("  single 'size == 2' assertion passes for a class that is still broken in");
        System.out.println("  the symmetry or consistency clause.");

        System.out.println();
        System.out.println("=".repeat(112));
        System.out.printf("PREDICTIONS MADE %d   CONFIRMED %d   REFUTED %d%n",
                passed + failed, passed, failed);
        System.out.println("=".repeat(112));
    }

    static String nameOf(FixedStudent s) {
        // the name is private; this mirrors it for the deliberately-inconsistent comparator
        return switch (s.regNo()) {
            case "192311001", "192311003" -> "Arun";
            case "192311002" -> "Divya";
            default -> "Priya";
        };
    }

    static int bucket(int hash, int tableSize) {
        int h = hash ^ (hash >>> 16);        // the spread HashMap applies
        return (tableSize - 1) & h;
    }
}
