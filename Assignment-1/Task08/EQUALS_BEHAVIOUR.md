# Task 8 - Why the last line of `FeeDemo` prints `true`

**Name:** Vignesh S  **Register Number:** 192325002
**Course:** Programming in Java - Assignment 1 (CO1)

The brief asks: *explain why the last line prints `true`, and decide whether
this is the correct behaviour for your system.*

## The line

```java
System.out.println(new Car("TN02B2").equals(new ElectricCar("TN02B2")));
```

Output: `true`

## Why it prints `true`

`equals()` is overridden once, in `Vehicle`, and the subclasses do not override
it again:

```java
@Override public boolean equals(Object o) {
    return o instanceof Vehicle && regNo.equals(((Vehicle) o).regNo);
}
```

Three facts combine:

1. **The test is `instanceof Vehicle`, not `getClass() == o.getClass()`.**
   `ElectricCar` extends `Car` extends `Vehicle`, so an `ElectricCar` *is* a
   `Vehicle` and passes the test.
2. **The only field compared is `regNo`.** Both objects were constructed with
   the same string `"TN02B2"`, and `String.equals` compares characters, so the
   comparison succeeds.
3. **No subclass adds a field to the comparison.** `ElectricCar` carries extra
   state in the full design, but it inherits `equals()` unchanged, so that state
   is invisible to equality.

So two distinct objects, of two different classes, on two different heap
addresses, compare equal - because the design has declared that *a vehicle's
identity is its number plate*.

`hashCode()` is overridden to `regNo.hashCode()` in the same class, so the two
objects also produce the same hash code. That consistency is what makes the
behaviour usable rather than merely surprising: the pair can be stored in and
retrieved from a `HashSet` or `HashMap` interchangeably.

## Is this correct for a campus parking system?

**Yes - and it is the behaviour the system actively needs.** The reasoning is
physical, not technical:

- A number plate identifies **one real vehicle**. `TN02B2` is a single car in
  the car park; it cannot be in two slots at once.
- The system must therefore **refuse a duplicate entry** for a plate already
  inside. That check is `activeVehicles.contains(scannedVehicle)`, and it only
  works if two objects bearing the same plate are equal.
- The exit barrier scans a plate and constructs a **fresh** object to look the
  stay up. It does not hold the object created at entry. Without this
  definition of equality, `tickets.get(scanned)` would return `null` and no
  vehicle could ever leave.
- Crossing the class boundary is correct too. If a plate were recorded as a
  `Car` at one gate and an `ElectricCar` at another - a plausible data-entry
  difference, since the categories overlap - they are still the same car.
  A `getClass()` comparison would call them different and let one vehicle
  occupy two slots.

Task 7's `OverrideDemo` demonstrates all of this running:
`HashSet.add` rejects the second and third objects carrying plate `TN02CD2222`,
and `HashMap.get` finds the entry time from a different object and even from a
different subclass.

## The cost of the choice, and how it is contained

Using `instanceof` rather than `getClass()` breaks the **symmetry** clause of
the `equals()` contract if any subclass ever adds its own equality fields:
`car.equals(ev)` would be `true` while `ev.equals(car)` was `false`, and
`HashSet` behaviour would then depend on insertion order.

This is contained deliberately, not ignored:

- No subclass overrides `equals()` - equality is defined in exactly one place.
- `ElectricCar` is declared **`final`** in the Task 7 design, so the branch is
  closed and nothing can extend it to add a field and reintroduce the asymmetry.

## Where this would be the wrong choice

If the system ever needed to distinguish a vehicle *record* from a vehicle
*identity* - for example keeping a history of every registration of the same
plate across years, where two entries for `TN02B2` are genuinely different rows
- then equality on the plate alone would be wrong. The fix would not be to
change `equals()`, but to separate the two concepts: a `Vehicle` identified by
its plate, and a `Registration` identified by a surrogate key. For the
requirement as stated, where only vehicles currently inside the facility are
modelled, plate equality is both correct and necessary.
