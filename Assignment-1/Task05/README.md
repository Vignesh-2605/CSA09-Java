# Task05 - Apply static Members and the Object Life Cycle

**3 marks - K4 Analyse**  ·  Vignesh S · 192325002

## Files

- **`StaticMembersDemo.java`** - static counter, static block, static methods, and garbage collection observed
- **`CounterScopeComparison.java`** - alternative: static vs instance counter across two gates

## How to run

```bash
cd Task05
javac StaticMembersDemo.java && java StaticMembersDemo
javac CounterScopeComparison.java && java CounterScopeComparison
```

## Result

- The static block runs **before any object exists** - `Ticket.rateFor(1)` returns 30 with no Ticket created.
- Garbage collection is **observed, not asserted**: a `WeakReference` still resolves after `close()`, and resolves `false` only after the ticket leaves the active list.
- An instance counter produces **4 collisions out of 8 tickets** across two gates. The proposed fix is a per-facility counter plus a facility code (`N-1001`, `S-1001`), costing ~36 B more per ticket.

## Evidence in this folder

- `*_output.txt` - the captured console output of each run
- `screenshots/` - the same output as terminal screenshots

Full discussion: [`../REPORT.md`](../REPORT.md)
