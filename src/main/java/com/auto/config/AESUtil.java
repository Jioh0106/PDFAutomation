package com.auto.config;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

public class AESUtil {
    private static final String AES_ALGORITHM = "AES";
    private static final int AES_KEY_SIZE = 256;
    
    // AES 키 생성
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGenerator.init(AES_KEY_SIZE, new SecureRandom());
        return keyGenerator.generateKey();
    }

    // AES 키를 Base64로 변환하여 저장
    public static String encodeKey(SecretKey secretKey) {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    // Base64에서 SecretKey로 변환
    public static SecretKey decodeKey(String key) {
        byte[] decodedKey = Base64.getDecoder().decode(key);
        return new SecretKeySpec(decodedKey, AES_ALGORITHM);
    }

    // 파일 암호화
    public static void encryptFile(SecretKey key, String inputFilePath, String outputFilePath) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] inputBytes = Files.readAllBytes(Paths.get(inputFilePath));
        byte[] encryptedBytes = cipher.doFinal(inputBytes);

        try (FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
            outputStream.write(encryptedBytes);
        }
    }

    // 파일 복호화
    public static void decryptFile(SecretKey key, String inputFilePath, String outputFilePath) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] inputBytes = Files.readAllBytes(Paths.get(inputFilePath));
        byte[] decryptedBytes = cipher.doFinal(inputBytes);

        try (FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
            outputStream.write(decryptedBytes);
        }
    }
}
