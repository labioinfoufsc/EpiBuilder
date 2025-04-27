package ufsc.br.epibuilder.loader;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ufsc.br.epibuilder.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
@Order(1)
public class DataLoader implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    private final PasswordEncoder passwordEncoder;

    public DataLoader(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        if (shouldSkipDataLoading(args)) {
            System.out.println("DataLoader: Initial data loading disabled by argument");
            return;
        }

        Long userCount = (Long) entityManager.createQuery("SELECT COUNT(u) FROM User u").getSingleResult();

        if (userCount == 0) {
            System.out.println("DataLoader: Starting initial data loading...");
            loadInitialData();
            System.out.println("DataLoader: Initial data loaded successfully!");
        } else {
            System.out.println("DataLoader: Database already contains data. No initial data was loaded.");
        }
    }

    private boolean shouldSkipDataLoading(String... args) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--skipDataLoader")) {
                return true;
            }
        }
        return false;
    }

    private void loadInitialData() {
        User admin = createUser("Admin", "admin", "admin123", Role.ADMIN);
        User regularUser = createUser("User", "user", "user123", Role.USER);
        String filePath = "/pipeline/db/iedb.fasta";
        loadDatabaseFile(filePath);
    }

    private User createUser(String name, String username, String password, Role role) {
        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        entityManager.persist(user);
        return user;
    }

    private void loadDatabaseFile(String filePath) {
        File file = new File(filePath);

        if (file.exists() && file.isFile()) {
            // Criar o objeto Database
            Database database = new Database();
            database.setAlias("iedb");
            database.setFileName(file.getName());
            database.setAbsolutePath(file.getAbsolutePath());
            database.setDate(LocalDateTime.now());

            // Persistir no banco de dados
            entityManager.persist(database);
            System.out.println("DataLoader: File loaded and persisted: " + file.getName());
        } else {
            System.out.println("DataLoader: File does not exist: " + filePath);
        }
    }
}
