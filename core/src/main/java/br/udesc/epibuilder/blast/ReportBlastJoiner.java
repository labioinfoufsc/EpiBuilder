/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.udesc.epibuilder.blast;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

/**
 *
 * @author renato
 */
public class ReportBlastJoiner {
    
    public static String joinReport(String epibuilderDetailFile, String blastFile, String genome) {
        String epiDetailTxt = (epibuilderDetailFile);
        String blastTxt = (blastFile);
        String[] blastLines = blastTxt.split("\n");
        HashMap<String, String[]> mapBlast = new HashMap<>();
        for (int i = 1; i < blastLines.length; i++) {
            String blastLine = blastLines[i];
            String[] blastln = blastLine.split("\t");
            mapBlast.put(blastln[0].trim(), blastln);
        }
        
        StringBuilder sb = new StringBuilder();
        
        String[] epibuilderDetailRes = epiDetailTxt.split("\n");
        sb.append(String.format("%s\tblast_%s_%s\tblast_%s_%s\n",epibuilderDetailRes[0], genome, "count", genome, "acc"));
        for (int i = 1; i < epibuilderDetailRes.length; i++) {
            String epibuilderDetailRe = epibuilderDetailRes[i];
            
            String[] epiLine = epibuilderDetailRe.split("\t");
            String id = String.format("%s-%s", epiLine[0].trim(), epiLine[2].trim());
            
            String[] blastAux = mapBlast.get(id);
            if(blastAux==null){
                blastAux = new String[]{"-", "-", "-"};
            }
            
            sb.append(String.format("%s\t%s\t%s", epibuilderDetailRe, blastAux[1], blastAux[2]));
            
            sb.append("\n");
        }
        
        return  sb.toString();
    }
    public static String joinReport(File epibuilderDetailFile, File blastFile, String genome) throws FileNotFoundException{
        String epiDetailTxt = readFile(epibuilderDetailFile);
        String blastTxt = readFile(blastFile);
        
        return  joinReport(epiDetailTxt, blastTxt, genome);
    }
    public static String readFile(File file) throws FileNotFoundException{
        StringBuilder sb = new StringBuilder();
        Scanner s = new Scanner(file);
        while(s.hasNext()){
            sb.append(s.nextLine());
            sb.append("\n");
        }
        return sb.toString();
    }
}
