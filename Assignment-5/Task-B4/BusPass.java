/* Programming in Java - Assignment 5 (CO5)
 * The bus-pass record, as designed in Part A, Task A1.
 * Name: Vignesh S   Register Number: 192325002 */
import java.time.LocalDate;

public record BusPass(String passId, String registerNo, String name,
                      String routeNo, String boardingStop,
                      LocalDate validUntil, long amountPaise) {

    public static final char DELIM = '|';

    /** The delimited-text layout: 7 fields, pipe separated, one record per line. */
    public String toLine() {
        return String.join(String.valueOf(DELIM),
                passId, registerNo, name, routeNo, boardingStop,
                validUntil.toString(), String.valueOf(amountPaise));
    }

    /** Throws DamagedRecordException rather than returning null, so the caller
     *  must decide what a damaged line means. */
    public static BusPass parse(String line) throws DamagedRecordException {
        if (line == null || line.isBlank())
            throw new DamagedRecordException("empty line");
        String[] f = line.split("\\" + DELIM, -1);     // -1 keeps trailing empties
        if (f.length != 7)
            throw new DamagedRecordException("expected 7 fields, found " + f.length);
        try {
            return new BusPass(f[0].trim(), f[1].trim(), f[2].trim(), f[3].trim(), f[4].trim(),
                    LocalDate.parse(f[5].trim()), Long.parseLong(f[6].trim()));
        } catch (java.time.format.DateTimeParseException e) {
            throw new DamagedRecordException("bad date: " + f[5]);
        } catch (NumberFormatException e) {
            throw new DamagedRecordException("bad amount: " + f[6]);
        }
    }

    public String amountRupees() { return String.format("Rs %.2f", amountPaise / 100.0); }

    public static class DamagedRecordException extends Exception {
        public DamagedRecordException(String m) { super(m); }
    }
}
