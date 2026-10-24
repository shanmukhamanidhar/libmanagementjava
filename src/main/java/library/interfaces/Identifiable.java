package library.interfaces;

/**
 * 24CS202 Unit III Concept: Interfaces and Nested Interfaces
 * 
 * An interface in Java is a contract that specifies what a class must do,
 * but not how it does it. All fields declared in an interface are implicitly 
 * 'public static final' (constants), and methods without a body are implicitly 
 * 'public abstract'.
 */
public interface Identifiable {

    /**
     * Interface constant: Standard ID format regex pattern.
     * Demonstrates: Interface variables (public static final by default).
     */
    String ID_PATTERN = "^[A-Za-z0-9_-]+$";

    /**
     * Obtains the unique identity string for this library entity.
     * 
     * @return Unique identifier string.
     */
    String getId();

    /**
     * Obtains a human-readable display label for tables and dropdowns.
     * Demonstrates: Default interface method with concrete implementation.
     * 
     * @return Short display text.
     */
    default String getDisplayLabel() {
        return getId();
    }

    /**
     * 24CS202 Unit III Concept: Nested Interface
     * Demonstrates defining an interface inside another interface.
     */
    interface Validator<T extends Identifiable> {
        /**
         * Validates the given identifiable entity according to business rules.
         * 
         * @param item Entity to validate
         * @return true if valid
         * @throws IllegalArgumentException if validation fails
         */
        boolean validate(T item);
    }
}
