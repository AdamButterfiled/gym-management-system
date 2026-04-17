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

    <div v-if="menuPaginationState?.showFooter" class="std-table-footer">
      <Select
        :model-value="String(menuPaginationState.pageSize)"
        @update:model-value="handlePageSizeChange"
      >
        <SelectTrigger class="std-table-size-trigger" size="sm" aria-label="选择每页条数">
          <SelectValue />
        </SelectTrigger>
        <SelectContent class="std-table-size-content">
          <SelectItem
            v-for="size in menuPaginationState.pageSizeOptions"
            :key="size"
            :value="String(size)"
          >
            {{ size }} 条/页
          </SelectItem>
        </SelectContent>
      </Select>

      <div class="std-table-footer-shell">
        <span v-if="menuPaginationState.totalText" class="std-table-footer-total">
          {{ menuPaginationState.totalText }}
        </span>

        <Pagination
          v-if="menuPaginationState.showPager"
          class="std-table-footer-pagination"
          :page="menuPaginationState.currentPage"
          :items-per-page="menuPaginationState.pageSize"
          :total="menuPaginationState.total"
          :sibling-count="menuPaginationState.siblingCount"
          :show-edges="menuPaginationState.totalPages > 1"
          @update:page="handleMenuPageChange"
        >
          <template #default="{ page, pageCount }">
            <PaginationContent v-slot="{ items }">
              <PaginationPrevious
                size="icon-sm"
                class="std-table-pagination-button std-table-pagination-nav"
                :disabled="page <= 1"
              >
                <ChevronLeft class="size-4" aria-hidden="true" />
                <span class="sr-only">上一页</span>
              </PaginationPrevious>

              <template
                v-for="(item, index) in items"
                :key="`${item.type}-${item.value ?? index}`"
              >
                <template v-if="item.type === 'page'">
                  <div
                    v-if="item.value === page && isPageEditing"
                    class="std-table-page-editor"
                  >
                    <input
                      ref="pageEditorInput"
                      v-model="pageEditDraft"
                      class="std-table-page-editor-input"
                      inputmode="numeric"
                      aria-label="输入页码"
                      @blur="commitPageEdit"
                      @keydown.enter.prevent="commitPageEdit"
                      @keydown.esc.prevent="cancelPageEdit"
                    >
                  </div>
                  <PaginationItem
                    v-else
                    :value="item.value"
                    size="icon-sm"
                    class="std-table-pagination-button std-table-pagination-page"
                    :title="item.value === page ? '双击直接跳转页码' : undefined"
                    @dblclick.stop.prevent="item.value === page && startPageEdit(page)"
                  >
                    {{ item.value }}
                  </PaginationItem>
                </template>
                <PaginationEllipsis
                  v-else
                  class="std-table-pagination-ellipsis-node"
                />
              </template>

              <PaginationNext
                size="icon-sm"
                class="std-table-pagination-button std-table-pagination-nav"
                :disabled="page >= pageCount"
              >
                <ChevronRight class="size-4" aria-hidden="true" />
                <span class="sr-only">下一页</span>
              </PaginationNext>
            </PaginationContent>
          </template>
        </Pagination>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, nextTick, ref, useAttrs, watch } from 'vue';
import { ChevronLeft, ChevronRight } from 'lucide-vue-next';
import { cn } from '@/lib/utils';
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationNext,
  PaginationPrevious,
} from '@/components/ui/pagination';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

const props = defineProps<{
  configStyle?: 'default' | 'glass';
  surface?: 'default' | 'menu-list';
}>();

const attrs = useAttrs();

type TablePaginationConfig = Record<string, any>;
const DEFAULT_PAGE_SIZE_OPTIONS = [10, 20, 30, 50];
const isPageEditing = ref(false);
const pageEditDraft = ref('');
const pageEditorInput = ref<HTMLInputElement | null>(null);

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

const rawPagination = computed(() => attrs.pagination);

const useCustomMenuPagination = computed(() => (
  props.surface === 'menu-list'
  && rawPagination.value !== false
  && rawPagination.value !== undefined
));

const menuPaginationState = computed(() => {
  if (!useCustomMenuPagination.value) {
    return null;
  }

  const base = typeof rawPagination.value === 'object' && rawPagination.value !== null
    ? rawPagination.value as TablePaginationConfig
    : {};

  const pageSize = Math.max(1, Number(base.pageSize ?? 10));
  const total = Math.max(0, Number(base.total ?? 0));
  const totalPages = Math.max(1, Math.ceil(total / pageSize));
  const currentPage = Math.min(Math.max(1, Number(base.current ?? 1)), totalPages);
  const siblingCount = base.showLessItems ? 1 : 2;
  const rangeStart = total === 0 ? 0 : (currentPage - 1) * pageSize + 1;
  const rangeEnd = total === 0 ? 0 : Math.min(total, currentPage * pageSize);
  const totalText = typeof base.showTotal === 'function'
    ? base.showTotal(total, [rangeStart, rangeEnd])
    : '';
  const hideOnSinglePage = Boolean(base.hideOnSinglePage);
  const showPager = !hideOnSinglePage || totalPages > 1;
  const pageSizeOptions = Array.from(
    new Set(
      [...DEFAULT_PAGE_SIZE_OPTIONS, pageSize, ...(base.pageSizeOptions || [])]
        .map((value) => Number(value))
        .filter((value) => Number.isFinite(value) && value > 0),
    ),
  ).sort((left, right) => left - right);
  const showFooter = showPager || Boolean(totalText) || pageSizeOptions.length > 0;

  return {
    base,
    currentPage,
    pageSize,
    total,
    totalPages,
    siblingCount,
    totalText,
    showPager,
    pageSizeOptions,
    showFooter,
  };
});

const invokeChangeHandler = (nextPagination: TablePaginationConfig) => {
  const handler = attrs.onChange;
  const payload = [nextPagination, {}, {}, { currentDataSource: (attrs.dataSource as unknown[]) || [] }] as const;

  if (Array.isArray(handler)) {
    handler.forEach((entry) => {
      if (typeof entry === 'function') {
        entry(...payload);
      }
    });
    return;
  }

  if (typeof handler === 'function') {
    handler(...payload);
  }
};

const handleMenuPageChange = (page: number) => {
  const state = menuPaginationState.value;

  if (!state || page === state.currentPage) {
    return;
  }

  if (typeof state.base.onChange === 'function') {
    state.base.onChange(page, state.pageSize);
  }

  invokeChangeHandler({
    ...state.base,
    current: page,
    pageSize: state.pageSize,
    total: state.total,
  });
};

const startPageEdit = async (page: number) => {
  isPageEditing.value = true;
  pageEditDraft.value = String(page);
  await nextTick();
  pageEditorInput.value?.focus();
  pageEditorInput.value?.select();
};

const cancelPageEdit = () => {
  isPageEditing.value = false;
  pageEditDraft.value = '';
};

const commitPageEdit = () => {
  const state = menuPaginationState.value;

  if (!state) {
    cancelPageEdit();
    return;
  }

  const parsed = Number.parseInt(pageEditDraft.value, 10);
  cancelPageEdit();

  if (!Number.isFinite(parsed)) {
    return;
  }

  const targetPage = Math.min(Math.max(parsed, 1), state.totalPages);
  handleMenuPageChange(targetPage);
};

const handlePageSizeChange = (value: string | number) => {
  const state = menuPaginationState.value;

  if (!state) {
    return;
  }

  const nextPageSize = Number(value);

  if (!Number.isFinite(nextPageSize) || nextPageSize <= 0 || nextPageSize === state.pageSize) {
    return;
  }

  const nextPage = 1;

  if (typeof state.base.onShowSizeChange === 'function') {
    state.base.onShowSizeChange(nextPage, nextPageSize);
  }

  if (typeof state.base.onChange === 'function') {
    state.base.onChange(nextPage, nextPageSize);
  }

  invokeChangeHandler({
    ...state.base,
    current: nextPage,
    pageSize: nextPageSize,
    total: state.total,
  });
};

watch(
  () => [menuPaginationState.value?.currentPage, menuPaginationState.value?.pageSize],
  () => {
    cancelPageEdit();
  },
);

const tableAttrs = computed(() => ({
  ...attrs,
  components: tableComponents.value,
  pagination: useCustomMenuPagination.value ? false : rawPagination.value,
}));

const containerClass = computed(() => {
  return cn(
    props.configStyle === 'glass'
      ? 'transparent-glass-mode'
      : props.configStyle === 'default'
        ? 'default-yellow-mode'
        : 'mono-table-shell',
    props.surface === 'menu-list' && 'table-surface--menu-list',
    useCustomMenuPagination.value && 'table-surface--menu-list--custom-pagination',
  );
});
</script>

<style scoped>
.table-surface--menu-list--custom-pagination :deep(.ant-table-container) {
  overflow: hidden;
  border-radius: var(--mono-radius-xl) !important;
}

.table-surface--menu-list--custom-pagination :deep(.ant-table),
.table-surface--menu-list--custom-pagination :deep(.ant-table-wrapper),
.table-surface--menu-list--custom-pagination :deep(.ant-spin-nested-loading),
.table-surface--menu-list--custom-pagination :deep(.ant-spin-container),
.table-surface--menu-list--custom-pagination :deep(.ant-table-content) {
  border-radius: inherit;
}

.table-surface--menu-list--custom-pagination :deep(.ant-table-tbody > tr:last-child > td),
.table-surface--menu-list--custom-pagination :deep(.ant-table-placeholder .ant-table-cell) {
  border-bottom: none !important;
}

.table-surface--menu-list--custom-pagination .std-table-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  padding: 12px 0 0;
}

.table-surface--menu-list--custom-pagination .std-table-size-trigger {
  min-width: auto;
  height: 32px;
  border: none !important;
  border-radius: var(--mono-radius-pill) !important;
  background: var(--mono-select-trigger-bg) !important;
  box-shadow:
    inset 0 1px 0 var(--mono-select-glass-highlight),
    inset 0 0 0 1px var(--mono-select-glass-stroke) !important;
  color: var(--mono-text-secondary) !important;
  padding: 0 10px !important;
  gap: 6px !important;
  font-size: 13px !important;
  font-weight: 400;
  backdrop-filter: blur(28px) saturate(180%);
}

.table-surface--menu-list--custom-pagination .std-table-size-trigger:hover {
  color: var(--mono-text) !important;
  background: var(--mono-select-trigger-bg-hover) !important;
}

.table-surface--menu-list--custom-pagination .std-table-size-content {
  border: none !important;
  border-radius: var(--mono-radius-lg) !important;
  background: var(--mono-select-menu-bg) !important;
  box-shadow:
    inset 0 1px 0 var(--mono-select-glass-highlight),
    inset 0 0 0 1px var(--mono-select-glass-stroke) !important;
  backdrop-filter: blur(32px) saturate(185%);
}

.table-surface--menu-list--custom-pagination .std-table-footer-shell {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  min-height: 32px;
  padding: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.table-surface--menu-list--custom-pagination .std-table-footer-total {
  color: var(--mono-text-secondary);
  font-size: 13px;
  font-weight: 400;
  white-space: nowrap;
  margin-right: 6px;
}

.table-surface--menu-list--custom-pagination .std-table-footer-pagination {
  width: auto;
  margin: 0;
  justify-content: flex-end;
}

.table-surface--menu-list--custom-pagination .std-table-footer-pagination :deep([data-slot='pagination-content']) {
  gap: 4px;
}

.table-surface--menu-list--custom-pagination .std-table-pagination-button {
  min-width: 28px;
  height: 32px;
  border-radius: 0 !important;
  border: none !important;
  background: transparent !important;
  box-shadow: none !important;
  color: var(--mono-text-secondary) !important;
  padding: 0 !important;
}

.table-surface--menu-list--custom-pagination .std-table-pagination-nav {
  width: 28px;
  padding: 0 !important;
  gap: 0 !important;
  opacity: 0.72;
}

.table-surface--menu-list--custom-pagination .std-table-pagination-page[data-selected='true'] {
  color: var(--mono-text) !important;
  font-weight: 400 !important;
}

.table-surface--menu-list--custom-pagination .std-table-page-editor {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 32px;
}

.table-surface--menu-list--custom-pagination .std-table-page-editor-input {
  width: 30px;
  height: 28px;
  border: none;
  background: transparent;
  color: var(--mono-text);
  font-size: 17px;
  font-weight: 400;
  text-align: center;
  outline: none;
}

.table-surface--menu-list--custom-pagination .std-table-pagination-ellipsis-node {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 32px;
  color: var(--mono-text-secondary);
}

.table-surface--menu-list--custom-pagination .std-table-pagination-button:not(:disabled):hover {
  color: var(--mono-text) !important;
  background: transparent !important;
}

.table-surface--menu-list--custom-pagination .std-table-pagination-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

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
  border-radius: var(--mono-radius-pill) !important;
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
  border-radius: var(--mono-radius-pill) !important;
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
  border-radius: var(--mono-radius-pill) !important;
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
  border-radius: var(--mono-radius-pill) !important;
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

html.dark .table-surface--menu-list--custom-pagination .std-table-pagination-button {
  color: rgba(255, 255, 255, 0.68) !important;
}

html.dark .table-surface--menu-list--custom-pagination .std-table-size-trigger {
  background: var(--mono-select-trigger-bg) !important;
  box-shadow: none !important;
}

html.dark .table-surface--menu-list--custom-pagination .std-table-footer-shell {
  background: transparent !important;
  box-shadow: none !important;
}

html.dark .table-surface--menu-list--custom-pagination .std-table-size-content {
  background: var(--mono-select-menu-bg) !important;
  box-shadow: none !important;
}

html.dark .table-surface--menu-list--custom-pagination .std-table-pagination-button:not(:disabled):hover,
html.dark .table-surface--menu-list--custom-pagination .std-table-pagination-page[data-selected='true'] {
  color: rgba(255, 255, 255, 0.92) !important;
}
</style>
