package library.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import library.learning.SyllabusDemo;
import library.model.Book;
import library.model.LoanRecord;
import library.model.ReferenceBook;
import library.model.Student;
import library.operations.BookUnavailableException;
import library.operations.LibraryManager;
import library.reports.LibraryReport;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced Embedded Standard Java HTTP Server (Java 23 Standard Library)
 * 
 * Provides interactive localhost browser controls for:
 * 1. Book Management: Add, edit, delete, and search books.
 * 2. Student Management: Register, edit, delete, and search students.
 * 3. Issue & Return Desk: Issue books to students and process returns with live fine calculations.
 * 4. Reports & Analytics: View audit reports, category distributions, and run parallel stream benchmarks.
 * 5. Real-time Synchronization: Actions directly mutate the shared LibraryManager instance,
 *    triggering automatic persistence to text files and real-time refresh of the Swing desktop GUI.
 */
public class LibraryHttpServer {

    private final LibraryManager manager;
    private final LibraryReport report;
    private final int port;
    private HttpServer server;

    public LibraryHttpServer(LibraryManager manager, int port) {
        this.manager = manager;
        this.report = new LibraryReport(manager);
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Web Page Endpoints
        server.createContext("/", new MainPortalHandler());
        server.createContext("/syllabus", new SyllabusHandler());

        // Book Management Form Actions
        server.createContext("/books/add", new AddBookHandler());
        server.createContext("/books/edit", new EditBookHandler());
        server.createContext("/books/delete", new DeleteBookHandler());

        // Student Management Form Actions
        server.createContext("/students/add", new AddStudentHandler());
        server.createContext("/students/edit", new EditStudentHandler());
        server.createContext("/students/delete", new DeleteStudentHandler());

        // Issue & Return Form Actions
        server.createContext("/loans/issue", new IssueBookHandler());
        server.createContext("/loans/return", new ReturnBookHandler());

        server.setExecutor(null); // Default single-thread or internal pool
        server.start();
        System.out.printf("Local Web Dashboard running at: http://localhost:%d/\n", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // =========================================================================
    // MAIN HTML PORTAL HANDLER (GET /)
    // =========================================================================

    private class MainPortalHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendRedirect(exchange, "/");
                return;
            }

            Map<String, String> qparams = parseQueryParams(exchange.getRequestURI());
            String successMsg = qparams.get("msg");
            String errorMsg = qparams.get("err");
            String bookSearch = qparams.getOrDefault("bsearch", "").trim();
            String catFilter = qparams.getOrDefault("bcat", "All").trim();
            String stuSearch = qparams.getOrDefault("ssearch", "").trim();
            String activeTab = qparams.getOrDefault("tab", "dashboard").trim();

            LibraryReport.DashboardStats stats = report.getDashboardStats();
            List<Book> books = manager.searchBooks(bookSearch, catFilter);
            List<Student> students = manager.searchStudents(stuSearch);
            List<LoanRecord> activeLoans = manager.getActiveLoans();
            List<Student> allStudents = manager.getAllStudents();
            List<Book> allBooks = manager.getAllBooks();

            StringBuilder h = new StringBuilder();
            h.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>");
            h.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            h.append("<title>24CS202 Library Management System - Localhost Web Portal</title>");
            h.append("<style>");
            h.append(":root { --primary: #182b49; --accent: #2980b9; --green: #27ae60; --orange: #e67e22; --red: #e74c3c; --purple: #8e44ad; --bg: #f4f6f9; }");
            h.append("* { box-sizing: border-box; }");
            h.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; background: var(--bg); color: #2c3e50; }");
            h.append(".navbar { background: var(--primary); color: white; padding: 14px 28px; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
            h.append(".navbar h1 { margin: 0; font-size: 19px; }");
            h.append(".navbar-sub { font-size: 12px; color: #cbd5e1; }");
            h.append(".badge-online { background: var(--green); color: white; padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: bold; }");
            h.append(".container { max-width: 1140px; margin: 20px auto; padding: 0 15px; }");
            h.append(".tabs-bar { display: flex; gap: 8px; margin-bottom: 20px; border-bottom: 2px solid #e2e8f0; padding-bottom: 8px; flex-wrap: wrap; }");
            h.append(".tab-btn { background: white; border: 1px solid #cbd5e1; padding: 9px 18px; border-radius: 6px; font-size: 14px; font-weight: 600; cursor: pointer; text-decoration: none; color: #475569; display: inline-block; transition: all 0.2s; }");
            h.append(".tab-btn:hover { background: #f1f5f9; color: var(--accent); }");
            h.append(".tab-btn.active { background: var(--primary); color: white; border-color: var(--primary); }");
            h.append(".alert-box { padding: 12px 18px; border-radius: 6px; margin-bottom: 20px; font-size: 14px; font-weight: 500; }");
            h.append(".alert-success { background: #d4edda; color: #155724; border: 1px solid #c3e6cb; }");
            h.append(".alert-error { background: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }");
            h.append(".cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 15px; margin-bottom: 25px; }");
            h.append(".card { background: white; border-radius: 8px; padding: 16px; box-shadow: 0 2px 5px rgba(0,0,0,0.05); text-align: center; border-left: 4px solid var(--accent); }");
            h.append(".card.green { border-color: var(--green); } .card.orange { border-color: var(--orange); } .card.red { border-color: var(--red); } .card.purple { border-color: var(--purple); }");
            h.append(".card h3 { margin: 0 0 6px 0; font-size: 12px; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; }");
            h.append(".card .num { font-size: 26px; font-weight: 700; color: #1e293b; }");
            h.append(".section { background: white; border-radius: 8px; padding: 22px; box-shadow: 0 2px 6px rgba(0,0,0,0.05); margin-bottom: 25px; }");
            h.append(".section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; border-bottom: 1px solid #e2e8f0; padding-bottom: 10px; flex-wrap: wrap; gap: 10px; }");
            h.append(".section-header h2 { margin: 0; font-size: 18px; color: var(--primary); }");
            h.append(".btn { display: inline-flex; align-items: center; justify-content: center; background: var(--accent); color: white; padding: 8px 14px; border-radius: 5px; text-decoration: none; font-size: 13px; font-weight: 600; border: none; cursor: pointer; transition: opacity 0.2s; }");
            h.append(".btn:hover { opacity: 0.9; }");
            h.append(".btn-sm { padding: 4px 8px; font-size: 11px; }");
            h.append(".btn-green { background: var(--green); } .btn-orange { background: var(--orange); } .btn-red { background: var(--red); } .btn-gray { background: #64748b; } .btn-purple { background: var(--purple); }");
            h.append(".filter-bar { display: flex; gap: 10px; align-items: center; margin-bottom: 15px; flex-wrap: wrap; }");
            h.append(".filter-bar input, .filter-bar select { padding: 7px 10px; border: 1px solid #cbd5e1; border-radius: 5px; font-size: 13px; }");
            h.append("table { width: 100%; border-collapse: collapse; font-size: 13px; }");
            h.append("th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #edf2f7; }");
            h.append("th { background: #f8fafc; font-weight: 600; color: #475569; }");
            h.append(".tag-avail { background: #dcfce7; color: #166534; padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; }");
            h.append(".tag-issued { background: #fee2e2; color: #991b1b; padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; }");
            h.append(".tag-ref { background: #fef3c7; color: #92400e; padding: 3px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; }");
            h.append(".grid-2 { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 20px; }");
            h.append(".form-group { margin-bottom: 14px; }");
            h.append(".form-group label { display: block; font-size: 13px; font-weight: 600; margin-bottom: 5px; color: #334155; }");
            h.append(".form-group input, .form-group select { width: 100%; padding: 8px 10px; border: 1px solid #cbd5e1; border-radius: 5px; font-size: 13px; }");
            h.append(".modal-overlay { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 1000; justify-content: center; align-items: center; }");
            h.append(".modal { background: white; border-radius: 8px; width: 450px; max-width: 90%; padding: 22px; box-shadow: 0 4px 15px rgba(0,0,0,0.2); }");
            h.append(".modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; border-bottom: 1px solid #e2e8f0; padding-bottom: 8px; }");
            h.append(".modal-header h3 { margin: 0; font-size: 16px; color: var(--primary); }");
            h.append(".close-btn { font-size: 20px; font-weight: bold; cursor: pointer; color: #64748b; }");
            h.append("pre { background: #1e293b; color: #f8fafc; padding: 15px; border-radius: 6px; font-family: monospace; font-size: 12px; overflow-x: auto; }");
            h.append("</style></head><body>");

            // Navigation Bar
            h.append("<div class='navbar'>");
            h.append("<div><h1>24CS202 Library Management System</h1>");
            h.append("<div class='navbar-sub'>Java 23 & Swing Desktop with Synchronized Localhost Controls</div></div>");
            h.append("<div><span class='badge-online'>LOCAL HOST 8080 CONNECTED</span></div>");
            h.append("</div>");

            h.append("<div class='container'>");

            // Flash Messages
            if (successMsg != null && !successMsg.isEmpty()) {
                h.append("<div class='alert-box alert-success'>&#10004; ").append(escapeHtml(successMsg)).append("</div>");
            }
            if (errorMsg != null && !errorMsg.isEmpty()) {
                h.append("<div class='alert-box alert-error'>&#9888; ").append(escapeHtml(errorMsg)).append("</div>");
            }

            // Tab Navigation Bar
            h.append("<div class='tabs-bar'>");
            h.append(String.format("<a href='/?tab=dashboard' class='tab-btn %s'>Dashboard & Stats</a>", activeTab.equals("dashboard") ? "active" : ""));
            h.append(String.format("<a href='/?tab=books' class='tab-btn %s'>Book Management (%d)</a>", activeTab.equals("books") ? "active" : "", allBooks.size()));
            h.append(String.format("<a href='/?tab=students' class='tab-btn %s'>Student Management (%d)</a>", activeTab.equals("students") ? "active" : "", allStudents.size()));
            h.append(String.format("<a href='/?tab=loans' class='tab-btn %s'>Issue & Return Desk (%d Active)</a>", activeTab.equals("loans") ? "active" : "", activeLoans.size()));
            h.append(String.format("<a href='/?tab=reports' class='tab-btn %s'>Reports & Analytics</a>", activeTab.equals("reports") ? "active" : ""));
            h.append("<a href='/syllabus' class='tab-btn' style='background:#7c3aed; color:white; border-color:#7c3aed;'>24CS202 Syllabus Demos</a>");
            h.append("</div>");

            // =================================================================
            // TAB 1: DASHBOARD
            // =================================================================
            if ("dashboard".equals(activeTab)) {
                h.append("<div class='cards'>");
                h.append(String.format("<div class='card'><h3>Total Books</h3><div class='num'>%d</div></div>", stats.totalBooks));
                h.append(String.format("<div class='card green'><h3>Registered Students</h3><div class='num'>%d</div></div>", stats.totalStudents));
                h.append(String.format("<div class='card green'><h3>Available Books</h3><div class='num'>%d</div></div>", stats.availableBooks));
                h.append(String.format("<div class='card orange'><h3>Currently Issued</h3><div class='num'>%d</div></div>", stats.issuedBooks));
                h.append(String.format("<div class='card red'><h3>Overdue Loans</h3><div class='num'>%d</div></div>", stats.overdueLoans));
                h.append(String.format("<div class='card purple'><h3>Outstanding Fines</h3><div class='num'>₹%.2f</div></div>", stats.totalOutstandingFines));
                h.append("</div>");

                h.append("<div class='grid-2'>");
                // Quick actions
                h.append("<div class='section'>");
                h.append("<h2>Quick Actions</h2>");
                h.append("<p style='font-size:13px; color:#64748b; margin-top:0;'>Instant shortcuts to common library desk workflows:</p>");
                h.append("<div style='display:flex; flex-direction:column; gap:10px;'>");
                h.append("<button onclick=\"openModal('addBookModal')\" class='btn btn-green'>+ Add New Book</button>");
                h.append("<button onclick=\"openModal('addStudentModal')\" class='btn'>+ Register New Student</button>");
                h.append("<a href='/?tab=loans' class='btn btn-orange'>&rarr; Issue Book to Student</a>");
                h.append("<a href='/?tab=loans' class='btn btn-purple'>&larr; Process Book Return</a>");
                h.append("</div></div>");

                // Active loans overview
                h.append("<div class='section'>");
                h.append("<h2>Current Lending Snapshot</h2>");
                h.append(String.format("<p><b>Circulation Policy:</b> %d Days loan period | Overdue fine: ₹%.2f per day</p>",
                        manager.getDefaultBorrowDays(), manager.getFineRatePerDay()));
                if (activeLoans.isEmpty()) {
                    h.append("<p style='color:#64748b;'>No books currently checked out.</p>");
                } else {
                    h.append("<ul style='font-size:13px; padding-left:20px; line-height:1.8;'>");
                    for (LoanRecord l : activeLoans) {
                        boolean overdue = l.isOverdue(LocalDate.now());
                        double fine = l.calculateFine(LocalDate.now(), manager.getFineRatePerDay());
                        h.append(String.format("<li><b>%s</b> &rarr; Student <b>%s</b> (Book: %s) Due: %s %s</li>",
                                l.getId(), l.getStudentId(), l.getBookId(), l.getDueDate(),
                                overdue ? "<span class='tag-issued'>OVERDUE ₹" + String.format("%.2f", fine) + "</span>" : "<span class='tag-avail'>ON TIME</span>"));
                    }
                    h.append("</ul>");
                }
                h.append("</div></div>");
            }

            // =================================================================
            // TAB 2: BOOK MANAGEMENT (Add, Edit, Delete, Search)
            // =================================================================
            if ("books".equals(activeTab)) {
                h.append("<div class='section'>");
                h.append("<div class='section-header'>");
                h.append("<h2>Book Catalog Management</h2>");
                h.append("<button onclick=\"openModal('addBookModal')\" class='btn btn-green'>+ Add New Book</button>");
                h.append("</div>");

                // Filter & Search bar
                h.append("<form method='GET' action='/' class='filter-bar'>");
                h.append("<input type='hidden' name='tab' value='books'>");
                h.append(String.format("<input type='text' name='bsearch' placeholder='Search ID, title, author...' value='%s'>", escapeHtml(bookSearch)));
                h.append("<select name='bcat'>");
                String[] cats = { "All", "Computer Science", "Information Technology", "Electronics", "Mathematics", "Physics", "Literature", "General", "Science" };
                for (String c : cats) {
                    h.append(String.format("<option value='%s' %s>%s</option>", c, c.equalsIgnoreCase(catFilter) ? "selected" : "", c));
                }
                h.append("</select>");
                h.append("<button type='submit' class='btn'>Filter</button>");
                h.append("<a href='/?tab=books' class='btn btn-gray'>Reset</a>");
                h.append("</form>");

                // Books Table
                h.append("<table><thead><tr><th>ID</th><th>Title</th><th>Author</th><th>Category</th><th>Year</th><th>Type</th><th>Status</th><th>Actions</th></tr></thead><tbody>");
                if (books.isEmpty()) {
                    h.append("<tr><td colspan='8' style='text-align:center; color:#64748b;'>No books matching search criteria.</td></tr>");
                } else {
                    for (Book b : books) {
                        String statusTag = b.isAvailable() 
                                ? "<span class='tag-avail'>AVAILABLE</span>" 
                                : "<span class='tag-issued'>ISSUED</span>";
                        String typeTag = b.getItemType().contains("Reference") 
                                ? "<span class='tag-ref'>REFERENCE</span>" 
                                : "STANDARD";
                        
                        String editArgs = String.format("'%s', '%s', '%s', '%s', %d",
                                escapeJs(b.getId()), escapeJs(b.getTitle()), escapeJs(b.getAuthor()),
                                escapeJs(b.getCategory()), b.getPublicationYear());

                        h.append("<tr>");
                        h.append(String.format("<td><b>%s</b></td>", b.getId()));
                        h.append(String.format("<td>%s</td>", escapeHtml(b.getTitle())));
                        h.append(String.format("<td>%s</td>", escapeHtml(b.getAuthor())));
                        h.append(String.format("<td>%s</td>", escapeHtml(b.getCategory())));
                        h.append(String.format("<td>%d</td>", b.getPublicationYear()));
                        h.append(String.format("<td>%s</td>", typeTag));
                        h.append(String.format("<td>%s</td>", statusTag));
                        h.append("<td>");
                        h.append(String.format("<button onclick=\"openEditBook(%s)\" class='btn btn-sm btn-orange' style='margin-right:5px;'>Edit</button>", editArgs));
                        h.append(String.format("<form method='POST' action='/books/delete' style='display:inline;' onsubmit=\"return confirm('Delete book %s?');\">", b.getId()));
                        h.append(String.format("<input type='hidden' name='id' value='%s'>", b.getId()));
                        h.append("<button type='submit' class='btn btn-sm btn-red'>Delete</button></form>");
                        h.append("</td>");
                        h.append("</tr>");
                    }
                }
                h.append("</tbody></table></div>");
            }

            // =================================================================
            // TAB 3: STUDENT MANAGEMENT (Register, Edit, Delete, Search)
            // =================================================================
            if ("students".equals(activeTab)) {
                h.append("<div class='section'>");
                h.append("<div class='section-header'>");
                h.append("<h2>Student Registration Management</h2>");
                h.append("<button onclick=\"openModal('addStudentModal')\" class='btn btn-green'>+ Register New Student</button>");
                h.append("</div>");

                // Student search filter
                h.append("<form method='GET' action='/' class='filter-bar'>");
                h.append("<input type='hidden' name='tab' value='students'>");
                h.append(String.format("<input type='text' name='ssearch' placeholder='Search Student ID, Name, Dept...' value='%s'>", escapeHtml(stuSearch)));
                h.append("<button type='submit' class='btn'>Search</button>");
                h.append("<a href='/?tab=students' class='btn btn-gray'>Reset</a>");
                h.append("</form>");

                // Students Table
                h.append("<table><thead><tr><th>Student ID</th><th>Name</th><th>Department</th><th>Email</th><th>Contact</th><th>Actions</th></tr></thead><tbody>");
                if (students.isEmpty()) {
                    h.append("<tr><td colspan='6' style='text-align:center; color:#64748b;'>No students found matching query.</td></tr>");
                } else {
                    for (Student s : students) {
                        String editArgs = String.format("'%s', '%s', '%s', '%s', '%s'",
                                escapeJs(s.getId()), escapeJs(s.getName()), escapeJs(s.getDepartment()),
                                escapeJs(s.getEmail()), escapeJs(s.getContactNumber()));

                        h.append("<tr>");
                        h.append(String.format("<td><b>%s</b></td>", s.getId()));
                        h.append(String.format("<td>%s</td>", escapeHtml(s.getName())));
                        h.append(String.format("<td>%s</td>", escapeHtml(s.getDepartment())));
                        h.append(String.format("<td>%s</td>", escapeHtml(s.getEmail())));
                        h.append(String.format("<td>%s</td>", escapeHtml(s.getContactNumber())));
                        h.append("<td>");
                        h.append(String.format("<button onclick=\"openEditStudent(%s)\" class='btn btn-sm btn-orange' style='margin-right:5px;'>Edit</button>", editArgs));
                        h.append(String.format("<form method='POST' action='/students/delete' style='display:inline;' onsubmit=\"return confirm('Delete student %s?');\">", s.getId()));
                        h.append(String.format("<input type='hidden' name='id' value='%s'>", s.getId()));
                        h.append("<button type='submit' class='btn btn-sm btn-red'>Delete</button></form>");
                        h.append("</td>");
                        h.append("</tr>");
                    }
                }
                h.append("</tbody></table></div>");
            }

            // =================================================================
            // TAB 4: ISSUE & RETURN DESK
            // =================================================================
            if ("loans".equals(activeTab)) {
                h.append("<div class='grid-2'>");
                
                // Issue Book Form
                h.append("<div class='section'>");
                h.append("<h2>Issue Book to Student</h2>");
                h.append("<form method='POST' action='/loans/issue'>");
                
                h.append("<div class='form-group'><label>Select Registered Student:</label>");
                h.append("<select name='studentId' required>");
                h.append("<option value=''>-- Select Student --</option>");
                for (Student s : allStudents) {
                    h.append(String.format("<option value='%s'>%s - %s (%s)</option>", s.getId(), s.getId(), escapeHtml(s.getName()), s.getDepartment()));
                }
                h.append("</select></div>");

                h.append("<div class='form-group'><label>Select Available Book:</label>");
                h.append("<select name='bookId' required>");
                h.append("<option value=''>-- Select Book --</option>");
                for (Book b : allBooks) {
                    if (b.canBeBorrowed()) {
                        h.append(String.format("<option value='%s'>%s - %s</option>", b.getId(), b.getId(), escapeHtml(b.getTitle())));
                    }
                }
                h.append("</select></div>");

                h.append(String.format("<div class='form-group'><label>Issue Date:</label><input type='date' name='issueDate' value='%s' required></div>", LocalDate.now()));
                h.append(String.format("<p style='font-size:12px; color:#64748b;'>Policy: Due date will be calculated automatically (%d days loan).</p>", manager.getDefaultBorrowDays()));
                h.append("<button type='submit' class='btn btn-orange' style='width:100%;'>Confirm & Issue Book</button>");
                h.append("</form></div>");

                // Return Book Form
                h.append("<div class='section'>");
                h.append("<h2>Process Book Return</h2>");
                if (activeLoans.isEmpty()) {
                    h.append("<p style='color:#64748b;'>No books currently on loan to return.</p>");
                } else {
                    h.append("<form method='POST' action='/loans/return'>");
                    h.append("<div class='form-group'><label>Select Borrowed Book to Return:</label>");
                    h.append("<select name='bookId' required>");
                    h.append("<option value=''>-- Select Issued Book --</option>");
                    for (LoanRecord l : activeLoans) {
                        manager.getBookById(l.getBookId()).ifPresent(b -> {
                            h.append(String.format("<option value='%s'>%s - %s (Student: %s | Due: %s)</option>",
                                    b.getId(), b.getId(), escapeHtml(b.getTitle()), l.getStudentId(), l.getDueDate()));
                        });
                    }
                    h.append("</select></div>");

                    h.append(String.format("<div class='form-group'><label>Return Date:</label><input type='date' name='returnDate' value='%s' required></div>", LocalDate.now()));
                    h.append(String.format("<p style='font-size:12px; color:#64748b;'>Overdue rate: ₹%.2f per day for returns after due date.</p>", manager.getFineRatePerDay()));
                    h.append("<button type='submit' class='btn btn-purple' style='width:100%;'>Calculate Fines & Return Book</button>");
                    h.append("</form>");
                }
                h.append("</div></div>");

                // Active Loans Table
                h.append("<div class='section'>");
                h.append("<h2>Currently Active Loans</h2>");
                h.append("<table><thead><tr><th>Loan ID</th><th>Student ID</th><th>Book ID</th><th>Issue Date</th><th>Due Date</th><th>Days Overdue</th><th>Current Fine</th></tr></thead><tbody>");
                if (activeLoans.isEmpty()) {
                    h.append("<tr><td colspan='7' style='text-align:center; color:#64748b;'>No active loans.</td></tr>");
                } else {
                    for (LoanRecord l : activeLoans) {
                        long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(l.getDueDate(), LocalDate.now());
                        String overdueBadge = (overdueDays > 0)
                                ? "<span class='tag-issued'>" + overdueDays + " days overdue</span>"
                                : "<span class='tag-avail'>On time</span>";
                        double fine = l.calculateFine(LocalDate.now(), manager.getFineRatePerDay());

                        h.append("<tr>");
                        h.append(String.format("<td><b>%s</b></td>", l.getId()));
                        h.append(String.format("<td>%s</td>", l.getStudentId()));
                        h.append(String.format("<td>%s</td>", l.getBookId()));
                        h.append(String.format("<td>%s</td>", l.getIssueDate()));
                        h.append(String.format("<td>%s</td>", l.getDueDate()));
                        h.append(String.format("<td>%s</td>", overdueBadge));
                        h.append(String.format("<td><b>₹%.2f</b></td>", fine));
                        h.append("</tr>");
                    }
                }
                h.append("</tbody></table></div>");
            }

            // =================================================================
            // TAB 5: REPORTS & ANALYTICS
            // =================================================================
            if ("reports".equals(activeTab)) {
                h.append("<div class='section'>");
                h.append("<h2>Library Audit Report & Statistical Summary</h2>");
                h.append("<pre>").append(escapeHtml(report.generateFullTextReport())).append("</pre>");
                h.append("</div>");

                h.append("<div class='section'>");
                h.append("<h2>24CS202 Unit V Parallel Stream Benchmark</h2>");
                h.append("<p style='font-size:13px; color:#64748b;'>Processes 200,000 synthetic catalog items to benchmark ForkJoinPool parallel stream acceleration:</p>");
                h.append("<pre>").append(escapeHtml(report.benchmarkParallelVsSequentialStream())).append("</pre>");
                h.append("</div>");
            }

            // =================================================================
            // MODAL DIALOGS FOR ADD / EDIT OPERATIONS
            // =================================================================
            
            // Add Book Modal
            h.append("<div id='addBookModal' class='modal-overlay'><div class='modal'>");
            h.append("<div class='modal-header'><h3>Add New Book to Catalog</h3><span class='close-btn' onclick=\"closeModal('addBookModal')\">&times;</span></div>");
            h.append("<form method='POST' action='/books/add'>");
            h.append("<div class='form-group'><label>Book ID:</label><input type='text' name='id' placeholder='e.g. B-108' required></div>");
            h.append("<div class='form-group'><label>Title:</label><input type='text' name='title' placeholder='Book Title' required></div>");
            h.append("<div class='form-group'><label>Author:</label><input type='text' name='author' placeholder='Author Name' required></div>");
            h.append("<div class='form-group'><label>Category:</label><input type='text' name='category' placeholder='Computer Science / Physics...' value='General' required></div>");
            h.append(String.format("<div class='form-group'><label>Publication Year:</label><input type='number' name='year' value='%d' required></div>", LocalDate.now().getYear()));
            h.append("<div class='form-group'><label><input type='checkbox' name='isRef' value='true' id='chkIsRef' onchange=\"document.getElementById('refSecGroup').style.display=this.checked?'block':'none'\"> Non-Circulating Reference Book</label></div>");
            h.append("<div class='form-group' id='refSecGroup' style='display:none;'><label>Reading Room Section:</label><input type='text' name='refSection' value='Section R1'></div>");
            h.append("<button type='submit' class='btn btn-green' style='width:100%;'>Save Book</button>");
            h.append("</form></div></div>");

            // Edit Book Modal
            h.append("<div id='editBookModal' class='modal-overlay'><div class='modal'>");
            h.append("<div class='modal-header'><h3>Edit Book Details</h3><span class='close-btn' onclick=\"closeModal('editBookModal')\">&times;</span></div>");
            h.append("<form method='POST' action='/books/edit'>");
            h.append("<input type='hidden' name='id' id='editBookId'>");
            h.append("<div class='form-group'><label>Book ID (Read-only):</label><input type='text' id='editBookIdDisplay' disabled></div>");
            h.append("<div class='form-group'><label>Title:</label><input type='text' name='title' id='editBookTitle' required></div>");
            h.append("<div class='form-group'><label>Author:</label><input type='text' name='author' id='editBookAuthor' required></div>");
            h.append("<div class='form-group'><label>Category:</label><input type='text' name='category' id='editBookCat' required></div>");
            h.append("<div class='form-group'><label>Publication Year:</label><input type='number' name='year' id='editBookYear' required></div>");
            h.append("<button type='submit' class='btn btn-orange' style='width:100%;'>Update Book</button>");
            h.append("</form></div></div>");

            // Add Student Modal
            h.append("<div id='addStudentModal' class='modal-overlay'><div class='modal'>");
            h.append("<div class='modal-header'><h3>Register New Student</h3><span class='close-btn' onclick=\"closeModal('addStudentModal')\">&times;</span></div>");
            h.append("<form method='POST' action='/students/add'>");
            h.append("<div class='form-group'><label>Student ID:</label><input type='text' name='id' placeholder='e.g. 24CS005' required></div>");
            h.append("<div class='form-group'><label>Full Name:</label><input type='text' name='name' placeholder='Student Name' required></div>");
            h.append("<div class='form-group'><label>Department:</label><input type='text' name='department' placeholder='CSE, IT, ECE...' value='CSE' required></div>");
            h.append("<div class='form-group'><label>Email Address:</label><input type='email' name='email' placeholder='name@college.edu' required></div>");
            h.append("<div class='form-group'><label>Contact Phone:</label><input type='text' name='phone' placeholder='9876543210' required></div>");
            h.append("<button type='submit' class='btn btn-green' style='width:100%;'>Register Student</button>");
            h.append("</form></div></div>");

            // Edit Student Modal
            h.append("<div id='editStudentModal' class='modal-overlay'><div class='modal'>");
            h.append("<div class='modal-header'><h3>Edit Student Details</h3><span class='close-btn' onclick=\"closeModal('editStudentModal')\">&times;</span></div>");
            h.append("<form method='POST' action='/students/edit'>");
            h.append("<input type='hidden' name='id' id='editStudentId'>");
            h.append("<div class='form-group'><label>Student ID (Read-only):</label><input type='text' id='editStudentIdDisplay' disabled></div>");
            h.append("<div class='form-group'><label>Full Name:</label><input type='text' name='name' id='editStudentName' required></div>");
            h.append("<div class='form-group'><label>Department:</label><input type='text' name='department' id='editStudentDept' required></div>");
            h.append("<div class='form-group'><label>Email Address:</label><input type='email' name='email' id='editStudentEmail' required></div>");
            h.append("<div class='form-group'><label>Contact Phone:</label><input type='text' name='phone' id='editStudentPhone' required></div>");
            h.append("<button type='submit' class='btn btn-orange' style='width:100%;'>Update Student</button>");
            h.append("</form></div></div>");

            // Modal Javascript functions
            h.append("<script>");
            h.append("function openModal(id) { document.getElementById(id).style.display = 'flex'; }");
            h.append("function closeModal(id) { document.getElementById(id).style.display = 'none'; }");
            h.append("function openEditBook(id, title, author, cat, year) {");
            h.append("  document.getElementById('editBookId').value = id;");
            h.append("  document.getElementById('editBookIdDisplay').value = id;");
            h.append("  document.getElementById('editBookTitle').value = title;");
            h.append("  document.getElementById('editBookAuthor').value = author;");
            h.append("  document.getElementById('editBookCat').value = cat;");
            h.append("  document.getElementById('editBookYear').value = year;");
            h.append("  openModal('editBookModal');");
            h.append("}");
            h.append("function openEditStudent(id, name, dept, email, phone) {");
            h.append("  document.getElementById('editStudentId').value = id;");
            h.append("  document.getElementById('editStudentIdDisplay').value = id;");
            h.append("  document.getElementById('editStudentName').value = name;");
            h.append("  document.getElementById('editStudentDept').value = dept;");
            h.append("  document.getElementById('editStudentEmail').value = email;");
            h.append("  document.getElementById('editStudentPhone').value = phone;");
            h.append("  openModal('editStudentModal');");
            h.append("}");
            h.append("window.onclick = function(e) { if(e.target.classList.contains('modal-overlay')) e.target.style.display='none'; };");
            h.append("</script>");

            h.append("</div></body></html>");

            byte[] bytes = h.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // =========================================================================
    // BOOK ACTION HANDLERS
    // =========================================================================

    private class AddBookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange);
                try {
                    String id = params.get("id");
                    String title = params.get("title");
                    String author = params.get("author");
                    String category = params.get("category");
                    int year = Integer.parseInt(params.getOrDefault("year", "2024"));
                    boolean isRef = "true".equalsIgnoreCase(params.get("isRef"));
                    String section = params.getOrDefault("refSection", "Reference Room");

                    Book b = isRef ? new ReferenceBook(id, title, author, category, year, section)
                                   : new Book(id, title, author, category, year);
                    manager.addBook(b);
                    sendRedirect(exchange, "/?tab=books&msg=Book+added+successfully:+[" + urlEncode(id) + "]");
                    return;
                } catch (Exception ex) {
                    sendRedirect(exchange, "/?tab=books&err=" + urlEncode(ex.getMessage()));
                    return;
                }
            }
            sendRedirect(exchange, "/?tab=books");
        }
    }

    private class EditBookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange);
                try {
                    String id = params.get("id");
                    String title = params.get("title");
                    String author = params.get("author");
                    String category = params.get("category");
                    int year = Integer.parseInt(params.getOrDefault("year", "2024"));

                    Optional<Book> opt = manager.getBookById(id);
                    if (opt.isEmpty()) {
                        throw new IllegalArgumentException("Book not found with ID: " + id);
                    }
                    Book current = opt.get();
                    Book updated;
                    if (current instanceof ReferenceBook) {
                        updated = new ReferenceBook(id, title, author, category, year, ((ReferenceBook) current).getReadingRoomSection());
                    } else {
                        updated = new Book(id, title, author, category, year);
                    }
                    manager.updateBook(updated);
                    sendRedirect(exchange, "/?tab=books&msg=Book+updated+successfully:+[" + urlEncode(id) + "]");
                    return;
                } catch (Exception ex) {
                    sendRedirect(exchange, "/?tab=books&err=" + urlEncode(ex.getMessage()));
                    return;
                }
            }
            sendRedirect(exchange, "/?tab=books");
        }
    }

    private class DeleteBookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange);
                try {
                    String id = params.get("id");
                    manager.deleteBook(id);
                    sendRedirect(exchange, "/?tab=books&msg=Book+deleted+successfully:+[" + urlEncode(id) + "]");
                    return;
                } catch (Exception ex) {
                    sendRedirect(exchange, "/?tab=books&err=" + urlEncode(ex.getMessage()));
                    return;
                }
            }
            sendRedirect(exchange, "/?tab=books");
        }
    }

    // =========================================================================
    // STUDENT ACTION HANDLERS
    // =========================================================================

    private class AddStudentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange);
                try {
                    String id = params.get("id");
                    String name = params.get("name");
                    String dept = params.get("department");
                    String email = params.get("email");
                    String phone = params.get("phone");

                    Student s = new Student(id, name, dept, email, phone);
                    manager.registerStudent(s);
                    sendRedirect(exchange, "/?tab=students&msg=Student+registered+successfully:+[" + urlEncode(id) + "]");
                    return;
                } catch (Exception ex) {
                    sendRedirect(exchange, "/?tab=students&err=" + urlEncode(ex.getMessage()));
                    return;
                }
            }
            sendRedirect(exchange, "/?tab=students");
        }
    }

    private class EditStudentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange);
                try {
                    String id = params.get("id");
                    String name = params.get("name");
                    String dept = params.get("department");
                    String email = params.get("email");
                    String phone = params.get("phone");

                    Student s = new Student(id, name, dept, email, phone);
                    manager.updateStudent(s);
                    sendRedirect(exchange, "/?tab=students&msg=Student+updated+successfully:+[" + urlEncode(id) + "]");
                    return;
                } catch (Exception ex) {
                    sendRedirect(exchange, "/?tab=students&err=" + urlEncode(ex.getMessage()));
                    return;
                }
            }
            sendRedirect(exchange, "/?tab=students");
        }
    }

    private class DeleteStudentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange);
                try {
                    String id = params.get("id");
                    manager.deleteStudent(id);
                    sendRedirect(exchange, "/?tab=students&msg=Student+deleted+successfully:+[" + urlEncode(id) + "]");
                    return;
                } catch (Exception ex) {
                    sendRedirect(exchange, "/?tab=students&err=" + urlEncode(ex.getMessage()));
                    return;
                }
            }
            sendRedirect(exchange, "/?tab=students");
        }
    }

    // =========================================================================
    // LOAN ACTION HANDLERS (Issue & Return)
    // =========================================================================

    private class IssueBookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange);
                try {
                    String studentId = params.get("studentId");
                    String bookId = params.get("bookId");
                    String dateStr = params.get("issueDate");
                    LocalDate issueDate = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();

                    LoanRecord loan = manager.issueBook(studentId, bookId, issueDate);
                    sendRedirect(exchange, "/?tab=loans&msg=Book+issued+successfully!+Loan+ID:+" + urlEncode(loan.getId()) + "+(Due:+" + loan.getDueDate() + ")");
                    return;
                } catch (BookUnavailableException bue) {
                    sendRedirect(exchange, "/?tab=loans&err=" + urlEncode(bue.getMessage()));
                    return;
                } catch (Exception ex) {
                    sendRedirect(exchange, "/?tab=loans&err=" + urlEncode(ex.getMessage()));
                    return;
                }
            }
            sendRedirect(exchange, "/?tab=loans");
        }
    }

    private class ReturnBookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange);
                try {
                    String bookId = params.get("bookId");
                    String dateStr = params.get("returnDate");
                    LocalDate retDate = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();

                    LoanRecord loan = manager.returnBook(bookId, retDate);
                    String fineMsg = (loan.getFineAmount() > 0)
                            ? String.format("Overdue fine assessed: ₹%.2f", loan.getFineAmount())
                            : "Returned on time (Fine: ₹0.00)";
                    sendRedirect(exchange, "/?tab=loans&msg=Book+returned+successfully!+" + urlEncode(fineMsg));
                    return;
                } catch (Exception ex) {
                    sendRedirect(exchange, "/?tab=loans&err=" + urlEncode(ex.getMessage()));
                    return;
                }
            }
            sendRedirect(exchange, "/?tab=loans");
        }
    }

    // =========================================================================
    // SYLLABUS HANDLER
    // =========================================================================

    private class SyllabusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>24CS202 Syllabus Demos</title>");
            sb.append("<style>body{font-family:monospace;background:#1e1e1e;color:#d4d4d4;padding:25px;line-height:1.5;}");
            sb.append("a{color:#58a6ff;} pre{background:#252526;padding:15px;border-radius:6px;overflow-x:auto;}</style></head><body>");
            sb.append("<p><a href='/'>&larr; Back to Dashboard</a></p>");
            sb.append("<h1>24CS202 Object Oriented Programming Through Java - Syllabus Suite</h1>");
            sb.append("<pre>");
            sb.append(SyllabusDemo.demoUnit1PrimitivesAndArrays()).append("\n");
            sb.append(SyllabusDemo.demoUnit2StringsAndInnerClasses()).append("\n");
            sb.append(SyllabusDemo.demoUnit3ExceptionsAndByteStreams()).append("\n");
            sb.append(SyllabusDemo.demoUnit4ThreadsAndLambdas()).append("\n");
            sb.append(SyllabusDemo.demoUnit5CollectionsAndIterators()).append("\n");
            sb.append("</pre></body></html>");

            byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // =========================================================================
    // HTTP UTILITY METHODS
    // =========================================================================

    private void sendRedirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    private Map<String, String> parseQueryParams(URI uri) {
        Map<String, String> map = new HashMap<>();
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isEmpty()) return map;

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            map.put(k, v);
        }
        return map;
    }

    private Map<String, String> parseFormData(HttpExchange exchange) throws IOException {
        Map<String, String> map = new HashMap<>();
        try (InputStream in = exchange.getRequestBody()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
            String body = out.toString(StandardCharsets.UTF_8);
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                if (pair.isEmpty()) continue;
                String[] kv = pair.split("=", 2);
                String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                map.put(k, v);
            }
        }
        return map;
    }

    private static String urlEncode(String val) {
        if (val == null) return "";
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"");
    }
}
