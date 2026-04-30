<template>
  <el-breadcrumb separator="/">
    <el-breadcrumb-item
      v-for="item in breadcrumbs"
      :key="item.path"
      :to="item.path !== route.path ? item.path : undefined"
    >
      {{ item.title }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const breadcrumbs = computed(() =>
  route.matched
    .filter(r => r.meta?.title)
    .map(r => ({ path: r.path, title: r.meta.title as string }))
)
</script>
