package parking.app;

import parking.model.*;
import parking.service.ParkingFacility;

/**
 * Programming in Java - Assignment 1 (CO1), Task 11
 * Name: Vignesh S   Register Number: 192325002
 *
 * Implements, tests and defends the design.  Every test states its EXPECTED
 * value before running, prints the ACTUAL value, and the run fails loudly if
 * any row disagrees - so the test table in the report is generated, not typed.
 */
public class SystemTest {

    private static int passed = 0, failed = 0;

    private static void check(String id, String description, Object expected, Object actual) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(actual));
        if (ok) passed++; else failed++;
        System.out.printf("%-5s | %-44s | %-16s | %-16s | %s%n",
                          id, description, expected, actual, ok ? "PASS" : "**FAIL**");
    }

    private static void checkThrows(String id, String description, String expectedType,
                                    Runnable action) {
        String actual;
        try { action.run(); actual = "no exception"; }
        catch (RuntimeException e) { actual = e.getClass().getSimpleName(); }
        check(id, description, expectedType, actual);
    }

    /** The wrap-around arithmetic the midnight tests check, stated once. */
    private static int crossMidnightDuration(int entryMinute, int exitMinute) {
        int d = exitMinute - entryMinute;
        return d < 0 ? d + 24 * 60 : d;
    }

    private static void header(String title) {
        System.out.println();
        System.out.println(title);
        System.out.println("-".repeat(112));
        System.out.printf("%-5s | %-44s | %-16s | %-16s | %s%n",
                          "ID", "WHAT IS TESTED", "EXPECTED", "ACTUAL", "RESULT");
        System.out.println("-".repeat(112));
    }

    public static void main(String[] args) {
        System.out.println("TASK 11  -  IMPLEMENT, TEST AND DEFEND THE DESIGN");
        System.out.println("=".repeat(112));
        System.out.println("Facility under test: 3 floors of 2, 2 and 1 slots = 5 slots");

        CampusUser student  = new CampusUser("Vignesh S", UserCategory.STUDENT, true);
        CampusUser studentNP= new CampusUser("Arun", UserCategory.STUDENT, false);
        CampusUser staff    = new CampusUser("Dr Ramesh", UserCategory.STAFF, true);
        CampusUser visitor  = new CampusUser("Guest", UserCategory.VISITOR, false);
        CampusUser visitorP = new CampusUser("Vendor", UserCategory.VISITOR, true);

        /* ================= 1. NORMAL OPERATION ================= */
        header("1.  NORMAL OPERATION  -  vehicles of different types entering and leaving");
        ParkingFacility f = new ParkingFacility("MLCP-N", new int[]{2, 2, 1});
        check("N1", "capacity of 2+2+1 floors", 5, f.capacity());
        check("N2", "all slots free at opening", 5, f.freeSlots());

        Vehicle bike = new TwoWheeler("TN01AB1111");
        Vehicle car  = new Car("TN02CD2222");
        ElectricCar ev = new ElectricCar("TN03EF3333", 40);

        ParkingTicket t1 = f.admit(bike, visitor, 540);     // 09:00
        check("N3", "first vehicle gets slot 0/0", "0/0", t1.getSlot().toString());
        ParkingTicket t2 = f.admit(car, visitor, 545);
        check("N4", "second vehicle gets slot 0/1", "0/1", t2.getSlot().toString());
        ParkingTicket t3 = f.admit(ev, staff, 550);
        check("N5", "third vehicle moves to floor 1", "1/0", t3.getSlot().toString());
        check("N6", "ticket numbers are consecutive",
              (t1.getTicketNo() + 1) + "," + (t1.getTicketNo() + 2),
              t2.getTicketNo() + "," + t3.getTicketNo());
        check("N7", "three vehicles inside", 3, f.vehiclesInside());
        check("N8", "two slots still free", 2, f.freeSlots());

        // two-wheeler, visitor, 85 min -> ceil(85/60)=2 h x Rs 10 = Rs 20
        check("N9", "two-wheeler 85 min visitor = 2h x Rs 10", 20L, f.release(t1, 625));
        // car, visitor, 130 min -> ceil(130/60)=3 h x Rs 30 = Rs 90
        check("N10", "car 130 min visitor = 3h x Rs 30", 90L, f.release(t2, 675));
        // EV, staff with pass, 130 min -> free parking, but 10 kWh x Rs 12 = Rs 120
        check("N11", "EV staff+pass, 10 kWh = Rs 0 + Rs 120", 120L, f.release(t3, 680, 10));
        check("N12", "facility empty again", 0, f.vehiclesInside());
        check("N13", "all slots free again", 5, f.freeSlots());
        check("N14", "collected 20+90+120", 230L, f.totalCollected());

        /* ================= 2. BOUNDARY CASES ================= */
        header("2.  BOUNDARY CASES  -  the last free slot, zero-minute stays, slab edges");
        ParkingFacility b = new ParkingFacility("MLCP-B", new int[]{2, 1});
        Vehicle v1 = new Car("TN10AA0001"), v2 = new Car("TN10AA0002"), v3 = new Car("TN10AA0003");
        ParkingTicket b1 = b.admit(v1, visitor, 540);
        ParkingTicket b2 = b.admit(v2, visitor, 540);
        ParkingTicket b3 = b.admit(v3, visitor, 540);
        check("B1", "the LAST free slot is allocated", "1/0", b3.getSlot().toString());
        check("B2", "no slots left", 0, b.freeSlots());
        check("B3", "one more vehicle is REFUSED (null)", "null",
              String.valueOf(b.admit(new Car("TN10AA0004"), visitor, 541)));
        check("B4", "refusal counter incremented", 1, b.refusedBecauseFull());
        check("B5", "a refused vehicle changes nothing", 3, b.vehiclesInside());
        b.release(b2, 600);
        check("B6", "freeing the middle slot leaves 1 free", 1, b.freeSlots());
        ParkingTicket b5 = b.admit(new Car("TN10AA0005"), visitor, 601);
        check("B7", "the freed slot 0/1 is reused", "0/1", b5.getSlot().toString());

        Vehicle zero = new Car("TN11BB0001");
        ParkingFacility z = new ParkingFacility("MLCP-Z", new int[]{3});
        ParkingTicket zt = z.admit(zero, visitor, 540);
        check("B8", "ZERO-minute stay: entry == exit", 0L, z.release(zt, 540));
        check("B9", "slab edge 60 min = exactly 1 hour", 30L, new Car("X1").calculateFee(60));
        check("B10", "slab edge 61 min = 2 started hours", 60L, new Car("X2").calculateFee(61));
        check("B11", "slab edge 59 min = 1 started hour", 30L, new Car("X3").calculateFee(59));
        check("B12", "slab edge 1 min = 1 started hour", 30L, new Car("X4").calculateFee(1));
        check("B13", "two-wheeler daily cap Rs 60 reached at 6h", 60L,
              new TwoWheeler("X5").calculateFee(360));
        check("B14", "two-wheeler cap holds for 24h", 60L, new TwoWheeler("X6").calculateFee(1440));
        check("B15", "car daily cap Rs 200 at 24h (not 720)", 200L, new Car("X7").calculateFee(1440));
        // 23:30 (minute 1410) to 00:30 (minute 30) is 60 minutes, i.e. ONE started
        // hour, so Rs 30.  The wrap-around must not be read as a negative stay.
        check("B16", "stay crossing midnight 23:30->00:30 = 1 h", 30L,
              new Car("X8").calculateFee(1410, 30));
        check("B16b", "the same stay is 60 minutes, not -1380", 60,
              crossMidnightDuration(1410, 30));
        check("B16c", "a 23:00->01:30 stay is 150 min = 3 h", 90L,
              new Car("X8b").calculateFee(1380, 90));
        check("B17", "EV draws its entire battery", 480.0,
              new ElectricCar("X9", 40).chargingFee(40));
        check("B18", "EV draws zero kWh", 0.0, new ElectricCar("XA", 40).chargingFee(0));

        /* ================= 3. INVALID CASES ================= */
        header("3.  INVALID CASES  -  every one must be REFUSED, not absorbed");
        ParkingFacility i = new ParkingFacility("MLCP-I", new int[]{3});
        Vehicle dup = new Car("TN20CC0001");
        ParkingTicket it = i.admit(dup, visitor, 540);

        checkThrows("I1", "duplicate registration number admitted twice",
                    "IllegalStateException", () -> i.admit(new Car("TN20CC0001"), visitor, 600));
        checkThrows("I2", "same plate as a DIFFERENT vehicle class",
                    "IllegalStateException", () -> i.admit(new ElectricCar("TN20CC0001", 40), visitor, 600));
        checkThrows("I3", "blank registration number",
                    "IllegalArgumentException", () -> new Car("   "));
        checkThrows("I4", "null registration number",
                    "IllegalArgumentException", () -> new Car(null));
        checkThrows("I5", "negative parking duration",
                    "IllegalArgumentException", () -> new Car("TN21DD0001").calculateFee(-30));
        checkThrows("I6", "entry minute outside 0..1439",
                    "IllegalArgumentException", () -> i.admit(new Car("TN21DD0002"), visitor, 2000));
        checkThrows("I7", "exit minute outside 0..1439",
                    "IllegalArgumentException", () -> i.release(it, 9999));
        checkThrows("I8", "releasing a ticket twice",
                    "IllegalStateException", () -> { i.release(it, 600); i.release(it, 700); });
        checkThrows("I9", "releasing a ticket of another facility",
                    "IllegalStateException", () -> i.release(b1, 700));
        checkThrows("I10", "charging a vehicle that is not Chargeable",
                    "IllegalArgumentException", () -> {
                        ParkingTicket x = i.admit(new Car("TN22EE0001"), visitor, 540);
                        i.release(x, 600, 10);
                    });
        checkThrows("I11", "EV drawing more kWh than its battery",
                    "IllegalArgumentException", () -> new ElectricCar("TN23FF0001", 40).chargingFee(50));
        checkThrows("I12", "negative kWh",
                    "IllegalArgumentException", () -> new ElectricCar("TN23FF0002", 40).chargingFee(-5));
        checkThrows("I13", "EV with zero battery capacity",
                    "IllegalArgumentException", () -> new ElectricCar("TN23FF0003", 0));
        checkThrows("I14", "facility with no floors",
                    "IllegalArgumentException", () -> new ParkingFacility("BAD", new int[]{}));
        checkThrows("I15", "facility with an empty floor",
                    "IllegalArgumentException", () -> new ParkingFacility("BAD", new int[]{3, 0}));
        checkThrows("I16", "null user category",
                    "IllegalArgumentException", () -> new CampusUser("X", null, true));
        checkThrows("I17", "slot number negative",
                    "IllegalArgumentException", () -> SlotFactory.create(0, -1));

        /* ================= 4. THE PASS RULE ================= */
        header("4.  THE PASS RULE  -  who pays and who does not");
        ParkingFacility p = new ParkingFacility("MLCP-P", new int[]{5});
        Object[][] passCases = {
            { "student with a pass",   student,   0L },
            { "student with NO pass",  studentNP, 90L },
            { "staff with a pass",     staff,     0L },
            { "visitor with no pass",  visitor,   90L },
            { "visitor WITH a pass",   visitorP,  90L },      // a pass never exempts a visitor
        };
        int n = 0;
        for (Object[] c : passCases) {
            CampusUser u = (CampusUser) c[1];
            Vehicle pv = new Car("TN30PP" + String.format("%04d", ++n));
            ParkingTicket pt = p.admit(pv, u, 540);
            check("P" + n, (String) c[0] + ", car 130 min", c[2], p.release(pt, 670));
        }

        /* ================= 5. DUPLICATE TICKET ================= */
        header("5.  RE-ISSUING A LOST TICKET");
        ParkingFacility r = new ParkingFacility("MLCP-R", new int[]{3});
        Vehicle rv = new Car("TN40RR0001");
        ParkingTicket orig = r.admit(rv, visitor, 540);
        int origNo = orig.getTicketNo();
        ParkingTicket copy = r.reissue(orig);
        check("D1", "the duplicate has a NEW number", true, copy.getTicketNo() != origNo);
        check("D2", "the duplicate is marked as such", true, copy.isDuplicate());
        check("D3", "the duplicate keeps the same slot",
              orig.getSlot().toString(), copy.getSlot().toString());
        check("D4", "the duplicate keeps the same entry time",
              orig.getEntryMinute(), copy.getEntryMinute());
        check("D5", "the duplicate bills the same fee", 90L, r.release(copy, 670));
        checkThrows("D6", "the ORIGINAL ticket no longer works",
                    "IllegalStateException", () -> r.release(orig, 670));

        /* ================= 6. HAND-CALCULATED DAY ================= */
        System.out.println();
        System.out.println("6.  A DAY'S COLLECTION, CALCULATED BY HAND AND BY THE PROGRAM");
        System.out.println("-".repeat(112));
        ParkingFacility day = new ParkingFacility("MLCP-DAY", new int[]{2, 2, 1});

        Object[][] schedule = {
            // vehicle,                    owner,     in,  out, kWh, hand-worked fee, working
            { new TwoWheeler("TN50AA1111"), visitor,  540, 625, 0, 20L,  "85 min  -> ceil(85/60)=2 h x Rs 10 = 20" },
            { new Car("TN50BB2222"),        visitor,  545, 675, 0, 90L,  "130 min -> ceil(130/60)=3 h x Rs 30 = 90" },
            { new ElectricCar("TN50CC3333", 40), staff, 550, 680, 10, 120L, "pass: parking 0; 10 kWh x Rs 12 = 120" },
            { new Car("TN50DD4444"),        studentNP,560, 1000, 0, 200L, "440 min -> 8 h x 30 = 240, capped at 200" },
            { new TwoWheeler("TN50EE5555"), student,  600, 1100, 0, 0L,   "student with a pass: free" },
            { new Car("TN50FF6666"),        visitorP, 610, 611, 0, 30L,   "1 min -> 1 started hour x Rs 30 = 30" },
            { new ElectricCar("TN50GG7777", 30), visitor, 620, 1340, 5, 260L, "720 min -> 12 h x 30 = 360 capped 200; +5x12=60" },
            { new TwoWheeler("TN50HH8888"), visitor,  700, 1300, 0, 60L,  "600 min -> 10 h x 10 = 100, capped at 60" },
        };

        System.out.printf("%-20s | %-10s | %-5s | %-5s | %-4s | %-9s | %-9s | %s%n",
                          "VEHICLE", "OWNER", "IN", "OUT", "kWh", "BY HAND", "PROGRAM", "WORKING");
        System.out.println("-".repeat(112));
        long handTotal = 0, programTotal = 0;
        for (Object[] row : schedule) {
            Vehicle v = (Vehicle) row[0];
            CampusUser u = (CampusUser) row[1];
            int in = (Integer) row[2], out = (Integer) row[3], kwh = (Integer) row[4];
            long hand = (Long) row[5];
            ParkingTicket t = day.admit(v, u, in);
            if (t == null) { System.out.println("  REFUSED - facility full: " + v.getRegNo()); continue; }
            long prog = day.release(t, out, kwh);
            handTotal += hand; programTotal += prog;
            System.out.printf("%-20s | %-10s | %-5d | %-5d | %-4d | Rs %-6d | Rs %-6d | %s%n",
                              v.getRegNo(), u.getCategory(), in, out, kwh, hand, prog,
                              (hand == prog ? "" : "MISMATCH! ") + row[6]);
        }
        System.out.println("-".repeat(112));
        System.out.printf("%-20s | %-10s | %-5s | %-5s | %-4s | Rs %-6d | Rs %-6d | %s%n",
                          "TOTAL FOR THE DAY", "", "", "", "", handTotal, programTotal,
                          handTotal == programTotal ? "hand calculation and program AGREE"
                                                    : "DISAGREE - investigate");
        check("H1", "hand-calculated day total == program total", handTotal, programTotal);
        check("H2", "facility's own running total agrees", handTotal, day.totalCollected());
        check("H3", "facility is empty at close of day", 0, day.vehiclesInside());
        check("H4", "every slot released", day.capacity(), day.freeSlots());

        /* ================= summary ================= */
        System.out.println();
        System.out.println("=".repeat(112));
        System.out.printf("TESTS RUN %d    PASSED %d    FAILED %d%n", passed + failed, passed, failed);
        System.out.println("Tickets issued across every facility in this run: " + ParkingTicket.issued());
        System.out.println("=".repeat(112));
        if (failed > 0) {
            System.out.println("RESULT: FAILED - the design does not satisfy its own specification.");
            System.exit(1);
        }
        System.out.println("RESULT: all tests passed.");
    }
}
