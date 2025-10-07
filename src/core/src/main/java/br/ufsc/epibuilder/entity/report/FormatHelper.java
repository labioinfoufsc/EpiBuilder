/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.entity.report;

import com.google.common.base.Joiner;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author renato
 */
public class FormatHelper {
 
    public static String getListAsString(String delim, List list){
        return Joiner.on(delim).join(list);
    }
    
    public static String convertTabString(String res) {
        String[] lines = res.split("\n");
        int[] maxLengthColumn = new int[lines[0].split("\t").length];
        for (String line : lines) {
            String[] cols = line.split("\t");
            for (int i = 0; i < cols.length; i++) {
                if (cols[i].length() >= maxLengthColumn[i]) {
                    maxLengthColumn[i] = cols[i].length();
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String[] cols = line.split("\t");
            for (int i = 0; i < cols.length; i++) {
                String col = cols[i];
                sb.append(StringUtils.rightPad(col, maxLengthColumn[i], ' '));
                sb.append("\t");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
