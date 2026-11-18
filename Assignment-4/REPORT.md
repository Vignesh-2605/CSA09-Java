# Programming in Java (CSA09) - Assignment 4 (CO4)

**Event-Driven GUI Design**

| | |
|---|---|
| Name | Vignesh S |
| Register Number | 192325002 |
| Course | CSA09 - Programming in Java |
| Assignment | 4 (CO4) - Individual |
| Total Marks | 50 |
| JDK | OpenJDK 21.0.12.1 |
| Toolkit | Swing / AWT |
| Database | MariaDB 10.11.14 (MySQL-compatible), driver 2.7.6 |
| Repository | `CSA09-Java/Assignment-4` |

> **On the screenshots.** Every image in `shots/` is a photograph of a running
> window, captured from the X server with `java.awt.Robot.createScreenCapture`
> while a harness drove the real components. None is a mock-up. The windows
> run under Xvfb, a headless X server; where that changes what can be
> demonstrated, it is stated at the point it matters (Task 4).

> **On the "Complete the Code" skeletons.** The deck supplied to me is the
> summary version and does not contain the source skeletons referred to in
> Tasks 4 and 5. Those programs have been reconstructed from the task
> descriptions, with the TODO markers placed exactly where the task says they
> are, and both the faulty and the completed versions are committed so the
> difference can be read directly.

---

## How to run everything

```bash
javac -d lib/out lib/Shot.java lib/Db.java
export DISPLAY=:99     # any X display; Xvfb :99 -screen 0 1400x900x24 &

# Task 1  fee payment: the fault, the redesign, and the headless test
javac -d Task01 Task01/BadFeePaymentWindow.java
javac -cp /usr/share/java/mariadb-java-client.jar -d Task01/redesign Task01/redesign/*.java
java -Djava.awt.headless=true -cp Task01/redesign FeeServiceTest

# Task 2  appointment desk and its recorded event trace
javac -d Task02 Task02/*.java && (cd Task02 && java -cp . TraceHarness)

# Task 3  library search at three window sizes
javac -d Task03 Task03/*.java && (cd Task03 && java -cp . ResizeHarness)

# Task 4  drawing-pad forensics
javac -d Task04 Task04/*.java && (cd Task04 && java -cp . ForensicsHarness)

# Task 5  four event styles
javac -d Task05 Task05/*.java && (cd Task05 && java -cp . StylesHarness)

# Task 6  campus event registration  (run sql/task06_schema.sql first)
mysql < sql/task06_schema.sql
javac -cp lib/out:/usr/share/java/mariadb-java-client.jar -d Task06 Task06/*.java
(cd Task06 && java -cp .:/usr/share/java/mariadb-java-client.jar AppHarness)
(cd Task06 && java -cp .:../lib/out:/usr/share/java/mariadb-java-client.jar PaginationBenchmark)
```

---

# Task 1 - GUI Design Review

**Files:** `Task01/BadFeePaymentWindow.java` (the fault, kept on purpose),
`Task01/redesign/` (the corrected design), `Task01/feeservice_test.txt`

## The diagnosis - eleven faults, not five

| # | Fault | Where | Impact on the user or the application |
|---|---|---|---|
| 1 | one listener for every button | `addActionListener(this)` x3 | the handler must ask *which* button, so every new button grows one method |
| 2 | `cmd == "Pay"` | top of `actionPerformed` | compares references. Works only because button labels are interned literals; silently stops working the moment a label is built at run time |
| 3 | all logic inside the handler | the whole method | no rule can be tested without opening a window and clicking |
| 4 | **no validation at all** | before `parseDouble` | a blank register number, letters, or a negative amount all reach the database |
| 5 | **JDBC on the EDT** | `DriverManager.getConnection` | the window cannot repaint, move or close while the query runs |
| 6 | **SQL by concatenation** | `executeUpdate("... '" + reg + "' ...")` | a register number of `x'); DROP TABLE fee_payment; --` executes as SQL |
| 7 | nothing is closed | no `finally`, no try-with-resources | every payment leaks a `Connection` and a `Statement` |
| 8 | **`Thread.sleep(2000)` on the EDT** | inside the try | freezes the entire interface for two seconds, on purpose |
| 9 | exception swallowed | `catch (Exception ex) { }` | a failed payment looks exactly like a slow successful one |
| 10 | stale status on Clear | the `Clear` branch | "Paid" stays on screen next to an empty form |
| 11 | **Receipt does nothing** | no branch for it | the button is registered, is enabled, and silently does nothing |

Faults 4, 5, 6, 8 and 11 are the five the task asks for; the rest are
recorded because they are in the same 60 lines.

Fault 6 deserves its own line: it is not a style problem. The window accepts a
register number from a counter clerk and splices it into an SQL string, so any
person at the counter can execute arbitrary SQL with the application's
privileges. Task B2 of Assignment 5 demonstrates the identical fault being
exploited.

## The corrected design

```
                 +--------------------------+
                 |   FeePaymentWindow       |   Swing only.
                 |   - collects input       |   No rule, no SQL.
                 |   - displays results     |
                 +------------+-------------+
                              | validate(...) / pay(...)
                 +------------v-------------+
                 |   FeeService             |   Every rule.
                 |   - validate()           |   No Swing import at all.
                 |   - pay()                |
                 +------------+-------------+
                              | save(Payment)
                 +------------v-------------+
                 |   FeeRepository          |   The only class with SQL.
                 |   - PreparedStatement    |   try-with-resources.
                 +--------------------------+
                              |
                 +------------v-------------+
                 |   Payment (record)       |   Immutable. Money in PAISE.
                 +--------------------------+
```

### Event handling, rewritten

Each button now owns an `Action`. An `Action` carries its name, its enabled
state and its behaviour in one object, so the window never has to ask which
button was pressed - the question that forced fault 2.

```java
private final Action payAction = new AbstractAction("Pay") {
    @Override public void actionPerformed(ActionEvent e) { onPay(); }
};
```

and the handler is now four lines of user-interface work:

```java
private void onPay() {
    final Payment p;
    try { p = service.validate(regField.getText(), (String) feeType.getSelectedItem(),
                               amountField.getText()); }
    catch (ValidationException ex) { error(ex.getMessage()); focusField(ex.field()); return; }

    new SwingWorker<Long, Void>() {                       // off the EDT
        @Override protected Long doInBackground() throws Exception { return service.pay(...); }
        @Override protected void done() { ... }           // back on the EDT
    }.execute();
}
```

### The defence, in under 100 words

> The window now does one job: collect text and show results. Every rule
> moved into `FeeService`, which imports no Swing class and can therefore be
> tested from a plain `main` - sixteen checks run in
> `feeservice_test.txt` with no window on screen, and none of them could have
> been written against the original. SQL moved into `FeeRepository`, which
> binds parameters instead of concatenating them and closes everything it
> opens. The slow call moved into a `SwingWorker`, so the interface stays
> alive. Money moved from `double` into `long` paise, because a fee ledger
> must represent its own values exactly.

(99 words.)

### Evidence: the rules are now testable

```
java.awt.headless = true   (no display is used by this test)
  V1    | accepted, 150000 paise    | expected 150000 paise  | PASS
  V2    | accepted, 150050 paise    | expected 150050 paise  | PASS
  ...
  VF    | rejected on amount        | Amount exceeds the Rs 5,00,000 counter limit | PASS
  16 of 16 checks passed.
```

> **A gap our own test found.** Check VG fed the amount `1e5`. `BigDecimal`
> parses scientific notation happily, so the first version of the validator
> accepted it as Rs 1,00,000. A clerk typing `1e5` has made a typing mistake,
> not a scientific-notation entry. The validator now requires the text to
> *look* like money (`\d{1,7}(\.\d{1,2})?`) before `BigDecimal` is asked to
> parse it.

### The money point

```
as double : 0.1 + 0.2 = 0.30000000000000004441
equals 0.3? false
as paise  : 10 + 20 = 30  (exact)
```

The original parsed with `Double.parseDouble` and stored a `double`.

### Screenshots

| Image | Shows |
|---|---|
| `shots/task01-1-empty.png` | the redesigned window as opened |
| `shots/task01-2-invalid-register.png` | `1923` rejected, focus moved to the field |
| `shots/task01-3-invalid-amount.png` | `-50` rejected before anything is sent |
| `shots/task01-4-paid.png` | Rs 1500.50 paid, receipt number returned from the database |

## Alternative concept: one shared listener, separate listeners, or `Action`

| | one shared listener | separate listener objects | `Action` |
|---|---|---|---|
| how the handler identifies the source | `getActionCommand()` string | not needed | not needed |
| adding a button | another branch in one method | another small class | another `Action` field |
| the handler grows with | number of buttons | nothing | nothing |
| enable/disable | manual, per component | manual, per component | **one call, every component follows** |
| same action from a menu *and* a toolbar | duplicate the branch | register the same object twice | **one object, both carry its text and state** |
| failure mode | a missing branch does nothing, silently (fault 11) | compiler requires a body | compiler requires a body |

The decisive column is the last. Fault 11 - the Receipt button that silently
does nothing - is only possible in the shared-listener design, because that
design permits a registered component with no behaviour. With an `Action`, the
behaviour and the registration are the same object.

### Complexity

Dispatch is O(1) in all three: Swing holds a listener list per component and
calls it directly. The shared listener adds a string comparison against up to
*k* commands, so dispatch is O(k) in the number of buttons - negligible in
time, but it is O(k) in *code* too, and that is the cost that matters.

---

# Task 2 - Requirement to Event-Driven Design

**Files:** `Task02/AppointmentDesk.java`, `Task02/event_trace.txt`

## The event table

| Event source | Event object | Listener interface | Action |
|---|---|---|---|
| `JComboBox` department | `ItemEvent` (SELECTED) | `ItemListener` | `refreshDoctors()` - repopulate the doctor list |
| `JButton` Book | `ActionEvent` | `ActionListener` | `validateAndBook()` |
| `JButton` Cancel | `ActionEvent` | `ActionListener` | `clearForm()` |
| `JTextField` patient | `ActionEvent` (Enter) | `ActionListener` | `validateAndBook()` |
| `JComboBox` slot | `ItemEvent` | `ItemListener` | (not handled - the slot is read when Book is pressed) |
| `JFrame` | `WindowEvent` | `WindowListener` | `windowOpened`, `windowActivated`, `windowClosing`, `windowClosed` |

`ItemListener` fires **twice** per change - once DESELECTED for the old value,
once SELECTED for the new one - so the handler tests
`e.getStateChange() != ItemEvent.SELECTED` and returns. Without that guard the
doctor list is rebuilt twice on every department change.

## The component containment tree

```
JFrame "Outpatient Appointment Desk"      BorderLayout
 +-- NORTH   JLabel status
 +-- CENTER  JPanel form                   GridBagLayout
 |            +-- JLabel "Patient name"   +-- JTextField patientField
 |            +-- JLabel "Department"     +-- JComboBox<String> department
 |            +-- JLabel "Doctor"         +-- JComboBox<String> doctor
 |            +-- JLabel "Slot"           +-- JComboBox<String> slot
 +-- SOUTH   JPanel buttons                FlowLayout(RIGHT)
              +-- JButton "Cancel"
              +-- JButton "Book"
```

## Pseudocode - refresh the doctor list when the department changes

```
ALGORITHM RefreshDoctors
INPUT    selectedDepartment
OUTPUT   the doctor combo repopulated
BEGIN
 1. IF the event is not a SELECTED state change THEN RETURN
 2. dept <- department.getSelectedItem()
 3. doctor.removeAllItems()
 4. doctorList <- DOCTORS.get(dept)            // one Map lookup, O(1)
 5. IF doctorList is empty THEN
 6.      status <- "No doctors in " + dept
 7.      RETURN
 8. FOR EACH d IN doctorList DO doctor.addItem(d)
 9. status <- count(doctorList) + " doctor(s) in " + dept
END
```

## Pseudocode - validate and book an appointment

```
ALGORITHM ValidateAndBook
INPUT    patientName, selectedDoctor, selectedSlot
OUTPUT   a booking, or a message naming the first failure
BEGIN
 1. name <- trim(patientName)
 2. IF name is empty THEN  show "Patient name is required";  RETURN
 3. IF name does not match [A-Za-z .]{2,40} THEN
 4.      show "Patient name may contain letters, spaces and dots only"; RETURN
 5. IF selectedDoctor is NULL THEN show "No doctor available"; RETURN
 6. key <- selectedDoctor + "@" + selectedSlot
 7. IF booked CONTAINS key THEN show "That slot is already taken"; RETURN
 8. ADD key TO booked
 9. show "Booked " + name + " with " + selectedDoctor + " at " + selectedSlot
END
```

## The method-call sequence, recorded by the running program

Rather than assert the order, every listener appends to a log. This is the
real output, with real timings:

| At | Event source | Event object | Listener interface | Action |
|---|---|---|---|---|
| 127.1 ms | JFrame | - | - | constructor runs, components created |
| 222.9 ms | JFrame | WindowEvent | WindowListener | `windowOpened()` |
| 226.0 ms | JFrame | WindowEvent | WindowListener | `windowActivated()` |
| 1520.1 ms | JComboBox department | ItemEvent(SELECTED) | ItemListener | `refreshDoctors()` |
| 2352.2 ms | JButton Book | ActionEvent | ActionListener | `validateAndBook()` - rejected, name empty |
| 3132.1 ms | JButton Book | ActionEvent | ActionListener | `validateAndBook()` - booked |
| 3908.4 ms | JButton Book | ActionEvent | ActionListener | `validateAndBook()` - rejected, slot taken |
| 4616.2 ms | JFrame | WindowEvent | WindowListener | `windowClosing()` |
| 4619.5 ms | JFrame | WindowEvent | WindowListener | `windowDeactivated()` |
| 4619.7 ms | JFrame | WindowEvent | WindowListener | `windowClosed()` |

Two details are worth reading off this trace. `windowOpened` fires about 96 ms
*after* the constructor finished - construction and opening are separate
events. And `windowClosing` precedes `windowClosed` by 3.5 ms, with
`windowDeactivated` between them: `windowClosing` is the *request*, and it is
the only one that can be refused.

## Comparison with the applet life cycle

| Applet | Frame equivalent | Difference that matters |
|---|---|---|
| `init()` | the constructor | the applet's is called by the browser; ours is called by us |
| `start()` | `windowOpened` / `windowActivated` | an applet may be started and stopped many times as the page scrolls |
| `paint(Graphics)` | `paintComponent(Graphics)` | the same idea, and the same trap - see Task 4 |
| `stop()` | `windowDeactivated` | an applet stops when the page is left, a frame when focus moves |
| `destroy()` | `windowClosed` | the last chance to release resources in both |

The structural difference is **who is in control**. The applet life cycle is
driven by a container that may start and stop it repeatedly, so an applet must
tolerate being stopped and restarted. A frame's life cycle is driven by the
user, is usually linear, and - importantly - `windowClosing` can be *vetoed*,
which has no applet equivalent. That is why this window uses
`DO_NOTHING_ON_CLOSE` plus a confirmation rather than `EXIT_ON_CLOSE`.

## Comparison: query the doctors each time, or load once into a Map

| | query on every change | load once into a `Map` |
|---|---|---|
| time per department change | one round trip: milliseconds | one hash lookup: O(1), nanoseconds |
| *d* changes while the window is open | *d* queries | 1 query at startup |
| freshness | always current | stale if a doctor is added while open |
| behaviour if the database is down | the list is empty, mid-session | the window would not have opened |
| memory | one department's doctors | every department's doctors |

For a doctor roster - a few hundred entries that change weekly - loading once
into a `Map` is clearly right, and that is what `AppointmentDesk.DOCTORS` does.
The roster is small, bounded and slow-changing, so paying a network round trip
on every combo-box change buys freshness nobody needs. If the roster were
large or volatile - available *slots*, which change every booking - the answer
reverses, and that is why slot availability is checked at Book time rather than
cached.

---

# Task 3 - From Sketch to Layout

**Files:** `Task03/LibrarySearch.java`, `Task03/resize_report.txt`

## The nested panel structure and the layout chosen for each container

```
JFrame                                    BorderLayout
 +-- NORTH   titlePanel                   FlowLayout(CENTER)
 |             +-- JLabel "CAMPUS LIBRARY CATALOGUE"
 +-- CENTER  centrePanel                  BorderLayout
 |             +-- NORTH  searchRow       FlowLayout(LEFT)
 |             |            +-- JLabel "Search"
 |             |            +-- JTextField searchField
 |             |            +-- JComboBox<String> category
 |             |            +-- JButton "Search"
 |             +-- CENTER JScrollPane     <- THE PART THAT GROWS
 |                          +-- JList results
 +-- SOUTH   southPanel                   BorderLayout
               +-- CENTER buttonRow       FlowLayout(CENTER)
               |            +-- JButton Prev, Next, Reserve
               +-- SOUTH  JLabel status
```

## Why each layout manager

| Container | Layout | Why that one, and not the others |
|---|---|---|
| `JFrame` | `BorderLayout` | the requirement *is* BorderLayout's behaviour: all spare space to CENTER, NORTH and SOUTH stay at preferred height |
| `titlePanel` | `FlowLayout(CENTER)` | a title must keep its own size and stay centred. `GridLayout` would stretch the label across the full width |
| `searchRow` | `FlowLayout(LEFT)` | field, Choice and button keep their preferred widths. `GridLayout(1,3)` would stretch the Search button to a third of the window |
| `centrePanel` | `BorderLayout` | lets the scroll pane take every spare pixel while the search row above it stays one row high |
| `southPanel` | `BorderLayout` | buttons in CENTER, status in SOUTH, each at its own height |
| `buttonRow` | `FlowLayout(CENTER)` | three equal buttons, centred, keeping their natural size |

`CardLayout` appears nowhere, because this screen never swaps one view for
another; it would be the right choice if Search and Reserve were separate
pages of the same window.

## Measured resize behaviour at three window sizes

| Size | Window | Results area (must grow) | Search field (must not) | Reserve button (must not) |
|---|---|---|---|---|
| small | 560 x 320 | **560 x 195** | 246 x 19 | 82 x 25 |
| medium | 820 x 500 | **820 x 375** | 246 x 19 | 82 x 25 |
| large | 1180 x 740 | **1180 x 615** | 246 x 19 | 82 x 25 |

The results area grows in both directions - 195 to 615 pixels of height, more
than three times - while the search field and the Reserve button are
pixel-identical at every size. That is the requirement, measured rather than
asserted.

Screenshots: `shots/task03-1-small.png`, `-2-medium.png`, `-3-large.png`.

> **A measurement bug of ours.** The first version of the harness searched the
> component tree for the first `JButton` and reported the button row as
> 20 x 24 pixels. That was the arrow button *inside* the `JComboBox`, found by
> the depth-first walk before any real button. The harness now looks the
> Reserve button up by its label.

## Alternative concept: nested layouts, `GridBagLayout`, or absolute positioning

| | nested layouts | `GridBagLayout` | absolute (`setBounds`) |
|---|---|---|---|
| code for this screen | 6 small containers | 1 container, ~10 constraint objects | 1 container, 12 x/y/w/h sets |
| resizing | automatic | automatic, via `weightx`/`weighty` | **nothing moves** |
| different font size or platform | adapts | adapts | clips or overlaps |
| reading the code | the structure *is* the layout | constraints must be read together | positions say nothing about intent |
| fine control | limited by the managers | total | total |

Nested layouts win here because the requirement is coarse: one area grows, two
rows do not. That is one `BorderLayout` decision repeated three times.
`GridBagLayout` would do it in one container but trades six obvious containers
for ten non-obvious constraint objects - and it is used in this assignment
where it genuinely earns its place: Task 1's form, where labels and fields must
align in a grid.

Absolute positioning is not a trade-off but a defect waiting to happen: the
measured table above would read 560 x 195 in all three rows, because nothing
would move.

### Complexity

Every layout manager here is O(n) in the number of children of a container,
and the nesting makes layout O(total components) per resize - about 15
components, recomputed only on a resize event. `GridBagLayout` is also O(n)
but with a larger constant, since it solves row and column sizes across the
whole grid. Neither is measurable at this scale; the choice is about
legibility, not speed.

---

# Task 4 - Event-Handling Forensics

**Files:** `Task04/DrawingPadBroken.java` (as supplied),
`Task04/DrawingPadHalfFixed.java` (TODO 1 only), `Task04/DrawingPadFixed.java`
(both TODOs), `Task04/override_proof.txt`, `Task04/forensics_report.txt`

## Why the click handler does not run

```java
addMouseListener(new MouseAdapter() {
    public void mouseClick(MouseEvent e) { ... }   // mouseClickED is the real name
});
```

`MouseAdapter` is a **class with empty method bodies**, not an interface with
abstract methods. An anonymous subclass may therefore declare any method it
likes. `mouseClick(MouseEvent)` is a perfectly legal *new* method that
overrides nothing, so nothing ever calls it. The file compiles cleanly and the
pad is silently dead to the mouse.

## Why the label does not update

Two reasons, and both had to be fixed. The handler never runs (above), and
even if it did, the supplied handler never touches the label - it increments
`clicks` and draws, and that is all.

## Why minimising and restoring reveals a repainting problem

The supplied handler draws with `getGraphics()`:

```java
Graphics g = getGraphics();
g.setColor(Color.BLUE);
g.fillOval(e.getX() - 4, e.getY() - 4, 8, 8);
```

That paints pixels **onto the component's current on-screen surface and
nowhere else**. Nothing records that a dot exists. When the window is
minimised and restored - or covered and uncovered, or resized - Swing repaints
the panel by calling `paintComponent`, which fills the background and draws
nothing, because the pad has no memory of what was clicked.

## The repair - TODO 1 and TODO 2

```java
/* TODO 1 */
@Override                                   // the compiler now checks the name
public void mouseClicked(MouseEvent e) {
    points.add(e.getPoint());               // 1. store the point
    clicks++;
    counter.setText("Clicks: " + clicks);   // 2. update the label
    repaint();                              // 3. ask Swing to repaint
}

/* TODO 2 */
@Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    g.setColor(Color.BLUE);
    for (Point p : points) g.fillOval(p.x - 4, p.y - 4, 8, 8);
}
```

## Measured: the same five clicks delivered to three versions

| Version | Label after 5 clicks | Dots before | Dots after repaint |
|---|---|---|---|
| `DrawingPadBroken` (as supplied) | **Clicks: 0** | 0 | 0 |
| `DrawingPadHalfFixed` (TODO 1 only) | Clicks: 5 | **5** | **0** |
| `DrawingPadFixed` (TODO 1 + 2) | Clicks: 5 | 5 | **5** |

Dots are counted by sampling the captured window pixels at the five clicked
coordinates.

The middle row exists because the two faults cannot be observed at once: in the
supplied program the handler never runs, so the repainting fault is invisible
behind it. `DrawingPadHalfFixed` completes TODO 1 and deliberately leaves TODO
2 undone, which isolates the repaint fault exactly.

Screenshots: `shots/task04-1a/1b`, `-2a/2b`, `-3a/3b` - before and after for
each version.

> **Two honest notes on the method.** First, these screenshots are captured on
> Xvfb with no window manager, and we recorded whether the iconify request was
> honoured - it was. Even so, the harness also forces the full repaint that a
> real restore triggers, so the fault is exercised regardless of what the
> window manager does. Second, our first run showed **0 dots even before**
> minimising, which looked like an even worse fault. It was not: the
> screenshot helper called `repaint()` before capturing, and so erased the
> `getGraphics` drawing itself. A helper that repaints cannot photograph a
> fault whose symptom is "a repaint erases it". The helper no longer repaints.

## How `@Override` catches it at compile time

The only difference between these two is the annotation:

```
$ javac Task04/DrawingPadBroken.java
(no output - it compiles cleanly, and the pad is silently dead to the mouse)

$ javac Task04/compile_proof/OverrideCatchesIt.java
OverrideCatchesIt.java:8: error: method does not override or implement a method from a supertype
        @Override
        ^
1 error
```

`@Override` asks the compiler to *assert* that the method overrides something.
The assertion fails before the program is ever run - rather than after a user
wonders why clicking does nothing.

## Alternative concept: stored `Point` objects versus an off-screen `BufferedImage`

| | `List<Point>` | off-screen `BufferedImage` |
|---|---|---|
| memory | O(k) for k clicks, ~32 bytes each | O(w x h) always: 460x300 at 4 bytes = **552 kB**, whatever k is |
| repaint cost | O(k) - redraw every dot | O(1) - blit one image |
| 10 clicks | 320 bytes, trivial repaint | 552 kB, one blit |
| 100,000 clicks | 3.2 MB, 100,000 fillOvals per repaint | 552 kB, one blit |
| window resized larger | dots redraw crisply at any size | the image must be recreated, or it stretches |
| undo one dot | `points.remove(last)` | impossible - repaint everything from a model you do not have |
| change the dot colour | redraw with a new colour | impossible - the pixels are already mixed |

The crossing point is where O(k) redraw cost exceeds a single blit - in
practice a few thousand marks. **A `List<Point>` is a model; a `BufferedImage`
is a cache of a model.** Keeping the model is what makes undo, recolouring and
clean resizing possible at all, which is why serious drawing programs keep
both: the model for truth, the image for speed.

---

# Task 5 - Same Screen, Different Event Styles

**Files:** `Task05/Converter.java` (the shared rule), `Ui.java` (the shared
window), and the four styles; `Task05/styles_report.txt`

All four build an identical window and share `Converter.convert()` and
`Ui.run()`, so **the only difference between them is the wiring** - which is
what makes the measurement below meaningful.

| Style | Valid 100 INR to USD | Invalid `12x` |
|---|---|---|
| `ConverterImplements` - frame implements `ActionListener` | OK | OK |
| `ConverterNamedInner` - named inner class | OK | OK |
| `ConverterAnonymous` - anonymous inner class | OK | OK |
| `ConverterLambda` - lambda expression | OK | OK |

Screenshots: `shots/task05-1a..4b` - a valid and an invalid conversion from
each of the four.

## How much code each style costs

| Style | Total lines | Wiring lines | Extra classes | To add a third trigger |
|---|---|---|---|---|
| `ConverterImplements` | 21 | 6 | 0 | another branch in the one handler |
| `ConverterNamedInner` | 24 | 4 | 1 | one more `new ConvertHandler(...)` |
| `ConverterAnonymous` | **25** | 4 | **2** | another 5-line anonymous class |
| `ConverterLambda` | **16** | **2** | 0 | one more line |

Blank lines and comments excluded.

## Comparison

| | implements | named inner | anonymous | lambda |
|---|---|---|---|---|
| readability | handler is far from the button | named, so it documents itself | behaviour sits at the registration | behaviour sits at the registration, shortest |
| access to frame fields | direct (`this`) | via `Outer.this` | via `Outer.this` | direct - `this` is the enclosing frame |
| handler reuse | one method, many sources | **the class can be instantiated repeatedly** | not reusable | assign to a variable to reuse |
| ease of change | must find the right branch | change one named class | find the right anonymous block | change one line |
| what `this` means | the frame | the inner instance | the inner instance | **the frame** |
| suits multiple triggers | poorly - branches multiply | **well** | poorly | well |

The `this` row is the one that bites. In an anonymous `ActionListener`, `this`
is the listener, so `ConverterAnonymous.this` is needed to reach the frame. In
a lambda there is no new scope, so `this` is already the frame - which is why
`ConverterLambda` reads `Ui.run(this, ...)` while `ConverterAnonymous` reads
`Ui.run(ConverterAnonymous.this, ...)`.

**The lambda is the right default** - fewest lines, no extra class, no scoping
surprise. The named inner class earns its place when the same handler is
needed more than once with different configuration, which is exactly why
`ConverterNamedInner` registers `new ConvertHandler(false)` for the button and
`new ConvertHandler(true)` for the Enter key: one class, two behaviours, no
duplication. A lambda would need the body twice or a shared helper.

## Enter in the amount field, and the dialog

Every version registers an `ActionListener` on the `JTextField` as well. A
`JTextField` fires an `ActionEvent` when Enter is pressed, so the same handler
serves both triggers; the boolean argument decides whether the result also
appears in a `JOptionPane`.

`ConverterImplements` is the one version that cannot take an argument - it has
a single `actionPerformed` - so it must ask `e.getSource() == amount`. That is
the shared-listener problem of Task 1 reappearing in miniature.

## Window closing

All four use a `WindowAdapter` with `DO_NOTHING_ON_CLOSE`, so closing is a
*request* the user confirms:

```java
f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
f.addWindowListener(new WindowAdapter() {
    @Override public void windowClosing(WindowEvent e) {
        if (JOptionPane.showConfirmDialog(f, "Close the converter?", ...) == YES_OPTION) f.dispose();
    }
});
```

`WindowAdapter` rather than `WindowListener` because the interface declares
seven methods and only one is wanted.

## Alternative concept: lambda, method reference, or a shared `Action`

| | lambda | method reference | shared `Action` |
|---|---|---|---|
| shortest form | `e -> convert()` | `this::convert` - but the signature must match | several lines |
| works when the handler ignores `e` | yes | **only if the method takes an `ActionEvent`** | yes |
| carries name, icon, enabled state | no | no | **yes** |
| one object for a button *and* a menu item | register twice | register twice | **one object, both update together** |
| disable from one place | no | no | **`setEnabled(false)` once** |

A method reference is the tidiest form when the handler genuinely takes an
`ActionEvent` - `convert.addActionListener(this::onConvert)`. It cannot be
used when the handler ignores the event, which is the common case, so a lambda
that discards `e` is usually what gets written.

The `Action` wins as soon as one behaviour has two entry points. Task 6 is
exactly that case: Export appears both as a button and as a menu item, and
Task 1's redesign uses `Action` for the same reason.

---

# Task 6 - Design Challenge: Campus Event Registration

**Files:** `sql/task06_schema.sql`, `Task06/Model.java`,
`Task06/RegistrationDao.java`, `Task06/RegistrationService.java`,
`Task06/EventRegistrationApp.java`, `Task06/PaginationBenchmark.java`,
`Task06/app_demo.txt`, `Task06/pagination_report.txt`

## Users, screens, data, rules and testable requirements

| Users | What they do |
|---|---|
| Student | registers for an event |
| Committee member | views registrations, filters by event, exports the list |

| Screen | Contents |
|---|---|
| Register tab | event chooser, register number, name, email, Register button, status line |
| Registrations tab | event filter, results table, Prev/Next, Export CSV, status line |
| Menu bar | File (Refresh events, Export, Exit), Help (About) |
| Dialogs | About, close confirmation |

| Rule | Where enforced |
|---|---|
| register number is exactly 9 digits | `RegistrationService.validate` |
| name is letters, spaces and dots, 2-60 | `RegistrationService.validate` |
| email matches an address pattern | `RegistrationService.validate` |
| **no student registers twice for one event** | service check **and** a `UNIQUE KEY` |
| an event cannot exceed its capacity | `RegistrationService.register` |

| ID | Testable requirement | Result |
|---|---|---|
| T1 | a valid registration is stored and acknowledged | **accepted**, id returned |
| T2 | a bad register number is rejected before any SQL runs | **rejected** |
| T3 | a bad email is rejected | **rejected** |
| T4 | a bad name is rejected | **rejected** |
| T5 | the same student cannot register twice | **rejected** |
| T6 | the table shows one page, not the whole table | 25 rows per page |
| T7 | the window stays responsive during database work | EDT latency 0.3 ms |

## Where validation, duplicate checking and database access happen

```
   EventRegistrationApp  (Swing)         collects text, shows results
            |                             NO rules, NO SQL
            v  register(event, reg, name, email)
   RegistrationService                    validate -> duplicate -> capacity
            |                             NO Swing, NO SQL text
            v  alreadyRegistered / countFor / insert
   RegistrationDao       (JDBC)           PreparedStatement only
            |                             try-with-resources on every call
            v
   MariaDB  csa09_test                    UNIQUE(event_id, reg_no)
```

**Duplicate checking happens twice, on purpose.** The service asks the DAO
before inserting, so the user gets a clear message. The table also carries
`UNIQUE KEY uq_one_registration (event_id, reg_no)`, because the service check
is a race: two committee members registering the same student at the same
moment both see "not registered" and both insert. The service catches
`SQLIntegrityConstraintViolationException` and reports it as the same
duplicate message, so the race produces a correct message rather than a stack
trace.

## Demonstrated - normal, invalid and duplicate

| Case | Status shown to the user | Outcome |
|---|---|---|
| normal registration | `Registered 192325002 for Tech Symposium 2026 (id 1)` | **ACCEPTED** |
| invalid register number (`1923`) | `Register number must be exactly 9 digits` | REJECTED |
| invalid email (`not-an-email`) | `Enter a valid email address` | REJECTED |
| invalid name (`R2D2`) | `Name may contain letters, spaces and dots only` | REJECTED |
| duplicate registration | `192325002 is already registered for Tech Symposium 2026` | REJECTED |

Screenshots `shots/task06-1` to `-7`, ending with the registrations table.

## Keeping the interface responsive

Every database call in the UI runs inside a `SwingWorker`:
`doInBackground()` on a worker thread, `done()` back on the Event Dispatch
Thread. The Register button disables itself while a registration is in flight
and an indeterminate `JProgressBar` shows at the foot of the window.

## Measured: loading all 100,000 registrations versus paginated loading

100,000 registrations were inserted into one event (3,144 ms, batches of 1,000
in a single transaction).

### Time to first screen

| Strategy | Median | RSD | Rows fetched | Rows the user sees |
|---|---|---|---|---|
| load all | 141.7 ms | 20.7% | 100,000 | 25 |
| paginated (`LIMIT 25 OFFSET 0`) | **1.4 ms** | 28.4% | 25 | 25 |

**Paginated is 104x faster to the first screen.**

### Memory held while the screen is showing

| Strategy | Held for the screen |
|---|---|
| load all | **82,474 kB** for 100,000 rows |
| paginated | **512 kB** for 25 rows |

About 844 bytes per registration. 100,000 of them is 80 MB of heap held for as
long as the window is open, to display 25 rows.

### Window responsiveness

A probe is posted to the Event Dispatch Thread every 20 ms while each strategy
runs; the number recorded is how long the EDT took to answer - exactly how
long a click or a repaint would have waited.

| How the load is run | Worst | Median | User experience |
|---|---|---|---|
| load all, **on the EDT** | **231.2 ms** | 231.2 ms | stutters - a visible freeze |
| load all, in a `SwingWorker` | 1.7 ms | 0.3 ms | stays responsive |
| paginated, in a `SwingWorker` | 0.3 ms | 0.3 ms | stays responsive |

### What this means for the design

The table shows **two independent decisions**, and both have to be right.

Running the load on the EDT freezes the window for the whole fetch. Moving the
*same* load into a `SwingWorker` fixes the freeze without making the load any
faster - 141.7 ms of work still happens, the user just is not locked out of
the window while it does.

Pagination then fixes the load itself. The user is shown 25 rows, so fetching
100,000 to display 25 is wasted work no matter which thread does it, and the
wasted 80 MB is held for as long as the screen is open.

This is why `EventRegistrationApp` uses `LIMIT`/`OFFSET` for the table **and**
fetches in a `SwingWorker` - and why the CSV export, which genuinely does need
every row, walks the data a page at a time rather than holding the whole result
set in memory.

### Complexity

| Operation | Load all | Paginated |
|---|---|---|
| first screen, n rows | O(n) time, O(n) memory | **O(p)** time and memory, p = page size |
| scrolling to page k | O(1) - already in memory | O(p) per page, O(k x p) rows skipped by OFFSET |
| total for reading all n | O(n) | O(n) across n/p pages |
| memory ceiling | grows with n, unbounded | **constant in n** |

Pagination's weakness is deep paging: `OFFSET k*p` makes the database skip
`k*p` rows, so page 4,000 of 4,000 costs more than page 1. The index
`idx_event_reg (event_id, reg_id)` keeps that scan on the index rather than the
table, and for genuinely deep paging a keyset cursor
(`WHERE reg_id > last_seen ORDER BY reg_id LIMIT 25`) makes every page O(p)
regardless of depth. That is the next change this design would need, and it is
the one remaining limitation worth naming.

---

# What this assignment demonstrated

Three things recurred across the six tasks, and all three were measured rather
than asserted:

1. **Separating the layers is what makes rules testable.** Task 1's sixteen
   validation checks run with no window on screen. Against the supplied
   window not one of them could have been written, because every rule was
   inside an event handler that required a mouse click and a live database.

2. **A Swing fault is usually silent.** A misnamed listener method compiles
   (Task 4). A button with no branch does nothing (Task 1, fault 11). A
   `getGraphics` drawing looks perfect until something repaints (Task 4).
   None of these produce an error message; each needs a designed test.

3. **The Event Dispatch Thread is a shared resource with one user.** Task 6
   measured it directly: 231 ms of EDT latency when a query runs on it, 0.3 ms
   when the same query runs in a `SwingWorker`.

Three measurements in this assignment were wrong before they were right, and
each is recorded above rather than quietly corrected: a validator that accepted
`1e5` as a rupee amount, a resize harness that measured a combo box's arrow
button, and a screenshot helper that repainted the very drawing it was called
to photograph.
