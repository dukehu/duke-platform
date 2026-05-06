package com.duke.storage.service;

import com.duke.framework.common.PageResult;
import com.duke.storage.dto.ChunkMergeDTO;
import com.duke.storage.dto.ChunkUploadDTO;
import com.duke.storage.dto.FileQueryDTO;
import com.duke.storage.entity.SysFile;
import com.duke.storage.vo.ChunkCheckVO;
import com.duke.storage.vo.FileCheckVO;
import com.duke.storage.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 文件存储服务接口
 */
public interface IFileStorageService {
    
    /**
     * 上传文件（普通上传）
     *
     * @param file 文件
     * @return 文件信息
     */
    FileVO uploadFile(MultipartFile file);
    
    /**
     * 校验文件是否存在（秒传）
     *
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @return 检查结果
     */
    FileCheckVO checkFileExists(String fileName, Long fileSize);
    
    /**
     * 上传分片
     *
     * @param dto 分片信息
     * @param chunkData 分片数据
     */
    void uploadChunk(ChunkUploadDTO dto, InputStream chunkData);
    
    /**
     * 检查分片上传状态
     *
     * @param chunkId 分片ID
     * @param chunkTotal 总分片数
     * @return 检查结果
     */
    ChunkCheckVO checkChunks(String chunkId, Integer chunkTotal);
    
    /**
     * 合并分片
     *
     * @param dto 合并信息
     * @return 文件信息
     */
    FileVO mergeChunks(ChunkMergeDTO dto);
    
    /**
     * 获取文件信息
     *
     * @param fileId 文件ID
     * @return 文件信息
     */
    FileVO getFileById(Long fileId);
    
    /**
     * 分页查询文件列表
     *
     * @param dto 查询条件
     * @return 分页结果
     */
    PageResult<FileVO> pageFiles(FileQueryDTO dto);
    
    /**
     * 逻辑删除文件
     *
     * @param fileId 文件ID
     */
    void deleteFile(Long fileId);
    
    /**
     * 获取文件输入流（用于下载/预览）
     *
     * @param fileId 文件ID
     * @return 文件输入流
     */
    InputStream getFileInputStream(Long fileId);
    
    /**
     * 获取文件信息实体
     *
     * @param fileId 文件ID
     * @return 文件实体
     */
    SysFile getFileEntity(Long fileId);
}
