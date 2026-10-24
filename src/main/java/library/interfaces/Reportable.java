package library.interfaces;

/**
 * 24CS202 Unit III Concept: Functional / Operational Interfaces
 * 
 * Provides a uniform contract for any entity that can be rendered
 * as a formatted report record or summary string.
 */
public interface Reportable {

    /**
     * Formats the entity into a structured, readable summary string
     * suitable for report exports or console/text display.
     * 
     * @return Formatted multi-column or summarized text.
     */
    String toReportRow();

    /**
     * Returns the column headers corresponding to toReportRow().
     * Demonstrates: Default method returning an array of Strings.
     * 
     * @return Array of column names.
     */
    default String[] getReportHeaders() {
        return new String[] { "ID", "Description", "Status" };
    }
}
