package ufsc.br.epibuilder.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<EpitopeTaskData> newEpitopeTask(
            @RequestPart("data") EpitopeTaskData taskData,
            @RequestPart("file") MultipartFile file) {

        if (taskData != null) {

            String username = taskData.getUser().getUsername();
            String runName = taskData.getRunName();

            Path basePath = Paths.get(System.getProperty("user.dir"));
            Path taskDir = basePath.resolve(username).resolve(runName);

            try {

                log.info("Saving file to: {}", taskDir.toString());

                Files.createDirectories(taskDir);
                Path filePath = taskDir.resolve(file.getOriginalFilename());
                file.transferTo(filePath.toFile());

                taskData.setFile(filePath.toFile());
                taskData.setAbsolutePath(taskDir.toString());

                log.info("Starting pipeline for task: {}", taskData.getId());
                Process process = pipelineService.runPipeline(taskData);
                log.info("Pipeline started with PID: {}", process.pid());

                TaskStatus taskStatus = new TaskStatus();
                taskStatus.setPid(process.pid());
                taskStatus.setStatus(Status.RUNNING);
                taskStatus.setEpitopeTaskData(taskData);
                taskData.setTaskStatus(taskStatus);
                taskData.setExecutionDate(LocalDateTime.now());

                log.info("Saving task data to DB: {}", taskData.toString());
                EpitopeTaskData savedTask = epitopeTaskDataService.save(taskData);
                log.info("Task persisted with DB ID {}", savedTask.getId());

                return ResponseEntity.ok(savedTask);

            } catch (IOException e) {
                log.error("Error saving file: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            log.error("Task data is null");
            return ResponseEntity.badRequest().build();
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

    @GetMapping("/tasks/status/{taskId}")
    public ResponseEntity<TaskStatus> getTaskStatus(@PathVariable String taskId) {
        TaskStatus status = pipelineService.getTaskStatus(taskId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

}
