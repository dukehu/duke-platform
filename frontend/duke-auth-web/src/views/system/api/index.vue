<template>
  <div class="page-container">
    <div class="api-layout">
      <el-card class="controller-card">
        <template #header>Controller 分组</template>
        <el-tree
          ref="treeRef"
          class="controller-tree"
          :data="treeData"
          :props="{ label: 'label', children: 'children' }"
          :filter-node-method="filterNode"
          highlight-current
          node-key="key"
          @node-click="handleNodeClick"
        />
      </el-card>
      <div class="api-table-area">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-form inline :model="query">
                <el-form-item label="接口名称">
                  <el-input v-model="query.keyword" placeholder="接口名称" clearable />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="loadData">搜索</el-button>
                </el-form-item>
              </el-form>
              <div>
                <el-button v-for="btn in headerButtons" :key="btn.buttonCode" type="primary"
                  :loading="btn.buttonCode === 'system:api:sync' ? syncing : false"
                  @click="actionMap[btn.buttonCode]?.()">{{ btn.buttonName }}</el-button>
              </div>
            </div>
          </template>
          <el-table :data="tableData" v-loading="loading" border height="calc(100vh - 272px)">
            <el-table-column prop="apiName" label="接口名称" min-width="140" />
            <el-table-column prop="apiPath" label="路径" min-width="200" />
            <el-table-column label="方法" width="200">
              <template #default="{ row }">
                <el-tag :type="methodColor(row.apiMethod)" size="small">{{ row.apiMethod }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="权限标识" min-width="180" prop = "permission"/>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-switch :model-value="row.status === 1" @change="toggleStatus(row)" />
              </template>
            </el-table-column>
          </el-table>
          <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total"
            layout="total, sizes, prev, pager, next" @change="loadData" style="margin-top:12px;justify-content:flex-end" />
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { ElTree } from 'element-plus'
import { getApiPage, getControllers, syncApis, updateApiStatus, updateApiPermission } from '@/api/api'
import { useMenuButtons } from '@/utils/useMenuButtons'

const { headerButtons } = useMenuButtons()

const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const syncing = ref(false)
const controllers = ref<Record<string, { controllerClass: string; controllerName: string }[]>>({})
const selectedController = ref('')
const controllerSearch = ref('')
const treeRef = ref<InstanceType<typeof ElTree>>()
const editingId = ref<number | null>(null)
const editingPerm = ref('')
const query = reactive({ keyword: '', controllerClass: '', current: 1, size: 10 })

interface TreeNode { key: string; label: string; children?: TreeNode[] }

const treeData = computed<TreeNode[]>(() => {
  const groups = Object.entries(controllers.value).map(([appId, list]) => ({
    key: `__app__${appId}`,
    label: appId,
    children: list.map(c => ({ key: c.controllerClass, label: c.controllerName }))
  }))
  return [...groups]
})

watch(controllerSearch, val => treeRef.value?.filter(val))

function filterNode(value: string, data: TreeNode) {
  if (!value) return true
  return data.label.toLowerCase().includes(value.toLowerCase())
}

const methodColor = (m: string) => ({ GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }[m] || 'info')

const actionMap: Record<string, Function> = {
  'system:api:sync': () => handleSync(),
}

async function loadData() {
  loading.value = true
  try {
    const res = await getApiPage({ ...query, controllerClass: selectedController.value || undefined })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function handleControllerSelect(key: string) { selectedController.value = key; query.current = 1; loadData() }

function handleNodeClick(node: { key: string; children?: unknown[] }) {
  if (node.children) return // appId 分组节点，不触发筛选
  handleControllerSelect(node.key)
}

async function handleSync() {
  syncing.value = true
  try {
    await syncApis()
    ElMessage.success('同步成功')
    loadData()
    const res = await getControllers()
    controllers.value = res.data
  } finally { syncing.value = false }
}

function startEdit(row: { id: number; permission: string }) { editingId.value = row.id; editingPerm.value = row.permission || '' }

async function savePermission(row: { id: number; permission: string }) {
  if (editingPerm.value !== row.permission) {
    await updateApiPermission(row.id, editingPerm.value)
    row.permission = editingPerm.value
    ElMessage.success('保存成功')
  }
  editingId.value = null
}

async function toggleStatus(row: { id: number; status: number }) {
  const newStatus = row.status === 1 ? 0 : 1
  await updateApiStatus(row.id, newStatus)
  row.status = newStatus
}

onMounted(async () => {
  const res = await getControllers()
  controllers.value = res.data
  loadData()
})
</script>

<style lang="scss" scoped>
.api-layout {
  display: flex;
  gap: 20px;
  align-items: stretch;

  .controller-card {
    width: 210px;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;

    :deep(.el-card__body) {
      padding: 0;
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
  }

  .api-table-area {
    flex: 1;
    min-width: 0;

    :deep(.el-card__header) {
      padding: 12px 16px;
      height: 60px;
      display: flex;
      align-items: center;
      box-sizing: border-box;
      overflow: hidden;
    }
  }
}

.search-box {
  padding: 8px 10px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.controller-tree {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
  min-height: 0;

  :deep(.el-tree-node__content) {
    height: 32px;
    font-size: 13px;
  }

  :deep(.el-tree-node.is-current > .el-tree-node__content) {
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    font-weight: 500;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: nowrap;
  width: 100%;
}
</style>
