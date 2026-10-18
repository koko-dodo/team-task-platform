import type { TaskDto, TeamTaskResponse } from './types'
import { personalTaskApi } from './personal-tasks'
import { teamTaskApi } from './team-tasks'

type TaskWithOptionalTeam = {
  id: number
  teamId?: number
}

type DependencyAugmentedTask = {
  dependencies: number[]
  dependents: number[]
}

async function fetchDependencyIdsForTask(task: TaskWithOptionalTeam): Promise<DependencyAugmentedTask> {
  if (task.teamId) {
    const [predecessors, successors] = await Promise.all([
      teamTaskApi.getTaskPredecessors(task.teamId, task.id),
      teamTaskApi.getTaskSuccessors(task.teamId, task.id),
    ])

    return {
      dependencies: predecessors.map(predecessor => predecessor.id),
      dependents: successors.map(successor => successor.id),
    }
  }

  const [predecessors, successors] = await Promise.all([
    personalTaskApi.getTaskPredecessors(task.id),
    personalTaskApi.getTaskSuccessors(task.id),
  ])

  return {
    dependencies: predecessors.map(predecessor => predecessor.id),
    dependents: successors.map(successor => successor.id),
  }
}

export async function withTaskDependencies<T extends TaskDto | TeamTaskResponse>(tasks: T[]): Promise<Array<T & DependencyAugmentedTask>> {
  const dependencyPairs = await Promise.all(tasks.map(task => fetchDependencyIdsForTask(task)))

  return tasks.map((task, index) => ({
    ...task,
    dependencies: dependencyPairs[index].dependencies,
    dependents: dependencyPairs[index].dependents,
  }))
}
