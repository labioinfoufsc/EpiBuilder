package ufsc.br.epibuilder.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import io.jsonwebtoken.lang.Objects;
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
import ufsc.br.epibuilder.model.Organism;

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
import java.io.File;
import java.time.ZonedDateTime;

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
            String epibuilderVolume = System.getenv("EPIBUILDER_VOLUME");
            if (epibuilderVolume == null || epibuilderVolume.isEmpty()) {
                epibuilderVolume = "/tmp/epibuilder";
            }

            StringBuilder fullCommand = new StringBuilder();
            fullCommand.append("docker run --rm ");
            fullCommand.append("-v /var/run/docker.sock:/var/run/docker.sock ");
            fullCommand.append(String.format("-v %s:/tmp/epibuilder ", epibuilderVolume));
            fullCommand.append(String.format("-e EPIBUILDER_VOLUME=%s ", epibuilderVolume));
            fullCommand.append("bioinfoufsc/epibuilder-core:latest epibuilder ");
            fullCommand.append("--input_file ").append(taskData.getFile().getAbsolutePath()).append(" ");
            fullCommand.append("--output ").append(taskData.getCompleteBasename()).append(" ");

            log.info("Adding parameters to command: {}", taskData.getActionType().getDesc());

            if (ActionType.DEFAULT.toString().equalsIgnoreCase(taskData.getActionType().getDesc())) {
                taskData.setBepipredThreshold(0.1512);
                taskData.setMinEpitopeLength(10);
                taskData.setMaxEpitopeLength(30);
            }

            String locParam = null;
            BiologicalClassification classification = taskData.getBiologicalClassification();

            if (classification != null && classification.getCellType() != null) {
                switch (classification.getCellType()) {
                    case EUKARYOTE:
                        if (classification.getOrganism() != null) {
                            locParam = classification.getOrganism().toString().toLowerCase();
                        }
                        break;

                    case BACTERIA:
                        if (classification.getBacterialType() != null) {
                            switch (classification.getBacterialType()) {
                                case GRAM_POSITIVE:
                                    locParam = "gram_pos";
                                    break;
                                case GRAM_NEGATIVE:
                                    locParam = "gram_neg";
                                    break;
                            }
                        }
                        break;

                    case ARCHAEA:
                        locParam = "arch";
                        break;

                    case NONE:
                        break;
                }
            }

            List<String> validParams = List.of("animal", "plant", "fungi", "arch", "gram_pos", "gram_neg");
            if (locParam != null && validParams.contains(locParam)) {
                fullCommand.append("--loc ").append(locParam).append(" ");
            }

            if (taskData.getBepipredThreshold() != null) {
                fullCommand.append("--threshold ").append(taskData.getBepipredThreshold()).append(" ");
            }
            if (taskData.getMinEpitopeLength() != null) {
                fullCommand.append("--min-length ").append(taskData.getMinEpitopeLength()).append(" ");
            }
            if (taskData.getMaxEpitopeLength() != null) {
                fullCommand.append("--max-length ").append(taskData.getMaxEpitopeLength()).append(" ");
            }

            if (taskData.isDoBlast()) {
                List<String> proteomes = taskData.getProteomes().stream()
                        .map(Database::toString)
                        .collect(Collectors.toList());

                fullCommand.append("--proteomes ").append(String.join(":", proteomes)).append(" ");

                if (taskData.getBlastMinCoverCutoff() != 90) {
                    fullCommand.append("--cover ").append(taskData.getBlastMinCoverCutoff()).append(" ");
                }

                if (taskData.getBlastMinIdentityCutoff() != 90) {
                    fullCommand.append("--identity ").append(taskData.getBlastMinIdentityCutoff()).append(" ");
                }
            }

            command.add(fullCommand.toString().trim());

            log.info("Command to run: {}", fullCommand.toString().trim());

            ProcessBuilder processBuilder = new ProcessBuilder(command);

            Map<String, String> env = processBuilder.environment();
            String blastPath = "/usr/local/bin";
            String currentPath = env.getOrDefault("PATH", "");

            log.info("Checking if BLAST path is already in PATH...");
            if (!currentPath.contains(blastPath)) {
                log.info("Adding BLAST path to environment variables...");
                env.put("PATH", blastPath + ":" + currentPath);
            }

            Path workDir = Paths.get(taskData.getCompleteBasename());
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

    /*
     * * Stops a running process by its PID.
     * 
     * @param pid
     * 
     * @return true if the process was successfully stopped, false otherwise.
     */
    public boolean stopProcessByPid(Long pid) {
        try {
            ProcessHandle.of(pid).ifPresent(process -> {
                process.destroy();
            });
            return true;
        } catch (Exception e) {
            log.error("Failed to stop process with PID {}: {}", pid, e.getMessage(), e);
            return false;
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

    public void processCompletedTask(EpitopeTaskData task) {
        Path completePath = Paths.get(task.getCompleteBasename());

        if (!Files.exists(completePath)) {
            log.error("Complete directory not found for task {}: {}", task.getId(), completePath);
            task.getTaskStatus().setStatus(Status.FAILED);
            epitopeTaskDataService.save(task);
            return;
        }

        try {

            log.info("Checking for result files in {}", completePath);

            Path topologyPath = completePath.resolve("topology.tsv");
            Path epitopePath = completePath.resolve("epitope-detail.tsv");
            Path proteinSummary = completePath.resolve("protein-summary.tsv");

            log.info("Localization param: {}", task.getLocalizationParam());
            Path localizationPath = null;
            if (task.getLocalizationParam() != null) {
                localizationPath = completePath.resolve("localization.tsv");
            }

            log.info("Verifying existence of required files...");
            List<String> missingFiles = new ArrayList<>();
            if (!Files.exists(topologyPath)) {
                missingFiles.add("topologyPath");
            }
            if (!Files.exists(epitopePath)) {
                missingFiles.add("epitopePath");
            }
            if (!Files.exists(proteinSummary)) {
                missingFiles.add("proteinSummary");
            }
            if (localizationPath != null) {
                if (!Files.exists(localizationPath) && task.getLocalizationParam() != null) {
                    missingFiles.add("localizationPath");
                }
            }

            if (!missingFiles.isEmpty()) {
                log.error("Missing result files in {} for task {}: {}", completePath, task.getId(),
                        String.join(", ", missingFiles));
                task.getTaskStatus().setStatus(Status.FAILED);
                epitopeTaskDataService.save(task);
                return;
            } else {
                log.info("All required files are present for task {}", task.getId());
            }

            log.info("Converting epitope file for task...");
            List<Epitope> epitopes = convertTsvToEpitopes(epitopePath.toString(), task);
            log.info("Epitopes converted: {}", epitopes.size());

            log.info("Converting topology file for task...");
            List<EpitopeTopology> topologies = parseEpitopeTopology(topologyPath.toString());
            log.info("Topologies converted: {}", topologies.size());

            log.info("Associating topologies with epitopes...");
            List<Epitope> completeEpitopes = associateTopologies(epitopes, topologies);
            log.info("Topologies associated with epitopes: {}", completeEpitopes.size());

            if (task.getLocalizationParam() != null) {
                log.info("Converting localization file for task...");
                List<Protein> proteins = convertTsvToProteins(localizationPath.toString());
                log.info("Proteins converted: {}", proteins.size());

                log.info("Associating localization with epitope proteins...");
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
                log.info("Localization successfully applied to epitope proteins.");

            }

            log.info("Checking for BLAST files in {}", completePath);
            try {
                List<Path> blastFiles = Files.list(completePath)
                        .filter(path -> path.getFileName().toString().endsWith("blast.csv"))
                        .collect(Collectors.toList());

                log.info("Found {} BLAST files", blastFiles.size());

                if (!blastFiles.isEmpty()) {
                    for (Path searchPath : blastFiles) {
                        log.info("Processing BLAST file: {}", searchPath.getFileName());

                        List<Blast> convertedBlasts = parseBlastCsv(searchPath.toString());
                        log.info("BLAST converted: {}", convertedBlasts.size());

                        associateBlasts(completeEpitopes, convertedBlasts);

                        log.info("BLAST completed for file {}", searchPath.getFileName());
                    }
                } else {
                    log.warn("No BLAST files found to process in directory: {}", completePath);
                }
            } catch (IOException e) {
                log.error("Error while processing BLAST files: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to process BLAST files", e);
            }

            log.info("Updating managed epitope list on task {}...", task.getId());

            List<Epitope> managedEpitopes = task.getEpitopes();
            if (managedEpitopes == null) {
                managedEpitopes = new ArrayList<>();
                task.setEpitopes(managedEpitopes); 
            }

            managedEpitopes.clear(); 
            managedEpitopes.addAll(completeEpitopes);
            log.info("Managed epitope list updated with {} epitopes.", completeEpitopes.size());

            int proteomeSize = countProteins(proteinSummary.toString());
            task.setProteomeSize(proteomeSize);

            if (task.getTaskStatus().getStatus() != Status.IMPORTED) {
                task.getTaskStatus().setStatus(Status.COMPLETED);
            }
            LocalDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalDateTime();
            task.setFinishedDate(now);

            epitopeTaskDataService.save(task);

            log.info("Successfully processed results for task {}", task.getId());

        } catch (IOException e) {
            log.error("Error processing result files for task {}: {}", task.getId(), e.getMessage());
            task.getTaskStatus().setStatus(Status.FAILED);
            epitopeTaskDataService.save(task);
        }
    }

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
                if (parts.length < 3)
                    continue;

                String seqId = parts[0].trim();
                String localization = parts[1].trim();

                Protein protein = new Protein();
                protein.setProteinId(seqId);
                protein.setLocalization(localization);

                proteins.add(protein);
            }

        } catch (IOException e) {
            log.error("Error reading localization file: {}", e.getMessage(), e);
        }

        return proteins;
    }

    public static String extractDBName(String path) {
        String regex = "(.*?)\\_blast.csv";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(path);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "DB name not found";
        }
    }

    public List<Blast> parseBlastCsv(String filePath) throws IOException {
        List<Blast> blastList = new ArrayList<>();

        String dbName = extractDBName(filePath);

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
                log.info("Columns length: {}", columns.length);
                for (int i = 0; i < columns.length; i++) {
                    columns[i] = columns[i].trim();
                }

                Blast blast = new Blast();

                String qacc = columns[0];
                String[] qaccParts = qacc.split("-");
                if (qaccParts.length > 0) {
                    try {
                        blast.setN(Long.parseLong(qaccParts[0]));
                    } catch (NumberFormatException e) {
                        log.warn("Value of N invalid for line: {}", line);
                        blast.setN(null);
                    }
                }

                blast.setSacc(columns[1]);

                try {
                    blast.setPident(Double.parseDouble(columns[2]));
                } catch (NumberFormatException e) {
                    log.warn("Invalid pindent: {}", line);
                    blast.setPident(null);
                }

                try {
                    blast.setQcovs(Double.parseDouble(columns[3]));
                } catch (NumberFormatException e) {
                    log.warn("Line Qcovs: {}", line);
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

    public static int countProteins(String pathFile) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(pathFile))) {
            String line = br.readLine();

            int countProtein = 0;
            while ((line = br.readLine()) != null) {
                countProtein++;
            }
            return countProtein;
        }
    }

    public static List<Epitope> convertTsvToEpitopes(String filePath, EpitopeTaskData task) throws IOException {
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

        reader.close();
        return epitopes;
    }

    public static List<EpitopeTopology> parseEpitopeTopology(String filePath) throws IOException {
        List<EpitopeTopology> topologies = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(filePath));

        Long currentN = null;

        for (int i = 1; i < lines.size(); i++) {

            String line = lines.get(i);

            log.info("Processing line: {}", line);

            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split("\t");

            log.info("Parts length: {}", parts.length);

            if (!parts[0].trim().isEmpty()) {
                currentN = Long.parseLong(parts[0]);
                String methodName = parts[4].trim();
                EpitopeTopology topology = createTopology(currentN, parts, methodName);
                topologies.add(topology);
            } else {
                String methodName = parts[4].trim();
                EpitopeTopology topology = createTopology(currentN, parts, methodName);
                topologies.add(topology);
            }
        }

        return topologies;
    }

    private static EpitopeTopology createTopology(Long n, String[] parts, String methodName) {
        EpitopeTopology topology = new EpitopeTopology();
        topology.setN(n);

        log.info("Creating topology for N: {}", n);
        log.info("Parts: {}", (Object) parts);
        log.info("Method name: {}", methodName);

        try {
            String cleanedMethodName = methodName.trim();

            if (cleanedMethodName.equals("BepiPred-3.0")) {
                topology.setDescription(parts[4]);
                cleanedMethodName = "BepiPred";
            }

            log.info("Method name: {}", cleanedMethodName);

            Method method = Method.fromDescription(cleanedMethodName);
            log.info(method.getDescription());
            topology.setMethod(method);
        } catch (IllegalArgumentException e) {
            log.error("Invalid method name: '{}', using ALL_MATCHES as fallback. Error: {}", methodName,
                    e.getMessage());
            topology.setMethod(Method.ALL_MATCHES);
        }

        try {
            if (parts == null || parts.length < 4) {
                log.warn("Insufficient data for topology. Parts length: {}", parts == null ? "null" : parts.length);
                return topology;
            }

            topology.setThreshold(parseDoubleSafe(parts[5]));
            topology.setAvgScore(parseDoubleSafe(parts[6]));
            topology.setCover(parts[7].equals("-") ? 0.0 : parseDoubleSafe(parts[7]));
            topology.setTopologyData(parts[8]);

        } catch (Exception e) {
            log.error("Error parsing topology data for method {}: {}", methodName, e.getMessage());
        }

        return topology;
    }

    private static Double parseDoubleSafe(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("-")) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.replaceAll("[^0-9.-]", ""));
        } catch (NumberFormatException e) {
            log.warn("Invalid number format: {}, using 0.0", value);
            return 0.0;
        }
    }

    public static List<Epitope> associateBlasts(List<Epitope> epitopes, List<Blast> blasts) {
        Map<Long, Epitope> epitopeMap = epitopes.stream()
                .collect(Collectors.toMap(Epitope::getN, e -> e));

        for (Blast blast : blasts) {
            Epitope epitope = epitopeMap.get(blast.getN());
            if (epitope != null) {
                blast.setEpitope(epitope);

                if (epitope.getBlasts() == null) {
                    epitope.setBlasts(new ArrayList<>());
                }
                epitope.getBlasts().add(blast);
            }
        }

        return epitopes;
    }

    public static List<Epitope> associateTopologies(List<Epitope> epitopes, List<EpitopeTopology> topologies) {
        Map<Long, Epitope> epitopeMap = epitopes.stream()
                .collect(Collectors.toMap(Epitope::getN, e -> e));

        for (EpitopeTopology topology : topologies) {
            Epitope epitope = epitopeMap.get(topology.getN());
            if (epitope != null) {
                if (epitope.getEpitopeTopologies() == null) {
                    epitope.setEpitopeTopologies(new ArrayList<>());
                }
                topology.setEpitope(epitope);
                epitope.getEpitopeTopologies().add(topology);
            }
        }

        return epitopes;
    }

    private boolean isProcessRunning(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }
}
