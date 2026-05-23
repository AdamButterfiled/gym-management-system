<template>
  <WorkspacePage class="private-schedule-page adaptive-table-page" title="私教排班管理" variant="menu-list">
    <template #actions>
      <StandardButton type="add" icon="plus" @click="openAdd">新增私教时段</StandardButton>
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

    <section ref="tableSectionRef" class="workspace-subsection">
      <StandardTable
        :configStyle="currentStyle"
        surface="menu-list"
        :dataSource="tableData"
        :columns="columns"
        :pagination="pagination"
        rowKey="id"
        :scroll="tableScroll"
        @change="handlePrivateTableChange"
      >
        <template #bodyCell="{ column, record }: { column: any; record: PrivateSchedule }">
          <template v-if="column.key === 'coachId'">
            {{ coachNameMap[record.coachId || 0] || '-' }}
          </template>
          <template v-if="column.key === 'venueId'">
            {{ venueNameMap[record.venueId || 0] || '-' }}
          </template>
          <template v-if="column.key === 'scheduleDate'">
            {{ formatScheduleDate(record.startTime) }}
          </template>
          <template v-if="column.key === 'timeRange'">
            {{ formatClockRange(record.startTime, record.endTime) }}
          </template>
          <template v-if="column.key === 'bookedCount'">
            {{ record.bookedCount || 0 }} / {{ record.capacity || 1 }}
          </template>
          <template v-if="column.key === 'status'">
            <span :class="['status-pill', record.status === 'OPEN' ? 'status-pill--strong' : 'status-pill--soft']">
              {{ record.status === 'CLOSED' ? '已关闭' : '开放预约' }}
            </span>
          </template>
          <template v-if="column.key === 'description'">
            <span class="private-note-cell">{{ record.description || '-' }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space class="private-row-actions">
              <StandardButton type="link" size="sm" class="table-action-link" @click="openEdit(record)">编辑</StandardButton>
              <a-popconfirm title="确定删除该私教时段吗？" @confirm="handleDelete(record.id!)">
                <StandardButton type="link" size="sm" danger class="table-action-link">删除</StandardButton>
              </a-popconfirm>
            </a-space>
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

    <StandardModal v-model:visible="modalVisible" :title="modalTitle" width="760px" @ok="handleSubmit">
      <a-form
        :model="formState"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
        class="workspace-modal-form"
      >
        <ConfiguredFormLayout :fields="primaryFormFields">
          <template #field-coachId>
            <a-form-item label="教练">
              <a-select v-model:value="formState.coachId">
                <a-select-option v-for="item in coachOptions" :key="item.id" :value="item.id">{{ item.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </template>
          <template #field-venueId>
            <a-form-item label="场馆">
              <a-select v-model:value="formState.venueId">
                <a-select-option v-for="item in venueOptions" :key="item.id" :value="item.id">{{ item.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </template>
          <template #field-startTime>
            <a-form-item label="开始时间">
              <a-date-picker v-model:value="startAt" show-time style="width: 100%" />
            </a-form-item>
          </template>
          <template #field-endTime>
            <a-form-item label="结束时间">
              <a-date-picker v-model:value="endAt" show-time style="width: 100%" />
            </a-form-item>
          </template>
          <template #field-capacity>
            <a-form-item label="可约人数">
              <a-input-number v-model:value="formState.capacity" :min="1" style="width: 100%" />
            </a-form-item>
          </template>
          <template #field-status>
            <a-form-item label="状态">
              <a-select v-model:value="formState.status">
                <a-select-option value="OPEN">OPEN</a-select-option>
                <a-select-option value="CLOSED">CLOSED</a-select-option>
              </a-select>
            </a-form-item>
          </template>
          <template #field-description>
            <a-form-item label="备注">
              <a-textarea v-model:value="formState.description" :rows="3" />
            </a-form-item>
          </template>
        </ConfiguredFormLayout>
      </a-form>
    </StandardModal>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import dayjs, { Dayjs } from 'dayjs';
import { message } from 'ant-design-vue';
import request from '@/request';
import { CoachProfile, PageResult, PrivateSchedule, VenueResource } from '@/types';
import { usePageStyle } from '@/hooks/usePageStyle';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardModal from '@/components/common/StandardModal.vue';
import StandardTable from '@/components/common/StandardTable.vue';
import WorkspacePage from '@/components/common/WorkspacePage.vue';
import TableSearchToolbar from '@/components/common/TableSearchToolbar.vue';
import AdvancedFilterModal from '@/components/common/AdvancedFilterModal.vue';
import ConfiguredFormLayout from '@/components/common/ConfiguredFormLayout.vue';
import { useConfiguredTablePage } from '@/composables/useConfiguredTablePage';
import { useAdaptiveTablePage } from '@/composables/useAdaptiveTablePage';
import { getTableColumnKey, sortColumnsByPriority } from '@/utils/tableColumns';

const { currentStyle, loadMenuConfig } = usePageStyle();
const coachOptions = ref<CoachProfile[]>([]);
const venueOptions = ref<VenueResource[]>([]);
const modalVisible = ref(false);
const modalTitle = ref('新增私教时段');
const startAt = ref<Dayjs>();
const endAt = ref<Dayjs>();

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
  primaryFormFields,
  ensureConfig,
  loadData,
  handleSearch,
  handleReset,
  handleTableChange,
  applyAdvancedFilters,
  buildColumns,
  isFieldVisible,
} = useConfiguredTablePage<PrivateSchedule>({
  routePath: '/private-schedule',
});

const {
  tableSectionRef,
  tableBodyHeight,
  applyAdaptiveTableLayout,
  handleAdaptiveTableChange,
  startAdaptiveTableLayout,
} = useAdaptiveTablePage({
  pagination,
  loadData,
  minPageSize: 4,
  maxPageSize: 50,
  maxAutoPageSize: 10,
  minBodyHeight: 220,
  fallbackRowHeight: 72,
  headerHeight: 44,
  footerHeight: 88,
  bottomGutter: 18,
  bodyReserve: 8,
});

const formState = reactive<PrivateSchedule>({
  id: undefined,
  coachId: undefined,
  venueId: undefined,
  startTime: '',
  endTime: '',
  capacity: 1,
  bookedCount: 0,
  description: '',
  status: 'OPEN'
});

const coachNameMap = computed<Record<number, string>>(() => {
  const map: Record<number, string> = {};
  coachOptions.value.forEach(item => {
    if (item.id) map[item.id] = item.name;
  });
  return map;
});

const venueNameMap = computed<Record<number, string>>(() => {
  const map: Record<number, string> = {};
  venueOptions.value.forEach(item => {
    if (item.id) map[item.id] = item.name;
  });
  return map;
});

const baseColumns = [
  { title: '教练', dataIndex: 'coachId', key: 'coachId', width: 120 },
  { title: '日期', key: 'scheduleDate', configKey: 'period', width: 138 },
  { title: '时间', key: 'timeRange', configKey: 'period', width: 136 },
  { title: '场馆', dataIndex: 'venueId', key: 'venueId', width: 140 },
  { title: '预约', dataIndex: 'bookedCount', key: 'bookedCount', width: 104 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 116 },
  { title: '可约人数', dataIndex: 'capacity', key: 'capacity', width: 104 },
  { title: '备注', dataIndex: 'description', key: 'description', width: 220 },
  { title: '操作', key: 'action', width: 112 }
];
const privateColumnPriority = ['coachId', 'scheduleDate', 'timeRange', 'venueId', 'bookedCount', 'status', 'capacity', 'description', 'action'];
const privateColumnWidths: Record<string, number> = {
  coachId: 120,
  scheduleDate: 138,
  timeRange: 136,
  venueId: 140,
  bookedCount: 104,
  status: 116,
  capacity: 104,
  description: 220,
  action: 112,
};

const columns = computed(() => sortColumnsByPriority(buildColumns(baseColumns), privateColumnPriority).map((column) => ({
  ...column,
  width: privateColumnWidths[getTableColumnKey(column)] ?? column.width,
})));
const tableScroll = computed(() => ({
  y: tableBodyHeight.value,
}));

const formatDateTime = (value?: string) => {
  if (!value) {
    return '-';
  }

  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm') : value.replace('T', ' ');
};

const formatScheduleDate = (value?: string) => {
  if (!value) {
    return '-';
  }

  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD') : value.replace('T', ' ').slice(0, 10);
};

const formatClockRange = (start?: string, end?: string) => {
  if (!start && !end) {
    return '-';
  }

  const startAtValue = start ? dayjs(start) : null;
  const endAtValue = end ? dayjs(end) : null;

  if (startAtValue?.isValid() && endAtValue?.isValid() && startAtValue.isSame(endAtValue, 'day')) {
    return `${startAtValue.format('HH:mm')} - ${endAtValue.format('HH:mm')}`;
  }

  return `${formatDateTime(start)} - ${formatDateTime(end)}`;
};

const loadOptions = async () => {
  const [coachRes, venueRes] = await Promise.all([
    request.get('/admin/coaches/page', { params: { pageNum: 1, pageSize: 200 } }),
    request.get('/admin/venues/page', { params: { pageNum: 1, pageSize: 200 } })
  ]);
  if (coachRes.code === 200) {
    coachOptions.value = (coachRes.data as PageResult<CoachProfile>).records;
  }
  if (venueRes.code === 200) {
    venueOptions.value = (venueRes.data as PageResult<VenueResource>).records;
  }
};

const resetForm = () => {
  Object.assign(formState, {
    id: undefined,
    coachId: undefined,
    venueId: undefined,
    startTime: '',
    endTime: '',
    capacity: 1,
    bookedCount: 0,
    description: '',
    status: 'OPEN'
  });
  startAt.value = undefined;
  endAt.value = undefined;
};

const openAdd = () => {
  modalTitle.value = '新增私教时段';
  resetForm();
  modalVisible.value = true;
};

const openEdit = (record: PrivateSchedule) => {
  modalTitle.value = '编辑私教时段';
  Object.assign(formState, record);
  startAt.value = record.startTime ? dayjs(record.startTime) : undefined;
  endAt.value = record.endTime ? dayjs(record.endTime) : undefined;
  modalVisible.value = true;
};

const handleSubmit = async () => {
  formState.startTime = startAt.value ? startAt.value.format('YYYY-MM-DD HH:mm:ss') : '';
  formState.endTime = endAt.value ? endAt.value.format('YYYY-MM-DD HH:mm:ss') : '';
  const res = await request.post('/admin/private-schedules', formState);
  if (res.code === 200) {
    message.success('私教时段保存成功');
    modalVisible.value = false;
    loadData();
  } else {
    message.error(res.msg || '保存失败');
  }
};

const handleDelete = async (id: number) => {
  const res = await request.delete(`/admin/private-schedules/${id}`);
  if (res.code === 200) {
    message.success('删除成功');
    loadData();
  }
};

const handlePrivateTableChange = (pag: { current: number; pageSize: number }) => {
  return handleAdaptiveTableChange(pag, handleTableChange);
};

onMounted(async () => {
  loadMenuConfig();
  await ensureConfig();
  await startAdaptiveTableLayout(false);
  await loadOptions();
  await loadData();
  await applyAdaptiveTableLayout(true);
});
</script>

<style scoped>
.private-schedule-page {
  --private-table-header-height: 44px;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.private-schedule-page :deep(.workspace-body-card),
.private-schedule-page :deep(.workspace-body--card),
.private-schedule-page :deep(.workspace-subsection) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.private-schedule-page :deep(.workspace-body-card) {
  overflow: hidden;
}

.private-schedule-page :deep(.workspace-subsection > .table-surface--menu-list) {
  flex: 1 1 auto;
}

.private-schedule-page :deep(.table-surface--menu-list--custom-pagination .std-table-main),
.private-schedule-page :deep(.table-surface--menu-list--custom-pagination .std-table-main > .ant-table-wrapper) {
  flex: 0 1 auto;
  min-height: 0;
}

.private-schedule-page :deep(.ant-table-wrapper) {
  min-height: 0;
}

.private-schedule-page :deep(.ant-table table) {
  table-layout: fixed !important;
}

.private-schedule-page :deep(.ant-table-thead > tr > th) {
  height: var(--private-table-header-height) !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  vertical-align: middle !important;
}

.private-schedule-page :deep(.ant-table-tbody > tr.ant-table-row > td) {
  padding-top: 12px !important;
  padding-bottom: 12px !important;
  vertical-align: middle !important;
}

.private-note-cell {
  display: inline-block;
  max-width: 210px;
  overflow: hidden;
  color: var(--mono-text);
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.private-row-actions {
  white-space: nowrap;
}
</style>
