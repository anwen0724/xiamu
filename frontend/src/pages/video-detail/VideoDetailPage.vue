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

type LeftTab = 'transcript' | 'subtitle'
type RightTab = 'summary' | 'chapters' | 'notes'
type Entry = { id: string; text: string; timeSeconds: number; label: string }
type Block = { id: string; startLabel: string; text: string; count: number }

const workspaceNav = [
  { label: '创建', icon: '＋' },
  { label: '我的总结', icon: '▣' },
  { label: '备忘录', icon: '✎' },
  { label: 'AI 聊天', icon: '✦' },
  { label: 'AI 工作室', icon: '◫' },
  { label: '标签', icon: '⌘' },
  { label: '设置', icon: '⚙' },
  { label: '帮助与支持', icon: '？' },
  { label: '订阅', icon: '◪' },
] as const

const steps = [
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
const leftTab = ref<LeftTab>('transcript')
const rightTab = ref<RightTab>('summary')
const selectedIndex = ref(0)
const noteForm = reactive({ title: '', content: '', timeSeconds: '' })
const notePending = ref(false)
const noteError = ref('')
const noteSuccess = ref('')
const deletingId = ref<EntityId | null>(null)
const retryPending = ref(false)
const retryError = ref('')
const streamMessage = ref('')
let eventSource: EventSource | null = null
let fallbackTimer: number | null = null

const statusQuery = useVideoStatusQuery(videoId)
const status = computed(() => statusQuery.data.value ?? null)
const statusLoading = computed(() => statusQuery.isLoading.value)
const statusHasError = computed(() => statusQuery.isError.value)
const isSuccess = computed(() => status.value?.status === 'SUCCESS')
const isFailed = computed(() => status.value?.status === 'FAILED')
const isTerminal = computed(() => isSuccess.value || isFailed.value)
const resultQuery = useVideoResultQuery(videoId, isSuccess)
const notesQuery = useVideoNotesQuery(videoId, isSuccess)
const result = computed(() => resultQuery.data.value ?? null)
const resultLoading = computed(() => resultQuery.isLoading.value)
const resultHasError = computed(() => resultQuery.isError.value)
const resultErrorMessage = computed(() => resultQuery.error.value?.message || '结果内容加载失败')
const notes = computed(() => notesQuery.data.value ?? [])
const notesLoading = computed(() => notesQuery.isLoading.value)
const notesHasError = computed(() => notesQuery.isError.value)
const notesErrorMessage = computed(() => notesQuery.error.value?.message || '笔记加载失败')
const chapters = computed(() => result.value?.chapters ?? [])
const displayTitle = computed(() => result.value?.title || status.value?.title || '视频详情')
const updatedAt = computed(() => result.value?.updateTime || status.value?.updateTime || null)
const stageLabel = computed(() => status.value?.stageMessage || '处理中')
const summaryBlocks = computed(() => splitSummary(result.value?.summary))
const keywordTags = computed(() => {
  const chapterTags = chapters.value.map((item) => item.title.trim()).filter(Boolean)
  const summaryTags = summaryBlocks.value
    .flatMap((block) => block.split(/[，,。.!！?？；;]/))
    .map((item) => item.trim())
    .filter((item) => item.length >= 4 && item.length <= 12)
  return [...new Set([...chapterTags, ...summaryTags])].slice(0, 6)
})
const entries = computed(() => buildEntries(result.value?.transcript, result.value?.duration ?? status.value?.duration ?? 0))
const blocks = computed(() => buildBlocks(entries.value))
const currentCue = computed(() => entries.value[selectedIndex.value] ?? entries.value[0] ?? null)
const progressPercent = computed(() => {
  const index = steps.findIndex((item) => item.code === status.value?.stage)
  const percent = steps[Math.max(index, 0)]?.percent ?? 0
  return isFailed.value ? Math.max(percent, 14) : percent
})

watch(entries, (list) => { if (!list.length || selectedIndex.value >= list.length) selectedIndex.value = 0 }, { immediate: true })
watch(videoId, () => { leftTab.value = 'transcript'; rightTab.value = 'summary'; selectedIndex.value = 0; noteError.value = ''; noteSuccess.value = ''; retryError.value = '' })
watch(() => statusQuery.isSuccess.value && !isTerminal.value, (on) => { if (on) startStream(); else closeStream() }, { immediate: true })
onBeforeUnmount(closeStream)

function splitSummary(summary?: string | null) { return summary?.replace(/\r/g, '').split(/\n+|(?<=[。！？!?；;])/).map((i) => i.trim()).filter(Boolean) ?? [] }
function buildEntries(text?: string | null, duration = 0): Entry[] {
  if (!text?.trim()) return []
  const lines = text
    .replace(/\r/g, '')
    .split(/\n+|(?<=[。！？!?；;])/)
    .flatMap((item) => splitLongText(item))
    .map((i) => i.trim())
    .filter(Boolean)
    .slice(0, 120)
  const safe = duration > 0 ? duration : lines.length * 7
  return lines.map((item, index) => ({ id: `${index}-${item.slice(0, 12)}`, text: item, timeSeconds: Math.floor((safe / Math.max(lines.length, 1)) * index), label: formatClock(Math.floor((safe / Math.max(lines.length, 1)) * index)) }))
}
function splitLongText(text: string) {
  const value = text.trim()
  if (!value) return []
  if (value.length <= 72) return [value]
  return value.match(/.{1,72}/g) ?? [value]
}
function buildBlocks(list: Entry[]): Block[] {
  const rows: Block[] = []
  for (let i = 0; i < list.length; i += 3) { const group = list.slice(i, i + 3); const first = group[0]; if (first) rows.push({ id: `b-${first.id}`, startLabel: first.label, text: group.map((x) => x.text).join(' '), count: group.length }) }
  return rows
}
function formatClock(total = 0) { const m = Math.floor(Math.max(0, total) / 60); const s = Math.max(0, total) % 60; return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}` }
function chapterRange(chapter: VideoChapterVO) { return `${formatClock(chapter.startSeconds)} - ${formatClock(chapter.endSeconds)}` }
function selectBlock(index: number) { const next = index * 3; if (next < entries.value.length) selectedIndex.value = next }
function selectChapter(chapter: VideoChapterVO) { rightTab.value = 'chapters'; noteForm.title = chapter.title; noteForm.timeSeconds = String(chapter.startSeconds); const index = entries.value.findIndex((i) => i.timeSeconds >= chapter.startSeconds); if (index >= 0) selectedIndex.value = index }
function closeStream() { if (eventSource) { eventSource.close(); eventSource = null } if (fallbackTimer != null) { window.clearInterval(fallbackTimer); fallbackTimer = null } }
function startStream() {
  if (!videoId.value || !status.value || isTerminal.value || eventSource) return
  eventSource = openVideoStatusStream(videoId.value)
  eventSource.onopen = () => { streamMessage.value = ''; if (fallbackTimer != null) { window.clearInterval(fallbackTimer); fallbackTimer = null } }
  eventSource.addEventListener('status', (event) => {
    const next = JSON.parse((event as MessageEvent<string>).data) as VideoProcessStatusVO
    queryClient.setQueryData(['video-status', videoId.value], next)
    if (next.status === 'SUCCESS' || next.status === 'FAILED') { closeStream(); void queryClient.invalidateQueries({ queryKey: ['video-result', videoId.value] }); void queryClient.invalidateQueries({ queryKey: ['video-notes', videoId.value] }) }
  })
  eventSource.onerror = () => { streamMessage.value = '实时连接中断，已切换为低频刷新。'; if (fallbackTimer == null) fallbackTimer = window.setInterval(() => { void queryClient.invalidateQueries({ queryKey: ['video-status', videoId.value] }) }, 10000) }
}
async function handleAddNote() {
  noteError.value = ''; noteSuccess.value = ''
  if (!noteForm.title.trim() || !noteForm.content.trim()) { noteError.value = '请先填写标题和内容'; return }
  notePending.value = true
  try { await addNote({ videoId: videoId.value, title: noteForm.title.trim(), content: noteForm.content.trim(), timeSeconds: noteForm.timeSeconds ? Number(noteForm.timeSeconds) : undefined }); noteForm.title = ''; noteForm.content = ''; noteForm.timeSeconds = ''; noteSuccess.value = '笔记已保存'; await queryClient.invalidateQueries({ queryKey: ['video-notes', videoId.value] }) } catch (error) { noteError.value = error instanceof Error ? error.message : '保存笔记失败' } finally { notePending.value = false }
}
async function handleDeleteNote(id: EntityId) { deletingId.value = id; try { await deleteNote(id); noteSuccess.value = '笔记已删除'; await queryClient.invalidateQueries({ queryKey: ['video-notes', videoId.value] }) } catch (error) { noteError.value = error instanceof Error ? error.message : '删除笔记失败' } finally { deletingId.value = null } }
async function handleRetry() { retryPending.value = true; retryError.value = ''; try { await retryVideo(videoId.value); queryClient.removeQueries({ queryKey: ['video-result', videoId.value] }); queryClient.removeQueries({ queryKey: ['video-notes', videoId.value] }); await queryClient.invalidateQueries({ queryKey: ['video-status', videoId.value] }); closeStream(); startStream() } catch (error) { retryError.value = error instanceof Error ? error.message : '重新处理失败' } finally { retryPending.value = false } }
async function handleRefresh() { await queryClient.invalidateQueries({ queryKey: ['video-status', videoId.value] }) }
function goBack() { void router.push('/dashboard') }
</script>

<template>
  <AppLayout hide-hero compact wide title="" description="">
    <div class="page">
      <section v-if="!isSuccess" class="top panel">
        <div class="title-row">
          <button class="ghost icon" type="button" @click="goBack">←</button>
          <div class="title-copy">
            <div class="title-main">
              <h1>{{ displayTitle }}</h1>
              <span class="chip" :data-state="status?.status">{{ isSuccess ? '已完成' : isFailed ? '处理失败' : stageLabel }}</span>
            </div>
            <p v-if="updatedAt">更新于 {{ formatRelativeDate(updatedAt) }}</p>
          </div>
        </div>
        <div class="actions">
          <button class="ghost" type="button" @click="rightTab = 'notes'">记录笔记</button>
          <button v-if="!isSuccess" class="ghost" type="button" @click="handleRefresh">刷新状态</button>
        </div>
      </section>

      <p v-if="statusLoading && !status" class="muted">正在读取视频状态...</p>
      <p v-else-if="statusHasError && !status" class="error">{{ statusQuery.error.value?.message || '视频状态加载失败' }}</p>

      <template v-else-if="status && isSuccess">
        <section class="workspace-shell">
          <aside class="panel workspace-rail">
            <div class="rail-brand">
              <button class="rail-toggle" type="button">≡</button>
              <button class="ghost rail-back" type="button" @click="goBack">返回</button>
            </div>

            <nav class="rail-nav" aria-label="工作台导航">
              <button v-for="item in workspaceNav" :key="item.label" class="rail-nav-link" type="button">
                <span class="rail-nav-icon">{{ item.icon }}</span>
                <span>{{ item.label }}</span>
              </button>
            </nav>

            <div class="rail-promo">
              <span>限时</span>
              <strong>30% 折扣</strong>
              <button class="rail-promo-action" type="button">升级</button>
            </div>
          </aside>

          <section class="workspace-main">
            <article class="panel work-frame">
              <div class="workspace-header">
                <div class="workspace-title">
                  <button class="ghost icon" type="button" @click="goBack">‹</button>
                  <strong>{{ displayTitle }}</strong>
                  <span class="workspace-edit">✎</span>
                </div>
                <div class="workspace-tools">
                  <button class="tool-pill" :class="{ active: leftTab === 'transcript' }" type="button" @click="leftTab = 'transcript'">语音转文本</button>
                  <button class="tool-pill" :class="{ active: leftTab === 'subtitle' }" type="button" @click="leftTab = 'subtitle'">原始字幕</button>
                </div>
              </div>

              <div class="work-layout">
                <div class="work-video-column">
                  <article class="panel work-media">
                    <div class="screen work-screen">
                      <div class="grid-overlay" />
                      <div v-if="chapters.length" class="chapter-strip">
                        <button
                          v-for="chapter in chapters.slice(0, 4)"
                          :key="`${chapter.title}-${chapter.startSeconds}`"
                          class="chapter-pill"
                          type="button"
                          @click="selectChapter(chapter)"
                        >
                          {{ chapter.title }}
                        </button>
                      </div>
                      <div class="screen-copy">
                        <div class="play">▶</div>
                        <p>{{ currentCue?.text || summaryBlocks[0] || '已生成结果，可以继续浏览。' }}</p>
                      </div>
                    </div>

                    <div class="media-bar-row work-bar">
                      <span>{{ currentCue?.label || '00:00' }}</span>
                      <div class="bar"><span /></div>
                      <span>{{ formatClock(result?.duration ?? status.duration) }}</span>
                    </div>
                  </article>

                  <article class="panel transcript-panel">
                    <div class="panel-head transcript-head">
                      <div class="transcript-meta">
                        <span class="transcript-badge">发言人</span>
                        <span class="muted">{{ entries.length }} 段</span>
                      </div>
                      <button class="ghost" type="button" @click="rightTab = 'notes'">记录笔记</button>
                    </div>
                    <div v-if="leftTab === 'transcript' && entries.length" class="transcript-list">
                      <button
                        v-for="(entry, index) in entries"
                        :key="entry.id"
                        class="transcript-item"
                        :class="{ active: index === selectedIndex }"
                        type="button"
                        @click="selectedIndex = index"
                      >
                        <div class="transcript-item-head">
                          <span class="speaker-dot">S</span>
                          <span class="time">{{ entry.label }}</span>
                        </div>
                        <p>{{ entry.text }}</p>
                      </button>
                    </div>
                    <div v-else-if="blocks.length" class="transcript-list">
                      <button
                        v-for="(block, index) in blocks"
                        :key="block.id"
                        class="transcript-item"
                        :class="{ active: selectedIndex >= index * 3 && selectedIndex < index * 3 + block.count }"
                        type="button"
                        @click="selectBlock(index)"
                      >
                        <div class="transcript-item-head">
                          <span class="speaker-dot">S</span>
                          <span class="time">{{ block.startLabel }}</span>
                        </div>
                        <p>{{ block.text }}</p>
                      </button>
                    </div>
                    <p v-else class="muted">当前没有可展示的转写内容。</p>
                  </article>
                </div>
              </div>
            </article>
          </section>

          <aside class="panel workspace-side">
            <div class="side-head">
              <div class="side-topbar">
                <button class="tab" :class="{ active: rightTab === 'summary' }" type="button" @click="rightTab = 'summary'">总结</button>
                <button class="tab" :class="{ active: rightTab === 'chapters' }" type="button" @click="rightTab = 'chapters'">章节</button>
                <button class="tab" :class="{ active: rightTab === 'notes' }" type="button" @click="rightTab = 'notes'">笔记</button>
              </div>
            </div>

            <div v-if="rightTab === 'summary'" class="right-body side-body">
              <div v-if="keywordTags.length" class="keyword-block">
                <span class="keyword-label">关键词</span>
                <div class="chips">
                  <span v-for="tag in keywordTags" :key="tag" class="tag">{{ tag }}</span>
                </div>
              </div>
              <p v-if="resultLoading" class="muted">正在加载总结...</p>
              <p v-else-if="resultHasError" class="error">{{ resultErrorMessage }}</p>
              <div v-else-if="summaryBlocks.length" class="summary side-summary">
                <p v-for="(block, index) in summaryBlocks" :key="`s-${index}`">{{ block }}</p>
              </div>
              <p v-else class="muted">当前还没有可展示的总结。</p>
            </div>

            <div v-else-if="rightTab === 'chapters'" class="right-body side-body list">
              <button
                v-for="chapter in chapters"
                :key="`${chapter.title}-${chapter.startSeconds}`"
                class="chapter-card"
                type="button"
                @click="selectChapter(chapter)"
              >
                <span class="time">{{ chapterRange(chapter) }}</span>
                <strong>{{ chapter.title }}</strong>
                <p>{{ chapter.summary || '该章节暂无摘要。' }}</p>
              </button>
              <p v-if="!chapters.length" class="muted">还没有章节数据。</p>
            </div>

            <div v-else class="right-body side-body notes">
              <label class="field"><span>标题</span><input v-model="noteForm.title" placeholder="输入笔记标题" /></label>
              <div class="note-row">
                <label class="field"><span>时间点</span><input v-model="noteForm.timeSeconds" type="number" min="0" placeholder="秒" /></label>
                <button class="ghost" type="button" @click="noteForm.timeSeconds = currentCue ? String(currentCue.timeSeconds) : noteForm.timeSeconds">引用当前时间</button>
              </div>
              <label class="field"><span>内容</span><textarea v-model="noteForm.content" rows="4" placeholder="记录这段内容..." /></label>
              <div class="actions">
                <button class="primary" type="button" :disabled="notePending" @click="handleAddNote">{{ notePending ? '保存中...' : '保存笔记' }}</button>
              </div>
              <p v-if="noteError" class="error">{{ noteError }}</p>
              <p v-if="noteSuccess" class="success">{{ noteSuccess }}</p>
              <p v-if="notesLoading" class="muted">正在加载笔记...</p>
              <p v-else-if="notesHasError" class="error">{{ notesErrorMessage }}</p>
              <div v-else-if="notes.length" class="list">
                <article v-for="note in notes" :key="note.id" class="note">
                  <div class="note-head">
                    <div><strong>{{ note.title }}</strong><span>{{ note.timeSeconds != null ? `时间点 ${formatClock(note.timeSeconds)}` : '无时间点' }}</span></div>
                    <button class="ghost danger" type="button" :disabled="deletingId === note.id" @click="handleDeleteNote(note.id)">{{ deletingId === note.id ? '删除中...' : '删除' }}</button>
                  </div>
                  <p>{{ note.content }}</p>
                  <small>{{ formatRelativeDate(note.updateTime) }}</small>
                </article>
              </div>
              <p v-else class="muted">还没有笔记。</p>
            </div>
          </aside>
        </section>
      </template>

      <template v-else-if="status">
        <section class="status-shell">
          <article class="panel status-main">
            <div class="row top-row">
              <div>
                <span class="chip" :data-state="status.status">{{ status.status }}</span>
                <h2>{{ isFailed ? '处理未完成' : stageLabel }}</h2>
                <p class="muted">{{ streamMessage || '正在持续刷新处理进度。' }}</p>
              </div>
              <strong class="big">{{ progressPercent }}%</strong>
            </div>
            <div class="progress"><span :style="{ width: `${progressPercent}%` }" /></div>
            <div class="steps">
              <article v-for="step in steps" :key="step.code" class="step" :data-state="(status.stage === step.code && isFailed) ? 'failed' : status.stage === step.code ? 'active' : step.percent < progressPercent ? 'done' : 'pending'">
                <span>{{ step.percent }}%</span><strong>{{ step.label }}</strong>
              </article>
            </div>
            <div class="chips">
              <span class="tag" :class="{ done: status.hasTranscript }">转写</span>
              <span class="tag" :class="{ done: status.hasSummary }">总结</span>
              <span class="tag" :class="{ done: status.hasChapters }">章节</span>
            </div>
            <p v-if="status.errorMessage" class="error">{{ status.errorMessage }}</p>
            <p v-if="retryError" class="error">{{ retryError }}</p>
            <div class="actions">
              <button class="ghost" type="button" @click="handleRefresh">刷新状态</button>
              <button v-if="isFailed" class="primary" type="button" :disabled="retryPending" @click="handleRetry">{{ retryPending ? '重试中...' : '重新处理' }}</button>
            </div>
          </article>
          <article class="panel status-side">
            <div class="meta-card"><span>标题</span><strong>{{ displayTitle }}</strong></div>
            <div class="meta-card"><span>阶段</span><strong>{{ stageLabel }}</strong></div>
            <div class="meta-card"><span>更新</span><strong>{{ updatedAt ? formatRelativeDate(updatedAt) : '刚刚' }}</strong></div>
          </article>
        </section>
      </template>
    </div>
  </AppLayout>
</template>

<style scoped>
:deep(.page-shell){width:calc(100vw - 24px);max-width:none;padding:12px 12px 28px}
:deep(.layout-header){padding:14px 18px;border-radius:16px;background:rgba(255,255,255,.92)}
.page,.left,.right-body,.summary,.list,.notes,.status-main,.status-side,.title-copy{display:grid;gap:14px}
.top,.title-row,.title-main,.actions,.section,.row,.tabs,.note-row,.note-head{display:flex;align-items:center}
.page{gap:14px}.top{justify-content:space-between;gap:16px;padding:14px 16px;border-radius:18px;background:rgba(255,255,255,.9)}.title-row{gap:14px;min-width:0}.title-copy{gap:6px;min-width:0}.title-main{gap:12px;min-width:0}.title-main h1,.row h2,.top-row h2{margin:0;color:#1b2033}.title-main h1{font-size:clamp(28px,3vw,38px);line-height:1.06;letter-spacing:-.05em}
.ghost,.primary,.tab,.item,.icon{cursor:pointer}.ghost,.icon{min-height:38px;padding:0 14px;border:1px solid rgba(143,108,255,.1);border-radius:999px;background:rgba(255,255,255,.82);color:#4a5066}.icon{width:40px;padding:0;font-size:18px}.primary{min-height:42px;padding:0 18px;border:0;border-radius:999px;background:#7a54ff;color:#fff;box-shadow:0 12px 24px rgba(122,84,255,.16)}
.chip{display:inline-flex;align-items:center;min-height:32px;padding:0 12px;border-radius:999px;background:rgba(63,171,112,.1);color:#26744c;font-size:12px;font-weight:700;white-space:nowrap}.chip[data-state='PROCESSING'],.chip[data-state='WAITING']{background:rgba(243,167,0,.12);color:#9b6900}.chip[data-state='FAILED']{background:rgba(208,57,57,.1);color:#a13232}
.muted,.summary p,.item p,.note p,.note small{margin:0;color:var(--text-muted)}.error,.success{margin:0;padding:12px 14px;border-radius:14px}.error{background:rgba(210,72,72,.1);color:#a13232}.success{background:rgba(91,191,142,.14);color:#26744c}
.workspace-shell{display:grid;grid-template-columns:170px minmax(0,1.08fr) minmax(360px,.78fr);gap:0;align-items:start;border:1px solid rgba(143,108,255,.08);border-radius:22px;overflow:hidden;background:rgba(255,255,255,.95);box-shadow:0 18px 40px rgba(62,46,115,.05)}
.workspace-rail,.workspace-main,.workspace-side,.rail-brand,.rail-nav,.rail-promo,.work-frame,.work-layout,.work-video-column,.transcript-panel,.transcript-list,.side-head,.side-body,.side-summary,.keyword-block{display:grid;gap:14px}
.workspace-rail,.workspace-side{position:sticky;top:94px;align-self:start}
.workspace-main{background:#fff}
.workspace-rail{min-height:calc(100vh - 132px);padding:14px 12px;border-right:1px solid rgba(143,108,255,.08);background:linear-gradient(180deg,#fbfaff 0%,#fff 100%)}
.rail-brand{grid-template-columns:34px 1fr;align-items:center;gap:10px;padding:2px 4px 10px}
.rail-toggle,.rail-promo-action{border:0;cursor:pointer}
.rail-toggle{display:grid;place-items:center;width:34px;height:34px;border-radius:12px;background:rgba(143,108,255,.08);color:#5d42d1;font-size:16px}
.rail-back{justify-content:flex-start;min-height:34px;padding:0 12px}
.rail-nav{align-content:start}
.rail-nav-link{display:flex;align-items:center;gap:10px;min-height:38px;padding:0 10px;border:0;border-radius:12px;background:transparent;color:#434a62;font-size:13px;font-weight:600;cursor:pointer;text-align:left}
.rail-nav-link:hover{background:rgba(143,108,255,.08)}
.rail-nav-icon{display:grid;place-items:center;width:18px;color:#6f53da;font-size:12px}
.rail-promo{margin-top:auto;padding:12px;border-radius:16px;background:linear-gradient(145deg,#b37fff 0%,#7f5bff 58%,#674dff 100%);color:#fff;box-shadow:0 16px 28px rgba(108,78,255,.22)}
.rail-promo span{font-size:12px;opacity:.82}.rail-promo strong{font-size:26px;line-height:1}
.rail-promo-action{min-height:34px;border-radius:10px;background:rgba(255,255,255,.16);color:#fff;font-weight:700}
.work-frame,.work-media,.transcript-panel,.workspace-side{padding:0}
.work-frame{gap:0}
.workspace-header,.workspace-title,.workspace-tools,.side-topbar,.chapter-strip,.transcript-item-head,.transcript-meta{display:flex;align-items:center}
.workspace-header{justify-content:space-between;gap:16px;padding:12px 16px;border-bottom:1px solid rgba(143,108,255,.08);background:rgba(255,255,255,.96)}
.workspace-title{gap:10px;min-width:0}.workspace-title strong{font-size:17px;color:#20243a;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.workspace-edit{color:#8c91a7;font-size:14px}
.workspace-tools{gap:8px;flex-wrap:wrap}
.tool-pill{min-height:30px;padding:0 10px;border:1px solid rgba(143,108,255,.08);border-radius:8px;background:#fff;color:#666d84;font-size:12px;font-weight:700;cursor:pointer}.tool-pill.active{border-color:rgba(143,108,255,.16);background:rgba(143,108,255,.08);color:#5d42d1}
.work-layout{padding:12px}.work-video-column{gap:10px}
.work-media{gap:10px}
.work-screen{min-height:314px;border:1px solid rgba(255,255,255,.08);border-radius:0;background:linear-gradient(180deg,#18181d 0%,#23252d 100%)}
.grid-overlay{position:absolute;inset:0;background-image:linear-gradient(rgba(255,255,255,.08) 1px,transparent 1px),linear-gradient(90deg,rgba(255,255,255,.08) 1px,transparent 1px);background-size:36px 36px;opacity:.55}
.work-screen::after{content:'';position:absolute;inset:0;background:linear-gradient(180deg,rgba(8,8,10,.18) 0%,rgba(8,8,10,.08) 38%,rgba(8,8,10,.34) 100%)}
.chapter-strip{position:absolute;top:0;left:0;right:0;z-index:1;justify-content:space-between;gap:12px;padding:0 18px;height:40px;background:rgba(5,5,8,.82);overflow:auto}
.chapter-pill{flex:0 0 auto;min-height:40px;padding:0 10px;border:0;background:transparent;color:#fff;font-size:12px;font-weight:700;cursor:pointer}
.screen-copy{z-index:1}
.work-bar{padding:0 8px 0 2px}
.transcript-panel{min-height:360px;border-top:1px solid rgba(143,108,255,.06);background:#fff}
.transcript-head{padding:0 8px 0 2px}
.transcript-meta{gap:10px}
.transcript-badge{display:inline-flex;align-items:center;justify-content:center;min-width:42px;height:20px;padding:0 8px;border-radius:999px;background:rgba(255,120,146,.12);color:#ff6e9f;font-size:11px;font-weight:700}
.transcript-list{max-height:390px;overflow:auto;padding-right:4px}
.transcript-item,.chapter-card{width:100%;display:grid;gap:8px;padding:12px 14px;border:1px solid rgba(143,108,255,.08);border-radius:10px;background:#fff;text-align:left;cursor:pointer;box-shadow:none}
.transcript-item.active,.chapter-card:hover{border-color:rgba(143,108,255,.18);background:rgba(250,247,255,.92)}
.transcript-item p,.chapter-card p{margin:0;color:#4b5267;line-height:1.65}
.transcript-item-head{gap:10px}
.speaker-dot{display:grid;place-items:center;width:18px;height:18px;border-radius:50%;background:rgba(255,120,146,.18);color:#ff6e9f;font-size:10px;font-weight:700}
.panel-head h2{margin:0;font-size:18px;color:#1b2033}
.workspace-side{min-height:calc(100vh - 132px);padding:12px 14px;border-left:1px solid rgba(143,108,255,.08);background:#fff;max-height:calc(100vh - 132px)}
.side-head{padding-bottom:10px;border-bottom:1px solid rgba(143,108,255,.08)}
.side-topbar{gap:8px;flex-wrap:wrap}
.side-body{min-height:0;overflow:auto;padding-right:4px}
.keyword-label{font-size:12px;font-weight:700;color:#7c8098}
.keyword-block{gap:10px;padding-bottom:8px}
.side-summary p{padding:0 0 14px;border-bottom:1px dashed rgba(143,108,255,.1);font-size:13px;line-height:1.8;color:#42485f}
.side-summary p:last-child{padding-bottom:0;border-bottom:0}
.shell,.status-shell{display:grid;grid-template-columns:minmax(0,1.1fr) minmax(320px,.9fr);gap:14px;align-items:start}
.panel{background:rgba(255,255,255,.9)}
.media,.status-main,.status-side,.right,.top{box-shadow:inset 0 1px 0 rgba(255,255,255,.88),0 16px 28px rgba(62,46,115,.04)}
.media,.right,.status-main,.status-side,.panel{padding:16px;border-radius:20px}
.screen{position:relative;min-height:420px;border-radius:18px;overflow:hidden;background:linear-gradient(180deg,#34214b 0%,#8b59ff 48%,#ffb57a 100%)}.glow{position:absolute;top:16%;right:10%;width:220px;height:220px;border-radius:50%;background:radial-gradient(circle,rgba(255,233,151,.95) 0%,rgba(255,200,112,.32) 100%)}.screen-copy{position:absolute;left:22px;right:22px;bottom:22px;display:grid;gap:16px}.play{display:inline-grid;place-items:center;width:56px;height:56px;border-radius:50%;background:rgba(255,255,255,.18);color:#fff;font-size:18px;backdrop-filter:blur(10px)}.screen-copy p{margin:0;max-width:20ch;color:#fff;font-size:clamp(24px,2vw,32px);line-height:1.28;text-shadow:0 10px 28px rgba(0,0,0,.32)}
.media-bar-row{display:grid;grid-template-columns:auto 1fr auto;align-items:center;gap:10px;color:#6d7288;font-size:13px}.bar{height:6px;border-radius:999px;background:rgba(122,84,255,.12);overflow:hidden}.bar span{display:block;width:32%;height:100%;border-radius:inherit;background:linear-gradient(90deg,#8f6cff,#ff9ac1)}
.row{justify-content:space-between;gap:14px}.tabs{gap:8px;flex-wrap:wrap}.tab{min-height:36px;padding:0 14px;border:1px solid rgba(143,108,255,.08);border-radius:999px;background:rgba(255,255,255,.74);color:#676c82;font-weight:600}.tab.active{background:rgba(143,108,255,.1);border-color:rgba(143,108,255,.12);color:#32364d}
.list{gap:10px}.right{max-height:calc(100vh - 138px)}.right-body{min-height:0;overflow:auto;padding-right:4px}
.item{width:100%;display:grid;gap:8px;padding:14px 16px;border:1px solid rgba(143,108,255,.08);border-radius:16px;background:rgba(255,255,255,.84);text-align:left}.item.active{border-color:rgba(143,108,255,.18);box-shadow:0 12px 24px rgba(122,84,255,.08)}.time{color:#7a54ff;font-size:12px;font-weight:700}.transcript-row,.subtitle-row{max-height:520px;overflow:auto}
.field{display:grid;gap:8px;flex:1}.field span,.note-head span,.meta-card span{color:#7b8096;font-size:12px}.field input,.field textarea{width:100%;padding:12px 14px;border:1px solid rgba(143,108,255,.1);border-radius:14px;background:rgba(255,255,255,.84);color:var(--text-main)}.field textarea{resize:vertical}
.note-row{gap:12px;align-items:end}.note{display:grid;gap:10px;padding:14px 16px;border-radius:16px;background:rgba(255,255,255,.8);border:1px solid rgba(143,108,255,.08)}.note-head{justify-content:space-between;align-items:flex-start;gap:12px}.note-head>div{display:grid;gap:4px}.danger{color:#a13232}
.chips{display:flex;flex-wrap:wrap;gap:8px}.tag{display:inline-flex;align-items:center;min-height:30px;padding:0 12px;border-radius:999px;background:rgba(143,108,255,.08);color:#6f53da;font-size:12px;font-weight:700}.tag.done{background:rgba(91,191,142,.14);color:#26744c}
.top-row{justify-content:space-between;align-items:flex-start}.top-row h2{margin-top:10px;font-size:28px}.big{font-size:42px;line-height:1;letter-spacing:-.05em;color:#1d2235}.progress{position:relative;height:10px;border-radius:999px;background:rgba(30,30,45,.08);overflow:hidden}.progress span{position:absolute;inset:0 auto 0 0;border-radius:inherit;background:linear-gradient(90deg,#8c6fff,#ffbf71)}
.steps{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px}.step{display:grid;gap:6px;padding:14px;border-radius:14px;background:rgba(255,255,255,.78);color:#756c87}.step span{font-size:12px;letter-spacing:.08em;text-transform:uppercase}.step[data-state='active']{background:rgba(127,97,255,.12);color:#6f53da}.step[data-state='done']{background:rgba(91,191,142,.12);color:#26744c}.step[data-state='failed']{background:rgba(210,72,72,.12);color:#a13232}
.meta-card{display:grid;gap:8px;padding:16px;border-radius:16px;background:rgba(255,255,255,.78);border:1px solid rgba(143,108,255,.08)}
@media (max-width:1320px){.workspace-shell{grid-template-columns:200px minmax(0,1fr) minmax(300px,.82fr)}}
@media (max-width:1180px){.workspace-shell,.shell,.status-shell{grid-template-columns:1fr}.workspace-rail,.workspace-side,.right{position:static;max-height:none}.rail-meta{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media (max-width:900px){:deep(.page-shell){width:calc(100vw - 16px);padding:8px 8px 24px}.top,.title-row,.title-main,.actions,.row,.note-row,.note-head,.top-row,.work-toolbar,.panel-head{flex-direction:column;align-items:flex-start}.screen{min-height:320px}.work-screen{min-height:320px}.screen-copy p{font-size:22px}.steps{grid-template-columns:1fr}.rail-meta{grid-template-columns:1fr}}
</style>
