<script setup lang="ts">
// GET /api/tasks/my → 返回个人任务 + 分配给我的团队任务
import { computed, ref, watch } from 'vue'
import { readDeletedTasks, writeDeletedTasks } from '@/utils/deleted-task-cache'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { tasksRefetchKey } from '@/constants/tasks-injection'
import { toast } from 'vue-sonner'
import { BasicPage } from '@/components/global-layout'
import { useAxios } from '@/composables/use-axios'
import { useAuthStore } from '@/stores/auth'
import { personalTaskApi } from '@/api/personal-tasks'
import { withTaskDependencies } from '@/api/task-dependency-utils'
import type { DependencyAwareTaskDto } from '@/api/task-types'
import { getDisplayDependents } from '@/utils/dependency-graph'
import { columns } from './components/columns'
import { createColumns } from './components/columns'
import DataTable from './components/data-table.vue'
import TaskCreate from './components/task-create.vue'
import TaskDetailDialog from './components/task-detail-dialog.vue'
import {
  Tabs,
  TabsList,
  TabsTrigger,
} from '@/components/ui/tabs'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Dialog,
  DialogContent,
} from '@/components/ui/dialog'
import type { Task } from './data/schema'
import type { TaskDto,TaskOperationLog } from '@/api/types'
// 已删除任务缓存（存储在localStorage中持久化）
interface DeletedTask extends Task {
  deletedAt: string
  logs?: TaskOperationLog[]
}

const authStore = useAuthStore()
const deletedTasks = ref<DeletedTask[]>([])
watch(() => authStore.userId, (userId) => {
  deletedTasks.value = readDeletedTasks<DeletedTask>(userId, 'personal')
}, { immediate: true, flush: 'sync' })

function saveDeletedTasks() {
  writeDeletedTasks(authStore.userId, 'personal', deletedTasks.value)
}

// 删除已删除任务记录
function handleRemoveDeletedRecord(taskId: number) {
  deletedTasks.value = deletedTasks.value.filter(t => t.id !== taskId)
  saveDeletedTasks()
  queryClient.invalidateQueries({ queryKey: ['tasks', 'my', authStore.userId] })
}

const { axiosInstance } = useAxios()
const queryClient = useQueryClient()
const UNASSIGNED_VALUE = '__UNASSIGNED__'
const activeTab = ref('tasks')
// 任务弹窗状态
const isViewOpen = ref(false)
const selectedTask = ref<Task | null>(null)
const isLoggedIn = computed(() => authStore.isLogin)
// 展开状态管理
const expandedTasks = ref<Set<number>>(new Set())
function toggleExpand(taskId: number) {
  if (expandedTasks.value.has(taskId)) {
    expandedTasks.value.delete(taskId)
  } else {
    expandedTasks.value.add(taskId)
  }
}
function isExpanded(taskId: number): boolean {
  return expandedTasks.value.has(taskId)
}
// 打开任务详情
function openView(task: Task) {
  selectedTask.value = task
  isViewOpen.value = true
}
// 使用 useQuery 调用 /api/tasks/my
const { data: tasks, isPending, error, refetch: queryRefetch } = useQuery({
  queryKey: computed(() => ['tasks', 'my', authStore.userId] as const),
  queryFn: async () => {
    const response = await axiosInstance.get<TaskDto[]>('/tasks/my')
    const tasksWithDependencies = await withTaskDependencies(response.data)

    // 将后端DTO转换为前端Task类型
    const activeTasks = tasksWithDependencies.map((taskDto: DependencyAwareTaskDto) => ({
      id: taskDto.id,
      title: taskDto.title,
      description: taskDto.description || '',
      status: taskDto.status,
      priority: taskDto.priority,
      deadline: taskDto.deadline || '',
      createdAt: taskDto.createdAt || '',
      updatedAt: taskDto.updatedAt || '',
      // 团队相关字段
      creatorId: taskDto.creatorId,
      creatorName: taskDto.creatorName,
      teamId: taskDto.teamId,
      assigneeId: taskDto.assigneeId,
      teamName: taskDto.teamName || '',
      assigneeName: taskDto.assigneeName || '',
      formerAssigneeId: taskDto.formerAssigneeId,
      formerAssigneeName: taskDto.formerAssigneeName || '',
      dependencies: taskDto.dependencies || [],
      dependents: taskDto.dependents || [],
      // 任务类型判断：根据teamId判断是个人任务还是团队任务
      taskType: taskDto.teamId ? '团队任务' : '个人任务'
    } as Task))
    return [...activeTasks, ...deletedTasks.value] as Task[]
  },
  enabled: computed(() => authStore.isLogin && authStore.userId != null),
})

// 获取个人任务列表（用于依赖选择）
const personalTasks = computed(() => 
  tasks.value?.filter(t => !t.teamId) || []
)
// 添加依赖mutation
const addDependencyMutation = useMutation({
  mutationFn: ({ taskId, dependencyId }: { taskId: number; dependencyId: number }) =>
    personalTaskApi.addDependency(taskId, dependencyId),
  onSuccess: () => {
    toast.success('依赖关系添加成功')
    queryClient.invalidateQueries({ queryKey: ['tasks', 'my', authStore.userId] })
  },
  onError: (error: any) => {
    toast.error('添加依赖失败: ' + (error.response?.data?.message || error.message))
  }
})
const isAddingDependency = computed(() => addDependencyMutation.isPending.value)
// 删除依赖mutation
const removeDependencyMutation = useMutation({
  mutationFn: ({ taskId, dependencyId }: { taskId: number; dependencyId: number }) =>
    personalTaskApi.removeDependency(taskId, dependencyId),
  onSuccess: () => {
    toast.success('依赖关系已删除')
    queryClient.invalidateQueries({ queryKey: ['tasks', 'my', authStore.userId] })
  },
  onError: (error: any) => {
    toast.error('删除依赖失败: ' + (error.response?.data?.message || error.message))
  }
})
const isRemovingDependency = computed(() => removeDependencyMutation.isPending.value)
// 删除任务mutation
const deleteTaskMutation = useMutation({
mutationFn: async ({ task }: { task: Task & { logs?: TaskOperationLog[] } }) => {
    const cacheUserId = authStore.userId
    // 删除前先获取任务日志
    try {
    const logs = await personalTaskApi.getPersonalTaskLogs(task.id)
    task.logs = logs
    } catch {
    // 获取日志失败不影响删除操作
    }
    if (cacheUserId !== authStore.userId) throw new Error('Session changed')
    await personalTaskApi.deletePersonalTask(task.id)
    return { task, cacheUserId }
},
onSuccess: ({ task, cacheUserId }) => {
    if (cacheUserId !== authStore.userId) return
    const deletedTask: DeletedTask = {
    ...task,
    deletedAt: new Date().toISOString()
    }
    deletedTasks.value.unshift(deletedTask)
    saveDeletedTasks()
    toast.success('任务删除成功')
    queryClient.invalidateQueries({ queryKey: ['tasks', 'my', authStore.userId] })
},
onError: (error: any) => {
    toast.error('任务删除失败: ' + (error.response?.data?.message || error.message))
}
})
// 获取任务依赖详情
function getTaskDependenciesDetail(task: Task) {
  const dependencies = task.dependencies?.map(depId => 
    tasks.value?.find(t => t.id === depId)
  ).filter(Boolean) as Task[] || []
  const dependents = task.dependents?.map(depId => 
    tasks.value?.find(t => t.id === depId)
  ).filter(Boolean) as Task[] || []
  return { dependencies, dependents }
}

function getMindmapDependents(task: Task): Task[] {
  return getDisplayDependents(task, personalTasks.value)
}
// 获取总依赖关系数
function getTotalDependencies(): number {
  return personalTasks.value.reduce((total, task) => total + (task.dependencies?.length || 0), 0)
}
// 获取有依赖的任务列表
function getTasksWithDependencies(): Task[] {
  return personalTasks.value.filter(task => (task.dependencies?.length || 0) > 0)
}
// 获取没有前置任务的任务（根节点）
function getRootTasks(): Task[] {
  return personalTasks.value.filter(task => !task.dependencies || task.dependencies.length === 0)
}
// 拓扑图层级数据
interface LevelTask extends Task {
  isConverge?: boolean
}
// 获取拓扑图层级
const dependencyLevels = computed(() => {
  const taskMap = new Map(personalTasks.value.map(t => [t.id, t]))
  const levels: LevelTask[][] = []
  
  // 找出汇合点（有多个前置任务的任务）
  const taskToPredecessors = new Map<number, number[]>()
  personalTasks.value.forEach(task => {
    task.dependencies?.forEach(depId => {
      if (!taskToPredecessors.has(task.id)) {
        taskToPredecessors.set(task.id, [])
      }
      taskToPredecessors.get(task.id)!.push(depId)
    })
  })
  
  // 获取节点层级（最长依赖深度）
  const getLevel = (taskId: number, path: Set<number> = new Set()): number => {
    if (path.has(taskId)) return 0
    path.add(taskId)
    const task = taskMap.get(taskId)
    if (!task || !task.dependencies || task.dependencies.length === 0) {
      return 0
    }

    
    const maxDepLevel = Math.max(...task.dependencies.map(depId => getLevel(depId, new Set(path))))
    return maxDepLevel + 1
  }
  
  // 按层级分组
  const levelMap = new Map<number, LevelTask[]>()
  personalTasks.value.forEach(task => {
    if ((task.dependencies?.length || 0) > 0 || (task.dependents?.length || 0) > 0) {
      const level = getLevel(task.id)
      if (!levelMap.has(level)) {
        levelMap.set(level, [])
      }
      const isConverge = (taskToPredecessors.get(task.id)?.length || 0) > 1
      levelMap.get(level)!.push({ ...task, isConverge })
    }
  })
  
  // 按层级排序
  const sortedLevels = Array.from(levelMap.keys()).sort((a, b) => a - b)
  sortedLevels.forEach(level => {
    levels.push(levelMap.get(level)!)
  })
  
  return levels
})

// 获取层级样式
const getLevelStyle = (level: LevelTask[], levelIndex: number) => {
  const estimatedCardWidth = 240
  const cardGap = 20
  const levelGap = 120
  const totalWidth = level.length * (estimatedCardWidth + cardGap)
  const startX = Math.max(0, (850 - totalWidth) / 2)
  return {
    position: 'absolute' as const,
    top: `${levelIndex * levelGap}px`,
    left: `${startX}px`
  }
}
// 获取任务卡片样式
const getTaskStyle = (task: LevelTask, level: LevelTask[], levelIndex: number) => {
  const estimatedCardWidth = 240
  const cardGap = 20
  const taskIndex = level.indexOf(task)
  const marginAdjust = task.isConverge ? -20 : 0
  return {
    marginLeft: `${marginAdjust}px`,
    marginRight: `${marginAdjust}px`,
    width: `${estimatedCardWidth}px`,
    position: 'absolute' as const,
    left: `${taskIndex * (estimatedCardWidth + cardGap)}px`,
    top: '0'
  }
}

// 获取箭头连接数据
const dependencyConnections = computed(() => {
  const taskMap = new Map(personalTasks.value.map(t => [t.id, t]))
  const connections: { x1: number; y1: number; x2: number; y2: number }[] = []
  
  // 获取节点位置（记录卡片边缘位置）
  const nodePositions = new Map<number, { centerX: number; top: number; bottom: number }>()
  const estimatedCardWidth = 240
  const cardHeight = 60
  const levelGap = 120
  const cardGap = 20
  
  dependencyLevels.value.forEach((level, levelIndex) => {
    const levelTopY = levelIndex * levelGap
    const totalWidth = level.length * (estimatedCardWidth + cardGap)
    const startX = Math.max(0, (850 - totalWidth) / 2)
    
    level.forEach((task, taskIndex) => {
      const cardLeft = startX + taskIndex * (estimatedCardWidth + cardGap)
      nodePositions.set(task.id, {
        centerX: cardLeft + estimatedCardWidth / 2,
        top: levelTopY,
        bottom: levelTopY + cardHeight
      })
    })
  })
  
  // 检查是否为传递依赖：如果存在中间节点M，使得 task -> M -> depId，则 task -> depId 是传递依赖
  const isTransitiveDependency = (taskId: number, depId: number): boolean => {
    const task = taskMap.get(taskId)
    if (!task || !task.dependencies) return false
    
    // 检查是否存在中间节点 M，使得 task 依赖 M，且 M 依赖 depId
    return task.dependencies.some(mId => {
      const middleTask = taskMap.get(mId)
      return middleTask && middleTask.dependencies?.includes(depId)
    })
  }
  
  // 生成连接（过滤传递依赖）
  personalTasks.value.forEach(task => {
    task.dependencies?.forEach(depId => {
      // 如果是传递依赖，则跳过
      if (isTransitiveDependency(task.id, depId)) {
        return
      }
      const fromPos = nodePositions.get(depId)
      const toPos = nodePositions.get(task.id)
      if (fromPos && toPos) {
        connections.push({
          x1: fromPos.centerX,
          y1: fromPos.bottom,
          x2: toPos.centerX,
          y2: toPos.top
        })
      }
    })
  })
  
  return connections
})
// 获取孤立任务（无任何依赖关系的任务）
const isolatedTasks = computed(() => {
  return personalTasks.value.filter(task => 
    (!task.dependencies || task.dependencies.length === 0) && 
    (!task.dependents || task.dependents.length === 0)
  )
})
// 检查任务是否有未完成的前置任务
function hasUnfinishedDependencies(task: Task): { hasUnfinished: boolean; unfinishedTasks: Task[] } {
  const unfinishedTasks = (task.dependencies?.map(depId => 
    tasks.value?.find(t => t.id === depId)
  ).filter(t => t && t.status !== 'DONE') as Task[]) || []
  return { hasUnfinished: unfinishedTasks.length > 0, unfinishedTasks }
}
// 检查任务是否被其他任务依赖
function hasDependents(task: Task): boolean {
  return (task.dependents?.length || 0) > 0
}
// 检测循环依赖
function hasCycleDependency(taskId: number, dependencyId: number): boolean {
  if (!tasks.value) return false
  const visited = new Set<number>()
  const stack = [dependencyId]
  
  while (stack.length > 0) {
    const current = stack.pop()!
    if (current === taskId) return true
    if (visited.has(current)) continue
    visited.add(current)
    
    const task = tasks.value?.find(t => t.id === current)
    task?.dependencies?.forEach(dep => {
      if (!visited.has(dep)) stack.push(dep)
    })
  }
  return false
}
// 添加依赖
function handleAddDependency(taskId: number, dependencyId: number) {
  // 检查是否为自己
  if (taskId === dependencyId) {
    toast.error('不能依赖自己')
    return
  }
  
  // 检查循环依赖
  if (hasCycleDependency(taskId, dependencyId)) {
    toast.error('添加此依赖将导致循环依赖')
    return
  }
  
  addDependencyMutation.mutate({ taskId, dependencyId })
}
// 删除依赖
function handleRemoveDependency(taskId: number, dependencyId: number) {
  removeDependencyMutation.mutate({ taskId, dependencyId })
}
// 删除任务
function handleDeleteTask(task: Task) {
  if (hasDependents(task)) {
    const dependentTitles = getTaskDependenciesDetail(task).dependents.map(t => t.title).join(', ')
    toast.error(`无法删除：此任务被以下后继任务依赖：${dependentTitles}`)
    return
  }
  deleteTaskMutation.mutate({ task: task as Task & { logs?: TaskOperationLog[] } })
}
// 移除已删除任务记录
function handleRestoreTask(taskId: number) {
  deletedTasks.value = deletedTasks.value.filter(t => t.id !== taskId)
  saveDeletedTasks()
  toast.success('已从已删除列表中移除')
}
// 清空已删除任务
function handleClearDeletedTasks() {
  deletedTasks.value = []
  saveDeletedTasks()
  toast.success('已清空已删除任务列表')
}
 
// 刷新功能实现 - 返回Promise以匹配类型要求
async function handleRefetch() {
  await queryClient.invalidateQueries({ queryKey: ['tasks'] })
  return queryRefetch()
}
provide(tasksRefetchKey, handleRefetch)
// 提供所有任务列表（用于依赖关系显示）
import { allTasksKey } from '@/constants/tasks-injection'
provide(allTasksKey, computed(() => tasks.value || []))
// 错误监控
watch(error, (e) => {
  if (e) {
    console.error('加载任务失败', e)
  }
})
// 状态标签映射
const statusLabel: Record<string, string> = {
  TODO: '待办',
  IN_PROGRESS: '进行中',
  DONE: '已完成',
}
const statusBadgeVariant: Record<string, 'default' | 'secondary' | 'outline'> = {
  TODO: 'secondary',
  IN_PROGRESS: 'default',
  DONE: 'outline',
}
const priorityLabel: Record<string, string> = {
  urgent:'紧急重要',
  high: '紧急不重要',
  medium: '重要不紧急',
  low: '不紧急不重要',
}
const priorityStyle: Record<string, string> = {
  low: 'bg-green-400 text-white',
  medium: 'bg-yellow-400 text-white',
  high: 'bg-orange-400 text-white',
  urgent: 'bg-red-500 text-white',
}
// 操作类型标签映射
const operationLabels: Record<string, string> = {
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
function getOperationLabel(type: string): string {
  return operationLabels[type] || type
}
function formatDeletedDate(dateString: string): string {
  try {
    const date = new Date(dateString)
    return date.toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
  } catch { return dateString }
}
function formatLogDate(dateString: string): string {
  try {
    const date = new Date(dateString)
    return date.toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
  } catch { return dateString }
}
// 暴露给子组件的方法
defineExpose({
  getTaskDependenciesDetail,
  hasUnfinishedDependencies,
  hasDependents,
  handleAddDependency,
  handleRemoveDependency,
  handleDeleteTask,
  personalTasks,
  isAddingDependency,
  isRemovingDependency,
  UNASSIGNED_VALUE,
  statusLabel,
  statusBadgeVariant,
})

</script>

<template>
  <BasicPage
    title="任务管理"
    description="个人任务 + 分配给我的团队任务"
    sticky
  >
    <template #actions>
      <TaskCreate />
    </template>

     <p v-if="!isLoggedIn" class="text-sm text-muted-foreground mb-4">
      请先 <RouterLink to="/auth/sign-in" class="text-primary underline">登录</RouterLink> 后查看任务。
    </p>

    <p v-else-if="error" class="text-sm text-destructive mb-4">
      加载失败，请确认后端已启动且数据库已初始化。
    </p>

    <!-- 标签栏 -->
    <Tabs v-model="activeTab" class="mb-4">
  <TabsList>
    <TabsTrigger value="tasks">个人任务</TabsTrigger>
    <TabsTrigger value="dependencies">任务依赖</TabsTrigger>
  </TabsList>
</Tabs>
    <!-- 个人任务视图 -->
    <div v-if="activeTab === 'tasks'" class="overflow-x-auto">
      <DataTable :data="tasks ?? []" :columns="createColumns(tasks ?? [], handleRemoveDeletedRecord)" :loading="isPending" />
    </div>
    
    <!-- 依赖关系视图 -->
    <div v-else-if="activeTab === 'dependencies'" class="space-y-6">
      
      <!-- 任务依赖图 -->
      <Card>
        <CardHeader>
          <CardTitle>任务依赖链</CardTitle>
          <CardDescription>展示任务间的依赖关系</CardDescription>
        </CardHeader>
        <CardContent>
          <div v-if="personalTasks.length > 0">
            <!-- 拓扑图容器 -->
            <div class="flex justify-center overflow-x-auto pb-4" style="min-height: 400px;">
              <div class="relative" style="min-width: 850px; min-height: 400px;">
                <svg class="absolute inset-0 w-full h-full pointer-events-none" style="z-index: 0;">
                  <defs>
                    <marker
                      id="arrowhead-personal"
                      markerWidth="10"
                      markerHeight="7"
                      refX="9"
                      refY="3.5"
                      orient="auto"
                    >
                      <polygon points="0 0, 10 3.5, 0 7" fill="#6366f1" />
                    </marker>
                  </defs>
                  <!-- 渲染箭头 -->
                  <template v-for="(connection, idx) in dependencyConnections" :key="idx">
                    <line
                      :x1="connection.x1"
                      :y1="connection.y1"
                      :x2="connection.x2"
                      :y2="connection.y2"
                      stroke="#6366f1"
                      stroke-width="2"
                      marker-end="url(#arrowhead-personal)"
                    />
                  </template>
                </svg>
              
              <!-- 渲染拓扑层级 -->
              <div class="relative" style="z-index: 1; min-height: 400px;">
                <div
                  v-for="(level, levelIndex) in dependencyLevels"
                  :key="levelIndex"
                  class="flex gap-4 mb-8 last:mb-0"
                  :style="getLevelStyle(level, levelIndex)"
                >
                  <div
                    v-for="task in level"
                    :key="task.id"
                    :style="getTaskStyle(task, level, levelIndex)"
                  >
                    <div
                      class="flex items-center gap-2 px-4 py-3 rounded-lg border transition-colors shadow-sm w-[240px] cursor-pointer hover:shadow-md hover:border-primary/30 transition-all"
                      :class="'bg-white border-muted'"
                      :title="task.title"
                      @click="openView(task)"
                    >
                      <Badge :variant="statusBadgeVariant[task.status] ?? 'secondary'" class="text-xs shrink-0">
                        {{ statusLabel[task.status] ?? task.status }}
                      </Badge>
                      <span class="font-medium text-sm truncate flex-1 min-w-0">{{ task.title }}</span>
                      <span
                        v-if="task.priority"
                        class="px-2 py-0.5 text-xs font-medium rounded-md border shrink-0"
                        :class="priorityStyle[task.priority] ?? ''"
                      >
                        {{ priorityLabel[task.priority] ?? task.priority }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
              </div>
            </div>
            
            <!-- 孤立任务（无依赖关系的任务） -->
            <div v-if="isolatedTasks.length > 0" class="mt-8 pt-6 border-t">
              <h3 class="text-sm font-medium text-muted-foreground mb-4">孤立任务</h3>
              <div class="flex flex-wrap gap-4">
                <div
                  v-for="task in isolatedTasks"
                  :key="task.id"
                  class="flex items-center gap-2 px-4 py-3 rounded-lg border bg-muted/20 shadow-sm"
                >
                  <Badge :variant="statusBadgeVariant[task.status] ?? 'secondary'" class="text-xs">
                    {{ statusLabel[task.status] ?? task.status }}
                  </Badge>
                  <span class="font-medium text-sm">{{ task.title }}</span>
                  <span
                    v-if="task.priority"
                    class="px-2 py-0.5 text-xs font-medium rounded-md border"
                    :class="priorityStyle[task.priority] ?? ''"
                  >
                    {{ priorityLabel[task.priority] ?? task.priority }}
                  </span>
                </div>
               
              </div>
            </div>
          </div>
          <div v-else class="text-center py-8 text-muted-foreground">
            暂无个人任务
          </div>
        </CardContent>
      </Card>
    </div>

    <!-- 查看详情弹窗 -->
    <Dialog v-model:open="isViewOpen">
      <DialogContent>
        <TaskDetailDialog 
          v-if="selectedTask" 
          :task="selectedTask" 
          @close="isViewOpen = false" 
        />
      </DialogContent>
    </Dialog>
  </BasicPage>
</template>

<route lang="yaml">
meta:
  auth: true
</route>