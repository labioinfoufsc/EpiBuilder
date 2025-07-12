package ufsc.br.epibuilder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ufsc.br.epibuilder.model.Database;
import ufsc.br.epibuilder.repository.DatabaseRepository;
import java.time.ZonedDateTime;

@SpringBootTest
@ActiveProfiles("test")
public class DatabaseServiceTest {

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private DatabaseRepository databaseRepository;

    private Database testDatabase;

    @BeforeEach
    void setUp() {
        databaseRepository.deleteAll();

        testDatabase = new Database();
        testDatabase.setAlias("test-db");
        testDatabase.setFileName("test-file.sql");
        testDatabase.setAbsolutePath("/path/to/test-file.sql");
        LocalDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDateTime();
        testDatabase.setDate(now);
    }

    @AfterEach
    void tearDown() {
        databaseRepository.deleteAll();
    }

    @Test
    void saveShouldPersistDatabase() {
        Database savedDatabase = databaseService.save(testDatabase);

        assertNotNull(savedDatabase.getId());
        assertEquals(testDatabase.getAlias(), savedDatabase.getAlias());
        assertEquals(testDatabase.getFileName(), savedDatabase.getFileName());
        assertEquals(testDatabase.getAbsolutePath(), savedDatabase.getAbsolutePath());

        Optional<Database> foundDatabase = databaseRepository.findById(savedDatabase.getId().toString());
        assertTrue(foundDatabase.isPresent());
        assertEquals(savedDatabase.getId(), foundDatabase.get().getId());
    }

    @Test
    void getAllShouldReturnAllDatabases() {
        databaseService.save(testDatabase);

        Database anotherDatabase = new Database();
        anotherDatabase.setAlias("another-db");
        anotherDatabase.setFileName("another-file.sql");
        anotherDatabase.setAbsolutePath("/path/to/another-file.sql");
        LocalDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDateTime();
        anotherDatabase.setDate(now);
        databaseService.save(anotherDatabase);

        List<Database> databases = databaseService.getAll();

        assertEquals(2, databases.size());
    }

    @Test
    void getByIdShouldReturnCorrectDatabase() {
        Database savedDatabase = databaseService.save(testDatabase);

        Database foundDatabase = databaseService.getById(savedDatabase.getId());

        assertNotNull(foundDatabase);
        assertEquals(savedDatabase.getId(), foundDatabase.getId());
        assertEquals(savedDatabase.getAlias(), foundDatabase.getAlias());
    }

    @Test
    void getByAliasShouldReturnCorrectDatabase() {
        Database savedDatabase = databaseService.save(testDatabase);

        Database foundDatabase = databaseService.getByAlias(savedDatabase.getAlias());

        assertNotNull(foundDatabase);
        assertEquals(savedDatabase.getId(), foundDatabase.getId());
        assertEquals(savedDatabase.getAlias(), foundDatabase.getAlias());
    }

    @Test
    void deleteByIdShouldRemoveDatabase() {
        Database savedDatabase = databaseService.save(testDatabase);
        Long id = savedDatabase.getId();

        databaseService.deleteById(id);

        Optional<Database> deletedDatabase = databaseRepository.findById(id.toString());
        assertFalse(deletedDatabase.isPresent());
    }
}
