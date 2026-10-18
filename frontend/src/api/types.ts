/** 与后端 Auth 接口包装一致 {@link com.group15.lab1.dto.ApiResponse} */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp?: number
}

export interface UserBrief {
  id: number
  username: string
  createdAt?: string | null
}

export interface LoginResponseBody {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserBrief
  message?: string
}

export interface CreateTeamRequest {
  name: string
  description?: string
}

export interface UpdateTeamRequest {
  name?: string
  description?: string
}

export type TeamStatus = 'ACTIVE' | 'DISBANDED'

export interface TeamResponse {
  id: number
  name: string
  description?: string
  ownerId: number
  ownerUsername: string
  currentUserRole: string
  status: TeamStatus
  createdAt: string
  updatedAt: string
  memberCount?: number
}

export type TeamRole = 'Owner' | 'Admin' | 'Member'

export interface TeamMemberResponse {
  userId: number
  username: string
  role: TeamRole
  joinedAt: string
}

export interface CreateTeamTaskRequest {
  title: string
  description?: string
  status?: string
  priority?: string
  deadline?: string
}
export interface UpdateTeamTaskRequest {
  title?: string
  description?: string
  status?: string
  priority?: string
  deadline?: string
}
export interface TeamTaskResponse {
  id: number
  title: string
  description?: string
  status: string
  priority: string
  deadline?: string
  createdAt: string
  updatedAt: string
  teamId: number
  creatorId?: number
  creatorName?: string
  assigneeId?: number
  assigneeName?: string
  teamName?: string
  dependencies?: number[]
  dependents?: number[]
  formerAssigneeId?: number
  formerAssigneeName?: string
}

export interface TaskDependencyResponse {
  id: number
  taskId: number
  dependencyId: number
  createdAt: string
}

export interface AddDependencyRequest {
  predecessorTaskId: number
}

export interface BatchAddDependenciesRequest {
  dependencyIds: number[]
}
export interface AssignTeamTaskRequest {
  assigneeId: number | null
}

/** 与 {@link com.group15.lab1.controller.dto.TaskResponse} 对齐 */
export interface TaskDto {
  id: number
  title: string
  description: string | null
  status: string
  priority: string
  deadline: string | null
  createdAt: string | null
  updatedAt: string | null
  // 团队相关字段
  creatorId?: number
  creatorName?: string
  teamId?: number
  assigneeId?: number
  teamName?: string
  assigneeName?: string
  dependencies?: number[]
  dependents?: number[]
  formerAssigneeId?: number
  formerAssigneeName?: string
}

/** 后端 {@link com.group15.lab1.exception.ApiError} */
export interface ApiErrorBody {
  status: number
  error: string
  message: string
  details?: string
  timestamp?: string
}

/** 任务操作类型，与后端 {@link com.group15.lab1.entity.TaskOperationType} 对齐 */
export type TaskOperationType = 
  | 'CREATE_TASK'
  | 'UPDATE_TASK_INFO'
  | 'UPDATE_TASK_STATUS'
  | 'UPDATE_ASSIGNEE'
  | 'ADD_DEPENDENCY'
  | 'REMOVE_DEPENDENCY'
  | 'DELETE_TASK'
  | 'MEMBER_LEAVE_TASK_CHANGE'
  | 'TEAM_DISBANDED_TASK_CHANGE'

/** 任务操作范围类型，与后端 {@link com.group15.lab1.entity.TaskOperationScopeType} 对齐 */
export type TaskOperationScopeType = 'PERSONAL' | 'TEAM'

/** 任务操作日志，与后端 {@link com.group15.lab1.controller.dto.TaskOperationLogResponse} 对齐 */
export interface TaskOperationLog {
  id: number
  taskId: number
  actorId: number
  actorName: string
  operationType: TaskOperationType
  scopeType: TaskOperationScopeType
  teamId?: number
  summary: string
  oldValue?: string
  newValue?: string
  createdAt: string
}