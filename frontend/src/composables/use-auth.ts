import type { AxiosError } from 'axios'

import { storeToRefs } from 'pinia'
import { toast } from 'vue-sonner'

import type { ApiErrorBody, ApiResponse, LoginResponseBody } from '@/api/types'

import { useAxios } from '@/composables/use-axios'
import { useAuthStore } from '@/stores/auth'

function messageFromAxios(e: AxiosError<ApiErrorBody>): string {
  const d = e.response?.data
  if (d?.message)
    return d.message
  return e.message || '请求失败'
}

export function useAuth() {
  const router = useRouter()
  const authStore = useAuthStore()
  const { isLogin } = storeToRefs(authStore)
  const loading = ref(false)

  function logout() {
    authStore.clearSession()
    router.push({ path: '/auth/sign-in' })
  }

  function toHome() {
    router.push({ path: '/tasks' })
  }

  /**
   * 调用后端 POST /auth/login（用户名即账号，非邮箱）
   */
  async function login(username: string, password: string) {
    if (!username || !password) {
      toast.error('请输入用户名和密码')
      return
    }
    loading.value = true
    try {
      const { axiosInstance } = useAxios()
      const res = await axiosInstance.post<ApiResponse<LoginResponseBody>>('/auth/login', {
        username,
        password,
      })
      const body = res.data
      if (body.code !== 200 || !body.data) {
        toast.error(body.message || '登录失败')
        return
      }
      authStore.setSession(body.data)
      toast.success(body.message || '登录成功')

      const redirect = router.currentRoute.value.query.redirect as string
      if (!redirect || redirect.startsWith('//'))
        toHome()
      else
        router.push(redirect)
    }
    catch (e) {
      toast.error(messageFromAxios(e as AxiosError<ApiErrorBody>))
    }
    finally {
      loading.value = false
    }
  }

  return {
    loading,
    logout,
    login,
    toHome,
    authStore,
  }
}
