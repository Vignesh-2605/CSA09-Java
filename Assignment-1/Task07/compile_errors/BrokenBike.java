/* Task 7 - evidence that @Override catches a bad signature at COMPILE time.
 * This file is NOT part of the working program; it exists to be compiled and fail.
 *   javac -cp .. BrokenBike.java
 */
class BrokenBike extends TwoWheeler {
    BrokenBike(String r) { super(r); }
    @Override protected double ratePerHour(int unused) { return 5; }   // wrong signature
}
