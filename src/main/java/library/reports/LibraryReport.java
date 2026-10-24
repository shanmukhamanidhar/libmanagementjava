package library.reports;

import library.model.Book;
import library.model.LoanRecord;
import library.model.Student;
import library.operations.LibraryManager;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 24CS202 Unit V Concept: Collections Framework & Stream API
 * 
 * Member 3 Responsibility: Reports and Statistical Analytics
 * 
 * Demonstrates intermediate stream operations (filter, map, sorted),
 * terminal operations (collect, reduce, count, sum), and parallel streams.
 */
public class LibraryReport {

    private final LibraryManager manager;

    public LibraryReport(LibraryManager manager) {
        this.manager = manager;
    }

    /**
     * Data Transfer Record representing summary dashboard metrics.
     */
    public static class DashboardStats {
        public final int totalBooks;
        public final int totalStudents;
        public final long availableBooks;
        public final long issuedBooks;
        public final long overdueLoans;
        public final double totalOutstandingFines;

        public DashboardStats(int totalBooks, int totalStudents, long availableBooks, 
                              long issuedBooks, long overdueLoans, double totalOutstandingFines) {
            this.totalBooks = totalBooks;
            this.totalStudents = totalStudents;
            this.availableBooks = availableBooks;
            this.issuedBooks = issuedBooks;
            this.overdueLoans = overdueLoans;
            this.totalOutstandingFines = totalOutstandingFines;
        }
    }

    /**
     * Computes real-time dashboard statistics using Stream API.
     */
    public DashboardStats getDashboardStats() {
        List<Book> books = manager.getAllBooks();
        List<Student> students = manager.getAllStudents();
        List<LoanRecord> activeLoans = manager.getActiveLoans();
        LocalDate today = LocalDate.now();

        // 24CS202 Unit V: Stream filtering and count terminal operation
        long availableCount = books.stream()
                .filter(Book::isAvailable)
                .count();

        long issuedCount = books.stream()
                .filter(b -> !b.isAvailable())
                .count();

        long overdueCount = activeLoans.stream()
                .filter(l -> l.isOverdue(today))
                .count();

        // 24CS202 Unit V: Stream mapToDouble and sum reduction
        double finesSum = activeLoans.stream()
                .mapToDouble(l -> l.calculateFine(today, manager.getFineRatePerDay()))
                .sum();

        return new DashboardStats(books.size(), students.size(), availableCount, issuedCount, overdueCount, finesSum);
    }

    /**
     * 24CS202 Unit V: Grouping and Counting with Streams
     * Returns count of books per category.
     */
    public Map<String, Long> getBookCountByCategory() {
        return manager.getAllBooks().stream()
                .collect(Collectors.groupingBy(Book::getCategory, Collectors.counting()));
    }

    /**
     * 24CS202 Unit V: Sorting with Comparator and Streams
     * Returns books sorted alphabetically by title.
     */
    public List<Book> getBooksSortedByTitle() {
        return manager.getAllBooks().stream()
                .sorted(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    /**
     * Returns books sorted by publication year (descending).
     */
    public List<Book> getBooksSortedByYear() {
        return manager.getAllBooks().stream()
                .sorted(Comparator.comparingInt(Book::getPublicationYear).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 24CS202 Unit V: Stream Reduction Operation
     * Calculates the average publication year of all cataloged books using reduce().
     */
    public double calculateAveragePublicationYear() {
        List<Book> books = manager.getAllBooks();
        if (books.isEmpty()) return 0.0;

        // Demonstrates stream reduce() with identity and accumulator
        int totalYears = books.stream()
                .map(Book::getPublicationYear)
                .reduce(0, Integer::sum);

        return (double) totalYears / books.size();
    }

    /**
     * 24CS202 Unit V: Parallel Stream Demonstration
     * 
     * Parallel streams partition the collection across available CPU cores using the
     * common ForkJoinPool. They are advantageous for large datasets with CPU-intensive
     * calculations, but incur thread orchestration overhead for small in-memory collections.
     * 
     * @return Simulated computational benchmark result comparing sequential vs parallel.
     */
    public String benchmarkParallelVsSequentialStream() {
        List<Book> allBooks = manager.getAllBooks();
        if (allBooks.isEmpty()) {
            return "Catalog is empty. Add books to run parallel stream benchmark.";
        }

        // Create a large synthetic test list of books based on current catalog
        List<Book> largeDataSet = new ArrayList<>();
        for (int i = 0; i < 200_000; i++) {
            Book sample = allBooks.get(i % allBooks.size());
            largeDataSet.add(sample);
        }

        // 1. Sequential Stream Benchmark
        long startSeq = System.currentTimeMillis();
        long seqCount = largeDataSet.stream()
                .filter(b -> b.getTitle().length() > 3)
                .map(Book::getCategory)
                .filter(c -> !c.isEmpty())
                .count();
        long timeSeq = System.currentTimeMillis() - startSeq;

        // 2. Parallel Stream Benchmark
        long startPar = System.currentTimeMillis();
        long parCount = largeDataSet.parallelStream()
                .filter(b -> b.getTitle().length() > 3)
                .map(Book::getCategory)
                .filter(c -> !c.isEmpty())
                .count();
        long timePar = System.currentTimeMillis() - startPar;

        return String.format(
                "Parallel Stream Benchmark (200,000 items processed):\n" +
                " - Sequential Stream Time: %d ms (Count: %d)\n" +
                " - Parallel Stream Time:   %d ms (Count: %d)\n" +
                "Note: Parallel streams distribute workloads across %d CPU cores.",
                timeSeq, seqCount, timePar, parCount, Runtime.getRuntime().availableProcessors()
        );
    }

    /**
     * Generates a comprehensive plain-text audit report of the entire library.
     */
    public String generateFullTextReport() {
        StringBuilder sb = new StringBuilder();
        DashboardStats stats = getDashboardStats();

        sb.append("========================================================================================\n");
        sb.append("                    COLLEGE LIBRARY SYSTEM - COMPREHENSIVE AUDIT REPORT                 \n");
        sb.append("========================================================================================\n");
        sb.append(String.format("Generated On: %s | Active Borrow Policy: %d Days | Overdue Rate: ₹%.2f/day\n\n",
                LocalDate.now(), manager.getDefaultBorrowDays(), manager.getFineRatePerDay()));

        sb.append("--- EXECUTIVE SUMMARY ---\n");
        sb.append(String.format("Total Cataloged Books:  %d\n", stats.totalBooks));
        sb.append(String.format("Total Students:         %d\n", stats.totalStudents));
        sb.append(String.format("Available Copies:       %d\n", stats.availableBooks));
        sb.append(String.format("Currently Issued:       %d\n", stats.issuedBooks));
        sb.append(String.format("Overdue Loans:          %d\n", stats.overdueLoans));
        sb.append(String.format("Outstanding Fines:      ₹%.2f\n\n", stats.totalOutstandingFines));

        sb.append("--- BOOKS BY CATEGORY ---\n");
        getBookCountByCategory().forEach((cat, count) -> 
                sb.append(String.format("  * %-20s : %d books\n", cat, count)));
        sb.append("\n");

        sb.append("--- CURRENTLY ACTIVE LOANS ---\n");
        List<LoanRecord> active = manager.getActiveLoans();
        if (active.isEmpty()) {
            sb.append("  (No active loans at this time)\n");
        } else {
            for (LoanRecord loan : active) {
                double currentFine = loan.calculateFine(LocalDate.now(), manager.getFineRatePerDay());
                String status = loan.isOverdue(LocalDate.now()) ? "OVERDUE" : "OK";
                sb.append(String.format("  * Loan: %-8s | Book: %-8s | Student: %-10s | Due: %s | Status: %-7s | Fine: ₹%.2f\n",
                        loan.getId(), loan.getBookId(), loan.getStudentId(), loan.getDueDate(), status, currentFine));
            }
        }
        sb.append("\n========================================================================================\n");
        return sb.toString();
    }
}
