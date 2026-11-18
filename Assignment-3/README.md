# Assignment 3 (CO3) - Exception Handling, Multithreading and Concurrency

**Vignesh S - 192325002 - CSA09 Programming in Java**

Full write-up with every measurement: **[REPORT.md](REPORT.md)**

## Contents

| Folder | Question | Marks | What is in it |
|---|---|---|---|
| `Q1/` | Handling Failure in a Bank ATM Simulation | 6 | `AtmSimulation.java`, `ExceptionCost.java`, `compile_errors/` (three real `javac` errors) |
| `Q2/` | User-Defined Exceptions and Propagation | - | `StorageDemo.java` (Complete the Code) |
| `Q3/` | Resource Safety, Debugging and Robust Code | 7 | `SalesReport.java` (three versions), `debug/` (`jdb` transcript, descriptor-limit reproduction) |
| `Q4/` | Thread Creation and the Thread Life Cycle | 7 | `ScannerStations.java`, `StationExperiments.java`, `DaemonDemo.java`, `ThreadsVsPool.java` |
| `Q5/` | Race Conditions in a Cinema Seat-Booking System | 6 | `SeatBooking.java`, `unsynchronized/`, `RaceAnalysis.java` |
| `Q6/` | Deadlock and Inter-Thread Communication | 7 | `TokenCounter.java`, `if_version/` (the `if`-vs-`while` stress harness) |
| `Q7/` | A Concurrent, Fault-Tolerant Processor | 10 | `ResultProcessor.java`, `ProcessorRunner.java` |
| `lib/bench/` | - | - | the shared measurement harness, carried over from Assignment 2 |
| `data/` | - | - | raw per-run timings (CSV) for every timed experiment |
| `charts/` | - | - | the three charts used in the report |

## Building and running

```bash
javac -d lib/out lib/bench/Bench.java                 # the harness first
javac -d Q1 Q1/AtmSimulation.java && java -cp Q1 AtmSimulation
```

Every question's full command line is listed at the top of `REPORT.md`. Each
program's console output is committed beside its source as a `.txt` file, so
every number in the report can be traced to the run that produced it.

## Three findings worth reading first

1. **Q5** - the unsynchronised seat booking printed the correct answer in
   **10 of 10 runs**. With a starting gate and a widened check-to-act window it
   **oversold in 9 of 40 trials**, worst case 18 seats sold for a 10-seat
   screen. The defect never changed; only the chance of observing it did.

2. **Q6** - replacing `while` with `if` around `wait()` passed **12 of 12**
   runs at the supplied workload. Under a slow producer and six consumers it
   produced **25 wrong sums out of 25**, with a buffer `count` reaching -238.

3. **Q1** - an exception costs the same as an `if` when it is never thrown, and
   **about 1,000x more** when it is always thrown. About **98% of that cost is
   capturing the stack trace**, not the `throw`.

## Requirements

JDK 21 or later. No external dependencies.
