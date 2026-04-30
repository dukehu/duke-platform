<template>
  <el-dialog title="管理按钮" v-model="visible" width="700px" @open="loadList">
    <div style="margin-bottom:12px">
      <el-button type="primary" size="small" @click="handleAdd">新增按钮</el-button>
    </div>
    <el-table :data="list" v-loading="loading" border size="small">
      <el-table-column prop="buttonName" label="按钮名称" width="100" />
      <el-table-column prop="buttonCode" label="按钮编码" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.buttonType === 1 ? 'primary' : 'success'" size="small">
            {{ row.buttonType === 1 ? '头部' : '行操作' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="70" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="formVisible" :title="isEdit ? '编辑按钮' : '新增按钮'" width="440px" append-to-body @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="按钮名称" prop="buttonName"><el-input v-model="form.buttonName" /></el-form-item>
        <el-form-item label="按钮编码" prop="buttonCode"><el-input v-model="form.buttonCode" placeholder="如: system:user:add" /></el-form-item>
        <el-form-item label="按钮类型" prop="buttonType">
          <el-radio-group v-model="form.buttonType">
            <el-radio :value="1">头部</el-radio>
            <el-radio :value="2">行操作</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { listButtons, createButton, updateButton, deleteButton } from '@/api/button'

const props = defineProps<{ modelValue: boolean; menuId: number }>()
const emit = defineEmits(['update:modelValue'])

const visible = computed({ get: () => props.modelValue, set: v => emit('update:modelValue', v) })

const list = ref([])
const loading = ref(false)
const formVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({ id: undefined as number | undefined, buttonName: '', buttonCode: '', buttonType: 1, sortOrder: 0, status: 1 })
const rules = {
  buttonName: [{ required: true, message: '请输入按钮名称' }],
  buttonCode: [{ required: true, message: '请输入按钮编码' }],
  buttonType: [{ required: true, message: '请选择按钮类型' }],
}

async function loadList() {
  loading.value = true
  try {
    const res = await listButtons(props.menuId)
    list.value = res.data
  } finally { loading.value = false }
}

function handleAdd() {
  isEdit.value = false
  Object.assign(form, { id: undefined, buttonName: '', buttonCode: '', buttonType: 1, sortOrder: 0, status: 1 })
  formVisible.value = true
}

function handleEdit(row: Record<string, unknown>) {
  isEdit.value = true
  Object.assign(form, row)
  formVisible.value = true
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除该按钮？', '提示', { type: 'warning' })
  await deleteButton(id)
  ElMessage.success('删除成功')
  loadList()
}

function resetForm() { formRef.value?.resetFields() }

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const data = { ...form, menuId: props.menuId }
    isEdit.value ? await updateButton(data) : await createButton(data)
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    formVisible.value = false
    loadList()
  } finally { submitting.value = false }
}
</script>
