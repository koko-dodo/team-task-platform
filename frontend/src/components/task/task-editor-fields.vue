<script setup lang="ts">
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'

interface TaskMemberOption {
  userId: number
  username: string
}

const props = withDefaults(defineProps<{
  titleLabel?: string
  titlePlaceholder?: string
  descriptionPlaceholder?: string
  showAssignee?: boolean
  assigneeLabel?: string
  assigneePlaceholder?: string
  assigneeDisabled?: boolean
  statusDisabled?: boolean
  detailFieldsDisabled?: boolean
  members?: TaskMemberOption[]
  unassignedValue?: string
}>(), {
  titleLabel: '标题',
  titlePlaceholder: '任务标题',
  descriptionPlaceholder: '任务描述',
  showAssignee: false,
  assigneeLabel: '分配给',
  assigneePlaceholder: '选择成员',
  assigneeDisabled: false,
  statusDisabled: false,
  detailFieldsDisabled: false,
  members: () => [],
  unassignedValue: '__UNASSIGNED__',
})

const title = defineModel<string>('title', { required: true })
const description = defineModel<string | undefined>('description', { required: false, default: '' })
const status = defineModel<string>('status', { required: true })
const priority = defineModel<string>('priority', { required: true })
const deadline = defineModel<string | undefined>('deadline', { required: false, default: '' })
const assigneeId = defineModel<string>('assigneeId', { default: undefined })
const minDeadline = new Date().toISOString().split('T')[0]
</script>

<template>
  <div class="space-y-4">
    <div class="space-y-2">
      <Label for="task-title">{{ props.titleLabel }}</Label>
      <Input id="task-title" v-model="title" :placeholder="props.titlePlaceholder" :disabled="props.detailFieldsDisabled" />
    </div>
    <div class="space-y-2">
      <Label for="task-description">描述</Label>
      <Textarea
        id="task-description"
        v-model="description"
        :placeholder="props.descriptionPlaceholder"
        class="resize-none"
        :disabled="props.detailFieldsDisabled"
      />
    </div>
    <div class="grid grid-cols-2 gap-4">
      <div class="space-y-2">
        <Label for="task-status">状态</Label>
        <Select v-model="status" :disabled="props.statusDisabled">
          <SelectTrigger id="task-status">
            <SelectValue placeholder="选择状态" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="TODO">待办</SelectItem>
            <SelectItem value="IN_PROGRESS">进行中</SelectItem>
            <SelectItem value="DONE">已完成</SelectItem>
          </SelectContent>
        </Select>
      </div>
      <div class="space-y-2">
        <Label for="task-priority">优先级</Label>
        <Select v-model="priority" :disabled="props.detailFieldsDisabled">
          <SelectTrigger id="task-priority">
            <SelectValue placeholder="选择优先级" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="low">不紧急不重要</SelectItem>
            <SelectItem value="medium">重要不紧急</SelectItem>
            <SelectItem value="high">紧急不重要</SelectItem>
            <SelectItem value="urgent">紧急重要</SelectItem>
          </SelectContent>
        </Select>
      </div>
    </div>
    <div class="space-y-2">
      <Label for="task-deadline">截止日期</Label>
      <Input id="task-deadline" v-model="deadline" type="date" :min="minDeadline" :disabled="props.detailFieldsDisabled" />
    </div>
    <div v-if="props.showAssignee" class="space-y-2">
      <Label for="task-assignee">{{ props.assigneeLabel }}</Label>
      <Select v-model="assigneeId" :disabled="props.assigneeDisabled">
        <SelectTrigger id="task-assignee">
          <SelectValue :placeholder="props.assigneePlaceholder" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem :value="props.unassignedValue">未分配</SelectItem>
          <SelectItem
            v-for="member in props.members"
            :key="member.userId"
            :value="String(member.userId)"
          >
            {{ member.username }}
          </SelectItem>
        </SelectContent>
      </Select>
    </div>
  </div>
</template>
