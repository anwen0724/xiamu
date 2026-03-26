<script setup lang="ts">
import { computed, ref } from 'vue'
import { useQueryClient } from '@tanstack/vue-query'
import { useRouter } from 'vue-router'

import { uploadVideo } from '../../api/video'
import type { EntityId, VideoDashboardVO } from '../../api/types'
import AppLayout from '../../layouts/AppLayout.vue'
import { useDashboardVideosQuery } from '../../queries'
import { useAuthStore } from '../../stores/auth'
import { formatDuration, formatRelativeDate } from '../../utils'

const router = useRouter()
const queryClient = useQueryClient()
const authStore = useAuthStore()

const selectedStatus = ref('')
const inputRef = ref<HTMLInputElement | null>(null)
const uploadPending = ref(false)
const uploadErrorMessage = ref('')
const selectedFileName = ref('')

const centerNavItems = [
  { label: '产品', to: { name: 'placeholder', query: { label: '产品' } } },
  { label: '热门精选总结', to: { name: 'placeholder', query: { label: '热门精选总结' } }, hot: true },
  { label: '安装插件', to: { name: 'placeholder', query: { label: '安装插件' } } },
  { label: '价格', to: { name: 'placeholder', query: { label: '价格' } } },
  { label: '常见问题', to: { name: 'placeholder', query: { label: '常见问题' } } },
  { label: '博客', to: { name: 'placeholder', query: { label: '博客' } } },
]

const avatarLabel = computed(() => {
  const seed = authStore.currentUser?.userName || authStore.currentUser?.userAccount || 'U'
  return seed.trim().charAt(0).toUpperCase()
})

const navItems = computed(() => [
  { label: 'Workspace', to: '/dashboard', kind: 'workspace' as const, icon: '▣' },
  { label: avatarLabel.value, to: '/dashboard', kind: 'avatar' as const },
])

const statusFilters = [
  { label: '全部', value: '' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '等待中', value: 'WAITING' },
  { label: '已完成', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
]

const allVideosQuery = useDashboardVideosQuery('')
const videosQuery = useDashboardVideosQuery(selectedStatus)

const allVideoRecords = computed(() => allVideosQuery.data.value?.records ?? [])
const videoRecords = computed(() => videosQuery.data.value?.records ?? [])
const dashboardLoading = computed(() => videosQuery.isLoading.value && !videoRecords.value.length)
const dashboardHasError = computed(() => videosQuery.isError.value)
const dashboardErrorMessage = computed(
  () => videosQuery.error.value?.message || '视频列表加载失败，请稍后重试',
)

const displayName = computed(
  () => authStore.currentUser?.userName || authStore.currentUser?.userAccount || '用户',
)

const greeting = computed(() => {
  const hour = new Date().getHours()

  if (hour < 12) {
    return '早上好'
  }

  if (hour < 18) {
    return '下午好'
  }

  return '晚上好'
})

const stats = computed(() => {
  const records = allVideoRecords.value
  const total = Number(allVideosQuery.data.value?.total ?? records.length)
  const processing = records.filter((item) => ['WAITING', 'PROCESSING'].includes(item.status)).length
  const success = records.filter((item) => item.status === 'SUCCESS').length
  const failed = records.filter((item) => item.status === 'FAILED').length

  return [
    { label: '全部视频', value: total, tone: 'violet' },
    { label: '处理中', value: processing, tone: 'amber' },
    { label: '已完成', value: success, tone: 'emerald' },
    { label: '失败', value: failed, tone: 'rose' },
  ]
})

type StatusMeta = {
  label: string
  tone: string
  actionLabel: string
  placeholder: string
}

const defaultStatusMeta: StatusMeta = {
  label: '等待处理',
  tone: 'waiting',
  actionLabel: '查看进度',
  placeholder: '队列中，稍后会开始转录和总结。',
}

const statusMetaMap: Record<string, StatusMeta> = {
    WAITING: {
      label: '等待处理',
      tone: 'waiting',
      actionLabel: '查看进度',
      placeholder: '队列中，稍后会开始转录和总结。',
    },
    PROCESSING: {
      label: '处理中',
      tone: 'processing',
      actionLabel: '查看进度',
      placeholder: '正在生成摘要、章节和转录结果。',
    },
    SUCCESS: {
      label: '已完成',
      tone: 'success',
      actionLabel: '查看结果',
      placeholder: '结果已经准备好，可以继续查看。',
    },
    FAILED: {
      label: '处理失败',
      tone: 'failed',
      actionLabel: '查看详情',
      placeholder: '处理未完成，进入详情页查看原因。',
    },
}

function getStatusMeta(status: string): StatusMeta {
  return statusMetaMap[status] ?? defaultStatusMeta
}

function pickFile() {
  inputRef.value?.click()
}

async function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) {
    return
  }

  selectedFileName.value = file.name
  uploadErrorMessage.value = ''
  uploadPending.value = true

  try {
    const result = await uploadVideo(file)
    await queryClient.invalidateQueries({ queryKey: ['dashboard-videos'] })
    await router.push(`/video/${result.id}`)
  } catch (error) {
    uploadErrorMessage.value = error instanceof Error ? error.message : '上传失败，请稍后重试'
  } finally {
    uploadPending.value = false
    target.value = ''
  }
}

function openVideo(item: { id: EntityId }) {
  void router.push(`/video/${item.id}`)
}

function summaryPreview(item: VideoDashboardVO) {
  return item.summary || getStatusMeta(item.status).placeholder
}
</script>

<template>
  <AppLayout
    title="Workspace"
    description="查看你最近上传的视频与处理状态。"
    :center-nav-items="centerNavItems"
    locale-label="简体中文"
    :nav-items="navItems"
    :hide-hero="true"
    wide
  >
    <section class="dashboard-page">
      <section class="dashboard-hero">
        <div class="dashboard-copy">
          <h1>{{ greeting }}，{{ displayName }}</h1>
        </div>

        <div class="dashboard-upload panel">
          <div class="upload-lead">
            <span class="upload-icon">＋</span>
            <div class="upload-copy">
              <strong>上传新视频</strong>
              <span>{{ selectedFileName || '本地 MP4' }}</span>
            </div>
          </div>
          <button class="upload-trigger" :disabled="uploadPending" type="button" @click="pickFile">
            {{ uploadPending ? '上传中...' : '选择 MP4' }}
          </button>
        </div>
      </section>

      <p v-if="uploadErrorMessage" class="dashboard-error">{{ uploadErrorMessage }}</p>

      <section class="stats-grid">
        <article
          v-for="item in stats"
          :key="item.label"
          class="stat-card"
          :data-tone="item.tone"
        >
          <span class="stat-label">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </section>

      <section class="workspace-panel panel">
        <div class="workspace-head">
          <div>
            <h2>最近视频</h2>
          </div>

          <div class="filter-pills">
            <button
              v-for="item in statusFilters"
              :key="item.value || 'all'"
              type="button"
              :class="{ active: selectedStatus === item.value }"
              @click="selectedStatus = item.value"
            >
              {{ item.label }}
            </button>
          </div>
        </div>

        <div class="workspace-body" :class="{ 'workspace-body--compact': !videoRecords.length }">
          <p v-if="dashboardLoading" class="workspace-feedback">正在加载最近视频...</p>
          <p v-else-if="dashboardHasError" class="workspace-feedback workspace-feedback--error">
            {{ dashboardErrorMessage }}
          </p>
          <div v-else-if="videoRecords.length" class="video-grid">
          <article
            v-for="item in videoRecords"
            :key="item.id"
            class="video-card"
            :data-tone="getStatusMeta(item.status).tone"
          >
            <div class="video-card-head">
              <div class="video-card-copy">
                <span class="video-time">{{ formatRelativeDate(item.createTime) }}</span>
                <h3>{{ item.title }}</h3>
                </div>
                <span class="status-pill" :data-tone="getStatusMeta(item.status).tone">
                  {{ getStatusMeta(item.status).label }}
                </span>
              </div>

              <p class="video-meta">{{ formatDuration(item.duration) }} · {{ item.format?.toUpperCase?.() || 'MP4' }}</p>
              <p class="video-summary">{{ summaryPreview(item) }}</p>

              <div class="video-card-foot">
                <span class="result-chip" :data-tone="item.summary ? 'ready' : 'pending'">
                  {{ item.summary ? '摘要已生成' : '结果待生成' }}
                </span>
                <button type="button" @click="openVideo(item)">
                  {{ getStatusMeta(item.status).actionLabel }}
                </button>
              </div>
            </article>
          </div>
          <div v-else class="empty-state">
            <strong>{{ selectedStatus ? '当前筛选下还没有视频' : '还没有上传过视频' }}</strong>
            <p>先上传一个本地 MP4 开始处理。</p>
            <button type="button" @click="pickFile">上传视频</button>
          </div>
        </div>
      </section>

      <input
        ref="inputRef"
        class="hidden-input"
        type="file"
        accept="video/mp4"
        @change="onFileChange"
      />
    </section>
  </AppLayout>
</template>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 22px;
  padding: 34px 0 48px;
}

.dashboard-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 408px;
  gap: 20px;
  align-items: end;
}

.dashboard-copy {
  display: grid;
  gap: 10px;
  align-content: start;
}

.dashboard-copy h1,
.workspace-head h2,
.video-card h3 {
  margin: 0;
  color: #1d2235;
}

.dashboard-copy h1 {
  font-size: clamp(34px, 4vw, 52px);
  line-height: 1.04;
  letter-spacing: -0.05em;
}

.dashboard-copy p,
.video-meta,
.video-summary,
.upload-copy span,
.workspace-feedback,
.empty-state p {
  margin: 0;
  color: var(--text-muted);
}

.dashboard-upload {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-height: 108px;
  padding: 18px 18px 18px 20px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(255, 142, 182, 0.06), transparent 32%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.84));
  border: 1px solid rgba(143, 108, 255, 0.08);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.84),
    0 14px 28px rgba(62, 46, 115, 0.045),
    0 3px 8px rgba(62, 46, 115, 0.02);
}

.upload-lead {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.upload-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 16px;
  background: rgba(143, 108, 255, 0.08);
  border: 1px solid rgba(143, 108, 255, 0.1);
  color: var(--accent-strong);
  font-size: 20px;
  line-height: 1;
  flex-shrink: 0;
}

.upload-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.upload-copy strong {
  font-size: 18px;
  letter-spacing: -0.02em;
  color: #262b40;
}

.upload-copy span {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  line-height: 1.4;
  color: #7a7f96;
}

.upload-trigger,
.video-card-foot button,
.empty-state button {
  min-height: 46px;
  border: 0;
  border-radius: 999px;
  background: #7a54ff;
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0.01em;
  cursor: pointer;
  box-shadow: 0 12px 24px rgba(122, 84, 255, 0.16);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    filter 0.2s ease;
}

.upload-trigger {
  min-width: 136px;
  padding: 0 20px;
  flex-shrink: 0;
}

.upload-trigger:hover,
.video-card-foot button:hover,
.empty-state button:hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 28px rgba(122, 84, 255, 0.18);
  filter: saturate(1.03);
}

.upload-trigger:disabled {
  cursor: not-allowed;
  opacity: 0.72;
  box-shadow: none;
}

.dashboard-error {
  margin: 0;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(208, 57, 57, 0.08);
  color: #a02c2c;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  display: grid;
  gap: 14px;
  min-height: 142px;
  padding: 18px 18px 20px;
  border-radius: 22px;
  border: 1px solid rgba(143, 108, 255, 0.08);
  background: rgba(255, 255, 255, 0.84);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.88),
    0 14px 26px rgba(62, 46, 115, 0.035),
    0 3px 8px rgba(62, 46, 115, 0.02);
}

.stat-card::before {
  content: '';
  width: 34px;
  height: 8px;
  border-radius: 999px;
  background: rgba(143, 108, 255, 0.18);
}

.stat-card[data-tone='violet'] {
  background:
    radial-gradient(circle at top left, rgba(143, 108, 255, 0.06), transparent 38%),
    rgba(255, 255, 255, 0.84);
}

.stat-card[data-tone='violet']::before {
  background: rgba(143, 108, 255, 0.26);
}

.stat-card[data-tone='amber'] {
  background:
    radial-gradient(circle at top left, rgba(243, 167, 0, 0.07), transparent 38%),
    rgba(255, 255, 255, 0.84);
}

.stat-card[data-tone='amber']::before {
  background: rgba(243, 167, 0, 0.28);
}

.stat-card[data-tone='emerald'] {
  background:
    radial-gradient(circle at top left, rgba(63, 171, 112, 0.07), transparent 38%),
    rgba(255, 255, 255, 0.84);
}

.stat-card[data-tone='emerald']::before {
  background: rgba(63, 171, 112, 0.28);
}

.stat-card[data-tone='rose'] {
  background:
    radial-gradient(circle at top left, rgba(255, 142, 182, 0.07), transparent 38%),
    rgba(255, 255, 255, 0.84);
}

.stat-card[data-tone='rose']::before {
  background: rgba(255, 142, 182, 0.28);
}

.stat-label {
  font-size: 14px;
  font-weight: 700;
  color: #63677e;
}

.stat-card strong {
  font-size: clamp(34px, 4vw, 46px);
  line-height: 1;
  letter-spacing: -0.04em;
  color: #1e2336;
}

.workspace-panel {
  display: grid;
  gap: 18px;
  padding: 24px;
  border-radius: 30px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.86)),
    radial-gradient(circle at top right, rgba(143, 108, 255, 0.045), transparent 36%);
  border: 1px solid rgba(143, 108, 255, 0.08);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 18px 34px rgba(62, 46, 115, 0.04),
    0 3px 10px rgba(62, 46, 115, 0.02);
}

.workspace-body {
  min-height: 760px;
  align-content: start;
}

.workspace-body--compact {
  min-height: 420px;
}

.workspace-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.workspace-head h2 {
  font-size: 28px;
  letter-spacing: -0.04em;
}

.filter-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-pills button {
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid rgba(143, 108, 255, 0.08);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  color: #666b82;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    border-color 0.2s ease,
    transform 0.2s ease;
}

.filter-pills button:hover,
.filter-pills button.active {
  background: rgba(143, 108, 255, 0.08);
  border-color: rgba(143, 108, 255, 0.12);
  color: #32364d;
  transform: translateY(-1px);
}

.workspace-feedback {
  padding: 10px 2px;
  font-size: 15px;
}

.workspace-feedback--error {
  color: #a02c2c;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.video-card {
  display: grid;
  gap: 14px;
  min-height: 232px;
  padding: 20px;
  border-radius: 24px;
  border: 1px solid rgba(143, 108, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.84)),
    radial-gradient(circle at top right, rgba(143, 108, 255, 0.035), transparent 36%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.88),
    0 10px 20px rgba(62, 46, 115, 0.03);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.video-card[data-tone='success'] {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(249, 255, 252, 0.84)),
    radial-gradient(circle at top right, rgba(63, 171, 112, 0.06), transparent 36%);
  border-color: rgba(63, 171, 112, 0.14);
}

.video-card[data-tone='processing'],
.video-card[data-tone='waiting'] {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(255, 252, 245, 0.84)),
    radial-gradient(circle at top right, rgba(243, 167, 0, 0.06), transparent 36%);
  border-color: rgba(243, 167, 0, 0.16);
}

.video-card[data-tone='failed'] {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(255, 248, 248, 0.84)),
    radial-gradient(circle at top right, rgba(208, 57, 57, 0.05), transparent 36%);
  border-color: rgba(208, 57, 57, 0.16);
}

.video-card:hover {
  transform: translateY(-3px);
  border-color: rgba(143, 108, 255, 0.14);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.92),
    0 16px 30px rgba(62, 46, 115, 0.045);
}

.video-card-head,
.video-card-foot {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.video-card-copy {
  display: grid;
  gap: 6px;
}

.video-time {
  font-size: 13px;
  color: #7b7f93;
}

.video-card h3 {
  font-size: 22px;
  line-height: 1.16;
  letter-spacing: -0.03em;
  display: -webkit-box;
  min-height: 2.32em;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
}

.status-pill,
.result-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.status-pill::before {
  content: '';
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: currentColor;
  opacity: 0.9;
}

.status-pill[data-tone='waiting'],
.status-pill[data-tone='processing'] {
  background: rgba(243, 167, 0, 0.1);
  color: #9b6900;
}

.status-pill[data-tone='success'] {
  background: rgba(63, 171, 112, 0.1);
  color: #26724c;
}

.status-pill[data-tone='failed'] {
  background: rgba(208, 57, 57, 0.08);
  color: #a02c2c;
}

.video-meta {
  font-size: 14px;
}

.video-summary {
  font-size: 15px;
  line-height: 1.7;
  display: -webkit-box;
  min-height: 5.1em;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  line-clamp: 3;
}

.result-chip[data-tone='ready'] {
  background: rgba(143, 108, 255, 0.08);
  color: var(--accent-strong);
}

.result-chip[data-tone='pending'] {
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(143, 108, 255, 0.08);
  color: #767b91;
}

.video-card-foot {
  margin-top: auto;
  align-items: center;
}

.video-card-foot button,
.empty-state button {
  padding: 0 18px;
}

.video-card-foot button {
  min-width: 108px;
}

.empty-state {
  display: grid;
  justify-items: center;
  gap: 12px;
  padding: 40px 24px;
  text-align: center;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.68);
  border: 1px dashed rgba(143, 108, 255, 0.18);
}

.empty-state strong {
  font-size: 22px;
  letter-spacing: -0.03em;
  color: #24293f;
}

.empty-state p {
  font-size: 15px;
}

.hidden-input {
  display: none;
}

@media (max-width: 1080px) {
  .dashboard-hero,
  .video-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-upload {
    justify-content: space-between;
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .dashboard-page {
    gap: 18px;
    padding-top: 18px;
  }

  .dashboard-copy h1 {
    font-size: clamp(30px, 10vw, 42px);
  }

  .dashboard-upload,
  .workspace-head,
  .video-card-head,
  .video-card-foot {
    flex-direction: column;
    align-items: flex-start;
  }

  .workspace-panel {
    padding: 20px;
    border-radius: 24px;
  }

  .workspace-body,
  .workspace-body--compact {
    min-height: auto;
  }

  .video-card {
    min-height: auto;
  }
}
</style>
