# Task10 - Enforce Encapsulation through Packages and Access Modifiers

**3 marks - K4 Analyse**  ·  Vignesh S · 192325002

## Files

- **`src/parking/{model,service,app}/`** - the packaged system; `SlotGrid` and `SlotRow` are package-private
- **`PublicFieldsComparison.java`** - alternative: public fields with caller-side validation
- **`compile_errors/`** - three programs that are MEANT to fail, plus the real `javac` output

## How to run

```bash
cd Task10
javac -d out $(find src -name '*.java')
java -cp out parking.app.EncapsulationDemo
java -cp out parking.app.AutoboxingRiskDemo
javac -d . PublicFieldsComparison.java && java -cp . PublicFieldsComparison
javac -cp out -d /tmp/t10 compile_errors/BreakEncapsulation.java   # expected to FAIL
```

## Result

- Dependency runs one way only: `parking.app -> parking.service -> parking.model`.
- **Five real compiler errors** prove the boundary: the app cannot free a slot, invent a slot, read a raw tariff, see the storage layout, or redefine the billing rule.
- `Integer 900 == Integer 900` is **`false`** while `Integer 90 == Integer 90` is `true` - a fee comparison tested with Rs 90 would pass and then fail at Rs 200.
- Validation costs ~1.7 ns per update; against ~5,000 updates/day that is a few microseconds. No performance case for removing it.

## Evidence in this folder

- `*_output.txt` - the captured console output of each run
- `screenshots/` - the same output as terminal screenshots
- `compile_errors/compiler_errors.txt` - the real `javac` messages, captured

Full discussion: [`../REPORT.md`](../REPORT.md)
