package library.model;

/**
 * 24CS202 Unit II Concept: Multilevel Inheritance
 * 
 * Inheritance Hierarchy:
 * LibraryItem (Grandparent: Abstract)
 *    └── Book (Parent: Concrete)
 *          └── ReferenceBook (Child: Specialized)
 * 
 * Business Rule:
 * Reference books are cataloged and searchable, but strictly non-circulating.
 * They cannot be issued for home lending.
 */
public class ReferenceBook extends Book {

    private String readingRoomSection;

    /**
     * Parameterized Constructor for ReferenceBook.
     * Demonstrates: Multilevel constructor chaining using 'super(...)'.
     * 
     * @param id Unique reference book ID (e.g. "REF-501")
     * @param title Title of the reference work (e.g. Encyclopedia, Dictionary)
     * @param author Author / Editor
     * @param category Subject category
     * @param publicationYear Publication year
     * @param readingRoomSection Designated reading room or shelf rack (e.g. "Section R3")
     */
    public ReferenceBook(String id, String title, String author, String category, 
                         int publicationYear, String readingRoomSection) {
        super(id, title, author, category, publicationYear);
        this.readingRoomSection = (readingRoomSection != null && !readingRoomSection.trim().isEmpty())
                ? readingRoomSection.trim()
                : "General Reference Section";
    }

    // 24CS202 Unit II Concept: Method Overriding (Multilevel Polymorphism)
    @Override
    public String getItemType() {
        return "Reference (Non-Circulating)";
    }

    /**
     * Core Business Rule: Reference books can NEVER be borrowed.
     * Overrides Book.canBeBorrowed() which would check isAvailable().
     */
    @Override
    public boolean canBeBorrowed() {
        return false;
    }

    public String getReadingRoomSection() {
        return readingRoomSection;
    }

    public void setReadingRoomSection(String readingRoomSection) {
        this.readingRoomSection = readingRoomSection;
    }

    @Override
    public String toReportRow() {
        return String.format("%-8s | %-30s | %-20s | %-15s | %-4d | %-10s [Loc: %s]",
                getId(), getTitle(), getAuthor(), getCategory(), getPublicationYear(), 
                "IN-REF", readingRoomSection);
    }

    @Override
    public String toString() {
        return String.format("ReferenceBook[id=%s, title='%s', author='%s', section='%s', non-circulating=true]",
                getId(), getTitle(), getAuthor(), readingRoomSection);
    }
}
