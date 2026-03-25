<script setup lang="ts">
import AppLayout from '../../layouts/AppLayout.vue'
</script>

<template>
  <AppLayout
    eyebrow="处理状态"
    title="视频正在生成摘要与章节"
    description="后续会在这里接入状态轮询、阶段提示和失败重试。"
  >
    <section class="panel status-panel">
      <div class="status-pill">PROCESSING</div>
      <ul>
        <li>已上传视频</li>
        <li>正在转写与生成结构化结果</li>
        <li>完成后将跳转结果页</li>
      </ul>
    </section>
  </AppLayout>
</template>

<style scoped>
.status-panel {
  padding: 28px;
}

.status-pill {
  display: inline-flex;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(31, 94, 255, 0.1);
  color: var(--accent-strong);
  font-weight: 700;
}

ul {
  margin: 20px 0 0;
  padding-left: 18px;
  color: var(--text-muted);
}
</style>
