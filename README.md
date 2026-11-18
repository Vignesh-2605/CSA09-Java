# CSA09 - Programming in Java

Coursework repository for **CSA09 Programming in Java**.

| | |
|---|---|
| **Name** | Vignesh S |
| **Register Number** | 192325002 |
| **Course** | CSA09 - Programming in Java |
| **Java version** | OpenJDK 21.0.12, language level 21 |

## Contents

| Folder | Assignment | CO | Marks | Status |
|---|---|---|---|---|
| [`Assignment-1/`](Assignment-1) | Design, Implement and Validate an Object-Oriented Model for a Campus Smart-Parking System | CO1 | 50 | Complete - 130 automated assertions passing |
| [`Assignment-2/`](Assignment-2) | Strings and Collections - predict, measure, analyse, redesign | CO2 | 50 | Complete - 46 checks passing, 2,389 timing repetitions |
| [`Assignment-3/`](Assignment-3) | Exception Handling, Multithreading and Concurrency | CO3 | 50 | Complete - 7 questions, 3 latent defects reproduced on purpose |
| [`Assignment-4/`](Assignment-4) | Event-Driven GUI Design | CO4 | 50 | Complete - 6 tasks, 33 real window screenshots |
| [`Assignment-5/`](Assignment-5) | Data Persistence Engineering - Files and Databases | CO5 | 50 | Complete - Part A design, Part B measured against a live database |

Each assignment folder is self-contained: it holds one folder per task with the
Java source, the captured output of running it, output screenshots, and its own
README and report.

## Assignment 5 at a glance

Files, streams, serialization and JDBC - assessed on whether the storage
choice is defended by measurement rather than by habit.

- Part A: two record layouts, SIMATS-standard pseudocode, DAO design,
  serialization versioning and a transaction design
- Part B: every program run against a live MariaDB test database
- reading 50 MB one byte at a time makes **52,428,830 system calls**; an 8 kB
  buffer makes **6,402**
- the unsafe login was **breached by 5 of 7 crafted inputs**, including
  logging in as ADMIN with no password
- a load that fails halfway leaves **499 orphan rows** under auto-commit and
  **zero** inside one transaction - and the transaction is **6.6x faster**
- full report: [`Assignment-5/REPORT.md`](Assignment-5/REPORT.md)

## Assignment 4 at a glance

Event-driven interfaces in Swing and AWT, from diagnosis through to a
database-backed application - assessed on evidence, not on screenshots.

- 6 tasks, **33 screenshots, every one a real window** captured with
  `java.awt.Robot` while a harness drove the components
- paginated loading reaches the first screen **104x faster** than loading all
  100,000 registrations, and holds **512 kB instead of 82 MB**
- a query on the Event Dispatch Thread blocks it for **231 ms**; the same
  query in a `SwingWorker` leaves it answering in **0.3 ms**
- the supplied drawing pad compiles cleanly and is silently dead to the mouse;
  `@Override` turns that into a compile error
- full report: [`Assignment-4/REPORT.md`](Assignment-4/REPORT.md)

## Assignment 3 at a glance

Exception handling, resource safety, threads and concurrency - assessed on
whether a defect can be *demonstrated*, not merely described.

- 7 questions, every program compiled and run, every output committed
- three programs that **passed while defective** and were then made to fail on
  purpose: the unsynchronised seat booking (correct 10 of 10 runs, then
  **oversold in 9 of 40 trials**), `if` instead of `while` around `wait()`
  (correct 12 of 12, then **25 wrong sums out of 25**), and a resource leak
  that crashes only once the descriptor limit is reached
- real `jdb` step-through evidence of the leak, and three real `javac` errors
  kept as design evidence
- the cost of an exception measured at five failure rates: identical to an `if`
  at 0%, **about 1,000x slower** at 100%, of which **98% is the stack trace**
- a 10,000-file fault-tolerant processor measured at pool sizes 1 to 16
- full report: [`Assignment-3/REPORT.md`](Assignment-3/REPORT.md)

## Assignment 2 at a glance

Strings, StringBuilder, regular expressions and the Collections Framework -
List, Queue, Deque, PriorityQueue, Set and Map, the hashCode-equals contract,
generics, Comparable and Comparator - assessed not on using the collections but
on the cycle **predict, design, implement, measure, analyse, evaluate,
diagnose, redesign, validate**.

- 7 questions, every claim backed by repeated averaged measurements
- a shared benchmark harness: 5 warm-up runs discarded, 11 measured, median reported
- **2,389 individual timing repetitions** written to CSV as raw evidence
- 7 charts generated from that data
- three Complete-the-Code programs matching expected output exactly
- **three predictions refuted by measurement** and analysed rather than revised
- full report: [`Assignment-2/REPORT.md`](Assignment-2/REPORT.md)

## Assignment 1 at a glance

Java fundamentals and object-oriented programming - data types, operators,
control statements, arrays, classes and objects, constructors, static members,
inheritance, method overriding, polymorphism, abstract classes, interfaces,
packages, access modifiers and encapsulation - applied to a campus
multi-level parking facility, then re-evaluated against changed requirements.

- 12 tasks, each with runnable, executed code
- both "Complete the Code" programs match their expected output exactly
- 66-case and 64-case test harnesses, **130 assertions, all passing**
- 10 compiler errors captured as design evidence
- full report: [`Assignment-1/REPORT.md`](Assignment-1/REPORT.md)

```bash
git clone https://github.com/Vignesh-2605/CSA09-Java.git
cd CSA09-Java/Assignment-1
cat README.md
```