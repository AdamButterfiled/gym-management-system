<template>
  <div :class="['table-search-toolbar', `table-search-toolbar--${variant}`]" role="search">
    <StandardInput
      v-if="showKeyword"
      :value="modelValue"
      no-margin
      :class="[
        'workspace-filter-control',
        'toolbar-keyword-input',
        variant === 'menu-list' && 'menu-search-input',
      ]"
      :width="keywordWidth"
      :placeholder="placeholder"
      @update:value="handleKeywordUpdate"
      @pressEnter="handleKeywordEnter"
    >
      <template #prefix>
        <Search class="toolbar-search-icon" aria-hidden="true" />
      </template>
    </StandardInput>

    <div
      :class="[
        'toolbar-action-group',
        `toolbar-action-group--${variant}`,
        variant === 'menu-list' && 'menu-search-actions',
      ]"
    >
      <StandardButton
        v-if="showFilter"
        type="search"
        icon="filter"
        @click="handleOpenFilter"
      >
        <span>筛选</span>
        <span v-if="filterCount > 0" class="filter-count filter-count--primary">{{ filterCount }}</span>
      </StandardButton>

      <StandardButton type="reset" icon="reload" @click="$emit('reset')">
        重置
      </StandardButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount } from 'vue';
import { Search } from 'lucide-vue-next';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardInput from '@/components/common/StandardInput.vue';

const props = withDefaults(defineProps<{
  modelValue?: string;
  placeholder?: string;
  loading?: boolean;
  filterCount?: number;
  showKeyword?: boolean;
  showFilter?: boolean;
  keywordWidth?: string;
  variant?: 'default' | 'menu-list';
}>(), {
  modelValue: '',
  placeholder: '请输入关键词',
  loading: false,
  filterCount: 0,
  showKeyword: true,
  showFilter: true,
  keywordWidth: 'min(340px, 100%)',
  variant: 'default',
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'search'): void;
  (e: 'openFilter'): void;
  (e: 'reset'): void;
}>();

let searchTimer: ReturnType<typeof setTimeout> | undefined;

function clearScheduledSearch() {
  if (!searchTimer) {
    return;
  }
  clearTimeout(searchTimer);
  searchTimer = undefined;
}

function scheduleSearch() {
  clearScheduledSearch();
  searchTimer = setTimeout(() => {
    emit('search');
  }, 280);
}

function handleKeywordUpdate(value: string | number | null | undefined) {
  emit('update:modelValue', String(value ?? ''));
  if (!props.showKeyword) {
    return;
  }
  scheduleSearch();
}

function handleKeywordEnter() {
  clearScheduledSearch();
  emit('search');
}

function handleOpenFilter() {
  clearScheduledSearch();
  emit('openFilter');
}

onBeforeUnmount(() => {
  clearScheduledSearch();
});
</script>

<style scoped>
.table-search-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  width: 100%;
}

.toolbar-keyword-input {
  flex: 0 0 auto;
  max-width: 100%;
}

.toolbar-keyword-input :deep(.std-input) {
  min-height: 40px;
  border-radius: var(--mono-radius-pill) !important;
}

.toolbar-keyword-input :deep(.std-input-inner) {
  padding: 0 18px;
}

.toolbar-keyword-input :deep(.std-input-control) {
  font-size: 14px;
  font-weight: 500;
}

.toolbar-search-icon {
  width: 15px;
  height: 15px;
}

.toolbar-action-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.table-search-toolbar--menu-list {
  gap: 14px !important;
}

.table-search-toolbar--menu-list .toolbar-keyword-input {
  flex: 0 1 250px;
  width: 250px;
  max-width: min(100%, 250px);
}

.toolbar-action-group--menu-list {
  align-self: center;
  gap: 10px !important;
  margin-left: 4px !important;
}

.toolbar-action-group--menu-list > * {
  flex: 0 0 auto;
}

.table-search-toolbar--menu-list .toolbar-keyword-input :deep(.std-input) {
  box-sizing: border-box !important;
  min-height: 44px;
  height: 44px;
  border-radius: var(--mono-radius-pill) !important;
  background: rgba(255, 255, 255, 0.72) !important;
  border: 1px solid rgba(17, 17, 17, 0.1) !important;
  box-shadow: none !important;
  transition: border-color 0.18s ease, background 0.18s ease !important;
}

.table-search-toolbar--menu-list .toolbar-keyword-input :deep(.std-input:hover),
.table-search-toolbar--menu-list .toolbar-keyword-input :deep(.std-input:focus),
.table-search-toolbar--menu-list .toolbar-keyword-input :deep(.std-input:focus-within) {
  background: #ffffff !important;
  border-color: rgba(17, 17, 17, 0.22) !important;
  box-shadow: none !important;
}

.table-search-toolbar--menu-list .toolbar-keyword-input :deep(.std-input-inner) {
  padding: 0 14px !important;
  gap: 8px;
}

.table-search-toolbar--menu-list .toolbar-keyword-input :deep(.std-input-control) {
  background: transparent !important;
  font-size: 13px;
  font-weight: 500;
  line-height: 1 !important;
  letter-spacing: -0.01em;
}

.table-search-toolbar--menu-list .toolbar-keyword-input :deep(.std-input-control::placeholder) {
  color: rgba(17, 17, 17, 0.38) !important;
  font-weight: 500;
}

.table-search-toolbar--menu-list .toolbar-keyword-input :deep(.std-input-prefix) {
  margin-right: 8px;
  color: rgba(17, 17, 17, 0.34) !important;
  font-size: 14px;
  transition: color 0.22s ease;
}

.table-search-toolbar--menu-list .toolbar-keyword-input :deep(.std-input:hover .std-input-prefix),
.table-search-toolbar--menu-list .toolbar-keyword-input :deep(.std-input:focus-within .std-input-prefix) {
  color: rgba(17, 17, 17, 0.58) !important;
}

.table-search-toolbar--menu-list :deep(.std-button) {
  --std-button-label-shift: 1px;
  min-width: 92px;
  height: 38px;
  min-height: 38px;
  padding-inline: 14px !important;
}

.table-search-toolbar--menu-list :deep(.std-button .std-button-label) {
  font-size: 14px !important;
  font-weight: 500 !important;
  line-height: 1 !important;
}

.table-search-toolbar--menu-list :deep(.std-button--icon-text .std-button-label) {
  transform: translateY(var(--std-button-label-shift)) !important;
}

.table-search-toolbar--menu-list :deep(.std-button .std-button-icon),
.table-search-toolbar--menu-list :deep(.std-button svg) {
  width: 15px;
  height: 15px;
}

.filter-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: var(--mono-radius-pill);
  background: rgba(17, 17, 17, 0.08);
  color: currentColor;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.filter-count--primary {
  background: rgba(255, 255, 255, 0.18);
}

:deep(.std-button--filter .filter-count),
:deep(.std-button--search .filter-count) {
  background: rgba(17, 17, 17, 0.08);
}

:deep(.std-button--search .filter-count--primary) {
  background: rgba(255, 255, 255, 0.18);
}
</style>

<style>
html.dark .table-search-toolbar--menu-list .toolbar-keyword-input .std-input {
  background: rgba(255, 255, 255, 0.055) !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
  color: var(--mono-control-text) !important;
  box-shadow: none !important;
}

html.dark .table-search-toolbar--menu-list .toolbar-keyword-input .std-input:hover,
html.dark .table-search-toolbar--menu-list .toolbar-keyword-input .std-input:focus,
html.dark .table-search-toolbar--menu-list .toolbar-keyword-input .std-input:focus-within {
  background: rgba(255, 255, 255, 0.08) !important;
  border-color: rgba(255, 255, 255, 0.18) !important;
  box-shadow: none !important;
}

html.dark .table-search-toolbar--menu-list .toolbar-keyword-input .std-input-control {
  color: var(--mono-control-text) !important;
}

html.dark .table-search-toolbar--menu-list .toolbar-keyword-input .std-input-control::placeholder {
  color: var(--mono-text-tertiary) !important;
}

html.dark .table-search-toolbar--menu-list .toolbar-keyword-input .std-input-prefix,
html.dark .table-search-toolbar--menu-list .toolbar-keyword-input .std-input:hover .std-input-prefix,
html.dark .table-search-toolbar--menu-list .toolbar-keyword-input .std-input:focus-within .std-input-prefix {
  color: var(--mono-text-secondary) !important;
}
</style>
