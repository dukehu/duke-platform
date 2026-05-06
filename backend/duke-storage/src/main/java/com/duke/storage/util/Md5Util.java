package com.duke.storage.util;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.security.MessageDigest;

/**
 * MD5计算工具类
 */
@Slf4j
public class Md5Util {
    
    /**
     * 从输入流计算MD5
     *
     * @param inputStream 输入流
     * @return MD5字符串（32位小写）
     */
    public static String calculateMd5(InputStream inputStream) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString();
        } catch (Exception e) {
            log.error("计算MD5失败", e);
            throw new RuntimeException("计算MD5失败", e);
        }
    }
}
