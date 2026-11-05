# Task07 - Override and Specialise Behaviour

**4 marks - K4 Analyse**  ·  Vignesh S · 192325002

## Files

- **`OverrideDemo.java`** - overriding, `@Override`, `final` on variable/method/class, `equals`/`hashCode`/`toString`
- **`SwitchFeeComparison.java`** - alternative: overriding vs a `switch` on a type field, k = 12
- **`compile_errors/`** - two programs that are MEANT to fail, plus the real `javac` output

## How to run

```bash
cd Task07
javac OverrideDemo.java && java OverrideDemo
javac SwitchFeeComparison.java && java SwitchFeeComparison
cd compile_errors && javac -cp .. BrokenBike.java SneakyCar.java   # expected to FAIL
```

## Result

- Two **real compiler errors** are captured: a wrong `@Override` signature, and an attempt to override the `final` `calculateFee()` - the guarantee that no subclass can make parking free.
- `HashSet` refuses the 2nd and 3rd objects carrying the same plate; `HashMap.get()` finds a stay from a **different object** and even a **different subclass**.
- A dense `int` switch compiles to `tableswitch` and is **O(1) in k, not O(k)** - the report corrects the textbook claim. The measured `if-else` chain costs 3.85 ns for the first type and 6.25 ns for the last.

## Evidence in this folder

- `*_output.txt` - the captured console output of each run
- `screenshots/` - the same output as terminal screenshots
- `compile_errors/compiler_errors.txt` - the real `javac` messages, captured

Full discussion: [`../REPORT.md`](../REPORT.md)
