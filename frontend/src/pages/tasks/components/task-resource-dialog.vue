<script lang="ts" setup>
import { computed, ref, inject } from 'vue'
import { useQueryClient } from '@tanstack/vue-query'
import { useAuthStore } from '@/stores/auth'

import { ModalDescription, ModalHeader, ModalTitle } from '@/components/prop-ui/modal'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Plus, X, AlertCircle } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import type { Task } from '../data/schema'
import { priorities, statuses } from '../data/data'
import { personalTaskApi } from '@/api/personal-tasks'
import { allTasksKey } from '@/constants/tasks-injection'
import TaskForm from './task-form.vue'
const queryClient = useQueryClient()
const authStore = useAuthStore()
const taskFormRef = ref<InstanceType<typeof TaskForm> | null>(null)
const props = defineProps<{
  task: Task | null
  allTasks?: Task[]
}>()
// 优先使用 props 传入的 allTasks，否则从 inject 获取
const injectedTasks = inject(allTasksKey, computed(() => [] as Task[]))
const allTasks = computed(() => props.allTasks || injectedTasks.value)

const emit = defineEmits(['close'])
const UNASSIGNED_VALUE = '__UNASSIGNED__'

const task = computed(() => props.task)
const title = computed(() => {
  if (!task.value)
    return '新建任务'
  return '编辑任务'
})
const description = computed(() => {
  if (!task.value)
    return '创建个人任务'
  if (task.value.teamId)
    return `修改任务「${task.value.title}」的状态`
  return `修改任务「${task.value.title}」的信息`
})
// 获取任务的所有后继任务（递归获取）
function getDependents(taskId: number, visited: Set<number> = new Set()): number[] {
  if (visited.has(taskId)) return []
  visited.add(taskId)
  const dependents = allTasks.value.filter(t => t.dependencies?.includes(taskId))
  const dependentIds = dependents.map(t => t.id)
  // 递归获取后继任务的后继任务
  for (const depId of dependentIds) {
    const nestedDependents = getDependents(depId, visited)
    dependentIds.push(...nestedDependents)
  }
  return [...new Set(dependentIds)]
}
// 可用的前置任务（排除自己、已有的依赖、后继任务链中的任务，仅个人任务）
const availableTasks = computed(() => {
  const currentTask = props.task
  if (!currentTask) return []
   // 获取所有后继任务（包括间接依赖）
  const allDependents = getDependents(currentTask.id)
  return allTasks.value.filter(t => 
    !t.teamId && 
    t.id !== currentTask.id && 
    !currentTask.dependencies?.includes(t.id) &&
    !allDependents.includes(t.id)
  )
})
// 获取依赖详情
const dependencyDetail = computed(() => {
  if (!props.task) return { dependencies: [], dependents: [] }
  const dependencies = props.task.dependencies?.map(depId => 
    allTasks.value.find(t => t.id === depId)
  ).filter(Boolean) as Task[] || []
  const dependents = allTasks.value.filter(t => 
    t.dependencies?.includes(props.task!.id)
  )
  return { dependencies, dependents }
})
// 获取状态标签
function getStatusLabel(status: string) {
  const statusObj = statuses.find(s => s.value === status)
  return statusObj?.label || status
}
function getStatusVariant(status: string) {
  switch (status) {
    case 'TODO': return 'secondary'
    case 'IN_PROGRESS': return 'default'
    case 'DONE': return 'outline'
    default: return 'outline'
  }
}
// 获取优先级样式
function getPriorityStyle(priority: string) {
  const p = priorities.find(x => x.value === priority)
  return p?.style || ''
}
const selectedDependencyId = ref(UNASSIGNED_VALUE)
const isAddingDependency = ref(false)
const isRemovingDependency = ref(false)
// 待删除的依赖（暂存，点击确认后才真正删除）
const pendingRemovedDependencies = ref<number[]>([])
// 待添加的依赖（暂存，点击确认后才真正添加）
const pendingAddedDependencies = ref<number[]>([])

// 检测循环依赖
function hasCycleDependency(taskId: number, dependencyId: number): boolean {

  const visited = new Set<number>()
  const stack = [dependencyId]
  
  while (stack.length > 0) {
    const current = stack.pop()!
    if (current === taskId) return true
    if (visited.has(current)) continue
    visited.add(current)
    
    const taskItem = allTasks.value.find(t => t.id === current)
    taskItem?.dependencies?.forEach(dep => {
      if (!visited.has(dep)) stack.push(dep)
    })
  }
  return false
}
// 添加依赖（暂存到待添加列表）
function handleAddDependency() {
  if (!props.task) return
  const dependencyId = parseInt(selectedDependencyId.value)
  if (isNaN(dependencyId)) {
    toast.error('请选择一个任务作为前置任务')
    toast.error('添加此依赖将导致循环依赖')
    return
  }
  
  // 检查是否已存在或已在待添加列表中
  if (props.task.dependencies?.includes(dependencyId) || 
      pendingAddedDependencies.value.includes(dependencyId)) {
    toast.error('该依赖已存在')
    return
  }
  
  // 添加到待添加列表
  pendingAddedDependencies.value.push(dependencyId)
  selectedDependencyId.value = UNASSIGNED_VALUE
}
// 删除依赖（暂存到待删除列表）
function handleRemoveDependency(dependencyId: number) {
  if (!props.task) return
  
  // 如果在待添加列表中，直接移除
  const addIndex = pendingAddedDependencies.value.indexOf(dependencyId)
  if (addIndex > -1) {
    pendingAddedDependencies.value.splice(addIndex, 1)
    return
  }
  
  // 添加到待删除列表
  if (!pendingRemovedDependencies.value.includes(dependencyId)) {
    pendingRemovedDependencies.value.push(dependencyId)
  }
}
// 当前依赖列表（排除待删除的）
const currentDependencies = computed((): Task[] => {
  if (!props.task?.dependencies) return []
  return props.task.dependencies
    .map(depId => allTasks.value.find(t => t.id === depId))
    .filter((t): t is Task => t !== undefined)
    .filter(dep => !pendingRemovedDependencies.value.includes(dep.id))
})
// 保存所有变更（先保存基本信息，再保存依赖）
async function handleSave() {
  // 先调用 TaskForm 的 onSubmit 保存基本信息
  const formSaved = await taskFormRef.value?.onSubmit()
  
  // 如果基本信息保存失败，不继续保存依赖
  if (!formSaved) {
    return
  }
  
  // 如果没有待处理的依赖变更，直接关闭
  if (pendingAddedDependencies.value.length === 0 && pendingRemovedDependencies.value.length === 0) {
    emit('close')
    return
  }
  
  let successCount = 0
  let failCount = 0
  
  // 先批量删除待删除的依赖
  for (const depId of pendingRemovedDependencies.value) {
    try {
      await personalTaskApi.removeDependency(props.task!.id, depId)
      successCount++
    } catch (error) {
      failCount++
    }
  }
  
  // 再批量添加待添加的依赖
  for (const depId of pendingAddedDependencies.value) {
    try {
      await personalTaskApi.addDependency(props.task!.id, depId)
      successCount++
    } catch (error) {
      failCount++
    }
  }
  
  // 更新缓存
  queryClient.invalidateQueries({ queryKey: ['tasks', 'my', authStore.userId] })
  
  // 显示结果
  if (failCount === 0) {
    toast.success(`成功更新 ${successCount} 个依赖关系`)
  } else {
    toast.success(`成功更新 ${successCount} 个依赖关系，失败 ${failCount} 个`)
  }
  
  // 关闭弹窗
  emit('close')
}
// 检查是否有后继任务依赖
function hasDependents(): boolean {
  if (!props.task) return false
  return (props.task.dependents?.length || 0) > 0
}


</script>

<template>
  <div>
    <ModalHeader>
      <ModalTitle>
        {{ title }}
      </ModalTitle>
      <ModalDescription>
        {{ description }}
      </ModalDescription>
    </ModalHeader>
    <div class="space-y-6 mt-4">
      <!-- 基本信息编辑 -->
      <TaskForm 
        ref="taskFormRef"
        class="mt-2" 
        :task="task" 
        :all-tasks="allTasks"
        
      />
      
      <!-- 依赖管理模块（仅个人任务） -->
      <div v-if="task && !task.teamId && allTasks.length > 0" class="border-t pt-4">
        <div class="flex items-center justify-between">
          <h4 class="font-medium">任务依赖管理</h4>
          <!-- 删除警告 -->
          <div v-if="hasDependents()" class="flex items-center gap-2 px-3 py-1.5 bg-warning/10 border border-warning/30 rounded-md">
            <AlertCircle class="h-4 w-4 text-warning" />
            <span class="text-xs text-warning">被其他任务依赖</span>
          </div>
        </div>
        
        <!-- 添加前置任务 -->
        <div class="space-y-2 mb-4 mt-3">
          <label class="text-sm font-medium text-muted-foreground">添加前置任务</label>
          <Select v-model="selectedDependencyId">
            <SelectTrigger><SelectValue placeholder="选择前置任务" /></SelectTrigger>
            <SelectContent>
              <template v-if="availableTasks.length > 0">
                <SelectItem :value="UNASSIGNED_VALUE">选择任务</SelectItem>
                <SelectItem
                  v-for="t in availableTasks"
                  :key="t.id"
                  :value="String(t.id)"
                >
                  {{ t.title }} ({{ getStatusLabel(t.status) }})
                </SelectItem>
              </template>
              <template v-else>
                <SelectItem :value="UNASSIGNED_VALUE" disabled>
                  无可选任务
                </SelectItem>
              </template>
            </SelectContent>
          </Select>
          <Button 
            @click="handleAddDependency" 
            :disabled="selectedDependencyId === UNASSIGNED_VALUE"
            class="w-full"
          >
            <Plus class="mr-2 h-4 w-4" />
            添加依赖
          </Button>
        </div>
        
        <!-- 当前任务依赖的前置任务 -->
        <div class="space-y-2 mb-4">
          <label class="text-sm font-medium text-muted-foreground">当前任务依赖的前置任务</label>
          <div v-if="currentDependencies.length > 0 || pendingAddedDependencies.length > 0" class="space-y-1">
            <!-- 现有依赖（排除待删除的） -->
            <div
              v-for="dep in currentDependencies"
              :key="dep.id"
              class="flex items-center justify-between p-2 bg-muted rounded-md"
              :class="{ 'opacity-50 line-through': pendingRemovedDependencies.includes(dep.id) }"
            >
              <div class="flex items-center gap-2">
                <Badge :variant="getStatusVariant(dep.status)">
                  {{ getStatusLabel(dep.status) }}
                </Badge>
                <span class="text-sm">{{ dep.title }}</span>
                <span 
                  class="text-xs px-1.5 py-0.5 rounded"
                  :class="getPriorityStyle(dep.priority)"
                >
                  {{ dep.priority }}
                </span>
              </div>
              <Button 
                variant="ghost" 
                size="icon" 
                class="h-7 w-7 text-destructive hover:text-destructive hover:bg-destructive/10" 
                @click="handleRemoveDependency(dep.id)"
                
              >
                <X class="h-4 w-4" />
              </Button>
            </div>
            <!-- 待添加的依赖 -->
            <div
              v-for="depId in pendingAddedDependencies"
              :key="'add-' + depId"
              class="flex items-center justify-between p-2 bg-primary/10 border border-primary/30 rounded-md"
            >
              <div class="flex items-center gap-2">
                <Badge variant="secondary" class="bg-primary/20 text-primary">待添加</Badge>
                <span class="text-sm">{{ allTasks.find(t => t.id === depId)?.title }}</span>
              </div>
              <Button 
                variant="ghost" 
                size="icon" 
                class="h-7 w-7 text-destructive hover:text-destructive hover:bg-destructive/10" 
                @click="handleRemoveDependency(depId)"
              >
                <X class="h-4 w-4" />
              </Button>
            </div>
          </div>
          <p v-else class="text-sm text-muted-foreground">暂无前置任务</p>
        </div>
        
        <!-- 依赖当前任务的后继任务 -->
        <div class="space-y-2">
          <label class="text-sm font-medium text-muted-foreground">依赖当前任务的后继任务</label>
          <div v-if="dependencyDetail.dependents.length > 0" class="space-y-1">
            <div
              v-for="dep in dependencyDetail.dependents"
              :key="dep.id"
              class="flex items-center gap-2 p-2 bg-muted rounded-md"
            >
              <Badge :variant="getStatusVariant(dep.status)">
                {{ getStatusLabel(dep.status) }}
              </Badge>
              <span class="text-sm">{{ dep.title }}</span>
              <span 
                class="text-xs px-1.5 py-0.5 rounded"
                :class="getPriorityStyle(dep.priority)"
              >
                {{ dep.priority }}
              </span>
            </div>
          </div>
          <p v-else class="text-sm text-muted-foreground">暂无后继任务</p>
        </div>
      </div>
    </div>
    <!-- 操作按钮 -->
    <div class="flex justify-end gap-3 mt-6 pt-4 border-t">
      <Button variant="outline" @click="$emit('close')">取消</Button>
      <Button @click="handleSave">确定</Button>
    </div>
  </div>
</template>
