package com.duke.knowledgeqa.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/files")
public class FileController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @GetMapping("/{filename}")
    public void serve(@PathVariable String filename, HttpServletResponse response) throws IOException {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();

            // 安全校验：路径必须在 uploadDir 内（防路径穿越）
            if (!filePath.startsWith(Paths.get(uploadDir).normalize())) {
                log.warn("尝试访问非法路径: {}", filename);
                response.setStatus(400);
                return;
            }

            if (!Files.exists(filePath)) {
                log.warn("文件不存在: {}", filename);
                response.setStatus(404);
                return;
            }

            String ext = getFileExtension(filename).toLowerCase();
            String contentType = getContentType(ext);
            response.setContentType(contentType);

            // PDF/TXT/MD 使用 inline 让浏览器内嵌展示，DOCX 使用 attachment 下载
            String disposition = ("docx".equals(ext) || "doc".equals(ext)) ? "attachment" : "inline";
            response.setHeader("Content-Disposition",
                    disposition + "; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");

            response.setContentLengthLong(Files.size(filePath));
            Files.copy(filePath, response.getOutputStream());
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("文件访问出错: {}", filename, e);
            response.setStatus(500);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private String getContentType(String ext) {
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain; charset=utf-8";
            case "md" -> "text/plain; charset=utf-8";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc" -> "application/msword";
            default -> "application/octet-stream";
        };
    }
}
