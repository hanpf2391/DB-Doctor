package com.dbdoctor.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 加密解密服务
 * 使用 AES 加密算法保护敏感信息（密码、API Key 等）
 *
 * @author DB-Doctor
 * @version 3.1.0
 */
@Slf4j
@Service
public class EncryptionService {

    /**
     * 加密密钥（从配置文件读取，默认值用于开发环境）
     * 生产环境必须在配置文件中设置强密钥
     */
    @Value("${db-doctor.encryption.key:db-doctor-secret-key-32-bytes!!}")
    private String encryptionKey;

    /**
     * 加密算法
     */
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int KEY_LENGTH = 32; // AES-256 需要 32 字节密钥

    /**
     * 加密字符串
     *
     * @param plainText 明文
     * @return Base64 编码的密文
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            // 确保密钥长度为 32 字节
            byte[] keyBytes = prepareKey(encryptionKey);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("加密失败", e);
            throw new RuntimeException("加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解密字符串（容错模式：兼容明文密码）
     *
     * @param cipherText Base64 编码的密文或明文（向后兼容）
     * @return 明文
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }

        try {
            // 确保密钥长度为 32 字节
            byte[] keyBytes = prepareKey(encryptionKey);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Base64 解码失败，说明可能是明文密码（向后兼容旧数据）
            log.warn("⚠️  Base64 解码失败，视为明文密码: {}", e.getMessage());
            return cipherText;
        } catch (Exception e) {
            // 其他解密错误，记录日志并返回原值（容错处理）
            log.error("❌ 解密失败，返回原值（可能是明文密码或损坏的数据）: {}", e.getMessage());
            return cipherText;
        }
    }

    /**
     * 准备密钥，确保长度为 32 字节（AES-256）
     *
     * @param key 原始密钥
     * @return 32 字节的密钥
     */
    private byte[] prepareKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[KEY_LENGTH];

        if (keyBytes.length >= KEY_LENGTH) {
            // 如果密钥足够长，截取前 32 字节
            System.arraycopy(keyBytes, 0, result, 0, KEY_LENGTH);
        } else {
            // 如果密钥不够长，复制并填充 0
            System.arraycopy(keyBytes, 0, result, 0, keyBytes.length);
            // 剩余字节保持为 0
        }

        return result;
    }
}
