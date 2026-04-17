<template>
  <WorkspacePage title="教练信息管理" variant="menu-list">
    <template #actions>
      <StandardButton type="add" icon="plus" @click="openAdd">新增教练</StandardButton>
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
        <template #bodyCell="{ column, record }: { column: any; record: CoachProfile }">
          <template v-if="column.key === 'gender'">
            <span>{{ record.gender === 0 ? '女' : '男' }}</span>
          </template>
          <template v-if="column.key === 'status'">
            <span :class="['status-pill', record.status === 1 ? 'status-pill--strong' : 'status-pill--muted']">
              {{ record.status === 1 ? '在职' : '离职' }}
            </span>
          </template>
          <template v-if="column.key === 'hourlyPrice'">
            ¥ {{ Number(record.hourlyPrice || 0).toFixed(2) }}
          </template>
          <template v-if="column.key === 'rating'">
            <span>{{ Number(record.rating || 0).toFixed(1) }}</span>
          </template>
          <template v-if="column.key === 'intro'">
            <span>{{ record.intro || '暂无简介' }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <StandardButton type="link" size="sm" class="table-action-link" @click="openEdit(record)">编辑</StandardButton>
              <a-popconfirm title="确定删除该教练吗？" @confirm="handleDelete(record.id!)">
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
            <a-form-item label="姓名">
              <a-input v-model:value="formState.name" />
            </a-form-item>
          </template>
          <template #field-phone>
            <a-form-item label="联系电话">
              <a-input v-model:value="formState.phone" />
            </a-form-item>
          </template>
          <template #field-gender>
            <a-form-item label="性别">
              <a-select v-model:value="formState.gender">
                <a-select-option :value="1">男</a-select-option>
                <a-select-option :value="0">女</a-select-option>
              </a-select>
            </a-form-item>
          </template>
          <template #field-age>
            <a-form-item label="年龄">
              <a-input-number v-model:value="formState.age" :min="18" :max="60" style="width: 100%" />
            </a-form-item>
          </template>
          <template #field-status>
            <a-form-item label="状态">
              <a-select v-model:value="formState.status">
                <a-select-option :value="1">在职</a-select-option>
                <a-select-option :value="0">离职</a-select-option>
              </a-select>
            </a-form-item>
          </template>
          <template #field-specialization>
            <a-form-item label="擅长方向">
              <a-input v-model:value="formState.specialization" />
            </a-form-item>
          </template>
          <template #field-hourlyPrice>
            <a-form-item label="私教时薪">
              <a-input-number v-model:value="formState.hourlyPrice" :min="0" :step="10" style="width: 100%" />
            </a-form-item>
          </template>
          <template #field-rating>
            <a-form-item label="评分">
              <a-input-number v-model:value="formState.rating" :min="0" :max="5" :step="0.1" style="width: 100%" />
            </a-form-item>
          </template>
          <template #field-entryDate>
            <a-form-item label="入职日期">
              <a-date-picker v-model:value="formState.entryDate" value-format="YYYY-MM-DD" style="width: 100%" />
            </a-form-item>
          </template>
          <template #field-intro>
            <a-form-item label="教练简介">
              <a-textarea v-model:value="formState.intro" :rows="4" />
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
import { CoachProfile } from '@/types';
import { usePageStyle } from '@/hooks/usePageStyle';
import ConfiguredFormLayout from '@/components/common/ConfiguredFormLayout.vue';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardModal from '@/components/common/StandardModal.vue';
import StandardTable from '@/components/common/StandardTable.vue';
import WorkspacePage from '@/components/common/WorkspacePage.vue';
import TableSearchToolbar from '@/components/common/TableSearchToolbar.vue';
import AdvancedFilterModal from '@/components/common/AdvancedFilterModal.vue';
import { useConfiguredTablePage } from '@/composables/useConfiguredTablePage';

const { currentStyle, loadMenuConfig } = usePageStyle();
const modalVisible = ref(false);
const modalTitle = ref('新增教练');

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
} = useConfiguredTablePage<CoachProfile>({
  routePath: '/coach',
});

const formState = reactive<CoachProfile>({
  id: undefined,
  name: '',
  gender: 1,
  age: 25,
  phone: '',
  specialization: '',
  intro: '',
  status: 1,
  hourlyPrice: 299,
  rating: 4.8
});

const baseColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '性别', dataIndex: 'gender', key: 'gender', width: 90 },
  { title: '年龄', dataIndex: 'age', key: 'age', width: 90 },
  { title: '电话', dataIndex: 'phone', key: 'phone' },
  { title: '专长', dataIndex: 'specialization', key: 'specialization' },
  { title: '入职日期', dataIndex: 'entryDate', key: 'entryDate', width: 140 },
  { title: '时薪', dataIndex: 'hourlyPrice', key: 'hourlyPrice', width: 120 },
  { title: '评分', dataIndex: 'rating', key: 'rating', width: 150 },
  { title: '简介', dataIndex: 'intro', key: 'intro', width: 220 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 140 }
];
const columns = computed(() => buildColumns(baseColumns));

const resetForm = () => {
  Object.assign(formState, {
    id: undefined,
    name: '',
    gender: 1,
    age: 25,
    phone: '',
    specialization: '',
    intro: '',
    status: 1,
    hourlyPrice: 299,
    rating: 4.8
  });
};

const openAdd = () => {
  modalTitle.value = '新增教练';
  resetForm();
  modalVisible.value = true;
};

const openEdit = (record: CoachProfile) => {
  modalTitle.value = '编辑教练';
  Object.assign(formState, record);
  modalVisible.value = true;
};

const handleSubmit = async () => {
  const res = await request.post('/admin/coaches', formState);
  if (res.code === 200) {
    message.success('教练保存成功');
    modalVisible.value = false;
    loadData();
  } else {
    message.error(res.msg || '保存失败');
  }
};

const handleDelete = async (id: number) => {
  const res = await request.delete(`/admin/coaches/${id}`);
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

<style scoped></style>
