<template>
  <div :class="containerClass">
    <a-table v-bind="tableAttrs">
      <template #bodyCell="scope">
        <slot name="bodyCell" v-bind="scope" />
      </template>
      <template v-if="$slots.headerCell" #headerCell="scope">
        <slot name="headerCell" v-bind="scope" />
      </template>
      <template v-if="$slots.summary" #summary>
        <slot name="summary" />
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, useAttrs } from 'vue';
import { ChevronLeft, ChevronRight, MoreHorizontal } from 'lucide-vue-next';
import { buttonVariants } from '@/components/ui/button';
import { cn } from '@/lib/utils';

const props = defineProps<{
  configStyle?: 'default' | 'glass';
  surface?: 'default' | 'menu-list';
}>();

const attrs = useAttrs();

const createTablePrimitive = (tag: string, classes: string) => defineComponent({
  name: `Shadcn${tag.charAt(0).toUpperCase()}${tag.slice(1)}`,
  inheritAttrs: false,
  setup(_, { attrs: primitiveAttrs, slots }) {
    return () => h(
      tag as any,
      {
        ...primitiveAttrs,
        class: cn(classes, primitiveAttrs.class as any),
      },
      slots.default?.(),
    );
  },
});

const ShadTableRoot = createTablePrimitive('table', 'w-full caption-bottom text-sm');
const ShadTableHeader = createTablePrimitive('thead', '[&_tr]:border-b');
const ShadTableBody = createTablePrimitive('tbody', '[&_tr:last-child]:border-0');
const ShadTableRow = createTablePrimitive('tr', 'border-b transition-colors hover:bg-muted/40 data-[state=selected]:bg-muted');
const ShadTableHeadCell = createTablePrimitive('th', 'text-foreground h-10 px-2 align-middle font-medium whitespace-nowrap');
const ShadTableBodyCell = createTablePrimitive('td', 'p-2 align-middle whitespace-nowrap');

const tableComponents = computed(() => {
  const external = (attrs.components as Record<string, any>) || {};
  const externalHeader = external.header || {};
  const externalBody = external.body || {};

  return {
    ...external,
    table: external.table || ShadTableRoot,
    header: {
      ...externalHeader,
      wrapper: externalHeader.wrapper || ShadTableHeader,
      row: externalHeader.row || ShadTableRow,
      cell: externalHeader.cell || ShadTableHeadCell,
    },
    body: {
      ...externalBody,
      wrapper: externalBody.wrapper || ShadTableBody,
      row: externalBody.row || ShadTableRow,
      cell: externalBody.cell || ShadTableBodyCell,
    },
  };
});

const normalizedPagination = computed(() => {
  const rawPagination = attrs.pagination;

  if (rawPagination === false || rawPagination === undefined || props.surface !== 'menu-list') {
    return rawPagination;
  }

  const base = typeof rawPagination === 'object' && rawPagination !== null
    ? rawPagination as Record<string, any>
    : {};

  const currentPage = Number(base.current ?? 1);

  return {
    ...base,
    showLessItems: base.showLessItems ?? true,
    showTitle: base.showTitle ?? false,
    itemRender: (page: number, type: string, originalElement: unknown) => {
      if (typeof base.itemRender === 'function') {
        return base.itemRender(page, type, originalElement);
      }

      if (type === 'jump-prev' || type === 'jump-next') {
        return h('span', { class: 'std-table-pagination-ellipsis' }, [
          h(MoreHorizontal, { class: 'std-table-pagination-icon', 'aria-hidden': 'true' }),
        ]);
      }

      if (type === 'prev' || type === 'next') {
        const Icon = type === 'prev' ? ChevronLeft : ChevronRight;

        return h(
          'span',
          {
            class: cn(
              buttonVariants({ variant: 'ghost', size: 'icon-sm' }),
              'std-table-pagination-node std-table-pagination-control',
            ),
          },
          [h(Icon, { class: 'std-table-pagination-icon', 'aria-hidden': 'true' })],
        );
      }

      if (type === 'page') {
        return h(
          'span',
          {
            class: cn(
              buttonVariants({ variant: 'ghost', size: 'icon-sm' }),
              'std-table-pagination-node std-table-pagination-page',
              page === currentPage && 'std-table-pagination-page--active',
            ),
          },
          String(page),
        );
      }

      return originalElement;
    },
  };
});

const tableAttrs = computed(() => ({
  ...attrs,
  components: tableComponents.value,
  pagination: normalizedPagination.value,
}));

const containerClass = computed(() => {
  return cn(
    props.configStyle === 'glass'
      ? 'transparent-glass-mode'
      : props.configStyle === 'default'
        ? 'default-yellow-mode'
        : 'mono-table-shell',
    props.surface === 'menu-list' && 'table-surface--menu-list',
  );
});
</script>

<style scoped>
.default-yellow-mode :deep(.ant-table-header) {
  background-color: #f8f6ef !important;
}

.default-yellow-mode :deep(.ant-table-thead > tr > th) {
  background-color: #f8f6ef !important;
  color: #867e7e !important;
  font-size: 13.6px !important;
  text-align: center !important;
  border-radius: 0;
  border-bottom: 0.05px solid #f4efee !important;
  border-top: none !important;
  border-left: none !important;
  border-right: none !important;
}

.default-yellow-mode :deep(.ant-table-thead > tr > th::before) {
  display: none !important;
  width: 0 !important;
  background-color: transparent !important;
}

.default-yellow-mode :deep(.ant-table-tbody > tr > td) {
  text-align: center !important;
  border-bottom: 0.05px solid #f4efee !important;
  border-top: none !important;
  border-left: none !important;
  border-right: none !important;
}

.default-yellow-mode :deep(.ant-table-tbody) {
  background-color: #fdfcf9 !important;
}

.default-yellow-mode :deep(.ant-table-tbody > tr:nth-child(even) > td) {
  background-color: #fdfcf9 !important;
}

.default-yellow-mode :deep(.ant-table-tbody > tr:nth-child(odd) > td) {
  background-color: #ffffff !important;
}

.default-yellow-mode :deep(.ant-table-tbody > tr:hover > td) {
  background-color: #faf7f0 !important;
}

.default-yellow-mode :deep(.ant-table-container) {
  border: 0.8px solid #f4efee !important;
  border-top: none !important;
}

.default-yellow-mode.table-surface--menu-list :deep(.ant-table-thead > tr > th) {
  padding: 12px 14px !important;
}

.default-yellow-mode.table-surface--menu-list :deep(.ant-table-tbody > tr > td) {
  padding: 12px 14px !important;
}

.default-yellow-mode.table-surface--menu-list :deep(.ant-table-tbody > tr.ant-table-measure-row) {
  height: 0 !important;
}

.default-yellow-mode.table-surface--menu-list :deep(.ant-table-tbody > tr.ant-table-measure-row > td) {
  height: 0 !important;
  padding: 0 !important;
  border: 0 !important;
  line-height: 0 !important;
  font-size: 0 !important;
}

.default-yellow-mode.table-surface--menu-list :deep(.ant-table-tbody > tr.ant-table-measure-row + tr > td),
.default-yellow-mode.table-surface--menu-list :deep(.ant-table-tbody > tr.ant-table-row:first-of-type > td) {
  padding-top: 20px !important;
}

.default-yellow-mode :deep(.ant-table-row-indent + .ant-table-row-expand-icon) {
  margin-right: 8px;
}

.default-yellow-mode :deep(.ant-table-row-indent.indent-level-1) {
  border-right: none !important;
}

.default-yellow-mode :deep(td),
.default-yellow-mode :deep(th) {
  border-right: none !important;
  border-left: none !important;
}

.transparent-glass-mode :deep(.ant-table),
.transparent-glass-mode :deep(.ant-table-container),
.transparent-glass-mode :deep(.ant-table-content),
.transparent-glass-mode :deep(.ant-table-body) {
  background: transparent !important;
  border: none !important;
}

.transparent-glass-mode :deep(.ant-table-thead > tr > th) {
  background: transparent !important;
  color: rgba(75, 85, 99, 0.85) !important;
  font-size: 13px !important;
  text-align: center !important;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06) !important;
  border-top: none !important;
  border-left: none !important;
  border-right: none !important;
}

.transparent-glass-mode :deep(.ant-table-thead > tr > th::before) {
  display: none !important;
}

.transparent-glass-mode :deep(.ant-table-tbody > tr > td) {
  background: transparent !important;
  text-align: center !important;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06) !important;
  border-top: none !important;
  border-left: none !important;
  border-right: none !important;
}

.transparent-glass-mode :deep(.ant-table-tbody > tr:hover > td) {
  background: rgba(255, 255, 255, 0.22) !important;
}

.transparent-glass-mode :deep(.ant-table-container) {
  border: 1px solid rgba(15, 23, 42, 0.05) !important;
  border-top: none !important;
  background: rgba(255, 255, 255, 0.18) !important;
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.transparent-glass-mode :deep(.ant-pagination-item),
.transparent-glass-mode :deep(.ant-pagination-prev .ant-pagination-item-link),
.transparent-glass-mode :deep(.ant-pagination-next .ant-pagination-item-link) {
  background: rgba(255, 255, 255, 0.4) !important;
  border: 1px solid rgba(15, 23, 42, 0.06) !important;
}

.transparent-glass-mode.table-surface--menu-list :deep(.ant-table),
.transparent-glass-mode.table-surface--menu-list :deep(.ant-table-container),
.transparent-glass-mode.table-surface--menu-list :deep(.ant-table-content),
.transparent-glass-mode.table-surface--menu-list :deep(.ant-table-body) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

.transparent-glass-mode.table-surface--menu-list :deep(.ant-table-container) {
  background: transparent !important;
  backdrop-filter: none !important;
  -webkit-backdrop-filter: none !important;
}

.transparent-glass-mode.table-surface--menu-list :deep(.ant-table-thead > tr > th) {
  padding: 12px 14px !important;
  background: transparent !important;
  color: var(--mono-text-secondary) !important;
  font-size: 12px !important;
  font-weight: 400 !important;
  text-align: center !important;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06) !important;
}

.transparent-glass-mode.table-surface--menu-list :deep(.ant-table-tbody > tr > td) {
  padding: 12px 14px !important;
  background: transparent !important;
  border-bottom: none !important;
}

.transparent-glass-mode.table-surface--menu-list :deep(.ant-table-tbody > tr.ant-table-measure-row) {
  height: 0 !important;
}

.transparent-glass-mode.table-surface--menu-list :deep(.ant-table-tbody > tr.ant-table-measure-row > td) {
  height: 0 !important;
  padding: 0 !important;
  border: 0 !important;
  background: transparent !important;
  line-height: 0 !important;
  font-size: 0 !important;
}

.transparent-glass-mode.table-surface--menu-list :deep(.ant-table-tbody > tr.ant-table-measure-row + tr > td),
.transparent-glass-mode.table-surface--menu-list :deep(.ant-table-tbody > tr.ant-table-row:first-of-type > td) {
  padding-top: 20px !important;
}

.transparent-glass-mode.table-surface--menu-list :deep(.ant-table-tbody > tr:hover > td) {
  background: #fafaf9 !important;
}

.transparent-glass-mode.table-surface--menu-list :deep(.ant-table-cell-fix-left),
.transparent-glass-mode.table-surface--menu-list :deep(.ant-table-cell-fix-right) {
  background: #ffffff !important;
}

.table-surface--menu-list :deep(.ant-pagination.ant-table-pagination) {
  margin: 10px 0 0 !important;
  width: 100%;
  display: flex !important;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}

.table-surface--menu-list :deep(.ant-pagination-total-text) {
  margin: 0 14px 0 0 !important;
  color: var(--mono-text-secondary) !important;
  font-size: 14px;
  font-weight: 500;
  height: 36px;
  line-height: 1 !important;
  display: flex !important;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  transform: translateY(-1px);
}

.table-surface--menu-list :deep(.ant-pagination-prev),
.table-surface--menu-list :deep(.ant-pagination-next),
.table-surface--menu-list :deep(.ant-pagination-item) {
  min-width: 36px;
  height: 36px;
  margin: 0 !important;
  display: flex !important;
  align-items: center;
  justify-content: center;
  line-height: 1 !important;
}

.table-surface--menu-list :deep(.ant-pagination-prev .ant-pagination-item-link),
.table-surface--menu-list :deep(.ant-pagination-next .ant-pagination-item-link) {
  width: 36px;
  min-width: 36px;
  height: 36px;
  border-radius: 999px !important;
  border: none !important;
  background: transparent !important;
  box-shadow: none !important;
  display: flex !important;
  align-items: center;
  justify-content: center;
  padding: 0 !important;
}

.table-surface--menu-list :deep(.ant-pagination-item a) {
  display: flex !important;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  line-height: 1 !important;
  color: var(--mono-text-secondary) !important;
}

.table-surface--menu-list :deep(.ant-pagination-item-active) {
  border: none !important;
  background: transparent !important;
}

.table-surface--menu-list :deep(.ant-pagination-item-active a) {
  color: var(--mono-text) !important;
  font-weight: 700 !important;
}

.table-surface--menu-list :deep(.std-table-pagination-node) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  min-width: 32px;
  border-radius: 999px !important;
  border: none !important;
  background: transparent !important;
  box-shadow: none !important;
  color: var(--mono-text-secondary) !important;
  font-size: 13px;
  line-height: 1 !important;
  padding: 0 !important;
  transition: color 0.18s ease, background 0.18s ease;
}

.table-surface--menu-list :deep(.std-table-pagination-node:hover) {
  background: transparent !important;
  color: var(--mono-text) !important;
}

.table-surface--menu-list :deep(.std-table-pagination-page--active) {
  color: var(--mono-text) !important;
  font-weight: 700 !important;
}

.table-surface--menu-list :deep(.std-table-pagination-control) {
  min-width: 36px;
}

.table-surface--menu-list :deep(.std-table-pagination-ellipsis) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--mono-text-secondary) !important;
}

.table-surface--menu-list :deep(.std-table-pagination-icon) {
  width: 14px;
  height: 14px;
}

.mono-table-shell :deep(.ant-table),
.mono-table-shell :deep(.ant-table-container),
.mono-table-shell :deep(.ant-table-content),
.mono-table-shell :deep(.ant-table-body) {
  background: transparent !important;
  border: none !important;
}

.mono-table-shell :deep(.ant-table-thead > tr > th) {
  padding: 12px 14px !important;
  background: transparent !important;
  color: var(--mono-text-secondary) !important;
  font-size: 12px !important;
  font-weight: 600 !important;
  text-align: center !important;
  border-bottom: 1px solid var(--mono-line) !important;
  border-top: none !important;
  border-left: none !important;
  border-right: none !important;
}

.mono-table-shell :deep(.ant-table-thead > tr > th::before) {
  display: none !important;
}

.mono-table-shell :deep(.ant-table-tbody > tr > td) {
  padding: 20px 14px !important;
  background: transparent !important;
  color: var(--mono-text) !important;
  text-align: center !important;
  border-bottom: 1px solid var(--mono-line) !important;
  border-top: none !important;
  border-left: none !important;
  border-right: none !important;
}

.mono-table-shell :deep(.ant-table-tbody > tr:hover > td) {
  background: transparent !important;
}

.mono-table-shell :deep(.ant-table-placeholder .ant-table-cell) {
  padding: 40px 16px !important;
  color: var(--mono-text-secondary) !important;
}

.mono-table-shell :deep(.ant-table-row-expand-icon) {
  border-radius: 999px !important;
  border-color: var(--mono-control-border) !important;
  background: var(--mono-control-bg) !important;
  color: var(--mono-control-text) !important;
  box-shadow: none !important;
}

.mono-table-shell :deep(.ant-table-cell-fix-left),
.mono-table-shell :deep(.ant-table-cell-fix-right) {
  background: var(--mono-surface-bg-elevated) !important;
}

.mono-table-shell :deep(.ant-pagination.ant-table-pagination) {
  margin: 16px 0 0 !important;
  display: flex !important;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}

.mono-table-shell :deep(.ant-pagination-total-text) {
  margin-right: 12px !important;
  color: var(--mono-text-secondary) !important;
}

.mono-table-shell :deep(.ant-pagination-item),
.mono-table-shell :deep(.ant-pagination-prev .ant-pagination-item-link),
.mono-table-shell :deep(.ant-pagination-next .ant-pagination-item-link) {
  min-width: 36px;
  height: 36px;
  border: none !important;
  border-radius: 999px !important;
  background: transparent !important;
  box-shadow: none !important;
}

.mono-table-shell :deep(.ant-pagination-item a),
.mono-table-shell :deep(.ant-pagination-prev .ant-pagination-item-link),
.mono-table-shell :deep(.ant-pagination-next .ant-pagination-item-link) {
  color: var(--mono-text-secondary) !important;
}

.mono-table-shell :deep(.ant-pagination-item-active) {
  background: transparent !important;
}

.mono-table-shell :deep(.ant-pagination-item-active a) {
  color: var(--mono-text) !important;
  font-weight: 700 !important;
}

html.dark .transparent-glass-mode :deep(.ant-table-thead > tr > th) {
  color: rgba(255, 255, 255, 0.72) !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

html.dark .transparent-glass-mode :deep(.ant-table-tbody > tr > td) {
  color: rgba(255, 255, 255, 0.88) !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

html.dark .transparent-glass-mode :deep(.ant-table-tbody > tr:hover > td) {
  background: rgba(255, 255, 255, 0.06) !important;
}

html.dark .transparent-glass-mode :deep(.ant-table-container) {
  background: rgba(18, 18, 18, 0.35) !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
}

html.dark .transparent-glass-mode.table-surface--menu-list :deep(.ant-table-tbody > tr:hover > td) {
  background: rgba(255, 255, 255, 0.06) !important;
}

html.dark .transparent-glass-mode.table-surface--menu-list :deep(.ant-table-cell-fix-left),
html.dark .transparent-glass-mode.table-surface--menu-list :deep(.ant-table-cell-fix-right) {
  background: rgba(18, 18, 18, 0.55) !important;
}
</style>
