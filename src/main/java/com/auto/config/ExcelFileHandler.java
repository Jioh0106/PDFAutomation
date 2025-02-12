package com.auto.config;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * gradle import APACHE_POI : implementation 'org.apache.poi:poi-ooxml:5.2.3'
 */
public class ExcelFileHandler {
	
    @Value("${file.pos-info-path}")
    private String filePath;
    
    @Value("${file.upload-dir}")
    private String uploadFilePath;

    /**
     * Excel 파일 읽어서 첫번째 sheet 읽기
     * @param excelFilePath ex) "C:\\Users\\happy\\Desktop\\xxx\\test.xlsx"
     * @return
     */
    public List<List<String>> readExcelFile(String excelFilePath) {
        return this.getSheetData(this.getSheetByNumber(this.readExcelFileWorkBook(excelFilePath), 0));
    }

    /**
     * Excel 파일 읽기
     * @param excelFilePath ex) "C:\\Users\\happy\\Desktop\\xxx\\test.xlsx"
     * @return
     */
    public XSSFWorkbook readExcelFileWorkBook(String excelFilePath) {
        XSSFWorkbook workbook = null;
        try {
            FileInputStream fis = new FileInputStream(new File(excelFilePath));
            workbook = new XSSFWorkbook(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("엑셀 읽기 성공:" + workbook);
        return workbook;
    }

    /**
     * workbook에서 sheet 찾기
     * @param workbook
     * @param sheetName sheet 이름
     * @return
     */
    public XSSFSheet getSheetByName(XSSFWorkbook workbook, String sheetName){
        return workbook.getSheet(sheetName);
    }

    /**
     * workbook에서 sheet 찾기
     * @param workbook
     * @param sheetNumber sheet 번호(0부터 시작)
     * @return
     */
    public XSSFSheet getSheetByNumber(XSSFWorkbook workbook, int sheetNumber){
        return workbook.getSheetAt(sheetNumber);
    }

    /**
     * sheet data 파싱
     * @param sheet
     * @return
     */
    public List<List<String>> getSheetData(XSSFSheet sheet) {
        List<List<String>> dataList = new ArrayList<>();
        // 모든 행을 반복합니다.
        for (Row row : sheet) {
            List<String> rowData = new ArrayList<>();
            // 각 행의 모든 셀을 반복합니다.
            for (Cell cell : row) {
                // 셀 타입에 따라 값을 가져옵니다.
                //String value = getCellValue(cell);
                String value = cell.getStringCellValue();
                if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    value = value.replaceAll("-", "");
                }
                rowData.add(value);
            }
            dataList.add(rowData);
        }
        System.out.println("엑셀 읽기 성공:" + dataList.size() + "건");
        return dataList;
    }

    /**
     * 엑셀 파일 생성 (byte Array)
     * @param sheetName
     * @param headers
     * @param dataList
     * @return
     */
    public byte[] createExcelFileBytes(String sheetName, List<String> headers, List<List<String>> dataList) {
        //Excel Sheet 생성
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);

        //Setting Headers
        this.setOutputHeader(sheet, headers);
        //Setting data
        this.setOutputData(sheet, dataList);
        //create excelFileBytes
        return this.createByteFile(workbook);
    }

    /**
     * 헤더 세팅(첫째줄 컬럼)
     * @param sheet
     * @param headers
     */
    private void setOutputHeader(Sheet sheet, List<String> headers){
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
        }
    }

    /**
     * 데이터 세팅(둘째줄 부터)
     * @param sheet
     * @param dataList
     */
    private void setOutputData(Sheet sheet, List<List<String>> dataList){
        int colCount = dataList.get(0).size();
        for (int i = 0; i < dataList.size(); i++) {
            Row row = sheet.createRow(i + 1);
            List<String> data = dataList.get(i);
            for (int j = 0; j < colCount; j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(data.get(j));
            }
        }
    }

    /**
     * 엑셀 파일 byte[] 생성
     * @param workbook
     * @return
     */
    private byte[] createByteFile(Workbook workbook){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();
        } catch (IOException e) {
            System.out.println("엑셀 파일 생성 실패");
            e.printStackTrace();
        }

        return outputStream.toByteArray();
    }

    /**
     * 엑셀 파일 저장
     * @param excelFile byte Array
     * @param fileDir ex) "C:\\Users\\happy\\Desktop\\xxx\\"
     * @param fileName ex) "test.xlsx"
     */
    public void saveFile(byte[] excelFile, String fileDir, String fileName){
        try {
            // 파일 저장
            FileOutputStream fos = new FileOutputStream(fileDir + fileName);
            fos.write(excelFile);
            fos.close();
            System.out.println("엑셀 파일 저장 성공. path=" + fileDir + fileName);
        } catch (IOException e) {
            System.out.println("엑셀 파일 저장 실패");
            e.printStackTrace();
        }
    }

    /**
     * cell의 값을 파싱
     * @param cell
     * @return
     */
    private String getCellValue(Cell cell) {
        String value = "";
        switch (cell.getCellType()) {
            case STRING:
                value = cell.getStringCellValue();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    value = cell.getDateCellValue().toString();
                    System.out.println(value);
                } else {
                    value = Double.toString(cell.getNumericCellValue());
                }
                break;
            case BOOLEAN:
                value = Boolean.toString(cell.getBooleanCellValue());
                break;
            case FORMULA:
                value = cell.getCellFormula();
                break;
            default:
                value = "";
        }
        return value;
    }
}