import type { RouteRecordRaw } from 'vue-router'
import Layout from '@/layout/index.vue'

export const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/weixin/callback',
    name: 'weixin-callback',
    component: () => import('@/views/weixin-callback/index.vue'),
    meta: { title: '微信登录' }
  },
  {
    path: '/auth/github/callback',
    name: 'github-callback',
    component: () => import('@/views/github-callback/index.vue'),
    meta: { title: 'GitHub 登录' }
  },
  {
    path: '/',
    name: 'layout',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      }
    ]
  },
  {
    path: '/403',
    component: () => import('@/views/error/403.vue')
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/error/404.vue')
  }
]
