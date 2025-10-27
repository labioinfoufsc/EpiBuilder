package br.ufsc.epibuilder.localization;

import br.ufsc.epibuilder.Parameters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class LocalizationRunner {
    public static File runLocalization(File fastaFile) throws IOException, InterruptedException {
        if (!fastaFile.exists()) {
            throw new IllegalArgumentException("FASTA file not found: " + fastaFile.getAbsolutePath());
        }

        ProcessBuilder builder = new ProcessBuilder("localization", fastaFile.getAbsolutePath(), Parameters.LOCALIZATION_TYPE.toString());

        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (InputStream is = process.getInputStream();
             Scanner scanner = new Scanner(is)) {
            while (scanner.hasNextLine()) {
                System.out.println(scanner.nextLine());
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Error executing Subcelullar Localization. Exit code: " + exitCode);
        }else{
            System.out.println("Subcelullar Localization -  Finished");
            File output = new File(fastaFile.getParent()+"/localization.tsv");
            if(output.exists()){
                System.out.println("File saved at: "  + output.getAbsolutePath());

                return output;
            }
        }
        throw new RuntimeException("Error executing Subcelullar Localization. Exit code: " + exitCode);
    }
}
