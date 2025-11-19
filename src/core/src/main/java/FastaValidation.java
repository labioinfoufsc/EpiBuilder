import br.ufsc.epibuilder.EpitopeFinder;
import br.ufsc.epibuilder.converter.ProteinConverter;
import br.ufsc.epibuilder.util.FastaUtils;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.io.FastaReaderHelper;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Command-line and web model utility for validating protein FASTA sequences.
 * <p>
 * This class supports two execution modes:
 * <ul>
 * <li><b>CLI mode (NextFlow)</b>: Reads input FASTA file, validates sequences,
 * and writes outputs to files.</li>
 * <li><b>Web mode</b>: Provides safe methods for validation
 * without touching disk or calling System.exit.</li>
 * </ul>
 * Validation separates sequences into valid and invalid sets, and extracts
 * descriptions.
 */
@Command(name = "FastaValidation", mixinStandardHelpOptions = true, version = "1.0", description = "Validate protein FASTA sequences and separate valid/invalid ones.")
public class FastaValidation implements Callable<Integer> {

    @Option(names = { "-i",
            "--input" }, description = "Input FASTA file containing protein sequences.", required = true)
    private File inputFile;

    @Option(names = { "-d", "--description" }, description = "Output file to save descriptions.", required = true)
    private File descriptionFile;

    @Option(names = { "-v", "--valid" }, description = "Output file to save valid protein sequences.", required = true)
    private File validProteinsFile;

    @Option(names = { "-n",
            "--invalid" }, description = "Output file to save invalid protein sequences.", required = true)
    private File invalidProteinsFile;

    /**
     * Internal data structure to hold validation results in memory.
     * Contains lists of valid and invalid proteins, and a mapping of IDs to
     * descriptions.
     */
    private static class ValidationResult {
        List<ProteinConverter> validProteins = new ArrayList<>();
        List<ProteinConverter> invalidProteins = new ArrayList<>();
        Map<String, String> mapDescription = new HashMap<>();
    }

    /**
     * Executes FASTA validation in CLI mode.
     * <p>
     * Reads the input file, validates sequences, and writes results to output
     * files.
     *
     * @return exit code (0 for success, 1 if no valid proteins found)
     * @throws Exception if file reading or validation fails
     */
    @Override
    public Integer call() throws Exception {
        try (InputStream is = new FileInputStream(inputFile)) {
            ValidationResult result = validateStream(is);

            if (result.validProteins.isEmpty()) {
                System.out.println("No valid proteins. Please check your FASTA file.");
                return 1;
            }

            writeOutputs(result, descriptionFile, validProteinsFile, invalidProteinsFile);

            System.out.println("✅ FASTA validation completed successfully!");
            System.out.println("📄 Descriptions saved to: " + descriptionFile.getAbsolutePath());
            System.out.println("✅ Valid proteins saved to: " + validProteinsFile.getAbsolutePath());
            System.out.println("❌ Invalid proteins saved to: " + invalidProteinsFile.getAbsolutePath());

            return 0;
        }
    }

    /**
     * CLI entry point. Isolated from web module usage.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new FastaValidation()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Validates FASTA sequences for web module usage.
     * <p>
     * This method does not write to disk or call System.exit.
     *
     * @param inputStream input stream containing FASTA data
     * @return true if at least one valid protein is found
     * @throws Exception if parsing or validation fails
     */
    public static boolean validateForWeb(InputStream inputStream) throws Exception {
        ValidationResult result = validateStream(inputStream);
        return !result.validProteins.isEmpty();
    }

    /**
     * Reads FASTA sequences from an input stream, validates them, and separates
     * into valid/invalid sets.
     *
     * @param inputStream input stream containing FASTA data
     * @return validation results including valid proteins, invalid proteins, and
     *         descriptions
     * @throws Exception if parsing or validation fails
     */
    private static ValidationResult validateStream(InputStream inputStream) throws Exception {
        ValidationResult result = new ValidationResult();

        LinkedHashMap<String, ProteinSequence> mapProtein = FastaReaderHelper.readFastaProteinSequence(inputStream);

        if (mapProtein == null || mapProtein.isEmpty()) {
            return result;
        }

        for (ProteinSequence protein : mapProtein.values()) {
            String id = protein.getAccession().getID();
            String proteinId = id.split(" ")[0].trim();
            String sequence = protein.getSequenceAsString().toUpperCase();
            String header = protein.getOriginalHeader();
            String description = FastaUtils.extractName(header);

            ProteinConverter proteinProcess = new ProteinConverter(proteinId, sequence);

            if (FastaUtils.isSequenceValid(sequence)) {
                result.validProteins.add(proteinProcess);
                result.mapDescription.put(proteinId, description);
            } else {
                result.invalidProteins.add(proteinProcess);
            }
        }
        return result;
    }

    /**
     * Writes validation results to output files.
     * <p>
     * Generates:
     * <ul>
     * <li>Descriptions file (ID → description)</li>
     * <li>Valid proteins FASTA file</li>
     * <li>Invalid proteins FASTA file</li>
     * </ul>
     *
     * @param result      validation results
     * @param descFile    output file for descriptions
     * @param validFile   output file for valid proteins
     * @param invalidFile output file for invalid proteins
     * @throws Exception if writing to files fails
     */
    private void writeOutputs(ValidationResult result, File descFile, File validFile, File invalidFile)
            throws Exception {
        StringBuilder descriptions = new StringBuilder();
        descriptions.append("Id\tDescription\n");
        for (Map.Entry<String, String> entry : result.mapDescription.entrySet()) {
            descriptions.append(entry.getKey())
                    .append("\t")
                    .append(entry.getValue() == null ? "" : entry.getValue())
                    .append("\n");
        }

        Files.writeString(descFile.toPath(), descriptions);
        Files.writeString(validFile.toPath(), EpitopeFinder.generateReportFastaProteins(result.validProteins));
        Files.writeString(invalidFile.toPath(), EpitopeFinder.generateReportFastaProteins(result.invalidProteins));
    }
}