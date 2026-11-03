# Task01 - Understand the Problem and Identify the Objects

**4 marks - K4 Analyse**  ·  Vignesh S · 192325002

## Files

- **`ProblemAnalysisDemo.java`** - the candidate classes, the business rules, and each rule violated on purpose
- **`ParallelArraysComparison.java`** - alternative: objects vs parallel String arrays, measured

## How to run

```bash
cd Task01
javac ProblemAnalysisDemo.java && java ProblemAnalysisDemo
javac ParallelArraysComparison.java && java ParallelArraysComparison
```

## Result

- 5 candidate classes identified with justified responsibilities and 5 stated assumptions.
- All 5 business rules were violated deliberately and every guard fired (see `actual_output.txt`).
- Alternative measured at n=200,000: **both designs are O(n)** for search, so the object design is kept on correctness, not speed. Parallel arrays save ~16 B/vehicle but cannot express a type hierarchy.

## Evidence in this folder

- `*_output.txt` - the captured console output of each run
- `screenshots/` - the same output as terminal screenshots

Full discussion: [`../REPORT.md`](../REPORT.md)
