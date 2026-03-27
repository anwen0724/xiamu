<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useVideoResultQuery, useVideoStatusQuery } from '../../queries'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const videoId = computed(() => String(route.params.id ?? ''))
const statusQuery = useVideoStatusQuery(videoId)
const resultQuery = useVideoResultQuery(videoId)
const fileName = computed(
  () => resultQuery.data.value?.title || statusQuery.data.value?.title || `video-${videoId.value}`,
)
const videoResult = computed(() => resultQuery.data.value ?? null)
const displayName = computed(
  () => authStore.currentUser?.userName || authStore.currentUser?.userAccount || 'wen an',
)
const avatarLabel = computed(() => {
  const seed = authStore.currentUser?.userName || authStore.currentUser?.userAccount || 'W'
  return seed.trim().charAt(0).toLowerCase()
})
const shareModalOpen = ref(false)
const accountMenuOpen = ref(false)
const shareLink = computed(() =>
  typeof window === 'undefined'
    ? route.fullPath
    : `${window.location.origin}${route.fullPath}`,
)
const shareAccessEnabled = ref(true)
const allowCreateCopy = ref(true)
const copySuccess = ref(false)
const sidebarExpanded = ref(true)
const leftContentTab = ref<'transcript' | 'subtitle'>('transcript')
const rightContentTab = ref<'summary' | 'chapters' | 'notes' | 'studio'>('summary')

const menuItems = [
  { icon: '⊕', label: '创建', expandable: true },
  { icon: '▣', label: '我的总结', expandable: true },
  { icon: '⌑', label: '备忘录', expandable: false },
  { icon: '⌂', label: 'AI 聊天', expandable: false },
  { icon: '✣', label: 'AI 工作室', expandable: false },
  { icon: '⌁', label: '标签', expandable: false },
  { icon: '⚙', label: '设置', expandable: true },
  { icon: 'ⓘ', label: '帮助与支持', expandable: true },
  { icon: '☰', label: '订阅', expandable: false },
] as const

const previewChapters = ['开头', '跪了五千年', '三刀剖骨', '站起来的民族'] as const

const transcriptEntries = [
  { id: 't-1', speaker: '发言人', time: '00:00', text: '问大家一个问题，为什么一个一辈子反对个人崇拜的人走了快五十年，怀念他的人反而越来越多了？' },
  { id: 't-2', speaker: '发言人', time: '00:07', text: '翻遍中华上下五千年，哪个帝王不是拼了命让后人记住自己？' },
  { id: 't-3', speaker: '发言人', time: '00:11', text: '秦始皇以兵马俑重塑军魂，汉武帝把功绩焊在泰山顶，乾隆把名字题遍半个江山。' },
  { id: 't-4', speaker: '发言人', time: '00:18', text: '可结果呢？' },
  { id: 't-5', speaker: '发言人', time: '00:18', text: '不过是老百姓茶余饭后的谈资罢了。' },
  { id: 't-6', speaker: '发言人', time: '00:21', text: '反倒是他，反对挂像、反对喊万岁，把个人崇拜当敌人打了一辈子，几乎用尽了一个领袖所能动用的一切方式，试图拦住这个民族，把他推上神坛。' },
  { id: 't-7', speaker: '发言人', time: '00:31', text: '可如今，哪怕是没见过他的年轻人，只是站在韶山的石像前，眼眶就红了。' },
] as const

const subtitleEntries = [
  { id: 's-1', time: '00:00', text: '问大家一个问题，为什么一个一辈子反对个人崇拜的人走了快五十年，怀念他的人反而越来越多了？' },
  { id: 's-2', time: '00:06', text: '翻遍中华上下五千年，哪个帝王不是拼了命让后人记住自己？' },
  { id: 's-3', time: '00:11', text: '秦始皇以兵马俑重塑军魂，汉武帝把功绩焊在泰山顶，乾隆把名字题遍半个江山。' },
  { id: 's-4', time: '00:17', text: '可结果呢？' },
  { id: 's-5', time: '00:18', text: '不过是老百姓茶余饭后的谈资罢了。' },
  { id: 's-6', time: '00:20', text: '反倒是他，反对挂像、反对喊万岁，把个人崇拜当敌人打了一辈子，几乎用尽了一个领袖所能动用的一切方式，试图拦住这个民族，把他推上神坛。' },
  { id: 's-7', time: '00:30', text: '可如今，哪怕是没见过他的年轻人，只是站在韶山的石像前，眼眶就红了。' },
  { id: 's-8', time: '00:35', text: '大山里不识字的老人，什么都不挂，唯独把他画像供了一辈子。' },
  { id: 's-9', time: '00:40', text: '你要问他为什么，他也说不出来，只知道他是为咱们穷人活的。' },
  { id: 's-10', time: '00:48', text: '心情好了就赏你一口饭，免你一年税，但本质上还是居高临下的施舍，你得跪着谢主隆恩。' },
  { id: 's-11', time: '00:55', text: '而唯有他，把一个跪了五千年的民族从泥里拽了起来，然后拼了命的。' },
] as const

const rightTabs: Array<{
  key: 'summary' | 'chapters' | 'notes' | 'studio' | 'ai'
  label: string
  icon: string
  badge?: string
}> = [
  { key: 'summary', label: '总结', icon: '✦' },
  { key: 'chapters', label: '思维导图', icon: '⌘' },
  { key: 'notes', label: '视频转图文', icon: '◫', badge: 'NEW' },
  { key: 'studio', label: '备忘录', icon: '⌑' },
  { key: 'ai', label: 'AI 工作室', icon: '✣' },
] as const

const splitRatio = ref(0.56)
const dragging = ref(false)

const summaryText = computed(() => {
  const summary = videoResult.value?.summary?.trim()
  return summary || ''
})

const chapterCards = computed(() =>
  (videoResult.value?.chapters ?? []).map((chapter, index) => ({
    id: `chapter-${index}`,
    title: chapter.title?.trim() || `章节 ${index + 1}`,
    range: formatChapterRange(chapter.startSeconds, chapter.endSeconds),
    text: chapter.summary?.trim() || '该章节暂未生成摘要。',
  })),
)

const keywordTags = computed(() => {
  const chapterTitleTags = (videoResult.value?.chapters ?? [])
    .map((chapter) => normalizeKeywordCandidate(chapter.title ?? ''))
    .filter(Boolean)

  if (chapterTitleTags.length >= 3) {
    return [...new Set(chapterTitleTags)].slice(0, 5)
  }

  const textSources = [
    videoResult.value?.title ?? '',
    videoResult.value?.summary ?? '',
    ...(videoResult.value?.chapters ?? []).flatMap((chapter) => [chapter.title ?? '', chapter.summary ?? '']),
  ]

  return extractKeywordTags(textSources.join(' '))
})

const summaryErrorText = computed(() => {
  const error = resultQuery.error.value
  if (error instanceof Error) {
    return error.message
  }

  return ''
})

onMounted(() => {
  if (!authStore.initialized) {
    void authStore.restoreSession()
  }
})

function clampRatio(value: number) {
  return Math.min(0.72, Math.max(0.28, value))
}

function formatSeconds(seconds?: number | null) {
  if (typeof seconds !== 'number' || Number.isNaN(seconds) || seconds < 0) {
    return '--:--'
  }

  const totalSeconds = Math.floor(seconds)
  const minutes = Math.floor(totalSeconds / 60)
  const remainSeconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(remainSeconds).padStart(2, '0')}`
}

function formatChapterRange(startSeconds?: number | null, endSeconds?: number | null) {
  return `${formatSeconds(startSeconds)} - ${formatSeconds(endSeconds)}`
}

function normalizeKeywordCandidate(value: string) {
  const candidate = value
    .replace(/\.[a-z0-9]+$/gi, '')
    .replace(/^观察点[一二三四五六七八九十0-9]+[：:\s-]*/u, '')
    .replace(/^第[一二三四五六七八九十0-9]+部分[：:\s-]*/u, '')
    .split(/[：:]/u)
    .pop()
    ?.trim()
    .replace(/[“”"'《》【】（）()]/g, '')

  if (!candidate) {
    return ''
  }

  if (candidate.length < 2 || candidate.length > 12) {
    return ''
  }

  const blacklist = new Set(['引言与案例警示', '总结与建议', '开头', '结尾', '章节', '片段'])
  if (blacklist.has(candidate)) {
    return ''
  }

  return candidate
}

function extractKeywordTags(content: string) {
  const source = content.replace(/\.[a-z0-9]+$/gi, '').replace(/\s+/g, '')
  const chineseOnly = source.replace(/[^\u4e00-\u9fa5]/g, '')
  if (!chineseOnly) {
    return []
  }

  const stopTerms = new Set([
    '这个', '这个视', '这个视频', '视频', '这段', '本段', '部分', '有关', '进行', '一种', '因为',
    '以及', '同时', '其中', '通过', '如何', '不是', '一个', '没有', '他们', '我们', '你们',
    '自己', '可以', '需要', '阶段', '当前', '历史', '人物', '内容', '结果', '总结', '章节',
    '速览', '概要', '片段', '发言', '视频详', '详情', '问题', '表明', '指出', '强调', '揭示',
  ])

  const counts = new Map<string, number>()
  for (let size = 2; size <= 4; size += 1) {
    for (let index = 0; index <= chineseOnly.length - size; index += 1) {
      const token = chineseOnly.slice(index, index + size)
      if (stopTerms.has(token)) {
        continue
      }

      const current = counts.get(token) ?? 0
      counts.set(token, current + 1)
    }
  }

  const sorted = [...counts.entries()]
    .filter(([token, count]) => count > 1 || token.length >= 4)
    .sort((left, right) => {
      if (right[1] !== left[1]) {
        return right[1] - left[1]
      }
      if (right[0].length !== left[0].length) {
        return right[0].length - left[0].length
      }
      return left[0].localeCompare(right[0], 'zh-CN')
    })

  const selected: string[] = []
  for (const [token] of sorted) {
    if (selected.some((item) => item.includes(token) || token.includes(item))) {
      continue
    }

    selected.push(token)
    if (selected.length === 5) {
      break
    }
  }

  return selected
}

function handlePointerMove(event: PointerEvent) {
  if (!dragging.value) {
    return
  }

  const shell = document.querySelector<HTMLElement>('.detail-shell')
  const workspace = document.querySelector<HTMLElement>('.workspace-main')
  if (!shell || !workspace) {
    return
  }

  const shellRect = shell.getBoundingClientRect()
  const workspaceRect = workspace.getBoundingClientRect()
  const offsetLeft = workspaceRect.left - shellRect.left
  const relativeX = event.clientX - shellRect.left - offsetLeft
  splitRatio.value = clampRatio(relativeX / workspaceRect.width)
}

function stopDrag() {
  dragging.value = false
  window.removeEventListener('pointermove', handlePointerMove)
  window.removeEventListener('pointerup', stopDrag)
}

function startDrag(event: PointerEvent) {
  event.preventDefault()
  dragging.value = true
  window.addEventListener('pointermove', handlePointerMove)
  window.addEventListener('pointerup', stopDrag)
}

async function handleBack() {
  await router.push('/dashboard')
}

async function copyShareLink() {
  try {
    await navigator.clipboard.writeText(shareLink.value)
    copySuccess.value = true
    window.setTimeout(() => {
      copySuccess.value = false
    }, 1600)
  } catch {
    copySuccess.value = false
  }
}

async function handleLogout() {
  accountMenuOpen.value = false
  await authStore.logout()
  await router.push('/')
}

onBeforeUnmount(stopDrag)
</script>

<template>
  <div
    class="detail-shell"
    :class="{ 'is-dragging': dragging, 'sidebar-collapsed': !sidebarExpanded }"
    :style="{ '--sidebar-width': sidebarExpanded ? '184px' : '56px' }"
  >
    <aside class="workspace-sidebar">
      <div class="sidebar-head">
        <strong v-if="sidebarExpanded" class="sidebar-brand">智看视频</strong>
        <button
          class="sidebar-toggle"
          type="button"
          :aria-label="sidebarExpanded ? '收起菜单栏' : '展开菜单栏'"
          @click="sidebarExpanded = !sidebarExpanded"
        >
          {{ sidebarExpanded ? '⇆' : '☰' }}
        </button>
      </div>

      <nav class="sidebar-nav" aria-label="左侧菜单">
        <button v-for="item in menuItems" :key="item.label" class="sidebar-item" type="button">
          <span class="sidebar-item-icon">{{ item.icon }}</span>
          <span v-if="sidebarExpanded" class="sidebar-item-label">{{ item.label }}</span>
          <span v-if="sidebarExpanded && item.expandable" class="sidebar-item-arrow">⌄</span>
        </button>
      </nav>

      <div v-if="sidebarExpanded" class="sidebar-upgrade">
        <div class="upgrade-banner">
          <span class="upgrade-tag">限时</span>
          <strong>30% 折扣</strong>
          <span class="upgrade-arrow">›</span>
        </div>

        <div class="upgrade-progress">
          <div class="upgrade-progress-head">
            <span>✦</span>
            <span>0 / 10</span>
          </div>
          <div class="upgrade-progress-track">
            <span />
          </div>
          <div class="upgrade-progress-foot">
            <span>升级以获得更多积分</span>
            <span>0%</span>
          </div>
          <button class="upgrade-action" type="button">升级</button>
        </div>
      </div>
    </aside>

    <section class="workspace-content">
      <header class="workspace-topbar">
        <div class="topbar-left">
          <button class="topbar-icon-button" type="button" aria-label="返回" @click="handleBack">
            <span>‹</span>
          </button>
          <strong class="topbar-title">{{ fileName }}</strong>
          <button class="topbar-icon-button" type="button" aria-label="编辑文件名">
            <span>✎</span>
          </button>
      </div>

      <div class="topbar-right">
          <button class="topbar-share" type="button" @click="shareModalOpen = true">
            <span class="topbar-share-icon">⤴</span>
            <span>分享</span>
          </button>
          <button class="topbar-icon-button" type="button" aria-label="耳机设置">
            <span>🎧</span>
          </button>
          <button class="topbar-language" type="button">
            <span>简体中文</span>
            <span class="topbar-language-arrow">⌄</span>
          </button>
          <div class="topbar-account">
            <button
              class="topbar-avatar"
              type="button"
              aria-label="当前用户"
              @click="accountMenuOpen = !accountMenuOpen"
            >
              {{ avatarLabel }}
            </button>

            <div v-if="accountMenuOpen" class="account-dropdown">
              <div class="account-name">
                <span class="account-name-icon">⌘</span>
                <strong>{{ displayName }}</strong>
              </div>
              <button class="account-logout" type="button" @click="handleLogout">
                <span>↪</span>
                <span>退出登录</span>
              </button>
            </div>
          </div>
        </div>
      </header>

      <main class="workspace-main">
        <section class="workspace-pane workspace-pane--left" :style="{ width: `${splitRatio * 100}%` }">
          <div class="editor-panel">
            <div class="editor-toolbar">
              <div class="editor-tabs">
                <button
                  class="editor-tab"
                  :class="{ 'is-active': leftContentTab === 'transcript' }"
                  type="button"
                  @click="leftContentTab = 'transcript'"
                >
                  <span>⊞</span>
                  <span>语音转文本</span>
                </button>
                <button
                  class="editor-tab"
                  :class="{ 'is-active': leftContentTab === 'subtitle' }"
                  type="button"
                  @click="leftContentTab = 'subtitle'"
                >
                  <span>CC</span>
                  <span>原始字幕</span>
                </button>
              </div>

              <div class="editor-toolbar-actions">
                <button class="toolbar-action" type="button" aria-label="搜索">⌕</button>
                <button class="toolbar-action" type="button" aria-label="翻译">⟲</button>
                <button class="toolbar-switch" type="button" aria-label="切换状态">
                  <span />
                </button>
                <button class="toolbar-action" type="button" aria-label="展开">⤢</button>
              </div>
            </div>

            <div class="preview-panel">
              <div class="preview-chapter-strip">
                <button v-for="chapter in previewChapters" :key="chapter" type="button">
                  {{ chapter }}
                </button>
              </div>

              <div class="preview-stage">
                <button class="preview-play" type="button" aria-label="播放">
                  <span>▶</span>
                </button>
                <div class="preview-caption">问大家一个问题</div>
              </div>
            </div>

            <div class="content-panel">
              <template v-if="leftContentTab === 'transcript'">
                <div class="transcript-toolbar">
                  <div class="transcript-toolbar-spacer" />
                  <div class="transcript-toolbar-actions">
                    <button type="button">👥</button>
                    <button type="button">◔</button>
                    <button type="button">⧉</button>
                    <button type="button">↓</button>
                  </div>
                </div>

                <div class="transcript-list">
                  <article v-for="item in transcriptEntries" :key="item.id" class="transcript-card">
                    <div class="transcript-card-meta">
                      <span class="speaker-badge">S</span>
                      <span class="speaker-name">{{ item.speaker }}</span>
                      <span class="speaker-time">{{ item.time }}</span>
                    </div>
                    <div class="transcript-card-body">
                      <p>{{ item.text }}</p>
                      <div class="transcript-card-tools">
                        <button type="button">⟲</button>
                        <button type="button">⧉</button>
                      </div>
                    </div>
                  </article>
                </div>
              </template>

              <template v-else>
                <div class="subtitle-toolbar">
                  <label class="merge-toggle">
                    <span>合并</span>
                    <span class="merge-switch"><span /></span>
                    <span>1 条</span>
                  </label>

                  <div class="subtitle-toolbar-actions">
                    <button type="button">⧉</button>
                    <button type="button">↓</button>
                  </div>
                </div>

                <div class="subtitle-list">
                  <article
                    v-for="(item, index) in subtitleEntries"
                    :key="item.id"
                    class="subtitle-row"
                    :class="{ 'is-active': index === 0 }"
                  >
                    <span class="subtitle-time">{{ item.time }}</span>
                    <p>{{ item.text }}</p>
                  </article>
                </div>
              </template>
            </div>
          </div>
        </section>

        <button
          class="workspace-divider"
          type="button"
          aria-label="调整左右区域宽度"
          @pointerdown="startDrag"
        >
          <span />
        </button>

        <section class="workspace-pane workspace-pane--right">
          <div class="inspector-panel">
            <div class="inspector-tabs">
              <button
                v-for="tab in rightTabs"
                :key="tab.key"
                class="inspector-tab"
                :class="{ 'is-active': rightContentTab === tab.key }"
                type="button"
                @click="rightContentTab = tab.key === 'ai' ? 'studio' : tab.key"
              >
                <span>{{ tab.icon }}</span>
                <span>{{ tab.label }}</span>
                <span v-if="tab.badge" class="inspector-tab-badge">{{ tab.badge }}</span>
              </button>

              <div class="inspector-assistant">
                <button type="button">Qwen Plus ⌄</button>
              </div>
            </div>

            <div class="inspector-body">
              <template v-if="rightContentTab === 'summary'">
                <section class="summary-panel">
                  <div class="summary-section">
                    <div class="summary-section-head">
                      <h3>关键词</h3>
                      <span class="summary-section-note">基于结果内容提取</span>
                    </div>
                    <div v-if="keywordTags.length" class="keyword-list">
                      <span v-for="tag in keywordTags" :key="tag">{{ tag }}</span>
                    </div>
                    <p v-else class="summary-empty">当前结果不足以提取关键词。</p>
                  </div>

                  <div class="summary-section">
                    <div class="summary-section-head">
                      <h3>概要</h3>
                      <span v-if="resultQuery.isPending.value" class="summary-section-note">生成中</span>
                    </div>
                    <p v-if="summaryText">{{ summaryText }}</p>
                    <p v-else-if="summaryErrorText" class="summary-empty">摘要加载失败：{{ summaryErrorText }}</p>
                    <p v-else class="summary-empty">当前还没有生成摘要结果。</p>
                  </div>

                  <div class="summary-section">
                    <div class="summary-section-head">
                      <h3>章节速览</h3>
                      <span v-if="chapterCards.length" class="summary-section-note">{{ chapterCards.length }} 段</span>
                    </div>

                    <div v-if="chapterCards.length" class="chapter-card-list">
                      <article v-for="card in chapterCards" :key="card.id" class="chapter-card">
                        <div class="chapter-card-head">
                          <span>{{ card.range }}</span>
                          <strong>{{ card.title }}</strong>
                        </div>
                        <p>{{ card.text }}</p>
                      </article>
                    </div>
                    <p v-else class="summary-empty">当前还没有生成章节速览。</p>
                  </div>
                </section>
              </template>

              <template v-else-if="rightContentTab === 'chapters'">
                <section class="placeholder-panel">
                  <h3>思维导图</h3>
                  <p>这里先预留思维导图视图，后续接真实结构化内容。</p>
                </section>
              </template>

              <template v-else-if="rightContentTab === 'notes'">
                <section class="placeholder-panel">
                  <h3>视频转图文</h3>
                  <p>这里先预留图文整理视图，后续接真实图文结果。</p>
                </section>
              </template>

              <template v-else>
                <section class="placeholder-panel">
                  <h3>备忘录 / AI 工作室</h3>
                  <p>这里先预留右侧扩展工作区，后续再接笔记和 AI 交互能力。</p>
                </section>
              </template>
            </div>
          </div>
        </section>
      </main>
    </section>

    <div v-if="shareModalOpen" class="share-modal-backdrop" @click.self="shareModalOpen = false">
      <section class="share-modal" aria-label="复制并分享链接">
        <div class="share-modal-head">
          <strong>复制并分享链接</strong>
          <button type="button" @click="shareModalOpen = false">×</button>
        </div>

        <div class="share-link-box">
          <span class="share-link-icon">🔗</span>
          <p>{{ shareLink }}</p>
          <button class="share-copy-button" type="button" @click="copyShareLink">
            {{ copySuccess ? '已复制' : '复制' }}
          </button>
        </div>

        <div class="share-toggle-card">
          <div class="share-toggle-copy">
            <strong>链接访问</strong>
            <p>链接访问权限，开启后分享链接可以公开访问。</p>
          </div>
          <button
            class="share-switch"
            :class="{ 'is-on': shareAccessEnabled }"
            type="button"
            @click="shareAccessEnabled = !shareAccessEnabled"
          >
            <span />
          </button>
        </div>

        <div class="share-toggle-card">
          <div class="share-toggle-copy">
            <strong>允许创建副本</strong>
            <p>被分享者可以对当前记录创建副本，并对副本进行编辑、分享等操作。</p>
          </div>
          <button
            class="share-switch"
            :class="{ 'is-on': allowCreateCopy }"
            type="button"
            @click="allowCreateCopy = !allowCreateCopy"
          >
            <span />
          </button>
        </div>

        <div class="share-social-grid">
          <button type="button">
            <span>X</span>
            <small>Twitter</small>
          </button>
          <button type="button">
            <span>f</span>
            <small>Facebook</small>
          </button>
          <button type="button">
            <span>r</span>
            <small>Reddit</small>
          </button>
          <button type="button">
            <span>in</span>
            <small>LinkedIn</small>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.detail-shell {
  --topbar-height: 58px;
  display: grid;
  grid-template-columns: var(--sidebar-width) minmax(0, 1fr);
  width: 100%;
  min-height: 100vh;
  background: #eef2f7;
}

.workspace-sidebar {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  padding: 14px 10px 12px;
  background: #ffffff;
  border-right: 1px solid rgba(50, 78, 128, 0.08);
  overflow: hidden;
}

.sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 32px;
  margin-bottom: 18px;
}

.sidebar-brand {
  color: #527df7;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.03em;
}

.sidebar-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #5b6980;
  font-size: 14px;
  cursor: pointer;
}

.sidebar-toggle:hover {
  background: #f1f4fa;
}

.sidebar-nav {
  display: grid;
  gap: 4px;
}

.sidebar-item {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 34px;
  padding: 0 2px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: #37465f;
  text-align: left;
  cursor: pointer;
}

.sidebar-item:hover {
  background: #f5f7fb;
}

.sidebar-item-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  color: #586577;
  font-size: 12px;
}

.sidebar-item-label {
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.sidebar-item-arrow {
  color: #b2bcc9;
  font-size: 12px;
}

.sidebar-upgrade {
  margin-top: auto;
  display: grid;
  gap: 10px;
}

.upgrade-banner {
  position: relative;
  display: grid;
  gap: 6px;
  padding: 12px 12px 14px;
  border-radius: 10px;
  background: linear-gradient(135deg, #8c55ff, #6e35f3 65%, #6127db);
  color: #ffffff;
  overflow: hidden;
}

.upgrade-tag {
  font-size: 10px;
  font-weight: 700;
  opacity: 0.82;
}

.upgrade-banner strong {
  font-size: 16px;
  font-weight: 800;
}

.upgrade-arrow {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 20px;
  opacity: 0.84;
}

.upgrade-progress {
  display: grid;
  gap: 8px;
  padding: 10px 8px 8px;
  border-radius: 10px;
  background: #f7f9fc;
  box-shadow: inset 0 0 0 1px rgba(80, 99, 126, 0.08);
}

.upgrade-progress-head,
.upgrade-progress-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #7d8899;
  font-size: 11px;
  font-weight: 600;
}

.upgrade-progress-track {
  width: 100%;
  height: 4px;
  border-radius: 999px;
  background: #e0e7f1;
  overflow: hidden;
}

.upgrade-progress-track span {
  display: block;
  width: 0%;
  height: 100%;
  background: linear-gradient(90deg, #8b57ff, #bb8bff);
}

.upgrade-action {
  width: 100%;
  min-height: 30px;
  border: 0;
  border-radius: 8px;
  background: #0a0f17;
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.workspace-content {
  display: grid;
  grid-template-rows: var(--topbar-height) minmax(0, 1fr);
  min-width: 0;
  min-height: 100vh;
}

.workspace-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 0 18px 0 20px;
  background: #ffffff;
  border-bottom: 1px solid rgba(50, 78, 128, 0.08);
}

.topbar-left,
.topbar-right {
  display: flex;
  align-items: center;
}

.topbar-left {
  gap: 6px;
  min-width: 0;
}

.topbar-right {
  gap: 10px;
  margin-left: auto;
}

.topbar-title {
  color: #243349;
  font-size: 15px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}

.topbar-icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #5e6e86;
  font-size: 15px;
  cursor: pointer;
}

.topbar-icon-button:hover {
  background: #f4f7fb;
}

.topbar-share {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid rgba(137, 81, 255, 0.42);
  border-radius: 8px;
  background: #ffffff;
  color: #7c4dff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.topbar-share-icon {
  font-size: 13px;
}

.topbar-language {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
  border: 0;
  padding: 0;
  background: transparent;
  color: #34445f;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.topbar-language-arrow {
  color: #8fa0b5;
  font-size: 11px;
}

.topbar-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: 0;
  border-radius: 999px;
  background: #f06b00;
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.topbar-account {
  position: relative;
}

.account-dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 188px;
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid rgba(112, 126, 150, 0.12);
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 18px 38px rgba(30, 39, 58, 0.14);
  z-index: 20;
}

.account-name {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 36px;
  color: #394860;
}

.account-name-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  color: #68788f;
  font-size: 12px;
}

.account-name strong {
  font-size: 14px;
  font-weight: 600;
}

.account-logout {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 38px;
  border: 0;
  border-radius: 10px;
  padding: 0 8px;
  background: transparent;
  color: #485871;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.account-logout:hover {
  background: #f4f7fb;
}

.share-modal-backdrop {
  position: fixed;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(18, 24, 39, 0.2);
  z-index: 40;
}

.share-modal {
  width: min(446px, calc(100vw - 32px));
  display: grid;
  gap: 14px;
  padding: 16px;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 24px 54px rgba(29, 37, 55, 0.18);
}

.share-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.share-modal-head strong {
  color: #202d42;
  font-size: 20px;
  font-weight: 700;
}

.share-modal-head button {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #4f5f77;
  font-size: 20px;
  cursor: pointer;
}

.share-link-box,
.share-toggle-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f3f5f9;
}

.share-link-icon {
  font-size: 16px;
}

.share-link-box p {
  flex: 1;
  margin: 0;
  color: #586779;
  font-size: 14px;
  line-height: 1.4;
  word-break: break-all;
}

.share-copy-button {
  min-width: 64px;
  min-height: 34px;
  border: 0;
  border-radius: 10px;
  padding: 0 14px;
  background: #8a55ff;
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.share-toggle-copy {
  flex: 1;
  display: grid;
  gap: 6px;
}

.share-toggle-copy strong {
  color: #28364b;
  font-size: 15px;
  font-weight: 700;
}

.share-toggle-copy p {
  margin: 0;
  color: #ff4c4c;
  font-size: 13px;
  line-height: 1.5;
}

.share-switch {
  position: relative;
  width: 40px;
  height: 24px;
  flex: 0 0 40px;
  border: 0;
  border-radius: 999px;
  background: #d9dfeb;
  cursor: pointer;
}

.share-switch span {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  border-radius: 999px;
  background: #ffffff;
  box-shadow: 0 1px 4px rgba(35, 43, 58, 0.22);
  transition: transform 0.18s ease;
}

.share-switch.is-on {
  background: #8a55ff;
}

.share-switch.is-on span {
  transform: translateX(16px);
}

.share-social-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.share-social-grid button {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 110px;
  border: 0;
  border-radius: 12px;
  background: #f3f5f9;
  color: #2a364b;
  cursor: pointer;
}

.share-social-grid span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  font-size: 30px;
  line-height: 1;
}

.share-social-grid small {
  color: #4d5c72;
  font-size: 14px;
  font-weight: 500;
}

.workspace-main {
  display: flex;
  min-width: 0;
  min-height: 0;
  background: #e9edf3;
}

.workspace-pane {
  display: flex;
  align-items: stretch;
  justify-content: stretch;
  min-width: 0;
  min-height: 0;
}

.workspace-pane--left {
  background: #ffffff;
}

.workspace-pane--right {
  flex: 1;
  background: #ffffff;
}

.workspace-divider {
  position: relative;
  width: 4px;
  flex: 0 0 4px;
  border: 0;
  padding: 0;
  background: #eef2f7;
  cursor: col-resize;
}

.workspace-divider span {
  position: absolute;
  inset: 0;
  margin: auto;
  width: 2px;
  height: 100%;
  border-radius: 999px;
  background: rgba(169, 181, 199, 0.4);
}

.panel-label {
  color: #31465f;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.is-dragging,
.is-dragging * {
  user-select: none;
  cursor: col-resize;
}

.sidebar-collapsed .workspace-sidebar {
  padding-left: 8px;
  padding-right: 8px;
}

.sidebar-collapsed .sidebar-head {
  justify-content: center;
}

.sidebar-collapsed .sidebar-nav {
  gap: 6px;
}

.sidebar-collapsed .sidebar-item {
  display: flex;
  justify-content: center;
  min-height: 32px;
  padding: 0;
}

.editor-panel {
  display: grid;
  grid-template-rows: 44px 480px minmax(0, 1fr);
  width: 100%;
  min-height: 0;
  background: #ffffff;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 10px 0 8px;
  border-bottom: 1px solid rgba(110, 128, 158, 0.12);
  background: #ffffff;
}

.editor-tabs {
  display: flex;
  align-items: stretch;
  gap: 2px;
  height: 100%;
}

.editor-tab {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 100%;
  padding: 0 8px;
  border: 0;
  background: transparent;
  color: #26344b;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.editor-tab.is-active {
  color: #8d55ff;
}

.editor-tab.is-active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2px;
  border-radius: 999px;
  background: #8d55ff;
}

.editor-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #4b5d76;
  font-size: 14px;
  cursor: pointer;
}

.toolbar-switch {
  position: relative;
  width: 30px;
  height: 18px;
  border: 0;
  border-radius: 999px;
  padding: 0;
  background: #d7deea;
  cursor: pointer;
}

.toolbar-switch span {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 14px;
  height: 14px;
  border-radius: 999px;
  background: #ffffff;
}

.preview-panel {
  display: grid;
  grid-template-rows: 28px minmax(0, 1fr);
  min-height: 0;
  border-bottom: 1px solid rgba(110, 128, 158, 0.12);
  background: #10131a;
}

.preview-chapter-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  background: #090b10;
}

.preview-chapter-strip button {
  border: 0;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  background: transparent;
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  cursor: pointer;
}

.preview-stage {
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.08) 1px, transparent 1px),
    radial-gradient(circle at 18% 85%, rgba(169, 46, 42, 0.35), transparent 24%),
    linear-gradient(180deg, #232427 0%, #111216 100%);
  background-size:
    24px 24px,
    24px 24px,
    100% 100%,
    100% 100%;
}

.preview-stage::before {
  content: '';
  position: absolute;
  left: 8%;
  right: 8%;
  bottom: -10%;
  height: 45%;
  background: linear-gradient(180deg, rgba(177, 93, 49, 0.85), rgba(101, 53, 22, 0.95));
  clip-path: polygon(12% 62%, 38% 18%, 61% 12%, 85% 48%, 93% 70%, 6% 82%);
  opacity: 0.9;
}

.preview-stage::after {
  content: '';
  position: absolute;
  left: 52%;
  top: 32%;
  width: 58px;
  height: 38px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.62);
  background: rgba(255, 255, 255, 0.12);
  transform: translate(-50%, -50%);
}

.preview-play {
  position: absolute;
  inset: 0;
  margin: auto;
  width: 58px;
  height: 38px;
  border: 0;
  background: transparent;
  color: #ffffff;
  font-size: 18px;
  z-index: 1;
  cursor: pointer;
}

.preview-caption {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 20px;
  text-align: center;
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.46);
  z-index: 1;
}

.content-panel {
  min-height: 0;
  background: #ffffff;
}

.transcript-toolbar,
.subtitle-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 38px;
  padding: 0 10px;
  border-bottom: 1px solid rgba(110, 128, 158, 0.08);
}

.transcript-toolbar-spacer {
  flex: 1;
}

.transcript-toolbar-actions,
.subtitle-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.transcript-toolbar-actions button,
.subtitle-toolbar-actions button {
  width: 22px;
  height: 22px;
  border: 0;
  border-radius: 8px;
  background: rgba(141, 85, 255, 0.12);
  color: #8d55ff;
  font-size: 12px;
  cursor: pointer;
}

.transcript-list,
.subtitle-list {
  display: grid;
  gap: 10px;
  max-height: calc(100vh - 58px - 44px - 480px - 40px);
  padding: 10px;
  overflow: auto;
}

.transcript-card {
  display: grid;
  gap: 8px;
}

.transcript-card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #9b89c6;
  font-size: 12px;
  font-weight: 600;
}

.speaker-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 999px;
  background: #f36c96;
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
}

.speaker-name {
  color: #a06b9c;
}

.speaker-time {
  color: #8d55ff;
}

.transcript-card-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid rgba(174, 142, 255, 0.28);
  border-radius: 10px;
  background: #ffffff;
}

.transcript-card-body p {
  margin: 0;
  color: #2d3d56;
  font-size: 13px;
  line-height: 1.55;
}

.transcript-card-tools {
  display: grid;
  gap: 6px;
}

.transcript-card-tools button {
  width: 18px;
  height: 18px;
  border: 0;
  background: transparent;
  color: #c3cad6;
  font-size: 11px;
  cursor: pointer;
}

.merge-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #18263d;
  font-size: 13px;
  font-weight: 500;
}

.merge-switch {
  position: relative;
  width: 34px;
  height: 16px;
  border-radius: 999px;
  background: #e5e8f0;
}

.merge-switch span {
  position: absolute;
  top: 1px;
  left: 1px;
  width: 14px;
  height: 14px;
  border-radius: 999px;
  background: #ffffff;
  box-shadow: 0 1px 4px rgba(29, 39, 60, 0.2);
}

.subtitle-row {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 10px;
  padding: 12px 12px 12px 10px;
  border: 1px solid rgba(133, 146, 170, 0.16);
  border-radius: 10px;
  background: #ffffff;
}

.subtitle-row.is-active {
  border-color: rgba(141, 85, 255, 0.58);
  box-shadow: inset 0 0 0 1px rgba(141, 85, 255, 0.12);
}

.subtitle-time {
  color: #8d55ff;
  font-size: 12px;
  font-weight: 600;
}

.subtitle-row p {
  margin: 0;
  color: #26354d;
  font-size: 13px;
  line-height: 1.55;
}

.inspector-panel {
  display: grid;
  grid-template-rows: 44px minmax(0, 1fr);
  width: 100%;
  min-height: 0;
  background: #ffffff;
}

.inspector-tabs {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  padding: 0 10px 0 12px;
  border-bottom: 1px solid rgba(110, 128, 158, 0.12);
  background: #ffffff;
}

.inspector-tab {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 100%;
  border: 0;
  padding: 0;
  background: transparent;
  color: #33445d;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
}

.inspector-tab.is-active {
  color: #8d55ff;
}

.inspector-tab.is-active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2px;
  border-radius: 999px;
  background: #8d55ff;
}

.inspector-tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 16px;
  padding: 0 5px;
  border-radius: 999px;
  background: #ff6159;
  color: #ffffff;
  font-size: 9px;
  font-weight: 700;
}

.inspector-assistant {
  margin-left: auto;
}

.inspector-assistant button {
  min-height: 28px;
  border: 1px solid rgba(116, 131, 158, 0.16);
  border-radius: 8px;
  padding: 0 10px;
  background: #ffffff;
  color: #66758d;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.inspector-body {
  min-height: 0;
  overflow: auto;
  background: #ffffff;
}

.summary-panel {
  display: grid;
  gap: 18px;
  padding: 12px 12px 20px;
}

.summary-section {
  display: grid;
  gap: 10px;
}

.summary-section h3 {
  margin: 0;
  color: #25344a;
  font-size: 14px;
  font-weight: 700;
}

.summary-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.summary-section-note {
  color: #8d55ff;
  font-size: 12px;
  font-weight: 600;
}

.keyword-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.keyword-list span,
.keyword-list button {
  min-height: 28px;
  border: 1px solid rgba(118, 131, 154, 0.16);
  border-radius: 8px;
  padding: 0 10px;
  background: #ffffff;
  color: #5b687d;
  font-size: 12px;
  font-weight: 600;
}

.summary-section p {
  margin: 0;
  color: #3a495f;
  font-size: 13px;
  line-height: 1.7;
}

.summary-empty {
  color: #7b889d;
}

.chapter-card-list {
  display: grid;
  gap: 12px;
}

.chapter-card {
  display: grid;
  gap: 8px;
}

.chapter-card-head {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.chapter-card-head span {
  color: #8d55ff;
  font-size: 13px;
  font-weight: 600;
}

.chapter-card-head strong {
  color: #25344a;
  font-size: 13px;
  font-weight: 700;
}

.placeholder-panel {
  display: grid;
  gap: 10px;
  padding: 18px 16px;
}

.placeholder-panel h3 {
  margin: 0;
  color: #25344a;
  font-size: 16px;
  font-weight: 700;
}

.placeholder-panel p {
  margin: 0;
  color: #66758d;
  font-size: 13px;
  line-height: 1.65;
}

@media (max-width: 960px) {
  .detail-shell {
    --sidebar-width: 56px !important;
  }

  .workspace-sidebar {
    padding-left: 8px;
    padding-right: 8px;
  }

  .sidebar-brand,
  .sidebar-item-label,
  .sidebar-item-arrow,
  .sidebar-upgrade {
    display: none;
  }

  .sidebar-head {
    justify-content: center;
  }

  .sidebar-item {
    display: flex;
    justify-content: center;
    min-height: 32px;
    padding: 0;
  }

  .editor-panel {
    grid-template-rows: 44px 380px minmax(0, 1fr);
  }
}
</style>
