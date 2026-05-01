<template>
  <div class="page-container">
    <el-card class="search-card">
      <el-input
        v-model="searchQuery"
        placeholder="输入搜索内容..."
        :prefix-icon="Search"
        clearable
        @keyup.enter="handleSearch"
      />

      <div class="search-controls">
        <el-radio-group v-model="searchMode" @change="() => { results = [] }">
          <el-radio-button label="semantic">语义搜索</el-radio-button>
          <el-radio-button label="hybrid">混合搜索</el-radio-button>
        </el-radio-group>

        <el-button type="primary" @click="handleSearch" :loading="loading">
          搜索
        </el-button>
      </div>

      <el-collapse>
        <el-collapse-item title="高级选项" name="advanced">
          <div class="param-group">
            <div>
              <label>分类：</label>
              <el-select v-model="category" placeholder="选择分类" clearable style="width: 100%">
                <el-option label="技术文档" value="技术文档" />
                <el-option label="业务文档" value="业务文档" />
                <el-option label="其他" value="其他" />
              </el-select>
            </div>
            <div style="margin-top: 12px">
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
    </el-card>

    <el-card style="margin-top: 20px">
      <div v-if="loading" style="padding: 40px; text-align: center">
        <el-skeleton :rows="4" animated />
      </div>
      <div v-else-if="results.length" class="results-list">
        <div v-for="(result, idx) in results" :key="idx" class="result-card">
          <div class="result-header">
            <div class="result-title">{{ result.documentTitle }}</div>
            <el-tag type="primary" effect="plain">
              {{ (result.score * 100).toFixed(1) }}%
            </el-tag>
          </div>
          <div class="result-meta">
            <el-tag>{{ result.category }}</el-tag>
            <span style="margin-left: 8px; font-size: 12px; color: #6B7280">
              Chunk {{ result.chunkIndex }}
            </span>
          </div>
          <div class="result-content">{{ truncateText(result.content, 200) }}</div>
        </div>
      </div>
      <div v-else style="text-align: center; padding: 40px; color: #9CA3AF">
        <el-empty description="暂无搜索结果" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useSearchStore } from '@/stores/searchStore'
import type { SearchResult } from '@/types/search'
import { truncateText } from '@/utils/format'

const searchStore = useSearchStore()

const searchQuery = ref('')
const category = ref('')
const topK = ref(10)
const scoreThreshold = ref(0.5)
const loading = ref(false)
const results = ref<SearchResult[]>([])
const searchMode = ref<'semantic' | 'hybrid'>('semantic')

async function handleSearch() {
  if (!searchQuery.value.trim()) {
    ElMessage.warning('请输入搜索内容')
    return
  }

  loading.value = true
  try {
    await searchStore.search({
      query: searchQuery.value,
      topK: topK.value,
      scoreThreshold: scoreThreshold.value,
      category: category.value || undefined
    })
    results.value = searchStore.results
    if (results.value.length === 0) {
      ElMessage.info('未找到匹配的结果')
    }
  } catch (error) {
    ElMessage.error('搜索失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.search-card {
  :deep(.el-input) {
    margin-bottom: 16px;
  }

  .search-controls {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;
  }

  .param-group {
    label {
      display: block;
      margin-bottom: 8px;
      font-weight: 500;
      font-size: 13px;
    }
  }
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 600px;
  overflow-y: auto;
}

.result-card {
  padding: 16px;
  border: 1px solid #E5E8F0;
  border-radius: 8px;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.1);
  }

  .result-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;

    .result-title {
      font-weight: 600;
      color: #1A2340;
      flex: 1;
    }
  }

  .result-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    font-size: 12px;
  }

  .result-content {
    color: #6B7280;
    line-height: 1.6;
    font-size: 13px;
  }
}
</style>
