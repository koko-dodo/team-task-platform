<script lang="ts" setup>
import { useAuth } from '@/composables/use-auth'

const { login, loading } = useAuth()

const username = ref('')
const password = ref('')

async function onSubmit() {
  await login(username.value.trim(), password.value)
}
</script>

<template>
  <UiCard class="w-full max-w-sm">
    <form @submit.prevent="onSubmit">
      <UiCardHeader>
        <UiCardTitle class="text-2xl">
          登录
        </UiCardTitle>
        <UiCardDescription>
          使用用户名与密码登录。
          没有账号？
          <UiButton
            type="button"
            variant="link" class="px-0 text-muted-foreground font-semibold"
            @click="$router.push('/auth/sign-up')"
          >
            注册
          </UiButton>
        </UiCardDescription>
      </UiCardHeader>
      <UiCardContent class="grid gap-4">
        <div class="grid gap-2">
          <UiLabel for="username">
            用户名
          </UiLabel>
          <UiInput
            id="username"
            v-model="username"
            type="text"
            autocomplete="username"
            placeholder="只能包含字母、数字、下划线，长度为4-20位"
            required
          />
        </div>
        <div class="grid gap-2">
          <div class="flex items-center justify-between">
            <UiLabel for="password">
              密码
            </UiLabel>
          </div>
          <UiInput
            id="password"
            v-model="password"
            type="password"
            autocomplete="current-password"
            required
            placeholder="至少 6 位，需含字母与数字"
          />
        </div>

        <UiButton type="submit" class="w-full" :disabled="loading">
          <UiSpinner v-if="loading" class="mr-2" />
          登录
        </UiButton>

        
      </UiCardContent>
    </form>
  </UiCard>
</template>
