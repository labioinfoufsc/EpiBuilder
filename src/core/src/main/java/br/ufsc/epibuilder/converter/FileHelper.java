/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.converter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 *
 * @author renato
 */
public class FileHelper {
    public static String readFile(File file) throws FileNotFoundException {
        StringBuilder sb = new StringBuilder();
        Scanner s = new Scanner(file);
        while (s.hasNext()) {
            sb.append(s.nextLine());
            sb.append("\n");
        }
        return sb.toString();
    }
}
