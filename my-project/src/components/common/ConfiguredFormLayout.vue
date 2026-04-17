<template>
  <template v-if="variant === 'flow'">
    <template v-for="field in visibleFields" :key="field.fieldKey">
      <slot :name="slotName(field.fieldKey)" :field="field">
        <a-form-item :label="field.label">
          <a-input :value="field.placeholder || field.label" disabled />
        </a-form-item>
      </slot>
    </template>
  </template>

  <div v-else class="configured-form-layout">
    <div
      v-for="field in visibleFields"
      :key="field.fieldKey"
      class="configured-form-layout__cell"
      :style="fieldStyle(field)"
    >
      <slot :name="slotName(field.fieldKey)" :field="field">
        <a-form-item :label="field.label">
          <a-input :value="field.placeholder || field.label" disabled />
        </a-form-item>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { CSSProperties } from 'vue';
import type { FormFieldConfig } from '@/types/formConfig';

const props = defineProps<{
  fields: FormFieldConfig[];
  variant?: 'grid' | 'flow';
}>();

const visibleFields = computed(() =>
  [...(props.fields || [])]
    .filter((field) => (field.visible ?? field.columnVisible ?? true) !== false)
    .sort((left, right) => {
      const leftLayout = left.layout || { x: 0, y: left.order ?? left.columnOrder ?? 0, w: 12, h: 1 };
      const rightLayout = right.layout || { x: 0, y: right.order ?? right.columnOrder ?? 0, w: 12, h: 1 };
      if (leftLayout.y !== rightLayout.y) {
        return leftLayout.y - rightLayout.y;
      }
      if (leftLayout.x !== rightLayout.x) {
        return leftLayout.x - rightLayout.x;
      }
      return (left.order ?? left.columnOrder ?? 0) - (right.order ?? right.columnOrder ?? 0);
    })
);

function slotName(fieldKey: string) {
  return `field-${fieldKey}`;
}

function fieldStyle(field: FormFieldConfig): CSSProperties {
  const layout = field.layout || { x: 0, y: field.order ?? field.columnOrder ?? 0, w: 12, h: 1 };
  return {
    gridColumn: `${Math.max(1, (layout.x || 0) + 1)} / span ${Math.min(24, Math.max(1, layout.w || 12))}`,
    gridRow: `${Math.max(1, (layout.y || 0) + 1)} / span ${Math.max(1, layout.h || 1)}`,
  };
}
</script>

<style scoped>
.configured-form-layout {
  display: grid;
  grid-template-columns: repeat(24, minmax(0, 1fr));
  grid-auto-rows: minmax(56px, auto);
  gap: 0 16px;
}

.configured-form-layout__cell {
  min-width: 0;
}
</style>
