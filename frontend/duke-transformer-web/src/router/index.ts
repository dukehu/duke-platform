import { createRouter, createWebHistory } from 'vue-router'

// @ts-ignore
const routes = [
  {
    path: '/transformer',
    name: 'transformer',
    component: () => import('@/views/transformer/index.vue'),
    meta: { title: 'Transformer 学习系统' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
