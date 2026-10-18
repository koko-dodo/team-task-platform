import type {
  ApiResponse,
  CreateTeamRequest,
  TeamMemberResponse,
  TeamResponse,
  TeamRole,
  UpdateTeamRequest,
} from './types'
import { useAxios } from '@/composables/use-axios'

const { axiosInstance } = useAxios()

export const teamApi = {
  // 创建团队
  async createTeam(data: CreateTeamRequest) {
    const response = await axiosInstance.post<ApiResponse<TeamResponse>>('/teams', data)
    return response.data.data
  },

  // 获取用户创建的团队
  async getOwnedTeams() {
    const response = await axiosInstance.get<ApiResponse<TeamResponse[]>>('/teams')
    return response.data.data.filter(team => team.currentUserRole === 'Owner')
  },

  // 获取用户参与的团队
  async getMemberTeams() {
    const response = await axiosInstance.get<ApiResponse<TeamResponse[]>>('/teams')
    return response.data.data.filter(team => team.currentUserRole !== 'Owner')
  },

  // 获取用户相关的所有团队
  async getUserTeams() {
    const response = await axiosInstance.get<ApiResponse<TeamResponse[]>>('/teams')
    return response.data.data
  },

  // 获取团队详情
  async getTeam(teamId: number) {
    const response = await axiosInstance.get<ApiResponse<TeamResponse>>(`/teams/${teamId}`)
    return response.data.data
  },

  // 更新团队信息
  async updateTeam(teamId: number, data: UpdateTeamRequest) {
    const response = await axiosInstance.put<ApiResponse<TeamResponse>>(`/teams/${teamId}`, data)
    return response.data.data
  },

  // 删除团队
  async deleteTeam(teamId: number) {
    await axiosInstance.delete(`/teams/${teamId}`)
  },

  // 添加团队成员
  async addTeamMember(teamId: number, userIdentifier: string, role: Exclude<TeamRole, 'Owner'> = 'Member') {
    const normalized = userIdentifier.trim()
    const payload: { userId?: number; username?: string; role: Exclude<TeamRole, 'Owner'> } = { role }

    if (/^\d+$/.test(normalized)) {
      payload.userId = Number.parseInt(normalized, 10)
      payload.username = normalized
    }
    else {
      payload.username = normalized
    }

    const response = await axiosInstance.post<ApiResponse<TeamMemberResponse>>(`/teams/${teamId}/members`, payload)
    return response.data.data
  },

  // 修改成员角色
  async updateMemberRole(teamId: number, memberUserId: number, role: Exclude<TeamRole, 'Owner'>) {
    const response = await axiosInstance.patch<ApiResponse<TeamMemberResponse>>(
      `/teams/${teamId}/members/${memberUserId}/role`,
      { role },
    )
    return response.data.data
  },

  // 移除团队成员
  async removeTeamMember(teamId: number, memberUserId: number) {
    await axiosInstance.delete(`/teams/${teamId}/members/${memberUserId}`)
  },

  // 获取团队成员列表
  async getTeamMembers(teamId: number) {
    const response = await axiosInstance.get<ApiResponse<TeamMemberResponse[]>>(`/teams/${teamId}/members`)
    return response.data.data
  },

  // 主动离开团队
  async leaveTeam(teamId: number) {
    await axiosInstance.delete(`/teams/${teamId}/members/me`)
  },

  // 转让团队所有权
  async transferOwnership(teamId: number, newOwnerId: number) {
    const response = await axiosInstance.patch<ApiResponse<TeamResponse>>(`/teams/${teamId}/ownership`, { newOwnerId })
    return response.data.data
  },
  
  // 解散团队
  async disbandTeam(teamId: number) {
    await axiosInstance.patch(`/teams/${teamId}/disband`)
  }
}
