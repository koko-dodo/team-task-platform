<script setup lang="ts">
import AuthTitle from './components/auth-title.vue'
import LoginForm from './components/login-form.vue'
</script>

<template>
  <div class="flex items-center justify-center min-h-screen p-4 min-w-screen">
    <main class="flex flex-col gap-6 w-full max-w-md md:max-w-lg lg:max-w-xl">
      <AuthTitle />
      <LoginForm />
    </main>
  </div>
</template>
