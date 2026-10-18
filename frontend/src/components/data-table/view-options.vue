<script setup lang="ts" generic="T">
import type { Table } from '@tanstack/vue-table'

import { RefreshCcw, Settings2 } from 'lucide-vue-next'

interface DataTableViewOptionsProps {
  table: Table<T>
}

const props = defineProps<DataTableViewOptionsProps>()

const columns = computed(() => props.table.getAllColumns()
  .filter(
    column =>
      typeof column.accessorFn !== 'undefined' && column.getCanHide(),
  ))

// 列名映射函数 - 将列ID转换为中文显示
function getColumnLabel(columnId: string): string {
  const columnLabels: Record<string, string> = {
    'id': '任务ID',
    'title': '标题',
    'description': '描述',
    'deadline': '截止时间',
    'createdAt': '创建时间',
    'updatedAt': '更新时间',
    'status': '任务状态',
    'priority': '优先级',
    'username': '用户名',
  }
  
  return columnLabels[columnId] || columnId
}

function resetColumnVisible() {
  columns.value.forEach(column => column.toggleVisibility(true))
}
</script>

<template>
  <UiDropdownMenu>
    <UiDropdownMenuTrigger as-child>
      <UiButton
        variant="outline"
        size="sm"
        class="hidden h-8 ml-auto lg:flex"
      >
        <Settings2 class="size-4 mr-2" />
        列视图
      </UiButton>
    </UiDropdownMenuTrigger>
    <UiDropdownMenuContent align="end" class="w-[150px]">
      <UiDropdownMenuLabel>切换列</UiDropdownMenuLabel>
      <UiDropdownMenuSeparator />

      <UiDropdownMenuCheckboxItem
        v-for="column in columns"
        :key="column.id"
        class="capitalize"
        :model-value="column.getIsVisible()"
        @update:model-value="(value:boolean) => column.toggleVisibility(!!value)"
      >
         {{ getColumnLabel(column.id) }}
      </UiDropdownMenuCheckboxItem>

      <UiDropdownMenuSeparator />
      <UiDropdownMenuItem
        class="capitalize"
        @click="resetColumnVisible"
      >
        <RefreshCcw />
        重置
      </UiDropdownMenuItem>
    </UiDropdownMenuContent>
  </UiDropdownMenu>
</template>
