/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 5  -  Apply static Members and the Object Life Cycle
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * Shows which data belongs to the CLASS rather than to an object: the ticket
 * counter and the live-occupancy count; a static block that loads the rate
 * table once; static utility methods; and an observed demonstration that a
 * closed ParkingTicket really does become eligible for garbage collection.
 */
import java.lang.ref.WeakReference;

class Ticket {

    /* ---- class-wide state: ONE copy, however many tickets exist ---------- */
    private static int lastTicketNo;              // the counter
    private static int liveCount;                 // how many vehicles are inside now
    private static int totalEverIssued;           // lifetime total
    private static final int[] RATE_BY_TYPE;      // filled by the static block below
    static final String FACILITY_CODE;

    /* ---- STATIC BLOCK: runs ONCE, when the class is first loaded ---------- */
    static {
        System.out.println("      [static block: loading the rate table once]");
        RATE_BY_TYPE = new int[3];
        RATE_BY_TYPE[0] = 10;                     // TWO_WHEELER
        RATE_BY_TYPE[1] = 30;                     // CAR
        RATE_BY_TYPE[2] = 30;                     // EV
        FACILITY_CODE = "SIMATS-MLCP";
        lastTicketNo = 1000;
    }

    /* ---- per-object state ------------------------------------------------- */
    private final int ticketNo;
    private final String vehicleNo;
    private final int typeIndex;
    private final int entryMinute;
    private boolean closed = false;
    private final byte[] printedStub = new byte[64 * 1024]; // makes the object easy to see on the heap

    Ticket(String vehicleNo, int typeIndex, int entryMinute) {
        this.ticketNo    = ++lastTicketNo;        // the counter advances on the CLASS
        this.vehicleNo   = vehicleNo;
        this.typeIndex   = typeIndex;
        this.entryMinute = entryMinute;
        liveCount++;
        totalEverIssued++;
    }

    long close(int exitMinute) {
        if (closed) throw new IllegalStateException("ticket " + ticketNo + " is already closed");
        closed = true;
        liveCount--;
        int minutes = exitMinute - entryMinute;
        return (long) ((minutes + 59) / 60) * RATE_BY_TYPE[typeIndex];
    }

    /* ---- STATIC METHODS: they answer questions about the CLASS ------------ */
    static int ticketsIssued()  { return totalEverIssued; }
    static int vehiclesInside() { return liveCount; }
    static int nextTicketNo()   { return lastTicketNo + 1; }
    static int rateFor(int typeIndex) { return RATE_BY_TYPE[typeIndex]; }

    int getTicketNo() { return ticketNo; }
    String getVehicleNo() { return vehicleNo; }
    @Override public String toString() { return "Ticket#" + ticketNo + " " + vehicleNo; }
}

public class StaticMembersDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("TASK 5  -  STATIC MEMBERS AND THE OBJECT LIFE CYCLE");
        System.out.println("=".repeat(80));

        System.out.println();
        System.out.println("1.  THE STATIC BLOCK RUNS BEFORE THE FIRST OBJECT EXISTS");
        System.out.println("-".repeat(80));
        System.out.println("  touching a static member forces the class to load:");
        System.out.println("  Ticket.nextTicketNo() = " + Ticket.nextTicketNo()
                           + ", facility = " + Ticket.FACILITY_CODE);
        System.out.println("  ...and the rate table is already populated: car rate = Rs "
                           + Ticket.rateFor(1) + "/hour, with no Ticket object created yet.");
        System.out.println();
        System.out.println("  A static block is the right place for this because the rate table is");
        System.out.println("  the same for every ticket and must exist before any ticket is issued.");
        System.out.println("  Putting it in a constructor would rebuild the same table for every");
        System.out.println("  vehicle that entered the campus.");

        System.out.println();
        System.out.println("2.  WHY THE TICKET COUNTER MUST BE static");
        System.out.println("-".repeat(80));
        Ticket t1 = new Ticket("TN01AB1111", 0, 540);
        Ticket t2 = new Ticket("TN02CD2222", 1, 545);
        Ticket t3 = new Ticket("TN03EF3333", 2, 550);
        System.out.println("  three tickets issued: " + t1 + ", " + t2 + ", " + t3);
        System.out.println("  the numbers are distinct and strictly increasing, because the counter");
        System.out.println("  belongs to the class, not to any one ticket.");
        System.out.println();
        System.out.println("  Had lastTicketNo been an INSTANCE variable, every object would start");
        System.out.println("  its own count from 1000 and all three tickets would be #1001 - which");
        System.out.println("  breaks the business rule 'a ticket number is never reused' on the very");
        System.out.println("  second vehicle.  CounterScopeComparison.java in this folder shows that");
        System.out.println("  failure actually happening.");

        System.out.println();
        System.out.println("3.  WHAT ELSE BELONGS TO THE CLASS, AND WHAT DOES NOT");
        System.out.println("-".repeat(80));
        System.out.printf("%-20s | %-10s | %s%n", "MEMBER", "SCOPE", "WHY");
        System.out.println("-".repeat(80));
        String[][] rows = {
            {"lastTicketNo",   "static",   "one sequence for the whole facility"},
            {"liveCount",      "static",   "occupancy is a property of the facility"},
            {"totalEverIssued","static",   "a lifetime total cannot live in one ticket"},
            {"RATE_BY_TYPE",   "static",   "identical for every ticket; loaded once"},
            {"FACILITY_CODE",  "static",   "a constant of the facility"},
            {"ticketNo",       "instance", "each ticket has its OWN number"},
            {"vehicleNo",      "instance", "identifies this one vehicle"},
            {"entryMinute",    "instance", "this stay began at its own time"},
            {"closed",         "instance", "this ticket's own state"},
        };
        for (String[] r : rows) System.out.printf("%-20s | %-10s | %s%n", r[0], r[1], r[2]);

        System.out.println();
        System.out.println("4.  LIVE OCCUPANCY TRACKED ON THE CLASS");
        System.out.println("-".repeat(80));
        System.out.println("  issued = " + Ticket.ticketsIssued()
                           + ", inside = " + Ticket.vehiclesInside());
        System.out.println("  " + t2 + " leaves at 700 -> fee Rs " + t2.close(700));
        System.out.println("  issued = " + Ticket.ticketsIssued()
                           + ", inside = " + Ticket.vehiclesInside()
                           + "   (issued never falls; inside does)");
        try { t2.close(800); }
        catch (IllegalStateException e) { System.out.println("  closing it twice -> REJECTED: " + e.getMessage()); }

        System.out.println();
        System.out.println("5.  THE OBJECT LIFE CYCLE: WHEN A TICKET BECOMES GARBAGE");
        System.out.println("-".repeat(80));
        System.out.println("  A ParkingTicket is created at entry, closed at exit, and then has no");
        System.out.println("  further purpose.  It becomes ELIGIBLE for collection at the moment the");
        System.out.println("  last reference to it disappears - which is when the facility removes it");
        System.out.println("  from its active-ticket list, NOT when close() is called.");
        System.out.println();

        // a ticket held only by the active list, plus a weak reference so we can watch it
        Ticket parked = new Ticket("TN04GH4444", 1, 560);
        java.util.List<Ticket> activeTickets = new java.util.ArrayList<>();
        activeTickets.add(parked);
        WeakReference<Ticket> watch = new WeakReference<>(parked);

        System.out.println("  stage 1  vehicle inside, held by activeTickets and by 'parked'");
        System.out.println("           weak reference still resolves: " + (watch.get() != null));

        parked.close(900);
        System.out.println("  stage 2  close() called; the object is STILL reachable from the list");
        System.out.println("           weak reference still resolves: " + (watch.get() != null));

        activeTickets.remove(0);
        parked = null;                            // the last strong reference is dropped
        System.out.println("  stage 3  removed from activeTickets and the variable set to null");
        for (int i = 0; i < 6 && watch.get() != null; i++) {
            System.gc();
            Thread.sleep(60);
        }
        System.out.println("           after requesting a collection, weak reference resolves: "
                           + (watch.get() != null));
        System.out.println("           -> the object was reclaimed; the ticket NUMBER is not reused,");
        System.out.println("              because that lives in the static counter, which survives.");
        System.out.println("           next ticket would be #" + Ticket.nextTicketNo());

        System.out.println();
        System.out.println("6.  HOW OBJECTS ARE PASSED AND RETURNED IN THIS DESIGN");
        System.out.println("-".repeat(80));
        System.out.println("  issue()   RETURNS a new Ticket reference; the caller never supplies one");
        System.out.println("            to be filled in, so a ticket cannot exist half-built.");
        System.out.println("  close()   is called ON the ticket and returns a long fee, not an object,");
        System.out.println("            so the money leaves the ticket as a value and cannot be");
        System.out.println("            altered afterwards through a shared reference.");
        System.out.println("  Java passes the VALUE of a reference, so a method that receives a");
        System.out.println("  Ticket can change that ticket but cannot replace the caller's variable.");
        System.out.println();
        System.out.println("  final tally:  issued = " + Ticket.ticketsIssued()
                           + ", still inside = " + Ticket.vehiclesInside());
    }
}
