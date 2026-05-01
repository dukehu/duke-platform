import { defineStore } from 'pinia'
import { ref } from 'vue'
import { semanticSearch as semanticSearchApi, hybridSearch as hybridSearchApi } from '@/api/search'
import type { SearchRequest, SearchResult } from '@/types/search'

export const useSearchStore = defineStore('search', () => {
  const results = ref<SearchResult[]>([])
  const loading = ref(false)
  const mode = ref<'semantic' | 'hybrid'>('semantic')

  async function search(data: SearchRequest) {
    loading.value = true
    try {
      const res = mode.value === 'semantic'
        ? (await semanticSearchApi(data)) as any
        : (await hybridSearchApi(data)) as any
      results.value = res
    } finally {
      loading.value = false
    }
  }

  function setMode(newMode: 'semantic' | 'hybrid') {
    mode.value = newMode
  }

  function clearResults() {
    results.value = []
  }

  return {
    results,
    loading,
    mode,
    search,
    setMode,
    clearResults
  }
})
