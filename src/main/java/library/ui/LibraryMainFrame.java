package library.ui;

import library.learning.SyllabusDemo;
import library.model.Book;
import library.model.LoanRecord;
import library.model.ReferenceBook;
import library.model.Student;
import library.notifications.NotificationThread;
import library.operations.BookUnavailableException;
import library.operations.LibraryManager;
import library.reports.LibraryReport;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * 24CS202 Desktop Application Interface: Modern Java Swing Frame
 * 
 * Implements an integrated desktop user interface with:
 * - Dashboard with real-time statistics and notification monitor
 * - Book Management (Add, Search, Filter, Catalog Table)
 * - Student Management (Register, Search, Student Table, History)
 * - Issue & Return Desk (Borrow validation, Return processing, Fine calculation)
 * - Reports & Analytics (Audit reports, Stream grouping, Parallel benchmarks)
 * - Educational Java Concepts Sandbox (Interactive 24CS202 syllabus explorer)
 */
public class LibraryMainFrame extends JFrame {

    private final LibraryManager manager;
    private final LibraryReport report;
    private NotificationThread notificationThread;

    // Main Tabbed Pane
    private JTabbedPane tabbedPane;

    // Dashboard Components
    private JLabel lblTotalBooks;
    private JLabel lblTotalStudents;
    private JLabel lblAvailableBooks;
    private JLabel lblIssuedBooks;
    private JLabel lblOverdueLoans;
    private JLabel lblOutstandingFines;
    private DefaultListModel<String> notificationListModel;

    // Books Panel Components
    private JTable booksTable;
    private DefaultTableModel booksTableModel;
    private JTextField txtSearchBook;
    private JComboBox<String> cmbBookCategoryFilter;

    // Students Panel Components
    private JTable studentsTable;
    private DefaultTableModel studentsTableModel;
    private JTextField txtSearchStudent;

    // Loans Panel Components
    private JTable activeLoansTable;
    private DefaultTableModel activeLoansTableModel;
    private JComboBox<String> cmbIssueStudent;
    private JComboBox<String> cmbIssueBook;
    private JComboBox<String> cmbReturnBook;

    // Reports Panel Components
    private JTextArea txtReportOutput;

    // Learning Panel Components
    private JTextArea txtLearningOutput;

    public LibraryMainFrame(LibraryManager manager) {
        super("24CS202 Library Management System - College Academic Project");
        this.manager = manager;
        this.report = new LibraryReport(manager);

        // Register listener for live real-time updates from Web Browser actions
        manager.addDataChangeListener(() -> SwingUtilities.invokeLater(this::refreshAllData));

        initComponents();
        startNotificationWorker();
        refreshAllData();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 43, 73));
        headerPanel.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel titleLabel = new JLabel("Library Management System");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        JLabel subTitleLabel = new JLabel("Course: 24CS202 Object Oriented Programming Through Java | Java 23 Edition");
        subTitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subTitleLabel.setForeground(new Color(210, 225, 245));

        JPanel titleContainer = new JPanel(new GridLayout(2, 1));
        titleContainer.setOpaque(false);
        titleContainer.add(titleLabel);
        titleContainer.add(subTitleLabel);

        // Policy badge in header
        JLabel policyBadge = new JLabel(String.format("<html><b>Policy:</b> %d Days Loan | ₹%.2f/Day Fine</html>",
                manager.getDefaultBorrowDays(), manager.getFineRatePerDay()));
        policyBadge.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        policyBadge.setForeground(Color.WHITE);
        policyBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 110, 170), 1, true),
                new EmptyBorder(5, 10, 5, 10)
        ));

        headerPanel.add(titleContainer, BorderLayout.WEST);
        headerPanel.add(policyBadge, BorderLayout.EAST);

        // Build Tabs
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        tabbedPane.addTab("  Dashboard  ", createDashboardPanel());
        tabbedPane.addTab("  Book Management  ", createBooksPanel());
        tabbedPane.addTab("  Student Management  ", createStudentsPanel());
        tabbedPane.addTab("  Issue & Return  ", createLoansPanel());
        tabbedPane.addTab("  Reports & Analytics  ", createReportsPanel());
        tabbedPane.addTab("  Java Concepts (24CS202)  ", createLearningPanel());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(headerPanel, BorderLayout.NORTH);
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
    }

    // =========================================================================
    // TAB 1: DASHBOARD PANEL
    // =========================================================================

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(245, 247, 250));

        // Metric Stat Cards Panel
        JPanel metricsPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        metricsPanel.setOpaque(false);

        lblTotalBooks = new JLabel("0", SwingConstants.CENTER);
        lblTotalStudents = new JLabel("0", SwingConstants.CENTER);
        lblAvailableBooks = new JLabel("0", SwingConstants.CENTER);
        lblIssuedBooks = new JLabel("0", SwingConstants.CENTER);
        lblOverdueLoans = new JLabel("0", SwingConstants.CENTER);
        lblOutstandingFines = new JLabel("₹0.00", SwingConstants.CENTER);

        metricsPanel.add(createCard("Total Books Cataloged", lblTotalBooks, new Color(52, 152, 219)));
        metricsPanel.add(createCard("Registered Students", lblTotalStudents, new Color(46, 204, 113)));
        metricsPanel.add(createCard("Available for Loan", lblAvailableBooks, new Color(26, 188, 156)));
        metricsPanel.add(createCard("Currently Issued", lblIssuedBooks, new Color(243, 156, 18)));
        metricsPanel.add(createCard("Overdue Loans", lblOverdueLoans, new Color(231, 76, 60)));
        metricsPanel.add(createCard("Total Outstanding Fines", lblOutstandingFines, new Color(155, 89, 182)));

        // Quick Actions & Notifications split panel
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 15, 15));
        bottomPanel.setOpaque(false);

        // Quick Actions
        JPanel actionsPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        actionsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 220)), "Quick Library Operations",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14)));
        actionsPanel.setBackground(Color.WHITE);

        JButton btnQuickAddBook = new JButton("+ Add New Book");
        JButton btnQuickRegStudent = new JButton("+ Register New Student");
        JButton btnQuickIssue = new JButton("-> Issue Book to Student");
        JButton btnQuickReturn = new JButton("<- Return Borrowed Book");

        styleButton(btnQuickAddBook, new Color(41, 128, 185));
        styleButton(btnQuickRegStudent, new Color(39, 174, 96));
        styleButton(btnQuickIssue, new Color(211, 84, 0));
        styleButton(btnQuickReturn, new Color(142, 68, 173));

        btnQuickAddBook.addActionListener(e -> tabbedPane.setSelectedIndex(1));
        btnQuickRegStudent.addActionListener(e -> tabbedPane.setSelectedIndex(2));
        btnQuickIssue.addActionListener(e -> tabbedPane.setSelectedIndex(3));
        btnQuickReturn.addActionListener(e -> tabbedPane.setSelectedIndex(3));

        actionsPanel.add(btnQuickAddBook);
        actionsPanel.add(btnQuickRegStudent);
        actionsPanel.add(btnQuickIssue);
        actionsPanel.add(btnQuickReturn);

        // Notifications List Panel
        JPanel notificationsPanel = new JPanel(new BorderLayout(5, 5));
        notificationsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 220)), "Background Notification Monitor",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14)));
        notificationsPanel.setBackground(Color.WHITE);

        notificationListModel = new DefaultListModel<>();
        JList<String> listNotifications = new JList<>(notificationListModel);
        listNotifications.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane notifScroll = new JScrollPane(listNotifications);
        notifScroll.setPreferredSize(new Dimension(300, 120));

        JPanel notifControls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        notifControls.setOpaque(false);
        JButton btnClearNotifs = new JButton("Clear Log");
        btnClearNotifs.addActionListener(e -> notificationListModel.clear());
        notifControls.add(btnClearNotifs);

        notificationsPanel.add(notifScroll, BorderLayout.CENTER);
        notificationsPanel.add(notifControls, BorderLayout.SOUTH);

        bottomPanel.add(actionsPanel);
        bottomPanel.add(notificationsPanel);

        panel.add(metricsPanel, BorderLayout.NORTH);
        panel.add(bottomPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230), 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        titleLbl.setForeground(new Color(110, 120, 130));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(accentColor);

        JPanel strip = new JPanel();
        strip.setPreferredSize(new Dimension(4, 30));
        strip.setBackground(accentColor);

        card.add(strip, BorderLayout.WEST);
        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    // =========================================================================
    // TAB 2: BOOK MANAGEMENT PANEL
    // =========================================================================

    private JPanel createBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Form Panel (Left)
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Add New Book Record"));
        formPanel.setPreferredSize(new Dimension(340, 500));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtBookId = new JTextField();
        JTextField txtBookTitle = new JTextField();
        JTextField txtBookAuthor = new JTextField();
        JComboBox<String> cmbCategory = new JComboBox<>(new String[] {
                "Computer Science", "Information Technology", "Electronics", 
                "Mathematics", "Physics", "Literature", "General"
        });
        cmbCategory.setEditable(true);
        JTextField txtBookYear = new JTextField(String.valueOf(LocalDate.now().getYear()));
        JCheckBox chkReference = new JCheckBox("Non-Circulating Reference Book");
        JTextField txtRefSection = new JTextField("Section R1");
        txtRefSection.setEnabled(false);

        chkReference.addActionListener(e -> txtRefSection.setEnabled(chkReference.isSelected()));

        int row = 0;
        addFormRow(formPanel, gbc, row++, "Book ID:", txtBookId);
        addFormRow(formPanel, gbc, row++, "Title:", txtBookTitle);
        addFormRow(formPanel, gbc, row++, "Author:", txtBookAuthor);
        addFormRow(formPanel, gbc, row++, "Category:", cmbCategory);
        addFormRow(formPanel, gbc, row++, "Publication Year:", txtBookYear);
        
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        formPanel.add(chkReference, gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 1;
        formPanel.add(new JLabel("Reference Section:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtRefSection, gbc);

        JButton btnAddBook = new JButton("Save Book to Catalog");
        styleButton(btnAddBook, new Color(41, 128, 185));
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        formPanel.add(btnAddBook, gbc);

        btnAddBook.addActionListener(e -> {
            try {
                String id = txtBookId.getText().trim();
                String title = txtBookTitle.getText().trim();
                String author = txtBookAuthor.getText().trim();
                String category = cmbCategory.getSelectedItem() != null ? cmbCategory.getSelectedItem().toString() : "General";
                int year = Integer.parseInt(txtBookYear.getText().trim());

                Book newBook;
                if (chkReference.isSelected()) {
                    newBook = new ReferenceBook(id, title, author, category, year, txtRefSection.getText().trim());
                } else {
                    newBook = new Book(id, title, author, category, year);
                }

                manager.addBook(newBook);
                refreshAllData();
                JOptionPane.showMessageDialog(this, "Book successfully added to catalog!", "Success", JOptionPane.INFORMATION_MESSAGE);

                txtBookId.setText("");
                txtBookTitle.setText("");
                txtBookAuthor.setText("");
                chkReference.setSelected(false);
                txtRefSection.setEnabled(false);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid publication year format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error Adding Book", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Table Panel (Center)
        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        
        // Search & Filter Bar
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        txtSearchBook = new JTextField(15);
        cmbBookCategoryFilter = new JComboBox<>(new String[] {
                "All", "Computer Science", "Information Technology", "Electronics", 
                "Mathematics", "Physics", "Literature", "General"
        });
        JButton btnSearchBook = new JButton("Filter / Search");
        JButton btnResetSearch = new JButton("Reset");

        searchBar.add(new JLabel("Search:"));
        searchBar.add(txtSearchBook);
        searchBar.add(new JLabel("Category:"));
        searchBar.add(cmbBookCategoryFilter);
        searchBar.add(btnSearchBook);
        searchBar.add(btnResetSearch);

        btnSearchBook.addActionListener(e -> populateBooksTable());
        btnResetSearch.addActionListener(e -> {
            txtSearchBook.setText("");
            cmbBookCategoryFilter.setSelectedIndex(0);
            populateBooksTable();
        });

        String[] cols = { "Book ID", "Title", "Author", "Category", "Year", "Type", "Status" };
        booksTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        booksTable = new JTable(booksTableModel);
        booksTable.setRowHeight(24);
        booksTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        tablePanel.add(searchBar, BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(booksTable), BorderLayout.CENTER);

        // Book Table Action Buttons
        JPanel bookActionsBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btnEditBook = new JButton("Edit Selected Book");
        JButton btnDeleteBook = new JButton("Delete Selected Book");
        styleButton(btnEditBook, new Color(211, 84, 0));
        styleButton(btnDeleteBook, new Color(192, 57, 43));

        btnEditBook.addActionListener(e -> {
            int selectedRow = booksTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a book from the table to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String id = booksTableModel.getValueAt(selectedRow, 0).toString();
            manager.getBookById(id).ifPresent(this::showEditBookDialog);
        });

        btnDeleteBook.addActionListener(e -> {
            int selectedRow = booksTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a book to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String id = booksTableModel.getValueAt(selectedRow, 0).toString();
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete book " + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    manager.deleteBook(id);
                    JOptionPane.showMessageDialog(this, "Book deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Delete Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        bookActionsBar.add(btnEditBook);
        bookActionsBar.add(btnDeleteBook);
        tablePanel.add(bookActionsBar, BorderLayout.SOUTH);

        panel.add(formPanel, BorderLayout.WEST);
        panel.add(tablePanel, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // TAB 3: STUDENT MANAGEMENT PANEL
    // =========================================================================

    private JPanel createStudentsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Registration Form (Left)
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Register New Student"));
        formPanel.setPreferredSize(new Dimension(340, 500));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtStuId = new JTextField();
        JTextField txtStuName = new JTextField();
        JComboBox<String> cmbDept = new JComboBox<>(new String[] {
                "CSE", "ECE", "IT", "EEE", "MECH", "CIVIL", "AIDS", "CSBS"
        });
        cmbDept.setEditable(true);
        JTextField txtStuEmail = new JTextField();
        JTextField txtStuPhone = new JTextField();

        int row = 0;
        addFormRow(formPanel, gbc, row++, "Student ID:", txtStuId);
        addFormRow(formPanel, gbc, row++, "Full Name:", txtStuName);
        addFormRow(formPanel, gbc, row++, "Department:", cmbDept);
        addFormRow(formPanel, gbc, row++, "Email:", txtStuEmail);
        addFormRow(formPanel, gbc, row++, "Contact No:", txtStuPhone);

        JButton btnRegStudent = new JButton("Register Student");
        styleButton(btnRegStudent, new Color(39, 174, 96));
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        formPanel.add(btnRegStudent, gbc);

        btnRegStudent.addActionListener(e -> {
            try {
                String id = txtStuId.getText().trim();
                String name = txtStuName.getText().trim();
                String dept = cmbDept.getSelectedItem() != null ? cmbDept.getSelectedItem().toString() : "CSE";
                String email = txtStuEmail.getText().trim();
                String phone = txtStuPhone.getText().trim();

                Student s = new Student(id, name, dept, email, phone);
                manager.registerStudent(s);
                refreshAllData();
                JOptionPane.showMessageDialog(this, "Student registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

                txtStuId.setText("");
                txtStuName.setText("");
                txtStuEmail.setText("");
                txtStuPhone.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Table Panel (Center)
        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        txtSearchStudent = new JTextField(18);
        JButton btnSearchStudent = new JButton("Search Student");
        JButton btnResetStudent = new JButton("Reset");
        JButton btnViewHistory = new JButton("View Borrow History");

        searchBar.add(new JLabel("Search ID / Name:"));
        searchBar.add(txtSearchStudent);
        searchBar.add(btnSearchStudent);
        searchBar.add(btnResetStudent);
        searchBar.add(btnViewHistory);

        btnSearchStudent.addActionListener(e -> populateStudentsTable());
        btnResetStudent.addActionListener(e -> {
            txtSearchStudent.setText("");
            populateStudentsTable();
        });

        btnViewHistory.addActionListener(e -> {
            int selectedRow = studentsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a student from the table first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String stuId = studentsTableModel.getValueAt(selectedRow, 0).toString();
            showStudentLoanHistoryDialog(stuId);
        });

        String[] cols = { "Student ID", "Full Name", "Department", "Email", "Phone" };
        studentsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        studentsTable = new JTable(studentsTableModel);
        studentsTable.setRowHeight(24);
        studentsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        tablePanel.add(searchBar, BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(studentsTable), BorderLayout.CENTER);

        // Student Table Action Buttons
        JPanel stuActionsBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btnEditStu = new JButton("Edit Selected Student");
        JButton btnDeleteStu = new JButton("Delete Selected Student");
        styleButton(btnEditStu, new Color(211, 84, 0));
        styleButton(btnDeleteStu, new Color(192, 57, 43));

        btnEditStu.addActionListener(e -> {
            int selectedRow = studentsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a student from the table to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String id = studentsTableModel.getValueAt(selectedRow, 0).toString();
            manager.getStudentById(id).ifPresent(this::showEditStudentDialog);
        });

        btnDeleteStu.addActionListener(e -> {
            int selectedRow = studentsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a student to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String id = studentsTableModel.getValueAt(selectedRow, 0).toString();
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete student " + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    manager.deleteStudent(id);
                    JOptionPane.showMessageDialog(this, "Student deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Delete Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        stuActionsBar.add(btnEditStu);
        stuActionsBar.add(btnDeleteStu);
        tablePanel.add(stuActionsBar, BorderLayout.SOUTH);

        panel.add(formPanel, BorderLayout.WEST);
        panel.add(tablePanel, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // TAB 4: ISSUE AND RETURN DESK
    // =========================================================================

    private JPanel createLoansPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top Operations Grid: Issue Box and Return Box
        JPanel opsPanel = new JPanel(new GridLayout(1, 2, 20, 10));

        // Issue Box
        JPanel issueBox = new JPanel(new GridBagLayout());
        issueBox.setBorder(BorderFactory.createTitledBorder("Issue Book to Student"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        cmbIssueStudent = new JComboBox<>();
        cmbIssueBook = new JComboBox<>();
        JLabel lblIssueCalc = new JLabel("Loan Period: " + manager.getDefaultBorrowDays() + " Days (Due in 2 weeks)");
        lblIssueCalc.setForeground(new Color(41, 128, 185));
        JButton btnExecuteIssue = new JButton("Issue Book");
        styleButton(btnExecuteIssue, new Color(211, 84, 0));

        int r = 0;
        addFormRow(issueBox, gbc, r++, "Select Student:", cmbIssueStudent);
        addFormRow(issueBox, gbc, r++, "Select Available Book:", cmbIssueBook);
        gbc.gridx = 0; gbc.gridy = r++; gbc.gridwidth = 2;
        issueBox.add(lblIssueCalc, gbc);
        gbc.gridx = 0; gbc.gridy = r++; gbc.gridwidth = 2;
        issueBox.add(btnExecuteIssue, gbc);

        btnExecuteIssue.addActionListener(e -> {
            try {
                if (cmbIssueStudent.getSelectedItem() == null || cmbIssueBook.getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(this, "Select both a student and an available book.", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String studentItem = cmbIssueStudent.getSelectedItem().toString();
                String bookItem = cmbIssueBook.getSelectedItem().toString();

                String stuId = extractIdFromDropdown(studentItem);
                String bookId = extractIdFromDropdown(bookItem);

                LoanRecord loan = manager.issueBook(stuId, bookId, LocalDate.now());
                refreshAllData();
                JOptionPane.showMessageDialog(this,
                        String.format("Book Issued Successfully!\nLoan ID: %s\nDue Date: %s", loan.getId(), loan.getDueDate()),
                        "Issue Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (BookUnavailableException bue) {
                JOptionPane.showMessageDialog(this, bue.getMessage(), "Circulation Rule Warning", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error Issuing Book", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Return Box
        JPanel returnBox = new JPanel(new GridBagLayout());
        returnBox.setBorder(BorderFactory.createTitledBorder("Return Issued Book"));
        GridBagConstraints gbcRet = new GridBagConstraints();
        gbcRet.insets = new Insets(5, 5, 5, 5);
        gbcRet.fill = GridBagConstraints.HORIZONTAL;

        cmbReturnBook = new JComboBox<>();
        JLabel lblReturnPolicy = new JLabel("Overdue Fine Rate: ₹" + manager.getFineRatePerDay() + " per day");
        lblReturnPolicy.setForeground(new Color(142, 68, 173));
        JButton btnExecuteReturn = new JButton("Process Book Return");
        styleButton(btnExecuteReturn, new Color(142, 68, 173));

        int retR = 0;
        addFormRow(returnBox, gbcRet, retR++, "Select Issued Book:", cmbReturnBook);
        gbcRet.gridx = 0; gbcRet.gridy = retR++; gbcRet.gridwidth = 2;
        returnBox.add(lblReturnPolicy, gbcRet);
        gbcRet.gridx = 0; gbcRet.gridy = retR++; gbcRet.gridwidth = 2;
        returnBox.add(btnExecuteReturn, gbcRet);

        btnExecuteReturn.addActionListener(e -> {
            try {
                if (cmbReturnBook.getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(this, "No issued book selected for return.", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String bookItem = cmbReturnBook.getSelectedItem().toString();
                String bookId = extractIdFromDropdown(bookItem);

                LoanRecord completedLoan = manager.returnBook(bookId, LocalDate.now());
                refreshAllData();

                String fineMsg = (completedLoan.getFineAmount() > 0)
                        ? String.format("Note: Book was overdue! Assessed fine: ₹%.2f", completedLoan.getFineAmount())
                        : "Returned on time! No overdue fine assessed.";

                JOptionPane.showMessageDialog(this,
                        String.format("Book Returned Successfully!\nLoan ID: %s\n%s", completedLoan.getId(), fineMsg),
                        "Return Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Return Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        opsPanel.add(issueBox);
        opsPanel.add(returnBox);

        // Active Loans Table (Bottom)
        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        tablePanel.setBorder(BorderFactory.createTitledBorder("Currently Active Loans"));

        String[] cols = { "Loan ID", "Student ID", "Book ID", "Issue Date", "Due Date", "Days Overdue", "Current Fine" };
        activeLoansTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        activeLoansTable = new JTable(activeLoansTableModel);
        activeLoansTable.setRowHeight(24);
        activeLoansTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        tablePanel.add(new JScrollPane(activeLoansTable), BorderLayout.CENTER);

        panel.add(opsPanel, BorderLayout.NORTH);
        panel.add(tablePanel, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // TAB 5: REPORTS & ANALYTICS PANEL
    // =========================================================================

    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton btnFullReport = new JButton("Generate Comprehensive Audit");
        JButton btnCategorySummary = new JButton("Category Breakdown");
        JButton btnParallelBenchmark = new JButton("Run Parallel Stream Benchmark (Unit V)");
        JButton btnBinaryBackup = new JButton("Create Raw Byte Stream Backup (Unit III)");

        styleButton(btnFullReport, new Color(41, 128, 185));
        styleButton(btnCategorySummary, new Color(39, 174, 96));
        styleButton(btnParallelBenchmark, new Color(211, 84, 0));
        styleButton(btnBinaryBackup, new Color(127, 140, 141));

        buttonBar.add(btnFullReport);
        buttonBar.add(btnCategorySummary);
        buttonBar.add(btnParallelBenchmark);
        buttonBar.add(btnBinaryBackup);

        txtReportOutput = new JTextArea();
        txtReportOutput.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtReportOutput.setEditable(false);
        txtReportOutput.setBackground(new Color(250, 252, 255));
        txtReportOutput.setMargin(new Insets(10, 10, 10, 10));

        btnFullReport.addActionListener(e -> {
            txtReportOutput.setText(report.generateFullTextReport());
        });

        btnCategorySummary.addActionListener(e -> {
            StringBuilder sb = new StringBuilder("=== BOOKS BY CATEGORY (Stream groupingBy) ===\n\n");
            report.getBookCountByCategory().forEach((cat, cnt) -> {
                sb.append(String.format("%-25s : %d copies\n", cat, cnt));
            });
            sb.append(String.format("\nAverage Publication Year of Catalog: %.1f\n",
                    report.calculateAveragePublicationYear()));
            txtReportOutput.setText(sb.toString());
        });

        btnParallelBenchmark.addActionListener(e -> {
            txtReportOutput.setText("Running 200,000 item parallel stream benchmark on CPU ForkJoinPool...\n");
            // Run in background worker so Swing UI remains smooth
            SwingWorker<String, Void> worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() {
                    return report.benchmarkParallelVsSequentialStream();
                }

                @Override
                protected void done() {
                    try {
                        txtReportOutput.setText(get());
                    } catch (Exception ex) {
                        txtReportOutput.setText("Benchmark error: " + ex.getMessage());
                    }
                }
            };
            worker.execute();
        });

        btnBinaryBackup.addActionListener(e -> {
            try {
                File src = new File(manager.getFileHandler().getDataDirectory(), "books.txt");
                File backup = new File(manager.getFileHandler().getDataDirectory(), "books_backup_raw_bytes.dat");
                long bytes = manager.getFileHandler().createBinaryBackup(src, backup);
                txtReportOutput.setText(String.format(
                        "=== 24CS202 UNIT III: BYTE STREAM BACKUP COMPLETED ===\n" +
                        "Source File:      %s\n" +
                        "Backup File:      %s\n" +
                        "Bytes Copied:     %d bytes\n" +
                        "Streams Utilized: FileInputStream & FileOutputStream with 4KB Byte Buffer",
                        src.getAbsolutePath(), backup.getAbsolutePath(), bytes
                ));
            } catch (Exception ex) {
                txtReportOutput.setText("Byte stream backup failed: " + ex.getMessage());
            }
        });

        panel.add(buttonBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(txtReportOutput), BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // TAB 6: JAVA CONCEPTS LEARNING SECTION (24CS202 EDUCATIONAL EXPLORER)
    // =========================================================================

    private JPanel createLearningPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JButton btnDemo1 = new JButton("Run Unit 1 Demo");
        JButton btnDemo2 = new JButton("Run Unit 2 Demo");
        JButton btnDemo3 = new JButton("Run Unit 3 Demo");
        JButton btnDemo4 = new JButton("Run Unit 4 Demo");
        JButton btnDemo5 = new JButton("Run Unit 5 Demo");
        JButton btnDemoAll = new JButton("Execute All Syllabus Demos");

        buttonBar.add(btnDemo1);
        buttonBar.add(btnDemo2);
        buttonBar.add(btnDemo3);
        buttonBar.add(btnDemo4);
        buttonBar.add(btnDemo5);
        buttonBar.add(btnDemoAll);

        txtLearningOutput = new JTextArea();
        txtLearningOutput.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtLearningOutput.setEditable(false);
        txtLearningOutput.setBackground(new Color(248, 249, 250));
        txtLearningOutput.setMargin(new Insets(10, 10, 10, 10));

        btnDemo1.addActionListener(e -> txtLearningOutput.setText(SyllabusDemo.demoUnit1PrimitivesAndArrays()));
        btnDemo2.addActionListener(e -> txtLearningOutput.setText(SyllabusDemo.demoUnit2StringsAndInnerClasses()));
        btnDemo3.addActionListener(e -> txtLearningOutput.setText(SyllabusDemo.demoUnit3ExceptionsAndByteStreams()));
        btnDemo4.addActionListener(e -> txtLearningOutput.setText(SyllabusDemo.demoUnit4ThreadsAndLambdas()));
        btnDemo5.addActionListener(e -> txtLearningOutput.setText(SyllabusDemo.demoUnit5CollectionsAndIterators()));
        btnDemoAll.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            sb.append(SyllabusDemo.demoUnit1PrimitivesAndArrays()).append("\n");
            sb.append(SyllabusDemo.demoUnit2StringsAndInnerClasses()).append("\n");
            sb.append(SyllabusDemo.demoUnit3ExceptionsAndByteStreams()).append("\n");
            sb.append(SyllabusDemo.demoUnit4ThreadsAndLambdas()).append("\n");
            sb.append(SyllabusDemo.demoUnit5CollectionsAndIterators()).append("\n");
            txtLearningOutput.setText(sb.toString());
        });

        // Initial educational overview text
        txtLearningOutput.setText(
                "========================================================================================\n" +
                "       24CS202 OBJECT ORIENTED PROGRAMMING THROUGH JAVA - SYLLABUS MAPPING & CODE EXPLORER\n" +
                "========================================================================================\n\n" +
                "Welcome to the 24CS202 Educational Demonstration Panel.\n" +
                "This project is engineered to embody every unit of the curriculum:\n\n" +
                "  * UNIT I:   Fundamental Constructs (Primitives, Promotion, Casting, Arrays, Loops)\n" +
                "  * UNIT II:  Classes, Encapsulation, Multilevel Inheritance, Dynamic Dispatch, Strings\n" +
                "  * UNIT III: Packages, Interfaces, Custom Checked Exceptions, Character & Byte Streams\n" +
                "  * UNIT IV:  Multithreading Lifecycle, Synchronization, EDT Rules, Generic Lambdas\n" +
                "  * UNIT V:   Collections (Map, Set, List, Tree), Stream API (Filter, Map, Reduce, Parallel)\n\n" +
                "Click any button above to execute live interactive demonstrations and view the outputs."
        );

        panel.add(buttonBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(txtLearningOutput), BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // REFRESH & SYNCHRONIZATION HELPERS
    // =========================================================================

    public void refreshAllData() {
        populateDashboard();
        populateBooksTable();
        populateStudentsTable();
        populateLoansData();
    }

    private void populateDashboard() {
        LibraryReport.DashboardStats stats = report.getDashboardStats();
        lblTotalBooks.setText(String.valueOf(stats.totalBooks));
        lblTotalStudents.setText(String.valueOf(stats.totalStudents));
        lblAvailableBooks.setText(String.valueOf(stats.availableBooks));
        lblIssuedBooks.setText(String.valueOf(stats.issuedBooks));
        lblOverdueLoans.setText(String.valueOf(stats.overdueLoans));
        lblOutstandingFines.setText(String.format("₹%.2f", stats.totalOutstandingFines));
    }

    private void populateBooksTable() {
        booksTableModel.setRowCount(0);
        String query = txtSearchBook != null ? txtSearchBook.getText().trim() : "";
        String cat = (cmbBookCategoryFilter != null && cmbBookCategoryFilter.getSelectedItem() != null)
                ? cmbBookCategoryFilter.getSelectedItem().toString() : "All";

        List<Book> books = manager.searchBooks(query, cat);
        for (Book b : books) {
            booksTableModel.addRow(new Object[] {
                    b.getId(),
                    b.getTitle(),
                    b.getAuthor(),
                    b.getCategory(),
                    b.getPublicationYear(),
                    b.getItemType(),
                    b.isAvailable() ? "AVAILABLE" : "ISSUED"
            });
        }
    }

    private void populateStudentsTable() {
        studentsTableModel.setRowCount(0);
        String query = txtSearchStudent != null ? txtSearchStudent.getText().trim() : "";
        List<Student> students = manager.searchStudents(query);
        for (Student s : students) {
            studentsTableModel.addRow(new Object[] {
                    s.getId(),
                    s.getName(),
                    s.getDepartment(),
                    s.getEmail(),
                    s.getContactNumber()
            });
        }
    }

    private void populateLoansData() {
        activeLoansTableModel.setRowCount(0);
        List<LoanRecord> activeLoans = manager.getActiveLoans();
        LocalDate today = LocalDate.now();

        for (LoanRecord l : activeLoans) {
            long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(l.getDueDate(), today);
            String overdueDisplay = (overdueDays > 0) ? overdueDays + " days" : "On time";
            double fine = l.calculateFine(today, manager.getFineRatePerDay());

            activeLoansTableModel.addRow(new Object[] {
                    l.getId(),
                    l.getStudentId(),
                    l.getBookId(),
                    l.getIssueDate(),
                    l.getDueDate(),
                    overdueDisplay,
                    String.format("₹%.2f", fine)
            });
        }

        // Update Dropdowns
        cmbIssueStudent.removeAllItems();
        for (Student s : manager.getAllStudents()) {
            cmbIssueStudent.addItem(String.format("%s - %s", s.getId(), s.getName()));
        }

        cmbIssueBook.removeAllItems();
        for (Book b : manager.getAllBooks()) {
            if (b.canBeBorrowed()) {
                cmbIssueBook.addItem(String.format("%s - %s", b.getId(), b.getTitle()));
            }
        }

        cmbReturnBook.removeAllItems();
        for (LoanRecord l : activeLoans) {
            manager.getBookById(l.getBookId()).ifPresent(b ->
                    cmbReturnBook.addItem(String.format("%s - %s (Loan: %s)", b.getId(), b.getTitle(), l.getId())));
        }
    }

    private void showStudentLoanHistoryDialog(String studentId) {
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

        JTextArea area = new JTextArea(sb.toString(), 15, 50);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Student Loan History", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showEditBookDialog(Book b) {
        JTextField txtTitle = new JTextField(b.getTitle(), 20);
        JTextField txtAuthor = new JTextField(b.getAuthor(), 20);
        JTextField txtCategory = new JTextField(b.getCategory(), 20);
        JTextField txtYear = new JTextField(String.valueOf(b.getPublicationYear()), 6);

        JPanel panel = new JPanel(new GridLayout(4, 2, 8, 8));
        panel.add(new JLabel("Title:"));
        panel.add(txtTitle);
        panel.add(new JLabel("Author:"));
        panel.add(txtAuthor);
        panel.add(new JLabel("Category:"));
        panel.add(txtCategory);
        panel.add(new JLabel("Year:"));
        panel.add(txtYear);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Book: " + b.getId(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int year = Integer.parseInt(txtYear.getText().trim());
                Book updated;
                if (b instanceof ReferenceBook) {
                    updated = new ReferenceBook(b.getId(), txtTitle.getText().trim(), txtAuthor.getText().trim(),
                            txtCategory.getText().trim(), year, ((ReferenceBook) b).getReadingRoomSection());
                } else {
                    updated = new Book(b.getId(), txtTitle.getText().trim(), txtAuthor.getText().trim(),
                            txtCategory.getText().trim(), year);
                }
                manager.updateBook(updated);
                JOptionPane.showMessageDialog(this, "Book updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditStudentDialog(Student s) {
        JTextField txtName = new JTextField(s.getName(), 20);
        JTextField txtDept = new JTextField(s.getDepartment(), 20);
        JTextField txtEmail = new JTextField(s.getEmail(), 20);
        JTextField txtPhone = new JTextField(s.getContactNumber(), 20);

        JPanel panel = new JPanel(new GridLayout(4, 2, 8, 8));
        panel.add(new JLabel("Full Name:"));
        panel.add(txtName);
        panel.add(new JLabel("Department:"));
        panel.add(txtDept);
        panel.add(new JLabel("Email:"));
        panel.add(txtEmail);
        panel.add(new JLabel("Contact:"));
        panel.add(txtPhone);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Student: " + s.getId(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                Student updated = new Student(s.getId(), txtName.getText().trim(), txtDept.getText().trim(),
                        txtEmail.getText().trim(), txtPhone.getText().trim());
                manager.updateStudent(updated);
                JOptionPane.showMessageDialog(this, "Student updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void startNotificationWorker() {
        notificationThread = new NotificationThread(manager, message -> {
            notificationListModel.addElement(message);
            // Auto refresh metrics when notification thread detects overdue scans
            populateDashboard();
        }, 15);
        notificationThread.start();
    }

    // --- GUI Utilities ---

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        panel.add(field, gbc);
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
    }

    private String extractIdFromDropdown(String item) {
        if (item == null) return "";
        int dashIdx = item.indexOf(" - ");
        if (dashIdx != -1) {
            return item.substring(0, dashIdx).trim();
        }
        return item.trim();
    }
}
