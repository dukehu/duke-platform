package com.duke.demo.document;

import cn.hutool.core.io.IoUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * PDF 文档解析器，使用 PDFBox 提取文本
 */
@Slf4j
@Component
public class PdfDocumentParser implements DocumentParser {

    @Override
    public String parse(InputStream in, String fileName) throws IOException {
        byte[] pdfBytes = IoUtil.readBytes(in);
        PDDocument document = Loader.loadPDF(pdfBytes);
        try (document) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("Parsed PDF {}, extracted {} characters", fileName, text.length());
            return text;
        }
    }
}
