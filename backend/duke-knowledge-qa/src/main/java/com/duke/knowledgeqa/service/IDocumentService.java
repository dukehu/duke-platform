package com.duke.knowledgeqa.service;

import com.duke.framework.common.PageResult;
import com.duke.knowledgeqa.dto.DocumentQueryDTO;
import com.duke.knowledgeqa.vo.DocumentVO;
import com.duke.knowledgeqa.vo.UploadResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface IDocumentService {

    UploadResultVO upload(MultipartFile file, String category, String tags, Long userId);

    PageResult<DocumentVO> page(DocumentQueryDTO dto);

    DocumentVO getById(Long id);

    void delete(Long id);
}
