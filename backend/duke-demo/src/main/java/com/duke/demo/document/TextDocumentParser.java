package com.duke.demo.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 纯文本文档解析器
 */
@Slf4j
@Component
public class TextDocumentParser implements DocumentParser {

    @Override
    public String parse(InputStream in, String fileName) throws IOException {
        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        log.info("Parsed text {}, extracted {} characters", fileName, text.length());
        return text;
    }
}
