/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q6  -  Priority, Order and Fairness in a Queue  ("Complete the Code")
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * The given skeleton is unchanged apart from TODO 1.
 */
import java.util.Comparator;
import java.util.PriorityQueue;

class Patient {
    final String name; final int severity; final long arrival;
    Patient(String name, int severity, long arrival) {
        this.name = name; this.severity = severity; this.arrival = arrival;
    }
    @Override public String toString() { return name + "(S" + severity + ")"; }
}

public class Triage {
    private long counter = 0;

    // TODO 1: Create a PriorityQueue that serves the lowest severity number first and,
    //   for equal severity, the earliest arrival first
    //
    // Severity ascends because severity 1 is critical.  The arrival counter is
    // the tie-breaker: a PriorityQueue is NOT a stable structure, so without a
    // second key two patients of equal severity would come out in whatever
    // order the heap happened to hold them.
    private final PriorityQueue<Patient> queue = new PriorityQueue<>(
            Comparator.comparingInt((Patient p) -> p.severity)
                      .thenComparingLong(p -> p.arrival));

    void admit(String name, int severity) {
        queue.add(new Patient(name, severity, counter++));
    }

    Patient next() { return queue.poll(); }

    public static void main(String[] args) {
        Triage er = new Triage();
        er.admit("Ravi", 3); er.admit("Meena", 1); er.admit("John", 3);
        er.admit("Asha", 5); er.admit("Kumar", 1); er.admit("Lata", 2);
        StringBuilder order = new StringBuilder();
        Patient p;
        while ((p = er.next()) != null) order.append(p).append(' ');
        System.out.println(order.toString().trim());
    }
}
