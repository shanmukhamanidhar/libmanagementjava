# College Project Team Division & Method Contract Guide

**Course:** 24CS202 — Object Oriented Programming Through Java  
**Project:** Library Management System  
**Development Team Size:** 3 Members

---

## 1. Member Responsibilities & File Ownership

```text
                               ┌────────────────────────────────────────────────────────┐
                               │                    SHARED INTEGRATION                  │
                               │  - LibraryManagementSystem.java (Bootstrap)           │
                               │  - LibraryMainFrame.java (Swing Desktop Interface)     │
                               │  - SyllabusDemo.java (Educational Suite)               │
                               │  - Identifiable.java & Reportable.java (Interfaces)    │
                               │  - LoanRecord.java (Transaction Entity)                │
                               └─────────────────────────┬──────────────────────────────┘
                                                         │
                ┌────────────────────────────────────────┼────────────────────────────────────────┐
                │                                        │                                        │
                ▼                                        ▼                                        ▼
    ┌───────────────────────┐                ┌───────────────────────┐                ┌───────────────────────┐
    │  MEMBER 1 (You)       │                │  MEMBER 2             │                │  MEMBER 3             │
    │  Domain Models        │                │  Library Operations   │                │  Storage & Reports    │
    ├───────────────────────┤                ├───────────────────────┤                ├───────────────────────┤
    │ - LibraryItem.java    │                │ - LibraryManager.java │                │ - LibraryFileHandler  │
    │ - Book.java           │                │ - BookUnavailable     │                │ - LibraryReport.java  │
    │ - ReferenceBook.java  │                │   Exception.java      │                │ - NotificationThread  │
    │ - Student.java        │                │                       │                │                       │
    └───────────────────────┘                └───────────────────────┘                └───────────────────────┘
```

---

## 2. Public Method Contracts & API Specifications

### Member 1: Book & Student Management
*Encapsulates core domain entities, constructor validation, inheritance, and search helpers.*

```java
// LibraryItem.java (Abstract Root)
public abstract String getItemType();
public abstract boolean canBeBorrowed();
public String getId();
public String getTitle();
public void setTitle(String title);
public boolean isAvailable();
public void setAvailable(boolean available);
public static int getTotalItemsCreated();

// Book.java (extends LibraryItem)
public Book(String id, String title, String author, String category, int publicationYear);
public String getAuthor();
public String getCategory();
public int getPublicationYear();
public boolean matchesSearch(String keyword);
public boolean matchesSearch(String keyword, String categoryFilter);

// ReferenceBook.java (extends Book)
public ReferenceBook(String id, String title, String author, String category, int publicationYear, String readingRoomSection);
public String getReadingRoomSection();
@Override public boolean canBeBorrowed(); // Always returns false

// Student.java (implements Identifiable, Reportable)
public Student(String studentId, String name, String department, String email, String contactNumber);
public String getName();
public String getDepartment();
public String getEmail();
public String getContactNumber();
public boolean matchesSearch(String query);
```

---

### Member 2: Library Operations
*Coordinates business rules, issuing, returns, overdue calculations, and duplicate prevention.*

```java
// BookUnavailableException.java (Checked Exception)
public BookUnavailableException(String bookId, String reason);
public String getBookId();
public String getReason();

// LibraryManager.java (Central Business Coordinator)
public synchronized void addBook(Book book) throws IllegalArgumentException;
public synchronized Optional<Book> getBookById(String id);
public synchronized List<Book> getAllBooks();
public synchronized List<Book> searchBooks(String keyword, String category);

public synchronized void registerStudent(Student student) throws IllegalArgumentException;
public synchronized Optional<Student> getStudentById(String id);
public synchronized List<Student> getAllStudents();
public synchronized List<Student> searchStudents(String query);

public synchronized LoanRecord issueBook(String studentId, String bookId, LocalDate issueDate) 
        throws BookUnavailableException, IllegalArgumentException;
public synchronized LoanRecord returnBook(String bookId, LocalDate returnDate) 
        throws IllegalArgumentException;

public synchronized List<LoanRecord> getAllLoans();
public synchronized List<LoanRecord> getActiveLoans();
public synchronized List<LoanRecord> getOverdueLoans(LocalDate asOfDate);
public synchronized List<LoanRecord> getLoansForStudent(String studentId);
public synchronized double calculateTotalOutstandingFines(LocalDate asOfDate);

public synchronized int getDefaultBorrowDays();
public synchronized void setDefaultBorrowDays(int days);
public synchronized double getFineRatePerDay();
public synchronized void setFineRatePerDay(double rate);
```

---

### Member 3: Storage, Reports & Concurrency
*Handles delimited character file persistence, binary backups, Stream API analytics, and background multithreading.*

```java
// LibraryFileHandler.java (Character & Byte Stream I/O)
public LibraryFileHandler(String dataDirPath);
public synchronized void saveAll(List<Book> books, List<Student> students, List<LoanRecord> loans) throws IOException;
public synchronized List<Book> loadBooks() throws IOException;
public synchronized List<Student> loadStudents() throws IOException;
public synchronized List<LoanRecord> loadLoans() throws IOException;
public long createBinaryBackup(File sourceFile, File targetBackupFile) throws IOException;

// LibraryReport.java (Stream API & Analytics)
public LibraryReport(LibraryManager manager);
public DashboardStats getDashboardStats();
public Map<String, Long> getBookCountByCategory();
public List<Book> getBooksSortedByTitle();
public List<Book> getBooksSortedByYear();
public double calculateAveragePublicationYear();
public String benchmarkParallelVsSequentialStream();
public String generateFullTextReport();

// NotificationThread.java (extends Thread)
public NotificationThread(LibraryManager manager, Consumer<String> uiNotificationCallback, int intervalSeconds);
public void run();
public void stopService();
```

---

## 3. Integration Order

The team successfully integrated the project in the following strict order:
1. **Foundation (Member 1 & Interfaces):** Built `Identifiable`, `Reportable`, `LibraryItem`, `Book`, `ReferenceBook`, `Student`, and `LoanRecord`.
2. **Operations (Member 2):** Implemented `BookUnavailableException` and `LibraryManager`, establishing validations and in-memory collections.
3. **Storage (Member 3):** Implemented `LibraryFileHandler` with character streams and `try-with-resources` to persist records to `data/`.
4. **Analytics & Concurrency (Member 3):** Created `LibraryReport` with Stream operations and `NotificationThread` with Swing EDT safety.
5. **Presentation & Application (Shared):** Developed `LibraryMainFrame` (Swing UI) and `LibraryManagementSystem` (Bootstrap and shutdown hooks).
6. **Educational Suite (Shared):** Built `SyllabusDemo` covering remaining syllabus concepts across all 5 units.
7. **Verification:** Executed automated JUnit 5 tests in `LibraryOperationsTest` to verify all business rules.
