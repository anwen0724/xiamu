<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppLayout from '../../layouts/AppLayout.vue'
import { useVideoStatusQuery } from '../../queries'

const route = useRoute()
const router = useRouter()
const videoId = computed(() => String(route.params.id ?? ''))
const statusQuery = useVideoStatusQuery(videoId)

const videoStatus = computed(() => statusQuery.data.value ?? null)
const statusLoading = computed(() => statusQuery.isLoading.value)
const statusHasError = computed(() => statusQuery.isError.value)
const statusErrorMessage = computed(() => statusQuery.error.value?.message || '状态加载失败')

const stageText = computed(() => {
  const status = videoStatus.value?.status

  if (status === 'FAILED') {
    return '处理失败，请查看错误信息并决定是否重试。'
  }

  if (status === 'SUCCESS') {
    return '结果已可读取，现在可以进入结果页查看摘要与章节。'
  }

  if (status === 'PROCESSING') {
    return '系统正在提取音频、转写文本并生成摘要与章节。'
  }

  return '视频已进入队列，正在等待处理。'
})

function openResult() {
  void router.push(`/video/result/${videoId.value}`)
}
</script>

<template>
  <AppLayout
    eyebrow="处理状态"
    title="视频正在生成摘要与章节"
    description="这里会显示当前视频的处理进展，并在成功后进入结果页。"
  >
    <section class="panel status-panel">
      <p v-if="statusLoading" class="status-note">正在读取处理状态...</p>
      <p v-else-if="statusHasError" class="error-note">{{ statusErrorMessage }}</p>
      <template v-else-if="videoStatus">
        <div class="status-pill" :data-status="videoStatus.status">
          {{ videoStatus.status }}
        </div>
        <p class="stage-text">{{ stageText }}</p>

        <ul class="state-list">
          <li :class="{ done: videoStatus.hasTranscript }">已生成转写文本</li>
          <li :class="{ done: videoStatus.hasSummary }">已生成摘要</li>
          <li :class="{ done: videoStatus.hasChapters }">已生成章节</li>
        </ul>

        <p v-if="videoStatus.errorMessage" class="error-note">
          {{ videoStatus.errorMessage }}
        </p>

        <div class="actions">
          <button v-if="videoStatus.status === 'SUCCESS'" type="button" @click="openResult">
            查看结果页
          </button>
          <span v-else class="polling-tip">每 3 秒自动刷新一次</span>
        </div>
      </template>
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

.status-pill[data-status='FAILED'] {
  background: rgba(208, 57, 57, 0.1);
  color: #a02c2c;
}

.status-pill[data-status='WAITING'],
.status-pill[data-status='PROCESSING'] {
  background: rgba(243, 167, 0, 0.12);
  color: #9d6a00;
}

.stage-text,
.status-note {
  margin: 16px 0 0;
  color: var(--text-muted);
}

.state-list {
  margin: 20px 0 0;
  padding-left: 18px;
  color: var(--text-muted);
}

.state-list li {
  margin-bottom: 10px;
}

.state-list li.done {
  color: #2e7d51;
}

.error-note {
  margin: 18px 0 0;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(208, 57, 57, 0.08);
  color: #a02c2c;
}

.actions {
  margin-top: 18px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.actions button {
  min-height: 42px;
  padding: 0 16px;
  border: 0;
  border-radius: 999px;
  background: var(--accent);
  color: #fff;
  cursor: pointer;
}

.polling-tip {
  color: var(--text-muted);
}
</style>
