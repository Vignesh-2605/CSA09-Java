# Task04 - Design Classes, Objects and Constructors

**4 marks - K4 Analyse**  ·  Vignesh S · 192325002

## Files

- **`ConstructorDemo.java`** - 4 constructors, `this(...)` chaining, the copy constructor, stack vs heap
- **`ObjectCreationComparison.java`** - alternative: chained constructors vs default+setters vs static factory

## How to run

```bash
cd Task04
javac ConstructorDemo.java && java ConstructorDemo
javac ObjectCreationComparison.java && java ObjectCreationComparison
```

## Result

- The chaining order is proved by the trace: the **full constructor prints first** in every case, because `this(...)` must be the first statement.
- The copy constructor keeps the stay (same vehicle, slot, entry) but takes a **new ticket number** and is flagged `duplicate`. A plain assignment gives `alias == t1 -> true`: one object, two names.
- All three creation strategies are **O(n)** and measure within 1-2 ns of each other - the JIT inlines the setters. The decision is made on the invariant, not on speed.

## Evidence in this folder

- `*_output.txt` - the captured console output of each run
- `screenshots/` - the same output as terminal screenshots

Full discussion: [`../REPORT.md`](../REPORT.md)
