<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'

import AppHeader from '../components/AppHeader.vue'

type NavItem = {
  label: string
  to?: RouteLocationRaw
  hot?: boolean
  kind?: 'ghost' | 'primary' | 'workspace' | 'avatar' | 'single'
  icon?: string
}

defineProps<{
  title: string
  description: string
  eyebrow?: string
  navItems?: NavItem[]
  centerNavItems?: NavItem[]
  localeLabel?: string
  compact?: boolean
  hideHero?: boolean
  wide?: boolean
}>()
</script>

<template>
  <div class="page-shell" :class="{ compact, wide }">
    <AppHeader
      :nav-items="navItems"
      :center-nav-items="centerNavItems"
      :locale-label="localeLabel"
    />

    <section v-if="!hideHero" class="hero">
      <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
      <h1 class="page-title">{{ title }}</h1>
      <p class="page-desc">{{ description }}</p>
    </section>

    <main class="layout-main">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.layout-main,
.hero {
  width: min(var(--page-max-width), calc(100vw - (var(--page-gutter) * 2)));
  margin: 0 auto;
}

.layout-main {
  padding-top: 22px;
}

.hero {
  display: grid;
  gap: 14px;
  padding: 28px 0 12px;
}

.compact .hero {
  padding-bottom: 20px;
}

@media (max-width: 720px) {
  .layout-main {
    padding-top: 20px;
  }
}
</style>
