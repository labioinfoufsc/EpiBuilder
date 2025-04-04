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

import ufsc.br.epibuilder.model.TaskStatus;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/epitopes")
public class EpitopeController {

    private final EpitopeTaskDataService epitopeTaskDataService;

    private final PipelineService pipelineService;

    public EpitopeController(EpitopeTaskDataService epitopeTaskDataService, PipelineService pipelineService) {
        this.epitopeTaskDataService = epitopeTaskDataService;
        this.pipelineService = pipelineService;
    }

    @PostMapping("/tasks/new")
    public ResponseEntity<String> newEpitopeTask(@RequestBody EpitopeTaskData taskData) {
        String taskId = pipelineService.runPipeline(taskData);
        if (taskId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(taskId);
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
        boolean deleted = epitopeTaskDataService.deleteById(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
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
