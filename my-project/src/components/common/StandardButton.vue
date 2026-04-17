<template>
  <Button
    v-bind="$attrs"
    :variant="buttonVariant"
    :size="buttonSize"
    :class="buttonClass"
    :disabled="buttonDisabled"
    @click="$emit('click', $event)"
  >
    <slot name="icon">
      <component
        :is="iconComponent"
        v-if="iconComponent"
        aria-hidden="true"
        :class="['std-button-icon', { 'std-button-icon--spinning': loading }]"
      />
    </slot>
    <template v-if="$slots.default">
      <span v-if="wrapLabel" class="std-button-label"><slot /></span>
      <slot v-else />
    </template>
  </Button>
</template>

<script setup lang="ts">
import { computed, useAttrs, useSlots } from 'vue';
import { Funnel, LoaderCircle, Plus, RotateCcw, Search, Trash2 } from 'lucide-vue-next';
import { Button } from '@/components/ui/button';
import type { ButtonVariants } from '@/components/ui/button';
import { cn } from '@/lib/utils';

type StandardButtonType =
  | 'search'
  | 'reset'
  | 'add'
  | 'delete'
  | 'default'
  | 'primary'
  | 'filter'
  | 'link'
  | 'ghost'
  | 'text'
  | 'icon';

const props = withDefaults(defineProps<{
  type?: StandardButtonType;
  size?: ButtonVariants['size'];
  loading?: boolean;
  icon?: 'search' | 'reload' | 'plus' | 'delete' | 'filter';
  danger?: boolean;
  wrapLabel?: boolean;
}>(), {
  type: 'default',
  wrapLabel: true,
});

defineEmits(['click']);

const attrs = useAttrs();
const slots = useSlots();

const resolvedType = computed(() => props.type);
const wrapLabel = computed(() => props.wrapLabel);
const hasLabel = computed(() => Boolean(slots.default));
const hasVisualIcon = computed(() => Boolean(slots.icon) || props.loading || Boolean(props.icon));

const isPrimary = computed(() => ['primary', 'add', 'search'].includes(resolvedType.value));
const isLinkLike = computed(() => ['link', 'text'].includes(resolvedType.value));
const isGhostLike = computed(() => ['ghost', 'icon'].includes(resolvedType.value));
const isDanger = computed(() => props.danger || resolvedType.value === 'delete');

const buttonVariant = computed<ButtonVariants['variant']>(() => {
  if (isLinkLike.value || isGhostLike.value) {
    return 'ghost';
  }

  if (resolvedType.value === 'delete') {
    return 'destructive';
  }

  if (isPrimary.value) {
    return 'default';
  }

  return 'outline';
});

const buttonSize = computed<ButtonVariants['size']>(() => {
  if (props.size) {
    return props.size;
  }

  if (resolvedType.value === 'icon') {
    return 'icon';
  }

  if (isLinkLike.value) {
    return 'sm';
  }

  return 'lg';
});

const buttonDisabled = computed(() => {
  return props.loading || attrs.disabled === '' || attrs.disabled === true || attrs.disabled === 'true';
});

const buttonClass = computed(() => cn(
  'std-button border shadow-none rounded-[var(--mono-radius-pill)] text-[14px] tracking-normal transition-all duration-200 ease-[cubic-bezier(0.22,1,0.36,1)]',
  hasLabel.value && !isLinkLike.value && resolvedType.value !== 'icon' ? 'min-w-[96px]' : 'min-w-0',
  hasLabel.value && hasVisualIcon.value && 'std-button--icon-text',
  `std-button--${resolvedType.value}`,
  isDanger.value && 'std-button--danger',
  isPrimary.value && [
    'border-transparent',
    'bg-[var(--shad-primary-bg)]',
    'text-[var(--shad-primary-foreground)]',
    'hover:bg-[var(--shad-primary-hover)]',
    'hover:text-[var(--shad-primary-foreground)]',
    'focus-visible:ring-[var(--shad-ring)]',
    'focus-visible:border-transparent',
  ],
  resolvedType.value === 'delete' && [
    'border-[var(--button-danger-bg)]',
    'bg-[var(--button-danger-bg)]',
    'text-[var(--button-danger-text)]',
    'hover:border-[var(--button-danger-hover)]',
    'hover:bg-[var(--button-danger-hover)]',
    'hover:text-[var(--button-danger-text)]',
    'focus-visible:ring-[var(--button-danger-ring)]',
  ],
  !isPrimary.value && resolvedType.value !== 'delete' && [
    'border-[var(--mono-line-strong)]',
    'bg-[var(--mono-bg-elevated)]',
    'text-[var(--mono-text)]',
    'hover:border-[var(--mono-control-border-strong)]',
    'hover:bg-[var(--mono-surface-soft)]',
    'hover:text-[var(--mono-text)]',
    'focus-visible:ring-[var(--shad-ring)]',
  ],
));

const iconComponent = computed(() => {
  if (props.loading) {
    return LoaderCircle;
  }

  switch (props.icon) {
    case 'search':
      return Search;
    case 'reload':
      return RotateCcw;
    case 'plus':
      return Plus;
    case 'delete':
      return Trash2;
    case 'filter':
      return Funnel;
    default:
      return null;
  }
});
</script>

<style scoped>
.std-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: var(--std-button-font-size, 14px);
  font-weight: 500;
  line-height: 1;
  letter-spacing: 0;
  border: 1px solid var(--mono-line-strong);
  border-radius: var(--mono-radius-pill);
  background: var(--mono-bg-elevated);
  color: var(--mono-text);
  box-shadow: none !important;
}

.std-button:hover {
  border-color: var(--mono-control-border-strong);
  background: var(--mono-surface-soft);
  color: var(--mono-text);
}

.std-button:focus-visible {
  outline: none;
  box-shadow: var(--mono-focus-ring) !important;
}

.std-button.std-button--search,
.std-button.std-button--add,
.std-button.std-button--primary {
  border-color: var(--shad-primary-bg);
  background: var(--shad-primary-bg);
  color: var(--shad-primary-foreground);
}

.std-button.std-button--search:hover,
.std-button.std-button--add:hover,
.std-button.std-button--primary:hover {
  border-color: var(--shad-primary-hover);
  background: var(--shad-primary-hover);
  color: var(--shad-primary-foreground);
}

.std-button.std-button--delete {
  border-color: var(--button-danger-bg);
  background: var(--button-danger-bg);
  color: var(--button-danger-text);
}

.std-button.std-button--delete:hover {
  border-color: var(--button-danger-hover);
  background: var(--button-danger-hover);
  color: var(--button-danger-text);
}

.std-button.std-button--ghost {
  border-color: var(--mono-control-border);
  color: var(--mono-text-secondary);
}

.std-button.std-button--ghost:hover {
  border-color: var(--mono-control-border-strong);
  background: var(--mono-surface-soft);
  color: var(--mono-text);
}

.std-button.std-button--icon {
  width: 40px;
  min-width: 40px;
  padding: 0;
}

.std-button.std-button--link,
.std-button.std-button--text {
  height: auto !important;
  min-height: 0;
  padding: 0 !important;
  border-color: transparent !important;
  border-radius: 0;
  background: transparent !important;
  color: var(--mono-text-secondary) !important;
}

.std-button.std-button--link:hover,
.std-button.std-button--link:focus,
.std-button.std-button--link:active,
.std-button.std-button--text:hover,
.std-button.std-button--text:focus,
.std-button.std-button--text:active {
  background: transparent !important;
  color: var(--mono-text) !important;
}

.std-button.std-button--danger.std-button--link,
.std-button.std-button--danger.std-button--text,
.std-button.std-button--danger.std-button--ghost,
.std-button.std-button--danger.std-button--icon {
  color: var(--action-tone-danger) !important;
}

.std-button.std-button--danger.std-button--ghost,
.std-button.std-button--danger.std-button--icon {
  border-color: var(--tag-tone-red-border);
  background: var(--tag-tone-red-bg) !important;
}

.std-button.std-button--danger.std-button--ghost:hover,
.std-button.std-button--danger.std-button--icon:hover {
  border-color: var(--tag-tone-red-border);
  background: var(--tag-tone-red-bg) !important;
  color: var(--action-tone-danger) !important;
}

.std-button:disabled,
.std-button[disabled] {
  cursor: not-allowed;
  border-color: var(--mono-control-border) !important;
  background: var(--mono-control-bg-disabled) !important;
  color: var(--mono-control-text-disabled) !important;
}

.std-button-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font: inherit;
  letter-spacing: inherit;
  line-height: 1;
}

.std-button.std-button--icon-text .std-button-label {
  transform: translateY(var(--std-button-label-shift, 1px));
}

.std-button-icon {
  display: block;
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.std-button :deep(svg) {
  display: block;
  width: 16px;
  height: 16px;
}

.std-button :deep(.anticon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  line-height: 0;
  vertical-align: middle;
}

.std-button :deep(.anticon svg) {
  display: block;
  width: 16px;
  height: 16px;
}

.std-button-icon--spinning {
  animation: std-button-spin 0.8s linear infinite;
}

@keyframes std-button-spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}
</style>
