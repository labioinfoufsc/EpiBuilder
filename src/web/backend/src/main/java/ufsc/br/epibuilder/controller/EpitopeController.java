package ufsc.br.epibuilder.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import ufsc.br.epibuilder.model.Database;
import ufsc.br.epibuilder.model.EpitopeTaskData;
import ufsc.br.epibuilder.model.Status;
import ufsc.br.epibuilder.model.TaskStatus;
import ufsc.br.epibuilder.model.User;
import ufsc.br.epibuilder.service.DatabaseService;
import ufsc.br.epibuilder.service.EpitopeTaskDataService;
import ufsc.br.epibuilder.service.PipelineService;
import ufsc.br.epibuilder.service.UserService;

/**
 * REST controller for managing epitope tasks.
 * Provides endpoints for validation, task creation, import, monitoring,
 * stopping, downloading, and deletion of epitope tasks.
 */
@RestController
@Slf4j
@RequestMapping("/epitopes")
public class EpitopeController {

    private final UserService userService;
    private final EpitopeTaskDataService epitopeTaskDataService;
    private final PipelineService pipelineService;
    private final DatabaseService databaseService;

    public EpitopeController(EpitopeTaskDataService epitopeTaskDataService,
            PipelineService pipelineService,
            DatabaseService databaseService,
            UserService userService) {
        this.databaseService = databaseService;
        this.epitopeTaskDataService = epitopeTaskDataService;
        this.pipelineService = pipelineService;
        this.userService = userService;
    }

    /**
     * Saves an uploaded multipart file under the specified base directory.
     *
     * @param baseDir base directory to save the file
     * @param file    multipart file to save
     * @return saved file path
     * @throws IOException if saving fails
     */
    private Path saveFile(Path baseDir, MultipartFile file) throws IOException {
        Path filePath = baseDir.resolve(file.getOriginalFilename());
        file.transferTo(filePath.toFile());
        log.info("File saved: {}", filePath);
        return filePath;
    }

    /**
     * Validates protein FASTA sequences.
     * Accepts either a FASTA file or a raw sequence string as multipart input.
     * Uses reflection to invoke
     * br.ufsc.epibuilder.FastaValidation.validateForWeb(InputStream).
     *
     * @param file     optional FASTA file containing protein sequences
     * @param sequence optional raw protein sequence string
     * @return ResponseEntity containing validation result as JSON
     */
    @PostMapping(value = "/validate/fasta", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> validateFasta(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "sequence", required = false) String sequence) {

        try {
            InputStream streamToValidate = null;

            if (file != null && !file.isEmpty()) {
                streamToValidate = file.getInputStream();
            } else if (sequence != null && !sequence.trim().isEmpty()) {
                streamToValidate = new ByteArrayInputStream(sequence.getBytes(StandardCharsets.UTF_8));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("valid", false, "message", "No FASTA file or sequence provided."));
            }

            Class<?> validationClass = Class.forName("br.ufsc.epibuilder.FastaValidation");
            java.lang.reflect.Method method = validationClass.getMethod("validateForWeb", InputStream.class);
            boolean isValid = (boolean) method.invoke(null, streamToValidate);

            if (isValid) {
                return ResponseEntity.ok(Map.of("valid", true));
            } else {
                return ResponseEntity.ok(Map.of(
                        "valid", false,
                        "message", "No valid protein sequences found in the provided file or input."));
            }

        } catch (Exception e) {
            log.error("Validation error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("valid", false, "message",
                            "Internal server error during validation: " + e.getMessage()));
        }
    }

    /**
     * Imports a task from a ZIP file with previously computed results.
     *
     * @param userId  user ID
     * @param zipFile ZIP file containing task data
     * @return ResponseEntity with import result
     */
    @PostMapping("/tasks/import/{userId}")
    public ResponseEntity<Map<String, Object>> importTask(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile zipFile) {

        String originalFilename = zipFile.getOriginalFilename();
        if (zipFile.isEmpty() || originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            return errorResponse("Invalid file. Only .zip files are allowed.", HttpStatus.BAD_REQUEST);
        }

        Path taskDir = null;

        try {
            User user = userService.findUserEntityById(userId);
            String username = user.getUsername();

            Path baseDir = Paths.get("/tmp/epibuilder", username);
            Files.createDirectories(baseDir);

            String baseName = originalFilename.substring(0, originalFilename.lastIndexOf(".zip"));
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
            String unifiedRunName = baseName + "_" + timestamp;

            taskDir = baseDir.resolve(unifiedRunName);

            try {
                Files.createDirectory(taskDir);
            } catch (FileAlreadyExistsException e) {
                return errorResponse("A task with this name ('" + unifiedRunName + "') already exists.",
                        HttpStatus.CONFLICT);
            }

            String rootToStrip = baseName.replace(java.io.File.separator, "/") + "/";
            unzipFile(zipFile.getInputStream(), taskDir, rootToStrip);
            log.info("Zip file successfully unzipped to {}", taskDir);

            Path fastaFile = findFastaFile(taskDir);
            if (fastaFile == null) {
                log.error("No FASTA file (.fasta, .fna, .fa) found in the task directory: {}", taskDir);
                throw new IOException("Import failed: No FASTA file (.fasta, .fna, .fa) found in the zip archive.");
            }
            log.info("Found main FASTA file: {}", fastaFile);

            EpitopeTaskData taskData = new EpitopeTaskData();
            taskData.setRunName(unifiedRunName);
            taskData.setUser(user);
            taskData.setExecutionDate(null);
            taskData.setCompleteBasename(taskDir.toString());
            taskData.setAbsolutePath(fastaFile.toString());
            taskData.setFile(fastaFile.toFile());

            TaskStatus taskStatus = new TaskStatus();
            taskStatus.setStatus(Status.IMPORTED);
            taskData.setTaskStatus(taskStatus);

            EpitopeTaskData savedTaskData = epitopeTaskDataService.save(taskData);
            log.info("Import task pre-saved with ID: {}", savedTaskData.getId());
            taskStatus.setEpitopeTaskData(taskData);

            pipelineService.processCompletedTask(savedTaskData);
            log.info("processCompletedTask finished for imported task ID {}", savedTaskData.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Task imported successfully");
            response.put("importedTaskId", savedTaskData.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to import task {}: {}", originalFilename, e.getMessage(), e);

            if (taskDir != null && Files.exists(taskDir)) {
                try (var stream = Files.walk(taskDir)) {
                    stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ioex) {
                            throw new UncheckedIOException(ioex);
                        }
                    });
                    log.info("Cleanup successful for directory: {}", taskDir);
                } catch (Exception cleanupEx) {
                    log.error("Failed to cleanup directory {}: {}", taskDir, cleanupEx.getMessage());
                }
            }

            return errorResponse("Error during import: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Finds the first FASTA file in a directory (excluding proteins_invalid.fasta).
     *
     * @param directory directory to search
     * @return path to the first FASTA file found or null
     * @throws IOException if traversal fails
     */
    private Path findFastaFile(Path directory) throws IOException {
        try (Stream<Path> stream = Files.walk(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        boolean isFasta = name.endsWith(".fasta") || name.endsWith(".fna") || name.endsWith(".fa");
                        boolean isInvalidFile = name.equals("proteins_invalid.fasta");
                        return isFasta && !isInvalidFile;
                    })
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * Unzips a ZIP archive into the destination directory.
     * Optionally strips a root path prefix from entries.
     *
     * @param zipInputStream input stream of ZIP file
     * @param destDir        destination directory
     * @param rootToStrip    root folder prefix to strip from entry names
     * @throws IOException if extraction fails
     */
    private void unzipFile(InputStream zipInputStream, Path destDir, String rootToStrip) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {

                String entryName = zipEntry.getName().replace(java.io.File.separator, "/");
                String finalEntryName = entryName;

                if (entryName.startsWith(rootToStrip)) {
                    finalEntryName = entryName.substring(rootToStrip.length());
                }

                if (finalEntryName.isEmpty()) {
                    zis.closeEntry();
                    zipEntry = zis.getNextEntry();
                    continue;
                }

                Path newFilePath = destDir.resolve(finalEntryName);

                if (!newFilePath.normalize().startsWith(destDir.normalize())) {
                    throw new IOException("Bad zip entry: " + zipEntry.getName());
                }

                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newFilePath);
                } else {
                    if (newFilePath.getParent() != null) {
                        Files.createDirectories(newFilePath.getParent());
                    }
                    Files.copy(zis, newFilePath, StandardCopyOption.REPLACE_EXISTING);
                }

                zis.closeEntry();
                zipEntry = zis.getNextEntry();
            }
        }
    }

    /**
     * Creates a new epitope task by receiving metadata (JSON) and a FASTA file,
     * plus optional proteomes.
     *
     * @param taskDataJson JSON with task metadata
     * @param fastaFile    main FASTA file
     * @param proteomes    optional proteome files for BLAST
     * @return ResponseEntity with creation result
     */
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

        } catch (IllegalArgumentException e) {
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

            if (taskData.isDoBlast()) {
                processProteomes(taskData, baseDir, proteomes);
            }

            EpitopeTaskData savedTaskData = epitopeTaskDataService.save(taskData);
            Process process = pipelineService.runPipeline(savedTaskData);

            EpitopeTaskData finalTask = saveTask(savedTaskData, process);
            return successResponse(finalTask);

        } catch (IOException e) {
            log.error("IO Error: {}", e.getMessage(), e);
            return errorResponse("Error while processing files: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            return errorResponse("Internal server error: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Prepares the base directory for a new task under
     * /tmp/epibuilder/{username}/{runName_timestamp}.
     *
     * @param taskData task metadata
     * @return created base directory path
     * @throws IOException if directory creation fails
     */
    private Path prepareBaseDirectory(EpitopeTaskData taskData) throws IOException {
        String username = taskData.getUser().getUsername();
        String timestamp = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"))
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path baseDir = Paths.get("/tmp/epibuilder", username, taskData.getRunName() + "_" + timestamp);

        Files.createDirectories(baseDir);
        log.info("Directory created: {}", baseDir);
        taskData.setCompleteBasename(baseDir.toString());
        return baseDir;
    }

    /**
     * Processes proteome files (either uploaded FASTA for BLAST or existing
     * database aliases).
     *
     * @param taskData  task metadata
     * @param baseDir   base directory of the task
     * @param proteomes uploaded proteome files (optional)
     * @throws IOException if file operations fail
     */
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

            if ("fasta_blast".equals(proteome.getSourceType())) {
                if (proteomes == null || fileIndex >= proteomes.length) {
                    throw new IllegalArgumentException("No proteome file provided for: " + proteome.getAlias());
                }

                MultipartFile proteomeFile = proteomes[fileIndex++];
                if (proteomeFile.isEmpty()) {
                    throw new IllegalArgumentException("Empty proteome file for: " + proteome.getAlias());
                }

                Database db = new Database();
                db.setAlias(proteome.getAlias());
                db.setSourceType("fasta_blast");

                String sanitizedFilename = Paths.get(proteomeFile.getOriginalFilename())
                        .getFileName().toString();
                Path proteomePath = proteomesDir.resolve(sanitizedFilename);

                Files.copy(proteomeFile.getInputStream(), proteomePath, StandardCopyOption.REPLACE_EXISTING);

                db.setFileName(sanitizedFilename);
                db.setAbsolutePath(proteomePath.toString());
                LocalDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDateTime();
                db.setDate(now);

                log.info("New proteome file saved: {}", db);
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

    /**
     * Persists final task state after starting the pipeline.
     *
     * @param taskData task metadata
     * @param process  started process (unused here but kept for future)
     * @return saved task
     */
    private EpitopeTaskData saveTask(EpitopeTaskData taskData, Process process) {
        String locParam = taskData.getBiologicalClassification() != null
                ? taskData.getBiologicalClassification().toLocParam()
                : null;
        taskData.setLocalizationParam(locParam);
        LocalDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDateTime();
        taskData.setExecutionDate(now);

        return epitopeTaskDataService.save(taskData);
    }

    /**
     * Stops a running task by container name, updates status, deletes task files.
     *
     * @param id task ID
     * @return ResponseEntity with operation result
     */
    @PostMapping("/tasks/stop/{id}")
    public ResponseEntity<Map<String, Object>> stopTask(@PathVariable Long id) {
        log.info("Received request to stop task with ID: {}", id);
        try {
            EpitopeTaskData taskData = epitopeTaskDataService.findById(id).get();
            if (taskData == null) {
                return errorResponse("Task not found", HttpStatus.NOT_FOUND);
            }

            TaskStatus taskStatus = taskData.getTaskStatus();
            if (taskStatus == null || taskStatus.getStatus() != Status.RUNNING) {
                return errorResponse("Task is not running or already finalized", HttpStatus.CONFLICT);
            }

            String containerName = taskStatus.getContainerName();
            if (containerName == null) {
                return errorResponse("Task container name is missing", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            boolean stopped = pipelineService.stopProcessInsideContainer(containerName);
            if (!stopped) {
                log.warn("Container {} was not running or could not be stopped. Marking task as STOPPED.",
                        containerName);
            }

            taskStatus.setStatus(Status.STOPPED);
            taskData.setFinishedDate(LocalDateTime.now());
            epitopeTaskDataService.save(taskData);

            deleteTaskFiles(taskData.getCompleteBasename());
            log.info("Deleted task directory: {}", taskData.getCompleteBasename());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Task stopped and files deleted successfully");
            response.put("taskId", taskData.getId());
            response.put("status", taskStatus.getStatus().name());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error stopping task {}: {}", id, e.getMessage(), e);
            return errorResponse("Internal server error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Success response builder for task creation.
     *
     * @param savedTask saved task
     * @return OK response with task info
     */
    private ResponseEntity<Map<String, Object>> successResponse(EpitopeTaskData savedTask) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Task created. PID: " + savedTask.getTaskStatus().getPid());
        response.put("taskId", savedTask.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Error response builder with custom message and status.
     *
     * @param message error message
     * @param status  HTTP status
     * @return ResponseEntity with error payload
     */
    private ResponseEntity<Map<String, Object>> errorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Downloads a ZIP containing the entire task directory.
     *
     * @param id task ID
     * @return ResponseEntity streaming the ZIP file
     */
    @GetMapping("/tasks/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        try {
            EpitopeTaskData task = epitopeTaskDataService.findById(id).get();
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
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(Files.size(zipFilePath))
                    .body(resource);

        } catch (UncheckedIOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Retrieves the pipeline log content for a task.
     * If the log indicates completion, triggers async monitoring.
     *
     * @param id task ID
     * @return plain text log content or error status
     */
    @GetMapping("/tasks/{id}/log")
    public ResponseEntity<?> getTaskLog(@PathVariable Long id) {
        EpitopeTaskData task = epitopeTaskDataService.findById(id).get();
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Log file not found");
        }

        try {
            String logContent = Files.readString(logFile);

            if (logContent.contains("Your results are")) {
                this.monitorRunningTasksAsync();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(logContent);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading log file: " + e.getMessage());
        }
    }

    /**
     * Triggers asynchronous monitoring of running tasks.
     */
    @Async
    public void monitorRunningTasksAsync() {
        pipelineService.monitorRunningTasks();
    }

    /**
     * Lists all tasks for a user.
     *
     * @param userId user ID
     * @return list of task data or 404 if none
     */
    @GetMapping("/tasks/user/{userId}")
    public ResponseEntity<?> getTasksByUser(@PathVariable Long userId) {
        List<EpitopeTaskData> tasks = epitopeTaskDataService.findTasksByUserId(userId);
        if (tasks.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tasks);
    }

    /**
     * Deletes a task and all associated data.
     *
     * @param id task ID
     * @return OK or error response
     */
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Map<String, String>> deleteTask(@PathVariable Long id) {
        try {
            EpitopeTaskData taskFound = epitopeTaskDataService.findById(id).get();
            if (taskFound == null) {
                return ResponseEntity.notFound().build();
            }

            epitopeTaskDataService.deleteEpitopeTaskDataWithAssociations(id);
            this.deleteTaskFiles(taskFound.getCompleteBasename());

            return ResponseEntity.ok(Map.of("message", "Task and all associated data deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting task {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete task: " + e.getMessage()));
        }
    }

    /**
     * Marks a task as completed by verifying log content and process state.
     *
     * @param id task ID
     * @return OK if status updated, CONFLICT if still running, or error status
     */
    @PutMapping("/tasks/{id}/complete")
    public ResponseEntity<Void> markTaskAsCompleted(@PathVariable Long id) {
        EpitopeTaskData task = epitopeTaskDataService.findById(id).get();
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        TaskStatus status = task.getTaskStatus();
        if (status == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        try {
            boolean processRunning = pipelineService.isProcessRunning(status.getPid());

            if (!processRunning) {
                Path logFile = Paths.get(task.getCompleteBasename(), "pipeline.log");
                if (Files.exists(logFile)) {
                    String logContent = Files.readString(logFile);
                    if (logContent.contains("Your results are in")) {
                        status.setStatus(Status.COMPLETED);
                        task.setFinishedDate(LocalDateTime.now());
                        epitopeTaskDataService.save(task);
                        return ResponseEntity.ok().build();
                    }
                }
                status.setStatus(Status.STOPPED);
                task.setFinishedDate(LocalDateTime.now());
                epitopeTaskDataService.save(task);
                return ResponseEntity.ok().build();
            }

            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        } catch (IOException e) {
            log.error("Error while verifying task {} log: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Recursively deletes all files and directories under the given base directory.
     * Attempts to adjust permissions before deletion to improve success on
     * restrictive filesystems.
     *
     * @param baseDir base directory to delete
     */
    private void deleteTaskFiles(String baseDir) {
        Path dir = Paths.get(baseDir);
        if (Files.exists(dir)) {
            try {
                Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        try {
                            try {
                                file.toFile().setWritable(true, false);
                            } catch (Exception ignored) {
                            }
                            Files.delete(file);
                            log.info("Deleted file: {}", file);
                        } catch (IOException e) {
                            log.warn("Could not delete file {} (possibly in use or permission issue): {}", file,
                                    e.getMessage());
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path d, IOException exc) {
                        try {
                            try {
                                d.toFile().setWritable(true, false);
                            } catch (Exception ignored) {
                            }
                            Files.delete(d);
                            log.info("Deleted directory: {}", d);
                        } catch (IOException e) {
                            log.warn("Could not delete directory {} (possibly in use or permission issue): {}", d,
                                    e.getMessage());
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });

                if (!Files.exists(dir)) {
                    log.info("Deleted task directory: {}", baseDir);
                } else {
                    log.error("Failed to fully delete task directory {}", baseDir);
                }

            } catch (IOException e) {
                log.error("Failed to walk file tree for {}: {}", baseDir, e.getMessage());
            }
        }
    }

    /**
     * Finds tasks by user with RUNNING status.
     *
     * @param userId user ID
     * @return list of running tasks
     */
    @GetMapping("/tasks/user/{userId}/status")
    public ResponseEntity<List<EpitopeTaskData>> findTasksByTaskStatusStatus(@PathVariable Long userId) {
        List<EpitopeTaskData> tasks = epitopeTaskDataService.findTasksByUserIdAndStatus(userId, Status.RUNNING);
        return ResponseEntity.ok(tasks);
    }
}
