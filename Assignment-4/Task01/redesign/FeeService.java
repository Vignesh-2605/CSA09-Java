/* Assignment 4 (CO4) Task 1 - validation and processing, with NO Swing import.
 *
 * This class is the whole argument of the redesign.  It contains every rule
 * the old event handler contained, and it can be tested from a plain main()
 * with no window on screen - see FeeServiceTest in this folder.
 *
 * Name: Vignesh S   Register Number: 192325002 */
import java.sql.SQLException;
import java.util.Set;

public class FeeService {

    public static final Set<String> FEE_TYPES =
            Set.of("Tuition", "Hostel", "Exam", "Library fine");
    private static final long MAX_PAISE = 5_00_000_00L;      // Rs 5,00,000

    private final FeeRepository repository;
    public FeeService(FeeRepository repository) { this.repository = repository; }

    /** Parses and checks the raw text the window collected. */
    public Payment validate(String rawReg, String rawFeeType, String rawAmount)
            throws ValidationException {

        String reg = rawReg == null ? "" : rawReg.trim();
        if (reg.isEmpty())
            throw new ValidationException("registerNumber", "Register number is required");
        if (!reg.matches("\\d{9}"))
            throw new ValidationException("registerNumber",
                    "Register number must be exactly 9 digits");

        // equals(), not ==.  Compare by VALUE, because the text came from a
        // component at run time and is not an interned literal.
        if (rawFeeType == null || !FEE_TYPES.contains(rawFeeType))
            throw new ValidationException("feeType", "Choose a fee type");

        String amt = rawAmount == null ? "" : rawAmount.trim();
        if (amt.isEmpty())
            throw new ValidationException("amount", "Amount is required");

        // The amount must LOOK like money before BigDecimal is asked to parse
        // it.  BigDecimal accepts "1e5" and "+1500", which are valid numbers
        // but not plausible things for a clerk to type into a rupee field;
        // our own test caught "1e5" being accepted as Rs 1,00,000.
        if (!amt.matches("\\d{1,7}(\\.\\d{1,2})?"))
            throw new ValidationException("amount",
                    "Amount must be digits, optionally with two decimal places");

        long paise;
        try {
            // Money is parsed into PAISE as a long.  Never a double: 0.1 + 0.2
            // is not 0.3 in binary floating point, and a fee ledger cannot
            // carry that error.
            java.math.BigDecimal rupees = new java.math.BigDecimal(amt);
            paise = rupees.movePointRight(2).longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new ValidationException("amount",
                    "Amount must be a number with at most two decimal places");
        }
        if (paise <= 0)
            throw new ValidationException("amount", "Amount must be greater than zero");
        if (paise > MAX_PAISE)
            throw new ValidationException("amount", "Amount exceeds the Rs 5,00,000 counter limit");

        return new Payment(reg, rawFeeType, paise);
    }

    /** Validates, then stores.  Returns the receipt number. */
    public long pay(String rawReg, String rawFeeType, String rawAmount)
            throws ValidationException, SQLException {
        return repository.save(validate(rawReg, rawFeeType, rawAmount));
    }

    public Payment receiptFor(String registerNumber) throws SQLException {
        return repository.findLatest(registerNumber);
    }
}
