# Assignment 4 (CO4) - Event-Driven GUI Design

**Vignesh S - 192325002 - CSA09 Programming in Java**

Full write-up with every measurement: **[REPORT.md](REPORT.md)**

## Contents

| Folder | Task | What is in it |
|---|---|---|
| `Task01/` | GUI Design Review | `BadFeePaymentWindow.java` (the faults, kept on purpose) and `redesign/` - window, service, repository, plus a headless test of all the rules |
| `Task02/` | Requirement to Event-Driven Design | `AppointmentDesk.java` - every listener also writes to an event log, so the method-call trace is recorded by the running program |
| `Task03/` | From Sketch to Layout | `LibrarySearch.java` and a harness that measures what grows and what does not at three window sizes |
| `Task04/` | Event-Handling Forensics | three versions of the drawing pad - as supplied, TODO 1 only, both TODOs - plus the captured `javac` error that `@Override` produces |
| `Task05/` | Same Screen, Four Event Styles | the converter wired four ways, sharing one rule and one window so only the wiring differs |
| `Task06/` | Design Challenge | campus event registration: Swing + JDBC in three layers, `SwingWorker`, pagination, CSV export, and a 100,000-row benchmark |
| `sql/` | - | `task06_schema.sql` |
| `shots/` | - | 33 screenshots, each one a real window captured with `java.awt.Robot` |
| `lib/` | - | the screenshot helper, the database config, and the Assignment 2 benchmark harness |

## Running

```bash
export DISPLAY=:99          # Xvfb :99 -screen 0 1400x900x24 &
mysql < sql/task06_schema.sql
javac -d lib/out lib/Shot.java lib/Db.java
```

Each task's full command line is at the top of `REPORT.md`. Every program's
console output is committed beside its source as a `.txt` file.

## Four findings worth reading first

1. **Task 6** - paginated loading reaches the first screen **104x faster**
   than loading all 100,000 registrations (1.4 ms against 141.7 ms) and holds
   **512 kB instead of 82 MB**. Running that same query on the Event Dispatch
   Thread blocks it for **231 ms**; in a `SwingWorker` the EDT answers in
   **0.3 ms**.

2. **Task 4** - the supplied drawing pad compiles cleanly and is silently dead
   to the mouse, because `mouseClick` is not `mouseClicked`. Adding
   `@Override` turns that into a compile error. Captured from `javac`.

3. **Task 1** - moving the rules out of the event handler is what made them
   testable: **16 validation checks now run with no window on screen**, and
   none of them could have been written against the original.

4. **Task 5** - the lambda version needs 16 lines and 2 wiring lines; the
   anonymous-inner-class version needs 25 and 4, plus two extra classes, for
   an identical window.

## Requirements

JDK 21 or later, an X display, and MariaDB or MySQL for Task 6
(`sql/task06_schema.sql` creates everything it needs).
