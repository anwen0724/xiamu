<script setup lang="ts">
import { useRouter } from 'vue-router'

import AppLayout from '../../layouts/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'

const navItems = [
  { label: '首页', to: '/' },
  { label: '上传', to: '/' },
  { label: '结果页', to: '/video/result/1' },
]

const router = useRouter()
const authStore = useAuthStore()

async function handleLogout() {
  await authStore.logout()
  await router.push('/')
}
</script>

<template>
  <AppLayout
    eyebrow="Dashboard"
    title="最近处理的视频"
    description="这里会承接当前用户的视频列表、状态筛选和继续学习入口。"
    :nav-items="navItems"
  >
    <section class="user-strip panel">
      <div>
        <strong>{{ authStore.currentUser?.userName || authStore.currentUser?.userAccount }}</strong>
        <p>{{ authStore.currentUser?.userAccount }}</p>
      </div>
      <button type="button" @click="handleLogout">退出登录</button>
    </section>

    <section class="dashboard-grid">
      <aside class="panel side-panel">
        <strong>快速入口</strong>
        <a class="menu-item" href="#">最近处理</a>
        <a class="menu-item" href="#">全部视频</a>
        <a class="menu-item" href="#">我的笔记</a>
      </aside>
      <section class="panel list-panel">
        <article class="video-card">
          <div>
            <h3>Python 入门教程</h3>
            <p>2 小时 15 分钟 · SUCCESS · 3 天前</p>
          </div>
          <div class="card-actions">
            <a href="/video/result/1">查看结果</a>
            <a href="/video/status/1">查看状态</a>
          </div>
        </article>
        <article class="video-card">
          <div>
            <h3>Vue 3 实战课程</h3>
            <p>45 分钟 · PROCESSING · 20 分钟前</p>
          </div>
          <div class="card-actions">
            <a href="/video/status/2">继续等待</a>
          </div>
        </article>
      </section>
    </section>
  </AppLayout>
</template>

<style scoped>
.user-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px;
  margin-bottom: 20px;
}

.user-strip p {
  margin: 6px 0 0;
  color: var(--text-muted);
}

.user-strip button {
  min-height: 42px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.76);
  cursor: pointer;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 20px;
}

.side-panel,
.list-panel {
  padding: 24px;
}

.side-panel {
  display: grid;
  gap: 10px;
}

.menu-item {
  padding: 10px 12px;
  border-radius: 12px;
  color: var(--text-muted);
}

.menu-item:hover {
  background: rgba(31, 94, 255, 0.08);
  color: var(--text-main);
}

.list-panel {
  display: grid;
  gap: 16px;
}

.video-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid var(--line);
}

.video-card h3 {
  margin: 0 0 6px;
}

.video-card p {
  margin: 0;
  color: var(--text-muted);
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

@media (max-width: 900px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
