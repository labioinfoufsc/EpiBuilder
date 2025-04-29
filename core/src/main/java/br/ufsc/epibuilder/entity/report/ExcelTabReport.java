/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.entity.report;

/**
 *
 * @author renato
 */
public class ExcelTabReport {

    private String name;
    private String content;

    public ExcelTabReport(String name, String content) {
        this.name = name;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object[][] getMatrix() {
        String[] lines = content.split("\n");
        Object[][] matrix = new Object[lines.length][lines[0].split("\t").length];
        matrix[0]=lines[0].split("\t");
        
        for (int i = 1; i < lines.length; i++) {
            String[] cols = lines[i].split("\t");
            for (int j = 0; j < cols.length; j++) {
                try {
                    matrix[i][j] = Double.parseDouble(cols[j]);
                } catch (Exception e) {
                    matrix[i][j] = cols[j];
                }
            }
        }
        return matrix;
    }
}
