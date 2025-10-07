/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder;

import java.io.*;

/**
 *
 * @author renato
 */
public class CommandRunner {

    public static void run(String command) throws IOException, InterruptedException {
        String s;
        System.out.println("Executing: " + command);
        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();
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
            FileWriter fw = new FileWriter(new File("error.txt"));
            fw.append(stError);
            fw.flush();
            fw.close();
//            System.out.println("Error:");
//            System.err.println(stError);
        }
    }
}
