import br.ufsc.epibuilder.EpitopeFinder;
import br.ufsc.epibuilder.converter.ProteinConverter;
import br.ufsc.epibuilder.util.FastaUtils;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.io.FastaReaderHelper;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Callable;

@Command(
        name = "FastaValidation",
        mixinStandardHelpOptions = true,
        version = "1.0",
        description = "Validate protein FASTA sequences and separate valid/invalid ones."
)
public class FastaValidation implements Callable<Integer> {

    @Option(
            names = {"-i", "--input"},
            description = "Input FASTA file containing protein sequences.",
            required = true
    )
    private File inputFile;

    @Option(
            names = {"-d", "--description"},
            description = "Output file to save the protein descriptions extracted from Fasta File (ID and description).",
            required = true
    )
    private File descriptionFile;

    @Option(
            names = {"-v", "--valid"},
            description = "Output file to save valid protein sequences.",
            required = true
    )
    private File validProteinsFile;

    @Option(
            names = {"-n", "--invalid"},
            description = "Output file to save invalid protein sequences.",
            required = true
    )
    private File invalidProteinsFile;

    @Override
    public Integer call() throws Exception {
        validate(inputFile, descriptionFile, validProteinsFile, invalidProteinsFile);
        System.out.println("✅ FASTA validation completed successfully!");
        System.out.println("📄 Descriptions saved to: " + descriptionFile.getAbsolutePath());
        System.out.println("✅ Valid proteins saved to: " + validProteinsFile.getAbsolutePath());
        System.out.println("❌ Invalid proteins saved to: " + invalidProteinsFile.getAbsolutePath());
        return 0;
    }

    public static void validate(File inputFile, File descriptionFile, File validProteinsFile, File invalidProteinsFile) throws Exception {
        HashMap<String, ProteinSequence> mapProtein = FastaReaderHelper.readFastaProteinSequence(inputFile);
        Collection<ProteinSequence> proteinsFasta = mapProtein.values();
        ArrayList<ProteinConverter> validProteins = new ArrayList<>();
        ArrayList<ProteinConverter> invalidProteins = new ArrayList<>();
        HashMap<String, String> mapDescription = new HashMap<>();

        for (ProteinSequence protein : proteinsFasta) {
            String id = protein.getAccession().getID();
            String proteinId = id.split(" ")[0].trim();
            String sequence = protein.getSequenceAsString().toUpperCase();
            String header = protein.getOriginalHeader();
            String description = FastaUtils.extractName(header);
            ProteinConverter proteinProcess = new ProteinConverter(proteinId, sequence);

            if (FastaUtils.isSequenceValid(sequence)) {
                validProteins.add(proteinProcess);
                mapDescription.put(proteinId, description);
            } else {
                invalidProteins.add(proteinProcess);
            }
        }

        StringBuilder descriptions = new StringBuilder();
        descriptions.append("Id\tDescription\n");
        for (Map.Entry<String, String> entry : mapDescription.entrySet()) {
            descriptions.append(entry.getKey())
                    .append("\t")
                    .append(entry.getValue() == null ? "" : entry.getValue())
                    .append("\n");
        }
        if(validProteins.isEmpty()){
            System.out.println("No valid proteins. Please check your FASTA file.");
            System.exit(1);
        }

        Files.writeString(descriptionFile.toPath(), descriptions);
        Files.writeString(validProteinsFile.toPath(), EpitopeFinder.generateReportFastaProteins(validProteins));
        Files.writeString(invalidProteinsFile.toPath(), EpitopeFinder.generateReportFastaProteins(invalidProteins));
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new FastaValidation()).execute(args);
        System.exit(exitCode);
    }
}
