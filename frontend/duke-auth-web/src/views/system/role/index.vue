<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-form inline :model="query" @submit.prevent="loadData">
            <el-form-item label="角色名称">
              <el-input v-model="query.roleName" placeholder="角色名称" clearable />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadData">搜索</el-button>
              <el-button @click="() => { query.roleName = ''; loadData() }">重置</el-button>
            </el-form-item>
          </el-form>
          <div>
            <el-button v-for="btn in headerButtons" :key="btn.buttonCode" type="primary"
              @click="actionMap[btn.buttonCode]?.()">{{ btn.buttonName }}</el-button>
          </div>
        </div>
      </template>
      <el-table :data="tableData" v-loading="loading" border height="calc(100vh - 272px)">
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column prop="roleCode" label="角色编码" />
        <el-table-column label="数据权限">
          <template #default="{ row }">{{ dataScopeLabel(row.dataScope) }}</template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="230" />
        <el-table-column v-if="rowButtons.length" label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-for="btn in rowButtons" :key="btn.buttonCode" size="small"
              :type="btn.buttonCode.includes('delete') ? 'danger' : 'primary'"
              @click="actionMap[btn.buttonCode]?.(row)">{{ btn.buttonName }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total"
        layout="total, sizes, prev, pager, next" @change="loadData" style="margin-top:12px;justify-content:flex-end" />
    </el-card>
    <RoleDialog v-model="dialogVisible" :data="currentRole" @success="loadData" />
    <PermissionDialog v-model="permDialogVisible" :role="currentRole" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRolePage, deleteRole } from '@/api/role'
import RoleDialog from './components/RoleDialog.vue'
import PermissionDialog from './components/PermissionDialog.vue'
import type { SysRole } from '@/types/role'
import { useMenuButtons } from '@/utils/useMenuButtons'

const { headerButtons, rowButtons } = useMenuButtons()

const tableData = ref<SysRole[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const permDialogVisible = ref(false)
const currentRole = ref<Partial<SysRole>>({})
const query = reactive({ roleName: '', current: 1, size: 10 })

const scopeMap: Record<number, string> = { 1: '全部', 2: '自定义', 3: '本部门', 4: '本部门及下级', 5: '仅本人' }
const dataScopeLabel = (v: number) => scopeMap[v] || '-'

const actionMap: Record<string, Function> = {
  'system:role:add': () => handleAdd(),
  'system:role:edit': (row: SysRole) => handleEdit(row),
  'system:role:assignPerm': (row: SysRole) => handlePermission(row),
  'system:role:delete': (row: SysRole) => handleDelete(row.id!),
}

async function loadData() {
  loading.value = true
  try {
    const res = await getRolePage(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function handleAdd() { currentRole.value = {}; dialogVisible.value = true }
function handleEdit(row: SysRole) { currentRole.value = { ...row }; dialogVisible.value = true }
function handlePermission(row: SysRole) { currentRole.value = { ...row }; permDialogVisible.value = true }

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除该角色？', '提示', { type: 'warning' })
  await deleteRole(id)
  ElMessage.success('删除成功')
  loadData()
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
