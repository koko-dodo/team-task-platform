<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { Plus, Pencil, UserX, LogOut, Trash2, AlertTriangle, ArrowRight } from 'lucide-vue-next'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { toast } from 'vue-sonner'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Tabs,
  TabsList,
  TabsTrigger,
} from '@/components/ui/tabs'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { teamApi } from '@/api/team'
import type { TeamMemberResponse, TeamRole, TeamResponse } from '@/api/types'
const UNASSIGNED_VALUE = '__UNASSIGNED__'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const queryClient = useQueryClient()
const teamId = computed(() => parseInt((route.params as any).teamId as string))
const activeTab = ref('members')
const teamQueryKey = computed(() => ['team', teamId.value, authStore.userId])
const teamMembersQueryKey = computed(() => ['team-members', teamId.value, authStore.userId])
const teamTasksQueryKey = computed(() => ['team-tasks', teamId.value, authStore.userId])

const { data: team } = useQuery({
  queryKey: teamQueryKey,
  queryFn: () => teamApi.getTeam(teamId.value),
  enabled: computed(() => teamId.value > 0 && authStore.isLogin),
})

const { data: members } = useQuery({
  queryKey: teamMembersQueryKey,
  queryFn: () => teamApi.getTeamMembers(teamId.value),
  enabled: computed(() => teamId.value > 0 && authStore.isLogin),
})

const canManageMembers = computed(() => team.value?.currentUserRole === 'Owner')
const isOwner = computed(() => team.value?.currentUserRole === 'Owner')
const isMemberOrAdmin = computed(() => ['Member', 'Admin'].includes(team.value?.currentUserRole || ''))
const isTeamDisbanded = computed(() => team.value?.status === 'DISBANDED')

const addMemberMutation = useMutation({
  mutationFn: ({ userIdentifier, role }: { userIdentifier: string; role: Exclude<TeamRole, 'Owner'> }) =>
    teamApi.addTeamMember(teamId.value, userIdentifier, role),
  onSuccess: () => {
    toast.success('成员添加成功')
    queryClient.invalidateQueries({ queryKey: teamMembersQueryKey.value })
    queryClient.invalidateQueries({ queryKey: teamQueryKey.value })
    isAddOpen.value = false
  },
  onError: (error: any) => {
    toast.error(`添加成员失败: ${error.response?.data?.message || error.message}`)
  },
})

const updateRoleMutation = useMutation({
  mutationFn: ({ memberId, role }: { memberId: number; role: Exclude<TeamRole, 'Owner'> }) =>
    teamApi.updateMemberRole(teamId.value, memberId, role),
  onSuccess: () => {
    toast.success('成员角色修改成功')
    queryClient.invalidateQueries({ queryKey: teamMembersQueryKey.value })
    isRoleOpen.value = false
  },
  onError: (error: any) => {
    toast.error(`修改角色失败: ${error.response?.data?.message || error.message}`)
  },
})

const removeMemberMutation = useMutation({
  mutationFn: (memberId: number) => teamApi.removeTeamMember(teamId.value, memberId),
  onSuccess: () => {
    toast.success('成员移除成功')
    queryClient.invalidateQueries({ queryKey: teamMembersQueryKey.value })
    queryClient.invalidateQueries({ queryKey: teamQueryKey.value })
    queryClient.invalidateQueries({ queryKey: teamTasksQueryKey.value })
    isRemoveOpen.value = false
  },
  onError: (error: any) => {
    toast.error(`移除成员失败: ${error.response?.data?.message || error.message}`)
  },
})

// 离开团队mutation
const leaveTeamMutation = useMutation({
  mutationFn: () => teamApi.leaveTeam(teamId.value),
  onSuccess: () => {
    toast.success('已成功离开团队')
    router.push('/teams')
  },
  onError: (error: any) => {
    toast.error(`离开团队失败: ${error.response?.data?.message || error.message}`)
  },
})

// 转让所有权 mutation
const transferOwnershipMutation = useMutation({
  mutationFn: (newOwnerId: number) => teamApi.transferOwnership(teamId.value, newOwnerId),
  onSuccess: () => {
    toast.success('所有权转让成功，如需退出团队请再执行离开团队')
    isOwnerLeaveOpen.value = false
    selectedNewOwnerId.value = UNASSIGNED_VALUE
    queryClient.invalidateQueries({ queryKey: teamMembersQueryKey.value })
    queryClient.invalidateQueries({ queryKey: teamQueryKey.value })
    queryClient.invalidateQueries({ queryKey: ['teams', authStore.userId] })
  },
  onError: (error: any) => {
    toast.error(`转让所有权失败: ${error.response?.data?.message || error.message}`)
  },
})
// 可转让的成员列表（排除Owner自己）
const transferableMembers = computed(() => {
  return (members.value || []).filter(m => m.role !== 'Owner')
})

// 解散团队mutation
const disbandTeamMutation = useMutation({
  mutationFn: () => teamApi.disbandTeam(teamId.value),
  onSuccess: () => {
    toast.success('团队已解散')
    queryClient.invalidateQueries({ queryKey: teamQueryKey.value })
    queryClient.invalidateQueries({ queryKey: ['teams', authStore.userId] })
    isDisbandOpen.value = false
    router.push('/teams')
  },
  onError: (error: any) => {
    toast.error(`解散团队失败: ${error.response?.data?.message || error.message}`)
    isDisbandOpen.value = false
  },
})

onMounted(() => {
  activeTab.value = `members`
})

function switchTab(tab: string) {
  activeTab.value = tab
  router.push(tab)
}

// 弹窗状态
const isAddOpen = ref(false)
const isRoleOpen = ref(false)
const selectedMember = ref<TeamMemberResponse | null>(null)
const newMemberUserId = ref('')
const newMemberRole = ref<Exclude<TeamRole, 'Owner'>>('Member')
const changeRole = ref<Exclude<TeamRole, 'Owner'>>('Member')
const removeTarget = ref<TeamMemberResponse | null>(null)
const isRemoveOpen = ref(false)
const isLeaveOpen = ref(false)
const isOwnerLeaveOpen = ref(false)
const isDisbandOpen = ref(false)
const selectedNewOwnerId = ref(UNASSIGNED_VALUE)

function openAdd() {
  newMemberUserId.value = ''
  newMemberRole.value = 'Member'
  isAddOpen.value = true
}

function openRoleChange(member: TeamMemberResponse) {
  selectedMember.value = member
  changeRole.value = member.role === 'Owner' ? 'Member' : member.role
  isRoleOpen.value = true
}

function openRemove(member: TeamMemberResponse) {
  removeTarget.value = member
  isRemoveOpen.value = true
}

function handleAdd() {
  if (!canManageMembers.value) {
    toast.error('仅 Owner 可管理成员')
    return
  }

  const userIdentifier = newMemberUserId.value.trim()
  if (!userIdentifier) {
    toast.error('请输入用户ID或用户名')
    return
  }

  addMemberMutation.mutate({ userIdentifier, role: newMemberRole.value })
}

function handleRoleChange() {
  if (!canManageMembers.value) {
    toast.error('仅 Owner 可管理成员')
    return
  }
  if (!selectedMember.value) {
    return
  }
  updateRoleMutation.mutate({
    memberId: selectedMember.value.userId,
    role: changeRole.value,
  })
}

function handleRemove() {
  if (!canManageMembers.value) {
    toast.error('仅 Owner 可管理成员')
    return
  }
  if (!removeTarget.value) {
    return
  }
  removeMemberMutation.mutate(removeTarget.value.userId)
}

function openLeave() {
  if (isOwner.value) {
    isOwnerLeaveOpen.value = true
  } else {
    isLeaveOpen.value = true
  }
}
function openOwnerLeave() {
  isOwnerLeaveOpen.value = true
}
function handleCancelOwnerLeave() {
  isOwnerLeaveOpen.value = false
  isDisbandOpen.value = true
}
function handleTransferOwnership() {
  if (selectedNewOwnerId.value === UNASSIGNED_VALUE) {
    toast.error('请选择新的团队负责人')
    return
  }
  transferOwnershipMutation.mutate(Number(selectedNewOwnerId.value))
}
function handleLeave() {
  leaveTeamMutation.mutate()
}
function openDisband() {
  isDisbandOpen.value = true
}
function handleDisband() {
  disbandTeamMutation.mutate()
}

const roleBadgeVariant: Record<string, 'default' | 'secondary' | 'outline'> = {
  Owner: 'default',
  Admin: 'secondary',
  Member: 'outline',
}
</script>

<template>
  <!-- 团队已解散警告 -->
  <div v-if="isTeamDisbanded" class="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-3">
    <AlertTriangle class="h-5 w-5 text-red-500" />
    <div>
      <p class="font-medium text-red-700">团队已解散</p>
      <p class="text-sm text-red-600">您只能查看历史信息，无法进行任何操作</p>
    </div>
  </div>
  <!-- 团队信息头部 -->
  <Card class="mb-4" :class="{ 'opacity-60': isTeamDisbanded }">
    <CardHeader>
      <div class="flex items-center justify-between">
        <div>
          <CardTitle class="text-xl">{{ team?.name || '团队详情' }}</CardTitle>
        </div>
        <Badge v-if="team?.status === 'DISBANDED'" variant="destructive">已解散</Badge>
      </div>
    </CardHeader>
    <CardContent class="flex items-center gap-3">
      <Badge :variant="roleBadgeVariant[team?.currentUserRole || ''] ?? 'outline'">
        {{ team?.currentUserRole || '-' }}
      </Badge>
      <span class="text-sm text-muted-foreground">
        创建者：{{ team?.ownerUsername }}
      </span>
      <span class="text-sm text-muted-foreground">
        成员数：{{ team?.memberCount }}
      </span>
      <span class="text-sm text-muted-foreground">
        创建时间：{{ team?.createdAt ? new Date(team.createdAt).toLocaleDateString() : '-' }}
      </span>
    </CardContent>
  </Card>

  <!-- 标签栏 -->
  <Tabs v-model="activeTab" class="mb-4">
    <TabsList>
      <TabsTrigger value="tasks" @click="router.push(`/teams/${teamId}`)">
        团队任务
      </TabsTrigger>
      <TabsTrigger value="dependencies" @click="router.push(`/teams/${teamId}#dependencies`)">
        依赖关系
      </TabsTrigger>
      <TabsTrigger value="members">
        成员管理
      </TabsTrigger>
    </TabsList>
  </Tabs>

  <!-- 操作按钮 -->
  <div class="mb-4 flex gap-3">
    <Button v-if="!isTeamDisbanded" @click="openAdd" :disabled="!canManageMembers">
      <Plus class="mr-2 h-4 w-4" />
      添加成员
    </Button>
    <Button v-if="isMemberOrAdmin && !isTeamDisbanded" variant="outline" @click="openLeave">
      <LogOut class="mr-2 h-4 w-4" />
      离开团队
    </Button>
    <Button v-if="isOwner && !isTeamDisbanded" variant="outline" @click="openOwnerLeave">
      <LogOut class="mr-2 h-4 w-4" />
      离开团队
    </Button>
    <Button v-if="isOwner && !isTeamDisbanded" variant="destructive" @click="openDisband">
      <Trash2 class="mr-2 h-4 w-4" />
      解散团队
    </Button>
  </div>

  <!-- 成员表格 -->
  <div class="overflow-x-auto rounded-md border">
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>用户名</TableHead>
          <TableHead>角色</TableHead>
          <TableHead>加入时间</TableHead>
          <TableHead>操作</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        <TableRow v-for="member in (members || [])" :key="member.userId">
          <TableCell class="font-medium">{{ member.username }}</TableCell>
          <TableCell>
            <Badge :variant="roleBadgeVariant[member.role] ?? 'outline'">
              {{ member.role }}
            </Badge>
          </TableCell>
          <TableCell>{{ member.joinedAt ? new Date(member.joinedAt).toLocaleString() : '-' }}</TableCell>
          <TableCell>
            <div class="flex gap-1">
              <Button
                v-if="!isTeamDisbanded"
                variant="ghost"
                size="icon"
                :disabled="!canManageMembers || member.role === 'Owner'"
                @click="openRoleChange(member)"
                title="修改角色"
              >
                <Pencil class="h-4 w-4" />
              </Button>
              <Button
                v-if="!isTeamDisbanded"
                variant="ghost"
                size="icon"
                :disabled="!canManageMembers || member.role === 'Owner'"
                @click="openRemove(member)"
                title="移除成员"
              >
                <UserX class="h-4 w-4 text-destructive" />
              </Button>
            </div>
          </TableCell>
        </TableRow>
        <TableRow v-if="!members || members.length === 0">
          <TableCell colspan="4" class="text-center text-muted-foreground py-8">
            暂无团队成员
          </TableCell>
        </TableRow>
      </TableBody>
    </Table>
  </div>

  <!-- 添加成员弹窗 -->
  <Dialog v-model:open="isAddOpen">
    <DialogContent>
      <DialogHeader>
        <DialogTitle>添加成员</DialogTitle>
        <DialogDescription>
          选择用户并设置角色后添加到团队
        </DialogDescription>
      </DialogHeader>
      <div class="space-y-4 py-4">
        <div class="space-y-2">
            <Label for="user-id">用户 ID 或用户名</Label>
          <Input
            id="user-id"
            v-model="newMemberUserId"
              placeholder=""
          />
        </div>
        <div class="space-y-2">
          <Label>角色</Label>
          <Select v-model="newMemberRole">
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="Admin">Admin</SelectItem>
              <SelectItem value="Member">Member</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>
      <DialogFooter>
        <Button variant="outline" @click="isAddOpen = false">取消</Button>
        <Button @click="handleAdd">确定</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>

  <!-- 修改角色弹窗 -->
  <Dialog v-model:open="isRoleOpen">
    <DialogContent>
      <DialogHeader>
        <DialogTitle>修改角色</DialogTitle>
        <DialogDescription>
          修改成员「{{ selectedMember?.username }}」的角色
        </DialogDescription>
      </DialogHeader>
      <div class="space-y-4 py-4">
        <div class="space-y-2">
          <Label>当前角色</Label>
          <p class="text-sm text-muted-foreground">{{ selectedMember?.role }}</p>
        </div>
        <div class="space-y-2">
          <Label>新角色</Label>
          <Select v-model="changeRole">
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="Admin">Admin</SelectItem>
              <SelectItem value="Member">Member</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>
      <DialogFooter>
        <Button variant="outline" @click="isRoleOpen = false">取消</Button>
        <Button @click="handleRoleChange">确定</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>

  <!-- 移除成员确认弹窗 -->
  <Dialog v-model:open="isRemoveOpen">
    <DialogContent>
      <DialogHeader>
        <DialogTitle>确认移除</DialogTitle>
        <DialogDescription>
          确定要将「{{ removeTarget?.username }}」移出团队吗？
        </DialogDescription>
      </DialogHeader>
      <DialogFooter>
        <Button variant="outline" @click="isRemoveOpen = false">取消</Button>
        <Button variant="destructive" @click="handleRemove">确认移除</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>

  <!-- 离开团队确认弹窗 -->
  <Dialog v-model:open="isLeaveOpen">
    <DialogContent>
      <DialogHeader>
        <DialogTitle>确认离开团队</DialogTitle>
        <DialogDescription>
          确定要离开「{{ team?.name }}」团队吗？
          离开后您将无法访问该团队的任务和成员信息。
        </DialogDescription>
      </DialogHeader>
      <DialogFooter>
        <Button variant="outline" @click="isLeaveOpen = false">取消</Button>
        <Button variant="destructive" @click="handleLeave">确认离开</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>

  <!-- Owner 离开团队提示弹窗 -->
  <Dialog v-model:open="isOwnerLeaveOpen">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle class="text-lg font-semibold">
          <AlertTriangle class="inline-block h-5 w-5 mr-2 text-yellow-500" />
          离开团队
        </DialogTitle>
        <DialogDescription class="text-sm text-muted-foreground mt-2">
          作为团队 Owner，您需要先转让所有权或解散团队才能离开。
        </DialogDescription>
      </DialogHeader>
      <div class="py-4 space-y-4">
        <!-- 转让所有权选项 -->
        <div class="space-y-3">
          <div class="flex items-center gap-2">
            <ArrowRight class="h-5 w-5 text-blue-500" />
            <Label class="font-medium">转让所有权</Label>
          </div>
          <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p class="text-sm text-blue-700 mb-3">选择新的团队 owner。转让成功后，您可以再主动离开团队。</p>
            <Select v-model="selectedNewOwnerId">
              <SelectTrigger>
                <SelectValue placeholder="选择新负责人" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem
                  v-for="member in transferableMembers"
                  :key="member.userId"
                  :value="String(member.userId)"
                >
                  {{ member.username }} ({{ member.role }})
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
        <!-- 分割线 -->
        <div class="flex items-center gap-4 py-2">
          <div class="flex-1 h-px bg-gray-200"></div>
          <span class="text-xs text-gray-400">或</span>
          <div class="flex-1 h-px bg-gray-200"></div>
        </div>
        <!-- 解散团队选项 -->
        <div class="flex items-center gap-2">
          <Trash2 class="h-5 w-5 text-red-500" />
          <Label class="font-medium text-red-600">解散团队</Label>
          <p class="text-sm text-red-500">此操作将解散整个团队，所有成员将自动离开</p>
        </div>
      </div>
      <DialogFooter>
        <Button variant="outline" @click="isOwnerLeaveOpen = false">取消</Button>
        <Button 
          variant="default" 
          @click="handleTransferOwnership" 
          :disabled="selectedNewOwnerId === UNASSIGNED_VALUE || transferableMembers.length === 0"
          class="mr-2"
        >
          转让所有权
        </Button>
        <Button variant="destructive" @click="handleCancelOwnerLeave">解散团队</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>

  <!-- 解散团队确认弹窗 -->
  <Dialog v-model:open="isDisbandOpen">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle class="text-lg font-semibold">
          <AlertTriangle class="inline-block h-5 w-5 mr-2 text-red-500" />
          确认解散团队
        </DialogTitle>
        <DialogDescription class="text-sm text-muted-foreground mt-2">
          此操作将解散「{{ team?.name }}」团队，团队状态将变为"已解散"并保留历史数据。
          <br />
          <strong class="text-red-600">请注意：此操作不可撤销！</strong>
        </DialogDescription>
      </DialogHeader>
      <div class="py-4 space-y-2 text-sm text-muted-foreground">
        <p>解散后：</p>
        <ul class="list-disc list-inside space-y-1">
          <li>团队状态将变为"已解散"</li>
          <li>团队任务和成员信息会作为历史数据保留</li>
          <li>普通成员不能继续访问团队任务</li>
          <li>Owner/Admin 只能查看历史，不能继续写入或管理</li>
        </ul>
      </div>
      <DialogFooter>
        <Button variant="outline" @click="isDisbandOpen = false">取消</Button>
        <Button variant="destructive" @click="handleDisband">确认解散</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<route lang="yaml">
meta:
  auth: true
</route>
