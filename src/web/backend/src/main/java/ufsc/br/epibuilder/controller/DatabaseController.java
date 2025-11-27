package ufsc.br.epibuilder.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import ufsc.br.epibuilder.model.Database;
import ufsc.br.epibuilder.service.DatabaseService;
import ufsc.br.epibuilder.service.UniProtService;
import ufsc.br.epibuilder.dto.UniProtDownloadStatus;
import ufsc.br.epibuilder.helper.HelperMethods;

@RestController
@Slf4j
@RequestMapping("/dbs")
public class DatabaseController {

    private final DatabaseService databaseService;
    private final UniProtService uniProtService;

    private static final String DB_DIRECTORY = "/tmp/epibuilder/db";

    public DatabaseController(DatabaseService databaseService, UniProtService uniProtService) {
        this.databaseService = databaseService;
        this.uniProtService = uniProtService;
    }

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

    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(DB_DIRECTORY, fileName);
            if (!Files.exists(filePath)) {
                log.warn("Attempt to download non-existent file: {}", fileName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            byte[] fileBytes = Files.readAllBytes(filePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .body(fileBytes);
        } catch (IOException e) {
            log.error("Error downloading file {}: {}", fileName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Database> create(
            @RequestPart("data") Database database,
            @RequestPart("file") MultipartFile file) {
        try {
            log.info("Attempting to create a new database with alias: {}", database.getAlias());

            Path databasesDir = Paths.get(DB_DIRECTORY);
            if (Files.notExists(databasesDir)) {
                Files.createDirectories(databasesDir);
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return ResponseEntity.badRequest().build();
            }
            String sanitizedFilename = Paths.get(originalFilename).getFileName().toString();
            Path destinationFile = databasesDir.resolve(sanitizedFilename);

            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            database.setAbsolutePath(destinationFile.toString());
            LocalDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDateTime();
            database.setDate(now);
            database.setFileName(sanitizedFilename);

            int sequenceCount = HelperMethods.countSequences(destinationFile);
            database.setAmountSequences(sequenceCount);

            Database createdDatabase = databaseService.save(database);
            log.info("Database saved with ID: {}", createdDatabase.getId());

            // --- Start BLAST database creation (Asynchronous or background) ---
            log.info("Creating BLAST database for file: {}", destinationFile);
            ProcessBuilder pb = new ProcessBuilder(
                    "makeblastdb",
                    "-in", destinationFile.toString(),
                    "-dbtype", "prot",
                    "-out", destinationFile.toString()
            );
            Process process = pb.start();
            log.info("BLAST database process started with PID: {}", process.pid());

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
                Path filePath = Paths.get(absolutePath);

                String baseName = filePath.toString();

                try {
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                        log.info("Deleted main FASTA file: {}", filePath);
                    }

                    String[] blastExtensions = { ".phr", ".pin", ".psq" };
                    for (String ext : blastExtensions) {
                        Path blastFilePath = Paths.get(baseName + ext);
                        if (Files.exists(blastFilePath)) {
                            Files.delete(blastFilePath);
                            log.info("Deleted associated BLAST index file: {}", blastFilePath);
                        }
                    }

                } catch (IOException e) {
                    log.error("Error while deleting physical files for database {}: {}", id, e.getMessage());
                }
            }

            databaseService.deleteById(id);
            log.info("Database record deleted successfully for ID: {}", id);

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error while deleting database: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- UniProt Download Endpoints ---

    @PostMapping("/download/uniprot")
    public ResponseEntity<String> triggerUniProtDownload() {
        UniProtDownloadStatus status = uniProtService.getStatus();
        if (status.isInProgress()) {
            return new ResponseEntity<>("UniProt download is already in progress.", HttpStatus.ACCEPTED);
        }

        uniProtService.startDownload();
        log.info("Manual UniProt download initiated by user.");

        return new ResponseEntity<>("UniProt download initiated successfully.", HttpStatus.ACCEPTED);
    }

    @GetMapping("/download/uniprot/status")
    public ResponseEntity<UniProtDownloadStatus> getUniProtDownloadStatus() {
        return ResponseEntity.ok(uniProtService.getStatus());
    }

}
            