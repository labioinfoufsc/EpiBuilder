/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.converter;

import br.ufsc.epibuilder.exceptions.InputException;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author renato
 */
public class BepiPred3Converter {

    /**
     * Convert a Bepipred3 file obtained from http://biolab.com/DTU/BepiPred-3
     *
     * @param csvFile
     * @return
     * @throws FileNotFoundException
     * @throws InputException
     */
    public static ArrayList<ProteinConverter> getBepipred3FromBiolib(File csvFile) throws FileNotFoundException, InputException {
        Scanner s = new Scanner(csvFile);
        ArrayList<ProteinConverter> proteins = new ArrayList<>();
        ProteinConverter protein = new ProteinConverter("");
        s.nextLine();
        while (s.hasNext()) {
            try {
                String val = s.nextLine();

                String[] fields = val.split(",");
                String id = fields[0];
                String aa = fields[1].toUpperCase();
                double score = Double.parseDouble(fields[2]);

                if (!protein.getId().equals(id)) {
                    protein = new ProteinConverter(id);
                    proteins.add(protein);
                }
                protein.addAmino(aa, score);

            } catch (Exception e) {
                e.printStackTrace();
                throw new InputException(e);
            }

        }
        return proteins;
    }

    public static void main(String[] args) throws Exception, InputException {
        File f = new File("/bioinformatic/epibuilder2/raw_output.csv");
        ArrayList<ProteinConverter> proteins = getBepipred3FromBiolib(f);
        for (ProteinConverter protein : proteins) {
            System.out.println(">" + protein.getId());
            System.out.println(protein.getSequenceFromMap());
        }
        System.out.println("Teste");
    }
}
