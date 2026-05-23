<template>
  <WorkspacePage title="器材信息管理" variant="menu-list">
    <template #actions>
      <StandardButton type="add" icon="plus" @click="handleAdd">新增器材</StandardButton>
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
        <template #bodyCell="{ column, record }: { column: any, record: Equipment }">
            <template v-if="column.key === 'venueId'">
                <span>{{ venueNameMap[record.venueId || 0] || '未绑定场馆' }}</span>
            </template>
            <template v-else-if="column.key === 'quantity'">
                <span>{{ record.quantity ?? 0 }}</span>
            </template>
            <template v-if="column.key === 'status'">
                <span :class="['status-pill', equipmentStatusTone(record.status)]">
                    {{ equipmentStatusLabel(record.status) }}
                </span>
            </template>
            <template v-if="column.key === 'action'">
                <StandardButton type="link" size="sm" class="table-action-link" @click="handleEdit(record)">编辑</StandardButton>
                <a-popconfirm title="确定删除吗?" @confirm="handleDelete(record.id)">
                    <StandardButton type="link" size="sm" danger class="table-action-link">删除</StandardButton>
                </a-popconfirm>
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

    <StandardModal
      v-model:visible="modalVisible"
      :title="modalTitle"
      @ok="handleModalOk"
    >
      <a-form :model="formState" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" class="workspace-modal-form">
        <ConfiguredFormLayout :fields="primaryFormFields">
          <template #field-name>
            <a-form-item label="器材名称">
              <StandardInput v-model:value="formState.name" variant="grey" class="modal-input-unified" />
            </a-form-item>
          </template>
          <template #field-description>
            <a-form-item label="描述">
              <StandardInput v-model:value="formState.description" variant="grey" class="modal-input-unified" />
            </a-form-item>
          </template>
          <template #field-venueId>
            <a-form-item label="所属场馆">
              <a-select v-model:value="formState.venueId" class="modal-input-unified" placeholder="请选择场馆">
                <a-select-option v-for="item in venueOptions" :key="item.id" :value="item.id">{{ item.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </template>
          <template #field-quantity>
            <a-form-item label="数量">
              <a-input-number v-model:value="formState.quantity" :min="0" class="modal-input-unified" />
            </a-form-item>
          </template>
          <template #field-status>
            <a-form-item label="状态">
              <a-select v-model:value="formState.status" class="modal-input-unified" placeholder="请选择状态">
                <a-select-option value="AVAILABLE">可用</a-select-option>
                <a-select-option value="IN_USE">使用中</a-select-option>
                <a-select-option value="MAINTENANCE">维护中</a-select-option>
              </a-select>
            </a-form-item>
          </template>
        </ConfiguredFormLayout>
      </a-form>
    </StandardModal>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import request from '@/request';
import { Equipment, PageResult, VenueResource } from '@/types';
import { usePageStyle } from '@/hooks/usePageStyle';

// Shared Components
import StandardInput from '@/components/common/StandardInput.vue';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardTable from '@/components/common/StandardTable.vue';
import StandardModal from '@/components/common/StandardModal.vue';
import WorkspacePage from '@/components/common/WorkspacePage.vue';
import TableSearchToolbar from '@/components/common/TableSearchToolbar.vue';
import AdvancedFilterModal from '@/components/common/AdvancedFilterModal.vue';
import ConfiguredFormLayout from '@/components/common/ConfiguredFormLayout.vue';
import { useConfiguredTablePage } from '@/composables/useConfiguredTablePage';
import { sortColumnsByPriority } from '@/utils/tableColumns';

// Page Style
const { currentStyle, loadMenuConfig } = usePageStyle();
const venueOptions = ref<VenueResource[]>([]);

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
} = useConfiguredTablePage<Equipment>({
  routePath: '/equipment',
});

const modalVisible = ref(false);
const modalTitle = ref('新增器材');
const formState = reactive<Equipment>({
    id: 0,
    name: '',
    description: '',
    status: 'AVAILABLE',
    venueId: 0,
    quantity: 1,
});

const venueNameMap = computed<Record<number, string>>(() => {
    const map: Record<number, string> = {};
    venueOptions.value.forEach((item) => {
        if (item.id) {
            map[item.id] = item.name;
        }
    });
    return map;
});

const baseColumns = [
  { title: '器材名称', dataIndex: 'name', key: 'name' },
  { title: '所属场馆', dataIndex: 'venueId', key: 'venueId', width: 160 },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 100 },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '操作', key: 'action', width: 150 },
];
const equipmentColumnPriority = ['name', 'venueId', 'status', 'quantity', 'description', 'id', 'action'];
const columns = computed(() => sortColumnsByPriority(buildColumns(baseColumns), equipmentColumnPriority));

const loadOptions = async () => {
    const venueRes = await request.get('/admin/venues/page', { params: { pageNum: 1, pageSize: 200 } });
    if (venueRes.code === 200) {
        venueOptions.value = (venueRes.data as PageResult<VenueResource>).records;
    }
};

const handleAdd = () => {
    modalTitle.value = '新增器材';
    formState.id = 0;
    formState.name = '';
    formState.description = '';
    formState.status = 'AVAILABLE';
    formState.venueId = 0;
    formState.quantity = 1;
    modalVisible.value = true;
}

const handleEdit = (record: Equipment) => {
    modalTitle.value = '编辑器材';
    Object.assign(formState, record);
    modalVisible.value = true;
}

const handleModalOk = () => {
    request.post("/equipment", formState).then((res: any) => {
        if(res.code === 200) {
            message.success("保存成功");
            modalVisible.value = false;
            loadData();
        }
    });
}

const equipmentStatusLabel = (status: string) => {
  if (status === 'IN_USE') return '使用中';
  if (status === 'MAINTENANCE') return '维护中';
  return '可用';
};

const equipmentStatusTone = (status: string) => {
  if (status === 'IN_USE') return 'status-pill--muted';
  if (status === 'MAINTENANCE') return 'status-pill--soft';
  return 'status-pill--strong';
};

const handleDelete = (id: number) => {
     request.delete("/equipment/" + id).then((res: any) => {
        if(res.code === 200) {
            message.success("删除成功");
            loadData();
        }
    });
}

onMounted(async () => {
  loadMenuConfig();
  await ensureConfig();
  await loadOptions();
  loadData();
});
</script>

<style scoped></style>
