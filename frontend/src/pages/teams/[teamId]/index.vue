<script setup lang="ts">
import { readDeletedTasks, writeDeletedTasks } from '@/utils/deleted-task-cache'
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Tabs,
  TabsList,
  TabsTrigger,
} from '@/components/ui/tabs'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Plus, Eye, Pencil, Trash2, UserPlus, Link2, X, AlertCircle, ChevronDown, UserX, LogOut, AlertTriangle, ArrowRight , User, RefreshCw } from 'lucide-vue-next'
import type { Row } from '@tanstack/vue-table'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { toast } from 'vue-sonner'
import { teamApi } from '@/api/team'
import { teamTaskApi } from '@/api/team-tasks'
import { withTaskDependencies } from '@/api/task-dependency-utils'
import type { DependencyAwareTeamTaskResponse } from '@/api/task-types'
import type { TeamTaskResponse, TeamMemberResponse, TeamRole , TaskOperationLog, TaskOperationType } from '@/api/types'
import { getDisplayDependents } from '@/utils/dependency-graph'
import TaskEditorFields from '@/components/task/task-editor-fields.vue'
import TeamTaskRowActions from './components/team-task-row-actions.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const queryClient = useQueryClient()
const teamId = computed(() => parseInt((route.params as any).teamId as string))
const activeTab = ref('tasks')
const isTeamDisbanded = computed(() => team.value?.status === 'DISBANDED')
// 依赖关系展开状态管理
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
// 获取根节点任务（无前置任务的任务）
function getRootTasks(): TeamTaskResponse[] {
  return tasks.value?.filter(task => !task.dependencies || task.dependencies.length === 0) || []
}
// 拓扑图层级数据
interface LevelTask extends TeamTaskResponse {
  isConverge?: boolean
}
 
// 获取拓扑图层级
const dependencyLevels = computed(() => {
  const allTasksData = tasks.value || []
  const taskMap = new Map(allTasksData.map(t => [t.id, t]))
  const visited = new Set<number>()
  const levels: LevelTask[][] = []
  
  // 找出汇合点（有多个前置任务的任务）
  const taskToPredecessors = new Map<number, number[]>()
  allTasksData.forEach(task => {
    task.dependencies?.forEach(depId => {
      if (!taskToPredecessors.has(task.id)) {
        taskToPredecessors.set(task.id, [])
      }
      taskToPredecessors.get(task.id)!.push(depId)
    })
  })
  
  // 获取节点层级（最长路径长度）
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
  allTasksData.forEach(task => {
    if ((task.dependencies?.length || 0) > 0 || (task.dependents?.length || 0) > 0) {
      const level = getLevel(task.id)
      if (!levelMap.has(level)) {
        levelMap.set(level, [])
      }
      const isConverge = (taskToPredecessors.get(task.id)?.length || 0) > 1
      levelMap.get(level)!.push({ ...task, isConverge })
      visited.add(task.id)
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

// 获取箭头连接数据
const dependencyConnections = computed(() => {
  const allTasksData = tasks.value || []
  const taskMap = new Map(allTasksData.map(t => [t.id, t]))
  const connections: { x1: number; y1: number; x2: number; y2: number }[] = []
  
  // 获取节点位置
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
  allTasksData.forEach(task => {
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
 
// 获取孤立任务（无任何依赖关系的任务）
const isolatedTasks = computed(() => {
  const allTasksData = tasks.value || []
  return allTasksData.filter(task => 
    (!task.dependencies || task.dependencies.length === 0) && 
    (!task.dependents || task.dependents.length === 0)
  )
})

const canManageTasks = computed(() => ['Owner', 'Admin'].includes(team.value?.currentUserRole ?? ''))
const canUpdateAssignedTaskStatus = (task: TeamTaskResponse) =>
  task.assigneeId === authStore.userId
const UNASSIGNED_VALUE = '__UNASSIGNED__'
const teamQueryKey = computed(() => ['team', teamId.value, authStore.userId])
const teamMembersQueryKey = computed(() => ['team-members', teamId.value, authStore.userId])
const teamTasksQueryKey = computed(() => ['team-tasks', teamId.value, authStore.userId])

// 获取团队详情
const { data: team, isLoading, error } = useQuery({
  queryKey: teamQueryKey,
  queryFn: () => teamApi.getTeam(teamId.value),
  enabled: computed(() => teamId.value > 0 && authStore.isLogin),
})
// 添加任务依赖mutation
const addDependencyMutation = useMutation({
  mutationFn: ({ taskId, dependencyId }: { taskId: number; dependencyId: number }) =>
    teamTaskApi.addTaskDependency(teamId.value, taskId, dependencyId),
  onSuccess: () => {
    toast.success('依赖关系添加成功')
    queryClient.invalidateQueries({ queryKey: teamTasksQueryKey.value })
  },
  onError: (error: any) => {
    toast.error('添加依赖失败: ' + (error.response?.data?.message || error.message))
  }
})
const isAddingDependency = computed(() => addDependencyMutation.isPending.value)
// 删除任务依赖mutation
const removeDependencyMutation = useMutation({
  mutationFn: ({ taskId, dependencyId }: { taskId: number; dependencyId: number }) =>
    teamTaskApi.removeTaskDependency(teamId.value, taskId, dependencyId),
  onSuccess: () => {
    toast.success('依赖关系已删除')
    queryClient.invalidateQueries({ queryKey: teamTasksQueryKey.value })
  },
  onError: (error: any) => {
    toast.error('删除依赖失败: ' + (error.response?.data?.message || error.message))
  }
})
const isRemovingDependency = computed(() => removeDependencyMutation.isPending.value)
// 获取团队成员
const { data: members } = useQuery({
  queryKey: teamMembersQueryKey,
  queryFn: () => teamApi.getTeamMembers(teamId.value),
  enabled: computed(() => teamId.value > 0 && authStore.isLogin),
})

// 获取团队任务
const { data: tasks, isLoading: tasksLoading, error: tasksError, refetch: refetchTasks } = useQuery<DependencyAwareTeamTaskResponse[]>({
  queryKey: teamTasksQueryKey,
  queryFn: async () => {
    const teamTasks = await teamTaskApi.getTeamTasks(teamId.value)
    const withDeps = await withTaskDependencies(teamTasks)
    return [...withDeps, ...deletedTeamTasks.value] as DependencyAwareTeamTaskResponse[]
  },
   enabled: computed(() => teamId.value > 0 && authStore.isLogin && !(isTeamDisbanded.value && !canManageTasks.value)),
})

function getErrorMessage(error: any, fallback: string) {
  return error?.response?.data?.message || error?.message || fallback
}
 
// 创建团队任务mutation
const createTaskMutation = useMutation({
  mutationFn: async ({
    data,
    assigneeId,
  }: {
    data: { title: string; description?: string; status?: string; priority?: string; deadline?: string }
    assigneeId: number | null
  }) => {
    const createdTask = await teamTaskApi.createTeamTask(teamId.value, data)
    if (assigneeId !== null)
      await teamTaskApi.assignTeamTask(teamId.value, createdTask.id, assigneeId)
    return createdTask
  },
  onSuccess: () => {
    toast.success('任务创建成功')
    queryClient.invalidateQueries({ queryKey: teamTasksQueryKey.value })
    isCreateOpen.value = false
  },
  onError: (error: any) => {
    toast.error('任务创建失败: ' + (error.response?.data?.message || error.message))
  }
})

const isCreatingTask = computed(() => createTaskMutation.isPending.value)
 
// 更新团队任务mutation
const updateTaskMutation = useMutation({
  mutationFn: async ({
    taskId,
    data,
    assigneeId,
  }: {
    taskId: number
    data: any
    assigneeId: number | null
  }) => {
    const updatedTask = await teamTaskApi.updateTeamTask(teamId.value, taskId, data)
    await teamTaskApi.assignTeamTask(teamId.value, taskId, assigneeId as number)
    return updatedTask
  },
  onSuccess: () => {
    toast.success('任务更新成功')
    queryClient.invalidateQueries({ queryKey: teamTasksQueryKey.value })
    isEditOpen.value = false
  },
  onError: (error: any) => {
    toast.error('任务更新失败: ' + (error.response?.data?.message || error.message))
  }
})

const isUpdatingTask = computed(() => updateTaskMutation.isPending.value)

const updateTaskStatusMutation = useMutation({
  mutationFn: ({ taskId, status }: { taskId: number; status: string }) =>
    teamTaskApi.updateTeamTaskStatus(teamId.value, taskId, status),
  onSuccess: () => {
    toast.success('任务状态更新成功')
    queryClient.invalidateQueries({ queryKey: teamTasksQueryKey.value })
    isEditOpen.value = false
  },
  onError: (error: any) => {
    toast.error(`任务状态更新失败: ${error.response?.data?.message || error.message}`)
  },
})

const isStatusUpdating = computed(() => updateTaskStatusMutation.isPending.value)
 
// 删除团队任务mutation
const deleteTaskMutation = useMutation({
  mutationFn: async (taskId: number) => {
    const cacheUserId = authStore.userId
    const cacheTeamId = teamId.value
    const task = tasks.value?.find(t => t.id === taskId)
    let logs: TaskOperationLog[] = []
    try {
      logs = await teamTaskApi.getTeamTaskLogs(teamId.value, taskId)
    } catch {
      // 获取日志失败不影响删除操作
    }
    if (cacheUserId !== authStore.userId || cacheTeamId !== teamId.value) throw new Error('Session changed')
    await teamTaskApi.deleteTeamTask(cacheTeamId, taskId)
    return { task, logs, cacheUserId, cacheTeamId }
  },
  onSuccess: ({ task, logs, cacheUserId, cacheTeamId }) => {
    if (cacheUserId !== authStore.userId || cacheTeamId !== teamId.value) return
    if (task) {
      const deletedTask: DeletedTeamTask = {
        ...task,
        logs,
        deletedAt: new Date().toISOString()
      }
      deletedTeamTasks.value.unshift(deletedTask)
      saveDeletedTeamTasks()
    }
    toast.success('任务删除成功')
    queryClient.invalidateQueries({ queryKey: teamTasksQueryKey.value })
    isDeleteOpen.value = false
  },
  onError: (error: any) => {
    toast.error('任务删除失败: ' + (error.response?.data?.message || error.message))
  }
})
// 分配团队任务mutation
const assignTaskMutation = useMutation({
  mutationFn: ({ taskId, assigneeId }: { taskId: number; assigneeId: number | null }) =>
    teamTaskApi.assignTeamTask(teamId.value, taskId, assigneeId),
  onSuccess: () => {
    toast.success('任务分配成功')
    queryClient.invalidateQueries({ queryKey: teamTasksQueryKey.value })
    queryClient.invalidateQueries({ queryKey: teamMembersQueryKey.value })
    isAssignOpen.value = false
  },
  onError: (error: any) => {
    toast.error('任务分配失败: ' + (error.response?.data?.message || error.message))
  }
})
onMounted(() => {
  activeTab.value = `tasks`
})

function switchTab(tab: string) {
  activeTab.value = tab
}

// 任务弹窗状态
const isCreateOpen = ref(false)
const isViewOpen = ref(false)
const isEditOpen = ref(false)
const isDeleteOpen = ref(false)
const selectedTask = ref<TeamTaskResponse | null>(null)
// 新建任务表单数据
const newTaskTitle = ref('')
const newTaskDescription = ref('')
const newTaskStatus = ref('TODO')
const newTaskPriority = ref('medium')
const newTaskDeadline = ref('')
const newTaskAssigneeId = ref(UNASSIGNED_VALUE)
// 编辑任务表单数据
const editTaskTitle = ref('')
const editTaskDescription = ref('')
const editTaskStatus = ref('TODO')
const editTaskPriority = ref('medium')
const editTaskDeadline = ref('')
// 分配成员数据
const assigneeId = ref(UNASSIGNED_VALUE)
// 依赖管理数据
const selectedDependencyTaskId = ref(UNASSIGNED_VALUE)
// 待删除的依赖（暂存，点击确认后才真正删除）
const pendingRemovedDependencies = ref<number[]>([])
// 待添加的依赖（暂存，点击确认后才真正添加）
const pendingAddedDependencies = ref<number[]>([])
// 获取任务详情（包含依赖关系）
const getTaskDependenciesDetail = (task: TeamTaskResponse) => {
  const dependencies = task.dependencies?.map(depId => 
    tasks.value?.find(t => t.id === depId)
  ).filter(Boolean) as TeamTaskResponse[] || []
  const dependents = task.dependents?.map(depId => 
    tasks.value?.find(t => t.id === depId)
  ).filter(Boolean) as TeamTaskResponse[] || []
  return { dependencies, dependents }
}

// 操作日志相关
const taskLogs = ref<TaskOperationLog[]>([])
const loadingLogs = ref(false)
// 操作类型标签映射
const operationLabels: Record<TaskOperationType, string> = {
  CREATE_TASK: '创建任务',
  UPDATE_TASK_INFO: '修改任务基本信息',
  UPDATE_TASK_STATUS: '修改任务状态',
  UPDATE_ASSIGNEE: '修改负责人',
  ADD_DEPENDENCY: '添加依赖',
  REMOVE_DEPENDENCY: '移除依赖',
  DELETE_TASK: '删除任务',
  MEMBER_LEAVE_TASK_CHANGE: '成员离开导致任务归属变化',
  TEAM_DISBANDED_TASK_CHANGE: '团队解散导致任务状态变化',
}
// 字段名映射（全局变量，供模板使用）
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
  Object.entries(fieldNames).forEach(([key, label]) => {
    result = result.replace(new RegExp(`\\b${key}\\b`, 'gi'), label)
  })
  // 替换状态值（复用已有的 statusLabel）
  Object.entries(statusLabel).forEach(([key, label]) => {
    result = result.replace(new RegExp(`\\b${key}\\b`, 'gi'), label)
  })
  
  // 替换优先级值（复用已有的 priorityLabel）
  Object.entries(priorityLabel).forEach(([key, label]) => {
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
// 格式化字段值用于显示
function formatFieldValue(field: string, value: string): string {
  if (!value) return '(空)'
  if (field === 'status') return statusLabel[value] || value
  if (field === 'priority') return priorityLabel[value.toLowerCase()] || value
  if (field === 'deadline' && value === 'null') return '(无)'
  // 团队状态值
  if (teamStatusLabel[value]) return teamStatusLabel[value]
  return value
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
// 加载日志
async function loadTaskLogs(taskId: number) {
  loadingLogs.value = true
  try {
    taskLogs.value = await teamTaskApi.getTeamTaskLogs(teamId.value, taskId)
    // 按时间倒序排序
    taskLogs.value.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
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

const getMindmapDependents = (task: TeamTaskResponse): TeamTaskResponse[] => {
  return getDisplayDependents(task, tasks.value || [])
}
// 检查任务是否有未完成的前置任务
const hasUnfinishedDependencies = (task: TeamTaskResponse): { hasUnfinished: boolean; unfinishedTasks: TeamTaskResponse[] } => {
  const unfinishedTasks = (task.dependencies?.map(depId => 
    tasks.value?.find(t => t.id === depId)
  ).filter(t => t && t.status !== 'DONE') as TeamTaskResponse[]) || []
  return { hasUnfinished: unfinishedTasks.length > 0, unfinishedTasks }
}
// 检查任务是否被其他任务依赖
const hasDependents = (task: TeamTaskResponse): boolean => {
  return (task.dependents?.length || 0) > 0
}
// 获取任务的所有后继任务（递归获取，用于避免循环依赖）
function getTaskDependents(taskId: number, visited: Set<number> = new Set()): number[] {
  if (visited.has(taskId)) return []
  visited.add(taskId)
  const allTasks = tasks.value || []
  const dependents = allTasks.filter(t => t.dependencies?.includes(taskId))
  const dependentIds = dependents.map(t => t.id)
  // 递归获取后继任务的后继任务
  for (const depId of dependentIds) {
    const nestedDependents = getTaskDependents(depId, visited)
    dependentIds.push(...nestedDependents)
  }
  return [...new Set(dependentIds)]
}
// 获取总依赖关系数
const getTotalDependencies = (): number => {
  return tasks.value?.reduce((total, task) => total + (task.dependencies?.length || 0), 0) || 0
}
// 获取有依赖的任务列表
const getTasksWithDependencies = (): TeamTaskResponse[] => {
  return tasks.value?.filter(task => (task.dependencies?.length || 0) > 0) || []
}

const formatAssigneeLabel = (task: TeamTaskResponse) => {
  if (task.assigneeName)
    return task.assigneeName
  if (task.formerAssigneeName)
    return `未分配（原负责人：${task.formerAssigneeName}）`
  return '未分配'
}

function normalizePriority(priority?: string | null) {
  return (priority || 'medium').toLowerCase()
}

function parseAssigneeId(value: string) {
  if (!value || value === UNASSIGNED_VALUE)
    return null
  const parsed = Number.parseInt(value, 10)
  return Number.isNaN(parsed) ? null : parsed
}

function openView(task: TeamTaskResponse) {
  selectedTask.value = task
  editTaskTitle.value = task.title
  editTaskDescription.value = task.description || ''
  editTaskStatus.value = task.status
  editTaskPriority.value = normalizePriority(task.priority)
  editTaskDeadline.value = task.deadline || ''
  assigneeId.value = task.assigneeId ? String(task.assigneeId) : UNASSIGNED_VALUE
  isViewOpen.value = true
  loadTaskLogs(task.id)
}

function openEdit(task: TeamTaskResponse) {
  if (!canManageTasks.value && !canUpdateAssignedTaskStatus(task)) {
    toast.error('仅 Owner/Admin 可编辑任务，负责人仅可修改状态')
    return
  }
  // 重置待删除和待添加依赖列表
  pendingRemovedDependencies.value = []
  pendingAddedDependencies.value = []
  selectedTask.value = task
  editTaskTitle.value = task.title
  editTaskDescription.value = task.description || ''
  editTaskStatus.value = task.status
  editTaskPriority.value = normalizePriority(task.priority)
  editTaskDeadline.value = task.deadline || ''
  assigneeId.value = task.assigneeId ? String(task.assigneeId) : UNASSIGNED_VALUE
  isEditOpen.value = true
}
function openDelete(task: TeamTaskResponse) {
  if (!canManageTasks.value) {
    toast.error('仅 Owner 或 Admin 可删除团队任务')
    return
  }
  selectedTask.value = task
  isDeleteOpen.value = true
}
function openCreate() {
  if (!canManageTasks.value) {
    toast.error('仅 Owner 或 Admin 可创建团队任务')
    return
  }
  createTaskMutation.reset()
  newTaskTitle.value = ''
  newTaskDescription.value = ''
  newTaskStatus.value = 'TODO'
  newTaskPriority.value = 'medium'
  newTaskDeadline.value = ''
  newTaskAssigneeId.value = UNASSIGNED_VALUE
  isCreateOpen.value = true
}

function handleCreateTask() {
  if (!canManageTasks.value) {
    toast.error('仅 Owner 或 Admin 可创建团队任务')
    return
  }

  createTaskMutation.mutate({
    data: {
      title: newTaskTitle.value.trim(),
      description: newTaskDescription.value || undefined,
      status: newTaskStatus.value,
      priority: normalizePriority(newTaskPriority.value),
      deadline: newTaskDeadline.value || undefined,
    },
    assigneeId: parseAssigneeId(newTaskAssigneeId.value),
  })
}

async function handleEdit() {
  if (!selectedTask.value) return
  // 如果是更新状态为DONE，检查前置任务是否都已完成
  if (editTaskStatus.value === 'DONE') {
    const { hasUnfinished, unfinishedTasks } = hasUnfinishedDependencies(selectedTask.value)
    if (hasUnfinished) {
      const taskTitles = unfinishedTasks.map(t => t.title).join(', ')
      toast.error(`无法设为已完成：以下前置任务尚未完成：${taskTitles}`)
      return
    }
  }
  
  // 先批量删除待删除的依赖
  for (const depId of pendingRemovedDependencies.value) {
    try {
      await removeDependencyMutation.mutateAsync({ taskId: selectedTask.value!.id, dependencyId: depId })
    } catch (error) {
      // 单个依赖删除失败不中断其他操作
    }
  }
  pendingRemovedDependencies.value = []
  
  // 再批量添加待添加的依赖
  for (const depId of pendingAddedDependencies.value) {
    try {
      await addDependencyMutation.mutateAsync({ taskId: selectedTask.value!.id, dependencyId: depId })
    } catch (error) {
      // 单个依赖添加失败不中断其他操作
    }
  }
  pendingAddedDependencies.value = []

  if (!canManageTasks.value) {
    if (!canUpdateAssignedTaskStatus(selectedTask.value)) {
      toast.error('仅负责人可修改任务状态')
      return
    }

    updateTaskStatusMutation.mutate({
      taskId: selectedTask.value.id,
      status: editTaskStatus.value,
    })
    return
  }

  updateTaskMutation.mutate({
    taskId: selectedTask.value.id,
    data: {
      title: editTaskTitle.value.trim(),
      description: editTaskDescription.value || undefined,
      status: editTaskStatus.value,
      priority: normalizePriority(editTaskPriority.value),
      deadline: editTaskDeadline.value || undefined,
    },
    assigneeId: parseAssigneeId(assigneeId.value),
  })
}

function handleDelete() {
  if (!canManageTasks.value) {
    toast.error('仅 Owner 或 Admin 可删除团队任务')
    return
  }
  if (!selectedTask.value) return
  // 检查是否有后继任务依赖
  if (hasDependents(selectedTask.value)) {
    const dependentTitles = getTaskDependenciesDetail(selectedTask.value).dependents.map(t => t.title).join(', ')
    toast.error(`无法删除：此任务被以下后继任务依赖：${dependentTitles}`)
    return
  }
  deleteTaskMutation.mutate(selectedTask.value.id)
}
function handleAddDependency() {
  if (!selectedTask.value) return
  const dependencyId = parseAssigneeId(selectedDependencyTaskId.value)
  if (dependencyId === null) {
    toast.error('请选择一个任务作为前置任务')
    return
  }
  if (dependencyId === selectedTask.value.id) {
    toast.error('不能依赖自己')
    return
  }
  // 检查是否已存在该依赖（包括待添加的）
  const existingDeps = [...(selectedTask.value.dependencies || []), ...pendingAddedDependencies.value]
  if (existingDeps.includes(dependencyId)) {
    toast.error('该依赖已存在')
    return
  }
  // 添加到待添加列表
  pendingAddedDependencies.value.push(dependencyId)
  selectedDependencyTaskId.value = UNASSIGNED_VALUE
}
function handleRemoveDependency(dependencyId: number) {
  if (!selectedTask.value) return
  // 将依赖添加到待删除列表（不在此发送API请求）
  if (!pendingRemovedDependencies.value.includes(dependencyId)) {
    pendingRemovedDependencies.value.push(dependencyId)
  }
}

const statusBadgeVariant: Record<string, 'default' | 'secondary' | 'outline'> = {
  TODO: 'secondary',
  IN_PROGRESS: 'default',
  DONE: 'outline',
}

const statusLabel: Record<string, string> = {
  TODO: '待办',
  IN_PROGRESS: '进行中',
  DONE: '已完成',
}

const priorityLabel: Record<string, string> = {
  low: '不紧急不重要',
  medium: '重要不紧急',
  high: '紧急不重要',
  urgent: '紧急重要',
}

const priorityStyle: Record<string, string> = {
  low: 'bg-green-400 text-white',
  medium: 'bg-yellow-400 text-white',
  high: 'bg-orange-400 text-white',
  urgent: 'bg-red-500 text-white',
}

// 已删除任务缓存
interface DeletedTeamTask extends TeamTaskResponse {
  deletedAt: string
  logs?: TaskOperationLog[]
}

const deletedTeamTasks = ref<DeletedTeamTask[]>([])
watch([() => authStore.userId, teamId], ([userId, id]) => {
  deletedTeamTasks.value = readDeletedTasks<DeletedTeamTask>(userId, `team:${id}`)
}, { immediate: true, flush: 'sync' })

function saveDeletedTeamTasks() {
  writeDeletedTasks(authStore.userId, `team:${teamId.value}`, deletedTeamTasks.value)
}

function handleRemoveDeletedTeamTask(task: TeamTaskResponse) {
  deletedTeamTasks.value = deletedTeamTasks.value.filter(t => t.id !== task.id)
  saveDeletedTeamTasks()
  queryClient.invalidateQueries({ queryKey: teamTasksQueryKey.value })
}

const roleBadgeVariant: Record<string, 'default' | 'secondary' | 'outline'> = {
  Owner: 'default',
  Admin: 'secondary',
  Member: 'outline',
}
// ===== 成员管理相关 =====
const canManageMembers = computed(() => team.value?.currentUserRole === 'Owner')
const isOwner = computed(() => team.value?.currentUserRole === 'Owner')
const isMemberOrAdmin = computed(() => ['Member', 'Admin'].includes(team.value?.currentUserRole || ''))
// 添加成员mutation
const addMemberMutation = useMutation({
  mutationFn: ({ userIdentifier, role }: { userIdentifier: string; role: Exclude<TeamRole, 'Owner'> }) =>
    teamApi.addTeamMember(teamId.value, userIdentifier, role),
  onSuccess: () => {
    toast.success('成员添加成功')
    queryClient.invalidateQueries({ queryKey: teamMembersQueryKey.value })
    queryClient.invalidateQueries({ queryKey: teamQueryKey.value })
    isAddOpen.value = false
  },
  onError: (error: any) => {
    toast.error(`添加成员失败: ${error.response?.data?.message || error.message}`)
  },
})
// 更新角色mutation
const updateRoleMutation = useMutation({
  mutationFn: ({ memberId, role }: { memberId: number; role: Exclude<TeamRole, 'Owner'> }) =>
    teamApi.updateMemberRole(teamId.value, memberId, role),
  onSuccess: () => {
    toast.success('成员角色修改成功')
    queryClient.invalidateQueries({ queryKey: teamMembersQueryKey.value })
    isRoleOpen.value = false
  },
  onError: (error: any) => {
    toast.error(`修改角色失败: ${error.response?.data?.message || error.message}`)
  },
})
// 移除成员mutation
const removeMemberMutation = useMutation({
  mutationFn: (memberId: number) => teamApi.removeTeamMember(teamId.value, memberId),
  onSuccess: () => {
    toast.success('成员移除成功')
    queryClient.invalidateQueries({ queryKey: teamMembersQueryKey.value })
    queryClient.invalidateQueries({ queryKey: teamQueryKey.value })
    queryClient.invalidateQueries({ queryKey: teamTasksQueryKey.value })
    isRemoveOpen.value = false
  },
  onError: (error: any) => {
    toast.error(`移除成员失败: ${error.response?.data?.message || error.message}`)
  },
})
// 离开团队mutation
const leaveTeamMutation = useMutation({
  mutationFn: () => teamApi.leaveTeam(teamId.value),
  onSuccess: () => {
    toast.success('已成功离开团队')
    router.push('/teams')
  },
  onError: (error: any) => {
    toast.error(`离开团队失败: ${error.response?.data?.message || error.message}`)
  },
})
// 转让所有权 mutation
const transferOwnershipMutation = useMutation({
  mutationFn: (newOwnerId: number) => teamApi.transferOwnership(teamId.value, newOwnerId),
  onSuccess: () => {
    toast.success('所有权转让成功，如需退出团队请再执行离开团队')
    isOwnerLeaveOpen.value = false
    selectedNewOwnerId.value = UNASSIGNED_VALUE
    queryClient.invalidateQueries({ queryKey: teamMembersQueryKey.value })
    queryClient.invalidateQueries({ queryKey: teamQueryKey.value })
    queryClient.invalidateQueries({ queryKey: ['teams', authStore.userId] })
  },
  onError: (error: any) => {
    toast.error(`转让所有权失败: ${error.response?.data?.message || error.message}`)
  },
})
// 可转让的成员列表（排除Owner自己）
const transferableMembers = computed(() => {
  return (members.value || []).filter(m => m.role !== 'Owner')
})
// 解散团队mutation
const disbandTeamMutation = useMutation({
  mutationFn: () => teamApi.disbandTeam(teamId.value),
  onSuccess: () => {
    toast.success('团队已解散')
    queryClient.invalidateQueries({ queryKey: teamQueryKey.value })
    queryClient.invalidateQueries({ queryKey: ['teams', authStore.userId] })
    isDisbandOpen.value = false
    router.push('/teams')
  },
  onError: (error: any) => {
    toast.error(`解散团队失败: ${error.response?.data?.message || error.message}`)
    isDisbandOpen.value = false
  },
})
// 成员管理弹窗状态
const isAddOpen = ref(false)
const isRoleOpen = ref(false)
const isRemoveOpen = ref(false)
const isLeaveOpen = ref(false)
const isOwnerLeaveOpen = ref(false)
const isDisbandOpen = ref(false)
const isAssignOpen = ref(false)
const selectedMember = ref<TeamMemberResponse | null>(null)
const removeTarget = ref<TeamMemberResponse | null>(null)
const newMemberUserId = ref('')
const newMemberRole = ref<Exclude<TeamRole, 'Owner'>>('Member')
const changeRole = ref<Exclude<TeamRole, 'Owner'>>('Member')
const selectedNewOwnerId = ref(UNASSIGNED_VALUE)
function openAdd() {
  newMemberUserId.value = ''
  newMemberRole.value = 'Member'
  isAddOpen.value = true
}
function openRoleChange(member: TeamMemberResponse) {
  selectedMember.value = member
  changeRole.value = member.role === 'Owner' ? 'Member' : member.role
  isRoleOpen.value = true
}
function openRemove(member: TeamMemberResponse) {
  removeTarget.value = member
  isRemoveOpen.value = true
}
function handleAdd() {
  if (!canManageMembers.value) {
    toast.error('仅 Owner 可管理成员')
    return
  }
  const userIdentifier = newMemberUserId.value.trim()
  if (!userIdentifier) {
    toast.error('请输入用户ID或用户名')
    return
  }
  addMemberMutation.mutate({ userIdentifier, role: newMemberRole.value })
}
function handleRoleChange() {
  if (!canManageMembers.value) {
    toast.error('仅 Owner 可管理成员')
    return
  }
  if (!selectedMember.value) {
    return
  }
  updateRoleMutation.mutate({
    memberId: selectedMember.value.userId,
    role: changeRole.value,
  })
}
function handleRemove() {
  if (!canManageMembers.value) {
    toast.error('仅 Owner 可管理成员')
    return
  }
  if (!removeTarget.value) {
    return
  }
  removeMemberMutation.mutate(removeTarget.value.userId)
}
function openLeave() {
  if (isOwner.value) {
    isOwnerLeaveOpen.value = true
  } else {
    isLeaveOpen.value = true
  }
}
function openOwnerLeave() {
  isOwnerLeaveOpen.value = true
}
function handleCancelOwnerLeave() {
  isOwnerLeaveOpen.value = false
  isDisbandOpen.value = true
}
function handleTransferOwnership() {
  if (selectedNewOwnerId.value === UNASSIGNED_VALUE) {
    toast.error('请选择新的团队负责人')
    return
  }
  transferOwnershipMutation.mutate(Number(selectedNewOwnerId.value))
}
function handleLeave() {
  leaveTeamMutation.mutate()
}
function openDisband() {
  isDisbandOpen.value = true
}
function handleDisband() {
  disbandTeamMutation.mutate()
}
</script>

<template>
  <!-- 加载状态 -->
  <div v-if="isLoading" class="flex justify-center py-12">
    <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
  </div>
  <!-- 错误状态 -->
  <div v-else-if="error" class="text-center py-12">
    <p class="text-destructive">加载团队详情失败</p>
    <Button variant="outline" class="mt-2" @click="queryClient.invalidateQueries({ queryKey: teamQueryKey })">
      重试
    </Button>
  </div>
  <!-- 团队信息头部 -->
  <div v-else-if="team">
    <Card class="mb-4">
      <CardHeader>
        <CardTitle class="text-xl">{{ team.name }}</CardTitle>
      </CardHeader>
      <CardContent class="flex items-center gap-3">
        <Badge :variant="roleBadgeVariant[team.currentUserRole] ?? 'outline'">
          {{ team.currentUserRole || '-' }}
        </Badge>
        <span class="text-sm text-muted-foreground">
          创建者：{{ team.ownerUsername }}
        </span>
        <span class="text-sm text-muted-foreground">
          成员数：{{ team.memberCount }}
        </span>
        <span class="text-sm text-muted-foreground">
          创建时间：{{ new Date(team.createdAt).toLocaleDateString() }}
        </span>
      </CardContent>
    </Card>
  </div>
  <!-- 团队已解散警告 -->
  <div v-if="isTeamDisbanded" class="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-3">
    <AlertTriangle class="h-5 w-5 text-red-500" />
    <div>
      <p class="font-medium text-red-700">团队已解散</p>
      <p class="text-sm text-red-600">您只能查看历史信息，无法进行任何操作</p>
    </div>
  </div>
  <!-- 标签栏 -->
  <Tabs v-model="activeTab" class="mb-4">
    <TabsList>
      <TabsTrigger value="tasks">
        团队任务
      </TabsTrigger>
      <TabsTrigger value="dependencies">
        任务依赖
      </TabsTrigger>
      <TabsTrigger 
        value="members">
        成员管理
      </TabsTrigger>
    </TabsList>
  </Tabs>

  <div v-if="tasksError" class="mb-4 rounded-md border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
    {{ getErrorMessage(tasksError, '加载团队任务失败') }}
  </div>
 <!-- 成员管理视图 -->
  <div
    v-if="activeTab === 'members'"
    class="transition-all duration-300 ease-out animate-in fade-in slide-in-from-top-2"
  >
    <!-- 操作按钮 -->
    <div class="mb-4 flex gap-3">
      <Button v-if="!isTeamDisbanded" @click="openAdd" :disabled="!canManageMembers">
        <Plus class="mr-2 h-4 w-4" />
        添加成员
      </Button>
      <Button v-if="isMemberOrAdmin && !isTeamDisbanded" variant="outline" @click="openLeave">
        <LogOut class="mr-2 h-4 w-4" />
        离开团队
      </Button>
      <Button v-if="isOwner && !isTeamDisbanded" variant="outline" @click="openOwnerLeave">
        <LogOut class="mr-2 h-4 w-4" />
        离开团队
      </Button>
      <Button v-if="isOwner && !isTeamDisbanded" variant="destructive" @click="openDisband">
        <Trash2 class="mr-2 h-4 w-4" />
        解散团队
      </Button>
    </div>
    <!-- 成员表格 -->
    <div class="overflow-x-auto rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>用户名</TableHead>
            <TableHead>角色</TableHead>
            <TableHead>加入时间</TableHead>
            <TableHead>操作</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <TableRow v-for="member in (members || [])" :key="member.userId">
            <TableCell class="font-medium">{{ member.username }}</TableCell>
            <TableCell>
              <Badge :variant="roleBadgeVariant[member.role] ?? 'outline'">
                {{ member.role }}
              </Badge>
            </TableCell>
            <TableCell>{{ member.joinedAt ? new Date(member.joinedAt).toLocaleString() : '-' }}</TableCell>
            <TableCell>
              <div class="flex gap-1">
                <Button
                  v-if="!isTeamDisbanded"
                  variant="ghost"
                  size="icon"
                  :disabled="!canManageMembers || member.role === 'Owner'"
                  @click="openRoleChange(member)"
                  title="修改角色"
                >
                  <Pencil class="h-4 w-4" />
                </Button>
                <Button
                  v-if="!isTeamDisbanded"
                  variant="ghost"
                  size="icon"
                  :disabled="!canManageMembers || member.role === 'Owner'"
                  @click="openRemove(member)"
                  title="移除成员"
                >
                  <UserX class="h-4 w-4 text-destructive" />
                </Button>
              </div>
            </TableCell>
          </TableRow>
          <TableRow v-if="!members || members.length === 0">
            <TableCell colspan="4" class="text-center text-muted-foreground py-8">
              暂无团队成员
            </TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </div>
  </div>
  <!-- 添加成员弹窗 -->
  <Dialog v-model:open="isAddOpen">
    <DialogContent>
      <DialogHeader>
        <DialogTitle>添加成员</DialogTitle>
        <DialogDescription>
          选择用户并设置角色后添加到团队
        </DialogDescription>
      </DialogHeader>
      <div class="space-y-4 py-4">
        <div class="grid grid-cols-1 gap-2">
          <Label for="user-id">用户 ID 或用户名</Label>
          <Input
            id="user-id"
            v-model="newMemberUserId"
            placeholder=""
          />
        </div>
        <div class="space-y-2">
          <Label>角色</Label>
          <Select v-model="newMemberRole">
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="Admin">Admin</SelectItem>
              <SelectItem value="Member">Member</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>
      <DialogFooter>
        <Button variant="outline" @click="isAddOpen = false">取消</Button>
        <Button @click="handleAdd">确定</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
  <!-- 修改角色弹窗 -->
  <Dialog v-model:open="isRoleOpen">
    <DialogContent>
      <DialogHeader>
        <DialogTitle>修改角色</DialogTitle>
        <DialogDescription>
          修改成员「{{ selectedMember?.username }}」的角色
        </DialogDescription>
      </DialogHeader>
      <div class="space-y-4 py-4">
        <div class="space-y-2">
          <Label>当前角色</Label>
          <p class="text-sm text-muted-foreground">{{ selectedMember?.role }}</p>
        </div>
        <div class="space-y-2">
          <Label>新角色</Label>
          <Select v-model="changeRole">
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="Admin">Admin</SelectItem>
              <SelectItem value="Member">Member</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>
      <DialogFooter>
        <Button variant="outline" @click="isRoleOpen = false">取消</Button>
        <Button @click="handleRoleChange">确定</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
  <!-- 移除成员确认弹窗 -->
  <Dialog v-model:open="isRemoveOpen">
    <DialogContent>
      <DialogHeader>
        <DialogTitle>确认移除</DialogTitle>
        <DialogDescription>
          确定要将「{{ removeTarget?.username }}」移出团队吗？
        </DialogDescription>
      </DialogHeader>
      <DialogFooter>
        <Button variant="outline" @click="isRemoveOpen = false">取消</Button>
        <Button variant="destructive" @click="handleRemove">确认移除</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
  <!-- 离开团队确认弹窗 -->
  <Dialog v-model:open="isLeaveOpen">
    <DialogContent>
      <DialogHeader>
        <DialogTitle>确认离开团队</DialogTitle>
        <DialogDescription>
          确定要离开「{{ team?.name }}」团队吗？
          离开后您将无法访问该团队的任务和成员信息。
        </DialogDescription>
      </DialogHeader>
      <DialogFooter>
        <Button variant="outline" @click="isLeaveOpen = false">取消</Button>
        <Button variant="destructive" @click="handleLeave">确认离开</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
  <!-- Owner 离开团队提示弹窗 -->
  <Dialog v-model:open="isOwnerLeaveOpen">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle class="text-lg font-semibold">
          <AlertTriangle class="inline-block h-5 w-5 mr-2 text-yellow-500" />
          离开团队
        </DialogTitle>
        <DialogDescription class="text-sm text-muted-foreground mt-2">
          作为团队 Owner，您需要先转让所有权或解散团队才能离开。
        </DialogDescription>
      </DialogHeader>
      <div class="py-4 space-y-4">
        <!-- 转让所有权选项 -->
        <div class="space-y-3">
          <div class="flex items-center gap-2">
            <ArrowRight class="h-5 w-5 text-blue-500" />
            <Label class="font-medium">转让所有权</Label>
          </div>
          <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p class="text-sm text-blue-700 mb-3">选择新的团队 owner。转让成功后，您可以再主动离开团队。</p>
            <Select v-model="selectedNewOwnerId">
              <SelectTrigger>
                <SelectValue placeholder="选择新负责人" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem
                  v-for="member in transferableMembers"
                  :key="member.userId"
                  :value="String(member.userId)"
                >
                  {{ member.username }} ({{ member.role }})
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
        <!-- 分割线 -->
        <div class="flex items-center gap-4 py-2">
          <div class="flex-1 h-px bg-gray-200"></div>
          <span class="text-xs text-gray-400">或</span>
          <div class="flex-1 h-px bg-gray-200"></div>
        </div>
        <!-- 解散团队选项 -->
        <div class="flex items-center gap-2">
          <Trash2 class="h-5 w-5 text-red-500" />
          <Label class="font-medium text-red-600">解散团队</Label>
          <p class="text-sm text-red-500">此操作将解散整个团队，所有成员将自动离开</p>
        </div>
      </div>
      <DialogFooter>
        <Button variant="outline" @click="isOwnerLeaveOpen = false">取消</Button>
        <Button 
          variant="default" 
          @click="handleTransferOwnership" 
          :disabled="selectedNewOwnerId === UNASSIGNED_VALUE || transferableMembers.length === 0"
          class="mr-2"
        >
          转让所有权
        </Button>
        <Button variant="destructive" @click="handleCancelOwnerLeave">解散团队</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>

  <!-- 解散团队确认弹窗 -->
  <Dialog v-model:open="isDisbandOpen">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle class="text-lg font-semibold">
          <AlertTriangle class="inline-block h-5 w-5 mr-2 text-red-500" />
          确认解散团队
        </DialogTitle>
        <DialogDescription class="text-sm text-muted-foreground mt-2">
          此操作将解散「{{ team?.name }}」团队，团队状态将变为"已解散"并保留历史数据。
          <br />
          <strong class="text-red-600">请注意：此操作不可撤销！</strong>
        </DialogDescription>
      </DialogHeader>
      <div class="py-4 space-y-2 text-sm text-muted-foreground">
        <p>解散后：</p>
        <ul class="list-disc list-inside space-y-1">
          <li>团队状态将变为"已解散"</li>
          <li>团队任务和成员信息会作为历史数据保留</li>
          <li>普通成员不能继续访问团队任务</li>
          <li>Owner/Admin 只能查看历史，不能继续写入或管理</li>
        </ul>
      </div>
      <DialogFooter>
        <Button variant="outline" @click="isDisbandOpen = false">取消</Button>
        <Button variant="destructive" @click="handleDisband">确认解散</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
  <!-- 依赖关系视图 -->
  <div
    v-if="activeTab === 'dependencies'"
    class="space-y-6 transition-all duration-300 ease-out animate-in fade-in slide-in-from-top-2"
  >
  
    <!-- 任务依赖图 -->
    <Card>
      <CardHeader>
        <CardTitle>任务依赖链</CardTitle>
        <CardDescription>展示任务间的依赖关系</CardDescription>
      </CardHeader>
      <CardContent>

      <div v-if="!tasksError && tasks && tasks.length > 0">
          <!-- 拓扑图容器 -->
          <div class="flex justify-center overflow-x-auto pb-4" style="min-height: 400px;">
            <div class="relative" style="min-width: 800px; min-height: 400px;">
              <svg class="absolute inset-0 w-full h-full pointer-events-none" style="z-index: 0;">
                <defs>
                  <marker
                    id="arrowhead"
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
                    marker-end="url(#arrowhead)"
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
                      :class="task.status === 'DONE' ? 'bg-green-50 border-green-200' : 'bg-white border-muted'"
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
        <div v-else-if="!tasksError" class="text-center py-8 text-muted-foreground">
          暂无任务
        </div>
      </CardContent>
    </Card>
  </div>
 
  <!-- 团队任务视图 -->
  <div
     v-if="activeTab === 'tasks'"
    class="transition-all duration-300 ease-out animate-in fade-in slide-in-from-top-2"
  >
   
    <!-- 创建任务按钮 -->
    <div class="mb-4">
      <Button @click="openCreate" :disabled="!canManageTasks">
        <Plus class="mr-2 h-4 w-4" />
        新建团队任务
      </Button>
    </div>
    <!-- 任务表格 -->
  <div class="overflow-x-auto rounded-md border">
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>标题</TableHead>
          <TableHead>描述</TableHead>
          <TableHead>状态</TableHead>
          <TableHead>优先级</TableHead>
          <TableHead>创建人</TableHead>
          <TableHead>分配给</TableHead>
          <TableHead>截止日期</TableHead>
          <TableHead>创建时间</TableHead>
          <TableHead>操作</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
          <TableRow v-for="task in tasks" :key="task.id" :class="{ 'opacity-60': (task as any).deletedAt }">
            <TableCell class="font-medium" :class="{ 'relative line-through-cell line-through': (task as any).deletedAt }">{{ task.title }}</TableCell>
            <TableCell class="max-w-[200px] truncate text-muted-foreground" :class="{ 'relative line-through-cell line-through': (task as any).deletedAt }">
              {{ task.description || '无描述' }}
            </TableCell>
            <TableCell :class="{ 'relative line-through-cell': (task as any).deletedAt }">
              <Badge v-if="(task as any).deletedAt" variant="destructive" class="bg-red-500 dark:bg-red-500">已删除</Badge>
              <Badge v-else :variant="statusBadgeVariant[task.status] ?? 'secondary'">
                {{ statusLabel[task.status] ?? task.status }}
              </Badge>
            </TableCell>
            <TableCell :class="{ 'relative line-through-cell': (task as any).deletedAt }">
              <span
                class="px-2 py-1 text-xs font-medium rounded-md border"
                :class="priorityStyle[task.priority] ?? ''"
              >
                {{ priorityLabel[task.priority] ?? task.priority }}
              </span>
            </TableCell>
            <TableCell :class="{ 'relative line-through-cell': (task as any).deletedAt }">{{ task.creatorName || '未知' }}</TableCell>
            <TableCell class="text-muted-foreground" :class="{ 'relative line-through-cell': (task as any).deletedAt }">
              {{ formatAssigneeLabel(task) }}
            </TableCell>
            <TableCell :class="{ 'relative line-through-cell': (task as any).deletedAt }">{{ task.deadline ? new Date(task.deadline).toLocaleDateString() : '-' }}</TableCell>
            <TableCell :class="{ 'relative line-through-cell': (task as any).deletedAt }">{{ task.createdAt ? new Date(task.createdAt).toLocaleDateString() : '-' }}</TableCell>
            <TableCell>
              <TeamTaskRowActions
                :row="{ original: task } as Row<TeamTaskResponse>"
                :is-team-disbanded="isTeamDisbanded"
                :can-manage-tasks="canManageTasks"
                :deleted="!!(task as any).deletedAt"
                @view="openView"
                @edit="openEdit"
                @delete="openDelete"
                @removeRecord="handleRemoveDeletedTeamTask"
              />
            </TableCell>
          </TableRow>
          <TableRow v-if="!tasksError && (!tasks || tasks.length === 0)">
            <TableCell colspan="9" class="text-center text-muted-foreground py-8">
              {{ isTeamDisbanded && !canManageTasks ? '团队已解散,普通成员无权访问团队任务' : (tasksLoading ? '加载中...' : '暂无团队任务') }}
            </TableCell>
          </TableRow>
        </TableBody>
    </Table>
  </div>
  </div>
  <!-- 新建任务弹窗 -->
  <Dialog v-model:open="isCreateOpen">
    <DialogContent>
      <DialogHeader>
        <DialogTitle>新建团队任务</DialogTitle>
        <DialogDescription>为团队 {{ team?.name }} 创建新任务</DialogDescription>
      </DialogHeader>
      <TaskEditorFields
        v-model:title="newTaskTitle"
        v-model:description="newTaskDescription"
        v-model:status="newTaskStatus"
        v-model:priority="newTaskPriority"
        v-model:deadline="newTaskDeadline"
        v-model:assignee-id="newTaskAssigneeId"
        title-label="任务标题"
        title-placeholder="请输入任务标题"
        description-placeholder="请输入任务描述"
        :show-assignee="true"
        :members="members || []"
        :unassigned-value="UNASSIGNED_VALUE"
      />
      <DialogFooter>
        <Button variant="outline" @click="isCreateOpen = false">取消</Button>
        <Button 
          @click="handleCreateTask" 
          :disabled="isCreatingTask || !newTaskTitle.trim()"
        >
          {{ isCreatingTask ? '创建中...' : '创建任务' }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
  <!-- 查看详情弹窗 -->
  <Dialog v-model:open="isViewOpen">
  <DialogContent>
    <div class="space-y-6">
      <!-- 基本信息 -->
      <div class="space-y-4">
        <h3 class="text-lg font-semibold">基本信息</h3>
        
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="text-sm font-medium text-muted-foreground">标题</label>
            <p class="text-sm font-medium">{{ selectedTask?.title }}</p>
          </div>
        </div>

        <div>
          <label class="text-sm font-medium text-muted-foreground">描述</label>
          <p class="text-sm mt-1 p-3 bg-muted rounded-md min-h-[80px]">
            {{ selectedTask?.description || '暂无描述' }}
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
              <Badge :variant="statusBadgeVariant[selectedTask?.status || 'TODO'] ?? 'secondary'">
                {{ statusLabel[selectedTask?.status || 'TODO'] ?? (selectedTask?.status || '未知') }}
              </Badge>
            </div>
          </div>
          <div>
            <label class="text-sm font-medium text-muted-foreground">优先级</label>
            <div class="mt-1">
              <span 
                :class="`px-2 py-1 text-xs font-medium rounded-md border ${priorityStyle[selectedTask?.priority || 'medium'] ?? ''}`"
              >
                {{ priorityLabel[selectedTask?.priority || 'medium'] ?? (selectedTask?.priority || '未知') }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 负责人信息 -->
      <div class="space-y-4">
        <h3 class="text-lg font-semibold">负责人信息</h3>
        
        <div class="grid grid-cols-1 gap-3">
          <div>
            <label class="text-sm font-medium text-muted-foreground">创建人</label>
            <p class="text-sm mt-1">{{ selectedTask?.creatorName || '未知' }}</p>
          </div>
          <div>
            <label class="text-sm font-medium text-muted-foreground">负责人</label>
            <p class="text-sm mt-1">
              {{ selectedTask?.assigneeName || '未分配' }}
              <span v-if="selectedTask?.formerAssigneeName && !selectedTask?.assigneeName" class="text-xs text-gray-500 ml-2">
                (原负责人: {{ selectedTask.formerAssigneeName }})
              </span>
            </p>
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
              {{ selectedTask?.deadline ? new Date(selectedTask.deadline).toLocaleDateString() : '未设置' }}
            </p>
          </div>
          <div>
            <label class="text-sm font-medium text-muted-foreground">创建时间</label>
            <p class="text-sm mt-1">{{ selectedTask?.createdAt ? new Date(selectedTask.createdAt).toLocaleDateString() : '-' }}</p>
          </div>
        </div>
      </div>

      <!-- 依赖关系 -->
      <div v-if="selectedTask" class="space-y-4">
        <h3 class="text-lg font-semibold">依赖关系</h3>
        
        <!-- 当前任务依赖的前置任务 -->
        <div>
          <label class="text-sm font-medium text-muted-foreground">当前任务依赖的前置任务</label>
          <div v-if="getTaskDependenciesDetail(selectedTask).dependencies.length > 0" class="mt-1 space-y-1">
            <div
              v-for="dep in getTaskDependenciesDetail(selectedTask).dependencies"
              :key="dep.id"
              class="flex items-center gap-2 text-sm p-2 bg-muted rounded-md"
            >
              <Badge :variant="statusBadgeVariant[dep.status] ?? 'secondary'">
                {{ statusLabel[dep.status] ?? dep.status }}
              </Badge>
              <span>{{ dep.title }}</span>
            </div>
          </div>
          <p v-else class="mt-1 text-sm text-muted-foreground">暂无前置任务</p>
        </div>
        <!-- 依赖当前任务的后继任务 -->
        <div>
          <label class="text-sm font-medium text-muted-foreground">依赖当前任务的后继任务</label>
          <div v-if="getTaskDependenciesDetail(selectedTask).dependents.length > 0" class="mt-1 space-y-1">
            <div
              v-for="dep in getTaskDependenciesDetail(selectedTask).dependents"
              :key="dep.id"
              class="flex items-center gap-2 text-sm p-2 bg-muted rounded-md"
            >
              <Badge :variant="statusBadgeVariant[dep.status] ?? 'secondary'">
                {{ statusLabel[dep.status] ?? dep.status }}
              </Badge>
              <span>{{ dep.title }}</span>
            </div>
          </div>
          <p v-else class="mt-1 text-sm text-muted-foreground">暂无后继任务</p>
        </div>
        <!-- 删除警告 -->
        <div v-if="hasDependents(selectedTask)" class="flex items-center gap-2 p-3 bg-warning/10 border border-warning/30 rounded-md">
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
            @click="selectedTask && loadTaskLogs(selectedTask.id)"
          >
            <RefreshCw class="h-4 w-4 mr-1" />
            刷新
          </Button>
          <div v-else class="h-6 w-6 flex items-center justify-center">
            <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-primary"></div>
          </div>
        </div>
        
        <div v-if="loadingLogs" class="flex justify-center py-4">
          <div class="animate-spin rounded-full h-5 w-5 border-b-2 border-primary"></div>
        </div>
        
        <div v-else-if="taskLogs.length > 0" class="space-y-3 max-h-[300px] overflow-y-auto">
          <div
            v-for="log in taskLogs"
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
      <Button @click="isViewOpen = false">关闭</Button>
    </div>
  </DialogContent>
</Dialog>

  <!-- 编辑任务弹窗 -->
  <Dialog v-model:open="isEditOpen">
    <DialogContent>
      <DialogHeader>
        <DialogTitle>编辑任务</DialogTitle>
        <DialogDescription>修改任务 "{{ selectedTask?.title }}" 的信息</DialogDescription>
      </DialogHeader>
      <div class="space-y-6">
        <TaskEditorFields
          v-model:title="editTaskTitle"
          v-model:description="editTaskDescription"
          v-model:status="editTaskStatus"
          v-model:priority="editTaskPriority"
          v-model:deadline="editTaskDeadline"
          v-model:assignee-id="assigneeId"
          title-placeholder="任务标题"
          description-placeholder="任务描述"
          :show-assignee="true"
          :members="members || []"
          :detail-fields-disabled="!canManageTasks"
          :assignee-disabled="!canManageTasks"
          :unassigned-value="UNASSIGNED_VALUE"
        />
        
        <!-- 依赖管理模块（仅 Owner/Admin） -->
        <div v-if="canManageTasks && selectedTask" class="border-t pt-4">
          <div class="flex items-center justify-between">
            <h4 class="font-medium">任务依赖管理</h4>
            <!-- 删除警告 -->
            <div v-if="hasDependents(selectedTask)" class="flex items-center gap-2 px-3 py-1.5 bg-warning/10 border border-warning/30 rounded-md">
              <AlertCircle class="h-4 w-4 text-warning" />
              <span class="text-xs text-warning">被其他任务依赖</span>
            </div>
          </div>
          
          <!-- 添加前置任务 -->
          <div class="space-y-2 mb-4 mt-3">
            <label class="text-sm font-medium text-muted-foreground">添加前置任务</label>
            <Select v-model="selectedDependencyTaskId">
              <SelectTrigger><SelectValue placeholder="选择前置任务" /></SelectTrigger>
              <SelectContent>
                <template v-if="selectedTask">
                  <template v-if="(tasks || []).filter(t => {
                    if (t.id === selectedTask!.id) return false
                    if (selectedTask!.dependencies?.includes(t.id)) return false
                    // 排除后继任务链中的任务（避免循环依赖）
                    const allDependents = getTaskDependents(selectedTask!.id)
                    return !allDependents.includes(t.id)
                  }).length > 0">
                    <SelectItem :value="UNASSIGNED_VALUE">选择任务</SelectItem>
                    <SelectItem
                      v-for="t in (tasks || []).filter(t => {
                        if (t.id === selectedTask!.id) return false
                        if (selectedTask!.dependencies?.includes(t.id)) return false
                        // 排除后继任务链中的任务（避免循环依赖）
                        const allDependents = getTaskDependents(selectedTask!.id)
                        return !allDependents.includes(t.id)
                      })"
                      :key="t.id"
                      :value="String(t.id)"
                    >
                      {{ t.title }} ({{ statusLabel[t.status] }})
                    </SelectItem>
                  </template>
                  <template v-else>
                    <SelectItem :value="UNASSIGNED_VALUE" disabled>
                      无可选任务
                    </SelectItem>
                  </template>
                </template>
              </SelectContent>
            </Select>
            <Button 
              @click="handleAddDependency" 
              :disabled="isAddingDependency || selectedDependencyTaskId === UNASSIGNED_VALUE"
              class="w-full"
            >
              <Plus class="mr-2 h-4 w-4" />
              添加依赖
            </Button>
          </div>
          
          <!-- 当前任务依赖的前置任务 -->
          <div class="space-y-2 mb-4">
            <label class="text-sm font-medium text-muted-foreground">当前任务依赖的前置任务</label>
            <div v-if="(getTaskDependenciesDetail(selectedTask).dependencies.filter(dep => !pendingRemovedDependencies.includes(dep.id)).length + pendingAddedDependencies.length) > 0" class="space-y-1">
              <!-- 已有的依赖 -->
              <div
                v-for="dep in getTaskDependenciesDetail(selectedTask).dependencies.filter(dep => !pendingRemovedDependencies.includes(dep.id))"
                :key="dep.id"
                class="flex items-center justify-between p-2 bg-muted rounded-md"
              >
                <div class="flex items-center gap-2">
                  <Badge :variant="statusBadgeVariant[dep.status] ?? 'secondary'">
                    {{ statusLabel[dep.status] ?? dep.status }}
                  </Badge>
                  <span class="text-sm">{{ dep.title }}</span>
                </div>
                <Button 
                  variant="ghost" 
                  size="icon" 
                  class="h-7 w-7 text-destructive hover:text-destructive hover:bg-destructive/10" 
                  @click="handleRemoveDependency(dep.id)"
                  :disabled="isRemovingDependency"
                >
                  <X class="h-4 w-4" />
                </Button>
              </div>
              <!-- 待添加的依赖（显示为待确认状态） -->
              <div
                v-for="depId in pendingAddedDependencies"
                :key="'pending-' + depId"
                class="flex items-center justify-between p-2 bg-blue-50 border border-blue-200 rounded-md"
              >
                <div class="flex items-center gap-2">
                  <Badge variant="default" class="text-xs">待确认</Badge>
                  <span class="text-sm">{{ (tasks || []).find(t => t.id === depId)?.title || '加载中...' }}</span>
                </div>
                <Button 
                  variant="ghost" 
                  size="icon" 
                  class="h-7 w-7 text-destructive hover:text-destructive hover:bg-destructive/10" 
                  @click="pendingAddedDependencies = pendingAddedDependencies.filter(id => id !== depId)"
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
            <div v-if="getTaskDependenciesDetail(selectedTask).dependents.length > 0" class="space-y-1">
              <div
                v-for="dep in getTaskDependenciesDetail(selectedTask).dependents"
                :key="dep.id"
                class="flex items-center gap-2 p-2 bg-muted rounded-md"
              >
                <Badge :variant="statusBadgeVariant[dep.status] ?? 'secondary'">
                  {{ statusLabel[dep.status] ?? dep.status }}
                </Badge>
                <span class="text-sm">{{ dep.title }}</span>
              </div>
            </div>
            <p v-else class="text-sm text-muted-foreground">暂无后继任务</p>
          </div>
        </div>
      </div>
      <DialogFooter>
        <Button variant="outline" @click="isEditOpen = false">取消</Button>
        <Button @click="handleEdit">确定</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>

  <!-- 删除确认弹窗 -->
  <Dialog v-model:open="isDeleteOpen">
    <DialogContent>
      <DialogHeader>
        <DialogTitle>确认删除</DialogTitle>
        <DialogDescription>
          确定要删除任务「{{ selectedTask?.title }}」吗？此操作不可撤销。
        </DialogDescription>
      </DialogHeader>
      <DialogFooter>
        <Button variant="outline" @click="isDeleteOpen = false">取消</Button>
        <Button variant="destructive" @click="handleDelete">确认删除</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<route lang="yaml">
meta:
  auth: true
</route>