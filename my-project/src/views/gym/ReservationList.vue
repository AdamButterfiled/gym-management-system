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
        :scroll="{ x: 1450 }"
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
            {{ formatTimeRange(record.startTime, record.endTime) }}
          </template>
          <template v-if="column.key === 'actions'">
            <div class="order-row-actions">
              <StandardButton
                v-if="canReview(record)"
                type="text"
                size="sm"
                @click="reviewBooking(record, true)"
              >
                通过
              </StandardButton>
              <StandardButton
                v-if="canReview(record)"
                type="text"
                size="sm"
                danger
                @click="reviewBooking(record, false)"
              >
                拒绝
              </StandardButton>
              <StandardButton
                v-if="canCancel(record)"
                type="text"
                size="sm"
                danger
                @click="cancelBooking(record)"
              >
                取消
              </StandardButton>
            </div>
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
import { message } from 'ant-design-vue';
import request from '@/request';
import { BookingOrder } from '@/types';
import { usePageStyle } from '@/hooks/usePageStyle';
import StandardTable from '@/components/common/StandardTable.vue';
import WorkspacePage from '@/components/common/WorkspacePage.vue';
import TableSearchToolbar from '@/components/common/TableSearchToolbar.vue';
import AdvancedFilterModal from '@/components/common/AdvancedFilterModal.vue';
import StandardButton from '@/components/common/StandardButton.vue';
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
} = useConfiguredTablePage<BookingOrder>({
  routePath: '/reservation',
});

const baseColumns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '资源名称', dataIndex: 'resourceName', key: 'resourceName', width: 180 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 100 },
  { title: '资源类型', dataIndex: 'resourceType', key: 'resourceType', width: 140 },
  { title: '预约时间', key: 'period', width: 260 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '支付状态', dataIndex: 'paymentStatus', key: 'paymentStatus', width: 140 },
  { title: '预约状态', dataIndex: 'status', key: 'status', width: 140 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 180 },
  { title: '操作', key: 'actions', width: 190, fixed: 'right' }
];
const reservationColumnPriority = [
  'orderNo',
  'resourceName',
  'userId',
  'resourceType',
  'period',
  'amount',
  'paymentStatus',
  'status',
  'remark',
  'actions',
];

const columns = computed(() => sortColumnsByPriority(buildColumns(baseColumns), reservationColumnPriority));

const formatDateTime = (value?: string) => {
  if (!value) {
    return '-';
  }

  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm') : value.replace('T', ' ');
};

const formatTimeRange = (start?: string, end?: string) => {
  if (!start && !end) {
    return '-';
  }

  const startAtValue = start ? dayjs(start) : null;
  const endAtValue = end ? dayjs(end) : null;

  if (startAtValue?.isValid() && endAtValue?.isValid() && startAtValue.isSame(endAtValue, 'day')) {
    return `${startAtValue.format('YYYY-MM-DD HH:mm')} - ${endAtValue.format('HH:mm')}`;
  }

  return `${formatDateTime(start)} - ${formatDateTime(end)}`;
};

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
  if (bookingStatus === 'PENDING_PAY') return '待支付';
  if (bookingStatus === 'CREATED') return '待确认';
  if (bookingStatus === 'CHECKED_IN') return '已签到';
  if (bookingStatus === 'COMPLETED') return '已完成';
  if (bookingStatus === 'CANCELLED') return '已取消';
  if (bookingStatus === 'REFUNDED') return '已退款';
  return bookingStatus || '-';
};

const bookingTone = (bookingStatus: string) => {
  if (bookingStatus === 'CONFIRMED' || bookingStatus === 'CHECKED_IN') return 'status-pill--strong';
  if (bookingStatus === 'CANCELLED' || bookingStatus === 'REFUNDED') return 'status-pill--soft';
  return 'status-pill--muted';
};

const canReview = (record: BookingOrder) => (
  record.resourceType === 'PRIVATE_COACH' && record.status === 'CREATED'
);

const canCancel = (record: BookingOrder) => (
  !['CANCELLED', 'REFUNDED', 'CHECKED_IN', 'COMPLETED'].includes(record.status)
);

const reviewBooking = async (record: BookingOrder, approved: boolean) => {
  const action = approved ? 'approve' : 'reject';
  const res = await request.post(`/admin/bookings/${record.id}/${action}`) as any;
  if (res.code === 200) {
    message.success(approved ? '预约已通过' : '预约已拒绝');
    await loadData();
  } else {
    message.error(res.msg || (approved ? '预约通过失败' : '预约拒绝失败'));
  }
};

const cancelBooking = async (record: BookingOrder) => {
  const res = await request.post(`/admin/bookings/${record.id}/cancel`) as any;
  if (res.code === 200) {
    message.success('预约已取消');
    await loadData();
  } else {
    message.error(res.msg || '预约取消失败');
  }
};

onMounted(() => {
  loadMenuConfig();
  ensureConfig();
  loadData();
});
</script>

<style scoped>
.order-row-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}
</style>
