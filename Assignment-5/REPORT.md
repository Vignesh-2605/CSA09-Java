# Programming in Java (CSA09) - Assignment 5 (CO5)

**Data Persistence Engineering - Files and Databases**

| | |
|---|---|
| Name | Vignesh S |
| Register Number | 192325002 |
| Course | CSA09 - Programming in Java |
| Assignment | 5 (CO5) - Individual |
| Total Marks | 50 (Part A 25, Part B 25) |
| JDK | OpenJDK 21.0.12.1 |
| Database | MariaDB 10.11.14 (MySQL-compatible), a local test instance |
| JDBC driver | MariaDB Connector/J 2.7.6 |
| Repository | `CSA09-Java/Assignment-5` |

> **On the database.** The brief for Task B2 requires a local test database
> created only for this assignment. `csa09_test` is exactly that, served by
> MariaDB - the MySQL-compatible server available in this environment - and
> reached through a dedicated account with rights on that one schema. Every
> statement used here is standard MySQL and runs unchanged on MySQL itself.
> `Db.java` holds the connection details so that no task hard-codes them.

> **On the "Complete the Code" skeletons.** The deck supplied to me is the
> summary version and does not carry the source skeletons for `PassStore` or
> `PassLoader`. Both have been reconstructed from the task descriptions, with
> the TODO markers placed exactly where the task says they are. For
> `PassStore.find()` the task states the expected output precisely - the
> P1003 record followed by `null` for the damaged P1002 line - and the
> program reproduces it.

> **On pseudocode.** Part A is written in a consistent
> `ALGORITHM / INPUT / OUTPUT / BEGIN ... END` form with numbered steps,
> uppercase keywords and no Java syntax, as the SIMATS standard for
> algorithmic pseudocode requires. Java code appears only in Part B.

---

# PART A - PERSISTENCE DESIGN AND PSEUDOCODE

# Task A1 - Design a File Format

About 8,000 bus-pass records a year. Each holds pass ID, register number,
name, route number, boarding stop, validity date and amount paid.

## Layout 1 - delimited text

```
P1001|192325001|Arun K|R12|Poonamallee|2027-03-31|450000
```

| Field | Position | Type | Rule |
|---|---|---|---|
| passId | 1 | text | `P` + 4-7 digits, unique |
| registerNo | 2 | text | exactly 9 digits |
| name | 3 | text | letters, spaces and dots |
| routeNo | 4 | text | `R` + digits |
| boardingStop | 5 | text | free text, no `\|` |
| validUntil | 6 | text | ISO-8601 `YYYY-MM-DD` |
| amountPaise | 7 | integer | **paise, not rupees** |

Four decisions worth defending:

- **Pipe, not comma.** A name or a stop may contain a comma; neither contains
  a pipe in practice. This narrows the problem but does not remove it, and
  Task B2 records the residual weakness honestly.
- **One record per line.** The newline is the record boundary, which is what
  makes the format self-synchronising - see "damaged line" below.
- **Amount in paise as an integer.** `450000` means Rs 4,500.00 exactly. A
  decimal in a text file invites `Double.parseDouble`, and Assignment 4 Task 1
  measured what that does to money.
- **ISO-8601 dates.** Sorts correctly as text, and is unambiguous;
  `31-03-2027` and `03-31-2027` are not.

**Size:** about 57 bytes per record, so 8,000 records is roughly 456 kB a
year - small enough that the whole year's file fits in the page cache.

## Layout 2 - binary with `DataOutputStream`

Written in a fixed field order; strings via `writeUTF`, which prefixes each
with a two-byte length.

| Order | Call | Bytes |
|---|---|---|
| 1 | `writeUTF(passId)` | 2 + length |
| 2 | `writeUTF(registerNo)` | 2 + 9 |
| 3 | `writeUTF(name)` | 2 + length |
| 4 | `writeUTF(routeNo)` | 2 + length |
| 5 | `writeUTF(boardingStop)` | 2 + length |
| 6 | `writeLong(validUntil.toEpochDay())` | 8 |
| 7 | `writeLong(amountPaise)` | 8 |

Roughly 60 bytes, so no smaller than the text file here - the gain is not
size but **type safety**: a `long` is written and read as a `long`, with no
parsing and no chance of `"FIVELAKH"` arriving where a number belongs.

The cost is severe and is the reason Part B implements the text format: the
binary file has **no record boundary**. Every record must be read in order
from the start, and one short write misaligns everything after it.

## Pseudocode

```
ALGORITHM AddPass
INPUT    pass (passId, registerNo, name, routeNo, boardingStop, validUntil, amountPaise)
OUTPUT   the pass appended to the file
BEGIN
 1. VALIDATE pass
 2. IF validation fails THEN REPORT the first failure; RETURN
 3. OPEN the pass file in APPEND mode
 4. IF the file does not exist THEN CREATE it
 5. line <- JOIN the seven fields WITH '|'
 6. WRITE line, THEN a newline
 7. FLUSH and CLOSE the file            // in a finally, or try-with-resources
END
```

```
ALGORITHM FindPassById
INPUT    passId
OUTPUT   the matching pass, or NULL
BEGIN
 1. IF the file does not exist THEN RAISE FileMissing
 2. OPEN the file for BUFFERED reading
 3. lineNo <- 0
 4. WHILE there is another line DO
 5.      lineNo <- lineNo + 1
 6.      IF the line is blank THEN CONTINUE
 7.      fields <- SPLIT the line ON '|'
 8.      IF COUNT(fields) <> 7 THEN
 9.           RECORD "line lineNo: expected 7 fields"
10.           CONTINUE                   // damaged, but not fatal
11.      record <- PARSE fields
12.      IF parsing fails THEN RECORD the reason; CONTINUE
13.      IF record.passId = passId THEN
14.           CLOSE the file
15.           RETURN record              // stop at the first match
16. CLOSE the file
17. RETURN NULL
END
```

```
ALGORITHM ListPassesOnRoute
INPUT    routeNo
OUTPUT   every pass on that route
BEGIN
 1. result <- EMPTY LIST
 2. OPEN the file for BUFFERED reading
 3. WHILE there is another line DO
 4.      record <- PARSE the line
 5.      IF parsing fails THEN RECORD the reason; CONTINUE
 6.      IF record.routeNo = routeNo THEN APPEND record TO result
 7. CLOSE the file
 8. RETURN result                        // the whole file must be read: no early exit
END
```

`FindPassById` can stop early; `ListPassesOnRoute` cannot, because a match may
appear on the last line. Both are O(n), but their *average* cost differs by a
factor of two.

## Damaged data, a missing file, and an incomplete write

| Situation | How it is detected | What the program does |
|---|---|---|
| line has the wrong field count | `SPLIT` returns other than 7 | skip the line, record it, carry on |
| amount is not a number | parse fails | skip, record, carry on |
| date is malformed | parse fails | skip, record, carry on |
| blank line | the line is empty | skip silently - not a record |
| **the file is missing** | existence check before opening | **raise**, do not create an empty file |
| **incomplete write after a power failure** | the last line has no newline, so it parses short | the last record is lost; every earlier one survives |

The missing file is deliberately treated differently from a damaged line. A
damaged line means *one record* is unreadable; a missing file means the
question cannot be answered at all, and silently returning "not found" would
report an absent file as an absent pass.

The incomplete write is where the format earns its keep. A torn final line
costs exactly one record, because the newline before it is intact and every
earlier record is still delimited. Writing with append mode and flushing per
record bounds the loss to the record in flight.

## Alternative: fixed-length records in `RandomAccessFile`

| | delimited text | fixed-length `RandomAccessFile` |
|---|---|---|
| find the k-th record | read k lines, O(k) | `seek(k * RECORD_SIZE)`, **O(1)** |
| find by pass ID | O(n) scan | O(n) scan, or O(log n) if sorted and binary-searched |
| update one record in place | rewrite the whole file | `seek` then write - **O(1)** |
| delete a record | rewrite the whole file | mark a tombstone byte, O(1) |
| a name longer than the field | no limit | **truncated or rejected** |
| a damaged record | contained to one line | **misaligns every following record** |
| readable with `cat` | yes | no |
| space | ~57 bytes, variable | fixed, padded - wasted on short names |

The deciding property is the last-but-two row. A fixed-length file has no
self-synchronising boundary: one short write and every subsequent `seek`
lands mid-record. The format that is *faster* is also the one that fails
*worse*.

For 8,000 records a year, read far more often than updated, delimited text is
right. If the office needed to update records in place at scale - marking
passes used on every boarding, say - `RandomAccessFile` or a database would
become necessary, and Task B4 is where that line is crossed.

---

# Task A2 - Buffered I/O

A program reads a 50 MB attendance log **one byte at a time** to count lines.

## The prediction

A 50 MB file is 52,428,800 bytes. `InputStream.read()` returns one byte per
call, so the loop makes **52,428,801 calls** - one per byte plus the final
`-1`. Each is a method call, and each unbuffered call reaches the operating
system as a `read` system call: a transition out of the JVM into the kernel,
which costs far more than the byte it returns.

## Measured

| Size | Method | Time | `read()` calls | Lines |
|---|---|---|---|---|
| 1 MB | unbuffered, 1 byte | 479 ms | **1,048,618** | 22,311 |
| | `BufferedInputStream` | 23 ms | **130** | 22,311 |
| | `Files.readAllLines` | 19 ms | n/a | 22,311 |
| 10 MB | unbuffered, 1 byte | 3,459 ms | **10,485,795** | 223,102 |
| | `BufferedInputStream` | 186 ms | **1,282** | 223,102 |
| | `Files.readAllLines` | 23 ms | n/a | 223,102 |
| 50 MB | unbuffered, 1 byte | **17,614 ms** | **52,428,830** | 1,115,507 |
| | `BufferedInputStream` | **882 ms** | **6,402** | 1,115,507 |
| | `Files.readAllLines` | 196 ms | n/a | 1,115,507 |

The prediction holds exactly: 52,428,830 calls against a predicted 52,428,801.
Buffering cuts that to 6,402 - a reduction of about **8,190x** - and the time
from 17.6 seconds to 0.88 seconds, about **20x**.

## The redesign

```
ALGORITHM CountLinesBuffered
INPUT    path to the log file
OUTPUT   the number of lines
BEGIN
 1. OPEN the file wrapped in a BUFFERED stream of 8192 bytes
 2. count <- 0
 3. WHILE a byte can be read DO
 4.      IF the byte is NEWLINE THEN count <- count + 1
 5. CLOSE the stream                     // in a finally, or try-with-resources
 6. RETURN count
END
```

The loop is unchanged. Only the wrapper differs, and that is the point: the
buffer changes *how many times the operating system is asked*, not what the
program does.

## How buffering reduces calls to the operating system

`BufferedInputStream` keeps an 8 kB array. The first `read()` fills the whole
array with one system call; the next 8,191 calls are served from memory with
no kernel involvement. So the number of system calls falls from *n* to
*n / 8192* - which is 52,428,800 / 8192 = 6,400, against the 6,402 measured.

**Buffering does not make reading faster. It makes the program ask less
often.**

## When a buffer gives little or no benefit

1. **When the reads are already large.** A program calling
   `read(byte[8192])` directly is already transferring a block per call; a
   buffer adds a copy and buys nothing.
2. **When the file is tiny.** A 200-byte file is one system call either way.
3. **When another buffer is already present.** `Files.newBufferedReader`
   wrapped in a `BufferedReader` buffers twice - one of them is wasted.
4. **When the pattern is random, not sequential.** A `RandomAccessFile`
   seeking to scattered offsets discards the buffer on every seek, so the
   read-ahead is thrown away unused.

Case 3 is the common mistake, and case 4 is the interesting one: buffering is
a bet on sequential access, and a random-access pattern loses that bet.

## Alternative: `Files.readAllLines()`

| | buffered streaming | `Files.readAllLines()` |
|---|---|---|
| time, 50 MB | 882 ms | **196 ms** |
| **memory, 50 MB** | **512 kB** (one 8 kB buffer) | **114,009 kB** |
| memory as the file grows | constant | O(file size) |
| 500 MB file | works | `OutOfMemoryError` on a default heap |
| code | a loop | one line |
| random access to line k | must re-read | `list.get(k)` |

`readAllLines` is **4.5x faster and 222x more memory-hungry**. It is faster
because it reads in large blocks and decodes in bulk; the memory is the price
of holding 1,115,507 `String` objects, each with its own header and character
array - 114 MB of heap for a 50 MB file, because Java strings carry overhead
the file does not.

For *counting lines*, streaming is correct: the task needs one number, so
holding a million strings to produce it is pure waste. `readAllLines` is right
when the lines are genuinely all needed at once and the file is known to be
small.

---

# Task A3 - Data-Access Design Pattern

`StudentDB`, `CourseDB` and `MarksDB` repeat similar JDBC operations.

## What is common, and what differs

| Step | Common? | What differs |
|---|---|---|
| obtain a `Connection` | **identical** | nothing |
| prepare a statement | same shape | **the SQL text** |
| bind parameters | same shape | **which fields, in what order** |
| execute | identical | nothing |
| walk the `ResultSet` | same shape | **which columns become which fields** |
| close everything | **identical** | nothing |
| wrap `SQLException` | **identical** | nothing |

Five of seven steps are identical; the three that differ are all the same
*kind* of difference - the SQL text, the binding and the row mapping. That is
precisely what a generic DAO parameterises.

## The generic interface

```
INTERFACE Dao<T, K>
    insert(entity: T)            RETURNS K        // the generated key
    findById(key: K)             RETURNS T or NULL
    findAll()                    RETURNS LIST OF T
    update(entity: T)            RETURNS BOOLEAN
    delete(key: K)               RETURNS BOOLEAN
END INTERFACE
```

`T` is the entity type, `K` the primary-key type - so
`StudentDao IMPLEMENTS Dao<Student, String>` keys by register number, while a
`MarksDao IMPLEMENTS Dao<Mark, Long>` keys by a generated id. The key type is
a separate parameter precisely because it is not always the same.

```
ALGORITHM StudentDao.FindById
INPUT    registerNo
OUTPUT   the Student, or NULL
BEGIN
 1. sql <- "SELECT register_no, name, department, year_of_study, email
             FROM student WHERE register_no = ?"
 2. OPEN a connection
 3. PREPARE sql
 4. BIND registerNo TO parameter 1
 5. EXECUTE the query
 6. IF no row is returned THEN CLOSE everything; RETURN NULL
 7. student <- MAP the row TO a Student
 8. CLOSE the result set, the statement and the connection
 9. RETURN student
END
```

Steps 2, 3, 5, 8 are identical in every DAO; only 1, 4 and 7 are specific to
`Student`. A base class can own the common steps and leave three small
methods abstract - `sqlFor`, `bind`, `map`.

## Object-to-table mapping

| `Student` field | Java type | `student` column | SQL type | Notes |
|---|---|---|---|---|
| `registerNo` | `String` | `register_no` | `VARCHAR(16)` | **PRIMARY KEY** |
| `name` | `String` | `name` | `VARCHAR(80)` | `NOT NULL` |
| `department` | `String` | `department` | `VARCHAR(40)` | `NOT NULL` |
| `yearOfStudy` | `int` | `year_of_study` | `TINYINT` | 1 to 5 |
| `email` | `String` | `email` | `VARCHAR(120)` | `UNIQUE` |
| `feesPaidPaise` | `long` | `fees_paid_paise` | `BIGINT` | money as an integer |

The primary key is `register_no` - a **natural key**, unique and already
meaningful. The alternative is a surrogate `student_id`; the natural key is
chosen here because it is externally defined, never reassigned, and already
printed on every document the student carries.

## Alternative: a generic DAO, or SQL inside service and GUI classes

| | generic DAO | SQL inside service or GUI |
|---|---|---|
| where SQL lives | one class per table | scattered across the codebase |
| changing a column name | one file | find every string that mentions it |
| testing the rules | swap in a fake DAO | needs a live database |
| swapping the database | one layer | everywhere |
| code per new table | one small class | duplicated connect/close everywhere |
| resource leaks | closed in one place | **one forgotten `finally` leaks forever** |
| injection risk | one place to audit | every call site is a risk |
| layers crossed | UI to service to DAO | UI talks to the database directly |

Assignment 4 is the demonstration, not a hypothetical.
`BadFeePaymentWindow.actionPerformed` opened a connection, concatenated SQL,
and closed nothing - three of the eleven faults diagnosed there exist *only*
because SQL was written inside a GUI event handler. The redesign moved it into
`FeeRepository` and all three disappeared at once.

The cost of the DAO is indirection and some generality that is never used.
For three tables that is a fair price; for one table it would not be.

---

# Task A4 - Serialization Design

A `Hostel` contains `Room` objects; each `Room` contains `Resident` objects. A
`Resident` has a name, register number, password and a live database
`Connection`.

## Which fields must be `transient`

| Field | `transient`? | Why |
|---|---|---|
| `Connection connection` | **yes, mandatory** | a `Connection` is a live socket, file handles and server-side state. It is not `Serializable`, so without `transient` the write fails with `NotSerializableException`. Even if it could be written, a socket restored tomorrow would point at nothing |
| `password` | **yes** | a serialized file is plain bytes; `strings` on the file shows the password. Secrets must not be written to a file that is not itself protected |
| `name`, `registerNo` | no | they are the data |
| a cached `age` derived from a date | yes | derived values go stale; recompute on read |
| an open `Scanner` or stream | yes | same reason as the connection |
| a `Logger` | yes | not serializable, and belongs to the process |

The rule: **`transient` marks what belongs to the running process rather than
to the object's identity.** A connection, a thread, a stream, an open file - a
restored object must reacquire them, not inherit them.

## What happens to the object graph when `Hostel` is serialized

Serialization is **transitive and automatic**. Writing one `Hostel` writes:

```
Hostel
  +-- name, address                       (written)
  +-- List<Room>                          (written - ArrayList is Serializable)
        +-- Room 101                      (written)
        |     +-- List<Resident>          (written)
        |           +-- Resident A        (written, minus transient fields)
        |           +-- Resident B        (written, minus transient fields)
        +-- Room 102 ...
```

Three consequences that surprise people:

1. **Every reachable object is written**, whether intended or not. One stray
   reference to a large cache drags the whole cache into the file.
2. **Shared references stay shared.** If two `Room` objects point at the same
   `Warden`, the warden is written **once** and both references point at the
   same restored object. Serialization tracks identity, so the graph's shape
   survives - and so do cycles, which it handles without looping.
3. **Every class in the graph must be `Serializable`**, or the whole write
   fails. One non-serializable field anywhere aborts the entire `Hostel`.

## Adding a phone-number field after old files were saved

With a declared `serialVersionUID` that is **unchanged**:

- Old file read by new code: the phone field is **not present**, so it is set
  to its default - `null` for a `String`. The read succeeds.
- New file read by old code: the phone field is **ignored**. The read
  succeeds.

Both work, which is the point of keeping the UID stable for a compatible
change. The new field must therefore tolerate `null`, and code that assumes a
phone number exists will fail on every record saved before the change.

Without a declared `serialVersionUID`, the JVM computes one from the class's
name, fields, methods and interfaces. **Adding a field changes it**, so the
new class's computed UID differs from the one in the old file, and every read
fails with `InvalidClassException` - "local class incompatible". Years of
saved files become unreadable because a field was added.

**So `serialVersionUID` should always be declared explicitly**, from the first
version:

```
private static final long serialVersionUID = 1L;
```

It is a promise: "this class can still read files written by earlier versions
of itself." It should be changed only for a genuinely incompatible change -
removing a field, or changing its type - where failing loudly is better than
reading nonsense.

## Serialization, CSV, or JSON

| | Java serialization | CSV | JSON |
|---|---|---|---|
| nested objects | **native** | must be flattened | **native** |
| shared references and cycles | **preserved** | impossible | duplicated, or a manual id scheme |
| readable by a person | no | **yes** | **yes** |
| readable by other languages | **no - Java only** | yes | yes |
| survives a field being added | yes, if the UID is fixed | yes, if order is kept | **yes, naturally** |
| survives a class being renamed | **no** | yes | yes, with mapping |
| size | compact | **smallest** | largest |
| speed | fast | fast | slower - parsing |
| security | **deserialization of untrusted data executes code** | safe to parse | safe with a sane parser |

The last row is decisive for anything crossing a trust boundary. Java
deserialization reconstructs arbitrary object graphs and can invoke code
during the process, which is a long-standing source of remote-code-execution
vulnerabilities. **A serialized file should never be read from an untrusted
source.**

For a hostel register: JSON, or a database. Serialization is tempting because
it is one line to save a whole graph - and that is exactly how a format gets
chosen that no other program can read and that breaks when a class is renamed.
Task B4 reaches the same conclusion from the opposite direction.

---

# Task A5 - Correctness and Safety

## The transaction - Rs 500 from a hostel wallet to a canteen wallet

```
ALGORITHM TransferMoney
INPUT    fromWallet, toWallet, amountPaise
OUTPUT   the transfer committed, or nothing changed at all
BEGIN
 1. OPEN a connection
 2. SET autoCommit TO FALSE              // the transaction starts here
 3. TRY
 4.      balance <- SELECT balance FROM wallet
                     WHERE wallet_id = fromWallet FOR UPDATE
 5.      IF balance < amountPaise THEN
 6.           RAISE InsufficientFunds
 7.      UPDATE wallet SET balance = balance - amountPaise
              WHERE wallet_id = fromWallet
 8.      UPDATE wallet SET balance = balance + amountPaise
              WHERE wallet_id = toWallet
 9.      INSERT INTO transfer_log (fromWallet, toWallet, amountPaise, at_time)
10.      COMMIT                           // both updates become visible together
11. CATCH any error
12.      ROLLBACK                         // neither update survives
13.      RE-RAISE so the caller knows
14. FINALLY
15.      RESTORE autoCommit TO TRUE
16.      CLOSE the connection
END
```

Four details that are not decoration:

- **Step 2 before step 4.** Auto-commit must be off *before* the first
  statement, or step 7 commits on its own and step 12 has nothing to undo.
- **`FOR UPDATE` in step 4.** Without it, two concurrent transfers both read
  the same balance and both decide there is enough money. This is the
  check-then-act race of Assignment 3 Task 5, in a ledger.
- **Step 12 in a `CATCH`, step 15 in a `FINALLY`.** Rollback happens only on
  failure; restoring auto-commit happens on every path, because a pooled
  connection handed back with auto-commit off poisons the next user.
- **Step 13 re-raises.** A silent rollback tells the student their money
  vanished with no explanation.

Without the transaction, a failure between steps 7 and 8 **destroys Rs 500**:
debited from the hostel wallet, never credited to the canteen. Between 8 and 9
it would be untraceable instead.

## SQL injection in the login query

The query joins user input directly into SQL:

```
"SELECT username, role FROM app_user WHERE username = '" + user
                                   + "' AND password = '" + pass + "'"
```

With `user = admin' -- ` the database receives:

```sql
SELECT username, role FROM app_user WHERE username = 'admin' -- ' AND password = 'x'
                                                             ^^^^^^^^^^^^^^^^^^^^^^^
                                                             a comment: discarded
```

`--` begins an SQL comment, so the entire password test is discarded. The
query asks only whether a user named `admin` exists, which it does - and the
attacker is admitted **as ADMIN with no password**. Task B2 runs exactly this
input and records the result.

The error is not insufficient escaping. It is that **data was placed where the
parser expects code**, so any escaping scheme is a race between the programmer
and the attacker.

## The redesign

```
ALGORITHM SafeLogin
INPUT    username, password
OUTPUT   the user's role, or NULL
BEGIN
 1. sql <- "SELECT username, role FROM app_user
             WHERE username = ? AND password_hash = ?"
 2. PREPARE sql                           // parsed ONCE, before any value is supplied
 3. BIND username TO parameter 1
 4. BIND hash(password, salt) TO parameter 2
 5. EXECUTE the query
 6. IF a row is returned THEN RETURN its role ELSE RETURN NULL
END
```

The statement is parsed at step 2 with the `?` markers in place. The values
bound at steps 3 and 4 travel separately and **can never be parsed as SQL**.
`admin' -- ` becomes a username no row has.

Step 4 is an addition the task does not require but the design does: storing
passwords in clear means a single leaked backup exposes every account.
`sql/b2_login.sql` stores them in clear **only** because the unsafe query had
to be reproduced as described, and that file says so.

## One transaction, or two independent auto-commit updates

| | one transaction | two auto-commit updates |
|---|---|---|
| failure between the two | **nothing changed** | **money destroyed** |
| what another session sees mid-way | nothing - isolation | the debit, before the credit |
| round trips | one commit | two commits |
| speed, 100,000 operations | measured **6.6x faster** (Task B3) | baseline |
| rows left after a failure at 50% | **0** (measured) | **499** (measured) |
| complexity | must manage auto-commit | none |

Task B3 measured both columns on real data. The auto-commit import left **499
rows** behind when it failed halfway; the same import inside one transaction
left **zero**. The transaction was also **6.6x faster**, because committing
once beats committing 100,000 times.

The usual framing - safety costs speed - is simply wrong here. Both the
correctness column and the speed column favour the transaction.

---

# PART B - IMPLEMENTATION AND EXPERIMENTAL INVESTIGATION

# Task B1 - File Store

**Files:** `Task-B1/BusPass.java`, `PassStore.java`, `PassStoreDemo.java`,
`IoBenchmark.java`, `b1_demo.txt`, `b1_benchmark.txt`

## TODO 1 - completed

```java
public BusPass find(String passId) throws IOException {
    damaged.clear();
    if (!Files.exists(file)) throw new NoSuchFileException(file.toString());
    int lineNo = 0;
    try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
        String line;
        while ((line = r.readLine()) != null) {
            lineNo++;
            if (line.isBlank()) continue;
            try {
                BusPass p = BusPass.parse(line);
                if (p.passId().equals(passId)) return p;   // stop at the first match
            } catch (BusPass.DamagedRecordException e) {
                damaged.add("line " + lineNo + ": " + e.getMessage());  // skip, carry on
            }
        }
    }
    return null;
}
```

## The supplied example - output matches exactly

```
the file as written:
    P1001|192325001|Arun K|R12|Poonamallee|2027-03-31|450000
    P1002|192325002|Vignesh S|R12|Poonamallee          <- damaged, 5 fields
    P1003|192325003|Divya R|R07|Tambaram|2027-03-31|520000

store.find("P1003") -> BusPass[passId=P1003, ..., amountPaise=520000]
store.find("P1002") -> null      <- the damaged line, skipped

EXPECTED: the P1003 record, then null for the damaged P1002 line.
MATCHES EXPECTED OUTPUT: YES
damaged lines reported : [line 2: expected 7 fields, found 5]
```

## Test table

| ID | Case | Expected | Actual | Verdict |
|---|---|---|---|---|
| N1 | normal: a pass that exists | the P1001 record | P1001 Arun K | PASS |
| N2 | a pass ID that does not exist | null | null | PASS |
| N3 | a damaged line (5 fields) | null, line reported | null, 1 reported | PASS |
| N4 | a record **after** the damaged line | found, not hidden | P1003 | PASS |
| N5 | non-numeric amount | null, reported | null | PASS |
| N6 | wrong date format | null, reported | null | PASS |
| N7 | a record after **three** damaged lines | still found | P1006 | PASS |
| N8 | list passes on route R12 | 2 (P1001, P1006) | 2 found | PASS |
| N9 | the file itself is missing | `NoSuchFileException` | `NoSuchFileException` | PASS |

**9 of 9 checks passed.** The file ends with 3 usable records and 3 damaged
lines.

## Comparison with the Part A pseudocode

The implementation follows `FindPassById` step for step, with two additions
the pseudocode did not specify: the damaged lines are *collected* rather than
merely skipped, so the caller can report them; and `find` clears that list at
entry, so a second call does not inherit the first call's damage. Both are
details the pseudocode was right to leave out and the implementation was
wrong to omit at first.

## Why a damaged line does not stop the search

Because **the record boundary is the newline, and the newline survives the
damage**. `BufferedReader.readLine()` finds the next record without
understanding the broken one, so the parse failure is contained to a single
line.

Test N7 is the evidence: `P1006` is found with three damaged lines between it
and the start of the file. That property belongs to the **format**, not the
parsing code - a fixed-length binary file has no such boundary, and one short
write there misaligns every record that follows.

## Measured: line counting at 1 MB, 10 MB and 50 MB

(The full table is in Task A2 above.) At 50 MB: **17,614 ms and 52,428,830
read calls unbuffered, against 882 ms and 6,402 calls buffered** - 20x the
speed from 1/8190th the system calls.

## Measured: pass search as the file grows

| Records | First (ms) | Middle (ms) | Last (ms) | Missing (ms) |
|---|---|---|---|---|
| 1,000 | 0.06 | 3.90 | 4.80 | 2.17 |
| 10,000 | 0.03 | 6.98 | 13.77 | 15.33 |
| 50,000 | 0.02 | 24.33 | 35.74 | 31.60 |
| 100,000 | **0.02** | 20.50 | **34.57** | **47.69** |

The **First** column barely moves - `find` stops at the first match, so a hit
at the top of the file costs the same whatever the file size. The **Last** and
**Missing** columns grow with n, because both read every line. **A missing
pass is the worst case every time**, and a counter clerk typing a wrong ID
pays the full O(n).

## Alternative: scan every `find()`, or load a `HashMap` once

| q searches | Scan total | HashMap total (incl. build) | Map build | Winner |
|---|---|---|---|---|
| 1 | 10.9 ms | 117.1 ms | 117.0 ms | **scan** |
| 10 | 345.9 ms | 117.0 ms | 117.0 ms | **HashMap** |
| 100 | 2,165.3 ms | 117.1 ms | 117.0 ms | **HashMap** |
| 1,000 | **19,607.6 ms** | **117.7 ms** | 117.0 ms | **HashMap** |

### Complexity

| | scan per search | `HashMap` |
|---|---|---|
| q searches over n passes | **O(q x n)** | **O(n + q)** |
| 1,000 searches, 100,000 passes | 100,000,000 line reads | 100,000 reads + 1,000 lookups |
| memory | one 8 kB buffer | **about 39 MB** for 100,000 passes |
| freshness | always current | stale the moment another process writes |
| startup cost | none | 117 ms before the first answer |

**The crossover is between 1 and 10 searches** - measured, not estimated. One
lookup and the scan wins, because the map's 117 ms build dwarfs a single
10.9 ms scan. Ten lookups and the map has already repaid itself, and by 1,000
it is **167x faster**.

The memory column is why this is a real decision rather than an obvious one:
39 MB per process, and at 12 campuses that is 12 copies, each stale the moment
another campus issues a pass. That is the argument Task B4 finishes.

---

# Task B2 - Adversarial Input

**Files:** `sql/b2_login.sql`, `Task-B2/LoginDemo.java`, `b2_report.txt`

## The same eight inputs through both versions

| ID | Intent | Unsafe (`Statement`) | Safe (`PreparedStatement`) | Verdict |
|---|---|---|---|---|
| L1 | the genuine login | vignesh/STUDENT | vignesh/STUDENT | both correct |
| L2 | right user, wrong password | rejected | rejected | both rejected |
| L3 | a user that does not exist | rejected | rejected | both rejected |
| L4 | comment out the password test | **vignesh/STUDENT** | rejected | **UNSAFE BREACHED** |
| L5 | always-true in both fields | **vignesh/STUDENT** | rejected | **UNSAFE BREACHED** |
| L6 | log in as ADMIN with no password | **admin/ADMIN** | rejected | **UNSAFE BREACHED** |
| L7 | take the first row, whoever it is | **vignesh/STUDENT** | rejected | **UNSAFE BREACHED** |
| L8 | read every row through a UNION | **vignesh/STUDENT** | rejected | **UNSAFE BREACHED** |

**The unsafe version was breached by 5 of 7 crafted inputs. The
`PreparedStatement` version rejected every one.**

L6 is the one to dwell on: the input `admin' -- ` with password `x` returns
**admin/ADMIN**. An attacker with no password obtains the administrator's
role.

## Why

```
L6  input: username=[admin' -- ] password=[x]
    unsafe -> SELECT username, role FROM app_user WHERE username = 'admin' -- ' AND password = 'x'
    safe   -> SELECT ... WHERE username = ? AND password = ?   with ? bound to the text above
```

`PreparedStatement` does not *fail to be fooled by a cleverer string* - it
**cannot be fooled by any string**. The statement is parsed once, with the `?`
markers in place, before any value exists. The values then travel separately
and are bound to those markers.

## Damaged bus-pass data - what each input reveals

| Adversarial input | What the store does | What it reveals |
|---|---|---|
| wrong field count (5 of 7) | skipped, line reported | the format is self-synchronising at the newline |
| non-numeric amount `FIVELAKH` | skipped, line reported | parsing is per-field, so one bad field is contained |
| empty line | skipped silently | blank lines are not records and must not be counted |
| date written `31-03-2027` | skipped, line reported | a plausible-looking value can still be invalid |
| **a pipe inside a name** | **mis-parsed as 8 fields** | **the delimiter is the weakness of a delimited format** |
| file truncated mid-line | last line skipped | an incomplete write loses one record, not the file |
| file absent entirely | `NoSuchFileException` | a missing file is a different fault from a damaged one |

The fifth row is the honest weakness of the Task A1 design. A name containing
the delimiter breaks the record **silently**, because it still parses - into
the wrong number of fields. Guarding it needs either an escape rule or a
delimiter that cannot occur in the data. This is the file-format equivalent of
SQL injection: data placed where the parser expects structure.

---

# Task B3 - JDBC Performance Comparison

**Files:** `sql/b3_bus_pass.sql`, `Task-B3/PassLoader.java` (TODO 1-3),
`LoaderBenchmark.java`, `b3_report.txt`, `data/B3-insert-times.csv`

## TODO 1, 2 and 3 - completed

```java
/* TODO 1 */ st.executeUpdate("INSERT INTO bus_pass VALUES('" + ... + ")");
/* TODO 2 */ bind(ps, p); ps.executeUpdate();
/* TODO 3 */ c.setAutoCommit(false);
             bind(ps, p); ps.addBatch();
             if (++n % batchSize == 0) ps.executeBatch();
             ps.executeBatch(); c.commit();
```

## Ten records, all three ways, verified by `SELECT`

| Approach | Time | Rows | Verified |
|---|---|---|---|
| `Statement` | 21.85 ms | 10 | 10 rows present |
| `PreparedStatement` | 46.90 ms | 10 | 10 rows present |
| `PreparedStatement` batch | 14.62 ms | 10 | 10 rows present |

## Column types, from `ResultSetMetaData`

| # | Column | SQL type | Size | Nullable | Java class |
|---|---|---|---|---|---|
| 1 | pass_id | VARCHAR | 16 | NO | java.lang.String |
| 2 | register_no | VARCHAR | 16 | NO | java.lang.String |
| 3 | name | VARCHAR | 80 | NO | java.lang.String |
| 4 | route_no | VARCHAR | 8 | NO | java.lang.String |
| 5 | boarding_stop | VARCHAR | 60 | NO | java.lang.String |
| 6 | valid_until | DATE | 10 | NO | java.sql.Date |
| 7 | amount_paise | BIGINT | 20 | NO | java.lang.Long |

## Insertion time at 1,000, 10,000 and 100,000 records

| Records | Approach | Time | Rows/second | Relative |
|---|---|---|---|---|
| 1,000 | **PreparedStatement batch (1000)** | **135.0 ms** | 7,406 | baseline |
| 1,000 | PreparedStatement batch, autocommit | 196.6 ms | 5,086 | 1.5x slower |
| 1,000 | PreparedStatement, row by row | 206.5 ms | 4,844 | 1.5x slower |
| 1,000 | Statement, row by row | 195.8 ms | 5,108 | 1.4x slower |
| 10,000 | **PreparedStatement batch (1000)** | **385.5 ms** | 25,943 | baseline |
| 10,000 | PreparedStatement batch, autocommit | 1,720.5 ms | 5,812 | 4.5x slower |
| 10,000 | PreparedStatement, row by row | 1,734.6 ms | 5,765 | 4.5x slower |
| 10,000 | Statement, row by row | 1,806.8 ms | 5,535 | 4.7x slower |
| 100,000 | **PreparedStatement batch (1000)** | **2,710.8 ms** | **36,889** | baseline |
| 100,000 | PreparedStatement batch, autocommit | 16,851.9 ms | 5,934 | 6.2x slower |
| 100,000 | PreparedStatement, row by row | 20,705.5 ms | 4,689 | 7.6x slower |
| 100,000 | Statement, row by row | *not run* | | see below |

`Statement` was not run at 100,000 rows: at 10,000 it already takes long
enough to extrapolate honestly, and the shape is not in doubt.

### A result that does not match the usual story

**The three row-by-row rows are indistinguishable from each other.** At 10,000
records they sit within a few per cent, and across two executions of this
program their order changed - `Statement` came out ahead of
`PreparedStatement` in one run and behind it in the next.

The textbook ordering assumes the reparse dominates. Here it plainly does not,
because all three pay **one network round trip per row**, and that cost swamps
the parse.

Preparing a statement earns its keep when the statement is **reused**, which
is what the batch does - and the batch is the only row in the table that pulls
away as the data grows, from 1.5x at 1,000 records to **7.6x at 100,000**.

`PreparedStatement` is still the one to use, but on this evidence the reason
is **SQL injection (Task B2), not speed**.

## A failure halfway through

Pass 500 of 1,000 carries `amount_paise = -1`, which violates the
`chk_amount` CHECK constraint. The same 1,000 records are loaded twice.

| Mode | What happened | Rows left behind | Verdict |
|---|---|---|---|
| auto-commit, batches of 100 | `BatchUpdateException` | **499** | **partial data left in the table** |
| one transaction, batches of 100 | `BatchUpdateException` | **0** | no rows remain - correct |

With auto-commit, 499 rows were already committed before the bad record was
reached, and they stay. The import is now half-done, and re-running it would
hit duplicate primary keys - so the failure is not merely untidy, it blocks
the retry.

Inside one transaction, nothing is visible until `commit()`, and the exception
means `commit()` is never reached.

## Recommendations

| Situation | Recommendation | Why |
|---|---|---|
| **nightly import of 100,000 passes** | `PreparedStatement` batches of 1,000 in **one transaction** | 2.7 s against 20.7 s, and a failure leaves no partial data |
| **issuing one pass at the counter** | a single `PreparedStatement`, auto-commit | one row, so batching has nothing to batch; the clerk needs it committed and visible at once |

The second is worth stating plainly because the measurement might suggest
otherwise: batching one row is slower than not batching it, and a counter
transaction that is not committed immediately is a pass the student cannot be
given.

## Alternative: batches, multi-row `INSERT`, or `LOAD DATA`

| | `PreparedStatement` batch | multi-row `INSERT` | `LOAD DATA INFILE` |
|---|---|---|---|
| 100,000 rows | **2.7 s** (measured) | similar, often slightly faster | fastest - typically several times faster |
| SQL injection | **safe - values bound** | safe if built with placeholders | n/a - reads a file |
| per-row validation in Java | **yes** | yes | **no** |
| per-row error reporting | which row, from the update counts | harder | a log, after the fact |
| statement length | constant | grows with the batch - can exceed `max_allowed_packet` | n/a |
| the file must be reachable by | the application | the application | **the database server** |
| portability | any JDBC database | most | **MySQL-specific** |

`LOAD DATA` is the fastest and the least controllable: the server reads the
file directly, so no Java validation runs and a bad row is a server-side
warning rather than a Java exception. Task B4 needs per-record validation with
a reason for each rejection, so the batch is the right tool there despite not
being the fastest available.

---

# Task B4 - Final Engineering Decision

**Files:** `Task-B4/PassMigration.java`, `b4_report.txt`

## What `DatabaseMetaData` reports

| | |
|---|---|
| database product | MariaDB |
| database version | 10.11.14-MariaDB-0ubuntu0.24.04.1 |
| JDBC driver | MariaDB Connector/J |
| driver version | 2.7.6 |
| JDBC specification | 4.2 |
| supports transactions | true |
| supports batch updates | true |
| default isolation | REPEATABLE READ |

Tables found in `csa09_test`: `app_user`, `bus_pass`, `campus_event`,
`fee_payment`, `registration` - the last three built by Assignment 4,
discovered here without being told they exist. That is the point of
`DatabaseMetaData`: a migration tool can inspect an unfamiliar database rather
than assume its shape.

## The migration

| | |
|---|---|
| source file | `migration-source.txt`, 3,106,771 bytes |
| lines read | 50,008 |
| **records migrated** | **50,000** |
| **records rejected** | **8** |
| time | 1,864 ms |
| rows in `bus_pass` afterwards | 50,000 |
| migrated + rejected = lines read | **YES - every line accounted for** |

### Every record that failed validation

| Line | Reason | The line |
|---|---|---|
| 1,001 | register number is not 9 digits | `P9000001\|1923\|Short Reg\|...` |
| 2,002 | amount is not positive | `P9000002\|...\|-5` |
| 3,003 | amount above the Rs 50,000 ceiling | `P9000003\|...\|9900000` |
| 4,004 | expected 7 fields, found 5 | `P9000004\|192399997\|Five Fields\|R1\|Stop 1` |
| 5,005 | blank line | |
| 6,006 | validity date before 2020 | `P9000006\|...\|2019-01-01\|450000` |
| 7,007 | duplicate pass ID in the file | `P0000001\|...` |
| 8,008 | bad date: 31-03-2027 | `P9000008\|...\|31-03-2027\|450000` |

Each injected fault was caught individually and reported with its line number
and reason. The accounting identity - migrated plus rejected equals lines read
- is checked by the program, because a migration that silently drops records
is worse than one that fails.

Two of the eight are **not** format faults: the Rs 50,000 ceiling and the
pre-2020 validity date are well-formed records that break a *business* rule. A
migration has to apply both kinds of check, and the file format can only
enforce the first.

## The expanded system: 12 campuses, 100x the passes

About 800,000 passes a year, 12 campuses issuing at once.

| Requirement | Text file | Serialization | Database |
|---|---|---|---|
| find one pass by ID | O(n) scan - **35 ms at 100,000** | load the whole graph first | O(log n) on the primary key |
| **12 counters writing at once** | **corruption - no locking** | **whole-file rewrite** | row locks and transactions |
| a failed half-write | a torn last line | the whole file is unreadable | **rolled back - measured** |
| search by route | O(n) | O(n) in memory | an index, O(log n) |
| adding a phone column | every reader must change | **breaks old files (A4)** | `ALTER TABLE` |
| who changed what | nothing | nothing | timestamps, audit tables |
| memory to serve a query | one 8 kB buffer | the entire object graph | one page |
| concurrent read while writing | may read a half-written line | not possible | isolation levels |

## Decision: a database

**Searching.** B1 measured 35 ms per lookup over 100,000 records, and that is
O(n) - at 800,000 it is roughly 280 ms for one counter query. The `HashMap`
alternative fixes the lookup but costs about 39 MB per process and must be
rebuilt in each one; 12 campuses means 12 copies, each stale the moment
another campus issues a pass.

**Concurrent counters.** This is the requirement that settles it. A text file
has no way for 12 processes to append safely, and a serialized file is
rewritten whole, so two campuses saving at once lose one of the two.

**Safety.** B3 measured 499 orphan rows left by an auto-commit import that
failed halfway, and zero left by the same import inside one transaction.
Neither file format offers rollback at all.

**Loading speed is not the reason.** B3 measured 100,000 rows inserted in
about 2.7 seconds by batch - fast enough, but a text file would append them
faster. Writing speed is the one contest the files win, and it is the least
important requirement on the list.

## One shared database, or one serialized file per campus?

| | 12 serialized files | one shared database |
|---|---|---|
| **a student at two campuses** | **invisible - no file knows** | one `UNIQUE` constraint |
| total passes issued | open and merge 12 files | one `COUNT` query |
| one campus offline | its file is simply missing | its rows are still there |
| schema change | 12 files to migrate (A4) | one `ALTER TABLE` |
| a campus-local query | fast - its own file only | indexed, still fast |
| **failure blast radius** | **one campus** | **all 12 - the honest cost** |
| backup | 12 files to keep in step | one consistent backup |

**One shared database, with a `campus_id` column.** The deciding row is the
first: a transport office that cannot tell whether a student already holds a
pass at another campus cannot enforce its own rule, and no arrangement of 12
separate files fixes that without rebuilding what a database already is.

The honest cost is the blast radius - one shared database is one thing to
lose. That is answered with backups and replication, not by splitting the data
into 12 files that cannot answer the first question.

---

# What this assignment demonstrated

Four measurements changed what the design should be, rather than confirming
what was already assumed:

1. **Buffering is about system calls, not speed.** 52,428,830 `read` calls
   became 6,402 - and the 20x speed-up followed from that, not the other way
   round.

2. **The `HashMap` crossover is at fewer than ten searches.** The index costs
   117 ms to build and 39 MB to hold, so one lookup is cheaper by scanning and
   a thousand are 167x cheaper by indexing. Neither is right in general.

3. **`PreparedStatement` is not faster than `Statement` row by row.** Across
   two runs their order swapped. What is actually 7.6x faster is the *batch*,
   because the round trip - not the parse - is the cost. The reason to prefer
   `PreparedStatement` is Task B2's five breaches, not performance.

4. **The transaction was both safer and faster.** 499 orphan rows against
   zero, and 6.6x the throughput. The usual trade-off between safety and speed
   did not appear.
