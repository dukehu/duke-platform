import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: Layout,
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { title: '首页' }
        },
        {
          path: 'document',
          component: () => import('@/views/document/index.vue'),
          meta: { title: '文档管理' }
        },
        {
          path: 'question',
          component: () => import('@/views/question/index.vue'),
          meta: { title: '知识问答' }
        },
        {
          path: 'search',
          component: () => import('@/views/search/index.vue'),
          meta: { title: '语义搜索' }
        }
      ]
    }
  ]
})

export default router
