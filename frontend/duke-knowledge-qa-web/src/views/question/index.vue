<template>
  <div class="question-page">
    <!-- 左栏：提问 -->
    <div class="left-column">
      <!-- 提问卡片 -->
      <div class="question-card card">
        <div class="card-title">
          <el-icon><EditPen /></el-icon>
          <span>提出您的问题</span>
        </div>
        <el-input
          v-model="questionText"
          type="textarea"
          :rows="10"
          placeholder="请详细描述您的问题..."
          maxlength="1000"
          show-word-limit
          class="question-input"
          style="height: 70vh"
        />

        <!-- 高级参数 -->
        <el-collapse class="advanced-params">
          <el-collapse-item name="advanced">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>高级参数</span>
            </template>
            <div class="param-group">
              <div class="param-item">
                <div class="param-label">Top K <span class="param-value">({{ topK }})</span></div>
                <el-slider v-model="topK" :min="1" :max="20" />
                <div class="param-desc">获取相关文档的数量</div>
              </div>
              <div class="param-item">
                <div class="param-label">相似度阈值 <span class="param-value">({{ scoreThreshold.toFixed(2) }})</span></div>
                <el-slider v-model="scoreThreshold" :min="0" :max="1" :step="0.1" />
                <div class="param-desc">文档相似度最低要求</div>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>

        <el-button
          type="primary"
          @click="submitQuestion"
          :loading="questionLoading"
          size="large"
          class="submit-btn"
        >
          <el-icon><DocumentCopy /></el-icon>
          <span>提交问题</span>
        </el-button>
      </div>
    </div>

    <!-- 右栏：答案 + 参考 + 反馈 -->
    <div class="right-column">
      <!-- 答案卡片 -->
      <div class="answer-card card">
        <div class="card-title">
          <el-icon><DocumentCopy /></el-icon>
          <span>AI 回答</span>
        </div>
        <div class="answer-content">
          <div v-if="questionLoading" class="loading-state">
            <el-skeleton :rows="6" animated />
          </div>
          <div v-else-if="currentAnswer" class="markdown-content" v-html="renderedAnswer" />
          <div v-else class="empty-state">
            <el-icon><DocumentCopy /></el-icon>
            <p>提交问题后，AI 将在这里显示回答</p>
          </div>
        </div>
      </div>

      <!-- 参考来源 -->
      <div v-if="currentAnswer" class="sources-card card">
        <div class="card-title">
          <el-icon><Link /></el-icon>
          <span>参考来源</span>
          <span class="badge">{{ currentAnswer.sourceChunks?.length || 0 }}</span>
        </div>
        <div v-if="currentAnswer.sourceChunks?.length" class="source-list">
          <div v-for="(chunk, idx) in currentAnswer.sourceChunks" :key="idx" class="source-item">
            <div class="source-title">{{ chunk.documentTitle }}</div>
            <div class="source-content">{{ truncateText(chunk.content, 100) }}</div>
            <div class="source-score">
              <span class="score-label">相关度</span>
              <el-progress
                :percentage="Math.round(chunk.score * 100)"
                :color="getScoreColor(chunk.score)"
                :show-text="false"
                style="flex: 1"
              />
              <span class="score-value">{{ (chunk.score * 100).toFixed(0) }}%</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-sources">
          <p>暂无参考来源</p>
        </div>
      </div>

      <!-- 反馈卡片 -->
      <div v-if="currentAnswer" class="feedback-card card">
        <div class="card-title">
          <el-icon><Thumb /></el-icon>
          <span>反馈</span>
        </div>
        <div class="feedback-form">
          <div class="rating-group">
            <label class="rating-label">答案评分</label>
            <el-rate
              v-model="feedbackRating"
              :colors="['#F59E0B', '#F59E0B', '#10B981']"
              size="large"
              allow-half
            />
          </div>
          <el-input
            v-model="feedbackText"
            type="textarea"
            :rows="3"
            placeholder="分享您的想法..."
            maxlength="500"
            show-word-limit
            class="feedback-input"
          />
          <el-button
            type="primary"
            @click="submitFeedback"
            :loading="feedbackLoading"
            class="feedback-btn"
          >
            提交反馈
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import { useQuestionStore } from '@/stores/questionStore'
import { truncateText } from '@/utils/format'

const questionStore = useQuestionStore()

const questionText = ref('')
const topK = ref(10)
const scoreThreshold = ref(0.5)
const questionLoading = ref(false)
const feedbackLoading = ref(false)
const feedbackRating = ref(0)
const feedbackText = ref('')

const currentAnswer = computed(() => questionStore.currentAnswer)
const renderedAnswer = computed(() => {
  if (!currentAnswer.value?.content) return ''
  return marked(currentAnswer.value.content)
})

async function submitQuestion() {
  if (!questionText.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }

  questionLoading.value = true
  try {
    await questionStore.askQuestion({
      content: questionText.value,
      topK: topK.value,
      scoreThreshold: scoreThreshold.value
    })
    ElMessage.success('问题已提交，正在获取答案...')
  } catch (error) {
    ElMessage.error('提交问题失败，请重试')
  } finally {
    questionLoading.value = false
  }
}

async function submitFeedback() {
  if (!feedbackRating.value) {
    ElMessage.warning('请评分')
    return
  }

  feedbackLoading.value = true
  try {
    await questionStore.submitFeedback(feedbackRating.value, feedbackText.value)
    ElMessage.success('感谢您的反馈')
    feedbackRating.value = 0
    feedbackText.value = ''
  } catch (error) {
    ElMessage.error('提交反馈失败，请重试')
  } finally {
    feedbackLoading.value = false
  }
}

function getScoreColor(score: number): string {
  if (score >= 0.8) return '#10B981'
  if (score >= 0.6) return '#F59E0B'
  return '#EF4444'
}
</script>

<style lang="scss" scoped>
.question-page {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 8px 12px;
  height: calc(100vh - 40px);

  .left-column {
    display: flex;
    flex-direction: column;
    gap: 12px;
    min-width: 0;
  }

  .right-column {
    display: flex;
    flex-direction: column;
    gap: 12px;
    min-width: 0;
    overflow: hidden;
  }
}

.card {
  background: white;
  border-radius: 12px;
  border: 1px solid #E5E8F0;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid #E5E8F0;
  background: #F9FAFB;
  font-weight: 600;
  color: #1F2937;
  font-size: 15px;

  :deep(.el-icon) {
    color: #4F6EF7;
    font-size: 16px;
  }

  .badge {
    margin-left: auto;
    background: #EEF1FE;
    color: #4F6EF7;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;
  }
}

// 提问卡片
.question-card {
  flex: 1;

  .question-input {
    padding: 8px 12px;

    :deep(.el-textarea__inner) {
      padding: 6px;
      border-radius: 4px;
      min-height: 160px;
    }
  }

  .advanced-params {
    margin: 0 12px;
    border: 1px solid #E5E8F0;
    border-radius: 8px;

    :deep(.el-collapse-item__header) {
      display: flex;
      align-items: center;
      gap: 8px;
      height: 44px;
      padding: 0 16px;
      font-weight: 500;
      font-size: 14px;

      .el-icon {
        color: #6B7280;
      }
    }

    :deep(.el-collapse-item__content) {
      padding: 16px;
    }
  }

  .param-group {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .param-item {
    .param-label {
      display: flex;
      align-items: center;
      gap: 4px;
      margin-bottom: 8px;
      font-weight: 500;
      font-size: 14px;
      color: #374151;

      .param-value {
        color: #4F6EF7;
        font-size: 13px;
      }
    }

    .param-desc {
      margin-top: 6px;
      font-size: 12px;
      color: #9CA3AF;
    }
  }

  .submit-btn {
    width: calc(100% - 24px);
    margin: 0 12px 8px;
  }
}

// 答案卡片
.answer-card {
  flex: 1;
  min-height: 0;

  .answer-content {
    flex: 1;
    padding: 12px;
    overflow-y: auto;
    min-height: 0;

    .markdown-content {
      :deep(h1) { font-size: 20px; margin: 16px 0 8px; }
      :deep(h2) { font-size: 18px; margin: 14px 0 8px; }
      :deep(h3) { font-size: 16px; margin: 12px 0 8px; }
      :deep(p) { margin: 8px 0; line-height: 1.6; color: #374151; }
      :deep(code) {
        background: #F3F4F6;
        padding: 2px 6px;
        border-radius: 4px;
        font-family: 'Monaco', 'Courier New', monospace;
        font-size: 13px;
        color: #D97706;
      }
      :deep(pre) {
        background: #1F2937;
        color: #E5E7EB;
        padding: 12px;
        border-radius: 8px;
        overflow-x: auto;
        margin: 12px 0;
      }
      :deep(ul, ol) { margin: 8px 0 8px 20px; }
      :deep(li) { margin: 4px 0; }
      :deep(blockquote) {
        border-left: 4px solid #4F6EF7;
        margin: 8px 0;
        padding: 8px 12px;
        background: #EEF1FE;
        border-radius: 4px;
      }
    }

    .loading-state {
      padding: 20px 0;
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 300px;
      color: #9CA3AF;

      :deep(.el-icon) {
        font-size: 48px;
        color: #D1D5DB;
        margin-bottom: 12px;
      }

      p {
        margin: 0;
        font-size: 14px;
      }
    }
  }
}

// 参考来源
.sources-card {
  flex: 0.5;
  min-height: 0;
  display: flex;
  flex-direction: column;

  .source-list {
    padding: 0;
    flex: 1;
    overflow-y: auto;
    min-height: 0;
  }

  .source-item {
    padding: 6px 8px;
    border-bottom: 1px solid #E5E8F0;
    font-size: 11px;

    &:last-child { border-bottom: none; }

    .source-title {
      font-weight: 600;
      font-size: 13px;
      color: #1F2937;
      margin-bottom: 6px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .source-content {
      font-size: 12px;
      color: #6B7280;
      margin-bottom: 8px;
      line-height: 1.5;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .source-score {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 12px;

      .score-label {
        color: #9CA3AF;
      }

      .score-value {
        color: #4F6EF7;
        font-weight: 600;
        min-width: 35px;
        text-align: right;
      }
    }
  }

  .empty-sources {
    padding: 8px 12px;
    text-align: center;
    color: #9CA3AF;
    font-size: 11px;
  }
}

// 反馈卡片
.feedback-card {
  flex: 0.5;
  min-height: 0;

  .feedback-form {
    padding: 8px 12px;
    display: flex;
    flex-direction: column;
    gap: 6px;
    flex: 1;
    overflow-y: auto;

    .rating-group {
      margin-bottom: 4px;

      .rating-label {
        display: block;
        margin-bottom: 4px;
        font-weight: 500;
        font-size: 11px;
        color: #374151;
      }
    }

    .feedback-input {
      margin-bottom: 6px;
      flex: 1;

      :deep(.el-textarea__inner) {
        padding: 4px;
        border-radius: 3px;
        min-height: 50px;
      }
    }

    .feedback-btn {
      width: 100%;
      flex-shrink: 0;
      height: 28px;
      font-size: 12px;
    }
  }
}

// 响应式设计
@media (max-width: 1400px) {
  .question-page {
    grid-template-columns: 1fr 1.1fr;
  }

  .question-card {
    .question-input {
      :deep(.el-textarea__inner) {
        min-height: 280px;
      }
    }
  }
}

@media (max-width: 1024px) {
  .question-page {
    grid-template-columns: 1fr;
    gap: 16px;

    .left-column {
      gap: 16px;
    }

    .right-column {
      gap: 16px;
      max-height: none;
      overflow-y: visible;
      padding-right: 0;
    }
  }
}

@media (max-width: 768px) {
  .question-page {
    padding: 12px;
    gap: 12px;
  }

  .card {
    margin-bottom: 0;
  }

  .left-column,
  .right-column {
    gap: 12px;
  }

  .question-card {
    .submit-btn {
      font-size: 14px;
    }
  }

  .advanced-params {
    :deep(.el-collapse-item__content) {
      padding: 12px;
    }
  }

  .param-group {
    flex-direction: column;
    gap: 12px;
  }

  .source-list {
    max-height: 300px;
  }
}
</style>
