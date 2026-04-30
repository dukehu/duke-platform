<template>
  <div class="gh-callback">
    <el-icon v-if="status === 'loading'" class="spin" :size="40"><Loading /></el-icon>
    <el-result v-else-if="status === 'error'" icon="error" title="GitHub 登录失败" :sub-title="errorMsg">
      <template #extra>
        <el-button type="primary" @click="goLogin">返回登录</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const status = ref<'loading' | 'error'>('loading')
const errorMsg = ref('')

function goLogin() {
  router.replace('/login')
}

onMounted(async () => {
  const params = new URLSearchParams(window.location.search)
  const code = params.get('code')
  const state = params.get('state')
  const error = params.get('error')
  const errorDescription = params.get('error_description')

  // 处理 GitHub OAuth 授权拒绝
  if (error) {
    status.value = 'error'
    errorMsg.value = errorDescription || 'GitHub 授权被拒绝'
    ElMessageBox.alert(errorMsg.value, 'GitHub 登录失败', {
      confirmButtonText: '返回登录',
      type: 'error'
    }).then(() => goLogin()).catch(() => goLogin())
    return
  }

  if (!code || !state) {
    status.value = 'error'
    errorMsg.value = '缺少必要参数，请重新授权'
    ElMessageBox.alert(errorMsg.value, 'GitHub 登录失败', {
      confirmButtonText: '返回登录',
      type: 'error'
    }).then(() => goLogin()).catch(() => goLogin())
    return
  }

  try {
    await userStore.githubLogin(code, state)
    permissionStore.reset()
    ElMessage.success('GitHub 登录成功')
    router.replace('/')
  } catch (e: unknown) {
    const errorMessage = e instanceof Error ? e.message : '登录失败，请重试'
    status.value = 'error'
    errorMsg.value = errorMessage
    ElMessageBox.alert(errorMessage, 'GitHub 登录失败', {
      confirmButtonText: '返回登录',
      type: 'error',
      dangerouslyUseHTMLString: false
    }).then(() => goLogin()).catch(() => goLogin())
  }
})
</script>

<style scoped>
.gh-callback {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
