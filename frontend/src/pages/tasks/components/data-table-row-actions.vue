<script setup lang="ts">
import type { Row } from '@tanstack/vue-table'
import type { Component } from 'vue'

import { Ellipsis, FilePenLine, Trash2, Eye, X } from 'lucide-vue-next'
import { toast } from 'vue-sonner'

import { Modal, ModalContent } from '@/components/prop-ui/modal'
import { useAuthStore } from '@/stores/auth'

import type { Task } from '../data/schema'

import { taskSchema } from '../data/schema'

const props = defineProps<{
  row: Row<Task>
  allTasks?: Task[]
  deleted?: boolean
  onRemoveRecord?: (taskId: number) => void
}>()
const authStore = useAuthStore()

const task = computed((): Task => {
  const raw = props.row.original
  const parsed = taskSchema.safeParse(raw)
  return parsed.success ? parsed.data : (raw as Task)
})
const canEdit = computed(() => {
  if (!task.value.teamId)
    return true
  // 团队任务在个人任务列表中不能编辑，只能在团队任务列表中修改
  return false
})
const canDelete = computed(() => !task.value.teamId)

const showComponent = shallowRef<Component | null>(null)
const isOpen = ref(false)

type TCommand = 'view' | 'edit' | 'delete'

const componentLoader: Record<TCommand, () => Promise<{ default: Component }>> = {
  view: () => import('./task-detail-dialog.vue'),
  edit: () => import('./task-resource-dialog.vue'),
  delete: () => import('./task-delete.vue'),
}

async function handleSelect(command: TCommand) {
  if (command === 'edit' && !canEdit.value) {
    toast.error('只有作为负责人的团队任务才可在任务列表中修改状态')
    return
  }

  if (command === 'delete' && !canDelete.value) {
    toast.error('团队任务不能在此处删除')
    return
  }

  try {
    const { default: component } = await componentLoader[command]()
    showComponent.value = component
    isOpen.value = true
  }
  catch (e) {
    console.error(`Failed to load component for "${command}"`, e)
  }
}

function onDialogClose() {
  isOpen.value = false
}

function handleRemoveRecord() {
  props.onRemoveRecord?.(task.value.id)
}
</script>

<template>
  <Modal v-model:open="isOpen">
    <UiDropdownMenu>
      <UiDropdownMenuTrigger as-child>
        <UiButton
          variant="ghost"
          class="flex h-8 w-8 p-0 data-[state=open]:bg-muted"
        >
          <Ellipsis class="size-4" />
          <span class="sr-only">操作菜单</span>
        </UiButton>
      </UiDropdownMenuTrigger>
      <UiDropdownMenuContent align="end" class="w-[160px]">
        <UiDropdownMenuItem @select.stop="handleSelect('view')">
          <Eye class="mr-2 size-4" />
          <span>查看详情</span>
        </UiDropdownMenuItem>
        <template v-if="!deleted">
          <UiDropdownMenuItem :disabled="!canEdit" @select.stop="handleSelect('edit')">
            <FilePenLine class="mr-2 size-4" />
            <span>编辑</span>
          </UiDropdownMenuItem>

          <UiDropdownMenuSeparator />

          <UiDropdownMenuItem :disabled="!canDelete" @select.stop="handleSelect('delete')">
            <Trash2 class="mr-2 size-4" />
            <span>删除</span>
          </UiDropdownMenuItem>
        </template>
        <template v-else>
          <UiDropdownMenuItem @select.stop="handleRemoveRecord">
            <X class="mr-2 size-4" />
            <span>删除记录</span>
          </UiDropdownMenuItem>
        </template>
      </UiDropdownMenuContent>
    </UiDropdownMenu>

    <ModalContent>
      <component :is="showComponent" :task="task" :all-tasks="allTasks" @close="onDialogClose" />
    </ModalContent>
  </Modal>
</template>