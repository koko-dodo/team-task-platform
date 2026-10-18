import { clearDeletedTaskCaches } from '@/utils/deleted-task-cache'
import { defineStore } from 'pinia'

import type { LoginResponseBody } from '@/api/types'

/**
 * 登录态：与后端 JWT Bearer 认证配合使用。
 * 使用 sessionStorage 持久化（见 pinia 全局 persistedstate 配置）。
 */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const userId = ref<number | null>(null)
  const username = ref<string | null>(null)

  const isLogin = computed(() => !!accessToken.value && userId.value != null)

  function setSession(payload: LoginResponseBody) {
    if (userId.value !== payload.user.id)
      clearDeletedTaskCaches()
    accessToken.value = payload.accessToken
    refreshToken.value = payload.refreshToken
    userId.value = payload.user.id
    username.value = payload.user.username
  }

  function clearSession() {
    clearDeletedTaskCaches()
    accessToken.value = null
    refreshToken.value = null
    userId.value = null
    username.value = null
  }

  return {
    accessToken,
    refreshToken,
    userId,
    username,
    isLogin,
    setSession,
    clearSession,
  }
}, {
  persist: true,
})
