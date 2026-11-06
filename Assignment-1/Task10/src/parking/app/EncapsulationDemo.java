package parking.app;

import parking.model.Car;
import parking.model.Chargeable;
import parking.model.ElectricCar;
import parking.model.ParkingTicket;
import parking.model.TwoWheeler;
import parking.model.Vehicle;
import parking.service.Facility;

/**
 * Programming in Java - Assignment 1 (CO1), Task 10
 * Name: Vignesh S   Register Number: 192325002
 *
 * The APPLICATION package.  It is the only package a user runs, and it can see
 * almost nothing: it names model types and calls service operations, and every
 * attempt to reach past those is a compile error (see compile_errors/).
 */
public class EncapsulationDemo {

    public static void main(String[] args) {
        System.out.println("TASK 10  -  PACKAGES, ACCESS MODIFIERS AND ENCAPSULATION");
        System.out.println("=".repeat(80));

        System.out.println();
        System.out.println("1.  PACKAGE STRUCTURE AND THE DIRECTION OF DEPENDENCY");
        System.out.println("-".repeat(80));
        System.out.println("  parking.model    Vehicle, TwoWheeler, Car, ElectricCar, Chargeable,");
        System.out.println("                   Slot, SlotFactory, ParkingTicket");
        System.out.println("                   -> WHAT the system is.  Depends on nothing.");
        System.out.println("  parking.service  Facility, SlotGrid (package-private),");
        System.out.println("                   SlotRow (package-private)");
        System.out.println("                   -> HOW the operations work.  Depends on model only.");
        System.out.println("  parking.app      EncapsulationDemo, AutoboxingRiskDemo");
        System.out.println("                   -> the gate console.  Depends on model + service.");
        System.out.println();
        System.out.println("      parking.app  ---->  parking.service  ---->  parking.model");
        System.out.println();
        System.out.println("  The arrows point one way only.  parking.model never imports service");
        System.out.println("  or app, so the whole domain compiles and can be tested with no");
        System.out.println("  facility and no user interface present at all.");

        System.out.println();
        System.out.println("2.  ACCESS MODIFIER CHOSEN FOR EACH MEMBER, AND WHY");
        System.out.println("-".repeat(80));
        System.out.printf("%-28s | %-12s | %s%n", "MEMBER", "MODIFIER", "REASON");
        System.out.println("-".repeat(80));
        String[][] mods = {
            {"Vehicle.regNo",          "private",   "identity; no code anywhere may change it"},
            {"Vehicle(String,int)",    "protected", "only subclasses build vehicles"},
            {"Vehicle.ratePerHour()",  "protected", "a raw rate is a model-internal detail"},
            {"Vehicle.dailyCap()",     "protected", "specialisable by subclasses only"},
            {"Vehicle.calculateFee()", "public final","service must bill; rule must not vary"},
            {"Vehicle.getRegNo()",     "public",    "everyone may read identity"},
            {"Slot.occupy() / free()", "default",   "only parking.model may change occupancy"},
            {"Slot.Slot(int,int)",     "default",   "slots are created through SlotFactory"},
            {"SlotGrid, SlotRow",      "default",   "storage layout is a service-internal detail"},
            {"Facility.grid",          "private",   "the layout cannot be reached or replaced"},
            {"ParkingTicket fields",   "private",   "a ticket is read-only once issued"},
            {"setExitMinute(int)",     "public",    "the one mutator, and it validates"},
        };
        for (String[] r : mods) System.out.printf("%-28s | %-12s | %s%n", r[0], r[1], r[2]);

        System.out.println();
        System.out.println("3.  THE FACILITY WORKING THROUGH THE PUBLIC API ONLY");
        System.out.println("-".repeat(80));
        Facility f = new Facility(new int[]{2, 3});
        System.out.println("  facility built: capacity " + f.capacity()
                           + ", free " + f.freeSlots());

        Vehicle bike = new TwoWheeler(" tn01ab1111 ");       // note the untidy input
        Vehicle car  = new Car("TN02CD2222");
        ElectricCar ev = new ElectricCar("TN03EF3333", 40);

        System.out.println("  constructor normalised \" tn01ab1111 \" to \""
                           + bike.getRegNo() + "\"");
        System.out.println();
        System.out.printf("%-14s | %-12s | %-10s | %s%n", "VEHICLE", "TICKET", "SLOT", "RESULT");
        System.out.println("-".repeat(80));
        ParkingTicket t1 = f.admit(bike, 540);
        System.out.printf("%-14s | %-12s | %-10s | %s%n", bike.getRegNo(),
                          "#" + t1.getTicketNo(), t1.getSlot().toString(), "admitted");
        ParkingTicket t2 = f.admit(car, 545);
        System.out.printf("%-14s | %-12s | %-10s | %s%n", car.getRegNo(),
                          "#" + t2.getTicketNo(), t2.getSlot().toString(), "admitted");
        ParkingTicket t3 = f.admit(ev, 550);
        System.out.printf("%-14s | %-12s | %-10s | %s%n", ev.getRegNo(),
                          "#" + t3.getTicketNo(), t3.getSlot().toString(), "admitted");
        System.out.println("  inside = " + f.vehiclesInside() + ", free = " + f.freeSlots());

        System.out.println();
        System.out.println("4.  VALIDATION DONE BY THE GETTERS AND SETTERS");
        System.out.println("-".repeat(80));
        System.out.printf("%-46s | %s%n", "ATTEMPT", "OUTCOME");
        System.out.println("-".repeat(80));
        attempt("new Car(\"\")                    blank plate", () -> new Car(""));
        attempt("new Car(null)                  null plate", () -> new Car(null));
        attempt("new ElectricCar(\"TN9\", 0)      zero battery", () -> new ElectricCar("TN9", 0));
        attempt("t1.setExitMinute(400)          before entry", () -> t1.setExitMinute(400));
        attempt("t1.setExitMinute(2000)         not a time", () -> t1.setExitMinute(2000));
        attempt("ev.chargingFee(-5)             negative units", () -> ev.chargingFee(-5));
        attempt("bike.calculateFee(-30)         negative stay", () -> bike.calculateFee(-30));
        attempt("f.admit(bike, 600)             already inside", () -> f.admit(bike, 600));
        attempt("new Facility(new int[]{0})     empty floor", () -> new Facility(new int[]{0}));
        attempt("new Facility(new int[]{})      no floors", () -> new Facility(new int[]{}));

        System.out.println();
        System.out.println("  Each guard lives in exactly ONE place - the setter or the constructor");
        System.out.println("  of the class that owns the field - so no caller can skip it and no");
        System.out.println("  caller has to remember it.  That is the practical payoff of private");
        System.out.println("  fields: the check cannot be bypassed because the field cannot be");
        System.out.println("  reached.");

        System.out.println();
        System.out.println("5.  A VALID STAY, END TO END");
        System.out.println("-".repeat(80));
        System.out.printf("%-14s | %-10s | %-10s | %-8s | %s%n",
                          "VEHICLE", "ENTRY", "EXIT", "PASS?", "FEE");
        System.out.println("-".repeat(80));
        long fee1 = f.release(t1, 625, false);          // 85 min, two-wheeler, visitor
        System.out.printf("%-14s | %-10d | %-10d | %-8s | Rs %d%n", bike.getRegNo(), 540, 625, "no", fee1);
        long fee2 = f.release(t2, 675, false);          // 130 min, car, visitor
        System.out.printf("%-14s | %-10d | %-10d | %-8s | Rs %d%n", car.getRegNo(), 545, 675, "no", fee2);
        long fee3 = f.release(t3, 680, true);           // staff with a pass
        System.out.printf("%-14s | %-10d | %-10d | %-8s | Rs %d%n", ev.getRegNo(), 550, 680, "yes", fee3);
        System.out.println("-".repeat(80));
        System.out.println("  collected = Rs " + (fee1 + fee2 + fee3)
                           + ", inside = " + f.vehiclesInside()
                           + ", free = " + f.freeSlots() + " of " + f.capacity());
        System.out.println("  tickets issued in this run: " + ParkingTicket.issued());
        System.out.println();
        System.out.println("  charging is billed separately, through the capability interface:");
        Chargeable c = ev;
        System.out.printf("  ev drew 10 kWh -> Rs %.0f at Rs %.0f/kWh, needs a charging slot: %s%n",
                          c.chargingFee(10), Chargeable.RATE_PER_KWH, c.needsChargingSlot());

        System.out.println();
        System.out.println("6.  WHAT THIS PACKAGE CANNOT DO");
        System.out.println("-".repeat(80));
        System.out.println("  Each line below is a COMPILE ERROR, captured for real in");
        System.out.println("  compile_errors/compiler_errors.txt:");
        System.out.println("     t1.getSlot().free();          - free() is package-private to model");
        System.out.println("     new Slot(0, 0);               - the constructor is package-private");
        System.out.println("     car.ratePerHour();            - protected, not visible here");
        System.out.println("     new SlotGrid(new int[]{2});   - the class is not public");
        System.out.println();
        System.out.println("  Those four errors ARE the encapsulation.  An application that cannot");
        System.out.println("  free a slot behind the facility's back, cannot invent a slot, cannot");
        System.out.println("  read a raw tariff and cannot see the storage layout can only drive the");
        System.out.println("  system through operations that keep its invariants - which is why the");
        System.out.println("  slot array and the fee rule can both be replaced in Task 12 without");
        System.out.println("  touching a single line of this file.");
    }

    private static void attempt(String label, Runnable r) {
        System.out.printf("%-46s | ", label);
        try { r.run(); System.out.println("accepted"); }
        catch (RuntimeException e) {
            System.out.println("REJECTED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
}
