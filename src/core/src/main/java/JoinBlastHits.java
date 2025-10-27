import br.ufsc.epibuilder.blast.Blast;
import br.ufsc.epibuilder.blast.ReportBlastJoiner;

import java.io.File;
import java.nio.file.Files;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
        name = "JoinBlastHits",
        mixinStandardHelpOptions = true,
        version = "1.0",
        description = "Join BLAST output hits into a detailed report."
)
public class JoinBlastHits implements Callable<Integer> {

    @Option(
            names = {"-r", "--report"},
            description = "Path to the detailed report file.",
            required = true
    )
    private File reportDetailedFile;

    @Option(
            names = {"-b", "--blast"},
            description = "Path(s) to the BLAST output file(s).",
            required = true,
            arity = "1..*"   // aceita 1 ou mais arquivos
    )
    private File[] blastOutputFiles;

    @Option(
            names = {"--output"},
            description = "Path to the detailed report file joined.",
            required = true
    )
    private File reportDetailedFileBlast;

    // Novos parâmetros para identidade e cobertura
    @Option(
            names = {"--identity"},
            description = "Minimum percent identity to filter BLAST hits.",
            required = true
    )
    private double identity;

    @Option(
            names = {"--cover"},
            description = "Minimum percent coverage to filter BLAST hits.",
            required = true
    )
    private double cover;

    @Override
    public Integer call() throws Exception {
        joinBlastHits(reportDetailedFile, blastOutputFiles, reportDetailedFileBlast, identity, cover);
        System.out.println("✅ Report successfully joined and updated: " + reportDetailedFileBlast.getAbsolutePath());
        return 0;
    }

    public static void joinBlastHits(File reportDetailedFile, File blastOutputFiles[],
                                     File reportDetailedFileBlast, double identity, double cover) throws Exception {
        String reportDetailed = Files.readString(reportDetailedFile.toPath());
        for(File blastFile: blastOutputFiles) {
            if (blastFile.getName().endsWith("raw.csv")) {
                continue;
            }
            String organism =  blastFile.getName().replace("_blast.csv", "");
            Blast blast = new Blast(organism, blastFile);

            reportDetailed = ReportBlastJoiner.joinReport(
                    reportDetailed,
                    blast.getListReport(identity, cover),  // usa os valores passados como parâmetro
                    blast.getName()
            );
            Files.writeString(reportDetailedFileBlast.toPath(), reportDetailed);
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new JoinBlastHits()).execute(args);
        System.exit(exitCode);
    }
}
