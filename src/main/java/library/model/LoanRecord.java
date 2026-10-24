package library.model;

import library.interfaces.Identifiable;
import library.interfaces.Reportable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * 24CS202 Unit I & II Concept: Java Time API, State Encapsulation, and Business Logic
 * 
 * Shared Integration Model:
 * Connects a registered student to a borrowed book with date tracking and fine calculation.
 */
public class LoanRecord implements Identifiable, Reportable {

    private final String loanId;
    private final String studentId;
    private final String bookId;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private LocalDate returnDate;
    private boolean returned;
    private double fineAmount;

    /**
     * Standard Constructor for new loans.
     * 
     * @param loanId Unique loan identifier
     * @param studentId Student borrowing the book
     * @param bookId Book being issued
     * @param issueDate Date of issue
     * @param borrowDays Loan duration in days (default typically 14)
     */
    public LoanRecord(String loanId, String studentId, String bookId, LocalDate issueDate, int borrowDays) {
        if (loanId == null || loanId.trim().isEmpty()) {
            throw new IllegalArgumentException("Loan ID cannot be empty.");
        }
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be empty.");
        }
        if (bookId == null || bookId.trim().isEmpty()) {
            throw new IllegalArgumentException("Book ID cannot be empty.");
        }
        if (issueDate == null) {
            throw new IllegalArgumentException("Issue date cannot be null.");
        }
        if (borrowDays <= 0) {
            throw new IllegalArgumentException("Borrow duration must be positive.");
        }

        this.loanId = loanId.trim();
        this.studentId = studentId.trim().toUpperCase();
        this.bookId = bookId.trim();
        this.issueDate = issueDate;
        this.dueDate = issueDate.plusDays(borrowDays);
        this.returnDate = null;
        this.returned = false;
        this.fineAmount = 0.0;
    }

    /**
     * Constructor for restoring loan records from persistent storage.
     * 
     * @param loanId Loan ID
     * @param studentId Student ID
     * @param bookId Book ID
     * @param issueDate Date issued
     * @param dueDate Due date
     * @param returnDate Date returned (or null if active)
     * @param returned Whether return is completed
     * @param fineAmount Stored fine amount
     */
    public LoanRecord(String loanId, String studentId, String bookId, 
                      LocalDate issueDate, LocalDate dueDate, 
                      LocalDate returnDate, boolean returned, double fineAmount) {
        this.loanId = loanId;
        this.studentId = studentId;
        this.bookId = bookId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.returned = returned;
        this.fineAmount = fineAmount;
    }

    @Override
    public String getId() {
        return loanId;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getBookId() {
        return bookId;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public boolean isReturned() {
        return returned;
    }

    public double getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(double fineAmount) {
        this.fineAmount = fineAmount;
    }

    /**
     * Determines whether this loan is currently overdue.
     * An unreturned loan is overdue if today is after the due date.
     * A returned loan is overdue if returnDate was after dueDate.
     * 
     * @param currentDate Current reference date (usually LocalDate.now())
     * @return true if overdue
     */
    public boolean isOverdue(LocalDate currentDate) {
        if (returned) {
            return returnDate != null && returnDate.isAfter(dueDate);
        } else {
            return currentDate.isAfter(dueDate);
        }
    }

    /**
     * Calculates the overdue fine based on configured fine rate per day.
     * If returned on or before due date, fine is 0.
     * 
     * @param asOfDate Calculation target date (usually LocalDate.now() or returnDate)
     * @param finePerDay Fine rate in Rupees per day (e.g. 5.0)
     * @return Outstanding or assessed fine amount
     */
    public double calculateFine(LocalDate asOfDate, double finePerDay) {
        LocalDate effectiveEndDate = returned ? returnDate : asOfDate;
        if (effectiveEndDate == null || !effectiveEndDate.isAfter(dueDate)) {
            return 0.0;
        }
        long overdueDays = ChronoUnit.DAYS.between(dueDate, effectiveEndDate);
        return Math.max(0, overdueDays * finePerDay);
    }

    /**
     * Completes the book return operation.
     * 
     * @param actualReturnDate Date returned
     * @param fineRate Daily fine rate
     * @return Calculated fine for this return
     */
    public double markReturned(LocalDate actualReturnDate, double fineRate) {
        if (this.returned) {
            throw new IllegalStateException("Loan " + loanId + " has already been returned.");
        }
        this.returnDate = (actualReturnDate != null) ? actualReturnDate : LocalDate.now();
        this.returned = true;
        this.fineAmount = calculateFine(this.returnDate, fineRate);
        return this.fineAmount;
    }

    @Override
    public String toReportRow() {
        return String.format("%-10s | %-10s | %-8s | %-11s | %-11s | %-11s | %-8s | ₹%-6.2f",
                loanId, studentId, bookId, issueDate, dueDate,
                (returnDate != null ? returnDate.toString() : "ACTIVE"),
                (returned ? "RETURNED" : "BORROWED"), fineAmount);
    }

    @Override
    public String[] getReportHeaders() {
        return new String[] { "Loan ID", "Student ID", "Book ID", "Issue Date", "Due Date", "Return Date", "Status", "Fine" };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanRecord that = (LoanRecord) o;
        return Objects.equals(loanId, that.loanId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanId);
    }

    @Override
    public String toString() {
        return String.format("LoanRecord[id=%s, student=%s, book=%s, issue=%s, due=%s, returned=%s, fine=₹%.2f]",
                loanId, studentId, bookId, issueDate, dueDate, returned, fineAmount);
    }
}
