<template>
  <div class="page-container question-layout">
    <div class="left-panel">
      <el-card>
        <template #header>
          <span style="font-weight: 600">提问</span>
        </template>
        <el-input
          v-model="questionText"
          type="textarea"
          :rows="8"
          placeholder="请输入您的问题..."
          maxlength="1000"
          show-word-limit
        />
        <el-collapse style="margin-top: 12px">
          <el-collapse-item title="高级参数" name="advanced">
            <div class="param-group">
              <div>
                <label>Top K ({{ topK }}):</label>
                <el-slider v-model="topK" :min="1" :max="20" />
              </div>
              <div style="margin-top: 12px">
                <label>相似度阈值 ({{ scoreThreshold.toFixed(2) }}):</label>
                <el-slider v-model="scoreThreshold" :min="0" :max="1" :step="0.1" />
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
        <el-button type="primary" @click="submitQuestion" :loading="questionLoading" style="width: 100%; margin-top: 12px">
          提交问题
        </el-button>
      </el-card>

      <el-card style="margin-top: 20px">
        <template #header>
          <span style="font-weight: 600">答案</span>
        </template>
        <div v-if="questionLoading" style="padding: 20px; text-align: center">
          <el-skeleton :rows="5" animated />
        </div>
        <div v-else-if="currentAnswer" class="markdown-content" v-html="renderedAnswer" />
        <div v-else style="text-align: center; color: #9CA3AF; padding: 20px">
          提交问题后，答案将显示在这里
        </div>
      </el-card>
    </div>

    <div class="right-panel">
      <el-card v-if="currentAnswer">
        <template #header>
          <span style="font-weight: 600">参考来源 ({{ currentAnswer.sourceChunks?.length || 0 }})</span>
        </template>
        <div v-if="currentAnswer.sourceChunks?.length" class="source-list">
          <div v-for="(chunk, idx) in currentAnswer.sourceChunks" :key="idx" class="source-item">
            <div class="source-title">{{ chunk.documentTitle }}</div>
            <div class="source-content">{{ truncateText(chunk.content, 120) }}</div>
            <el-progress :percentage="Math.round(chunk.score * 100)" :color="getScoreColor(chunk.score)" />
          </div>
        </div>
      </el-card>

      <el-card v-if="currentAnswer" style="margin-top: 20px">
        <template #header>
          <span style="font-weight: 600">反馈</span>
        </template>
        <div style="margin-bottom: 12px">
          <label style="display: block; margin-bottom: 8px; font-weight: 500">答案评分：</label>
          <el-rate v-model="feedbackRating" :colors="['#F59E0B', '#F59E0B', '#10B981']" />
        </div>
        <el-input
          v-model="feedbackText"
          type="textarea"
          :rows="4"
          placeholder="请输入您的反馈意见..."
          maxlength="500"
          show-word-limit
        />
        <el-button type="primary" @click="submitFeedback" :loading="feedbackLoading" style="width: 100%; margin-top: 12px">
          提交反馈
        </el-button>
      </el-card>
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
.question-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;

  .left-panel {
    flex: 1;
    min-width: 0;
  }

  .right-panel {
    width: 380px;
    flex-shrink: 0;
  }
}

.param-group > div {
  label {
    display: block;
    margin-bottom: 8px;
    font-weight: 500;
    font-size: 13px;
  }
}

.source-list {
  max-height: 400px;
  overflow-y: auto;
}

.source-item {
  padding: 12px;
  border: 1px solid #E5E8F0;
  border-radius: 8px;
  margin-bottom: 12px;

  &:last-child { margin-bottom: 0; }

  .source-title {
    font-weight: 600;
    margin-bottom: 8px;
    color: #1A2340;
  }

  .source-content {
    font-size: 12px;
    color: #6B7280;
    margin-bottom: 8px;
    line-height: 1.5;
  }
}

@media (max-width: 1200px) {
  .question-layout {
    flex-direction: column;

    .right-panel { width: 100%; }
  }
}
</style>
