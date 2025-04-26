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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.lang.Objects;
import lombok.extern.slf4j.Slf4j;
import ufsc.br.epibuilder.model.ActionType;
import ufsc.br.epibuilder.model.Blast;
import ufsc.br.epibuilder.model.Database;
import ufsc.br.epibuilder.model.Epitope;
import ufsc.br.epibuilder.model.EpitopeTaskData;
import ufsc.br.epibuilder.model.EpitopeTopology;
import ufsc.br.epibuilder.model.Method;
import ufsc.br.epibuilder.model.Status;
import ufsc.br.epibuilder.model.TaskStatus;

import ufsc.br.epibuilder.service.*;

import ufsc.br.epibuilder.model.Blast;
import ufsc.br.epibuilder.model.Epitope;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;

@Service
@Slf4j
public class PipelineService {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, TaskStatus> tasks = new ConcurrentHashMap<>();
    private final EpitopeTaskDataService epitopeTaskDataService;
    private final EpitopeService epitopeService;
    private final EpitopeTopologyService epitopeTopologyService;
    private final AuthService authService;

    public PipelineService(EpitopeTaskDataService epitopeTaskDataService, EpitopeTopologyService epitopeTopologyService,
            EpitopeService epitopeService, AuthService authService) {
        this.epitopeTaskDataService = epitopeTaskDataService;
        this.authService = authService;
        this.epitopeTopologyService = epitopeTopologyService;
        this.epitopeService = epitopeService;
    }

    /**
     * This method runs the pipeline using the provided EpitopeTaskData.
     * It constructs a command to run the pipeline using Nextflow and executes it.
     * 
     * @param taskData
     * @return
     */
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

            log.info("Adding parameters to command: {}", taskData.getActionType().getDesc());
            if (ActionType.DEFAULT.toString().equalsIgnoreCase(taskData.getActionType().getDesc())) {
                taskData.setBepipredThreshold(null);
                taskData.setMinEpitopeLength(null);
                taskData.setMaxEpitopeLength(null);
            } else {
                addOptionalParameter(command, "--threshold", taskData.getBepipredThreshold());
                addOptionalParameter(command, "--min-length", taskData.getMinEpitopeLength());
                addOptionalParameter(command, "--max-length", taskData.getMaxEpitopeLength());
            }

            if (taskData.isDoBlast() == true) {
                addOptionalParameter(command, "--search", "blast");

                List<Database> proteomes = taskData.getProteomes();
                StringBuilder proteomesFormatted = new StringBuilder();
                Set<String> addedAliases = new HashSet<>();

                for (int i = 0; i < proteomes.size(); i++) {
                    Database db = proteomes.get(i);
                    String alias = db.getAlias();

                    if (!addedAliases.contains(alias)) {
                        addedAliases.add(alias);

                        if (proteomesFormatted.length() > 0) {
                            proteomesFormatted.append(":");
                        }
                        proteomesFormatted.append(alias)
                                .append("=")
                                .append(db.getAbsolutePath());
                    }
                }

                command.add("--proteomes " + proteomesFormatted.toString());

                if (taskData.getBlastMinCoverCutoff() != 90) {
                    addOptionalParameter(command, "--cover", taskData.getBlastMinCoverCutoff());
                }

                if (taskData.getBlastMinIdentityCutoff() != 90) {
                    addOptionalParameter(command, "--identity", taskData.getBlastMinIdentityCutoff());
                }

                if (taskData.getBlastWordSize() != 4) {
                    addOptionalParameter(command, "--word-size", taskData.getBlastWordSize());
                }

            }

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
            Path proteinSummary = completePath.resolve("protein-summary.tsv");

            log.info("Checking for result files in {}", completePath);

            if (!Files.exists(topologyPath) || !Files.exists(epitopePath) || !Files.exists(proteinSummary)) {
                log.error("Required result files missing in {} for task {}", completePath, task.getId());
                task.getTaskStatus().setStatus(Status.FAILED);
                epitopeTaskDataService.save(task);
                return;
            }

            log.info("Converting epitope file for task...");
            List<Epitope> epitopes = convertTsvToEpitopes(epitopePath.toString());
            log.info("Epitopes converted: {}", epitopes.size());

            log.info("Saving epitopes to database...");
            for (Epitope epitope : epitopes) {
                epitope.setEpitopeTaskData(task);
                Epitope epitopeSaved = epitopeService.save(epitope);
                epitope.setId(epitopeSaved.getId());
                log.info("Epitope saved: {}", epitope.getEpitopeId());
            }
            log.info("Epitopes saved to database.");

            log.info("Checking BLAST files...");
            if (task.isDoBlast()) {
                log.info("Processing BLAST files in {}", completePath);
                try {
                    // Lista todos os arquivos que começam com "blast-" e terminam com ".csv"
                    List<Path> blastFiles = Files.list(completePath)
                            .filter(path -> path.getFileName().toString().startsWith("blast-")
                                    && path.getFileName().toString().endsWith(".csv"))
                            .collect(Collectors.toList());

                    log.info("Found {} BLAST files", blastFiles.size());
                    if (blastFiles.isEmpty()) {
                        log.warn("No BLAST files found in directory: {}", completePath);
                    } else {
                        for (Path blastPath : blastFiles) {
                            log.info("Processing BLAST file: {}", blastPath.getFileName());

                            // Associa os dados do BLAST aos epítopos
                            List<Epitope> epitopesWithBlasts = associateBlastsFromCsv(blastPath.toString(), epitopes);
                            log.info("BLASTs converted from {}: {}", blastPath.getFileName(),
                                    epitopesWithBlasts.size());

                            // Salva apenas os epítopos que tiveram dados de BLAST associados
                            for (Epitope epitope : epitopesWithBlasts) {
                                if (epitope.getBlasts() != null) {
                                    epitopeService.save(epitope);
                                    log.debug("Epitope updated with BLAST data from {}: {}",
                                            blastPath.getFileName(), epitope.getEpitopeId());
                                }
                            }
                            log.info("Epitopes updated with BLAST data from {}", blastPath.getFileName());
                        }
                        log.info("All BLAST files processed.");
                    }
                } catch (IOException e) {
                    log.error("Error while processing BLAST files: {}", e.getMessage(), e);
                }
            }

            /*
             * log.info("Converting topology file for task...");
             * List<EpitopeTopology> topologies =
             * convertTsvToTopologies(topologyPath.toString(), epitopes);
             * log.info("Topologies converted: {}", topologies.size());
             * 
             * log.info("Saving topologies to database...");
             * for (EpitopeTopology topology : topologies) {
             * epitopeTopologyService.save(topology);
             * log.info("Topology saved: {}", topology.getId());
             * }
             */

            // Save the task with the epitopes
            task.setEpitopes(epitopes);

            // Count proteome size
            int proteomeSize = countProteins(proteinSummary.toString());
            task.setProteomeSize(proteomeSize);

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

    public static List<Epitope> associateBlastsFromCsv(String pathFile, List<Epitope> epitopes) {
        try (BufferedReader br = new BufferedReader(new FileReader(pathFile))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] values = line.split(",");
                if (values.length >= 6) {
                    String qacc = values[0].trim();
                    String[] qaccParts = qacc.split("-");
                    if (qaccParts.length > 0) {
                        try {
                            Long blastN = Long.parseLong(qaccParts[0]);

                            Blast blast = new Blast();
                            blast.setSacc(values[1].trim());
                            blast.setPident(Double.parseDouble(values[2].trim()));
                            blast.setQcovs(Double.parseDouble(values[3].trim()));
                            blast.setQseq(values[4].trim());
                            blast.setSseq(values[5].trim());

                            for (Epitope epitope : epitopes) {
                                if (epitope.getN() != null && epitope.getN().equals(blastN)) {
                                    blast.setEpitope(epitope);
                                    epitope.getBlasts().add(blast);
                                    break;
                                }
                            }
                        } catch (NumberFormatException e) {
                            log.info("Error while parsing qacc: {}", qacc);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.info("Error while reading CSV file: {}", e.getMessage());
            e.printStackTrace();
        }

        return epitopes;
    }

    public static int countProteins(String pathFile) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(pathFile))) {
            br.readLine();

            int countProtein = 0;
            while (br.readLine() != null) {
                countProtein++;
            }
            return countProtein;
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
            log.info("Columns length: {}", columns.length);
            log.info("Columns: {}", (Object) columns);
            log.info("Processing line: {}", line);

            epitope.setN(Long.parseLong(columns[0]));
            epitope.setEpitopeId(columns[1]);
            epitope.setEpitope(columns[2]);
            epitope.setStart(Integer.parseInt(columns[3]));
            epitope.setEnd(Integer.parseInt(columns[4]));
            epitope.setNGlyc(columns[5]);
            epitope.setNGlycCount(Integer.parseInt(columns[6]));
            epitope.setLength(Integer.parseInt(columns[8]));
            epitope.setMolecularWeight(Double.parseDouble(columns[9]));
            epitope.setIsoelectricPoint(Double.parseDouble(columns[10]));
            epitope.setHydropathy(Double.parseDouble(columns[11]));
            epitope.setBepiPred3(Double.parseDouble(columns[14]));
            epitope.setEmini(Double.parseDouble(columns[15]));
            epitope.setKolaskar(Double.parseDouble(columns[16]));
            epitope.setChouFosman(Double.parseDouble(columns[17]));
            epitope.setKarplusSchulz(Double.parseDouble(columns[18]));
            epitope.setParker(Double.parseDouble(columns[19]));

            epitopes.add(epitope);
        }

        reader.close();
        return epitopes;
    }

    private boolean isProcessRunning(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }
}
