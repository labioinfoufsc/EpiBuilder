package ufsc.br.epibuilder.loader;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ufsc.br.epibuilder.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Component responsible for the initial system setup and data seeding.
 * *
 * <p>
 * This runner executes on startup before the application signals that it is
 * ready.
 * It performs the following critical tasks:
 * <ol>
 * <li>Creates default administrative and regular users.</li>
 * <li>Downloads and decompresses the UniProt Swiss-Prot database if
 * missing.</li>
 * <li>Registers local FASTA files into the database.</li>
 * </ol>
 * *
 * <p>
 * <strong>Note:</strong> The application startup is blocked during the UniProt
 * download process.
 */
@Component
@Order(1)
public class DataLoader implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    private final PasswordEncoder passwordEncoder;

    private static final String UNIPROT_URL = "https://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.fasta.gz";
    private static final String DB_DIRECTORY = "/tmp/epibuilder/db";
    private static final String TIMEZONE = "America/Sao_Paulo";
    private static final int BUFFER_SIZE = 8192; // 8KB buffer for streams
    private static final long LOG_THRESHOLD_BYTES = 10 * 1024 * 1024; // Log progress every 10MB

    public DataLoader(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Entry point for the initialization logic.
     * Blocks the main thread, effectively preventing the application from accepting
     * requests until the download and setup are complete.
     */
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (shouldSkip(args)) {
            log("Skipping data loader via arguments.");
            return;
        }

        long userCount = (long) entityManager.createQuery("SELECT COUNT(u) FROM User u").getSingleResult();

        if (userCount == 0) {
            log("System is empty. Starting strict initialization sequence...");
            log("WARNING: Application startup is PAUSED until resources are downloaded.");

            initializeSystem();

            log("Initialization finished. Application is now RESUMING startup.");
        } else {
            log("System data found. Skipping initialization.");
        }
    }

    private boolean shouldSkip(String... args) {
        for (String arg : args) {
            if ("--skipDataLoader".equalsIgnoreCase(arg))
                return true;
        }
        return false;
    }

    private void initializeSystem() {
        createDefaultUsers();
        downloadUniProtIfMissing();
        registerLocalDatabases();
    }

    private void createDefaultUsers() {
        persistUser("Admin", "admin", "admin", Role.ADMIN);
        persistUser("User", "user", "user", Role.USER);
        log("Default users created.");
    }

    /**
     * Checks if a UniProt database exists locally. If not, initiates the download
     * stream.
     * The file is named with the current date (versioning).
     */
    private void downloadUniProtIfMissing() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String fileName = "uniprot_sprot_" + today + ".fasta";
        Path targetPath = Paths.get(DB_DIRECTORY, fileName);
        Path directory = Paths.get(DB_DIRECTORY);

        ensureDirectoryExists(directory);

        if (checkIfUniProtExists(directory)) {
            log("UniProt database already exists locally. Skipping download.");
            return;
        }

        log("UniProt not found. Starting download/decompression stream from: " + UNIPROT_URL);
        try {
            downloadAndDecompress(UNIPROT_URL, targetPath);
        } catch (IOException e) {
            logError("Failed to download UniProt. The system may lack essential data.", e);
        }
    }

    private void ensureDirectoryExists(Path dir) {
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                logError("Failed to create directory: " + dir, e);
            }
        }
    }

    private boolean checkIfUniProtExists(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            return files.anyMatch(p -> p.getFileName().toString().startsWith("uniprot_sprot_"));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Downloads a GZIP file and decompresses it on-the-fly to the target path.
     * Uses a temporary file (.tmp) to ensure atomicity; the file is only renamed
     * to .fasta upon successful completion.
     */
    private void downloadAndDecompress(String urlString, Path targetPath) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        Path tempPath = targetPath.resolveSibling(targetPath.getFileName() + ".tmp");

        try (InputStream in = conn.getInputStream();
                GZIPInputStream gzipIn = new GZIPInputStream(in);
                FileOutputStream out = new FileOutputStream(tempPath.toFile())) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalBytes = 0;
            long lastLogBytes = 0;
            long startTime = System.currentTimeMillis();

            log("Download started. Please wait...");

            while ((bytesRead = gzipIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;

                if (totalBytes - lastLogBytes > LOG_THRESHOLD_BYTES) {
                    double mb = totalBytes / (1024.0 * 1024.0);
                    log(String.format("Downloading & Decompressing: %.2f MB processed...", mb));
                    lastLogBytes = totalBytes;
                }
            }

            long duration = (System.currentTimeMillis() - startTime) / 1000;
            log(String.format("Download completed in %d seconds. Finalizing file...", duration));
        }

        Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        log("UniProt Swiss-Prot saved successfully at: " + targetPath);
    }

    /**
     * Scans the database directory for valid FASTA files and registers them.
     */
    private void registerLocalDatabases() {
        Path dir = Paths.get(DB_DIRECTORY);
        if (!Files.exists(dir))
            return;

        try (Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".fasta"))
                    .forEach(this::processAndPersistDatabase);
        } catch (IOException e) {
            logError("Error scanning database directory", e);
        }
    }

    private void processAndPersistDatabase(Path path) {
        String fileName = path.getFileName().toString();
        String alias = fileName.replace(".fasta", "");

        // Applies specific naming convention for the auto-downloaded UniProt file
        if (fileName.startsWith("uniprot_sprot_")) {
            // Extract date: uniprot_sprot_2025-11-27.fasta -> 2025-11-27
            String datePart = fileName.substring(14, 24);
            alias = "UniProt Swiss-Prot (Reviewed) - " + datePart;
        }

        persistDatabaseMetadata(path.toString(), alias);
    }

    private void persistDatabaseMetadata(String filePath, String alias) {
        File file = new File(filePath);

        long exists = (long) entityManager.createQuery("SELECT COUNT(d) FROM Database d WHERE d.absolutePath = :path")
                .setParameter("path", file.getAbsolutePath())
                .getSingleResult();

        if (exists > 0)
            return;

        if (file.exists()) {
            try {
                Database db = new Database();
                db.setAlias(alias);
                db.setFileName(file.getName());
                db.setAbsolutePath(file.getAbsolutePath());
                db.setDate(ZonedDateTime.now(ZoneId.of(TIMEZONE)).toLocalDateTime());

                log("Counting sequences for: " + alias);
                db.setAmountSequences(countSequences(file.toPath()));

                entityManager.persist(db);
                log("Registered database: " + alias);
            } catch (IOException e) {
                logError("Failed to process file: " + file.getName(), e);
            }
        }
    }

    private int countSequences(Path path) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()), BUFFER_SIZE * 4)) {
            int count = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(">"))
                    count++;
            }
            return count;
        }
    }

    private void persistUser(String name, String username, String pass, Role role) {
        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(pass));
        user.setRole(role);
        entityManager.persist(user);
    }

    private void log(String message) {
        System.out.println("[DataLoader] " + message);
    }

    private void logError(String message, Exception e) {
        System.err.println("[DataLoader] ERROR: " + message);
        e.printStackTrace();
    }
}