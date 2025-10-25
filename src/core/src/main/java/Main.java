
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import br.ufsc.epibuilder.converter.ProteinDescriptionReader;
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
                    "\nblast - perform search with blast (at least one proteome is mandatory, -proteomes to set them)"
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

    @Option(names = { "-proteomes",
            "--proteomes" }, required = false, description = "Input proteome files format (separated by :) <alias1>=<fasta1>:<alias2>=<fasta2>\nUse this option to search in one or more proteomes. This option can be used with the p1-p6 option.")
    String proteomes;

    @Option(names = { "-d", "--description" }, required = false, description = "Input file with id\tproteins to join with final result")
    File proteinsDescription;

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
        Parameters.MAP_PROTEIN_DESCRIPTION = ProteinDescriptionReader.readTsvToMap(proteinsDescription);
        EpitopeFinder.process();
        return 0;
    }

    private void addProteome(ArrayList<Proteome> proteomes, String proteomeFile, String alias, int i) {
        if (proteomeFile != null && !proteomeFile.trim().equals("")) {
            File f = new File(proteomeFile);
            if (f.exists() || new File(proteomeFile+".phr").exists() ) {
                if (alias.trim().equals("")) {
                    alias = "proteome" + i;
                }
                proteomes.add(new Proteome(alias, f));
            }else{
                System.out.println(proteomeFile+" does not exist - ignoring");
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
