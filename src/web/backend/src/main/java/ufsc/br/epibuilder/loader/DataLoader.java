package ufsc.br.epibuilder.loader;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ufsc.br.epibuilder.model.*;
import ufsc.br.epibuilder.service.SystemStatusService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

/**
 * Component responsible for the initial system setup and data seeding.
 *
 * This runner executes on startup before the application signals that it is
 * ready.
 * It performs the following critical tasks:
 * <ol>
 * <li>Creates default administrative and regular users if the system is
 * empty.</li>
 * <li>Registers local FASTA files (pre-downloaded by entrypoint.sh or manually
 * uploaded) into the database.</li>
 * </ol>
 *
 */
@Component
@Slf4j
@Order(1)
public class DataLoader implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    private final PasswordEncoder passwordEncoder;
    private final SystemStatusService systemStatusService;

    // Database directory where the entrypoint.sh saves the file and local files are
    // stored
    private static final String DB_DIRECTORY = "/tmp/epibuilder/db";
    private static final String TIMEZONE = "America/Sao_Paulo";
    private static final int BUFFER_SIZE = 8192; // 8KB buffer for streams

    public DataLoader(PasswordEncoder passwordEncoder, SystemStatusService systemStatusService) {
        this.passwordEncoder = passwordEncoder;
        this.systemStatusService = systemStatusService;
    }

    /**
     * Entry point for the initialization logic.
     */
    @Override
    @Transactional
    public void run(String... args) {
        if (shouldSkip(args)) {
            log.info("Skipping data loader via arguments.");
            return;
        }

        long userCount = (long) entityManager.createQuery("SELECT COUNT(u) FROM User u").getSingleResult();

        systemStatusService.setStatus("STARTING", "Checking required initial data.");

        try {
            if (userCount == 0) {
                log.info("System is empty. Starting strict initialization sequence...");

                initializeSystem();

                log.info("Initialization finished. Application is now RESUMING startup.");
            } else {
                log.info("System data found. Skipping user initialization.");
            }
            // Database registration should always run to catch files added by entrypoint or
            // manual updates
            registerLocalDatabases();
        } catch (Exception e) {
            log.info("Fatal error during data loading.", e);
        } finally {
            systemStatusService.setStatus("READY", "System is fully operational.");
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
    }

    private void createDefaultUsers() {
        persistUser("Admin", "admin", "admin", Role.ADMIN);
        persistUser("User", "user", "user", Role.USER);
        log.info("Default users created.");
    }

    /**
     * Scans the database directory for valid FASTA files and registers them if not
     * already in the DB.
     */
    private void registerLocalDatabases() {
        Path dir = Paths.get(DB_DIRECTORY);
        if (!Files.exists(dir)) {
            log.info("Database directory /db does not exist. Skipping file registration.");
            return;
        }

        try (Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".fasta"))
                    .forEach(this::processAndPersistDatabase);
        } catch (IOException e) {
            log.info("Error scanning database directory: " + dir, e);
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

                log.info("Counting sequences for: " + alias);
                db.setAmountSequences(countSequences(file.toPath()));

                entityManager.persist(db);
                log.info("Registered database: " + alias);
            } catch (IOException e) {
                log.info("Failed to process file: " + file.getName(), e);
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

}