<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import { addNote, deleteNote } from '../../api/note'
import { openVideoStatusStream, retryVideo } from '../../api/video'
import type { EntityId, VideoChapterVO, VideoProcessStatusVO } from '../../api/types'
import AppLayout from '../../layouts/AppLayout.vue'
import { useVideoNotesQuery, useVideoResultQuery, useVideoStatusQuery } from '../../queries'
import { formatRelativeDate } from '../../utils'

type LeftPanelMode = 'transcript' | 'subtitle'
type RightPanelTab = 'summary' | 'mindmap' | 'notes'
type TranscriptEntry = {
  id: string
  text: string
  timeSeconds: number
  label: string
  speaker: string
}
type SubtitleBlock = {
  id: string
  startLabel: string
  text: string
  count: number
}
type MindMapNode = {
  id: string
  label: string
  tone: 'primary' | 'secondary' | 'accent'
  top: string
  left: string
}

const stageSteps = [
  { code: 'QUEUED', label: '等待处理', percent: 10 },
  { code: 'EXTRACTING_AUDIO', label: '提取音频', percent: 25 },
  { code: 'TRANSCRIBING', label: '转写文本', percent: 55 },
  { code: 'GENERATING_SUMMARY', label: '生成总结', percent: 78 },
  { code: 'GENERATING_CHAPTERS', label: '整理章节', percent: 92 },
  { code: 'COMPLETED', label: '处理完成', percent: 100 },
] as const

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const videoId = computed(() => String(route.params.id ?? ''))

const leftMode = ref<LeftPanelMode>('transcript')
const rightTab = ref<RightPanelTab>('summary')
const selectedTranscriptIndex = ref(0)

const statusQuery = useVideoStatusQuery(videoId)
const status = computed(() => statusQuery.data.value ?? null)
const isSuccess = computed(() => status.value?.status === 'SUCCESS')
const isFailed = computed(() => status.value?.status === 'FAILED')
const isTerminal = computed(() => isSuccess.value || isFailed.value)

const resultQuery = useVideoResultQuery(videoId, isSuccess)
const notesQuery = useVideoNotesQuery(videoId, isSuccess)
const result = computed(() => resultQuery.data.value ?? null)
const notes = computed(() => notesQuery.data.value ?? [])
const chapters = computed(() => result.value?.chapters ?? [])

const statusLoading = computed(() => statusQuery.isLoading.value)
const statusHasError = computed(() => statusQuery.isError.value)
const resultLoading = computed(() => resultQuery.isLoading.value)
const resultHasError = computed(() => resultQuery.isError.value)
const notesLoading = computed(() => notesQuery.isLoading.value)
const notesHasError = computed(() => notesQuery.isError.value)

const displayTitle = computed(() => result.value?.title || status.value?.title || '视频详情')
const updatedAt = computed(() => result.value?.updateTime || status.value?.updateTime || null)
const resultErrorMessage = computed(() => resultQuery.error.value?.message || '结果内容加载失败')
const notesErrorMessage = computed(() => notesQuery.error.value?.message || '笔记加载失败')
const streamState = ref<'idle' | 'live' | 'reconnecting'>('idle')
const streamMessage = ref('')
const retryPending = ref(false)
const retryError = ref('')

const noteForm = reactive({
  title: '',
  content: '',
  timeSeconds: '',
})
const notePending = ref(false)
const noteError = ref('')
const noteSuccess = ref('')
const deletingId = ref<EntityId | null>(null)

const summaryBlocks = computed(() => splitSummary(result.value?.summary))
const keywordTags = computed(() => buildKeywordTags(chapters.value, summaryBlocks.value))
const transcriptEntries = computed(() =>
  buildTranscriptEntries(result.value?.transcript, result.value?.duration ?? status.value?.duration ?? 0),
)
const subtitleBlocks = computed(() => buildSubtitleBlocks(transcriptEntries.value))
const currentCue = computed(() => transcriptEntries.value[selectedTranscriptIndex.value] ?? transcriptEntries.value[0] ?? null)
const headerChapters = computed(() => chapters.value.slice(0, 4))
const chapterPreview = computed(() => chapters.value.slice(0, 6))
const progressPercent = computed(() => {
  const index = stageSteps.findIndex((item) => item.code === status.value?.stage)
  const percent = stageSteps[Math.max(index, 0)]?.percent ?? 0
  return isFailed.value ? Math.max(percent, 14) : percent
})
const stageLabel = computed(() => status.value?.stageMessage || '处理中')
const leftCountLabel = computed(() =>
  leftMode.value === 'transcript' ? `${transcriptEntries.value.length} 条字幕` : `${subtitleBlocks.value.length} 条合并字幕`,
)
const mindMapNodes = computed(() => buildMindMapNodes(keywordTags.value, chapters.value))

let eventSource: EventSource | null = null
let fallbackTimer: number | null = null

watch(
  transcriptEntries,
  (entries) => {
    if (!entries.length) {
      selectedTranscriptIndex.value = 0
      return
    }
    if (selectedTranscriptIndex.value >= entries.length) {
      selectedTranscriptIndex.value = 0
    }
  },
  { immediate: true },
)

watch(
  () => statusQuery.isSuccess.value && !isTerminal.value,
  (shouldStream) => {
    if (shouldStream) startStream()
    else closeStream()
  },
  { immediate: true },
)

watch(videoId, () => {
  leftMode.value = 'transcript'
  rightTab.value = 'summary'
  selectedTranscriptIndex.value = 0
  retryError.value = ''
  streamMessage.value = ''
})

onBeforeUnmount(closeStream)

function splitSummary(summary?: string | null) {
  if (!summary?.trim()) return []
  const text = summary.replace(/\r/g, '').trim()
  const chunks = text.includes('\n') ? text.split('\n') : (text.match(/[^。！？!?；;]+[。！？!?；;]?/g) ?? [text])
  const rows = chunks.map((item) => item.trim()).filter(Boolean)
  const blocks: string[] = []
  for (let i = 0; i < rows.length; i += 2) {
    blocks.push(rows.slice(i, i + 2).join(' ').trim())
  }
  return blocks
}

function buildKeywordTags(items: VideoChapterVO[], blocks: string[]) {
  const chapterTags = items
    .map((item) => item.title.replace(/[：:，,。.!！?？]/g, '').trim())
    .filter(Boolean)
  const summaryTags = blocks
    .flatMap((block) => block.split(/[，,。.!！?？；;]/))
    .map((item) => item.trim())
    .filter((item) => item.length >= 4 && item.length <= 10)
  return [...new Set([...chapterTags, ...summaryTags])].slice(0, 6)
}

function buildTranscriptEntries(transcript?: string | null, duration = 0): TranscriptEntry[] {
  if (!transcript?.trim()) return []
  const lines = transcript
    .replace(/\r/g, '')
    .split(/\n+|(?<=[。！？!?；;])/)
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 120)

  const safeDuration = duration > 0 ? duration : lines.length * 7
  return lines.map((text, index) => {
    const seconds = Math.floor((safeDuration / Math.max(lines.length, 1)) * index)
    return {
      id: `${index}-${text.slice(0, 12)}`,
      text,
      timeSeconds: seconds,
      label: formatClock(seconds),
      speaker: '发言人',
    }
  })
}

function buildSubtitleBlocks(entries: TranscriptEntry[]): SubtitleBlock[] {
  if (!entries.length) return []
  const blocks: SubtitleBlock[] = []
  for (let i = 0; i < entries.length; i += 3) {
    const group = entries.slice(i, i + 3)
    const first = group[0]
    if (!first) continue
    blocks.push({
      id: `block-${first.id}`,
      startLabel: first.label,
      text: group.map((item) => item.text).join(' '),
      count: group.length,
    })
  }
  return blocks
}

function buildMindMapNodes(tags: string[], items: VideoChapterVO[]): MindMapNode[] {
  const labels = [...tags, ...items.map((item) => item.title)].filter(Boolean)
  const picks = [...new Set(labels)].slice(0, 6)
  const positions = [
    { top: '12%', left: '40%', tone: 'primary' },
    { top: '24%', left: '73%', tone: 'secondary' },
    { top: '56%', left: '77%', tone: 'accent' },
    { top: '75%', left: '46%', tone: 'primary' },
    { top: '58%', left: '12%', tone: 'secondary' },
    { top: '25%', left: '10%', tone: 'accent' },
  ] as const

  return picks.map((label, index) => ({
    id: `${label}-${index}`,
    label,
    top: positions[index]?.top ?? '50%',
    left: positions[index]?.left ?? '50%',
    tone: positions[index]?.tone ?? 'secondary',
  }))
}

function formatClock(totalSeconds = 0) {
  const minutes = Math.floor(Math.max(0, totalSeconds) / 60)
  const seconds = Math.max(0, totalSeconds) % 60
  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
}

function chapterRange(chapter: VideoChapterVO) {
  return `${formatClock(chapter.startSeconds)} - ${formatClock(chapter.endSeconds)}`
}

function selectTranscript(index: number) {
  selectedTranscriptIndex.value = index
}

function selectSubtitleBlock(index: number) {
  const nextIndex = index * 3
  if (nextIndex < transcriptEntries.value.length) {
    selectedTranscriptIndex.value = nextIndex
  }
}

function selectChapter(chapter: VideoChapterVO) {
  rightTab.value = 'summary'
  noteForm.title = chapter.title
  noteForm.timeSeconds = String(chapter.startSeconds)
  const index = transcriptEntries.value.findIndex((item) => item.timeSeconds >= chapter.startSeconds)
  if (index >= 0) {
    selectedTranscriptIndex.value = index
  }
}

function closeStream() {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
  if (fallbackTimer != null) {
    window.clearInterval(fallbackTimer)
    fallbackTimer = null
  }
  if (!isTerminal.value) {
    streamState.value = 'idle'
  }
}

function startStream() {
  if (!videoId.value || !status.value || isTerminal.value || eventSource) return
  const source = openVideoStatusStream(videoId.value)
  eventSource = source
  streamState.value = 'reconnecting'

  source.onopen = () => {
    streamState.value = 'live'
    streamMessage.value = ''
    if (fallbackTimer != null) {
      window.clearInterval(fallbackTimer)
      fallbackTimer = null
    }
  }

  source.addEventListener('status', (event) => {
    const nextStatus = JSON.parse((event as MessageEvent<string>).data) as VideoProcessStatusVO
    queryClient.setQueryData(['video-status', videoId.value], nextStatus)
    streamState.value = 'live'
    streamMessage.value = ''
    if (nextStatus.status === 'SUCCESS' || nextStatus.status === 'FAILED') {
      closeStream()
      void queryClient.invalidateQueries({ queryKey: ['video-result', videoId.value] })
      void queryClient.invalidateQueries({ queryKey: ['video-notes', videoId.value] })
    }
  })

  source.onerror = () => {
    streamState.value = 'reconnecting'
    streamMessage.value = '实时连接中断，已自动切换为低频刷新。'
    if (fallbackTimer == null) {
      fallbackTimer = window.setInterval(() => {
        void queryClient.invalidateQueries({ queryKey: ['video-status', videoId.value] })
      }, 10000)
    }
  }
}

async function handleAddNote() {
  noteError.value = ''
  noteSuccess.value = ''
  notePending.value = true
  try {
    await addNote({
      videoId: videoId.value,
      title: noteForm.title.trim(),
      content: noteForm.content.trim(),
      timeSeconds: noteForm.timeSeconds ? Number(noteForm.timeSeconds) : undefined,
    })
    noteForm.title = ''
    noteForm.content = ''
    noteForm.timeSeconds = ''
    noteSuccess.value = '笔记已保存'
    await queryClient.invalidateQueries({ queryKey: ['video-notes', videoId.value] })
  } catch (error) {
    noteError.value = error instanceof Error ? error.message : '保存笔记失败'
  } finally {
    notePending.value = false
  }
}

async function handleDeleteNote(id: EntityId) {
  deletingId.value = id
  noteError.value = ''
  noteSuccess.value = ''
  try {
    await deleteNote(id)
    noteSuccess.value = '笔记已删除'
    await queryClient.invalidateQueries({ queryKey: ['video-notes', videoId.value] })
  } catch (error) {
    noteError.value = error instanceof Error ? error.message : '删除笔记失败'
  } finally {
    deletingId.value = null
  }
}

async function handleRetry() {
  retryPending.value = true
  retryError.value = ''
  try {
    await retryVideo(videoId.value)
    queryClient.removeQueries({ queryKey: ['video-result', videoId.value] })
    queryClient.removeQueries({ queryKey: ['video-notes', videoId.value] })
    await queryClient.invalidateQueries({ queryKey: ['video-status', videoId.value] })
    closeStream()
    startStream()
  } catch (error) {
    retryError.value = error instanceof Error ? error.message : '重试失败，请稍后再试'
  } finally {
    retryPending.value = false
  }
}

async function handleManualRefresh() {
  await queryClient.invalidateQueries({ queryKey: ['video-status', videoId.value] })
}

function goBack() {
  void router.push('/dashboard')
}
</script>

<template>
  <AppLayout hide-hero compact title="" description="">
    <div class="detail-page">
      <section class="workspace-top panel">
        <div class="workspace-left">
          <button class="icon-button" type="button" @click="goBack">‹</button>
          <div>
            <p class="workspace-label">结果详情</p>
            <h1>{{ displayTitle }}</h1>
          </div>
        </div>
        <div class="workspace-right">
          <button class="top-action" type="button">分享</button>
          <button class="top-action" type="button" @click="rightTab = 'notes'">备忘录</button>
          <span class="status-chip" :data-state="status?.status">{{ isSuccess ? '处理完成' : stageLabel }}</span>
        </div>
      </section>

      <p v-if="statusLoading && !status" class="status-copy">正在读取视频状态...</p>
      <p v-else-if="statusHasError && !status" class="error-note">{{ statusQuery.error.value?.message || '视频状态加载失败' }}</p>

      <template v-else-if="status && isSuccess">
        <section class="workspace-layout">
          <div class="left-stage">
            <div class="player-nav-row">
              <div class="left-tabs panel-lite">
                <button class="left-tab" :class="{ active: leftMode === 'transcript' }" type="button" @click="leftMode = 'transcript'">
                  <span class="tab-icon">▣</span>
                  语音转文本
                </button>
                <button class="left-tab" :class="{ active: leftMode === 'subtitle' }" type="button" @click="leftMode = 'subtitle'">
                  <span class="tab-icon">CC</span>
                  原始字幕
                </button>
              </div>
            </div>

            <article class="player-card panel">
              <div class="chapter-ribbon">
                <span
                  v-for="chapter in headerChapters"
                  :key="`${chapter.title}-${chapter.startSeconds}`"
                  class="chapter-ribbon-item"
                  @click="selectChapter(chapter)"
                >
                  {{ chapter.title }}
                </span>
              </div>
              <div class="player-frame">
                <div class="placeholder-overlay">
                  <div class="placeholder-badge">视频占位区</div>
                  <div class="placeholder-sun" />
                  <div class="placeholder-figure" />
                  <div class="placeholder-bottom" />
                </div>
                <div class="subtitle-overlay">
                  <strong>{{ currentCue?.text || summaryBlocks[0] || '处理完成后，这里将展示视频播放区与字幕联动内容。' }}</strong>
                </div>
                <div class="player-controls">
                  <button class="control-button" type="button">▶</button>
                  <span class="control-bar"><span class="control-progress" /></span>
                  <span class="control-time">{{ currentCue?.label || '00:00' }}</span>
                  <span class="control-time">{{ formatClock(result?.duration ?? status.duration) }}</span>
                </div>
              </div>
            </article>

            <article class="timeline-card panel">
              <div class="timeline-head">
                <div>
                  <strong>{{ leftMode === 'transcript' ? '字幕时间轴' : '原始字幕' }}</strong>
                  <p>{{ leftCountLabel }}</p>
                </div>
                <div v-if="leftMode === 'subtitle'" class="timeline-merge">
                  <span>合并</span>
                  <span class="merge-switch"><span class="merge-dot" /></span>
                  <span>{{ subtitleBlocks.length }} 条</span>
                </div>
                <button v-else class="tiny-action" type="button" @click="rightTab = 'summary'">查看总结</button>
              </div>

              <div v-if="leftMode === 'transcript'" class="speech-list">
                <button
                  v-for="(entry, index) in transcriptEntries"
                  :key="entry.id"
                  class="speech-row"
                  :class="{ active: index === selectedTranscriptIndex }"
                  type="button"
                  @click="selectTranscript(index)"
                >
                  <span class="speaker-mark">S</span>
                  <div class="speech-meta">
                    <span>{{ entry.speaker }}</span>
                    <em>{{ entry.label }}</em>
                  </div>
                  <p>{{ entry.text }}</p>
                  <span class="row-tools">✧</span>
                </button>
              </div>

              <div v-else class="subtitle-list">
                <button
                  v-for="(block, index) in subtitleBlocks"
                  :key="block.id"
                  class="subtitle-row"
                  :class="{ active: selectedTranscriptIndex >= index * 3 && selectedTranscriptIndex < index * 3 + block.count }"
                  type="button"
                  @click="selectSubtitleBlock(index)"
                >
                  <span>{{ block.startLabel }}</span>
                  <p>{{ block.text }}</p>
                </button>
              </div>

              <p v-if="!transcriptEntries.length" class="empty-copy">当前没有可展示的转写文本。</p>
            </article>
          </div>

          <aside class="right-stage panel">
            <div class="right-tabs">
              <button class="right-tab" :class="{ active: rightTab === 'summary' }" type="button" @click="rightTab = 'summary'">总结</button>
              <button class="right-tab" :class="{ active: rightTab === 'mindmap' }" type="button" @click="rightTab = 'mindmap'">思维导图</button>
              <button class="right-tab" :class="{ active: rightTab === 'notes' }" type="button" @click="rightTab = 'notes'">备忘录</button>
            </div>

            <div v-if="rightTab === 'summary'" class="right-body summary-body">
              <section class="summary-section">
                <div class="section-headline">
                  <strong>关键词</strong>
                </div>
                <div class="keyword-row">
                  <span v-for="tag in keywordTags" :key="tag" class="keyword-chip">{{ tag }}</span>
                </div>
              </section>

              <section class="summary-section">
                <div class="section-headline">
                  <strong>概要</strong>
                  <span>{{ formatRelativeDate(updatedAt) }}</span>
                </div>
                <p v-if="resultLoading" class="status-copy">正在整理摘要...</p>
                <p v-else-if="resultHasError" class="error-note compact">{{ resultErrorMessage }}</p>
                <div v-else-if="summaryBlocks.length" class="summary-paragraphs">
                  <p v-for="block in summaryBlocks" :key="block">{{ block }}</p>
                </div>
                <p v-else class="empty-copy">当前还没有可展示的概要内容。</p>
              </section>

              <section class="summary-section">
                <div class="section-headline">
                  <strong>章节速览</strong>
                  <span>{{ chapterPreview.length }} 个章节</span>
                </div>
                <div class="chapter-preview-list">
                  <button
                    v-for="chapter in chapterPreview"
                    :key="`${chapter.title}-${chapter.startSeconds}`"
                    class="chapter-preview-card"
                    type="button"
                    @click="selectChapter(chapter)"
                  >
                    <span>{{ chapterRange(chapter) }}</span>
                    <strong>{{ chapter.title }}</strong>
                    <p>{{ chapter.summary || '暂无章节说明' }}</p>
                  </button>
                </div>
                <p v-if="!chapterPreview.length" class="empty-copy">当前还没有章节结果。</p>
              </section>
            </div>

            <div v-else-if="rightTab === 'mindmap'" class="right-body mindmap-body">
              <div class="mindmap-shell">
                <div class="mindmap-center">
                  <small>视频主题</small>
                  <strong>{{ displayTitle }}</strong>
                </div>
                <span
                  v-for="node in mindMapNodes"
                  :key="node.id"
                  class="mindmap-node"
                  :class="node.tone"
                  :style="{ top: node.top, left: node.left }"
                >
                  {{ node.label }}
                </span>
                <span class="mindmap-ring ring-one" />
                <span class="mindmap-ring ring-two" />
              </div>
              <p class="mindmap-note">当前使用关键词、章节标题和摘要内容构造导图占位布局，后续如果要接真实导图数据，可以直接替换节点来源。</p>
            </div>

            <div v-else class="right-body notes-body">
              <section class="note-editor">
                <div class="section-headline">
                  <strong>记录笔记</strong>
                  <span>{{ notes.length }} 条</span>
                </div>
                <label class="field">
                  <span>标题</span>
                  <input v-model="noteForm.title" type="text" maxlength="120" placeholder="例如：第 2 章里的核心论点" />
                </label>
                <div class="field-row">
                  <label class="field">
                    <span>时间点</span>
                    <input v-model="noteForm.timeSeconds" type="number" min="0" placeholder="例如：120" />
                  </label>
                  <button class="save-button" type="button" :disabled="notePending" @click="handleAddNote">
                    {{ notePending ? '保存中...' : '保存笔记' }}
                  </button>
                </div>
                <label class="field">
                  <span>内容</span>
                  <textarea v-model="noteForm.content" rows="5" maxlength="2000" placeholder="记录你的想法、摘录或下一步行动。" />
                </label>
                <p v-if="noteError" class="error-note compact">{{ noteError }}</p>
                <p v-if="noteSuccess" class="success-note compact">{{ noteSuccess }}</p>
              </section>

              <section class="note-history">
                <div class="section-headline">
                  <strong>已有笔记</strong>
                  <span>{{ notes.length }} 条</span>
                </div>
                <p v-if="notesLoading" class="status-copy">正在读取笔记...</p>
                <p v-else-if="notesHasError" class="error-note compact">{{ notesErrorMessage }}</p>
                <div v-else-if="notes.length" class="note-list">
                  <article v-for="note in notes" :key="note.id" class="note-card">
                    <div class="note-card-head">
                      <div>
                        <strong>{{ note.title || '未命名笔记' }}</strong>
                        <p>{{ note.timeSeconds != null ? formatClock(note.timeSeconds) : '未绑定时间点' }}</p>
                      </div>
                      <button class="tiny-action danger" type="button" :disabled="deletingId === note.id" @click="handleDeleteNote(note.id)">
                        {{ deletingId === note.id ? '删除中...' : '删除' }}
                      </button>
                    </div>
                    <p class="note-content">{{ note.content }}</p>
                    <span class="note-time">{{ formatRelativeDate(note.updateTime) }}</span>
                  </article>
                </div>
                <p v-else class="empty-copy">还没有笔记，可以先在上方记录。</p>
              </section>
            </div>
          </aside>
        </section>
      </template>

      <template v-else-if="status">
        <section class="processing-layout">
          <article class="processing-main panel">
            <div class="processing-head">
              <div>
                <span class="status-chip" :data-state="status.status">{{ status.status }}</span>
                <h2>{{ stageLabel }}</h2>
                <p>{{ streamState === 'live' ? '状态已接入实时推送。' : streamMessage || '当前详情页会自动刷新处理进度。' }}</p>
              </div>
              <strong class="progress-value">{{ progressPercent }}%</strong>
            </div>
            <div class="progress-track"><span class="progress-fill" :style="{ width: `${progressPercent}%` }" /></div>
            <div class="progress-grid">
              <article
                v-for="step in stageSteps"
                :key="step.code"
                class="progress-card"
                :data-state="(status.stage === step.code && isFailed) ? 'failed' : status.stage === step.code ? 'active' : step.percent < progressPercent ? 'done' : 'pending'"
              >
                <span>{{ step.percent }}%</span>
                <strong>{{ step.label }}</strong>
              </article>
            </div>
            <div class="artifact-strip">
              <span :class="{ done: status.hasTranscript }">转写</span>
              <span :class="{ done: status.hasSummary }">总结</span>
              <span :class="{ done: status.hasChapters }">章节</span>
            </div>
            <p v-if="status.errorMessage" class="error-note compact">{{ status.errorMessage }}</p>
            <p v-if="retryError" class="error-note compact">{{ retryError }}</p>
            <div class="processing-actions">
              <button class="top-action" type="button" @click="handleManualRefresh">手动刷新</button>
              <button v-if="isFailed" class="save-button" type="button" :disabled="retryPending" @click="handleRetry">
                {{ retryPending ? '重试中...' : '重新处理' }}
              </button>
            </div>
          </article>

          <article class="processing-side panel">
            <strong>当前产物</strong>
            <div class="processing-checklist">
              <div class="check-item" :class="{ done: status.hasTranscript }">
                <span>01</span>
                <p>转写文本{{ status.hasTranscript ? '已生成' : '待生成' }}</p>
              </div>
              <div class="check-item" :class="{ done: status.hasSummary }">
                <span>02</span>
                <p>总结内容{{ status.hasSummary ? '已生成' : '待生成' }}</p>
              </div>
              <div class="check-item" :class="{ done: status.hasChapters }">
                <span>03</span>
                <p>章节结构{{ status.hasChapters ? '已生成' : '待生成' }}</p>
              </div>
            </div>
          </article>
        </section>
      </template>
    </div>
  </AppLayout>
</template>

<style scoped>
:deep(.page-shell) {
  width: calc(100vw - 24px);
  max-width: none;
  padding: 12px 12px 28px;
}

:deep(.layout-header) {
  padding: 14px 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
}

:deep(main) {
  width: 100%;
}

.detail-page {
  display: grid;
  gap: 14px;
}

.workspace-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
}

.workspace-left,
.workspace-right,
.player-nav-row,
.timeline-head,
.processing-head,
.processing-actions,
.note-card-head,
.section-headline,
.field-row {
  display: flex;
  align-items: center;
}

.workspace-left {
  gap: 12px;
}

.workspace-right,
.field-row,
.processing-actions {
  gap: 10px;
}

.workspace-label {
  margin: 0 0 4px;
  color: #7d748f;
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.workspace-top h1 {
  margin: 0;
  font-size: 28px;
  line-height: 1.05;
}

.icon-button,
.top-action,
.save-button,
.tiny-action,
.left-tab,
.right-tab,
.control-button {
  cursor: pointer;
}

.icon-button,
.top-action,
.tiny-action {
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid rgba(116, 104, 142, 0.14);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: var(--text-main);
}

.icon-button {
  width: 36px;
  padding: 0;
  font-size: 18px;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(92, 201, 130, 0.14);
  color: #26744c;
  font-size: 12px;
  font-weight: 700;
}

.status-chip[data-state='PROCESSING'],
.status-chip[data-state='WAITING'] {
  background: rgba(244, 180, 62, 0.14);
  color: #9a6200;
}

.status-chip[data-state='FAILED'] {
  background: rgba(210, 72, 72, 0.14);
  color: #a13232;
}

.workspace-layout {
  display: grid;
  grid-template-columns: minmax(620px, 0.92fr) minmax(720px, 1.08fr);
  gap: 14px;
  align-items: start;
}

.left-stage,
.right-stage {
  min-width: 0;
}

.left-stage {
  display: grid;
  gap: 12px;
}

.panel-lite {
  display: inline-flex;
  gap: 6px;
  padding: 6px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(116, 104, 142, 0.1);
}

.left-tab,
.right-tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 0 12px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: #7c728f;
  font-size: 13px;
  font-weight: 600;
}

.left-tab.active,
.right-tab.active {
  background: rgba(127, 97, 255, 0.12);
  color: #6a4fe0;
}

.tab-icon {
  font-size: 12px;
  line-height: 1;
}

.player-card {
  overflow: hidden;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
}

.chapter-ribbon {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  background: #0d0d12;
}

.chapter-ribbon-item {
  display: inline-flex;
  justify-content: center;
  padding: 10px 8px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  border-right: 1px solid rgba(255, 255, 255, 0.16);
}

.chapter-ribbon-item:last-child {
  border-right: 0;
}

.player-frame {
  position: relative;
  min-height: 420px;
  background: linear-gradient(180deg, #3f1a1c 0%, #ce643a 52%, #f0d26b 100%);
}

.placeholder-overlay,
.placeholder-bottom,
.subtitle-overlay,
.player-controls {
  position: absolute;
}

.placeholder-overlay {
  inset: 0;
  overflow: hidden;
}

.placeholder-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 2;
  display: inline-flex;
  min-height: 28px;
  padding: 0 10px;
  align-items: center;
  border-radius: 999px;
  background: rgba(13, 13, 18, 0.68);
  color: rgba(255, 255, 255, 0.85);
  font-size: 12px;
}

.placeholder-sun {
  position: absolute;
  top: 66px;
  right: 86px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 240, 138, 1) 0%, rgba(247, 210, 101, 0.95) 62%, rgba(244, 155, 49, 0.38) 100%);
  box-shadow: 0 0 80px rgba(255, 223, 112, 0.38);
}

.placeholder-figure {
  position: absolute;
  left: 40px;
  bottom: 58px;
  width: 220px;
  height: 280px;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.98) 0%, rgba(0, 0, 0, 0.98) 100%);
  clip-path: polygon(28% 0%, 42% 4%, 48% 12%, 52% 22%, 58% 100%, 0% 100%, 4% 74%, 8% 45%, 16% 16%);
  filter: drop-shadow(0 4px 20px rgba(0, 0, 0, 0.36));
}

.placeholder-bottom {
  inset: auto 0 0;
  height: 150px;
  background: linear-gradient(180deg, transparent 0%, rgba(10, 8, 12, 0.76) 100%);
}

.subtitle-overlay {
  left: 30px;
  right: 30px;
  bottom: 56px;
  z-index: 2;
}

.subtitle-overlay strong {
  color: #fff;
  font-size: clamp(23px, 1.7vw, 31px);
  line-height: 1.3;
  text-shadow: 0 8px 22px rgba(0, 0, 0, 0.42);
}

.player-controls {
  left: 14px;
  right: 14px;
  bottom: 12px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 12px;
  background: rgba(8, 8, 13, 0.58);
}

.control-button {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
}

.control-bar {
  flex: 1;
  height: 5px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  overflow: hidden;
}

.control-progress {
  display: block;
  width: 28%;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #9b79ff, #c58dff);
}

.control-time {
  color: rgba(255, 255, 255, 0.84);
  font-size: 12px;
}

.timeline-card,
.right-stage,
.processing-main,
.processing-side {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
}

.timeline-head,
.section-headline,
.note-card-head,
.processing-head {
  justify-content: space-between;
  gap: 12px;
}

.timeline-head {
  margin-bottom: 12px;
}

.timeline-head strong,
.timeline-head p,
.note-card-head strong,
.note-card-head p,
.processing-head h2,
.processing-head p,
.note-content,
.note-time,
.empty-copy,
.status-copy {
  margin: 0;
}

.timeline-head p,
.section-headline span,
.note-card-head p,
.note-time,
.status-copy {
  color: var(--text-muted);
  font-size: 12px;
}

.timeline-merge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #5c536c;
  font-size: 12px;
}

.merge-switch {
  position: relative;
  display: inline-flex;
  width: 38px;
  height: 18px;
  padding: 2px;
  border-radius: 999px;
  background: rgba(127, 97, 255, 0.16);
}

.merge-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #7f61ff;
}

.speech-list,
.subtitle-list,
.chapter-preview-list,
.note-list {
  display: grid;
  gap: 10px;
}

.speech-list,
.subtitle-list,
.right-stage,
.note-history {
  min-height: 0;
}

.speech-list,
.subtitle-list {
  max-height: 440px;
  overflow: auto;
  padding-right: 4px;
}

.speech-row,
.subtitle-row,
.chapter-preview-card {
  width: 100%;
  border: 1px solid rgba(116, 104, 142, 0.1);
  border-radius: 14px;
  background: #fff;
  text-align: left;
}

.speech-row {
  display: grid;
  grid-template-columns: 28px 86px 1fr 18px;
  gap: 12px;
  align-items: start;
  padding: 12px 14px;
}

.speech-row.active,
.subtitle-row.active {
  border-color: rgba(127, 97, 255, 0.34);
  box-shadow: 0 10px 24px rgba(118, 92, 212, 0.1);
}

.speaker-mark {
  display: inline-grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(255, 115, 153, 0.16);
  color: #d45479;
  font-size: 12px;
  font-weight: 700;
}

.speech-meta {
  display: grid;
  gap: 4px;
}

.speech-meta span {
  color: #827892;
  font-size: 12px;
}

.speech-meta em {
  color: #8f69ff;
  font-style: normal;
  font-weight: 700;
}

.speech-row p,
.subtitle-row p,
.chapter-preview-card p,
.summary-paragraphs p,
.mindmap-note,
.note-content,
.check-item p {
  margin: 0;
  color: #463f57;
  line-height: 1.65;
}

.row-tools {
  color: #c1b8d6;
  justify-self: end;
}

.subtitle-row {
  display: grid;
  grid-template-columns: 68px 1fr;
  gap: 14px;
  padding: 12px 14px;
  align-items: start;
}

.subtitle-row span,
.chapter-preview-card span {
  color: #8f69ff;
  font-size: 12px;
  font-weight: 700;
}

.right-stage {
  display: grid;
  gap: 14px;
  max-height: calc(100vh - 134px);
}

.right-tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(116, 104, 142, 0.1);
}

.right-body {
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
}

.summary-body,
.notes-body {
  display: grid;
  gap: 18px;
}

.summary-section,
.note-editor,
.note-history {
  display: grid;
  gap: 12px;
}

.keyword-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.keyword-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(127, 97, 255, 0.1);
  color: #6f53da;
  font-size: 12px;
  font-weight: 700;
}

.summary-paragraphs {
  display: grid;
  gap: 10px;
}

.chapter-preview-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  background: rgba(250, 248, 255, 0.88);
}

.chapter-preview-card strong {
  color: #1f2130;
}

.mindmap-body {
  display: grid;
  gap: 14px;
}

.mindmap-shell {
  position: relative;
  min-height: 540px;
  border-radius: 18px;
  background:
    radial-gradient(circle at center, rgba(127, 97, 255, 0.08), transparent 40%),
    linear-gradient(180deg, rgba(250, 248, 255, 0.9), rgba(244, 240, 252, 0.96));
  border: 1px solid rgba(116, 104, 142, 0.1);
  overflow: hidden;
}

.mindmap-center,
.mindmap-node,
.mindmap-ring {
  position: absolute;
}

.mindmap-center {
  top: 50%;
  left: 50%;
  z-index: 3;
  display: grid;
  gap: 6px;
  width: 220px;
  padding: 18px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(127, 97, 255, 0.16);
  box-shadow: 0 18px 36px rgba(106, 79, 224, 0.12);
  transform: translate(-50%, -50%);
  text-align: center;
}

.mindmap-center small {
  color: #7d748f;
  font-size: 12px;
}

.mindmap-center strong {
  font-size: 20px;
  line-height: 1.3;
}

.mindmap-node {
  z-index: 2;
  display: inline-flex;
  max-width: 170px;
  padding: 10px 14px;
  border-radius: 16px;
  transform: translate(-50%, -50%);
  font-size: 13px;
  font-weight: 700;
  line-height: 1.45;
}

.mindmap-node.primary {
  background: rgba(127, 97, 255, 0.12);
  color: #6648df;
}

.mindmap-node.secondary {
  background: rgba(91, 191, 142, 0.12);
  color: #26744c;
}

.mindmap-node.accent {
  background: rgba(255, 184, 82, 0.15);
  color: #9a6200;
}

.mindmap-ring {
  top: 50%;
  left: 50%;
  border-radius: 50%;
  border: 1px dashed rgba(127, 97, 255, 0.18);
  transform: translate(-50%, -50%);
}

.ring-one {
  width: 320px;
  height: 320px;
}

.ring-two {
  width: 460px;
  height: 460px;
}

.mindmap-note {
  color: var(--text-muted);
}

.field {
  display: grid;
  gap: 8px;
  flex: 1;
}

.field span {
  color: #7d748f;
  font-size: 12px;
}

.field input,
.field textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid rgba(116, 104, 142, 0.12);
  border-radius: 14px;
  background: #fff;
  color: var(--text-main);
}

.field textarea {
  resize: vertical;
}

.save-button {
  min-width: 120px;
  min-height: 44px;
  padding: 0 16px;
  border: 0;
  border-radius: 14px;
  background: linear-gradient(135deg, #8c6fff, #b591ff);
  color: #fff;
  box-shadow: 0 14px 28px rgba(120, 92, 212, 0.18);
}

.save-button:disabled,
.top-action:disabled,
.tiny-action:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.tiny-action.danger {
  color: #a13232;
}

.note-card {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(250, 248, 255, 0.9);
  border: 1px solid rgba(116, 104, 142, 0.08);
}

.note-time {
  display: inline-block;
  margin-top: 10px;
}

.processing-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.16fr) minmax(320px, 0.84fr);
  gap: 14px;
}

.processing-head {
  align-items: flex-start;
  margin-bottom: 16px;
}

.processing-head h2 {
  margin-top: 8px;
  margin-bottom: 8px;
  font-size: 26px;
}

.processing-head p {
  color: var(--text-muted);
}

.progress-value {
  font-family: var(--font-display);
  font-size: 42px;
  line-height: 1;
}

.progress-track {
  position: relative;
  height: 10px;
  margin-bottom: 16px;
  border-radius: 999px;
  background: rgba(30, 30, 45, 0.08);
  overflow: hidden;
}

.progress-fill {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: inherit;
  background: linear-gradient(90deg, #8c6fff, #ffbf71);
}

.progress-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.progress-card {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 14px;
  background: rgba(250, 248, 255, 0.88);
  color: #756c87;
}

.progress-card span {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.progress-card[data-state='active'] {
  background: rgba(127, 97, 255, 0.12);
  color: #6f53da;
}

.progress-card[data-state='done'] {
  background: rgba(91, 191, 142, 0.12);
  color: #26744c;
}

.progress-card[data-state='failed'] {
  background: rgba(210, 72, 72, 0.12);
  color: #a13232;
}

.artifact-strip {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.artifact-strip span {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(116, 104, 142, 0.08);
  color: #73698a;
  font-size: 12px;
}

.artifact-strip span.done {
  background: rgba(91, 191, 142, 0.14);
  color: #26744c;
}

.processing-checklist {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.check-item {
  display: grid;
  grid-template-columns: 34px 1fr;
  gap: 12px;
  align-items: start;
  padding: 14px;
  border-radius: 14px;
  background: rgba(250, 248, 255, 0.88);
}

.check-item span {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(127, 97, 255, 0.12);
  color: #6f53da;
  font-size: 12px;
  font-weight: 700;
}

.check-item.done {
  background: rgba(91, 191, 142, 0.12);
}

.error-note,
.success-note {
  margin: 0;
  padding: 12px 14px;
  border-radius: 14px;
}

.error-note {
  background: rgba(210, 72, 72, 0.1);
  color: #a13232;
}

.success-note {
  background: rgba(91, 191, 142, 0.14);
  color: #26744c;
}

.compact {
  margin-top: 6px;
}

.empty-copy {
  color: var(--text-muted);
}

@media (max-width: 1380px) {
  .workspace-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .right-stage {
    max-height: none;
  }
}

@media (max-width: 900px) {
  :deep(.page-shell) {
    width: calc(100vw - 16px);
    padding: 8px 8px 24px;
  }

  .workspace-top,
  .workspace-left,
  .workspace-right,
  .timeline-head,
  .processing-head,
  .processing-actions,
  .note-card-head,
  .section-headline,
  .field-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .chapter-ribbon {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .player-frame {
    min-height: 320px;
  }

  .subtitle-overlay {
    left: 18px;
    right: 18px;
  }

  .subtitle-overlay strong {
    font-size: 20px;
  }

  .speech-row {
    grid-template-columns: 28px 1fr;
  }

  .speech-row p {
    grid-column: 1 / -1;
  }

  .row-tools {
    display: none;
  }

  .subtitle-row,
  .processing-layout,
  .progress-grid {
    grid-template-columns: 1fr;
  }

  .mindmap-shell {
    min-height: 420px;
  }

  .ring-one {
    width: 240px;
    height: 240px;
  }

  .ring-two {
    width: 340px;
    height: 340px;
  }
}
</style>
