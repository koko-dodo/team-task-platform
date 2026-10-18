import type { AxiosError } from 'axios'

import axios from 'axios'

import type { ApiErrorBody } from '@/api/types'
import pinia from '@/plugins/pinia/setup'
import env from '@/utils/env'

import { useAuthStore } from '@/stores/auth'

export function useAxios() {
  const axiosInstance = axios.create({
    baseURL: env.VITE_SERVER_API_URL + env.VITE_SERVER_API_PREFIX,
    timeout: env.VITE_SERVER_API_TIMEOUT,
  })

  axiosInstance.interceptors.request.use((config) => {
    const auth = useAuthStore(pinia)
    if (auth.accessToken)
      config.headers.Authorization = `Bearer ${auth.accessToken}`

    return config
  }, error => Promise.reject(error))

  axiosInstance.interceptors.response.use(
    response => response,
    (error: AxiosError<ApiErrorBody>) => Promise.reject(error),
  )

  return { axiosInstance }
}
