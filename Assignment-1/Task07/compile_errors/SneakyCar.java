/* Task 7 - evidence that a final method cannot be overridden.
 *   javac -cp .. SneakyCar.java
 */
class SneakyCar extends Car {
    SneakyCar(String r) { super(r); }
    @Override public long calculateFee(int m) { return 0; }            // the rule is final
}
