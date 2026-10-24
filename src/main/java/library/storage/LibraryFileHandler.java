package library.storage;

import library.model.Book;
import library.model.LoanRecord;
import library.model.ReferenceBook;
import library.model.Student;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 24CS202 Unit III Concept: Java I/O Streams, Character Streams & Byte Streams
 * 
 * Member 3 Responsibility: Persistence Subsystem
 * 
 * Manages loading and saving domain records to delimited text files using character
 * streams (BufferedReader/BufferedWriter, FileReader/FileWriter). Also demonstrates
 * raw byte streams (FileInputStream/FileOutputStream) through an automated binary backup routine.
 */
public class LibraryFileHandler {

    private final File dataDirectory;
    private final File booksFile;
    private final File studentsFile;
    private final File loansFile;

    // Field delimiter for text file persistence
    private static final String DELIMITER = ";";

    /**
     * Constructor specifying the base data directory.
     * 
     * @param dataDirPath Directory path where text files reside (e.g. "./data")
     */
    public LibraryFileHandler(String dataDirPath) {
        this.dataDirectory = new File(dataDirPath != null ? dataDirPath : "data");
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }
        this.booksFile = new File(dataDirectory, "books.txt");
        this.studentsFile = new File(dataDirectory, "students.txt");
        this.loansFile = new File(dataDirectory, "loans.txt");
    }

    public File getDataDirectory() {
        return dataDirectory;
    }

    // =========================================================================
    // 24CS202 Unit III: CHARACTER STREAMS (BufferedWriter, FileWriter, try-with-resources)
    // =========================================================================

    /**
     * Persists all library data to respective text files.
     * 
     * @param books List of all books
     * @param students List of all students
     * @param loans List of all loans
     * @throws IOException on file write error
     */
    public synchronized void saveAll(List<Book> books, List<Student> students, List<LoanRecord> loans) 
            throws IOException {
        saveBooks(books);
        saveStudents(students);
        saveLoans(loans);
    }

    /**
     * Saves books using Character Stream (BufferedWriter with FileWriter).
     */
    public synchronized void saveBooks(List<Book> books) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(booksFile), StandardCharsets.UTF_8))) {
            writer.write("# BOOK RECORDS (Format: TYPE;ID;TITLE;AUTHOR;CATEGORY;YEAR;AVAILABLE;EXTRA)");
            writer.newLine();
            for (Book b : books) {
                String type = (b instanceof ReferenceBook) ? "REF" : "STANDARD";
                String extra = (b instanceof ReferenceBook) ? ((ReferenceBook) b).getReadingRoomSection() : "N/A";
                String line = String.join(DELIMITER,
                        type,
                        b.getId(),
                        escapeField(b.getTitle()),
                        escapeField(b.getAuthor()),
                        escapeField(b.getCategory()),
                        String.valueOf(b.getPublicationYear()),
                        String.valueOf(b.isAvailable()),
                        escapeField(extra)
                );
                writer.write(line);
                writer.newLine();
            }
        }
    }

    /**
     * Saves students using Character Stream.
     */
    public synchronized void saveStudents(List<Student> students) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(studentsFile), StandardCharsets.UTF_8))) {
            writer.write("# STUDENT RECORDS (Format: ID;NAME;DEPARTMENT;EMAIL;PHONE)");
            writer.newLine();
            for (Student s : students) {
                String line = String.join(DELIMITER,
                        s.getId(),
                        escapeField(s.getName()),
                        escapeField(s.getDepartment()),
                        escapeField(s.getEmail()),
                        escapeField(s.getContactNumber())
                );
                writer.write(line);
                writer.newLine();
            }
        }
    }

    /**
     * Saves loans using Character Stream.
     */
    public synchronized void saveLoans(List<LoanRecord> loans) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(loansFile), StandardCharsets.UTF_8))) {
            writer.write("# LOAN RECORDS (Format: LOAN_ID;STUDENT_ID;BOOK_ID;ISSUE_DATE;DUE_DATE;RETURN_DATE;RETURNED;FINE)");
            writer.newLine();
            for (LoanRecord l : loans) {
                String retDateStr = (l.getReturnDate() != null) ? l.getReturnDate().toString() : "NULL";
                String line = String.join(DELIMITER,
                        l.getId(),
                        l.getStudentId(),
                        l.getBookId(),
                        l.getIssueDate().toString(),
                        l.getDueDate().toString(),
                        retDateStr,
                        String.valueOf(l.isReturned()),
                        String.format("%.2f", l.getFineAmount())
                );
                writer.write(line);
                writer.newLine();
            }
        }
    }

    // =========================================================================
    // 24CS202 Unit III: CHARACTER STREAMS (BufferedReader, FileReader)
    // =========================================================================

    /**
     * Loads books from books.txt. Returns empty list if file doesn't exist yet.
     */
    public synchronized List<Book> loadBooks() throws IOException {
        List<Book> list = new ArrayList<>();
        if (!booksFile.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(booksFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(DELIMITER, -1);
                if (parts.length < 7) continue;

                String type = parts[0].trim();
                String id = parts[1].trim();
                String title = unescapeField(parts[2].trim());
                String author = unescapeField(parts[3].trim());
                String category = unescapeField(parts[4].trim());
                int year = Integer.parseInt(parts[5].trim());
                boolean available = Boolean.parseBoolean(parts[6].trim());

                Book book;
                if ("REF".equalsIgnoreCase(type)) {
                    String section = parts.length > 7 ? unescapeField(parts[7].trim()) : "Reference Room";
                    book = new ReferenceBook(id, title, author, category, year, section);
                } else {
                    book = new Book(id, title, author, category, year);
                }
                book.setAvailable(available);
                list.add(book);
            }
        }
        return list;
    }

    /**
     * Loads students from students.txt.
     */
    public synchronized List<Student> loadStudents() throws IOException {
        List<Student> list = new ArrayList<>();
        if (!studentsFile.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(studentsFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(DELIMITER, -1);
                if (parts.length < 5) continue;

                String id = parts[0].trim();
                String name = unescapeField(parts[1].trim());
                String dept = unescapeField(parts[2].trim());
                String email = unescapeField(parts[3].trim());
                String phone = unescapeField(parts[4].trim());

                list.add(new Student(id, name, dept, email, phone));
            }
        }
        return list;
    }

    /**
     * Loads loans from loans.txt.
     */
    public synchronized List<LoanRecord> loadLoans() throws IOException {
        List<LoanRecord> list = new ArrayList<>();
        if (!loansFile.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(loansFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(DELIMITER, -1);
                if (parts.length < 8) continue;

                try {
                    String loanId = parts[0].trim();
                    String studentId = parts[1].trim();
                    String bookId = parts[2].trim();
                    LocalDate issueDate = LocalDate.parse(parts[3].trim());
                    LocalDate dueDate = LocalDate.parse(parts[4].trim());
                    String retStr = parts[5].trim();
                    LocalDate returnDate = "NULL".equalsIgnoreCase(retStr) ? null : LocalDate.parse(retStr);
                    boolean returned = Boolean.parseBoolean(parts[6].trim());
                    double fine = Double.parseDouble(parts[7].trim());

                    list.add(new LoanRecord(loanId, studentId, bookId, issueDate, dueDate, returnDate, returned, fine));
                } catch (DateTimeParseException | NumberFormatException ex) {
                    System.err.println("Skipping malformed loan line: " + line + " (" + ex.getMessage() + ")");
                }
            }
        }
        return list;
    }

    // =========================================================================
    // 24CS202 Unit III: BYTE STREAMS (FileInputStream, FileOutputStream)
    // =========================================================================

    /**
     * Demonstrates Raw Byte Streams by performing an exact binary backup
     * of a library data file to an archive copy.
     * 
     * Unlike character streams which encode text characters, byte streams
     * read and write raw 8-bit octets, perfect for binary data, images, or bit-exact backups.
     * 
     * @param sourceFile File to back up
     * @param targetBackupFile Destination file
     * @return Number of total bytes transferred
     * @throws IOException on I/O failure
     */
    public long createBinaryBackup(File sourceFile, File targetBackupFile) throws IOException {
        if (!sourceFile.exists()) {
            throw new FileNotFoundException("Source file for backup does not exist: " + sourceFile.getPath());
        }

        long totalBytesRead = 0;
        // 24CS202 Unit III: try-with-resources with FileInputStream and FileOutputStream
        try (InputStream inStream = new FileInputStream(sourceFile);
             OutputStream outStream = new FileOutputStream(targetBackupFile)) {

            byte[] buffer = new byte[4096]; // 4KB chunk buffer
            int bytesRead;
            while ((bytesRead = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
            outStream.flush();
        }
        return totalBytesRead;
    }

    // --- Delimiter escaping helpers ---
    private static String escapeField(String val) {
        if (val == null) return "";
        return val.replace(";", ",");
    }

    private static String unescapeField(String val) {
        return val != null ? val : "";
    }
}
