package parking.model;

/** A slot now knows how much room it has, so a van cannot take a bike bay. */
public class Slot {

    private final int floor;
    private final int number;
    private final int units;                 // capacity in slot units
    private String occupantRegNo;

    Slot(int floor, int number, int units) {
        this.floor = floor; this.number = number; this.units = units;
    }

    public boolean isFree() { return occupantRegNo == null; }
    public boolean fits(VehicleCategory c) { return c.slotUnits() <= units; }
    public int getFloor() { return floor; }
    public int getNumber() { return number; }
    public int getUnits() { return units; }
    public String getOccupantRegNo() { return occupantRegNo; }

    void occupy(String regNo) {
        if (!isFree())
            throw new IllegalStateException("slot " + this + " already holds " + occupantRegNo);
        this.occupantRegNo = regNo;
    }
    void free() {
        if (isFree()) throw new IllegalStateException("slot " + this + " is already free");
        this.occupantRegNo = null;
    }

    @Override public String toString() { return floor + "/" + number; }
}
