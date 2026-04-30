<template>
  <router-view/>
</template>

<script setup lang="ts">

</script>

<style scoped>
.loading-mask {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-icon {
  font-size: 32px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
