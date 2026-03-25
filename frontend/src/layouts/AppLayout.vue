<script setup lang="ts">
import { RouterLink } from 'vue-router'

type NavItem = {
  label: string
  to: string
}

defineProps<{
  title: string
  description: string
  eyebrow?: string
  navItems?: NavItem[]
  compact?: boolean
  hideHero?: boolean
  wide?: boolean
}>()
</script>

<template>
  <div class="page-shell">
    <header class="layout-header panel">
      <RouterLink class="brand-mark" to="/">智看视频</RouterLink>

      <nav v-if="navItems?.length" class="layout-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          class="nav-link"
          :to="item.to"
        >
          {{ item.label }}
        </RouterLink>
      </nav>
    </header>

    <section v-if="!hideHero" class="hero">
      <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
      <h1 class="page-title">{{ title }}</h1>
      <p class="page-desc">{{ description }}</p>
    </section>

    <main>
      <slot />
    </main>
  </div>
</template>

<style scoped>
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
}

.brand-mark {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.layout-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero {
  display: grid;
  gap: 14px;
  padding: 42px 0 30px;
}

.compact .hero {
  padding-bottom: 20px;
}


@media (max-width: 640px) {
  .layout-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
