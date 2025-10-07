/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.udesc.epibuilder.blast;

import br.ufsc.epibuilder.Parameters;
import br.ufsc.epibuilder.converter.FileHelper;
import br.ufsc.epibuilder.entity.Proteome;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

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

    public static void chmodBlast(Parameters.SO so) {
        System.out.println("Giving permission to execution to blastp and makeblastdb");
        try {
            if (so == Parameters.SO.linux || so == Parameters.SO.macos) {
                runCommand("chmod +x " + Parameters.BLASTP_PATH);
                runCommand("chmod +x " + Parameters.MAKEBLASTDB_PATH);
            }
        } catch (Exception e) {
//            e.printStackTrace();
            System.out.println("Error giving permission: "+ e.getMessage());
        }
    }

    public static void addHeader(File file) throws Exception {
        String res = FileHelper.readFile(file);
        FileWriter fw = new FileWriter(file);
        fw.write("qacc\tsacc\tpident\tqcovs\tqseq\tsseq\tqacc\n" + res);
        fw.close();
    }

    public static File getBlastResults(Proteome proteome, String epiBuilderFastaEpitopesFile) {
        String s = null;
        String db = String.format("%s/%s-epibuilder-blast-%s", Parameters.DESTINATION_FOLDER, Parameters.BASENAME, proteome.getOrganism());
        String blastOutput = db + "_blast.csv";

        String[] makeblast = {Parameters.MAKEBLASTDB_PATH,
            "-dbtype", "prot",
            "-in", proteome.getFile().getAbsolutePath(),
            "-out", db};

        String[] blastp = {Parameters.BLASTP_PATH,
            "-query", epiBuilderFastaEpitopesFile,
            "-db", db,
            "-outfmt", "6 qacc sacc pident qcovs qseq sseq qacc",
            "-task", Parameters.BLAST_TASK,
            "-word_size", Parameters.BLAST_WORD_SIZE + "",
            "-out", blastOutput};

        ArrayList<String[]> cmds = new ArrayList<>();
        cmds.add(makeblast);
        cmds.add(blastp);
        try {
            for (String[] cmd : cmds) {
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

            }
            File blastfile = new File(blastOutput);
            addHeader(blastfile);

            return blastfile;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return null;
    }
}
