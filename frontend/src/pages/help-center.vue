<script setup lang="ts">
import { BasicPage } from '@/components/global-layout'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'

interface ISocialMediaAccount {
  title: string
  desc: string
  url: string
}

const socialMediaAccounts = ref<ISocialMediaAccount[]>([
  { title: 'Frontend Template (Third Party)', desc: 'shadcn-vue-admin by Whbbit1999. This is an upstream resource, not a project-team contact.', url: 'https://github.com/Whbbit1999/shadcn-vue-admin' },
])
</script>

<template>
  <BasicPage
    title="Help Center"
    description="Third-party resources"
  >
    <Alert>
      <AlertTitle>Heads up!</AlertTitle>
      <AlertDescription>
        The following link belongs to the frontend template author, not this project team.
      </AlertDescription>
    </Alert>

    <ul class="grid grid-cols-1 gap-4 mt-4 sm:grid-cols-2 lg:grid-cols-3">
      <li v-for="account in socialMediaAccounts" :key="account.title">
        <a :href="account.url || '#'" target="_blank" rel="noopener noreferrer" class="block p-4 border rounded-lg shadow-sm bg-background border-border">
          <h2 class="mb-2 text-lg font-bold">
            {{ account.title }}
          </h2>
          <p class="text-sm text-stone-500">
            {{ account.desc }}
          </p>
        </a>
      </li>
    </ul>
  </BasicPage>
</template>
