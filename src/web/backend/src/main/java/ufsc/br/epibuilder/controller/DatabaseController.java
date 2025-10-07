package ufsc.br.epibuilder.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;

import java.util.HashMap;
import java.util.Map;
import ufsc.br.epibuilder.model.Status;
import ufsc.br.epibuilder.model.TaskStatus;

import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ufsc.br.epibuilder.model.Database;
import ufsc.br.epibuilder.service.DatabaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import java.nio.file.StandardCopyOption;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Comparator;
import java.time.ZonedDateTime;

@RestController
@Slf4j
@RequestMapping("/dbs")
public class DatabaseController {

    private final DatabaseService databaseService;

    public DatabaseController(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * Retrieves a list of all databases.
     *
     * @return a ResponseEntity containing the list of databases or an internal
     *         server error status
     */
    @GetMapping
    public ResponseEntity<List<Database>> getAll() {
        try {
            log.info("Attempting to list all databases...");
            List<Database> databases = databaseService.getAll();
            return ResponseEntity.ok(databases);
        } catch (Exception e) {
            log.error("Error listing databases: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Database> create(
            @RequestPart("data") Database database,
            @RequestPart("file") MultipartFile file) {
        try {
            log.info("Attempting to create a new database with name: {}", database.getFileName());

            Path databasesDir = Paths.get(System.getProperty("user.dir"), "databases");
            if (Files.notExists(databasesDir)) {
                Files.createDirectories(databasesDir);
            }

            String sanitizedFilename = Paths.get(file.getOriginalFilename()).getFileName().toString();
            Path destinationFile = databasesDir.resolve(sanitizedFilename);

            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved to: {}", destinationFile.toAbsolutePath());

            database.setAbsolutePath(destinationFile.toString());
            LocalDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDateTime();
            database.setDate(now);
            database.setFileName(sanitizedFilename);

            Database createdDatabase = databaseService.save(database);

            return ResponseEntity.status(HttpStatus.CREATED).body(createdDatabase);
        } catch (Exception e) {
            log.error("Error creating database: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            Database db = databaseService.getById(id);

            if (db == null) {
                return ResponseEntity.notFound().build();
            }

            String absolutePath = db.getAbsolutePath();

            if (absolutePath != null) {
                try {
                    Path path = Path.of(absolutePath);
                    if (Files.exists(path)) {
                        Files.walk(path)
                                .sorted(Comparator.reverseOrder())
                                .forEach(p -> {
                                    try {
                                        Files.delete(p);
                                    } catch (IOException e) {
                                        throw new RuntimeException("Error while deleting: " + p, e);
                                    }
                                });
                    }
                } catch (Exception e) {
                    log.error("Error while deleting directory: {}", e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            }

            databaseService.deleteById(id);

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error while deleting database: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
