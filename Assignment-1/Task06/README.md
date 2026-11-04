# Task06 - Design the Inheritance Hierarchy

**4 marks - K4 Analyse**  ·  Vignesh S · 192325002

## Files

- **`InheritanceDemo.java`** - the hierarchy, `super` used three ways, and the constructor order traced
- **`CompositionComparison.java`** - alternative: inheritance vs composition (`Vehicle has-a VehicleType`)

## How to run

```bash
cd Task06
javac InheritanceDemo.java && java InheritanceDemo
javac CompositionComparison.java && java CompositionComparison
```

## Result

- Multilevel `Vehicle -> Car -> ElectricCar` plus hierarchical `CampusUser`. UML: `../docs/original-design.png`.
- Constructor order traced as **`Vehicle(...) -> Car(...) -> ElectricCar(...)`** - the most general constructor finishes first.
- The composition alternative adds a DELIVERY_VAN tariff **at run time** with no class compiled for it. Noted as the right answer for the Day-4 challenge, which Task 12 adopts as a hybrid.

## Evidence in this folder

- `*_output.txt` - the captured console output of each run
- `screenshots/` - the same output as terminal screenshots

Full discussion: [`../REPORT.md`](../REPORT.md)
