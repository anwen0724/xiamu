<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { uploadVideo } from '../../api/video'
import AppLayout from '../../layouts/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const inputRef = ref<HTMLInputElement | null>(null)
const pending = ref(false)
const selectedFileName = ref('')
const errorMessage = ref('')

const avatarLabel = computed(() => {
  const seed = authStore.currentUser?.userName || authStore.currentUser?.userAccount || 'U'
  return seed.trim().charAt(0).toUpperCase()
})

const centerNavItems = [
  { label: '产品', to: { name: 'placeholder', query: { label: '产品' } } },
  { label: '热门精选总结', to: { name: 'placeholder', query: { label: '热门精选总结' } }, hot: true },
  { label: '安装插件', to: { name: 'placeholder', query: { label: '安装插件' } } },
  { label: '价格', to: { name: 'placeholder', query: { label: '价格' } } },
  { label: '常见问题', to: { name: 'placeholder', query: { label: '常见问题' } } },
  { label: '博客', to: { name: 'placeholder', query: { label: '博客' } } },
]

const navItems = computed(() => {
  if (authStore.isLoggedIn) {
    return [
      { label: 'Workspace', to: '/dashboard', kind: 'workspace' as const, icon: '▣' },
      { label: avatarLabel.value, to: '/dashboard', kind: 'avatar' as const },
    ]
  }

  return [
    { label: '登录', to: '/login', kind: 'ghost' as const },
    { label: '注册', to: '/register', kind: 'primary' as const },
  ]
})

const featureCards = [
  {
    icon: '🎙️',
    title: '视频转录',
    description: '快速提取字幕',
    tone: 'blue',
  },
  {
    icon: '🧠',
    title: '智能总结',
    description: 'AI 生成摘要',
    tone: 'pink',
  },
  {
    icon: '🧭',
    title: '章节导航',
    description: '自动整理结构',
    tone: 'green',
  },
  {
    icon: '📝',
    title: '结果笔记',
    description: '边看边记更方便',
    tone: 'peach',
  },
]

function pickFile() {
  if (!authStore.isLoggedIn) {
    void router.push('/login')
    return
  }

  inputRef.value?.click()
}

async function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) {
    return
  }

  selectedFileName.value = file.name
  errorMessage.value = ''
  pending.value = true

  try {
    const result = await uploadVideo(file)
    await router.push(`/video/${result.id}`)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '上传失败，请稍后重试'
  } finally {
    pending.value = false
    target.value = ''
  }
}
</script>

<template>
  <AppLayout
    title="AI 音视频转录与总结"
    description="上传学习视频并生成结构化结果。"
    :center-nav-items="centerNavItems"
    locale-label="简体中文"
    :nav-items="navItems"
    :hide-hero="true"
  >
    <section class="home-landing">
      <div class="hero-copy">
        <h1 class="hero-title">
          <span class="hero-title-icon">👏</span>
          <span>AI 音视频转录与总结</span>
        </h1>
        <p class="hero-highlight"><span>10x</span> 你的学习效率</p>
      </div>

      <div class="hero-stage">
        <div class="upload-shell">
          <button class="upload-input" :class="{ 'has-file': Boolean(selectedFileName) }" type="button" @click="pickFile">
            <span class="input-icon">⭱</span>
            <span class="input-text">
              {{ selectedFileName || '选择本地 MP4 学习视频文件' }}
            </span>
          </button>
          <button class="upload-submit" type="button" :disabled="pending" @click="pickFile">
            {{ pending ? '上传中...' : '开始' }}
          </button>
        </div>

        <div class="hero-sub-action">
          <span>或者</span>
          <button class="inline-upload" type="button" @click="pickFile">
            {{ authStore.isLoggedIn ? '上传本地视频文件' : '登录后上传本地视频文件' }}
          </button>
        </div>

        <p v-if="errorMessage" class="error-box">{{ errorMessage }}</p>

        <div class="feature-grid">
          <article v-for="item in featureCards" :key="item.title" class="feature-card">
            <div class="feature-icon" :data-tone="item.tone">{{ item.icon }}</div>
            <h2>{{ item.title }}</h2>
            <p>{{ item.description }}</p>
          </article>
        </div>
      </div>

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

<style>
.home-landing {
  position: relative;
  display: grid;
  gap: 24px;
  padding: 154px 0 72px;
  min-height: calc(100vh - 128px);
  align-content: start;
  overflow: hidden;
}

.hero-copy {
  display: grid;
  gap: 8px;
  justify-items: center;
  text-align: center;
  position: relative;
  z-index: 1;
  animation: rise-in 0.6s ease both;
}

.hero-highlight {
  margin: 0;
}

.hero-title {
  margin: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  font-size: clamp(52px, 5.3vw, 78px);
  font-weight: 900;
  line-height: 1.02;
  letter-spacing: -0.07em;
  color: #12192d;
  text-align: center;
}

.hero-title-icon {
  font-size: 0.72em;
  line-height: 1;
}

.hero-highlight {
  font-size: clamp(30px, 2.9vw, 44px);
  font-weight: 800;
  background: linear-gradient(135deg, var(--accent-strong) 0%, var(--accent) 62%, var(--accent-rose) 120%);
  -webkit-background-clip: text;
  color: transparent;
}

.hero-highlight span {
  font-size: 1.14em;
}

.hero-stage {
  position: relative;
  width: min(1160px, 100%);
  margin: 0 auto;
  display: grid;
  gap: 16px;
  padding: 4px 0 0;
  z-index: 1;
  animation: rise-in 0.75s ease both;
}

.upload-shell {
  width: min(920px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 142px;
  gap: 8px;
  padding: 8px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow:
    0 14px 30px rgba(62, 46, 115, 0.045),
    0 4px 10px rgba(62, 46, 115, 0.03);
  backdrop-filter: blur(12px);
  position: relative;
  z-index: 1;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.upload-shell:hover {
  transform: translateY(-1px);
  box-shadow:
    0 18px 36px rgba(62, 46, 115, 0.06),
    0 6px 14px rgba(62, 46, 115, 0.035);
  border-color: rgba(143, 108, 255, 0.14);
}

.upload-input,
.upload-submit {
  min-height: 60px;
  border: 0;
}

.upload-input {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 0 20px;
  border-radius: 22px;
  background: rgba(250, 247, 255, 0.96);
  color: var(--text-muted);
  text-align: left;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    transform 0.2s ease,
    color 0.2s ease;
}

.input-icon {
  display: inline-grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: rgba(143, 108, 255, 0.08);
  color: var(--accent-strong);
  font-size: 13px;
  line-height: 1;
  flex-shrink: 0;
}

.input-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 16px;
  font-weight: 500;
}

.upload-input.has-file {
  color: #42435d;
}

.upload-submit {
  border-radius: 22px;
  background: linear-gradient(135deg, #9a7aff, #7a54ff 68%, #ff8fbc 130%);
  color: #fff;
  font-size: 24px;
  font-weight: 800;
  box-shadow: 0 10px 20px rgba(122, 84, 255, 0.18);
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    filter 0.2s ease;
}

.upload-submit:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.upload-submit:hover,
.upload-input:hover {
  transform: translateY(-1px);
}

.upload-submit:hover {
  box-shadow: 0 14px 24px rgba(122, 84, 255, 0.22);
  filter: saturate(1.04);
}

.upload-input:focus-visible,
.upload-submit:focus-visible,
.inline-upload:focus-visible {
  outline: 2px solid rgba(122, 84, 255, 0.26);
  outline-offset: 2px;
}

.hero-sub-action {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 15px;
  position: relative;
  z-index: 1;
  margin-top: -2px;
}

.inline-upload {
  border: 0;
  background: transparent;
  color: var(--accent-strong);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  width: min(920px, 100%);
  margin: 12px auto 0;
  position: relative;
  z-index: 1;
  animation: rise-in 0.9s ease both;
}

.feature-card {
  display: grid;
  justify-items: center;
  gap: 10px;
  grid-template-rows: auto auto 1fr;
  min-height: 150px;
  padding: 22px 18px 18px;
  text-align: center;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(255, 255, 255, 0.82);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.88),
    0 12px 24px rgba(60, 48, 108, 0.028);
  backdrop-filter: blur(10px);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    background-color 0.2s ease,
    border-color 0.2s ease;
}

.feature-card:hover {
  transform: translateY(-3px);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.94),
    0 18px 32px rgba(60, 48, 108, 0.045);
  background: rgba(255, 255, 255, 0.84);
  border-color: rgba(143, 108, 255, 0.16);
}

.feature-icon {
  display: inline-grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: linear-gradient(180deg, #f4ebff, #fef6ff);
  font-size: 19px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 8px 18px rgba(122, 84, 255, 0.08);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.feature-card:hover .feature-icon {
  transform: translateY(-1px) scale(1.04);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 10px 20px rgba(122, 84, 255, 0.12);
}

.feature-icon[data-tone='blue'] {
  background: linear-gradient(180deg, #ebe9ff, #dfe4ff);
}

.feature-icon[data-tone='pink'] {
  background: linear-gradient(180deg, #ffe0f4, #f6ddff);
}

.feature-icon[data-tone='green'] {
  background: linear-gradient(180deg, #e5f8ee, #d9f5e4);
}

.feature-icon[data-tone='peach'] {
  background: linear-gradient(180deg, #ffe9df, #ffe1f2);
}

.feature-card h2,
.feature-card p {
  margin: 0;
}

.feature-card h2 {
  font-size: 17px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.feature-card p {
  color: var(--text-muted);
  font-size: 14px;
  line-height: 1.45;
  max-width: 14ch;
}

.error-box {
  width: min(1120px, 100%);
  margin: 0 auto;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(210, 72, 72, 0.1);
  color: #a13232;
}

.hidden-input {
  display: none;
}

@keyframes rise-in {
  from {
    opacity: 0;
    transform: translateY(14px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 980px) {
  .feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .home-landing {
    gap: 20px;
    padding-top: 92px;
    min-height: auto;
  }

  .hero-title {
    font-size: clamp(42px, 13vw, 64px);
  }

  .hero-highlight {
    font-size: 26px;
  }

  .upload-shell {
    grid-template-columns: 1fr;
    padding: 8px;
    border-radius: 28px;
  }

  .upload-input,
  .upload-submit {
    min-height: 58px;
  }

  .input-text {
    font-size: 14px;
  }

  .upload-submit {
    font-size: 20px;
  }

  .hero-sub-action {
    flex-direction: column;
    gap: 8px;
  }

  .feature-grid {
    grid-template-columns: 1fr;
    width: min(420px, 100%);
  }

  .feature-card {
    min-height: 142px;
    padding: 18px 16px;
  }

  .hero-title {
    gap: 10px;
  }
}
</style>
