import request from '@/utils/request'
import type { Answer, QuestionRequest, FeedbackRequest } from '@/types/question'

export function submitQuestion(data: QuestionRequest) {
  return request.post<{ questionId: number; status: string }>('/knowledge-qa/questions', data)
}

export function getAnswer(questionId: number) {
  return request.get<Answer>(`/knowledge-qa/questions/${questionId}/answer`)
}

export function submitFeedback(answerId: number, data: FeedbackRequest) {
  return request.post<void>(`/knowledge-qa/answers/${answerId}/feedback`, data)
}
