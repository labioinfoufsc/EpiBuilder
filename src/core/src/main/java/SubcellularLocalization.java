import br.ufsc.epibuilder.util.docker.PSORTbDocker;
import br.ufsc.epibuilder.util.docker.WolfPSORTDocker;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;

@Command(name = "subcellular-localization",
        mixinStandardHelpOptions = true,
        version = "1.0",
        description = "Executes WolfPsort or PSORTb for subcellular localization prediction")
public class SubcellularLocalization implements Callable<Integer> {
    @Option(names = {"-i", "--input"}, required = true, description = "Path to input FASTA file")
    private Path inputFile;

    @Option(names = {"-o", "--output"}, required = true, description = "Path for output tsv results")
    private Path outputFile;

    @Option(names = {"-loc", "--localization"}, required=false, description = "Localization type: animal, fungi, plant, arch, gram_pos, gram_neg, none", defaultValue = "none")
    private String loc;

    private static String EPIBUILDER_VOLUME = System.getenv().getOrDefault("EPIBUILDER_VOLUME", "/tmp/epibuilder");
    // Mapeamento de localizações WolfPsort
    private static final Map<String, String> LOC_MAP = new HashMap<>() {{
        put("cyts", "Cytoskeleton");
        put("cyto", "Cytosol");
        put("E.R.", "Endoplasmic Reticulum");
        put("extr", "Extracellular");
        put("golg", "Golgi apparatus");
        put("mito", "Mitochondrion");
        put("nucl", "Nucleus");
        put("plas", "Plasma membrane");
        put("pero", "Peroxisome");
        put("vacu", "Vacuolar membrane");
        put("chlo", "Chloroplast");
    }};

    private static final Set<String> WOLFPSORT_LOCS = Set.of("animal", "fungi", "plant");
    private static final Set<String> PSORTB_LOCS = Set.of("arch", "gram_pos", "gram_neg");
    private static final Map<String, String> PSORT_FLAGS = Map.of(
            "arch", "-a",
            "gram_pos", "-p",
            "gram_neg", "-n"
    );

    @Override
    public Integer call() throws Exception {

        // Validação do arquivo de entrada
        if (!inputFile.toFile().exists()) {
            System.err.println("Error: File not found: " + inputFile);
            return 1;
        }

        // Validação do parâmetro loc
        if (!WOLFPSORT_LOCS.contains(loc) && !PSORTB_LOCS.contains(loc) && !loc.equals("none")) {
            System.err.println("Error: loc must be one of: animal, fungi, plant, arch, gram_pos, gram_neg, none");
            return 1;
        }

        // Define diretórios

        String baseDir = WOLFPSORT_LOCS.contains(loc)?"/tmp/epibuilder/wolfpsort": "/tmp/epibuilder/psortb";

        String filename = inputFile.toFile().getName();
        String randomId = UUID.randomUUID().toString();
        Path tmpDir = Paths.get(baseDir, randomId);

        // Cria diretório temporário
        Files.createDirectories(tmpDir);
        Path tmpFile = tmpDir.resolve(filename);
        Files.copy(inputFile, tmpFile, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("[INFO] Copied " + inputFile + " to " + tmpFile);

        try {
            // Executa WolfPsort, PSORTb ou cria arquivo vazio
            if (WOLFPSORT_LOCS.contains(loc)) {
                System.out.println("[INFO] PSORTb - WolfPSORT Localization: " +loc);
                executeWolfPsort(tmpDir, tmpFile);
                Path rawFile = tmpDir.getParent().resolve("raw_subcell.txt");
                Path tsvFile = tmpDir.resolve("localization.tsv");
                if(rawFile.toFile().exists()) {
                    copyResults(rawFile, outputFile.resolve("raw_subcell.txt"));
                }
                // Copia resultados de volta
                copyResults(tsvFile, outputFile);
            } else if (PSORTB_LOCS.contains(loc)) {
                System.out.println("[INFO] PSORTb - Subcellular Localization: " +loc);
                executePsortb(tmpDir, tmpFile);
                Path tsvFile = tmpDir.resolve("localization.tsv");
                // Copia resultados de volta
                copyResults(tsvFile, outputFile);
            } else if (loc.equals("none")) {
                System.out.println("[INFO] No predictions to perform.");
                createEmptyLocalization(outputFile);
            }

            System.out.println("[INFO] Results copied to: " + outputFile);

        } finally {
            // Limpa diretório temporário
//            deleteDirectory(tmpDir.toFile());
            System.out.println("[INFO] Temporary directory removed.");
        }

        return 0;
    }

    private void executeWolfPsort(Path tmpDir, Path tmpFile) throws Exception {
        Path rawFile = tmpDir.resolve("raw_subcell.txt");
        Path tsvFile = tmpDir.resolve("localization.tsv");

        System.out.println("[INFO] Running WolfPsort for " + loc + "...");

        // Monta comando Docker
        List<String> dockerCmd = Arrays.asList(
                "docker", "run", "--rm",
                "-v", EPIBUILDER_VOLUME + ":" + "/tmp/epibuilder",
                "bioinfoufsc/wolfpsort",
                "-i", tmpFile.toString(),
                "-s", loc,
                "-o", rawFile.toString()
        );

       int exitCode = new WolfPSORTDocker(tmpFile,loc, rawFile).call();
       if(exitCode == 0) {
           System.out.println("[INFO] Converting WolfPsort output to localization.tsv...");
           convertWolfPsortOutput(rawFile, tsvFile);
       }else{
           System.out.println("[ERROR] WolfPsort returned non-zero exit code.");
           System.exit(1);
       }
       // executeDockerCommand(dockerCmd, tmpDir.resolve("pipeline.log"));

    }

    private void convertWolfPsortOutput(Path rawFile, Path tsvFile) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(tsvFile);
             BufferedReader reader = Files.newBufferedReader(rawFile)) {

            // Escreve cabeçalho
            writer.write("SeqID\tLocalization\tScore\n");

            // Pula primeira linha (cabeçalho)
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Parse da linha: seqID locKey score
                String prediction = line.split(",")[0];
                String[] parts = prediction.trim().split("\\s+");

                if (parts.length >= 3) {
                    String seqId = parts[0];
                    String locKey = parts[1];
                    String score = parts[2];
                    String locName = LOC_MAP.getOrDefault(locKey, "Unknown");

                    writer.write(seqId + "\t" + locName + "\t" + score + "\n");
                }
            }
        }
    }

    private void executePsortb(Path tmpDir, Path tmpFile) throws Exception {
        String flag = PSORT_FLAGS.get(loc);
        if (flag == null) {
            throw new IllegalArgumentException("Invalid loc for PSORTb: " + loc);
        }

        Path tsvFile = tmpDir.resolve("localization.tsv");

        System.out.println("[INFO] Running PSORTb for " + loc + " (" + flag + ")...");

        List<String> dockerCmd = Arrays.asList(
                "docker", "run", "--rm",
                "-v", EPIBUILDER_VOLUME + ":" + "/tmp/epibuilder",
                "bioinfoufsc/psortb",
                "-i", tmpFile.toString(),
                flag,
                "-o", "terse",
                "-r", tsvFile.toString()
        );

        new PSORTbDocker(tmpFile,flag, tsvFile).call();
    }

    private void createEmptyLocalization(Path outputFile) throws IOException {

        Files.writeString(outputFile,"SeqID\tLocalization\tScore\n");
        System.out.println("[INFO] LOC=none → Created empty localization file at: " + outputFile);
    }

    private void executeDockerCommand(List<String> command, Path logFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        System.out.println(command.toString());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Captura output e escreve no log
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedWriter logWriter = Files.newBufferedWriter(logFile,
                     StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                logWriter.write(line + "\n");
            }
        }

        int exitCode = process.waitFor();

    }

    private void copyResults(Path source, Path target) throws IOException {
        System.out.println("[INFO] Copying results from " + source.toString() + " to " + target.toString());
        System.out.println(String.join("\n",Files.readAllLines(source)));
        Files.writeString(target,String.join("\n",Files.readAllLines(source)));
        System.out.println("=============================");
        System.out.println(String.join("\n",Files.readAllLines(target)));
    }

    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SubcellularLocalization()).execute(args);
        System.exit(exitCode);
    }
}