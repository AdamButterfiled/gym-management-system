<template>
  <WorkspacePage title="签到核销记录" variant="menu-list">
    <template #actions>
      <div class="checkin-token-actions">
        <a-input
          v-model:value="token"
          class="workspace-filter-control checkin-token-input"
          placeholder="输入会员签到 token"
          @pressEnter="consumeToken"
        />
        <StandardButton type="search" icon="search" @click="consumeToken">立即核销</StandardButton>
      </div>
    </template>

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
import { computed, onMounted, ref } from 'vue';
import { message } from 'ant-design-vue';
import request from '@/request';
import { CheckinRecord } from '@/types';
import { usePageStyle } from '@/hooks/usePageStyle';
import { useConfiguredTablePage } from '@/composables/useConfiguredTablePage';
import TableSearchToolbar from '@/components/common/TableSearchToolbar.vue';
import AdvancedFilterModal from '@/components/common/AdvancedFilterModal.vue';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardTable from '@/components/common/StandardTable.vue';
import WorkspacePage from '@/components/common/WorkspacePage.vue';

const { currentStyle, loadMenuConfig } = usePageStyle();
const token = ref('');
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
} = useConfiguredTablePage<CheckinRecord>({
  routePath: '/checkin-records',
});

const baseColumns = [
  { title: '记录ID', dataIndex: 'id', key: 'id', width: 100 },
  { title: '预约单ID', dataIndex: 'bookingOrderId', key: 'bookingOrderId', width: 120 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 100 },
  { title: '签到码', dataIndex: 'checkinCode', key: 'checkinCode', width: 260 },
  { title: '签到时间', dataIndex: 'checkinTime', key: 'checkinTime', width: 180 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '核销人', dataIndex: 'operatorName', key: 'operatorName', width: 120 }
];
const columns = computed(() => buildColumns(baseColumns));

const consumeToken = async () => {
  if (!token.value) {
    message.warning('请输入签到码');
    return;
  }
  const res = await request.post('/admin/checkins/consume', { token: token.value });
  if (res.code === 200) {
    message.success('核销成功');
    token.value = '';
    loadData();
  } else {
    message.error(res.msg || '核销失败');
  }
};

onMounted(async () => {
  loadMenuConfig();
  await ensureConfig();
  await loadData();
});
</script>

<style scoped>
.checkin-token-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.checkin-token-input.ant-input {
  width: 320px;
  min-height: 40px;
  padding: 0 18px !important;
  border-radius: var(--mono-radius-pill) !important;
}
</style>
