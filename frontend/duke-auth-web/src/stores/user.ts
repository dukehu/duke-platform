import { defineStore } from 'pinia'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { login, getUserInfo, logout, smsLogin, weixinLogin, githubLogin } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: null as Record<string, unknown> | null,
    buttons: [] as string[],
    roles: [] as string[]
  }),
  actions: {
    async login(username: string, password: string, captchaId?: string, captchaCode?: string) {
      const res = await login({ username, password, captchaId, captchaCode })
      this._applyLoginResult(res.data)
    },
    async smsLogin(phone: string, code: string) {
      const res = await smsLogin(phone, code)
      this._applyLoginResult(res.data)
    },
    async weixinLogin(code: string, state: string) {
      const res = await weixinLogin(code, state)
      this._applyLoginResult(res.data)
    },
    async githubLogin(code: string, state: string) {
      const res = await githubLogin(code, state)
      this._applyLoginResult(res.data)
    },
    _applyLoginResult(data: { token: string; username: string; realName?: string; avatar?: string; buttons?: string[]; roles?: string[] }) {
      this.token = data.token
      setToken(data.token)
      this.userInfo = { username: data.username, nickname: data.realName }
      this.buttons = data.buttons ? [...data.buttons] : []
      this.roles = data.roles || []
    },
    async getInfo() {
      const res = await getUserInfo()
      this.userInfo = { username: res.data.username, nickname: res.data.realName }
      this.buttons = res.data.buttons ? [...res.data.buttons] : []
      this.roles = res.data.roles || []
    },
    async logout() {
      try {
        await logout()
      } catch {
        // 忽略logout错误（可能token已过期），直接清理本地状态
      } finally {
        this.token = ''
        this.userInfo = null
        this.buttons = []
        this.roles = []
        removeToken()
      }
    }
  }
})
