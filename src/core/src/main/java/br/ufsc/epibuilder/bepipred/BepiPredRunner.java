package br.ufsc.epibuilder.bepipred;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class BepiPredRunner {

    public static File runBepipred(File fastaFile) throws IOException, InterruptedException {
        if (!fastaFile.exists()) {
            throw new IllegalArgumentException("FASTA file not found: " + fastaFile.getAbsolutePath());
        }

        ProcessBuilder builder = new ProcessBuilder("bepipred3", fastaFile.getAbsolutePath());

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
            throw new RuntimeException("Error executing bepipred3. Exit code: " + exitCode);
        }else{
            System.out.println("BepiPred-3.0 -  Finished");
            File bepipredOutput = new File(fastaFile.getParent()+"/raw_output.csv");
            if(bepipredOutput.exists()){
                System.out.println("File saved at: "  + bepipredOutput.getAbsolutePath());

                return bepipredOutput;
            }
        }
        throw new RuntimeException("Error executing bepipred3. Exit code: " + exitCode);
    }
}
