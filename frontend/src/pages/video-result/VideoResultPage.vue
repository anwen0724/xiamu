<script setup lang="ts">
import AppLayout from '../../layouts/AppLayout.vue'
</script>

<template>
  <AppLayout
    eyebrow="结果页"
    title="摘要、章节与笔记会在这里汇合"
    description="这是首版最核心的消费页，下一轮会开始接入真实结果接口和笔记联动。"
  >
    <section class="result-grid">
      <article class="panel summary-panel">
        <h2>视频摘要</h2>
        <p>
          当前是工程初始化阶段占位。后续这里会读取 `/api/video/result/{videoId}` 返回的 summary。
        </p>
      </article>

      <aside class="panel chapters-panel">
        <h2>章节导航</h2>
        <ul>
          <li>第 1 章：课程导入</li>
          <li>第 2 章：核心概念</li>
          <li>第 3 章：代码演示</li>
        </ul>
      </aside>

      <section class="panel notes-panel">
        <h2>学习笔记</h2>
        <p>笔记列表和新增表单会在结果页内联动实现。</p>
      </section>
    </section>
  </AppLayout>
</template>

<style scoped>
.result-grid {
  display: grid;
  grid-template-columns: 1.25fr 0.75fr;
  gap: 20px;
}

.summary-panel,
.chapters-panel,
.notes-panel {
  padding: 24px;
}

.notes-panel {
  grid-column: 1 / -1;
}

ul {
  margin: 0;
  padding-left: 18px;
  color: var(--text-muted);
}

@media (max-width: 900px) {
  .result-grid {
    grid-template-columns: 1fr;
  }

  .notes-panel {
    grid-column: auto;
  }
}
</style>
