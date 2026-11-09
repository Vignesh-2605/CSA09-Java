/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q3  -  the SAME program with TODO 1 left incomplete.
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * The brief asks for the output to be recorded BEFORE completing TODO 1.  This
 * file preserves that state so the two runs can be compared directly rather
 * than described from memory.
 */
import java.util.HashSet;
import java.util.Set;

class StudentBeforeFix {
    private final String regNo;
    private final String name;

    StudentBeforeFix(String regNo, String name) { this.regNo = regNo; this.name = name; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentBeforeFix)) return false;
        return regNo.equals(((StudentBeforeFix) o).regNo);
    }

    // TODO 1 deliberately NOT completed: hashCode() is inherited from Object,
    // which derives it from the object's identity, not from regNo.

    @Override public String toString() { return regNo + " " + name; }
}

public class AttendanceBeforeFix {
    public static void main(String[] args) {
        Set<StudentBeforeFix> present = new HashSet<>();
        StudentBeforeFix a1 = new StudentBeforeFix("192311001", "Arun");
        StudentBeforeFix d  = new StudentBeforeFix("192311002", "Divya");
        StudentBeforeFix a2 = new StudentBeforeFix("192311001", "Arun");
        present.add(a1);
        present.add(d);
        present.add(a2);
        System.out.println("Unique students: " + present.size());
        System.out.println();
        System.out.println("why: the two Arun scans are EQUAL but hash differently");
        System.out.printf("  a1.equals(a2)      = %s%n", a1.equals(a2));
        System.out.printf("  a1.hashCode()      = %d%n", a1.hashCode());
        System.out.printf("  a2.hashCode()      = %d%n", a2.hashCode());
        System.out.printf("  same hash?         = %s%n", a1.hashCode() == a2.hashCode());
        System.out.println();
        System.out.println("HashSet looks in the bucket chosen by hashCode() and only then");
        System.out.println("calls equals().  Different hash codes send the two scans to");
        System.out.println("different buckets, so equals() is never consulted and the");
        System.out.println("duplicate is stored as a second element.");
    }
}
