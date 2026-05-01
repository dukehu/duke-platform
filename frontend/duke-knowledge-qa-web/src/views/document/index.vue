<template>
  <div class="document-page">
    <!-- 上传区域 -->
    <div class="upload-section">
      <div class="upload-wrapper">
        <div class="upload-header">
          <div class="title-section">
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <h3>上传文档</h3>
          </div>
        </div>

        <div class="upload-options">
          <div class="option-group">
            <label>分类</label>
            <el-select
              v-model="uploadCategory"
              placeholder="选择分类"
              size="small"
              class="category-selector"
            >
              <el-option label="技术文档" value="技术文档" />
              <el-option label="业务文档" value="业务文档" />
              <el-option label="其他" value="其他" />
            </el-select>
          </div>

          <div class="option-group">
            <label>标签</label>
            <el-input
              v-model="uploadTags"
              placeholder="用逗号分隔，如：AI,LLM"
              size="small"
              class="tags-input"
            />
          </div>
        </div>

        <p class="upload-hint">支持 PDF、Word、TXT、Markdown 格式</p>

        <el-upload
          drag
          action="#"
          :auto-upload="true"
          :http-request="handleUpload"
          accept=".pdf,.doc,.docx,.txt,.md"
          multiple
        >
          <div class="upload-content">
            <div class="drag-hint">拖放文件到此，或<em>点击上传</em></div>
            <div class="file-size-hint">单个文件不超过 100MB</div>
          </div>
        </el-upload>
        <el-progress
          v-if="uploadProgress > 0 && uploadProgress < 100"
          :percentage="uploadProgress"
          class="upload-progress"
        />
      </div>
    </div>

    <!-- 文档列表 -->
    <div class="documents-section">
      <div class="section-header">
        <h3>文档库</h3>
      </div>

      <!-- 搜索过滤 -->
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索文档名称..."
          clearable
          class="search-input"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="query.category"
          placeholder="选择分类"
          clearable
          class="category-select"
        >
          <el-option label="技术文档" value="技术文档" />
          <el-option label="业务文档" value="业务文档" />
          <el-option label="其他" value="其他" />
        </el-select>
        <el-button type="primary" @click="loadData" class="search-btn">搜索</el-button>
        <el-button @click="resetQuery" class="reset-btn">重置</el-button>
      </div>

      <!-- 文档表格 -->
      <div class="table-container" v-loading="loading">
        <el-table :data="documents" stripe>
          <el-table-column prop="title" label="文档名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="category" label="分类" width="100" align="center" />
          <el-table-column label="标签" min-width="140">
            <template #default="{ row }">
              <div class="tag-group">
                <el-tag v-for="tag in row.tags" :key="tag" type="info" size="small">{{ tag }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="(getStatusType(row.status) as any)" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="fileType" label="类型" width="70" align="center" />
          <el-table-column prop="createTime" label="创建时间" width="160" align="center" show-overflow-tooltip />
          <el-table-column label="操作" width="140" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                type="primary"
                size="small"
                text
                @click="handlePreview(row)"
              >
                预览
              </el-button>
              <el-button
                type="danger"
                size="small"
                text
                @click="handleDelete(row.id)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="query.current"
            v-model:page-size="query.size"
            :page-sizes="[10, 20, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @change="loadData"
          />
        </div>
      </div>
    </div>

    <!-- 文件预览抽屉 -->
    <el-drawer
      v-model="previewVisible"
      :title="previewDoc?.title"
      direction="rtl"
      size="50%"
      destroy-on-close
    >
      <!-- PDF -->
      <iframe
        v-if="previewDoc?.fileType === 'pdf'"
        :src="previewDoc.fileUrl"
        style="width: 100%; height: calc(100vh - 60px); border: none"
      />
      <!-- TXT / MD -->
      <div
        v-else-if="['txt', 'md'].includes(previewDoc?.fileType ?? '')"
        style="height: calc(100vh - 60px); overflow: auto; padding: 16px"
      >
        <div
          v-if="previewDoc?.fileType === 'md'"
          v-html="previewHtml"
          class="markdown-body"
        />
        <pre v-else style="white-space: pre-wrap; margin: 0">{{ previewText }}</pre>
      </div>
      <!-- DOCX / DOC -->
      <div
        v-else-if="['docx', 'doc'].includes(previewDoc?.fileType ?? '')"
        ref="docxContainer"
        style="height: calc(100vh - 60px); overflow: auto; padding: 16px; background: #fff"
      />
      <!-- 未知类型兜底 -->
      <div v-else style="padding: 40px; text-align: center; color: #999">
        暂不支持该格式预览
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import { renderAsync } from 'docx-preview'
import { useDocumentStore } from '@/stores/documentStore'
import type { Document, DocumentQueryDTO } from '@/types/document'

const docStore = useDocumentStore()

const documents = ref<Document[]>([])
const total = ref(0)
const loading = ref(false)
const uploadProgress = ref(0)
const uploadCategory = ref('技术文档')
const uploadTags = ref('')
const previewVisible = ref(false)
const previewDoc = ref<Document | null>(null)
const previewText = ref('')
const previewHtml = ref('')
const docxContainer = ref<HTMLElement | null>(null)
const query = reactive<DocumentQueryDTO>({
  category: '',
  keyword: '',
  current: 1,
  size: 10
})

async function loadData() {
  loading.value = true
  try {
    await docStore.fetchDocuments()
    documents.value = docStore.documents
    total.value = docStore.total
  } finally {
    loading.value = false
  }
}

async function handleUpload(options: any) {
  const formData = new FormData()
  formData.append('file', options.file)
  formData.append('category', uploadCategory.value || '其他')

  // 将标签字符串转换为数组
  const tags = uploadTags.value
    .split(',')
    .map(tag => tag.trim())
    .filter(tag => tag.length > 0)
  formData.append('tags', JSON.stringify(tags))

  loading.value = true
  try {
    await docStore.uploadDocument(formData)
    ElMessage.success('上传成功')
    uploadTags.value = '' // 清空标签
    await loadData()
  } catch (error) {
    ElMessage.error('上传失败，请重试')
  } finally {
    loading.value = false
  }
}

function handleFileChange() {
  uploadProgress.value = 0
}

async function handlePreview(row: Document) {
  previewDoc.value = row
  previewText.value = ''
  previewHtml.value = ''
  previewVisible.value = true

  const type = (row.fileType || '').toLowerCase()
  console.log('预览文件类型:', type, '完整数据:', row)

  if (type === 'txt') {
    try {
      const res = await fetch(row.fileUrl)
      previewText.value = await res.text()
    } catch (error) {
      ElMessage.error('加载文本文件失败: ' + (error as any).message)
    }
  } else if (type === 'md') {
    try {
      const res = await fetch(row.fileUrl)
      const text = await res.text()
      previewHtml.value = await marked.parse(text)
    } catch (error) {
      ElMessage.error('加载 Markdown 文件失败: ' + (error as any).message)
    }
  } else if (type === 'docx' || type === 'doc') {
    try {
      await nextTick()
      const res = await fetch(row.fileUrl)
      const buffer = await res.arrayBuffer()
      if (docxContainer.value) {
        await renderAsync(buffer, docxContainer.value)
      }
    } catch (error) {
      ElMessage.error('加载 Word 文件失败: ' + (error as any).message)
    }
  } else if (type === 'pdf') {
    // PDF 直接用 iframe，无需处理
  } else {
    ElMessage.warning('暂不支持此格式预览: ' + type)
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该文档？', '提示', { type: 'warning' })
    await docStore.deleteDocument(id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (error) {
    // 用户取消或出错
  }
}

function resetQuery() {
  query.category = ''
  query.keyword = ''
  query.current = 1
  loadData()
}

function getStatusType(status: string): string {
  const statusMap: Record<string, string> = {
    'DRAFT': 'info',
    'PUBLISHED': 'success',
    'ARCHIVED': 'warning'
  }
  return statusMap[status] || 'info'
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.document-page {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 12px;
  height: calc(100vh - 40px);
}

.upload-section {
  flex-shrink: 0;

  .upload-wrapper {
    background: linear-gradient(135deg, #F0F2F7 0%, #FFFFFF 100%);
    border: 2px dashed #D5D7EB;
    border-radius: 12px;
    padding: 8px 12px;
    text-align: center;
    transition: all 0.3s ease;

    &:hover {
      border-color: #4F6EF7;
      background: linear-gradient(135deg, #EEF1FE 0%, #FFFFFF 100%);
    }

    .upload-header {
      display: flex;
      align-items: center;
      margin-bottom: 12px;

      .title-section {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .upload-icon {
        font-size: 28px;
        color: #4F6EF7;
      }

      h3 {
        margin: 0;
        color: #1F2937;
        font-size: 15px;
      }
    }

    .upload-options {
      display: flex;
      gap: 16px;
      margin-bottom: 12px;
      padding: 10px 12px;
      background: rgba(79, 110, 247, 0.05);
      border-radius: 8px;

      .option-group {
        display: flex;
        align-items: center;
        gap: 8px;
        flex: 1;
        min-width: 0;

        label {
          color: #4F6EF7;
          font-size: 13px;
          font-weight: 600;
          white-space: nowrap;
          margin: 0;
        }

        :deep(.el-input__wrapper) {
          background: white;
          border-radius: 6px;
          padding: 4px 10px !important;
          border: 1px solid #E5E8F0;
          transition: all 0.2s;

          &:hover {
            border-color: #4F6EF7;
          }

          &:focus-within {
            border-color: #4F6EF7;
            box-shadow: 0 0 0 2px rgba(79, 110, 247, 0.1);
          }
        }

        :deep(.el-select__wrapper) {
          background: white;
          border-radius: 6px;
          padding: 0 !important;
          border: 1px solid #E5E8F0;
          transition: all 0.2s;

          &:hover {
            border-color: #4F6EF7;
          }

          &:focus-within {
            border-color: #4F6EF7;
            box-shadow: 0 0 0 2px rgba(79, 110, 247, 0.1);
          }
        }

        .category-selector,
        .tags-input {
          width: 100%;
          min-width: 0;

          :deep(.el-input__inner),
          :deep(.el-select-v2__combobox-input) {
            font-size: 13px;
            height: 32px;
          }
        }
      }
    }

    .upload-hint {
      color: #6B7280;
      font-size: 12px;
      margin-bottom: 12px;
    }

    :deep(.el-upload-dragger) {
      background: transparent;
      border: none;
      padding: 8px 0;

      .upload-content {
        .drag-hint {
          color: #374151;
          font-weight: 500;
          font-size: 13px;
          margin-bottom: 4px;
          em { color: #4F6EF7; font-style: normal; }
        }

        .file-size-hint {
          color: #9CA3AF;
          font-size: 11px;
        }
      }
    }

    .upload-progress {
      margin-top: 8px;
    }
  }
}

.documents-section {
  flex: 1;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.section-header {
  padding: 8px 12px 6px;
  border-bottom: 1px solid #E5E8F0;

  h3 {
    margin: 0;
    color: #1F2937;
    font-size: 16px;
  }
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #F9FAFB;
  border-bottom: 1px solid #E5E8F0;

  .search-input {
    flex: 1;
    min-width: 200px;
    max-width: 350px;
  }

  .category-select {
    width: 140px;
  }

  .search-btn {
    min-width: 70px;
  }

  .reset-btn {
    min-width: 70px;
  }
}

.table-container {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;

  :deep(.el-table) {
    flex: 1;
    border: none;
    --el-table-border-color: #E5E8F0;
    --el-table-header-bg-color: #F9FAFB;

    .el-table__header th {
      background: #F9FAFB;
      color: #374151;
      font-weight: 600;
    }

    .el-table__row {
      &:hover > td {
        background-color: #F9FAFB !important;
      }
    }
  }

  .tag-group {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;

    :deep(.el-tag) {
      margin-right: 0;
    }
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 8px 12px;
  border-top: 1px solid #E5E8F0;
  background: #F9FAFB;
  flex-shrink: 0;
}

@media (max-width: 1024px) {
  .document-page {
    height: auto;
  }

  .filter-bar {
    flex-wrap: wrap;
    gap: 8px;

    .search-input {
      max-width: none;
      min-width: 150px;
    }

    .category-select {
      width: 120px;
    }

    .search-btn,
    .reset-btn {
      min-width: 60px;
    }
  }
}

.markdown-body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  line-height: 1.6;
  color: #333;

  :deep(h1) { font-size: 2em; margin: 0.67em 0; border-bottom: 1px solid #eee; padding-bottom: 0.3em; }
  :deep(h2) { font-size: 1.5em; margin: 0.75em 0; border-bottom: 1px solid #eee; padding-bottom: 0.3em; }
  :deep(h3) { font-size: 1.25em; margin: 0.83em 0; }
  :deep(h4) { font-size: 1em; margin: 1em 0; }
  :deep(h5) { font-size: 0.875em; margin: 1.17em 0; }
  :deep(h6) { font-size: 0.75em; margin: 1.33em 0; color: #666; }

  :deep(p) { margin: 0.5em 0; }
  :deep(pre) { background: #f6f8fa; border: 1px solid #ddd; border-radius: 3px; padding: 1em; overflow: auto; }
  :deep(code) { background: #f6f8fa; padding: 2px 4px; border-radius: 3px; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; }
  :deep(blockquote) { border-left: 4px solid #ddd; margin: 0; padding-left: 1em; color: #666; }
  :deep(ul) { margin: 0.5em 0; padding-left: 2em; }
  :deep(ol) { margin: 0.5em 0; padding-left: 2em; }
  :deep(li) { margin: 0.25em 0; }
  :deep(a) { color: #4F6EF7; text-decoration: none; &:hover { text-decoration: underline; } }
  :deep(table) { border-collapse: collapse; width: 100%; margin: 0.5em 0; }
  :deep(th), :deep(td) { border: 1px solid #ddd; padding: 0.5em; }
  :deep(th) { background: #f6f8fa; }
}

@media (max-width: 768px) {
  .document-page {
    padding: 12px;
    gap: 12px;
  }

  .section-header {
    padding: 16px 16px 8px;
  }

  .filter-bar {
    padding: 12px 16px;
    gap: 8px;

    .search-input {
      min-width: 100%;
    }

    .category-select,
    .search-btn,
    .reset-btn {
      width: 100%;
    }
  }

  .pagination-wrapper {
    padding: 12px 16px;
    justify-content: center;
  }
}
</style>
