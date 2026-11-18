# Assignment 5 (CO5) - Data Persistence Engineering

**Vignesh S - 192325002 - CSA09 Programming in Java**

Full write-up, Part A and Part B: **[REPORT.md](REPORT.md)**

## Contents

| Folder | Task | What is in it |
|---|---|---|
| `Task-B1/` | File Store | `BusPass`, `PassStore` (TODO 1 completed), the 9-case test, and the I/O benchmark |
| `Task-B2/` | Adversarial Input | the unsafe login and the `PreparedStatement` one, run against eight crafted inputs |
| `Task-B3/` | JDBC Performance | `PassLoader` (TODO 1-3 completed) and the 1k/10k/100k comparison with the transaction test |
| `Task-B4/` | Final Decision | the migration, `DatabaseMetaData`, and the storage decision |
| `sql/` | - | `b2_login.sql`, `b3_bus_pass.sql` |
| `data/` | - | generated test files and `B3-insert-times.csv` |
| `shots/` | - | output screenshots, one per task |
| `lib/` | - | the database config and the Assignment 2 benchmark harness |

Part A (design, record layouts and SIMATS-standard pseudocode) is in
`REPORT.md`; it is design work, not code, as the brief requires.

## Running

```bash
mysql < sql/b2_login.sql
mysql < sql/b3_bus_pass.sql
javac -d lib/out lib/bench/Bench.java

javac -d Task-B1 Task-B1/*.java && (cd Task-B1 && java -cp . PassStoreDemo)
(cd Task-B1 && java -Xmx1g -cp .:../lib/out IoBenchmark)
javac -cp /usr/share/java/mariadb-java-client.jar -d Task-B2 Task-B2/*.java
(cd Task-B2 && java -cp .:/usr/share/java/mariadb-java-client.jar LoginDemo)
javac -cp /usr/share/java/mariadb-java-client.jar -d Task-B3 Task-B3/*.java
(cd Task-B3 && java -cp .:/usr/share/java/mariadb-java-client.jar LoaderBenchmark)
javac -cp /usr/share/java/mariadb-java-client.jar -d Task-B4 Task-B4/*.java
(cd Task-B4 && java -cp .:/usr/share/java/mariadb-java-client.jar PassMigration)
```

`Db.java` holds the connection details for the local test database.

## Four findings worth reading first

1. **B1 / A2** - reading a 50 MB file one byte at a time makes **52,428,830
   `read` calls**; an 8 kB buffer makes **6,402**. That is the 20x speed-up,
   and it is a story about system calls rather than about speed.

2. **B2** - the unsafe login was breached by **5 of 7 crafted inputs**,
   including logging in as ADMIN with no password at all. The
   `PreparedStatement` version rejected every one.

3. **B3** - `PreparedStatement` used row by row is **not** faster than
   `Statement`; across two runs their order swapped. What is 7.6x faster at
   100,000 rows is the **batch**. The reason to use `PreparedStatement` is
   finding 2, not speed.

4. **B3** - a load that fails halfway leaves **499 orphan rows** under
   auto-commit and **zero** inside one transaction - and the transaction is
   also **6.6x faster**.

## Requirements

JDK 21 or later, and MariaDB or MySQL with the schema scripts in `sql/`.
