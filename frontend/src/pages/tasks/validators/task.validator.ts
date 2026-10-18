import z from 'zod'

export const taskValidator = z.object({
  title: z.string().min(1).max(50),
  description: z.string().optional(),
  status: z.enum(['TODO', 'IN_PROGRESS', 'DONE']),
  priority: z.enum(['urgent', 'high', 'medium', 'low']),
  deadline: z.string().optional(),
})

export type TaskValidator = z.infer<typeof taskValidator>
