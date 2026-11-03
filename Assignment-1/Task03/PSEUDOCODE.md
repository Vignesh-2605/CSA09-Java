# Task 3 - Pseudocode for the Two Core Operations

**Name:** Vignesh S  **Register Number:** 192325002
**Course:** Programming in Java - Assignment 1 (CO1)

Written in the SIMATS Engineering standard form for algorithmic pseudocode:
a named algorithm, a declared input and output, numbered steps, structured
control words in upper case, explicit `BEGIN`/`END` block delimiters, and a
single exit point per algorithm.

---

## ALGORITHM 1 - ALLOCATE_SLOT

```
ALGORITHM   ALLOCATE_SLOT
PURPOSE     Find the first free slot in the facility, occupy it, and report
            its position.
INPUT       slots[0..F-1][...]  : jagged array of vehicle numbers, NULL = free
            F                   : number of floors
            vehicleNo           : registration number of the arriving vehicle
OUTPUT      position            : the pair (floor, slot) that was occupied,
                                  or NULL if no slot was free
PRE         vehicleNo is not NULL and is not already parked
POST        Exactly one cell of slots changes from NULL to vehicleNo, or no
            cell changes at all

BEGIN
 1.  FOR floor <- 0 TO F - 1 DO
 2.      S <- LENGTH(slots[floor])            // each floor carries its own size
 3.      FOR slot <- 0 TO S - 1 DO
 4.          IF slots[floor][slot] = NULL THEN
 5.              slots[floor][slot] <- vehicleNo
 6.              position <- (floor, slot)
 7.              RETURN position               // stops both loops at once
 8.          END IF
 9.      END FOR
10.  END FOR
11.  position <- NULL                          // every slot on every floor taken
12.  RETURN position
END
```

**Termination.** The outer loop runs exactly `F` times and the inner loop
exactly `LENGTH(slots[floor])` times, both bounded and strictly increasing, so
the algorithm always halts. The search stops at step 7 the first time a free
cell is seen; it reaches step 11 only after every cell has been tested.

**Cost.** Best case `O(1)` - slot 0 of floor 0 is free. Worst case
`O(F × S)` - the facility is full, or only the last cell is free.
Space `O(1)` beyond the slot array itself.

---

## ALGORITHM 2 - COMPUTE_FEE

```
ALGORITHM   COMPUTE_FEE
PURPOSE     Compute the amount payable for one completed stay, charging every
            started hour in full and never exceeding the daily maximum.
INPUT       entryMinute   : minute of entry, measured from midnight
            exitMinute    : minute of exit, measured from midnight
            vehicleType   : TWO_WHEELER | CAR | EV
            user          : the owner, carrying category and pass status
OUTPUT      fee           : whole rupees payable
PRE         exitMinute >= entryMinute
POST        0 <= fee <= DAILY_CAP(vehicleType)

BEGIN
 1.  duration <- exitMinute - entryMinute
 2.  IF duration < 0 THEN
 3.      RAISE InvalidDuration                 // clock went backwards
 4.  END IF
 5.  IF duration = 0 THEN
 6.      fee <- 0                              // drive-through, nothing to charge
 7.      RETURN fee
 8.  END IF
 9.  IF user HAS a valid monthly pass AND user.category <> VISITOR THEN
10.      fee <- 0                              // staff and students park free
11.      RETURN fee
12.  END IF
13.  billableHours <- CEILING(duration / 60)
         // computed as (duration + 59) DIV 60 so that only integer
         // arithmetic is used and no part-hour is given away free
14.  rate <- BASE_RATE(vehicleType)
15.  IF billableHours = 1 THEN
16.      fee <- rate                           // first-hour slab
17.  ELSE
18.      fee <- rate + (billableHours - 1) * rate   // first hour + each extra hour
19.  END IF
20.  cap <- DAILY_CAP(vehicleType)
21.  IF fee > cap THEN
22.      fee <- cap                            // daily maximum slab
23.  END IF
24.  IF vehicleType = EV AND charging was used THEN
25.      fee <- fee + CHARGE_RATE * kWhDrawn   // charged separately from parking
26.  END IF
27.  RETURN fee
END
```

**Termination.** The algorithm contains no loop; it is a straight-line sequence
of guarded assignments, so it halts in a fixed number of steps.

**Cost.** `O(1)` time and `O(1)` space for every input. `BASE_RATE` and
`DAILY_CAP` are table lookups on the vehicle type, not searches.

---

## Supporting function

```
FUNCTION    CEILING_DIV
INPUT       a, b : integers, b > 0
OUTPUT      the smallest integer q such that q * b >= a
BEGIN
 1.  RETURN (a + b - 1) DIV b
END
```

Used at step 13 as `CEILING_DIV(duration, 60)`. Adding `b - 1` before the
integer division pushes any non-zero remainder up into the next whole unit, so
the division rounds **up** while remaining pure integer arithmetic - no
floating-point value is ever created in the money path, and so no rounding
error can enter it.

---

## Trace of ALGORITHM 2 - a car parked for 130 minutes by a visitor

| Step | Expression | Value |
|------|------------|-------|
| 1 | `duration <- 670 - 540` | `130` |
| 2 | `130 < 0` | false |
| 5 | `130 = 0` | false |
| 9 | visitor holds a pass but category = VISITOR | false |
| 13 | `(130 + 59) DIV 60 = 189 DIV 60` | `3` |
| 14 | `BASE_RATE(CAR)` | `30` |
| 15 | `3 = 1` | false |
| 18 | `30 + (3 - 1) × 30` | `90` |
| 20 | `DAILY_CAP(CAR)` | `200` |
| 21 | `90 > 200` | false |
| 24 | type is CAR, not EV | false |
| 27 | **return** | **Rs 90** |

This is the value the Task 8 program prints for `Car TN02B2` at 130 minutes,
which confirms the pseudocode and the implementation agree.
