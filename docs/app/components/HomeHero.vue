<script setup>
import { useMediaQuery } from "@vueuse/core";

const showSample = useMediaQuery("(min-width: 1024px)");
</script>

<template>
    <div class="sample-container">
        <div class="left-section">
            <h1 class="text-5xl font-bold">caif</h1>
            <p class="text-lg text-gray-500">
                Compose Animated Interactions Framework. Explore the interactive WasmJS sample and API documentation.
            </p>
            <div class="flex gap-3 flex-wrap mt-4">
                <UButton
                    color="neutral"
                    icon="i-lucide-code"
                    size="xl"
                    to="/other/dokka/index.html"
                    target="_blank"
                    rel="noopener noreferrer"
                    trailing-icon="i-lucide-arrow-right"
                    >API Reference</UButton
                >
                <UButton
                    v-if="!showSample"
                    color="neutral"
                    icon="i-lucide-eye"
                    size="xl"
                    to="/other/sample/index.html"
                    variant="outline"
                    target="_blank"
                    rel="noopener noreferrer"
                    >Sample</UButton
                >
            </div>
        </div>

        <div v-if="showSample" class="right-section">
            <iframe
                src="/other/sample/index.html"
                class="w-full h-full border border-accented rounded-xl shadow-lg"
            />
        </div>
    </div>
</template>

<style scoped>
.sample-enter-active,
.sample-leave-active {
    transition: all 0.4s;
}

.sample-enter-from,
.sample-leave-to {
    opacity: 0;
    transform: scale(0.98);
}

.sample-enter-to,
.sample-leave-from {
    opacity: 1;
    transform: translateY(0) scale(1);
}

.right-section {
    will-change: transform, opacity;
}

.sample-container {
    display: flex;
    flex-direction: row;
    gap: 32px;
    margin: 32px;
    height: 600px;
}

.left-section {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: flex-start;
    padding: 0 24px;
}

.right-section {
    flex: 1;
    height: 600px;
    overflow: hidden;
}
</style>
