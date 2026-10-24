# Project Architecture & System Design Document

**Course:** 24CS202 — Object Oriented Programming Through Java  
**System:** College Library Management System  
**Development Version:** Java 23 (JDK 23)

---

## 1. Architectural Layers Overview

The application follows a clean, decoupled 5-tier architecture tailored for academic clarity and strict separation of concerns:

```mermaid
flowchart TD
    UI["Presentation Layer\n(Swing GUI: LibraryMainFrame)"]
    MGR["Business Operations Layer\n(LibraryManager)"]
    MODEL["Domain Model Layer\n(LibraryItem, Book, ReferenceBook, Student, LoanRecord)"]
    STORAGE["Storage Layer\n(LibraryFileHandler - Text & Byte Streams)"]
    NOTIF["Concurrency Layer\n(NotificationThread - Background Worker)"]
    LEARN["Educational Sandbox\n(SyllabusDemo - Units I to V)"]

    UI -->|Invokes business actions| MGR
    UI -->|Fetches analytics| MGR
    UI -->|Reads concepts| LEARN
    NOTIF -->|Periodically scans overdues| MGR
    NOTIF -->|Dispatches safe EDT alerts| UI
    MGR -->|Operates on collections of| MODEL
    MGR -->|Coordinates persistence with| STORAGE
    STORAGE -->|Reads & writes| FS[("Local Text Files\nbooks.txt, students.txt, loans.txt")]
```

---

## 2. Object-Oriented Domain Class Hierarchy

```mermaid
classDiagram
    direction TB

    class Identifiable {
        <<interface>>
        +String ID_PATTERN$
        +getId() String
        +getDisplayLabel() String
    }

    class Reportable {
        <<interface>>
        +toReportRow() String
        +getReportHeaders() String[]
    }

    class LibraryItem {
        <<abstract>>
        -static int totalItemsCreated
        -final String id
        -String title
        -boolean available
        +LibraryItem(id, title)
        +getItemType()* String
        +canBeBorrowed()* boolean
        +getId() String
        +getTitle() String
        +isAvailable() boolean
        +setAvailable(boolean)
    }

    class Book {
        -String author
        -String category
        -int publicationYear
        +Book(id, title, author, category, year)
        +getItemType() String
        +canBeBorrowed() boolean
        +matchesSearch(query) boolean
        +matchesSearch(query, category) boolean
    }

    class ReferenceBook {
        -String readingRoomSection
        +ReferenceBook(id, title, author, category, year, section)
        +getItemType() String
        +canBeBorrowed() boolean
        +getReadingRoomSection() String
    }

    class Student {
        -final String studentId
        -String name
        -String department
        -String email
        -String contactNumber
        +Student(id, name, department, email, phone)
        +matchesSearch(query) boolean
    }

    class LoanRecord {
        -final String loanId
        -final String studentId
        -final String bookId
        -final LocalDate issueDate
        -final LocalDate dueDate
        -LocalDate returnDate
        -boolean returned
        -double fineAmount
        +isOverdue(currentDate) boolean
        +calculateFine(asOfDate, rate) double
        +markReturned(returnDate, rate) double
    }

    Identifiable <|.. LibraryItem
    Reportable <|.. LibraryItem
    Identifiable <|.. Student
    Reportable <|.. Student
    Identifiable <|.. LoanRecord
    Reportable <|.. LoanRecord

    LibraryItem <|-- Book
    Book <|-- ReferenceBook
```

---

## 3. Data Integrity & Single Source of Truth

A critical flaw in naive library applications is inconsistent state between loan records and item availability flags. In this system:
1. **Source of Truth for Circulation:** 
   - A book is circulating if and only if an unreturned `LoanRecord` exists where `loan.getBookId().equalsIgnoreCase(book.getId())` and `loan.isReturned() == false`.
   - `activeIssuedBookIds` (`Set<String>`) provides O(1) set-membership validation.
2. **Preventing Concurrent Double-Issue:**
   - `LibraryManager.issueBook(...)` and `returnBook(...)` are `synchronized` methods.
   - When a book is issued, `book.setAvailable(false)` and `activeIssuedBookIds.add(bookId)` happen atomically within the synchronized critical section.
3. **Reference Book Safeguard:**
   - `ReferenceBook` overrides `canBeBorrowed()` to return `false` unconditionally.
   - Any attempt to check out a reference book immediately throws `BookUnavailableException` before any state is mutated.

---

## 4. File Persistence Format Specification

Data is saved in character-delimited UTF-8 text files under `data/`:

### A. `books.txt`
```text
TYPE;ID;TITLE;AUTHOR;CATEGORY;YEAR;AVAILABLE;EXTRA
STANDARD;B-101;Core Java: Fundamentals;Cay S. Horstmann;Computer Science;2024;true;N/A
REF;REF-201;Oxford Advanced Learner's Dictionary;A.S. Hornby;General;2023;true;Reading Room Section R-1
```

### B. `students.txt`
```text
ID;NAME;DEPARTMENT;EMAIL;PHONE
24CS001;Aarav Sharma;CSE;aarav.s@college.edu;9876543210
24CS002;Priya Venkatesh;CSE;priya.v@college.edu;9876543211
```

### C. `loans.txt`
```text
LOAN_ID;STUDENT_ID;BOOK_ID;ISSUE_DATE;DUE_DATE;RETURN_DATE;RETURNED;FINE
LN-1001;24CS001;B-101;2026-10-22;2026-11-05;NULL;false;0.00
LN-1002;24CS002;B-102;2026-10-04;2026-10-18;NULL;false;0.00
```

---

## 5. Concurrency & Swing EDT Model

Swing is single-threaded. Modifying UI elements from worker threads causes intermittent race conditions and visual corruption.

```mermaid
sequenceDiagram
    participant Worker as NotificationThread (Background Daemon)
    participant Mgr as LibraryManager (Synchronized)
    participant EDT as Swing Event Dispatch Thread (EDT)
    participant UI as LibraryMainFrame UI Components

    loop Every 15 Seconds
        Worker->>Mgr: synchronized getOverdueLoans(today)
        Mgr-->>Worker: List<LoanRecord>
        alt Overdue Loans Found
            Worker->>EDT: SwingUtilities.invokeLater(() -> callback.accept(alert))
            EDT->>UI: Append alert to notification list model & repaint dashboard
        end
    end
```
