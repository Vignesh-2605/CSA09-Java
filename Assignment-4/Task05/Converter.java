/* Assignment 4 (CO4) Task 5 - the conversion rule, shared by all four styles
 * so that the ONLY difference between them is how the event is wired.
 * Name: Vignesh S   Register Number: 192325002 */
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public final class Converter {
    /** Rates against INR, fixed so the four versions are comparable. */
    public static final Map<String, BigDecimal> RATES = new LinkedHashMap<>();
    static {
        RATES.put("INR", new BigDecimal("1.00"));
        RATES.put("USD", new BigDecimal("83.20"));
        RATES.put("EUR", new BigDecimal("90.15"));
        RATES.put("GBP", new BigDecimal("105.40"));
        RATES.put("JPY", new BigDecimal("0.55"));
    }
    public static String[] codes() { return RATES.keySet().toArray(new String[0]); }

    public static class BadAmount extends Exception {
        public BadAmount(String m) { super(m); }
    }

    /** Shared by every style: parse, check, convert. */
    public static String convert(String raw, String from, String to) throws BadAmount {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) throw new BadAmount("Enter an amount");
        if (!s.matches("\\d{1,9}(\\.\\d{1,2})?"))
            throw new BadAmount("\"" + s + "\" is not a valid amount");
        BigDecimal amt = new BigDecimal(s);
        if (amt.signum() <= 0) throw new BadAmount("Amount must be greater than zero");
        BigDecimal inr = amt.multiply(RATES.get(from));
        BigDecimal out = inr.divide(RATES.get(to), 2, RoundingMode.HALF_UP);
        return String.format("%s %s = %s %s", s, from, out.toPlainString(), to);
    }
}
