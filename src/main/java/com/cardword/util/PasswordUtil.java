package com.cardword.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码加密工具类
 * 使用 SHA-256 算法对密码进行加密
 */
public class PasswordUtil {

    /**
     * 对密码进行 SHA-256 加密
     *
     * @param password 原始密码
     * @return 加密后的密码（64 位十六进制字符串）
     */
    public static String encrypt(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            
            // 将字节数组转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }

    /**
     * 验证密码是否匹配
     *
     * @param rawPassword     原始密码
     * @param encryptedPassword 加密后的密码
     * @return 匹配返回 true，否则返回 false
     */
    public static boolean verify(String rawPassword, String encryptedPassword) {
        String encryptedInput = encrypt(rawPassword);
        return encryptedInput.equals(encryptedPassword);
    }
}
