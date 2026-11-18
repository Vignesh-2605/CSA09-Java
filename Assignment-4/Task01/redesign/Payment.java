/* Assignment 4 (CO4) Task 1 - the redesign: the data, with no UI and no JDBC.
 * Name: Vignesh S   Register Number: 192325002 */
public record Payment(String registerNumber, String feeType, long amountPaise) {
    public String amountRupees() { return String.format("Rs %.2f", amountPaise / 100.0); }
}
