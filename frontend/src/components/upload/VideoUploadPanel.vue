<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { uploadVideo } from '../../api/video'

const props = withDefaults(
  defineProps<{
    title: string
    description: string
    ctaLabel?: string
    requiresLogin?: boolean
    tone?: 'default' | 'landing'
  }>(),
  {
    tone: 'default',
  },
)

const router = useRouter()
const inputRef = ref<HTMLInputElement | null>(null)
const pending = ref(false)
const errorMessage = ref('')
const selectedFileName = ref('')

const buttonLabel = computed(() => {
  if (pending.value) {
    return '上传中...'
  }

  return props.ctaLabel || '选择视频'
})

const panelClassName = computed(() =>
  props.tone === 'landing' ? 'upload-panel landing' : 'upload-panel',
)

function pickFile() {
  if (props.requiresLogin) {
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
  <article :class="panelClassName">
    <div class="copy">
      <p class="panel-kicker">上传工作台</p>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
    </div>

    <div class="dropzone" @click="pickFile">
      <div class="dropzone-meta">
        <span class="dropzone-chip">MP4</span>
        <span class="dropzone-chip muted">详情页实时展示处理进度</span>
      </div>
      <span class="dropzone-label">拖拽 MP4 文件到此处</span>
      <span class="dropzone-tip">或点击选择本地视频文件</span>
      <span v-if="selectedFileName" class="file-name">{{ selectedFileName }}</span>
      <button class="select-btn" type="button" :disabled="pending">
        {{ buttonLabel }}
      </button>
    </div>

    <p v-if="errorMessage" class="error-box">{{ errorMessage }}</p>

    <input
      ref="inputRef"
      class="hidden-input"
      type="file"
      accept="video/mp4"
      @change="onFileChange"
    />
  </article>
</template>

<style scoped>
.upload-panel {
  display: grid;
  gap: 18px;
  padding: 26px;
  background:
    linear-gradient(180deg, rgba(255, 252, 246, 0.9), rgba(255, 255, 255, 0.78)),
    radial-gradient(circle at top left, rgba(31, 94, 255, 0.1), transparent 30%);
}

.upload-panel.landing {
  padding: 30px;
  min-height: 520px;
  align-content: start;
  background:
    linear-gradient(180deg, rgba(255, 252, 246, 0.96), rgba(255, 255, 255, 0.88)),
    radial-gradient(circle at top left, rgba(31, 94, 255, 0.14), transparent 34%);
}

.copy {
  display: grid;
  gap: 8px;
}

.panel-kicker {
  margin: 0;
  color: var(--accent-strong);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.copy h2,
.copy p {
  margin: 0;
}

.copy h2 {
  font-family: var(--font-display);
  font-size: clamp(28px, 3vw, 36px);
  line-height: 1.06;
  letter-spacing: -0.03em;
}

.copy p {
  color: var(--text-muted);
}

.dropzone {
  min-height: 260px;
  display: grid;
  place-items: center;
  gap: 10px;
  padding: 24px;
  border-radius: 28px;
  border: 1px dashed rgba(31, 94, 255, 0.34);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(241, 246, 255, 0.74)),
    repeating-linear-gradient(
      -45deg,
      rgba(31, 94, 255, 0.04),
      rgba(31, 94, 255, 0.04) 12px,
      transparent 12px,
      transparent 24px
    );
  text-align: center;
  cursor: pointer;
  transition:
    transform 0.22s ease,
    border-color 0.22s ease,
    box-shadow 0.22s ease;
}

.dropzone:hover {
  transform: translateY(-2px);
  border-color: rgba(31, 94, 255, 0.46);
  box-shadow: 0 20px 36px rgba(31, 94, 255, 0.08);
}

.dropzone-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.dropzone-chip {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(31, 94, 255, 0.14);
  color: var(--accent-strong);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.dropzone-chip.muted {
  color: var(--text-muted);
}

.dropzone-label {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.12;
}

.dropzone-tip {
  color: var(--text-muted);
}

.file-name {
  max-width: 100%;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  color: var(--accent-strong);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.select-btn {
  margin-top: 8px;
  min-height: 46px;
  padding: 0 20px;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #1e58ea, #275dff 58%, #4b79ff);
  color: #fff;
  box-shadow: 0 16px 28px rgba(31, 94, 255, 0.2);
  cursor: pointer;
}

.select-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.error-box {
  margin: 0;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(208, 57, 57, 0.08);
  color: #a02c2c;
}

.hidden-input {
  display: none;
}
</style>