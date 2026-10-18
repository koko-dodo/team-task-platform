import {
  Circle,
  CircleCheck,
  TimerOff,
} from 'lucide-vue-next'
import { h } from 'vue'

/** 与库表默认 / 后端约定一致：TODO、IN_PROGRESS、DONE */
export const statuses = [
  {
    value: 'TODO',
    label: '待办',
    icon: h(Circle),
  },
  {
    value: 'IN_PROGRESS',
    label: '进行中',
    icon: h(TimerOff),
  },
  {
    value: 'DONE',
    label: '已完成',
    icon: h(CircleCheck),
  },
]

/** LOW / MEDIUM / HIGH */
export const priorities = [
  {
    value: 'low',
    label: '不紧急不重要',
    style: 'bg-green-400 text-white border-green-500',
  },
  {
    value: 'medium',
    label: '重要不紧急',
    style: 'bg-yellow-400 text-white border-yellow-500',
  },
  {
    value: 'high',
    label: '紧急不重要',
    style: 'bg-orange-400 text-white border-orange-500',
  },
  {
    value: 'urgent',
    label: '紧急重要',
    style: 'bg-red-500 text-white border-red-600',
  },
]
