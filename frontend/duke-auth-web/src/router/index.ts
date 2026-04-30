import { createRouter, createWebHistory } from 'vue-router'
import { staticRoutes } from './routes'
import { getToken, removeToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

const router = createRouter({
  history: createWebHistory(),
  routes: staticRoutes
})

function safeRedirect(path: string): string {
  return path.startsWith('/') && !path.startsWith('//') ? encodeURIComponent(path) : '/'
}

// 无需登录即可访问的路径（OAuth 回调页在拿到 token 之前就需要加载）
const NO_AUTH_PATHS = ['/login', '/weixin/callback', '/auth/github/callback']

router.beforeEach(async (to, _from, next) => {
  const token = getToken()
  if (!token) {
    if (NO_AUTH_PATHS.includes(to.path)) return next()
    return next(`/login?redirect=${safeRedirect(to.path)}`)
  }
  if (to.path === '/login') return next('/')

  const userStore = useUserStore()
  const permissionStore = usePermissionStore()

  if (!userStore.userInfo) {
    try {
      await userStore.getInfo()
    } catch {
      removeToken()
      return next(`/login?redirect=${safeRedirect(to.path)}`)
    }
  }

  if (!permissionStore.loaded) {
    const routes = await permissionStore.generateRoutes()
    routes.forEach(r => router.addRoute('layout', r))
    return next({ ...to, replace: true })
  }

  next()
})

export default router
