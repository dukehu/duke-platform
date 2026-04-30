<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-select v-model="selectedAppId" placeholder="选择应用" clearable style="width:200px" @change="loadTree">
            <el-option v-for="app in appList" :key="app.id" :value="app.id" :label="app.appName" />
          </el-select>
          <div>
            <el-button v-for="btn in headerButtons" :key="btn.buttonCode" type="primary"
              @click="actionMap[btn.buttonCode]?.()">{{ btn.buttonName }}</el-button>
          </div>
        </div>
      </template>
      <el-table :data="treeData" v-loading="loading" row-key="id" border default-expand-all height="calc(100vh - 215px)">
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="['', 'info', '', 'success'][row.menuType]" size="small">{{ ['', '目录', '菜单'][row.menuType] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" />
        <el-table-column prop="component" label="组件路径" />
        <el-table-column prop="sortOrder" label="排序" width="70" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-for="btn in rowButtons" :key="btn.buttonCode" size="small"
              :type="btn.buttonCode.includes('delete') ? 'danger' : 'primary'"
              @click="actionMap[btn.buttonCode]?.(row)">{{ btn.buttonName }}</el-button>
            <el-button v-if="row.menuType === 2" link type="warning" @click="handleManageButtons(row)">管理按钮</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <MenuDialog v-model="dialogVisible" :data="currentMenu" :app-id="selectedAppId" :tree-data="treeData" @success="loadTree" />
    <ButtonManageDialog v-model="buttonDialogVisible" :menu-id="currentMenuId" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMenuTree, deleteMenu } from '@/api/menu'
import { getAppList } from '@/api/app'
import MenuDialog from './components/MenuDialog.vue'
import ButtonManageDialog from './components/ButtonManageDialog.vue'
import { useMenuButtons } from '@/utils/useMenuButtons'

const { headerButtons, rowButtons } = useMenuButtons()

const treeData = ref([])
const appList = ref([])
const selectedAppId = ref<number | undefined>()
const loading = ref(false)
const dialogVisible = ref(false)
const currentMenu = ref<Record<string, unknown>>({})
const buttonDialogVisible = ref(false)
const currentMenuId = ref(0)

const actionMap: Record<string, Function> = {
  'system:menu:add': (row?: Record<string, unknown>) => handleAdd(row?.id as number ?? 0),
  'system:menu:edit': (row: Record<string, unknown>) => handleEdit(row),
  'system:menu:delete': (row: Record<string, unknown>) => handleDelete(row.id as number),
}

async function loadTree() {
  loading.value = true
  try {
    const res = await getMenuTree(selectedAppId.value)
    treeData.value = res.data
  } finally { loading.value = false }
}

function handleAdd(parentId: number) { currentMenu.value = { parentId, appId: selectedAppId.value }; dialogVisible.value = true }
function handleEdit(row: Record<string, unknown>) { currentMenu.value = { ...row }; dialogVisible.value = true }
function handleManageButtons(row: Record<string, unknown>) { currentMenuId.value = row.id as number; buttonDialogVisible.value = true }

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除该菜单？', '提示', { type: 'warning' })
  await deleteMenu(id)
  ElMessage.success('删除成功')
  loadTree()
}

onMounted(async () => {
  const res = await getAppList()
  appList.value = res.data
  loadTree()
})
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
