<script setup lang="ts">
import { computed } from 'vue'
import type { Row } from '@tanstack/vue-table'

import { Ellipsis, Eye, Pencil, Trash2, UserPlus, X } from 'lucide-vue-next'
import { toast } from 'vue-sonner'

import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

import type { TeamTaskResponse } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  row: Row<TeamTaskResponse>
  isTeamDisbanded: boolean
  canManageTasks: boolean
  deleted?: boolean
}>()

const emit = defineEmits<{
  (e: 'view', task: TeamTaskResponse): void
  (e: 'edit', task: TeamTaskResponse): void
  (e: 'delete', task: TeamTaskResponse): void
  (e: 'removeRecord', task: TeamTaskResponse): void
}>()

const authStore = useAuthStore()

const task = computed((): TeamTaskResponse => {
  return props.row.original
})

const canEdit = computed(() => {
  if (props.canManageTasks) {
    return true
  }
  return task.value.assigneeId === authStore.userId
})

const canDelete = computed(() => props.canManageTasks)

function handleAction(action: 'view' | 'edit' | 'delete' | 'assign' | 'removeRecord') {
  if (action === 'edit' && !canEdit.value) {
    toast.error('只有作为负责人的团队任务才可修改状态')
    return
  }

  if (action === 'delete' && !canDelete.value) {
    toast.error('仅 Owner 或 Admin 可删除团队任务')
    return
  }

  if (action === 'assign' && !canDelete.value) {
    toast.error('仅 Owner 或 Admin 可分配团队任务')
    return
  }

  switch (action) {
    case 'view':
      emit('view', task.value)
      break
    case 'edit':
      emit('edit', task.value)
      break
    case 'delete':
      emit('delete', task.value)
      break
    case 'removeRecord':
      emit('removeRecord', task.value)
      break
    
  }
}
</script>

<template>
  <DropdownMenu>
    <DropdownMenuTrigger as-child>
      <Button variant="ghost" class="flex h-8 w-8 p-0 data-[state=open]:bg-muted">
        <Ellipsis class="size-4" />
        <span class="sr-only">操作</span>
      </Button>
    </DropdownMenuTrigger>
    <DropdownMenuContent align="end" class="w-[160px]">
      <DropdownMenuItem @select.stop="handleAction('view')">
        <Eye class="mr-2 size-4" />
        <span>查看详情</span>
      </DropdownMenuItem>
      <template v-if="!deleted">
        <DropdownMenuItem 
          :disabled="isTeamDisbanded || !canEdit" 
          @select.stop="handleAction('edit')"
        >
          <Pencil class="mr-2 size-4" />
          <span>编辑</span>
        </DropdownMenuItem>
        <DropdownMenuItem 
          :disabled="isTeamDisbanded || !canDelete" 
          @select.stop="handleAction('delete')"
        >
          <Trash2 class="mr-2 size-4" />
          <span>删除</span>
        </DropdownMenuItem>
      </template>
      <template v-else>
        <DropdownMenuItem @select.stop="handleAction('removeRecord')">
          <X class="mr-2 size-4" />
          <span>删除记录</span>
        </DropdownMenuItem>
      </template>
    </DropdownMenuContent>
  </DropdownMenu>
</template>