<template>
  <div class="search-page">
    <!-- 搜索区域 -->
    <div class="search-section">
      <div class="search-box">
        <el-input
          v-model="searchQuery"
          placeholder="搜索文档、关键词、技术术语..."
          :prefix-icon="Search"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
        />
        <div class="search-toolbar">
          <div class="mode-group">
            <el-radio-group v-model="searchMode" @change="() => { results = [] }" size="large">
              <el-radio-button label="semantic" class="mode-btn">
                <el-icon><Sparkles /></el-icon>
                语义搜索
              </el-radio-button>
              <el-radio-button label="hybrid" class="mode-btn">
                <el-icon><Connection /></el-icon>
                混合搜索
              </el-radio-button>
            </el-radio-group>
          </div>
          <el-button
            type="primary"
            @click="handleSearch"
            :loading="loading"
            size="large"
            class="search-btn"
          >
            <el-icon><Search /></el-icon>
            <span>搜索</span>
          </el-button>
        </div>

        <!-- 高级选项 -->
        <el-collapse class="advanced-options">
          <el-collapse-item name="advanced">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>高级选项</span>
            </template>
            <div class="params-grid">
              <div class="param-item">
                <label class="param-label">分类</label>
                <el-select v-model="category" placeholder="选择分类" clearable>
                  <el-option label="技术文档" value="技术文档" />
                  <el-option label="业务文档" value="业务文档" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </div>
              <div class="param-item">
                <label class="param-label">Top K <span class="value">({{ topK }})</span></label>
                <el-slider v-model="topK" :min="1" :max="20" />
              </div>
              <div class="param-item">
                <label class="param-label">相似度阈值 <span class="value">({{ scoreThreshold.toFixed(2) }})</span></label>
                <el-slider v-model="scoreThreshold" :min="0" :max="1" :step="0.1" />
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </div>

    <!-- 搜索结果 -->
    <div class="results-section">
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="4" animated />
      </div>
      <div v-else-if="results.length" class="results-container">
        <div class="results-header">
          <span class="result-count">找到 <strong>{{ results.length }}</strong> 条结果</span>
        </div>
        <div class="results-list">
          <div v-for="(result, idx) in results" :key="idx" class="result-card">
            <div class="result-header">
              <h3 class="result-title">{{ result.documentTitle }}</h3>
              <div class="result-score" :style="{ backgroundColor: getScoreColor(result.score) }">
                {{ (result.score * 100).toFixed(0) }}%
              </div>
            </div>
            <div class="result-meta">
              <el-tag type="info" size="small">{{ result.category }}</el-tag>
              <span class="chunk-info">Chunk {{ result.chunkIndex }}</span>
            </div>
            <p class="result-content">{{ truncateText(result.content, 180) }}</p>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">
        <el-empty description="还没有搜索结果">
          <template #default>
            <p class="empty-hint">输入关键词开始搜索</p>
          </template>
        </el-empty>
      </div>
    </div>
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

function getScoreColor(score: number): string {
  if (score >= 0.8) return '#10B981'
  if (score >= 0.6) return '#F59E0B'
  return '#EF4444'
}
</script>

<style lang="scss" scoped>
.search-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 20px;
}

// 搜索区域
.search-section {
  .search-box {
    background: linear-gradient(135deg, #F0F2F7 0%, #FFFFFF 100%);
    border: 1px solid #D5D7EB;
    border-radius: 12px;
    padding: 24px;
    box-shadow: 0 2px 8px rgba(79, 110, 247, 0.08);

    .search-input {
      margin-bottom: 16px;

      :deep(.el-input__wrapper) {
        background: white;
        border-radius: 8px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
      }

      :deep(.el-input__inner) {
        font-size: 16px;
      }
    }

    .search-toolbar {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 12px;
      flex-wrap: wrap;

      .mode-group {
        flex: 1;
        min-width: 300px;

        :deep(.el-radio-button__inner) {
          display: flex;
          align-items: center;
          gap: 6px;
        }

        .mode-btn {
          :deep(.el-icon) {
            font-size: 16px;
          }
        }
      }

      .search-btn {
        min-width: 120px;
        height: 40px;
      }
    }

    .advanced-options {
      border: 1px solid #E5E8F0;
      border-radius: 8px;
      background: white;

      :deep(.el-collapse-item__header) {
        display: flex;
        align-items: center;
        gap: 8px;
        height: 44px;
        padding: 0 16px;
        font-weight: 500;

        .el-icon {
          color: #6B7280;
        }
      }

      :deep(.el-collapse-item__content) {
        padding: 16px;
      }

      .params-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
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

          .value {
            color: #4F6EF7;
            font-size: 13px;
          }
        }

        :deep(.el-select) {
          width: 100%;
        }
      }
    }
  }
}

// 结果区域
.results-section {
  min-height: 300px;
}

.loading-state {
  background: white;
  border-radius: 12px;
  padding: 40px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.results-container {
  .results-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
    padding: 12px 16px;
    background: #F9FAFB;
    border-radius: 8px;

    .result-count {
      font-size: 14px;
      color: #6B7280;

      strong {
        color: #4F6EF7;
        font-weight: 600;
      }
    }
  }

  .results-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .result-card {
    background: white;
    border: 1px solid #E5E8F0;
    border-radius: 10px;
    padding: 16px;
    transition: all 0.3s ease;
    cursor: pointer;

    &:hover {
      border-color: #4F6EF7;
      box-shadow: 0 4px 16px rgba(79, 110, 247, 0.12);
    }

    .result-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 12px;
      margin-bottom: 12px;

      .result-title {
        flex: 1;
        font-weight: 600;
        font-size: 15px;
        color: #1F2937;
        margin: 0;
        line-height: 1.5;
      }

      .result-score {
        flex-shrink: 0;
        padding: 4px 10px;
        border-radius: 6px;
        color: white;
        font-weight: 600;
        font-size: 12px;
        white-space: nowrap;
      }
    }

    .result-meta {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 10px;

      .chunk-info {
        font-size: 12px;
        color: #9CA3AF;
      }
    }

    .result-content {
      margin: 0;
      font-size: 13px;
      color: #6B7280;
      line-height: 1.6;
    }
  }
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background: white;
  border-radius: 12px;

  :deep(.el-empty) {
    width: 100%;
  }

  .empty-hint {
    color: #9CA3AF;
    font-size: 14px;
    margin: 0;
  }
}

// 工具函数
@function getScoreColor($score) {
  @if $score >= 0.8 {
    @return #10B981;
  } @else if $score >= 0.6 {
    @return #F59E0B;
  } @else {
    @return #EF4444;
  }
}

@media (max-width: 768px) {
  .search-page {
    padding: 12px;
    gap: 16px;
  }

  .search-section .search-box {
    padding: 16px;

    .search-toolbar {
      flex-direction: column;

      .mode-group {
        width: 100%;
      }

      .search-btn {
        width: 100%;
      }
    }
  }

  .params-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
