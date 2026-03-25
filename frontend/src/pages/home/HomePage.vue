<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

import AppLayout from '../../layouts/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()

const navItems = computed(() =>
  authStore.isLoggedIn
    ? [
        { label: 'Dashboard', to: '/dashboard' },
        { label: '结果页骨架', to: '/video/result/1' },
      ]
    : [
        { label: '登录', to: '/login' },
        { label: '注册', to: '/register' },
      ],
)
</script>

<template>
  <AppLayout
    eyebrow="长视频快速理解工具"
    title="把学习视频整理成可读、可跳、可记录的学习现场。"
    description="上传教程、课程或知识分享类视频，系统会生成摘要、章节与笔记入口，帮助你直接进入重点。"
    :nav-items="navItems"
  >
    <section class="home-grid">
      <article class="panel hero-card">
        <div class="hero-copy">
          <span class="section-kicker">MVP 已接通后端处理链路</span>
          <h2>现在先把“登录、上传、等待、查看结果”这条前端主流程做扎实。</h2>
          <p>
            当前后端已经具备注册登录、上传、状态查询、结果页查询和笔记接口。前端首版会按学习工作台的方式承接这些能力。
          </p>
        </div>
        <div class="hero-actions">
          <RouterLink v-if="authStore.isLoggedIn" class="primary-btn" to="/dashboard">
            进入 Dashboard
          </RouterLink>
          <RouterLink v-else class="primary-btn" to="/login">登录开始使用</RouterLink>
          <RouterLink class="ghost-btn" to="/register">创建账号</RouterLink>
        </div>
      </article>

      <article class="panel upload-panel">
        <h2>上传区预留</h2>
        <p>这里会承接首页拖拽上传和本地文件选择。当前阶段先保留视觉和结构，不抢先接上传链路。</p>
        <div class="dropzone">拖拽 MP4 文件到此处 / 点击选择文件</div>
      </article>

      <article class="panel feature-panel">
        <div class="feature-head">
          <h2>首版闭环</h2>
          <span>面向在线学习者</span>
        </div>
        <ul class="feature-list">
          <li>
            <strong>摘要</strong>
            <span>快速把握主题、结论和知识点</span>
          </li>
          <li>
            <strong>章节</strong>
            <span>按时间导航，直接回到重点片段</span>
          </li>
          <li>
            <strong>笔记</strong>
            <span>围绕结果继续记录和沉淀学习内容</span>
          </li>
        </ul>
      </article>
    </section>
  </AppLayout>
</template>

<style scoped>
.home-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 20px;
}

.hero-card,
.upload-panel,
.feature-panel {
  padding: 26px;
}

.hero-card {
  display: grid;
  gap: 24px;
  background:
    linear-gradient(145deg, rgba(255, 250, 241, 0.94), rgba(255, 255, 255, 0.62)),
    linear-gradient(135deg, rgba(31, 94, 255, 0.08), rgba(31, 94, 255, 0));
}

.hero-copy {
  display: grid;
  gap: 14px;
}

.hero-copy h2 {
  margin: 0;
  font-size: clamp(28px, 3vw, 42px);
  line-height: 1.08;
}

.hero-copy p {
  margin: 0;
  color: var(--text-muted);
}

.section-kicker {
  color: var(--accent-strong);
  font-size: 13px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.dropzone {
  margin-top: 18px;
  min-height: 220px;
  display: grid;
  place-items: center;
  border-radius: 22px;
  border: 1px dashed rgba(31, 94, 255, 0.35);
  background:
    linear-gradient(135deg, rgba(31, 94, 255, 0.08), rgba(255, 255, 255, 0.7)),
    repeating-linear-gradient(
      -45deg,
      rgba(31, 94, 255, 0.04),
      rgba(31, 94, 255, 0.04) 10px,
      transparent 10px,
      transparent 20px
    );
  color: var(--text-muted);
}

.primary-btn,
.ghost-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 12px 18px;
  border-radius: 999px;
}

.primary-btn {
  background: var(--accent);
  color: #fff;
}

.ghost-btn {
  border: 1px solid var(--line);
}

.feature-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.feature-head h2 {
  margin: 0;
}

.feature-head span {
  color: var(--text-muted);
  font-size: 13px;
}

.feature-list {
  margin: 18px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 14px;
}

.feature-list li {
  display: grid;
  gap: 6px;
  padding: 14px 0;
  border-top: 1px solid rgba(42, 56, 87, 0.1);
}

.feature-list li:first-child {
  border-top: 0;
  padding-top: 0;
}

.feature-list strong {
  font-size: 18px;
}

.feature-list span {
  color: var(--text-muted);
}

@media (max-width: 900px) {
  .home-grid {
    grid-template-columns: 1fr;
  }
}
</style>
