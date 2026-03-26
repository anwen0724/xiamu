<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import AppLayout from '../../layouts/AppLayout.vue'

const route = useRoute()

const sectionLabel = computed(() => {
  const label = route.query.label
  return typeof label === 'string' && label.trim() ? label.trim() : '功能页面'
})

const navItems = [
  { label: '首页', to: '/', kind: 'ghost' as const },
  { label: '登录', to: '/login', kind: 'ghost' as const },
  { label: '注册', to: '/register', kind: 'primary' as const },
]
</script>

<template>
  <AppLayout
    eyebrow="占位页面"
    :title="`${sectionLabel} 正在建设中`"
    description="当前先统一使用占位页承接导航入口，后续再补真实页面内容。"
    :nav-items="navItems"
  >
    <section class="panel placeholder-card">
      <strong>{{ sectionLabel }}</strong>
      <p>这个入口已经预留，当前阶段先完成首页导航结构，后续再补完整功能页。</p>
    </section>
  </AppLayout>
</template>

<style scoped>
.placeholder-card {
  width: min(760px, 100%);
  padding: 32px;
}

.placeholder-card strong {
  display: block;
  font-size: 24px;
  margin-bottom: 12px;
}

.placeholder-card p {
  margin: 0;
  color: var(--text-muted);
  font-size: 16px;
}
</style>
