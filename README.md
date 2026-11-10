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

Each assignment folder is self-contained: it holds one folder per task with the
Java source, the captured output of running it, output screenshots, and its own
README and report.

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
