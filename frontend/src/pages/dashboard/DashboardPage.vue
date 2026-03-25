<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import VideoUploadPanel from '../../components/upload/VideoUploadPanel.vue'
import AppLayout from '../../layouts/AppLayout.vue'
import type { EntityId } from '../../api/types'
import { useDashboardVideosQuery } from '../../queries'
import { useAuthStore } from '../../stores/auth'
import { formatDuration, formatRelativeDate } from '../../utils'

const navItems = [
  { label: '首页', to: '/' },
  { label: '上传', to: '/' },
]

const router = useRouter()
const authStore = useAuthStore()
const selectedStatus = ref('')
const videosQuery = useDashboardVideosQuery(selectedStatus)

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '等待处理', value: 'WAITING' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '处理完成', value: 'SUCCESS' },
  { label: '处理失败', value: 'FAILED' },
]

const videoRecords = computed(() => videosQuery.data.value?.records ?? [])
const dashboardLoading = computed(() => videosQuery.isLoading.value)
const dashboardHasError = computed(() => videosQuery.isError.value)
const dashboardErrorMessage = computed(
  () => videosQuery.error.value?.message || '视频列表加载失败',
)

async function handleLogout() {
  await authStore.logout()
  await router.push('/')
}

function openVideo(item: { id: EntityId }) {
  void router.push(`/video/${item.id}`)
}
</script>

<template>
  <AppLayout
    eyebrow="Dashboard"
    title="最近处理的视频"
    description="这里承接当前用户的视频列表、状态筛选和继续学习入口。"
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
        <strong>快捷入口</strong>
        <button class="menu-item active" type="button">最近处理</button>
        <button class="menu-item" type="button">全部视频</button>
        <button class="menu-item" type="button">我的笔记</button>
      </aside>

      <section class="content-column">
        <VideoUploadPanel
          title="上传新视频"
          description="这里已经接入真实上传接口。上传成功后会直接进入统一详情页。"
          cta-label="选择视频并上传"
        />

        <section class="panel list-panel">
          <div class="list-head">
            <div>
              <h2>最近处理的视频</h2>
              <p>支持按状态筛选，优先回访已生成摘要与章节的内容。</p>
            </div>

            <select v-model="selectedStatus">
              <option
                v-for="option in statusOptions"
                :key="option.value || 'all'"
                :value="option.value"
              >
                {{ option.label }}
              </option>
            </select>
          </div>

          <p v-if="dashboardLoading" class="info-text">正在加载视频列表...</p>
          <p v-else-if="dashboardHasError" class="error-text">{{ dashboardErrorMessage }}</p>
          <div v-else-if="videoRecords.length" class="video-list">
            <article
              v-for="item in videoRecords"
              :key="item.id"
              class="video-card"
            >
              <div class="card-main">
                <div class="status-tag" :data-status="item.status">{{ item.status }}</div>
                <h3>{{ item.title }}</h3>
                <p>{{ formatDuration(item.duration) }} · {{ formatRelativeDate(item.createTime) }}</p>
                <p v-if="item.summary" class="summary-text">{{ item.summary }}</p>
                <p v-else class="summary-placeholder">摘要尚未生成，先查看当前处理进度。</p>
              </div>
              <div class="card-actions">
                <button type="button" @click="openVideo(item)">
                  {{ item.status === 'SUCCESS' ? '查看结果' : '查看进度' }}
                </button>
              </div>
            </article>
          </div>
          <div v-else class="empty-box">
            <strong>还没有可回访的视频</strong>
            <p>先上传一个学习视频，处理完成后这里会显示你的历史记录。</p>
          </div>
        </section>
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

.content-column {
  display: grid;
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
  text-align: left;
  padding: 10px 12px;
  border: 0;
  background: transparent;
  cursor: pointer;
  border-radius: 12px;
  color: var(--text-muted);
}

.menu-item:hover,
.menu-item.active {
  background: rgba(31, 94, 255, 0.08);
  color: var(--text-main);
}

.list-panel {
  display: grid;
  gap: 16px;
}

.list-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.list-head h2,
.list-head p {
  margin: 0;
}

.list-head p {
  margin-top: 6px;
  color: var(--text-muted);
}

.list-head select {
  min-height: 42px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.88);
}

.video-list {
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

.card-main {
  display: grid;
  gap: 6px;
}

.status-tag {
  justify-self: start;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(31, 94, 255, 0.08);
  color: var(--accent-strong);
  font-size: 12px;
  font-weight: 700;
}

.status-tag[data-status='FAILED'] {
  background: rgba(208, 57, 57, 0.1);
  color: #a02c2c;
}

.status-tag[data-status='PROCESSING'],
.status-tag[data-status='WAITING'] {
  background: rgba(243, 167, 0, 0.12);
  color: #9d6a00;
}

.video-card h3 {
  margin: 0 0 6px;
}

.video-card p {
  margin: 0;
  color: var(--text-muted);
}

.summary-text {
  max-width: 54ch;
}

.summary-placeholder {
  color: #8f8290;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.card-actions button {
  min-height: 42px;
  padding: 0 16px;
  border: 0;
  border-radius: 999px;
  background: var(--accent);
  color: #fff;
  cursor: pointer;
}

.info-text,
.error-text {
  margin: 0;
}

.error-text {
  color: #a02c2c;
}

.empty-box {
  padding: 22px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid var(--line);
}

.empty-box strong,
.empty-box p {
  display: block;
  margin: 0;
}

.empty-box p {
  margin-top: 8px;
  color: var(--text-muted);
}

@media (max-width: 980px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .list-head,
  .video-card,
  .user-strip {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-actions {
    width: 100%;
  }
}
</style>