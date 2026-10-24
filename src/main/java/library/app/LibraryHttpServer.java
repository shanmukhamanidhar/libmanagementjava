package library.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import library.learning.SyllabusDemo;
import library.model.Book;
import library.model.LoanRecord;
import library.model.ReferenceBook;
import library.model.Student;
import library.notifications.NotificationThread;
import library.operations.BookUnavailableException;
import library.operations.LibraryManager;
import library.reports.LibraryReport;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * High-Fidelity Embedded Web Server for 24CS202 Library Management System
 * 
 * Recreates the complete Java Swing desktop interface (LibraryMainFrame) in the browser
 * with identical styling, navigation tabs, forms, tables, metric cards, background
 * notification monitor, reports, and interactive syllabus demonstrations.
 * 
 * Powered by pure standard Java (com.sun.net.httpserver) with ZERO external dependencies.
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

        // Core UI Page
        server.createContext("/", new WebAppHandler());

        // REST APIs
        server.createContext("/api/data", new ApiDataHandler());
        server.createContext("/api/books/add", new ApiAddBookHandler());
        server.createContext("/api/books/update", new ApiUpdateBookHandler());
        server.createContext("/api/books/delete", new ApiDeleteBookHandler());
        server.createContext("/api/students/add", new ApiAddStudentHandler());
        server.createContext("/api/students/update", new ApiUpdateStudentHandler());
        server.createContext("/api/students/delete", new ApiDeleteStudentHandler());
        server.createContext("/api/students/history", new ApiStudentHistoryHandler());
        server.createContext("/api/loans/issue", new ApiIssueLoanHandler());
        server.createContext("/api/loans/return", new ApiReturnLoanHandler());
        server.createContext("/api/reports/audit", new ApiReportAuditHandler());
        server.createContext("/api/reports/category", new ApiReportCategoryHandler());
        server.createContext("/api/reports/benchmark", new ApiReportBenchmarkHandler());
        server.createContext("/api/reports/backup", new ApiReportBackupHandler());
        server.createContext("/api/syllabus/demo", new ApiSyllabusDemoHandler());
        server.createContext("/api/notifications/clear", new ApiClearNotificationsHandler());

        server.setExecutor(null);
        server.start();
        System.out.printf("Local Web Dashboard running at: http://localhost:%d/\n", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // =========================================================================
    // 1. MAIN APPLICATION HTML / CSS / JS HANDLER
    // =========================================================================

    private class WebAppHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }

            String html = generateIndexHtml();
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private String generateIndexHtml() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>24CS202 Library Management System - College Academic Project</title>
    <style>
        :root {
            --header-bg: #182b49;
            --page-bg: #f5f7fa;
            --card-border: #dcdfe6;
            --blue: #2980b9;
            --green: #27ae60;
            --orange: #d35400;
            --purple: #8e44ad;
            --red: #c0392b;
            --text-dark: #2c3e50;
            --text-muted: #6e7882;
            --panel-border: #c8d2dc;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: var(--page-bg);
            color: var(--text-dark);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }

        /* --- Header matching Swing headerPanel --- */
        .app-header {
            background-color: var(--header-bg);
            padding: 12px 20px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-bottom: 2px solid #0f1c30;
        }
        .header-title-container h1 {
            font-size: 22px;
            font-weight: 700;
            color: #ffffff;
            letter-spacing: 0.3px;
        }
        .header-title-container p {
            font-size: 13px;
            color: #d2e1f5;
            margin-top: 2px;
        }
        .policy-badge {
            font-size: 12px;
            color: #ffffff;
            border: 1px solid #466eaa;
            border-radius: 6px;
            padding: 5px 12px;
            background: rgba(255, 255, 255, 0.05);
        }

        /* --- Tabbed Navigation Bar --- */
        .tabs-header {
            background-color: #e9edf2;
            display: flex;
            border-bottom: 1px solid #cbd5e1;
            padding: 0 15px;
            overflow-x: auto;
        }
        .tab-button {
            padding: 10px 20px;
            font-size: 14px;
            font-weight: 500;
            color: #475569;
            background: transparent;
            border: none;
            border-bottom: 3px solid transparent;
            cursor: pointer;
            white-space: nowrap;
            transition: all 0.15s ease-in-out;
        }
        .tab-button:hover {
            color: var(--header-bg);
            background: rgba(255, 255, 255, 0.5);
        }
        .tab-button.active {
            color: var(--header-bg);
            font-weight: 700;
            border-bottom-color: var(--blue);
            background: #ffffff;
        }

        /* --- Main Content Container --- */
        .tab-content {
            flex: 1;
            padding: 20px;
            display: none;
        }
        .tab-content.active {
            display: block;
        }

        /* --- Dashboard Tab Layout --- */
        .metrics-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 15px;
            margin-bottom: 20px;
        }
        @media (max-width: 900px) {
            .metrics-grid { grid-template-columns: repeat(2, 1fr); }
        }
        @media (max-width: 600px) {
            .metrics-grid { grid-template-columns: 1fr; }
        }
        .metric-card {
            background: #ffffff;
            border: 1px solid var(--card-border);
            border-radius: 4px;
            padding: 14px 18px;
            display: flex;
            align-items: center;
            box-shadow: 0 1px 3px rgba(0,0,0,0.03);
            position: relative;
            overflow: hidden;
        }
        .metric-strip {
            position: absolute;
            left: 0;
            top: 0;
            bottom: 0;
            width: 5px;
        }
        .metric-body {
            margin-left: 6px;
        }
        .metric-title {
            font-size: 13px;
            color: var(--text-muted);
            margin-bottom: 4px;
        }
        .metric-value {
            font-size: 26px;
            font-weight: 700;
        }

        .dashboard-bottom-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 15px;
        }
        @media (max-width: 850px) {
            .dashboard-bottom-grid { grid-template-columns: 1fr; }
        }

        /* --- Titled Border Panels (Swing-style) --- */
        .titled-panel {
            background: #ffffff;
            border: 1px solid var(--panel-border);
            border-radius: 4px;
            padding: 16px;
            position: relative;
            margin-top: 10px;
        }
        .titled-panel-header {
            position: absolute;
            top: -11px;
            left: 14px;
            background: #ffffff;
            padding: 0 8px;
            font-size: 14px;
            font-weight: 700;
            color: var(--text-dark);
        }

        /* Quick Action Buttons */
        .quick-actions-container {
            display: flex;
            flex-direction: column;
            gap: 10px;
            margin-top: 5px;
        }
        .btn-action {
            padding: 11px 16px;
            font-size: 13px;
            font-weight: 700;
            color: #ffffff;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            text-align: left;
            transition: opacity 0.15s;
        }
        .btn-action:hover { opacity: 0.92; }

        /* Notification Monitor */
        .notification-log-box {
            font-family: 'Consolas', 'Courier New', monospace;
            font-size: 12px;
            background: #f8fafc;
            border: 1px solid #e2e8f0;
            border-radius: 4px;
            height: 150px;
            overflow-y: auto;
            padding: 10px;
            margin-top: 5px;
            line-height: 1.6;
        }
        .notif-line {
            margin-bottom: 4px;
            color: #1e293b;
        }
        .notif-line.overdue {
            color: #b91c1c;
            font-weight: 600;
        }

        /* --- Split Panels (Forms on Left, Tables on Right) --- */
        .split-layout {
            display: grid;
            grid-template-columns: 340px 1fr;
            gap: 15px;
            align-items: start;
        }
        @media (max-width: 950px) {
            .split-layout { grid-template-columns: 1fr; }
        }

        /* Form elements */
        .form-row {
            display: flex;
            flex-direction: column;
            margin-bottom: 12px;
        }
        .form-row label {
            font-size: 12px;
            font-weight: 600;
            color: #334155;
            margin-bottom: 4px;
        }
        .form-row input[type="text"],
        .form-row input[type="number"],
        .form-row input[type="date"],
        .form-row input[type="email"],
        .form-row select {
            padding: 7px 10px;
            font-size: 13px;
            border: 1px solid #cbd5e1;
            border-radius: 4px;
            background: #ffffff;
            outline: none;
        }
        .form-row input:focus, .form-row select:focus {
            border-color: var(--blue);
            box-shadow: 0 0 0 2px rgba(41, 128, 185, 0.15);
        }
        .form-checkbox-row {
            display: flex;
            align-items: center;
            gap: 8px;
            margin: 10px 0;
            font-size: 13px;
            font-weight: 500;
        }
        .btn-submit {
            padding: 9px 16px;
            font-size: 13px;
            font-weight: 700;
            color: #ffffff;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            width: 100%;
            margin-top: 6px;
        }

        /* Search Filter Toolbar */
        .toolbar-bar {
            display: flex;
            gap: 8px;
            align-items: center;
            padding-bottom: 10px;
            margin-bottom: 10px;
            flex-wrap: wrap;
        }
        .toolbar-bar input, .toolbar-bar select {
            padding: 6px 10px;
            font-size: 13px;
            border: 1px solid #cbd5e1;
            border-radius: 4px;
        }
        .btn-toolbar {
            padding: 6px 12px;
            font-size: 12px;
            font-weight: 600;
            background: #e2e8f0;
            color: #1e293b;
            border: 1px solid #cbd5e1;
            border-radius: 4px;
            cursor: pointer;
        }
        .btn-toolbar:hover { background: #cbd5e1; }

        /* Tables matching JTable */
        .table-container {
            border: 1px solid #cbd5e1;
            border-radius: 4px;
            background: #ffffff;
            overflow: auto;
            max-height: 480px;
        }
        table.swing-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 13px;
            text-align: left;
        }
        table.swing-table th {
            background-color: #f1f5f9;
            color: #1e293b;
            font-weight: 700;
            padding: 8px 12px;
            border-bottom: 1px solid #cbd5e1;
            position: sticky;
            top: 0;
            z-index: 1;
        }
        table.swing-table td {
            padding: 7px 12px;
            border-bottom: 1px solid #edf2f7;
            color: #334155;
        }
        table.swing-table tr:hover {
            background-color: #f8fafc;
        }
        table.swing-table tr.selected {
            background-color: #e0f2fe !important;
        }

        /* Status badges */
        .badge-status {
            padding: 2px 7px;
            border-radius: 3px;
            font-size: 11px;
            font-weight: 700;
            display: inline-block;
        }
        .badge-avail { background: #dcfce7; color: #15803d; }
        .badge-issued { background: #fee2e2; color: #b91c1c; }
        .badge-ref { background: #fef3c7; color: #b45309; }

        /* Table Bottom Action Buttons Bar */
        .table-bottom-bar {
            display: flex;
            justify-content: flex-end;
            gap: 10px;
            padding-top: 10px;
        }
        .btn-bottom {
            padding: 7px 14px;
            font-size: 12px;
            font-weight: 700;
            color: #ffffff;
            border: none;
            border-radius: 4px;
            cursor: pointer;
        }

        /* Reports & Learning Console Display */
        .console-output {
            font-family: 'Consolas', monospace;
            font-size: 13px;
            background: #f8fafc;
            border: 1px solid #cbd5e1;
            border-radius: 4px;
            padding: 16px;
            min-height: 380px;
            max-height: 550px;
            overflow: auto;
            white-space: pre-wrap;
            line-height: 1.5;
            color: #1e293b;
        }

        /* Modal Dialog (JOptionPane-style) */
        .modal-mask {
            display: none;
            position: fixed;
            top: 0; left: 0; width: 100%; height: 100%;
            background: rgba(0, 0, 0, 0.4);
            z-index: 9999;
            align-items: center;
            justify-content: center;
        }
        .modal-box {
            background: #ffffff;
            border-radius: 6px;
            width: 480px;
            max-width: 95%;
            box-shadow: 0 10px 25px rgba(0,0,0,0.2);
            overflow: hidden;
            animation: modalPop 0.15s ease-out;
        }
        @keyframes modalPop {
            from { transform: scale(0.95); opacity: 0; }
            to { transform: scale(1); opacity: 1; }
        }
        .modal-header {
            background: var(--header-bg);
            color: #ffffff;
            padding: 12px 18px;
            font-size: 15px;
            font-weight: 700;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .modal-body {
            padding: 20px;
        }
        .modal-footer {
            padding: 12px 18px;
            background: #f8fafc;
            border-top: 1px solid #e2e8f0;
            display: flex;
            justify-content: flex-end;
            gap: 10px;
        }

        /* Toast notifications */
        .toast-container {
            position: fixed;
            bottom: 20px;
            right: 20px;
            z-index: 10000;
            display: flex;
            flex-direction: column;
            gap: 8px;
        }
        .toast {
            background: #1e293b;
            color: #ffffff;
            padding: 12px 18px;
            border-radius: 5px;
            font-size: 13px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            animation: toastSlide 0.2s ease-out;
        }
        .toast.success { border-left: 4px solid var(--green); }
        .toast.error { border-left: 4px solid var(--red); }
        @keyframes toastSlide {
            from { transform: translateX(50px); opacity: 0; }
            to { transform: translateX(0); opacity: 1; }
        }
    </style>
</head>
<body>

    <!-- Header identical to LibraryMainFrame -->
    <header class="app-header">
        <div class="header-title-container">
            <h1>Library Management System</h1>
            <p>Course: 24CS202 Object Oriented Programming Through Java | Java 23 Edition</p>
        </div>
        <div class="policy-badge" id="headerPolicyBadge">
            <b>Policy:</b> 14 Days Loan | ₹5.00/Day Fine
        </div>
    </header>

    <!-- Tabbed Pane Navigation -->
    <nav class="tabs-header">
        <button class="tab-button active" onclick="switchTab('dashboard')">  Dashboard  </button>
        <button class="tab-button" onclick="switchTab('books')">  Book Management  </button>
        <button class="tab-button" onclick="switchTab('students')">  Student Management  </button>
        <button class="tab-button" onclick="switchTab('loans')">  Issue & Return  </button>
        <button class="tab-button" onclick="switchTab('reports')">  Reports & Analytics  </button>
        <button class="tab-button" onclick="switchTab('learning')">  Java Concepts (24CS202)  </button>
    </nav>

    <!-- TAB 1: DASHBOARD -->
    <main id="tab-dashboard" class="tab-content active">
        <!-- 6 Metric Cards -->
        <div class="metrics-grid">
            <div class="metric-card">
                <div class="metric-strip" style="background-color: #3498db;"></div>
                <div class="metric-body">
                    <div class="metric-title">Total Books Cataloged</div>
                    <div class="metric-value" style="color: #3498db;" id="statTotalBooks">0</div>
                </div>
            </div>
            <div class="metric-card">
                <div class="metric-strip" style="background-color: #2ecc71;"></div>
                <div class="metric-body">
                    <div class="metric-title">Registered Students</div>
                    <div class="metric-value" style="color: #2ecc71;" id="statTotalStudents">0</div>
                </div>
            </div>
            <div class="metric-card">
                <div class="metric-strip" style="background-color: #1abc9c;"></div>
                <div class="metric-body">
                    <div class="metric-title">Available for Loan</div>
                    <div class="metric-value" style="color: #1abc9c;" id="statAvailableBooks">0</div>
                </div>
            </div>
            <div class="metric-card">
                <div class="metric-strip" style="background-color: #f39c12;"></div>
                <div class="metric-body">
                    <div class="metric-title">Currently Issued</div>
                    <div class="metric-value" style="color: #f39c12;" id="statIssuedBooks">0</div>
                </div>
            </div>
            <div class="metric-card">
                <div class="metric-strip" style="background-color: #e74c3c;"></div>
                <div class="metric-body">
                    <div class="metric-title">Overdue Loans</div>
                    <div class="metric-value" style="color: #e74c3c;" id="statOverdueLoans">0</div>
                </div>
            </div>
            <div class="metric-card">
                <div class="metric-strip" style="background-color: #9b59b6;"></div>
                <div class="metric-body">
                    <div class="metric-title">Total Outstanding Fines</div>
                    <div class="metric-value" style="color: #9b59b6;" id="statOutstandingFines">₹0.00</div>
                </div>
            </div>
        </div>

        <!-- Quick Actions & Notification Split Grid -->
        <div class="dashboard-bottom-grid">
            <div class="titled-panel">
                <div class="titled-panel-header">Quick Library Operations</div>
                <div class="quick-actions-container">
                    <button class="btn-action" style="background-color: #2980b9;" onclick="switchTab('books')">+ Add New Book</button>
                    <button class="btn-action" style="background-color: #27ae60;" onclick="switchTab('students')">+ Register New Student</button>
                    <button class="btn-action" style="background-color: #d35400;" onclick="switchTab('loans')">-> Issue Book to Student</button>
                    <button class="btn-action" style="background-color: #8e44ad;" onclick="switchTab('loans')"><- Return Borrowed Book</button>
                </div>
            </div>

            <div class="titled-panel">
                <div class="titled-panel-header">Background Notification Monitor</div>
                <div class="notification-log-box" id="notifLogBox">
                    <div class="notif-line">Connecting to Background Notification Worker...</div>
                </div>
                <div style="display: flex; justify-content: flex-end; margin-top: 8px;">
                    <button class="btn-toolbar" onclick="clearNotifications()">Clear Log</button>
                </div>
            </div>
        </div>
    </main>

    <!-- TAB 2: BOOK MANAGEMENT -->
    <main id="tab-books" class="tab-content">
        <div class="split-layout">
            <!-- Left: Add Book Form -->
            <div class="titled-panel">
                <div class="titled-panel-header">Add New Book Record</div>
                <form id="formAddBook" onsubmit="submitAddBook(event)">
                    <div class="form-row">
                        <label>Book ID:</label>
                        <input type="text" id="addBookId" required placeholder="e.g. B-108">
                    </div>
                    <div class="form-row">
                        <label>Title:</label>
                        <input type="text" id="addBookTitle" required placeholder="Book Title">
                    </div>
                    <div class="form-row">
                        <label>Author:</label>
                        <input type="text" id="addBookAuthor" required placeholder="Author Name">
                    </div>
                    <div class="form-row">
                        <label>Category:</label>
                        <select id="addBookCategory">
                            <option value="Computer Science">Computer Science</option>
                            <option value="Information Technology">Information Technology</option>
                            <option value="Electronics">Electronics</option>
                            <option value="Mathematics">Mathematics</option>
                            <option value="Physics">Physics</option>
                            <option value="Literature">Literature</option>
                            <option value="General" selected>General</option>
                        </select>
                    </div>
                    <div class="form-row">
                        <label>Publication Year:</label>
                        <input type="number" id="addBookYear" value="2024" required>
                    </div>
                    <div class="form-checkbox-row">
                        <input type="checkbox" id="addBookIsRef" onchange="toggleRefSection(this.checked)">
                        <label for="addBookIsRef">Non-Circulating Reference Book</label>
                    </div>
                    <div class="form-row" id="rowRefSection" style="display: none;">
                        <label>Reference Section:</label>
                        <input type="text" id="addBookRefSection" value="Section R1">
                    </div>
                    <button type="submit" class="btn-submit" style="background-color: #2980b9;">Save Book to Catalog</button>
                </form>
            </div>

            <!-- Right: Search Bar & Table -->
            <div>
                <div class="toolbar-bar">
                    <label style="font-size: 13px; font-weight: 600;">Search:</label>
                    <input type="text" id="searchBookQuery" placeholder="Keyword..." style="width: 150px;">
                    <label style="font-size: 13px; font-weight: 600;">Category:</label>
                    <select id="searchBookCategory">
                        <option value="All">All</option>
                        <option value="Computer Science">Computer Science</option>
                        <option value="Information Technology">Information Technology</option>
                        <option value="Electronics">Electronics</option>
                        <option value="Mathematics">Mathematics</option>
                        <option value="Physics">Physics</option>
                        <option value="Literature">Literature</option>
                        <option value="General">General</option>
                    </select>
                    <button class="btn-toolbar" onclick="renderBooksTable()">Filter / Search</button>
                    <button class="btn-toolbar" onclick="resetBookSearch()">Reset</button>
                </div>

                <div class="table-container">
                    <table class="swing-table" id="booksTable">
                        <thead>
                            <tr>
                                <th>Book ID</th>
                                <th>Title</th>
                                <th>Author</th>
                                <th>Category</th>
                                <th>Year</th>
                                <th>Type</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody id="booksTableBody"></tbody>
                    </table>
                </div>

                <div class="table-bottom-bar">
                    <button class="btn-bottom" style="background-color: #d35400;" onclick="promptEditSelectedBook()">Edit Selected Book</button>
                    <button class="btn-bottom" style="background-color: #c0392b;" onclick="promptDeleteSelectedBook()">Delete Selected Book</button>
                </div>
            </div>
        </div>
    </main>

    <!-- TAB 3: STUDENT MANAGEMENT -->
    <main id="tab-students" class="tab-content">
        <div class="split-layout">
            <!-- Left: Register Student Form -->
            <div class="titled-panel">
                <div class="titled-panel-header">Register New Student</div>
                <form id="formAddStudent" onsubmit="submitAddStudent(event)">
                    <div class="form-row">
                        <label>Student ID:</label>
                        <input type="text" id="addStuId" required placeholder="e.g. 24CS005">
                    </div>
                    <div class="form-row">
                        <label>Full Name:</label>
                        <input type="text" id="addStuName" required placeholder="Student Name">
                    </div>
                    <div class="form-row">
                        <label>Department:</label>
                        <select id="addStuDept">
                            <option value="CSE" selected>CSE</option>
                            <option value="ECE">ECE</option>
                            <option value="IT">IT</option>
                            <option value="EEE">EEE</option>
                            <option value="MECH">MECH</option>
                            <option value="CIVIL">CIVIL</option>
                            <option value="AIDS">AIDS</option>
                            <option value="CSBS">CSBS</option>
                        </select>
                    </div>
                    <div class="form-row">
                        <label>Email:</label>
                        <input type="email" id="addStuEmail" required placeholder="name@college.edu">
                    </div>
                    <div class="form-row">
                        <label>Contact No:</label>
                        <input type="text" id="addStuPhone" required placeholder="9876543210">
                    </div>
                    <button type="submit" class="btn-submit" style="background-color: #27ae60;">Register Student</button>
                </form>
            </div>

            <!-- Right: Search Bar & Students Table -->
            <div>
                <div class="toolbar-bar">
                    <label style="font-size: 13px; font-weight: 600;">Search ID / Name:</label>
                    <input type="text" id="searchStudentQuery" placeholder="Query..." style="width: 170px;">
                    <button class="btn-toolbar" onclick="renderStudentsTable()">Search Student</button>
                    <button class="btn-toolbar" onclick="resetStudentSearch()">Reset</button>
                    <button class="btn-toolbar" style="background:#e0f2fe; color:#0369a1; border-color:#bae6fd;" onclick="promptViewBorrowHistory()">View Borrow History</button>
                </div>

                <div class="table-container">
                    <table class="swing-table" id="studentsTable">
                        <thead>
                            <tr>
                                <th>Student ID</th>
                                <th>Full Name</th>
                                <th>Department</th>
                                <th>Email</th>
                                <th>Phone</th>
                            </tr>
                        </thead>
                        <tbody id="studentsTableBody"></tbody>
                    </table>
                </div>

                <div class="table-bottom-bar">
                    <button class="btn-bottom" style="background-color: #d35400;" onclick="promptEditSelectedStudent()">Edit Selected Student</button>
                    <button class="btn-bottom" style="background-color: #c0392b;" onclick="promptDeleteSelectedStudent()">Delete Selected Student</button>
                </div>
            </div>
        </div>
    </main>

    <!-- TAB 4: ISSUE & RETURN DESK -->
    <main id="tab-loans" class="tab-content">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 20px;">
            <!-- Issue Box -->
            <div class="titled-panel">
                <div class="titled-panel-header">Issue Book to Student</div>
                <form id="formIssueBook" onsubmit="submitIssueBook(event)">
                    <div class="form-row">
                        <label>Select Student:</label>
                        <select id="issueSelectStudent" required></select>
                    </div>
                    <div class="form-row">
                        <label>Select Available Book:</label>
                        <select id="issueSelectBook" required></select>
                    </div>
                    <div style="color: #2980b9; font-size: 12px; margin-bottom: 12px; font-weight: 600;">
                        Loan Period: 14 Days (Due in 2 weeks)
                    </div>
                    <button type="submit" class="btn-submit" style="background-color: #d35400;">Issue Book</button>
                </form>
            </div>

            <!-- Return Box -->
            <div class="titled-panel">
                <div class="titled-panel-header">Return Issued Book</div>
                <form id="formReturnBook" onsubmit="submitReturnBook(event)">
                    <div class="form-row">
                        <label>Select Issued Book:</label>
                        <select id="returnSelectBook" required></select>
                    </div>
                    <div style="color: #8e44ad; font-size: 12px; margin-bottom: 12px; font-weight: 600;">
                        Overdue Fine Rate: ₹5.00 per day
                    </div>
                    <button type="submit" class="btn-submit" style="background-color: #8e44ad;">Process Book Return</button>
                </form>
            </div>
        </div>

        <!-- Active Loans Table -->
        <div class="titled-panel">
            <div class="titled-panel-header">Currently Active Loans</div>
            <div class="table-container" style="max-height: 350px;">
                <table class="swing-table" id="loansTable">
                    <thead>
                        <tr>
                            <th>Loan ID</th>
                            <th>Student ID</th>
                            <th>Book ID</th>
                            <th>Issue Date</th>
                            <th>Due Date</th>
                            <th>Days Overdue</th>
                            <th>Current Fine</th>
                        </tr>
                    </thead>
                    <tbody id="loansTableBody"></tbody>
                </table>
            </div>
        </div>
    </main>

    <!-- TAB 5: REPORTS & ANALYTICS -->
    <main id="tab-reports" class="tab-content">
        <div class="toolbar-bar" style="margin-bottom: 12px;">
            <button class="btn-action" style="background: #2980b9;" onclick="runReport('audit')">Generate Comprehensive Audit</button>
            <button class="btn-action" style="background: #27ae60;" onclick="runReport('category')">Category Breakdown</button>
            <button class="btn-action" style="background: #d35400;" onclick="runReport('benchmark')">Run Parallel Stream Benchmark (Unit V)</button>
            <button class="btn-action" style="background: #7f8c8d;" onclick="runReport('backup')">Create Raw Byte Stream Backup (Unit III)</button>
        </div>
        <div class="console-output" id="reportConsoleOutput">Click any report button above to generate statistics.</div>
    </main>

    <!-- TAB 6: JAVA CONCEPTS LEARNING SECTION -->
    <main id="tab-learning" class="tab-content">
        <div class="toolbar-bar" style="margin-bottom: 12px;">
            <button class="btn-toolbar" onclick="runSyllabus('1')">Run Unit 1 Demo</button>
            <button class="btn-toolbar" onclick="runSyllabus('2')">Run Unit 2 Demo</button>
            <button class="btn-toolbar" onclick="runSyllabus('3')">Run Unit 3 Demo</button>
            <button class="btn-toolbar" onclick="runSyllabus('4')">Run Unit 4 Demo</button>
            <button class="btn-toolbar" onclick="runSyllabus('5')">Run Unit 5 Demo</button>
            <button class="btn-toolbar" style="background:#2980b9; color:white;" onclick="runSyllabus('all')">Execute All Syllabus Demos</button>
        </div>
        <div class="console-output" id="syllabusConsoleOutput">
========================================================================================
       24CS202 OBJECT ORIENTED PROGRAMMING THROUGH JAVA - SYLLABUS MAPPING & CODE EXPLORER
========================================================================================

Welcome to the 24CS202 Educational Demonstration Panel.
This project is engineered to embody every unit of the curriculum:

  * UNIT I:   Fundamental Constructs (Primitives, Promotion, Casting, Arrays, Loops)
  * UNIT II:  Classes, Encapsulation, Multilevel Inheritance, Dynamic Dispatch, Strings
  * UNIT III: Packages, Interfaces, Custom Checked Exceptions, Character & Byte Streams
  * UNIT IV:  Multithreading Lifecycle, Synchronization, EDT Rules, Generic Lambdas
  * UNIT V:   Collections (Map, Set, List, Tree), Stream API (Filter, Map, Reduce, Parallel)

Click any button above to execute live interactive demonstrations and view the outputs.
        </div>
    </main>

    <!-- MODAL: EDIT BOOK -->
    <div id="modalEditBook" class="modal-mask">
        <div class="modal-box">
            <div class="modal-header">
                <span id="modalEditBookTitleHeader">Edit Book</span>
                <span style="cursor:pointer;" onclick="closeModal('modalEditBook')">&times;</span>
            </div>
            <form onsubmit="submitEditBook(event)">
                <div class="modal-body">
                    <input type="hidden" id="editBookId">
                    <div class="form-row">
                        <label>Title:</label>
                        <input type="text" id="editBookTitle" required>
                    </div>
                    <div class="form-row">
                        <label>Author:</label>
                        <input type="text" id="editBookAuthor" required>
                    </div>
                    <div class="form-row">
                        <label>Category:</label>
                        <input type="text" id="editBookCategory" required>
                    </div>
                    <div class="form-row">
                        <label>Publication Year:</label>
                        <input type="number" id="editBookYear" required>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn-toolbar" onclick="closeModal('modalEditBook')">Cancel</button>
                    <button type="submit" class="btn-bottom" style="background: #d35400;">Update Book</button>
                </div>
            </form>
        </div>
    </div>

    <!-- MODAL: EDIT STUDENT -->
    <div id="modalEditStudent" class="modal-mask">
        <div class="modal-box">
            <div class="modal-header">
                <span id="modalEditStuTitleHeader">Edit Student</span>
                <span style="cursor:pointer;" onclick="closeModal('modalEditStudent')">&times;</span>
            </div>
            <form onsubmit="submitEditStudent(event)">
                <div class="modal-body">
                    <input type="hidden" id="editStuId">
                    <div class="form-row">
                        <label>Full Name:</label>
                        <input type="text" id="editStuName" required>
                    </div>
                    <div class="form-row">
                        <label>Department:</label>
                        <input type="text" id="editStuDept" required>
                    </div>
                    <div class="form-row">
                        <label>Email Address:</label>
                        <input type="email" id="editStuEmail" required>
                    </div>
                    <div class="form-row">
                        <label>Contact Phone:</label>
                        <input type="text" id="editStuPhone" required>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn-toolbar" onclick="closeModal('modalEditStudent')">Cancel</button>
                    <button type="submit" class="btn-bottom" style="background: #d35400;">Update Student</button>
                </div>
            </form>
        </div>
    </div>

    <!-- MODAL: STUDENT BORROW HISTORY -->
    <div id="modalHistory" class="modal-mask">
        <div class="modal-box" style="width: 650px;">
            <div class="modal-header">
                <span id="modalHistoryTitle">Student Borrowing History</span>
                <span style="cursor:pointer;" onclick="closeModal('modalHistory')">&times;</span>
            </div>
            <div class="modal-body">
                <div class="console-output" id="modalHistoryContent" style="min-height: 200px; max-height: 350px;">Loading...</div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn-toolbar" onclick="closeModal('modalHistory')">Close</button>
            </div>
        </div>
    </div>

    <!-- Toast Notifications -->
    <div id="toastContainer" class="toast-container"></div>

    <!-- JAVASCRIPT APPLICATION LOGIC -->
    <script>
        // Global State Cache
        let appState = {
            stats: {},
            policy: { defaultBorrowDays: 14, fineRatePerDay: 5.0 },
            books: [],
            students: [],
            loans: [],
            activeLoans: [],
            notifications: []
        };
        let selectedBookId = null;
        let selectedStudentId = null;

        // Tab Switching
        function switchTab(tabName) {
            document.querySelectorAll('.tab-button').forEach(btn => {
                btn.classList.toggle('active', btn.getAttribute('onclick').includes(tabName));
            });
            document.querySelectorAll('.tab-content').forEach(c => {
                c.classList.toggle('active', c.id === 'tab-' + tabName);
            });
        }

        // Fetch Complete State from Java Backend
        async function loadAppData() {
            try {
                const res = await fetch('/api/data');
                if (!res.ok) throw new Error('HTTP ' + res.status);
                appState = await res.json();
                renderAll();
            } catch (err) {
                console.error('Data sync error:', err);
            }
        }

        function renderAll() {
            renderDashboard();
            renderBooksTable();
            renderStudentsTable();
            renderLoansView();
            renderNotifications();
        }

        // Render Dashboard Stats
        function renderDashboard() {
            const s = appState.stats || {};
            document.getElementById('statTotalBooks').innerText = s.totalBooks || 0;
            document.getElementById('statTotalStudents').innerText = s.totalStudents || 0;
            document.getElementById('statAvailableBooks').innerText = s.availableBooks || 0;
            document.getElementById('statIssuedBooks').innerText = s.issuedBooks || 0;
            document.getElementById('statOverdueLoans').innerText = s.overdueLoans || 0;
            document.getElementById('statOutstandingFines').innerText = '₹' + Number(s.totalOutstandingFines || 0).toFixed(2);
            
            if (appState.policy) {
                document.getElementById('headerPolicyBadge').innerHTML = 
                    `<b>Policy:</b> ${appState.policy.defaultBorrowDays} Days Loan | ₹${Number(appState.policy.fineRatePerDay).toFixed(2)}/Day Fine`;
            }
        }

        // Render Background Notification Log
        function renderNotifications() {
            const box = document.getElementById('notifLogBox');
            const notifs = appState.notifications || [];
            if (notifs.length === 0) {
                box.innerHTML = '<div class="notif-line" style="color:#64748b;">(No notifications recorded)</div>';
                return;
            }
            box.innerHTML = notifs.map(n => {
                const isOverdue = n.includes('OVERDUE');
                return `<div class="notif-line ${isOverdue ? 'overdue' : ''}">${escapeHtml(n)}</div>`;
            }).join('');
            box.scrollTop = box.scrollHeight;
        }

        async function clearNotifications() {
            await fetch('/api/notifications/clear', { method: 'POST' });
            appState.notifications = [];
            renderNotifications();
            showToast('Notification log cleared.', 'success');
        }

        // Render Books Table
        function renderBooksTable() {
            const q = (document.getElementById('searchBookQuery').value || '').toLowerCase().trim();
            const cat = document.getElementById('searchBookCategory').value;
            const tbody = document.getElementById('booksTableBody');

            const filtered = (appState.books || []).filter(b => {
                const matchesQ = !q || b.id.toLowerCase().includes(q) || b.title.toLowerCase().includes(q) || b.author.toLowerCase().includes(q);
                const matchesCat = (cat === 'All') || (b.category.toLowerCase() === cat.toLowerCase());
                return matchesQ && matchesCat;
            });

            if (filtered.length === 0) {
                tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; color:#64748b; padding:15px;">No books matching query.</td></tr>';
                selectedBookId = null;
                return;
            }

            tbody.innerHTML = filtered.map(b => {
                const isSel = (b.id === selectedBookId);
                const statusBadge = b.available 
                    ? '<span class="badge-status badge-avail">AVAILABLE</span>' 
                    : '<span class="badge-status badge-issued">ISSUED</span>';
                const typeBadge = b.itemType.includes('Reference')
                    ? '<span class="badge-status badge-ref">Reference</span>'
                    : 'Standard Book';

                return `<tr class="${isSel ? 'selected' : ''}" onclick="selectBookRow('${b.id}')">
                    <td><b>${escapeHtml(b.id)}</b></td>
                    <td>${escapeHtml(b.title)}</td>
                    <td>${escapeHtml(b.author)}</td>
                    <td>${escapeHtml(b.category)}</td>
                    <td>${b.publicationYear}</td>
                    <td>${typeBadge}</td>
                    <td>${statusBadge}</td>
                </tr>`;
            }).join('');
        }

        function selectBookRow(id) {
            selectedBookId = (selectedBookId === id) ? null : id;
            renderBooksTable();
        }

        function resetBookSearch() {
            document.getElementById('searchBookQuery').value = '';
            document.getElementById('searchBookCategory').value = 'All';
            renderBooksTable();
        }

        // Render Students Table
        function renderStudentsTable() {
            const q = (document.getElementById('searchStudentQuery').value || '').toLowerCase().trim();
            const tbody = document.getElementById('studentsTableBody');

            const filtered = (appState.students || []).filter(s => {
                return !q || s.id.toLowerCase().includes(q) || s.name.toLowerCase().includes(q) || s.department.toLowerCase().includes(q);
            });

            if (filtered.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color:#64748b; padding:15px;">No students matching query.</td></tr>';
                selectedStudentId = null;
                return;
            }

            tbody.innerHTML = filtered.map(s => {
                const isSel = (s.id === selectedStudentId);
                return `<tr class="${isSel ? 'selected' : ''}" onclick="selectStudentRow('${s.id}')">
                    <td><b>${escapeHtml(s.id)}</b></td>
                    <td>${escapeHtml(s.name)}</td>
                    <td>${escapeHtml(s.department)}</td>
                    <td>${escapeHtml(s.email)}</td>
                    <td>${escapeHtml(s.contactNumber)}</td>
                </tr>`;
            }).join('');
        }

        function selectStudentRow(id) {
            selectedStudentId = (selectedStudentId === id) ? null : id;
            renderStudentsTable();
        }

        function resetStudentSearch() {
            document.getElementById('searchStudentQuery').value = '';
            renderStudentsTable();
        }

        // Render Loans View & Dropdowns
        function renderLoansView() {
            // Dropdowns
            const issueStuSelect = document.getElementById('issueSelectStudent');
            issueStuSelect.innerHTML = '<option value="">-- Select Student --</option>' +
                (appState.students || []).map(s => `<option value="${s.id}">${s.id} - ${escapeHtml(s.name)} (${s.department})</option>`).join('');

            const issueBookSelect = document.getElementById('issueSelectBook');
            const availableBooks = (appState.books || []).filter(b => b.canBeBorrowed);
            issueBookSelect.innerHTML = '<option value="">-- Select Available Book --</option>' +
                availableBooks.map(b => `<option value="${b.id}">${b.id} - ${escapeHtml(b.title)}</option>`).join('');

            const returnSelect = document.getElementById('returnSelectBook');
            const activeLoans = appState.activeLoans || [];
            returnSelect.innerHTML = '<option value="">-- Select Issued Book --</option>' +
                activeLoans.map(l => `<option value="${l.bookId}">${l.bookId} - Loan: ${l.id} (Student: ${l.studentId})</option>`).join('');

            // Loans Table
            const tbody = document.getElementById('loansTableBody');
            if (activeLoans.length === 0) {
                tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; color:#64748b; padding:15px;">No active loans.</td></tr>';
                return;
            }

            tbody.innerHTML = activeLoans.map(l => {
                const overdueStr = (l.overdueDays > 0) ? `${l.overdueDays} days` : 'On time';
                const statusBadge = (l.overdueDays > 0) 
                    ? `<span class="badge-status badge-issued">${overdueStr}</span>` 
                    : `<span class="badge-status badge-avail">${overdueStr}</span>`;

                return `<tr>
                    <td><b>${escapeHtml(l.id)}</b></td>
                    <td>${escapeHtml(l.studentId)}</td>
                    <td>${escapeHtml(l.bookId)}</td>
                    <td>${l.issueDate}</td>
                    <td>${l.dueDate}</td>
                    <td>${statusBadge}</td>
                    <td><b>₹${Number(l.currentFine || 0).toFixed(2)}</b></td>
                </tr>`;
            }).join('');
        }

        // --- Form Submissions ---

        function toggleRefSection(checked) {
            document.getElementById('rowRefSection').style.display = checked ? 'flex' : 'none';
        }

        async function submitAddBook(e) {
            e.preventDefault();
            const body = {
                id: document.getElementById('addBookId').value.trim(),
                title: document.getElementById('addBookTitle').value.trim(),
                author: document.getElementById('addBookAuthor').value.trim(),
                category: document.getElementById('addBookCategory').value,
                year: parseInt(document.getElementById('addBookYear').value, 10),
                isRef: document.getElementById('addBookIsRef').checked,
                refSection: document.getElementById('addBookRefSection').value.trim()
            };

            const res = await apiPost('/api/books/add', body);
            if (res.success) {
                showToast('Book successfully added to catalog!', 'success');
                document.getElementById('formAddBook').reset();
                toggleRefSection(false);
                await loadAppData();
            } else {
                showToast(res.error, 'error');
            }
        }

        function promptEditSelectedBook() {
            if (!selectedBookId) {
                showToast('Please select a book from the table to edit.', 'error');
                return;
            }
            const b = appState.books.find(x => x.id === selectedBookId);
            if (!b) return;

            document.getElementById('editBookId').value = b.id;
            document.getElementById('modalEditBookTitleHeader').innerText = 'Edit Book: ' + b.id;
            document.getElementById('editBookTitle').value = b.title;
            document.getElementById('editBookAuthor').value = b.author;
            document.getElementById('editBookCategory').value = b.category;
            document.getElementById('editBookYear').value = b.publicationYear;
            openModal('modalEditBook');
        }

        async function submitEditBook(e) {
            e.preventDefault();
            const body = {
                id: document.getElementById('editBookId').value,
                title: document.getElementById('editBookTitle').value.trim(),
                author: document.getElementById('editBookAuthor').value.trim(),
                category: document.getElementById('editBookCategory').value.trim(),
                year: parseInt(document.getElementById('editBookYear').value, 10)
            };
            const res = await apiPost('/api/books/update', body);
            if (res.success) {
                showToast('Book updated successfully!', 'success');
                closeModal('modalEditBook');
                await loadAppData();
            } else {
                showToast(res.error, 'error');
            }
        }

        async function promptDeleteSelectedBook() {
            if (!selectedBookId) {
                showToast('Please select a book from the table to delete.', 'error');
                return;
            }
            if (!confirm(`Are you sure you want to delete book [${selectedBookId}]?`)) return;

            const res = await apiPost('/api/books/delete', { id: selectedBookId });
            if (res.success) {
                showToast('Book deleted successfully.', 'success');
                selectedBookId = null;
                await loadAppData();
            } else {
                showToast(res.error, 'error');
            }
        }

        async function submitAddStudent(e) {
            e.preventDefault();
            const body = {
                id: document.getElementById('addStuId').value.trim(),
                name: document.getElementById('addStuName').value.trim(),
                department: document.getElementById('addStuDept').value,
                email: document.getElementById('addStuEmail').value.trim(),
                phone: document.getElementById('addStuPhone').value.trim()
            };

            const res = await apiPost('/api/students/add', body);
            if (res.success) {
                showToast('Student registered successfully!', 'success');
                document.getElementById('formAddStudent').reset();
                await loadAppData();
            } else {
                showToast(res.error, 'error');
            }
        }

        function promptEditSelectedStudent() {
            if (!selectedStudentId) {
                showToast('Please select a student from the table to edit.', 'error');
                return;
            }
            const s = appState.students.find(x => x.id === selectedStudentId);
            if (!s) return;

            document.getElementById('editStuId').value = s.id;
            document.getElementById('modalEditStuTitleHeader').innerText = 'Edit Student: ' + s.id;
            document.getElementById('editStuName').value = s.name;
            document.getElementById('editStuDept').value = s.department;
            document.getElementById('editStuEmail').value = s.email;
            document.getElementById('editStuPhone').value = s.contactNumber;
            openModal('modalEditStudent');
        }

        async function submitEditStudent(e) {
            e.preventDefault();
            const body = {
                id: document.getElementById('editStuId').value,
                name: document.getElementById('editStuName').value.trim(),
                department: document.getElementById('editStuDept').value.trim(),
                email: document.getElementById('editStuEmail').value.trim(),
                phone: document.getElementById('editStuPhone').value.trim()
            };
            const res = await apiPost('/api/students/update', body);
            if (res.success) {
                showToast('Student updated successfully!', 'success');
                closeModal('modalEditStudent');
                await loadAppData();
            } else {
                showToast(res.error, 'error');
            }
        }

        async function promptDeleteSelectedStudent() {
            if (!selectedStudentId) {
                showToast('Please select a student from the table to delete.', 'error');
                return;
            }
            if (!confirm(`Are you sure you want to delete student [${selectedStudentId}]?`)) return;

            const res = await apiPost('/api/students/delete', { id: selectedStudentId });
            if (res.success) {
                showToast('Student deleted successfully.', 'success');
                selectedStudentId = null;
                await loadAppData();
            } else {
                showToast(res.error, 'error');
            }
        }

        async function promptViewBorrowHistory() {
            if (!selectedStudentId) {
                showToast('Please select a student from the table first.', 'error');
                return;
            }
            document.getElementById('modalHistoryTitle').innerText = 'Borrowing History: Student ' + selectedStudentId;
            document.getElementById('modalHistoryContent').innerText = 'Loading borrowing history...';
            openModal('modalHistory');

            try {
                const res = await fetch('/api/students/history?id=' + encodeURIComponent(selectedStudentId));
                const data = await res.json();
                document.getElementById('modalHistoryContent').innerText = data.text;
            } catch (err) {
                document.getElementById('modalHistoryContent').innerText = 'Error loading history: ' + err.message;
            }
        }

        async function submitIssueBook(e) {
            e.preventDefault();
            const studentId = document.getElementById('issueSelectStudent').value;
            const bookId = document.getElementById('issueSelectBook').value;
            if (!studentId || !bookId) {
                showToast('Please select both a student and an available book.', 'error');
                return;
            }

            const res = await apiPost('/api/loans/issue', { studentId, bookId });
            if (res.success) {
                showToast(`Book Issued Successfully!\\nLoan ID: ${res.loanId}\\nDue Date: ${res.dueDate}`, 'success');
                await loadAppData();
            } else {
                showToast(res.error, 'error');
            }
        }

        async function submitReturnBook(e) {
            e.preventDefault();
            const bookId = document.getElementById('returnSelectBook').value;
            if (!bookId) {
                showToast('Please select an issued book to return.', 'error');
                return;
            }

            const res = await apiPost('/api/loans/return', { bookId });
            if (res.success) {
                const fineMsg = res.fine > 0 
                    ? `Overdue fine assessed: ₹${Number(res.fine).toFixed(2)}` 
                    : 'Returned on time! No overdue fine assessed.';
                showToast(`Book Returned Successfully!\\n${fineMsg}`, 'success');
                await loadAppData();
            } else {
                showToast(res.error, 'error');
            }
        }

        // --- Reports & Syllabus Actions ---
        async function runReport(type) {
            const out = document.getElementById('reportConsoleOutput');
            out.innerText = 'Generating report...';
            try {
                let url = '/api/reports/' + type;
                let method = (type === 'backup') ? 'POST' : 'GET';
                const res = await fetch(url, { method });
                const json = await res.json();
                out.innerText = json.output;
            } catch (err) {
                out.innerText = 'Report generation failed: ' + err.message;
            }
        }

        async function runSyllabus(unit) {
            const out = document.getElementById('syllabusConsoleOutput');
            out.innerText = `Executing 24CS202 syllabus demonstration (Unit ${unit})...`;
            try {
                const res = await fetch('/api/syllabus/demo?unit=' + encodeURIComponent(unit));
                const json = await res.json();
                out.innerText = json.output;
            } catch (err) {
                out.innerText = 'Demo execution error: ' + err.message;
            }
        }

        // --- Helper Utilities ---
        async function apiPost(url, data) {
            try {
                const res = await fetch(url, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(data)
                });
                return await res.json();
            } catch (err) {
                return { success: false, error: err.message };
            }
        }

        function openModal(id) { document.getElementById(id).style.display = 'flex'; }
        function closeModal(id) { document.getElementById(id).style.display = 'none'; }
        window.onclick = function(e) { if (e.target.classList.contains('modal-mask')) e.target.style.display = 'none'; };

        function showToast(msg, type = 'success') {
            const c = document.getElementById('toastContainer');
            const t = document.createElement('div');
            t.className = 'toast ' + type;
            t.innerText = msg;
            c.appendChild(t);
            setTimeout(() => { t.remove(); }, 4000);
        }

        function escapeHtml(s) {
            if (!s) return '';
            return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
        }

        // Poll for updates every 3 seconds to ensure real-time consistency with background workers & Swing
        setInterval(loadAppData, 3000);
        // Initial load
        window.onload = loadAppData;
    </script>
</body>
</html>
""";
    }

    // =========================================================================
    // 2. DATA SYNCHRONIZATION API HANDLERS
    // =========================================================================

    private class ApiDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            LibraryReport.DashboardStats stats = report.getDashboardStats();
            List<Book> books = manager.getAllBooks();
            List<Student> students = manager.getAllStudents();
            List<LoanRecord> activeLoans = manager.getActiveLoans();
            List<String> notifications = NotificationThread.getNotificationHistory();
            LocalDate today = LocalDate.now();

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"stats\":{");
            sb.append("\"totalBooks\":").append(stats.totalBooks).append(",");
            sb.append("\"totalStudents\":").append(stats.totalStudents).append(",");
            sb.append("\"availableBooks\":").append(stats.availableBooks).append(",");
            sb.append("\"issuedBooks\":").append(stats.issuedBooks).append(",");
            sb.append("\"overdueLoans\":").append(stats.overdueLoans).append(",");
            sb.append("\"totalOutstandingFines\":").append(stats.totalOutstandingFines);
            sb.append("},");

            sb.append("\"policy\":{");
            sb.append("\"defaultBorrowDays\":").append(manager.getDefaultBorrowDays()).append(",");
            sb.append("\"fineRatePerDay\":").append(manager.getFineRatePerDay());
            sb.append("},");

            sb.append("\"books\":[");
            for (int i = 0; i < books.size(); i++) {
                Book b = books.get(i);
                sb.append("{");
                sb.append("\"id\":\"").append(escapeJson(b.getId())).append("\",");
                sb.append("\"title\":\"").append(escapeJson(b.getTitle())).append("\",");
                sb.append("\"author\":\"").append(escapeJson(b.getAuthor())).append("\",");
                sb.append("\"category\":\"").append(escapeJson(b.getCategory())).append("\",");
                sb.append("\"publicationYear\":").append(b.getPublicationYear()).append(",");
                sb.append("\"itemType\":\"").append(escapeJson(b.getItemType())).append("\",");
                sb.append("\"available\":").append(b.isAvailable()).append(",");
                sb.append("\"canBeBorrowed\":").append(b.canBeBorrowed());
                sb.append("}").append(i < books.size() - 1 ? "," : "");
            }
            sb.append("],");

            sb.append("\"students\":[");
            for (int i = 0; i < students.size(); i++) {
                Student s = students.get(i);
                sb.append("{");
                sb.append("\"id\":\"").append(escapeJson(s.getId())).append("\",");
                sb.append("\"name\":\"").append(escapeJson(s.getName())).append("\",");
                sb.append("\"department\":\"").append(escapeJson(s.getDepartment())).append("\",");
                sb.append("\"email\":\"").append(escapeJson(s.getEmail())).append("\",");
                sb.append("\"contactNumber\":\"").append(escapeJson(s.getContactNumber())).append("\"");
                sb.append("}").append(i < students.size() - 1 ? "," : "");
            }
            sb.append("],");

            sb.append("\"activeLoans\":[");
            for (int i = 0; i < activeLoans.size(); i++) {
                LoanRecord l = activeLoans.get(i);
                long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(l.getDueDate(), today);
                double fine = l.calculateFine(today, manager.getFineRatePerDay());
                sb.append("{");
                sb.append("\"id\":\"").append(escapeJson(l.getId())).append("\",");
                sb.append("\"studentId\":\"").append(escapeJson(l.getStudentId())).append("\",");
                sb.append("\"bookId\":\"").append(escapeJson(l.getBookId())).append("\",");
                sb.append("\"issueDate\":\"").append(l.getIssueDate()).append("\",");
                sb.append("\"dueDate\":\"").append(l.getDueDate()).append("\",");
                sb.append("\"overdueDays\":").append(Math.max(0, overdueDays)).append(",");
                sb.append("\"currentFine\":").append(fine);
                sb.append("}").append(i < activeLoans.size() - 1 ? "," : "");
            }
            sb.append("],");

            sb.append("\"notifications\":[");
            for (int i = 0; i < notifications.size(); i++) {
                sb.append("\"").append(escapeJson(notifications.get(i))).append("\"")
                  .append(i < notifications.size() - 1 ? "," : "");
            }
            sb.append("]");
            sb.append("}");

            sendResponse(exchange, 200, sb.toString(), "application/json");
        }
    }

    private class ApiAddBookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> body = parseJsonBody(exchange);
            try {
                String id = (String) body.get("id");
                String title = (String) body.get("title");
                String author = (String) body.get("author");
                String category = (String) body.get("category");
                int year = ((Number) body.get("year")).intValue();
                boolean isRef = Boolean.TRUE.equals(body.get("isRef"));
                String refSection = (String) body.get("refSection");

                Book book = isRef ? new ReferenceBook(id, title, author, category, year, refSection)
                                  : new Book(id, title, author, category, year);
                manager.addBook(book);
                NotificationThread.logMessage("[Catalog Update] New Book added: [" + id + "] " + title);
                sendResponse(exchange, 200, "{\"success\":true}", "application/json");
            } catch (Exception ex) {
                sendResponse(exchange, 200, "{\"success\":false,\"error\":\"" + escapeJson(ex.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class ApiUpdateBookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> body = parseJsonBody(exchange);
            try {
                String id = (String) body.get("id");
                String title = (String) body.get("title");
                String author = (String) body.get("author");
                String category = (String) body.get("category");
                int year = ((Number) body.get("year")).intValue();

                Optional<Book> opt = manager.getBookById(id);
                if (opt.isEmpty()) throw new IllegalArgumentException("Book not found: " + id);
                Book existing = opt.get();
                Book updated;
                if (existing instanceof ReferenceBook) {
                    updated = new ReferenceBook(id, title, author, category, year, ((ReferenceBook) existing).getReadingRoomSection());
                } else {
                    updated = new Book(id, title, author, category, year);
                }
                manager.updateBook(updated);
                NotificationThread.logMessage("[Catalog Update] Book details updated: [" + id + "]");
                sendResponse(exchange, 200, "{\"success\":true}", "application/json");
            } catch (Exception ex) {
                sendResponse(exchange, 200, "{\"success\":false,\"error\":\"" + escapeJson(ex.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class ApiDeleteBookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> body = parseJsonBody(exchange);
            try {
                String id = (String) body.get("id");
                manager.deleteBook(id);
                NotificationThread.logMessage("[Catalog Update] Book deleted from catalog: [" + id + "]");
                sendResponse(exchange, 200, "{\"success\":true}", "application/json");
            } catch (Exception ex) {
                sendResponse(exchange, 200, "{\"success\":false,\"error\":\"" + escapeJson(ex.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class ApiAddStudentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> body = parseJsonBody(exchange);
            try {
                String id = (String) body.get("id");
                String name = (String) body.get("name");
                String dept = (String) body.get("department");
                String email = (String) body.get("email");
                String phone = (String) body.get("phone");

                Student s = new Student(id, name, dept, email, phone);
                manager.registerStudent(s);
                NotificationThread.logMessage("[Student Desk] New Student registered: [" + id + "] " + name);
                sendResponse(exchange, 200, "{\"success\":true}", "application/json");
            } catch (Exception ex) {
                sendResponse(exchange, 200, "{\"success\":false,\"error\":\"" + escapeJson(ex.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class ApiUpdateStudentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> body = parseJsonBody(exchange);
            try {
                String id = (String) body.get("id");
                String name = (String) body.get("name");
                String dept = (String) body.get("department");
                String email = (String) body.get("email");
                String phone = (String) body.get("phone");

                Student s = new Student(id, name, dept, email, phone);
                manager.updateStudent(s);
                NotificationThread.logMessage("[Student Desk] Student record updated: [" + id + "]");
                sendResponse(exchange, 200, "{\"success\":true}", "application/json");
            } catch (Exception ex) {
                sendResponse(exchange, 200, "{\"success\":false,\"error\":\"" + escapeJson(ex.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class ApiDeleteStudentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> body = parseJsonBody(exchange);
            try {
                String id = (String) body.get("id");
                manager.deleteStudent(id);
                NotificationThread.logMessage("[Student Desk] Student record removed: [" + id + "]");
                sendResponse(exchange, 200, "{\"success\":true}", "application/json");
            } catch (Exception ex) {
                sendResponse(exchange, 200, "{\"success\":false,\"error\":\"" + escapeJson(ex.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class ApiStudentHistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String query = uri.getQuery();
            String studentId = "";
            if (query != null && query.startsWith("id=")) {
                studentId = URLDecoder.decode(query.substring(3), StandardCharsets.UTF_8);
            }

            List<LoanRecord> history = manager.getLoansForStudent(studentId);
            StringBuilder sb = new StringBuilder();
            sb.append("Borrowing History for Student ID: ").append(studentId).append("\n\n");
            if (history.isEmpty()) {
                sb.append("No borrowing records found for this student.");
            } else {
                for (LoanRecord l : history) {
                    sb.append(String.format("Loan %s | Book: %s | Issued: %s | Due: %s | Return: %s | Fine: ₹%.2f\n",
                            l.getId(), l.getBookId(), l.getIssueDate(), l.getDueDate(),
                            (l.getReturnDate() != null ? l.getReturnDate() : "STILL ACTIVE"),
                            l.getFineAmount()));
                }
            }
            sendResponse(exchange, 200, "{\"text\":\"" + escapeJson(sb.toString()) + "\"}", "application/json");
        }
    }

    private class ApiIssueLoanHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> body = parseJsonBody(exchange);
            try {
                String studentId = (String) body.get("studentId");
                String bookId = (String) body.get("bookId");
                LoanRecord loan = manager.issueBook(studentId, bookId, LocalDate.now());
                NotificationThread.logMessage(String.format("[Circulation Desk] Book [%s] issued to [%s] under Loan %s",
                        bookId, studentId, loan.getId()));
                sendResponse(exchange, 200, String.format("{\"success\":true,\"loanId\":\"%s\",\"dueDate\":\"%s\"}",
                        loan.getId(), loan.getDueDate()), "application/json");
            } catch (BookUnavailableException | IllegalArgumentException ex) {
                sendResponse(exchange, 200, "{\"success\":false,\"error\":\"" + escapeJson(ex.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class ApiReturnLoanHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> body = parseJsonBody(exchange);
            try {
                String bookId = (String) body.get("bookId");
                LoanRecord loan = manager.returnBook(bookId, LocalDate.now());
                NotificationThread.logMessage(String.format("[Circulation Desk] Book [%s] returned. Assessed fine: ₹%.2f",
                        bookId, loan.getFineAmount()));
                sendResponse(exchange, 200, String.format("{\"success\":true,\"fine\":%.2f}", loan.getFineAmount()), "application/json");
            } catch (Exception ex) {
                sendResponse(exchange, 200, "{\"success\":false,\"error\":\"" + escapeJson(ex.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class ApiReportAuditHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String auditText = report.generateFullTextReport();
            sendResponse(exchange, 200, "{\"output\":\"" + escapeJson(auditText) + "\"}", "application/json");
        }
    }

    private class ApiReportCategoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder("=== BOOKS BY CATEGORY (Stream groupingBy) ===\n\n");
            report.getBookCountByCategory().forEach((cat, cnt) -> {
                sb.append(String.format("%-25s : %d copies\n", cat, cnt));
            });
            sb.append(String.format("\nAverage Publication Year of Catalog: %.1f\n",
                    report.calculateAveragePublicationYear()));
            sendResponse(exchange, 200, "{\"output\":\"" + escapeJson(sb.toString()) + "\"}", "application/json");
        }
    }

    private class ApiReportBenchmarkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String result = report.benchmarkParallelVsSequentialStream();
            sendResponse(exchange, 200, "{\"output\":\"" + escapeJson(result) + "\"}", "application/json");
        }
    }

    private class ApiReportBackupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                File src = new File(manager.getFileHandler().getDataDirectory(), "books.txt");
                File backup = new File(manager.getFileHandler().getDataDirectory(), "books_backup_raw_bytes.dat");
                long bytes = manager.getFileHandler().createBinaryBackup(src, backup);
                String msg = String.format(
                        "=== 24CS202 UNIT III: BYTE STREAM BACKUP COMPLETED ===\n" +
                        "Source File:      %s\n" +
                        "Backup File:      %s\n" +
                        "Bytes Copied:     %d bytes\n" +
                        "Streams Utilized: FileInputStream & FileOutputStream with 4KB Byte Buffer",
                        src.getAbsolutePath(), backup.getAbsolutePath(), bytes
                );
                sendResponse(exchange, 200, "{\"output\":\"" + escapeJson(msg) + "\"}", "application/json");
            } catch (Exception ex) {
                sendResponse(exchange, 200, "{\"output\":\"Backup failed: " + escapeJson(ex.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class ApiSyllabusDemoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String query = uri.getQuery();
            String unit = "all";
            if (query != null && query.startsWith("unit=")) {
                unit = query.substring(5).toLowerCase();
            }

            String output;
            switch (unit) {
                case "1" -> output = SyllabusDemo.demoUnit1PrimitivesAndArrays();
                case "2" -> output = SyllabusDemo.demoUnit2StringsAndInnerClasses();
                case "3" -> output = SyllabusDemo.demoUnit3ExceptionsAndByteStreams();
                case "4" -> output = SyllabusDemo.demoUnit4ThreadsAndLambdas();
                case "5" -> output = SyllabusDemo.demoUnit5CollectionsAndIterators();
                default -> {
                    output = SyllabusDemo.demoUnit1PrimitivesAndArrays() + "\n" +
                             SyllabusDemo.demoUnit2StringsAndInnerClasses() + "\n" +
                             SyllabusDemo.demoUnit3ExceptionsAndByteStreams() + "\n" +
                             SyllabusDemo.demoUnit4ThreadsAndLambdas() + "\n" +
                             SyllabusDemo.demoUnit5CollectionsAndIterators();
                }
            }
            sendResponse(exchange, 200, "{\"output\":\"" + escapeJson(output) + "\"}", "application/json");
        }
    }

    private class ApiClearNotificationsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            NotificationThread.clearNotificationHistory();
            sendResponse(exchange, 200, "{\"success\":true}", "application/json");
        }
    }

    // =========================================================================
    // 3. UTILITY METHODS
    // =========================================================================

    private void sendResponse(HttpExchange exchange, int statusCode, String responseText, String contentType) throws IOException {
        byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, Object> parseJsonBody(HttpExchange exchange) throws IOException {
        Map<String, Object> map = new HashMap<>();
        try (InputStream in = exchange.getRequestBody()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
            String json = out.toString(StandardCharsets.UTF_8).trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1);
                // Simple JSON parser for single-level key-values
                String[] tokens = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                for (String t : tokens) {
                    String[] kv = t.split(":", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim().replaceAll("^\"|\"$", "");
                        String rawVal = kv[1].trim();
                        if (rawVal.startsWith("\"") && rawVal.endsWith("\"")) {
                            map.put(key, rawVal.substring(1, rawVal.length() - 1));
                        } else if ("true".equalsIgnoreCase(rawVal) || "false".equalsIgnoreCase(rawVal)) {
                            map.put(key, Boolean.parseBoolean(rawVal));
                        } else {
                            try {
                                if (rawVal.contains(".")) {
                                    map.put(key, Double.parseDouble(rawVal));
                                } else {
                                    map.put(key, Integer.parseInt(rawVal));
                                }
                            } catch (Exception ex) {
                                map.put(key, rawVal);
                            }
                        }
                    }
                }
            }
        }
        return map;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
