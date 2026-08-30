<script setup lang="ts">
const props = withDefaults(defineProps<{
  text?: string
  privacyPolicy?: string | null
  icon?: string
}>(), {
  text: "We use cookies to improve your experience, analyze traffic, and personalize content.",
  icon: "ph:cookie-bold"
})

const isDismissed = ref(true)

onMounted(() => {
  isDismissed.value = sessionStorage.getItem('cookie-banner-dismissed') === 'true'
})

const dismiss = () => {
  sessionStorage.setItem('cookie-banner-dismissed', 'true')
  isDismissed.value = true
}
</script>

<template>
  <div
    v-if="!isDismissed"
    class="fixed z-50 bottom-4 left-4 right-4 sm:left-auto sm:right-6 sm:bottom-6 sm:max-w-xs
           rounded-xl border border-gray-200/50 bg-white p-4 shadow-xl ring-1 ring-gray-200/30
           dark:bg-zinc-900 dark:border-zinc-800 dark:ring-white/5"
  >
    <div class="flex items-start gap-3">
      <div class="flex shrink-0 items-center justify-center rounded-lg bg-primary-500/10 p-2 text-primary-600 dark:text-primary-400">
        <Icon :name="icon" class="h-5 w-5" />
      </div>

      <div class="flex-1">
        <p class="text-sm leading-relaxed dark:text-zinc-200">
          {{ text }}
        </p>
        <div class="mt-4 flex flex-wrap items-center gap-2">
          <button
            @click="dismiss"
            class="rounded-lg bg-primary-600 px-4 py-1.5 text-xs font-bold text-white hover:bg-primary-700"
          >
            Dismiss
          </button>

          <NuxtLink
            v-if="privacyPolicy"
            :to="privacyPolicy"
            target="_blank"
            class="text-xs text-gray-500 underline hover:text-gray-700 dark:text-zinc-400 dark:hover:text-zinc-200"
          >
            Privacy Policy
          </NuxtLink>
        </div>
      </div>
    </div>
  </div>
</template>
