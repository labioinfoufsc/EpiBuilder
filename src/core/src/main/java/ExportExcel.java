import br.ufsc.epibuilder.entity.report.ExcelReport;
import br.ufsc.epibuilder.entity.report.ExcelTabReport;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.Callable;

@Command(
        name = "ExportExcel",
        mixinStandardHelpOptions = true,
        version = "1.0",
        description = "Generate an Excel file combining detailed, protein, and topology reports."
)
public class ExportExcel implements Callable<Integer> {

    @Option(
            names = {"-d", "--detailed"},
            description = "Path to the detailed report file.",
            required = true
    )
    private File reportDetailedFile;

    @Option(
            names = {"-p", "--protein"},
            description = "Path to the report by protein summary.",
            required = true
    )
    private File reportByProteinFile;

    @Option(
            names = {"-t", "--topology"},
            description = "Path to the topology report file.",
            required = true
    )
    private File reportTopologyFile;

    @Option(
            names = {"-o", "--output"},
            description = "Output Excel file path (.xlsx).",
            required = true
    )
    private File excelFile;

    @Override
    public Integer call() throws Exception {
        exportExcel(reportDetailedFile, reportByProteinFile, reportTopologyFile, excelFile);
        System.out.println("✅ Excel file successfully generated: " + excelFile.getAbsolutePath());
        return 0;
    }

    public static void exportExcel(File reportDetailedFile, File reportByProteinFile, File reportTopologyFile, File excelFile) throws Exception {
        ArrayList<ExcelTabReport> excelTab = new ArrayList<>();

        String reportDetailed = Files.readString(reportDetailedFile.toPath());
        String reportByProtein = Files.readString(reportByProteinFile.toPath());
        String reportTopology = Files.readString(reportTopologyFile.toPath());

        excelTab.add(new ExcelTabReport("Epitopes Detailed", reportDetailed));
        excelTab.add(new ExcelTabReport("Protein Summary", reportByProtein));
        excelTab.add(new ExcelTabReport("Epitopes Topology", reportTopology));

        ExcelReport.generateExcelXlsx(excelTab, excelFile.getAbsolutePath());
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ExportExcel()).execute(args);
        System.exit(exitCode);
    }
}
