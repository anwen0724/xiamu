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
    <div class="panel-head">
      <div class="copy">
        <p class="panel-kicker">Upload Workspace</p>
        <h2>{{ title }}</h2>
        <p>{{ description }}</p>
      </div>

      <div class="status-pill">
        <strong>真实链路</strong>
        <span>上传后跳转到 /video/:id</span>
      </div>
    </div>

    <div class="dropzone" @click="pickFile">
      <div class="dropzone-meta">
        <span class="dropzone-chip">本地 MP4</span>
        <span class="dropzone-chip muted">状态页持续展示处理进度</span>
      </div>
      <span class="dropzone-label">选择一个学习视频开始处理</span>
      <span class="dropzone-tip">
        适用于课程录屏、教程讲解、知识分享。上传完成后自动进入详情页查看摘要、章节和笔记入口。
      </span>
      <span v-if="selectedFileName" class="file-name">{{ selectedFileName }}</span>
      <button class="select-btn" type="button" :disabled="pending">
        {{ buttonLabel }}
      </button>
    </div>

    <div class="workflow-grid">
      <div class="workflow-card">
        <span>01</span>
        <strong>上传视频</strong>
        <p>只保留当前已支持的本地上传流程。</p>
      </div>
      <div class="workflow-card">
        <span>02</span>
        <strong>等待处理</strong>
        <p>统一详情页展示状态、进度和异常信息。</p>
      </div>
      <div class="workflow-card">
        <span>03</span>
        <strong>阅读结果</strong>
        <p>处理完成后查看摘要、章节和转写内容。</p>
      </div>
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
  padding: 24px;
  border: 1px solid var(--line);
  border-radius: 28px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 249, 247, 0.96)),
    radial-gradient(circle at top right, rgba(47, 127, 109, 0.08), transparent 32%);
  box-shadow: 0 24px 54px rgba(17, 24, 21, 0.08);
}

.upload-panel.landing {
  min-height: 100%;
  align-content: start;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.copy {
  display: grid;
  gap: 8px;
  max-width: 440px;
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
  font-size: clamp(28px, 3vw, 34px);
  line-height: 1.12;
  letter-spacing: -0.03em;
}

.copy p {
  color: var(--text-muted);
  line-height: 1.7;
}

.status-pill {
  min-width: 156px;
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 18px;
  border: 1px solid rgba(47, 127, 109, 0.14);
  background: var(--accent-wash);
}

.status-pill strong,
.status-pill span {
  margin: 0;
}

.status-pill strong {
  font-size: 13px;
}

.status-pill span {
  color: var(--text-muted);
  font-size: 12px;
}

.dropzone {
  min-height: 260px;
  display: grid;
  place-items: center;
  gap: 14px;
  padding: 28px;
  border-radius: 24px;
  border: 1px dashed rgba(47, 127, 109, 0.32);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(243, 248, 246, 0.98)),
    radial-gradient(circle at top, rgba(47, 127, 109, 0.08), transparent 46%);
  text-align: center;
  cursor: pointer;
  transition:
    transform 0.22s ease,
    border-color 0.22s ease,
    box-shadow 0.22s ease;
}

.dropzone:hover {
  transform: translateY(-2px);
  border-color: rgba(47, 127, 109, 0.48);
  box-shadow: 0 18px 34px rgba(47, 127, 109, 0.1);
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
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(47, 127, 109, 0.14);
  color: var(--accent-strong);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.dropzone-chip.muted {
  color: var(--text-muted);
}

.dropzone-label {
  max-width: 12ch;
  font-size: clamp(26px, 4vw, 34px);
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.04em;
}

.dropzone-tip {
  max-width: 46ch;
  color: var(--text-muted);
  line-height: 1.7;
}

.file-name {
  max-width: 100%;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  color: var(--accent-strong);
  border: 1px solid rgba(47, 127, 109, 0.14);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.select-btn {
  margin-top: 6px;
  min-height: 48px;
  padding: 0 24px;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--accent), var(--accent-strong));
  color: #fff;
  box-shadow: 0 16px 28px rgba(47, 127, 109, 0.22);
  cursor: pointer;
}

.select-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.workflow-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.workflow-card {
  display: grid;
  gap: 6px;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.76);
}

.workflow-card span,
.workflow-card strong,
.workflow-card p {
  margin: 0;
}

.workflow-card span {
  color: var(--accent-strong);
  font-size: 12px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.workflow-card strong {
  font-size: 15px;
}

.workflow-card p {
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
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

@media (max-width: 720px) {
  .panel-head,
  .workflow-grid {
    grid-template-columns: 1fr;
  }

  .panel-head {
    display: grid;
  }

  .status-pill {
    min-width: 0;
  }
}
</style>
