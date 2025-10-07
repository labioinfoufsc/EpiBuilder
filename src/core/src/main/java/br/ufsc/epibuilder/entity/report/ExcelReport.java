/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.entity.report;

import java.io.FileOutputStream;
import java.util.ArrayList;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author renato
 */
public class ExcelReport {

    /* public static void generateExcel(ArrayList<ExcelTabReport> tabs, String fileName) throws Exception {
        
        HSSFWorkbook workbook = new HSSFWorkbook();
        for (ExcelTabReport tab : tabs) {
            HSSFSheet sheet = workbook.createSheet(tab.getName());
            Object[][] data = tab.getMatrix();
            int rowCount = 0;
            for (Object[] line : data) {
                Row row = sheet.createRow(rowCount++);
                int columnCount = 0;
                for (Object field : line) {
                    Cell cell = row.createCell(columnCount++);
                    if (field instanceof String) {
                        cell.setCellValue((String) field);
                    } else if (field instanceof Double) {
                        cell.setCellValue((Double) field);
                    } 
                }
            }
        }
        try ( FileOutputStream outputStream = new FileOutputStream(fileName)) {
            workbook.write(outputStream);
        }

    }*/
    public static void generateExcelXlsx(ArrayList<ExcelTabReport> tabs, String fileName) throws Exception {

        XSSFWorkbook workbook = new XSSFWorkbook();
        CellStyle style = workbook.createCellStyle(); // Creating Style  
        // Creating Font and settings  
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        font.setFontName("Courier New");
        // Applying font to the style  
        style.setFont(font);
        // Applying style to the cell  

        for (ExcelTabReport tab : tabs) {
            XSSFSheet sheet = workbook.createSheet(tab.getName());
            Object[][] data = tab.getMatrix();
            int rowCount = 0;
            for (Object[] line : data) {
                Row row = sheet.createRow(rowCount++);
                int columnCount = 0;
                for (Object field : line) {
                    Cell cell = row.createCell(columnCount++);
                    if (field instanceof String) {
                        cell.setCellValue((String) field);
                    } else if (field instanceof Double) {
                        cell.setCellValue((Double) field);
                    }
                    cell.setCellStyle(style);
                }
            }
        }
        try ( FileOutputStream outputStream = new FileOutputStream(fileName)) {
            workbook.write(outputStream);
        }

    }

    public static void main(String[] args) throws Exception {
        ArrayList<ExcelTabReport> report = new ArrayList<>();

        report.add(new ExcelTabReport("tab1", "A\t2\t3\n11\t22\t33"));
        report.add(new ExcelTabReport("tab2", "B\t5\t6\n41\t51\t61"));
        report.add(new ExcelTabReport("tab3", "C\t8\t9\n71\t81\t91"));
        generateExcelXlsx(report, "teste.xlsx");

    }

}
