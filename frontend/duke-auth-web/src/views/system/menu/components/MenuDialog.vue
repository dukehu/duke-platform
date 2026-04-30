<template>
  <el-dialog :title="isEdit ? '编辑菜单' : '新增菜单'" v-model="visible" width="560px" @close="resetForm">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="上级菜单">
        <el-tree-select v-model="form.parentId" :data="treeData" :props="{ label: 'menuName', value: 'id', children: 'children' }" placeholder="根菜单" clearable style="width:100%" />
      </el-form-item>
      <el-form-item label="菜单类型">
        <el-radio-group v-model="form.menuType">
          <el-radio :value="1">目录</el-radio>
          <el-radio :value="2">菜单</el-radio>
          <el-radio :value="3">按钮</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="菜单名称" prop="menuName"><el-input v-model="form.menuName" /></el-form-item>
      <el-form-item label="路由路径" v-if="form.menuType !== 3"><el-input v-model="form.path" /></el-form-item>
      <el-form-item label="组件路径" v-if="form.menuType === 2"><el-input v-model="form.component" placeholder="如: system/user" /></el-form-item>
      <el-form-item label="权限标识" v-if="form.menuType !== 1"><el-input v-model="form.permission" placeholder="如: system:user:list" /></el-form-item>
      <el-form-item label="图标" v-if="form.menuType !== 3"><el-input v-model="form.icon" /></el-form-item>
      <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="是否显示" v-if="form.menuType !== 3">
        <el-radio-group v-model="form.visible">
          <el-radio :value="1">显示</el-radio>
          <el-radio :value="0">隐藏</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { createMenu, updateMenu } from '@/api/menu'

const props = defineProps<{ modelValue: boolean; data: Record<string, unknown>; appId?: number; treeData: unknown[] }>()
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({ get: () => props.modelValue, set: v => emit('update:modelValue', v) })
const isEdit = computed(() => !!props.data.id)
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive<Record<string, unknown>>({ parentId: 0, menuType: 1, menuName: '', status: 1, visible: 1, sortOrder: 0 })
const rules = { menuName: [{ required: true, message: '请输入菜单名称' }] }

watch(() => props.data, data => Object.assign(form, { parentId: 0, menuType: 1, menuName: '', status: 1, visible: 1, sortOrder: 0, appId: props.appId, ...data }), { immediate: true })
function resetForm() { formRef.value?.resetFields() }

async function handleSubmit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    isEdit.value ? await updateMenu(form) : await createMenu(form)
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    visible.value = false
    emit('success')
  } finally { loading.value = false }
}
</script>
