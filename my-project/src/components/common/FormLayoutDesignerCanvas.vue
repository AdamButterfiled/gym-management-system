<template>
  <div class="form-layout-designer" :class="{ 'form-layout-designer--dragging': isPointerDragging }">
    <div class="form-layout-designer__toolbar">
      <div class="form-layout-designer__toolbar-copy">
        <strong>{{ selectedField ? selectedField.label || selectedField.fieldKey : title || '表单预览' }}</strong>
        <span v-if="selectedField">
          {{ selectedField.fieldKey }} · {{ layoutText(selectedField) }}
        </span>
        <span v-else>
          拖进现有行会自动均分，拖到行间会新起一行；同一行内可直接拖拽调整左中右顺序。
        </span>
      </div>
      <div v-if="selectedField" class="form-layout-designer__toolbar-actions">
        <button type="button" class="designer-chip" @click="$emit('edit', selectedField)">编辑字段</button>
        <button type="button" class="designer-chip designer-chip--danger" @click="$emit('remove', selectedField.fieldKey)">
          删除字段
        </button>
      </div>
    </div>

    <section class="form-layout-designer__shell">
      <a-form
        :layout="resolvedLayoutMode"
        :label-col="resolvedLabelCol"
        :wrapper-col="resolvedWrapperCol"
        :class="['workspace-modal-form', 'form-layout-designer__form', normalizedFormClass]"
      >
        <div class="form-layout-designer__canvas">
          <template v-if="rows.length">
            <template v-for="(row, rowIndex) in rows" :key="row.rowKey">
              <div
                class="form-layout-designer__row-gap"
                :class="{ 'form-layout-designer__row-gap--active': isGapActive(rowIndex) }"
                data-drop-kind="gap"
                :data-row-index="rowIndex"
                @dragover.prevent="handleGapDragOver(rowIndex)"
                @drop.prevent="handleGapDrop(rowIndex)"
              >
                <span>拖到这里会新起一行</span>
              </div>

              <section
                class="form-layout-designer__row"
                :class="{ 'form-layout-designer__row--active': isRowActive(rowIndex) }"
                data-drop-kind="row"
                :data-row-index="rowIndex"
                @dragover.prevent="handleRowDragOver(rowIndex)"
                @drop.prevent="handleRowDrop(rowIndex)"
              >
                <article
                  v-for="(field, fieldIndex) in row.fields"
                  :key="field.fieldKey"
                  class="form-layout-designer__item"
                  :class="{
                    'form-layout-designer__item--selected': field.fieldKey === selectedFieldKey,
                    'form-layout-designer__item--hidden': !isFieldVisible(field),
                    'form-layout-designer__item--insert-before': isInsertBefore(rowIndex, fieldIndex),
                    'form-layout-designer__item--insert-after': isInsertAfter(rowIndex, fieldIndex),
                  }"
                  :style="itemStyle(field)"
                  data-drop-kind="item"
                  :data-field-key="field.fieldKey || ''"
                  :data-row-index="rowIndex"
                  :data-field-index="fieldIndex"
                  @pointerdown="handleItemPointerDown(field.fieldKey, $event)"
                  @click.stop="selectField(field.fieldKey)"
                  @dragover.prevent.stop="handleItemDragOver(rowIndex, fieldIndex, $event)"
                  @drop.prevent.stop="handleItemDrop(rowIndex, fieldIndex, $event)"
                >
                  <div class="form-layout-designer__item-tools">
                    <button
                      type="button"
                      class="designer-chip designer-chip--ghost designer-chip--drag"
                      :data-field-key="field.fieldKey || ''"
                      @pointerdown.stop.prevent="startPointerDrag(field.fieldKey, $event)"
                    >
                      拖拽
                    </button>
                    <button type="button" class="designer-chip designer-chip--ghost" data-no-drag="true" @click.stop="$emit('edit', field)">
                      编辑
                    </button>
                  </div>

                  <div class="form-layout-designer__preview">
                    <FormFieldPreviewRenderer :field="field" :height="getLayout(field).h" />
                  </div>

                  <button
                    type="button"
                    class="form-layout-designer__resize-edge"
                    data-no-drag="true"
                    @pointerdown.stop.prevent="startResize(field, rowIndex, 'width', $event)"
                  />

                  <button
                    type="button"
                    class="form-layout-designer__resize"
                    data-no-drag="true"
                    @pointerdown.stop.prevent="startResize(field, rowIndex, 'both', $event)"
                  />
                </article>
              </section>
            </template>

            <div
              class="form-layout-designer__row-gap"
              :class="{ 'form-layout-designer__row-gap--active': isGapActive(rows.length) }"
              data-drop-kind="gap"
              :data-row-index="rows.length"
              @dragover.prevent="handleGapDragOver(rows.length)"
              @drop.prevent="handleGapDrop(rows.length)"
            >
              <span>拖到这里会排在最后一行</span>
            </div>
          </template>

          <div v-else class="form-layout-designer__empty">
            当前目标还没有字段。新增字段后就会在这里按真实表单样式预览。
          </div>
        </div>
      </a-form>
    </section>

    <div
      v-if="dragGhost"
      class="form-layout-designer__drag-ghost"
      :style="{ transform: `translate3d(${dragGhost.x}px, ${dragGhost.y}px, 0)` }"
    >
      <span>{{ dragGhost.label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watchEffect } from 'vue';
import type { CSSProperties } from 'vue';
import FormFieldPreviewRenderer from '@/components/common/FormFieldPreviewRenderer.vue';
import type { FormFieldConfig, FormFieldLayout } from '@/types/formConfig';

const FORM_GRID_COLUMNS = 24;
const FORM_MIN_HEIGHT = 74;
const FORM_HEIGHT_UNIT = 82;
const FORM_HEIGHT_STEP = 88;

type DropState =
  | { kind: 'gap'; rowIndex: number }
  | { kind: 'row'; rowIndex: number }
  | { kind: 'item'; rowIndex: number; insertIndex: number };

interface DesignerRow {
  rowKey: string;
  rowIndex: number;
  fields: FormFieldConfig[];
}

const props = withDefaults(defineProps<{
  fields: FormFieldConfig[];
  title?: string;
  layoutMode?: string;
  formClass?: string;
  labelColSpan?: number | string | null;
  wrapperColSpan?: number | string | null;
}>(), {
  title: '',
  layoutMode: 'vertical',
  formClass: '',
  labelColSpan: null,
  wrapperColSpan: null,
});

defineEmits<{
  (e: 'edit', field: FormFieldConfig): void;
  (e: 'remove', fieldKey: string): void;
}>();

const selectedFieldKey = ref('');
const dragFieldKey = ref('');
const dropState = ref<DropState | null>(null);
const isPointerDragging = ref(false);
const dragGhost = ref<{ label: string; x: number; y: number } | null>(null);

let resizeCleanup: (() => void) | null = null;
let pointerDragCleanup: (() => void) | null = null;
let dropFrame = 0;
let queuedDropState: DropState | null = null;
let activePointerDropState: DropState | null = null;

const orderedFields = computed(() =>
  [...(props.fields || [])].sort((left, right) => {
    const leftLayout = getLayout(left);
    const rightLayout = getLayout(right);
    if (leftLayout.y !== rightLayout.y) {
      return leftLayout.y - rightLayout.y;
    }
    if (leftLayout.x !== rightLayout.x) {
      return leftLayout.x - rightLayout.x;
    }
    return (left.order ?? left.columnOrder ?? 0) - (right.order ?? right.columnOrder ?? 0);
  })
);

const rows = computed<DesignerRow[]>(() => {
  const grouped = new Map<number, FormFieldConfig[]>();
  orderedFields.value.forEach((field) => {
    const layout = getLayout(field);
    const rowFields = grouped.get(layout.y) || [];
    rowFields.push(field);
    grouped.set(layout.y, rowFields);
  });

  return [...grouped.entries()]
    .sort((left, right) => left[0] - right[0])
    .map(([rowIndex, fields]) => ({
      rowKey: fields.map((field) => field.fieldKey).join('|') || `row-${rowIndex}`,
      rowIndex,
      fields: [...fields].sort((left, right) => {
        const leftLayout = getLayout(left);
        const rightLayout = getLayout(right);
        if (leftLayout.x !== rightLayout.x) {
          return leftLayout.x - rightLayout.x;
        }
        return (left.order ?? left.columnOrder ?? 0) - (right.order ?? right.columnOrder ?? 0);
      }),
    }));
});

const selectedField = computed(() => orderedFields.value.find((field) => field.fieldKey === selectedFieldKey.value) || null);

const hasHorizontalGrid = computed(() => {
  const labelSpan = toPositiveNumber(props.labelColSpan);
  const wrapperSpan = toPositiveNumber(props.wrapperColSpan);
  return Boolean(labelSpan || wrapperSpan);
});

const resolvedLayoutMode = computed(() => {
  if (props.layoutMode === 'horizontal' || props.layoutMode === 'inline') {
    return props.layoutMode;
  }
  if (hasHorizontalGrid.value) {
    return 'horizontal';
  }
  return 'vertical';
});

const normalizedFormClass = computed(() => String(props.formClass || '').trim());

const resolvedLabelCol = computed(() => {
  if (resolvedLayoutMode.value !== 'horizontal') {
    return undefined;
  }
  const span = toPositiveNumber(props.labelColSpan);
  return span ? { span } : { span: 6 };
});

const resolvedWrapperCol = computed(() => {
  if (resolvedLayoutMode.value !== 'horizontal') {
    return undefined;
  }
  const span = toPositiveNumber(props.wrapperColSpan);
  return span ? { span } : { span: 18 };
});

watchEffect(() => {
  stabilizeRowLayouts();
});

watchEffect(() => {
  const keys = orderedFields.value.map((field) => field.fieldKey);
  if (!keys.includes(selectedFieldKey.value)) {
    selectedFieldKey.value = keys[0] || '';
  }
});

onBeforeUnmount(() => {
  resizeCleanup?.();
  pointerDragCleanup?.();
  if (dropFrame) {
    window.cancelAnimationFrame(dropFrame);
  }
});

function selectField(fieldKey: string) {
  selectedFieldKey.value = fieldKey;
}

function fieldDisplayLabel(fieldKey: string) {
  return orderedFields.value.find((field) => field.fieldKey === fieldKey)?.label || fieldKey;
}

function handleItemPointerDown(fieldKey: string, event: PointerEvent) {
  if (event.button !== 0) {
    return;
  }
  const target = event.target as HTMLElement | null;
  if (target?.closest('[data-no-drag="true"]')) {
    return;
  }
  event.preventDefault();
  const currentTarget = event.currentTarget as HTMLElement | null;
  startPointerDrag(fieldKey || currentTarget?.dataset.fieldKey || '', event);
}

function getLayout(field: FormFieldConfig): FormFieldLayout {
  const source = field.layout;
  const layout = {
    x: clampNumber(Math.round(source?.x ?? 0), 0, FORM_GRID_COLUMNS - 1),
    y: Math.max(0, Math.round(source?.y ?? field.order ?? field.columnOrder ?? 0)),
    w: clampNumber(Math.round(source?.w ?? 24), 1, FORM_GRID_COLUMNS),
    h: clampNumber(Math.round(source?.h ?? 1), 1, 6),
  };

  if (layout.x + layout.w > FORM_GRID_COLUMNS) {
    layout.x = Math.max(0, FORM_GRID_COLUMNS - layout.w);
  }

  return layout;
}

function ensureFieldLayout(field: FormFieldConfig): FormFieldLayout {
  const next = getLayout(field);
  if (!field.layout || isLayoutDifferent(field.layout, next)) {
    field.layout = next;
  }
  return field.layout;
}

function itemStyle(field: FormFieldConfig): CSSProperties {
  const layout = getLayout(field);
  return {
    gridColumn: `${clampNumber(layout.x + 1, 1, FORM_GRID_COLUMNS)} / span ${clampNumber(layout.w, 1, FORM_GRID_COLUMNS)}`,
    minHeight: `${FORM_MIN_HEIGHT + (layout.h - 1) * FORM_HEIGHT_STEP}px`,
  };
}

function layoutText(field: FormFieldConfig) {
  const layout = getLayout(field);
  return `第 ${layout.y + 1} 行 · 宽 ${layout.w}/24 · 高 ${layout.h}`;
}

function isFieldVisible(field: FormFieldConfig) {
  return (field.visible ?? field.columnVisible ?? true) !== false;
}

function isSelectField(field: FormFieldConfig) {
  return ['select', 'remote-api', 'remote-form'].includes(String(field.controlType || ''));
}

function isTextareaField(field: FormFieldConfig) {
  const text = `${field.label} ${field.fieldKey} ${field.placeholder || ''}`;
  return String(field.controlType || '') === 'textarea' || /备注|说明|描述|简介|内容|layout/i.test(text) || getLayout(field).h >= 2;
}

function textPlaceholder(field: FormFieldConfig) {
  return field.placeholder || `请输入${field.label || field.fieldKey}`;
}

function selectPlaceholder(field: FormFieldConfig) {
  return field.placeholder || `请选择${field.label || field.fieldKey}`;
}

function numberPlaceholder(field: FormFieldConfig, isStart = true) {
  if (field.controlType === 'number-range') {
    return isStart ? '最小值' : '最大值';
  }
  return field.placeholder || `请输入${field.label || field.fieldKey}`;
}

function mockSelectOptions(field: FormFieldConfig) {
  const label = field.label || field.fieldKey;
  if (/状态/.test(label)) {
    return [
      { label: '启用', value: 'enabled' },
      { label: '停用', value: 'disabled' },
    ];
  }
  return [
    { label: `${label} A`, value: 'a' },
    { label: `${label} B`, value: 'b' },
  ];
}

function startPointerDrag(fieldKey: string, event: PointerEvent) {
  if (event.button !== 0) {
    return;
  }
  const currentTarget = event.currentTarget as HTMLElement | null;
  const resolvedFieldKey = String(fieldKey || currentTarget?.dataset.fieldKey || '').trim();
  if (!resolvedFieldKey) {
    return;
  }
  pointerDragCleanup?.();
  dragFieldKey.value = resolvedFieldKey;
  selectedFieldKey.value = resolvedFieldKey;
  dropState.value = null;
  queuedDropState = null;
  activePointerDropState = null;
  isPointerDragging.value = true;

  const dragHandle = event.currentTarget as HTMLElement | null;
  dragHandle?.classList.add('is-dragging');
  dragHandle?.setPointerCapture?.(event.pointerId);
  document.body.classList.add('form-layout-designer-pointer-dragging');
  dragGhost.value = {
    label: fieldDisplayLabel(resolvedFieldKey),
    x: event.clientX + 14,
    y: event.clientY + 14,
  };
  updateDropStateFromPointer(event.clientX, event.clientY);

  const handleMove = (moveEvent: PointerEvent) => {
    dragGhost.value = {
      label: fieldDisplayLabel(resolvedFieldKey),
      x: moveEvent.clientX + 14,
      y: moveEvent.clientY + 14,
    };
    updateDropStateFromPointer(moveEvent.clientX, moveEvent.clientY);
    autoScrollWhileDragging(moveEvent.clientY);
  };

  const handleUp = (upEvent?: PointerEvent) => {
    if (upEvent) {
      updateDropStateFromPointer(upEvent.clientX, upEvent.clientY);
    }
    dragHandle?.releasePointerCapture?.(event.pointerId);
    commitPointerDrop();
    window.removeEventListener('pointermove', handleMove);
    window.removeEventListener('pointerup', handleUp);
    window.removeEventListener('pointercancel', handleUp);
    dragHandle?.classList.remove('is-dragging');
    document.body.classList.remove('form-layout-designer-pointer-dragging');
    pointerDragCleanup = null;
    isPointerDragging.value = false;
    handleDragEnd();
  };

  window.addEventListener('pointermove', handleMove);
  window.addEventListener('pointerup', handleUp);
  window.addEventListener('pointercancel', handleUp);
  pointerDragCleanup = () => {
    window.removeEventListener('pointermove', handleMove);
    window.removeEventListener('pointerup', handleUp);
    window.removeEventListener('pointercancel', handleUp);
    dragHandle?.releasePointerCapture?.(event.pointerId);
    dragHandle?.classList.remove('is-dragging');
    document.body.classList.remove('form-layout-designer-pointer-dragging');
    isPointerDragging.value = false;
    dragGhost.value = null;
    pointerDragCleanup = null;
  };
}

function handleDragEnd() {
  pointerDragCleanup?.();
  dragFieldKey.value = '';
  dropState.value = null;
  queuedDropState = null;
  activePointerDropState = null;
  dragGhost.value = null;
  if (dropFrame) {
    window.cancelAnimationFrame(dropFrame);
    dropFrame = 0;
  }
}

function updateDropStateFromPointer(clientX: number, clientY: number) {
  const targets = document.elementsFromPoint(clientX, clientY) as HTMLElement[];
  if (!targets.length) {
    setPointerDropState(null);
    return;
  }

  const target = targets.find((element) => element.closest('[data-drop-kind]')) || null;
  if (!target) {
    setPointerDropState(null);
    return;
  }

  const item = target.closest('[data-drop-kind="item"]') as HTMLElement | null;
  if (item) {
    const rowIndex = Number(item.dataset.rowIndex || 0);
    const fieldIndex = Number(item.dataset.fieldIndex || 0);
    const rect = item.getBoundingClientRect();
    const insertIndex = clientX < rect.left + rect.width / 2 ? fieldIndex : fieldIndex + 1;
    setPointerDropState({ kind: 'item', rowIndex, insertIndex });
    return;
  }

  const gap = target.closest('[data-drop-kind="gap"]') as HTMLElement | null;
  if (gap) {
    setPointerDropState({ kind: 'gap', rowIndex: Number(gap.dataset.rowIndex || 0) });
    return;
  }

  const row = target.closest('[data-drop-kind="row"]') as HTMLElement | null;
  if (row) {
    setPointerDropState({ kind: 'row', rowIndex: Number(row.dataset.rowIndex || 0) });
    return;
  }

  setPointerDropState(null);
}

function commitPointerDrop() {
  if (!dragFieldKey.value) {
    return;
  }

  const nextDropState = activePointerDropState || queuedDropState || dropState.value;
  if (!nextDropState) {
    return;
  }

  if (nextDropState.kind === 'gap') {
    moveFieldToNewRow(dragFieldKey.value, nextDropState.rowIndex);
    return;
  }

  if (nextDropState.kind === 'row') {
    const row = rows.value[nextDropState.rowIndex];
    moveFieldToRow(dragFieldKey.value, nextDropState.rowIndex, row?.fields.length || 0);
    return;
  }

  moveFieldToRow(dragFieldKey.value, nextDropState.rowIndex, nextDropState.insertIndex);
}

function autoScrollWhileDragging(clientY: number) {
  const edgePadding = 88;
  const scrollStep = 18;
  if (clientY < edgePadding) {
    window.scrollBy({ top: -scrollStep, behavior: 'auto' });
    return;
  }
  if (clientY > window.innerHeight - edgePadding) {
    window.scrollBy({ top: scrollStep, behavior: 'auto' });
  }
}

function handleGapDragOver(rowIndex: number) {
  if (!dragFieldKey.value) {
    return;
  }
  setPointerDropState({ kind: 'gap', rowIndex });
}

function handleGapDrop(rowIndex: number) {
  if (!dragFieldKey.value) {
    return;
  }
  moveFieldToNewRow(dragFieldKey.value, rowIndex);
  handleDragEnd();
}

function handleRowDragOver(rowIndex: number) {
  if (!dragFieldKey.value) {
    return;
  }
  setPointerDropState({ kind: 'row', rowIndex });
}

function handleRowDrop(rowIndex: number) {
  if (!dragFieldKey.value) {
    return;
  }
  const row = rows.value[rowIndex];
  moveFieldToRow(dragFieldKey.value, rowIndex, row?.fields.length || 0);
  handleDragEnd();
}

function handleItemDragOver(rowIndex: number, fieldIndex: number, event: DragEvent) {
  if (!dragFieldKey.value) {
    return;
  }
  const target = event.currentTarget as HTMLElement | null;
  if (!target) {
    return;
  }
  const rect = target.getBoundingClientRect();
  const insertIndex = event.clientX < rect.left + rect.width / 2 ? fieldIndex : fieldIndex + 1;
  setPointerDropState({ kind: 'item', rowIndex, insertIndex });
}

function handleItemDrop(rowIndex: number, fieldIndex: number, event: DragEvent) {
  if (!dragFieldKey.value) {
    return;
  }
  const target = event.currentTarget as HTMLElement | null;
  if (!target) {
    return;
  }
  const rect = target.getBoundingClientRect();
  const insertIndex = event.clientX < rect.left + rect.width / 2 ? fieldIndex : fieldIndex + 1;
  moveFieldToRow(dragFieldKey.value, rowIndex, insertIndex);
  handleDragEnd();
}

function isGapActive(rowIndex: number) {
  return dropState.value?.kind === 'gap' && dropState.value.rowIndex === rowIndex;
}

function isRowActive(rowIndex: number) {
  return dropState.value?.kind === 'row' && dropState.value.rowIndex === rowIndex;
}

function isInsertBefore(rowIndex: number, fieldIndex: number) {
  return dropState.value?.kind === 'item' && dropState.value.rowIndex === rowIndex && dropState.value.insertIndex === fieldIndex;
}

function isInsertAfter(rowIndex: number, fieldIndex: number) {
  return dropState.value?.kind === 'item' && dropState.value.rowIndex === rowIndex && dropState.value.insertIndex === fieldIndex + 1;
}

function moveFieldToNewRow(fieldKey: string, rowIndex: number) {
  const previousMeta = captureRowMeta();
  const rowGroups = rows.value.map((row) => [...row.fields]);
  const result = extractField(rowGroups, fieldKey);
  if (!result) {
    return;
  }

  let targetRowIndex = clampNumber(rowIndex, 0, rowGroups.length);
  if (!rowGroups[result.sourceRowIndex].length) {
    rowGroups.splice(result.sourceRowIndex, 1);
    if (result.sourceRowIndex < targetRowIndex) {
      targetRowIndex -= 1;
    }
  }

  rowGroups.splice(clampNumber(targetRowIndex, 0, rowGroups.length), 0, [result.field]);
  applyRows(rowGroups, previousMeta);
  selectField(fieldKey);
}

function moveFieldToRow(fieldKey: string, rowIndex: number, insertIndex: number) {
  const previousMeta = captureRowMeta();
  const rowGroups = rows.value.map((row) => [...row.fields]);
  const result = extractField(rowGroups, fieldKey);
  if (!result) {
    return;
  }

  let targetRowIndex = clampNumber(rowIndex, 0, Math.max(0, rowGroups.length - 1));
  const sourceRowEmptied = !rowGroups[result.sourceRowIndex].length;
  if (sourceRowEmptied) {
    rowGroups.splice(result.sourceRowIndex, 1);
    if (result.sourceRowIndex < targetRowIndex) {
      targetRowIndex -= 1;
    }
  }

  const targetRow = rowGroups[targetRowIndex] || [];
  let safeInsertIndex = clampNumber(insertIndex, 0, targetRow.length);
  if (!sourceRowEmptied && result.sourceRowIndex === targetRowIndex && result.sourceIndex < safeInsertIndex) {
    safeInsertIndex -= 1;
  }

  targetRow.splice(safeInsertIndex, 0, result.field);
  rowGroups[targetRowIndex] = targetRow;
  applyRows(rowGroups, previousMeta);
  selectField(fieldKey);
}

function stabilizeRowLayouts() {
  const grouped = new Map<number, FormFieldConfig[]>();
  orderedFields.value.forEach((field) => {
    const layout = getLayout(field);
    const rowFields = grouped.get(layout.y) || [];
    rowFields.push(field);
    grouped.set(layout.y, rowFields);
  });

  [...grouped.entries()]
    .sort((left, right) => left[0] - right[0])
    .forEach(([originalRowIndex, group], normalizedRowIndex) => {
      const sortedGroup = [...group].sort((left, right) => {
        const leftLayout = getLayout(left);
        const rightLayout = getLayout(right);
        if (leftLayout.x !== rightLayout.x) {
          return leftLayout.x - rightLayout.x;
        }
        return (left.order ?? left.columnOrder ?? 0) - (right.order ?? right.columnOrder ?? 0);
      });
      const rowHeight = Math.max(1, ...sortedGroup.map((field) => getLayout(field).h));
      const needsRepair = rowNeedsRepair(sortedGroup, originalRowIndex, normalizedRowIndex);
      const repairedWidths = needsRepair ? normalizeRowWidths(sortedGroup.map((field) => getLayout(field).w), sortedGroup.length) : [];
      let cursor = 0;

      sortedGroup.forEach((field, fieldIndex) => {
        const currentLayout = getLayout(field);
        const nextLayout = needsRepair
          ? {
              x: cursor,
              y: normalizedRowIndex,
              w: repairedWidths[fieldIndex],
              h: rowHeight,
            }
          : {
              x: currentLayout.x,
              y: normalizedRowIndex,
              w: currentLayout.w,
              h: rowHeight,
            };
        const nextOrder = normalizedRowIndex * 100 + fieldIndex;

        if (!field.layout || isLayoutDifferent(field.layout, nextLayout)) {
          field.layout = nextLayout;
        }
        if ((field.order ?? nextOrder) !== nextOrder) {
          field.order = nextOrder;
        }
        if ((field.columnOrder ?? nextOrder) !== nextOrder) {
          field.columnOrder = nextOrder;
        }

        cursor += nextLayout.w;
      });
    });
}

function rowNeedsRepair(group: FormFieldConfig[], originalRowIndex: number, normalizedRowIndex: number) {
  if (!group.length) {
    return false;
  }
  if (originalRowIndex !== normalizedRowIndex) {
    return true;
  }

  let cursor = 0;
  for (let index = 0; index < group.length; index += 1) {
    const layout = getLayout(group[index]);
    if (layout.x < 0 || layout.w < 1 || layout.x + layout.w > FORM_GRID_COLUMNS) {
      return true;
    }
    if (index > 0 && layout.x < cursor) {
      return true;
    }
    cursor = Math.max(cursor, layout.x) + layout.w;
  }

  return false;
}

function captureRowMeta() {
  return rows.value.map((row) => ({
    keySet: signatureBySet(row.fields),
    widthByKey: Object.fromEntries(row.fields.map((field) => [field.fieldKey, getLayout(field).w])),
  }));
}

function applyRows(rowGroups: FormFieldConfig[][], previousMeta: Array<{ keySet: string; widthByKey: Record<string, number> }>) {
  const normalizedGroups = rowGroups.filter((row) => row.length);
  normalizedGroups.forEach((group, rowIndex) => {
    const previous = previousMeta.find((item) => item.keySet === signatureBySet(group));
    const widths = normalizeRowWidths(
      previous ? group.map((field) => previous.widthByKey[field.fieldKey] || 1) : buildEqualSpans(group.length),
      group.length
    );
    const rowHeight = Math.max(1, ...group.map((field) => getLayout(field).h));

    let cursor = 0;
    group.forEach((field, fieldIndex) => {
      const width = widths[fieldIndex];
      field.layout = {
        x: cursor,
        y: rowIndex,
        w: width,
        h: rowHeight,
      };
      field.order = rowIndex * 100 + fieldIndex;
      field.columnOrder = field.order;
      cursor += width;
    });
  });
}

function setPointerDropState(next: DropState | null) {
  activePointerDropState = next;
  queueDropState(next);
}

function startResize(field: FormFieldConfig, rowIndex: number, mode: 'width' | 'both', event: PointerEvent) {
  resizeCleanup?.();
  selectField(field.fieldKey);

  const row = rows.value[rowIndex];
  if (!row) {
    return;
  }

  const startLayout = { ...ensureFieldLayout(field) };
  const startX = event.clientX;
  const startY = event.clientY;
  const rowElement = (event.currentTarget as HTMLElement | null)?.closest('.form-layout-designer__row') as HTMLElement | null;
  if (!rowElement) {
    return;
  }

  const rowWidth = rowElement.getBoundingClientRect().width;
  const columnWidth = Math.max(1, rowWidth / FORM_GRID_COLUMNS);
  const startRowFields = [...row.fields];
  const otherFields = startRowFields.filter((item) => item.fieldKey !== field.fieldKey);
  const startRowHeight = Math.max(1, ...startRowFields.map((item) => getLayout(item).h));
  const resizeHandle = event.currentTarget as HTMLElement | null;
  resizeHandle?.setPointerCapture?.(event.pointerId);

  const handleMove = (moveEvent: PointerEvent) => {
    const deltaColumns = Math.round((moveEvent.clientX - startX) / columnWidth);
    const deltaRows = mode === 'both' ? Math.round((moveEvent.clientY - startY) / FORM_HEIGHT_UNIT) : 0;
    const nextHeight = clampNumber(startRowHeight + deltaRows, 1, 6);

    if (!otherFields.length) {
      field.layout = {
        x: 0,
        y: getLayout(field).y,
        w: clampNumber(startLayout.w + deltaColumns, 1, FORM_GRID_COLUMNS),
        h: nextHeight,
      };
      field.order = rowIndex * 100;
      field.columnOrder = field.order;
      return;
    }

    const minWidthForOthers = otherFields.length;
    const nextWidth = clampNumber(startLayout.w + deltaColumns, 1, FORM_GRID_COLUMNS - minWidthForOthers);
    const remaining = FORM_GRID_COLUMNS - nextWidth;
    const siblingWidths = distributeWidth(
      remaining,
      otherFields.map((item) => getLayout(item).w)
    );

    let cursor = 0;
    startRowFields.forEach((item) => {
      if (item.fieldKey === field.fieldKey) {
        item.layout = {
          x: cursor,
          y: rowIndex,
          w: nextWidth,
          h: nextHeight,
        };
        cursor += nextWidth;
        return;
      }

      const width = siblingWidths.shift() || 1;
      item.layout = {
        x: cursor,
        y: rowIndex,
        w: width,
        h: nextHeight,
      };
      cursor += width;
    });

    startRowFields.forEach((item, index) => {
      item.order = rowIndex * 100 + index;
      item.columnOrder = item.order;
    });
  };

  const handleUp = () => {
    window.removeEventListener('pointermove', handleMove);
    window.removeEventListener('pointerup', handleUp);
    window.removeEventListener('pointercancel', handleUp);
    resizeHandle?.releasePointerCapture?.(event.pointerId);
    resizeCleanup = null;
  };

  window.addEventListener('pointermove', handleMove);
  window.addEventListener('pointerup', handleUp);
  window.addEventListener('pointercancel', handleUp);
  resizeCleanup = handleUp;
}

function extractField(rowGroups: FormFieldConfig[][], fieldKey: string) {
  for (let rowIndex = 0; rowIndex < rowGroups.length; rowIndex += 1) {
    const sourceIndex = rowGroups[rowIndex].findIndex((field) => field.fieldKey === fieldKey);
    if (sourceIndex !== -1) {
      const [field] = rowGroups[rowIndex].splice(sourceIndex, 1);
      return {
        field,
        sourceRowIndex: rowIndex,
        sourceIndex,
      };
    }
  }
  return null;
}

function buildEqualSpans(count: number) {
  if (count <= 0) {
    return [];
  }
  if (count >= FORM_GRID_COLUMNS) {
    return Array.from({ length: count }, () => 1);
  }
  const base = Math.floor(FORM_GRID_COLUMNS / count);
  const remainder = FORM_GRID_COLUMNS % count;
  return Array.from({ length: count }, (_, index) => base + (index < remainder ? 1 : 0));
}

function normalizeRowWidths(widths: number[], count: number) {
  if (!count) {
    return [];
  }
  if (count >= FORM_GRID_COLUMNS) {
    return Array.from({ length: count }, () => 1);
  }

  const normalized = [...widths];
  while (normalized.length < count) {
    normalized.push(1);
  }
  normalized.length = count;

  const result = normalized.map((width) => clampNumber(Math.round(width || 1), 1, FORM_GRID_COLUMNS - (count - 1)));
  let total = result.reduce((sum, width) => sum + width, 0);

  if (total < FORM_GRID_COLUMNS) {
    let remainder = FORM_GRID_COLUMNS - total;
    let cursor = 0;
    while (remainder > 0) {
      result[cursor % result.length] += 1;
      remainder -= 1;
      cursor += 1;
    }
    return result;
  }

  while (total > FORM_GRID_COLUMNS) {
    let changed = false;
    for (let index = result.length - 1; index >= 0 && total > FORM_GRID_COLUMNS; index -= 1) {
      if (result[index] > 1) {
        result[index] -= 1;
        total -= 1;
        changed = true;
      }
    }
    if (!changed) {
      break;
    }
  }
  return result;
}

function distributeWidth(total: number, preferred: number[]) {
  if (!preferred.length) {
    return [];
  }
  if (preferred.length >= total) {
    return Array.from({ length: preferred.length }, () => 1);
  }

  const safePreferred = preferred.map((value) => Math.max(1, Math.round(value || 1)));
  const base = Array.from({ length: preferred.length }, () => 1);
  let remaining = total - preferred.length;
  if (remaining <= 0) {
    return base;
  }

  const weightSum = safePreferred.reduce((sum, value) => sum + value, 0);
  const fractions: Array<{ index: number; fraction: number }> = [];
  safePreferred.forEach((value, index) => {
    const raw = (remaining * value) / weightSum;
    const whole = Math.floor(raw);
    base[index] += whole;
    remaining -= whole;
    fractions.push({ index, fraction: raw - whole });
  });

  fractions
    .sort((left, right) => right.fraction - left.fraction)
    .forEach((item) => {
      if (remaining > 0) {
        base[item.index] += 1;
        remaining -= 1;
      }
    });

  return normalizeRowWidths(base, preferred.length);
}

function signatureBySet(fields: FormFieldConfig[]) {
  return fields.map((field) => field.fieldKey).sort().join('|');
}

function queueDropState(next: DropState | null) {
  if (isSameDropState(dropState.value, next) || isSameDropState(queuedDropState, next)) {
    return;
  }
  queuedDropState = next;
  if (dropFrame) {
    return;
  }
  dropFrame = window.requestAnimationFrame(() => {
    dropFrame = 0;
    if (!isSameDropState(dropState.value, queuedDropState)) {
      dropState.value = queuedDropState;
    }
  });
}

function isSameDropState(left: DropState | null, right: DropState | null) {
  if (left === right) {
    return true;
  }
  if (!left || !right || left.kind !== right.kind || left.rowIndex !== right.rowIndex) {
    return false;
  }
  if (left.kind === 'item' && right.kind === 'item') {
    return left.insertIndex === right.insertIndex;
  }
  return true;
}

function isLayoutDifferent(left: FormFieldLayout, right: FormFieldLayout) {
  return left.x !== right.x || left.y !== right.y || left.w !== right.w || left.h !== right.h;
}

function toPositiveNumber(value: number | string | null | undefined) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric <= 0) {
    return null;
  }
  return numeric;
}

function clampNumber(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, Number.isFinite(value) ? value : min));
}
</script>

<style scoped>
.form-layout-designer {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 16px;
}

.form-layout-designer--dragging {
  user-select: none;
}

.form-layout-designer__drag-ghost {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 9999;
  pointer-events: none;
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid rgba(17, 17, 17, 0.18);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  color: var(--mono-text);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.form-layout-designer__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  border: 1px solid var(--mono-line);
  border-radius: var(--mono-radius-sm);
  background: #ffffff;
}

.form-layout-designer__toolbar-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.form-layout-designer__toolbar-copy strong {
  color: var(--mono-text);
  font-size: 15px;
  font-weight: 700;
}

.form-layout-designer__toolbar-copy span {
  color: var(--mono-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.form-layout-designer__toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.designer-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid var(--mono-line);
  border-radius: 8px;
  background: #ffffff;
  color: var(--mono-text);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.designer-chip--ghost {
  background: rgba(255, 255, 255, 0.96);
}

.designer-chip--drag {
  cursor: grab;
  user-select: none;
  touch-action: none;
}

.designer-chip--drag:active,
.designer-chip--drag.is-dragging {
  cursor: grabbing;
}

.designer-chip--danger {
  border-color: #111111;
}

.form-layout-designer__shell {
  border: 1px solid var(--mono-line);
  border-radius: var(--mono-radius-sm);
  background: #ffffff;
}

.form-layout-designer__form {
  padding: 20px;
}

.form-layout-designer__canvas {
  display: flex;
  flex-direction: column;
}

.form-layout-designer__row-gap {
  position: relative;
  height: 18px;
  margin: 2px 0 6px;
  border-radius: 8px;
  border: 1px dashed transparent;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.form-layout-designer__row-gap span {
  position: absolute;
  inset: 50% auto auto 12px;
  transform: translateY(-50%);
  color: transparent;
  font-size: 11px;
  font-weight: 600;
  transition: color 0.2s ease;
}

.form-layout-designer__row-gap--active {
  border-color: var(--mono-line-strong);
  background: rgba(17, 17, 17, 0.02);
}

.form-layout-designer__row-gap--active span {
  color: var(--mono-text-secondary);
}

.form-layout-designer__row {
  display: grid;
  grid-template-columns: repeat(24, minmax(0, 1fr));
  gap: 0 16px;
  padding: 6px 0;
  border-radius: 12px;
  transition: background-color 0.2s ease;
}

.form-layout-designer__row--active {
  background: rgba(17, 17, 17, 0.02);
}

.form-layout-designer__item {
  position: relative;
  min-width: 0;
  padding: 0;
  background: transparent;
  cursor: grab;
}

.form-layout-designer__item.is-dragging {
  cursor: grabbing;
}

.form-layout-designer__item::after {
  content: '';
  position: absolute;
  inset: -6px;
  border: 1px dashed transparent;
  border-radius: 12px;
  background: transparent;
  pointer-events: none;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.form-layout-designer__item::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 10px;
  width: 2px;
  border-radius: 999px;
  background: transparent;
  pointer-events: none;
  transition: background-color 0.2s ease;
}

.form-layout-designer__item:hover::after,
.form-layout-designer__item--selected::after {
  border-color: var(--mono-line-strong);
  background: rgba(17, 17, 17, 0.014);
}

.form-layout-designer__item--insert-before::before {
  left: -9px;
  background: #111111;
}

.form-layout-designer__item--insert-after::before {
  right: -9px;
  background: #111111;
}

.form-layout-designer__item--hidden {
  opacity: 0.45;
}

.form-layout-designer__item-tools {
  position: absolute;
  top: -14px;
  right: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 6px;
  opacity: 0;
  transform: translateY(-2px);
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.form-layout-designer__item:hover .form-layout-designer__item-tools,
.form-layout-designer__item--selected .form-layout-designer__item-tools,
.form-layout-designer--dragging .form-layout-designer__item-tools {
  opacity: 1;
  transform: translateY(0);
}

.form-layout-designer__preview {
  position: relative;
  z-index: 1;
  min-width: 0;
  pointer-events: none;
}

.form-layout-designer__preview :deep(.ant-form-item-label > label) {
  white-space: normal;
}

.form-layout-designer__preview :deep(textarea.ant-input) {
  resize: none;
}

.form-layout-designer__range {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
}

.form-layout-designer__range span {
  color: var(--mono-text-secondary);
  font-size: 12px;
}

.form-layout-designer__resize {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 18px;
  height: 18px;
  border: none;
  background: linear-gradient(
    135deg,
    rgba(17, 17, 17, 0) 0%,
    rgba(17, 17, 17, 0) 46%,
    rgba(17, 17, 17, 0.34) 46%,
    rgba(17, 17, 17, 0.34) 58%,
    rgba(17, 17, 17, 0) 58%,
    rgba(17, 17, 17, 0) 100%
  );
  cursor: nwse-resize;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.form-layout-designer__resize-edge {
  position: absolute;
  top: 22px;
  right: -8px;
  bottom: 22px;
  width: 16px;
  border: none;
  border-radius: 999px;
  background: transparent;
  cursor: ew-resize;
  opacity: 0.28;
  transition: opacity 0.2s ease;
}

.form-layout-designer__resize-edge::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 3px;
  height: 56px;
  border-radius: 999px;
  background: rgba(17, 17, 17, 0.28);
  transform: translate(-50%, -50%);
}

.form-layout-designer__item:hover .form-layout-designer__resize-edge,
.form-layout-designer__item--selected .form-layout-designer__resize-edge,
.form-layout-designer__item:hover .form-layout-designer__resize,
.form-layout-designer__item--selected .form-layout-designer__resize {
  opacity: 1;
}

.form-layout-designer__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  color: var(--mono-text-secondary);
  font-size: 14px;
}

@media (max-width: 1280px) {
  .form-layout-designer__toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .form-layout-designer__toolbar-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 960px) {
  .form-layout-designer__form {
    padding: 16px;
  }

  .form-layout-designer__row {
    gap: 0 12px;
  }
}
</style>
