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
import java.nio.file.*;
import java.util.List;
import java.util.ArrayList;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Stream;
import ufsc.br.epibuilder.model.Status;
import ufsc.br.epibuilder.service.AuthService;
import ufsc.br.epibuilder.model.User;
import java.util.concurrent.*;

@Service
@Slf4j
public class PipelineService {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, TaskStatus> tasks = new ConcurrentHashMap<>();
    private final EpitopeTaskDataService epitopeTaskDataService;
    private final AuthService authService;

    public PipelineService(EpitopeTaskDataService epitopeTaskDataService, AuthService authService) {
        this.epitopeTaskDataService = epitopeTaskDataService;
        this.authService = authService;
    }

    @Async
    public Process runPipeline(EpitopeTaskData taskData) {
        log.info("Starting pipeline for task: {}", taskData.getId());

        try {
            List<String> command = new ArrayList<>(List.of(
                    "nextflow", "run", "/pipeline/main.nf",
                    "--input_file", taskData.getFile().getAbsolutePath(),
                    "--basename", taskData.getCompleteBasename()));

            addOptionalParameter(command, "--threshold", taskData.getBepipredThreshold());
            addOptionalParameter(command, "--min-length", taskData.getMinEpitopeLength());
            addOptionalParameter(command, "--max-length", taskData.getMaxEpitopeLength());
            addOptionalParameter(command, "--search", "none");
            addOptionalParameter(command, "--proteomes", "none");

            String logFilePath = taskData.getUser().getUsername() + "_" + taskData.getExecutionDate() + "_"
                    + taskData.getRunName() + ".log";
            command.add(">>");
            command.add(logFilePath);

            log.info("Command to run: {}", String.join(" ", command));

            Path workDir = Paths.get(taskData.getCompleteBasename());
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(workDir.toFile());

            File logFile = workDir.resolve("pipeline.log").toFile();
            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
            processBuilder.redirectErrorStream(true);

            log.info("Starting process...");
            Process process = processBuilder.start();
            log.info("Process started with PID: {}", process.pid());

            return process;

        } catch (IOException e) {
            log.error("Error starting pipeline: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start pipeline", e);
        } catch (Exception ex) {
            log.error("Unexpected error running pipeline: {}", ex.getMessage(), ex);
            throw new RuntimeException("Unexpected pipeline error", ex);
        }
    }

    /**
     * Adds an optional parameter to the command if the parameter value is not null
     * or "none".
     * 
     * @param command
     * @param paramName
     * @param paramValue
     */
    private void addOptionalParameter(List<String> command, String paramName, Object paramValue) {
        if (paramValue != null && !(paramValue instanceof String && ((String) paramValue).equalsIgnoreCase("none"))) {
            command.add(paramName);
            command.add(paramValue.toString());
        }

    }

    /**
     * This method is scheduled to run every 5 minutes. It checks for running tasks
     * and updates their status if the process is no longer running.
     * It also logs the files in the complete directory of the task.
     * 
     * If the process is not running, it updates the task status to COMPLETED.
     * 
     */
    @Scheduled(fixedRate = 60_000)
    public void monitorRunningTasks() {
        try {
            User loggedInUser = authService.getLoggedInUser();
            long userId = loggedInUser.getId();

            List<EpitopeTaskData> runningTasks = epitopeTaskDataService.findByUserIdAndTaskStatusStatus(userId,
                    Status.RUNNING);
            for (EpitopeTaskData task : runningTasks) {
                long pid = task.getTaskStatus().getPid();
                boolean isRunning = isProcessRunning(pid);

                if (!isRunning) {
                    log.info("PID {} is not running anymore. Processing task ID {}", pid, task.getId());

                    String logFile = task.getUser().getUsername() + "_" + task.getExecutionDate() + "_"
                            + task.getRunName() + ".log";
                    Path logFilePath = Paths.get(logFile);

                    try {
                        String content = new String(Files.readAllBytes(logFilePath));
                        if (content.contains("Your results are in")) {
                            Path completePath = Paths.get(task.getCompleteBasename());
                            if (completePath != null && Files.exists(completePath)) {
                                try (Stream<Path> files = Files.list(completePath)) {
                                    files.forEach(path -> {
                                        log.info("Files: {}", path.getFileName());
                                    });

                                    task.getTaskStatus().setStatus(Status.COMPLETED);
                                } catch (IOException e) {
                                    log.error("Error while reading files: {}", completePath, e);
                                    task.getTaskStatus().setStatus(Status.FAILED);
                                }
                            }

                        } else if (content.contains("terminated with an error")) {
                            log.error("Pipeline terminated with an error. Check the log file: {}", logFilePath);
                            task.getTaskStatus().setStatus(Status.FAILED);
                        } else {
                            log.warn("Log file does not contain expected output: {}", logFilePath);
                            task.getTaskStatus().setStatus(Status.FAILED);
                        }
                    } catch (IOException e) {
                        log.error("Error reading the log file: {}", e.getMessage());
                    }

                    epitopeTaskDataService.save(task);
                }
            }
        } catch (Exception e) {
            log.error("Error in monitorRunningTasks: {}", e.getMessage());
        }
    }

    private boolean isProcessRunning(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }
}
