# Library Management System (Java 23 / Swing Desktop)

> **College Team Academic Project**  
> **Course:** 24CS202 — Object Oriented Programming Through Java  
> **Platform:** Java 23 (JDK 23), Apache Maven 3.9.6, Java Swing Desktop GUI  
> **Target Audience:** College Faculty, Viva Evaluators, and Engineering Students

---

## 1. Project Overview

The **Library Management System** is a complete, modular, and fully functional desktop application developed in Java Swing. It fulfills two primary goals:
1. **Real-world Library Workflows:** Manages books, student registrations, book checkout/issue, returns, availability status, automated overdue fine calculations, and persistent storage via text files.
2. **Comprehensive Coursework Coverage:** Directly demonstrates, implements, and provides runnable educational sandboxes for all five units of the **24CS202 OOP Through Java** syllabus.

---

## 2. Java 23 & Maven Setup Guide

### Prerequisites & Verified Versions
- **Java Development Kit:** Eclipse Temurin JDK 23 (build 23.0.2+7) or Oracle JDK 23
- **Build Tool:** Apache Maven 3.9.6 or later
- **Operating System:** Windows 10/11, Linux, or macOS

### Step-by-Step Installation

#### A. Installing JDK 23 on Windows
1. **Using Windows Package Manager (winget) [Recommended]:**
   ```powershell
   winget install --id EclipseAdoptium.Temurin.23.JDK -e --silent --accept-source-agreements --accept-package-agreements
   ```
2. **Manual Download:**
   - Download the Windows x64 MSI installer from [Eclipse Adoptium](https://adoptium.net/temurin/releases/?version=23).
   - Run the installer and check the box to **"Set JAVA_HOME variable"** and **"Add to PATH"**.
3. **Verify Java Installation:**
   ```powershell
   java -version
   javac -version
   ```
   *Expected output:* `openjdk version "23.0.2" ... javac 23.0.2`

#### B. Installing Apache Maven
1. **Download:**
   Download `apache-maven-3.9.6-bin.zip` from [Apache Maven Downloads](https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/).
2. **Extract:**
   Extract to `C:\Users\<username>\AppData\Local\Programs\apache-maven-3.9.6` or `C:\Program Files\Apache\maven`.
3. **Configure Environment Variables:**
   - Set `M2_HOME` and `MAVEN_HOME` to the extracted Maven directory.
   - Add `%MAVEN_HOME%\bin` to your user or system `PATH`.
4. **Verify Maven Installation:**
   ```powershell
   mvn -version
   ```
   *Expected output:* `Apache Maven 3.9.6 ... Java version: 23.0.2`

---

## 3. Quick Build and Run Instructions

All commands should be executed from the project root directory (`LibraryManagementSystem`):

### 1. Compile and Run Unit Tests
```powershell
mvn clean test
```
*Executes all JUnit 5 business rule tests (Duplicate IDs, Unavailable Books, Non-circulating reference restrictions, Fine calculations, File persistence).*

### 2. Run the Java Swing Desktop Application
```powershell
mvn exec:java
```
*Or package into an executable JAR and run directly:*
```powershell
mvn package -DskipTests
java -jar target/LibraryManagementSystem-1.0.0.jar
```

### 3. Run the Standalone Syllabus Educational Console Demo
```powershell
java -cp target/classes library.learning.SyllabusDemo
```

---

## 4. Key Application Features

| Section | Capabilities |
|---|---|
| **Dashboard** | Real-time metric cards for Total Books, Registered Students, Available Books, Issued Books, Overdue Loans, and Outstanding Fines. Includes quick action shortcuts and live background notification monitor. |
| **Book Management** | Add, edit, delete, and search books (both from Swing GUI and Web Browser). Dynamic search by ID, title, author, and category. Real-time catalog table. |
| **Student Management** | Register, edit, delete, and search students (both from Swing GUI and Web Browser). Instant search. View borrowing history dialog. |
| **Issue & Return Desk** | Select student and available book. Automatic due-date calculation based on 14-day policy. Process returns and automatically calculate overdue fines at ₹5.00/day. |
| **Reports & Analytics** | Generate comprehensive text audit reports, category distribution via Streams, execute Unit V parallel stream benchmarks (200k items), and create raw byte stream backups. |
| **Localhost Web Controls** | Complete browser-based management portal running on `http://localhost:8080/` with live updates synchronized with the desktop GUI. |
| **Java Concepts Sandbox** | Dedicated interactive screen allowing faculty to execute live demonstrations of all 5 syllabus units with output displayed on-screen. |

---

## 5. Library Policies

The system enforces a configurable library policy maintained centrally in `LibraryManager.java`:
- **Default Borrow Duration:** 14 days
- **Overdue Fine Rate:** ₹5.00 per day
- **Grace Policy:** Books returned on or before their due date incur **₹0.00** fine.
- **Reference Book Policy:** Reference works (dictionaries, encyclopedias) are strictly non-circulating and cannot be issued.

---

## 6. Directory Structure

```text
LibraryManagementSystem/
├── pom.xml                                  # Maven project configuration (Java 23)
├── README.md                                # Setup, installation, and project overview
├── docs/
│   ├── PROJECT_ARCHITECTURE.md              # Architecture, class diagrams, design rules
│   ├── SYLLABUS_MAPPING.md                  # Comprehensive 24CS202 syllabus topic mapping
│   ├── VIVA_QUESTIONS.md                    # Question & Answer bank for faculty viva
│   └── TEAM_GUIDE.md                        # 3-member team division & method contracts
├── data/
│   ├── books.txt                            # Delimited persistent book records
│   ├── students.txt                         # Delimited persistent student records
│   └── loans.txt                            # Delimited persistent loan transactions
├── src/
│   ├── main/
│   │   └── java/
│   │       └── library/
│   │           ├── app/
│   │           │   └── LibraryManagementSystem.java   # App entry point & shutdown hooks
│   │           ├── interfaces/
│   │           │   ├── Identifiable.java             # Entity contract & nested interfaces
│   │           │   └── Reportable.java               # Report generation contract
│   │           ├── model/
│   │           │   ├── LibraryItem.java              # Abstract root item (Encapsulation)
│   │           │   ├── Book.java                     # Concrete standard book (Inheritance)
│   │           │   ├── ReferenceBook.java            # Specialized book (Multilevel)
│   │           │   ├── Student.java                  # Registered student entity
│   │           │   └── LoanRecord.java               # Loan transaction & fine calculation
│   │           ├── operations/
│   │           │   ├── LibraryManager.java           # Business logic & collection manager
│   │           │   └── BookUnavailableException.java # Custom checked exception
│   │           ├── storage/
│   │           │   └── LibraryFileHandler.java       # Text I/O & Byte stream backups
│   │           ├── reports/
│   │           │   └── LibraryReport.java            # Stream API analytics & parallel tests
│   │           ├── notifications/
│   │           │   └── NotificationThread.java       # Background worker thread & Swing EDT
│   │           ├── ui/
│   │           │   └── LibraryMainFrame.java         # Java Swing GUI desktop interface
│   │           └── learning/
│   │               └── SyllabusDemo.java             # Executable syllabus concept suite
│   └── test/
│       └── java/
│           └── library/
│               └── LibraryOperationsTest.java        # Automated JUnit 5 test suite
└── .gitignore
```

---

## 7. Team Division

- **Member 1 (Domain Models):** `Book.java`, `Student.java`, `LibraryItem.java`, `ReferenceBook.java`
- **Member 2 (Library Operations):** `LibraryManager.java`, `BookUnavailableException.java`
- **Member 3 (Storage & Concurrency):** `LibraryFileHandler.java`, `LibraryReport.java`, `NotificationThread.java`
- **Shared Integration:** `LibraryManagementSystem.java`, `LibraryMainFrame.java`, `SyllabusDemo.java`, interfaces, and tests.

---

## 8. Troubleshooting

- **`javac: release version 23 not supported`:** Verify that your `JAVA_HOME` points to JDK 23 and not an older JDK version. Check with `mvn -version`.
- **Swing UI looks low resolution on high-DPI screens:** Java 23 automatically handles DPI scaling on Windows 11.
- **Portability:** Data is stored in standard UTF-8 text files under `data/`, making it portable across Windows, macOS, and Linux without external database servers.
