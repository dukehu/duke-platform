<template>
  <el-dialog title="权限配置" v-model="visible" width="700px" @open="loadData" style="height: 560px; overflow-y: auto;">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="菜单权限" name="menu" style="height: 300px; overflow-y: auto;">
        <el-tree
          ref="menuTreeRef"
          :data="menuTree"
          :props="{ label: 'label', children: 'children' }"
          node-key="nodeKey"
          show-checkbox
          check-strictly
        >
          <template #default="{ node, data }">
            <span v-if="data.isButton" style="font-size:12px;color:#606266">
              <el-tag :type="data.buttonType === 1 ? 'primary' : data.buttonType === 2 ? 'danger' : 'info'" size="small" style="margin-right:4px">按钮</el-tag>
              {{ data.label }}
            </span>
            <span v-else>{{ data.label }}</span>
          </template>
        </el-tree>
      </el-tab-pane>
      <el-tab-pane label="API权限" name="api" style="height: 300px; overflow-y: auto;">
        <el-tree
          ref="apiTreeRef"
          :data="apiTree"
          :props="{ label: 'label', children: 'children' }"
          node-key="nodeKey"
          show-checkbox
        >
          <template #default="{ data }">
            <span v-if="data.isApi">
              <el-tag :type="methodColor(data.apiMethod)" size="small" style="margin-right:6px">{{ data.apiMethod }}</el-tag>
              {{ data.label }}
              <span style="color:#909399;font-size:12px;margin-left:6px">{{ data.apiPath }}</span>
            </span>
            <span v-else style="font-weight:bold">{{ data.label }}</span>
          </template>
        </el-tree>
      </el-tab-pane>
    </el-tabs>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import type { ElTree } from 'element-plus'
import { getMenuTree } from '@/api/menu'
import { getGroupedApis } from '@/api/api'
import { assignMenus, assignApis, assignButtons, getRoleMenuIds, getRoleApiIds, getRoleButtonIds } from '@/api/role'
import type { SysRole } from '@/types/role'

const props = defineProps<{ modelValue: boolean; role: Partial<SysRole> }>()
const emit = defineEmits(['update:modelValue'])

const visible = computed({ get: () => props.modelValue, set: v => emit('update:modelValue', v) })
const activeTab = ref('menu')
const menuTreeRef = ref<InstanceType<typeof ElTree>>()
const apiTreeRef = ref<InstanceType<typeof ElTree>>()
const menuTree = ref<any[]>([])
const apiTree = ref<any[]>([])
const loading = ref(false)

const methodColor = (m: string) => ({ GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }[m] || 'info')

function buildTreeWithButtons(menus: any[]): any[] {
  return menus.map(menu => {
    const node: any = {
      nodeKey: `m_${menu.id}`,
      label: menu.menuName,
      id: menu.id,
      isButton: false,
      children: [] as any[]
    }
    if (menu.children?.length) {
      node.children = buildTreeWithButtons(menu.children)
    }
    if (menu.menuType === 2 && menu.buttons?.length) {
      const btnNodes = menu.buttons.map((b: any) => ({
        nodeKey: `b_${b.id}`,
        label: b.buttonName,
        id: b.id,
        isButton: true,
        buttonType: b.buttonType,
        children: []
      }))
      node.children = [...node.children, ...btnNodes]
    }
    return node
  })
}

function buildApiTree(grouped: Record<string, Record<string, any[]>>): any[] {
  return Object.entries(grouped).map(([appId, controllers]) => ({
    nodeKey: `app_${appId}`,
    label: appId,
    isApi: false,
    children: Object.entries(controllers).map(([ctrlName, apis]) => ({
      nodeKey: `ctrl_${appId}_${ctrlName}`,
      label: ctrlName,
      isApi: false,
      children: apis.map(api => ({
        nodeKey: `api_${api.id}`,
        label: api.apiName,
        id: api.id,
        isApi: true,
        apiMethod: api.apiMethod,
        apiPath: api.apiPath,
        children: []
      }))
    }))
  }))
}

async function loadData() {
  const [menuRes, apiRes, menuIdsRes, apiIdsRes, buttonIdsRes] = await Promise.all([
    getMenuTree(),
    getGroupedApis(),
    getRoleMenuIds(props.role.id!),
    getRoleApiIds(props.role.id!),
    getRoleButtonIds(props.role.id!)
  ])

  menuTree.value = buildTreeWithButtons(menuRes.data)
  apiTree.value = buildApiTree(apiRes.data)

  await nextTick()

  const menuKeys = (menuIdsRes.data || []).map((id: number) => `m_${id}`)
  const buttonKeys = (buttonIdsRes.data || []).map((id: number) => `b_${id}`)
  menuTreeRef.value?.setCheckedKeys([...menuKeys, ...buttonKeys], false)

  const apiKeys = (apiIdsRes.data || []).map((id: number) => `api_${id}`)
  apiTreeRef.value?.setCheckedKeys(apiKeys, false)
}

async function handleSubmit() {
  loading.value = true
  try {
    const menuChecked = menuTreeRef.value?.getCheckedKeys(false) as string[]
    const menuIds = menuChecked.filter(k => k.startsWith('m_')).map(k => Number(k.slice(2)))
    const buttonIds = menuChecked.filter(k => k.startsWith('b_')).map(k => Number(k.slice(2)))

    const apiIds = (apiTreeRef.value?.getCheckedKeys(false) as string[])
      .filter(k => k.startsWith('api_'))
      .map(k => Number(k.slice(4)))

    await Promise.all([
      assignMenus(props.role.id!, menuIds),
      assignApis(props.role.id!, apiIds),
      assignButtons(props.role.id!, buttonIds)
    ])
    ElMessage.success('保存成功')
    visible.value = false
  } finally {
    loading.value = false
  }
}
</script>
