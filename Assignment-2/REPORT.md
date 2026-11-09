# Strings and Collections - CO2

**Programming in Java - Assignment 2**

| | |
|---|---|
| **Name** | Vignesh S |
| **Register Number** | 192325002 |
| **Course Code** | CSA09 - Programming in Java |
| **Assignment** | Assignment 2, CO2 - Individual |
| **Total Marks** | 50 |
| **Java Version** | OpenJDK 21.0.12 (language level 21) |
| **Repository** | `CSA09-Java`, folder `Assignment-2` |

---

## How this report is evidenced

The brief states that *"repeated, averaged measurements and operation counts should be treated as the primary experimental evidence"* and that *"a single execution time or a screenshot of output is not sufficient."* Every figure in this report therefore comes from `lib/bench/Bench.java`, a shared harness that:

* runs **5 warm-up repetitions** that are discarded, so the JIT has compiled the code before anything is recorded;
* then runs **11 measured repetitions** and keeps every one;
* reports the **median** rather than the mean, because a single run interrupted by a garbage collection skews a mean and not a median;
* reports the **relative standard deviation (RSD)** beside each figure, so a noisy measurement is visible as noise;
* writes **every individual repetition** to `data/Q*-raw.csv` - 2,389 rows across the assignment - so the tables below can be re-derived independently.

Where a claim can be settled by **counting operations** rather than timing them, it is: Q1 counts characters copied and objects created analytically, and those counts are machine-independent.

**Predictions are recorded before measurement.** Each program prints its prediction before running the experiment. Three predictions in this assignment turned out to be wrong. They are reported as wrong, with an analysis of why, rather than quietly revised.

---

# Q1 - Building Text Under Load  *(6 marks, K4)*

**Evidence:** `Q1/`, `data/Q1-*.csv`, `charts/q1-workloads.png`, `charts/q1-scaling.png`

## 1.1 Prediction, and the reasoning behind it

A `String` is immutable, so `s = s + f` cannot extend the existing object. It must allocate a new character array, copy everything already written, then append the new field. Building a ticket of n fields therefore copies `1 + 2 + ... + n` field-widths - the arithmetic series `n(n+1)/2` - which is **O(n²) characters copied to produce O(n) characters of output**, and allocates two throwaway objects per field.

`StringBuilder` keeps one growable array and writes each character once: **O(n)**, with a constant number of objects. `StringBuffer` does identical work with every method `synchronized`.

| Workload | Predicted outcome | Reasoning |
|---|---|---|
| W1 - 12 fields x 1 ticket | all within noise | too few fields for n² to matter |
| W2 - 50 fields x 10,000 tickets | `+` clearly slowest, but finite | n² small per ticket, repeated 10,000 times |
| W3 - 4,000 fields x 1 ticket | `+` catastrophically slowest | n² dominates inside one ticket |
| all | `StringBuffer` slightly behind `StringBuilder` | same algorithm, plus uncontended lock |
| all | pre-sized `StringBuilder` fastest | no internal array doubling |

**Quantified:** for W3 the `+` loop copies roughly 4,000/2 = 2,000x as many characters, so the gap should be **hundreds to thousands of times**, not a few percent.

## 1.2 Work counted analytically (independent of any clock)

| Workload | Fields | Tickets | Chars copied, `+` | Chars copied, SB | Ratio | Objects `+` vs SB |
|---|---|---|---|---|---|---|
| W1 | 12 | 1 | 878 | 138 | **6.4x** | 24 vs 2 |
| W2 | 50 | 10,000 | 161,130,000 | 6,480,000 | **24.9x** | 1,000,000 vs 20,000 |
| W3 | 4,000 | 1 | 129,942,296 | 66,875 | **1,943x** | 8,000 vs 2 |

These counts are derived from the algorithm, so they hold on any machine. They are what the Big-O claim actually rests on; the timings below merely confirm it.

## 1.3 Measured results

**W2 - the bulk booking run (50 fields x 10,000 tickets):**

| Design | Median (ms) | Per ticket (ns) | RSD % |
|---|---|---|---|
| `String +` | 33.720 | 3,372 | 2.7 |
| `StringBuilder` | 14.027 | 1,403 | 28.8 |
| `StringBuilder(sized)` | **11.469** | **1,147** | 9.4 |
| `StringBuffer` | 13.101 | 1,310 | 4.0 |
| `String.join` | 8.721 | 872 | 10.6 |
| `String.format` | 43.820 | 4,382 | 14.6 |

**W3 - one pathological ticket (4,000 fields):**

| Design | Median (ms) | Relative to StringBuilder |
|---|---|---|
| `String +` | **21.951** | **460.3x** |
| `StringBuilder` | 0.048 | 1.0x |
| `StringBuilder(sized)` | 0.033 | 0.69x |
| `StringBuffer` | 0.050 | 1.06x |
| `String.format` | 0.770 | 16.1x |

## 1.4 Prediction compared with observation

| Prediction | Outcome |
|---|---|
| W3 gap in the hundreds-to-thousands | **Confirmed** - 460x measured against 1,943x characters copied |
| W1 all within noise | **Confirmed** - but RSD reached 90%, so W1 is at the limit of timer resolution and carries little weight |
| `StringBuffer` slightly behind `StringBuilder` | **Not confirmed** - measured at 0.87x and 0.93x, i.e. marginally *faster*. The difference is inside the noise band, and modern JVMs elide an uncontended lock, so the honest reading is "no measurable difference single-threaded" |
| pre-sized `StringBuilder` fastest | **Confirmed** in all three workloads |

## 1.5 Does it really grow as n²? The scaling probe

A single ratio does not demonstrate a complexity class. Doubling n should multiply a quadratic time by ~4 and a linear time by ~2.

| n (fields) | `String +` (ms) | x prev | `StringBuilder` (ms) | x prev | ratio |
|---|---|---|---|---|---|
| 250 | 0.285 | - | 0.0511 | - | 6x |
| 500 | 0.530 | 1.86 | 0.0630 | 1.23 | 8x |
| 1,000 | 1.169 | 2.21 | 0.0664 | 1.05 | 18x |
| 2,000 | 5.717 | **4.89** | 0.1853 | 2.79 | 31x |
| 4,000 | 21.910 | **3.83** | 0.0706 | 0.38 | 310x |
| 8,000 | 81.264 | **3.71** | 0.1749 | 2.48 | **465x** |

The `x prev` column for `String +` converges on **4** once n is large enough for the quadratic term to dominate - exactly the quadratic signature. The early rows are unreliable because the absolute times are microseconds.

**The decisive column is the last one.** The ratio between the two designs is not constant: it climbs from 6x to 465x. A constant-factor difference would keep it flat. A difference in *complexity class* makes it grow, and that is what separates "slower" from "asymptotically worse".

## 1.6 The multithreaded design decision

8 threads x 20,000 appends to one shared log; a correct log must be exactly 1,760,000 characters.

| Shared log | Trial 1 | 2 | 3 | 4 | 5 | Wrong in |
|---|---|---|---|---|---|---|
| `StringBuilder` | 1,479,280 | **1,760,000** | 1,415,040 | 1,642,619 | 1,540,000 | **4 of 5** |
| `StringBuffer` | 1,760,000 | 1,760,000 | 1,760,000 | 1,760,000 | 1,760,000 | 0 of 5 |

The run also threw a real exception from inside the builder:

```
java.lang.ArrayIndexOutOfBoundsException: arraycopy: last destination index 1179651
  out of bounds for byte[1179646]
    at java.lang.AbstractStringBuilder.append(AbstractStringBuilder.java:593)
```

Trial 2 passed **by luck**. That is the most dangerous property of this bug, not a mitigating one: a race that fails intermittently passes testing and corrupts the booking log in production.

### Recommendation

| Use | Class | Why |
|---|---|---|
| **per-ticket text** | `StringBuilder`, pre-sized | the builder never escapes one thread's call stack, so there is nothing for synchronization to protect; `new StringBuilder(expectedChars)` measured fastest in all three workloads |
| **shared booking log** | `StringBuffer` *within the three classes named* | `StringBuilder` is simply incorrect, as the table shows |

The honest engineering answer goes further: a shared ever-growing `StringBuffer` is the wrong structure for a booking log at all. Every thread serialises on one monitor, so the log becomes the server's bottleneck, and it grows without bound. A `ConcurrentLinkedQueue` of finished entries drained by one writer keeps appends lock-free - measured correct at 1,760,000 characters - and bounds the memory. That is outside the three classes the question names, which is why `StringBuffer` remains the stated answer, but a recommendation that ignored the real bottleneck would not be useful.

## 1.7 Alternative Concept and Complexity Analysis

| Approach | Time to build n fields | Intermediate objects | Measured W2 (ms) |
|---|---|---|---|
| repeated `+` | **O(n²) chars copied** | 2n | 33.720 |
| `StringBuilder` | **O(n)** | 2 | 14.027 |
| `String.join` | O(n) | ~2 | **8.721** |
| `StringJoiner` | O(n) | ~3 | 12.436 |
| `String.format` | O(n) + parsing | many | 43.820 |

`String.join` was the fastest of all in W2 - it is given the array up front, so it can size the result exactly in one pass, which is the same advantage the pre-sized builder has. It applies only when every piece is already in a collection and the separator is uniform. `String.format` was the slowest in two of three workloads because it parses the format pattern at run time; it buys locale-aware formatting that none of the others provide.

---

# Q2 - When Is ArrayList an Appropriate Engineering Choice?  *(4 marks, K4)*

**Evidence:** `Q2/`, `data/Q2-*.csv`, `charts/q2-operations.png`

## 2.1 Prediction

| Operation | ArrayList | LinkedList | ArrayDeque | Why |
|---|---|---|---|---|
| add at end | O(1) amortised | O(1) | O(1) amortised | array doubling is rare |
| insert at front | O(n) | O(1) | O(1) | the array must shift |
| insert in middle | O(n) shift | O(n) walk | n/a | **both O(n), but a shift should be far cheaper than a walk** |
| get k-th | O(1) | O(n) | no index API | one index vs k dereferences |
| iterate | O(n), cache-friendly | O(n), pointer chase | O(n) | contiguous vs scattered |

The claim actually worth testing was that **ArrayList would win on small playlists** despite being asymptotically worse, because `System.arraycopy` moves a contiguous block at memory-bandwidth speed while a node allocation costs a heap allocation and a cache miss.

## 2.2 Measured at n = 100,000 (ns per operation)

| Operation | ArrayList | LinkedList | ArrayDeque |
|---|---|---|---|
| add at end | **6.73** | 10.03 | 6.87 |
| insert at front | 17,967.88 | **40.99** | **37.99** |
| insert in middle | **8,917.94** | 361,632.59 | n/a |
| get k-th | **2.47** | 102,558.11 | n/a |
| iterate all | **3.93** | 4.64 | 4.13 |

## 2.3 The prediction was wrong, and the crossover does not exist

Scanning much smaller sizes for front insertion (ns per operation):

| n | ArrayList | LinkedList | ArrayDeque | Winner |
|---|---|---|---|---|
| 8 | 200.26 | 130.43 | 109.45 | LinkedList/Deque |
| 64 | 101.12 | 16.43 | 15.50 | LinkedList/Deque |
| 512 | 133.21 | 30.67 | 17.70 | LinkedList/Deque |
| 4,096 | 394.61 | 10.38 | 4.43 | LinkedList/Deque |

**There is no crossover.** ArrayList loses front insertion at every size down to n = 8, where the array holds eight references and the shift is a single cache line.

**Why the prediction failed.** It assumed the comparison was "copy n references" against "allocate one node". It is not. `ArrayList.add(0, x)` does the `arraycopy` *and* range-checks, bumps `modCount` and may grow the backing array. `LinkedList.addFirst` writes three references and returns. The fixed overhead of the array path already exceeds a node allocation before the shift is counted.

**What the prediction got right** is visible in the middle-insert row: there, where LinkedList must *also* walk to the position, the contiguous shift wins - and wins by **40x**. The mechanism was real; it was applied to the wrong operation.

> **The row most people get wrong.** Both structures are O(n) for middle insertion, so the textbook says they are equivalent. LinkedList measured **40x slower**. ArrayList's n is a single `System.arraycopy` of a contiguous block at memory-bandwidth speed; LinkedList's n is n/2 *dependent* pointer dereferences, each a potential cache miss the CPU cannot prefetch. **Equal complexity classes do not imply equal cost.**

## 2.4 When is ArrayList reasonable, and when would I stop?

| Usage pattern | Verdict | Evidence |
|---|---|---|
| append at end, read by index, iterate | **use it** | 6.7 ns add, 2.5 ns get, 3.9 ns/element iterate |
| playlist under ~500 songs with **rare** front edits | use it | the front cost is paid seldom; get/iterate dominate |
| frequent inserts/removes at the **front** | **stop** | 17,968 ns/op vs 41 ns for LinkedList |
| frequent inserts in the **middle** | **still use it** | 17.8 ms vs LinkedList's 723 ms |
| random access by index | **use it** | 2.5 ns vs 102,558 ns |
| queue behaviour at both ends | **stop** | use ArrayDeque |

The earlier claim that "ArrayList is fine for small playlists even with front edits" is **not supported by this evidence and is withdrawn**. The defensible statement is narrower: ArrayList is fine when front insertion is *rare*, whatever the size.

## 2.5 Changed condition - the "Up Next" queue

Songs added at both ends, removed from the front; 50,000 mixed operations:

| Design | Median (ms) | Per op (ns) |
|---|---|---|
| ArrayList | 50.172 | 1,003.44 |
| LinkedList | 1.551 | 31.02 |
| **ArrayDeque** | **0.911** | **18.21** |

ArrayDeque is **55x faster than ArrayList** and 1.70x faster than LinkedList.

**Recommendation: ArrayDeque for Up Next.** It is a circular array, so adding at either end is O(1) amortised with *no shifting* - the head and tail indices move instead of the data. It keeps ArrayList's contiguous cache behaviour while gaining LinkedList's cheap ends, and allocates one array rather than one node per element.

**What it costs:** ArrayDeque has no index-based `get(k)`, so it cannot replace the main playlist, where jumping to the k-th song is a core operation. The right design is therefore **both**: ArrayList for the playlist, ArrayDeque for Up Next. Choosing one structure for the whole app is what forces a bad trade; choosing per access-pattern does not.

## 2.6 Alternative Concept - a plain array with manual shifting

| Operation | manual array | ArrayList | LinkedList | ArrayDeque |
|---|---|---|---|---|
| add at end | O(1) amortised | O(1) amortised | O(1) | O(1) amortised |
| insert at front | O(n) | O(n) | O(1) | O(1) amortised |
| insert in middle | O(n) | O(n) | O(n) | not supported |
| get k-th | O(1) | O(1) | O(n) | no index API |
| **space per element** | **4 B slot** | 4 B slot | **24 B node** | 4 B slot |

Measured, the manual array and ArrayList are within noise of each other - because **they are the same algorithm**. ArrayList's methods are thin wrappers over exactly these `arraycopy` calls, and the JIT inlines them.

So the plain array buys no speed. What it costs is bounds checks with clear messages, the growth policy, nulling the vacated slot on remove (without which the array leaks a removed Song), the `List` interface, and `modCount` - which turns concurrent modification into an immediate exception instead of silent corruption. A plain array is the right choice only for **primitives**, where `int[]` avoids boxing that `ArrayList<Integer>` cannot - a genuine 4x space difference, and a different argument from this one.

---

# Q3 - Engineer the hashCode-equals Contract  *(6 marks, K4)*

**Evidence:** `Q3/`, `data/Q3-*.csv`

## 3.1 Three designed experiments, with predictions recorded first

**All 18 predictions were confirmed.**

### Experiment 1 - scans for which the broken class *appears* to work

**Design:** every student scanned exactly once, at one gate.
**Prediction:** size 4. With no duplicate scans there is nothing for `equals()` to catch, so a missing `hashCode()` cannot cause a visible fault.

| ID | Test | Predicted | Observed |
|---|---|---|---|
| E1 | 4 distinct students, 1 scan each | 4 | **4** |

This is the dangerous case: a test suite built only from data like this passes, and the class ships.

### Experiment 2 - scans designed to expose the duplicates

**Design:** three students scanned again at a second gate, as *separate objects* - which is what happens when a card is read twice and a new `Student` is constructed from each scan.
**Prediction:** 7, not 4.

| ID | Test | Predicted | Observed |
|---|---|---|---|
| E2 | 7 scans of 4 students, **broken** class | 7 | **7** |
| E2b | the same 7 scans, **corrected** class | 4 | **4** |

The mechanism, shown directly:

```
b1.equals(b2)                  = true
b1.hashCode() == b2.hashCode() = false   (705927765 vs 1829164700)
```

### How HashSet locates an element

1. compute `hashCode()` and **spread** it (`HashMap.hash` mixes high bits down, so keys differing only above the table size still scatter);
2. `index = (table.length - 1) & spreadHash` - pick **one** bucket;
3. walk only that bucket's chain, calling `equals()` on each entry;
4. if nothing matches, append to that chain.

**Step 2 is the whole story.** `equals()` is only ever consulted inside the bucket chosen in step 2, so two objects that hash differently are never compared at all - however equal they are.

### Experiment 3a - mutating a hashed field after insertion

| ID | Test | Predicted | Observed |
|---|---|---|---|
| E3a1 | `contains(m)` **before** mutation | true | **true** |
| E3a2 | `contains(m)` **after** mutating `regNo` | false | **false** |
| E3a3 | set size is still | 1 | **1** |
| E3a4 | iteration still yields the object | true | **true** |
| E3a5 | `remove(m)` succeeds | false | **false** |

The set holds an object it can **neither find nor delete**. This is why the corrected `Student` declares `regNo` **final**.

### Experiment 3b - a TreeSet inconsistent with equals

| ID | Test | Predicted | Observed |
|---|---|---|---|
| E3b1 | TreeSet ordered by **name** (inconsistent) | 3 | **3** |
| E3b2 | TreeSet ordered by **regNo** (consistent) | 4 | **4** |
| E3b3 | HashSet on the same roll | 4 | **4** |

A `TreeSet` never calls `equals()` at all. Its notion of duplicate is `compare(a,b) == 0`. "Consistent with equals" is therefore not a style preference - it decides whether two structures holding the same data report the same size.

## 3.2 Complete the Code - `Attendance`

```java
// TODO 1: Override hashCode() so that it is consistent with equals()
@Override public int hashCode() { return regNo.hashCode(); }
```

`equals()` decides identity on `regNo` alone, so `hashCode()` must be derived from `regNo` alone.

| | Output |
|---|---|
| **Before completing TODO 1** | `Unique students: 3` |
| **After completing TODO 1** | `Unique students: 2` |
| **Expected** | `Unique students: 2` - `diff` reports no difference |

**The difference explained:** with `hashCode()` inherited from `Object`, the two `Arun` scans are *equal* but hash to different buckets, so `equals()` is never consulted and the duplicate is stored as a second element.

## 3.3 The correction, and how it is verified

```java
final class Student {
    private final String regNo;      // final: hashed state cannot drift
    private final String name;
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        return regNo.equals(((Student) o).regNo);
    }
    @Override public int hashCode() { return regNo.hashCode(); }
}
```

`hashCode()` uses `regNo` and only `regNo`, because `equals()` does. Adding `name` would break the contract in the other direction.

Verification is **by property, not by example** - a single `size == 2` assertion passes for a class still broken in the symmetry or consistency clause:

| ID | Property | Result |
|---|---|---|
| V1 | reflexive | PASS |
| V2 | symmetric | PASS |
| V3 | equal objects have equal hash codes | PASS |
| V4 | consistent across repeated calls | PASS |
| V6 | `null` is never equal | PASS |
| V7 | a different type is never equal | PASS |

## 3.4 Alternative Concept and Complexity Analysis

Measured at n = 200,000 unique students, 600,000 scans:

| Structure | Median (ms) | ns per scan | Ordered output | Needs `Student.hashCode`? |
|---|---|---|---|---|
| `HashSet<Student>` | 27.169 | 45.28 | no | **YES - this is the trap** |
| `TreeSet<Student>` + Comparator | 179.942 | 299.90 | **yes, by regNo** | no - never calls equals |
| `HashMap<String,Student>` | **22.102** | **36.84** | no | no - String's is used |
| `LinkedHashMap<String,..>` | 23.163 | 38.60 | yes, by arrival | no |

| Structure | Per scan | Space per entry |
|---|---|---|
| HashSet | O(1) average | ~36 B |
| TreeSet | **O(log n)** | ~40 B |
| HashMap | O(1) average | ~36 B + key ref |

### Recommendation

* **if only the count matters:** `HashSet` with the corrected `Student` - fastest, and the most direct expression of "a set of students";
* **if the register must print in roll-number order:** `TreeSet` with a comparator on `regNo`, paying O(log n) per scan to avoid an O(n log n) sort at the end;
* **if the entity class is outside my control, or may be edited by someone else later:** `HashMap<String,Student>` keyed by `regNo`. It de-duplicates on a **String**, whose `equals`/`hashCode` are correct by definition, so the de-duplication cannot be broken by an edit to `Student`. Experiment 1 showed how silently that edit can happen.

---

# Q4 - Choosing a Map for a Pharmacy Inventory  *(6 marks, K4)*

**Evidence:** `Q4/`, `data/Q4-*.csv`, `charts/q4-lookups.png`

## 4.1 Expected cost, determined before implementation

| Map | put | get | Iteration order | Which report it gives free |
|---|---|---|---|---|
| HashMap | O(1) avg | O(1) avg | unspecified | **neither** |
| TreeMap | O(log n) | O(log n) | sorted by key | the **code-order** report |
| LinkedHashMap | O(1) avg | O(1) avg | insertion order | the **arrival-order** report |

### Extra work if the wrong map is chosen

* **HashMap -> code-order report:** sort all keys, O(n log n) **every evening**, plus an O(n) copy.
* **HashMap -> arrival-order report:** *impossible to recover at all* - the information was never stored. This is the case worth noticing: the other mismatches cost time, this one costs **correctness**.
* **TreeMap -> arrival order:** likewise unrecoverable.
* **LinkedHashMap -> code order:** the same O(n log n) sort.

## 4.2 Measured at n = 200,000

| Map | build (ms) | 100,000 lookups (ms) | ns/lookup | code-order report (ms) | arrival-order report (ms) |
|---|---|---|---|---|---|
| HashMap | **27.609** | **2.616** | **26.16** | 60.397 (sort needed) | impossible |
| TreeMap | 169.198 | 98.416 | 984.16 | **32.664** (free) | impossible |
| LinkedHashMap | 30.963 | 2.583 | 25.83 | 138.455 (sort needed) | **1.417** (free) |

TreeMap lookups are **38x slower** - it compares keys down ~17 levels of a red-black tree, each a String comparison, against one hash and one bucket probe.

Order verification, with codes deliberately generated in shuffled order so that arrival and code order genuinely differ:

| Map | Code-order report | Arrival-order report | First 3 keys as iterated |
|---|---|---|---|
| HashMap | WRONG without a sort | **IMPOSSIBLE** | MED-0176070 MED-0176071 MED-0176072 |
| TreeMap | **correct, free** | **IMPOSSIBLE** | MED-0000000 MED-0000001 MED-0000002 |
| LinkedHashMap | WRONG without a sort | **correct, free** | MED-0116154 MED-0052993 MED-0109745 |

## 4.3 Acceptability criterion, fixed before measuring

A pharmacist waits at a counter while the lookup runs. I set the budget at **1,000 ns (1 microsecond) per lookup** - deliberately generous, allowing 100,000 lookups per second on one thread. Setting it loose matters: a criterion tight enough that only one option passes is not a criterion, it is a conclusion written in advance.

| Map | ns per lookup | Verdict |
|---|---|---|
| HashMap | 16.3 | PASSES, 61x inside budget |
| TreeMap | 726.0 | PASSES, 1x inside budget |
| LinkedHashMap | 59.7 | PASSES, 17x inside budget |
| Hashtable | 48.4 | PASSES, 21x inside budget |

**All four pass.** The budget does not decide the question, which is the correct outcome - it means the decision must be made on the reports instead.

### Where the remaining cost comes from

Even the winner is not free. A HashMap lookup on a String key does: `String.hashCode()` (O(length), cached after first call); the hash spread; **one array index into the bucket table - a likely cache miss at n = 200,000, since the table is ~1 MB and does not fit in L2**; then `String.equals()`, another O(length) scan. The dominant term at this size is the **cache miss**, not the hashing - which is why HashMap's per-lookup cost rose with n even though its complexity is O(1).

## 4.4 Deployment decision

**I deploy a single `LinkedHashMap`.**

| Option | Lookups | Code-order report | Arrival-order report |
|---|---|---|---|
| TreeMap alone | 726 ns | free | **impossible - rejected** |
| **LinkedHashMap alone** | **59.7 ns** | 138 ms nightly sort | **free** |
| LinkedHashMap + TreeMap index | 59.7 ns | free | free, but two structures to keep in step |

The two costs are not comparable in importance. Lookups happen hundreds of times a day **while a pharmacist waits**; the code-order report happens **once, overnight, when nobody is waiting**. Paying ~138 ms once a day to keep every lookup at 59.7 ns is the right trade. The dual-index option buys back 138 ms per day at the cost of a second structure that can silently fall out of sync - a correctness risk taken on to save a tenth of a second at midnight.

I would revisit this if the code-order report became interactive, or if n grew by two orders of magnitude.

## 4.5 Why Hashtable is not preferred in new code

Measured at 48.4 ns against HashMap's 16.3 ns - but speed is the least of it:

* every method is `synchronized`, so a single-threaded pharmacy pays lock overhead for safety it never uses;
* that synchronization is **not enough** for real concurrent use anyway - get-then-put is still a race, so callers need external locking, making the per-method locks pure waste;
* it is a legacy class retrofitted to `Map`, carrying parallel legacy methods returning `Enumeration`;
* it rejects null keys and values with `NullPointerException`, inconsistent with HashMap - demonstrated in the output.

For genuine concurrency the modern answer is `ConcurrentHashMap`, which locks per bucket and offers atomic compound operations such as `computeIfAbsent`.

## 4.6 Alternative Concept - a sorted array with `Arrays.binarySearch()`

| Operation | sorted array | HashMap | TreeMap |
|---|---|---|---|
| lookup | O(log n) | **O(1) avg** | O(log n) |
| insert a medicine | **O(n) shift** | O(1) avg | O(log n) |
| code-order report | **O(n), free** | O(n log n) sort | **O(n), free** |
| space per entry | **4 B slot** | ~36 B node | ~40 B entry |

The sorted array is by far the most **space**-efficient - about 781 KB of references against ~7 MB of hash nodes at n = 200,000 - and its code-order report is free.

It is nonetheless wrong here, for one specific reason: **stock arrives and medicines are discontinued**, so the inventory changes, and every insertion shifts on average n/2 references. A sorted array is right for data built once and then only read - a published price list, a reference table - not for a live inventory.

---

# Q5 - Derive a Generic Ranking Component from a Principle  *(7 marks, K4)*

**Evidence:** `Q5/`, `data/Q5-*.csv`

## 5.1 What the principle requires, and what it forces

The principle names **transitivity** and a **well-defined equality**. Together with the antisymmetry they imply, that is a **total order**. Three design consequences follow:

1. the comparison must be a **function of the items' fields only** - anything else (insertion order, a random tiebreak, a mutable field) breaks transitivity the moment it changes;
2. the key sequence must **end in a key that is unique**, or two distinct items compare equal and the ranking is ambiguous - which is why `name` is the final key, not an optional extra;
3. the arithmetic must **not overflow**, because an overflowed subtraction flips a sign and destroys transitivity outright.

## 5.2 Comparable or Comparator? The derivation

| Question | `Comparable<Team>` | `Comparator<Team>` |
|---|---|---|
| how many orders per type? | exactly one | as many as needed |
| where does the rule live? | inside the entity | beside the use case |
| can the rule change per event? | no, compiled in | yes, pass a different one |
| works for a type I don't own? | no | yes |
| reusable for Player, College? | no | yes |

**Decision: Comparator.** The brief states the component will be reused for players, colleges and quiz teams. Reuse across *types* needs generics; reuse across *orderings of the same type* needs Comparator. A team is ranked by points in the league table, by name in the programme, and by goal difference in the statistics page - three orderings of one type, and a class can have only one `compareTo`.

## 5.3 The multi-field comparison

```java
Comparator.comparingInt(Team::points).reversed()
          .thenComparing(comparingInt(Team::goalDiff).reversed())
          .thenComparing(Team::name);
```

A detail easy to get wrong: `.reversed()` is applied to **each numeric key separately**, not to the whole chain. Writing `comparing(points).thenComparing(goalDiff).thenComparing(name).reversed()` would reverse the **name** too, ranking Z before A among tied teams.

## 5.4 Testing with ties at every level

| ID | Test | Expected | Result |
|---|---|---|---|
| T1 | tie on points only, split by goal difference | `[Yaks(12,0), Zebras(12,0)]` | PASS |
| T2 | tie on points **and** goal difference, split by name | `[Bisons(9,4), Panthers(9,4)]` | PASS |
| T3 | three-way tie, alphabetical | `[Antelopes, Cobras, Dingoes]` | PASS |
| T4 | the comparator is a total order (machine-checked) | true | PASS |
| T5 | `ranking()` does not disturb the stored order | 8 | PASS |

## 5.5 Data designed to break a faulty comparator

| ID | Test | Expected | Result |
|---|---|---|---|
| F1 | subtraction comparator: is it a total order? | **false** | PASS |
| F2 | correct comparator on the same data | true | PASS |
| F3 | `b.points - a.points` for MAX vs MIN+1 has the wrong sign | true | PASS |
| F4 | missing tie-break still a total order? | **true** | PASS |
| F5 | but it ranks a worse goal difference first | `Bisons(9,4)` | PASS |
| F5b | the correct comparator ranks the best first | `Eagles(9,6)` | PASS |
| F6 | "always 1" comparator is a total order? | **false** | PASS |
| F7 | does `List.sort` reject it? | **never, up to 4096 elements** | PASS |

**Fault A - overflow.** `MAX_VALUE - (MIN_VALUE+1)` overflows to 2, so "Huge" with `Integer.MAX_VALUE` points is ranked **below** "Tiny".

**Fault B - a missing tie-breaker is still a valid total order.** It simply encodes the **wrong rule**. A consistency check alone would pass it, which is why ties must also be tested against the stated competition rules.

**Fault C - and this is the important finding, because it is not what I predicted.** I expected `TimSort` to throw *"Comparison method violates its general contract!"*. **It did not, at any size tested up to 4,096.** That check fires only when a merge happens to encounter a specific inconsistency; it is a best-effort diagnostic, **not a validation**. The faulty comparator produced:

```
[Panthers(9,4), Eagles(9,6), Cobras(6,1), Bisons(9,4), Dingoes(6,1), Antelopes(6,1), Zebras(12,0), Yaks(12,0)]
```

A team with **12 points ranked seventh**, with no error of any kind. Nothing in that output announces that it is wrong. This is the strongest argument for the `isTotalOrder()` check used in T4: **the library will not verify the principle for me, so the component has to verify it itself.**

## 5.6 Why the comparator satisfies the principle - by reasoning, not output

The chain is a **lexicographic composition** of three keys: `k1 = -points`, `k2 = -goalDiff`, `k3 = name`.

Each key maps a Team to a value in a set that already carries a total order: `int` for k1 and k2, `String` for k3. `comparingInt` compares those with `<` on int, transitive and antisymmetric by the definition of the integers; `String.compareTo` is likewise a total order. **No subtraction is performed anywhere**, so no comparison can overflow.

`thenComparing` composes them lexicographically: the result is decided by the first key that differs. A lexicographic composition of total orders is itself a total order - if A>B and B>C, let i be the first key where A and B differ and j the first where B and C differ; at `min(i,j)` the comparison of A and C is decided in the same direction, because every earlier key is equal across all three. **Transitivity follows.**

Equal items compare as equal: two Teams compare 0 exactly when all three fields are equal - which, since `Team` is a record whose `equals` uses those same fields, is exactly when they are equal. The comparator is therefore **consistent with equals**, so the component behaves identically in a List, a TreeSet or a TreeMap.

## 5.7 Complete the Code - `LeaderboardDemo`

```java
// TODO 1: Return a new sorted copy of items using order, without modifying items
List<T> copy = new ArrayList<>(items);
copy.sort(order);
return copy;

// TODO 2: points (high first), then goal difference (high first), then name (A-Z)
Comparator<Team> byRank =
        Comparator.comparingInt((Team t) -> t.points).reversed()
                  .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
                  .thenComparing(t -> t.name);
```

| | Output |
|---|---|
| **Expected** | `[Eagles(9,6), Bisons(9,4), Panthers(9,4), Cobras(6,1)]` |
| **Actual** | identical - `diff` reports no difference |

### Why Bisons is ranked above Panthers

Both have **9 points** and both have a **goal difference of 4**, so the first two keys are exhausted without a decision. The third key decides: `"Bisons".compareTo("Panthers")` is negative because `B` precedes `P`, so Bisons sorts first. Without that third key the two would compare equal and their relative order would be whatever the sort left - which, since `List.sort` is stable, would be *insertion* order, putting Panthers first. The tie-breaker is what makes the result defined rather than accidental.

## 5.8 Generics - bounded types and wildcards

`Comparator<? super T>` rather than `Comparator<T>`: a Comparator **consumes** T, so by PECS it takes `? super T`. This lets a `Leaderboard<Team>` be built from a `Comparator<Object>`, which a `Comparator<Team>` parameter would reject for no good reason.

`addAll(List<? extends T>)`: `addAll` **produces** T values for the board, so it takes `? extends T`. Demonstrated in the output - `Leaderboard<Object>.addAll(List<Team>)` compiles; with a plain `List<T>` it would not, since `List<Team>` is not a subtype of `List<Object>`.

A bounded version `RankedBoard<T extends Comparable<? super T>>` is included for the case where the type carries its own order.

## 5.9 Alternative Concept and Complexity Analysis

All three produce an identical ranking. Measured at n = 500,000:

| Approach | Sorts | Median (ms) | Time | Extra space |
|---|---|---|---|---|
| A Comparator chain | 1 | 260.853 | O(n log n) | O(n) copy |
| B Comparable | 1 | **203.301** | O(n log n) | same |
| C three stable sorts | **3** | 330.666 | **3 x O(n log n)** | allocated 3 times |

**Why C works at all:** `List.sort` is **stable**. Sorting by the least significant key first and the most significant last means each later sort preserves the ordering earlier ones established among its own ties. Reverse the three calls and the result is simply wrong - the technique depends entirely on a property of the sort, which makes it fragile.

| Approach | Second ordering of Team | New type (Player) | Verdict |
|---|---|---|---|
| **A** | pass a different Comparator | same `Leaderboard<Player>` | **reusable** |
| B | impossible without a Comparator | edit Player | least reusable |
| C | rewrite the sequence of sorts | rewrite again | reusable, verbose |

**A is the answer.** B bakes one ordering into the type, so the programme listing and the statistics page each need a Comparator anyway - at which point the Comparable adds a fourth way of ordering the same type and invites the two to disagree.

---

# Q6 - Priority, Order and Fairness in a Queue  *(8 marks, K5)*

**Evidence:** `Q6/`, `data/Q6-*.csv`, `charts/q6-queues.png`

## 6.1 Why a plain Queue is not sufficient

A `Queue` is strictly FIFO. The rule here is the opposite. Demonstrated:

```
ArrayDeque order : Ravi(S3) Meena(S1) Asha(S5) Kumar(S1)
required order   : Meena(S1) Kumar(S1) Ravi(S3) Asha(S5)
```

A **critical patient is served fourth**, behind two less severe ones. Sorting on every admission would fix the order at O(n log n) per arrival; a `PriorityQueue` maintains a binary heap so the minimum is always at the root - O(log n) to admit, O(log n) to serve, O(1) to peek.

## 6.2 How arrival order is preserved among equal severities

A `PriorityQueue` is **not stable**. Two patients of equal severity emerge in whatever order the heap's sift operations left them. The fix is to make the ordering **total**: a strictly increasing arrival counter as a second key, so no two cases ever compare equal.

A wall-clock timestamp would **not** be safe - two admissions in the same millisecond would tie again - so a counter is used.

## 6.3 Correctness check on 8 patients worked out by hand

By hand: group by severity, then by arrival within each group.

```
S1 : Meena(#1), Kumar(#4)      S2 : Lata(#5), Dev(#7)
S3 : Ravi(#0),  John(#2)       S5 : Asha(#3), Sita(#6)
=> Meena Kumar Lata Dev Ravi John Asha Sita
```

| ID | Test | Expected (by hand) | Result |
|---|---|---|---|
| C1 | waiting list in treatment order | `[Meena(S1), Kumar(S1), Lata(S2), Dev(S2), Ravi(S3), John(S3), Asha(S5), Sita(S5)]` | PASS |
| C2 | `peek()` shows the next patient | `Meena(S1)` | PASS |
| C4 | actual treatment order | `Meena Kumar Lata Dev Ravi John Asha Sita` | PASS |
| C6 | `next()` on empty returns null | `null` | PASS |
| C7/C8 | severity 0 and 6 rejected | `IllegalArgumentException` | PASS |

**8 of 8 hand-verified checks pass.**

### Why printing the PriorityQueue directly does not show treatment order

```
toString() / iterator() : [Meena(S1), Dev(S2), Lata(S2), John(S3), Kumar(S1), Asha(S5), Sita(S5), Ravi(S3)]
waitingList()           : [Meena(S1), Kumar(S1), Lata(S2), Dev(S2), Ravi(S3), John(S3), Asha(S5), Sita(S5)]
```

A `PriorityQueue` is a **binary heap stored in an array**. The heap property guarantees only that each node is <= its two children - nothing about siblings or about the array order beyond that. The array is **partially ordered**: index 0 is genuinely the next patient, and the rest is not a ranking at all. Iteration walks that array, showing the heap's internal layout. Displaying a waiting list requires a copy and a sort - O(n log n), the cost Q7 takes up.

## 6.4 Growth - predicted, then measured

**Prediction:** `admit` and `next` are O(log n); `peek` O(1); `waitingList` O(n log n). Because log2(n) grows so slowly, going from 1,000 to 100,000 patients (100x) should raise per-operation cost by only about **1.7x**, not 100x.

| n | admit (ns/op) | serve all (ns/op) | waitingList (ms) |
|---|---|---|---|
| 1,000 | 511.21 | 528.75 | 0.241 |
| 10,000 | 94.30 | 302.98 | 1.216 |
| 100,000 | 147.01 | 408.22 | 6.608 |
| 1,000,000 | - | - | - |

The admit and serve columns stay in the same order of magnitude across a 100x range of n, confirming the log n prediction. `waitingList` rises roughly in proportion to n log n, from 0.24 ms to 6.6 ms.

## 6.5 Complete the Code - `Triage`

```java
// TODO 1: a PriorityQueue serving lowest severity first, then earliest arrival
private final PriorityQueue<Patient> queue = new PriorityQueue<>(
        Comparator.comparingInt((Patient p) -> p.severity)
                  .thenComparingLong(p -> p.arrival));
```

| | Output |
|---|---|
| **Expected** | `Meena(S1) Kumar(S1) Lata(S2) Ravi(S3) John(S3) Asha(S5)` |
| **Actual** | identical - `diff` reports no difference |

### How the arrival counter keeps equal severities in arrival order

`counter++` is evaluated inside `admit()`, so each patient receives a strictly larger `arrival` than everyone admitted before. The comparator tests severity first; when two patients tie there, `thenComparingLong` breaks the tie on `arrival`, and since those values are strictly increasing and never repeat, the earlier admission always wins. The effect is that the ordering becomes **total** - no two patients ever compare equal - which is what removes the heap's freedom to return them in an arbitrary order.

## 6.6 Starvation - why, and by how much

The comparator orders on severity **first** and arrival only as a tie-break. A severity-5 case is overtaken by every severity 1-4 patient, no matter how long it has waited. Formally, it reaches the head only when the queue contains no case of severity 1-4; if such cases arrive at least as fast as they are served, that condition is never reached. **This is a property of the ordering, not a bug in the implementation.**

Simulated over 20,000 ticks, 1.15 arrivals per tick, 1 served per tick:

| Severity | Arrived | Served | Mean wait | Worst wait | **Never served** |
|---|---|---|---|---|---|
| S1 | 1,844 | 1,844 | 0.0 | 1 | 0 |
| S2 | 3,657 | 3,657 | 0.1 | 3 | 0 |
| S3 | 6,410 | 6,410 | 0.3 | 8 | 0 |
| S4 | 5,963 | 5,963 | 2.5 | 43 | 0 |
| **S5** | 5,091 | 2,126 | **5,878.9** | **11,463** | **2,965** |

## 6.7 The fair rule - and the first version of it was wrong

**Design:** a case waiting longer than a threshold is promoted by one severity level *for ordering purposes only*; the clinical severity on the chart is never altered.

```
effectiveSeverity(c, now) = max(ownFloor, c.severity - (now - c.arrival) / THRESHOLD)
```

**My first implementation had no floor**, and the data refuted it:

| Measure | Strict | Aging, no floor | Verdict |
|---|---|---|---|
| S5 mean wait | 5,878.9 | 1,318.7 | improved |
| S5 never served | 2,965 | 672 | improved |
| **S1 mean wait** | **0.0** | **1,268.8** | **MADE WORSE** |
| **S1 worst wait** | **1** | **2,583** | **MADE WORSE** |

The cause is structural, not a tuning error. With no floor, **every** waiting case eventually reaches level 1, so all cases share the same effective severity and the arrival tie-break decides everything - the rule **degenerates to FIFO**, precisely the behaviour section 6.1 showed to be unacceptable. Lengthening the threshold only delays that collapse.

**Correction: a promotion floor of `min(severity, 2)`.** A case may age up to level 2 but never to level 1, so a genuine severity-1 arrival is never overtaken.

| Measure | Strict | No floor | **Floor = 2** | Best |
|---|---|---|---|---|
| S1 mean wait | 0.0 | 1,268.8 | **0.0** | tie |
| S1 worst wait | 1 | 2,583 | **1** | tie |
| S5 mean wait | 5,878.9 | 1,318.7 | **1,421.8** | no floor |
| S5 worst wait | 11,463 | 2,583 | **2,818** | no floor |
| S5 never served | 2,965 | 672 | **731** | no floor |

The floored rule is the only one of the three acceptable on **both** measures: critical patients keep an essentially immediate wait, and minor patients are no longer left unseen indefinitely. Whether the remaining S5 wait is clinically acceptable is a decision for the department; the threshold and the floor are the two dials that set it, which is why both are named constants.

### Why aging rather than the obvious alternatives

* a **fixed quota** ("serve one S5 every 10 patients") serves a minor case ahead of a critical one on a fixed schedule - clinically indefensible;
* a **pure deadline** has the same flaw at the deadline, and produces a stampede when many expire together;
* **aging degrades gradually**: a long-waiting S5 first competes with S4, then S3, and reaches the head only if it keeps being overtaken.

## 6.8 Deployment decision - 5,000 patients a day?

5,000 patients a day is one arrival every 17.3 seconds. From the measurements: 5,000 admissions + 5,000 services is about **2.8 ms of CPU per day**. A realistic waiting room holds tens of patients, so n stays small and log n stays under 6. The structure would be comfortable at a hundred times this load.

**Verdict: yes on throughput, with two caveats that matter more than the timings:**

1. **Without the fair rule it should not be deployed at any scale.** The simulation left 2,965 minor patients unseen and the worst minor wait reached 11,463 ticks. That is a clinical safety failure, and no amount of throughput compensates for it.
2. **The waiting-list display is O(n log n)** because the heap must be copied and sorted. On a screen refreshed every few seconds that is the dominant cost of the whole system - far larger than admit and serve combined. This is the limitation Q7 takes up.

## 6.9 Alternative Concept and Complexity Analysis

All three produce an identical treatment order. Measured (ns per operation):

| n | A PriorityQueue | B sorted LinkedList | C five FIFO buckets |
|---|---|---|---|
| 1,000 | 484.26 | 832.67 | **161.05** |
| 10,000 | 232.79 | 7,494.05 | **66.32** |
| 100,000 | 267.68 | *omitted* | **30.62** |

B is omitted above n = 10,000 because its O(n) insertion makes the whole run O(n²) - at n = 100,000 it would take minutes. **Leaving it out is itself the result.**

| Design | admit | next | waiting list | space |
|---|---|---|---|---|
| A PriorityQueue | O(log n) | O(log n) | O(n log n) sort | 1 slot/case |
| B sorted LinkedList | **O(n)** | O(1) | O(n), free | 24 B node/case |
| **C five FIFO buckets** | **O(1)** | **O(1)\*** | **O(n), free** | 1 slot + 5 deques |

\* C's `next()` scans at most 5 buckets - O(k) where k is the number of severity **levels**, fixed at 5 by the triage scale. A constant bound of 5 is O(1).

C is best on every operation, and the reason is worth stating precisely: **it exploits a property of the problem the other two ignore.** Severity is drawn from a small, fixed, known range. A PriorityQueue is a general-purpose comparison structure that would work for any key, and pays log n for that generality. When the key range is bounded, comparisons can be replaced by **indexing** - the same insight that makes counting sort beat comparison sorts on small integer keys.

C also needs **no comparator and no arrival counter at all**: FIFO within a bucket preserves arrival order automatically, and scanning buckets 1..5 enforces severity. The two rules the comparator encoded are expressed by the **structure** instead.

**Where C would stop being right:** if severity became a continuous clinical score rather than 5 discrete levels, the bucket array would need one entry per distinct score and the approach would collapse. A PriorityQueue does not care how many distinct keys exist.

---

# Q7 - DAY-4 DESIGN CHALLENGE: Diagnose, Redesign and Validate  *(13 marks, K5)*

**Evidence:** `Q7/`, `data/Q7-*.csv`, `charts/q7-display.png`, `charts/q7-admit-serve.png`

**Solution selected: the Q6 triage queue.** It is chosen over the obvious candidate - Q1's string concatenation - because replacing `+` with `StringBuilder` is a one-line substitution with nothing to design. The triage queue shows a limitation that **no collection swap fixes**: one of its two defects is a property of the ordering rule, not of the structure holding it.

## A. Diagnose the Bottleneck  *(2 marks)*

Evidence gathered in Q6, not assumed here:

| Measurement | Value |
|---|---|
| `admit()` at n = 100,000 | ~147 ns (O(log n)) |
| `next()` at n = 100,000 | ~408 ns (O(log n)) |
| **`waitingList()` at n = 100,000** | **~6.6 ms (O(n log n))** |
| severity-5 patients never served | 2,965 of 5,091 |
| worst severity-5 wait | 11,463 ticks |

**Where the time goes.** The waiting-room screen refreshes continuously. At one refresh per second, `waitingList()` costs 6.6 ms **every second**, while admissions and services together cost under 3 ms **per day**. The display is roughly **four orders of magnitude** more expensive than the queue operations it exists to show.

**Which operation grows.** `waitingList()` is O(n log n); admit and next are O(log n). The display is the only operation whose cost grows faster than the patient count.

**Is work repeated unnecessarily? Yes - and this is the core finding.** The heap **already knows** the ordering, but cannot expose it, because a binary heap is only *partially* ordered. Every refresh re-derives, by a full sort, an ordering the structure has already paid O(log n) per insertion to maintain. The same sort runs every second even when no patient has arrived or left.

**Which characteristic causes it.** `PriorityQueue` is a comparison-based binary heap. It guarantees only `parent <= children`, exactly enough to find the minimum in O(1) and no more. **A total order over all n elements is simply not information the structure holds.**

**The second defect, which is not a performance problem at all.** The comparator orders on severity first, so waiting time never influences priority and minor cases starve. **No substitution of one collection for another fixes this** - the defect is in the ordering rule, and any structure given the same comparator starves patients identically.

## B. Define the Design Objective  *(2 marks)*

| | Objective | Measurable target |
|---|---|---|
| **O1** | keep the waiting list ordered **by construction** | display O(n), no sort |
| **O2** | remove the per-operation log n factor | admit and next O(1) |
| **O3** | bound the wait of the least urgent patients | S5 never-served under 1,000 |
| **O4** | do not degrade critical care in doing so | S1 mean wait stays at 0 ticks |
| **O5** | preserve correctness | identical treatment order |

O4 and O5 are stated as explicitly as O1-O3 on purpose. A redesign that improved the display while reordering patients would be a failure, and without a stated target there would be nothing to measure that against.

## C. Design the Revised Solution  *(4 marks)*

**What changed:** one `PriorityQueue` becomes an **array of five `ArrayDeque`s**, indexed by severity.

```
level[1] -> [ Meena  Kumar ]        <- served first, FIFO within
level[2] -> [ Lata   Dev   ]
level[3] -> [ Ravi   John  ]
level[4] -> [ ]
level[5] -> [ Asha   Sita  ]        <- served last
```

**Which property is exploited:** severity is drawn from a fixed, small, known range. A comparison-based heap pays O(log n) to be able to order *any* key type; when the key range is bounded and tiny, comparisons can be replaced by **array indexing**. This is the same insight that lets counting sort beat comparison sorts on small integer keys.

**Which operations are avoided:**

* the sift-up on admit and sift-down on serve disappear - a deque append and poll are both O(1);
* **the sort on every display disappears** - concatenating the buckets in index order *is* the treatment order, so the display becomes a walk;
* the arrival counter disappears from the ordering logic - FIFO within a bucket preserves arrival order structurally.

**Additional memory or preprocessing:** five deque objects, a fixed cost independent of n. Per patient the storage is one array slot, the same as the heap. No preprocessing.

**How the fair rule becomes expressible:** aging means **moving a case to a more urgent bucket** - an O(1) unlink and append. In the heap this was impossible to do cleanly: changing a key of an element *inside* a heap invalidates the heap property, and `PriorityQueue` offers no reheapify, so Q6 had to rebuild the ordering at every service. Because each bucket is in arrival order, the longest-waiting case is always at its **front**, so the promotion sweep stops at the first case not yet due rather than scanning.

**New trade-offs created:**

* the design is now **specific to a bounded key range** - a continuous clinical score would collapse it;
* `next()` scans up to 5 buckets, so on a near-empty queue it does 5 checks where the heap did 1;
* five deques have five backing arrays, raising the memory floor.

## D. Predict Before Implementing  *(1 mark)*

| | Predicted outcome | Expected magnitude |
|---|---|---|
| **P1** | `waitingList()` becomes dramatically cheaper | 20x or more at n = 100,000 |
| **P2** | admit+serve improves, but only modestly | 2x to 10x - log n is small |
| **P3** | the gap on display **widens** with n | it is O(n log n) vs O(n) |
| **P4** | treatment order byte-for-byte identical | exact match required |
| **P5** | aging bounds the S5 wait, S1 unaffected | S1 mean stays 0.0 |

P2 is deliberately modest. log2(100,000) is only 17, so removing a log factor cannot produce the kind of gain the display will show. Predicting a large speed-up there would be predicting the wrong thing.

## E. Implement and Validate  *(2 marks)*

### E.1 Correctness first

| ID | Test | Result |
|---|---|---|
| V1 | waiting list identical to the original | PASS |
| V2 | treatment order matches the Q6 hand-worked answer | PASS |
| V3 | original produces the same order | PASS |
| V4 | empty queue returns null | PASS |
| V5 | invalid severity rejected | PASS |
| **V6** | **identical order over 200 randomised admission streams** | **PASS** |

**6 of 6 validation checks pass.**

### E.2 Performance, same conditions as the original

| n | Operation | ORIGINAL heap | REDESIGN buckets | Gain |
|---|---|---|---|---|
| 1,000 | admit+serve (ns/op) | 428.93 | **139.45** | 3.1x |
| 10,000 | admit+serve (ns/op) | 154.25 | **64.61** | 2.4x |
| 100,000 | admit+serve (ns/op) | 182.32 | **20.62** | **8.8x** |

### E.3 The display - the objective that mattered

| n | ORIGINAL (ms) | REDESIGN (ms) | Speed-up |
|---|---|---|---|
| 1,000 | 0.260 | 0.007 | **37x** |
| 10,000 | 1.384 | 0.045 | **31x** |
| 100,000 | 10.089 | 0.120 | **84x** |

The speed-up **grows** with n (37x -> 84x), which is the signature of removing a complexity factor rather than a constant. A constant-factor improvement would keep that column flat.

### E.4 Fairness

| Measure | ORIGINAL | REDESIGN + aging | Change |
|---|---|---|---|
| S1 mean wait (ticks) | 0.0 | **0.0** | unchanged |
| S5 mean wait (ticks) | 5,878.9 | **3,028.0** | improved 48% |
| S5 worst wait (ticks) | 11,463 | **5,934** | improved 48% |
| S5 never served | 2,965 | **1,515** | improved 49% |
| promotions performed | *impossible* | 20,915 | O(1) each |

## F. Defend the Final Design  *(2 marks)*

| | Objective | Target | Outcome |
|---|---|---|---|
| O1 | display ordered by construction | O(n), no sort | **MET** - 84x faster at n = 100k |
| O2 | remove the log n factor | admit/next O(1) | **MET** |
| O3 | bound the least urgent wait | S5 unseen < 1,000 | **NOT MET** - 1,515 |
| O4 | do not degrade critical care | S1 mean stays 0.0 | **MET** - 0.0 |
| O5 | preserve correctness | identical order | **MET** - 200/200 streams |

### Why O3 was not met, and why that is not a failure of the redesign

The cause is **arithmetic, not algorithmic**. The simulation admits 1.15 patients per tick and serves 1. Over 20,000 ticks that is ~23,000 arrivals against 20,000 services, so **about 3,000 patients must remain unseen no matter how they are ordered**. The queue cannot serve patients the doctor does not have time to see.

What an ordering policy *can* decide is **which** patients are left, and that is what changed: under the original rule every single unseen patient was severity 5; the aging rule spreads the shortfall across severities instead of concentrating it on the least urgent.

The right target would therefore have been a **proportion of the unavoidable shortfall**, not an absolute count - setting it absolutely was a mistake in section B, made before the capacity arithmetic was worked through. O3 is not reachable by any queue design at this arrival rate. **It needs a second doctor.** Reporting the objective as met by quietly loosening it would have hidden the one finding here that actually matters to the hospital.

### Was the prediction supported?

| | Prediction | Outcome |
|---|---|---|
| P1 | display 20x or more | **SUPPORTED** (84x) |
| P2 | admit+serve 2x to 10x | **SUPPORTED** (2.4x to 8.8x) |
| P3 | gap widens with n | **SUPPORTED** (37x -> 84x) |
| P4 | identical order | **SUPPORTED** (200/200) |
| P5 | S5 bounded, S1 unaffected | **SUPPORTED** |

### New costs and trade-offs accepted

* the design depends on severity having a small fixed range - a real loss of generality, written into the class rather than hidden: `LEVELS` is a named constant and `admit()` validates against it, so the assumption fails loudly;
* five backing arrays instead of one raises the memory floor;
* `next()` does up to 5 checks where the heap did 1, so on a near-empty queue the redesign is marginally **slower** per operation.

### When the original would still be preferable

* if severity became a **continuous score**, or the number of levels grew large or unbounded, the bucket array becomes impractical and the heap is correct again;
* if the waiting list were **never displayed**, the dominant cost disappears and the remaining gain is a modest constant - not enough on its own to justify a hand-written structure over a standard library class;
* `PriorityQueue` is tested, documented and familiar. Replacing it puts the correctness burden on me, which is why V6 checks **200 randomised streams** rather than one example.

### Final engineering conclusion

**Adopt the redesign for this system.** The decisive argument is not the 84x display speed-up, welcome as it is - it is that the redesign made the **fairness rule expressible**. Starvation was a clinical safety defect, no collection swap could fix it, and in the heap the only available workaround was to recompute the ordering at every service. In the bucket design a promotion is an O(1) move between two deques.

The general lesson, and the one I would defend at a viva: **the heap was not slow because it was badly implemented. It was slow because it solves a more general problem than this one** - ordering arbitrary keys by comparison - and the price of that generality is a log factor and the loss of a total ordering over the contents. Narrowing the problem to what it actually is, five discrete levels served FIFO within each, is what removed the cost. Choosing a less general structure is only safe when the narrower assumption is genuinely guaranteed - and when it is written down, as `LEVELS` is here.

---

# Summary of evidence

| Q | Programs | Key evidence |
|---|---|---|
| Q1 | `TicketBuilder`, `StringWorkloadBenchmark`, `ScalingProbe`, `SharedLogContention` | 460x gap on W3; doubling ratio converges on 4; shared `StringBuilder` corrupted 4 of 5 logs |
| Q2 | `PlaylistBenchmark`, `OperatingBoundary`, `ManualArrayPlaylist` | LinkedList middle-insert 40x slower despite equal O(n); **no crossover found - prediction refuted** |
| Q3 | **`Attendance`**, `ContractExperiments`, `DedupAlternatives` | 18/18 predictions confirmed; mutated key makes an element unfindable *and* unremovable |
| Q4 | `PharmacyInventory`, `ScalabilityDecision` | TreeMap lookups 38x slower; arrival order **unrecoverable** from HashMap |
| Q5 | **`LeaderboardDemo`**, `RankingDesign`, `SortingAlternatives` | 14/14 checks; **TimSort did NOT reject the inconsistent comparator - prediction refuted** |
| Q6 | **`Triage`**, `TriageDesign`, `Starvation`, `QueueAlternatives` | 8/8 hand-verified; 2,965 patients starved; **first fair rule degenerated to FIFO - corrected** |
| Q7 | `TriageRedesign` | 6/6 validation; display 84x faster, speed-up grows with n; O3 honestly reported unmet |

**Totals: 46 automated checks across Q3, Q5, Q6 and Q7, all passing. 2,389 individual timing repetitions written to CSV. Three Complete-the-Code programs matching their expected output exactly. Three predictions refuted by measurement and analysed rather than revised.**
