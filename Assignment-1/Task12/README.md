# Task12 - Re-evaluate and Adapt the Design (Day-4 Challenge)

**8 marks - K5 Evaluate**  ·  Vignesh S · 192325002

## Files

- **`src/parking/pricing/`** - **NEW** - `PricingRule` + `HourlyRate`, `FlatFee`, `TimeOfDayRate`, `CappedRule`, `Tariffs`
- **`src/parking/pass/`** - **NEW** - `Pass` + `MonthlyPass`, `OneDayPass`
- **`src/parking/model/`** - revised `Vehicle` (has-a `PricingRule`), plus `EBike` and `DeliveryVan`
- **`src/parking/app/ChallengeTest.java`** - the 64-case harness

## How to run

```bash
cd Task12
javac -d out $(find src -name '*.java')
java -cp out parking.app.ChallengeTest
```

## Result

- **Decision: MODIFY** - keep the hierarchy for identity, move the tariff into a composed `PricingRule`.
- **64 tests run, 64 passed, 0 failed**, including a full regression of the original behaviour (C1-C10).
- Each new vehicle category cost **2 new classes and 0 modifications**. The `PricingRule` abstraction itself cost **6 modified classes** up front - that is the trade, stated plainly.
- The 5 ragged floors {20,50,50,80,12} required **no code change at all** - the jagged array absorbed it, as the Task 2 analysis predicted.
- A seasonal tariff (1.5x between 10:00-14:00, capped at Rs 400) was added using **only existing classes** - a configuration line, not a class.

## Evidence in this folder

- `*_output.txt` - the captured console output of each run
- `screenshots/` - the same output as terminal screenshots

Full discussion: [`../REPORT.md`](../REPORT.md)
