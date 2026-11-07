# CSA09 - Programming in Java · Assignment 1 (CO1)

## Design, Implement and Validate an Object-Oriented Model for a Campus Smart-Parking System

| | |
|---|---|
| **Name** | Vignesh S |
| **Register Number** | 192325002 |
| **Course** | CSA09 - Programming in Java |
| **Assignment** | Assignment 1 - CO1 (Individual, 50 marks) |
| **Java version** | OpenJDK **21.0.12**, language level 21 |
| **Build tool** | none required - plain `javac` / `java` |
| **Report** | [`REPORT.md`](REPORT.md) - full answers to Tasks 1-12 |

---

## Quick start

```bash
git clone https://github.com/Vignesh-2605/CSA09-Java.git
cd CSA09-Java/Assignment-1

# the two "Complete the Code" programs
cd Task03 && javac ParkingFacility.java && java ParkingFacility && cd ..
cd Task08 && javac FeeDemo.java         && java FeeDemo         && cd ..

# the full system and its 66-case test harness
cd Task11 && javac -d out $(find src -name '*.java') \
          && java -cp out parking.app.SystemTest && cd ..

# the Day-4 design challenge and its 64-case test harness
cd Task12 && javac -d out $(find src -name '*.java') \
          && java -cp out parking.app.ChallengeTest && cd ..
```

Java 16 or later is required (the code uses `instanceof` pattern matching and
switch expressions). Java 21 is what the captured output was produced with.

---

## Folder layout

```
CSA09-Java/Assignment-1/
├── README.md                 this file
├── REPORT.md                 the full assignment report, Tasks 1-12
├── docs/
│   ├── original-design.png   UML class diagram - the Task 1-11 design
│   ├── revised-design.png    UML class diagram - the Task 12 redesign
│   └── *.mmd                 diagram sources
├── Task01/ ... Task12/         one folder per task
│   ├── *.java                source
│   ├── input.txt             input file, where the task reads one
│   ├── *_output.txt          captured console output of the run
│   ├── compile_errors/       programs that are MEANT to fail, + the real javac output
│   └── screenshots/          the same output as terminal screenshots
└── .gitignore
```

Every task folder contains the Java source, the captured output of running it,
and a screenshot of that output. Where a design claim is proved by a **compiler
error** rather than by program output, the offending source and the real `javac`
message are in that task's `compile_errors/` folder.

---

## How to compile and run each task

| Task | Compile | Run |
|---|---|---|
| **01** | `javac ProblemAnalysisDemo.java ParallelArraysComparison.java` | `java ProblemAnalysisDemo` · `java ParallelArraysComparison` |
| **02** | `javac DataRepresentationDemo.java ScannerInputDemo.java SlotStorageComparison.java` | `java DataRepresentationDemo` · `java ScannerInputDemo < input.txt` · `java SlotStorageComparison` |
| **03** | `javac ParkingFacility.java FeeSlabLogic.java FastAllocator.java` | `java ParkingFacility` · `java FeeSlabLogic` · `java FastAllocator` |
| **04** | `javac ConstructorDemo.java ObjectCreationComparison.java` | `java ConstructorDemo` · `java ObjectCreationComparison` |
| **05** | `javac StaticMembersDemo.java CounterScopeComparison.java` | `java StaticMembersDemo` · `java CounterScopeComparison` |
| **06** | `javac InheritanceDemo.java CompositionComparison.java` | `java InheritanceDemo` · `java CompositionComparison` |
| **07** | `javac OverrideDemo.java SwitchFeeComparison.java` | `java OverrideDemo` · `java SwitchFeeComparison` |
| **08** | `javac FeeDemo.java PolymorphismDemo.java InstanceofComparison.java` | `java FeeDemo` · `java PolymorphismDemo` · `java InstanceofComparison` |
| **09** | `javac AbstractionDemo.java InvertedAbstractionComparison.java` | `java AbstractionDemo` · `java InvertedAbstractionComparison` |
| **10** | `javac -d out $(find src -name '*.java')` and `javac -d . PublicFieldsComparison.java` | `java -cp out parking.app.EncapsulationDemo` · `java -cp out parking.app.AutoboxingRiskDemo` · `java -cp . PublicFieldsComparison` |
| **11** | `javac -d out $(find src -name '*.java')` and `javac -d . TicketStoreComparison.java` | `java -cp out parking.app.SystemTest` · `java -cp out parking.app.ParkingApp < input.txt` · `java -cp . TicketStoreComparison` |
| **12** | `javac -d out $(find src -name '*.java')` | `java -cp out parking.app.ChallengeTest` |

On Windows PowerShell, replace `$(find src -name '*.java')` with
`(Get-ChildItem -Recurse src -Filter *.java).FullName`.

**Reproducing the captured compiler errors** (these are *expected* to fail):

```bash
cd Task07/compile_errors && javac -cp .. BrokenBike.java SneakyCar.java
cd Task09/compile_errors && javac -cp .. InstantiateAbstract.java && javac BadInterface.java
cd Task10 && javac -cp out -d /tmp/t10 compile_errors/BreakEncapsulation.java
```

---

## Result of each task

| Task | Marks | What was built | Result |
|---|---|---|---|
| **01** Identify the objects | 4 | 5 candidate classes with responsibilities; every business rule violated on purpose and refused | all 5 rules refused as designed; parallel-array alternative measured - **same O(n)**, so the object design is kept on correctness, not speed |
| **02** Data representation | 3 | types justified per field; jagged array; `Scanner` with validation | integer division under-charges by **Rs 180 over 9 stays**; `(m+59)/60` fixes it. Jagged array wastes **0 of 320 cells** against 180 for a rectangular one |
| **03** Control logic | 4 | **Complete the Code** + SIMATS-form pseudocode + slab logic | output **identical to the expected output**; `if-else` and `switch` agree on all 30 cases; free-slot stack measured **7.4× faster**, trade-off explained |
| **04** Classes & constructors | 4 | 4 constructors incl. a copy constructor; chaining traced | chaining order proved by the trace; duplicate ticket keeps the stay, gets a **new number**; 3 creation strategies all **O(n)** |
| **05** `static` & life cycle | 3 | static counter, static block, GC observed | `WeakReference` resolves **false** after the last reference drops; instance counter **collides 4 of 8** across two gates |
| **06** Inheritance | 4 | 3-level vehicle hierarchy + user hierarchy + UML | constructor order **`Vehicle → Car → ElectricCar`** traced; composition alternative adds a type **at run time** |
| **07** Overriding | 4 | overriding, `final`, `equals`/`hashCode`/`toString` | **2 real compiler errors** prove `@Override` and `final`; `HashSet` refuses duplicate plates, `HashMap` finds a stay across subclasses |
| **08** Polymorphism | 4 | **Complete the Code** + overloading + dispatch trace | output **identical to the expected output**; `instanceof` chain measured **2.2× slower for the last type**, dispatch position-independent |
| **09** Abstraction | 4 | abstract class + 3 interfaces, `default` and `static` methods | **3 compiler errors** justify the abstract-class/interface split; the inverted design lets `BFreeloader` bill **Rs 0** |
| **10** Packages & encapsulation | 3 | `parking.model` / `service` / `app`, validated setters | **5 compiler errors** prove the boundary; `Integer 900 == 900` is **`false`** while `Integer 90 == 90` is `true` |
| **11** Implement, test, defend | 5 | full packaged system, gate console, 66-case harness | **66 / 66 tests pass**; day total **Rs 780** matches the hand calculation; `HashMap` store measured **2,295× faster** at n = 50,000 |
| **12** Design challenge | 8 | `PricingRule` abstraction, `Pass` interface, e-bike, delivery van, 5 ragged floors | **64 / 64 tests pass**; day total **Rs 430** matches by hand; new categories cost **2 new classes and 0 modifications** each |

**Totals: 130 automated assertions, all passing. 10 compiler errors captured as
design evidence. Both Complete-the-Code programs match their expected output
exactly.**

---

## The two "Complete the Code" programs

### Task 3 - `ParkingFacility`

```
Expected          Actual
[0, 0]            [0, 0]
[0, 1]            [0, 1]
[1, 0]            [1, 0]
null              null
true false false  true false false
[0, 1]            [0, 1]
```

`diff actual_output.txt expected_output.txt` → no difference.

### Task 8 - `FeeDemo`

```
Expected                            Actual
TwoWheeler TN01A1 -> Rs 30.0        TwoWheeler TN01A1 -> Rs 30.0
Car TN02B2 -> Rs 90.0               Car TN02B2 -> Rs 90.0
ElectricCar TN03C3 -> Rs 210.0      ElectricCar TN03C3 -> Rs 210.0
true                                true
```

`diff actual_output.txt expected_output.txt` → no difference.

Why the last line prints `true`, and whether that is correct for this system, is
answered in [`Task08/EQUALS_BEHAVIOUR.md`](Task08/EQUALS_BEHAVIOUR.md).

---

## Design at a glance

**Original design (Tasks 1-11)** - `docs/original-design.png`

```
Vehicle (abstract)                    CampusUser
  ├── TwoWheeler                        (category + monthly pass flag)
  ├── Car
  │     └── ElectricCar (final) ──implements── Chargeable
  │
ParkingFacility ──owns──> SlotGrid ──> SlotRow ──> Slot   (jagged array)
                └──holds──> List<ParkingTicket>
```

**Revised design (Task 12)** - `docs/revised-design.png`

```
Vehicle (abstract) ──HAS-A──> PricingRule (interface)
  ├── TwoWheeler                 ├── HourlyRate
  ├── Car                        ├── FlatFee          (NEW - e-bikes)
  ├── ElectricCar                ├── TimeOfDayRate    (NEW - delivery vans)
  ├── EBike        (NEW)         └── CappedRule       (decorator)
  └── DeliveryVan  (NEW)              assembled by Tariffs

CampusUser ──HAS-A──> Pass (interface)
                        ├── MonthlyPass
                        └── OneDayPass  (NEW)
```

The single structural change is that the **tariff moved out of the inheritance
hierarchy and became a composed object**, which is what allows a delivery van's
rate to depend on the time of day - something the original design could not
express at any price.

---

## Notes on the measurements

Timings in the report were produced on the machine that captured the output,
with a JIT warm-up phase before every measured loop. Absolute nanosecond figures
vary between runs and machines; the *shape* of each comparison (flat versus
growing, O(1) versus O(n)) is what the conclusions rest on.

Where a measurement contradicted the expected result, the report says so rather
than quoting the textbook - for example, a dense `int` `switch` compiles to a
jump table and is **O(1) in the number of types, not O(k)**, and the three
object-creation strategies in Task 4 are indistinguishable in time once the JIT
has inlined them.

---

## Commit history

Work was committed progressively, at least one commit per task, in task order.
`git log --oneline` shows the sequence.
