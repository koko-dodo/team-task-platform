<script setup lang="ts">
import type { Table } from '@tanstack/vue-table'

import type { Task } from '../data/schema'


interface DataTableToolbarProps {
  table: Table<Task>
}

const props = defineProps<DataTableToolbarProps>()

const isFiltered = computed(() => props.table.getState().columnFilters.length > 0)
</script>

<template>
  
</template>
