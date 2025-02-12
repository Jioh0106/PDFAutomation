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

    @Value("${file.pos-info-path}")
    private String posFilePath;
    
    @Value("${file.region-info-path}")
    private String regionFilePath;
    
    
    public List<Map<String, String>> readSelectData() throws IOException {
    	File file = new File(regionFilePath);
    	if (!file.exists()) {
            throw new IOException("Excel 파일이 존재하지 않습니다: " + file.getAbsolutePath());
        }
    	
    	List<Map<String, String>> selectList = new ArrayList<>();
    	
    	try (FileInputStream fis = new FileInputStream(file);
                Workbook workbook = new XSSFWorkbook(fis)) {

               Sheet sheet = workbook.getSheetAt(0);
               if (sheet == null) {
                   throw new IOException("엑셀 파일에 시트가 존재하지 않습니다: " + regionFilePath);
               }

               Row headerRow = sheet.getRow(0);
               if (headerRow == null) {
                   throw new IOException("엑셀 파일에 헤더가 없습니다: " + regionFilePath);
               }

               // 헤더 정보 추출
               List<String> headers = new ArrayList<>();
               for (Cell cell : headerRow) {
                   headers.add(cell.getStringCellValue().trim());
               }

               // 실제 데이터가 존재하는 행 찾기
               int lastRow = getLastDataRow(sheet);
               for (int i = 1; i <= lastRow; i++) { 
                   Row row = sheet.getRow(i);
                   if (row == null || isRowEmpty(row)) continue; // 빈 행 건너뛰기

                   Map<String, String> rowData = new HashMap<>();
                   for (int j = 0; j < headers.size(); j++) {
                       Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                       rowData.put(headers.get(j), cell.toString().trim());
                   }
                   selectList.add(rowData);
               }
           }
           return selectList;
    	
    	
	}
    
    
    /*
     * 내부 엑셀 파일 읽기 - 여러 시트가 있는 엑셀 파일
     */
    public Map<String, List<Map<String, Object>>> readExcelDataALL(String type) throws IOException {
    	String filePath = posFilePath;
    	
    	if(type.equals("region")) {
    		filePath = regionFilePath;
    	}
    	
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Excel 파일이 존재하지 않습니다: " + file.getAbsolutePath());
        }

        FileInputStream fis = new FileInputStream(file);
        Workbook workbook = new XSSFWorkbook(fis);
        Map<String, List<Map<String, Object>>> excelData = new HashMap<>();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            String sheetName = sheet.getSheetName();
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

            excelData.put(sheetName, sheetData);
        }

        workbook.close();
        return excelData;
    }
    
    /*
     * 복호화 된 엑셀 파일 읽기 (한 개의 시트 / 모든 값 String으로 저장) 
     */
    public List<Map<String, String>> readExcelData(String filePath) throws IOException {
        File file = new File(filePath.replace("\\", "/"));
        if (!file.exists()) {
            throw new IOException("파일이 존재하지 않습니다: " + filePath);
        }

        List<Map<String, String>> dataList = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IOException("엑셀 파일에 시트가 존재하지 않습니다: " + filePath);
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("엑셀 파일에 헤더가 없습니다: " + filePath);
            }

            // 헤더 정보 추출
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue().trim());
            }

            // 실제 데이터가 존재하는 행 찾기
            int lastRow = getLastDataRow(sheet);
            for (int i = 1; i <= lastRow; i++) { 
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue; // 빈 행 건너뛰기

                Map<String, String> rowData = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowData.put(headers.get(j), cell.toString().trim());
                }
                dataList.add(rowData);
            }
        }
        return dataList;
	}
    
    /*
     * 실제 데이터가 있는 마지막 행 번호를 찾는 메서드
     */
    public int getLastDataRow(Sheet sheet) {
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

    /*
     * 해당 Row가 완전히 비어있는지 확인하는 메서드
     */
    public boolean isRowEmpty(Row row) {
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

    public Object getCellValue(Cell cell) {
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
