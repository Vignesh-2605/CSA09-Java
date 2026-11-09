/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q1  -  Building Text Under Load: String, StringBuilder or StringBuffer?
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * The five ways of assembling one railway e-ticket that this question compares.
 * They are kept in one class so that every workload measures exactly the same
 * field data through five different assembly strategies.
 */
import java.util.StringJoiner;

public final class TicketBuilder {

    private TicketBuilder() { }

    /* ---- 1. the code as it stands today: + inside a loop ---------------- */
    /** Each += creates a NEW String and copies everything written so far. */
    public static String withConcat(String[] fields) {
        String ticket = "";
        for (String f : fields) {
            ticket = ticket + f + "\n";      // two new String objects per field
        }
        return ticket;
    }

    /* ---- 2. StringBuilder: one growable buffer, not synchronized -------- */
    public static String withStringBuilder(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (String f : fields) sb.append(f).append('\n');
        return sb.toString();
    }

    /* ---- 3. StringBuilder with the final length known in advance -------- */
    /** Removes the internal array copies that growing from 16 chars causes. */
    public static String withSizedStringBuilder(String[] fields, int expectedChars) {
        StringBuilder sb = new StringBuilder(expectedChars);
        for (String f : fields) sb.append(f).append('\n');
        return sb.toString();
    }

    /* ---- 4. StringBuffer: identical to StringBuilder but synchronized --- */
    public static String withStringBuffer(String[] fields) {
        StringBuffer sb = new StringBuffer();
        for (String f : fields) sb.append(f).append('\n');
        return sb.toString();
    }

    /* ---- 5. the library helpers the alternative-concept section asks about */
    public static String withStringJoin(String[] fields) {
        return String.join("\n", fields) + "\n";
    }
    public static String withStringJoiner(String[] fields) {
        StringJoiner j = new StringJoiner("\n", "", "\n");
        for (String f : fields) j.add(f);
        return j.toString();
    }
    public static String withFormat(String[] fields) {
        StringBuilder pattern = new StringBuilder(fields.length * 3);
        for (int i = 0; i < fields.length; i++) pattern.append("%s%n");
        return String.format(pattern.toString(), (Object[]) fields);
    }

    /* ---- the field data of one realistic e-ticket ----------------------- */
    public static String[] ticketFields(int fieldCount, int seed) {
        String[] f = new String[fieldCount];
        String[] labels = { "PNR", "TRAIN", "CLASS", "COACH", "BERTH", "NAME",
                            "AGE", "SEX", "FROM", "TO", "DATE", "DEPART",
                            "ARRIVE", "FARE", "GST", "QUOTA", "STATUS", "BOOKED" };
        for (int i = 0; i < fieldCount; i++) {
            String label = labels[i % labels.length];
            f[i] = label + "-" + (i + 1) + ": " + (seed * 31L + i * 7L);
        }
        return f;
    }

    /** Characters the finished ticket will contain, for the pre-sized builder. */
    public static int charCount(String[] fields) {
        int n = 0;
        for (String f : fields) n += f.length() + 1;
        return n;
    }

    /**
     * Intermediate String objects created while building one ticket of n fields.
     *
     * For the + loop: each iteration evaluates ticket + f + "\n", which the
     * compiler turns into a fresh StringBuilder, two appends and one toString -
     * so two throwaway objects per field (the builder and the new String).
     * The characters copied are 1 + 2 + 3 + ... + n field-widths, which is the
     * arithmetic series that makes the work O(n^2) in characters.
     */
    public static long concatObjects(int fields) { return 2L * fields; }
    public static long builderObjects(int fields) { return 2L; }   // the builder, plus one final String
    public static long concatCharsCopied(String[] fields) {
        long running = 0, copied = 0;
        for (String f : fields) {
            copied += running;                 // the whole ticket so far is copied
            running += f.length() + 1;
            copied += f.length() + 1;          // plus the new field
        }
        return copied;
    }
    public static long builderCharsCopied(String[] fields) {
        long n = 0;
        for (String f : fields) n += f.length() + 1;
        return n;                              // each character is written once
    }
}
