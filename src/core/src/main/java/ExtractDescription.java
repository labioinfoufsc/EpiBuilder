import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.io.FastaReaderHelper;

import java.io.*;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Command(name = "ExtractDescription", mixinStandardHelpOptions = true,
        description = "Extracts IDs and descriptions from a protein FASTA file and saves them to a TSV file.")
public class ExtractDescription implements Callable<Integer> {

    @Option(names = {"-i", "--input"}, required = true, description = "Input protein FASTA file path")
    private File inputFasta;

    @Option(names = {"-o", "--output"}, required = true, description = "Full path for output TSV file (required)")
    private File outputFile;

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new ExtractDescription()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {

        if (!inputFasta.exists()) {
            System.err.println("Error: Input FASTA file not found: " + inputFasta.getAbsolutePath());
            return 1;
        }

        Path outputDir = outputFile.toPath().getParent();
        if (outputDir == null) {
            outputDir = Paths.get(".");
        }
        Files.createDirectories(outputDir);
        System.out.println("Output directory ensured: " + outputDir.toAbsolutePath());

        System.out.println("Processing file: " + inputFasta.getAbsolutePath());
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath())) {
            writer.write("ID\tDescription\n");
            LinkedHashMap<String, ProteinSequence> sequences = FastaReaderHelper.readFastaProteinSequence(inputFasta);
            for (ProteinSequence record : sequences.values()) {
                String proteinId = record.getAccession().toString();
                String pattern = "^(\\S+)(?:[ \\t]+|\\s\\|\\s).*";
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(record.getOriginalHeader());
                if (m.find()) {
                    proteinId = m.group(1);
                }
                String fullHeader = record.getOriginalHeader().trim();

                String rawDescription = fullHeader.startsWith(proteinId) ? fullHeader.substring(proteinId.length()).trim() : fullHeader;
                String finalDescription = rawDescription;

                Pattern gpPattern = Pattern.compile("gene_product=(.*?) \\| transcript_product=");
                Matcher matcher = gpPattern.matcher(finalDescription);
                if (matcher.find()) {
                    finalDescription = matcher.group(1).trim();
                } else if (finalDescription.contains(" OS=")) {
                    finalDescription = finalDescription.split(" OS=")[0].trim();
                } else {
                    int bracketIndex = finalDescription.indexOf(" [");
                    if (bracketIndex >= 0) {
                        finalDescription = finalDescription.substring(0, bracketIndex).trim();
                    } else {

                        Pattern fallbackPattern = Pattern.compile("[ \t]+\\|?[ \t]*");
                        String[] parts = fallbackPattern.split(finalDescription, 2);
                        if (parts.length > 1) {
                            finalDescription = parts[1].trim();
                        }
                    }
                }
                finalDescription = finalDescription.replace("\t", " ");


                writer.write(proteinId + "\t" + finalDescription + "\n");
            }

        } catch (Exception e) {
            System.err.println("An error occurred during processing: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }

        System.out.println("Success! Data saved to: " + outputFile.getAbsolutePath());
        return 0;
    }
}
