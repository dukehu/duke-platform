<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <el-form inline :model="query" @submit.prevent="loadData">
          <el-form-item label="操作模块">
            <el-input v-model="query.module" placeholder="操作模块" clearable />
          </el-form-item>
          <el-form-item label="操作人">
            <el-input v-model="query.operatorName" placeholder="操作人" clearable />
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
              start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadData">搜索</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </template>
      <el-table :data="tableData" v-loading="loading" border height="calc(100vh - 272px)">
        <el-table-column prop="module" label="操作模块" width="120" />
        <el-table-column prop="operation" label="操作类型" width="120" />
        <el-table-column prop="requestUrl" label="请求URL" min-width="100" show-overflow-tooltip />
        <el-table-column prop="requestMethod" label="请求方式" width="150" />
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column prop="operatorIp" label="IP地址" width="130" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="120" />
        <el-table-column prop="createTime" label="操作时间" width="160" />
        <el-table-column v-if="rowButtons.length" label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-for="btn in rowButtons" :key="btn.buttonCode" size="small" type="danger"
              @click="actionMap[btn.buttonCode]?.(row)">{{ btn.buttonName }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total"
        layout="total, sizes, prev, pager, next" @change="loadData" style="margin-top:12px;justify-content:flex-end" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLogPage, deleteLog } from '@/api/log'
import { useMenuButtons } from '@/utils/useMenuButtons'

const { rowButtons } = useMenuButtons()

const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const dateRange = ref<string[]>([])
const query = reactive({ module: '', operatorName: '', startTime: '', endTime: '', current: 1, size: 10 })

const actionMap: Record<string, Function> = {
  'system:log:delete': (row: { id: number }) => handleDelete(row.id),
}

async function loadData() {
  if (dateRange.value?.length === 2) { query.startTime = dateRange.value[0]; query.endTime = dateRange.value[1] }
  loading.value = true
  try {
    const res = await getLogPage(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function resetQuery() {
  Object.assign(query, { module: '', operatorName: '', startTime: '', endTime: '', current: 1 })
  dateRange.value = []
  loadData()
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除该日志？', '提示', { type: 'warning' })
  await deleteLog(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
:deep(.el-card__header) {
  padding: 12px 16px;
  height: 60px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
  overflow: hidden;

  .el-form--inline {
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    gap: 0;
  }
}
</style>
