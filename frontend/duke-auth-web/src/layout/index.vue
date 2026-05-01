<template>
  <div class="layout">
    <Sidebar />
    <div class="main-container" :class="{ collapsed: appStore.collapsed }">
      <Header />
      <div class="content">
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
</script>

<style lang="scss" scoped>
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}
.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  margin-left: 210px;
  transition: margin-left 0.3s cubic-bezier(.4,0,.2,1);
  &.collapsed { margin-left: 64px; }
}
.content {
  flex: 1;
  overflow: hidden;
  //padding: 20px;
  background: #F0F2F7;
}
</style>
