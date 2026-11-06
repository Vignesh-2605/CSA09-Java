# Task11 - Implement, Test and Defend the Design

**5 marks - K5 Evaluate**  ·  Vignesh S · 192325002

## Files

- **`src/parking/{model,service,app}/`** - the complete system
- **`src/parking/app/SystemTest.java`** - the 66-case harness; exits non-zero if any row disagrees
- **`src/parking/app/ParkingApp.java`** - the interactive gate console (Scanner); reads `input.txt`
- **`TicketStoreComparison.java`** - alternative: array vs ArrayList vs HashMap for exit processing

## How to run

```bash
cd Task11
javac -d out $(find src -name '*.java')
java -cp out parking.app.SystemTest
java -cp out parking.app.ParkingApp < input.txt
javac -d . TicketStoreComparison.java && java -cp . TicketStoreComparison
```

## Result

- **66 tests run, 66 passed, 0 failed.** Normal, boundary and invalid cases, the pass rule, and ticket re-issue.
- A day's collection of **Rs 780** computed by the program matches the hand calculation line by line.
- The harness caught a real defect in case B16 - the *expected value* was wrong (60 minutes is one started hour, Rs 30, not Rs 60). Two further cases were added rather than quietly editing the number.
- The HashMap store is **2,295x faster at n = 50,000**; per-operation cost stays flat while array and list grow linearly, i.e. **O(n) vs O(n^2)** for a full day. Recommendation: `LinkedHashMap` (keeps entry order for the report).

## Evidence in this folder

- `*_output.txt` - the captured console output of each run
- `screenshots/` - the same output as terminal screenshots

Full discussion: [`../REPORT.md`](../REPORT.md)
