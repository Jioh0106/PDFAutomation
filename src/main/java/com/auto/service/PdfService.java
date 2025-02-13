package com.auto.service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
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

    @Value("${file.pos-info-path}") // field_pos.xlsx 경로
    private String fieldPosPath;

    @Value("${image.source-dir}") // 원본 이미지 폴더
    private String imageSourceDir;

    @Value("${image.output-dir}") // 생성된 이미지 폴더
    private String imageOutputDir;

    @Value("${pdf.output-dir}") // 생성된 PDF 폴더
    private String pdfOutputDir;
    
    @Value("${file.fonts-path}") // 내부 폰트 경로 - 나눔고딕
    private String fontsPath;
    
    @Autowired
    private ExcelService excelService;
    
    public static final String PDF_FILE_NAME = "면적조사서_출력용(4장).pdf"; // PDF 파일명
    
    private final SecretKey aesKey;
    
    
    /* *** 배포 시 환경변수 설정 및 변경 필요 *** */
    public PdfService() throws Exception {
        // AES 키 생성 및 저장 (운영 환경에서는 안전한 저장 방식 필요)
        this.aesKey = AESUtil.generateAESKey();
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
    public void generatePdfFromImages() throws IOException {
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
        
    } // generatePdfFromImages

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
    } // getSortedImageFiles
    
    /*
     *  생성된 이미지 및 PDF 파일 삭제 (PDF 다운로드 후 순서)
     */
    public void deleteGeneratedFiles() {
        deleteFilesInDirectory(imageOutputDir);
        deleteFilesInDirectory(pdfOutputDir);
        System.out.println("✅ 변환된 이미지 및 PDF 파일 삭제 완료");
    }

    /*
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
        
    } // deleteFilesInDirectory
    
    /*
     * 이미지에 텍스트 삽입 후 저장 (핵심 작업 로직)
     * */
	public void generateImage(List<Map<String, String>> targetData, Map<String, List<Map<String, Object>>> posData, 
								Map<String, List<Map<String, Object>>> regionData, String regionNum) throws IOException {
		
		
		List<String> savedImagePaths = new ArrayList<>();
		
		// 저장할 폴더 생성 (존재하지 않으면)
		File outputDir = new File(imageOutputDir);
		if (!outputDir.exists()) {
		    outputDir.mkdirs();
		}
		
		// 원본 이미지 파일 리스트 
		String[] imagePaths = {
			imageSourceDir + "/survey_form_1.jpg",
			imageSourceDir + "/survey_form_2.jpg",
			imageSourceDir + "/survey_form_3.jpg",
			imageSourceDir + "/survey_form_4.jpg"
		};
		
		// 시공회사 정보
		List<Map<String, Object>> regionDataSheet = regionData.get(regionNum);
		System.out.println("regionDataSheet = " + regionDataSheet);
		
	    String val1 = getSafeValue(regionDataSheet, 0); // 공사명
	    String val2 = getSafeValue(regionDataSheet, 1)+"원"; // 비계단가
	    String val3 = getSafeValue(regionDataSheet, 2)+"원"; // 개량단가
	    String val4 = getSafeValue(regionDataSheet, 3); // 시공회사
	    String val5 = getSafeValue(regionDataSheet, 4); // 회사대표자
	    String val6 = getSafeValue(regionDataSheet, 5); // 회사주소
	    String val7 = getSafeValue(regionDataSheet, 6); // 회사등록번호
	    String val8 = getSafeValue(regionDataSheet, 7); // 회사전화번호
	    String val9 = getSafeValue(regionDataSheet, 8); // 조사원명
	    String val10 = getSafeValue(regionDataSheet, 9); // 계약담당자명
	    
		
		
		// 업로드된 타겟 엑셀 데이터 리스트 반복문 시작
		for (int i = 0; i < targetData.size(); i++) {
			Map<String, String> target = targetData.get(i);
			System.out.println("target = " + target);
			
			// 현재 이미지 선택
			// 4장의 이미지를 각 targetData에 대해 처리
		    for (int imgIndex = 0; imgIndex < imagePaths.length; imgIndex++) {
		        String imagePath = imagePaths[imgIndex]; // 4장을 순서대로 가져옴
		        BufferedImage image = ImageIO.read(new File(imagePath));
			
				// Graphics2D를 사용하여 텍스트 삽입
				Graphics2D g2d = image.createGraphics();
				g2d.setColor(Color.BLACK);
			
				List<Map<String, Object>> posDataSheet = new ArrayList<>();
			
				if (imgIndex == 0) {
	                posDataSheet = posData.get("survey1");
	            } else if (imgIndex == 1) {
	                posDataSheet = posData.get("survey2");
	            } else if (imgIndex == 2) {
	                posDataSheet = posData.get("survey3");
	            } 
				
				for (Map<String, Object> fieldData : posDataSheet) {
	                int fontSize = getSafeIntValue(fieldData, "폰트크기", 16);
	                float x = getSafeFloatValue(fieldData, "X좌표", 0.0f);
	                float y = getSafeFloatValue(fieldData, "Y좌표", 0.0f);
	                String key = (String) fieldData.get("필드명");
	                String text = target.getOrDefault(key, "").trim();
	                String date = "2025.\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0.\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\00A0.";

	                // 특정 필드명에 대한 값 변경
	                if (key.equals("용역명")) text = val1;
	                if (key.equals("비계단가")) text = val2;
	                if (key.equals("개량단가")) text = val3;
	                if (key.equals("시공회사")) text = val4;
	                if (key.equals("회사대표자")) text = val5;
	                if (key.equals("회사주소")) text = val6;
	                if (key.equals("회사등록번호")) text = val7;
	                if (key.equals("회사전화번호")) text = val8;
	                if (key.equals("조사원명")) text = val9;
	                if (key.equals("계약담당자명")) text = val10;
	                if (key.equals("계약회사")) text = val4;
	                if (key.equals("계약부서")) text = "운영지원팀";
	                if (key.equals("감독부서")) text = "운영지원팀";
	                if (key.equals("주민번호")) text = target.get("생년월일").replace(".", "").substring(2, 8) + " - ";
	                if (key.equals("계약일자")) text = date;
	                if (key.equals("확인일자")) text = date;
	                if (key.equals("동의일자")) text = date;
	                
	                if(key.equals("구분") && text.equals("일반")) {
						text = "□ 우선지원가구   ■ 일반가구";
					} else if(key.equals("구분") && !text.equals("일반")) {
						text = "■ 우선지원가구   □ 일반가구";
					}
	                
					if(key.equals("소유주명")) {
						text = target.get("건축주명");
					}
					if(key.equals("감독관명")) {
						text = target.get("감독관");
					}
					
					
					try {
					    File fontFile = new File(fontsPath); 
					    Font customFont = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(Font.PLAIN, fontSize);
					    g2d.setFont(customFont);
					} catch (Exception e) {
					    e.printStackTrace();
					    g2d.setFont(new Font("SansSerif", Font.PLAIN, fontSize)); // 기본 폰트 적용
					}

					g2d.drawString(text, x, y);
	            }

				g2d.dispose(); // 리소스 해제
				
				// 순서대로 파일을 저장하기 위한 인덱스값 얻기  
				Object indexObj = targetData.get(i).get("순번");
				String indexStr = (indexObj instanceof Number) ? String.valueOf(((Number) indexObj).intValue()) : (indexObj != null ? indexObj.toString() : "");
	
				if (indexStr.isEmpty()) {
				    System.out.println("🔴 순번 값이 비어 있어 처리할 수 없습니다. (index: " + i + ")");
				    continue;
				}
				String index = indexStr.split("\\.")[0]; // "1.0" → "1" 순번 값에서 정수 부분만 추출
				// 🔹 숫자를 최소 2자리(01, 02...) 또는 3자리(001, 002...)로 맞추기
				String formattedIndex = String.format("%02d", Integer.parseInt(index));
				
				// 이미지 파일 저장 경로
				String outputFilePath = imageOutputDir + "/" + formattedIndex + "-" + (imgIndex +1) + ".jpg";
				File outputFile = new File(outputFilePath);
				ImageIO.write(image, "jpg", outputFile);
				
				savedImagePaths.add(outputFilePath);
			
		  
		    }
			
			
		} // targetData 반복문 끝
            
		
		
	} // generateImage
	
	// 안전한 값 가져오기 함수
	private String getSafeValue(List<Map<String, Object>> sheet, int index) {
	    return (index < sheet.size() && sheet.get(index).get("값") != null) ? sheet.get(index).get("값").toString() : "";
	}

	private int getSafeIntValue(Map<String, Object> map, String key, int defaultValue) {
	    return (map.get(key) instanceof Number) ? ((Number) map.get(key)).intValue() : defaultValue;
	}

	private float getSafeFloatValue(Map<String, Object> map, String key, float defaultValue) {
	    return (map.get(key) instanceof Number) ? ((Number) map.get(key)).floatValue() : defaultValue;
	}

} // PdfService
