<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import { addNote, deleteNote } from '../../api/note'
import type { EntityId } from '../../api/types'
import AppLayout from '../../layouts/AppLayout.vue'
import { useVideoNotesQuery, useVideoResultQuery } from '../../queries'
import { formatDuration, formatRelativeDate } from '../../utils'

const route = useRoute()
const queryClient = useQueryClient()
const videoId = computed(() => String(route.params.id ?? ''))

const resultQuery = useVideoResultQuery(videoId)
const notesQuery = useVideoNotesQuery(videoId)

const noteForm = reactive({
  title: '',
  content: '',
  timeSeconds: '',
})

const notePending = ref(false)
const noteError = ref('')
const noteSuccess = ref('')
const deletingId = ref<EntityId | null>(null)

const result = computed(() => resultQuery.data.value ?? null)
const notes = computed(() => notesQuery.data.value ?? [])
const resultLoading = computed(() => resultQuery.isLoading.value)
const resultHasError = computed(() => resultQuery.isError.value)
const notesLoading = computed(() => notesQuery.isLoading.value)
const notesHasError = computed(() => notesQuery.isError.value)
const resultErrorMessage = computed(() => resultQuery.error.value?.message || '结果加载失败')
const notesErrorMessage = computed(() => notesQuery.error.value?.message || '笔记加载失败')

async function refreshNotes() {
  await queryClient.invalidateQueries({
    queryKey: ['video-notes', videoId.value],
  })
}

function presetNoteFromChapter(title: string, timeSeconds?: number | null) {
  noteForm.title = title
  noteForm.timeSeconds = typeof timeSeconds === 'number' ? String(timeSeconds) : ''
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
    await refreshNotes()
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
    await refreshNotes()
  } catch (error) {
    noteError.value = error instanceof Error ? error.message : '删除笔记失败'
  } finally {
    deletingId.value = null
  }
}
</script>

<template>
  <AppLayout
    eyebrow="结果页"
    title="摘要、章节与笔记在这里合流"
    description="结果页是当前 MVP 的核心消费页面：用户在这里快速理解内容、定位章节，并立刻开始记录学习笔记。"
  >
    <p v-if="resultLoading" class="status-note">正在读取结果页内容...</p>
    <p v-else-if="resultHasError" class="error-note">{{ resultErrorMessage }}</p>

    <template v-else-if="result">
      <section class="meta-strip panel">
        <div>
          <span class="meta-label">视频标题</span>
          <strong>{{ result.title }}</strong>
        </div>
        <div>
          <span class="meta-label">处理状态</span>
          <strong>{{ result.status }}</strong>
        </div>
        <div>
          <span class="meta-label">视频时长</span>
          <strong>{{ formatDuration(result.duration) }}</strong>
        </div>
        <div>
          <span class="meta-label">处理时间</span>
          <strong>{{ formatRelativeDate(result.updateTime) }}</strong>
        </div>
      </section>

      <section class="result-grid">
        <article class="panel summary-panel">
          <div class="section-head">
            <h2>视频摘要</h2>
            <span>内容优先</span>
          </div>
          <p v-if="result.summary" class="summary-text">{{ result.summary }}</p>
          <p v-else class="empty-copy">当前还没有可读摘要，请先回到状态页确认处理结果。</p>
        </article>

        <aside class="panel chapters-panel">
          <div class="section-head">
            <h2>章节导航</h2>
            <span>{{ result.chapters.length }} 个章节</span>
          </div>
          <div v-if="result.chapters.length" class="chapter-list">
            <button
              v-for="chapter in result.chapters"
              :key="`${chapter.title}-${chapter.startSeconds}`"
              class="chapter-item"
              type="button"
              @click="presetNoteFromChapter(chapter.title, chapter.startSeconds)"
            >
              <span class="chapter-time">
                {{ formatDuration(chapter.startSeconds) }} - {{ formatDuration(chapter.endSeconds) }}
              </span>
              <strong>{{ chapter.title }}</strong>
              <p>{{ chapter.summary || '暂无章节摘要' }}</p>
            </button>
          </div>
          <p v-else class="empty-copy">当前还没有可用章节，结果页仍可先围绕摘要记笔记。</p>
        </aside>

        <section class="panel notes-panel">
          <div class="notes-grid">
            <section class="notes-editor">
              <div class="section-head">
                <h2>学习笔记</h2>
                <span>围绕当前视频记录重点</span>
              </div>

              <p v-if="noteError" class="error-note compact">{{ noteError }}</p>
              <p v-if="noteSuccess" class="success-note compact">{{ noteSuccess }}</p>

              <form class="note-form" @submit.prevent="handleAddNote">
                <div class="field">
                  <label>笔记标题</label>
                  <input v-model="noteForm.title" placeholder="例如：第 2 章里的核心概念" />
                </div>

                <div class="field field-inline">
                  <div>
                    <label>时间点（秒）</label>
                    <input
                      v-model="noteForm.timeSeconds"
                      inputmode="numeric"
                      placeholder="可选，例如 120"
                    />
                  </div>
                  <button class="submit-btn" :disabled="notePending" type="submit">
                    {{ notePending ? '保存中...' : '保存笔记' }}
                  </button>
                </div>

                <div class="field">
                  <label>笔记内容</label>
                  <textarea
                    v-model="noteForm.content"
                    rows="8"
                    placeholder="记录当前视频里你真正想保留的结论、定义、代码片段或下一步行动。"
                  />
                </div>
              </form>
            </section>

            <section class="notes-list-wrap">
              <div class="section-head">
                <h2>已有笔记</h2>
                <span>{{ notes.length }} 条</span>
              </div>

              <p v-if="notesLoading" class="status-note compact">正在加载笔记...</p>
              <p v-else-if="notesHasError" class="error-note compact">{{ notesErrorMessage }}</p>
              <div v-else-if="notes.length" class="notes-list">
                <article v-for="note in notes" :key="note.id" class="note-card">
                  <div class="note-card-head">
                    <div>
                      <strong>{{ note.title }}</strong>
                      <p>
                        {{
                          note.timeSeconds != null
                            ? `时间点 ${formatDuration(note.timeSeconds)}`
                            : '未绑定时间点'
                        }}
                      </p>
                    </div>
                    <button
                      class="delete-btn"
                      type="button"
                      :disabled="deletingId === note.id"
                      @click="handleDeleteNote(note.id)"
                    >
                      {{ deletingId === note.id ? '删除中...' : '删除' }}
                    </button>
                  </div>
                  <p class="note-content">{{ note.content }}</p>
                  <span class="note-meta">{{ formatRelativeDate(note.updateTime || note.createTime) }}</span>
                </article>
              </div>
              <div v-else class="empty-box">
                <strong>还没有笔记</strong>
                <p>可以先点击章节预填标题，再把自己的理解写下来。</p>
              </div>
            </section>
          </div>
        </section>
      </section>
    </template>
  </AppLayout>
</template>

<style scoped>
.status-note {
  margin: 0 0 20px;
  color: var(--text-muted);
}

.compact {
  margin-bottom: 14px;
}

.meta-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  padding: 18px 22px;
  margin-bottom: 20px;
}

.meta-strip div {
  display: grid;
  gap: 6px;
}

.meta-label {
  color: var(--text-muted);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.result-grid {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
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

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
}

.section-head h2 {
  margin: 0;
}

.section-head span {
  color: var(--text-muted);
  font-size: 13px;
}

.summary-text {
  margin: 0;
  color: var(--text-main);
  line-height: 1.9;
  white-space: pre-wrap;
}

.chapter-list {
  display: grid;
  gap: 12px;
}

.chapter-item {
  display: grid;
  gap: 6px;
  text-align: left;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.6);
  cursor: pointer;
}

.chapter-item strong,
.chapter-item p {
  margin: 0;
}

.chapter-item p,
.chapter-time {
  color: var(--text-muted);
}

.chapter-time {
  font-size: 12px;
}

.notes-grid {
  display: grid;
  grid-template-columns: 0.95fr 1.05fr;
  gap: 20px;
}

.field {
  display: grid;
  gap: 8px;
  margin-bottom: 16px;
}

.field-inline {
  grid-template-columns: 1fr auto;
  align-items: end;
  gap: 14px;
}

input,
textarea,
.submit-btn {
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.86);
  font: inherit;
}

input,
textarea {
  padding: 12px 14px;
}

.submit-btn {
  min-height: 46px;
  padding: 0 18px;
  background: var(--accent);
  border: 0;
  color: #fff;
  cursor: pointer;
}

.submit-btn:disabled,
.delete-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.notes-list {
  display: grid;
  gap: 14px;
}

.note-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.58);
}

.note-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.note-card-head strong,
.note-card-head p,
.note-content,
.note-meta {
  margin: 0;
}

.note-card-head p,
.note-meta {
  color: var(--text-muted);
}

.note-content {
  margin-top: 12px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.note-meta {
  display: inline-block;
  margin-top: 12px;
  font-size: 12px;
}

.delete-btn {
  min-height: 36px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(208, 57, 57, 0.18);
  background: rgba(208, 57, 57, 0.08);
  color: #a02c2c;
  cursor: pointer;
}

.error-note,
.success-note {
  padding: 12px 14px;
  border-radius: 14px;
}

.error-note {
  background: rgba(208, 57, 57, 0.08);
  color: #a02c2c;
}

.success-note {
  background: rgba(48, 136, 84, 0.1);
  color: #2e7d51;
}

.empty-copy,
.empty-box p {
  margin: 0;
  color: var(--text-muted);
}

.empty-box {
  padding: 18px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.52);
}

.empty-box strong {
  display: block;
  margin-bottom: 8px;
}

@media (max-width: 960px) {
  .meta-strip,
  .result-grid,
  .notes-grid,
  .field-inline {
    grid-template-columns: 1fr;
  }

  .notes-panel {
    grid-column: auto;
  }
}
</style>
