import { z } from 'zod'

/** 与后端 TaskResponse JSON 字段一致（id 为数字） */
export const taskSchema = z.object({
  id: z.number(),
  title: z.string(),
  description: z.string().optional(),
  status: z.string(),
  priority: z.string(),
  deadline: z.string().nullable().optional(),
  createdAt: z.string().nullable().optional(),
  updatedAt: z.string().nullable().optional(),
  taskType: z.string().optional(),
  // 团队相关字段
  creatorId: z.number().optional(),
  creatorName: z.string().optional(),
  teamId: z.number().optional(),
  assigneeId: z.number().optional(),
  teamName: z.string().optional(),
  assigneeName: z.string().optional(),
  formerAssigneeId: z.number().optional(),
  formerAssigneeName: z.string().optional(),
  dependencies: z.array(z.number()).optional(),
  dependents: z.array(z.number()).optional(),
  deletedAt: z.string().optional(),
  logs: z.array(z.any()).optional(),
})

export type Task = z.infer<typeof taskSchema>