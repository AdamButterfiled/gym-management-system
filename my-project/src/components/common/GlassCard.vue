<template>
  <div class="glass-container" :class="variantClass" :style="customStyle">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  variant?: 'search' | 'table';
}>();

const variantClass = computed(() => ({
  'glass-container--search': props.variant === 'search',
  'glass-container--table': props.variant === 'table',
}));

const customStyle = computed(() => {
  if (props.variant === 'search') {
    return { marginTop: '0' };
  }

  if (props.variant === 'table') {
    return { paddingBottom: '20px', minHeight: 'auto' };
  }

  return {};
});
</script>

<style scoped>
.glass-container {
  margin-top: 20px;
  padding: 20px;
  border: none;
  border-radius: var(--mono-radius-xl);
  background: var(--mono-surface-bg-elevated);
  box-shadow: none;
}

.glass-container--search {
  margin-bottom: 0;
}

.glass-container--table {
  margin-bottom: 0;
}

html.theme-glass-global:not(.dark) .glass-container {
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

html.dark .glass-container {
  background: rgba(20, 20, 20, 0.9);
}
</style>
