package parking.pass;

/** NEW: a pass a visitor may buy, valid for one calendar day only. */
public final class OneDayPass implements Pass {

    private final int validDay;

    public OneDayPass(int validDay) {
        if (validDay < 1 || validDay > 366) throw new IllegalArgumentException("day must be 1..366");
        this.validDay = validDay;
    }

    @Override public boolean coversStay(int dayOfYear, int entryMinute) {
        return dayOfYear == validDay;
    }
    @Override public long priceWhenSold() { return 100; }
    @Override public String describe() { return "one-day pass, day " + validDay; }
}
