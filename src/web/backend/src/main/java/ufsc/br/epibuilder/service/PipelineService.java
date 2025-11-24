package ufsc.br.epibuilder.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import ufsc.br.epibuilder.model.ActionType;
import ufsc.br.epibuilder.model.Blast;
import ufsc.br.epibuilder.model.Database;
import ufsc.br.epibuilder.model.Epitope;
import ufsc.br.epibuilder.model.EpitopeTaskData;
import ufsc.br.epibuilder.model.EpitopeTopology;
import ufsc.br.epibuilder.model.Method;
import ufsc.br.epibuilder.model.Protein;
import ufsc.br.epibuilder.model.Status;
import ufsc.br.epibuilder.model.TaskStatus;
import ufsc.br.epibuilder.model.BiologicalClassification;
import ufsc.br.epibuilder.model.BacterialType;
import ufsc.br.epibuilder.model.CellType;

import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

/**
 * Service responsible for managing and executing epitope pipeline tasks.
 * This includes starting and stopping Docker containers, monitoring running
 * tasks,
 * parsing pipeline results, and updating task status.
 */
@Service
@Slf4j
public class PipelineService {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, TaskStatus> tasks = new ConcurrentHashMap<>();
    private final EpitopeTaskDataService epitopeTaskDataService;
    private final EpitopeService epitopeService;
    private final EpitopeTopologyService epitopeTopologyService;
    private final AuthService authService;

    public PipelineService(EpitopeTaskDataService epitopeTaskDataService,
            EpitopeTopologyService epitopeTopologyService,
            EpitopeService epitopeService,
            AuthService authService) {
        this.epitopeTaskDataService = epitopeTaskDataService;
        this.authService = authService;
        this.epitopeTopologyService = epitopeTopologyService;
        this.epitopeService = epitopeService;
    }

    /**
     * Stops a running Docker container by its name.
     *
     * @param containerName the name of the container
     * @return true if the container was stopped successfully, false otherwise
     */
    public boolean stopProcessInsideContainer(String containerName) {
        try {
            log.info("Attempting to stop container {}", containerName);
            ProcessBuilder pb = new ProcessBuilder("docker", "stop", containerName);
            Process proc = pb.start();
            int exitCode = proc.waitFor();

            if (exitCode == 0) {
                log.info("Container {} stopped successfully.", containerName);
                return true;
            } else {
                log.error("Failed to stop container {}. Exit code: {}", containerName, exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("Error stopping container {}: {}", containerName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks if a Docker container with the given name is still running.
     *
     * @param containerName the name of the container
     * @return true if the container is running, false otherwise
     */
    public boolean isContainerRunning(String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "ps", "--filter", "name=" + containerName, "--format",
                    "{{.Names}}");
            Process proc = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line = reader.readLine();
                int exitCode = proc.waitFor();
                if (exitCode == 0 && line != null && line.trim().equals(containerName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Error checking container {}: {}", containerName, e.getMessage(), e);
        }
        return false;
    }

    /**
     * Runs the pipeline using the provided {@link EpitopeTaskData}.
     *
     * @param taskData configuration for the pipeline execution
     * @return the process running the pipeline
     */
    public Process runPipeline(EpitopeTaskData taskData) {
        log.info("Starting pipeline for task: {}", taskData.getId());

        if (taskData.getId() == null) {
            throw new IllegalStateException("TaskData must be saved before running pipeline (ID is null)");
        }

        try {
            String epibuilderVolume = resolveEpibuilderVolume();
            String command = buildDockerCommand(taskData, epibuilderVolume);

            log.info("Command to run: {}", command);

            ProcessBuilder processBuilder = configureProcessBuilder(command, taskData);
            Process process = startProcess(processBuilder);

            String containerName = "epibuilder-task-" + taskData.getId();
            updateTaskStatus(taskData, process.pid(), containerName);

            return process;

        } catch (IOException e) {
            log.error("Error starting pipeline: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start pipeline", e);
        } catch (Exception ex) {
            log.error("Unexpected error running pipeline: {}", ex.getMessage(), ex);
            throw new RuntimeException("Unexpected pipeline error", ex);
        }
    }

    private String resolveEpibuilderVolume() {
        String volume = System.getenv("EPIBUILDER_VOLUME");
        return (volume == null || volume.isEmpty()) ? "/tmp/epibuilder" : volume;
    }

    private String buildDockerCommand(EpitopeTaskData taskData, String epibuilderVolume) {
        String containerName = "epibuilder-task-" + taskData.getId();

        StringBuilder cmd = new StringBuilder("docker run --rm ")
                .append("--name ").append(containerName).append(" ")
                .append("-v /var/run/docker.sock:/var/run/docker.sock ")
                .append(String.format("-v %s:/tmp/epibuilder ", epibuilderVolume))
                .append(String.format("-e EPIBUILDER_VOLUME=%s ", epibuilderVolume))
                .append("bioinfoufsc/epibuilder-core:latest epibuilder ")
                .append("--input_file ").append(taskData.getFile().getAbsolutePath()).append(" ")
                .append("--output ").append(taskData.getCompleteBasename()).append(" ")
                .append("--task_id ").append(taskData.getId()).append(" ");

        addDefaultParameters(taskData);
        addClassificationParameter(taskData, cmd);
        addThresholdParameters(taskData, cmd);
        addBlastParameters(taskData, cmd);

        return cmd.toString().trim();
    }

    private void addDefaultParameters(EpitopeTaskData taskData) {
        if (ActionType.DEFAULT.toString().equalsIgnoreCase(taskData.getActionType().getDesc())) {
            taskData.setBepipredThreshold(0.1512);
            taskData.setMinEpitopeLength(10);
            taskData.setMaxEpitopeLength(30);
        }
    }

    private void addClassificationParameter(EpitopeTaskData taskData, StringBuilder cmd) {
        String locParam = resolveLocParam(taskData.getBiologicalClassification());
        List<String> validParams = List.of("animal", "plant", "fungi", "arch", "gram_pos", "gram_neg");

        if (locParam != null && validParams.contains(locParam)) {
            cmd.append("--loc ").append(locParam).append(" ");
        }
    }

    private String resolveLocParam(BiologicalClassification classification) {
        if (classification == null || classification.getCellType() == null)
            return null;

        return switch (classification.getCellType()) {
            case EUKARYOTE ->
                classification.getOrganism() != null ? classification.getOrganism().toString().toLowerCase() : null;
            case BACTERIA -> resolveBacteriaParam(classification.getBacterialType());
            case ARCHAEA -> "arch";
            case NONE -> null;
        };
    }

    private String resolveBacteriaParam(BacterialType type) {
        if (type == null)
            return null;
        return switch (type) {
            case GRAM_POSITIVE -> "gram_pos";
            case GRAM_NEGATIVE -> "gram_neg";
        };
    }

    private void addThresholdParameters(EpitopeTaskData taskData, StringBuilder cmd) {
        if (taskData.getBepipredThreshold() != null) {
            cmd.append("--threshold ").append(taskData.getBepipredThreshold()).append(" ");
        }
        if (taskData.getMinEpitopeLength() != null) {
            cmd.append("--min-length ").append(taskData.getMinEpitopeLength()).append(" ");
        }
        if (taskData.getMaxEpitopeLength() != null) {
            cmd.append("--max-length ").append(taskData.getMaxEpitopeLength()).append(" ");
        }
    }

    private void addBlastParameters(EpitopeTaskData taskData, StringBuilder cmd) {
        if (!taskData.isDoBlast())
            return;

        List<String> proteomes = taskData.getProteomes().stream()
                .map(Database::toString)
                .toList();

        cmd.append("--proteomes ").append(String.join(":", proteomes)).append(" ");

        if (taskData.getBlastMinCoverCutoff() != 90) {
            cmd.append("--cover ").append(taskData.getBlastMinCoverCutoff()).append(" ");
        }
        if (taskData.getBlastMinIdentityCutoff() != 90) {
            cmd.append("--identity ").append(taskData.getBlastMinIdentityCutoff()).append(" ");
        }
    }

    private ProcessBuilder configureProcessBuilder(String command, EpitopeTaskData taskData) {
        List<String> bashCommand = List.of("bash", "-c", command);
        ProcessBuilder pb = new ProcessBuilder(bashCommand);

        configureEnvironment(pb);
        configureWorkingDirectory(pb, taskData);

        return pb;
    }

    private void configureEnvironment(ProcessBuilder pb) {
        Map<String, String> env = pb.environment();
        String blastPath = "/usr/local/bin";
        String currentPath = env.getOrDefault("PATH", "");

        if (!currentPath.contains(blastPath)) {
            env.put("PATH", blastPath + ":" + currentPath);
        }
    }

    private void configureWorkingDirectory(ProcessBuilder pb, EpitopeTaskData taskData) {
        Path workDir = Paths.get(taskData.getCompleteBasename());
        pb.directory(workDir.toFile());

        File logFile = workDir.resolve("pipeline.log").toFile();
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
        pb.redirectErrorStream(true);
    }

    private Process startProcess(ProcessBuilder pb) throws IOException {
        log.info("Starting process...");
        Process process = pb.start();
        log.info("Process started with host PID: {}", process.pid());
        return process;
    }

    private void updateTaskStatus(EpitopeTaskData taskData, Long pid, String containerName) {
        TaskStatus taskStatus = new TaskStatus();
        taskStatus.setPid(pid);
        taskStatus.setContainerName(containerName);
        taskStatus.setStatus(Status.RUNNING);
        taskStatus.setEpitopeTaskData(taskData);
        taskData.setTaskStatus(taskStatus);
    }

    /**
     * Stops a running process by its PID.
     *
     * @param pid the process ID
     * @return true if the process was successfully stopped, false otherwise
     */
    public boolean stopProcessByPid(Long pid) {
        try {
            log.info("Attempting to stop host process with PID {}", pid);
            return ProcessHandle.of(pid).map(ph -> {
                boolean terminated = ph.destroy();
                if (!terminated) {
                    ph.destroyForcibly();
                }
                boolean dead = !ph.isAlive();
                log.info("Process {} terminated: {}", pid, dead);
                return dead;
            }).orElse(false);
        } catch (Exception e) {
            log.error("Failed to stop process with PID {}: {}", pid, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Monitors running tasks every minute. If a container is no longer running,
     * updates the task status accordingly.
     */
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void monitorRunningTasks() {
        log.debug("Monitoring running tasks...");
        try {
            List<EpitopeTaskData> runningTasks = epitopeTaskDataService.findTasksByTaskStatusStatus(Status.RUNNING);
            if (runningTasks.isEmpty())
                return;

            Set<String> runningContainers = getRunningContainers();

            for (EpitopeTaskData task : runningTasks) {
                TaskStatus status = task.getTaskStatus();
                if (status == null || status.getContainerName() == null)
                    continue;

                if (!runningContainers.contains(status.getContainerName())) {
                    log.info("Container {} finished. Processing task {}", status.getContainerName(), task.getId());
                    processCompletedTask(task);
                }
            }
        } catch (Exception e) {
            log.error("Error in monitorRunningTasks", e);
        }
    }

    private Set<String> getRunningContainers() {
        Set<String> running = new HashSet<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "ps", "--format", "{{.Names}}");
            Process proc = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    running.add(line.trim());
                }
            }

            proc.waitFor();
        } catch (IOException e) {
            log.error("Error while checking running containers", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for docker ps", e);
        }
        return running;
    }

    /**
     * Checks if a process with the given PID is still running.
     *
     * @param pid the process ID
     * @return true if the process is alive, false otherwise
     */
    public boolean isProcessRunning(Long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    @Transactional
    public void processCompletedTask(EpitopeTaskData task) {
        EpitopeTaskData managedTask = epitopeTaskDataService.findById(task.getId()).get();
        if (managedTask == null) {
            log.error("Task {} not found when processing completion.", task.getId());
            return;
        }

        Path completePath = Paths.get(managedTask.getCompleteBasename());

        if (!Files.exists(completePath)) {
            log.error("Complete directory not found for task {}: {}", managedTask.getId(), completePath);
            managedTask.getTaskStatus().setStatus(Status.FAILED);
            epitopeTaskDataService.save(managedTask);
            return;
        }

        try {
            Path topologyPath = completePath.resolve("topology.tsv");
            Path epitopePath = completePath.resolve("epitope-detail.tsv");
            Path proteinSummary = completePath.resolve("protein-summary.tsv");

            Path localizationPath = null;
            if (managedTask.getLocalizationParam() != null) {
                localizationPath = completePath.resolve("localization.tsv");
            }

            List<String> missingFiles = new ArrayList<>();
            if (!Files.exists(topologyPath))
                missingFiles.add("topologyPath");
            if (!Files.exists(epitopePath))
                missingFiles.add("epitopePath");
            if (!Files.exists(proteinSummary))
                missingFiles.add("proteinSummary");
            if (localizationPath != null && !Files.exists(localizationPath))
                missingFiles.add("localizationPath");

            if (!missingFiles.isEmpty()) {
                log.error("Missing result files for task {}: {}", managedTask.getId(), String.join(", ", missingFiles));
                managedTask.getTaskStatus().setStatus(Status.FAILED);
                epitopeTaskDataService.save(managedTask);
                return;
            }

            log.info("Parsing epitopes from epitope-detail.tsv...");
            List<Epitope> epitopes = convertTsvToEpitopes(epitopePath.toString(), managedTask);
            log.info("Parsed {} epitopes", epitopes.size());

            log.info("Parsing topologies from topology.tsv...");
            List<EpitopeTopology> topologies = parseEpitopeTopology(topologyPath.toString());
            log.info("Parsed {} topologies", topologies.size());

            log.info("Associating topologies with epitopes...");
            List<Epitope> completeEpitopes = associateTopologies(epitopes, topologies);
            log.info("Topologies associated with epitopes. Total epitopes after association: {}",
                    completeEpitopes.size());

            if (localizationPath != null) {
                log.info("Parsing localization data from localization.tsv...");
                List<Protein> proteins = convertTsvToProteins(localizationPath.toString());
                log.info("Parsed {} proteins with localization info", proteins.size());

                Map<String, String> localizationMap = proteins.stream()
                        .collect(Collectors.toMap(Protein::getProteinId, Protein::getLocalization));

                for (Epitope epitope : completeEpitopes) {
                    Protein protein = epitope.getProtein();
                    if (protein != null) {
                        String loc = localizationMap.get(protein.getProteinId());
                        if (loc != null) {
                            protein.setLocalization(loc);
                        }
                    }
                }
                log.info("Localization data associated with epitopes");
            }

            log.info("Searching for BLAST result files (*.blast.csv)...");
            List<Path> blastFiles = Files.list(completePath)
                    .filter(path -> path.getFileName().toString().endsWith("blast.csv"))
                    .collect(Collectors.toList());
            log.info("Found {} BLAST files", blastFiles.size());

            for (Path searchPath : blastFiles) {
                log.info("Parsing BLAST file: {}", searchPath.getFileName());
                List<Blast> convertedBlasts = parseBlastCsv(searchPath.toString());
                log.info("Parsed {} BLAST entries from {}", convertedBlasts.size(), searchPath.getFileName());
                associateBlasts(completeEpitopes, convertedBlasts);
                log.info("Associated BLAST results with epitopes");
            }

            log.info("Updating managed epitopes for task {}", managedTask.getId());
            List<Epitope> managedEpitopes = managedTask.getEpitopes();
            if (managedEpitopes == null) {
                managedEpitopes = new ArrayList<>();
                managedTask.setEpitopes(managedEpitopes);
            }
            managedEpitopes.clear();
            managedEpitopes.addAll(completeEpitopes);
            log.info("Managed epitopes updated. Total epitopes: {}", managedEpitopes.size());

            log.info("Counting proteins from protein-summary.tsv...");
            int proteomeSize = countProteins(proteinSummary.toString());
            log.info("Proteome size counted: {}", proteomeSize);

            if (proteomeSize > 0) {
                managedTask.setProteomeSize(proteomeSize);
            } else {
                managedTask.setProteomeSize(0);
            }

            if (managedTask.getTaskStatus().getStatus() != Status.IMPORTED) {
                managedTask.getTaskStatus().setStatus(Status.COMPLETED);
                managedTask.setFinishedDate(
                        ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDateTime());
                log.info("Task {} marked as COMPLETED", managedTask.getId());
            } else {
                managedTask.setFinishedDate(null);
                log.info("Task {} remains IMPORTED, finishedDate set to null", managedTask.getId());
            }

            epitopeTaskDataService.save(managedTask);
            log.info("Task {} saved successfully with updated epitope and topology data", managedTask.getId());

        } catch (IOException e) {
            log.error("Error processing result files for task {}: {}", managedTask.getId(), e.getMessage());
            managedTask.getTaskStatus().setStatus(Status.FAILED);
            epitopeTaskDataService.save(managedTask);
        }
    }

    // --- Helper methods for parsing and conversions ---

    public List<Protein> convertTsvToProteins(String filePath) {
        List<Protein> proteins = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String[] parts = line.split("\t");
                if (parts.length < 2)
                    continue;
                Protein protein = new Protein();
                protein.setProteinId(parts[0].trim());
                protein.setLocalization(parts[1].trim());
                proteins.add(protein);
            }
        } catch (IOException e) {
            log.error("Error reading localization file: {}", e.getMessage(), e);
        }
        return proteins;
    }

    public static String extractDBName(String path) {
        String regex = "(.*?)\\_blast.csv";
        Matcher matcher = Pattern.compile(regex).matcher(path);
        return matcher.find() ? matcher.group(1) : "DB name not found";
    }

    public List<Blast> parseBlastCsv(String filePath) throws IOException {
        List<Blast> blastList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                if (line.trim().isEmpty())
                    continue;
                String[] columns = line.split("\\t");
                Blast blast = new Blast();

                String qacc = columns[0];
                String[] qaccParts = qacc.split("-");
                if (qaccParts.length > 0) {
                    try {
                        blast.setN(Long.parseLong(qaccParts[0]));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid N value for line: {}", line);
                        blast.setN(null);
                    }
                }

                blast.setSacc(columns[1]);

                try {
                    blast.setPident(Double.parseDouble(columns[2]));
                } catch (NumberFormatException e) {
                    log.warn("Invalid pident: {}", line);
                    blast.setPident(null);
                }

                try {
                    blast.setQcovs(Double.parseDouble(columns[3]));
                } catch (NumberFormatException e) {
                    log.warn("Invalid qcovs: {}", line);
                    blast.setQcovs(null);
                }

                blast.setQseq(columns[4]);
                blast.setSseq(columns[5]);

                Path path = Paths.get(filePath);
                String fileName = path.getFileName().toString();
                blast.setDatabase(fileName);
                blast.setDb(extractDBName(fileName));

                blastList.add(blast);
            }
        }
        return blastList;
    }

    /**
     * Counts the number of proteins in a TSV file.
     *
     * @param pathFile path to the protein summary file
     * @return number of proteins
     * @throws IOException if file cannot be read
     */
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

    /**
     * Converts a TSV file into a list of {@link Epitope}.
     *
     * @param filePath path to the epitope TSV file
     * @param task     associated task
     * @return list of epitopes
     * @throws IOException if file cannot be read
     */
    public static List<Epitope> convertTsvToEpitopes(String filePath, EpitopeTaskData task) throws IOException {
        List<Epitope> epitopes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split("\t");
                Epitope epitope = new Epitope();
                epitope.setN(Long.parseLong(columns[0]));

                Protein protein = new Protein();
                protein.setProteinId(columns[1]);
                protein.setDescription(columns[2]);
                protein.setLocalization(columns[3]);
                protein.setEpitope(epitope);
                epitope.setProtein(protein);

                epitope.setEpitope(columns[4]);
                epitope.setStart(Integer.parseInt(columns[5]));
                epitope.setEndEpitope(Integer.parseInt(columns[6]));
                epitope.setNGlyc(columns[7]);
                epitope.setNGlycCount(Integer.parseInt(columns[8]));
                epitope.setNGlycMotifs(columns[9]);
                epitope.setLength(Integer.parseInt(columns[10]));
                epitope.setMolecularWeight(Double.parseDouble(columns[11]));
                epitope.setIsoelectricPoint(Double.parseDouble(columns[12]));
                epitope.setHydropathy(Double.parseDouble(columns[13]));
                epitope.setAllMatchesCover(Double.parseDouble(columns[14]));
                epitope.setAvgCover(Double.parseDouble(columns[15]));
                epitope.setBepiPred3(Double.parseDouble(columns[16]));
                epitope.setEmini(Double.parseDouble(columns[17]));
                epitope.setKolaskar(Double.parseDouble(columns[18]));
                epitope.setChouFosman(Double.parseDouble(columns[19]));
                epitope.setKarplusSchulz(Double.parseDouble(columns[20]));
                epitope.setParker(Double.parseDouble(columns[21]));
                epitope.setEpitopeTaskData(task);

                epitopes.add(epitope);
            }
        }
        return epitopes;
    }

    /**
     * Parses epitope topology data from a TSV file.
     *
     * @param filePath path to the topology TSV file
     * @return list of epitope topologies
     * @throws IOException if file cannot be read
     */
    public static List<EpitopeTopology> parseEpitopeTopology(String filePath) throws IOException {
        List<EpitopeTopology> topologies = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(filePath));

        Long currentN = null;
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty())
                continue;

            String[] parts = line.split("\t");
            if (!parts[0].trim().isEmpty()) {
                currentN = Long.parseLong(parts[0]);
            }
            String methodName = parts[4].trim();
            EpitopeTopology topology = createTopology(currentN, parts, methodName);
            topologies.add(topology);
        }
        return topologies;
    }

    private static EpitopeTopology createTopology(Long n, String[] parts, String methodName) {
        EpitopeTopology topology = new EpitopeTopology();
        topology.setN(n);

        try {
            String cleanedMethodName = methodName.trim();
            if (cleanedMethodName.equals("BepiPred-3.0")) {
                topology.setDescription(parts[4]);
                cleanedMethodName = "BepiPred";
            }
            Method method = Method.fromDescription(cleanedMethodName);
            topology.setMethod(method);
        } catch (IllegalArgumentException e) {
            log.error("Invalid method name '{}', using ALL_MATCHES as fallback.", methodName);
            topology.setMethod(Method.ALL_MATCHES);
        }

        try {
            if (parts.length >= 9) {
                topology.setThreshold(parseDoubleSafe(parts[5]));
                topology.setAvgScore(parseDoubleSafe(parts[6]));
                topology.setCover(parts[7].equals("-") ? 0.0 : parseDoubleSafe(parts[7]));
                topology.setTopologyData(parts[8]);
            }
        } catch (Exception e) {
            log.error("Error parsing topology data for method {}: {}", methodName, e.getMessage());
        }
        return topology;
    }

    private static double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Associates epitope topologies with epitopes.
     *
     * @param epitopes   list of epitopes
     * @param topologies list of topologies
     * @return epitopes with associated topologies
     */
    private List<Epitope> associateTopologies(List<Epitope> epitopes, List<EpitopeTopology> topologies) {
        Map<Long, List<EpitopeTopology>> topologyMap = topologies.stream()
                .collect(Collectors.groupingBy(EpitopeTopology::getN));

        for (Epitope epitope : epitopes) {
            List<EpitopeTopology> epitopeTopologies = topologyMap.get(epitope.getN());
            if (epitopeTopologies != null) {
                for (EpitopeTopology topo : epitopeTopologies) {
                    topo.setEpitope(epitope);
                }
                epitope.setEpitopeTopologies(epitopeTopologies);
            }
        }
        return epitopes;
    }

    /**
     * Associates BLAST results with epitopes.
     *
     * @param epitopes list of epitopes
     * @param blasts   list of BLAST results
     */
    private void associateBlasts(List<Epitope> epitopes, List<Blast> blasts) {
        Map<Long, List<Blast>> blastMap = blasts.stream()
                .collect(Collectors.groupingBy(Blast::getN));
        for (Epitope epitope : epitopes) {
            List<Blast> epitopeBlasts = blastMap.get(epitope.getN());
            if (epitopeBlasts != null) {
                for (Blast blast : epitopeBlasts) {
                    blast.setEpitope(epitope);
                }
                epitope.setBlasts(epitopeBlasts);
            }
        }
    }

}