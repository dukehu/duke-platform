<template>
  <iframe
    ref="iframeRef"
    :src="iframeSrc"
    style="width: 100%; height: 100%; border: none"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getToken, removeToken } from '@/utils/auth'
import { useRouter, useRoute } from 'vue-router'

const iframeRef = ref<HTMLIFrameElement>()
const router = useRouter()
const route = useRoute()

// 根据当前路由的最后一段路径来决定 iframe 加载的页面
const iframeSrc = computed(() => {
  const lastSegment = route.path.split('/').pop() || 'dashboard'
  return `http://localhost:3002/${lastSegment}`
})

function handleMessage(event: MessageEvent) {
  if (event.origin !== 'http://localhost:3002') return

  if (event.data?.type === 'IFRAME_READY') {
    iframeRef.value?.contentWindow?.postMessage(
      { type: 'AUTH_TOKEN', token: getToken() },
      'http://localhost:3002'
    )
  }

  if (event.data?.type === 'AUTH_EXPIRED') {
    removeToken()
    router.push('/login')
  }
}

onMounted(() => window.addEventListener('message', handleMessage))
onUnmounted(() => window.removeEventListener('message', handleMessage))
</script>
