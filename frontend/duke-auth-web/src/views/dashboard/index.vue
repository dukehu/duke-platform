<template>
  <div class="dashboard">
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.title">
        <el-card shadow="hover">
          <div class="card-content">
            <div class="icon-wrap" :style="{ background: card.bg }">
              <el-icon :size="24" :color="card.color"><component :is="card.icon" /></el-icon>
            </div>
            <div>
              <div class="card-title">{{ card.title }}</div>
              <div class="card-desc">{{ card.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-card style="margin-top:20px">
      <template #header>系统信息</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="当前用户">{{ userStore.userInfo?.nickname }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ userStore.userInfo?.username }}</el-descriptions-item>
        <el-descriptions-item label="角色数量">{{ userStore.roles.length }}</el-descriptions-item>
        <el-descriptions-item label="权限数量">{{ userStore.buttons.length }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const cards = [
  { title: '用户管理', desc: '管理系统用户', icon: 'User', color: '#4F6EF7', bg: '#EEF1FE' },
  { title: '角色管理', desc: '管理用户角色', icon: 'UserFilled', color: '#10B981', bg: '#ECFDF5' },
  { title: '菜单管理', desc: '管理系统菜单', icon: 'Menu', color: '#F59E0B', bg: '#FFFBEB' },
  { title: '操作日志', desc: '查看操作记录', icon: 'Document', color: '#EF4444', bg: '#FEF2F2' }
]
</script>

<style lang="scss" scoped>
.dashboard { padding: 0; }

.card-content {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 4px 0;

  .icon-wrap {
    width: 52px;
    height: 52px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .card-title {
    font-size: 15px;
    font-weight: 600;
    color: #1A2340;
  }

  .card-desc {
    font-size: 12px;
    color: #6B7280;
    margin-top: 4px;
  }
}
</style>
