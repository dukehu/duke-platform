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

    <el-row :gutter="16" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span style="font-weight: 600">快速操作</span>
          </template>
          <div class="action-buttons">
            <el-button type="primary" @click="handleUpload">
              <el-icon><Upload /></el-icon>
              上传文档
            </el-button>
            <el-button type="primary" @click="handleAsk">
              <el-icon><ChatDotRound /></el-icon>
              开始提问
            </el-button>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索知识库
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span style="font-weight: 600">系统信息</span>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="系统名称">知识问答系统</el-descriptions-item>
            <el-descriptions-item label="版本">1.0.0</el-descriptions-item>
            <el-descriptions-item label="文档库模式">私有知识库</el-descriptions-item>
            <el-descriptions-item label="搜索方式">语义搜索 + 混合搜索</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const cards = [
  { title: '文档总数', desc: '知识库文档', icon: 'Document', color: '#4F6EF7', bg: '#EEF1FE', count: 0 },
  { title: '已发布', desc: '已发布文档', icon: 'Check', color: '#10B981', bg: '#ECFDF5', count: 0 },
  { title: '提问总数', desc: '用户提问', icon: 'ChatDotRound', color: '#F59E0B', bg: '#FFFBEB', count: 0 },
  { title: '已答问题', desc: '已完成问题', icon: 'CircleCheck', color: '#8B5CF6', bg: '#F3E8FF', count: 0 }
]

function handleUpload() {
  router.push('/document')
}

function handleAsk() {
  router.push('/question')
}

function handleSearch() {
  router.push('/search')
}
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

.action-buttons {
  display: flex;
  gap: 12px;
}
</style>
