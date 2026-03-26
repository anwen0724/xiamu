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

const navItems = computed(() => {
  if (authStore.isLoggedIn) {
    return [{ label: 'Dashboard', to: '/dashboard' }]
  }

  return [
    { label: '登录', to: '/login' },
    { label: '注册', to: '/register' },
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
          <button class="upload-input" type="button" @click="pickFile">
            <span class="input-icon">🔗</span>
            <span class="input-text">
              {{ selectedFileName || '选择本地 MP4 学习视频文件...' }}
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
  gap: 28px;
  padding: 152px 0 44px;
  min-height: calc(100vh - 128px);
  align-content: start;
  overflow: hidden;
}

.hero-copy {
  display: grid;
  gap: 10px;
  justify-items: center;
  text-align: center;
  position: relative;
  z-index: 1;
  margin-top: 10px;
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
  font-size: clamp(62px, 6.6vw, 96px);
  font-weight: 900;
  line-height: 1.02;
  letter-spacing: -0.08em;
  color: #12192d;
  text-align: center;
}

.hero-title-icon {
  font-size: 0.72em;
  line-height: 1;
}

.hero-highlight {
  font-size: clamp(34px, 3.3vw, 50px);
  font-weight: 800;
  color: var(--accent);
}

.hero-highlight span {
  font-size: 1.14em;
}

.hero-stage {
  position: relative;
  width: min(1320px, 100%);
  margin: 0 auto;
  display: grid;
  gap: 26px;
  padding: 4px 0 0;
  z-index: 1;
}

.upload-shell {
  width: min(1120px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 170px;
  gap: 12px;
  padding: 14px;
  border-radius: 36px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.62);
  box-shadow: 0 10px 24px rgba(41, 42, 77, 0.06);
  backdrop-filter: blur(6px);
  position: relative;
  z-index: 1;
}

.upload-input,
.upload-submit {
  min-height: 78px;
  border: 0;
}

.upload-input {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 0 24px;
  border-radius: 26px;
  background: rgba(247, 248, 252, 0.88);
  color: var(--text-muted);
  text-align: left;
  cursor: pointer;
}

.input-icon {
  font-size: 20px;
  opacity: 0.7;
}

.input-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 19px;
}

.upload-submit {
  border-radius: 26px;
  background: linear-gradient(135deg, #7f61ff, #6a49ff);
  color: #fff;
  font-size: 30px;
  font-weight: 800;
  box-shadow: 0 10px 22px rgba(111, 88, 255, 0.2);
  cursor: pointer;
}

.upload-submit:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.upload-submit:hover,
.upload-input:hover {
  transform: translateY(-1px);
}

.hero-sub-action {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-muted);
  font-size: 18px;
  position: relative;
  z-index: 1;
}

.inline-upload {
  border: 0;
  background: transparent;
  color: var(--accent-strong);
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  width: min(1220px, 100%);
  margin: 12px auto 0;
  position: relative;
  z-index: 1;
}

.feature-card {
  display: grid;
  justify-items: center;
  gap: 12px;
  min-height: 186px;
  padding: 28px 24px 24px;
  text-align: center;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(255, 255, 255, 0.66);
  box-shadow: 0 8px 18px rgba(38, 40, 70, 0.04);
  backdrop-filter: blur(4px);
}

.feature-icon {
  display: inline-grid;
  place-items: center;
  width: 54px;
  height: 54px;
  border-radius: 16px;
  background: linear-gradient(180deg, #f1ecff, #eaf3ff);
  font-size: 26px;
}

.feature-icon[data-tone='blue'] {
  background: linear-gradient(180deg, #dbe9ff, #c9d9ff);
}

.feature-icon[data-tone='pink'] {
  background: linear-gradient(180deg, #fbe0f9, #f3d6ff);
}

.feature-icon[data-tone='green'] {
  background: linear-gradient(180deg, #dff8e7, #cff3d8);
}

.feature-icon[data-tone='peach'] {
  background: linear-gradient(180deg, #ffe6dc, #ffd9f2);
}

.feature-card h2,
.feature-card p {
  margin: 0;
}

.feature-card h2 {
  font-size: 22px;
  font-weight: 800;
}

.feature-card p {
  color: var(--text-muted);
  font-size: 17px;
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

@media (max-width: 980px) {
  .feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .home-landing {
    gap: 20px;
    padding-top: 88px;
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
    padding: 10px;
    border-radius: 28px;
  }

  .upload-input,
  .upload-submit {
    min-height: 66px;
  }

  .input-text {
    font-size: 16px;
  }

  .upload-submit {
    font-size: 24px;
  }

  .hero-sub-action {
    flex-direction: column;
    gap: 8px;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }

  .feature-card {
    min-height: 168px;
    padding: 24px 20px;
  }

  .hero-title {
    gap: 10px;
  }
}
</style>
