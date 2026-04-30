<template>
  <el-dialog :title="isEdit ? '编辑部门' : '新增部门'" v-model="visible" width="500px" @close="resetForm">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="上级部门">
        <el-tree-select
          v-model="form.parentId"
          :data="treeData"
          :props="{ label: 'deptName', value: 'id', children: 'children' }"
          placeholder="根部门"
          clearable
          style="width:100%"
        />
      </el-form-item>
      <el-form-item label="部门名称" prop="deptName">
        <el-input v-model="form.deptName" placeholder="请输入部门名称" />
      </el-form-item>
      <el-form-item label="部门编码">
        <el-input v-model="form.deptCode" placeholder="请输入部门编码" />
      </el-form-item>
      <el-form-item label="负责人">
        <el-input v-model="form.leader" placeholder="请输入负责人" />
      </el-form-item>
      <el-form-item label="联系电话">
        <el-input v-model="form.phone" placeholder="请输入联系电话" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sortOrder" :min="0" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">禁用</el-radio>
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
import { createDept, updateDept } from '@/api/dept'
import type { SysDept } from '@/types/dept'

const props = defineProps<{ modelValue: boolean; data: Partial<SysDept>; treeData: SysDept[] }>()
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({ get: () => props.modelValue, set: v => emit('update:modelValue', v) })
const isEdit = computed(() => !!props.data.id)
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive<Partial<SysDept>>({ parentId: 0, deptName: '', status: 1, sortOrder: 0 })
const rules = { deptName: [{ required: true, message: '请输入部门名称' }] }

watch(() => props.data, data => Object.assign(form, { parentId: 0, deptName: '', status: 1, sortOrder: 0, ...data }), { immediate: true })

function resetForm() { formRef.value?.resetFields() }

async function handleSubmit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    isEdit.value ? await updateDept(form) : await createDept(form)
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    visible.value = false
    emit('success')
  } finally {
    loading.value = false
  }
}
</script>
