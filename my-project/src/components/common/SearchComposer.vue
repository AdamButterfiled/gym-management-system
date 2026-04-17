<template>
  <div class="search-composer" :style="{ width }">
    <StandardInput
      :value="modelValue"
      no-margin
      class="search-input search-input--refined"
      :placeholder="placeholder"
      @update:value="emit('update:modelValue', String($event ?? ''))"
      @pressEnter="emit('search')"
    />
    <StandardButton
      type="ghost"
      size="icon-sm"
      icon="search"
      class="search-icon-button"
      :aria-label="actionLabel"
      @click="emit('search')"
    />
  </div>
</template>

<script setup lang="ts">
import StandardButton from '@/components/common/StandardButton.vue';
import StandardInput from '@/components/common/StandardInput.vue';

withDefaults(
  defineProps<{
    modelValue?: string;
    placeholder?: string;
    actionLabel?: string;
    width?: string;
  }>(),
  {
    modelValue: '',
    placeholder: '请输入关键词',
    actionLabel: '搜索',
    width: '252px',
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'search'): void;
}>();
</script>
