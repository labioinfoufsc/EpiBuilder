import br.ufsc.epibuilder.EpitopeFinder;
import br.ufsc.epibuilder.converter.BepiPred3Converter;
import br.ufsc.epibuilder.converter.ProteinConverter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.concurrent.Callable;


@Command(name = "FastaFromBepiPredCSV", mixinStandardHelpOptions = true,
        description = "Converts a BepiPred CSV file into a FASTA file")
public class FastaFromBepiPredCSV implements Callable<Integer> {

    @Option(names = "--input", required = true, description = "Input CSV file path")
    private File inputFile;

    @Option(names = "--output", required = true, description = "Output FASTA file path")
    private File outputFile;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new FastaFromBepiPredCSV()).execute(args);
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
            ArrayList<ProteinConverter> proteins = BepiPred3Converter.getBepipred3FromBiolib(inputFile);
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
