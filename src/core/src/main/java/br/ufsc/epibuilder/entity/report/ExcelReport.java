/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.epibuilder.entity.report;

import java.io.FileOutputStream;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
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
                        cell.setCellValue(StringUtils.truncate((String) field, 32767));
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
}
