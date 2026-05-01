import request from '@/utils/request'
import type { SearchRequest, SearchResult } from '@/types/search'

export function semanticSearch(data: SearchRequest) {
  return request.post<SearchResult[]>('/knowledge-qa/search', data)
}

export function hybridSearch(data: SearchRequest) {
  return request.post<SearchResult[]>('/knowledge-qa/search/hybrid', data)
}
