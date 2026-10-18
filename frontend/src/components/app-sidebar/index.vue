<script lang="ts" setup>
import { computed } from 'vue'

import { useAuthStore } from '@/stores/auth'

import { sidebarData } from './data/sidebar-data'
import NavFooter from './nav-footer.vue'
import NavTeam from './nav-team.vue'

const authStore = useAuthStore()

const footerUser = computed(() => {
  if (authStore.isLogin && authStore.username) {
    return {
      ...sidebarData.user,
      name: authStore.username,
      email: '当前登录用户',
      avatar: '',
    }
  }
  return sidebarData.user
})
</script>

<template>
  <UiSidebar collapsible="icon" class="z-50">
    <UiSidebarHeader>
      <div class="flex items-center gap-2">
        <span class="text-lg font-bold">任务管理系统</span>
      </div>
    </UiSidebarHeader>

    <UiSidebarContent>
      <NavTeam :nav-main="sidebarData.navMain" />
    </UiSidebarContent>

    <UiSidebarFooter>
      <NavFooter :user="footerUser" />
    </UiSidebarFooter>

    <UiSidebarRail />
  </UiSidebar>
</template>
