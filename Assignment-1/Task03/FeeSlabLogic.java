/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 3  -  Design the Control Logic (decision and looping logic)
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * The two core operations are allocating a free slot and computing the fee.
 * This program shows the fee slabs written BOTH ways - if-else and switch - and
 * proves they agree, and shows where break, continue and return each save work.
 *
 * FEE SLABS (per vehicle type)
 *   first hour          : base rate
 *   each extra hour     : base rate
 *   daily maximum (cap) : charged once the computed fee reaches the cap
 *   every started hour is charged in full
 */
public class FeeSlabLogic {

    enum Type { TWO_WHEELER, CAR, EV }

    static int baseRate(Type t) {
        switch (t) {                          // switch on an enum: compiler checks the cases
            case TWO_WHEELER: return 10;
            case CAR:         return 30;
            case EV:          return 30;
            default:          throw new IllegalArgumentException("unknown type " + t);
        }
    }

    static int dailyCap(Type t) {
        switch (t) {
            case TWO_WHEELER: return 60;
            case CAR:         return 200;
            case EV:          return 200;
            default:          throw new IllegalArgumentException("unknown type " + t);
        }
    }

    /** Slab logic written with if-else. */
    static long feeIfElse(Type t, int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("negative duration");
        if (minutes == 0) return 0;                       // guard: drive-through, no charge

        int hours = (minutes + 59) / 60;                  // every started hour in full
        int rate  = baseRate(t);
        long fee;

        if (hours == 1) {
            fee = rate;                                   // first-hour slab
        } else {
            fee = (long) rate + (long) (hours - 1) * rate; // first hour + each extra hour
        }

        int cap = dailyCap(t);
        if (fee > cap) {
            fee = cap;                                    // daily maximum slab
        }
        return fee;
    }

    /** The same slab logic written with switch. */
    static long feeSwitch(Type t, int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("negative duration");
        int hours = (minutes + 59) / 60;
        switch (hours) {
            case 0:  return 0;                            // no started hour
            case 1:  return baseRate(t);                  // first-hour slab
            default:                                      // first hour + extra hours, then cap
                long fee = (long) hours * baseRate(t);
                return Math.min(fee, dailyCap(t));
        }
    }

    /* ---------- allocation: where break, continue and return earn their place ---- */

    /**
     * Allocates the first free slot, but SKIPS any floor that is already known to be
     * full and STOPS the whole search the moment a slot is taken.
     * Returns {floor, slot}, or null when nothing is free.
     */
    static int[] allocate(String[][] slots, boolean[] floorFull, String vehicleNo,
                          int[] workCounter) {
        for (int f = 0; f < slots.length; f++) {
            if (floorFull[f]) {
                continue;                      // CONTINUE: skip a floor we know is full
            }
            for (int s = 0; s < slots[f].length; s++) {
                workCounter[0]++;              // count every cell actually examined
                if (slots[f][s] == null) {
                    slots[f][s] = vehicleNo;
                    return new int[]{f, s};    // RETURN: leaves both loops at once
                }
                if (s == slots[f].length - 1) {
                    // the last slot on this floor is occupied, and every earlier slot
                    // was too (otherwise the return above would already have fired)
                    floorFull[f] = true;       // remember it, so the next call skips the floor
                    break;                     // BREAK: nothing more to learn on this floor
                }
            }
        }
        return null;                           // the facility is full
    }

    public static void main(String[] args) {
        System.out.println("TASK 3  -  CONTROL LOGIC FOR THE TWO CORE OPERATIONS");
        System.out.println("=".repeat(78));

        System.out.println();
        System.out.println("1.  FEE SLABS: if-else AND switch COMPARED");
        System.out.println("-".repeat(78));
        System.out.printf("%-12s | %-9s | %-7s | %-10s | %-10s | %s%n",
                          "TYPE", "MINUTES", "HOURS", "if-else", "switch", "AGREE?");
        System.out.println("-".repeat(78));
        int[] durations = { 0, 1, 59, 60, 61, 130, 180, 360, 600, 1440 };
        boolean allAgree = true;
        for (Type t : Type.values()) {
            for (int m : durations) {
                long a = feeIfElse(t, m), b = feeSwitch(t, m);
                boolean ok = (a == b);
                if (!ok) allAgree = false;
                System.out.printf("%-12s | %-9d | %-7d | %-10s | %-10s | %s%n",
                                  t, m, (m + 59) / 60, "Rs " + a, "Rs " + b, ok ? "yes" : "NO");
            }
        }
        System.out.println("-".repeat(78));
        System.out.println("  Both formulations agree on every case: " + allAgree);
        System.out.println("  if-else is kept in the final design because the slab boundaries are");
        System.out.println("  RANGES (hours > 1, fee > cap) and switch can only test equality of a");
        System.out.println("  single value - the switch version above has to compute the hour count");
        System.out.println("  first and still falls through to a Math.min for the cap.");

        System.out.println();
        System.out.println("2.  WHEN THE FEE SLAB IS REJECTED");
        System.out.println("-".repeat(78));
        try { feeIfElse(Type.CAR, -10); }
        catch (IllegalArgumentException e) { System.out.println("  minutes = -10  -> REJECTED: " + e.getMessage()); }
        System.out.println("  minutes =   0  -> Rs " + feeIfElse(Type.CAR, 0) + "  (no started hour, nothing to charge)");

        System.out.println();
        System.out.println("3.  ALLOCATION: HOW break, continue AND return AVOID WORK");
        System.out.println("-".repeat(78));
        int[] perFloor = { 3, 2, 4 };
        String[][] slots = new String[perFloor.length][];
        for (int f = 0; f < perFloor.length; f++) slots[f] = new String[perFloor[f]];
        boolean[] floorFull = new boolean[perFloor.length];

        System.out.println("  facility = " + java.util.Arrays.toString(perFloor)
                           + "  (9 slots)");
        System.out.println();
        System.out.printf("%-6s | %-12s | %-14s | %-11s | %s%n",
                          "CALL", "VEHICLE", "RESULT", "CELLS READ", "FLOORS SKIPPED");
        System.out.println("-".repeat(78));
        for (int i = 1; i <= 10; i++) {
            int[] work = new int[1];
            int[] pos = allocate(slots, floorFull, "TN" + String.format("%02d", i), work);
            int skipped = 0;
            for (boolean b : floorFull) if (b) skipped++;
            System.out.printf("%-6d | %-12s | %-14s | %-11d | %s%n", i,
                              "TN" + String.format("%02d", i),
                              pos == null ? "FULL, refused" : java.util.Arrays.toString(pos),
                              work[0], skipped == 0 ? "-" : skipped + " known-full");
        }
        System.out.println("-".repeat(78));
        System.out.println("  return  : ends the search the instant a slot is taken, so a vehicle");
        System.out.println("            never reads a cell beyond the one it occupies.");
        System.out.println("  break   : stops re-reading a floor once it is proved full.");
        System.out.println("  continue: skips such a floor entirely on every later call, which is");
        System.out.println("            why 'CELLS READ' stays small instead of growing to 9.");
        System.out.println();
        System.out.println("  WHEN NO SUITABLE SLOT IS FREE the method returns null, and the caller");
        System.out.println("  must refuse entry rather than issue a ticket - no ticket is created,");
        System.out.println("  no slot is written, and the counter is not advanced, so a refused");
        System.out.println("  vehicle leaves the system in exactly the state it found it.");
    }
}
