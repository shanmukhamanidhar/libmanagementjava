package library.notifications;

import library.model.LoanRecord;
import library.operations.LibraryManager;

import javax.swing.SwingUtilities;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 24CS202 Unit IV Concept: Java Multithreading & Synchronization
 * 
 * Member 3 Responsibility: Background Concurrency Subsystem
 * 
 * Demonstrates:
 * 1. Creating and starting a worker thread (extends Thread).
 * 2. Thread lifecycle methods (start, isAlive, join, interrupt).
 * 3. Thread priority configuration (scheduling hints).
 * 4. Safe shared state access with synchronization.
 * 5. Swing Event Dispatch Thread (EDT) safety via SwingUtilities.invokeLater.
 */
public class NotificationThread extends Thread {

    // Thread-safe shared notification log buffer accessible to both Swing UI and Web Browser
    private static final List<String> notificationHistory = Collections.synchronizedList(new ArrayList<>());

    private final LibraryManager manager;
    private final Consumer<String> uiNotificationCallback;
    private final int intervalSeconds;
    private volatile boolean running = true;

    /**
     * Parameterized Constructor.
     * 
     * @param manager Business logic coordinator
     * @param uiNotificationCallback Safe callback to post notifications to Swing UI
     * @param intervalSeconds How frequently the thread wakes up to scan records
     */
    public NotificationThread(LibraryManager manager, Consumer<String> uiNotificationCallback, int intervalSeconds) {
        super("LibraryNotificationWorkerThread"); // Give thread an identifiable name
        this.manager = manager;
        this.uiNotificationCallback = uiNotificationCallback;
        this.intervalSeconds = Math.max(5, intervalSeconds);

        // 24CS202 Unit IV: Thread Priority
        // Priorities range from Thread.MIN_PRIORITY (1) to Thread.MAX_PRIORITY (10).
        // NORM_PRIORITY is 5. We give this background monitor MIN_PRIORITY so it does
        // not contend with user interface responsiveness.
        this.setPriority(Thread.MIN_PRIORITY);

        // Daemon thread ensures JVM shuts down smoothly when UI closes
        this.setDaemon(true);
    }

    /**
     * Retrieves a snapshot copy of all recorded notification messages.
     */
    public static List<String> getNotificationHistory() {
        synchronized (notificationHistory) {
            return new ArrayList<>(notificationHistory);
        }
    }

    /**
     * Clears the shared notification history.
     */
    public static void clearNotificationHistory() {
        notificationHistory.clear();
    }

    /**
     * Programmatically appends an event to the shared notification history.
     */
    public static void logMessage(String message) {
        notificationHistory.add(message);
    }

    /**
     * Thread run() execution loop.
     */
    @Override
    public void run() {
        dispatchMessage("[Background Service] Overdue Notification Monitor started on thread: " + getName());

        while (running && !isInterrupted()) {
            try {
                // Synchronized read of overdue loans
                checkOverdueLoans();

                // Sleep between scan cycles
                Thread.sleep(intervalSeconds * 1000L);
            } catch (InterruptedException e) {
                // Restore interrupted flag and exit gracefully
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                dispatchMessage("[Background Alert Error]: " + ex.getMessage());
            }
        }

        dispatchMessage("[Background Service] Overdue Notification Monitor terminated cleanly.");
    }

    /**
     * Scans for overdue loans and notifies via callback.
     */
    private void checkOverdueLoans() {
        LocalDate today = LocalDate.now();
        List<LoanRecord> overdues;

        // Safe synchronization when querying manager
        synchronized (manager) {
            overdues = manager.getOverdueLoans(today);
        }

        if (!overdues.isEmpty()) {
            String alert = String.format("[OVERDUE ALERT] %d book loan(s) currently overdue as of %s! Please check loan records.",
                    overdues.size(), today);
            dispatchMessage(alert);
        }
    }

    /**
     * Safely routes messages to both the shared buffer and the Swing Event Dispatch Thread (EDT).
     * 
     * 24CS202 Unit IV Rule:
     * Never touch Swing UI components from an arbitrary background thread!
     * Always delegate through SwingUtilities.invokeLater.
     */
    private void dispatchMessage(String message) {
        notificationHistory.add(message);
        if (uiNotificationCallback != null) {
            SwingUtilities.invokeLater(() -> uiNotificationCallback.accept(message));
        }
    }

    /**
     * Signals the thread to stop executing.
     */
    public void stopService() {
        this.running = false;
        this.interrupt();
    }
}
