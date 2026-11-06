package parking.app;

import parking.model.*;
import parking.service.ParkingFacility;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Programming in Java - Assignment 1 (CO1), Task 11
 * Name: Vignesh S   Register Number: 192325002
 *
 * The interactive gate console.  Reads commands with Scanner, so the same
 * program serves an operator at a keyboard and a reproducible file run:
 *
 *   java -cp out parking.app.ParkingApp                 (interactive)
 *   java -cp out parking.app.ParkingApp < input.txt     (reproducible)
 *
 * COMMANDS
 *   IN   <plate> <type> <category> <pass:Y|N> <minute>
 *   OUT  <plate> <minute> [kWh]
 *   LIST
 *   REPORT
 *   QUIT
 *   type     = BIKE | CAR | EV
 *   category = STUDENT | STAFF | VISITOR
 */
public class ParkingApp {

    /** Open tickets, keyed by plate: the structure Task 11's analysis recommends. */
    private static final Map<String, ParkingTicket> openTickets = new LinkedHashMap<>();

    public static void main(String[] args) {
        ParkingFacility facility = new ParkingFacility("SIMATS-MLCP", new int[]{2, 3, 2});
        Scanner in = new Scanner(System.in);

        System.out.println("SIMATS CAMPUS SMART-PARKING  -  GATE CONSOLE");
        System.out.println("=".repeat(78));
        System.out.println("facility " + facility.getCode() + ": " + facility.floors()
                           + " floors, " + facility.capacity() + " slots");
        System.out.println();

        int line = 0, ok = 0, rejected = 0;
        while (in.hasNext()) {
            String command = in.next().toUpperCase();
            line++;
            try {
                switch (command) {
                    case "IN":     ok += doIn(facility, in)  ? 1 : 0; break;
                    case "OUT":    ok += doOut(facility, in) ? 1 : 0; break;
                    case "LIST":   doList(facility); ok++; break;
                    case "REPORT": doReport(facility); ok++; break;
                    case "QUIT":   System.out.println("\ngate closed.");
                                   doReport(facility);
                                   System.out.println("commands accepted " + ok
                                                      + ", rejected " + rejected);
                                   in.close();
                                   return;
                    default:
                        System.out.println("line " + line + ": unknown command '" + command + "'");
                        rejected++;
                        in.nextLine();
                }
            } catch (RuntimeException e) {
                System.out.println("  REJECTED: " + e.getMessage());
                rejected++;
            }
        }
        System.out.println();
        doReport(facility);
        System.out.println("commands accepted " + ok + ", rejected " + rejected);
        in.close();
    }

    private static boolean doIn(ParkingFacility f, Scanner in) {
        String plate = in.next();
        String type = in.next().toUpperCase();
        String cat = in.next().toUpperCase();
        String pass = in.next().toUpperCase();
        if (!in.hasNextInt()) {
            String bad = in.hasNext() ? in.next() : "<missing>";
            throw new IllegalArgumentException("entry minute '" + bad + "' is not a whole number");
        }
        int minute = in.nextInt();

        Vehicle v = switch (type) {
            case "BIKE" -> new TwoWheeler(plate);
            case "CAR"  -> new Car(plate);
            case "EV"   -> new ElectricCar(plate, 40);
            default -> throw new IllegalArgumentException("unknown vehicle type '" + type + "'");
        };
        UserCategory category;
        try { category = UserCategory.valueOf(cat); }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown user category '" + cat + "'");
        }
        CampusUser owner = new CampusUser(plate + "-owner", category, "Y".equals(pass));

        ParkingTicket t = f.admit(v, owner, minute);
        if (t == null) {
            System.out.printf("IN   %-12s %-5s -> REFUSED, facility is full (%d/%d occupied)%n",
                              plate, type, f.capacity() - f.freeSlots(), f.capacity());
            return false;
        }
        openTickets.put(v.getRegNo(), t);
        System.out.printf("IN   %-12s %-5s %-8s pass=%-3s -> slot %-6s ticket #%d%n",
                          v.getRegNo(), type, category, pass, t.getSlot(), t.getTicketNo());
        return true;
    }

    private static boolean doOut(ParkingFacility f, Scanner in) {
        String plate = in.next().trim().toUpperCase();
        if (!in.hasNextInt()) {
            String bad = in.hasNext() ? in.next() : "<missing>";
            throw new IllegalArgumentException("exit minute '" + bad + "' is not a whole number");
        }
        int minute = in.nextInt();
        int kWh = in.hasNextInt() ? in.nextInt() : 0;

        ParkingTicket t = openTickets.remove(plate);
        if (t == null) throw new IllegalArgumentException(plate + " has no open ticket");

        long fee = f.release(t, minute, kWh);
        System.out.printf("OUT  %-12s %-5d -> %3d min, slot %-6s freed, %-9s FEE Rs %d%n",
                          plate, minute, t.durationMinutes(), t.getSlot(),
                          kWh > 0 ? kWh + " kWh," : "", fee);
        return true;
    }

    private static void doList(ParkingFacility f) {
        System.out.println();
        System.out.println("  VEHICLES CURRENTLY INSIDE (" + f.vehiclesInside() + ")");
        System.out.printf("  %-12s | %-8s | %-8s | %-8s | %s%n",
                          "PLATE", "TICKET", "SLOT", "ENTRY", "OWNER");
        System.out.println("  " + "-".repeat(64));
        for (ParkingTicket t : openTickets.values())
            System.out.printf("  %-12s | #%-7d | %-8s | %-8d | %s%n",
                              t.getVehicle().getRegNo(), t.getTicketNo(), t.getSlot(),
                              t.getEntryMinute(), t.getOwner().getCategory());
        System.out.println();
    }

    private static void doReport(ParkingFacility f) {
        System.out.println();
        System.out.println("  FACILITY REPORT  " + f.getCode());
        System.out.println("  " + "-".repeat(44));
        System.out.printf("  %-28s %d%n", "capacity",            f.capacity());
        System.out.printf("  %-28s %d%n", "vehicles inside",      f.vehiclesInside());
        System.out.printf("  %-28s %d%n", "slots free",           f.freeSlots());
        System.out.printf("  %-28s %d%n", "refused (facility full)", f.refusedBecauseFull());
        System.out.printf("  %-28s %d%n", "tickets issued",       ParkingTicket.issued());
        System.out.printf("  %-28s Rs %d%n", "total collected",   f.totalCollected());
        System.out.println();
    }
}
