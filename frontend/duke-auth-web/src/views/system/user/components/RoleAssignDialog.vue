<template>
  <el-dialog title="分配角色" v-model="visible" width="500px" @open="loadData">
    <el-checkbox-group v-model="selectedRoles">
      <el-checkbox v-for="role in roleList" :key="role.id" :value="role.id">
        {{ role.roleName }}（{{ role.roleCode }}）
      </el-checkbox>
    </el-checkbox-group>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getRoleList } from '@/api/role'
import { assignRoles, getUserById } from '@/api/user'

const props = defineProps<{ modelValue: boolean; userId: number }>()
const emit = defineEmits(['update:modelValue'])

const visible = computed({ get: () => props.modelValue, set: v => emit('update:modelValue', v) })
const roleList = ref<{ id: number; roleName: string; roleCode: string }[]>([])
const selectedRoles = ref<number[]>([])
const loading = ref(false)

async function loadData() {
  const [rolesRes, userRes] = await Promise.all([
    getRoleList(),
    getUserById(props.userId)
  ])
  roleList.value = rolesRes.data
  selectedRoles.value = userRes.data.roleIds || []
}

async function handleSubmit() {
  loading.value = true
  try {
    await assignRoles(props.userId, selectedRoles.value)
    ElMessage.success('分配成功')
    visible.value = false
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.el-checkbox { display: block; margin: 8px 0; }
</style>
