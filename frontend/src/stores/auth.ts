import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { getLoginUser, loginUser, logoutUser, registerUser } from '../api/user'
import type { UserVO } from '../api/types'

export type CurrentUser = UserVO | null

export const useAuthStore = defineStore('auth', () => {
  const currentUser = ref<CurrentUser>(null)
  const initialized = ref(false)
  const pending = ref(false)

  const isLoggedIn = computed(() => currentUser.value !== null)

  function setCurrentUser(user: CurrentUser) {
    currentUser.value = user
  }

  function markInitialized() {
    initialized.value = true
  }

  async function restoreSession() {
    if (initialized.value) {
      return currentUser.value
    }

    try {
      currentUser.value = await getLoginUser()
    } catch {
      currentUser.value = null
    } finally {
      initialized.value = true
    }

    return currentUser.value
  }

  async function login(userAccount: string, userPassword: string) {
    pending.value = true
    try {
      const user = await loginUser({ userAccount, userPassword })
      currentUser.value = user
      initialized.value = true
      return user
    } finally {
      pending.value = false
    }
  }

  async function register(
    userAccount: string,
    userPassword: string,
    checkPassword: string,
    userName: string,
  ) {
    pending.value = true
    try {
      return await registerUser({
        userAccount,
        userPassword,
        checkPassword,
        userName,
      })
    } finally {
      pending.value = false
    }
  }

  async function logout() {
    pending.value = true
    try {
      await logoutUser()
      currentUser.value = null
      initialized.value = true
    } finally {
      pending.value = false
    }
  }

  return {
    currentUser,
    initialized,
    pending,
    isLoggedIn,
    setCurrentUser,
    markInitialized,
    restoreSession,
    login,
    register,
    logout,
  }
})
