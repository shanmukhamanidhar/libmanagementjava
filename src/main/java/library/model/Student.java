package library.model;

import library.interfaces.Identifiable;
import library.interfaces.Reportable;
import java.util.Objects;

/**
 * 24CS202 Unit II Concept: Encapsulation and Data Hiding
 * 
 * Member 1 Responsibility: Student Domain Entity
 * 
 * Represents a student registered in the library system.
 * Keeps fields private and validates all mutation through setter methods.
 */
public class Student implements Identifiable, Reportable {

    private final String studentId;
    private String name;
    private String department;
    private String email;
    private String contactNumber;

    /**
     * Parameterized Constructor.
     * 
     * @param studentId Unique student ID (e.g. "STU-1001" or "24CS001")
     * @param name Full name of the student
     * @param department Academic department (e.g. "CSE", "ECE", "IT")
     * @param email Contact email address
     * @param contactNumber Phone number
     */
    public Student(String studentId, String name, String department, String email, String contactNumber) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty.");
        }
        this.studentId = studentId.trim().toUpperCase();
        setName(name);
        setDepartment(department);
        setEmail(email);
        setContactNumber(contactNumber);
    }

    /**
     * Overloaded constructor for minimal required registration.
     * 
     * @param studentId Unique student ID
     * @param name Student name
     * @param department Academic department
     */
    public Student(String studentId, String name, String department) {
        this(studentId, name, department, "not-provided@college.edu", "N/A");
    }

    @Override
    public String getId() {
        return studentId;
    }

    @Override
    public String getDisplayLabel() {
        return String.format("[%s] %s (%s)", studentId, name, department);
    }

    // --- Encapsulation: Getters and Setters with Validation ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Student name cannot be null or empty.");
        }
        this.name = name.trim();
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = (department != null && !department.trim().isEmpty()) 
                ? department.trim() 
                : "General";
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = (email != null && !email.trim().isEmpty()) 
                ? email.trim() 
                : "N/A";
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = (contactNumber != null && !contactNumber.trim().isEmpty()) 
                ? contactNumber.trim() 
                : "N/A";
    }

    /**
     * Search helper matching ID, name, or department.
     * 
     * @param query Search query
     * @return true if matches
     */
    public boolean matchesSearch(String query) {
        if (query == null || query.trim().isEmpty()) return true;
        String q = query.trim().toLowerCase();
        return studentId.toLowerCase().contains(q) ||
               name.toLowerCase().contains(q) ||
               department.toLowerCase().contains(q) ||
               email.toLowerCase().contains(q);
    }

    @Override
    public String toReportRow() {
        return String.format("%-10s | %-25s | %-12s | %-25s | %-12s",
                studentId, name, department, email, contactNumber);
    }

    @Override
    public String[] getReportHeaders() {
        return new String[] { "Student ID", "Full Name", "Department", "Email", "Phone" };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return Objects.equals(studentId, student.studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId);
    }

    @Override
    public String toString() {
        return String.format("Student[id=%s, name='%s', dept='%s', email='%s']",
                studentId, name, department, email);
    }
}
