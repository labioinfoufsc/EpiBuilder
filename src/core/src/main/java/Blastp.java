import br.ufsc.epibuilder.blast.Blast;
import br.ufsc.epibuilder.blast.ReportBlastJoiner;
import br.ufsc.epibuilder.entity.Proteome;
import br.ufsc.epibuilder.util.docker.BlastpDocker;
import br.ufsc.epibuilder.util.docker.MakeblastdbDocker;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

@Command(
        name = "Blastp",
        mixinStandardHelpOptions = true,
        description = "Loads multiple proteomes from FASTA files in the format id=file.fasta:id2=file2.fasta..."
)
public class Blastp implements Callable<Integer> {

    @Option(names = {"-i", "--input"}, required = true, description = "Path to input epitope FASTA file")
    private Path inputEpitopeFile;

    @Option(
            names = {"-p", "--proteomes"},
            required = true,
            description = "Proteome input in the format id=file.fasta:id2=file2.fasta..."
    )
    private String proteomes;

    @Option(
            names = {"--identity"},
            defaultValue = "90",
            description = "Minimum percentage identity threshold",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS

    )
    private double identity;

    @Option(
            names = {"--cover"},
            defaultValue = "90",
            description = "Minimum query coverage percentage threshold",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS
    )
    private double cover;

    @Option(
            names = {"-r", "--report"},
            description = "Report parameter produced by epibuilder"
    )
    private File reportDetailedFile;

    @Option(
            names = {"--output"},
            description = "Path to the detailed report file joined.",
            required = true
    )
    private File reportDetailedFileBlast;
    private Path tmpDir;
    private Path tmpEpitopeFile;

    @Override
    public Integer call() {
        try {
            Path baseDir = Paths.get("/tmp/epibuilder/blast");
            String filename = inputEpitopeFile.getFileName().toString();

            String randomId = UUID.randomUUID().toString();
            tmpDir = Files.createDirectories(baseDir.resolve(randomId));
            tmpEpitopeFile = Files.copy(inputEpitopeFile, tmpDir.resolve(filename));

            String[] entries = proteomes.split(":");


            String reportDetailed = Files.readString(reportDetailedFile.toPath());

            for (String entry : entries) {
                if (!entry.contains("=")) {
                    System.err.println("Invalid format for entry: " + entry);
                    return 1;
                }

                String[] parts = entry.split("=", 2);
                String id = parts[0].trim();
                String filePath = parts[1].trim();

                Proteome proteome = new Proteome(id, new File(filePath));
                reportDetailed = runBlastp(proteome, reportDetailed);

            }
            Files.writeString(reportDetailedFileBlast.toPath(), reportDetailed);

            System.out.println("All proteomes are validated successfully!");
            return 0;

        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace(System.err);
            return 1;
        }
    }

    public String runBlastp(Proteome proteome, String reportDetailed) throws Exception {

        System.out.printf("BLAST in database %s%n", proteome.getFile().getAbsolutePath());
        Path tmpDb = tmpDir.resolve(proteome.getFile().getName());
        Files.copy(proteome.getFile().toPath(), tmpDb, StandardCopyOption.REPLACE_EXISTING);
        Path tmpOut = tmpDir.resolve(proteome.getOrganism() + "_blast.raw.csv");
        int exitCode = new MakeblastdbDocker(tmpDir, tmpDb).call();
        if (exitCode == 0) {
            exitCode = new BlastpDocker(tmpDir, tmpEpitopeFile, tmpDb, tmpOut).call();
            List<String> lines = Files.readAllLines(tmpOut);

            // Filtra as linhas baseado em identity e cover
            List<String> filteredLines = new ArrayList<>();
            filteredLines.add("qacc\tsacc\tpident\tqcovs\tqseq\tsseq");

            for (String line : lines) {
                String[] columns = line.split("\t");
                if (columns.length >= 4) {
                    try {
                        double pident = Double.parseDouble(columns[2]);
                        double qcovs = Double.parseDouble(columns[3]);

                        if (pident >= identity && qcovs >= cover) {
                            filteredLines.add(line);
                        }
                    } catch (NumberFormatException e) {
                        // Ignora linhas com formato inválido
                        System.err.println("Warning: Skipping line with invalid format: " + line);
                    }
                }
            }

            Path destination = Paths.get(tmpOut.getFileName().toString().replace("raw.csv",".csv"));
            Files.write(destination, filteredLines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            Blast blast = new Blast(proteome.getOrganism(), destination.toFile());


            System.out.printf("Filtered results: %d/%d lines passed filters (identity >= %.1f%%, cover >= %.1f%%)%n",
                    filteredLines.size() - 1, lines.size(), identity, cover);
            return ReportBlastJoiner.joinReport(
                    reportDetailed,
                    blast.getListReport(identity, cover),  // usa os valores passados como parâmetro
                    blast.getName()
            );


        } else {
            throw new Exception("Error executing BLAST for proteome: " + proteome.getFile().getAbsolutePath());
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Blastp()).execute(args);
        System.exit(exitCode);
    }
}