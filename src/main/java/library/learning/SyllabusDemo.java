package library.learning;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 24CS202 Educational Demonstration Suite: Units I - V
 * 
 * Provides runnable, executable demonstrations of syllabus concepts
 * that do not occur directly in the core library management workflow.
 * 
 * Can be run independently via main() or executed interactively from
 * the Swing Application's "Java Concepts" educational section.
 */
public class SyllabusDemo {

    // =========================================================================
    // UNIT I: FUNDAMENTAL JAVA CONSTRUCTS
    // =========================================================================

    /**
     * Demonstrates:
     * - Primitive types and memory footprints
     * - Automatic type promotion in arithmetic expressions
     * - Explicit type casting
     * - Arrays (1D and multidimensional)
     */
    public static String demoUnit1PrimitivesAndArrays() {
        StringBuilder out = new StringBuilder();
        out.append("=== UNIT I: PRIMITIVE TYPES, PROMOTION, AND ARRAYS ===\n");

        // 1. Primitive data types
        byte b = 42;
        char c = 'A'; // ASCII 65
        short s = 1024;
        int i = 50000;
        float f = 5.67f;
        double d = 0.1234;

        // 2. Automatic Type Promotion:
        // In expression (b * c), b and c are promoted to int.
        // In expression (f * d), f is promoted to double.
        double result = (b * c) + (i / s) - (f * d);
        out.append(String.format("1. Primitive evaluation result: %f\n", result));

        // 3. Explicit Type Casting (Narrowing conversion)
        int castFromDouble = (int) (d * 100);
        out.append(String.format("2. Explicit Cast: (int)(%f * 100) = %d\n", d, castFromDouble));

        // 4. Arrays: 1D and 2D arrays
        int[] scores = { 85, 92, 78, 96, 88 };
        out.append("3. 1D Array Iteration: [");
        for (int idx = 0; idx < scores.length; idx++) {
            out.append(scores[idx]).append(idx < scores.length - 1 ? ", " : "");
        }
        out.append("]\n");

        // 2D Array: Matrix representation of library rack shelves
        int[][] shelfCounts = {
            { 10, 15, 20 },
            { 25, 30, 35 }
        };
        out.append("4. 2D Array (2x3 Rack Shelf Matrix):\n");
        for (int r = 0; r < shelfCounts.length; r++) {
            out.append("   Row ").append(r).append(": ");
            for (int col = 0; col < shelfCounts[r].length; col++) {
                out.append(String.format("[%2d] ", shelfCounts[r][col]));
            }
            out.append("\n");
        }

        return out.toString();
    }

    // =========================================================================
    // UNIT II: STRINGS, STRINGBUFFER, TOKENIZER & INNER CLASSES
    // =========================================================================

    /**
     * Demonstrates:
     * - StringBuffer (Thread-safe mutable string buffer)
     * - StringTokenizer (Legacy delimiter splitting)
     * - Static Nested Class & Non-static Inner Class
     */
    public static String demoUnit2StringsAndInnerClasses() {
        StringBuilder out = new StringBuilder();
        out.append("=== UNIT II: STRINGS, STRINGBUFFER, STRINGTOKENIZER & INNER CLASSES ===\n");

        // 1. StringBuffer (Mutable, synchronized)
        StringBuffer sbuf = new StringBuffer("Library");
        sbuf.append(" Management");
        sbuf.append(" System");
        sbuf.insert(0, "[SRM 24CS202] ");
        out.append("1. StringBuffer manipulation: ").append(sbuf.toString()).append("\n");
        sbuf.reverse();
        out.append("   Reversed StringBuffer: ").append(sbuf.toString()).append("\n");

        // 2. StringTokenizer
        String catalogData = "Java:Herbert Schildt:McGraw-Hill:2024:Available";
        out.append("2. StringTokenizer delimiter parsing for '").append(catalogData).append("':\n");
        StringTokenizer tokenizer = new StringTokenizer(catalogData, ":");
        int tokenIdx = 1;
        while (tokenizer.hasMoreTokens()) {
            out.append(String.format("   Token %d: %s\n", tokenIdx++, tokenizer.nextToken()));
        }

        // 3. Static Nested Class
        OuterClass.StaticNestedClass staticNested = new OuterClass.StaticNestedClass("Nested Configuration");
        out.append("3. Static Nested Class: ").append(staticNested.describe()).append("\n");

        // 4. Non-static Inner Class (Requires enclosing instance)
        OuterClass outer = new OuterClass("Outer Library Context");
        OuterClass.InnerClass inner = outer.new InnerClass("Inner Member Access");
        out.append("4. Inner Class: ").append(inner.describe()).append("\n");

        return out.toString();
    }

    // Demonstration container for Unit II inner class demonstration
    public static class OuterClass {
        private final String outerName;

        public OuterClass(String outerName) {
            this.outerName = outerName;
        }

        // Static Nested Class: Does NOT hold reference to enclosing instance
        public static class StaticNestedClass {
            private final String info;
            public StaticNestedClass(String info) { this.info = info; }
            public String describe() { return "StaticNested[" + info + "]"; }
        }

        // Non-Static Inner Class: Can access private fields of OuterClass
        public class InnerClass {
            private final String innerName;
            public InnerClass(String innerName) { this.innerName = innerName; }
            public String describe() {
                return "Inner[" + innerName + "] within Outer[" + outerName + "]";
            }
        }
    }

    // =========================================================================
    // UNIT III: EXCEPTIONS & BYTE STREAM STREAMS
    // =========================================================================

    /**
     * Demonstrates:
     * - Multiple catch blocks (more specific exception before general exception)
     * - finally block execution
     * - Byte streams using ByteArrayInputStream and ByteArrayOutputStream
     */
    public static String demoUnit3ExceptionsAndByteStreams() {
        StringBuilder out = new StringBuilder();
        out.append("=== UNIT III: MULTI-CATCH EXCEPTIONS & BYTE STREAMS ===\n");

        // 1. Multiple Catch Blocks
        String[] testStrings = { "42", "invalid_number", null };
        for (String s : testStrings) {
            try {
                out.append("   Parsing string: '").append(s).append("' -> ");
                if (s == null) {
                    throw new NullPointerException("Input string reference was null!");
                }
                int parsed = Integer.parseInt(s);
                out.append("Successfully parsed integer: ").append(parsed).append("\n");
            } catch (NumberFormatException nfe) {
                out.append("Caught NumberFormatException: ").append(nfe.getMessage()).append("\n");
            } catch (NullPointerException npe) {
                out.append("Caught NullPointerException: ").append(npe.getMessage()).append("\n");
            } catch (Exception ex) {
                out.append("Caught General Exception: ").append(ex.getMessage()).append("\n");
            } finally {
                out.append("   [finally block executed: Cleanup guaranteed]\n");
            }
        }

        // 2. ByteArrayOutputStream / ByteArrayInputStream
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            byteOut.write("24CS202 Byte Stream Verification".getBytes());
            byte[] rawBytes = byteOut.toByteArray();

            ByteArrayInputStream byteIn = new ByteArrayInputStream(rawBytes);
            out.append("2. Byte Stream read: '");
            int ch;
            while ((ch = byteIn.read()) != -1) {
                out.append((char) ch);
            }
            out.append("' (Total ").append(rawBytes.length).append(" bytes transferred)\n");
        } catch (IOException e) {
            out.append("Byte stream error: ").append(e.getMessage()).append("\n");
        }

        return out.toString();
    }

    // =========================================================================
    // UNIT IV: MULTITHREADING LIFECYCLE & ADVANCED LAMBDAS
    // =========================================================================

    // 24CS202 Unit IV: Generic Functional Interface
    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }

    /**
     * Demonstrates:
     * - Thread creation, isAlive(), join(), priorities
     * - Custom Generic Functional Interface
     * - Block Lambdas and Variable Capture (effectively final)
     * - Method References
     */
    public static String demoUnit4ThreadsAndLambdas() {
        StringBuilder out = new StringBuilder();
        out.append("=== UNIT IV: THREAD LIFECYCLE & ADVANCED LAMBDA EXPRESSIONS ===\n");

        // 1. Thread lifecycle: isAlive(), join(), priority
        final List<String> threadLog = Collections.synchronizedList(new ArrayList<>());
        Thread worker = new Thread(() -> {
            threadLog.add("Worker executing in background...");
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
            threadLog.add("Worker completed task.");
        }, "SyllabusLifecycleThread");

        worker.setPriority(Thread.NORM_PRIORITY + 2); // Scheduling hint
        out.append(String.format("1. Thread created: Name='%s', Priority=%d, isAlive=%s\n",
                worker.getName(), worker.getPriority(), worker.isAlive()));

        worker.start();
        out.append(String.format("   After start(): isAlive=%s\n", worker.isAlive()));

        try {
            worker.join(); // Main thread waits until worker terminates
            out.append(String.format("   After join(): isAlive=%s\n", worker.isAlive()));
            out.append("   Thread log: ").append(threadLog).append("\n");
        } catch (InterruptedException e) {
            out.append("Thread interrupted: ").append(e.getMessage()).append("\n");
        }

        // 2. Generic Functional Interface with Block Lambda
        TriFunction<String, Integer, Double, String> feeCalculator = (name, daysOverdue, dailyRate) -> {
            // Block lambda with multiple statements
            double totalFine = daysOverdue * dailyRate;
            return String.format("Member: %s | Days: %d | Fine: ₹%.2f", name, daysOverdue, totalFine);
        };
        out.append("2. TriFunction Block Lambda: ").append(feeCalculator.apply("Student A", 4, 5.0)).append("\n");

        // 3. Variable Capture (effectively final variable from enclosing scope)
        String prefix = "Catalog Item: ";
        List<String> titles = Arrays.asList("Clean Code", "Design Patterns", "Effective Java");
        List<String> formattedTitles = titles.stream()
                .map(t -> prefix + t) // Captures 'prefix'
                .collect(Collectors.toList());
        out.append("3. Variable Capture in Lambda: ").append(formattedTitles).append("\n");

        // 4. Method References (Class::methodName)
        List<String> upper = titles.stream()
                .map(String::toUpperCase) // Method reference to instance method of arbitrary object
                .collect(Collectors.toList());
        out.append("4. Method Reference (String::toUpperCase): ").append(upper).append("\n");

        return out.toString();
    }

    // =========================================================================
    // UNIT V: COLLECTIONS (TREEMAP, LINKEDLIST, HASHSET) & ITERATORS
    // =========================================================================

    /**
     * Demonstrates:
     * - TreeMap (Sorted key map based on Red-Black Tree)
     * - LinkedList (Doubly-linked sequential structure)
     * - HashSet (Hash table backed unique elements)
     * - Explicit Iterator traversal vs Stream traversal
     */
    public static String demoUnit5CollectionsAndIterators() {
        StringBuilder out = new StringBuilder();
        out.append("=== UNIT V: TREEMAP, LINKEDLIST, HASHSET & ITERATORS ===\n");

        // 1. TreeMap (Maintains ascending natural key order)
        Map<String, String> sortedCategories = new TreeMap<>();
        sortedCategories.put("CS", "Computer Science & Engineering");
        sortedCategories.put("BIO", "Biomedical Engineering");
        sortedCategories.put("MECH", "Mechanical Engineering");
        sortedCategories.put("AI", "Artificial Intelligence & Data Science");

        out.append("1. TreeMap (Automatically Sorted Keys):\n");
        for (Map.Entry<String, String> entry : sortedCategories.entrySet()) {
            out.append(String.format("   Key: %-5s -> Value: %s\n", entry.getKey(), entry.getValue()));
        }

        // 2. LinkedList (Efficient insertions and queue operations)
        LinkedList<String> requestQueue = new LinkedList<>();
        requestQueue.addLast("Book Reservation 1");
        requestQueue.addLast("Book Reservation 2");
        requestQueue.addFirst("Priority Urgent Reservation");
        out.append("2. LinkedList Deque Order: ").append(requestQueue).append("\n");

        // 3. HashSet (Guarantees distinct elements)
        Set<String> uniqueIsbns = new HashSet<>();
        uniqueIsbns.add("978-0134685991");
        uniqueIsbns.add("978-0132350884");
        uniqueIsbns.add("978-0134685991"); // Duplicate ignored
        out.append("3. HashSet Uniqueness (Added 3 items with 1 duplicate, size=").append(uniqueIsbns.size()).append("): ")
                .append(uniqueIsbns).append("\n");

        // 4. Traditional Iterator traversal vs Stream
        out.append("4. Traditional Iterator Traversal:\n   ");
        Iterator<String> it = uniqueIsbns.iterator();
        while (it.hasNext()) {
            out.append("[").append(it.next()).append("] ");
        }
        out.append("\n");

        return out.toString();
    }

    /**
     * Main method enabling standalone command line execution.
     */
    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("  24CS202 OBJECT ORIENTED PROGRAMMING THROUGH JAVA - SYLLABUS DEMO");
        System.out.println("================================================================\n");

        System.out.println(demoUnit1PrimitivesAndArrays());
        System.out.println(demoUnit2StringsAndInnerClasses());
        System.out.println(demoUnit3ExceptionsAndByteStreams());
        System.out.println(demoUnit4ThreadsAndLambdas());
        System.out.println(demoUnit5CollectionsAndIterators());

        System.out.println("All syllabus demonstration modules executed successfully.");
    }
}
