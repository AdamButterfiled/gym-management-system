<template>
  <WorkspacePage title="预约订单管理" variant="menu-list">
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
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }: { column: any; record: BookingOrder }">
          <template v-if="column.key === 'resourceType'">
            <span :class="['status-pill', resourceTone(record.resourceType)]">{{ resourceLabel(record.resourceType) }}</span>
          </template>
          <template v-if="column.key === 'amount'">
            ¥ {{ Number(record.amount || 0).toFixed(2) }}
          </template>
          <template v-if="column.key === 'paymentStatus'">
            <span :class="['status-pill', paymentTone(record.paymentStatus)]">{{ paymentLabel(record.paymentStatus) }}</span>
          </template>
          <template v-if="column.key === 'status'">
            <span :class="['status-pill', bookingTone(record.status)]">{{ bookingLabel(record.status) }}</span>
          </template>
          <template v-if="column.key === 'period'">
            {{ record.startTime }}<br />{{ record.endTime }}
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
import { BookingOrder } from '@/types';
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
} = useConfiguredTablePage<BookingOrder>({
  routePath: '/reservation',
});

const baseColumns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 100 },
  { title: '资源类型', dataIndex: 'resourceType', key: 'resourceType', width: 140 },
  { title: '资源名称', dataIndex: 'resourceName', key: 'resourceName', width: 180 },
  { title: '预约时间', key: 'period', width: 220 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '支付状态', dataIndex: 'paymentStatus', key: 'paymentStatus', width: 140 },
  { title: '预约状态', dataIndex: 'status', key: 'status', width: 140 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 180 }
];
const columns = computed(() => buildColumns(baseColumns));

const resourceLabel = (type: string) => {
  if (type === 'GROUP_COURSE') return '团课';
  if (type === 'PRIVATE_COACH') return '私教';
  return '场馆';
};

const resourceTone = (type: string) => {
  if (type === 'GROUP_COURSE') return 'status-pill--soft';
  if (type === 'PRIVATE_COACH') return 'status-pill--muted';
  return 'status-pill--strong';
};

const paymentLabel = (paymentStatus: string) => {
  if (paymentStatus === 'PAID') return '已支付';
  if (paymentStatus === 'REFUNDED') return '已退款';
  if (paymentStatus === 'CLOSED') return '已关闭';
  return '待支付';
};

const paymentTone = (paymentStatus: string) => {
  if (paymentStatus === 'PAID') return 'status-pill--strong';
  if (paymentStatus === 'REFUNDED') return 'status-pill--muted';
  if (paymentStatus === 'CLOSED') return 'status-pill--soft';
  return 'status-pill--muted';
};

const bookingLabel = (bookingStatus: string) => {
  if (bookingStatus === 'CONFIRMED') return '已确认';
  if (bookingStatus === 'CHECKED_IN') return '已签到';
  if (bookingStatus === 'CANCELLED') return '已取消';
  if (bookingStatus === 'REFUNDED') return '已退款';
  return '已创建';
};

const bookingTone = (bookingStatus: string) => {
  if (bookingStatus === 'CONFIRMED' || bookingStatus === 'CHECKED_IN') return 'status-pill--strong';
  if (bookingStatus === 'CANCELLED' || bookingStatus === 'REFUNDED') return 'status-pill--soft';
  return 'status-pill--muted';
};

onMounted(() => {
  loadMenuConfig();
  ensureConfig();
  loadData();
});
</script>

<style scoped></style>
