// Browser-only display history; never an authorization source.
const prefix = 'task-history:v1:'

function removeLegacyCache() {
  for (const key of Object.keys(localStorage)) {
    if (key === 'deletedTasks' || key.startsWith('deletedTeamTasks_'))
      localStorage.removeItem(key)
  }
}

function cacheKey(userId: number | null, scope: string) {
  return userId == null ? null : `${prefix}${userId}:${scope}`
}

export function readDeletedTasks<T>(userId: number | null, scope: string): T[] {
  try {
    removeLegacyCache()
    const key = cacheKey(userId, scope)
    const data = key ? JSON.parse(localStorage.getItem(key) || '[]') : []
    return Array.isArray(data) ? data : []
  }
  catch {
    return []
  }
}

export function writeDeletedTasks<T>(userId: number | null, scope: string, tasks: T[]) {
  try {
    removeLegacyCache()
    const key = cacheKey(userId, scope)
    if (key)
      localStorage.setItem(key, JSON.stringify(tasks))
  }
  catch {
    // Storage can be unavailable or full; a successful API delete stays successful.
  }
}

export function clearDeletedTaskCaches() {
  try {
    removeLegacyCache()
    for (const key of Object.keys(localStorage)) {
      if (key.startsWith(prefix))
        localStorage.removeItem(key)
    }
  }
  catch {
    // Storage may be disabled by the browser.
  }
}
