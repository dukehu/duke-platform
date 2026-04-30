import request from '@/utils/request'

export function login(data: { username: string; password: string; captchaId?: string; captchaCode?: string }) {
  return request.post('/auth/login', data)
}

export function getCaptcha() {
  return request.get('/auth/captcha')
}

export function sendSmsCode(phone: string) {
  return request.post('/auth/sms/send', { phone })
}

export function smsLogin(phone: string, code: string) {
  return request.post('/auth/sms/login', { phone, code })
}

export function getWeixinLoginUrl() {
  return request.get('/auth/weixin/url')
}

export function weixinLogin(code: string, state: string) {
  return request.post('/auth/weixin/callback', { code, state })
}

export function getGithubLoginUrl() {
  return request.get('/auth/github/url')
}

export function githubLogin(code: string, state: string) {
  return request.post('/auth/github/callback', { code, state })
}

export function changePassword(oldPassword: string, newPassword: string, confirmPassword: string) {
  return request.post('/auth/change-password', { oldPassword, newPassword, confirmPassword })
}

export function getUserInfo() {
  return request.get('/auth/info')
}

export function getMenuTree() {
  return request.get('/auth/menu')
}

export function logout() {
  return request.post('/auth/logout')
}
