<template>
  <div class="space-y-6">
    <ModalHeader>
      <ModalTitle>任务详情</ModalTitle>
      <ModalDescription>查看任务的完整信息</ModalDescription>
    </ModalHeader>

    <div class="grid grid-cols-1 gap-6">
      <!-- 基本信息 -->
      <div class="space-y-4">
        <h3 class="text-lg font-semibold">基本信息</h3>
        

        <div>
          <label class="text-sm font-medium text-muted-foreground">标题</label>
          <p class="text-sm font-medium">{{ task.title }}</p>
        </div>
        <div>
          <label class="text-sm font-medium text-muted-foreground">描述</label>
          <p class="text-sm mt-1 p-3 bg-muted rounded-md min-h-[80px]">
            {{ task.description || '暂无描述' }}
          </p>
        </div>
      </div>

      <!-- 状态和优先级 -->
      <div class="space-y-4">
        <h3 class="text-lg font-semibold">任务属性</h3>
        
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="text-sm font-medium text-muted-foreground">状态</label>
            <div class="mt-1">
              <Badge :variant="getStatusVariant(task.status)">
                {{ getStatusLabel(task.status) }}
              </Badge>
            </div>
          </div>
          <div>
            <label class="text-sm font-medium text-muted-foreground">优先级</label>
            <div class="mt-1">
              <span 
                v-if="getPriorityStyle(task.priority)" 
                :class="`px-2 py-1 text-xs font-medium rounded-md border ${getPriorityStyle(task.priority)}`"
              >
                {{ getPriorityLabel(task.priority) }}
              </span>
              <Badge v-else :variant="getPriorityVariant(task.priority)">
                {{ getPriorityLabel(task.priority) }}
              </Badge>
            </div>
          </div>
        </div>
      </div>

      <!-- 时间信息 -->
      <div class="space-y-4">
        <h3 class="text-lg font-semibold">时间信息</h3>
        
        <div class="grid grid-cols-1 gap-3">
          <div>
            <label class="text-sm font-medium text-muted-foreground">截止时间</label>
            <p class="text-sm mt-1">
              {{ formatDate(task.deadline) || '未设置' }}
            </p>
          </div>
          <div>
            <label class="text-sm font-medium text-muted-foreground">创建时间</label>
            <p class="text-sm mt-1">{{ formatDate(task.createdAt) }}</p>
          </div>
          <div>
            <label class="text-sm font-medium text-muted-foreground">更新时间</label>
            <p class="text-sm mt-1">{{ formatDate(task.updatedAt) }}</p>
          </div>
        </div>
      </div>
       <!-- 团队信息 -->
      <div v-if="task.teamId" class="space-y-4">
        <h3 class="text-lg font-semibold">团队信息</h3>
        
        <div class="grid grid-cols-1 gap-3">
          <div>
            <label class="text-sm font-medium text-muted-foreground">所属团队</label>
            <p class="text-sm mt-1">{{ task.teamName || '未知' }}</p>
          </div>
          <div>
            <label class="text-sm font-medium text-muted-foreground">创建人</label>
            <p class="text-sm mt-1">{{ task.creatorName || '未知' }}</p>
          </div>
          <div>
            <label class="text-sm font-medium text-muted-foreground">负责人</label>
            <p class="text-sm mt-1">{{ formatAssigneeLabel(task) }}</p>
          </div>
        </div>
      </div>
      <!-- 依赖关系 -->
      <div class="space-y-4">
        <h3 class="text-lg font-semibold">依赖关系</h3>
        
        
        <!-- 当前任务依赖的前置任务 -->
        <div>
          <label class="text-sm font-medium text-muted-foreground">当前任务依赖的前置任务</label>
           <div v-if="dependencyDetail.dependencies.length > 0" class="mt-1 space-y-1">
            <div
              v-for="dep in dependencyDetail.dependencies"
              :key="dep.id"
              class="flex items-center gap-2 text-sm p-2 bg-muted rounded-md"
            >
              <Badge :variant="getStatusVariant(dep.status)">
                {{ getStatusLabel(dep.status) }}
              </Badge>
              <span>{{ dep.title }}</span>
            </div>
          </div>
          <p v-else class="mt-1 text-sm text-muted-foreground">暂无前置任务</p>
        </div>
        <!-- 依赖当前任务的后继任务 -->
        <div>
          <label class="text-sm font-medium text-muted-foreground">依赖当前任务的后继任务</label>
          <div v-if="dependencyDetail.dependents.length > 0" class="mt-1 space-y-1">
            <div
              v-for="dep in dependencyDetail.dependents"
              :key="dep.id"
              class="flex items-center gap-2 text-sm p-2 bg-muted rounded-md"
            >
              <Badge :variant="getStatusVariant(dep.status)">
                {{ getStatusLabel(dep.status) }}
              </Badge>
              <span>{{ dep.title }}</span>
            </div>
          </div>
          <p v-else class="mt-1 text-sm text-muted-foreground">暂无后继任务</p>
        </div>
        <!-- 删除警告 -->
        <div v-if="hasDependents(task)" class="flex items-center gap-2 p-3 bg-warning/10 border border-warning/30 rounded-md">
          <AlertCircle class="h-5 w-5 text-warning" />
          <span class="text-sm text-warning">此任务被其他任务依赖，删除前请先移除这些依赖关系</span>
        </div>
      </div>
      <!-- 操作日志 -->
      <div class="space-y-4">
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-semibold">操作日志</h3>
          <Button
            v-if="!loadingLogs"
            variant="ghost"
            size="sm"
            @click="loadLogs"
          >
            <RefreshCw class="h-4 w-4 mr-1" />
            刷新
          </Button>
          <Spinner v-else class="h-4 w-4" />
        </div>
        
        <div v-if="loadingLogs" class="flex justify-center py-4">
          <Spinner class="h-5 w-5" />
        </div>
        
        <div v-else-if="logs.length > 0" class="space-y-3 max-h-[300px] overflow-y-auto">
          <div
            v-for="log in logs"
            :key="log.id"
            class="p-3 bg-muted/50 rounded-md border border-muted"
          >
            <div class="flex items-start justify-between gap-4">
              <div class="flex-1">
                <div class="flex items-center gap-2 flex-wrap">
                  <User class="h-4 w-4 text-muted-foreground" />
                  <span class="text-sm font-medium">{{ log.actorName }}</span>
                  <span 
                    class="text-xs px-2 py-0.5 rounded-full"
                    :class="log.scopeType === 'TEAM' ? 'bg-blue-100 text-blue-600' : 'bg-green-100 text-green-600'"
                  >
                    {{ log.scopeType === 'TEAM' ? '团队任务' : '个人任务' }}
                  </span>
                  <span class="text-xs px-2 py-0.5 bg-primary/10 text-primary rounded-full">
                    {{ getOperationLabel(log.operationType) }}
                  </span>
                </div>
                <p class="text-sm text-foreground mt-1">{{ translateLogValue(log.summary) }}</p>
                <div 
                  v-if="(log.oldValue || log.newValue) && !['ADD_DEPENDENCY', 'REMOVE_DEPENDENCY', 'DELETE_TASK'].includes(log.operationType)" 
                  class="mt-2 text-xs space-y-2"
                >
                  <!-- 团队解散类型直接显示修改前后 -->
                  <template v-if="log.operationType === 'TEAM_DISBANDED_TASK_CHANGE'">
                    <div class="ml-4">
                      <div class="text-muted-foreground">
                        <span class="font-medium">状态 - 修改前：</span>{{ translateLogValue(log.oldValue || '') }}
                      </div>
                      <div class="text-foreground">
                        <span class="font-medium">状态 - 修改后：</span>{{ translateLogValue(log.newValue || '') }}
                      </div>
                    </div>
                  </template>
                  <!-- 其他类型按字段分行显示 -->
                  <template v-else>
                    <div v-for="(change, index) in getChangedFields(log.oldValue, log.newValue)" :key="index" class="ml-4 space-y-1">
                      <div class="text-muted-foreground">
                        <span class="font-medium">{{ fieldNames[change.field] }} - 修改前：</span>{{ translateLogValue(change.oldValue) }}
                      </div>
                      <div class="text-foreground">
                        <span class="font-medium">{{ fieldNames[change.field] }} - 修改后：</span>{{ translateLogValue(change.newValue) }}
                      </div>
                    </div>
                  </template>
                </div>
              </div>
              <div class="text-xs text-muted-foreground whitespace-nowrap">
                {{ formatLogDate(log.createdAt) }}
              </div>
            </div>
          </div>
        </div>
        
        <p v-else class="text-sm text-muted-foreground text-center py-4">暂无操作记录</p>
      </div>
    </div>

    <div class="flex justify-end pt-4">
      <Button @click="$emit('close')">关闭</Button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed ,inject, ref, onMounted } from 'vue'
import { Badge } from '@/components/ui/badge'
import { ModalDescription, ModalHeader, ModalTitle } from '@/components/prop-ui/modal'

import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import { AlertCircle, User, RefreshCw } from 'lucide-vue-next'
import type { Task } from '../data/schema'
import { priorities, statuses } from '../data/data'
import { allTasksKey } from '@/constants/tasks-injection'
import type { TaskOperationLog, TaskOperationType } from '@/api/types'
import { personalTaskApi } from '@/api/personal-tasks'
import { teamTaskApi } from '@/api/team-tasks'

const props = defineProps<{
  task: Task
  allTasks?: Task[]
}>()
/// 优先使用 props 传入的 allTasks，否则从 inject 获取
const injectedTasks = inject(allTasksKey, computed(() => [] as Task[]))
const allTasks = computed(() => props.allTasks || injectedTasks.value)

const emit = defineEmits<{
  close: []
}>()

// 日志相关状态
const logs = ref<TaskOperationLog[]>([])
const loadingLogs = ref(false)

// 操作类型标签映射
const operationLabels: Record<TaskOperationType, string> = {
  CREATE_TASK: '创建任务',
  UPDATE_TASK_INFO: '修改任务信息',
  UPDATE_TASK_STATUS: '修改任务状态',
  UPDATE_ASSIGNEE: '修改负责人',
  ADD_DEPENDENCY: '添加依赖',
  REMOVE_DEPENDENCY: '移除依赖',
  DELETE_TASK: '删除任务',
  MEMBER_LEAVE_TASK_CHANGE: '成员离开导致任务归属变化',
  TEAM_DISBANDED_TASK_CHANGE: '团队解散导致任务状态变化',
}
// 字段名中文映射
const fieldNameMapping: Record<string, string> = {
  title: '标题',
  description: '描述',
  status: '状态',
  priority: '优先级',
  deadline: '截止时间',
  assignee: '负责人',
  TODO: '待办',
  IN_PROGRESS: '进行中',
  DONE: '已完成',
  low: '不紧急不重要',
  medium: '重要不紧急',
  high: '紧急不重要',
  urgent: '紧急重要',
}
// 字段名映射（供模板使用）
const fieldNames: Record<string, string> = {
  title: '标题',
  description: '描述',
  status: '状态',
  priority: '优先级',
  deadline: '截止时间',
  assignee: '负责人',
}
// 团队状态映射
const teamStatusLabel: Record<string, string> = {
  ACTIVE: '有效（所有成员可见）',
  DISBANDED: '无效（普通成员不可见）',
}
 
// 将日志中的字段名和枚举值转换为中文
function translateLogValue(value: string | undefined): string {
  if (!value) return ''
  let result = value
  // 替换字段名
  Object.entries(fieldNameMapping).forEach(([key, label]) => {
    result = result.replace(new RegExp(`\\b${key}\\b`, 'gi'), label)
  })
  // 替换团队状态值
  Object.entries(teamStatusLabel).forEach(([key, label]) => {
    result = result.replace(new RegExp(`\\b${key}\\b`, 'gi'), label)
  })
  return result
}
// 解析任务信息快照
function parseTaskInfoSnapshot(snapshot: string | undefined): Record<string, string> {
  const result: Record<string, string> = {}
  if (!snapshot) return result
  
  const pairs = snapshot.split('; ')
  pairs.forEach(pair => {
    const [key, value] = pair.split('=', 2)
    if (key && value !== undefined) {
      result[key.trim()] = value.trim() === 'null' ? '' : value.trim()
    }
  })
  
  return result
}
// 比较两个快照，获取变化的字段
function getChangedFields(oldSnapshot: string | undefined, newSnapshot: string | undefined): Array<{field: string, oldValue: string, newValue: string}> {
  const oldValues = parseTaskInfoSnapshot(oldSnapshot)
  const newValues = parseTaskInfoSnapshot(newSnapshot)
  const changed: Array<{field: string, oldValue: string, newValue: string}> = []
  
  Object.keys(fieldNames).forEach(field => {
    const oldVal = oldValues[field] || ''
    const newVal = newValues[field] || ''
    if (oldVal !== newVal) {
      changed.push({
        field,
        oldValue: oldVal,
        newValue: newVal
      })
    }
  })
  
  return changed
}
function getOperationLabel(type: TaskOperationType): string {
  return operationLabels[type] || type
}
// 格式化字段值用于显示
function formatFieldValue(field: string, value: string): string {
  if (!value) return '(空)'
  if (field === 'status') return getStatusLabel(value)
  if (field === 'priority') return getPriorityLabel(value)
  if (field === 'deadline' && value === 'null') return '(无)'
  // 团队状态值
  if (teamStatusLabel[value]) return teamStatusLabel[value]
  return value
}

// 加载日志
async function loadLogs() {
  loadingLogs.value = true
  try {
    if (props.task.teamId) {
      // 团队任务
      logs.value = await teamTaskApi.getTeamTaskLogs(props.task.teamId, props.task.id)
    } else {
      // 个人任务
      logs.value = await personalTaskApi.getPersonalTaskLogs(props.task.id)
    }
    // 按时间倒序排序
    logs.value.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  } catch (error) {
    console.error('加载任务日志失败:', error)
  } finally {
    loadingLogs.value = false
  }
}

// 格式化日志日期
function formatLogDate(dateString: string): string {
  try {
    const date = new Date(dateString)
    return date.toLocaleString('zh-CN', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return dateString
  }
}
// 组件挂载时加载日志
onMounted(() => {
  loadLogs()
})
// 获取当前任务的最新数据（从 allTasks 中查找）
const currentTask = computed(() => {
  const updatedTask = allTasks.value.find(t => t.id === props.task.id)
  return updatedTask || props.task
})
// 获取依赖详情
const dependencyDetail = computed(() => {
  // 支持两种数据格式：ID数组或任务对象数组
  const deps = currentTask.value.dependencies || []
  const depsAreObjects = deps.length > 0 && typeof deps[0] !== 'number'
  
  let dependencies: Task[] = []
  if (depsAreObjects) {
    dependencies = deps as unknown as Task[]
  } else {
    const depIds = deps as unknown as number[]
    dependencies = depIds
      .map(depId => allTasks.value.find(t => t.id === depId))
      .filter((t): t is Task => t !== undefined)
  }
  
  // 获取后继任务：从 allTasks 中查找依赖当前任务的任务
  const dependents = allTasks.value.filter(t => {
    const taskDeps = t.dependencies || []
    const firstDep = taskDeps[0]
    let taskDepIds: number[] = []
    if (typeof firstDep === 'number') {
      taskDepIds = taskDeps as unknown as number[]
    } else if (firstDep !== undefined) {
      const depObjects = taskDeps as unknown as Task[]
      taskDepIds = depObjects.map(d => d.id)
    }
    return taskDepIds.includes(currentTask.value.id)
  })
  
  return { dependencies, dependents }
})
// 检查是否有后继任务依赖
function hasDependents(task: Task): boolean {
  return (task.dependents?.length || 0) > 0
}

function formatAssigneeLabel(task: Task) {
  if (task.assigneeName)
    return task.assigneeName
  if (task.formerAssigneeName)
    return `未分配（原负责人：${task.formerAssigneeName}）`
  return '未分配'
}
// 格式化日期显示
function formatDate(dateString?: string | null) {
  if (!dateString) return ''
  
  try {
    const date = new Date(dateString)
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    })
  } catch {
    return dateString
  }
}

// 状态相关函数
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

// 优先级相关函数
function getPriorityLabel(priority: string) {
  const priorityObj = priorities.find(p => p.value === priority)
  return priorityObj?.label || priority
}

function getPriorityStyle(priority: string) {
  const priorityObj = priorities.find(p => p.value === priority)
  return priorityObj?.style || ''
}

function getPriorityVariant(priority: string) {
  switch (priority) {
    case 'urgent': return 'destructive'
    case 'medium': return 'default'
    case 'low': return 'secondary'
    default: return 'outline'
  }
}
</script>