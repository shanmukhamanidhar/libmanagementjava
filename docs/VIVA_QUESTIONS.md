# 24CS202 Object Oriented Programming Through Java — Comprehensive Viva Question Bank

This guide is designed for your college project demonstration and oral viva examination. It gives direct, technically rigorous, and student-friendly answers to the most common questions asked by university faculty.

---

## Category 1: Fundamental Java & OOP Principles

### Q1: What makes Java platform-independent?
**Answer:**  
Java source code (`.java`) is compiled by the Java Compiler (`javac`) into an intermediate, architecture-neutral format called **Bytecode** (`.class` files), rather than native machine code. Any computer that has a **Java Virtual Machine (JVM)** written for its specific operating system (Windows, Linux, macOS) can execute this bytecode. Hence: *"Write Once, Run Anywhere"* (WORA).

### Q2: How does Java differ fundamentally from C and C++?
**Answer:**  
1. **Memory Management:** C and C++ require manual memory allocation and deallocation using pointers (`malloc`/`free` or `new`/`delete`), which often leads to memory leaks and segmentation faults. Java has no raw memory pointers and manages heap memory automatically via the **Garbage Collector**.
2. **Platform Independence:** C and C++ compile to native binary files bound to a particular OS/CPU. Java compiles to bytecode executed on a JVM.
3. **Multiple Inheritance of Classes:** C++ supports multiple inheritance of classes, which causes ambiguity (the Diamond Problem). Java forbids multiple class inheritance and allows a class to inherit only one superclass, while allowing multiple inheritance of type through **interfaces**.

### Q3: What is Encapsulation, and how is it implemented in your project?
**Answer:**  
Encapsulation is the mechanism of wrapping data fields and the methods that operate on that data into a single unit (class), while restricting direct outside access using private access modifiers (data hiding).  
*In our project:* In `Book.java` and `Student.java`, all fields (such as `title`, `author`, `department`, `email`) are declared `private`. Access and modification are strictly controlled through public getters and validated setters (for example, ensuring publication year is within valid ranges and names are not blank).

### Q4: Explain the role of the `this` and `super` keywords.
**Answer:**  
- **`this`:** Refers to the current invoking object instance. Used to disambiguate instance fields from constructor/method parameters with identical names (e.g., `this.id = id;`) and to chain constructors in the same class (e.g., `this(id, "Untitled");`).
- **`super`:** Refers directly to the immediate parent superclass. Used in subclass constructors to invoke the superclass constructor (e.g., `super(id, title);` in `Book.java`) or to invoke overridden superclass methods.

---

## Category 2: Inheritance, Polymorphism, Abstract Classes & Interfaces

### Q5: What is the difference between an Abstract Class and an Interface?
**Answer:**  
| Feature | Abstract Class (`LibraryItem`) | Interface (`Identifiable`, `Reportable`) |
|---|---|---|
| **Inheritance** | Single inheritance only (`extends`) | Multiple inheritance supported (`implements A, B`) |
| **State / Fields** | Can have instance variables (`private final String id`) | Only `public static final` constants |
| **Constructors** | Can have constructors (to initialize state) | Cannot have constructors |
| **Default Methods** | Standard concrete methods with bodies | Can have `default` and `static` methods (Java 8+) |
| **Conceptual Role** | Defines an identity and core shared state ("is-a") | Defines a behavioral capability or contract ("can-do") |

### Q6: What is Dynamic Method Dispatch (Runtime Polymorphism)?
**Answer:**  
Dynamic Method Dispatch is the mechanism by which a call to an overridden method is resolved at runtime rather than compile-time based on the actual object type, not the reference type.  
*In our project:*
```java
LibraryItem item = new ReferenceBook("REF-1", "Dictionary", "Author", "Gen", 2024, "R1");
boolean borrowable = item.canBeBorrowed(); // Resolves at runtime to ReferenceBook.canBeBorrowed() -> false
```
Even though the variable `item` is of type `LibraryItem`, Java resolves the call dynamically to the overridden version in `ReferenceBook`.

### Q7: How does Garbage Collection work in Java? Can we force it to run?
**Answer:**  
The JVM Garbage Collector automatically reclaims heap memory occupied by objects that are no longer reachable from any live thread or active root references (using generational tracing algorithms like G1 or ZGC).  
Calling `System.gc()` or `Runtime.getRuntime().gc()` merely **requests** or suggests that the JVM perform garbage collection; it **does not guarantee** immediate execution because the JVM prioritizes performance and memory thresholds.

---

## Category 3: Exception Handling & File I/O

### Q8: What is the difference between Checked and Unchecked Exceptions?
**Answer:**  
- **Checked Exceptions:** Subclasses of `java.lang.Exception` (excluding `RuntimeException`). The Java compiler strictly enforces that they must be either caught using `try-catch` or declared in the method signature using `throws`.  
  *Example in project:* `BookUnavailableException` and `java.io.IOException`.
- **Unchecked Exceptions:** Subclasses of `java.lang.RuntimeException` or `Error`. They represent logical programming errors (such as `NullPointerException`, `IllegalArgumentException`, `IndexOutOfBoundsException`) and do not require mandatory compile-time handling.

### Q9: Why did you create `BookUnavailableException` as a checked exception?
**Answer:**  
Because attempting to borrow an unavailable or non-circulating book is an expected domain operational condition rather than a fatal programming bug. By making it a checked exception, the compiler forces the UI and calling workflows to explicitly catch it and display a helpful, graceful warning message to the librarian without crashing the program.

### Q10: How do Character Streams differ from Byte Streams? Where are both used?
**Answer:**  
- **Character Streams (`Reader` / `Writer`):** Operate on 16-bit Unicode characters. They automatically handle character encoding (such as UTF-8).  
  *Used in our project:* In `LibraryFileHandler.java` using `BufferedReader` and `BufferedWriter` to read and write human-readable delimited records in `books.txt`, `students.txt`, and `loans.txt`.
- **Byte Streams (`InputStream` / `OutputStream`):** Operate on raw 8-bit octets (bytes) without character translation.  
  *Used in our project:* In `createBinaryBackup()` using `FileInputStream` and `FileOutputStream` with a 4KB byte buffer to create bit-exact binary archival copies of library data files.

### Q11: What is try-with-resources and why is it superior to manual `finally` closing?
**Answer:**  
Introduced in Java 7, try-with-resources automatically closes any resource implementing `java.lang.AutoCloseable` at the end of the statement block, regardless of whether an exception occurred. It prevents memory and file descriptor leaks, eliminates messy nested null-checks in `finally` blocks, and correctly handles suppressed exceptions.

---

## Category 4: Multithreading & Concurrency

### Q12: How does your application use multithreading?
**Answer:**  
Our system runs a dedicated background daemon thread called `NotificationThread` (extending `Thread`). It periodically scans all loan records in the background every 15 seconds to identify loans that have become overdue or incurred fines, and announces them to the librarian.

### Q13: What is the Swing Event Dispatch Thread (EDT) rule and how did you adhere to it?
**Answer:**  
**Rule:** Swing UI components are not thread-safe. All component creation, modifications, and repaint calls must happen exclusively on the single **Event Dispatch Thread (EDT)**.  
*Implementation:* In `NotificationThread.java`, whenever the background worker detects an overdue loan, it does not touch the UI directly; instead, it wraps the update inside:
```java
SwingUtilities.invokeLater(() -> uiNotificationCallback.accept(message));
```
This safely posts the update runnable onto the EDT's event queue.

### Q14: Why is `LibraryManager` synchronized?
**Answer:**  
Because the in-memory collections (`books`, `students`, `loans`, `activeIssuedBookIds`) are accessed concurrently by both the user-facing Swing EDT (when issuing/returning books or searching) and the background `NotificationThread` (when scanning overdue loans). Synchronization prevents race conditions and `ConcurrentModificationException`.

---

## Category 5: Collections Framework & Java Stream API

### Q15: Why did you choose `Map`, `List`, and `Set` for different data in `LibraryManager`?
**Answer:**  
Each collection type was selected based on algorithmic efficiency and data characteristics:
1. **`LinkedHashMap` for Books and Students:** Provides $O(1)$ average time complexity for lookups by ID, while preserving catalog insertion order.
2. **`ArrayList` for Loans:** Preserves historical, chronological transaction order and allows fast indexed access.
3. **`HashSet` for `activeIssuedBookIds`:** Provides $O(1)$ set membership checks to instantly verify whether a book ID is already currently borrowed, preventing double-lending.

### Q16: What is the difference between a `HashMap` and a `TreeMap`?
**Answer:**  
- **`HashMap`:** Backed by an array of hash buckets and linked nodes/red-black trees for collisions. It provides $O(1)$ lookup and does not guarantee any key ordering.
- **`TreeMap`:** Implements `NavigableMap` and is backed by a Red-Black self-balancing binary search tree. It maintains keys in strictly sorted natural order (or according to a supplied `Comparator`), with $O(\log n)$ operations. Demonstrated in `SyllabusDemo.demoUnit5CollectionsAndIterators()`.

### Q17: What is the Stream API, and what is the difference between Intermediate and Terminal operations?
**Answer:**  
The Stream API (introduced in Java 8) provides a functional, declarative approach to processing sequences of elements.
- **Intermediate Operations:** Return a new stream and are **lazy** (they do not execute until a terminal operation is called). Examples: `filter()`, `map()`, `sorted()`.
- **Terminal Operations:** Trigger pipeline execution and produce a final non-stream result (a value, collection, or side-effect). Examples: `collect()`, `count()`, `reduce()`, `sum()`, `forEach()`.

### Q18: What is a Parallel Stream, and when should it NOT be used?
**Answer:**  
A parallel stream decomposes the stream pipeline into multiple chunks and executes them concurrently across multiple CPU cores using the JVM's common `ForkJoinPool`.  
*When NOT to use:*
1. On small datasets (where the overhead of thread orchestration, synchronization, and splitting outweighs the calculation time).
2. When operations access shared mutable state (causing race conditions).
3. When order of processing matters (like sequential streams).  
Our Reports panel includes a runnable 200,000-item benchmark comparing sequential vs parallel stream execution time.

---

## Category 6: Application Domain Logic & System Design

### Q19: What is the source of truth for book borrowing status in your system?
**Answer:**  
The source of truth is the active loan status in `LoanRecord` (`returned == false`). To ensure zero discrepancies, whenever `issueBook()` or `returnBook()` is executed in `LibraryManager`, the book's availability flag and the active loan list are modified atomically within a synchronized method, and immediately persisted to text files.

### Q20: How are overdue fines calculated?
**Answer:**  
The system uses the modern Java Date/Time API (`java.time.LocalDate` and `java.time.temporal.ChronoUnit.DAYS`).
- If an item is returned on or before its `dueDate`, overdue days $\le 0$, resulting in a fine of **₹0.00**.
- If returned after `dueDate`, or if still unreturned after `dueDate`:
  $$\text{Overdue Days} = \text{ChronoUnit.DAYS.between(dueDate, effectiveDate)}$$
  $$\text{Fine Amount} = \text{Overdue Days} \times \text{fineRatePerDay (₹5.00)}$$
- The policy settings are configurable in `LibraryManager`.
