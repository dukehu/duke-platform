<template>
  <el-dialog :title="isEdit ? '编辑角色' : '新增角色'" v-model="visible" width="480px" @close="resetForm">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="角色名称" prop="roleName">
        <el-input v-model="form.roleName" />
      </el-form-item>
      <el-form-item label="角色编码" prop="roleCode">
        <el-input v-model="form.roleCode" :disabled="isEdit" />
      </el-form-item>
      <el-form-item label="数据权限">
        <el-select v-model="form.dataScope" style="width:100%">
          <el-option :value="1" label="全部数据" />
          <el-option :value="2" label="自定义部门" />
          <el-option :value="3" label="本部门" />
          <el-option :value="4" label="本部门及下级" />
          <el-option :value="5" label="仅本人" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import { createRole, updateRole } from '@/api/role'
import type { SysRole } from '@/types/role'

const props = defineProps<{ modelValue: boolean; data: Partial<SysRole> }>()
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({ get: () => props.modelValue, set: v => emit('update:modelValue', v) })
const isEdit = computed(() => !!props.data.id)
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive<Partial<SysRole>>({ roleName: '', roleCode: '', dataScope: 1, status: 1 })
const rules = {
  roleName: [{ required: true, message: '请输入角色名称' }],
  roleCode: [{ required: true, message: '请输入角色编码' }]
}

watch(() => props.data, data => Object.assign(form, { roleName: '', roleCode: '', dataScope: 1, status: 1, ...data }), { immediate: true })
function resetForm() { formRef.value?.resetFields() }

async function handleSubmit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    isEdit.value ? await updateRole(form) : await createRole(form)
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    visible.value = false
    emit('success')
  } finally {
    loading.value = false
  }
}
</script>
