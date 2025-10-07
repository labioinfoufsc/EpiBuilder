
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;

import br.ufsc.epibuilder.EpitopeFinder;
import br.ufsc.epibuilder.Parameters;
import br.ufsc.epibuilder.entity.Proteome;
import br.ufsc.epibuilder.entity.SoftwareBcellEnum;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "EpiBuilder-2.0", requiredOptionMarker = '*', abbreviateSynopsis = true, description = "A Tool for Assembling, Searching, and Classifying B-Cell Epitopes", version = "2.0", sortOptions = false)
public class Main implements Callable<Integer> {

    @Option(names = { "-i", "--input" }, required = true, description = "Input file")
    File input;
    @Option(names = { "-f",
            "--format" }, required = true, description = "Input file type: ${COMPLETION-CANDIDATES} \ncsv - BepiPred-3.0 generated file (default)"
                    +
                    "\nfasta - FASTA file, use this option only in Epibuilder customized Docker", defaultValue = "csv")
    FileType type;
    @Option(names = { "-min",
            "--min-length" }, description = "Minimum epitope length. Default: ${DEFAULT-VALUE}", defaultValue = "10")
    Integer minLength;
    @Option(names = { "-max",
            "--max-length" }, description = "Max epitope length. Default: ${DEFAULT-VALUE}", defaultValue = "30")
    Integer maxLength;
    @Option(names = { "-t",
            "--threshold" }, description = "Threshold default: ${DEFAULT-VALUE}", defaultValue = "0.1512")
    Double threshold;

    @Option(names = { "-o",
            "--output" }, description = "The common base name for the output generated files. Default: ${DEFAULT-VALUE}", defaultValue = "epibuilder-results")
    String basename;
    @Option(names = { "-search",
            "--search" }, description = "Method of search in the given proteome(s): ${COMPLETION-CANDIDATES}. " +
                    "\nblast - perform search with blast (at least one proteome is mandatory, use option -p1 to p6 or -proteomes to set them)"
                    +
                    "\nnone - don't search (default)", defaultValue = "none")
    Search search;
    @Option(names = { "-bi",
            "--ident" }, description = "Minimum identity cutoff. Default: ${DEFAULT-VALUE}", defaultValue = "90")
    Integer blastIdentity;
    @Option(names = { "-bc",
            "--cover" }, description = "Minimum cover cutoff. Default: ${DEFAULT-VALUE}", defaultValue = "90")
    Integer blastCover;
    @Option(names = { "-ws", "--word-size" }, description = "Word-size. Default: ${DEFAULT-VALUE}", defaultValue = "4")
    Integer blastWordsize;
    @Option(names = { "-p1", "--proteome1" }, description = "Proteome 1 file", defaultValue = "null")
    String proteome1;
    @Option(names = { "-p1a",
            "--proteome1-alias" }, description = "Proteome 1 alias - appears in the report file", defaultValue = "proteome1")
    String proteome1Alias;
    @Option(names = { "-p2", "--proteome2" }, description = "Proteome 2 file", defaultValue = "null")
    String proteome2;
    @Option(names = { "-p2a",
            "--proteome2-alias" }, description = "Proteome 2 alias - appears in the report file", defaultValue = "proteome2")
    String proteome2Alias;
    @Option(names = { "-p3", "--proteome3" }, description = "Proteome 3 file", defaultValue = "null")
    String proteome3;
    @Option(names = { "-p3a",
            "--proteome3-alias" }, description = "Proteome 3 alias - appears in the report file", defaultValue = "proteome3")
    String proteome3Alias;
    @Option(names = { "-p4", "--proteome4" }, description = "Proteome 4 file", defaultValue = "null")
    String proteome4;
    @Option(names = { "-p4a",
            "--proteome4-alias" }, description = "Proteome 4 alias - appears in the report file", defaultValue = "proteome4")
    String proteome4Alias;
    @Option(names = { "-p5", "--proteome5" }, description = "Proteome 5 file", defaultValue = "null")
    String proteome5;
    @Option(names = { "-p5a",
            "--proteome5-alias" }, description = "Proteome 5 alias - appears in the report file", defaultValue = "proteome5")
    String proteome5Alias;
    @Option(names = { "-p6", "--proteome6" }, description = "Proteome 6 file", defaultValue = "null")
    String proteome6;
    @Option(names = { "-p6a",
            "--proteome6-alias" }, description = "Proteome 6 alias - appears in the report file", defaultValue = "proteome6")
    String proteome6Alias;

    @Option(names = { "-proteomes",
            "--proteomes" }, required = false, description = "Input proteome files format (separated by :) <alias1>=<fasta1>:<alias2>=<fasta2>\nUse this option to search in one or more proteomes. This option can be used with the p1-p6 option.")
    String proteomes;

    @Override
    public Integer call() throws IOException {
        Parameters.FASTA = input;
        Parameters.BEPIPRED_FILE = input;
        if (type == FileType.fasta) {
            Parameters.BEPIPRED_INPUT = Parameters.BEPIPRED_TYPE.FASTA;
        } else {
            Parameters.BEPIPRED_INPUT = Parameters.BEPIPRED_TYPE.CSV;
        }

        Parameters.THRESHOLD_BEPIPRED = threshold;
        Parameters.MIN_LENGTH_BEPIPRED = minLength;
        Parameters.MAX_LENGTH_BEPIPRED = maxLength;

        Parameters.MAP_SOFTWARES.put(SoftwareBcellEnum.EMINI, null);
        Parameters.MAP_SOFTWARES.put(SoftwareBcellEnum.KOLASKAR, null);
        Parameters.MAP_SOFTWARES.put(SoftwareBcellEnum.CHOU_FOSMAN, null);
        Parameters.MAP_SOFTWARES.put(SoftwareBcellEnum.KARPLUS_SCHULZ, null);
        Parameters.MAP_SOFTWARES.put(SoftwareBcellEnum.PARKER, null);

        Parameters.BASENAME = basename;
        Path p1 = Paths.get(Parameters.BASENAME);
        Files.createDirectories(p1);

        Parameters.BASENAME += "/" + Parameters.BASENAME;

        if (search != Search.none) {
            if (search == Search.blast) {
                Parameters.SEARCH_BLAST = true;
                Parameters.BLAST_IDENTITY = blastIdentity;
                Parameters.BLAST_COVER = blastCover;
                Parameters.BLAST_WORD_SIZE = blastWordsize;
            }
            ArrayList<Proteome> proteomeFiles = new ArrayList<>();

            addProteome(proteomeFiles, proteome1, proteome1Alias, 1);
            addProteome(proteomeFiles, proteome2, proteome2Alias, 2);
            addProteome(proteomeFiles, proteome3, proteome3Alias, 3);
            addProteome(proteomeFiles, proteome4, proteome4Alias, 4);
            addProteome(proteomeFiles, proteome5, proteome5Alias, 5);
            addProteome(proteomeFiles, proteome6, proteome6Alias, 6);
            int totalProt = proteomeFiles.size();

            if (!StringUtils.isBlank(proteomes)) {
                String[] proteomas = proteomes.split(":");
                System.out.println(proteomes.toString());
                for (String proteoma : proteomas) {
                    proteoma = proteoma.trim();
                    String[] st = proteoma.split("=");
                    addProteome(proteomeFiles, st[1], st[0].trim(), ++totalProt);
                }
            }
            if (proteomeFiles.isEmpty()) {
                System.out.println("ERROR: Choose at least one proteome to perform the search");
                System.exit(0);
            }
            Parameters.PROTEOMES = proteomeFiles;
        }
        Parameters.OUTPUT_FILE = false;
        EpitopeFinder.process();
        return 0;
    }

    private void addProteome(ArrayList<Proteome> proteomes, String proteomeFile, String alias, int i) {
        if (proteomeFile != null && !proteomeFile.trim().equals("")) {
            File f = new File(proteomeFile);
            if (f.exists()) {
                if (alias.trim().equals("")) {
                    alias = "proteome" + i;
                }
                proteomes.add(new Proteome(alias, f));
            }
        }
    }

    public static void main(String... args) {
        System.out.println("EpiBuilder - Executing");
        System.out.println("Arguments");

        System.out.println("Execution started");
        System.exit(new CommandLine(new Main()).execute(args));

    }

    private enum Search {
        blast,
        none
    }

    public enum FileType {
        csv, fasta
    }
}
