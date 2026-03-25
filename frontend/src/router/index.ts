import { createRouter, createWebHistory } from 'vue-router'

import DashboardPage from '../pages/dashboard/DashboardPage.vue'
import HomePage from '../pages/home/HomePage.vue'
import LoginPage from '../pages/auth/LoginPage.vue'
import RegisterPage from '../pages/auth/RegisterPage.vue'
import VideoDetailPage from '../pages/video-detail/VideoDetailPage.vue'
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
      path: '/video/:id',
      name: 'video-detail',
      component: VideoDetailPage,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/video/status/:id',
      redirect: (to) => ({
        name: 'video-detail',
        params: { id: to.params.id },
      }),
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/video/result/:id',
      redirect: (to) => ({
        name: 'video-detail',
        params: { id: to.params.id },
      }),
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