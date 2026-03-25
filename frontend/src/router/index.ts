import { createRouter, createWebHistory } from 'vue-router'

import DashboardPage from '../pages/dashboard/DashboardPage.vue'
import HomePage from '../pages/home/HomePage.vue'
import LoginPage from '../pages/auth/LoginPage.vue'
import RegisterPage from '../pages/auth/RegisterPage.vue'
import VideoResultPage from '../pages/video-result/VideoResultPage.vue'
import VideoStatusPage from '../pages/video-status/VideoStatusPage.vue'
import { pinia } from '../stores'
import { useAuthStore } from '../stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    guestOnly?: boolean
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage,
    },
    {
      path: '/login',
      name: 'login',
      component: LoginPage,
      meta: {
        guestOnly: true,
      },
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterPage,
      meta: {
        guestOnly: true,
      },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: DashboardPage,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/video/status/:id',
      name: 'video-status',
      component: VideoStatusPage,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/video/result/:id',
      name: 'video-result',
      component: VideoResultPage,
      meta: {
        requiresAuth: true,
      },
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore(pinia)

  if (!authStore.initialized) {
    await authStore.restoreSession()
  }

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return {
      name: 'login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  if (to.meta.guestOnly && authStore.isLoggedIn) {
    return {
      name: 'dashboard',
    }
  }

  return true
})

export default router
