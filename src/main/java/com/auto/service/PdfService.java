package com.auto.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PdfService {
	
	@Value("${file.upload-dir}") // 업로드된 엑셀 저장 경로
    private String uploadDir;

    @Value("${file.path}") // field_pos.xlsx 경로
    private String fieldPosPath;

    @Value("${image.source-dir}") // 원본 이미지 폴더
    private String imageSourceDir;

    @Value("${image.output-dir}") // 생성된 이미지 폴더
    private String imageOutputDir;

    @Value("${pdf.output-dir}") // 생성된 PDF 폴더
    private String pdfOutputDir;
    
    @Autowired
    private ExcelService excelService;

    // 1. 업로드된 엑셀 파일 저장
    public String saveUploadedFile(InputStream fileInputStream, String fileName) throws IOException {
        Path savePath = Paths.get(uploadDir, fileName).toAbsolutePath().normalize();
        
        // ✅ 강제로 Unix 스타일 경로 사용 (Windows에서도 동일한 경로 유지)
        File file = new File(savePath.toString().replace("\\", "/"));

        // ✅ 파일이 저장될 디렉토리가 없으면 생성
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        Files.copy(fileInputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return file.getAbsolutePath();
    } // saveUploadedFile

    // 2. 엑셀 파일에서 데이터 읽기
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

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue().trim());
            }

            // ✅ ExcelService의 기존 메서드 활용
            int lastRow = excelService.getLastDataRow(sheet);

            for (int i = 1; i <= lastRow; i++) { // ✅ 실제 데이터가 존재하는 행까지만 읽음
                Row row = sheet.getRow(i);
                if (row == null || excelService.isRowEmpty(row)) continue;

                Map<String, String> rowData = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowData.put(headers.get(j), cell.toString().trim());
                }
                dataList.add(rowData);
            }
        }
        return dataList;
    } // readExcelData

    // 3. 이미지에 텍스트 삽입
    private void processImage(String imagePath, String outputImagePath, List<Map<String, String>> excelData, List<Map<String, Object>> fieldData) throws IOException {
        BufferedImage image = ImageIO.read(new File(imagePath));
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));

        for (Map<String, Object> field : fieldData) {
            String fieldName = field.get("필드명").toString();
            int x = 0;
            int y = 0;

            // ✅ X좌표, Y좌표가 숫자로 저장되지 않았다면 변환
            try {
                x = field.get("X좌표") instanceof Number
                    ? ((Number) field.get("X좌표")).intValue()
                    : Integer.parseInt(field.get("X좌표").toString());

                y = field.get("Y좌표") instanceof Number
                    ? ((Number) field.get("Y좌표")).intValue()
                    : Integer.parseInt(field.get("Y좌표").toString());
            } catch (NumberFormatException e) {
                throw new IOException("좌표 변환 오류: X=" + field.get("X좌표") + ", Y=" + field.get("Y좌표"), e);
            }

            for (Map<String, String> rowData : excelData) {
                if (rowData.containsKey(fieldName)) {
                    g.drawString(rowData.get(fieldName), x, y);
                }
            }
        }

        g.dispose();
        ImageIO.write(image, "jpg", new File(outputImagePath));

        
    } // processImage

    // 4. 이미지들을 PDF로 변환
    private String createPdfFromImages(List<String> imagePaths, String pdfFileName) throws IOException {
        PDDocument document = new PDDocument();

        for (String imagePath : imagePaths) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDImageXObject image = PDImageXObject.createFromFile(imagePath, document);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.drawImage(image, 0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
            contentStream.close();
        }

        String pdfPath = pdfOutputDir + "/" + pdfFileName;
        document.save(pdfPath);
        document.close();
        return pdfPath;
        
    } // createPdfFromImages

    // 5. 전체 프로세스 실행
    public String generatePdfFromExcel(InputStream fileInputStream, String fileName) throws IOException {
        String uploadedFilePath = saveUploadedFile(fileInputStream, fileName);
        List<Map<String, String>> uploadedData = readExcelData(uploadedFilePath);

        // field_pos.xlsx 데이터 읽기
        List<List<Map<String, Object>>> fieldData = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(fieldPosPath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            for (int i = 0; i < 3; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<Map<String, Object>> sheetData = new ArrayList<>();

                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;

                List<String> headers = new ArrayList<>();
                for (Cell cell : headerRow) {
                    headers.add(cell.getStringCellValue().trim());
                }

                for (int j = 1; j <= sheet.getLastRowNum(); j++) {
                    Row row = sheet.getRow(j);
                    if (row == null) continue;

                    Map<String, Object> rowData = new HashMap<>();
                    for (int k = 0; k < headers.size(); k++) {
                        Cell cell = row.getCell(k, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        rowData.put(headers.get(k), excelService.getCellValue(cell));
                    }
                    sheetData.add(rowData);
                }

                fieldData.add(sheetData);
            }
        }

        // 이미지 생성
        List<String> outputImagePaths = new ArrayList<>();
        for (int i = 0; i < uploadedData.size(); i++) {
            for (int j = 0; j < 4; j++) {
                String sourceImage = imageSourceDir + "/survey_form_" + (j + 1) + ".jpg";
                String outputImage = imageOutputDir + "/output_" + i + "_" + (j + 1) + ".jpg";
                if (j < 3) {
                    processImage(sourceImage, outputImage, uploadedData, fieldData.get(j));
                } else {
                    Files.copy(Paths.get(sourceImage), Paths.get(outputImage), StandardCopyOption.REPLACE_EXISTING);
                }
                outputImagePaths.add(outputImage);
            }
        }

        // PDF 생성
        return createPdfFromImages(outputImagePaths, "output.pdf");
        
    } // generatePdfFromExcel

    


} // PdfService
