import request from '@/utils/request'
import type { Document, DocumentQueryDTO } from '@/types/document'
import type { PageResult } from '@/types/common'
import type { AxiosProgressEvent } from 'axios'

export function uploadDocument(formData: FormData, onUploadProgress?: (progressEvent: AxiosProgressEvent) => void) {
  return request.post<{ documentId: number; status: string }>(
    '/knowledge-qa/documents/upload',
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress
    }
  )
}

export function getDocuments(params: DocumentQueryDTO) {
  return request.get<PageResult<Document>>('/knowledge-qa/documents', { params })
}

export function getDocumentById(id: number) {
  return request.get<Document>(`/knowledge-qa/documents/${id}`)
}

export function deleteDocument(id: number) {
  return request.delete<void>(`/knowledge-qa/documents/${id}`)
}
