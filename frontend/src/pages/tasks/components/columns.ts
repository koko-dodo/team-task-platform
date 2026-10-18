import type { ColumnDef } from '@tanstack/vue-table'

import { h } from 'vue'

import { DataTableColumnHeader, SelectColumn } from '@/components/data-table'
import { Badge } from '@/components/ui/badge'

import type { Task } from '../data/schema'

import { priorities, statuses } from '../data/data'
import DataTableRowActions from './data-table-row-actions.vue'

export function createColumns(allTasks?: Task[], onRemoveRecord?: (taskId: number) => void): ColumnDef<Task>[] {
  return [
    SelectColumn as ColumnDef<Task>,
  {
    accessorKey: 'taskType',
    header: ({ column }) => h(DataTableColumnHeader<Task>, { column, title: '任务类型' }),
    cell: ({ row }) => {
      const type = row.getValue('taskType') as string
      if (type === '团队任务') {
        return h(Badge, { variant: 'default' }, () => type)
      }
      return h(Badge, { variant: 'outline' }, () => type || '个人任务')
    },
    filterFn: (row, id, value) => {
      return (value as string[]).includes(row.getValue(id))
    },
  },
  {
    accessorKey: 'title',
    header: ({ column }) => h(DataTableColumnHeader<Task>, { column, title: '任务标题' }),

    cell: ({ row }) => {
      const task = row.original
      const isDeleted = !!(task as any).deletedAt
      return h('div', { class: `max-w-[500px] truncate font-medium ${isDeleted ? 'line-through text-muted-foreground opacity-60' : ''}` }, row.getValue('title'))
    },
    filterFn: (row, id, value) => {
      return value.includes(row.getValue(id))
    },
  },
  {
    accessorKey: 'description',
    header: ({ column }) => h(DataTableColumnHeader<Task>, { column, title: '描述' }),
    cell: ({ row }) => {
      const task = row.original
      const isDeleted = !!(task as any).deletedAt
      return h('div', { class: `max-w-[300px] truncate text-sm text-muted-foreground ${isDeleted ? 'line-through opacity-60' : ''}` }, row.getValue('description'))
    },
    enableSorting: false,
  },
  {
    accessorKey: 'deadline',
    header: ({ column }) => h(DataTableColumnHeader<Task>, { column, title: '截止时间' }),
    cell: ({ row }) => h('div', { class: 'text-sm' }, row.getValue('deadline')),
  },
  {
    accessorKey: 'createdAt',
    header: ({ column }) => h(DataTableColumnHeader<Task>, { column, title: '创建时间' }),
    cell: ({ row }) => h('div', { class: 'text-sm' }, row.getValue('createdAt')),
  },
  {
    accessorKey: 'updatedAt',
    header: ({ column }) => h(DataTableColumnHeader<Task>, { column, title: '更新时间' }),
    cell: ({ row }) => h('div', { class: 'text-sm' }, row.getValue('updatedAt')),
  },
  {
    accessorKey: 'status',
    header: ({ column }) => h(DataTableColumnHeader<Task>, { column, title: '任务状态' }),

    cell: ({ row }) => {
      const task = row.original
      const isDeleted = !!(task as any).deletedAt
      if (isDeleted) {
        return h(Badge, { variant: 'destructive', class: 'bg-red-500 dark:bg-red-500' }, () => '已删除')
      }
      const status = statuses.find(s => s.value === row.getValue('status'))
      if (!status)
        return h(Badge, { variant: 'secondary' }, () => row.getValue('status') as string)
      return h('div', { class: 'flex w-[120px] items-center gap-2' }, [
        status.icon && h(status.icon, { class: 'h-4 w-4 shrink-0 text-muted-foreground' }),
        h('span', status.label),
      ])
    },
    filterFn: (row, id, value) => {
      return (value as string[]).includes(row.getValue(id))
    },
  },
  {
    accessorKey: 'priority',
    header: ({ column }) => h(DataTableColumnHeader<Task>, { column, title: '优先级' }),
    cell: ({ row }) => {
      const p = priorities.find(
        x => x.value === row.getValue('priority'),
      )

      if (!p)
        return null

      return h('div', { class: 'flex items-center' }, [
        
        h('span', { 
          class: `px-2 py-1 text-xs font-medium rounded-md border ${p.style || ''}` 
        }, p.label),
      ])
    },
    filterFn: (row, id, value) => {
      return (value as string[]).includes(row.getValue(id))
    },
  },
  {
    id: 'actions',
    header: () => '操作',
    cell: ({ row }) => {
      const task = row.original
      const isDeleted = !!(task as any).deletedAt
      return h(DataTableRowActions, { row, allTasks, deleted: isDeleted, onRemoveRecord })
    },
  },
  ]
}
export const columns: ColumnDef<Task>[] = createColumns()