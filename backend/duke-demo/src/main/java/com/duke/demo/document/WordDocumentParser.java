package com.duke.demo.document;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Word .docx 文档解析器，使用 POI 提取文本
 */
@Slf4j
@Component
public class WordDocumentParser implements DocumentParser {

    @Override
    public String parse(InputStream in, String fileName) throws IOException {
        try (XWPFDocument document = new XWPFDocument(in)) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String paraText = paragraph.getText();
                if (!paraText.isEmpty()) {
                    text.append(paraText).append("\n");
                }
            }
            String result = text.toString();
            log.info("Parsed Word {}, extracted {} characters", fileName, result.length());
            return result;
        }
    }
}
