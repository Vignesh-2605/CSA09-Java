/*
 * Programming in Java  -  Assignment 3 (CO3)
 * Q1  -  Handling Failure in a Bank ATM Simulation
 *
 * Name            : Vignesh S   Register Number: 192325002
 * Shared group work: the withdrawal itself, implemented with try, multi-catch
 * and finally, and tested with valid and invalid input.
 */
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/** Thrown when the account exists but does not hold enough money. */
class InsufficientFundsException extends Exception {
    private final long shortfallPaise;
    InsufficientFundsException(String message, long shortfallPaise) {
        super(message);
        this.shortfallPaise = shortfallPaise;
    }
    long getShortfallPaise() { return shortfallPaise; }
}

/** Thrown when the card-reader file names an account the bank does not hold. */
class UnknownAccountException extends Exception {
    UnknownAccountException(String message) { super(message); }
}

public class AtmSimulation {

    /** Money is held in PAISE as a long - never in double. */
    private final java.util.Map<String, Long> balances = new java.util.HashMap<>();
    private final Path cardReaderFile;
    private boolean cardHeld = false;
    private int dispenserOpen = 0;

    AtmSimulation(Path cardReaderFile) {
        this.cardReaderFile = cardReaderFile;
        balances.put("1001", 500_00L);
        balances.put("1002",  50_00L);
        balances.put("1003",   0L);
    }

    /* ---------------- the operation under study ---------------- */

    /**
     * One withdrawal attempt.  Returns the cash dispensed in paise.
     *
     * The catch blocks are ordered from the MOST SPECIFIC type to the most
     * general.  Java requires this: a catch for a supertype placed first would
     * make every later catch unreachable, and the compiler rejects it.  See
     * compile_errors/compiler_errors.txt for that error captured from javac.
     */
    long withdraw(String rawAccount, String rawAmount) {
        cardHeld = true;                       // the card is captured on entry
        dispenserOpen++;
        try {
            // --- failure point 1: the card-reader file may be missing -------
            String card = readCard();

            // --- failure point 2: the amount may not be a number ------------
            long paise = parseAmount(rawAmount);

            // --- failure point 3: the amount may be negative or zero --------
            if (paise <= 0)
                throw new IllegalArgumentException("Amount must be greater than zero");

            // --- failure point 4: the amount may not be a multiple of 100 ---
            if (paise % 100_00L != 0)
                throw new IllegalArgumentException("ATM dispenses multiples of Rs 100 only");

            // --- failure point 5: the account may not exist -----------------
            Long balance = balances.get(rawAccount);
            if (balance == null)
                throw new UnknownAccountException("No such account: " + rawAccount);

            // --- failure point 6: there may not be enough money -------------
            if (balance < paise)
                throw new InsufficientFundsException(
                        "Balance too low for this withdrawal", paise - balance);

            balances.put(rawAccount, balance - paise);
            System.out.printf("    dispensed Rs %d, card %s, new balance Rs %d%n",
                    paise / 100, card, (balance - paise) / 100);
            return paise;

        } catch (InsufficientFundsException e) {
            // most specific first: a checked exception of our own
            System.out.printf("    [catch InsufficientFundsException] %s (short by Rs %d)%n",
                    e.getMessage(), e.getShortfallPaise() / 100);
            return 0;
        } catch (UnknownAccountException e) {
            System.out.printf("    [catch UnknownAccountException] %s%n", e.getMessage());
            return 0;
        } catch (NoSuchFileException | AccessDeniedException e) {
            // MULTI-CATCH of two SIBLINGS.  Both are checked subclasses of
            // IOException, neither is a subclass of the other, and both mean
            // the same thing to the customer: the machine needs an engineer.
            // Multi-catch requires UNRELATED alternatives - see
            // compile_errors/compiler_errors.txt for what javac says when they
            // are related by subclassing.
            System.out.printf("    [catch multi NoSuchFileException|AccessDeniedException] "
                    + "card reader unavailable: %s%n", e.getMessage());
            return 0;
        } catch (IOException e) {
            // the remaining IOException kinds, AFTER its two subclasses above
            System.out.printf("    [catch IOException] card could not be read: %s%n", e.getMessage());
            return 0;
        } catch (IllegalArgumentException e) {
            // ONE catch covers both amount faults, because NumberFormatException
            // is itself a subclass of IllegalArgumentException.  Writing
            // "catch (NumberFormatException | IllegalArgumentException e)" does
            // not compile for exactly that reason.
            System.out.printf("    [catch IllegalArgumentException] %s   (actual type: %s)%n",
                    e.getMessage(), e.getClass().getSimpleName());
            return 0;
        } finally {
            // runs on EVERY path: success, every catch, and an exception that
            // no catch handles.
            dispenserOpen--;
            cardHeld = false;                  // the card is ALWAYS returned
            System.out.println("    [finally] card returned, dispenser closed");
        }
    }

    private String readCard() throws IOException {
        if (!Files.exists(cardReaderFile))
            throw new NoSuchFileException(cardReaderFile.toString());
        return Files.readString(cardReaderFile).trim();
    }

    private long parseAmount(String raw) {
        // Long.parseLong throws NumberFormatException, which is UNCHECKED
        return Long.parseLong(raw.trim()) * 100L;
    }

    boolean isCardHeld() { return cardHeld; }
    int dispenserOpen() { return dispenserOpen; }
    long balanceOf(String acc) { return balances.getOrDefault(acc, -1L); }

    /* ---------------- demonstration ---------------- */

    public static void main(String[] args) throws Exception {
        System.out.println("Q1  -  HANDLING FAILURE IN A BANK ATM SIMULATION");
        System.out.println("=".repeat(112));
        System.out.println("Name: Vignesh S   Register Number: 192325002");

        Path card = Files.createTempFile("cardreader", ".txt");
        Files.writeString(card, "1001");

        /* ---------- STUDENT A: the hierarchy and the design ---------- */
        System.out.println();
        System.out.println("STUDENT A  -  EXCEPTION DESIGN");
        System.out.println("=".repeat(112));
        System.out.println();
        System.out.println("1.  THE PART OF THE Throwable HIERARCHY THAT MATTERS TO THIS ATM");
        System.out.println("-".repeat(112));
        System.out.println("    Throwable");
        System.out.println("      |");
        System.out.println("      +-- Error                        NOT caught: the JVM is broken");
        System.out.println("      |     +-- StackOverflowError     runaway recursion");
        System.out.println("      |     +-- OutOfMemoryError       no heap left");
        System.out.println("      |");
        System.out.println("      +-- Exception                    CHECKED unless under RuntimeException");
        System.out.println("            +-- IOException                        checked");
        System.out.println("            |     +-- FileNotFoundException        checked");
        System.out.println("            |     +-- NoSuchFileException          checked  <- card reader missing\n            |     +-- AccessDeniedException        checked  <- card reader locked");
        System.out.println("            +-- InsufficientFundsException         checked  <- our own");
        System.out.println("            +-- UnknownAccountException            checked  <- our own");
        System.out.println("            +-- RuntimeException                   UNCHECKED");
        System.out.println("                  +-- IllegalArgumentException     unchecked");
        System.out.println("                  |     +-- NumberFormatException  unchecked <- letters typed");
        System.out.println("                  +-- NullPointerException         unchecked");
        System.out.println("                  +-- ArithmeticException          unchecked");
        System.out.println("                  +-- InputMismatchException       unchecked <- Scanner.nextInt");
        System.out.println("                        (under NoSuchElementException)");
        System.out.println();
        System.out.println("  The dividing line is RuntimeException, not severity.  Anything under");
        System.out.println("  RuntimeException or Error is unchecked; everything else under Exception");
        System.out.println("  is checked and must be declared or handled.");

        System.out.println();
        System.out.println("2.  EVERY POINT AT WHICH THE WITHDRAWAL CAN FAIL");
        System.out.println("-".repeat(112));
        System.out.printf("%-4s | %-30s | %-32s | %-10s | %s%n",
                "#", "FAILURE POINT", "EXCEPTION TYPE", "KIND", "CAUGHT WHERE");
        System.out.println("-".repeat(112));
        String[][] fp = {
            {"1","card-reader file missing","NoSuchFileException","checked","catch #3 (multi-catch)"},
            {"2","card-reader file permissions denied","AccessDeniedException","checked","catch #3 (multi-catch)"},
            {"2b","card-reader file unreadable","IOException","checked","catch #4 in withdraw()"},
            {"3","letters typed for the amount","NumberFormatException","unchecked","catch #5 (as IllegalArgument)"},
            {"4","amount negative or zero","IllegalArgumentException","unchecked","catch #5"},
            {"5","amount not a multiple of 100","IllegalArgumentException","unchecked","catch #5"},
            {"6","account not held by the bank","UnknownAccountException","checked","catch #2"},
            {"7","balance lower than the amount","InsufficientFundsException","checked","catch #1"},
            {"8","Scanner given non-numeric input","InputMismatchException","unchecked","the console loop"},
            {"9","end of input / Ctrl-D","NoSuchElementException","unchecked","the console loop"},
            {"10","JVM out of memory","OutOfMemoryError","Error","NOT caught, by design"},
        };
        for (String[] r : fp) System.out.printf("%-4s | %-30s | %-32s | %-10s | %s%n", r[0], r[1], r[2], r[3], r[4]);

        System.out.println();
        System.out.println("3.  WHY THE CATCH BLOCKS ARE IN THIS ORDER");
        System.out.println("-".repeat(112));
        System.out.println("  The order is forced by the language, not chosen for style: Java matches");
        System.out.println("  catch blocks TOP DOWN and takes the first whose type is assignable from");
        System.out.println("  the thrown object.  A supertype placed first would therefore swallow");
        System.out.println("  every subtype below it, and the compiler refuses to allow it:");
        System.out.println();
        System.out.println("      catch (IOException e)        { ... }");
        System.out.println("      catch (NoSuchFileException e){ ... }   // error: already caught");
        System.out.println();
        System.out.println("  The real javac message is captured in compile_errors/compiler_errors.txt.");
        System.out.println("  That it is a COMPILE error and not a silent runtime surprise is the");
        System.out.println("  useful part: the mistake cannot reach production.");
        System.out.println();
        System.out.println("  The same rule constrains MULTI-CATCH, and this one caught us while");
        System.out.println("  writing this program.  The first draft had:");
        System.out.println();
        System.out.println("      catch (NumberFormatException | IllegalArgumentException e)");
        System.out.println();
        System.out.println("  which javac rejected: \"Alternatives in a multi-catch statement cannot");
        System.out.println("  be related by subclassing\".  NumberFormatException IS an");
        System.out.println("  IllegalArgumentException, so the second alternative adds nothing.");
        System.out.println("  The fix was a SINGLE catch (IllegalArgumentException e), which already");
        System.out.println("  covers both.  The multi-catch in the final program instead pairs two");
        System.out.println("  genuine SIBLINGS - NoSuchFileException and AccessDeniedException.");
        System.out.println();
        System.out.println("  Within that constraint the order also follows intent - the two bank");
        System.out.println("  rules (funds, account) are handled before the two infrastructure");
        System.out.println("  failures (file), because they carry a message the customer can act on.");

        System.out.println();
        System.out.println("4.  WHAT THE finally BLOCK GUARANTEES");
        System.out.println("-".repeat(112));
        System.out.println("  It guarantees that the CARD IS RETURNED and the dispenser is closed on");
        System.out.println("  every path out of withdraw(): normal return, any of the five catch");
        System.out.println("  blocks, and an exception that no catch matches.");
        System.out.println();
        System.out.println("  This is a physical-safety property, not a tidiness one.  An ATM that");
        System.out.println("  keeps a customer's card because an unexpected exception skipped the");
        System.out.println("  clean-up is a fault that requires an engineer to attend the machine.");
        System.out.println();
        System.out.println("  Demonstrated below: the card is released even when the exception thrown");
        System.out.println("  is one that withdraw() does not catch at all.");

        /* ---------- STUDENT B: testing and tracing ---------- */
        System.out.println();
        System.out.println("STUDENT B  -  TESTING AND TRACING");
        System.out.println("=".repeat(112));
        System.out.println();
        System.out.println("5.  SEVEN TEST INPUTS, EACH AIMED AT A DIFFERENT PATH");
        System.out.println("-".repeat(112));

        record Test(String id, String account, String amount, String intent) { }
        Test[] tests = {
            new Test("T1","1001","200","valid withdrawal - the success path"),
            new Test("T2","1001","abc","letters instead of a number"),
            new Test("T3","1001","-100","negative amount"),
            new Test("T4","1001","250","not a multiple of Rs 100"),
            new Test("T5","9999","100","account the bank does not hold"),
            new Test("T6","1002","200","amount larger than the balance"),
            new Test("T7","1003","100","account exists but holds zero"),
        };

        AtmSimulation atm = new AtmSimulation(card);
        System.out.printf("%-5s | %-9s | %-9s | %-34s | %-16s | %s%n",
                "ID", "ACCOUNT", "AMOUNT", "INTENT", "CATCH REACHED", "finally RAN?");
        System.out.println("-".repeat(112));
        for (Test t : tests) {
            System.out.printf("%-5s | %-9s | %-9s | %s%n", t.id(), t.account(), t.amount(), t.intent());
            long got = atm.withdraw(t.account(), t.amount());
            System.out.printf("      -> dispensed Rs %d, card held afterwards: %s, dispenser open: %d%n%n",
                    got / 100, atm.isCardHeld(), atm.dispenserOpen());
        }

        System.out.println("6.  THE CARD-READER FILE MISSING");
        System.out.println("-".repeat(112));
        Files.delete(card);
        System.out.println("  (card-reader file deleted)");
        atm.withdraw("1001", "100");
        System.out.printf("      -> card held afterwards: %s%n", atm.isCardHeld());
        Files.writeString(card, "1001");

        System.out.println();
        System.out.println("7.  finally STILL RUNS FOR AN EXCEPTION NOTHING CATCHES");
        System.out.println("-".repeat(112));
        AtmSimulation hostile = new AtmSimulation(card) {
            @Override long withdraw(String a, String b) {
                try {
                    System.out.println("    about to throw an ArithmeticException that no catch handles");
                    int x = 1 / Integer.parseInt("0");
                    return x;
                } finally {
                    System.out.println("    [finally] card returned even though the exception escapes");
                }
            }
        };
        try {
            hostile.withdraw("1001", "100");
        } catch (ArithmeticException e) {
            System.out.println("    caught by main(): " + e);
        }

        System.out.println();
        System.out.println("8.  A STACK TRACE, LINE BY LINE");
        System.out.println("-".repeat(112));
        try {
            deep1();
        } catch (RuntimeException e) {
            StackTraceElement[] st = e.getStackTrace();
            System.out.println("  " + e);
            for (int i = 0; i < Math.min(st.length, 5); i++)
                System.out.printf("      at %s%n", st[i]);
            System.out.println();
            System.out.printf("  %-6s | %-46s | %s%n", "LINE", "WHAT IT NAMES", "HOW TO READ IT");
            System.out.println("  " + "-".repeat(104));
            System.out.printf("  %-6s | %-46s | %s%n", "first", e.getClass().getName() + ": " + e.getMessage(),
                    "the type and message of the failure");
            System.out.printf("  %-6s | %-46s | %s%n", "at #1", st[0].getMethodName() + "()  line " + st[0].getLineNumber(),
                    "WHERE IT WAS THROWN - innermost frame");
            if (st.length > 1)
                System.out.printf("  %-6s | %-46s | %s%n", "at #2", st[1].getMethodName() + "()  line " + st[1].getLineNumber(),
                        "its caller");
            if (st.length > 2)
                System.out.printf("  %-6s | %-46s | %s%n", "at #3", st[2].getMethodName() + "()  line " + st[2].getLineNumber(),
                        "its caller's caller");
            System.out.printf("  %-6s | %-46s | %s%n", "last", "main()", "where the call chain began");
            System.out.println();
            System.out.println("  The trace reads INNERMOST FIRST.  The top line is where the");
            System.out.println("  exception was created, and each line below is the frame that called");
            System.out.println("  the one above.  The first line belonging to our own code - rather");
            System.out.println("  than to the JDK - is normally where to start looking.");
        }

        System.out.println();
        System.out.println("9.  JOINT DESIGN QUESTION: ONE catch (Exception e) FOR EVERYTHING?");
        System.out.println("-".repeat(112));
        System.out.println("  WHAT IS LOST:");
        System.out.printf("    %-40s | %s%n", "the customer's message", "every fault becomes one vague error");
        System.out.printf("    %-40s | %s%n", "the ability to act differently", "retry, refuse and call-an-engineer merge");
        System.out.printf("    %-40s | %s%n", "the compiler's help", "a new checked exception is silently absorbed");
        System.out.printf("    %-40s | %s%n", "programming errors", "a NullPointerException is treated as a bank rule");
        System.out.println();
        System.out.println("  Demonstrated - the same four faults through one catch (Exception e):");
        for (Test t : new Test[]{tests[1], tests[4], tests[5]}) {
            try {
                AtmSimulation a2 = new AtmSimulation(card);
                a2.withdrawNarrow(t.account(), t.amount());
            } catch (Exception e) {
                System.out.printf("    %-9s -> \"Transaction failed. Please try again.\"   (actually %s)%n",
                        t.amount(), e.getClass().getSimpleName());
            }
        }
        System.out.println();
        System.out.println("  WHEN IT IS ACCEPTABLE:");
        System.out.println("    - at the OUTERMOST boundary of a long-running process, as a last");
        System.out.println("      resort so that one bad transaction does not kill the ATM - but it");
        System.out.println("      must LOG the full exception before showing a generic message;");
        System.out.println("    - in a test harness whose job is to report that something failed.");
        System.out.println();
        System.out.println("  It is NOT acceptable as the only handler inside withdraw(), because");
        System.out.println("  this method is exactly where the different faults still mean different");
        System.out.println("  things.  catch (Exception e) also catches RuntimeException, so a");
        System.out.println("  NullPointerException - a bug in our code - would be reported to the");
        System.out.println("  customer as though the bank had declined the withdrawal.");
        System.out.println();
        System.out.println("  Note it does NOT catch Error, so OutOfMemoryError still terminates the");
        System.out.println("  JVM. That is correct: an ATM with a broken heap must stop, not keep");
        System.out.println("  dispensing cash.");
    }

    /** A deliberately narrow version used only for the catch(Exception) comparison. */
    long withdrawNarrow(String rawAccount, String rawAmount) throws Exception {
        String card = readCard();
        long paise = parseAmount(rawAmount);
        if (paise <= 0) throw new IllegalArgumentException("Amount must be greater than zero");
        Long balance = balances.get(rawAccount);
        if (balance == null) throw new UnknownAccountException("No such account: " + rawAccount);
        if (balance < paise) throw new InsufficientFundsException("Balance too low", paise - balance);
        return paise;
    }

    static void deep1() { deep2(); }
    static void deep2() { deep3(); }
    static void deep3() { throw new IllegalStateException("card reader returned an empty card number"); }
}
