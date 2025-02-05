package com.auto.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@Service
public class ExcelService {

    @Value("${file.path}")
    private String excelFilePath;

    public Map<String, List<Map<String, Object>>> readExcelData() throws IOException {
        File file = new File(excelFilePath);
        if (!file.exists()) {
            throw new IOException("Excel 파일이 존재하지 않습니다: " + file.getAbsolutePath());
        }

        FileInputStream fis = new FileInputStream(file);
        Workbook workbook = new XSSFWorkbook(fis);
        Map<String, List<Map<String, Object>>> excelData = new HashMap<>();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            List<Map<String, Object>> sheetData = new ArrayList<>();

            Row headerRow = sheet.getRow(0); // 첫 번째 행(헤더)
            if (headerRow == null) continue;

            int colCount = headerRow.getPhysicalNumberOfCells();
            List<String> headers = new ArrayList<>();

            // 헤더 읽기
            for (int j = 0; j < colCount; j++) {
                headers.add(headerRow.getCell(j).getStringCellValue().trim());
            }

            int lastRow = getLastDataRow(sheet); // 실제 데이터가 있는 마지막 행 찾기

            // 데이터 읽기
            for (int j = 1; j <= lastRow; j++) { // 두 번째 행부터 실제 데이터가 있는 마지막 행까지 반복
                Row row = sheet.getRow(j);
                if (row == null || isRowEmpty(row)) continue;

                Map<String, Object> rowData = new HashMap<>();
                for (int k = 0; k < colCount; k++) {
                    Cell cell = row.getCell(k, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowData.put(headers.get(k), getCellValue(cell));
                }
                sheetData.add(rowData);
            }

            excelData.put("grid" + (i + 1) + "Data", sheetData);
        }

        workbook.close();
        return excelData;
    }

    /**
     * 실제 데이터가 있는 마지막 행 번호를 찾는 메서드
     */
    private int getLastDataRow(Sheet sheet) {
        int lastRow = sheet.getLastRowNum();
        while (lastRow > 0) {
            Row row = sheet.getRow(lastRow);
            if (row != null && !isRowEmpty(row)) {
                break;
            }
            lastRow--;
        }
        return lastRow;
    }

    /**
     * 해당 Row가 완전히 비어있는지 확인하는 메서드
     */
    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            if (cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private Object getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
