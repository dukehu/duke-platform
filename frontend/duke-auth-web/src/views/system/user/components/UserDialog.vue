<template>
  <el-dialog :title="isEdit ? '编辑用户' : '新增用户'" v-model="visible" width="560px" @close="resetForm">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" :disabled="isEdit" />
      </el-form-item>
      <el-form-item label="姓名" prop="realName">
        <el-input v-model="form.realName" />
      </el-form-item>
      <el-form-item label="密码" prop="password" v-if="!isEdit">
        <el-input v-model="form.password" type="password" show-password />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="form.email" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="form.phone" />
      </el-form-item>
      <el-form-item label="所属部门">
        <el-tree-select
          v-model="form.deptIds"
          :data="deptTree"
          :props="{ label: 'deptName', value: 'id', children: 'children' }"
          multiple
          show-checkbox
          style="width:100%"
        />
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
import { ref, reactive, watch, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { createUser, updateUser } from '@/api/user'
import { getDeptTree } from '@/api/dept'
import type { SysUser } from '@/types/user'

const props = defineProps<{ modelValue: boolean; data: Partial<SysUser> }>()
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({ get: () => props.modelValue, set: v => emit('update:modelValue', v) })
const isEdit = computed(() => !!props.data.id)
const formRef = ref<FormInstance>()
const loading = ref(false)
const deptTree = ref([])

const form = reactive<Partial<SysUser> & { password?: string }>({ username: '', realName: '', status: 1, deptIds: [] })
const rules = {
  username: [{ required: true, message: '请输入用户名' }],
  realName: [{ required: true, message: '请输入姓名' }],
  password: [{ required: true, message: '请输入密码' }]
}

watch(() => props.data, data => Object.assign(form, { username: '', realName: '', status: 1, deptIds: [], ...data }), { immediate: true })

function resetForm() { formRef.value?.resetFields() }

async function handleSubmit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    isEdit.value ? await updateUser(form) : await createUser(form)
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    visible.value = false
    emit('success')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const res = await getDeptTree()
  deptTree.value = res.data
})
</script>
