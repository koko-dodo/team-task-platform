<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Plus } from 'lucide-vue-next'
import { useRouter } from 'vue-router'

import { BasicPage } from '@/components/global-layout'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'

import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { toast } from 'vue-sonner'
import { teamApi } from '@/api/team'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const queryClient = useQueryClient()
const isCreateOpen = ref(false)
const newTeamName = ref('')
const newTeamDesc = ref('')

// 获取用户相关的团队列表
const { data: teams, isLoading, error } = useQuery({
  queryKey: ['teams', authStore.userId],
  queryFn: () => teamApi.getUserTeams(),
  enabled: computed(() => authStore.isLogin && authStore.userId != null),
})
// 创建团队mutation
const createTeamMutation = useMutation({
  mutationFn: (data: { name: string; description?: string }) => teamApi.createTeam(data),
  onSuccess: () => {
    toast.success('团队创建成功')
    queryClient.invalidateQueries({ queryKey: ['teams'] })
    isCreateOpen.value = false
    newTeamName.value = ''
    newTeamDesc.value = ''
  },
  onError: (error: any) => {
    toast.error(error.response?.data?.message || '创建团队失败')
  }
})

function handleCreate() {
  if (!newTeamName.value.trim()) {
    toast.error('请输入团队名称')
    return
  }
  
  createTeamMutation.mutate({
    name: newTeamName.value.trim(),
    description: newTeamDesc.value.trim() || undefined
  })
}

const roleBadgeVariant: Record<string, 'default' | 'secondary' | 'outline'> = {
  Owner: 'default',
  Admin: 'secondary',
  Member: 'outline',
}
</script>

<template>
  <BasicPage
    title="我的团队"
    description="查看和管理你加入的团队"
    sticky
  >
    <template #actions>
      <Dialog v-model:open="isCreateOpen">
        <DialogTrigger as-child>
          <Button>
            <Plus class="mr-2 h-4 w-4" />
            创建团队
          </Button>
        </DialogTrigger>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>创建团队</DialogTitle>
            <DialogDescription>
              填写团队基本信息，创建者自动成为 Owner
            </DialogDescription>
          </DialogHeader>
          <div class="space-y-4 py-4">
            <div class="space-y-2">
              <Label for="team-name">团队名称</Label>
              <Input
                id="team-name"
                v-model="newTeamName"
                placeholder="请输入团队名称"
              />
            </div>
            <div class="space-y-2">
              <Label for="team-desc">团队描述</Label>
              <Textarea
                id="team-desc"
                v-model="newTeamDesc"
                placeholder="请输入团队描述"
                class="resize-none"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" @click="isCreateOpen = false">
              取消
            </Button>
            <Button @click="handleCreate">
              确定
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </template>
    
    <!-- 加载状态 -->
    <div v-if="isLoading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
    </div>
    <!-- 错误状态 -->
    <div v-else-if="error" class="text-center py-12">
      <p class="text-destructive">加载团队列表失败</p>
      <p class="text-sm text-muted-foreground mt-2">错误详情: {{ error.message }}</p>
      <Button variant="outline" class="mt-2" @click="queryClient.invalidateQueries({ queryKey: ['teams'] })">
        重试
      </Button>
    </div>
    
    <!-- 团队卡片网格 -->
    <div v-else-if="teams && teams.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <Card
        v-for="team in teams"
        :key="team.id"
        class="transition-shadow hover:shadow-md"
      >
        <CardHeader>
          <CardTitle class="text-lg">{{ team.name }}</CardTitle>
          <CardDescription>{{ team.description }}</CardDescription>
        </CardHeader>
        <CardContent class="flex items-center gap-3">
          <Badge :variant="roleBadgeVariant[team.currentUserRole] ?? 'outline'">
          {{ team.currentUserRole || '-' }}
          </Badge>
          <Badge v-if="team.status === 'DISBANDED'" variant="destructive">已解散</Badge>
          <span class="text-sm text-muted-foreground">
            {{ team.memberCount || 0 }} 位成员
          </span>
        </CardContent>
        <CardFooter>
          <Button
            v-if="team.status !== 'DISBANDED'"
            variant="secondary"
            class="w-full"
            @click="router.push(`/teams/${team.id}`)"
          >
            进入团队
          </Button>
          <Button
            v-else
            variant="outline"
            class="w-full"
            @click="router.push(`/teams/${team.id}`)"
          >
            查看历史
          </Button>
        </CardFooter>
      </Card>
    </div>

    <!-- 空态 -->
    <div v-else class="text-center py-12">
      <p class="text-muted-foreground">暂未加入任何团队，点击右上角「创建团队」开始。</p>
    </div>
  </BasicPage>
</template>

<route lang="yaml">
meta:
  auth: true
</route>
