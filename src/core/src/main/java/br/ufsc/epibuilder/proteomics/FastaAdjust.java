/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.proteomics;

import br.ufsc.epibuilder.converter.ProteinConverter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author renato
 */
public class FastaAdjust {

    public static ArrayList<ProteinConverter> getProteins(File file) throws FileNotFoundException {
        ArrayList<ProteinConverter> proteins = new ArrayList<>();
        Scanner s = new Scanner(file);

        ProteinConverter proteina = null;
        while (s.hasNext()) {
            String ln = s.nextLine();
            if (ln.startsWith(">")) {
                proteina = new ProteinConverter(ln.substring(1, ln.indexOf(" ")));
                proteins.add(proteina);
            } else {
                proteina.append(ln);
            }
        }
        return proteins;
    }

}
