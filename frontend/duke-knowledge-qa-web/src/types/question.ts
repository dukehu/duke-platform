export interface Question {
  id: number
  userId: number
  content: string
  status: 'PENDING' | 'ANSWERED' | 'ARCHIVED'
  createTime: string
}

export interface SourceChunk {
  documentId: number
  documentTitle: string
  chunkIndex: number
  content: string
  score: number
}

export interface Answer {
  id: number
  questionId: number
  content: string
  sourceChunks: SourceChunk[]
  model: string
  rating: number
  feedback: string
}

export interface QuestionRequest {
  content: string
  topK?: number
  scoreThreshold?: number
}

export interface FeedbackRequest {
  rating: number
  feedback: string
}
