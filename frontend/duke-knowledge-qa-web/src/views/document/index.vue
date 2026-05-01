<template>
  <div class="document-page">
    <!-- 上传区域 -->
    <div class="upload-section">
      <div class="upload-wrapper">
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <h3>上传文档</h3>
        <p class="upload-hint">支持 PDF、Word、TXT、Markdown 格式</p>
        <el-upload
          drag
          action="#"
          :auto-upload="false"
          :http-request="handleUpload"
          accept=".pdf,.doc,.docx,.txt,.md"
          multiple
          :on-change="handleFileChange"
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
          <el-table-column label="操作" width="80" align="center" fixed="right">
            <template #default="{ row }">
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDocumentStore } from '@/stores/documentStore'
import type { Document, DocumentQueryDTO } from '@/types/document'

const docStore = useDocumentStore()

const documents = ref<Document[]>([])
const total = ref(0)
const loading = ref(false)
const uploadProgress = ref(0)
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
  formData.append('category', query.category || '其他')

  loading.value = true
  try {
    await docStore.uploadDocument(formData)
    ElMessage.success('上传成功')
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
  gap: 24px;
  padding: 20px;
}

.upload-section {
  .upload-wrapper {
    background: linear-gradient(135deg, #F0F2F7 0%, #FFFFFF 100%);
    border: 2px dashed #D5D7EB;
    border-radius: 12px;
    padding: 30px;
    text-align: center;
    transition: all 0.3s ease;

    &:hover {
      border-color: #4F6EF7;
      background: linear-gradient(135deg, #EEF1FE 0%, #FFFFFF 100%);
    }

    .upload-icon {
      font-size: 48px;
      color: #4F6EF7;
      margin-bottom: 12px;
    }

    h3 {
      margin: 12px 0 4px 0;
      color: #1F2937;
      font-size: 18px;
    }

    .upload-hint {
      color: #6B7280;
      font-size: 13px;
      margin-bottom: 20px;
    }

    :deep(.el-upload-dragger) {
      background: transparent;
      border: none;
      padding: 0;

      .upload-content {
        .drag-hint {
          color: #374151;
          font-weight: 500;
          margin-bottom: 8px;
          em { color: #4F6EF7; font-style: normal; }
        }

        .file-size-hint {
          color: #9CA3AF;
          font-size: 12px;
        }
      }
    }

    .upload-progress {
      margin-top: 16px;
    }
  }
}

.documents-section {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.section-header {
  padding: 20px 20px 12px;
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
  gap: 12px;
  padding: 16px 20px;
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
  position: relative;

  :deep(.el-table) {
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
  padding: 16px 20px;
  border-top: 1px solid #E5E8F0;
  background: #F9FAFB;
}
</style>
