
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import br.ufsc.epibuilder.converter.ProteinCSVReader;
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

    @Option(names = { "-i", "--input" }, required = true, description = "Input file (fasta or csv)")
    File input;

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
            "--output" }, description = "The common base name for the output generated files. Default: ${DEFAULT-VALUE}", defaultValue = "")
    String outputFolder;

    @Option(names = { "--identity" }, description = "Minimum identity cutoff. Default: ${DEFAULT-VALUE}", defaultValue = "90")
    Integer blastIdentity;

    @Option(names = { "--cover" }, description = "Minimum cover cutoff. Default: ${DEFAULT-VALUE}", defaultValue = "90")
    Integer blastCover;

    @Option(names = { "--word-size" }, description = "Word-size. Default: ${DEFAULT-VALUE}", defaultValue = "4")
    Integer blastWordsize;

    @Option(names = { "--proteomes" }, required = false, description = "Input proteome files format (separated by :) <alias1>=<fasta1>:<alias2>=<fasta2>\nUse this option to search in one or more proteomes. ")
    String proteomes;

    @Option(names = { "-loc", "--localization" }, required = false, description = "${COMPLETION-CANDIDATES}")
    Parameters.LocalizationType localizationType;

    @Option(names = { "-loc_file", "--localization_file" }, required = false, description = "Input localization files id<tab>localization")
    File localizationFile;

    @Option(names = { "-desc_file", "--description_file" }, required = false, description = "Input description files id<tab>description")
    File descriptionFile;

    @Override
    public Integer call() throws IOException {
        Parameters.INPUT = input;

        Parameters.THRESHOLD_BEPIPRED = threshold;
        Parameters.MIN_LENGTH_BEPIPRED = minLength;
        Parameters.MAX_LENGTH_BEPIPRED = maxLength;

        Parameters.MAP_SOFTWARES.put(SoftwareBcellEnum.EMINI, null);
        Parameters.MAP_SOFTWARES.put(SoftwareBcellEnum.KOLASKAR, null);
        Parameters.MAP_SOFTWARES.put(SoftwareBcellEnum.CHOU_FOSMAN, null);
        Parameters.MAP_SOFTWARES.put(SoftwareBcellEnum.KARPLUS_SCHULZ, null);
        Parameters.MAP_SOFTWARES.put(SoftwareBcellEnum.PARKER, null);

        Path p1 = Paths.get(outputFolder);
        Path destinationFolder = Files.createDirectories(p1);

        Parameters.DESTINATION_FOLDER = destinationFolder.toAbsolutePath().toString();

        if (StringUtils.isNotEmpty(proteomes)) {
            Parameters.SEARCH_BLAST = true;
            Parameters.BLAST_IDENTITY = blastIdentity;
            Parameters.BLAST_COVER = blastCover;
            Parameters.BLAST_WORD_SIZE = blastWordsize;

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
        Parameters.LOCALIZATION_TYPE = localizationType;

        if (localizationFile != null && localizationFile.exists()) {
//            Path destLocalization = destinationFolder.resolve("localization.tsv");
//            Files.copy(localizationFile.toPath(), destLocalization, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
//            File localization = destLocalization.toFile();
//            Parameters.MAP_PROTEIN_LOCALIZATION = ProteinCSVReader.readTsvToMap(destLocalization.toFile());
            Parameters.MAP_PROTEIN_LOCALIZATION = ProteinCSVReader.readTsvToMap(localizationFile);
        }
        if (descriptionFile != null && descriptionFile.exists()) {
//            Path destDescription = destinationFolder.resolve("description.tsv");
//            Files.copy(descriptionFile.toPath(), destDescription, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
//            File description = destDescription.toFile();
//            Parameters.MAP_PROTEIN_DESCRIPTION = ProteinCSVReader.readTsvToMap(description);
            Parameters.MAP_PROTEIN_DESCRIPTION = ProteinCSVReader.readTsvToMap(descriptionFile);
        }
        EpitopeFinder.process();
        return 0;
    }

    private void addProteome(ArrayList<Proteome> proteomes, String proteomeFile, String alias, int i) {
        if (proteomeFile != null && !proteomeFile.trim().equals("")) {
            File f = new File(proteomeFile);
            if (f.exists() ) {
                if (alias.trim().equals("")) {
                    alias = "proteome" + i;
                }
                try {
                    proteomes.add(new Proteome(alias, f));
                } catch (IOException e) {
                    System.out.println("Error processing proteome " + proteomeFile);
                    System.out.println("Please check your inputs and --proteomes parameter format");
                    System.exit(1);
                }
            }else{
                System.out.println(proteomeFile+" does not exist - ignoring");
            }
        }
    }

    public static void main(String... args) {
        System.out.println("EpiBuilder - Executing");
        System.out.println("Arguments");

        System.out.println("Execution started");
        /**args =  new String[]{
                "--input", "/bioinformatic/labioinfoufsc/EpiBuilder/src/core/raw_output.csv",
                "-f", "csv",
                "-o", "/bioinformatic/labioinfoufsc/EpiBuilder/src/core/teste12456",
                "--proteomes", "p1=/bioinformatic/db/uniprot_sprot123.fasta",
                "-loc", "animal"
        };**/
        System.exit(new CommandLine(new Main()).execute(args));

    }
}
