# Task09 - Introduce Abstraction

**4 marks - K4 Analyse**  ·  Vignesh S · 192325002

## Files

- **`AbstractionDemo.java`** - abstract class + 3 interfaces, `default` and `static` interface methods
- **`InvertedAbstractionComparison.java`** - alternative: `Vehicle` as an interface, `Chargeable` as an abstract class
- **`compile_errors/`** - two programs that are MEANT to fail, plus the real `javac` output

## How to run

```bash
cd Task09
javac AbstractionDemo.java && java AbstractionDemo
javac InvertedAbstractionComparison.java && java InvertedAbstractionComparison
cd compile_errors && javac -cp .. InstantiateAbstract.java && javac BadInterface.java   # expected to FAIL
```

## Result

- Three **real compiler errors** justify the split: an abstract class cannot be instantiated, and an interface cannot hold instance state or a constructor.
- The test applied: *what IS it?* -> abstract class; *what can it DO?* -> interface.
- The inverted design compiles and runs, but a `default` method cannot be `final`, so `BFreeloader` legally bills **Rs 0** where Rs 90 is due. Per-object space is unchanged; the time difference is ~2 ns.

## Evidence in this folder

- `*_output.txt` - the captured console output of each run
- `screenshots/` - the same output as terminal screenshots
- `compile_errors/compiler_errors.txt` - the real `javac` messages, captured

Full discussion: [`../REPORT.md`](../REPORT.md)
