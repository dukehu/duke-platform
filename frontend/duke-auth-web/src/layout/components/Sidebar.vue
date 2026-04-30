<template>
  <div class="sidebar" :class="{ collapsed: appStore.collapsed }">
    <div class="logo">
      <span v-if="!appStore.collapsed">DUKE-PLATFORM</span>
      <span v-else>DP</span>
    </div>
    <el-menu
      :default-active="route.path"
      :collapse="appStore.collapsed"
      router
      text-color="#374151"
    >
      <el-menu-item index="/dashboard">
        <el-icon><HomeFilled /></el-icon>
        <template #title>首页</template>
      </el-menu-item>
      <template v-for="menu in permissionStore.routes" :key="menu.path">
        <el-sub-menu v-if="menu.children?.length" :index="menu.path">
          <template #title>
            <el-icon><component :is="menu.meta?.icon || 'Menu'" /></el-icon>
            <span>{{ menu.meta?.title }}</span>
          </template>
          <el-menu-item
            v-for="child in menu.children"
            :key="child.path"
            :index="child.path"
          >
            <template #title>{{ child.meta?.title }}</template>
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item v-else :index="menu.path">
          <el-icon><component :is="menu.meta?.icon || 'Menu'" /></el-icon>
          <template #title>{{ menu.meta?.title }}</template>
        </el-menu-item>
      </template>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { usePermissionStore } from '@/stores/permission'

const route = useRoute()
const appStore = useAppStore()
const permissionStore = usePermissionStore()
</script>

<style lang="scss" scoped>
.sidebar {
  width: 210px;
  height: 100vh;
  background: #FFFFFF;
  border-right: 1px solid #E5E8F0;
  position: fixed;
  left: 0;
  top: 0;
  transition: width 0.3s cubic-bezier(.4,0,.2,1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  z-index: 100;

  &.collapsed { width: 64px; }

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 15px;
    font-weight: 700;
    letter-spacing: 0.5px;
    border-bottom: 1px solid #E5E8F0;
    flex-shrink: 0;
    color: #4F6EF7;
    background: #FFFFFF;
  }

  .el-menu {
    border-right: none;
    flex: 1;
    overflow-y: auto;
    overflow-x: hidden;
    background: transparent !important;

    &::-webkit-scrollbar { width: 4px; }
    &::-webkit-scrollbar-thumb { background: #E5E8F0; border-radius: 2px; }

    :deep(.el-menu-item) {
      margin: 2px 8px;
      border-radius: 8px;
      height: 42px;
      line-height: 42px;
      color: #374151;
      transition: all 0.15s ease;

      &:hover {
        background: #F0F2F7 !important;
        color: #4F6EF7 !important;
      }

      &.is-active {
        background: #EEF1FE !important;
        color: #4F6EF7 !important;
        font-weight: 600;
      }
    }

    :deep(.el-sub-menu__title) {
      margin: 2px 8px;
      border-radius: 8px;
      height: 42px;
      line-height: 42px;
      color: #374151;
      transition: all 0.15s ease;

      &:hover {
        background: #F0F2F7 !important;
        color: #4F6EF7 !important;
      }
    }

    :deep(.el-sub-menu .el-menu-item) {
      padding-left: 48px !important;
      height: 38px;
      line-height: 38px;
      font-size: 13px;
    }

    :deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
      color: #4F6EF7 !important;
    }
  }
}
</style>
