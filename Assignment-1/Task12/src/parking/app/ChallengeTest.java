package parking.app;

import parking.model.*;
import parking.pass.*;
import parking.pricing.*;
import parking.service.ParkingFacility;

/**
 * Programming in Java - Assignment 1 (CO1), Task 12
 * Name: Vignesh S   Register Number: 192325002
 *
 * DAY-4 DESIGN CHALLENGE - the evidence.
 *
 * Tests the two new vehicle categories, the one-day pass and the 5-floor
 * ragged facility, and produces the counts the defence in section D relies on.
 */
public class ChallengeTest {

    private static int passed = 0, failed = 0;
    private static final int DAY = 100, OTHER_DAY = 101;

    private static void check(String id, String what, Object expected, Object actual) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(actual));
        if (ok) passed++; else failed++;
        System.out.printf("%-5s | %-48s | %-14s | %-14s | %s%n",
                          id, what, expected, actual, ok ? "PASS" : "**FAIL**");
    }
    private static void checkThrows(String id, String what, String expected, Runnable r) {
        String actual;
        try { r.run(); actual = "no exception"; }
        catch (RuntimeException e) { actual = e.getClass().getSimpleName(); }
        check(id, what, expected, actual);
    }
    private static void header(String t) {
        System.out.println();
        System.out.println(t);
        System.out.println("-".repeat(104));
        System.out.printf("%-5s | %-48s | %-14s | %-14s | %s%n",
                          "ID", "WHAT IS TESTED", "EXPECTED", "ACTUAL", "RESULT");
        System.out.println("-".repeat(104));
    }

    public static void main(String[] args) {
        System.out.println("TASK 12  -  DESIGN CHALLENGE: NEW CATEGORIES, NEW PRICING, NEW FACILITY");
        System.out.println("=".repeat(104));

        /* ---------- the extended facility ---------- */
        // 5 floors, different slot counts AND different slot sizes:
        // floor 0: bike bays (1 unit), floors 1-3: car bays (2 units), floor 4: van bays (3 units)
        int[] perFloor = { 20, 50, 50, 80, 12 };
        int[] unitsPerFloor = { 1, 2, 2, 2, 3 };
        ParkingFacility f = new ParkingFacility("MLCP-2026", perFloor, unitsPerFloor);

        header("A.  THE FACILITY EXTENDED FROM 3 FLOORS TO 5, WITH RAGGED FLOOR SIZES");
        check("A1", "5 floors", 5, f.floors());
        check("A2", "capacity 20+50+50+80+12", 212, f.capacity());
        for (int i = 0; i < 5; i++)
            check("A3." + i, "floor " + i + " has " + perFloor[i] + " slots of "
                             + unitsPerFloor[i] + " unit(s)",
                  perFloor[i] + "/" + unitsPerFloor[i],
                  f.slotsOnFloor(i) + "/" + f.unitsOnFloor(i));
        System.out.println();
        System.out.println("  The jagged array required NO code change: the constructor was handed a");
        System.out.println("  longer int[].  Had the slots been stored in a flat array with a single");
        System.out.println("  stride, 20/50/50/80/12 would have been unrepresentable - the Task 2");
        System.out.println("  analysis predicted exactly this, and this row is the evidence.");

        /* ---------- new categories ---------- */
        header("B.  THE TWO NEW VEHICLE CATEGORIES");
        Vehicle ebike = new EBike("TN60EB0001");
        Vehicle van   = new DeliveryVan("TN60DV0001");

        check("B1", "e-bike tariff is a flat fee", "flat Rs 20 per stay", ebike.tariffDescription());
        check("B2", "e-bike 30 min = flat Rs 20", 20L, ebike.parkingFee(600, 630));
        check("B3", "e-bike 8 hours = still flat Rs 20", 20L, ebike.parkingFee(540, 1020));
        check("B4", "e-bike 1 min = flat Rs 20", 20L, ebike.parkingFee(600, 601));
        check("B5", "e-bike 0 min (drive-through) = Rs 0", 0L, ebike.parkingFee(600, 600));

        check("B6", "van tariff names the peak window",
              "Rs 30/h, x2 between 09:00 and 17:00, capped at Rs 800/day",
              van.tariffDescription());
        // 10:00 -> 12:00 : hours start at 10:00 and 11:00, both peak -> 2 x 60 = 120
        check("B7", "van 10:00-12:00, 2 peak hours = 2 x Rs 60", 120L, van.parkingFee(600, 720));
        // 06:00 -> 08:00 : both hours off-peak -> 2 x 30 = 60
        check("B8", "van 06:00-08:00, 2 off-peak hours = 2 x Rs 30", 60L, van.parkingFee(360, 480));
        // 08:00 -> 11:00 : hours start 08(off), 09(peak), 10(peak) -> 30+60+60 = 150
        check("B9", "van 08:00-11:00, 1 off + 2 peak = Rs 150", 150L, van.parkingFee(480, 660));
        // 16:00 -> 19:00 : hours start 16(peak), 17(off), 18(off) -> 60+30+30 = 120
        check("B10", "van 16:00-19:00, 1 peak + 2 off = Rs 120", 120L, van.parkingFee(960, 1140));
        // 23:00 -> 01:00 crossing midnight: hours start 23(off), 00(off) -> 60
        check("B11", "van 23:00-01:00 across midnight = Rs 60", 60L, van.parkingFee(1380, 60));
        // a full day from 00:00: 9 off-peak (00-08), 8 peak (09-16), 7 off (17-23) = 9*30+8*60+7*30
        check("B12", "van 00:00-24:00 raw = 9x30+8x60+7x30 = Rs 960", 960L,
              new TimeOfDayRate(30, 2.0, 9, 17).fee(0, 1439));
        check("B13", "...but the Rs 800 cap applies", 800L, van.parkingFee(0, 1439));
        check("B14", "van peak hours counted in 08:00-11:00", 2,
              new TimeOfDayRate(30, 2.0, 9, 17).peakHoursIn(480, 660));

        header("C.  THE EXISTING CATEGORIES ARE UNCHANGED  (the regression check)");
        check("C1", "two-wheeler 130 min still Rs 30", 30L, new TwoWheeler("X1").parkingFee(540, 670));
        check("C2", "two-wheeler cap still Rs 60", 60L, new TwoWheeler("X2").parkingFee(540, 1080));
        check("C3", "car 130 min still Rs 90", 90L, new Car("X3").parkingFee(540, 670));
        check("C4", "car 1 min still Rs 30", 30L, new Car("X4").parkingFee(540, 541));
        check("C5", "car 60 min still Rs 30", 30L, new Car("X5").parkingFee(540, 600));
        check("C6", "car 61 min still Rs 60", 60L, new Car("X6").parkingFee(540, 601));
        check("C7", "car daily cap still Rs 200", 200L, new Car("X7").parkingFee(0, 1439));
        check("C8", "EV parks at the car tariff", 90L, new ElectricCar("X8", 40).parkingFee(540, 670));
        check("C9", "EV charging still Rs 12/kWh", 120.0, new ElectricCar("X9", 40).chargingFee(10));
        check("C10", "plate equality still crosses classes", true,
              new Car("TN70ZZ0001").equals(new EBike("TN70ZZ0001")));

        /* ---------- the one-day pass ---------- */
        header("D.  THE ONE-DAY PASS FOR VISITORS");
        CampusUser visitorNoPass = new CampusUser("Guest",  UserCategory.VISITOR, Pass.NONE);
        CampusUser visitorDay    = new CampusUser("Vendor", UserCategory.VISITOR, new OneDayPass(DAY));
        CampusUser staffMonthly  = new CampusUser("Dr Ramesh", UserCategory.STAFF, new MonthlyPass(90, 30));
        CampusUser studentMonthly= new CampusUser("Vignesh S", UserCategory.STUDENT, new MonthlyPass(90, 30));

        check("E1", "visitor with no pass pays", true,  visitorNoPass.paysFee(DAY, 600));
        check("E2", "visitor WITH a one-day pass, on that day", false, visitorDay.paysFee(DAY, 600));
        check("E3", "the same pass on the NEXT day", true, visitorDay.paysFee(OTHER_DAY, 600));
        check("E4", "staff monthly pass, inside the month", false, staffMonthly.paysFee(DAY, 600));
        check("E5", "staff monthly pass, outside the month", true, staffMonthly.paysFee(200, 600));
        check("E6", "student monthly pass", false, studentMonthly.paysFee(DAY, 600));
        check("E7", "one-day pass is priced at Rs 100", 100L, new OneDayPass(DAY).priceWhenSold());
        check("E8", "monthly pass is priced at Rs 600", 600L, new MonthlyPass(90, 30).priceWhenSold());
        checkThrows("E9", "a pass day outside the year is refused",
                    "IllegalArgumentException", () -> new OneDayPass(400));
        System.out.println();
        System.out.println("  E2 and E3 together are the whole point.  In the Task 11 design, a");
        System.out.println("  visitor's pass could NEVER exempt the fee - UserCategory said so - and");
        System.out.println("  a pass had no date at all, so neither row could have been written.");

        /* ---------- end to end, on the real facility ---------- */
        header("F.  A DAY ON THE EXTENDED FACILITY, HAND-CALCULATED");
        Object[][] day = {
            // vehicle,                      owner,            in,   out,  kWh, hand, working
            { new TwoWheeler("TN80AA0001"), visitorNoPass,     540,  625, 0,  20L, "85 min -> 2 h x Rs 10" },
            { new Car("TN80BB0002"),        visitorNoPass,     545,  675, 0,  90L, "130 min -> 3 h x Rs 30" },
            { new ElectricCar("TN80CC0003", 40), staffMonthly, 550,  680, 10, 120L,"pass: Rs 0 park; 10 kWh x 12" },
            { new EBike("TN80DD0004"),      visitorNoPass,     600,  900, 0,  20L, "flat fee, duration ignored" },
            { new EBike("TN80EE0005"),      visitorDay,        600,  900, 0,  0L,  "one-day pass covers day 100" },
            { new DeliveryVan("TN80FF0006"),visitorNoPass,     600,  720, 0,  120L,"10:00-12:00, 2 peak x Rs 60" },
            { new DeliveryVan("TN80GG0007"),visitorNoPass,     360,  480, 0,  60L, "06:00-08:00, 2 off x Rs 30" },
            { new Car("TN80HH0008"),        studentMonthly,    600, 1100, 0,  0L,  "student pass: free" },
            { new DeliveryVan("TN80II0009"),visitorDay,        480,  660, 0,  0L,  "one-day pass covers the van too" },
        };
        System.out.printf("%-14s | %-16s | %-5s | %-5s | %-4s | %-9s | %-9s | %s%n",
                          "VEHICLE", "CATEGORY", "IN", "OUT", "kWh", "BY HAND", "PROGRAM", "WORKING");
        System.out.println("-".repeat(104));
        long hand = 0, prog = 0;
        for (Object[] row : day) {
            Vehicle v = (Vehicle) row[0];
            CampusUser u = (CampusUser) row[1];
            int in = (Integer) row[2], out = (Integer) row[3], kwh = (Integer) row[4];
            long h = (Long) row[5];
            ParkingTicket t = f.admit(v, u, DAY, in);
            if (t == null) { System.out.println("  REFUSED: " + v.getRegNo()); continue; }
            long p = f.release(v.getRegNo(), out, kwh);
            hand += h; prog += p;
            System.out.printf("%-14s | %-16s | %-5d | %-5d | %-4d | Rs %-6d | Rs %-6d | %s%s%n",
                              v.getRegNo(), v.getCategory(), in, out, kwh, h, p,
                              h == p ? "" : "MISMATCH! ", row[6]);
        }
        System.out.println("-".repeat(104));
        System.out.printf("%-14s | %-16s | %-5s | %-5s | %-4s | Rs %-6d | Rs %-6d | %s%n",
                          "DAY TOTAL", "", "", "", "", hand, prog,
                          hand == prog ? "hand calculation and program AGREE" : "DISAGREE");
        check("F1", "hand-calculated total == program total", hand, prog);
        check("F2", "facility empty at close", 0, f.vehiclesInside());
        check("F3", "every slot released", f.capacity(), f.freeSlots());

        /* ---------- slot fitting ---------- */
        header("G.  SLOT FITTING: A VAN CANNOT TAKE A BIKE BAY");
        ParkingFacility small = new ParkingFacility("MLCP-S", new int[]{2, 1}, new int[]{1, 3});
        ParkingTicket bikeT = small.admit(new EBike("TN90AA0001"), visitorNoPass, DAY, 540);
        check("G1", "an e-bike takes the 1-unit floor 0", "0/0", bikeT.getSlot().toString());
        ParkingTicket vanT = small.admit(new DeliveryVan("TN90BB0002"), visitorNoPass, DAY, 541);
        check("G2", "a van skips floor 0 and takes the 3-unit floor 1", "1/0",
              vanT.getSlot().toString());
        check("G3", "a second van is refused - no van bay free", "null",
              String.valueOf(small.admit(new DeliveryVan("TN90CC0003"), visitorNoPass, DAY, 542)));
        check("G4", "the refusal is 'no fit', not 'full'", 1, small.refusedBecauseNoFit());
        check("G5", "a slot IS still free, just not a suitable one", 1, small.freeSlots());
        ParkingTicket bike2 = small.admit(new TwoWheeler("TN90DD0004"), visitorNoPass, DAY, 543);
        check("G6", "a two-wheeler still fits that free bay", "0/1", bike2.getSlot().toString());

        /* ---------- invalid cases still refused ---------- */
        header("H.  THE INVARIANTS SURVIVED THE REDESIGN");
        ParkingFacility inv = new ParkingFacility("MLCP-I", new int[]{3}, new int[]{3});
        inv.admit(new Car("TNA0AA0001"), visitorNoPass, DAY, 540);
        checkThrows("H1", "duplicate plate still refused", "IllegalStateException",
                    () -> inv.admit(new Car("TNA0AA0001"), visitorNoPass, DAY, 600));
        checkThrows("H2", "duplicate plate as a NEW category too", "IllegalStateException",
                    () -> inv.admit(new DeliveryVan("TNA0AA0001"), visitorNoPass, DAY, 600));
        checkThrows("H3", "blank plate still refused", "IllegalArgumentException",
                    () -> new EBike("  "));
        checkThrows("H4", "exit for an unknown plate", "IllegalStateException",
                    () -> inv.release("TNZZ9999", 600));
        checkThrows("H5", "charging a van (not Chargeable)", "IllegalArgumentException",
                    () -> { inv.admit(new DeliveryVan("TNA0BB0002"), visitorNoPass, DAY, 540);
                            inv.release("TNA0BB0002", 600, 10); });
        checkThrows("H6", "negative rate rejected at tariff construction",
                    "IllegalArgumentException", () -> new HourlyRate(-10));
        checkThrows("H7", "peak multiplier below 1 rejected", "IllegalArgumentException",
                    () -> new TimeOfDayRate(30, 0.5, 9, 17));
        checkThrows("H8", "inverted peak window rejected", "IllegalArgumentException",
                    () -> new TimeOfDayRate(30, 2.0, 17, 9));
        checkThrows("H9", "negative cap rejected", "IllegalArgumentException",
                    () -> new CappedRule(new HourlyRate(30), -1));
        checkThrows("H10", "a vehicle with no pricing rule cannot exist",
                    "IllegalArgumentException", () -> new Vehicle("TNA0CC0003",
                            VehicleCategory.CAR, null) { });
        checkThrows("H11", "mismatched floor/size arrays rejected", "IllegalArgumentException",
                    () -> new ParkingFacility("BAD", new int[]{2, 2}, new int[]{1}));

        /* ---------- a rule added at run time ---------- */
        header("I.  A TARIFF THE SYSTEM HAS NEVER SEEN, ADDED WITHOUT A NEW CLASS");
        PricingRule weekendVan = new CappedRule(new TimeOfDayRate(30, 1.5, 10, 14), 400);
        check("I1", "a new composed rule describes itself",
              "Rs 30/h, x2 between 10:00 and 14:00, capped at Rs 400/day",
              weekendVan.describe());
        check("I2", "11:00-13:00 at x1.5 = 2 x Rs 45", 90L, weekendVan.fee(660, 780));
        check("I3", "08:00-10:00 off-peak = 2 x Rs 30", 60L, weekendVan.fee(480, 600));
        check("I4", "the Rs 400 cap applies over a long stay", 400L, weekendVan.fee(0, 1439));
        System.out.println();
        System.out.println("  I1-I4 used only CappedRule and TimeOfDayRate, both of which already");
        System.out.println("  existed.  A seasonal tariff is now a configuration line, not a class -");
        System.out.println("  which is the extensibility the Day-4 requirement asked for.");

        /* ---------- the counts the defence relies on ---------- */
        System.out.println();
        System.out.println("J.  EVIDENCE FOR THE DEFENCE  -  WHAT THE CHANGE ACTUALLY COST");
        System.out.println("-".repeat(104));
        System.out.printf("%-44s | %-12s | %s%n", "REQUIREMENT", "NEW CLASSES", "EXISTING CLASSES MODIFIED");
        System.out.println("-".repeat(104));
        String[][] cost = {
            {"e-bike, flat fee",                 "2", "0  (EBike + FlatFee)"},
            {"delivery van, peak pricing",       "2", "0  (DeliveryVan + TimeOfDayRate)"},
            {"visitor one-day pass",             "1", "2  (CampusUser, UserCategory)"},
            {"5 ragged floors",                  "0", "0  (longer int[] to the constructor)"},
            {"typed slots so a van fits",        "0", "3  (Slot, SlotFactory, SlotGrid)"},
            {"the PricingRule abstraction",      "5", "6  (Vehicle + 5 vehicle classes)"},
        };
        for (String[] r : cost) System.out.printf("%-44s | %-12s | %s%n", r[0], r[1], r[2]);
        System.out.println("-".repeat(104));
        System.out.println("  Read the first two rows together with the last.  Once the PricingRule");
        System.out.println("  abstraction was paid for - and it WAS a real cost, 6 existing classes");
        System.out.println("  changed - each new vehicle category became two small additions and");
        System.out.println("  zero modifications.  That is the trade this redesign makes: a one-off");
        System.out.println("  cost today in exchange for an open-ended series of free extensions.");

        System.out.println();
        System.out.println("=".repeat(104));
        System.out.printf("TESTS RUN %d    PASSED %d    FAILED %d%n", passed + failed, passed, failed);
        System.out.println("=".repeat(104));
        if (failed > 0) { System.out.println("RESULT: FAILED."); System.exit(1); }
        System.out.println("RESULT: all tests passed.");
    }
}
