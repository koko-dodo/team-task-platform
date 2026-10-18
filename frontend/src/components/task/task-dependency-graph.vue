// d:\code\ss\Group15_lab1\frontend\src\components\task\task-dependency-graph.vue
<script setup lang="ts">
import { computed } from 'vue'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { ArrowRight, GitBranch } from 'lucide-vue-next'

import type { Task } from '../../pages/tasks/data/schema'
import { statuses, priorities } from '../../pages/tasks/data/data'

const props = defineProps<{
  tasks: Task[]
  selectedTaskId?: number | null
}>()

// 获取状态标签
function getStatusLabel(status: string) {
  const statusObj = statuses.find(s => s.value === status)
  return statusObj?.label || status
}

function getStatusVariant(status: string) {
  switch (status) {
    case 'TODO': return 'secondary'
    case 'IN_PROGRESS': return 'default'
    case 'DONE': return 'outline'
    default: return 'outline'
  }
}

// 获取优先级样式
function getPriorityStyle(priority: string) {
  const p = priorities.find(x => x.value === priority)
  return p?.style || ''
}

function getPriorityLabel(priority: string) {
  const p = priorities.find(x => x.value === priority)
  return p?.label || priority
}

// 构建依赖图数据
const dependencyGraph = computed(() => {
  const graph: { task: Task; dependencies: Task[]; dependents: Task[] }[] = []
  
  props.tasks.forEach(task => {
    const dependencies = task.dependencies?.map(depId => 
      props.tasks.find(t => t.id === depId)
    ).filter(Boolean) as Task[] || []
    
    const dependents = props.tasks.filter(t => 
      t.dependencies?.includes(task.id)
    )
    
    if (dependencies.length > 0 || dependents.length > 0) {
      graph.push({ task, dependencies, dependents })
    }
  })
  
  return graph
})

// 获取单个任务的依赖详情
function getTaskDependencyDetail(taskId: number) {
  const task = props.tasks.find(t => t.id === taskId)
  if (!task) return null
  
  const dependencies = task.dependencies?.map(depId => 
    props.tasks.find(t => t.id === depId)
  ).filter(Boolean) as Task[] || []
  
  const dependents = props.tasks.filter(t => 
    t.dependencies?.includes(taskId)
  )
  
  return { task, dependencies, dependents }
}

// 检查是否有依赖关系
const hasDependencies = computed(() => {
  return props.tasks.some(t => t.dependencies?.length && t.dependencies.length > 0) ||
         props.tasks.some(t => {
           return props.tasks.some(other => other.dependencies?.includes(t.id))
         })
})
</script>

<template>
  <Card class="mt-6">
    <CardHeader>
      <div class="flex items-center gap-2">
        <GitBranch class="h-5 w-5 text-primary" />
        <CardTitle>任务依赖关系</CardTitle>
      </div>
    </CardHeader>
    <CardContent>
      <div v-if="!hasDependencies" class="text-center py-8 text-muted-foreground">
        <GitBranch class="h-12 w-12 mx-auto mb-3 opacity-50" />
        <p>暂无任务依赖关系</p>
        <p class="text-sm mt-1">可以在任务详情页中设置任务间的依赖</p>
      </div>
      
      <div v-else class="space-y-6">
        <!-- 依赖流程图 -->
        <div class="space-y-4">
          <div 
            v-for="item in dependencyGraph" 
            :key="item.task.id"
            class="relative"
          >
            <!-- 前置任务 -->
            <div v-if="item.dependencies.length > 0" class="mb-3">
              <div class="text-xs text-muted-foreground mb-2">前置任务</div>
              <div class="flex flex-wrap gap-2">
                <div 
                  v-for="dep in item.dependencies" 
                  :key="dep.id"
                  class="flex items-center gap-2 px-3 py-2 bg-muted/50 rounded-lg border border-muted"
                >
                  <Badge :variant="getStatusVariant(dep.status)" class="text-xs">
                    {{ getStatusLabel(dep.status) }}
                  </Badge>
                  <span class="text-sm font-medium">{{ dep.title }}</span>
                  <span 
                    class="text-xs px-1.5 py-0.5 rounded"
                    :class="getPriorityStyle(dep.priority)"
                  >
                    {{ getPriorityLabel(dep.priority) }}
                  </span>
                </div>
              </div>
            </div>
            
            <!-- 箭头 -->
            <div v-if="item.dependencies.length > 0" class="flex justify-center mb-2">
              <ArrowRight class="h-4 w-4 text-muted-foreground" />
            </div>
            
            <!-- 当前任务 -->
            <div 
              class="flex items-center justify-center"
              :class="{ 'ring-2 ring-primary rounded-lg': selectedTaskId === item.task.id }"
            >
              <div class="flex items-center gap-3 px-4 py-3 bg-primary/10 rounded-lg border border-primary/20">
                <div class="flex items-center gap-2">
                  <Badge :variant="getStatusVariant(item.task.status)" class="text-sm">
                    {{ getStatusLabel(item.task.status) }}
                  </Badge>
                </div>
                <span class="font-semibold">{{ item.task.title }}</span>
                <span 
                  class="text-xs px-2 py-0.5 rounded"
                  :class="getPriorityStyle(item.task.priority)"
                >
                  {{ getPriorityLabel(item.task.priority) }}
                </span>
              </div>
            </div>
            
            <!-- 箭头 -->
            <div v-if="item.dependents.length > 0" class="flex justify-center mt-2">
              <ArrowRight class="h-4 w-4 text-muted-foreground rotate-180" />
            </div>
            
            <!-- 后继任务 -->
            <div v-if="item.dependents.length > 0" class="mt-3">
              <div class="text-xs text-muted-foreground mb-2">后继任务</div>
              <div class="flex flex-wrap gap-2">
                <div 
                  v-for="dep in item.dependents" 
                  :key="dep.id"
                  class="flex items-center gap-2 px-3 py-2 bg-muted/50 rounded-lg border border-muted"
                >
                  <Badge :variant="getStatusVariant(dep.status)" class="text-xs">
                    {{ getStatusLabel(dep.status) }}
                  </Badge>
                  <span class="text-sm font-medium">{{ dep.title }}</span>
                  <span 
                    class="text-xs px-1.5 py-0.5 rounded"
                    :class="getPriorityStyle(dep.priority)"
                  >
                    {{ getPriorityLabel(dep.priority) }}
                  </span>
                </div>
              </div>
            </div>
            
            <!-- 分隔线 -->
            <div class="border-t border-muted mt-4 pt-4" v-if="dependencyGraph.indexOf(item) < dependencyGraph.length - 1"></div>
          </div>
        </div>
        
        <!-- 依赖统计 -->
        <div class="mt-6 pt-6 border-t border-muted">
          <div class="grid grid-cols-3 gap-4">
            <div class="text-center p-3 bg-muted/50 rounded-lg">
              <div class="text-2xl font-bold text-primary">{{ tasks.length }}</div>
              <div class="text-sm text-muted-foreground">总任务数</div>
            </div>
            <div class="text-center p-3 bg-muted/50 rounded-lg">
              <div class="text-2xl font-bold text-blue-500">
                {{ tasks.filter(t => t.dependencies?.length && t.dependencies.length > 0).length }}
              </div>
              <div class="text-sm text-muted-foreground">有前置任务</div>
            </div>
            <div class="text-center p-3 bg-muted/50 rounded-lg">
              <div class="text-2xl font-bold text-green-500">
                {{ tasks.filter(t => tasks.some(other => other.dependencies?.includes(t.id))).length }}
              </div>
              <div class="text-sm text-muted-foreground">被其他任务依赖</div>
            </div>
          </div>
        </div>
      </div>
    </CardContent>
  </Card>
</template>