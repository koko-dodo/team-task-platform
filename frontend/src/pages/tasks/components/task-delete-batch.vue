<script lang="ts" setup generic="T = Task">
import type { Table as VueTable } from '@tanstack/vue-table'

import { toast } from 'vue-sonner'

import { tasksRefetchKey } from '@/constants/tasks-injection'

import ConfirmDialog from '@/components/confirm-dialog.vue'

import { useAxios } from '@/composables/use-axios'

import type { Task } from '../data/schema'

const { table } = defineProps<{
  table: VueTable<T>
}>()

const openModel = defineModel<boolean>('open', {
  default: false,
})

const CONFIRM_WORD = 'DELETE'

const confirmValue = ref('')

const { axiosInstance } = useAxios()
const refetchTasks = inject(tasksRefetchKey, null)

const selectedRows = computed(() => table.getSelectedRowModel().rows)
const selectedCount = computed(() => selectedRows.value.length || 0)

async function handleConfirm() {
  if (confirmValue.value !== CONFIRM_WORD) {
    toast.error(`请输入 ${CONFIRM_WORD} 以确认`)
    return
  }

  const rows = selectedRows.value.slice()
  openModel.value = false
  confirmValue.value = ''

  try {
    await Promise.all(
      rows.map((r) => {
        const t = r.original as Task
        return axiosInstance.delete(`/tasks/${t.id}`)
      }),
    )
    table.resetRowSelection()
    await refetchTasks?.()
    toast.success(`已删除 ${rows.length} 条任务`)
  }
  catch {
    toast.error('批量删除失败')
    await refetchTasks?.()
  }
}
</script>

<template>
  <ConfirmDialog
    v-model:open="openModel"
    confirm-button-text="删除"
    destructive
    :disabled="confirmValue.trim() !== CONFIRM_WORD"
    @confirm="handleConfirm"
  >
    <template #title>
      删除 {{ selectedCount }} 个任务？
    </template>
    <template #description>
      此操作不可恢复。
    </template>

    <template #default>
      <UiLabel class="my-4 flex flex-col items-start gap-1.5">
        <span>输入 {{ CONFIRM_WORD }} 确认：</span>
        <UiInput
          v-model="confirmValue"
          :placeholder="`请输入 ${CONFIRM_WORD}`"
        />
      </UiLabel>

      <UiAlert variant="destructive">
        <UiAlertTitle>注意</UiAlertTitle>
        <UiAlertDescription>
          将逐条调用后端删除接口。
        </UiAlertDescription>
      </UiAlert>
    </template>
  </ConfirmDialog>
</template>
