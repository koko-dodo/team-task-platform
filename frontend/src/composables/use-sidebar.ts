import { ListTodo, Users } from 'lucide-vue-next'
import type { NavGroup } from '@/components/app-sidebar/types'

export function useSidebar() {
  const navData = ref<NavGroup[]> ([
    {
      title: '',
      items: [
        { title: '任务列表', url: '/tasks', icon: ListTodo },
        { title: '我的团队', url: '/teams', icon: Users },
      ],
    },
  ])
  return {
    navData,
  }
}
