# Task08 - Apply Polymorphism

**4 marks - K4 Analyse**  ·  Vignesh S · 192325002

## Files

- **`FeeDemo.java`** - **Complete the Code** - all three TODOs filled, `main()` untouched
- **`PolymorphismDemo.java`** - overloaded `calculateFee`, upcasting, and dynamic dispatch traced
- **`InstanceofComparison.java`** - alternative: dispatch vs an `instanceof` + cast chain, k = 12
- **`EQUALS_BEHAVIOUR.md`** - why the last line prints `true`, and whether that is correct for this system

## How to run

```bash
cd Task08
javac FeeDemo.java && java FeeDemo
javac PolymorphismDemo.java && java PolymorphismDemo
javac InstanceofComparison.java && java InstanceofComparison
```

## Result

- **Output is identical to the expected output** - `diff actual_output.txt expected_output.txt` reports no difference.
- Dispatch traced per element: declared type `Vehicle`, actual type discovered at run time, and the body that actually ran printed from inside it.
- The `instanceof` chain costs **2.2x more for the last type than the first** (8.92 ns vs 4.10 ns) while dispatch is position-independent at 6.33 ns.

## Evidence in this folder

- `*_output.txt` - the captured console output of each run
- `screenshots/` - the same output as terminal screenshots

Full discussion: [`../REPORT.md`](../REPORT.md)
