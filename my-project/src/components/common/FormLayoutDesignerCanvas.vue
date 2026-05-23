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

    <section
      class="form-layout-designer__shell"
      :class="{ 'form-layout-designer__shell--modal': isModalPreview }"
      :style="modalShellStyle"
    >
      <div v-if="isModalPreview" class="form-layout-designer__modal-header">
        <h3>{{ resolvedPreviewTitle }}</h3>
        <button type="button" class="form-layout-designer__modal-close" aria-label="关闭预览">
          <CloseOutlined />
        </button>
      </div>

      <a-form
        :layout="resolvedLayoutMode"
        :label-col="resolvedLabelCol"
        :wrapper-col="resolvedWrapperCol"
        :class="['workspace-modal-form', 'form-layout-designer__form', normalizedFormClass]"
        :style="modalBodyStyle"
      >
        <div class="form-layout-designer__canvas">
          <ConfiguredFormLayout
            v-if="orderedFields.length"
            :fields="orderedFields"
            include-hidden
            class="form-layout-designer__configured-layout"
          >
            <template
              v-for="field in orderedFields"
              :key="field.fieldKey"
              #[fieldSlotName(field.fieldKey)]
            >
              <article
                class="form-layout-designer__item"
                :class="{
                  'form-layout-designer__item--selected': field.fieldKey === selectedFieldKey,
                  'form-layout-designer__item--hidden': !isFieldVisible(field),
                  'form-layout-designer__item--insert-before': isInsertBefore(fieldRowArrayIndex(field), fieldIndexInRow(field)),
                  'form-layout-designer__item--insert-after': isInsertAfter(fieldRowArrayIndex(field), fieldIndexInRow(field)),
                  'form-layout-designer__item--new-row-before': isNewRowBefore(fieldRowArrayIndex(field)),
                  'form-layout-designer__item--new-row-after': isNewRowAfter(fieldRowArrayIndex(field)),
                }"
                data-drop-kind="item"
                :data-field-key="field.fieldKey || ''"
                :data-row-index="fieldRowArrayIndex(field)"
                :data-field-index="fieldIndexInRow(field)"
                @click.stop="selectField(field.fieldKey)"
                @dragover.prevent.stop="handleItemDragOver(fieldRowArrayIndex(field), fieldIndexInRow(field), $event)"
                @drop.prevent.stop="handleItemDrop(fieldRowArrayIndex(field), fieldIndexInRow(field), $event)"
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
                  @pointerdown.stop.prevent="startResize(field, fieldRowArrayIndex(field), 'width', $event)"
                />

                <button
                  type="button"
                  class="form-layout-designer__resize"
                  data-no-drag="true"
                  @pointerdown.stop.prevent="startResize(field, fieldRowArrayIndex(field), 'both', $event)"
                />
              </article>
            </template>
          </ConfiguredFormLayout>

          <div v-else class="form-layout-designer__empty">
            当前目标还没有字段。新增字段后就会在这里按真实表单样式预览。
          </div>
        </div>
      </a-form>

      <div v-if="isModalPreview" class="form-layout-designer__modal-footer">
        <StandardButton type="default">取消</StandardButton>
        <StandardButton type="primary">确定</StandardButton>
      </div>
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
import { CloseOutlined } from '@ant-design/icons-vue';
import FormFieldPreviewRenderer from '@/components/common/FormFieldPreviewRenderer.vue';
import ConfiguredFormLayout from '@/components/common/ConfiguredFormLayout.vue';
import StandardButton from '@/components/common/StandardButton.vue';
import type { FormFieldConfig, FormFieldLayout } from '@/types/formConfig';
import { normalizeConfiguredFormFields } from '@/utils/formLayoutRuntime';

const FORM_GRID_COLUMNS = 24;
const FORM_GRID_PRECISION = 10;
const FORM_MIN_FIELD_WIDTH = 1;
const FORM_HEIGHT_UNIT = 82;
const DRAG_START_THRESHOLD = 6;
const NEW_ROW_DROP_ZONE_RATIO = 0.24;
const MODAL_HORIZONTAL_MIN_FIELD_WIDTH = 10;
const MODAL_HORIZONTAL_MAX_FIELDS_PER_ROW = 2;

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
  targetType?: string;
  modalWidth?: number | string | null;
  modalBodyStyle?: string;
  layoutMode?: string;
  formClass?: string;
  labelColSpan?: number | string | null;
  wrapperColSpan?: number | string | null;
}>(), {
  title: '',
  targetType: '',
  modalWidth: null,
  modalBodyStyle: '',
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

const isModalPreview = computed(() => props.targetType === 'modal-form');

const modalShellStyle = computed<CSSProperties | undefined>(() => {
  if (!isModalPreview.value) {
    return undefined;
  }
  const width = normalizeModalWidth(props.modalWidth);
  return width ? { width: `min(100%, ${width})` } : undefined;
});

const modalBodyStyle = computed<CSSProperties | undefined>(() => {
  if (!isModalPreview.value) {
    return undefined;
  }
  return {
    padding: '20px 28px 0',
    ...toStyleObject(props.modalBodyStyle || ''),
  };
});

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

const resolvedPreviewTitle = computed(() => {
  const title = String(props.title || '').trim();
  if (normalizedFormClass.value.includes('menu-config-form') && (!title || title === 'modalTitle')) {
    return '新增顶级菜单';
  }
  if (!title || /(?:^|[A-Z])modalTitle$/i.test(title) || title === 'modalTitle') {
    return '表单';
  }
  return title;
});

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

function getLayout(field: FormFieldConfig): FormFieldLayout {
  const source = field.layout;
  const minWidth = getMinFieldWidth();
  const layout = {
    x: clampNumber(roundLayoutValue(source?.x ?? 0), 0, FORM_GRID_COLUMNS - minWidth),
    y: Math.max(0, Math.round(source?.y ?? field.order ?? field.columnOrder ?? 0)),
    w: clampNumber(roundLayoutValue(source?.w ?? 24), minWidth, FORM_GRID_COLUMNS),
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

function layoutText(field: FormFieldConfig) {
  const layout = getLayout(field);
  return `第 ${layout.y + 1} 行 · 宽 ${formatWidth(layout.w)}/24 · 高 ${layout.h}`;
}

function fieldSlotName(fieldKey: string) {
  return `field-${fieldKey}`;
}

function fieldRowArrayIndex(field: FormFieldConfig) {
  const rowIndex = rows.value.findIndex((row) => row.fields.some((item) => item.fieldKey === field.fieldKey));
  return rowIndex === -1 ? 0 : rowIndex;
}

function fieldIndexInRow(field: FormFieldConfig) {
  const row = rows.value[fieldRowArrayIndex(field)];
  if (!row) {
    return 0;
  }
  const fieldIndex = row.fields.findIndex((item) => item.fieldKey === field.fieldKey);
  return fieldIndex === -1 ? 0 : fieldIndex;
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

  const dragHandle = event.currentTarget as HTMLElement | null;
  dragHandle?.setPointerCapture?.(event.pointerId);
  const startX = event.clientX;
  const startY = event.clientY;
  let hasStartedDrag = false;

  const beginDrag = (clientX: number, clientY: number) => {
    if (hasStartedDrag) {
      return;
    }
    hasStartedDrag = true;
    isPointerDragging.value = true;
    dragHandle?.classList.add('is-dragging');
    document.body.classList.add('form-layout-designer-pointer-dragging');
    dragGhost.value = {
      label: fieldDisplayLabel(resolvedFieldKey),
      x: clientX + 14,
      y: clientY + 14,
    };
    updateDropStateFromPointer(clientX, clientY);
  };

  const handleMove = (moveEvent: PointerEvent) => {
    if (!hasStartedDrag) {
      const distance = Math.hypot(moveEvent.clientX - startX, moveEvent.clientY - startY);
      if (distance < DRAG_START_THRESHOLD) {
        return;
      }
      beginDrag(moveEvent.clientX, moveEvent.clientY);
    }
    dragGhost.value = {
      label: fieldDisplayLabel(resolvedFieldKey),
      x: moveEvent.clientX + 14,
      y: moveEvent.clientY + 14,
    };
    updateDropStateFromPointer(moveEvent.clientX, moveEvent.clientY);
    autoScrollWhileDragging(moveEvent.clientY);
  };

  const handleUp = (upEvent?: PointerEvent) => {
    if (hasStartedDrag && upEvent) {
      updateDropStateFromPointer(upEvent.clientX, upEvent.clientY);
    }
    dragHandle?.releasePointerCapture?.(event.pointerId);
    if (hasStartedDrag) {
      commitPointerDrop();
    }
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
    setPointerDropState(resolveItemDropState(rowIndex, fieldIndex, item, clientX, clientY));
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
  setPointerDropState(resolveItemDropState(rowIndex, fieldIndex, target, event.clientX, event.clientY));
}

function handleItemDrop(rowIndex: number, fieldIndex: number, event: DragEvent) {
  if (!dragFieldKey.value) {
    return;
  }
  const target = event.currentTarget as HTMLElement | null;
  if (!target) {
    return;
  }
  const nextDropState = resolveItemDropState(rowIndex, fieldIndex, target, event.clientX, event.clientY);
  if (nextDropState.kind === 'gap') {
    moveFieldToNewRow(dragFieldKey.value, nextDropState.rowIndex);
  } else {
    moveFieldToRow(dragFieldKey.value, nextDropState.rowIndex, nextDropState.insertIndex);
  }
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

function resolveItemDropState(
  rowIndex: number,
  fieldIndex: number,
  itemElement: HTMLElement,
  clientX: number,
  clientY: number
): DropState {
  const rowRect = getRowClientRect(rowIndex) || itemElement.getBoundingClientRect();
  const verticalRatio = rowRect.height > 0 ? (clientY - rowRect.top) / rowRect.height : 0.5;
  if (verticalRatio <= NEW_ROW_DROP_ZONE_RATIO) {
    return { kind: 'gap', rowIndex };
  }
  if (verticalRatio >= 1 - NEW_ROW_DROP_ZONE_RATIO) {
    return { kind: 'gap', rowIndex: rowIndex + 1 };
  }

  return {
    kind: 'item',
    rowIndex,
    insertIndex: resolveInsertIndex(rowIndex, fieldIndex, itemElement, clientX),
  };
}

function resolveInsertIndex(rowIndex: number, fieldIndex: number, itemElement: HTMLElement, clientX: number) {
  const rowItems = getRowItemElements(rowIndex);
  if (!rowItems.length) {
    const rect = itemElement.getBoundingClientRect();
    return clientX < rect.left + rect.width / 2 ? fieldIndex : fieldIndex + 1;
  }

  const orderedItems = rowItems.sort((left, right) => {
    const leftIndex = Number(left.dataset.fieldIndex || 0);
    const rightIndex = Number(right.dataset.fieldIndex || 0);
    return leftIndex - rightIndex;
  });

  for (let index = 0; index < orderedItems.length; index += 1) {
    const rect = orderedItems[index].getBoundingClientRect();
    if (clientX < rect.left + rect.width / 2) {
      return index;
    }
  }
  return orderedItems.length;
}

function getRowClientRect(rowIndex: number) {
  const rowItems = getRowItemElements(rowIndex);
  if (!rowItems.length) {
    return null;
  }
  const rects = rowItems.map((item) => item.getBoundingClientRect());
  const left = Math.min(...rects.map((rect) => rect.left));
  const right = Math.max(...rects.map((rect) => rect.right));
  const top = Math.min(...rects.map((rect) => rect.top));
  const bottom = Math.max(...rects.map((rect) => rect.bottom));
  return {
    left,
    right,
    top,
    bottom,
    width: right - left,
    height: bottom - top,
  };
}

function getRowItemElements(rowIndex: number) {
  return Array.from(
    document.querySelectorAll<HTMLElement>(`.form-layout-designer__item[data-row-index="${rowIndex}"]`)
  );
}

function isNewRowBefore(rowIndex: number) {
  return dropState.value?.kind === 'gap' && dropState.value.rowIndex === rowIndex;
}

function isNewRowAfter(rowIndex: number) {
  return dropState.value?.kind === 'gap' && dropState.value.rowIndex === rowIndex + 1;
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

  const maxFields = getMaxFieldsPerRow();
  if (!(result.sourceRowIndex === targetRowIndex) && targetRow.length >= maxFields) {
    rowGroups.splice(targetRowIndex + 1, 0, [result.field]);
    applyRows(rowGroups, previousMeta);
    selectField(fieldKey);
    return;
  }

  targetRow.splice(safeInsertIndex, 0, result.field);
  rowGroups[targetRowIndex] = targetRow;
  applyRows(rowGroups, previousMeta);
  selectField(fieldKey);
}

function stabilizeRowLayouts() {
  const normalizedByKey = new Map(
    normalizeConfiguredFormFields(orderedFields.value, { includeHidden: true }).map((field, index) => [
      field.fieldKey,
      {
        layout: getLayout({ ...field, order: index, columnOrder: index }),
        order: field.order ?? index,
        columnOrder: field.columnOrder ?? field.order ?? index,
      },
    ]),
  );

  orderedFields.value.forEach((field) => {
    const normalized = normalizedByKey.get(field.fieldKey);
    if (!normalized) {
      return;
    }
    if (!field.layout || isLayoutDifferent(field.layout, normalized.layout)) {
      field.layout = normalized.layout;
    }
    if ((field.order ?? normalized.order) !== normalized.order) {
      field.order = normalized.order;
    }
    if ((field.columnOrder ?? normalized.columnOrder) !== normalized.columnOrder) {
      field.columnOrder = normalized.columnOrder;
    }
  });
}

function shouldResetBrokenModalRow(group: FormFieldConfig[]) {
  if (!isModalPreview.value || resolvedLayoutMode.value !== 'horizontal') {
    return false;
  }
  return group.length > getMaxFieldsPerRow();
}

function rowNeedsRepair(group: FormFieldConfig[], originalRowIndex: number, normalizedRowIndex: number) {
  if (!group.length) {
    return false;
  }
  if (originalRowIndex !== normalizedRowIndex) {
    return true;
  }

  let cursor = 0;
  const minWidth = getMinFieldWidth(group.length);
  for (let index = 0; index < group.length; index += 1) {
    const layout = getLayout(group[index]);
    if (layout.x < 0 || layout.w < minWidth || layout.x + layout.w > FORM_GRID_COLUMNS + 0.0001) {
      return true;
    }
    if (index > 0 && layout.x < cursor - 0.0001) {
      return true;
    }
    cursor = roundLayoutValue(Math.max(cursor, layout.x) + layout.w);
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
    const rowMinWidth = getMinFieldWidth(group.length);

    let cursor = 0;
    group.forEach((field, fieldIndex) => {
      const width = Math.max(rowMinWidth, widths[fieldIndex]);
      field.layout = {
        x: roundLayoutValue(cursor),
        y: rowIndex,
        w: width,
        h: rowHeight,
      };
      field.order = rowIndex * 100 + fieldIndex;
      field.columnOrder = field.order;
      cursor = roundLayoutValue(cursor + width);
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
  const layoutElement = (event.currentTarget as HTMLElement | null)?.closest('.form-layout-designer__configured-layout') as HTMLElement | null;
  if (!layoutElement) {
    return;
  }

  const rowWidth = layoutElement.getBoundingClientRect().width;
  const columnWidth = Math.max(1, rowWidth / FORM_GRID_COLUMNS);
  const startRowFields = [...row.fields];
  const otherFields = startRowFields.filter((item) => item.fieldKey !== field.fieldKey);
  const startRowHeight = Math.max(1, ...startRowFields.map((item) => getLayout(item).h));
  const rowMinWidth = getMinFieldWidth(startRowFields.length);
  const resizeHandle = event.currentTarget as HTMLElement | null;
  resizeHandle?.setPointerCapture?.(event.pointerId);
  document.body.classList.add('form-layout-designer-pointer-resizing');

  const handleMove = (moveEvent: PointerEvent) => {
    const deltaColumns = roundLayoutValue((moveEvent.clientX - startX) / columnWidth);
    const deltaRows = mode === 'both' ? Math.round((moveEvent.clientY - startY) / FORM_HEIGHT_UNIT) : 0;
    const nextHeight = clampNumber(startRowHeight + deltaRows, 1, 6);

    if (!otherFields.length) {
      field.layout = {
        x: 0,
        y: getLayout(field).y,
        w: clampNumber(roundLayoutValue(startLayout.w + deltaColumns), rowMinWidth, FORM_GRID_COLUMNS),
        h: nextHeight,
      };
      field.order = rowIndex * 100;
      field.columnOrder = field.order;
      return;
    }

    const minWidthForOthers = otherFields.length * rowMinWidth;
    const nextWidth = clampNumber(
      roundLayoutValue(startLayout.w + deltaColumns),
      rowMinWidth,
      FORM_GRID_COLUMNS - minWidthForOthers
    );
    const remaining = FORM_GRID_COLUMNS - nextWidth;
    const siblingWidths = distributeWidth(
      remaining,
      otherFields.map((item) => getLayout(item).w)
    );

    let cursor = 0;
    startRowFields.forEach((item) => {
      if (item.fieldKey === field.fieldKey) {
        item.layout = {
          x: roundLayoutValue(cursor),
          y: rowIndex,
          w: nextWidth,
          h: nextHeight,
        };
        cursor = roundLayoutValue(cursor + nextWidth);
        return;
      }

      const width = siblingWidths.shift() || rowMinWidth;
      item.layout = {
        x: roundLayoutValue(cursor),
        y: rowIndex,
        w: width,
        h: nextHeight,
      };
      cursor = roundLayoutValue(cursor + width);
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
    document.body.classList.remove('form-layout-designer-pointer-resizing');
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
  return fitRoundedWidths(Array.from({ length: count }, () => FORM_GRID_COLUMNS / count), FORM_GRID_COLUMNS);
}

function normalizeRowWidths(widths: number[], count: number) {
  if (!count) {
    return [];
  }

  const normalized = [...widths];
  const minWidth = getMinFieldWidth(count);
  while (normalized.length < count) {
    normalized.push(minWidth);
  }
  normalized.length = count;

  return distributeWidth(FORM_GRID_COLUMNS, normalized);
}

function distributeWidth(total: number, preferred: number[]) {
  if (!preferred.length) {
    return [];
  }
  const safeTotal = Math.max(0, roundLayoutValue(total));
  if (!safeTotal) {
    return Array.from({ length: preferred.length }, () => 0);
  }
  const minWidth = Math.min(getMinFieldWidth(preferred.length), safeTotal / preferred.length);
  const minTotal = minWidth * preferred.length;
  if (minTotal >= safeTotal) {
    return fitRoundedWidths(Array.from({ length: preferred.length }, () => safeTotal / preferred.length), safeTotal);
  }

  const safePreferred = preferred.map((value) => Math.max(minWidth, Number.isFinite(value) ? value : minWidth));
  const flexible = safePreferred.map((value) => Math.max(0, value - minWidth));
  const flexibleSum = flexible.reduce((sum, value) => sum + value, 0);
  const distributable = safeTotal - minTotal;
  const nextWidths = safePreferred.map((_, index) => {
    if (flexibleSum <= 0) {
      return minWidth + distributable / safePreferred.length;
    }
    return minWidth + (distributable * flexible[index]) / flexibleSum;
  });

  return fitRoundedWidths(nextWidths, safeTotal);
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

function normalizeModalWidth(value: number | string | null | undefined) {
  if (typeof value === 'number' && Number.isFinite(value) && value > 0) {
    return `${value}px`;
  }
  const raw = String(value || '').trim().replace(/^['"]|['"]$/g, '');
  if (!raw) {
    return '';
  }
  if (/^\d+(\.\d+)?$/.test(raw)) {
    return `${raw}px`;
  }
  if (/^\d+(\.\d+)?(px|%)$/.test(raw)) {
    return raw;
  }
  return '';
}

function toStyleObject(styleText: string): CSSProperties {
  const styleObject: CSSProperties = {};
  String(styleText || '')
    .split(';')
    .map((item) => item.trim())
    .filter(Boolean)
    .forEach((declaration) => {
      const [rawKey, ...valueParts] = declaration.split(':');
      if (!rawKey || !valueParts.length) {
        return;
      }
      const key = rawKey
        .trim()
        .replace(/-([a-z])/g, (_, letter: string) => letter.toUpperCase()) as keyof CSSProperties;
      styleObject[key] = valueParts.join(':').trim() as never;
    });
  return styleObject;
}

function getMaxFieldsPerRow() {
  return isModalPreview.value && resolvedLayoutMode.value === 'horizontal'
    ? MODAL_HORIZONTAL_MAX_FIELDS_PER_ROW
    : Number.MAX_SAFE_INTEGER;
}

function getMinFieldWidth(rowFieldCount = 1) {
  if (isModalPreview.value && resolvedLayoutMode.value === 'horizontal') {
    return Math.min(MODAL_HORIZONTAL_MIN_FIELD_WIDTH, FORM_GRID_COLUMNS / Math.max(1, rowFieldCount));
  }
  return FORM_MIN_FIELD_WIDTH;
}

function formatWidth(value: number) {
  return Number.isInteger(value) ? String(value) : value.toFixed(1).replace(/\.0$/, '');
}

function roundLayoutValue(value: number) {
  return Math.round((Number.isFinite(value) ? value : 0) * FORM_GRID_PRECISION) / FORM_GRID_PRECISION;
}

function fitRoundedWidths(widths: number[], total = FORM_GRID_COLUMNS) {
  const rounded = widths.map((width) => roundLayoutValue(Math.max(0, width)));
  if (!rounded.length) {
    return [];
  }
  const diff = roundLayoutValue(total - rounded.reduce((sum, width) => sum + width, 0));
  rounded[rounded.length - 1] = roundLayoutValue(Math.max(0, rounded[rounded.length - 1] + diff));
  return rounded;
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

:global(body.form-layout-designer-pointer-resizing),
:global(body.form-layout-designer-pointer-resizing *) {
  cursor: ew-resize !important;
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

.form-layout-designer__shell--modal {
  width: min(100%, 600px);
  margin: 16px auto 0;
  overflow: hidden;
  border-color: rgba(15, 23, 42, 0.08);
}

.form-layout-designer__modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 28px 28px 0;
}

.form-layout-designer__modal-header h3 {
  margin: 0;
  color: #111111;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.3;
  letter-spacing: 0;
}

.form-layout-designer__modal-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: var(--mono-radius-sm);
  background: transparent;
  color: rgba(17, 17, 17, 0.42);
  font-size: 16px;
  line-height: 1;
}

.form-layout-designer__modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 24px 28px 28px;
}

.form-layout-designer__modal-footer :deep(.std-button) {
  min-width: 108px;
  height: 42px;
  padding: 0 20px;
  border-radius: var(--mono-radius-sm);
  font-weight: 600;
}

.form-layout-designer__form {
  padding: 20px;
}

.form-layout-designer__shell--modal .form-layout-designer__form {
  padding: 30px 28px 18px;
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
  cursor: pointer;
}

.form-layout-designer--dragging .form-layout-designer__item {
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

.form-layout-designer__item--new-row-before {
  box-shadow: 0 -3px 0 #111111;
}

.form-layout-designer__item--new-row-after {
  box-shadow: 0 3px 0 #111111;
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
  opacity: 0;
  transition: opacity 0.2s ease;
  z-index: 3;
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

  .form-layout-designer__shell--modal .form-layout-designer__form {
    padding: 24px 20px 12px;
  }

  .form-layout-designer__modal-header {
    padding: 24px 20px 0;
  }

  .form-layout-designer__modal-footer {
    padding: 20px 20px 22px;
  }

  .form-layout-designer__row {
    gap: 0 12px;
  }
}
</style>

<style>
html.dark .form-layout-designer__drag-ghost,
html.dark .form-layout-designer__toolbar,
html.dark .form-layout-designer .designer-chip,
html.dark .form-layout-designer .designer-chip--ghost,
html.dark .form-layout-designer__shell {
  background: rgba(255, 255, 255, 0.045) !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
  color: var(--mono-text) !important;
  box-shadow: none !important;
}

html.dark .form-layout-designer__shell--modal {
  background: #1f1f1f !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
}

html.dark .form-layout-designer__modal-header h3,
html.dark .form-layout-designer__toolbar-copy strong {
  color: var(--mono-text) !important;
}

html.dark .form-layout-designer__modal-close,
html.dark .form-layout-designer__toolbar-copy span,
html.dark .form-layout-designer__range span {
  color: var(--mono-text-secondary) !important;
}

html.dark .form-layout-designer__row-gap--active,
html.dark .form-layout-designer__row--active,
html.dark .form-layout-designer__item:hover::after,
html.dark .form-layout-designer__item--selected::after {
  background: rgba(255, 255, 255, 0.045) !important;
  border-color: rgba(255, 255, 255, 0.14) !important;
}

html.dark .form-layout-designer__item--insert-before::before,
html.dark .form-layout-designer__item--insert-after::before {
  background-color: rgba(255, 255, 255, 0.78) !important;
}

html.dark .form-layout-designer__item--new-row-before {
  box-shadow: 0 -3px 0 rgba(255, 255, 255, 0.78) !important;
}

html.dark .form-layout-designer__item--new-row-after {
  box-shadow: 0 3px 0 rgba(255, 255, 255, 0.78) !important;
}

html.dark .form-layout-designer__resize {
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0) 0%,
    rgba(255, 255, 255, 0) 46%,
    rgba(255, 255, 255, 0.44) 46%,
    rgba(255, 255, 255, 0.44) 58%,
    rgba(255, 255, 255, 0) 58%,
    rgba(255, 255, 255, 0) 100%
  ) !important;
}

html.dark .form-layout-designer__resize-edge::before {
  background: rgba(255, 255, 255, 0.34) !important;
}
</style>
