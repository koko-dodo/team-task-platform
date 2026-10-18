import type { RouteLocationRaw } from 'vue-router'

export const RouterPath: Record<string, RouteLocationRaw> = {
  HOME: '/tasks',
  LOGIN: '/auth/sign-in',
} as const
