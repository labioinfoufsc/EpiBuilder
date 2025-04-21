package ufsc.br.epibuilder.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import ufsc.br.epibuilder.model.EpitopeTaskData;
import ufsc.br.epibuilder.model.Status;
import ufsc.br.epibuilder.model.TaskStatus;
import ufsc.br.epibuilder.model.User;

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

    public Process runPipeline(EpitopeTaskData taskData) {
        log.info("Starting pipeline for task: {}", taskData.getId());

        try {
            List<String> command = new ArrayList<>();
            command.add("bash");
            command.add("-c");

            String fullCommand = String.join(" ", List.of(
                    "source /venv/bin/activate",
                    "&& nextflow run /pipeline/main.nf",
                    "--input_file " + taskData.getFile().getAbsolutePath(),
                    "--basename " + taskData.getCompleteBasename()));

            command.add(fullCommand);
            addOptionalParameter(command, "--threshold", taskData.getBepipredThreshold());
            addOptionalParameter(command, "--min-length", taskData.getMinEpitopeLength());
            addOptionalParameter(command, "--max-length", taskData.getMaxEpitopeLength());
            addOptionalParameter(command, "--search", "none");
            addOptionalParameter(command, "--proteomes", "none");

            String logFilePath = "/" + taskData.getRunName() + ".log";
            command.add(">");
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
