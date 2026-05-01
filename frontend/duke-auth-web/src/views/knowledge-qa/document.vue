<template>
  <iframe ref="iframeRef" src="http://localhost:3002/document" style="width: 100%; height: 100%; border: none" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getToken, removeToken } from '@/utils/auth'
import { useRouter } from 'vue-router'

const iframeRef = ref<HTMLIFrameElement>()
const router = useRouter()

function handleMessage(event: MessageEvent) {
  if (event.origin !== 'http://localhost:3002') return
  if (event.data?.type === 'IFRAME_READY') {
    iframeRef.value?.contentWindow?.postMessage({ type: 'AUTH_TOKEN', token: getToken() }, 'http://localhost:3002')
  }
  if (event.data?.type === 'AUTH_EXPIRED') {
    removeToken()
    router.push('/login')
  }
}

onMounted(() => window.addEventListener('message', handleMessage))
onUnmounted(() => window.removeEventListener('message', handleMessage))
</script>
