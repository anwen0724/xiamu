<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'

import AppLayout from '../../layouts/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const form = reactive({
  userAccount: '',
  userPassword: '',
})

const errorMessage = ref('')

const submitDisabled = computed(
  () => authStore.pending || !form.userAccount.trim() || !form.userPassword.trim(),
)

async function handleSubmit() {
  errorMessage.value = ''

  try {
    await authStore.login(form.userAccount.trim(), form.userPassword)

    const redirect =
      typeof route.query.redirect === 'string' && route.query.redirect
        ? route.query.redirect
        : '/dashboard'

    await router.push(redirect)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试'
  }
}
</script>

<template>
  <AppLayout
    eyebrow="用户登录"
    title="进入你的学习工作台"
    description="输入账号和密码后，前端会调用后端登录接口并建立 HttpSession。"
    :compact="true"
  >
    <section class="panel auth-card">
      <form class="auth-form" @submit.prevent="handleSubmit">
        <p v-if="errorMessage" class="error-box">{{ errorMessage }}</p>

      <div class="field">
        <label>账号</label>
          <input v-model="form.userAccount" placeholder="邮箱或手机号" />
      </div>
      <div class="field">
        <label>密码</label>
          <input
            v-model="form.userPassword"
            type="password"
            placeholder="请输入密码"
          />
      </div>

        <button :disabled="submitDisabled" type="submit">
          {{ authStore.pending ? '登录中...' : '登录' }}
        </button>
      </form>

      <p class="switch-tip">
        还没有账号？
        <RouterLink to="/register">去注册</RouterLink>
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

.error-box {
  margin: 0 0 8px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(208, 57, 57, 0.08);
  color: #a02c2c;
}

.switch-tip {
  margin: 18px 0 0;
  color: var(--text-muted);
}

.switch-tip a {
  color: var(--accent-strong);
}
</style>
