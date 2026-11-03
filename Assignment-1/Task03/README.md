# Task03 - Design the Control Logic

**4 marks - K4 Analyse**  ·  Vignesh S · 192325002

## Files

- **`ParkingFacility.java`** - **Complete the Code** - both TODOs filled, `main()` untouched
- **`FeeSlabLogic.java`** - fee slabs written as `if-else` AND `switch`, plus where break/continue/return save work
- **`FastAllocator.java`** - alternative: scanning vs free-count + free-slot stack, measured
- **`PSEUDOCODE.md`** - SIMATS-form pseudocode for ALLOCATE_SLOT and COMPUTE_FEE, with a trace

## How to run

```bash
cd Task03
javac ParkingFacility.java && java ParkingFacility
javac FeeSlabLogic.java && java FeeSlabLogic
javac FastAllocator.java && java FastAllocator
```

## Result

- **Output is identical to the expected output** - `diff actual_output.txt expected_output.txt` reports no difference.
- `if-else` and `switch` slab formulations agree on all 30 cases (3 types x 10 durations).
- The free-slot stack is **7.4x faster** with **100x fewer cell reads**, O(F) vs O(F x S). Not adopted here because the required `main()` fixes the allocation order.

## Evidence in this folder

- `*_output.txt` - the captured console output of each run
- `screenshots/` - the same output as terminal screenshots

Full discussion: [`../REPORT.md`](../REPORT.md)
