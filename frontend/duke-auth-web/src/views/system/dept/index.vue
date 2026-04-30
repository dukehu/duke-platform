<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>部门管理</span>
          <div>
            <el-button v-for="btn in headerButtons" :key="btn.buttonCode" type="primary"
              @click="actionMap[btn.buttonCode]?.()">{{ btn.buttonName }}</el-button>
          </div>
        </div>
      </template>
      <el-table :data="treeData" v-loading="loading" row-key="id" border default-expand-all
        height="calc(100vh - 210px)">
        <el-table-column prop="deptName" label="部门名称" min-width="200" />
        <el-table-column prop="deptCode" label="部门编码" width="140" />
        <el-table-column prop="leader" label="负责人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="rowButtons.length" label="操作" min-width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-for="btn in rowButtons" :key="btn.buttonCode" size="small"
              :type="btn.buttonCode.includes('delete') ? 'danger' : 'primary'"
              @click="actionMap[btn.buttonCode]?.(row)">{{ btn.buttonName }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <DeptDialog v-model="dialogVisible" :data="currentDept" :tree-data="treeData" @success="loadTree" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeptTree, deleteDept } from '@/api/dept'
import DeptDialog from './components/DeptDialog.vue'
import type { SysDept } from '@/types/dept'
import { useMenuButtons } from '@/utils/useMenuButtons'

const { headerButtons, rowButtons } = useMenuButtons()

const treeData = ref<SysDept[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const currentDept = ref<Partial<SysDept>>({})

const actionMap: Record<string, Function> = {
  'system:dept:add': (data?: SysDept) => handleAdd(data?.id ?? 0),
  'system:dept:edit': (data: SysDept) => handleEdit(data),
  'system:dept:delete': (data: SysDept) => handleDelete(data.id!),
}

async function loadTree() {
  loading.value = true
  try {
    const res = await getDeptTree()
    treeData.value = res.data
  } finally { loading.value = false }
}

function handleAdd(parentId: number) { currentDept.value = { parentId }; dialogVisible.value = true }
function handleEdit(data: SysDept) { currentDept.value = { ...data }; dialogVisible.value = true }

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除该部门？', '提示', { type: 'warning' })
  await deleteDept(id)
  ElMessage.success('删除成功')
  loadTree()
}

onMounted(loadTree)
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

:deep(.el-card__header) {
  padding: 12px 16px;
  height: 60px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
}
</style>
