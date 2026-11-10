# CSA09 - Programming in Java - Assignment 2 (CO2)

## Strings and Collections

| | |
|---|---|
| **Name** | Vignesh S |
| **Register Number** | 192325002 |
| **Course** | CSA09 - Programming in Java |
| **Assignment** | Assignment 2 - CO2 (Individual, 50 marks) |
| **Java version** | OpenJDK **21.0.12**, language level 21 |
| **Report** | [`REPORT.md`](REPORT.md) - full answers to Q1-Q7 |

---

## Quick start

```bash
git clone https://github.com/Vignesh-2605/CSA09-Java.git
cd CSA09-Java/Assignment-2

# the shared measurement harness, needed by most questions
javac -d lib/out lib/bench/Bench.java

# the three "Complete the Code" programs
cd Q3 && javac -d . Attendance.java      && java -cp . Attendance      && cd ..
cd Q5 && javac -d . LeaderboardDemo.java && java -cp . LeaderboardDemo && cd ..
cd Q6 && javac -d . Triage.java          && java -cp . Triage          && cd ..

# the Day-4 design challenge
cd Q7 && javac -cp ../lib/out -d . TriageRedesign.java \
      && java -cp .:../lib/out TriageRedesign && cd ..
```

Java 16 or later is required (records, pattern matching, switch expressions).
On Windows PowerShell use `;` instead of `:` in the classpath.

---

## How the measurements were produced

The brief states that *"repeated, averaged measurements and operation counts should be treated as the primary experimental evidence"* and that *"a single execution time or a screenshot of output is not sufficient."*

Every figure in this submission comes from [`lib/bench/Bench.java`](lib/bench/Bench.java), which:

| Step | What it does | Why |
|---|---|---|
| warm-up | runs the workload **5 times**, discarding the results | so the JIT has compiled it before anything is recorded |
| measure | runs it **11 more times** and keeps every one | one run is not evidence |
| report | quotes the **median**, not the mean | a run interrupted by a GC skews a mean, not a median |
| report | prints the **RSD** beside each figure | so noisy measurements are visibly noisy |
| record | writes **every repetition** to `data/Q*-raw.csv` | 2,389 rows, so the tables can be re-derived |

Where a claim can be settled by **counting operations** instead of timing them, it is - Q1 counts characters copied and objects created analytically, and those counts hold on any machine.

**Predictions are printed before the measurement runs.** Three of them turned out wrong; they are reported as wrong with an analysis of why.

---

## Folder layout

```
Assignment-2/
├── README.md             this file
├── REPORT.md             the full report, Q1-Q7
├── lib/bench/Bench.java  the shared measurement harness
├── data/                 raw and summary CSV - 2,389 timing repetitions
├── charts/               graphs built from the CSV (SVG + PNG)
├── Q1/ ... Q7/             one folder per question
│   ├── *.java            source
│   ├── *_output.txt      captured console output
│   └── screenshots/      the same output as terminal screenshots
```

---

## How to compile and run each question

| Q | Compile | Run |
|---|---|---|
| **Q1** | `javac -cp ../lib/out -d . *.java` | `java -cp .:../lib/out StringWorkloadBenchmark` · `ScalingProbe` · `SharedLogContention` |
| **Q2** | `javac -cp ../lib/out -d . *.java` | `java -cp .:../lib/out PlaylistBenchmark` · `OperatingBoundary` · `ManualArrayPlaylist` |
| **Q3** | `javac -d . Attendance.java ContractExperiments.java DedupAlternatives.java` | `java -cp . Attendance` · `java -cp .:../lib/out ContractExperiments` · `DedupAlternatives` |
| **Q3 before** | `javac -d before before/AttendanceBeforeFix.java` | `java -cp before AttendanceBeforeFix` |
| **Q4** | `javac -cp ../lib/out -d . *.java` | `java -cp .:../lib/out PharmacyInventory` · `ScalabilityDecision` |
| **Q5** | `javac -cp ../lib/out -d . *.java` | `java -cp . LeaderboardDemo` · `java -cp .:../lib/out RankingDesign` · `SortingAlternatives` |
| **Q6** | `javac -cp ../lib/out -d . *.java` | `java -cp . Triage` · `java -cp .:../lib/out TriageDesign` · `Starvation` · `QueueAlternatives` |
| **Q7** | `javac -cp ../lib/out -d . TriageRedesign.java` | `java -cp .:../lib/out TriageRedesign` |

`Q3/ContractExperiments`, `Q5/RankingDesign`, `Q6/TriageDesign` and `Q7/TriageRedesign` exit with a **non-zero status** if any assertion fails, so they can be used as a test suite.

---

## Result of each question

| Q | Marks | Assessed area | Result |
|---|---|---|---|
| **Q1** | 6 | String, StringBuilder, StringBuffer | `String +` measured **460x** slower on a 4,000-field ticket; the doubling ratio converges on **4**, confirming O(n²). A shared `StringBuilder` **corrupted the log in 4 of 5 trials** and threw `ArrayIndexOutOfBoundsException`; `StringBuffer` was correct 5 of 5 |
| **Q2** | 4 | ArrayList, LinkedList, ArrayDeque | `LinkedList` middle-insert is **40x slower** than `ArrayList` despite both being O(n). **My prediction of a small-n crossover was refuted** - ArrayList loses front insertion at every size down to n = 8. `ArrayDeque` is **55x faster** than `ArrayList` for the Up Next queue |
| **Q3** | 6 | HashSet and the hashCode-equals contract | **18 of 18 predictions confirmed.** Mutating a hashed field makes an element **unfindable *and* unremovable** while still being iterable. A `TreeSet` inconsistent with `equals` silently reports a different size from a `HashSet` on identical data |
| **Q4** | 6 | HashMap, TreeMap, LinkedHashMap | `TreeMap` lookups **38x slower** (984 ns vs 26 ns), but its code-order report is free. Arrival order is **unrecoverable** from a `HashMap` - a correctness loss, not a performance one. Deployed `LinkedHashMap` |
| **Q5** | 7 | Generics, Comparable, Comparator | **14 of 14 checks pass.** **Prediction refuted:** `TimSort` did **not** reject an inconsistent comparator at any size up to 4,096 - it silently ranked a 12-point team seventh. Overflow via subtraction demonstrated on `Integer.MAX_VALUE` |
| **Q6** | 8 | Queue and PriorityQueue | **8 of 8 hand-verified checks pass.** Simulation left **2,965 minor patients unseen**, worst wait 11,463 ticks. **My first fair rule degenerated to FIFO** and pushed critical patients from a 0-tick to a 1,269-tick mean wait; corrected with a promotion floor |
| **Q7** | 13 | Day-4 challenge: diagnose, redesign, validate | Waiting-list display **84x faster**, and the speed-up **grows** with n (37x -> 84x). Admit+serve **8.8x** faster. **6 of 6 validation checks**, including identical ordering over **200 randomised streams**. Objective O3 **honestly reported as not met**, with a capacity analysis showing why no queue design could meet it |

**Totals: 46 automated checks, all passing. 2,389 individual timing repetitions in `data/`. Three Complete-the-Code programs matching expected output exactly. Three predictions refuted by measurement and analysed rather than revised.**

---

## The three "Complete the Code" programs

### Q3 - `Attendance`

```
Before completing TODO 1:  Unique students: 3
After  completing TODO 1:  Unique students: 2      <- matches expected
```

The difference: with `hashCode()` inherited from `Object`, the two `Arun` scans are *equal* but hash to different buckets, so `equals()` is never consulted.

### Q5 - `LeaderboardDemo`

```
Expected:  [Eagles(9,6), Bisons(9,4), Panthers(9,4), Cobras(6,1)]
Actual:    [Eagles(9,6), Bisons(9,4), Panthers(9,4), Cobras(6,1)]
```

Bisons ranks above Panthers because both have 9 points **and** a goal difference of 4, so the third key decides: `"Bisons".compareTo("Panthers")` is negative.

### Q6 - `Triage`

```
Expected:  Meena(S1) Kumar(S1) Lata(S2) Ravi(S3) John(S3) Asha(S5)
Actual:    Meena(S1) Kumar(S1) Lata(S2) Ravi(S3) John(S3) Asha(S5)
```

`diff` reports no difference for any of the three.

---

## Charts

| Chart | Shows |
|---|---|
| `charts/q1-workloads.png` | five string strategies across three designed workloads |
| `charts/q1-scaling.png` | the doubling test for the O(n²) claim |
| `charts/q2-operations.png` | five list operations at n = 100,000 |
| `charts/q4-lookups.png` | map lookup cost at three inventory sizes |
| `charts/q6-queues.png` | three triage queue implementations |
| `charts/q7-display.png` | the Q7 before/after on the waiting-list display |
| `charts/q7-admit-serve.png` | the Q7 before/after on queue operations |

All are generated from the CSV files in `data/`, so every bar can be traced to the repetitions behind it.

---

## A note on the measurements

Absolute nanosecond figures vary between runs and between machines. The *shape* of each comparison - flat versus growing, O(1) versus O(n) - is what the conclusions rest on, and that is stable.

Where a measurement contradicted the expected result, the report says so. Three examples: a dense `int` switch is O(1) in the number of types rather than O(k); `ArrayList` has no small-n advantage for front insertion; and `TimSort` does not reliably detect an inconsistent comparator.
