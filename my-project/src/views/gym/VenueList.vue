<template>
  <WorkspacePage title="场馆资源管理" variant="menu-list">
    <template #actions>
      <StandardButton type="add" icon="plus" @click="openAdd">新增场馆</StandardButton>
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
        <template #bodyCell="{ column, record }: { column: any; record: VenueResource }">
          <template v-if="column.key === 'pricePerHour'">
            ¥ {{ Number(record.pricePerHour || 0).toFixed(2) }}
          </template>
          <template v-if="column.key === 'status'">
            <span :class="['status-pill', record.status === 1 ? 'status-pill--strong' : 'status-pill--muted']">
              {{ record.status === 1 ? '开放' : '关闭' }}
            </span>
          </template>
          <template v-if="column.key === 'layoutJson'">
            <span class="muted">{{ record.layoutJson ? '已配置布局' : '未配置' }}</span>
          </template>
          <template v-if="column.key === 'description'">
            <span class="muted">{{ record.description || '暂无说明' }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <StandardButton type="link" size="sm" class="table-action-link" @click="openEdit(record)">编辑</StandardButton>
              <a-popconfirm title="确定删除该场馆吗？" @confirm="handleDelete(record.id!)">
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
          <template #field-name>
            <a-form-item label="场馆名称">
              <a-input v-model:value="formState.name" />
            </a-form-item>
          </template>
          <template #field-location>
            <a-form-item label="位置">
              <a-input v-model:value="formState.location" />
            </a-form-item>
          </template>
          <template #field-capacity>
            <a-form-item label="容纳人数">
              <a-input-number v-model:value="formState.capacity" style="width: 100%" />
            </a-form-item>
          </template>
          <template #field-pricePerHour>
            <a-form-item label="每小时价格">
              <a-input-number v-model:value="formState.pricePerHour" :min="0" :step="10" style="width: 100%" />
            </a-form-item>
          </template>
          <template #field-status>
            <a-form-item label="状态">
              <a-select v-model:value="formState.status">
                <a-select-option :value="1">开放</a-select-option>
                <a-select-option :value="0">关闭</a-select-option>
              </a-select>
            </a-form-item>
          </template>
          <template #field-openTime>
            <a-form-item label="开放时间">
              <a-time-picker v-model:value="openTimeValue" value-format="HH:mm:ss" style="width: 100%" />
            </a-form-item>
          </template>
          <template #field-closeTime>
            <a-form-item label="关闭时间">
              <a-time-picker v-model:value="closeTimeValue" value-format="HH:mm:ss" style="width: 100%" />
            </a-form-item>
          </template>
          <template #field-description>
            <a-form-item label="说明">
              <a-textarea v-model:value="formState.description" :rows="3" />
            </a-form-item>
          </template>
          <template #field-layoutJson>
            <a-form-item label="布局 JSON">
              <a-textarea v-model:value="formState.layoutJson" :rows="6" placeholder='[{"id":"A1","x":20,"y":20,"status":"OPEN"}]' />
            </a-form-item>
          </template>
        </ConfiguredFormLayout>
      </a-form>
    </StandardModal>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import request from '@/request';
import { VenueResource } from '@/types';
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
const modalVisible = ref(false);
const modalTitle = ref('新增场馆');
const openTimeValue = ref<string>();
const closeTimeValue = ref<string>();

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
} = useConfiguredTablePage<VenueResource>({
  routePath: '/venue',
});

const formState = reactive<VenueResource>({
  id: undefined,
  name: '',
  location: '',
  capacity: 20,
  description: '',
  status: 1,
  openTime: '06:00:00',
  closeTime: '22:00:00',
  layoutJson: '',
  pricePerHour: 0
});

const baseColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '场馆名称', dataIndex: 'name', key: 'name' },
  { title: '位置', dataIndex: 'location', key: 'location' },
  { title: '容量', dataIndex: 'capacity', key: 'capacity', width: 100 },
  { title: '价格', dataIndex: 'pricePerHour', key: 'pricePerHour', width: 120 },
  { title: '开放时间', dataIndex: 'openTime', key: 'openTime', width: 120 },
  { title: '关闭时间', dataIndex: 'closeTime', key: 'closeTime', width: 120 },
  { title: '说明', dataIndex: 'description', key: 'description', width: 220 },
  { title: '布局', dataIndex: 'layoutJson', key: 'layoutJson', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 140 }
];
const columns = computed(() => buildColumns(baseColumns));

const resetForm = () => {
  Object.assign(formState, {
    id: undefined,
    name: '',
    location: '',
    capacity: 20,
    description: '',
    status: 1,
    openTime: '06:00:00',
    closeTime: '22:00:00',
    layoutJson: '',
    pricePerHour: 0
  });
  openTimeValue.value = '06:00:00';
  closeTimeValue.value = '22:00:00';
};

const openAdd = () => {
  modalTitle.value = '新增场馆';
  resetForm();
  modalVisible.value = true;
};

const openEdit = (record: VenueResource) => {
  modalTitle.value = '编辑场馆';
  Object.assign(formState, record);
  openTimeValue.value = record.openTime;
  closeTimeValue.value = record.closeTime;
  modalVisible.value = true;
};

const handleSubmit = async () => {
  formState.openTime = openTimeValue.value;
  formState.closeTime = closeTimeValue.value;
  const res = await request.post('/admin/venues', formState);
  if (res.code === 200) {
    message.success('场馆保存成功');
    modalVisible.value = false;
    loadData();
  } else {
    message.error(res.msg || '保存失败');
  }
};

const handleDelete = async (id: number) => {
  const res = await request.delete(`/admin/venues/${id}`);
  if (res.code === 200) {
    message.success('删除成功');
    loadData();
  }
};

onMounted(() => {
  loadMenuConfig();
  ensureConfig();
  resetForm();
  loadData();
});
</script>

<style scoped>
.muted {
  color: #5f6368;
}
</style>
