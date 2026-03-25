<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'

import AppLayout from '../../layouts/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  userAccount: '',
  userName: '',
  userPassword: '',
  checkPassword: '',
})

const errorMessage = ref('')
const successMessage = ref('')

const submitDisabled = computed(
  () =>
    authStore.pending ||
    !form.userAccount.trim() ||
    !form.userName.trim() ||
    !form.userPassword.trim() ||
    !form.checkPassword.trim(),
)

async function handleSubmit() {
  errorMessage.value = ''
  successMessage.value = ''

  try {
    await authStore.register(
      form.userAccount.trim(),
      form.userPassword,
      form.checkPassword,
      form.userName.trim(),
    )

    successMessage.value = '注册成功，正在跳转登录页...'
    window.setTimeout(() => {
      void router.push('/login')
    }, 700)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '注册失败，请稍后重试'
  }
}
</script>

<template>
  <AppLayout
    eyebrow="用户注册"
    title="创建一个新的学习账号"
    description="首版只保留账号、用户名和密码字段，严格对齐当前后端注册接口。"
    :compact="true"
  >
    <section class="panel auth-card">
      <form class="auth-form" @submit.prevent="handleSubmit">
        <p v-if="errorMessage" class="error-box">{{ errorMessage }}</p>
        <p v-if="successMessage" class="success-box">{{ successMessage }}</p>

      <div class="field">
        <label>账号</label>
          <input v-model="form.userAccount" placeholder="邮箱或手机号" />
      </div>
      <div class="field">
        <label>用户名</label>
          <input v-model="form.userName" placeholder="用于显示的名称" />
      </div>
      <div class="field">
        <label>密码</label>
          <input v-model="form.userPassword" type="password" placeholder="请输入密码" />
      </div>
      <div class="field">
        <label>确认密码</label>
          <input
            v-model="form.checkPassword"
            type="password"
            placeholder="再次输入密码"
          />
      </div>
        <button :disabled="submitDisabled" type="submit">
          {{ authStore.pending ? '注册中...' : '注册' }}
        </button>
      </form>

      <p class="switch-tip">
        已有账号？
        <RouterLink to="/login">去登录</RouterLink>
      </p>
    </section>
  </AppLayout>
</template>

<style scoped>
.auth-card {
  width: min(520px, 100%);
  padding: 24px;
}

.auth-form {
  display: grid;
  gap: 4px;
}

.field {
  display: grid;
  gap: 8px;
  margin-bottom: 16px;
}

input,
button {
  min-height: 46px;
  border-radius: 14px;
  border: 1px solid var(--line);
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.9);
}

button {
  background: var(--accent);
  color: #fff;
  border: 0;
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.error-box,
.success-box {
  margin: 0 0 8px;
  padding: 12px 14px;
  border-radius: 14px;
}

.error-box {
  background: rgba(208, 57, 57, 0.08);
  color: #a02c2c;
}

.success-box {
  background: rgba(48, 136, 84, 0.1);
  color: #2e7d51;
}

.switch-tip {
  margin: 18px 0 0;
  color: var(--text-muted);
}

.switch-tip a {
  color: var(--accent-strong);
}
</style>
