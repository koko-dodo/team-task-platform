// ========================================================
// Lab2 假数据库 — 前端 A 用，前端 B 对接时替换为 API 调用
// ========================================================

/** 我的团队列表 */
export const mockTeams = [
  {
    id: 1,
    name: '软件工程小组',
    description: '完成 Lab2 实验项目开发',
    role: 'Owner' as const,
    memberCount: 5,
    createdAt: '2025-12-01',
  },
  {
    id: 2,
    name: '前端开发组',
    description: 'Vue3 + TypeScript 前端开发',
    role: 'Admin' as const,
    memberCount: 6,
    createdAt: '2025-12-05',
  },
  {
    id: 3,
    name: '设计组',
    description: 'UI/UX 设计与原型',
    role: 'Member' as const,
    memberCount: 8,
    createdAt: '2025-12-10',
  },
]

/** 当前 Mock 团队详情（软件工程小组） */
export const mockTeamDetail = {
  id: 1,
  name: '软件工程小组',
  description: '完成 Lab2 实验项目开发',
  currentUserRole: 'Owner' as const,
}

/** 团队成员列表 */
export const mockMembers = [
  { userId: 1, username: 'admin_test', role: 'Owner' as const, joinedAt: '2025-12-01' },
  { userId: 2, username: 'test_user', role: 'Admin' as const, joinedAt: '2025-12-02' },
  { userId: 3, username: 'demo_user', role: 'Member' as const, joinedAt: '2025-12-03' },
  { userId: 4, username: 'john_doe', role: 'Member' as const, joinedAt: '2025-12-04' },
  { userId: 5, username: 'jane_smith', role: 'Member' as const, joinedAt: '2025-12-05' },
]

/** 团队任务列表 */
export const mockTeamTasks = [
  {
    id: 1,
    title: '完成 Lab2',
    description: '后端 + 前端完整实现团队协作功能',
    status: 'TODO',
    priority: 'high',
    creator: 'admin_test',
    assignee: 'demo_user',
    deadline: '2025-12-30',
    createdAt: '2025-12-10',
  },
  {
    id: 2,
    title: '写数据库文档',
    description: '完成表结构设计与迁移脚本',
    status: 'IN_PROGRESS',
    priority: 'medium',
    creator: 'admin_test',
    assignee: 'john_doe',
    deadline: '2025-12-28',
    createdAt: '2025-12-11',
  },
  {
    id: 3,
    title: '写接口',
    description: '完成团队模块 REST API 开发',
    status: 'TODO',
    priority: 'medium',
    creator: 'test_user',
    assignee: 'test_user',
    deadline: '2025-12-29',
    createdAt: '2025-12-12',
  },
  {
    id: 4,
    title: 'UI 设计',
    description: '设计团队空间页面布局',
    status: 'DONE',
    priority: 'low',
    creator: 'demo_user',
    assignee: null,
    deadline: null,
    createdAt: '2025-12-13',
  },
]

/** 扩展任务列表（个人任务 + 团队任务混合，用于 /tasks 页面） */
export const mockExtendedTasks = [
  {
    id: 1,
    title: '个人任务1',
    description: '我自己的个人任务',
    status: 'TODO',
    priority: 'medium',
    deadline: '2025-12-20',
    createdAt: '2025-12-01',
    updatedAt: '2025-12-01',
    taskType: '个人任务' as const,
  },
  {
    id: 2,
    title: '学习计划',
    description: '完成实验报告',
    status: 'IN_PROGRESS',
    priority: 'high',
    deadline: '2025-12-25',
    createdAt: '2025-12-02',
    updatedAt: '2025-12-05',
    taskType: '个人任务' as const,
  },
  {
    id: 3,
    title: '写数据库文档',
    description: '完成表结构设计',
    status: 'IN_PROGRESS',
    priority: 'medium',
    deadline: '2025-12-28',
    createdAt: '2025-12-03',
    updatedAt: '2025-12-06',
    taskType: '团队任务' as const,
  },
  {
    id: 4,
    title: '写接口',
    description: '完成团队模块接口',
    status: 'TODO',
    priority: 'medium',
    deadline: '2025-12-29',
    createdAt: '2025-12-04',
    updatedAt: '2025-12-04',
    taskType: '团队任务' as const,
  },
  {
    id: 5,
    title: '个人学习',
    description: '复习期末考试',
    status: 'TODO',
    priority: 'urgent',
    deadline: '2025-12-31',
    createdAt: '2025-12-05',
    updatedAt: '2025-12-05',
    taskType: '个人任务' as const,
  },
]
