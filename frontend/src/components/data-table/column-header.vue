<script setup lang="ts" generic="T">
import type { Column } from '@tanstack/vue-table'

interface DataTableColumnHeaderProps {
  column: Column<T, any>
  title: string
}

const props = defineProps<DataTableColumnHeaderProps>()
</script>

<script lang="ts">
export default {
  inheritAttrs: false,
}
</script>

<template>
  <div :class="$attrs?.class ?? ''">
    {{ title }}
  </div>
</template>
