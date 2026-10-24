package library.model;

import library.interfaces.Identifiable;
import library.interfaces.Reportable;
import java.util.Objects;

/**
 * 24CS202 Unit II Concept: Abstract Classes, Encapsulation, and Constructors
 * 
 * Member 1 Responsibility: Domain Model Foundation
 * 
 * LibraryItem serves as the abstract root class of all items kept in the library catalog.
 * It cannot be instantiated directly with 'new LibraryItem()'. Subclasses like Book
 * and ReferenceBook must provide concrete implementations of abstract methods.
 */
public abstract class LibraryItem implements Identifiable, Reportable {

    // 24CS202 Unit II Concept: Static Member
    // Shared across all instances of LibraryItem to track total items cataloged.
    private static int totalItemsCreated = 0;

    // 24CS202 Unit II Concept: 'final' instance variable
    // The identifier is immutable once assigned in the constructor.
    private final String id;

    // Encapsulated properties
    private String title;
    private boolean available;

    /**
     * Parameterized Constructor.
     * Demonstrates: Parameter validation, 'this' keyword to resolve shadowing,
     * and incrementing a static member.
     * 
     * @param id Unique item identifier
     * @param title Title of the library item
     */
    public LibraryItem(String id, String title) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Item ID cannot be null or empty.");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty.");
        }
        this.id = id.trim();
        this.title = title.trim();
        this.available = true; // Default state on acquisition

        // Increment static item counter
        synchronized (LibraryItem.class) {
            totalItemsCreated++;
        }
    }

    /**
     * 24CS202 Unit II Concept: Constructor Overloading & Constructor Chaining
     * Demonstrates using 'this(id, title)' to invoke the main constructor.
     * 
     * @param id Unique item identifier
     */
    public LibraryItem(String id) {
        this(id, "Untitled Item");
    }

    // 24CS202 Unit II Concept: Abstract Method
    // Forces every subclass to define its own specific item type designation.
    public abstract String getItemType();

    // 24CS202 Unit II Concept: Abstract Method for borrowing rule check
    public abstract boolean canBeBorrowed();

    // --- Encapsulation: Getters and Setters ---

    @Override
    public String getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty.");
        }
        this.title = title.trim();
    }

    public boolean isAvailable() {
        return this.available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    /**
     * Static method to retrieve the count of all items constructed in memory.
     * 
     * @return Total items created.
     */
    public static int getTotalItemsCreated() {
        return totalItemsCreated;
    }

    @Override
    public String getDisplayLabel() {
        return String.format("[%s] %s (%s)", id, title, getItemType());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibraryItem that = (LibraryItem) o;
        return Objects.equals(id.toUpperCase(), that.id.toUpperCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id.toUpperCase());
    }

    @Override
    public String toString() {
        return String.format("LibraryItem{id='%s', title='%s', type='%s', available=%s}",
                id, title, getItemType(), available);
    }
}
