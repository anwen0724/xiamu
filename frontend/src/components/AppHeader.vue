<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRouter, type RouteLocationRaw } from 'vue-router'

import { useAuthStore } from '../stores/auth'

type NavItem = {
  label: string
  to?: RouteLocationRaw
  hot?: boolean
  kind?: 'ghost' | 'primary' | 'workspace' | 'avatar' | 'single'
  icon?: string
}

defineProps<{
  navItems?: NavItem[]
  centerNavItems?: NavItem[]
  localeLabel?: string
}>()

const router = useRouter()
const authStore = useAuthStore()

const accountMenuItems = computed(() => [
  { label: '工作台', to: '/dashboard', icon: '▣' },
  { label: '设置', to: { name: 'placeholder', query: { label: '设置' } }, icon: '⚙' },
  { label: '订阅管理', to: { name: 'placeholder', query: { label: '订阅管理' } }, icon: '▤' },
  { label: '积分兑换', to: { name: 'placeholder', query: { label: '积分兑换' } }, icon: '🎁' },
])

const accountDisplayName = computed(
  () => authStore.currentUser?.userName || authStore.currentUser?.userAccount || '用户',
)

const accountDisplaySubline = computed(() => authStore.currentUser?.userAccount || '未绑定账号')

async function handleLogout() {
  await authStore.logout()
  await router.push('/')
}
</script>

<template>
  <header class="layout-header">
    <div class="layout-header-inner">
      <div class="layout-brand">
        <RouterLink class="brand-mark" to="/">
          <span class="brand-badge">ZK</span>
          <span class="brand-copy">
            <strong>智看视频</strong>
            <small>AI Video Learning</small>
          </span>
        </RouterLink>
      </div>

      <nav v-if="centerNavItems?.length" class="layout-center-nav" aria-label="主导航">
        <RouterLink
          v-for="(item, index) in centerNavItems"
          :key="`${item.label}-${index}`"
          class="center-link"
          :class="{
            'center-link--hot': item.hot,
          }"
          :to="item.to ?? '/'"
        >
          <span v-if="item.hot" class="center-link-fire">🔥</span>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="layout-actions">
        <button v-if="localeLabel" class="locale-pill" type="button">{{ localeLabel }}</button>

        <nav v-if="navItems?.length" class="layout-nav" aria-label="账户导航">
          <template v-for="(item, index) in navItems" :key="`${item.label}-${index}`">
            <div v-if="item.kind === 'avatar'" class="account-menu">
              <button class="nav-link nav-link--avatar" type="button">
                {{ item.label }}
              </button>

              <div class="account-dropdown">
                <div class="account-summary">
                  <strong>{{ accountDisplayName }}</strong>
                  <span>{{ accountDisplaySubline }}</span>
                </div>

                <div class="account-menu-list">
                  <RouterLink
                    v-for="menuItem in accountMenuItems"
                    :key="menuItem.label"
                    class="account-menu-link"
                    :to="menuItem.to"
                  >
                    <span class="account-menu-icon">{{ menuItem.icon }}</span>
                    <span>{{ menuItem.label }}</span>
                  </RouterLink>
                </div>

                <button class="account-logout" type="button" @click="handleLogout">
                  <span class="account-menu-icon">⎋</span>
                  <span>退出登录</span>
                </button>
              </div>
            </div>

            <RouterLink
              v-else
              class="nav-link"
              :class="{
                'nav-link--ghost': item.kind === 'ghost' || item.label === '登录',
                'nav-link--primary': item.kind === 'primary' || item.label === '注册',
                'nav-link--workspace': item.kind === 'workspace',
                'nav-link--single':
                  item.kind === 'single' || (navItems.length === 1 && index === 0),
              }"
              :to="item.to ?? '/'"
            >
              <span v-if="item.icon" class="nav-link-icon">{{ item.icon }}</span>
              {{ item.label }}
            </RouterLink>
          </template>
        </nav>
      </div>
    </div>
  </header>
</template>

<style scoped>
.layout-header {
  position: sticky;
  top: 0;
  z-index: 20;
  width: 100%;
  margin: 0;
  padding: 14px 0;
  border-radius: 0;
  background: rgba(255, 255, 255, 0.76);
  border-bottom: 1px solid rgba(143, 108, 255, 0.08);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.82),
    0 10px 24px rgba(50, 38, 98, 0.025);
  backdrop-filter: blur(12px);
}

.layout-header-inner {
  width: min(1360px, calc(100vw - (var(--page-gutter) * 2)));
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 14px;
}

.layout-brand,
.layout-actions {
  display: flex;
  align-items: center;
}

.layout-brand {
  justify-self: start;
  padding-left: 12px;
}

.layout-center-nav {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  justify-self: center;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  transition: transform 0.2s ease;
}

.brand-mark:hover {
  transform: translateY(-1px);
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 15px;
  background: linear-gradient(145deg, #9c81ff, var(--accent-strong) 72%, #ff9ac1 130%);
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  box-shadow: 0 10px 22px rgba(117, 77, 255, 0.14);
}

.brand-copy {
  display: grid;
  gap: 2px;
}

.brand-copy strong {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: #222538;
}

.brand-copy small {
  color: var(--text-soft);
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.center-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 38px;
  padding: 0 13px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #61657c;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    transform 0.2s ease;
}

.center-link:hover {
  background: rgba(255, 255, 255, 0.58);
  color: #303348;
  transform: translateY(-1px);
}

.center-link--hot {
  color: #4c5168;
}

.center-link-fire {
  font-size: 14px;
  line-height: 1;
}

.layout-actions {
  gap: 10px;
  justify-content: flex-end;
  justify-self: end;
}

.locale-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  padding: 0 14px;
  border: 1px solid rgba(143, 108, 255, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.64);
  color: #666b82;
  font-size: 13px;
  font-weight: 600;
}

.layout-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.account-menu {
  position: relative;
}

.account-menu::after {
  content: '';
  position: absolute;
  top: 100%;
  right: 0;
  width: 100%;
  height: 14px;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  font-size: 14px;
  font-weight: 600;
  color: #72758b;
}

.nav-link-icon {
  font-size: 14px;
  line-height: 1;
}

.nav-link--ghost {
  border-color: rgba(143, 108, 255, 0.08);
  background: rgba(255, 255, 255, 0.6);
}

.nav-link--ghost:hover {
  border-color: rgba(143, 108, 255, 0.14);
  background: rgba(255, 255, 255, 0.86);
}

.nav-link--primary {
  color: #ffffff;
  background: linear-gradient(135deg, #9a7aff, #7a54ff 72%, #ff8fbc 130%);
  box-shadow: 0 10px 22px rgba(122, 84, 255, 0.18);
}

.nav-link--primary:hover {
  color: #ffffff;
  border-color: transparent;
  background: linear-gradient(135deg, #a286ff, #7a54ff 72%, #ff93bf 130%);
  box-shadow: 0 12px 24px rgba(122, 84, 255, 0.22);
}

.nav-link--single {
  border-color: rgba(143, 108, 255, 0.08);
  background: rgba(255, 255, 255, 0.72);
}

.nav-link--workspace {
  border-color: rgba(143, 108, 255, 0.1);
  background: rgba(255, 255, 255, 0.74);
  color: #4a4f67;
  min-height: 38px;
  padding: 0 15px;
  font-size: 14px;
}

.nav-link--workspace:hover {
  border-color: rgba(143, 108, 255, 0.16);
  background: rgba(255, 255, 255, 0.92);
}

.nav-link--avatar {
  width: 42px;
  min-width: 42px;
  height: 42px;
  padding: 0;
  border-radius: 999px;
  font-size: 15px;
  color: #ffffff;
  background: linear-gradient(135deg, #9a7aff, #7a54ff 72%, #ff8fbc 130%);
  box-shadow: 0 10px 22px rgba(122, 84, 255, 0.18);
}

.nav-link--avatar:hover {
  color: #ffffff;
  border-color: transparent;
  background: linear-gradient(135deg, #a286ff, #7a54ff 72%, #ff93bf 130%);
  box-shadow: 0 12px 24px rgba(122, 84, 255, 0.22);
}

.account-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 256px;
  padding: 14px 0 10px;
  border-radius: 22px;
  border: 1px solid rgba(143, 108, 255, 0.12);
  background: rgba(255, 255, 255, 0.94);
  box-shadow:
    0 20px 40px rgba(50, 38, 98, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(16px);
  opacity: 0;
  visibility: hidden;
  transform: translateY(8px);
  transition:
    opacity 0.18s ease,
    transform 0.18s ease,
    visibility 0.18s ease;
  pointer-events: none;
  z-index: 30;
}

.account-menu:hover .account-dropdown,
.account-menu:focus-within .account-dropdown {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
  pointer-events: auto;
}

.account-summary {
  display: grid;
  gap: 4px;
  padding: 0 18px 14px;
  border-bottom: 1px solid rgba(143, 108, 255, 0.08);
}

.account-summary strong {
  font-size: 15px;
  font-weight: 800;
  color: #272b3d;
}

.account-summary span {
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.45;
}

.account-menu-list {
  display: grid;
  gap: 2px;
  padding: 10px 8px 8px;
}

.account-menu-link,
.account-logout {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 42px;
  padding: 0 12px;
  border-radius: 14px;
  color: #43485d;
  font-size: 15px;
  font-weight: 600;
  transition:
    background-color 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.account-menu-link:hover,
.account-logout:hover {
  background: rgba(143, 108, 255, 0.08);
  transform: translateX(1px);
}

.account-menu-icon {
  display: inline-grid;
  place-items: center;
  width: 20px;
  color: #5b6179;
  font-size: 15px;
  line-height: 1;
  flex-shrink: 0;
}

.account-logout {
  width: calc(100% - 16px);
  margin: 8px auto 0;
  border: 0;
  border-top: 1px solid rgba(143, 108, 255, 0.08);
  border-radius: 0 0 14px 14px;
  padding-top: 12px;
  justify-content: flex-start;
  background: transparent;
  color: #f04848;
  cursor: pointer;
}

.account-logout .account-menu-icon {
  color: inherit;
}

@media (max-width: 720px) {
  .layout-header {
    padding: 12px 0;
  }

  .layout-header-inner {
    width: calc(100vw - 32px);
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
  }

  .layout-brand {
    padding-left: 0;
  }

  .layout-brand,
  .layout-actions {
    justify-content: space-between;
  }

  .layout-center-nav {
    overflow-x: auto;
    padding-bottom: 4px;
    scrollbar-width: none;
    margin-left: 0;
    order: 3;
    width: 100%;
  }

  .layout-center-nav::-webkit-scrollbar {
    display: none;
  }

  .layout-nav {
    gap: 8px;
  }

  .account-dropdown {
    right: -8px;
    width: 240px;
  }

  .brand-copy small {
    display: none;
  }
}
</style>
