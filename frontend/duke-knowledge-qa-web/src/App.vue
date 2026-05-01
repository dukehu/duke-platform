<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { setToken } from '@/utils/request'

function handleMessage(event: MessageEvent) {
  if (event.data?.type === 'AUTH_TOKEN') {
    setToken(event.data.token)
  }
}

onMounted(() => {
  window.addEventListener('message', handleMessage)
  window.parent.postMessage({ type: 'IFRAME_READY' }, '*')
})

onUnmounted(() => window.removeEventListener('message', handleMessage))
</script>
