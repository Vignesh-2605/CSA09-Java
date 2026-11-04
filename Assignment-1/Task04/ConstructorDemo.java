/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 4  -  Design Classes, Objects and Constructors
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Shows the default, parameterised, overloaded and copy constructors of
 * ParkingTicket, constructor chaining with this(...), the use of this to
 * disambiguate a field from a parameter, and how reference variables on the
 * stack point at objects on the heap.
 */

/** The proof of parking. Immutable once issued, apart from the exit time. */
class ParkingTicket {

    private static int lastTicketNo = 1000;     // class-wide, shared by all tickets

    private final int ticketNo;                 // final: set once, in a constructor
    private final String vehicleNo;
    private final int floor;
    private final int slot;
    private final int entryMinute;
    private int exitMinute = -1;                // -1 means the vehicle is still inside
    private final boolean duplicate;            // true only for a re-issued ticket

    /* ---- 1. FULL PARAMETERISED CONSTRUCTOR: the single real initialiser ---- */
    ParkingTicket(String vehicleNo, int floor, int slot, int entryMinute) {
        if (vehicleNo == null || vehicleNo.isBlank())
            throw new IllegalArgumentException("vehicle number is mandatory");
        if (floor < 0 || slot < 0)
            throw new IllegalArgumentException("floor and slot cannot be negative");
        if (entryMinute < 0 || entryMinute > 1439)
            throw new IllegalArgumentException("entry minute must be 0..1439");

        this.ticketNo    = ++lastTicketNo;      // this. is needed: field vs nothing
        this.vehicleNo   = vehicleNo;           // this. disambiguates field from parameter
        this.floor       = floor;
        this.slot        = slot;
        this.entryMinute = entryMinute;
        this.duplicate   = false;
        System.out.println("      [full constructor ran for " + vehicleNo + "]");
    }

    /* ---- 2. OVERLOADED CONSTRUCTOR: entry time defaults to 9:00 am -------- */
    ParkingTicket(String vehicleNo, int floor, int slot) {
        this(vehicleNo, floor, slot, 9 * 60);   // CHAINING: delegates, no repeated checks
        System.out.println("      [3-arg constructor chained to the full one]");
    }

    /* ---- 3. DEFAULT-STYLE CONSTRUCTOR: a walk-in with no slot yet --------- */
    ParkingTicket(String vehicleNo) {
        this(vehicleNo, 0, 0, 9 * 60);          // CHAINING again
        System.out.println("      [1-arg constructor chained to the full one]");
    }

    /* ---- 4. COPY CONSTRUCTOR: re-issuing a lost ticket ------------------- */
    ParkingTicket(ParkingTicket other) {
        if (other == null) throw new IllegalArgumentException("nothing to copy");
        this.ticketNo    = ++lastTicketNo;      // a NEW number: rule "never reused"
        this.vehicleNo   = other.vehicleNo;     // the stay itself is copied unchanged
        this.floor       = other.floor;
        this.slot        = other.slot;
        this.entryMinute = other.entryMinute;
        this.exitMinute  = other.exitMinute;
        this.duplicate   = true;                // marked, so the gate can see it is a copy
        System.out.println("      [copy constructor ran: " + other.ticketNo
                           + " re-issued as " + this.ticketNo + "]");
    }

    void close(int exitMinute) {
        if (exitMinute < this.entryMinute)
            throw new IllegalArgumentException("exit cannot precede entry");
        this.exitMinute = exitMinute;
    }

    int getTicketNo()  { return ticketNo; }
    String getVehicleNo() { return vehicleNo; }
    int getEntryMinute() { return entryMinute; }
    boolean isDuplicate() { return duplicate; }
    static int issued() { return lastTicketNo - 1000; }

    @Override public String toString() {
        return String.format("Ticket#%d %s at %d/%d entry=%d%s",
                ticketNo, vehicleNo, floor, slot, entryMinute, duplicate ? " (DUPLICATE)" : "");
    }
}

public class ConstructorDemo {

    /** Objects are passed by value OF THE REFERENCE: the callee can mutate the
     *  object, but cannot make the caller's variable point somewhere else. */
    static void closeIt(ParkingTicket t)   { t.close(700); }
    static void repoint(ParkingTicket t)   { t = new ParkingTicket("TN99ZZ9999"); }

    public static void main(String[] args) {
        System.out.println("TASK 4  -  CLASSES, OBJECTS AND CONSTRUCTORS");
        System.out.println("=".repeat(78));

        System.out.println();
        System.out.println("1.  THE FOUR CONSTRUCTORS, AND WHICH ONE IS USED WHEN");
        System.out.println("-".repeat(78));

        System.out.println("  a) full parameterised  - every fact known at the gate:");
        ParkingTicket t1 = new ParkingTicket("TN01AB1111", 0, 0, 540);
        System.out.println("     " + t1);

        System.out.println();
        System.out.println("  b) overloaded 3-arg    - slot known, entry time defaults to 09:00:");
        ParkingTicket t2 = new ParkingTicket("TN02CD2222", 1, 7);
        System.out.println("     " + t2);

        System.out.println();
        System.out.println("  c) overloaded 1-arg    - walk-in, slot assigned later:");
        ParkingTicket t3 = new ParkingTicket("TN03EF3333");
        System.out.println("     " + t3);

        System.out.println();
        System.out.println("  d) copy constructor    - driver lost the printed ticket:");
        ParkingTicket t4 = new ParkingTicket(t1);
        System.out.println("     original  " + t1);
        System.out.println("     re-issued " + t4);

        System.out.println();
        System.out.println("2.  WHY A COPY CONSTRUCTOR IS NEEDED HERE");
        System.out.println("-".repeat(78));
        System.out.println("  A duplicate must carry the SAME stay (vehicle, slot, entry time) so");
        System.out.println("  that the fee is unchanged, but a DIFFERENT ticket number, so that the");
        System.out.println("  rule 'a ticket number is never reused' still holds and the original");
        System.out.println("  cannot be presented a second time.");
        System.out.printf("  same vehicle?  %-6s   same entry?  %-6s   same number?  %s%n",
                          t1.getVehicleNo().equals(t4.getVehicleNo()),
                          t1.getEntryMinute() == t4.getEntryMinute(),
                          t1.getTicketNo() == t4.getTicketNo());
        System.out.println("  marked as a duplicate: " + t4.isDuplicate());
        System.out.println();
        System.out.println("  A plain assignment would NOT do this:");
        ParkingTicket alias = t1;                       // copies the reference, not the object
        System.out.println("     ParkingTicket alias = t1;");
        System.out.println("     alias == t1 ?  " + (alias == t1)
                           + "   -> one object with two names, not a copy");

        System.out.println();
        System.out.println("3.  CONSTRUCTOR CHAINING WITH this(...)");
        System.out.println("-".repeat(78));
        System.out.println("  The 1-arg and 3-arg constructors do no validation of their own; each");
        System.out.println("  delegates with this(...) to the full constructor, which is the ONE");
        System.out.println("  place the three guard clauses live.  The trace above shows the full");
        System.out.println("  constructor running FIRST in every case, because this(...) must be");
        System.out.println("  the first statement in a constructor body.");
        System.out.println();
        System.out.println("  Proof that the shared validation still protects the short forms:");
        String[][] bad = { {"blank vehicle number", ""}, };
        for (String[] b : bad) {
            try { new ParkingTicket(b[1]); }
            catch (IllegalArgumentException e) {
                System.out.printf("     %-24s -> REJECTED: %s%n", b[0], e.getMessage());
            }
        }
        try { new ParkingTicket("TN05IJ5555", -1, 0); }
        catch (IllegalArgumentException e) {
            System.out.printf("     %-24s -> REJECTED: %s%n", "negative floor", e.getMessage());
        }

        System.out.println();
        System.out.println("4.  REFERENCE VARIABLES, THE STACK AND THE HEAP");
        System.out.println("-".repeat(78));
        System.out.println("  t1, t2, t3, t4 and alias are reference VARIABLES; they live in the");
        System.out.println("  stack frame of main().  The ParkingTicket OBJECTS they name live on");
        System.out.println("  the heap.  alias and t1 hold the same address, so there are 4 objects");
        System.out.println("  on the heap but 5 references on the stack.");
        System.out.println();
        System.out.println("     stack (main)              heap");
        System.out.println("     t1    ----------------->  [Ticket#1001 TN01AB1111]  <--- alias");
        System.out.println("     t2    ----------------->  [Ticket#1002 TN02CD2222]");
        System.out.println("     t3    ----------------->  [Ticket#1003 TN03EF3333]");
        System.out.println("     t4    ----------------->  [Ticket#1004 TN01AB1111 DUP]");
        System.out.println("     alias ---- same address as t1");
        System.out.println();
        System.out.println("  static lastTicketNo is NOT in either place - it belongs to the class");
        System.out.println("  and lives in the method area, one copy however many tickets exist.");
        System.out.println("  Tickets issued so far: " + ParkingTicket.issued());

        System.out.println();
        System.out.println("5.  HOW OBJECTS ARE PASSED TO AND RETURNED FROM METHODS");
        System.out.println("-".repeat(78));
        System.out.println("  before closeIt(t2)   : " + t2);
        closeIt(t2);
        System.out.println("  after  closeIt(t2)   : exit time was set through the reference");
        System.out.println("  before repoint(t2)   : ticket no = " + t2.getTicketNo());
        repoint(t2);
        System.out.println("  after  repoint(t2)   : ticket no = " + t2.getTicketNo()
                           + "   (unchanged)");
        System.out.println();
        System.out.println("  Java passes the VALUE of the reference.  closeIt() could change the");
        System.out.println("  object because it followed the same address; repoint() only rebound");
        System.out.println("  its own local copy of the reference, so main()'s t2 still names the");
        System.out.println("  original object.  This is why a method cannot swap two objects for");
        System.out.println("  its caller, and why the facility returns a NEW ParkingTicket rather");
        System.out.println("  than filling in one the caller passed down.");
    }
}
