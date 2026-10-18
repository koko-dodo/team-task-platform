type DependencyNode = {
  id: number
  dependents?: number[]
}

function buildTaskMap<T extends DependencyNode>(tasks: readonly T[]) {
  return new Map(tasks.map(task => [task.id, task]))
}

function canReachTarget<T extends DependencyNode>(
  startId: number,
  targetId: number,
  taskMap: Map<number, T>,
): boolean {
  const visited = new Set<number>()
  const stack = [startId]

  while (stack.length > 0) {
    const currentId = stack.pop()!
    if (currentId === targetId) {
      return true
    }
    if (visited.has(currentId)) {
      continue
    }
    visited.add(currentId)

    const currentTask = taskMap.get(currentId)
    currentTask?.dependents?.forEach((dependentId) => {
      if (!visited.has(dependentId)) {
        stack.push(dependentId)
      }
    })
  }

  return false
}

export function getDisplayDependents<T extends DependencyNode>(
  task: T,
  tasks: readonly T[],
): T[] {
  const directDependentIds = task.dependents || []
  if (directDependentIds.length <= 1) {
    const taskMap = buildTaskMap(tasks)
    return directDependentIds
      .map(dependentId => taskMap.get(dependentId))
      .filter(Boolean) as T[]
  }

  const taskMap = buildTaskMap(tasks)

  return directDependentIds
    .filter((candidateId) => {
      return !directDependentIds.some((otherId) => {
        if (otherId === candidateId) {
          return false
        }
        return canReachTarget(otherId, candidateId, taskMap)
      })
    })
    .map(dependentId => taskMap.get(dependentId))
    .filter(Boolean) as T[]
}
