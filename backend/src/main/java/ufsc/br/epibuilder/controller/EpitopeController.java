package ufsc.br.epibuilder.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import ufsc.br.epibuilder.model.*;
import ufsc.br.epibuilder.service.EpitopeTaskDataService;
import ufsc.br.epibuilder.service.PipelineService;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.Resource;

@RestController
@Slf4j
@RequestMapping("/epitopes")
public class EpitopeController {

    private final EpitopeTaskDataService epitopeTaskDataService;

    private final PipelineService pipelineService;

    public EpitopeController(EpitopeTaskDataService epitopeTaskDataService, PipelineService pipelineService) {
        this.epitopeTaskDataService = epitopeTaskDataService;
        this.pipelineService = pipelineService;
    }

    @PostMapping(value = "/tasks/new", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> newEpitopeTask(
            @RequestPart("data") EpitopeTaskData taskData,
            @RequestPart("file") MultipartFile file) {

        if (taskData == null) {
            log.error("Task data is null");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid task data provided.");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        if (taskData.getAction().getDesc().equalsIgnoreCase(ActionType.PREDICT.getDesc())) {
            taskData.setAlgPredDisplayMode(null);
            taskData.setAlgPredictionModelType(null);
            taskData.setAlgPredDisplayMode(null);
            taskData.setBepipredThreshold(null);
            taskData.setMinEpitopeLength(null);
            taskData.setMaxEpitopeLength(null);
        }      

        try {
            String username = taskData.getUser().getUsername();
            String runName = taskData.getRunName();
            String timestamp = String.valueOf(System.currentTimeMillis());

            log.info("Creating directory structure for user: {}", username);
            String paths = "/www/" + username + "/" + runName + "_" + timestamp;
            Path baseDir = Paths.get(paths);
            Files.createDirectories(baseDir);
            log.info("Directory structure created: {}", baseDir);

            // Salva o arquivo diretamente no diretório base
            log.info("Saving file: {}", file.getOriginalFilename());
            Path filePath = baseDir.resolve(file.getOriginalFilename());
            file.transferTo(filePath.toFile());
            log.info("File saved: {}", filePath);

            taskData.setFile(filePath.toFile());
            taskData.setAbsolutePath(filePath.toString());  // Caminho absoluto: /www/username/runname_timestamp/arquivo.extensao
            taskData.setCompleteBasename(baseDir.toString());  // completeBasename: /www/username/runname_timestamp/

            Process process = pipelineService.runPipeline(taskData);

            TaskStatus taskStatus = new TaskStatus();
            taskStatus.setPid(process.pid());
            taskStatus.setStatus(Status.RUNNING);
            taskStatus.setEpitopeTaskData(taskData);
            taskData.setTaskStatus(taskStatus);
            taskData.setExecutionDate(LocalDateTime.now());

            log.info("Saving task data to DB: {}", taskData);
            EpitopeTaskData savedTask = epitopeTaskDataService.save(taskData);
            log.info("Task persisted with DB ID {}", savedTask.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("message",
                    String.format("Task successfully created. Task PID: %d", savedTask.getTaskStatus().getPid()));
            response.put("taskId", savedTask.getId());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Error saving file or creating task: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "An error occurred while saving the file or creating the task.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Unexpected server error occurred.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/task/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        try {
            EpitopeTaskData task = epitopeTaskDataService.findById(id);

            Path taskDir = Paths.get(task.getCompleteBasename()).normalize(); // Caminho do diretório da tarefa
            if (!Files.exists(taskDir) || !Files.isDirectory(taskDir)) {
                throw new RuntimeException("Task directory not found or is not a directory");
            }

            Path zipFilePath = Files.createTempFile("task_" + id + "_", ".zip");
            try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
                Files.walk(taskDir)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(filePath -> {
                            try {
                                ZipEntry zipEntry = new ZipEntry(taskDir.relativize(filePath).toString());
                                zipOut.putNextEntry(zipEntry);
                                Files.copy(filePath, zipOut);
                                zipOut.closeEntry();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            } catch (IOException e) {
                throw new RuntimeException("Error creating ZIP file", e);
            }

            Resource resource = new UrlResource(zipFilePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=\"" + zipFilePath.getFileName() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("File not found or not readable");
            }
        } catch (IOException | InvalidPathException ex) {
            return ResponseEntity.status(500).body(null);
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
        Long deleted = epitopeTaskDataService.deleteById(id);
        if (deleted == 0) {
            return ResponseEntity.notFound().build();
        }
        Map<String, String> response = new HashMap<>();
        response.put("message", "Task deleted successfully");
        return ResponseEntity.ok(response);
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
