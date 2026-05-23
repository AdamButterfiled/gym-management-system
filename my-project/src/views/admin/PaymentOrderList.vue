<template>
  <WorkspacePage title="支付订单管理" variant="menu-list">
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
      >
        <template #bodyCell="{ column, record }: { column: any; record: PaymentOrder }">
          <template v-if="column.key === 'amount'">
            ¥ {{ Number(record.amount || 0).toFixed(2) }}
          </template>
          <template v-if="column.key === 'status'">
            <span :class="['status-pill', badgeTone(record.status)]">{{ badgeLabel(record.status) }}</span>
          </template>
          <template v-if="column.key === 'paidAt'">
            {{ formatDateTime(record.paidAt) }}
          </template>
        </template>
      </StandardTable>
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
import dayjs from 'dayjs';
import { PaymentOrder } from '@/types';
import { usePageStyle } from '@/hooks/usePageStyle';
import StandardTable from '@/components/common/StandardTable.vue';
import WorkspacePage from '@/components/common/WorkspacePage.vue';
import TableSearchToolbar from '@/components/common/TableSearchToolbar.vue';
import AdvancedFilterModal from '@/components/common/AdvancedFilterModal.vue';
import { useConfiguredTablePage } from '@/composables/useConfiguredTablePage';
import { sortColumnsByPriority } from '@/utils/tableColumns';

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
} = useConfiguredTablePage<PaymentOrder>({
  routePath: '/payment-orders',
});

const baseColumns = [
  { title: '支付单号', dataIndex: 'paymentNo', key: 'paymentNo', width: 180 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 140 },
  { title: '支付时间', dataIndex: 'paidAt', key: 'paidAt', width: 180 },
  { title: '支付类型', dataIndex: 'paymentType', key: 'paymentType', width: 140 },
  { title: '目标类型', dataIndex: 'targetType', key: 'targetType', width: 150 },
  { title: '目标ID', dataIndex: 'targetId', key: 'targetId', width: 100 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 100 }
];
const paymentColumnPriority = ['paymentNo', 'amount', 'status', 'paidAt', 'paymentType', 'targetType', 'targetId', 'userId'];
const columns = computed(() => sortColumnsByPriority(buildColumns(baseColumns), paymentColumnPriority));

const formatDateTime = (value?: string) => {
  if (!value) {
    return '-';
  }

  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm') : value.replace('T', ' ');
};

const badgeLabel = (value: string) => {
  if (value === 'PAID') return '已支付';
  if (value === 'REFUNDED') return '已退款';
  if (value === 'CLOSED') return '已关闭';
  return '待支付';
};

const badgeTone = (value: string) => {
  if (value === 'PAID') return 'status-pill--strong';
  if (value === 'REFUNDED') return 'status-pill--muted';
  if (value === 'CLOSED') return 'status-pill--soft';
  return 'status-pill--muted';
};

onMounted(() => {
  loadMenuConfig();
  ensureConfig();
  loadData();
});
</script>

<style scoped></style>
