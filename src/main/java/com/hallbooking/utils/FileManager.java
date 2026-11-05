package com.hallbooking.utils;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * FileManager — the single point of contact for all .txt file I/O.
 *
 * Path resolution strategy (in order):
 *   1. Look for the data/ folder next to the running JAR (production)
 *   2. Look for the data/ folder at the project root (IDE / Maven run)
 *   3. Fall back to the current working directory
 *
 * This means the app works correctly whether you run it from IntelliJ,
 * from "mvn javafx:run", or as a packaged JAR.
 */
public class FileManager {

    // ── Resolved at startup — never changes during runtime ────────────
    private static final String DATA_DIR;

    static {
        DATA_DIR = resolveDataDir();
        // Create the data directory if it somehow doesn't exist yet
        new File(DATA_DIR).mkdirs();
        System.out.println("FileManager: data directory → " + DATA_DIR);
    }

    /**
     * Finds the correct absolute path for the data/ folder.
     *
     * Priority:
     *  1. Sibling of the JAR file (e.g. deployed anywhere)
     *  2. Project root determined by walking up from the compiled class location
     *  3. Current working directory as last resort
     */
    private static String resolveDataDir() {
        // Strategy 1 — find where the running JAR / class files live
        try {
            File classLocation = new File(
                FileManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());

            // Walk upwards until we find a directory that contains "data/"
            File candidate = classLocation.isFile()
                    ? classLocation.getParentFile()   // next to JAR
                    : classLocation;                  // classes/ directory

            for (int i = 0; i < 6; i++) {            // at most 6 levels up
                File dataFolder = new File(candidate, "data");
                if (dataFolder.exists() && dataFolder.isDirectory()) {
                    return dataFolder.getAbsolutePath() + File.separator;
                }
                if (candidate.getParentFile() == null) break;
                candidate = candidate.getParentFile();
            }
        } catch (URISyntaxException ignored) {}

        // Strategy 2 — working directory (mvn javafx:run sets this to project root)
        File wdData = new File(System.getProperty("user.dir"), "data");
        if (wdData.exists()) {
            return wdData.getAbsolutePath() + File.separator;
        }

        // Strategy 3 — create data/ in working directory
        return wdData.getAbsolutePath() + File.separator;
    }

    // ── File path constants ───────────────────────────────────────────
    public static final String USERS        = DATA_DIR + "users.txt";
    public static final String CUSTOMERS    = DATA_DIR + "customers.txt";
    public static final String STAFF        = DATA_DIR + "staff.txt";
    public static final String HALLS        = DATA_DIR + "halls.txt";
    public static final String AVAILABILITY = DATA_DIR + "availability.txt";
    public static final String BOOKINGS     = DATA_DIR + "bookings.txt";
    public static final String PAYMENTS     = DATA_DIR + "payments.txt";
    public static final String MAINTENANCE  = DATA_DIR + "maintenance.txt";
    public static final String ISSUES          = DATA_DIR + "issues.txt";
    public static final String ISSUE_ASSIGN    = DATA_DIR + "issue_assignments.txt";
    public static final String PAYMENT_DETAILS = DATA_DIR + "payment_details.txt";
    public static final String ACTIVITY_LOG    = DATA_DIR + "activity_log.txt";
    public static final String NOTIFICATIONS   = DATA_DIR + "notifications.txt";

    // ── Ensure a file exists (creates it empty if not) ────────────────
    private static void ensureFileExists(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs(); // create data/ dir if needed
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("FileManager: could not create " + filePath
                        + " — " + e.getMessage());
            }
        }
    }

    // ── Read all non-blank lines from a file ──────────────────────────
    public static List<String> readLines(String filePath) {
        List<String> lines = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) return lines;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) lines.add(line);
            }
        } catch (IOException e) {
            System.err.println("FileManager.readLines ERROR: "
                    + filePath + " — " + e.getMessage());
        }
        return lines;
    }

    // ── Overwrite entire file with given lines ────────────────────────
    public static void writeLines(String filePath, List<String> lines) {
        ensureFileExists(filePath);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, false))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("FileManager.writeLines ERROR: "
                    + filePath + " — " + e.getMessage());
        }
    }

    // ── Append a single line to a file ───────────────────────────────
    public static void appendLine(String filePath, String line) {
        ensureFileExists(filePath);
        try {
            File file = new File(filePath);

            // If file is non-empty and doesn't end with a newline,
            // add one first so the new record isn't glued to the last line
            if (file.length() > 0) {
                try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                    raf.seek(file.length() - 1);
                    if (raf.read() != '\n') {
                        try (BufferedWriter fix =
                                new BufferedWriter(new FileWriter(file, true))) {
                            fix.newLine();
                        }
                    }
                }
            }

            try (BufferedWriter bw =
                    new BufferedWriter(new FileWriter(file, true))) {
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("FileManager.appendLine ERROR: "
                    + filePath + " — " + e.getMessage());
        }
    }

    // ── Update one line by ID prefix ──────────────────────────────────
    public static boolean updateLine(String filePath, String id, String newLine) {
        List<String> lines = readLines(filePath);
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(id + "|")) {
                lines.set(i, newLine);
                found = true;
                break;
            }
        }
        if (found) writeLines(filePath, lines);
        return found;
    }

    // ── Delete one line by ID prefix ─────────────────────────────────
    public static boolean deleteLine(String filePath, String id) {
        List<String> lines = readLines(filePath);
        boolean removed = lines.removeIf(l -> l.startsWith(id + "|"));
        if (removed) writeLines(filePath, lines);
        return removed;
    }

    // ── Find a single line by ID prefix ──────────────────────────────
    public static String findById(String filePath, String id) {
        for (String line : readLines(filePath)) {
            if (line.startsWith(id + "|")) return line;
        }
        return null;
    }

    // ── Find lines where a specific field index matches a value ──────
    public static List<String> findByField(String filePath,
                                            int fieldIndex, String value) {
        List<String> result = new ArrayList<>();
        for (String line : readLines(filePath)) {
            String[] parts = line.split("\\|");
            if (parts.length > fieldIndex && parts[fieldIndex].equals(value)) {
                result.add(line);
            }
        }
        return result;
    }

    // ── Generate next ID from last record in file ─────────────────────
    public static String getNextId(String prefix, String filePath) {
        List<String> lines = readLines(filePath);
        if (lines.isEmpty()) return prefix + "001";
        String lastId = lines.get(lines.size() - 1).split("\\|")[0];
        try {
            int num = Integer.parseInt(lastId.replaceAll("[^0-9]", ""));
            return prefix + String.format("%03d", num + 1);
        } catch (NumberFormatException e) {
            return prefix + String.format("%03d", lines.size() + 1);
        }
    }
}
