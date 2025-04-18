package ufsc.br.epibuilder.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ufsc.br.epibuilder.model.ActionType;
import ufsc.br.epibuilder.model.Epitope;
import ufsc.br.epibuilder.model.EpitopeTaskData;
import ufsc.br.epibuilder.service.EpitopeService;
import ufsc.br.epibuilder.service.EpitopeTaskDataService;
import ufsc.br.epibuilder.service.PipelineService;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.nio.file.*;

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

        if (taskData.getAction().getDesc().equalsIgnoreCase(ActionType.ANALYSIS.getDesc())) {
            taskData.setAlgPredDisplayMode(null);
            taskData.setAlgPredictionModelType(null);
            taskData.setAlgPredDisplayMode(null);
            taskData.setBepipredThreshold(null);
            taskData.setMinEpitopeLength(null);
            taskData.setMaxEpitopeLength(null);
        }

        String username = taskData.getUser().getUsername();
        String runName = taskData.getRunName();

        Path basePath = Paths.get(System.getProperty("user.dir"));
        Path taskDir = basePath.resolve(username).resolve(runName);

        try {
            log.info("Saving file to: {}", taskDir);

            // Input file path
            Files.createDirectories(taskDir);
            Path filePath = taskDir.resolve(file.getOriginalFilename());
            file.transferTo(filePath.toFile());
            taskData.setFile(filePath.toFile());
            taskData.setAbsolutePath(taskDir.toString());

            // Results files path
            String timestamp = String.valueOf(System.currentTimeMillis());
            Path workDir = Paths.get("/www", username, runName + "_" + timestamp);
            Files.createDirectories(workDir);
            taskData.setCompleteBasename(workDir.toString());

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
            response.put("message", String.format("Task successfully created. Task ID: %d", savedTask.getId()));
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
        epitopeTaskDataService.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Task deleted successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tasks/user/{userId}/status")
    public ResponseEntity<List<EpitopeTaskData>> findByUserIdAndTaskStatusStatus(@PathVariable Long userId) {
        List<EpitopeTaskData> tasks = epitopeTaskDataService.findByUserIdAndTaskStatusStatus(userId, Status.RUNNING);
        if (tasks.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tasks);
    }

}
