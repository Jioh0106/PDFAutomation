package com.auto.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

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
    


} // PdfService
