package com.auto.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
//    		pdfService.copyAndRenameImages(targetExcelData);
    		
    		for(int i = 0; i < targetData.size(); i++ ) {
    			System.out.println("targetData " + i + " = " + targetData.get(i));
    			String index = targetData.get(i).get("순번").split("\\.")[0];
    			
    		}
            
            // PDF 변환 로직 실행 (이전 로직 유지)
//            String pdfPath = pdfService.generatePdfFromExcel(file.getInputStream(), file.getOriginalFilename());
//            return ResponseEntity.ok(pdfPath);
            return ResponseEntity.ok("");
            
        } catch (Exception e) {
        	
            return ResponseEntity.internalServerError().body("PDF 변환 중 오류 발생: " + e.getMessage());
        }
    	
    	
    	
    } // convertToPdf
    
    
    
    
} // PdfController
