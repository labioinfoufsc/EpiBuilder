import br.ufsc.epibuilder.EpitopeFinder;
import br.ufsc.epibuilder.Parameters;
import br.ufsc.epibuilder.converter.CSVConverter;
import br.ufsc.epibuilder.converter.ProteinConverter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 * Converts a CSV file containing residue-level B-cell epitope prediction
 * scores into a FASTA file by reconstructing protein sequences from
 * their corresponding accession identifiers and amino acid residues.
 *
 * The input CSV must contain four columns in the following order:
 * Accession, Residue, Score, Linear Epitope Score.
 *
 * The Residue column must contain single-letter amino acid codes.
 * Each row represents one amino acid residue, and residues belonging
 * to the same accession are assembled into a protein sequence.
 *
 * Expected CSV format:
 * Accession,Residue,Score,Linear Epitope Score
 */
@Command(name = "FastaFromCSV", mixinStandardHelpOptions = true,
        description = "Converts a CSV file into a FASTA file")
public class FastaFromCSV implements Callable<Integer> {

    @Option(names = "--input", required = true, description = "Input CSV file path")
    private File inputFile;

    @Option(names = "--output", required = true, description = "Output FASTA file path")
    private File outputFile;

    @Option(names = { "-pred", "--prediction" }, required = false, description = "Input prediction file (${COMPLETION-CANDIDATES}). Default: ${DEFAULT-VALUE} ", defaultValue = "bepipred")
    Parameters.PredictionType predictionType;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new FastaFromCSV()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("Input is CSV → creating FASTA");
        System.out.println("Input file: " + inputFile.getAbsolutePath());
        System.out.println("Output file: " + outputFile.getAbsolutePath());
        if (!inputFile.exists()) {
            throw new RuntimeException("Input file does not exist");
        }
        if (!inputFile.exists() && inputFile.isDirectory()) {
            throw new RuntimeException("Input file is a directory");
        }
        try {
            ArrayList<ProteinConverter> proteins;
            if(predictionType == Parameters.PredictionType.epidope) {
                proteins = CSVConverter.getProteinFromEpidopeCSV(inputFile);
            }else{
                proteins = CSVConverter.getProteinFromCSV(inputFile);
            }
            String fasta = EpitopeFinder.generateReportFastaProteins(proteins);
            FileWriter fw = new FileWriter(outputFile);
            fw.write(fasta);
            fw.close();
            System.out.println("FASTA file created successfully on " + outputFile.getAbsolutePath());

        }catch (Exception e){
            throw e;
        }

        return 0;
    }
}
