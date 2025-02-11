package com.auto.controller;

import java.io.File;
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
    public ResponseEntity<String> convertToPdf(@RequestParam("file") MultipartFile file) {
    	try {
    		// 업로드된 타켓 파일을 AES 암호화 후 저장
    		String encryptedFilePath = pdfService.saveUploadedFile(file.getInputStream(), file.getOriginalFilename());

    		// 복호화 후 타겟 엑셀 데이터 읽기
    		List<Map<String, String>> targetData = pdfService.readEncryptedExcelData(encryptedFilePath);
    		
    		// 내부 기준 정보 엑셀 파일 데이터 읽기
    		Map<String, List<Map<String, Object>>> infoData = excelService.readExcelData();
    		System.out.println("infoData = " + infoData.toString());
    		
    		// 양식 이미지(4장 세트) 타겟 행만큼 복사 및 이름 변경 실행
    		pdfService.copyAndRenameImages(targetData);
    		
    		// PDF 변환 실행
            String pdfPath = pdfService.generatePdfFromImages();
    		
    		for(int i = 0; i < targetData.size(); i++ ) {
    			System.out.println("targetData " + i + " = " + targetData.get(i));
    			String index = targetData.get(i).get("순번").split("\\.")[0];
    			
    		}
            
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

        // 🕒 10초 후 파일 삭제 (다운로드 이후 삭제하도록 변경)
        scheduleFileDeletion(10);


        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);

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
