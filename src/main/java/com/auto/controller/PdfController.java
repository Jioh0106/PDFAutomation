package com.auto.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.auto.service.PdfService;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @PostMapping("/convert")
    public ResponseEntity<String> convertToPdf(@RequestParam("file") MultipartFile file) {
        try {
            String pdfPath = pdfService.generatePdfFromExcel(file.getInputStream(), file.getOriginalFilename());
            return ResponseEntity.ok(pdfPath);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("PDF 변환 중 오류 발생: " + e.getMessage());
        }
    }
    
    
    
    
}
