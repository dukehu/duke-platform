<template>
  <div class="page-container">
    <el-card class="upload-card">
      <template #header>
        <span style="font-weight: 600">文档上传</span>
      </template>
      <el-upload
        drag
        action="#"
        :auto-upload="false"
        :http-request="handleUpload"
        accept=".pdf,.doc,.docx,.txt,.md"
        multiple
        :on-change="handleFileChange"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 PDF、Word、TXT、Markdown 格式，单个文件不超过 100MB</div>
        </template>
      </el-upload>
      <el-progress v-if="uploadProgress > 0 && uploadProgress < 100" :percentage="uploadProgress" style="margin-top: 12px" />
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <el-form inline :model="query" @submit.prevent="loadData">
            <el-form-item label="分类">
              <el-select v-model="query.category" placeholder="选择分类" clearable style="width: 120px">
                <el-option label="技术文档" value="技术文档" />
                <el-option label="业务文档" value="业务文档" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
            <el-form-item label="关键词">
              <el-input v-model="query.keyword" placeholder="搜索文档" clearable style="width: 200px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadData">搜索</el-button>
              <el-button @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </template>

      <el-table :data="documents" v-loading="loading" border height="calc(100vh - 400px)">
        <el-table-column prop="title" label="文档名称" width="200" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="标签" width="120">
          <template #default="{ row }">
            <el-tag v-for="tag in row.tags" :key="tag" style="margin-right: 4px">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="(getStatusType(row.status) as any)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="类型" width="80" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="100">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @change="loadData"
        style="margin-top: 12px; justify-content: flex-end"
      />
    </el-card>
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
.upload-card {
  :deep(.el-upload-dragger) {
    width: 100%;
    height: 200px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  width: 100%;
  flex-wrap: nowrap;

  > div { flex: 1; }
}
</style>
