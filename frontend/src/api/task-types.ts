import type { TaskDto, TeamTaskResponse } from './types'

export type DependencyAwareTaskDto = TaskDto & {
  dependencies: number[]
  dependents: number[]
}

export type DependencyAwareTeamTaskResponse = TeamTaskResponse & {
  dependencies: number[]
  dependents: number[]
}
