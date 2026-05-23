<template>
  <template v-if="variant === 'flow'">
    <template v-for="field in renderFields" :key="field.fieldKey">
      <slot :name="slotName(field.fieldKey)" :field="field">
        <a-form-item :label="field.label">
          <a-input :value="field.placeholder || field.label" disabled />
        </a-form-item>
      </slot>
    </template>
  </template>

  <div v-else class="configured-form-layout">
    <div
      v-for="field in renderFields"
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
import { computed, useSlots } from 'vue';
import type { CSSProperties } from 'vue';
import type { FormFieldConfig } from '@/types/formConfig';
import {
  FORM_LAYOUT_GRID_COLUMNS,
  FORM_LAYOUT_GRID_PRECISION,
  FORM_LAYOUT_RENDER_COLUMNS,
  normalizeConfiguredFieldLayout,
  normalizeConfiguredFormFields,
  toFormLayoutRenderTrack,
} from '@/utils/formLayoutRuntime';

const props = defineProps<{
  fields: FormFieldConfig[];
  variant?: 'grid' | 'flow';
  includeHidden?: boolean;
}>();

const slots = useSlots();

const fallbackSlotFields = computed<FormFieldConfig[]>(() =>
  Object.keys(slots)
    .filter((name) => name.startsWith('field-'))
    .map((name, index) => {
      const fieldKey = name.slice('field-'.length);
      return {
        fieldKey,
        label: humanizeFieldKey(fieldKey),
        queryKey: fieldKey,
        visible: true,
        order: index,
        columnVisible: true,
        columnOrder: index,
        layout: {
          x: 0,
          y: index,
          w: FORM_LAYOUT_GRID_COLUMNS,
          h: 1,
        },
      };
    })
);

const renderFields = computed(() =>
  normalizeConfiguredFormFields((props.fields || []).length ? props.fields : fallbackSlotFields.value, {
    includeHidden: props.includeHidden,
  })
);

function slotName(fieldKey: string) {
  return `field-${fieldKey}`;
}

function fieldStyle(field: FormFieldConfig): CSSProperties {
  const layout = normalizeConfiguredFieldLayout(field.layout, field.order ?? field.columnOrder ?? 0);
  const x = toFormLayoutRenderTrack(layout.x || 0);
  const w = Math.max(FORM_LAYOUT_GRID_PRECISION, toFormLayoutRenderTrack(layout.w || 12));
  return {
    gridColumn: `${Math.max(1, x + 1)} / span ${Math.min(FORM_LAYOUT_RENDER_COLUMNS, w)}`,
    gridRow: `${Math.max(1, (layout.y || 0) + 1)} / span ${Math.max(1, layout.h || 1)}`,
    paddingLeft: (layout.x || 0) > 0 ? '8px' : undefined,
    paddingRight: (layout.x || 0) + (layout.w || 12) < FORM_LAYOUT_GRID_COLUMNS ? '8px' : undefined,
  };
}

function humanizeFieldKey(fieldKey: string) {
  return fieldKey
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/[-_]/g, ' ')
    .trim();
}
</script>

<style scoped>
.configured-form-layout {
  display: grid;
  grid-template-columns: repeat(240, minmax(0, 1fr));
  grid-auto-rows: minmax(56px, auto);
  gap: 0;
}

.configured-form-layout__cell {
  box-sizing: border-box;
  min-width: 0;
}
</style>
