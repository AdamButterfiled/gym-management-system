<template>
  <WorkspacePage class="course-list-page adaptive-table-page" title="团课排期管理" variant="menu-list">
    <template #actions>
      <StandardButton type="add" icon="plus" @click="openAdd">发布团课</StandardButton>
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
        @change="handleCourseTableChange"
      >
        <template #bodyCell="{ column, record }: { column: any; record: CourseSchedule }">
          <template v-if="column.key === 'coachId'">
            {{ coachNameMap[record.coachId || 0] || '-' }}
          </template>
          <template v-if="column.key === 'venueId'">
            {{ venueNameMap[record.venueId || 0] || '-' }}
          </template>
          <template v-if="column.key === 'period'">
            {{ formatTimeRange(record.startTime, record.endTime) }}
          </template>
          <template v-if="column.key === 'bookedCount'">
            {{ record.bookedCount || 0 }} / {{ record.capacity }}
          </template>
          <template v-if="column.key === 'price'">
            <div>常规 ¥{{ Number(record.normalPrice || 0).toFixed(2) }}</div>
            <div v-if="record.flashSale === 1" class="muted">秒杀 ¥{{ Number(record.flashSalePrice || 0).toFixed(2) }}</div>
          </template>
          <template v-if="column.key === 'normalPrice'">
            ¥{{ Number(record.normalPrice || 0).toFixed(2) }}
          </template>
          <template v-if="column.key === 'flashSale'">
            {{ record.flashSale === 1 ? '开启' : '关闭' }}
          </template>
          <template v-if="column.key === 'flashSalePrice'">
            {{ record.flashSale === 1 ? `¥${Number(record.flashSalePrice || 0).toFixed(2)}` : '--' }}
          </template>
          <template v-if="column.key === 'description'">
            <span class="muted">{{ record.description || '暂无说明' }}</span>
          </template>
          <template v-if="column.key === 'status'">
            <span :class="['status-pill', record.status === 'PUBLISHED' ? 'status-pill--strong' : 'status-pill--muted']">
              {{ record.status === 'CLOSED' ? '已关闭' : '已发布' }}
            </span>
          </template>
        </template>

        <template #rightRail>
          <aside :class="['course-table-action-rail', isDataScrolledX && 'course-table-action-rail--separated']" aria-label="表格操作列">
            <div class="course-table-action-head">操作</div>
            <div
              ref="actionBodyRef"
              class="course-table-action-body"
              :style="{ height: `${tableBodyHeight}px`, maxHeight: `${tableBodyHeight}px` }"
            >
              <div
                v-for="(record, index) in tableData"
                :key="record.id"
                class="course-table-action-row"
                :style="getActionRowStyle(index)"
              >
                <a-space>
                  <StandardButton type="link" size="sm" class="table-action-link" @click="openEdit(record)">编辑</StandardButton>
                  <a-popconfirm title="确定删除该团课排期吗？" @confirm="handleDelete(record.id!)">
                    <StandardButton type="link" size="sm" danger class="table-action-link">删除</StandardButton>
                  </a-popconfirm>
                </a-space>
              </div>
            </div>
          </aside>
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

    <StandardModal v-model:visible="modalVisible" :title="modalTitle" width="820px" @ok="handleSubmit">
      <a-form
        :model="formState"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
        class="workspace-modal-form"
      >
        <ConfiguredFormLayout :fields="primaryFormFields">
          <template #field-name>
            <a-form-item label="课程名称">
              <a-input v-model:value="formState.name" />
            </a-form-item>
          </template>
          <template #field-status>
            <a-form-item label="排期状态">
              <a-select v-model:value="formState.status">
                <a-select-option value="PUBLISHED">PUBLISHED</a-select-option>
                <a-select-option value="CLOSED">CLOSED</a-select-option>
              </a-select>
            </a-form-item>
          </template>
          <template #field-coachId>
            <a-form-item label="教练">
              <a-select v-model:value="formState.coachId" show-search>
                <a-select-option v-for="item in coachOptions" :key="item.id" :value="item.id">{{ item.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </template>
          <template #field-venueId>
            <a-form-item label="场馆">
              <a-select v-model:value="formState.venueId" show-search>
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
            <a-form-item label="容量">
              <a-input-number v-model:value="formState.capacity" :min="1" style="width: 100%" />
            </a-form-item>
          </template>
          <template #field-normalPrice>
            <a-form-item label="常规价格">
              <a-input-number v-model:value="formState.normalPrice" :min="0" :step="10" style="width: 100%" />
            </a-form-item>
          </template>
          <template #field-flashSale>
            <a-form-item label="秒杀">
              <a-switch v-model:checked="flashChecked" />
            </a-form-item>
          </template>
          <template #field-flashSalePrice>
            <a-form-item v-if="flashChecked" label="秒杀价格">
              <a-input-number v-model:value="formState.flashSalePrice" :min="0" :step="10" style="width: 100%" />
            </a-form-item>
          </template>
          <template #field-description>
            <a-form-item label="课程说明">
              <a-textarea v-model:value="formState.description" :rows="4" />
            </a-form-item>
          </template>
        </ConfiguredFormLayout>
      </a-form>
    </StandardModal>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import dayjs, { Dayjs } from 'dayjs';
import { message } from 'ant-design-vue';
import request from '@/request';
import { CoachProfile, CourseSchedule, PageResult, VenueResource } from '@/types';
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
import { sortColumnsByPriority } from '@/utils/tableColumns';

const { currentStyle, loadMenuConfig } = usePageStyle();
const coachOptions = ref<CoachProfile[]>([]);
const venueOptions = ref<VenueResource[]>([]);
const modalVisible = ref(false);
const modalTitle = ref('发布团课');
const startAt = ref<Dayjs>();
const endAt = ref<Dayjs>();
const flashChecked = ref(false);
const actionBodyRef = ref<HTMLElement | null>(null);
const actionRowHeights = ref<number[]>([]);
const isDataScrolledX = ref(false);

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
} = useConfiguredTablePage<CourseSchedule>({
  routePath: '/course',
});

const {
  tableSectionRef,
  tableBodyHeight,
  measuredRowHeight,
  getTableBody,
  measureRenderedRowHeight,
  applyAdaptiveTableLayout,
  handleAdaptiveTableChange,
  startAdaptiveTableLayout,
} = useAdaptiveTablePage({
  pagination,
  loadData,
  tableBodySelector: '.ant-table-body',
  rowSelector: '.ant-table-tbody > tr.ant-table-row:not(.ant-table-measure-row)',
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

let tableBodyEl: HTMLElement | null = null;
let tableHorizontalEl: HTMLElement | null = null;
let scrollSyncing = false;

const syncScrollTop = (target: HTMLElement | null, scrollTop: number) => {
  if (!target || target.scrollTop === scrollTop) {
    return;
  }

  scrollSyncing = true;
  target.scrollTop = scrollTop;

  if (typeof window !== 'undefined') {
    window.requestAnimationFrame(() => {
      scrollSyncing = false;
    });
  } else {
    scrollSyncing = false;
  }
};

const handleDataPaneScroll = (event: Event) => {
  isDataScrolledX.value = (event.currentTarget as HTMLElement).scrollLeft > 2;

  if (scrollSyncing) {
    return;
  }

  syncScrollTop(actionBodyRef.value, (event.currentTarget as HTMLElement).scrollTop);
};

const handleHorizontalPaneScroll = (event: Event) => {
  isDataScrolledX.value = (event.currentTarget as HTMLElement).scrollLeft > 2;
};

const handleActionRailWheel = (event: WheelEvent) => {
  if (!tableBodyEl || Math.abs(event.deltaY) <= Math.abs(event.deltaX)) {
    return;
  }

  event.preventDefault();
  tableBodyEl.scrollTop += event.deltaY;
  syncScrollTop(actionBodyRef.value, tableBodyEl.scrollTop);
};

const unbindTableScrollSync = () => {
  tableBodyEl?.removeEventListener('scroll', handleDataPaneScroll);
  tableHorizontalEl?.removeEventListener('scroll', handleHorizontalPaneScroll);
  actionBodyRef.value?.removeEventListener('wheel', handleActionRailWheel);
  tableBodyEl = null;
  tableHorizontalEl = null;
};

const syncActionRowHeights = () => {
  const rows = Array.from(
    tableSectionRef.value?.querySelectorAll<HTMLElement>('.ant-table-tbody > tr.ant-table-row:not(.ant-table-measure-row)') || [],
  );

  actionRowHeights.value = rows.map((row) => Math.ceil(row.getBoundingClientRect().height || measuredRowHeight.value));
  measureRenderedRowHeight();
};

const getActionRowStyle = (index: number) => {
  const height = actionRowHeights.value[index] || measuredRowHeight.value;

  return {
    height: `${height}px`,
    minHeight: `${height}px`,
  };
};

const bindTableScrollSync = async () => {
  await nextTick();
  unbindTableScrollSync();

  tableBodyEl = getTableBody();
  const tableContentEl = tableSectionRef.value?.querySelector<HTMLElement>('.ant-table-content') || null;
  tableHorizontalEl = tableContentEl && tableContentEl !== tableBodyEl ? tableContentEl : null;
  const actionBody = actionBodyRef.value;
  syncActionRowHeights();

  if (!tableBodyEl || !actionBody) {
    return;
  }

  tableBodyEl.addEventListener('scroll', handleDataPaneScroll, { passive: true });
  tableHorizontalEl?.addEventListener('scroll', handleHorizontalPaneScroll, { passive: true });
  actionBody.addEventListener('wheel', handleActionRailWheel, { passive: false });
  isDataScrolledX.value = tableBodyEl.scrollLeft > 2 || Boolean(tableHorizontalEl && tableHorizontalEl.scrollLeft > 2);
  actionBody.scrollTop = tableBodyEl.scrollTop;
};

const formState = reactive<CourseSchedule>({
  id: undefined,
  name: '',
  coachId: undefined,
  venueId: undefined,
  startTime: '',
  endTime: '',
  capacity: 20,
  bookedCount: 0,
  normalPrice: 0,
  flashSale: 0,
  flashSalePrice: 0,
  description: '',
  status: 'PUBLISHED'
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
  { title: '课程名称', dataIndex: 'name', key: 'name', width: 180 },
  { title: '教练', dataIndex: 'coachId', key: 'coachId', width: 120 },
  { title: '价格', key: 'price', configKey: 'price', width: 150 },
  { title: '时间', key: 'period', configKey: 'period', width: 260 },
  { title: '场馆', dataIndex: 'venueId', key: 'venueId', width: 140 },
  { title: '人数', dataIndex: 'bookedCount', key: 'bookedCount', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '容量', dataIndex: 'capacity', key: 'capacity', width: 100 },
  { title: '说明', dataIndex: 'description', key: 'description', width: 260 }
];
const courseColumnPriority = ['name', 'coachId', 'price', 'period', 'venueId', 'bookedCount', 'status', 'capacity', 'description'];

const columns = computed(() => sortColumnsByPriority(buildColumns(baseColumns), courseColumnPriority));
const tableScroll = computed(() => ({
  x: Math.max(
    1480,
    columns.value.reduce((total, column) => {
      const width = Number((column as { width?: number | string }).width);
      return total + (Number.isFinite(width) && width > 0 ? width : 160);
    }, 0),
  ),
  y: tableBodyHeight.value,
}));

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
    name: '',
    coachId: undefined,
    venueId: undefined,
    startTime: '',
    endTime: '',
    capacity: 20,
    bookedCount: 0,
    normalPrice: 0,
    flashSale: 0,
    flashSalePrice: 0,
    description: '',
    status: 'PUBLISHED'
  });
  flashChecked.value = false;
  startAt.value = undefined;
  endAt.value = undefined;
};

const openAdd = () => {
  modalTitle.value = '发布团课';
  resetForm();
  modalVisible.value = true;
};

const openEdit = (record: CourseSchedule) => {
  modalTitle.value = '编辑团课';
  Object.assign(formState, record);
  flashChecked.value = record.flashSale === 1;
  startAt.value = record.startTime ? dayjs(record.startTime) : undefined;
  endAt.value = record.endTime ? dayjs(record.endTime) : undefined;
  modalVisible.value = true;
};

const handleSubmit = async () => {
  formState.startTime = startAt.value ? startAt.value.format('YYYY-MM-DD HH:mm:ss') : '';
  formState.endTime = endAt.value ? endAt.value.format('YYYY-MM-DD HH:mm:ss') : '';
  formState.flashSale = flashChecked.value ? 1 : 0;
  const res = await request.post('/admin/course-schedules', formState);
  if (res.code === 200) {
    message.success('团课排期保存成功');
    modalVisible.value = false;
    loadData();
  } else {
    message.error(res.msg || '保存失败');
  }
};

const handleDelete = async (id: number) => {
  const res = await request.delete(`/admin/course-schedules/${id}`);
  if (res.code === 200) {
    message.success('删除成功');
    loadData();
  }
};

const handleCourseTableChange = (pag: { current: number; pageSize: number }) => {
  return handleAdaptiveTableChange(pag, handleTableChange);
};

watch(
  [tableData, tableBodyHeight],
  () => {
    void bindTableScrollSync();
  },
  { flush: 'post' },
);

onMounted(async () => {
  loadMenuConfig();
  await ensureConfig();
  await startAdaptiveTableLayout(false);
  await loadOptions();
  await loadData();
  await applyAdaptiveTableLayout(true);
  await bindTableScrollSync();
});

onBeforeUnmount(() => {
  unbindTableScrollSync();
});
</script>

<style scoped>
.muted {
  color: #5f6368;
}

.course-list-page {
  --course-table-header-height: 44px;
  --course-table-action-width: 132px;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.course-list-page :deep(.workspace-body-card),
.course-list-page :deep(.workspace-body--card),
.course-list-page :deep(.workspace-subsection) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.course-list-page :deep(.workspace-body-card) {
  overflow: hidden;
}

.course-list-page :deep(.workspace-subsection > .table-surface--menu-list) {
  flex: 1 1 auto;
}

.course-table-action-rail {
  position: sticky;
  right: 0;
  z-index: 2;
  display: flex;
  flex: 0 0 var(--course-table-action-width);
  flex-direction: column;
  width: var(--course-table-action-width);
  min-height: 0;
  background: #ffffff;
  box-shadow: none;
}

.course-table-action-rail--separated {
  box-shadow: -1px 0 0 rgba(15, 23, 42, 0.025);
}

.course-table-action-head {
  display: flex;
  flex: 0 0 var(--course-table-header-height);
  align-items: center;
  justify-content: center;
  height: var(--course-table-header-height);
  color: var(--mono-text-secondary);
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  background: #ffffff;
}

.course-table-action-body {
  flex: 0 0 auto;
  overflow-x: hidden;
  overflow-y: hidden;
  overscroll-behavior: contain;
  background: #ffffff;
  scrollbar-width: none;
}

.course-table-action-body::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.course-table-action-row {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  background: #ffffff;
}

.course-table-action-row :deep(.ant-space) {
  flex-wrap: nowrap;
  white-space: nowrap;
}

.course-list-page :deep(.ant-table-wrapper) {
  min-height: 0;
}

.course-list-page :deep(.ant-table-thead > tr > th) {
  height: var(--course-table-header-height) !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  vertical-align: middle !important;
}

.course-list-page :deep(.ant-table-tbody > tr.ant-table-row > td) {
  padding-top: 12px !important;
  padding-bottom: 12px !important;
  vertical-align: middle !important;
}

.course-list-page :deep(.ant-table-content) {
  overflow-x: auto !important;
}

.course-list-page :deep(.ant-table-body) {
  overflow-y: auto !important;
}

</style>

<style>
html.dark .course-list-page .muted {
  color: var(--mono-text-secondary);
}

html.dark .course-list-page .course-table-action-rail,
html.dark .course-list-page .course-table-action-head,
html.dark .course-list-page .course-table-action-body,
html.dark .course-list-page .course-table-action-row {
  background: #111111 !important;
}

html.dark .course-list-page .course-table-action-rail--separated {
  box-shadow: -1px 0 0 rgba(255, 255, 255, 0.035);
}

html.dark .course-list-page .course-table-action-head {
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
  color: var(--mono-text-secondary) !important;
}
</style>
