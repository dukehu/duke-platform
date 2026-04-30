<template>
  <div class="header">
    <div class="left">
      <el-icon class="collapse-btn" @click="appStore.toggleCollapse()">
        <Fold v-if="!appStore.collapsed" />
        <Expand v-else />
      </el-icon>
      <Breadcrumb />
    </div>
    <div class="right">
      <el-dropdown @command="handleCommand">
        <span class="user-info">
          <el-avatar :size="32" icon="UserFilled" />
          <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="change-password">修改密码</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>

  <!-- 修改密码对话框 -->
  <el-dialog v-model="pwdDialogVisible" title="修改密码" width="400px" :close-on-click-modal="false">
    <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
      <el-form-item label="旧密码" prop="oldPassword">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少8位，含字母和数字" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="pwdLoading" @click="handleChangePwd">确认修改</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { changePassword } from '@/api/auth'
import Breadcrumb from './Breadcrumb.vue'

const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const pwdDialogVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码' }],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,}$/, message: '至少 8 位，需包含字母和数字' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码' },
    {
      validator: (_: unknown, value: string, cb: (e?: Error) => void) =>
        value !== pwdForm.newPassword ? cb(new Error('两次密码不一致')) : cb(),
      trigger: 'blur'
    }
  ]
}

async function handleCommand(cmd: string) {
  if (cmd === 'change-password') {
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    pwdDialogVisible.value = true
  } else if (cmd === 'logout') {
    await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
    await userStore.logout()
    permissionStore.reset()
    router.push('/login')
  }
}

async function handleChangePwd() {
  await pwdFormRef.value?.validate()
  pwdLoading.value = true
  try {
    await changePassword(pwdForm.oldPassword, pwdForm.newPassword, pwdForm.confirmPassword)
    ElMessage.success('密码修改成功，请重新登录')
    pwdDialogVisible.value = false
    await userStore.logout()
    permissionStore.reset()
    router.push('/login')
  } finally {
    pwdLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.header {
  height: 60px;
  background: #fff;
  box-shadow: 0 1px 0 #E5E8F0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
  z-index: 10;

  .left {
    display: flex;
    align-items: center;
    gap: 14px;
  }

  .collapse-btn {
    font-size: 30px;
    cursor: pointer;
    color: #6B7280;
    padding: 6px;
    border-radius: 8px;
    transition: all 0.15s ease;
    &:hover { color: #4F6EF7; background: #EEF1FE; }
  }

  .right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      color: #374151;
      padding: 6px 10px;
      border-radius: 8px;
      transition: all 0.15s ease;
      &:hover { background: #F0F2F7; }
      .username { font-size: 13px; font-weight: 500; }
    }
  }
}
</style>
