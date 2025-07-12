package ufsc.br.epibuilder.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.config.Task;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import ufsc.br.epibuilder.model.*;
import ufsc.br.epibuilder.service.*;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

import org.springframework.http.MediaType;
import java.io.UncheckedIOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.Comparator;
import java.time.ZonedDateTime;

@RestController
@Slf4j
@RequestMapping("/epitopes")
public class EpitopeController {

    private final EpitopeTaskDataService epitopeTaskDataService;

    private final PipelineService pipelineService;

    private final DatabaseService databaseService;

    public EpitopeController(EpitopeTaskDataService epitopeTaskDataService, PipelineService pipelineService,
            DatabaseService databaseService) {
        this.databaseService = databaseService;
        this.epitopeTaskDataService = epitopeTaskDataService;
        this.pipelineService = pipelineService;
    }

    private Path saveFile(Path baseDir, MultipartFile file) throws IOException {
        Path filePath = baseDir.resolve(file.getOriginalFilename());
        file.transferTo(filePath.toFile());
        log.info("File saved: {}", filePath);
        return filePath;
    }

    @PostMapping(value = "/tasks/new", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> newEpitopeTask(
            @RequestPart("data") String taskDataJson,
            @RequestPart("file") MultipartFile fastaFile,
            @RequestPart(value = "proteomes", required = false) MultipartFile[] proteomes) {

        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        EpitopeTaskData taskData;
        try {
            taskData = objectMapper.readValue(taskDataJson, EpitopeTaskData.class);
        } catch (JsonProcessingException e) {
            log.error("JSON parsing error", e);
            return errorResponse("Invalid request format", HttpStatus.BAD_REQUEST);
        }

        if (taskData.getUser() == null || taskData.getUser().getId() == null) {
            return errorResponse("Login expired. Please log in again.", HttpStatus.BAD_REQUEST);
        }

        try {

            log.info("Received new task request: {}", taskData);
            log.info("Fasta file: {}", fastaFile.getOriginalFilename());
            log.info("Proteomes: {}", proteomes != null ? proteomes.length : 0);

            if (fastaFile.isEmpty()) {
                return errorResponse("Fasta file is empty.", HttpStatus.BAD_REQUEST);
            }

            if (proteomes != null && proteomes.length > 0) {
                for (MultipartFile proteome : proteomes) {
                    if (proteome.isEmpty()) {
                        return errorResponse("One or more proteome files are empty.", HttpStatus.BAD_REQUEST);
                    }
                }
            }

        } catch (

        IllegalArgumentException e) {
            log.error("Database error: {}", e.getMessage());
            return errorResponse("Database configuration error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            return errorResponse("Unexpected error processing request", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            Path baseDir = prepareBaseDirectory(taskData);

            Path fastaPath = saveFile(baseDir, fastaFile);
            taskData.setFile(fastaPath.toFile());
            taskData.setAbsolutePath(fastaPath.toString());

            if (taskData.isDoBlast() == true) {
                processProteomes(taskData, baseDir, proteomes);
            }

            Process process = pipelineService.runPipeline(taskData);
            EpitopeTaskData savedTask = saveTask(taskData, process);

            return successResponse(savedTask);

        } catch (IOException e) {
            log.error("IO Error: {}", e.getMessage(), e);
            return errorResponse("Error while processing files: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Erro inesperado: {}", e.getMessage(), e);
            return errorResponse("Internal server error: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Path prepareBaseDirectory(EpitopeTaskData taskData) throws IOException {
        String username = taskData.getUser().getUsername();
        String timestamp = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"))
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path baseDir = Paths.get("/www", username, taskData.getRunName() + "_" + timestamp);

        Files.createDirectories(baseDir);
        log.info("Directory created: {}", baseDir);
        taskData.setCompleteBasename(baseDir.toString());
        return baseDir;
    }

    private void processProteomes(EpitopeTaskData taskData, Path baseDir,
            MultipartFile[] proteomes) throws IOException {
        if (taskData.getProteomes() == null || taskData.getProteomes().isEmpty()) {
            return;
        }

        log.info("Processing proteomes: {}", taskData.getProteomes());

        Path proteomesDir = baseDir.resolve("proteomes");
        Files.createDirectories(proteomesDir);

        List<Database> processedDatabases = new ArrayList<>();
        int fileIndex = 0;

        for (Database proteome : taskData.getProteomes()) {
            log.info("Processing proteome - Type: {}, Alias: {}", proteome.getSourceType(), proteome.getAlias());

            if ("fasta_blast".equals(proteome.getSourceType())) { // Alterado de "fasta_file" para "fasta_blast"
                // Lógica para novo arquivo enviado pelo usuário
                if (proteomes == null || fileIndex >= proteomes.length) {
                    throw new IllegalArgumentException("No proteome file provided for: " + proteome.getAlias());
                }

                MultipartFile proteomeFile = proteomes[fileIndex++];
                if (proteomeFile.isEmpty()) {
                    throw new IllegalArgumentException("Empty proteome file for: " + proteome.getAlias());
                }

                Database db = new Database();
                db.setAlias(proteome.getAlias());
                db.setSourceType("fasta_blast"); // Definindo explicitamente o tipo

                // Sanitiza o nome do arquivo
                String sanitizedFilename = Paths.get(proteomeFile.getOriginalFilename())
                        .getFileName().toString();
                Path proteomePath = proteomesDir.resolve(sanitizedFilename);

                // Salva o arquivo
                Files.copy(proteomeFile.getInputStream(), proteomePath,
                        StandardCopyOption.REPLACE_EXISTING);

                // Configura o objeto Database
                db.setFileName(sanitizedFilename);
                db.setAbsolutePath(proteomePath.toString());
                LocalDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDateTime();
                db.setDate(now);

                log.info("New proteome file saved: {}", db.toString());

                processedDatabases.add(db);

            } else if ("database".equals(proteome.getSourceType())) {
                Database existingDb = databaseService.getByAlias(proteome.getAlias());
                if (existingDb == null) {
                    throw new IllegalArgumentException("Database not found: " + proteome.getAlias());
                }
                processedDatabases.add(existingDb);
                log.info("Existing database found: {}", existingDb.getAbsolutePath());
            } else {
                throw new IllegalArgumentException("Unknown proteome source type: " + proteome.getSourceType());
            }
        }

        taskData.setProteomes(processedDatabases);
        log.info("Final processed proteomes: {}", processedDatabases);
    }

    private EpitopeTaskData saveTask(EpitopeTaskData taskData, Process process) {
        TaskStatus taskStatus = new TaskStatus();
        taskStatus.setPid(process.pid());
        taskStatus.setStatus(Status.RUNNING);
        taskStatus.setEpitopeTaskData(taskData);

        taskData.setTaskStatus(taskStatus);
        LocalDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDateTime();
        taskData.setExecutionDate(now);

        return epitopeTaskDataService.save(taskData);
    }

    private ResponseEntity<Map<String, Object>> successResponse(EpitopeTaskData savedTask) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Task created. PID: " + savedTask.getTaskStatus().getPid());
        response.put("taskId", savedTask.getId());
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> errorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/tasks/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        try {
            EpitopeTaskData task = epitopeTaskDataService.findById(id);
            Path taskDir = Paths.get(task.getCompleteBasename()).normalize();

            if (!Files.exists(taskDir)) {
                return ResponseEntity.notFound().build();
            }
            if (!Files.isDirectory(taskDir)) {
                return ResponseEntity.badRequest().body(null);
            }

            String originalDirName = taskDir.getFileName().toString();
            String zipFileName = originalDirName + ".zip";

            Path zipFilePath = Files.createTempFile("download_", ".zip");

            try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
                Files.walk(taskDir)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(filePath -> {
                            try {
                                String entryName = taskDir.relativize(filePath).toString();
                                zipOut.putNextEntry(new ZipEntry(entryName));
                                Files.copy(filePath, zipOut);
                                zipOut.closeEntry();
                            } catch (IOException e) {
                                throw new UncheckedIOException("Error adding file to ZIP: " + filePath, e);
                            }
                        });
            }

            Resource resource = new UrlResource(zipFilePath.toUri());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + zipFileName + "\"")
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                            HttpHeaders.CONTENT_DISPOSITION)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(Files.size(zipFilePath))
                    .body(resource);

        } catch (UncheckedIOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/tasks/{id}/log")
    public ResponseEntity<?> getTaskLog(@PathVariable Long id) {
        EpitopeTaskData task = epitopeTaskDataService.findById(id);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        Path taskDir = Paths.get(task.getCompleteBasename()).normalize();
        if (!Files.exists(taskDir) || !Files.isDirectory(taskDir)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Task directory not found or is not a directory");
        }

        Path logFile = taskDir.resolve("pipeline.log");
        if (!Files.exists(logFile)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Log file not found");
        }

        try {
            String logContent = Files.readString(logFile);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(logContent);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading log file: " + e.getMessage());
        }
    }

    @GetMapping("/tasks/user/{userId}")
    public ResponseEntity<?> getTasksByUser(@PathVariable Long userId) {
        List<EpitopeTaskData> tasks = epitopeTaskDataService.findTasksByUserId(userId);
        if (tasks.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tasks);
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Map<String, String>> deleteTask(@PathVariable Long id) {
        try {
            EpitopeTaskData taskFound = epitopeTaskDataService.findById(id);
            if (taskFound == null) {
                return ResponseEntity.notFound().build();
            }

            epitopeTaskDataService.deleteEpitopeTaskDataWithAssociations(id);

            this.deleteTaskFiles(taskFound.getCompleteBasename());

            return ResponseEntity.ok(Map.of(
                    "message", "Task and all associated data deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting task {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete task: " + e.getMessage()));
        }
    }

    private void deleteTaskFiles(String completeBasename) throws IOException {
        if (completeBasename == null || completeBasename.isEmpty()) {
            return;
        }

        Path directoryPath = Paths.get(completeBasename).normalize();
        if (!Files.exists(directoryPath)) {
            log.warn("Directory does not exist: {}", directoryPath);
            return;
        }

        try {
            Files.walk(directoryPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                            log.debug("Deleted file: {}", path);
                        } catch (IOException e) {
                            log.error("Failed to delete {}: {}", path, e.getMessage());
                            throw new UncheckedIOException(e);
                        }
                    });
            log.info("Successfully deleted directory: {}", directoryPath);
        } catch (UncheckedIOException e) {
            throw new IOException("Failed to delete task files", e.getCause());
        }
    }

    @GetMapping("/tasks/user/{userId}/status")
    public ResponseEntity<List<EpitopeTaskData>> findTasksByTaskStatusStatus(@PathVariable Long userId) {
        List<EpitopeTaskData> tasks = epitopeTaskDataService.findTasksByTaskStatusStatus(Status.RUNNING);
        if (tasks.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tasks);
    }

}
