import type { InjectionKey } from 'vue'
import type { ComputedRef } from 'vue'

import type { Task } from '@/pages/tasks/data/schema'

/** 任务列表在增删改后刷新（useQuery 的 refetch） */
export const tasksRefetchKey: InjectionKey<() => Promise<unknown>> = Symbol('tasksRefetch')

/** 所有任务列表（用于依赖关系显示） */
export const allTasksKey: InjectionKey<ComputedRef<Task[]>> = Symbol('allTasks')