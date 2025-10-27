/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.blast;

import br.ufsc.epibuilder.Parameters;
import br.ufsc.epibuilder.converter.FileHelper;
import br.ufsc.epibuilder.entity.Proteome;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author renato
 */
public class BlastRunner {

    public static void runCommand(String command) throws IOException {
        String s;
        Process p = Runtime.getRuntime().exec(command);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

        boolean success = false;
        String stSuccess = "";
        while ((s = stdInput.readLine()) != null) {
            stSuccess += s;
            stSuccess += "\n";
            success = true;
        }

        boolean error = false;
        String stError = "";

        while ((s = stdError.readLine()) != null) {
            stError += s;
            stError += "\n";
            error = true;
        }

        if (success) {
            System.out.println("Success:");
            System.out.println(stSuccess);
        }
        if (error) {
            System.out.println("Error:");
            System.out.println(stError);
        }
    }

    public static void addHeader(File file) throws Exception {
        String res = FileHelper.readFile(file);
        FileWriter fw = new FileWriter(file);
        fw.write("qacc\tsacc\tpident\tqcovs\tqseq\tsseq\n" + res);
        fw.close();
    }

    public static File getBlastResults(Proteome proteome, String epiBuilderFastaEpitopesFile) {
        String s = null;
        String db = String.format("%s/%s", Parameters.DESTINATION_FOLDER, proteome.getOrganism());
        String blastOutput = db + "_blast.csv";

        System.out.printf("BLAST in database %s",proteome.getFile().getAbsolutePath());
        db = proteome.getFile().getAbsolutePath();

        String[] cmd = {Parameters.BLASTP_PATH,
                epiBuilderFastaEpitopesFile,
                db,
                Parameters.BLAST_TASK,
                Parameters.BLAST_WORD_SIZE+"",
                Parameters.BLAST_IDENTITY+"",
                Parameters.BLAST_COVER+"",
                blastOutput};

        try {

                System.out.print("Running command[: ");
                for (String string : cmd) {
                    System.out.print(string + " ");
                }
                System.out.println("]");

                Process p = Runtime.getRuntime().exec(cmd);
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
                BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

                boolean success = false;
                String stSuccess = "";
                while ((s = stdInput.readLine()) != null) {
                    stSuccess += s;
                    stSuccess += "\n";
                    success = true;
                }

                boolean error = false;
                String stError = "";

                while ((s = stdError.readLine()) != null) {
                    stError += s;
                    stError += "\n";
                    error = true;
                }

                if (success) {
                    System.out.println("Success:");
                    System.out.println(stSuccess);
                }
                if (error) {
                    System.out.println("Error:");
                    System.out.println(stError);
                }

            File blastfile = new File(blastOutput);

            //addHeader(blastfile);

            return blastfile;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return null;
    }
}
