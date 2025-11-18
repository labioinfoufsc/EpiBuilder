import br.ufsc.epibuilder.util.docker.BepiPred3Docker;
import br.ufsc.epibuilder.util.docker.GPUChecker;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.io.FastaReaderHelper;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(
        name = "RunBepipred",
        description = "Runs BepiPred3 Docker analysis on a FASTA file",
        mixinStandardHelpOptions = true,
        version = "1.0"
)
public class Bepipred3 implements Callable<Integer> {

    @Option(names = {"-i", "--input"}, required = true, description = "Path to input FASTA file")
    private Path inputFile;

    @Option(names = {"-o", "--output"}, required = true, description = "Path for output csv results")
    private Path output;

    @Option(names = {"-s", "--size"}, required = false, description = "Protein sizes will be processed at the same time (split fasta). Default is 100.")
    private Integer size;

    @Option(
            names = {"--use-gpu"},
            defaultValue = "false",
            description = "Enable GPU acceleration (default: false)"
    )
    private boolean useGpu;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Bepipred3()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        try {
            // === 1. Verifica se o arquivo de entrada existe ===
            if (!Files.exists(inputFile)) {
                System.err.println("Error: File not found: " + inputFile);
                System.exit(1);
            }

            // === 3. Define diretórios internos ===
            Path baseDir = Paths.get("/tmp/epibuilder/bepipred");
            String filename = inputFile.getFileName().toString();

            String randomId = UUID.randomUUID().toString();
            Path tmpDir = baseDir.resolve(randomId);
            Files.createDirectories(tmpDir);

            Path tmpFile = tmpDir.resolve(filename);
            Files.copy(inputFile, tmpFile, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("[INFO] Copied " + inputFile + " to " + tmpFile);

            // === 4. Testa GPU ===
            System.out.println("Checking for GPU support...");
            if(useGpu) {
                useGpu = new GPUChecker().hasGpu();
                if (useGpu)
                    System.out.println("✅ GPU detected, running with GPU support...");
                else
                    System.out.println("⚠️ No GPU available or error occurred, running on CPU...");
            }

            // === 5. Define volume ===
            HashMap<String, ProteinSequence> mapProtein = FastaReaderHelper.readFastaProteinSequence(tmpFile.toFile());
            Collection<ProteinSequence> proteinsFasta = mapProtein.values();
            List<ProteinSequence> list = new ArrayList<>(proteinsFasta);

            Integer batchSize = size != null ?  size : Integer.valueOf(100);

            StringBuilder bepipredOut = new StringBuilder();
            bepipredOut.append("Accession,Residue,BepiPred-3.0 score,BepiPred-3.0 linear epitope score\n");
            for (int i = 0; i < list.size(); i += batchSize) {
                int end = Math.min(i + batchSize, list.size());
                List<ProteinSequence> batch = list.subList(i, end);
                System.out.printf("Processing batch %s to %s (size=%s) \n", i, (end - 1), batch.size());
                StringBuilder sb = new StringBuilder();
                for (ProteinSequence protein : batch) {
                    sb.append(">"+protein.getAccession()).append("\n");
                    sb.append(protein.getSequenceAsString()).append("\n");
                }
                Path inputTmp = tmpDir.resolve(inputFile.getFileName());
                if (!Files.exists(inputTmp)) {
                    Files.delete(inputTmp);
                }
                Files.writeString(inputTmp, sb);
                System.out.println("[INFO] Running container...");

                int exitCode = new BepiPred3Docker(inputTmp,useGpu).call();
                if(exitCode==0) {
                    Path path = tmpDir.resolve("raw_output.csv");
                    String resp = Files.lines(path)
                            .skip(1)
                            .collect(Collectors.joining(System.lineSeparator()));
                    bepipredOut.append(resp);
                    System.out.println("[INFO] Analysis completed inside container.");
                }
            }

            Files.writeString(output.toAbsolutePath(), bepipredOut.toString());

            System.out.println("[INFO] Results copied to: " + output.toAbsolutePath());
            deleteRecursively(tmpDir);
            System.out.println("[INFO] Temporary directory removed.");

        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
        return 0;
    }

    // === Métodos auxiliares ===

    private static boolean checkGpuSupport() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "bash", "-c", "docker run --rm --runtime=nvidia --gpus all ubuntu nvidia-smi > /dev/null 2>&1"
            );
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void runCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null)
                System.out.println(line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code " + exitCode);
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        System.err.println("Failed to delete: " + p);
                    }
                });
    }
}
