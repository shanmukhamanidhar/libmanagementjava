package library.app;

import library.model.Book;
import library.model.LoanRecord;
import library.model.ReferenceBook;
import library.model.Student;
import library.operations.LibraryManager;
import library.storage.LibraryFileHandler;
import library.ui.LibraryMainFrame;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * 24CS202 Main Application Bootstrap & Entry Point
 * 
 * Coordinates:
 * 1. Storage handler initialization and text file loading.
 * 2. Seeding default academic catalog data if starting with empty files.
 * 3. Business logic initialization (LibraryManager).
 * 4. Embedded Standard Java HTTP Server at http://localhost:8080.
 * 5. Swing Event Dispatch Thread (EDT) startup of LibraryMainFrame.
 * 6. Graceful JVM shutdown hook ensuring final data persistence.
 */
public class LibraryManagementSystem {

    public static void main(String[] args) {
        System.out.println("========================================================================");
        System.out.println("  Starting 24CS202 Library Management System (JDK 23 / Swing Desktop)   ");
        System.out.println("========================================================================");

        // Determine data directory (default to "data" in current working directory)
        String dataDirPath = System.getProperty("library.data.dir", "data");
        File dataDir = new File(dataDirPath);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        LibraryFileHandler fileHandler = new LibraryFileHandler(dataDir.getAbsolutePath());
        LibraryManager manager = new LibraryManager(fileHandler);

        // 1. Load existing records from text files
        try {
            List<Book> books = fileHandler.loadBooks();
            List<Student> students = fileHandler.loadStudents();
            List<LoanRecord> loans = fileHandler.loadLoans();

            // If empty, seed realistic college sample records for immediate testing/viva demonstration
            if (books.isEmpty() && students.isEmpty()) {
                System.out.println("No existing text records found. Seeding initial college library dataset...");
                seedInitialData(manager);
            } else {
                manager.loadInitialData(books, students, loans);
                System.out.printf("Loaded from text files: %d books, %d students, %d loans.\n",
                        books.size(), students.size(), loans.size());
            }
        } catch (IOException e) {
            System.err.println("Warning: Error loading records from text files: " + e.getMessage());
        }

        // 2. Start Embedded Standard Java Localhost HTTP Server (port 8080)
        LibraryHttpServer httpServer = null;
        try {
            httpServer = new LibraryHttpServer(manager, 8080);
            httpServer.start();
        } catch (Exception ex) {
            System.err.println("Notice: Could not bind localhost on 8080: " + ex.getMessage());
            try {
                httpServer = new LibraryHttpServer(manager, 8085);
                httpServer.start();
            } catch (Exception ignored) {}
        }
        final LibraryHttpServer activeServer = httpServer;

        // 3. Register JVM Shutdown Hook for safety
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("\n[Shutdown Hook] Stopping localhost HTTP server...");
                if (activeServer != null) {
                    activeServer.stop();
                }
                System.out.println("[Shutdown Hook] Persisting all records to text files before exit...");
                fileHandler.saveAll(manager.getAllBooks(), manager.getAllStudents(), manager.getAllLoans());
                System.out.println("[Shutdown Hook] Persistence completed successfully.");
            } catch (Exception ex) {
                System.err.println("[Shutdown Hook Error]: " + ex.getMessage());
            }
        }, "LibraryShutdownPersistenceHook"));

        // 4. Set Look and Feel for modern appearance
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        // 5. Launch Swing GUI on Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                LibraryMainFrame mainFrame = new LibraryMainFrame(manager);
                mainFrame.setVisible(true);
                System.out.println("Library Management System GUI active and ready for user interaction.");
                System.out.println(">>> Localhost Web Portal is also LIVE at: http://localhost:8080/ <<<");
            } catch (Exception ex) {
                System.err.println("Fatal error initializing Swing GUI: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    private static void seedInitialData(LibraryManager manager) {
        manager.addBook(new Book("B-101", "Core Java: Fundamentals", "Cay S. Horstmann", "Computer Science", 2024));
        manager.addBook(new Book("B-102", "Java: The Complete Reference", "Herbert Schildt", "Computer Science", 2023));
        manager.addBook(new Book("B-103", "Effective Java", "Joshua Bloch", "Computer Science", 2018));
        manager.addBook(new Book("B-104", "Data Structures & Algorithms in Java", "Robert Lafore", "Computer Science", 2021));
        manager.addBook(new Book("B-105", "Operating System Concepts", "Abraham Silberschatz", "Computer Science", 2020));
        manager.addBook(new Book("B-106", "Introduction to Electrodynamics", "David J. Griffiths", "Physics", 2017));
        manager.addBook(new Book("B-107", "Higher Engineering Mathematics", "B.S. Grewal", "Mathematics", 2022));

        manager.addBook(new ReferenceBook("REF-201", "Oxford Advanced Learner's Dictionary", "A.S. Hornby", "General", 2023, "Reading Room Section R-1"));
        manager.addBook(new ReferenceBook("REF-202", "Encyclopaedia Britannica: Science & Tech", "Britannica Editors", "Science", 2022, "Reading Room Section R-2"));

        manager.registerStudent(new Student("24CS001", "Aarav Sharma", "CSE", "aarav.s@college.edu", "9876543210"));
        manager.registerStudent(new Student("24CS002", "Priya Venkatesh", "CSE", "priya.v@college.edu", "9876543211"));
        manager.registerStudent(new Student("24IT015", "Rohan Verma", "IT", "rohan.v@college.edu", "9876543212"));
        manager.registerStudent(new Student("24EC032", "Ananya Reddy", "ECE", "ananya.r@college.edu", "9876543213"));

        try {
            manager.issueBook("24CS001", "B-101", LocalDate.now().minusDays(2));
            manager.issueBook("24CS002", "B-102", LocalDate.now().minusDays(20));
        } catch (Exception e) {
            System.err.println("Seeding loan notice: " + e.getMessage());
        }
    }
}
