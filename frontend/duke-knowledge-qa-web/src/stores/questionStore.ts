import { defineStore } from 'pinia'
import { ref } from 'vue'
import { submitQuestion as submitQuestionApi, getAnswer as getAnswerApi, submitFeedback as submitFeedbackApi } from '@/api/question'
import type { Question, Answer, QuestionRequest, FeedbackRequest } from '@/types/question'

export const useQuestionStore = defineStore('question', () => {
  const currentQuestion = ref<Question | null>(null)
  const currentAnswer = ref<Answer | null>(null)
  const loading = ref(false)
  const polling = ref(false)

  async function askQuestion(data: QuestionRequest) {
    loading.value = true
    try {
      const res = await submitQuestionApi(data) as any
      const questionId = res.questionId
      currentQuestion.value = {
        id: questionId,
        userId: 0,
        content: data.content,
        status: 'PENDING',
        createTime: new Date().toISOString()
      }
      await pollAnswer(questionId)
    } finally {
      loading.value = false
    }
  }

  async function pollAnswer(questionId: number) {
    polling.value = true
    let attempts = 0
    const maxAttempts = 30

    while (attempts < maxAttempts) {
      try {
        const res = await getAnswerApi(questionId) as any
        currentAnswer.value = res
        if (currentQuestion.value) {
          currentQuestion.value.status = 'ANSWERED'
        }
        break
      } catch (error) {
        attempts++
        if (attempts < maxAttempts) {
          await new Promise(resolve => setTimeout(resolve, 1000))
        }
      }
    }

    polling.value = false
  }

  async function submitFeedback(rating: number, feedback: string) {
    if (!currentAnswer.value) return
    const data: FeedbackRequest = { rating, feedback }
    await submitFeedbackApi(currentAnswer.value.id, data)
    if (currentAnswer.value) {
      currentAnswer.value.rating = rating
      currentAnswer.value.feedback = feedback
    }
  }

  return {
    currentQuestion,
    currentAnswer,
    loading,
    polling,
    askQuestion,
    submitFeedback
  }
})
