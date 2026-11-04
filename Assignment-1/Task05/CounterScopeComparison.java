/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 5  -  Alternative Concept and Complexity Analysis
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Question: the ticket counter is a static variable.  If it were made an
 * INSTANCE variable of ParkingFacility, what would happen when the university
 * runs two facilities (the north gate and the south gate)?  Propose the right
 * solution for each case and analyse the space used by the counter for k
 * facility objects and n tickets.
 */
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CounterScopeComparison {

    /* ================= DESIGN A: one static counter, shared ================= */
    static class TicketA {
        private static int last = 1000;                 // ONE counter for the whole system
        final int no; final String gate; final String vehicleNo;
        TicketA(String gate, String vehicleNo) {
            this.no = ++last; this.gate = gate; this.vehicleNo = vehicleNo;
        }
    }
    static class FacilityA {
        final String name;
        FacilityA(String name) { this.name = name; }
        TicketA issue(String vehicleNo) { return new TicketA(name, vehicleNo); }
    }

    /* ============ DESIGN B: the counter moved into each facility ============ */
    static class TicketB {
        final int no; final String gate; final String vehicleNo;
        TicketB(int no, String gate, String vehicleNo) {
            this.no = no; this.gate = gate; this.vehicleNo = vehicleNo;
        }
    }
    static class FacilityB {
        private int last = 1000;                        // ONE counter PER facility object
        final String name;
        FacilityB(String name) { this.name = name; }
        TicketB issue(String vehicleNo) { return new TicketB(++last, name, vehicleNo); }
    }

    /* ===== DESIGN C: per-facility counter + a facility prefix (the fix) ===== */
    static class TicketC {
        final String no; final String gate; final String vehicleNo;
        TicketC(String no, String gate, String vehicleNo) {
            this.no = no; this.gate = gate; this.vehicleNo = vehicleNo;
        }
    }
    static class FacilityC {
        private int last = 1000;
        final String code;
        FacilityC(String code) { this.code = code; }
        TicketC issue(String vehicleNo) {
            return new TicketC(code + "-" + (++last), code, vehicleNo);  // globally unique
        }
    }

    public static void main(String[] args) {
        System.out.println("TASK 5  -  ALTERNATIVE: static COUNTER  vs  INSTANCE COUNTER");
        System.out.println("=".repeat(80));
        System.out.println();
        System.out.println("SCENARIO  the university opens a SECOND facility.  Both gates issue");
        System.out.println("          tickets on the same day; the accounts office merges the two");
        System.out.println("          ticket registers at midnight.");

        final int PER_GATE = 4;

        /* ---- A: static counter ---- */
        System.out.println();
        System.out.println("DESIGN A  -  static counter shared by every facility");
        System.out.println("-".repeat(80));
        FacilityA north = new FacilityA("NORTH"), south = new FacilityA("SOUTH");
        List<TicketA> mergedA = new ArrayList<>();
        for (int i = 1; i <= PER_GATE; i++) {
            mergedA.add(north.issue("TNN" + i));
            mergedA.add(south.issue("TNS" + i));
        }
        Set<Integer> seenA = new HashSet<>();
        int clashA = 0;
        for (TicketA t : mergedA) if (!seenA.add(t.no)) clashA++;
        System.out.printf("%-8s | %-10s | %s%n", "NUMBER", "GATE", "VEHICLE");
        for (TicketA t : mergedA) System.out.printf("%-8d | %-10s | %s%n", t.no, t.gate, t.vehicleNo);
        System.out.println("  distinct numbers = " + seenA.size() + " of " + mergedA.size()
                           + ", collisions = " + clashA + "  -> CORRECT");

        /* ---- B: instance counter ---- */
        System.out.println();
        System.out.println("DESIGN B  -  counter moved into ParkingFacility as an instance field");
        System.out.println("-".repeat(80));
        FacilityB northB = new FacilityB("NORTH"), southB = new FacilityB("SOUTH");
        List<TicketB> mergedB = new ArrayList<>();
        for (int i = 1; i <= PER_GATE; i++) {
            mergedB.add(northB.issue("TNN" + i));
            mergedB.add(southB.issue("TNS" + i));
        }
        Set<Integer> seenB = new HashSet<>();
        int clashB = 0;
        System.out.printf("%-8s | %-10s | %-10s | %s%n", "NUMBER", "GATE", "VEHICLE", "STATUS");
        for (TicketB t : mergedB) {
            boolean fresh = seenB.add(t.no);
            if (!fresh) clashB++;
            System.out.printf("%-8d | %-10s | %-10s | %s%n", t.no, t.gate, t.vehicleNo,
                              fresh ? "" : "COLLISION - number already issued at the other gate");
        }
        System.out.println("  distinct numbers = " + seenB.size() + " of " + mergedB.size()
                           + ", collisions = " + clashB + "  -> BROKEN");
        System.out.println();
        System.out.println("  Every gate restarts at 1001, so ticket #1001 exists twice.  The");
        System.out.println("  business rule 'a ticket number is never reused' is violated the moment");
        System.out.println("  the second facility issues its first ticket.  At the exit barrier a");
        System.out.println("  lookup by ticket number is now ambiguous, and the merged day register");
        System.out.println("  cannot tell which Rs 90 belongs to which vehicle.");

        /* ---- C: the fix ---- */
        System.out.println();
        System.out.println("DESIGN C  -  per-facility counter PLUS a facility code (the proposal)");
        System.out.println("-".repeat(80));
        FacilityC northC = new FacilityC("N"), southC = new FacilityC("S");
        List<TicketC> mergedC = new ArrayList<>();
        for (int i = 1; i <= PER_GATE; i++) {
            mergedC.add(northC.issue("TNN" + i));
            mergedC.add(southC.issue("TNS" + i));
        }
        Set<String> seenC = new HashSet<>();
        int clashC = 0;
        for (TicketC t : mergedC) if (!seenC.add(t.no)) clashC++;
        System.out.printf("%-10s | %-10s | %s%n", "NUMBER", "GATE", "VEHICLE");
        for (TicketC t : mergedC) System.out.printf("%-10s | %-10s | %s%n", t.no, t.gate, t.vehicleNo);
        System.out.println("  distinct numbers = " + seenC.size() + " of " + mergedC.size()
                           + ", collisions = " + clashC + "  -> CORRECT");

        System.out.println();
        System.out.println("PROPOSED SOLUTION FOR EACH CASE");
        System.out.println("-".repeat(80));
        System.out.printf("%-28s | %s%n", "CASE", "SOLUTION");
        System.out.println("-".repeat(80));
        System.out.printf("%-28s | %s%n", "one facility only",
                          "keep the static counter (design A)");
        System.out.printf("%-28s | %s%n", "two or more facilities",
                          "per-facility counter + facility code (C)");
        System.out.printf("%-28s | %s%n", "two facilities, one register",
                          "static counter is still correct, but it");
        System.out.printf("%-28s | %s%n", "",
                          "becomes a single point of contention");
        System.out.println();
        System.out.println("  Design C is the better long-term answer because it keeps the number");
        System.out.println("  unique WITHOUT coupling the two gates to one shared variable: each");
        System.out.println("  gate can issue tickets independently, and the code in the number says");
        System.out.println("  where the ticket came from, which the accounts office needs anyway.");

        System.out.println();
        System.out.println("SPACE USED BY THE COUNTER FOR k FACILITIES AND n TICKETS");
        System.out.println("-".repeat(80));
        System.out.printf("%-28s | %-18s | %-18s | %s%n",
                          "DESIGN", "COUNTER SPACE", "PER TICKET", "TOTAL");
        System.out.println("-".repeat(80));
        System.out.printf("%-28s | %-18s | %-18s | %s%n", "A  static",
                          "4 B, O(1)", "4 B int no", "4 + 4n bytes");
        System.out.printf("%-28s | %-18s | %-18s | %s%n", "B  instance",
                          "4k B, O(k)", "4 B int no", "4k + 4n bytes");
        System.out.printf("%-28s | %-18s | %-18s | %s%n", "C  instance + code",
                          "4k B, O(k)", "ref + String", "4k + ~40n bytes");
        System.out.println();
        int[] ks = { 1, 2, 5, 20 };
        int n = 100_000;
        System.out.println("  Counter space alone, for n = " + String.format("%,d", n) + " tickets:");
        System.out.printf("  %-6s | %-14s | %-14s | %s%n", "k", "A static", "B instance", "DIFFERENCE");
        for (int k : ks) {
            System.out.printf("  %-6d | %-14s | %-14s | %s%n", k, "4 B", (4 * k) + " B",
                              (4 * k - 4) + " B");
        }
        System.out.println();
        System.out.println("  The counter itself is negligible either way - at most a few dozen");
        System.out.println("  bytes for any realistic k.  Design C's real cost is on the TICKETS,");
        System.out.println("  not the counter: a String identifier such as \"N-1001\" costs roughly");
        System.out.println("  40 bytes against 4 for an int, so for n = " + String.format("%,d", n)
                           + " that is about");
        System.out.println("  " + (36L * n / 1024) + " KB more.  That is the price of making the");
        System.out.println("  number globally unique, and it is worth paying: a duplicated ticket");
        System.out.println("  number is a correctness failure, while 3.5 MB on a modern machine is");
        System.out.println("  not a constraint.");
        System.out.println();
        System.out.println("CONCLUSION");
        System.out.println("-".repeat(80));
        System.out.println("  The counter is static in THIS submission because the requirement");
        System.out.println("  describes one facility, and a static counter is then both correct and");
        System.out.println("  the cheapest option.  It is kept static rather than made an instance");
        System.out.println("  field because 'the next ticket number' is a fact about the ticketing");
        System.out.println("  SEQUENCE, not about any one ticket or any one facility object - and as");
        System.out.println("  design B shows, moving it into the object silently breaks uniqueness");
        System.out.println("  the first time a second facility is constructed.");
    }
}
