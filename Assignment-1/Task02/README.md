# Task02 - Choose the Data Representation

**3 marks - K4 Analyse**  ·  Vignesh S · 192325002

## Files

- **`DataRepresentationDemo.java`** - data types per field, the jagged array, and the casting/integer-division risk
- **`ScannerInputDemo.java`** - Scanner input with `hasNextInt()` validation; reads `input.txt`
- **`SlotStorageComparison.java`** - alternative: jagged vs flat-with-stride vs prefix-sum vs ArrayList

## How to run

```bash
cd Task02
javac DataRepresentationDemo.java && java DataRepresentationDemo
javac ScannerInputDemo.java && java ScannerInputDemo < input.txt
javac SlotStorageComparison.java && java SlotStorageComparison
```

## Result

- Integer division / a careless cast under-charges by **Rs 180 over 9 sample stays**; a 1-minute stay bills Rs 0. `(minutes + 59) / 60` fixes it with pure integer arithmetic.
- The jagged array fits {20,50,50,100,100} in **320 cells**; a rectangular `String[5][100]` wastes **180**.
- A single stride is not merely wasteful but **wrong**: `floor*20 + slot` makes floor 1 slot 25 collide with floor 2 slot 5.
- Scanner run: 4 accepted, 3 rejected (non-numeric duration, unknown type, negative duration).

## Evidence in this folder

- `*_output.txt` - the captured console output of each run
- `screenshots/` - the same output as terminal screenshots

Full discussion: [`../REPORT.md`](../REPORT.md)
