package com.auto.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auto.service.ExcelService;

@RestController
@RequestMapping("/api")
public class ExcelController {

    @Autowired
    private ExcelService excelService;
    
    @Value("${file.info-path}") 
    private String excelFilePath;

    // 엑셀 데이터 조회 API
    @GetMapping("/info/check")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getExcelData() {
        try {
            Map<String, List<Map<String, Object>>> data = excelService.readExcelData();
            return ResponseEntity.ok(data);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
    
    
    // 엑셀 파일 다운로드 API
    @GetMapping("/info/download")
    public ResponseEntity<Resource> downloadExcel() throws IOException {
        File file = new File(excelFilePath);
        if (!file.exists()) {
            throw new IOException("Excel 파일이 존재하지 않습니다: " + file.getAbsolutePath());
        }

        Resource resource = new FileSystemResource(file);
        String contentDisposition = "attachment; filename=\"" + file.getName() + "\"";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
    
    
    

} // ExcelController
