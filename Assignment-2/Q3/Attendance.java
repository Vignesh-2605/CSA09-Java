/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q3  -  Engineer the hashCode-equals Contract  ("Complete the Code")
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * The given skeleton is unchanged apart from TODO 1.
 * Q3/BeforeFix.java is the SAME program with hashCode() still missing, kept so
 * the output before and after the fix can be compared side by side.
 */
import java.util.HashSet;
import java.util.Set;

class Student {
    private final String regNo;
    private final String name;

    Student(String regNo, String name) { this.regNo = regNo; this.name = name; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        return regNo.equals(((Student) o).regNo);
    }

    // TODO 1: Override hashCode() so that it is consistent with equals()
    //
    // equals() decides identity on regNo alone, so hashCode() must be derived
    // from regNo alone.  Any other field would let two objects that are equal
    // produce different hash codes, which is exactly the contract violation
    // this question is about.
    @Override public int hashCode() { return regNo.hashCode(); }

    @Override public String toString() { return regNo + " " + name; }
}

public class Attendance {
    public static void main(String[] args) {
        Set<Student> present = new HashSet<>();
        present.add(new Student("192311001", "Arun"));    // scanned at Gate 1
        present.add(new Student("192311002", "Divya"));
        present.add(new Student("192311001", "Arun"));    // scanned again at Gate 3
        System.out.println("Unique students: " + present.size());
    }
}
