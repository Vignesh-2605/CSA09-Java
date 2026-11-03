/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 1  -  Understand the Problem and Identify the Objects
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * This program turns the written analysis into executable form.  Each candidate
 * class identified from the problem statement is present as a minimal class, and
 * each business rule is enforced and then deliberately violated so that the
 * guard can be seen to fire.
 */
import java.util.ArrayList;
import java.util.List;

/* ---------- candidate classes: the nouns of the problem ---------- */

/** A physical, numbered place where exactly one vehicle may stand. */
class Slot {
    private final int floor;
    private final int number;
    private String occupantRegNo;            // null => free

    Slot(int floor, int number) { this.floor = floor; this.number = number; }

    boolean isFree() { return occupantRegNo == null; }

    /** Business rule 1: a slot cannot hold two vehicles. */
    void occupy(String regNo) {
        if (!isFree()) {
            throw new IllegalStateException(
                "Rule 1 violated: slot " + this + " already holds " + occupantRegNo);
        }
        occupantRegNo = regNo;
    }

    void free() { occupantRegNo = null; }

    @Override public String toString() { return floor + "/" + number; }
}

/** A vehicle presenting itself at the gate. */
class VehicleRecord {
    private final String regNo;
    private final String type;               // TWO_WHEELER | CAR | EV

    VehicleRecord(String regNo, String type) {
        if (regNo == null || regNo.isBlank()) {
            throw new IllegalArgumentException("Rule 4 violated: registration number is mandatory");
        }
        this.regNo = regNo; this.type = type;
    }

    String getRegNo() { return regNo; }
    String getType()  { return type; }
}

/** The proof of parking issued at entry and surrendered at exit. */
class Ticket {
    private static int counter = 1000;       // class-wide, never reused
    private final int ticketNo;
    private final VehicleRecord vehicle;
    private final Slot slot;
    private final int entryMinute;

    Ticket(VehicleRecord vehicle, Slot slot, int entryMinute) {
        this.ticketNo = ++counter;           // Business rule 2: a number is never reused
        this.vehicle = vehicle; this.slot = slot; this.entryMinute = entryMinute;
    }

    int getTicketNo()    { return ticketNo; }
    Slot getSlot()       { return slot; }
    int getEntryMinute() { return entryMinute; }
    VehicleRecord getVehicle() { return vehicle; }
}

/** The person the vehicle belongs to; decides whether a fee is payable. */
class User {
    private final String name;
    private final String category;           // STUDENT | STAFF | VISITOR
    private final boolean hasMonthlyPass;

    User(String name, String category, boolean hasMonthlyPass) {
        this.name = name; this.category = category; this.hasMonthlyPass = hasMonthlyPass;
    }

    boolean paysFee() { return !(hasMonthlyPass && !"VISITOR".equals(category)); }
    String getName()  { return name; }
    String getCategory() { return category; }
}

/** The facility: owns the slots and the active tickets, and runs the operations. */
class Facility {
    private final List<Slot> allSlots = new ArrayList<>();
    private final List<Ticket> active  = new ArrayList<>();

    Facility(int[] slotsPerFloor) {
        for (int f = 0; f < slotsPerFloor.length; f++) {
            for (int s = 0; s < slotsPerFloor[f]; s++) allSlots.add(new Slot(f, s));
        }
    }

    /** Business rule 3: the same vehicle cannot be parked twice at the same time. */
    Ticket admit(VehicleRecord v, int entryMinute) {
        for (Ticket t : active) {
            if (t.getVehicle().getRegNo().equals(v.getRegNo())) {
                throw new IllegalStateException(
                    "Rule 3 violated: " + v.getRegNo() + " is already inside on ticket "
                    + t.getTicketNo());
            }
        }
        for (Slot s : allSlots) {
            if (s.isFree()) {
                s.occupy(v.getRegNo());
                Ticket t = new Ticket(v, s, entryMinute);
                active.add(t);
                return t;
            }
        }
        return null;                         // facility full
    }

    /** Business rule 5: a slot is released only against a ticket the facility issued. */
    int release(Ticket t, int exitMinute) {
        if (!active.remove(t)) {
            throw new IllegalStateException(
                "Rule 5 violated: ticket " + t.getTicketNo() + " is not an active ticket");
        }
        t.getSlot().free();
        return exitMinute - t.getEntryMinute();
    }

    int freeSlots() {
        int n = 0;
        for (Slot s : allSlots) if (s.isFree()) n++;
        return n;
    }
}

/* ---------- the demonstration ---------- */

public class ProblemAnalysisDemo {

    private static void rule(String label, Runnable action) {
        System.out.printf("%-58s", label);
        try { action.run(); System.out.println("accepted"); }
        catch (RuntimeException e) { System.out.println("REJECTED  -> " + e.getMessage()); }
    }

    public static void main(String[] args) {
        System.out.println("TASK 1  -  OBJECTS IDENTIFIED FROM THE PROBLEM STATEMENT");
        System.out.println("=".repeat(78));
        System.out.printf("%-16s | %-10s | %s%n", "CANDIDATE CLASS", "FROM NOUN", "RESPONSIBILITY");
        System.out.println("-".repeat(78));
        String[][] model = {
            {"Facility",      "facility", "owns floors and slots; runs entry and exit"},
            {"Slot",          "slot",     "knows its position and its single occupant"},
            {"VehicleRecord", "vehicle",  "identity and category of what is parked"},
            {"Ticket",        "ticket",   "unique number, slot and entry time"},
            {"User",          "student",  "category and pass; decides if a fee is due"},
        };
        for (String[] r : model) System.out.printf("%-16s | %-10s | %s%n", r[0], r[1], r[2]);

        System.out.println();
        System.out.println("VERBS THAT BECOME METHODS");
        System.out.println("-".repeat(78));
        String[][] verbs = {
            {"enters / allocate", "Facility.admit(VehicleRecord, int)"},
            {"issue a ticket",    "new Ticket(...) with a class-wide counter"},
            {"leaves / release",  "Facility.release(Ticket, int)"},
            {"compute the fee",   "Vehicle.calculateFee(int)  (Tasks 7-8)"},
        };
        for (String[] r : verbs) System.out.printf("  %-20s -> %s%n", r[0], r[1]);

        System.out.println();
        System.out.println("INPUTS  : registration number, vehicle type, user category, pass flag,");
        System.out.println("          entry time, exit time, facility layout");
        System.out.println("OUTPUTS : allocated floor/slot, ticket number, duration, payable fee,");
        System.out.println("          refusal message when the facility is full or input is invalid");

        System.out.println();
        System.out.println("BUSINESS RULES, EACH TESTED AGAINST THE MODEL");
        System.out.println("=".repeat(78));

        Facility f = new Facility(new int[]{2, 1});       // 3 slots in total
        VehicleRecord bike = new VehicleRecord("TN01AB1111", "TWO_WHEELER");
        VehicleRecord car  = new VehicleRecord("TN02CD2222", "CAR");
        VehicleRecord ev   = new VehicleRecord("TN03EF3333", "EV");

        Ticket t1 = f.admit(bike, 540);
        System.out.println("entry  : " + bike.getRegNo() + " -> slot " + t1.getSlot()
                           + ", ticket " + t1.getTicketNo());
        Ticket t2 = f.admit(car, 545);
        System.out.println("entry  : " + car.getRegNo() + " -> slot " + t2.getSlot()
                           + ", ticket " + t2.getTicketNo());
        System.out.println("tickets are consecutive and never reused: "
                           + t1.getTicketNo() + " then " + t2.getTicketNo());
        System.out.println();

        rule("Rule 1  a slot cannot hold two vehicles",
             () -> t1.getSlot().occupy("TN99XX9999"));
        rule("Rule 3  the same vehicle cannot be admitted twice",
             () -> f.admit(new VehicleRecord("TN01AB1111", "TWO_WHEELER"), 600));
        rule("Rule 4  a registration number is mandatory",
             () -> new VehicleRecord("  ", "CAR"));

        Ticket t3 = f.admit(ev, 550);
        System.out.println();
        System.out.println("entry  : " + ev.getRegNo() + " -> slot " + t3.getSlot()
                           + ", ticket " + t3.getTicketNo());
        System.out.println("free slots now: " + f.freeSlots());
        VehicleRecord extra = new VehicleRecord("TN04GH4444", "CAR");
        System.out.println("entry  : " + extra.getRegNo() + " -> "
                           + (f.admit(extra, 560) == null ? "REFUSED, facility is full" : "admitted"));

        int stay = f.release(t1, 700);
        System.out.println();
        System.out.println("exit   : ticket " + t1.getTicketNo() + " released slot "
                           + t1.getSlot() + " after " + stay + " minutes");
        rule("Rule 5  a ticket cannot be surrendered twice",
             () -> f.release(t1, 720));

        System.out.println();
        System.out.println("USERS AND WHO PAYS");
        System.out.println("-".repeat(78));
        User[] users = { new User("Vignesh S", "STUDENT", true),
                         new User("Dr Ramesh",  "STAFF",   true),
                         new User("Guest",      "VISITOR", true),
                         new User("Arun",       "STUDENT", false) };
        System.out.printf("%-12s | %-8s | %s%n", "NAME", "CATEGORY", "FEE PAYABLE");
        for (User u : users) {
            System.out.printf("%-12s | %-8s | %s%n", u.getName(), u.getCategory(),
                              u.paysFee() ? "yes, full fee" : "no, valid monthly pass");
        }

        System.out.println();
        System.out.println("ASSUMPTIONS MADE");
        System.out.println("-".repeat(78));
        String[] assumptions = {
            "Slot numbering starts at 0 on every floor and is contiguous.",
            "Any vehicle type fits any slot in the base design; Task 12 adds typed slots.",
            "Time is held as whole minutes from midnight, so no date arithmetic is needed.",
            "A monthly pass is valid for the whole stay; it is not checked for expiry here.",
            "One gate operator acts at a time, so the model is single-threaded.",
        };
        for (int i = 0; i < assumptions.length; i++)
            System.out.println("  " + (i + 1) + ". " + assumptions[i]);
    }
}
