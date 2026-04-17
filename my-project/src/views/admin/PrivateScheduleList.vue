<template>
  <WorkspacePage title="私教排班管理" variant="menu-list">
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
        <template #bodyCell="{ column, record }: { column: any; record: PrivateSchedule }">
          <template v-if="column.key === 'coachId'">
            {{ coachNameMap[record.coachId || 0] || '-' }}
          </template>
          <template v-if="column.key === 'venueId'">
            {{ venueNameMap[record.venueId || 0] || '-' }}
          </template>
          <template v-if="column.key === 'period'">
            {{ record.startTime }}<br />{{ record.endTime }}
          </template>
          <template v-if="column.key === 'bookedCount'">
            {{ record.bookedCount || 0 }} / {{ record.capacity || 1 }}
          </template>
          <template v-if="column.key === 'status'">
            <span :class="['status-pill', record.status === 'OPEN' ? 'status-pill--strong' : 'status-pill--soft']">
              {{ record.status === 'CLOSED' ? '已关闭' : '开放预约' }}
            </span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
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
  { title: '教练', dataIndex: 'coachId', key: 'coachId', width: 140 },
  { title: '场馆', dataIndex: 'venueId', key: 'venueId', width: 160 },
  { title: '时段', key: 'period', configKey: 'period', width: 220 },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 180 },
  { title: '结束时间', dataIndex: 'endTime', key: 'endTime', width: 180 },
  { title: '可约人数', dataIndex: 'capacity', key: 'capacity', width: 120 },
  { title: '预约情况', dataIndex: 'bookedCount', key: 'bookedCount', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '备注', dataIndex: 'description', key: 'description' },
  { title: '操作', key: 'action', width: 140 }
];
const columns = computed(() => buildColumns(baseColumns));

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

onMounted(async () => {
  loadMenuConfig();
  await ensureConfig();
  await loadOptions();
  loadData();
});
</script>

<style scoped></style>
