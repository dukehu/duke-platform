<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-form inline :model="query">
            <el-form-item label="应用名称">
              <el-input v-model="query.appName" placeholder="应用名称" clearable />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadData">搜索</el-button>
              <el-button @click="() => { query.appName = ''; loadData() }">重置</el-button>
            </el-form-item>
          </el-form>
          <div>
          <el-button v-for="btn in headerButtons" :key="btn.buttonCode" type="primary"
              @click="actionMap[btn.buttonCode]?.()">{{ btn.buttonName }}</el-button>
          </div>
        </div>
      </template>
      <el-table :data="tableData" v-loading="loading" border height="calc(100vh - 272px)">
        <el-table-column prop="appName" label="应用名称" width="260" />
        <el-table-column prop="appCode" label="应用编码" width="260" />
        <el-table-column label="状态" width="160">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="260" />
        <el-table-column v-if="rowButtons.length" label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-for="btn in rowButtons" :key="btn.buttonCode" size="small"
              :type="btn.buttonCode.includes('delete') ? 'danger' : 'primary'"
              @click="actionMap[btn.buttonCode]?.(row)">{{ btn.buttonName }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" @change="loadData" style="margin-top:12px;justify-content:flex-end" />
    </el-card>
    <el-dialog :title="isEdit ? '编辑应用' : '新增应用'" v-model="dialogVisible" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="应用名称" prop="appName"><el-input v-model="form.appName" /></el-form-item>
        <el-form-item label="应用编码" prop="appCode"><el-input v-model="form.appCode" :disabled="isEdit" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { getAppPage, createApp, updateApp, deleteApp } from '@/api/app'
import { useMenuButtons } from '@/utils/useMenuButtons'

const { headerButtons, rowButtons } = useMenuButtons()

const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Record<string, unknown>>({ appName: '', appCode: '', status: 1, remark: '' })
const isEdit = computed(() => !!form.id)
const query = reactive({ appName: '', current: 1, size: 10 })
const rules = {
  appName: [{ required: true, message: '请输入应用名称' }],
  appCode: [{ required: true, message: '请输入应用编码' }]
}

const actionMap: Record<string, Function> = {
  'system:app:add': () => handleAdd(),
  'system:app:edit': (row: Record<string, unknown>) => handleEdit(row),
  'system:app:delete': (row: Record<string, unknown>) => handleDelete(row.id as number),
}

async function loadData() {
  loading.value = true
  try {
    const res = await getAppPage(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function handleAdd() {
  Object.assign(form, { id: undefined, appName: '', appCode: '', status: 1, remark: '' })
  dialogVisible.value = true
}

function handleEdit(row: Record<string, unknown>) {
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除该应用？', '提示', { type: 'warning' })
  await deleteApp(id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    isEdit.value ? await updateApp(form) : await createApp(form)
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally { saving.value = false }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: nowrap;
}

:deep(.el-card__header) {
  padding: 12px 16px;
  height: 60px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
  overflow: hidden;
}
</style>
