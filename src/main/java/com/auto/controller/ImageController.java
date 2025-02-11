package com.auto.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/download")
public class ImageController {
	
    @Value("${image.source-dir}")
    private String image_derectory;
    private static final String ZIP_FILE_NAME = "survey_forms.zip";
    
    
    @GetMapping("/zip")
    public ResponseEntity<Resource> downloadZipFile() {
    	
    	
        try {
            // 임시 ZIP 파일 생성
            File zipFile = new File(System.getProperty("java.io.tmpdir"), ZIP_FILE_NAME);
            try (FileOutputStream fos = new FileOutputStream(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                // JPG 파일들을 ZIP에 추가
                for (int i = 1; i <= 4; i++) {
                    String fileName = "survey_form_" + i + ".jpg";
                    File file = new File(image_derectory + "/" + fileName);
                    System.out.println("파일 경로: " + file.getAbsolutePath());
                    System.out.println("파일 존재 여부: " + file.exists());
                    
                    if (file.exists()) {
                        // ZIP에 파일 추가
                        zos.putNextEntry(new ZipEntry(fileName));
                        Files.copy(file.toPath(), zos);
                        zos.closeEntry();
                    }
                }
            }

            // ZIP 파일을 클라이언트에 반환
            InputStreamResource resource = new InputStreamResource(new FileInputStream(zipFile));
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + ZIP_FILE_NAME);
            headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(zipFile.length())
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    	
    }
    
    
}
