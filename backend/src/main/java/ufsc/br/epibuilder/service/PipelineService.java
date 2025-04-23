package ufsc.br.epibuilder.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.springframework.cglib.core.Local;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import ufsc.br.epibuilder.model.*;
import ufsc.br.epibuilder.model.EpitopeTopology;
import ufsc.br.epibuilder.model.Method;
import ufsc.br.epibuilder.model.Status;
import java.util.Scanner;

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

            if (taskData.isDoBlast()) {
                addOptionalParameter(command, "--search", "blast");

                List<Database> proteomes = taskData.getProteomes();

                StringBuilder proteomesFormatted = new StringBuilder();

                for (int i = 0; i < proteomes.size(); i++) {
                    Database db = proteomes.get(i);
                    if (i > 0) {
                        proteomesFormatted.append(":");
                    }
                    proteomesFormatted.append(db.getAbsolutePath())
                            .append("=")
                            .append(db.getAlias());
                }

                command.add("--proteomes " + proteomesFormatted.toString());
            }

            // String logFilePath = "/" + taskData.getRunName() + ".log";
            // command.add(">");
            // command.add(logFilePath);

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
        log.info("Monitoring running tasks...");
        try {
            log.info("Searching for running tasks...");
            List<EpitopeTaskData> runningTasks = epitopeTaskDataService.findTasksByTaskStatusStatus(Status.RUNNING);
            log.info("Found {} running tasks", runningTasks.size());
            for (EpitopeTaskData task : runningTasks) {
                log.info("Checking task ID {} with PID {}", task.getId(), task.getTaskStatus().getPid());
                long pid = task.getTaskStatus().getPid();
                boolean isRunning = isProcessRunning(pid);

                if (!isRunning) {
                    log.info("PID {} is not running anymore. Processing task ID {}", pid, task.getId());
                    processCompletedTask(task);
                }
            }
        } catch (Exception e) {
            log.error("Error in monitorRunningTasks: {}", e.getMessage());
        }
    }

    private void processCompletedTask(EpitopeTaskData task) {
        Path completePath = Paths.get(task.getCompleteBasename());

        if (!Files.exists(completePath)) {
            log.error("Complete directory not found for task {}: {}", task.getId(), completePath);
            task.getTaskStatus().setStatus(Status.FAILED);
            epitopeTaskDataService.save(task);
            return;
        }

        try {
            Path topologyPath = completePath.resolve("topology.tsv");
            Path epitopePath = completePath.resolve("epitope-detail.tsv");
            log.info("Checking for topology file: {}", topologyPath);
            log.info("Checking for epitope file: {}", completePath.resolve("epitope-detail.tsv"));

            if (!Files.exists(topologyPath) || !Files.exists(epitopePath)) {
                log.error("Required result files missing in {} for task {}", completePath, task.getId());
                task.getTaskStatus().setStatus(Status.FAILED);
                epitopeTaskDataService.save(task);
                return;
            }

            log.info("Converting epitope file for task...");
            List<Epitope> epitopes = convertTsvToEpitopes(epitopePath.toString());
            log.info("Epitopes converted: {}", epitopes.size());

            // Associate the task with the epitopes
            for (Epitope epitope : epitopes) {
                epitope.setEpitopeTaskData(task);
            }
            task.setEpitopes(epitopes);

            // Update task status
            task.getTaskStatus().setStatus(Status.COMPLETED);
            task.setFinishedDate(LocalDateTime.now());
            epitopeTaskDataService.save(task);

            log.info("Successfully processed results for task {}", task.getId());

        } catch (IOException e) {
            log.error("Error processing result files for task {}: {}", task.getId(), e.getMessage());
            task.getTaskStatus().setStatus(Status.FAILED);
            epitopeTaskDataService.save(task);
        }
    }

   public static List<Epitope> convertTsvToEpitopes(String filePath) throws IOException {
        List<Epitope> epitopes = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        reader.readLine();

        String line;
        while ((line = reader.readLine()) != null) {
            String[] columns = line.split("\t");
            Epitope epitope = new Epitope();

            log.info("Processing line: {}", line);

            epitope.setN(Long.parseLong(columns[0]));
            epitope.setEpitopeId(columns[1]);
            epitope.setEpitope(columns[2]);
            epitope.setStart(Integer.parseInt(columns[3]));
            epitope.setEnd(Integer.parseInt(columns[4]));
            epitope.setNGlyc(columns[5].equals("Y") ? 1 : 0);
            epitope.setNGlycCount(Integer.parseInt(columns[6]));
            epitope.setLength(Integer.parseInt(columns[8]));
            epitope.setMolecularWeight(Double.parseDouble(columns[9]));
            epitope.setIsoelectricPoint(Double.parseDouble(columns[10]));
            epitope.setHydropathy(Double.parseDouble(columns[11]));
            epitope.setBepiPred3(Double.parseDouble(columns[15]));
            epitope.setEmini(Double.parseDouble(columns[16]));
            epitope.setKolaskar(Double.parseDouble(columns[17]));
            epitope.setChouFosman(Double.parseDouble(columns[18]));
            epitope.setKarplusSchulz(Double.parseDouble(columns[19]));
            epitope.setParker(Double.parseDouble(columns[20]));

            epitopes.add(epitope);
        }

        reader.close();
        return epitopes;
    }

    private boolean isProcessRunning(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }
}
