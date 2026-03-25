<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

import VideoUploadPanel from '../../components/upload/VideoUploadPanel.vue'
import AppLayout from '../../layouts/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()

const navItems = computed(() => {
  if (authStore.isLoggedIn) {
    return [{ label: 'Dashboard', to: '/dashboard' }]
  }

  return [
    { label: '登录', to: '/login' },
    { label: '注册', to: '/register' },
  ]
})

const highlights = ['视频转写', '智能摘要', '章节导航', '结果笔记']

const featureSections = [
  {
    badge: 'Summary',
    title: '先把一节课讲了什么读明白。',
    description:
      '系统先生成可快速扫读的摘要，把结论、核心观点和重点段落放到一页里，避免你先被播放器进度条牵着走。',
    points: ['适合课程复盘', '支持快速扫读', '优先抓主线内容'],
  },
  {
    badge: 'Chapters',
    title: '再决定应该回看哪一段。',
    description:
      '章节区把视频切成更容易定位的目录索引。你不需要拖进度条试错，可以直接回到某个概念、案例或推导片段。',
    points: ['目录化回看', '减少无效拖动', '适合知识点定位'],
  },
  {
    badge: 'Notes',
    title: '最后把你的判断留在结果页里。',
    description:
      '笔记能力不是独立打开另一个文档，而是围绕摘要和章节继续沉淀，让学习结果和个人理解留在同一个工作台。',
    points: ['围绕结果沉淀', '学习上下文完整', '便于后续回访'],
  },
]

const useCases = [
  '公开课与网课回看',
  '知识分享视频整理',
  '课程助教整理内容要点',
  '个人学习复盘与回顾',
]
</script>

<template>
  <AppLayout
    title="把学习视频整理成能阅读、能跳转、能继续沉淀的学习工作台。"
    description="上传课程、讲解或知识分享视频，系统自动生成摘要、章节和笔记入口，让你先抓重点，再决定从哪里重看。"
    :nav-items="navItems"
    :hide-hero="true"
  >
    <section class="hero-section">
      <div class="hero-copy">
        <p class="eyebrow home-eyebrow">长视频学习整理工具</p>
        <h1 class="hero-title">把学习视频整理成可读、可跳、可记录的学习结果页。</h1>
        <p class="hero-description">
          面向课程、讲解和知识分享视频。你先得到摘要、章节和笔记入口，再决定从哪里开始回看。
        </p>

        <div class="hero-actions">
          <RouterLink v-if="authStore.isLoggedIn" class="primary-button" to="/dashboard">
            进入 Dashboard
          </RouterLink>
          <RouterLink v-else class="primary-button" to="/login">登录后开始上传</RouterLink>
          <RouterLink class="secondary-button" to="/register">创建账号</RouterLink>
        </div>

        <div class="hero-highlight-row">
          <span v-for="item in highlights" :key="item" class="highlight-chip">{{ item }}</span>
        </div>
      </div>

      <div class="hero-panel-shell">
        <div class="hero-panel-head">
          <p>上传入口</p>
          <span>本地 MP4 / 自动跳转状态页</span>
        </div>
        <VideoUploadPanel
          title="上传学习视频"
          description="上传成功后自动进入处理状态页，处理完成后在结果页查看摘要、章节与笔记。"
          cta-label="选择视频并上传"
          :requires-login="!authStore.isLoggedIn"
          tone="landing"
        />
      </div>
    </section>

    <section class="trust-strip panel">
      <div class="trust-item">
        <strong>主流程已打通</strong>
        <span>登录、上传、处理状态、结果页、笔记</span>
      </div>
      <div class="trust-item">
        <strong>核心目标</strong>
        <span>让长视频先变成学习索引页，再进入回看</span>
      </div>
      <div class="trust-item">
        <strong>面向内容</strong>
        <span>课程视频、教程讲解、知识分享、复盘材料</span>
      </div>
    </section>

    <section class="features-section">
      <header class="section-header">
        <p class="section-kicker">How it helps</p>
        <h2>首页不是播放器，而是进入学习结果的入口页。</h2>
      </header>

      <article
        v-for="(section, index) in featureSections"
        :key="section.badge"
        class="feature-row"
        :class="{ reverse: index % 2 === 1 }"
      >
        <div class="feature-copy">
          <span class="feature-badge">{{ section.badge }}</span>
          <h3>{{ section.title }}</h3>
          <p>{{ section.description }}</p>
          <ul class="feature-points">
            <li v-for="point in section.points" :key="point">{{ point }}</li>
          </ul>
        </div>

        <div class="feature-visual panel">
          <div class="visual-window">
            <div class="visual-toolbar">
              <span />
              <span />
              <span />
            </div>
            <div class="visual-body">
              <div class="visual-title" />
              <div class="visual-line wide" />
              <div class="visual-line" />
              <div class="visual-line" />
              <div class="visual-cards">
                <div class="visual-card" />
                <div class="visual-card" />
                <div class="visual-card" />
              </div>
            </div>
          </div>
        </div>
      </article>
    </section>

    <section class="use-case-section panel">
      <header class="section-header compact">
        <p class="section-kicker">Use cases</p>
        <h2>适合把“看过”变成“整理过”的学习场景。</h2>
      </header>

      <div class="use-case-grid">
        <article v-for="item in useCases" :key="item" class="use-case-card">
          <h3>{{ item }}</h3>
          <p>把视频先转成结构化结果，再决定哪里需要完整回放与继续记录。</p>
        </article>
      </div>
    </section>

    <section class="cta-section panel">
      <div>
        <p class="section-kicker">Start now</p>
        <h2>先让视频变清楚，再开始学习。</h2>
        <p class="cta-copy">现在这版 MVP 已经能完成上传、处理、结果查看和笔记沉淀。</p>
      </div>

      <div class="cta-actions">
        <RouterLink v-if="authStore.isLoggedIn" class="primary-button" to="/dashboard">
          进入 Dashboard
        </RouterLink>
        <RouterLink v-else class="primary-button" to="/login">登录体验</RouterLink>
        <RouterLink class="secondary-button" to="/register">注册账号</RouterLink>
      </div>
    </section>
  </AppLayout>
</template>

<style scoped>
.hero-section {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 460px);
  gap: 32px;
  align-items: center;
  padding: 18px 0 8px;
}

.hero-copy {
  display: grid;
  gap: 18px;
}

.home-eyebrow {
  margin: 0;
  width: fit-content;
}

.hero-title {
  margin: 0;
  max-width: 8.8ch;
  font-family: var(--font-display);
  font-size: clamp(46px, 6vw, 82px);
  line-height: 0.94;
  letter-spacing: -0.06em;
}

.hero-description {
  margin: 0;
  max-width: 620px;
  color: var(--text-muted);
  font-size: 17px;
  line-height: 1.7;
}

.hero-actions,
.cta-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.primary-button,
.secondary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 50px;
  padding: 0 22px;
  border-radius: 999px;
  font-weight: 600;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    background-color 0.18s ease;
}

.primary-button {
  background: linear-gradient(135deg, #1f5eff, #3a68ff 58%, #5d83ff);
  color: #fff;
  box-shadow: 0 16px 30px rgba(31, 94, 255, 0.24);
}

.secondary-button {
  border: 1px solid rgba(42, 56, 87, 0.14);
  background: rgba(255, 255, 255, 0.82);
}

.primary-button:hover,
.secondary-button:hover {
  transform: translateY(-1px);
}

.hero-highlight-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.highlight-chip {
  padding: 9px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(42, 56, 87, 0.08);
  color: var(--text-muted);
  font-size: 13px;
}

.hero-panel-shell {
  display: grid;
  gap: 14px;
}

.hero-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.hero-panel-head p,
.hero-panel-head span {
  margin: 0;
}

.hero-panel-head p {
  font-weight: 600;
}

.hero-panel-head span {
  color: var(--text-muted);
  font-size: 13px;
}

.trust-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 28px;
  padding: 22px 24px;
}

.trust-item {
  display: grid;
  gap: 6px;
}

.trust-item span {
  color: var(--text-muted);
}

.features-section,
.use-case-section,
.cta-section {
  margin-top: 34px;
}

.section-header {
  display: grid;
  gap: 10px;
  margin-bottom: 22px;
}

.section-header.compact {
  margin-bottom: 20px;
}

.section-kicker {
  margin: 0;
  color: var(--accent-strong);
  font-size: 13px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.section-header h2,
.cta-section h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(30px, 3.3vw, 46px);
  line-height: 1.02;
  letter-spacing: -0.04em;
}

.feature-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 460px);
  gap: 28px;
  align-items: center;
  padding: 28px 0;
}

.feature-row + .feature-row {
  border-top: 1px solid rgba(42, 56, 87, 0.08);
}

.feature-row.reverse {
  grid-template-columns: minmax(320px, 460px) minmax(0, 1fr);
}

.feature-row.reverse .feature-copy {
  order: 2;
}

.feature-row.reverse .feature-visual {
  order: 1;
}

.feature-copy {
  display: grid;
  gap: 16px;
}

.feature-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(31, 94, 255, 0.08);
  color: var(--accent-strong);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.feature-copy h3,
.use-case-card h3 {
  margin: 0;
  font-size: 30px;
  line-height: 1.12;
}

.feature-copy p,
.cta-copy,
.use-case-card p {
  margin: 0;
  color: var(--text-muted);
}

.feature-points {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.feature-points li {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(42, 56, 87, 0.08);
}

.feature-visual {
  padding: 22px;
}

.visual-window {
  overflow: hidden;
  border-radius: 20px;
  border: 1px solid rgba(42, 56, 87, 0.08);
  background: rgba(255, 255, 255, 0.88);
}

.visual-toolbar {
  display: flex;
  gap: 8px;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(42, 56, 87, 0.08);
}

.visual-toolbar span {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: rgba(42, 56, 87, 0.18);
}

.visual-body {
  display: grid;
  gap: 12px;
  padding: 18px;
}

.visual-title,
.visual-line,
.visual-card {
  border-radius: 12px;
  background: linear-gradient(90deg, rgba(31, 94, 255, 0.08), rgba(42, 56, 87, 0.08));
}

.visual-title {
  height: 18px;
  width: 48%;
}

.visual-line {
  height: 14px;
  width: 100%;
}

.visual-line.wide {
  width: 84%;
}

.visual-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 4px;
}

.visual-card {
  height: 98px;
}

.use-case-section,
.cta-section {
  padding: 24px;
}

.use-case-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.use-case-card {
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.54);
  border: 1px solid rgba(42, 56, 87, 0.08);
}

.cta-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

@media (max-width: 980px) {
  .hero-section,
  .feature-row,
  .feature-row.reverse,
  .trust-strip,
  .use-case-grid,
  .cta-section {
    grid-template-columns: 1fr;
  }

  .trust-strip,
  .use-case-grid {
    display: grid;
  }

  .feature-row.reverse .feature-copy,
  .feature-row.reverse .feature-visual {
    order: initial;
  }

  .cta-section {
    display: grid;
  }
}

@media (max-width: 720px) {
  .hero-title {
    max-width: none;
    font-size: clamp(38px, 13vw, 60px);
  }

  .visual-cards {
    grid-template-columns: 1fr;
  }
}
</style>
