<script lang="ts">
export default {
  name: 'StandardInput',
  inheritAttrs: false,
};
</script>

<template>
  <div class="standard-input-wrapper" :class="[wrapperClass, outerClass]" :style="{ width }">
    <div :class="['std-input', `variant-${variant}`, { 'std-input--with-prefix': hasPrefix, 'std-input--with-suffix': hasSuffix }]">
      <span v-if="$slots.addonBefore" class="std-input-addon std-input-addon--before">
        <slot name="addonBefore" />
      </span>

      <div class="std-input-inner">
        <span v-if="hasPrefix" class="std-input-prefix">
          <slot name="prefix" />
        </span>

        <Input
          v-bind="inputAttrs"
          :model-value="currentValue"
          class="std-input-control"
          @update:modelValue="handleValueChange"
          @keydown.enter="$emit('pressEnter')"
          @focus="$emit('focus', $event)"
          @blur="$emit('blur', $event)"
        />

        <button
          v-if="clearable && hasValue"
          type="button"
          class="std-input-clear"
          aria-label="清空输入"
          @click="clearValue"
        >
          <CircleX />
        </button>

        <span v-if="hasSuffix" class="std-input-suffix">
          <slot name="suffix" />
        </span>
      </div>

      <span v-if="$slots.addonAfter" class="std-input-addon std-input-addon--after">
        <slot name="addonAfter" />
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, useAttrs, useSlots } from 'vue';
import { CircleX } from 'lucide-vue-next';
import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils';

const props = defineProps<{
  value?: string | number;
  modelValue?: string | number;
  variant?: 'default' | 'grey' | 'glass';
  noMargin?: boolean;
  width?: string;
  allowClear?: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:value', value: string | number): void;
  (e: 'update:modelValue', value: string | number): void;
  (e: 'pressEnter'): void;
  (e: 'change', value: string | number): void;
  (e: 'focus', event: FocusEvent): void;
  (e: 'blur', event: FocusEvent): void;
}>();

const attrs = useAttrs();
const slots = useSlots();

const variant = computed(() => props.variant || 'default');
const width = computed(() => props.width || '100%');
const outerClass = computed(() => attrs.class);
const hasPrefix = computed(() => Boolean(slots.prefix));
const hasSuffix = computed(() => Boolean(slots.suffix));
const currentValue = computed(() => props.value ?? props.modelValue ?? '');
const hasValue = computed(() => `${currentValue.value ?? ''}`.length > 0);
const clearable = computed(() => {
  return props.allowClear || attrs.allowClear !== undefined || attrs['allow-clear'] !== undefined;
});

const inputAttrs = computed(() => {
  const raw = attrs as Record<string, unknown>;
  const {
    class: _class,
    style: _style,
    allowClear: _allowClear,
    'allow-clear': _allowClearKebab,
    value: _value,
    modelValue: _modelValue,
    ...rest
  } = raw;

  return {
    ...rest,
    class: cn(
      'h-auto min-h-0 border-0 bg-transparent px-0 py-0 text-[14px] font-medium shadow-none outline-none',
      'focus-visible:border-transparent focus-visible:ring-0 focus-visible:ring-offset-0',
    ),
  };
});

const wrapperClass = computed(() => ({
  'has-margin': props.noMargin !== true,
}));

const handleValueChange = (nextValue: string | number) => {
  emit('update:value', nextValue);
  emit('update:modelValue', nextValue);
  emit('change', nextValue);
};

const clearValue = () => {
  handleValueChange('');
};
</script>

<style scoped>
.standard-input-wrapper.has-margin {
  margin-top: 10px;
}

.std-input {
  display: flex;
  align-items: stretch;
  min-height: 44px;
  border: 1px solid var(--mono-control-border);
  border-radius: var(--mono-radius-md);
  background: var(--mono-control-bg);
  color: var(--mono-control-text);
  box-shadow: none;
  transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.std-input.variant-grey {
  background: var(--mono-control-bg-hover);
}

.std-input.variant-glass {
  background: var(--mono-control-bg-muted);
}

.std-input:hover {
  border-color: var(--mono-control-border-strong);
}

.std-input:focus-within {
  border-color: var(--mono-control-border-strong);
  box-shadow: var(--mono-focus-ring);
}

.std-input-addon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  color: var(--mono-text-secondary);
  white-space: nowrap;
}

.std-input-addon--before {
  border-right: 1px solid var(--mono-line);
}

.std-input-addon--after {
  border-left: 1px solid var(--mono-line);
}

.std-input-inner {
  display: flex;
  flex: 1 1 auto;
  align-items: center;
  min-width: 0;
  padding: 0 14px;
  gap: 8px;
}

.std-input-prefix,
.std-input-suffix,
.std-input-clear {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--mono-text-tertiary);
}

.std-input-prefix,
.std-input-suffix {
  pointer-events: none;
}

.std-input-control {
  flex: 1 1 auto;
  min-width: 0;
  width: auto !important;
  height: 100% !important;
  background: transparent !important;
  color: var(--mono-control-text) !important;
}

.std-input-control::placeholder {
  color: var(--mono-text-tertiary) !important;
}

.std-input-clear {
  appearance: none;
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
  transition: color 0.18s ease;
}

.std-input-clear:hover {
  color: var(--mono-text-secondary);
}

.std-input-clear :deep(svg) {
  width: 16px;
  height: 16px;
}
</style>
