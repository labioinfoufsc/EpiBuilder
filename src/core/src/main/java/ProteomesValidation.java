import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.io.FastaReaderHelper;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.Callable;

@Command(
        name = "ProteomesValidation",
        mixinStandardHelpOptions = true,
        description = "Loads multiple proteomes from FASTA files in the format id=file.fasta:id2=file2.fasta..."
)
public class ProteomesValidation implements Callable<Integer> {

    @Option(
            names = {"-p", "--proteomes"},
            required = true,
            description = "Proteome input in the format id=file.fasta:id2=file2.fasta..."
    )
    private String proteomes;

    @Override
    public Integer call() {
        try {
            String[] entries = proteomes.split(":");
            for (String entry : entries) {
                if (!entry.contains("=")) {
                    System.err.println("Invalid format for entry: " + entry);
                    return 1;
                }

                String[] parts = entry.split("=", 2);
                String id = parts[0].trim();
                String filePath = parts[1].trim();

                File inputFile = new File(filePath);
                if (!inputFile.exists()) {
                    System.err.println("File not found: " + filePath);
                    return 1;
                }

                System.out.println("Loading proteome: " + id + " (" + filePath + ")");
                try {
                    HashMap<String, ProteinSequence> mapProtein =
                            FastaReaderHelper.readFastaProteinSequence(inputFile);
                    System.out.println("  -> Loaded " + mapProtein.size() + " protein sequences.");
                } catch (Exception e) {
                    System.err.println("Error reading FASTA file for proteome " + id + ": " + e.getMessage());
                    e.printStackTrace(System.err);
                    return 1;
                }
            }

            System.out.println("All proteomes are validated successfully!");
            return 0;

        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace(System.err);
            return 1;
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ProteomesValidation()).execute(args);
        System.exit(exitCode);
    }
}
