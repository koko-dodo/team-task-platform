<script lang="ts" setup>
import { ref, inject } from 'vue'
import { readDeletedTasks, writeDeletedTasks } from '@/utils/deleted-task-cache'
import { useAuthStore } from '@/stores/auth'
import type { AxiosError } from 'axios'

import { toast } from 'vue-sonner'

import type { ApiErrorBody, TaskOperationLog } from '@/api/types'

import { tasksRefetchKey } from '@/constants/tasks-injection'

import { useAxios } from '@/composables/use-axios'
import { personalTaskApi } from '@/api/personal-tasks'
import { ModalClose, ModalDescription, ModalFooter, ModalHeader, ModalTitle } from '@/components/prop-ui/modal'

import type { Task } from '../data/schema'

const props = defineProps<{
  task: Task
}>()

const emit = defineEmits<{ close: [] }>()

const { axiosInstance } = useAxios()
const refetchTasks = inject(tasksRefetchKey, null)
const deleting = ref(false)
const authStore = useAuthStore()
// 检查任务是否被其他任务依赖
function hasDependents(task: Task): boolean {
  return (task.dependents?.length || 0) > 0
}
async function handleRemove() {
  if (hasDependents(props.task)) {
    toast.error(`无法删除：此任务被 ${props.task.dependents?.length || 0} 个后继任务依赖，请先移除这些依赖关系`)
    return
  }
  const cacheUserId = authStore.userId
  deleting.value = true
  try {
    // 删除前先获取任务日志
    let logs: TaskOperationLog[] = []
    try {
      logs = await personalTaskApi.getPersonalTaskLogs(props.task.id)
    } catch {
      // 获取日志失败不影响删除操作
    }

    if (cacheUserId !== authStore.userId) throw new Error('Session changed')
    await axiosInstance.delete(`/tasks/${props.task.id}`)

    // 保存已删除任务到 localStorage
    const deletedTask = {
      ...props.task,
      logs,
      deletedAt: new Date().toISOString()
    }
    if (cacheUserId !== authStore.userId) return
    const deletedTasks = readDeletedTasks<typeof deletedTask>(cacheUserId, 'personal')
    deletedTasks.unshift(deletedTask)
    writeDeletedTasks(cacheUserId, 'personal', deletedTasks)

    toast.success('任务已删除')
    await refetchTasks?.()
    emit('close')
  }
  catch (e) {
    const err = e as AxiosError<ApiErrorBody>
    toast.error(err.response?.data?.message || err.message || '删除失败')
  }
  finally {
    deleting.value = false
  }
}
</script>

<template>
  <div>
    <ModalHeader>
      <ModalTitle>
        删除任务？
      </ModalTitle>
      <ModalDescription>
        将永久删除「{{ task.title }}」，此操作不可撤销。
      </ModalDescription>
    </ModalHeader>

    <ModalFooter>
      <ModalClose as-child>
        <UiButton variant="outline" type="button" :disabled="deleting">
          取消
        </UiButton>
      </ModalClose>

      <UiButton variant="destructive" type="button" :disabled="deleting" @click="handleRemove">
        <UiSpinner v-if="deleting" class="mr-2 size-4" />
        删除
      </UiButton>
    </ModalFooter>
  </div>
</template>
