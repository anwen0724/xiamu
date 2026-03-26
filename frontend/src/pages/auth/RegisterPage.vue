<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import '../../styles/auth-pages.css'

import AppLayout from '../../layouts/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const centerNavItems = [
  { label: '产品', to: { name: 'placeholder', query: { label: '产品' } } },
  { label: '热门精选总结', to: { name: 'placeholder', query: { label: '热门精选总结' } }, hot: true },
  { label: '安装插件', to: { name: 'placeholder', query: { label: '安装插件' } } },
  { label: '价格', to: { name: 'placeholder', query: { label: '价格' } } },
  { label: '常见问题', to: { name: 'placeholder', query: { label: '常见问题' } } },
  { label: '博客', to: { name: 'placeholder', query: { label: '博客' } } },
]

const navItems = [
  { label: '登录', to: '/login', kind: 'ghost' as const },
  { label: '注册', to: '/register', kind: 'primary' as const },
]

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
    title="创建一个新的学习账号"
    description="注册后即可上传视频、生成结果并回访你的学习记录。"
    :center-nav-items="centerNavItems"
    locale-label="简体中文"
    :nav-items="navItems"
    :hide-hero="true"
    compact
  >
    <section class="auth-page auth-page--register">
      <section class="auth-card panel">
        <div class="auth-card-head">
          <h1>注册</h1>
          <p class="auth-switch auth-switch--top">
            已有账户？
            <RouterLink to="/login">登录</RouterLink>
          </p>
        </div>

        <form class="auth-form" @submit.prevent="handleSubmit">
          <p v-if="errorMessage" class="auth-feedback auth-feedback--error">{{ errorMessage }}</p>
          <p v-if="successMessage" class="auth-feedback auth-feedback--success">{{ successMessage }}</p>

          <label class="auth-field">
            <span class="auth-field-label">账号</span>
            <input v-model="form.userAccount" autocomplete="username" placeholder="邮箱或手机号" />
          </label>

          <label class="auth-field">
            <span class="auth-field-label">用户名</span>
            <input v-model="form.userName" autocomplete="nickname" placeholder="用于显示的名称" />
          </label>

          <label class="auth-field">
            <span class="auth-field-label">密码</span>
            <input
              v-model="form.userPassword"
              type="password"
              autocomplete="new-password"
              placeholder="请输入密码"
            />
          </label>

          <label class="auth-field">
            <span class="auth-field-label">确认密码</span>
            <input
              v-model="form.checkPassword"
              type="password"
              autocomplete="new-password"
              placeholder="再次输入密码"
            />
          </label>

          <button class="auth-submit" :disabled="submitDisabled" type="submit">
            {{ authStore.pending ? '注册中...' : '注册' }}
          </button>
        </form>

      </section>
    </section>
  </AppLayout>
</template>
