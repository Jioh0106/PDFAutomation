package com.auto.controller;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.auto.service.ExcelService;
import com.auto.service.PdfService;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    @Autowired
    private PdfService pdfService;
    
    @Autowired
    private ExcelService excelService;
    
    @Value("${pdf.output-dir}") // 생성된 PDF 폴더
    private String pdfOutputDir;
    
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;
    

    @PostMapping("/convert")
    public ResponseEntity<String> convertToPdf(@RequestParam("file") MultipartFile file, @RequestParam("regionNum") String regionNum) {
    	System.out.println("regionNum = " + regionNum);
    	try {
    		if (file.isEmpty()) {
                System.out.println("❌ 파일이 비어 있음");
                return ResponseEntity.badRequest().body("파일이 비어 있습니다.");
            }

            System.out.println("📂 업로드된 파일 이름: " + file.getOriginalFilename());
            System.out.println("📏 파일 크기: " + file.getSize());
            
    		// 1. 업로드된 타켓 파일을 AES 암호화 후 저장
    		String encryptedFilePath = pdfService.saveUploadedFile(file.getInputStream(), file.getOriginalFilename());
    		System.out.println("1. 업로드된 타켓 파일을 AES 암호화 후 저장 성공");
    		
    		// 2. 복호화 후 타겟 엑셀 데이터 읽기
    		List<Map<String, String>> targetData = pdfService.readEncryptedExcelData(encryptedFilePath);
    		System.out.println("2. 업로드 엑셀 파일 복호화 후 타겟 엑셀 데이터 읽기 성공");
    		
    		// 3. 내부 위치 기준 정보 엑셀 파일 데이터 읽기
    		Map<String, List<Map<String, Object>>> posData = excelService.readExcelDataALL("pos");
    		System.out.println("3. 내부 위치 기준 정보 엑셀 파일 데이터 읽기 성공");
    		System.out.println("posData = " + posData.toString());
    		
    		// 4. 내부 시공사 정보 엑셀 파일 데이터 읽기
    		Map<String, List<Map<String, Object>>> regionData = excelService.readExcelDataALL("region");
    		System.out.println("4. 내부 시공사 정보 엑셀 파일 데이터 읽기 성공");
    		System.out.println("regionData = " + regionData.toString());
    		
    		// 5. 이미지에 텍스트 삽입, 복사, 이름 변경하기
    		pdfService.generateImage(targetData, posData, regionData, regionNum);    	
    		System.out.println("5. 이미지에 텍스트 삽입 변환 성공");
    		
    		// 6. 최종 PDF 변환 실행
            pdfService.generatePdfFromImages();
            System.out.println("6. 최종 PDF 변환 실행 성공");
    		
            
            return ResponseEntity.ok("/api/pdf/download?file=" + PdfService.PDF_FILE_NAME);
            
        } catch (Exception e) {
        	
            return ResponseEntity.internalServerError().body("PDF 변환 중 오류 발생: " + e.getMessage());
        }
    	
    	
    	
    } // convertToPdf
    
    /*
     * PDF 다운로드 엔드포인트
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadPdf(@RequestParam("file") String fileName) {
        File pdfFile = new File(pdfOutputDir, fileName);

        if (!pdfFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(pdfFile);
        try {
            // 한글 파일명 UTF-8 인코딩 (공백 처리 포함)
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");

            // 🕒 10초 후 파일 삭제 (다운로드 이후 삭제하도록 변경)
            scheduleFileDeletion(10);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (UnsupportedEncodingException e) {
            return ResponseEntity.internalServerError().body(null);
        }
        

    } // downloadPdf
    
    /*
     * 🕒 일정 시간 후 이미지 및 PDF 삭제 (비동기 실행)
     */
    private void scheduleFileDeletion(int delaySeconds) {
        taskScheduler.schedule(() -> {
            pdfService.deleteGeneratedFiles(); // ✅ PDF + 이미지 삭제 실행
        }, Instant.now().plusSeconds(delaySeconds));
        
    } // scheduleFileDeletion
    
    
    
} // PdfController
