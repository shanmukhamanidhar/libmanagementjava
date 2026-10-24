package library;

import library.model.Book;
import library.model.LoanRecord;
import library.model.ReferenceBook;
import library.model.Student;
import library.operations.BookUnavailableException;
import library.operations.LibraryManager;
import library.storage.LibraryFileHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 24CS202 Automated Unit & Integration Tests
 * 
 * Verifies core library business rules:
 * - Duplicate ID prevention for books and students
 * - Unavailable book check & custom BookUnavailableException
 * - Non-circulating ReferenceBook restriction
 * - Return processing & state restoration
 * - Overdue fine calculation accuracy
 * - File persistence and reload integrity across sessions
 */
public class LibraryOperationsTest {

    private LibraryManager manager;

    @BeforeEach
    public void setUp() {
        // Create an in-memory manager without file persistence for pure unit testing
        manager = new LibraryManager();
        manager.setDefaultBorrowDays(14);
        manager.setFineRatePerDay(5.0);

        // Seed base test data
        manager.addBook(new Book("B-001", "Introduction to Algorithms", "CLRS", "Computer Science", 2022));
        manager.addBook(new ReferenceBook("REF-001", "Oxford English Dictionary", "Hornby", "General", 2020, "Room 1"));
        manager.registerStudent(new Student("STU-001", "Test Student", "CSE", "test@univ.edu", "9999999999"));
    }

    @Test
    @DisplayName("Rule 1: Prevent duplicate Book and Student IDs")
    public void testDuplicateIdPrevention() {
        // Duplicate Book ID
        IllegalArgumentException bookEx = assertThrows(IllegalArgumentException.class, () -> {
            manager.addBook(new Book("B-001", "Duplicate Book", "Author", "Fiction", 2021));
        });
        assertTrue(bookEx.getMessage().contains("already exists"));

        // Duplicate Student ID
        IllegalArgumentException stuEx = assertThrows(IllegalArgumentException.class, () -> {
            manager.registerStudent(new Student("STU-001", "Duplicate Student", "ECE"));
        });
        assertTrue(stuEx.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Rule 2: Cannot issue an already issued (unavailable) book")
    public void testCannotIssueUnavailableBook() throws BookUnavailableException {
        // First issue should succeed
        LoanRecord loan1 = manager.issueBook("STU-001", "B-001", LocalDate.now());
        assertNotNull(loan1);
        assertFalse(manager.getBookById("B-001").get().isAvailable());

        // Register second student
        manager.registerStudent(new Student("STU-002", "Second Student", "IT"));

        // Second issue of same book must fail with BookUnavailableException
        BookUnavailableException bue = assertThrows(BookUnavailableException.class, () -> {
            manager.issueBook("STU-002", "B-001", LocalDate.now());
        });
        assertTrue(bue.getMessage().contains("already issued") || bue.getMessage().contains("unavailable"));
    }

    @Test
    @DisplayName("Rule 3: Reference books cannot be issued (Non-circulating)")
    public void testReferenceBookCannotBeIssued() {
        BookUnavailableException bue = assertThrows(BookUnavailableException.class, () -> {
            manager.issueBook("STU-001", "REF-001", LocalDate.now());
        });
        assertTrue(bue.getMessage().contains("Reference books are strictly non-circulating"));
    }

    @Test
    @DisplayName("Rule 4: Returning a book restores its availability")
    public void testReturnBookRestoresAvailability() throws BookUnavailableException {
        manager.issueBook("STU-001", "B-001", LocalDate.now());
        assertFalse(manager.getBookById("B-001").get().isAvailable());

        // Return book
        LoanRecord completed = manager.returnBook("B-001", LocalDate.now());
        assertTrue(completed.isReturned());
        assertTrue(manager.getBookById("B-001").get().isAvailable());
        assertEquals(0, manager.getActiveLoans().size());
    }

    @Test
    @DisplayName("Rule 5: Accurate fine calculation on and after due date")
    public void testFineCalculation() throws BookUnavailableException {
        LocalDate issueDate = LocalDate.now().minusDays(20);
        // Borrow period = 14 days, so due date was 6 days ago
        LoanRecord loan = manager.issueBook("STU-001", "B-001", issueDate);

        // 1. Current outstanding fine check: 6 days overdue * ₹5.00/day = ₹30.00
        double outstandingFine = loan.calculateFine(LocalDate.now(), 5.0);
        assertEquals(30.0, outstandingFine, 0.001);

        // 2. Return on time check (hypothetical return within 14 days)
        LocalDate onTimeReturn = issueDate.plusDays(10);
        LoanRecord testOnTimeLoan = new LoanRecord("L-TEST", "STU-001", "B-001", issueDate, 14);
        testOnTimeLoan.markReturned(onTimeReturn, 5.0);
        assertEquals(0.0, testOnTimeLoan.getFineAmount(), 0.001);

        // 3. Return late check
        LoanRecord returnedLoan = manager.returnBook("B-001", LocalDate.now());
        assertEquals(30.0, returnedLoan.getFineAmount(), 0.001);
    }

    @Test
    @DisplayName("Rule 6: Text-file persistence and recovery across sessions")
    public void testPersistenceAndReload(@TempDir Path tempDir) throws IOException, BookUnavailableException {
        File dataDir = tempDir.toFile();
        LibraryFileHandler handler1 = new LibraryFileHandler(dataDir.getAbsolutePath());
        LibraryManager session1 = new LibraryManager(handler1);

        // Seed session 1 data
        session1.addBook(new Book("BK-PERSIST", "Persist Test Book", "Author P", "Tech", 2023));
        session1.registerStudent(new Student("STU-PERSIST", "Persist Student", "CSE"));
        session1.issueBook("STU-PERSIST", "BK-PERSIST", LocalDate.now());

        // Verify files were created
        assertTrue(new File(dataDir, "books.txt").exists());
        assertTrue(new File(dataDir, "students.txt").exists());
        assertTrue(new File(dataDir, "loans.txt").exists());

        // Session 2: Read files back using a fresh file handler and manager
        LibraryFileHandler handler2 = new LibraryFileHandler(dataDir.getAbsolutePath());
        List<Book> loadedBooks = handler2.loadBooks();
        List<Student> loadedStudents = handler2.loadStudents();
        List<LoanRecord> loadedLoans = handler2.loadLoans();

        LibraryManager session2 = new LibraryManager(handler2);
        session2.loadInitialData(loadedBooks, loadedStudents, loadedLoans);

        assertEquals(1, session2.getAllBooks().size());
        assertEquals(1, session2.getAllStudents().size());
        assertEquals(1, session2.getActiveLoans().size());
        assertFalse(session2.getBookById("BK-PERSIST").get().isAvailable());
    }

    @Test
    @DisplayName("Rule 7: Edit and Delete operations with circulation constraints")
    public void testEditAndDeleteConstraints() throws BookUnavailableException {
        // Test updateBook
        Book updated = new Book("B-001", "Updated Algorithms Title", "CLRS", "Computer Science", 2024);
        manager.updateBook(updated);
        assertEquals("Updated Algorithms Title", manager.getBookById("B-001").get().getTitle());

        // Test updateStudent
        Student updatedStu = new Student("STU-001", "Updated Student Name", "IT", "updated@univ.edu", "8888888888");
        manager.updateStudent(updatedStu);
        assertEquals("Updated Student Name", manager.getStudentById("STU-001").get().getName());
        assertEquals("IT", manager.getStudentById("STU-001").get().getDepartment());

        // Issue book B-001
        manager.issueBook("STU-001", "B-001", LocalDate.now());

        // Attempting to delete B-001 while issued must throw IllegalStateException
        IllegalStateException bookDeleteEx = assertThrows(IllegalStateException.class, () -> {
            manager.deleteBook("B-001");
        });
        assertTrue(bookDeleteEx.getMessage().contains("currently issued"));

        // Attempting to delete STU-001 while holding active loan must throw IllegalStateException
        IllegalStateException stuDeleteEx = assertThrows(IllegalStateException.class, () -> {
            manager.deleteStudent("STU-001");
        });
        assertTrue(stuDeleteEx.getMessage().contains("unreturned books"));

        // Return book and now delete succeeds
        manager.returnBook("B-001", LocalDate.now());
        manager.deleteBook("B-001");
        assertTrue(manager.getBookById("B-001").isEmpty());
    }
}
