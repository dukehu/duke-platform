<template>
  <div class="layout">
    <div class="content">
      <router-view v-slot="{ Component }">
        <keep-alive>
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style lang="scss" scoped>
.layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #F0F2F7;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}
</style>
