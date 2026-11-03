/*
 * Programming in Java  -  Assignment 1 (CO1)
 * Task 3  -  Design the Control Logic  ("Complete the Code")
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * The given skeleton and its main() method are unchanged.
 * Only TODO 1 and TODO 2 have been completed.
 */
public class ParkingFacility {
    // slots[floor][slot] holds a vehicle number, or null if the slot is free
    private final String[][] slots;

    public ParkingFacility(int[] slotsPerFloor) {
        slots = new String[slotsPerFloor.length][];
        for (int f = 0; f < slotsPerFloor.length; f++) {
            slots[f] = new String[slotsPerFloor[f]];
        }
    }

    /** Returns {floor, slot} of the first free slot, or null if it is full. */
    public int[] allocate(String vehicleNo) {
        // TODO 1: Search floor by floor and slot by slot; place vehicleNo in the first
        //   free slot and return {floor, slot}
        for (int f = 0; f < slots.length; f++) {          // outer loop: floors, lowest first
            for (int s = 0; s < slots[f].length; s++) {   // inner loop: slots on that floor
                if (slots[f][s] == null) {                // first free slot found
                    slots[f][s] = vehicleNo;              // occupy it
                    return new int[]{f, s};               // return stops both loops at once
                }
            }
        }
        return null;                                      // every slot on every floor is taken
    }

    /** Frees a slot. Returns false if the position is invalid or already free. */
    public boolean release(int floor, int slot) {
        // TODO 2: Validate floor and slot, free the slot and return true; otherwise
        //   return false
        if (floor < 0 || floor >= slots.length) {         // guard 1: floor out of range
            return false;
        }
        if (slot < 0 || slot >= slots[floor].length) {    // guard 2: slot out of range
            return false;
        }
        if (slots[floor][slot] == null) {                 // guard 3: nothing parked there
            return false;
        }
        slots[floor][slot] = null;                        // release the slot
        return true;
    }

    public static void main(String[] args) {
        // floor 0 has 2 slots, floor 1 has 1 slot
        ParkingFacility p = new ParkingFacility(new int[]{2, 1});
        System.out.println(java.util.Arrays.toString(p.allocate("TN01AB1111")));
        System.out.println(java.util.Arrays.toString(p.allocate("TN02CD2222")));
        System.out.println(java.util.Arrays.toString(p.allocate("TN03EF3333")));
        System.out.println(java.util.Arrays.toString(p.allocate("TN04GH4444")));
        System.out.println(p.release(0, 1) + " " + p.release(0, 1) + " "
                + p.release(5, 0));
        System.out.println(java.util.Arrays.toString(p.allocate("TN04GH4444")));
    }
}
