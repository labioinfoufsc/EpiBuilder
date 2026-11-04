import br.ufsc.epibuilder.util.EpidopeScoresConverter;
import br.ufsc.epibuilder.util.docker.EpidopeDocker;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.io.FastaReaderHelper;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(
        name = "RunEpidope",
        description = "Runs Epidope Docker analysis on a FASTA file",
        mixinStandardHelpOptions = true,
        version = "1.0"
)
public class Epidope implements Callable<Integer> {

    @Option(names = {"-i", "--input"}, required = true, description = "Path to input FASTA file")
    private Path inputFile;

    @Option(names = {"-o", "--output"}, required = true, description = "Path for output csv results")
    private Path output;

    @Option(names = {"-s", "--size"}, required = false, description = "Protein sizes will be processed at the same time (split fasta). Default is 2^31-1.")
    private Integer size;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Epidope()).execute(args);
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
            Path baseDir = Paths.get("/tmp/epibuilder/epidope");
            String filename = inputFile.getFileName().toString();

            String randomId = UUID.randomUUID().toString();
            Path tmpDir = baseDir.resolve(randomId);
            Files.createDirectories(tmpDir);

            Path tmpFile = tmpDir.resolve(filename);
            Files.copy(inputFile, tmpFile, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("[INFO] Copied " + inputFile + " to " + tmpFile);


            // === 5. Define volume ===
            HashMap<String, ProteinSequence> mapProtein = FastaReaderHelper.readFastaProteinSequence(tmpFile.toFile());
            Collection<ProteinSequence> proteinsFasta = mapProtein.values();
            List<ProteinSequence> list = new ArrayList<>(proteinsFasta);

            Integer batchSize = size != null ?  size : Integer.MAX_VALUE;

            StringBuilder bepipredOut = new StringBuilder();
            bepipredOut.append("Accession,Residue,Epidope-v0.3 score,Epidope-v0.3 linear epitope score\n");
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

                int exitCode = new EpidopeDocker(inputTmp).call();
                if(exitCode==0) {
                    Path path = tmpDir.resolve("epidope_scores.csv");
                    Path outputConverted = tmpDir.resolve("epidope_scores_converted.csv");
                    EpidopeScoresConverter.convert(path, outputConverted);

                    String resp = Files.lines(outputConverted)
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
