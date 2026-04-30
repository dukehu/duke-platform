<template>
  <div class="page-container">
    <div class="user-layout">
      <el-card class="dept-tree-card">
        <template #header>部门</template>
        <el-tree :data="deptTree" :props="{ label: 'deptName', children: 'children' }" node-key="id"
          default-expand-all highlight-current @node-click="handleDeptClick" />
      </el-card>
      <div class="user-table-area">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-form inline :model="query" @submit.prevent="loadData">
                <el-form-item label="用户名">
                  <el-input v-model="query.keyword" placeholder="用户名/姓名" clearable />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="loadData">搜索</el-button>
                  <el-button @click="resetQuery">重置</el-button>
                </el-form-item>
              </el-form>
              <div>
                <el-button v-for="btn in headerButtons" :key="btn.buttonCode" type="primary"
                  @click="actionMap[btn.buttonCode]?.()">{{ btn.buttonName }}</el-button>
              </div>
            </div>
          </template>
          <el-table :data="tableData" v-loading="loading" border height="calc(100vh - 272px)">
            <el-table-column prop="username" label="用户名" width="100" />
            <el-table-column prop="realName" label="姓名" width="140" />
            <el-table-column prop="email" label="邮箱" width="140" />
            <el-table-column prop="phone" label="手机号" width="140" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-switch :model-value="row.status === 1" @change="toggleStatus(row)" />
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="230" />
            <el-table-column v-if="rowButtons.length" label="操作" :width="rowButtons.length > 3 ? 340 : 200" min-width="160" fixed="right">
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
      </div>
    </div>
    <UserDialog v-model="dialogVisible" :data="currentUser" @success="loadData" />
    <RoleAssignDialog v-model="roleDialogVisible" :user-id="currentUserId" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserPage, deleteUser, updateUserStatus } from '@/api/user'
import { getDeptTree } from '@/api/dept'
import UserDialog from './components/UserDialog.vue'
import RoleAssignDialog from './components/RoleAssignDialog.vue'
import type { SysUser } from '@/types/user'
import { useMenuButtons } from '@/utils/useMenuButtons'

const { headerButtons, rowButtons } = useMenuButtons()

const deptTree = ref([])
const tableData = ref<SysUser[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const roleDialogVisible = ref(false)
const currentUser = ref<Partial<SysUser>>({})
const currentUserId = ref<number>(0)
const query = reactive({ keyword: '', current: 1, size: 10, deptId: undefined as number | undefined })

const actionMap: Record<string, Function> = {
  'system:user:add': () => handleAdd(),
  'system:user:edit': (row: SysUser) => handleEdit(row),
  'system:user:assignRole': (row: SysUser) => handleAssignRole(row),
  'system:user:delete': (row: SysUser) => handleDelete(row.id!),
}

async function loadData() {
  loading.value = true
  try {
    const res = await getUserPage(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function resetQuery() { query.keyword = ''; query.deptId = undefined; loadData() }
function handleDeptClick(data: { id: number }) { query.deptId = data.id; loadData() }
function handleAdd() { currentUser.value = {}; dialogVisible.value = true }
function handleEdit(row: SysUser) { currentUser.value = { ...row }; dialogVisible.value = true }
function handleAssignRole(row: SysUser) { currentUserId.value = row.id!; roleDialogVisible.value = true }

async function toggleStatus(row: SysUser) {
  const newStatus = row.status === 1 ? 0 : 1
  await updateUserStatus(row.id!, newStatus)
  row.status = newStatus
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除该用户？', '提示', { type: 'warning' })
  await deleteUser(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(async () => {
  const res = await getDeptTree()
  deptTree.value = res.data
  loadData()
})
</script>

<style lang="scss" scoped>
.user-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;

  .dept-tree-card {
    width: 220px;
    flex-shrink: 0;
    :deep(.el-card__body) {
      height: calc(100vh - 163px);
      overflow-y: auto;
      padding: 12px;
    }
  }

  .user-table-area {
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: nowrap;
  width: 100%;
}
</style>
