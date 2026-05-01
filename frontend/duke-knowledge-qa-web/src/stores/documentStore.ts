import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import { getDocuments, uploadDocument as uploadDocumentApi, deleteDocument as deleteDocumentApi } from '@/api/document'
import type { Document, DocumentQueryDTO } from '@/types/document'

export const useDocumentStore = defineStore('document', () => {
  const documents = ref<Document[]>([])
  const total = ref(0)
  const loading = ref(false)
  const uploadProgress = ref(0)
  const query = reactive<DocumentQueryDTO>({
    category: '',
    keyword: '',
    current: 1,
    size: 10
  })

  async function fetchDocuments() {
    loading.value = true
    try {
      const res = await getDocuments(query) as any
      documents.value = res.records
      total.value = res.total
    } finally {
      loading.value = false
    }
  }

  async function uploadDocument(formData: FormData) {
    loading.value = true
    uploadProgress.value = 0
    try {
      const res = await uploadDocumentApi(formData, (progressEvent) => {
        if (progressEvent.total) {
          uploadProgress.value = Math.round((progressEvent.loaded / progressEvent.total) * 100)
        }
      })
      uploadProgress.value = 0
      await fetchDocuments()
      return res
    } finally {
      loading.value = false
    }
  }

  async function deleteDocument(id: number) {
    await deleteDocumentApi(id)
    await fetchDocuments()
  }

  return {
    documents,
    total,
    loading,
    uploadProgress,
    query,
    fetchDocuments,
    uploadDocument,
    deleteDocument
  }
})
