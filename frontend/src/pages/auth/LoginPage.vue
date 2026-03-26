<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import '../../styles/auth-pages.css'

import AppLayout from '../../layouts/AppLayout.vue'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const route = useRoute()
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
        : '/'

    await router.push(redirect)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试'
  }
}
</script>

<template>
  <AppLayout
    title="进入你的学习工作台"
    description="登录后继续上传视频、查看处理进度与整理学习结果。"
    :center-nav-items="centerNavItems"
    locale-label="简体中文"
    :nav-items="navItems"
    :hide-hero="true"
    compact
  >
    <section class="auth-page auth-page--login">
      <section class="auth-card panel">
        <div class="auth-card-head">
          <h1>登录</h1>
          <p class="auth-switch auth-switch--top">
            还没有账户？
            <RouterLink to="/register">注册</RouterLink>
          </p>
        </div>

        <form class="auth-form" @submit.prevent="handleSubmit">
          <p v-if="errorMessage" class="auth-feedback auth-feedback--error">{{ errorMessage }}</p>

          <label class="auth-field">
            <span class="auth-field-label">账号</span>
            <input v-model="form.userAccount" autocomplete="username" placeholder="邮箱或手机号" />
          </label>

          <label class="auth-field">
            <span class="auth-field-label">密码</span>
            <input
              v-model="form.userPassword"
              type="password"
              autocomplete="current-password"
              placeholder="请输入密码"
            />
          </label>

          <button class="auth-submit" :disabled="submitDisabled" type="submit">
            {{ authStore.pending ? '登录中...' : '登录' }}
          </button>
        </form>

      </section>
    </section>
  </AppLayout>
</template>
