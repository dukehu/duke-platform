import request from '@/utils/request'
import type { Document, DocumentQueryDTO } from '@/types/document'
import type { PageResult } from '@/types/common'
import type { AxiosProgressEvent } from 'axios'

// 上传文件到 duke-storage
export function uploadDocument(formData: FormData, onUploadProgress?: (progressEvent: AxiosProgressEvent) => void) {
  return request.post<{ fileId: number; originalName: string; fileUrl: string }>(
    '/storage/files/upload',
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress
    }
  )
}

// 获取文件列表（从 duke-storage）
export function getDocuments(params: DocumentQueryDTO) {
  return request.get<PageResult<Document>>('/storage/files/list', { params })
}

// 获取文件详情
export function getDocumentById(id: number) {
  return request.get<Document>(`/storage/files/${id}`)
}

// 删除文件
export function deleteDocument(id: number) {
  return request.delete<void>(`/storage/files/${id}`)
}

// 下载文件
export function downloadFile(fileId: number) {
  return request.get(`/storage/files/download/${fileId}`, {
    responseType: 'blob'
  })
}

// 预览文件
export function previewFile(fileId: number) {
  return request.get(`/storage/files/preview/${fileId}`, {
    responseType: 'blob'
  })
}
