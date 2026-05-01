export interface SearchRequest {
  query: string
  topK?: number
  scoreThreshold?: number
  category?: string
}

export interface SearchResult {
  documentId: number
  documentTitle: string
  chunkIndex: number
  content: string
  score: number
  category: string
}
