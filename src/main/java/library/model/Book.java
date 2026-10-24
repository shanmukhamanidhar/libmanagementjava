package library.model;

/**
 * 24CS202 Unit II Concept: Inheritance, 'super' Keyword, and Method Overriding
 * 
 * Member 1 Responsibility: Book Domain Entity
 * 
 * Represents a standard circulating book in the library catalog.
 * Inherits id, title, and availability tracking from LibraryItem.
 */
public class Book extends LibraryItem {

    private String author;
    private String category;
    private int publicationYear;

    /**
     * Parameterized Constructor.
     * Demonstrates: Using 'super(id, title)' to initialize inherited state,
     * followed by validating and initializing subclass-specific fields.
     * 
     * @param id Unique book ID (e.g. "B-101")
     * @param title Title of the book
     * @param author Author of the book
     * @param category Classification category (e.g. "Computer Science", "Fiction")
     * @param publicationYear Year of publication
     */
    public Book(String id, String title, String author, String category, int publicationYear) {
        super(id, title);
        setAuthor(author);
        setCategory(category);
        setPublicationYear(publicationYear);
    }

    /**
     * Overloaded Constructor with default category and current year fallback.
     * Demonstrates: Constructor overloading within the subclass.
     * 
     * @param id Unique book ID
     * @param title Title of the book
     * @param author Author of the book
     */
    public Book(String id, String title, String author) {
        this(id, title, author, "General", 2024);
    }

    // 24CS202 Unit II Concept: Method Overriding & Dynamic Method Dispatch
    @Override
    public String getItemType() {
        return "Standard Book";
    }

    // Standard books can be borrowed if they are currently marked available
    @Override
    public boolean canBeBorrowed() {
        return isAvailable();
    }

    // --- Encapsulation: Getters and Setters with Validation ---

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        if (author == null || author.trim().isEmpty()) {
            throw new IllegalArgumentException("Author cannot be null or empty.");
        }
        this.author = author.trim();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            this.category = "General";
        } else {
            this.category = category.trim();
        }
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(int publicationYear) {
        if (publicationYear < 1400 || publicationYear > 2100) {
            throw new IllegalArgumentException("Invalid publication year: " + publicationYear);
        }
        this.publicationYear = publicationYear;
    }

    // 24CS202 Unit II Concept: Method Overloading
    /**
     * Search match check against ID, title, author, or category.
     * 
     * @param keyword Search term
     * @return true if any field contains keyword (case-insensitive)
     */
    public boolean matchesSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return true;
        String q = keyword.trim().toLowerCase();
        return getId().toLowerCase().contains(q) ||
               getTitle().toLowerCase().contains(q) ||
               author.toLowerCase().contains(q) ||
               category.toLowerCase().contains(q);
    }

    /**
     * Overloaded search match check with category filter.
     * 
     * @param keyword Search term
     * @param filterCategory Category filter (or "All")
     * @return true if both match
     */
    public boolean matchesSearch(String keyword, String filterCategory) {
        boolean matchKeyword = matchesSearch(keyword);
        if (filterCategory == null || filterCategory.equalsIgnoreCase("All")) {
            return matchKeyword;
        }
        return matchKeyword && this.category.equalsIgnoreCase(filterCategory);
    }

    // 24CS202 Unit III Concept: Implementing Interface Methods
    @Override
    public String toReportRow() {
        return String.format("%-8s | %-30s | %-20s | %-15s | %-4d | %-10s",
                getId(), getTitle(), author, category, publicationYear, 
                isAvailable() ? "AVAILABLE" : "ISSUED");
    }

    @Override
    public String[] getReportHeaders() {
        return new String[] { "Book ID", "Title", "Author", "Category", "Year", "Status" };
    }

    @Override
    public String toString() {
        return String.format("Book[id=%s, title='%s', author='%s', category='%s', year=%d, available=%s]",
                getId(), getTitle(), author, category, publicationYear, isAvailable());
    }
}
