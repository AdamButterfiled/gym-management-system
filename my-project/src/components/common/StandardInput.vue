<template>
  <div class="standard-input-wrapper" :class="wrapperClass">
    <a-input 
      v-bind="$attrs" 
      :class="['std-input', `variant-${variant}`]"
      :style="{ width: width }"
      @keydown.enter="$emit('pressEnter')"
    >
      <template #prefix v-if="$slots.prefix">
        <slot name="prefix" />
      </template>
      <template #suffix v-if="$slots.suffix">
        <slot name="suffix" />
      </template>
      <template #addonBefore v-if="$slots.addonBefore">
        <slot name="addonBefore" />
      </template>
      <template #addonAfter v-if="$slots.addonAfter">
        <slot name="addonAfter" />
      </template>
    </a-input>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps({
    variant: {
        type: String, // 'default' (white) | 'grey' (F7F5F5) | 'glass'
        default: 'default' 
    },
    noMargin: {
        type: Boolean,
        default: false
    },
    width: {
        type: String,
        default: '100%'
    }
});

defineEmits(['pressEnter']);

const wrapperClass = computed(() => {
    return {
        'has-margin': !props.noMargin
    }
});
</script>

<style scoped>
.standard-input-wrapper.has-margin {
    margin-top: 10px;
}

/* Base Input Style */
:deep(.std-input) {
    height: 40px;
    border-radius: 8px;
    font-size: 14px;
    transition: all 0.3s;
}

/* Variant: Default (White Background) */
:deep(.std-input.variant-default) {
    background-color: #FFFFFF !important;
    border: 1px solid #d9d9d9;
    color: #333;
}
:deep(.std-input.variant-default:hover),
:deep(.std-input.variant-default:focus) {
    border-color: #40a9ff;
}

/* Variant: Grey (Universal Search) */
:deep(.std-input.variant-grey) {
    background-color: #F7F5F5 !important;
    border: 1px solid transparent;
    color: #333;
}
:deep(.std-input.variant-grey:hover) {
    background-color: #F0EEEE !important;
    border-color: #d9d9d9;
}
:deep(.std-input.variant-grey:focus),
:deep(.std-input.variant-grey.ant-input-focused),
:deep(.std-input.variant-grey.ant-input-affix-wrapper-focused) {
    background-color: #F0EEEE !important;
    border-color: #d9d9d9;
    box-shadow: none !important;
}

/* Variant: Glass (Transparent Mode) - For Search Bars */
:deep(.std-input.variant-glass) {
    background-color: rgba(255, 255, 255, 0.6) !important;
    border: 1px solid rgba(0, 0, 0, 0.05);
    backdrop-filter: blur(8px);
    box-shadow: none;
}
:deep(.std-input.variant-glass:hover) {
    background-color: rgba(255, 255, 255, 0.8) !important;
    border-color: rgba(0, 0, 0, 0.1);
}
:deep(.std-input.variant-glass:focus),
:deep(.std-input.variant-glass.ant-input-focused),
:deep(.std-input.variant-glass.ant-input-affix-wrapper-focused) {
    background-color: rgba(255, 255, 255, 0.9) !important;
    border-color: #f7b500;
    box-shadow: 0 0 0 2px rgba(247, 181, 0, 0.1);
}

/* Fix Icon Colors */
:deep(.ant-input-prefix) {
    color: #999;
}

/* ===== DARK MODE ===== */

/* Dark: Grey variant */
:global(.dark) :deep(.std-input.variant-grey) {
    background-color: rgba(255, 255, 255, 0.06) !important;
    border: 1px solid rgba(255, 255, 255, 0.1) !important;
    color: #ffffffd9 !important;
}
:global(.dark) :deep(.std-input.variant-grey:hover) {
    background-color: rgba(255, 255, 255, 0.1) !important;
    border-color: rgba(255, 255, 255, 0.15) !important;
}
:global(.dark) :deep(.std-input.variant-grey:focus),
:global(.dark) :deep(.std-input.variant-grey.ant-input-focused),
:global(.dark) :deep(.std-input.variant-grey.ant-input-affix-wrapper-focused) {
    background-color: rgba(255, 255, 255, 0.1) !important;
    border-color: rgba(255, 255, 255, 0.2) !important;
    box-shadow: none !important;
}

/* Dark: Glass variant */
:global(.dark) :deep(.std-input.variant-glass) {
    background-color: transparent !important;
    border: 1px solid rgba(255, 255, 255, 0.08) !important;
    color: #ffffffd9 !important;
    backdrop-filter: none; 
    box-shadow: none !important;
}

/* Force affix wrapper transparency for glass variant in dark mode */
:global(.dark) :deep(.ant-input-affix-wrapper:has(.std-input.variant-glass)),
:global(.dark) :deep(.standard-input-wrapper .ant-input-affix-wrapper) {
    background-color: transparent !important;
    border-color: rgba(255, 255, 255, 0.08) !important;
}
:global(.dark) :deep(.std-input.variant-glass:hover) {
    background-color: rgba(255, 255, 255, 0.05) !important;
    border-color: rgba(255, 255, 255, 0.15) !important;
}
:global(.dark) :deep(.std-input.variant-glass:focus),
:global(.dark) :deep(.std-input.variant-glass.ant-input-focused),
:global(.dark) :deep(.std-input.variant-glass.ant-input-affix-wrapper-focused) {
    background-color: transparent !important;
    border-color: rgba(255, 255, 255, 0.2) !important;
    box-shadow: none !important; /* No glow */
}

/* Dark: Input prefix icon */
:global(.dark) :deep(.ant-input-prefix) {
    color: rgba(255, 255, 255, 0.45) !important;
}

/* Dark: Placeholder */
:global(.dark) :deep(.ant-input::placeholder) {
    color: rgba(255, 255, 255, 0.3) !important;
}

/* Dark: Wrapper & Inner Input Clean Sweep */
:global(.dark) :deep(.ant-input-affix-wrapper),
:global(.dark) :deep(.ant-input-affix-wrapper:hover),
:global(.dark) :deep(.ant-input-affix-wrapper:focus),
:global(.dark) :deep(.ant-input-affix-wrapper.ant-input-affix-wrapper-focused) {
    background-color: transparent !important;
    border-color: rgba(255, 255, 255, 0.2) !important;
    box-shadow: none !important;
}

:global(.dark) :deep(.ant-input-affix-wrapper > input.ant-input),
:global(.dark) :deep(.ant-input),
:global(.dark) :deep(input) {
    background-color: transparent !important;
    color: #ffffffd9 !important;
    box-shadow: none !important;
}
</style>
