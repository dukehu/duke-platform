package com.duke.storage.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duke.storage.config.properties.FileProperties;
import com.duke.storage.entity.SysFile;
import com.duke.storage.entity.SysFileChunk;
import com.duke.storage.mapper.SysFileChunkMapper;
import com.duke.storage.mapper.SysFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件清理定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupTask {
    
    private final SysFileChunkMapper sysFileChunkMapper;
    private final SysFileMapper sysFileMapper;
    private final FileProperties fileProperties;
    
    /**
     * 每天凌晨2点执行
     * 清理过期的分片临时文件和数据库记录
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredChunks() {
        log.info("开始清理过期分片...");
        
        try {
            // 计算过期时间
            LocalDateTime expireTime = LocalDateTime.now().minusDays(fileProperties.getChunkExpireDay());
            
            // 查询过期的分片记录
            List<SysFileChunk> expiredChunks = sysFileChunkMapper.selectList(
                new LambdaQueryWrapper<SysFileChunk>()
                    .lt(SysFileChunk::getCreateTime, expireTime)
            );
            
            if (expiredChunks.isEmpty()) {
                log.info("没有需要清理的过期分片");
                return;
            }
            
            log.info("找到 {} 个过期分片，开始清理...", expiredChunks.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (SysFileChunk chunk : expiredChunks) {
                try {
                    // 删除物理文件
                    Path chunkPath = Paths.get(fileProperties.getChunkTempPath(), chunk.getChunkPath());
                    Files.deleteIfExists(chunkPath);
                    
                    // 删除数据库记录
                    sysFileChunkMapper.deleteById(chunk.getId());
                    
                    successCount++;
                    log.debug("分片清理成功: id={}, path={}", chunk.getId(), chunk.getChunkPath());
                } catch (IOException e) {
                    failCount++;
                    log.error("分片清理失败: id={}, path={}", chunk.getId(), chunk.getChunkPath(), e);
                }
            }
            
            log.info("过期分片清理完成: 成功={}, 失败={}", successCount, failCount);
            
        } catch (Exception e) {
            log.error("清理过期分片时发生异常", e);
        }
    }
    
    /**
     * 每周日凌晨3点执行
     * 清理逻辑删除超过30天的物理文件
     */
    @Scheduled(cron = "0 0 3 * * 0")
    public void cleanupDeletedFiles() {
        log.info("开始清理已删除文件的物理文件...");
        
        try {
            // 计算过期时间
            LocalDateTime expireTime = LocalDateTime.now().minusDays(fileProperties.getDeleteExpireDay());
            
            // 查询已逻辑删除且超过保留期的文件
            List<SysFile> deletedFiles = sysFileMapper.selectList(
                new LambdaQueryWrapper<SysFile>()
                    .eq(SysFile::getDeleted, 1)
                    .lt(SysFile::getUpdateTime, expireTime)
            );
            
            if (deletedFiles.isEmpty()) {
                log.info("没有需要清理的已删除文件");
                return;
            }
            
            log.info("找到 {} 个需要清理的已删除文件，开始清理...", deletedFiles.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (SysFile file : deletedFiles) {
                try {
                    // 删除物理文件
                    Path filePath = Paths.get(fileProperties.getLocalBasePath(), file.getSavePath());
                    Files.deleteIfExists(filePath);
                    
                    // 物理删除数据库记录
                    sysFileMapper.deleteById(file.getId());
                    
                    successCount++;
                    log.debug("文件清理成功: id={}, path={}", file.getId(), file.getSavePath());
                } catch (IOException e) {
                    failCount++;
                    log.error("文件清理失败: id={}, path={}", file.getId(), file.getSavePath(), e);
                }
            }
            
            log.info("已删除文件清理完成: 成功={}, 失败={}", successCount, failCount);
            
        } catch (Exception e) {
            log.error("清理已删除文件时发生异常", e);
        }
    }
}
