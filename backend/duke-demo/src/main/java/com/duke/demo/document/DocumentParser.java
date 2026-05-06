package com.duke.demo.document;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文档解析器接口，将各格式文件转为纯文本
 */
public interface DocumentParser {

    /**
     * 解析文件为纯文本
     *
     * @param in       输入流
     * @param fileName 原始文件名
     * @return 提取的纯文本
     * @throws IOException 解析失败时抛出
     */
    String parse(InputStream in, String fileName) throws IOException;
}
