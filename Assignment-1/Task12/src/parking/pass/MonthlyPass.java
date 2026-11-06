package parking.pass;

/** The original entitlement, now an object: valid across a range of days. */
public final class MonthlyPass implements Pass {

    private final int firstDay;
    private final int lastDay;

    public MonthlyPass(int firstDay, int days) {
        if (firstDay < 1 || firstDay > 366) throw new IllegalArgumentException("day must be 1..366");
        if (days < 1) throw new IllegalArgumentException("a pass must cover at least one day");
        this.firstDay = firstDay;
        this.lastDay = firstDay + days - 1;
    }

    @Override public boolean coversStay(int dayOfYear, int entryMinute) {
        return dayOfYear >= firstDay && dayOfYear <= lastDay;
    }
    @Override public long priceWhenSold() { return 600; }
    @Override public String describe() { return "monthly pass, days " + firstDay + "-" + lastDay; }
}
