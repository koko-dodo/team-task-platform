<script lang="ts" setup>
import { computed, inject, reactive, ref } from 'vue'
import type { AxiosError } from 'axios'

import { toast } from 'vue-sonner'

import type { ApiErrorBody, TaskDto, TeamTaskResponse } from '@/api/types'

import TaskEditorFields from '@/components/task/task-editor-fields.vue'
import { tasksRefetchKey } from '@/constants/tasks-injection'

import { useAxios } from '@/composables/use-axios'

import type { Task } from '../data/schema'
import type { TaskValidator } from '../validators/task.validator'

import { taskValidator } from '../validators/task.validator'

const props = defineProps<{
  task: Task | null
  teamTasks?: TeamTaskResponse[]
  allTasks?: Task[]
}>()
const emit = defineEmits<{ close: [] }>()

const { axiosInstance } = useAxios()
const refetchTasks = inject(tasksRefetchKey, null)
const isAssignedTeamTask = computed(() => Boolean(props.task?.teamId))

function deadlineForInput(t: Task | null): string {
  if (!t?.deadline)
    return ''
  return t.deadline
}

const formValues = reactive<TaskValidator>({
  title: props.task ? props.task.title : '',
  description: props.task ? (props.task.description ?? '') : '',
  status: props.task && ['TODO', 'IN_PROGRESS', 'DONE'].includes(props.task.status as string)
    ? props.task.status as 'TODO' | 'IN_PROGRESS' | 'DONE'
    : 'TODO',
  priority: props.task && ['urgent', 'high', 'medium', 'low'].includes(props.task.priority as string)
    ? props.task.priority as 'urgent' | 'high' | 'medium' | 'low'
    : 'medium',
  deadline: deadlineForInput(props.task),
})
// 检查任务是否有未完成的前置任务（团队任务）
function hasUnfinishedTeamDependencies(): { hasUnfinished: boolean; unfinishedTasks: TeamTaskResponse[] }  {
  if (!props.task?.dependencies || !props.teamTasks) {
    return { hasUnfinished: false, unfinishedTasks: [] }
  }
  const unfinishedTasks = props.task.dependencies
    .map(depId => props.teamTasks?.find(t => t.id === depId))
    .filter((t): t is TeamTaskResponse => t !== undefined && t.status !== 'DONE')
  return { hasUnfinished: unfinishedTasks.length > 0, unfinishedTasks }
}
// 检查任务是否有未完成的前置任务（个人任务）
function hasUnfinishedPersonalDependencies(): { hasUnfinished: boolean; unfinishedTasks: Task[] } {
  if (!props.task?.dependencies || !props.allTasks) {
    return { hasUnfinished: false, unfinishedTasks: [] }
  }
  const unfinishedTasks = props.task.dependencies
    .map(depId => props.allTasks?.find(t => t.id === depId))
    .filter((t): t is Task => t !== undefined && t.status !== 'DONE')
  return { hasUnfinished: unfinishedTasks.length > 0, unfinishedTasks }
}
async function onSubmit(): Promise<boolean> {
  // 如果是更新状态为DONE，检查前置任务是否都已完成
  if (formValues.status === 'DONE' && props.task) {
    if (isAssignedTeamTask.value) {
      // 团队任务检查
      const { hasUnfinished, unfinishedTasks } = hasUnfinishedTeamDependencies()
      if (hasUnfinished) {
        const taskTitles = unfinishedTasks.map(t => t.title).join(', ')
        toast.error(`无法设为已完成：以下前置任务尚未完成：${taskTitles}`)
        return false
      }
    } else {
      // 个人任务检查
      const { hasUnfinished, unfinishedTasks } = hasUnfinishedPersonalDependencies()
      if (hasUnfinished) {
        const taskTitles = unfinishedTasks.map(t => t.title).join(', ')
        toast.error(`无法设为已完成：以下前置任务尚未完成：${taskTitles}`)
        return false
      }
    }
  }
  const parsedValues = taskValidator.safeParse(formValues)
  if (!parsedValues.success) {
    toast.error('请检查任务表单填写是否完整')
    return false
  }



  const values = parsedValues.data
  const payload = {
    title: values.title.trim(),
    description: values.description?.trim() || '',
    status: values.status,
    priority: values.priority,
    deadline: values.deadline || null,
  }
  try {
    if (props.task) {
      if (isAssignedTeamTask.value) {
        await axiosInstance.patch<TaskDto>(`/teams/${props.task.teamId}/tasks/${props.task.id}/status`, {
          status: values.status,
        })
        toast.success('任务状态已更新')
      }
      else {
        await axiosInstance.put<TaskDto>(`/tasks/${props.task.id}`, payload)
        toast.success('任务已更新')
      }
    }
    else {
      await axiosInstance.post<TaskDto>('/tasks', payload)
      toast.success('任务已创建')
    }
    await refetchTasks?.()
   
    return true
  }
  catch (e) {
    const err = e as AxiosError<ApiErrorBody>
    toast.error(err.response?.data?.message || err.message || '保存失败')
    return false
  }
}
defineExpose({ onSubmit })
</script>

<template>
  <div class="w-full max-w-lg space-y-5">
    <TaskEditorFields
      v-model:title="formValues.title"
      v-model:description="formValues.description"
      v-model:status="formValues.status"
      v-model:priority="formValues.priority"
      v-model:deadline="formValues.deadline"
      title-placeholder="请输入标题"
      description-placeholder="请输入任务描述"
      :detail-fields-disabled="isAssignedTeamTask"
    />
  
  </div>
</template>
