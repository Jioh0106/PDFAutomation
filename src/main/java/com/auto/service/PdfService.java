package com.auto.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auto.config.AESUtil;

@Service
public class PdfService {
	
	@Value("${file.upload-dir}") // 업로드된 엑셀 저장 경로
    private String uploadDir;

    @Value("${file.info-path}") // field_pos.xlsx 경로
    private String fieldPosPath;

    @Value("${image.source-dir}") // 원본 이미지 폴더
    private String imageSourceDir;

    @Value("${image.output-dir}") // 생성된 이미지 폴더
    private String imageOutputDir;

    @Value("${pdf.output-dir}") // 생성된 PDF 폴더
    private String pdfOutputDir;
    
    @Autowired
    private ExcelService excelService;
    
    private final SecretKey aesKey;
    
    public static final String PDF_FILE_NAME = "merged_images.pdf"; // PDF 파일명
    
    /* *** 배포 시 환경변수 설정 및 변경 필요 *** */
    public PdfService() throws Exception {
        // AES 키 생성 및 저장 (운영 환경에서는 안전한 저장 방식 필요)
        this.aesKey = AESUtil.generateAESKey();
        System.out.println("*** AES Secret Key: " + AESUtil.encodeKey(aesKey)); // 실제 운영에서는 노출 X 삭제하기
    }
    
    
    /* 
     * 업로드된 파일 암호화 후 저장 
     */
    public String saveUploadedFile(InputStream fileInputStream, String fileName) throws Exception {
        Path savePath = Paths.get(uploadDir, fileName + ".enc").toAbsolutePath().normalize();
        File file = new File(savePath.toString().replace("\\", "/"));
        
        // 폴더 생성
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 원본 파일 저장
        File tempFile = new File(uploadDir + "/" + fileName);
        Files.copy(fileInputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // AES 암호화 적용
        AESUtil.encryptFile(aesKey, tempFile.getAbsolutePath(), file.getAbsolutePath());

        // 원본 삭제
        tempFile.delete();

        return file.getAbsolutePath();
    }
    
    /*
     * 파일 복호화 후 읽기
     */
    public List<Map<String, String>> readEncryptedExcelData(String encryptedFilePath) throws Exception {
        // 복호화된 임시 파일 경로
        String decryptedFilePath = encryptedFilePath.replace(".enc", ".xlsx");

        // 파일 복호화
        AESUtil.decryptFile(aesKey, encryptedFilePath, decryptedFilePath);

        // 엑셀 데이터 읽기
        List<Map<String, String>> data = excelService.readExcelData(decryptedFilePath);

        // 복호화된 파일 삭제
        new File(decryptedFilePath).delete();

        return data;
    }
    

    /*
     * 이미지 복사 및 이름 변경 작업 실행
     */
    public void copyAndRenameImages(List<Map<String, String>> excelData) {
    	
        for (int i = 0; i < excelData.size(); i++) {
        	
            String indexStr = excelData.get(i).get("순번");
            
            if (indexStr == null || indexStr.isEmpty()) {
                System.out.println("순번 값이 비어 있어 처리할 수 없습니다. (index: " + i + ")");
                continue;
            }

            // 순번 값에서 정수 부분만 추출
            String index = indexStr.split("\\.")[0]; // "1.0" → "1"

            // 이미지 4장 복사 및 이름 변경
            copyImage("survey_form_1.jpg", index + "-1");
            copyImage("survey_form_2.jpg", index + "-2");
            copyImage("survey_form_3.jpg", index + "-3");
            copyImage("survey_form_4.jpg", index + "-4");
        }
    }

    /*
     * 원본 이미지를 복사하고 새로운 이름으로 저장
     */
    private void copyImage(String originalFileName, String newFileName) {
    	
        File sourceFile = new File(imageSourceDir + "/" + originalFileName);
        if (!sourceFile.exists()) {
            System.out.println("원본 이미지가 존재하지 않습니다: " + sourceFile.getAbsolutePath());
            return;
        }

        File destinationFile = new File(imageOutputDir + "/" + newFileName + ".jpg");

        try {
            // 이미지 복사 (덮어쓰기)
            Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("이미지 복사 완료: " + sourceFile.getName() + " → " + newFileName + ".jpg");
        } catch (IOException e) {
            System.out.println("오류: 이미지 복사 중 문제가 발생했습니다. (" + newFileName + ")");
            e.printStackTrace();
        }
    }
    
    /*
     *  저장된 이미지를 하나의 PDF로 변환
     */
    public String generatePdfFromImages() throws IOException {
        // 이미지 파일 가져오기 (정렬 포함)
        List<File> imageFiles = getSortedImageFiles(imageOutputDir);
        
        if (imageFiles.isEmpty()) {
            throw new IOException("이미지 파일이 존재하지 않습니다.");
        }

        // PDF 저장 경로
        File pdfFile = new File(pdfOutputDir, PDF_FILE_NAME);

        try (PDDocument document = new PDDocument()) {
            for (File imageFile : imageFiles) {
                BufferedImage image = ImageIO.read(imageFile);
                if (image == null) continue;
                
                // 원본 이미지 크기 가져오기
                float imageWidth = image.getWidth();
                float imageHeight = image.getHeight();
                
                // PDF 페이지 크기를 이미지 크기에 맞춤
                PDPage page = new PDPage(new PDRectangle(imageWidth, imageHeight));
                document.addPage(page);

                PDImageXObject pdImage = PDImageXObject.createFromFile(imageFile.getAbsolutePath(), document);
               
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                	// 이미지의 원래 크기로 PDF에 삽입 (비율 유지)
                	contentStream.drawImage(pdImage, 0, 0, imageWidth, imageHeight);
                }
            }
            document.save(pdfFile);
        }

        System.out.println("✅ PDF 생성 완료: " + pdfFile.getAbsolutePath());
        return pdfFile.getAbsolutePath();
    }

    /*
     *  이미지 폴더에서 JPG 파일을 정렬하여 가져오기
     */
    private List<File> getSortedImageFiles(String directoryPath) {
        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return List.of();
        }
        return Arrays.stream(dir.listFiles((dir1, name) -> name.toLowerCase().matches(".*\\.(jpg|jpeg|png)$")))
                .sorted(Comparator.comparing(File::getName))
                .collect(Collectors.toList());
    } 
    
    /**
     * PDF 다운로드 후 생성된 이미지 및 PDF 파일 삭제
     */
    public void deleteGeneratedFiles() {
        deleteFilesInDirectory(imageOutputDir);
        deleteFilesInDirectory(pdfOutputDir);
        System.out.println("✅ 변환된 이미지 및 PDF 파일 삭제 완료");
    }

    /**
     * 특정 디렉토리 내 파일 삭제 (디렉토리는 유지)
     */
    private void deleteFilesInDirectory(String directoryPath) {
        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("⚠ 삭제할 디렉토리가 존재하지 않습니다: " + directoryPath);
            return;
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("⚠ 삭제할 파일이 없습니다: " + directoryPath);
            return;
        }

        for (File file : files) {
            if (file.isFile()) {
                boolean deleted = file.delete();
                System.out.println(deleted ? "🗑 삭제 성공: " + file.getAbsolutePath() : "❌ 삭제 실패: " + file.getAbsolutePath());
            }
        }
    }

} // PdfService
