import type { CreateTeamTaskRequest, UpdateTeamTaskRequest, TeamTaskResponse , TaskOperationLog } from './types'
import { useAxios } from '@/composables/use-axios'

const { axiosInstance } = useAxios()

export const teamTaskApi = {
  // 获取团队任务列表
  async getTeamTasks(teamId: number) {
    const response = await axiosInstance.get<TeamTaskResponse[]>(`/teams/${teamId}/tasks`)
    return response.data
  },
  async getTaskPredecessors(teamId: number, taskId: number) {
    const response = await axiosInstance.get<TeamTaskResponse[]>(`/teams/${teamId}/tasks/${taskId}/dependencies/predecessors`)
    return response.data
  },

  async getTaskSuccessors(teamId: number, taskId: number) {
    const response = await axiosInstance.get<TeamTaskResponse[]>(`/teams/${teamId}/tasks/${taskId}/dependencies/successors`)
    return response.data
  },
  // 创建团队任务
  async createTeamTask(teamId: number, data: CreateTeamTaskRequest) {
    const response = await axiosInstance.post<TeamTaskResponse>(`/teams/${teamId}/tasks`, data)
    return response.data
  },
  // 添加任务依赖
  async addTaskDependency(teamId: number, taskId: number, dependencyId: number) {
    await axiosInstance.post(
      `/teams/${teamId}/tasks/${taskId}/dependencies`,
      { predecessorTaskId: dependencyId }
    )
  },
  // 删除任务依赖
  async removeTaskDependency(teamId: number, taskId: number, dependencyId: number) {
    await axiosInstance.delete(`/teams/${teamId}/tasks/${taskId}/dependencies/${dependencyId}`)
  },
  // 获取团队任务详情
  async getTeamTask(teamId: number, taskId: number) {
    const response = await axiosInstance.get<TeamTaskResponse>(`/teams/${teamId}/tasks/${taskId}`)
    return response.data
  },

  // 更新团队任务
  async updateTeamTask(teamId: number, taskId: number, data: UpdateTeamTaskRequest) {
    const response = await axiosInstance.put<TeamTaskResponse>(`/teams/${teamId}/tasks/${taskId}`, data)
    return response.data
  },

  // 负责人更新团队任务状态
  async updateTeamTaskStatus(teamId: number, taskId: number, status: string) {
    const response = await axiosInstance.patch<TeamTaskResponse>(`/teams/${teamId}/tasks/${taskId}/status`, { status })
    return response.data
  },

  // 删除团队任务
  async deleteTeamTask(teamId: number, taskId: number) {
    await axiosInstance.delete(`/teams/${teamId}/tasks/${taskId}`)
  },

  // 分配团队成员到任务
  async assignTeamTask(teamId: number, taskId: number, assigneeId: number | null) {
    const response = await axiosInstance.put<TeamTaskResponse>(`/teams/${teamId}/tasks/${taskId}/assign`, { assigneeId })
    return response.data
  },

  // 获取团队任务操作日志
  async getTeamTaskLogs(teamId: number, taskId: number) {
    const response = await axiosInstance.get<TaskOperationLog[]>(`/teams/${teamId}/tasks/${taskId}/logs`)
    return response.data
  },
}
