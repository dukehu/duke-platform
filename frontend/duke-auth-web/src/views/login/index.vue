<template>
  <div class="login-container">
    <div class="login-box">
      <h2>DUKE-PLATFORM</h2>

      <el-tabs v-model="activeTab" class="login-tabs">
        <!-- 账号密码登录 -->
        <el-tab-pane label="账号密码" name="password">
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" size="large">
            <el-form-item prop="username">
              <el-input v-model="pwdForm.username" placeholder="用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="pwdForm.password" type="password" placeholder="密码" prefix-icon="Lock"
                show-password @keyup.enter="handlePwdLogin" />
            </el-form-item>
            <el-form-item prop="captchaCode">
              <div class="captcha-row">
                <el-input v-model="pwdForm.captchaCode" placeholder="验证码" prefix-icon="CircleCheck"
                  @keyup.enter="handlePwdLogin" />
                <img v-if="captchaImage" :src="captchaImage" class="captcha-img" title="点击刷新"
                  @click="refreshCaptcha" />
                <div v-else class="captcha-placeholder" @click="refreshCaptcha">点击获取</div>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" style="width:100%" :loading="loading" @click="handlePwdLogin">登录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 手机验证码登录 -->
        <el-tab-pane label="手机验证码" name="sms">
          <el-form ref="smsFormRef" :model="smsForm" :rules="smsRules" size="large">
            <el-form-item prop="phone">
              <el-input v-model="smsForm.phone" placeholder="手机号" prefix-icon="Phone" maxlength="11" />
            </el-form-item>
            <el-form-item prop="code">
              <div class="captcha-row">
                <el-input v-model="smsForm.code" placeholder="6位验证码" prefix-icon="Message"
                  @keyup.enter="handleSmsLogin" />
                <el-button :disabled="countdown > 0" style="flex-shrink:0;height:44px;border-radius:10px"
                  @click="handleSendSms">
                  {{ countdown > 0 ? `${countdown}s 后重试` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" style="width:100%" :loading="loading" @click="handleSmsLogin">登录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <!-- 第三方登录 -->
      <div class="weixin-divider">
        <span>其他方式登录</span>
      </div>
      <div class="third-party-btns">
        <div class="third-btn" @click="handleWeixinLogin" title="微信扫码登录">
          <svg viewBox="0 0 24 24" width="26" height="26" fill="#07C160">
            <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-3.69-6.348-7.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.047c.134 0 .24-.111.24-.247 0-.06-.023-.12-.038-.177l-.327-1.233a.49.49 0 0 1 .176-.554 6.104 6.104 0 0 0 2.5-4.633c.021-3.311-2.774-6.076-6.058-6.11zm-2.545 2.508c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.969-.982zm4.905 0c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.969-.982z"/>
          </svg>
          <span>微信登录</span>
        </div>
        <div class="third-btn" @click="handleGithubLogin" title="GitHub登录">
          <svg viewBox="0 0 24 24" width="26" height="26" fill="#24292f">
            <path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"/>
          </svg>
          <span>GitHub 登录</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { FormInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCaptcha, sendSmsCode, getWeixinLoginUrl, getGithubLoginUrl } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref('password')
const loading = ref(false)
const captchaImage = ref('')
const captchaId = ref('')
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

// 账号密码表单
const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({ username: '', password: '', captchaCode: '' })
const pwdRules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }],
  captchaCode: [{ required: true, message: '请输入验证码' }]
}

// 手机号表单
const smsFormRef = ref<FormInstance>()
const smsForm = reactive({ phone: '', code: '' })
const smsRules = {
  phone: [
    { required: true, message: '请输入手机号' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
  ],
  code: [{ required: true, message: '请输入验证码' }]
}

async function refreshCaptcha() {
  try {
    const res = await getCaptcha()
    captchaId.value = res.data.captchaId
    captchaImage.value = res.data.image
  } catch {
    ElMessage.error('获取验证码失败')
  }
}

async function handlePwdLogin() {
  await pwdFormRef.value?.validate()
  loading.value = true
  try {
    await userStore.login(pwdForm.username, pwdForm.password, captchaId.value, pwdForm.captchaCode)
    const redirect = decodeURIComponent((route.query.redirect as string) || '/')
    router.push(redirect)
  } catch {
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

async function handleSendSms() {
  await smsFormRef.value?.validateField('phone')
  try {
    await sendSmsCode(smsForm.phone)
    ElMessage.success('验证码已发送')
    countdown.value = 60
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0 && countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }, 1000)
  } catch {
    // 错误由 request 拦截器统一提示
  }
}

async function handleSmsLogin() {
  await smsFormRef.value?.validate()
  loading.value = true
  try {
    await userStore.smsLogin(smsForm.phone, smsForm.code)
    const redirect = decodeURIComponent((route.query.redirect as string) || '/')
    router.push(redirect)
  } finally {
    loading.value = false
  }
}

async function handleWeixinLogin() {
  try {
    const res = await getWeixinLoginUrl()
    window.open(res.data.url, '_blank', 'width=520,height=540,toolbar=no,menubar=no')
  } catch {
    ElMessage.error('获取微信登录链接失败')
  }
}

async function handleGithubLogin() {
  try {
    const res = await getGithubLoginUrl()
    window.location.href = res.data.url
  } catch {
    ElMessage.error('获取 GitHub 登录链接失败')
  }
}

onMounted(refreshCaptcha)
</script>

<style lang="scss" scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1d4ed8 0%, #4f46e5 60%, #7c3aed 100%);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    width: 700px;
    height: 700px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(255,255,255,.08) 0%, transparent 65%);
    top: -220px;
    right: -150px;
    pointer-events: none;
  }

  &::after {
    content: '';
    position: absolute;
    width: 500px;
    height: 500px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(255,255,255,.06) 0%, transparent 65%);
    bottom: -150px;
    left: -120px;
    pointer-events: none;
  }
}

.login-box {
  width: 420px;
  padding: 48px;
  background: rgba(255,255,255,0.97);
  border-radius: 20px;
  box-shadow: 0 32px 80px rgba(30,15,80,.25), 0 8px 24px rgba(79,70,229,.15);
  position: relative;
  z-index: 1;

  h2 {
    text-align: center;
    margin-bottom: 24px;
    font-size: 22px;
    font-weight: 700;
    color: #1A2340;
    letter-spacing: -0.3px;
  }

  :deep(.login-tabs .el-tabs__nav-wrap::after) { height: 1px; }
  :deep(.el-form-item) { margin-bottom: 20px; }

  :deep(.el-input__wrapper) {
    height: 44px;
    border-radius: 10px !important;
  }

  :deep(.el-button--primary) {
    height: 44px;
    font-size: 15px;
    font-weight: 600;
    border-radius: 10px !important;
    letter-spacing: 0.5px;
  }
}

.captcha-row {
  display: flex;
  gap: 8px;
  width: 100%;

  :deep(.el-input) { flex: 1; }

  .captcha-img {
    height: 44px;
    border-radius: 10px;
    cursor: pointer;
    border: 1px solid #dcdfe6;
    flex-shrink: 0;
  }

  .captcha-placeholder {
    height: 44px;
    width: 120px;
    border-radius: 10px;
    border: 1px dashed #dcdfe6;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: #909399;
    cursor: pointer;
    flex-shrink: 0;
    &:hover { border-color: #4F6EF7; color: #4F6EF7; }
  }
}

.weixin-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 20px 0 16px;
  color: #9CA3AF;
  font-size: 12px;

  &::before, &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: #E5E7EB;
  }
}

.third-party-btns {
  display: flex;
  gap: 10px;
}

.third-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 10px;
  border: 1px solid #E5E7EB;
  border-radius: 10px;
  cursor: pointer;
  font-size: 13px;
  color: #374151;
  transition: all 0.15s ease;

  &:first-child:hover {
    border-color: #07C160;
    color: #07C160;
    background: #F0FDF4;
    svg { fill: #07C160; }
  }

  &:last-child:hover {
    border-color: #24292f;
    color: #24292f;
    background: #F6F8FA;
  }
}
</style>
