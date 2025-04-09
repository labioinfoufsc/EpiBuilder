package ufsc.br.epibuilder.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import ufsc.br.epibuilder.model.EpitopeTaskData;
import ufsc.br.epibuilder.model.TaskStatus;
import ufsc.br.epibuilder.model.Status;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

@Service
@Slf4j
public class PipelineService {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, TaskStatus> tasks = new ConcurrentHashMap<>();

    @Async
    public Process runPipeline(EpitopeTaskData taskData) {
        log.info("Starting pipeline for task: {}", taskData.getId());

        try {
            Path basePath = Paths.get(System.getProperty("user.dir"));
            Path pipelineScript = basePath.resolve("pipeline").resolve("main.nf");
            Path workDir = Paths.get(taskData.getAbsolutePath());
            Path workSubDir = workDir.resolve("work");

            List<String> command = List.of(
                    "wsl", "nextflow",
                    wslPath(pipelineScript.toString()),
                    "--input", wslPath(taskData.getFile().getAbsolutePath()),
                    "--outdir", wslPath(workDir.toString()),
                    "-work-dir", wslPath(workSubDir.toString()));

            log.info("Starting async pipeline: {}", String.join(" ", command));

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(basePath.toFile());

            File logFile = workDir.resolve("pipeline.log").toFile();
            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
            processBuilder.redirectErrorStream(true);

            return processBuilder.start();

        } catch (IOException e) {
            log.error("Erro ao iniciar execução do pipeline: {}", e.getMessage(), e);
        } catch (Exception ex) {
            log.error("Erro inesperado na execução do pipeline: {}", ex.getMessage(), ex);
        }

        return null;
    }

    public TaskStatus getTaskStatus(String taskId) {
        return tasks.get(taskId);
    }

    private String wslPath(String path) {
        return path.replace("C:\\", "/mnt/c/")
                .replace("\\", "/");
    }
}
