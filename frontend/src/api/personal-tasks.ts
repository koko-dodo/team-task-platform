import type { TaskDto , TaskOperationLog } from './types'
import { useAxios } from '@/composables/use-axios'

const { axiosInstance } = useAxios()

export const personalTaskApi = {
  // 获取当前用户的个人任务列表
  async getPersonalTasks() {
    const response = await axiosInstance.get<TaskDto[]>('/tasks/my')
    return response.data
  },

  // 获取单个个人任务详情
  async getPersonalTask(taskId: number) {
    const response = await axiosInstance.get<TaskDto>(`/tasks/${taskId}`)
    return response.data
  },

  async getTaskPredecessors(taskId: number) {
    const response = await axiosInstance.get<TaskDto[]>(`/tasks/${taskId}/dependencies/predecessors`)
    return response.data
  },

  async getTaskSuccessors(taskId: number) {
    const response = await axiosInstance.get<TaskDto[]>(`/tasks/${taskId}/dependencies/successors`)
    return response.data
  },

  // 创建个人任务
  async createPersonalTask(data: {
    title: string;
    description?: string;
    status?: string;
    priority?: string;
    deadline?: string
  }) {
    const response = await axiosInstance.post<TaskDto>('/tasks', data)
    return response.data
  },

  // 更新个人任务
  async updatePersonalTask(taskId: number, data: {
    title?: string;
    description?: string;
    status?: string;
    priority?: string;
    deadline?: string
  }) {
    const response = await axiosInstance.put<TaskDto>(`/tasks/${taskId}`, data)
    return response.data
  },

  // 添加任务依赖
  async addDependency(taskId: number, dependencyId: number) {
    await axiosInstance.post(
      `/tasks/${taskId}/dependencies`,
      { predecessorTaskId: dependencyId }
    )
  },

  // 删除任务依赖
  async removeDependency(taskId: number, dependencyId: number) {
    await axiosInstance.delete(`/tasks/${taskId}/dependencies/${dependencyId}`)
  },

  // 删除个人任务
  async deletePersonalTask(taskId: number) {
    await axiosInstance.delete(`/tasks/${taskId}`)
  },

  // 获取个人任务操作日志
  async getPersonalTaskLogs(taskId: number) {
    const response = await axiosInstance.get<TaskOperationLog[]>(`/tasks/${taskId}/logs`)
    return response.data
  },
}
