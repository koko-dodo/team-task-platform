<script setup lang="ts">
import type { AxiosError } from 'axios'

import { toast } from 'vue-sonner'

import type { ApiErrorBody, ApiResponse } from '@/api/types'

import { useAxios } from '@/composables/use-axios'

import AuthTitle from './components/auth-title.vue'

const router = useRouter()
const { axiosInstance } = useAxios()

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)

async function onSubmit() {
  if (password.value !== confirmPassword.value) {
    toast.error('两次密码不一致')
    return
  }
  loading.value = true
  try {
    const res = await axiosInstance.post<ApiResponse<null>>('/auth/register', {
      username: username.value.trim(),
      password: password.value,
      confirmPassword: confirmPassword.value,
    })
    if (res.data.code !== 200) {
      toast.error(res.data.message || '注册失败')
      return
    }
    toast.success(res.data.message || '注册成功，请登录')
    await router.push('/auth/sign-in')
  }
  catch (e) {
    const err = e as AxiosError<ApiErrorBody>
    toast.error(err.response?.data?.message || err.message || '注册失败')
  }
  finally {
    loading.value = false
  }
}
</script>

<template>
   <div class="flex items-center justify-center min-h-screen p-4 min-w-screen">
      <main class="flex flex-col gap-6 w-full max-w-md md:max-w-lg lg:max-w-xl">
      <AuthTitle />
      <UiCard class="w-full">
        <form @submit.prevent="onSubmit">
          <UiCardHeader>
            <UiCardTitle class="text-2xl">
              注册
            </UiCardTitle>
            <UiCardDescription>
              创建账号后即可登录并使用个人任务功能。
              已有账号？点击
              <UiButton
                type="button"
                variant="link" class="px-0 text-muted-foreground font-semibold"
                @click="$router.push('/auth/sign-in')"
              >
                登录
              </UiButton>
            </UiCardDescription>
          </UiCardHeader>
          <UiCardContent class="grid gap-4">
            <div class="grid gap-2">
              <UiLabel for="reg-username">用户名</UiLabel>
              <UiInput
                id="reg-username"
                v-model="username"
                required
                autocomplete="username"
                placeholder="请输入用户名（4-20位字母、数字、下划线）"
              />
            </div>
            <div class="grid gap-2">
              <UiLabel for="reg-password">密码</UiLabel>
              <UiInput
                id="reg-password"
                v-model="password"
                type="password"
                required
                autocomplete="new-password"
                placeholder="密码长度至少6位，必须包含字母和数字"
              />
            </div>
            <div class="grid gap-2">
              <UiLabel for="reg-confirm">确认密码</UiLabel>
              <UiInput
                id="reg-confirm"
                v-model="confirmPassword"
                type="password"
                required
                autocomplete="new-password"
              />
            </div>
            <UiButton type="submit" class="w-full" :disabled="loading">
              <UiSpinner v-if="loading" class="mr-2" />
              注册
            </UiButton>
          </UiCardContent>
        </form>
      </UiCard>
    </main>
  </div>
</template>
