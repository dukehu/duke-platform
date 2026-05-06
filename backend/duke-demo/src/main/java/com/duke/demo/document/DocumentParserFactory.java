package com.duke.demo.document;

import com.duke.framework.exception.BusinessException;
import com.duke.framework.common.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文档解析器工厂，根据文件后缀路由合适的解析器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParserFactory {

    private final PdfDocumentParser pdfParser;
    private final WordDocumentParser wordParser;
    private final TextDocumentParser textParser;

    /**
     * 根据文件名获取对应的解析器
     *
     * @param fileName 文件名
     * @return 匹配的解析器
     * @throws BusinessException 不支持的文件类型
     */
    public DocumentParser getParser(String fileName) {
        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".pdf")) {
            return pdfParser;
        } else if (lowerName.endsWith(".docx")) {
            return wordParser;
        } else if (lowerName.endsWith(".txt") || lowerName.endsWith(".md")) {
            return textParser;
        } else {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不支持的文件类型：" + fileName);
        }
    }
}
