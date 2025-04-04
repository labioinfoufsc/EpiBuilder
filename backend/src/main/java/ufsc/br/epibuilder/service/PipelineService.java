package ufsc.br.epibuilder.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import ufsc.br.epibuilder.model.EpitopeTaskData;
import ufsc.br.epibuilder.model.TaskStatus;
import ufsc.br.epibuilder.model.TaskStatus.Status;

@Service
public class PipelineService {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, TaskStatus> tasks = new ConcurrentHashMap<>();

    public String runPipeline(EpitopeTaskData taskData) {
        String taskId = UUID.randomUUID().toString();
        tasks.put(taskId, new TaskStatus(TaskStatus.Status.RUNNING));

        executor.submit(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(
                    "nextflow", "run", "pipeline_script.nf", "--input", taskData.getInputData()
                );
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                int exitCode = process.waitFor();
                TaskStatus status = tasks.get(taskId);
                if (exitCode == 0) {
                    status.setStatus(TaskStatus.Status.COMPLETED);
                    status.setOutput(output.toString());
                } else {
                    status.setStatus(TaskStatus.Status.FAILED);
                    status.setError("Process exited with code: " + exitCode);
                }

            } catch (Exception e) {
                TaskStatus status = tasks.get(taskId);
                status.setStatus(TaskStatus.Status.FAILED);
                status.setError("Exception: " + e.getMessage());
            }
        });

        return taskId;
    }

    public TaskStatus getTaskStatus(String taskId) {
        return tasks.get(taskId);
    }

}
