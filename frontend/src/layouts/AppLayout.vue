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
  <div class="page-shell" :class="{ compact, wide }">
    <header class="layout-header">
      <RouterLink class="brand-mark" to="/">
        <span class="brand-badge">ZK</span>
        <span class="brand-copy">
          <strong>智看视频</strong>
          <small>AI Video Learning</small>
        </span>
      </RouterLink>

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

    <main class="layout-main">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.layout-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  width: 100%;
  margin: 0;
  padding: 16px var(--page-gutter);
  border-radius: 0 0 20px 20px;
  background: rgba(255, 255, 255, 0.56);
  border: 1px solid rgba(255, 255, 255, 0.42);
  box-shadow: 0 5px 14px rgba(34, 35, 66, 0.015);
  backdrop-filter: blur(10px);
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: linear-gradient(145deg, var(--accent), var(--accent-strong));
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.08em;
  box-shadow: 0 12px 24px rgba(111, 88, 255, 0.18);
}

.brand-copy {
  display: grid;
  gap: 2px;
}

.brand-copy strong {
  font-size: 23px;
  font-weight: 800;
  letter-spacing: -0.05em;
}

.brand-copy small {
  color: var(--text-soft);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.layout-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.nav-link {
  font-size: 16px;
  font-weight: 600;
}

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
  .layout-header {
    padding: 14px 16px;
  }

  .layout-nav {
    gap: 8px;
  }

  .brand-copy small {
    display: none;
  }

  .layout-main {
    padding-top: 20px;
  }
}
</style>
