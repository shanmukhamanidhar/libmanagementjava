package library.operations;

import library.model.Book;
import library.model.LibraryItem;
import library.model.LoanRecord;
import library.model.ReferenceBook;
import library.model.Student;
import library.storage.LibraryFileHandler;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 24CS202 Unit II & V Concepts: Collections Framework, Synchronization, and Business Architecture
 * 
 * Member 2 Responsibility: Central Library Business Logic Manager
 * 
 * Coordinates in-memory collections of books, students, and loans, enforces library rules,
 * validates constraints, and interacts with the storage subsystem.
 */
public class LibraryManager {

    // 24CS202 Unit V Concept: Java Collections Framework
    // Map for O(1) keyed lookups; LinkedHashMap preserves catalog insertion order
    private final Map<String, Book> books = new LinkedHashMap<>();
    private final Map<String, Student> students = new LinkedHashMap<>();
    
    // List for chronological historical loans
    private final List<LoanRecord> loans = new ArrayList<>();
    
    // Set for O(1) set membership check of currently borrowed book IDs
    private final Set<String> activeIssuedBookIds = new HashSet<>();

    // Change listeners for real-time synchronization between Web Browser and Swing Desktop GUI
    private final List<Runnable> changeListeners = new ArrayList<>();

    // Central Library Lending Policy Settings
    private int defaultBorrowDays = 14;
    private double fineRatePerDay = 5.0; // ₹5 per day

    // Storage handler reference for persistence
    private LibraryFileHandler fileHandler;

    /**
     * Default Constructor.
     */
    public LibraryManager() {
        this(null);
    }

    /**
     * Parameterized Constructor with Storage Handler.
     * 
     * @param fileHandler Persistence coordinator
     */
    public LibraryManager(LibraryFileHandler fileHandler) {
        this.fileHandler = fileHandler;
    }

    public void setFileHandler(LibraryFileHandler fileHandler) {
        this.fileHandler = fileHandler;
    }

    public LibraryFileHandler getFileHandler() {
        return fileHandler;
    }

    public synchronized void addDataChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    private void notifyChangeListeners() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception ex) {
                System.err.println("Listener notification error: " + ex.getMessage());
            }
        }
    }

    // --- Policy Configuration Methods ---

    public synchronized int getDefaultBorrowDays() {
        return defaultBorrowDays;
    }

    public synchronized void setDefaultBorrowDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Borrow duration must be at least 1 day.");
        }
        this.defaultBorrowDays = days;
        notifyChangeListeners();
    }

    public synchronized double getFineRatePerDay() {
        return fineRatePerDay;
    }

    public synchronized void setFineRatePerDay(double rate) {
        if (rate < 0) {
            throw new IllegalArgumentException("Fine rate cannot be negative.");
        }
        this.fineRatePerDay = rate;
        notifyChangeListeners();
    }

    // --- Book Management Operations ---

    /**
     * Adds a new book to the library catalog.
     * Validates that the book ID is unique.
     * 
     * @param book Book to add
     * @throws IllegalArgumentException if duplicate or invalid
     */
    public synchronized void addBook(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null.");
        }
        String idKey = book.getId().trim().toUpperCase();
        if (books.containsKey(idKey)) {
            throw new IllegalArgumentException("Duplicate Book ID: Book with ID '" + book.getId() + "' already exists.");
        }
        books.put(idKey, book);
        saveChangesSafely();
        notifyChangeListeners();
    }

    /**
     * Updates an existing book record in the catalog.
     * 
     * @param updatedBook Book containing updated details
     */
    public synchronized void updateBook(Book updatedBook) {
        if (updatedBook == null) {
            throw new IllegalArgumentException("Book cannot be null.");
        }
        String idKey = updatedBook.getId().trim().toUpperCase();
        Book existing = books.get(idKey);
        if (existing == null) {
            throw new IllegalArgumentException("Book not found with ID: " + updatedBook.getId());
        }
        // Preserve current availability status
        updatedBook.setAvailable(existing.isAvailable());
        books.put(idKey, updatedBook);
        saveChangesSafely();
        notifyChangeListeners();
    }

    /**
     * Deletes a book from the catalog.
     * Prevents deletion if the book is currently on loan.
     * 
     * @param bookId ID of book to delete
     */
    public synchronized void deleteBook(String bookId) {
        if (bookId == null || bookId.trim().isEmpty()) {
            throw new IllegalArgumentException("Book ID must be provided.");
        }
        String idKey = bookId.trim().toUpperCase();
        if (!books.containsKey(idKey)) {
            throw new IllegalArgumentException("Book not found with ID: " + bookId);
        }
        if (activeIssuedBookIds.contains(idKey) || !books.get(idKey).isAvailable()) {
            throw new IllegalStateException("Cannot delete Book [" + bookId + "] because it is currently issued on loan.");
        }
        books.remove(idKey);
        saveChangesSafely();
        notifyChangeListeners();
    }

    public synchronized Optional<Book> getBookById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(books.get(id.trim().toUpperCase()));
    }

    public synchronized List<Book> getAllBooks() {
        return new ArrayList<>(books.values());
    }

    /**
     * Searches books by title, author, category, or ID.
     */
    public synchronized List<Book> searchBooks(String keyword, String category) {
        return books.values().stream()
                .filter(b -> b.matchesSearch(keyword, category))
                .collect(Collectors.toList());
    }

    // --- Student Management Operations ---

    /**
     * Registers a new student in the library.
     * Validates that the student ID is unique.
     * 
     * @param student Student to register
     * @throws IllegalArgumentException if duplicate or invalid
     */
    public synchronized void registerStudent(Student student) {
        if (student == null) {
            throw new IllegalArgumentException("Student cannot be null.");
        }
        String idKey = student.getId().trim().toUpperCase();
        if (students.containsKey(idKey)) {
            throw new IllegalArgumentException("Duplicate Student ID: Student with ID '" + student.getId() + "' already exists.");
        }
        students.put(idKey, student);
        saveChangesSafely();
        notifyChangeListeners();
    }

    /**
     * Updates an existing student's record.
     * 
     * @param updatedStudent Student with updated details
     */
    public synchronized void updateStudent(Student updatedStudent) {
        if (updatedStudent == null) {
            throw new IllegalArgumentException("Student cannot be null.");
        }
        String idKey = updatedStudent.getId().trim().toUpperCase();
        if (!students.containsKey(idKey)) {
            throw new IllegalArgumentException("Student not found with ID: " + updatedStudent.getId());
        }
        students.put(idKey, updatedStudent);
        saveChangesSafely();
        notifyChangeListeners();
    }

    /**
     * Deletes a student from the library system.
     * Prevents deletion if the student currently has books on loan.
     * 
     * @param studentId ID of student to delete
     */
    public synchronized void deleteStudent(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID must be provided.");
        }
        String idKey = studentId.trim().toUpperCase();
        if (!students.containsKey(idKey)) {
            throw new IllegalArgumentException("Student not found with ID: " + studentId);
        }
        boolean hasActiveLoans = loans.stream()
                .anyMatch(l -> !l.isReturned() && l.getStudentId().equalsIgnoreCase(idKey));
        if (hasActiveLoans) {
            throw new IllegalStateException("Cannot delete student [" + studentId + "] who currently has unreturned books.");
        }
        students.remove(idKey);
        saveChangesSafely();
        notifyChangeListeners();
    }

    public synchronized Optional<Student> getStudentById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(students.get(id.trim().toUpperCase()));
    }

    public synchronized List<Student> getAllStudents() {
        return new ArrayList<>(students.values());
    }

    public synchronized List<Student> searchStudents(String query) {
        return students.values().stream()
                .filter(s -> s.matchesSearch(query))
                .collect(Collectors.toList());
    }

    // --- Issue and Return Operations ---

    /**
     * Issues a book to a student according to library business rules.
     * 
     * @param studentId ID of registered student
     * @param bookId ID of book to issue
     * @param issueDate Date of issue (null defaults to today)
     * @return Generated LoanRecord
     * @throws BookUnavailableException if book is unavailable, non-circulating, or missing
     * @throws IllegalArgumentException if student is missing or arguments invalid
     */
    public synchronized LoanRecord issueBook(String studentId, String bookId, LocalDate issueDate) 
            throws BookUnavailableException {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID must be provided.");
        }
        if (bookId == null || bookId.trim().isEmpty()) {
            throw new IllegalArgumentException("Book ID must be provided.");
        }

        String normalizedStudentId = studentId.trim().toUpperCase();
        String normalizedBookId = bookId.trim().toUpperCase();

        // 1. Verify Student Existence
        Student student = students.get(normalizedStudentId);
        if (student == null) {
            throw new IllegalArgumentException("Student not registered: '" + studentId + "'. Please register the student first.");
        }

        // 2. Verify Book Existence
        Book book = books.get(normalizedBookId);
        if (book == null) {
            throw new BookUnavailableException(bookId, "Book does not exist in library catalog.");
        }

        // 3. Verify Circulation Policy (Reference books cannot be issued)
        if (!book.canBeBorrowed()) {
            if (book instanceof ReferenceBook) {
                throw new BookUnavailableException(bookId, "Reference books are strictly non-circulating and cannot be checked out.");
            } else {
                throw new BookUnavailableException(bookId, "Book is currently marked as unavailable/already issued.");
            }
        }

        // 4. Verify Active Loan Conflict
        if (activeIssuedBookIds.contains(normalizedBookId) || !book.isAvailable()) {
            throw new BookUnavailableException(bookId, "Book is already issued under an active loan.");
        }

        // 5. Construct Loan Record
        LocalDate actualIssueDate = (issueDate != null) ? issueDate : LocalDate.now();
        String loanId = "LN-" + (loans.size() + 1001);
        LoanRecord loan = new LoanRecord(loanId, normalizedStudentId, book.getId(), actualIssueDate, defaultBorrowDays);

        // 6. Update State Atomically
        book.setAvailable(false);
        activeIssuedBookIds.add(normalizedBookId);
        loans.add(loan);

        saveChangesSafely();
        notifyChangeListeners();
        return loan;
    }

    /**
     * Returns an issued book, marks loan completed, and calculates any overdue fine.
     * 
     * @param bookId ID of returning book
     * @param returnDate Actual date of return (null defaults to today)
     * @return Completed LoanRecord with assessed fine
     * @throws IllegalArgumentException if no active loan exists for the book
     */
    public synchronized LoanRecord returnBook(String bookId, LocalDate returnDate) {
        if (bookId == null || bookId.trim().isEmpty()) {
            throw new IllegalArgumentException("Book ID must be provided.");
        }

        String normalizedBookId = bookId.trim().toUpperCase();

        // Find active loan for this book
        LoanRecord activeLoan = loans.stream()
                .filter(l -> !l.isReturned() && l.getBookId().equalsIgnoreCase(normalizedBookId))
                .findFirst()
                .orElse(null);

        if (activeLoan == null) {
            throw new IllegalArgumentException("No active borrowing record found for Book ID: '" + bookId + "'.");
        }

        LocalDate actualReturnDate = (returnDate != null) ? returnDate : LocalDate.now();
        activeLoan.markReturned(actualReturnDate, fineRatePerDay);

        // Update book availability
        Book book = books.get(normalizedBookId);
        if (book != null) {
            book.setAvailable(true);
        }
        activeIssuedBookIds.remove(normalizedBookId);

        saveChangesSafely();
        notifyChangeListeners();
        return activeLoan;
    }

    // --- Query and History Methods ---

    public synchronized List<LoanRecord> getAllLoans() {
        return new ArrayList<>(loans);
    }

    public synchronized List<LoanRecord> getActiveLoans() {
        return loans.stream()
                .filter(l -> !l.isReturned())
                .collect(Collectors.toList());
    }

    public synchronized List<LoanRecord> getOverdueLoans(LocalDate asOfDate) {
        LocalDate checkDate = (asOfDate != null) ? asOfDate : LocalDate.now();
        return loans.stream()
                .filter(l -> !l.isReturned() && l.isOverdue(checkDate))
                .collect(Collectors.toList());
    }

    public synchronized List<LoanRecord> getLoansForStudent(String studentId) {
        if (studentId == null) return Collections.emptyList();
        String sid = studentId.trim().toUpperCase();
        return loans.stream()
                .filter(l -> l.getStudentId().equalsIgnoreCase(sid))
                .collect(Collectors.toList());
    }

    public synchronized double calculateTotalOutstandingFines(LocalDate asOfDate) {
        LocalDate checkDate = (asOfDate != null) ? asOfDate : LocalDate.now();
        return loans.stream()
                .filter(l -> !l.isReturned())
                .mapToDouble(l -> l.calculateFine(checkDate, fineRatePerDay))
                .sum();
    }

    // --- Internal State Restoration / Initialization ---

    public synchronized void loadInitialData(List<Book> loadedBooks, 
                                            List<Student> loadedStudents, 
                                            List<LoanRecord> loadedLoans) {
        books.clear();
        students.clear();
        loans.clear();
        activeIssuedBookIds.clear();

        if (loadedBooks != null) {
            for (Book b : loadedBooks) {
                books.put(b.getId().toUpperCase(), b);
            }
        }
        if (loadedStudents != null) {
            for (Student s : loadedStudents) {
                students.put(s.getId().toUpperCase(), s);
            }
        }
        if (loadedLoans != null) {
            for (LoanRecord l : loadedLoans) {
                loans.add(l);
                if (!l.isReturned()) {
                    activeIssuedBookIds.add(l.getBookId().toUpperCase());
                    Book b = books.get(l.getBookId().toUpperCase());
                    if (b != null) {
                        b.setAvailable(false);
                    }
                }
            }
        }
    }

    private void saveChangesSafely() {
        if (fileHandler != null) {
            try {
                fileHandler.saveAll(getAllBooks(), getAllStudents(), getAllLoans());
            } catch (Exception e) {
                System.err.println("Persistence warning: " + e.getMessage());
            }
        }
    }
}
