<template>
  <WorkspacePage title="排期冲突日志" variant="menu-list">
    <template #filters>
      <TableSearchToolbar
        v-model="keyword"
        variant="menu-list"
        :placeholder="quickSearchPlaceholder"
        :loading="loading"
        :filter-count="activeFilterCount"
        :show-keyword="quickSearchEnabled"
        @search="handleSearch"
        @open-filter="filterModalVisible = true"
        @reset="handleReset"
      />
    </template>

    <section class="workspace-subsection">
      <StandardTable
        :configStyle="currentStyle"
        surface="menu-list"
        :dataSource="tableData"
        :columns="columns"
        :pagination="pagination"
        rowKey="id"
        @change="handleTableChange"
      />
    </section>
  </WorkspacePage>

  <AdvancedFilterModal
    v-model:visible="filterModalVisible"
    :fields="filterableFields"
    :logic="filterLogic"
    :rules="filterRules"
    :text-suggestions="textSuggestions"
    @apply="applyAdvancedFilters"
  />
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { ScheduleConflict } from '@/types';
import { usePageStyle } from '@/hooks/usePageStyle';
import StandardTable from '@/components/common/StandardTable.vue';
import WorkspacePage from '@/components/common/WorkspacePage.vue';
import TableSearchToolbar from '@/components/common/TableSearchToolbar.vue';
import AdvancedFilterModal from '@/components/common/AdvancedFilterModal.vue';
import { useConfiguredTablePage } from '@/composables/useConfiguredTablePage';

const { currentStyle, loadMenuConfig } = usePageStyle();
const {
  filterableFields,
  quickSearchEnabled,
  quickSearchPlaceholder,
  keyword,
  filterLogic,
  filterRules,
  filterModalVisible,
  activeFilterCount,
  tableData,
  pagination,
  loading,
  textSuggestions,
  ensureConfig,
  loadData,
  handleSearch,
  handleReset,
  handleTableChange,
  applyAdvancedFilters,
  buildColumns,
} = useConfiguredTablePage<ScheduleConflict>({
  routePath: '/schedule-conflicts',
});

const baseColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '资源类型', dataIndex: 'resourceType', key: 'resourceType', width: 140 },
  { title: '冲突类型', dataIndex: 'conflictType', key: 'conflictType', width: 160 },
  { title: '消息', dataIndex: 'message', key: 'message' },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 180 },
  { title: '结束时间', dataIndex: 'endTime', key: 'endTime', width: 180 },
  { title: '记录时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 }
];
const columns = computed(() => buildColumns(baseColumns));

onMounted(() => {
  loadMenuConfig();
  ensureConfig();
  loadData();
});
</script>

<style scoped></style>
