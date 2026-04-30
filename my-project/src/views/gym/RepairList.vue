<template>
  <WorkspacePage title="报修工单管理" variant="menu-list">
    <template #actions>
      <div class="repair-toolbar">
        <div class="repair-status-filters">
          <StandardButton
            v-for="option in statusOptions"
            :key="option.value || 'ALL'"
            :type="statusFilter === option.value ? 'primary' : 'default'"
            size="sm"
            @click="applyStatusFilter(option.value)"
          >
            {{ option.label }}
          </StandardButton>
        </div>
        <StandardButton type="default" icon="reload" :loading="loading" @click="loadData">刷新记录</StandardButton>
      </div>
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
        <template #bodyCell="{ column, record }: { column: any, record: any }">
          <template v-if="column.key === 'status'">
            <span :class="['status-pill', repairStatusTone(record.status)]">
              {{ repairStatusLabel(record.status) }}
            </span>
          </template>
          <template v-if="column.key === 'action'">
            <template v-if="nextRepairAction(record.status)">
              <StandardButton
                type="link"
                size="sm"
                class="repair-action-btn"
                @click="handleStatusUpdate(record, nextRepairAction(record.status)?.status || 'FIXED')"
              >
                {{ nextRepairAction(record.status)?.label }}
              </StandardButton>
            </template>
            <span v-else class="repair-complete-text">已完成</span>
          </template>
        </template>
      </StandardTable>
    </section>
  </WorkspacePage>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import request from '@/request';
import { PageResult, Repair } from '@/types';
import { fetchRuntimeFormConfig } from '@/api/formConfig';
import { usePageStyle } from '@/hooks/usePageStyle';
import type { FormPageConfig } from '@/types/formConfig';
import { buildConfiguredColumns } from '@/utils/formConfig';
import { findFormConfigManifestPage } from '@/utils/formConfigManifest';
import { normalizePageConfig, resolveTableTarget } from '@/utils/formConfigDesigner';

// Shared Components
import StandardButton from '@/components/common/StandardButton.vue';
import StandardTable from '@/components/common/StandardTable.vue';
import WorkspacePage from '@/components/common/WorkspacePage.vue';

const tableData = ref<Repair[]>([]);
const runtimeConfig = ref<FormPageConfig | null>(null);
const loading = ref(false);
const statusFilter = ref('');
const statusOptions = [
  { label: '全部工单', value: '' },
  { label: '待维修', value: 'PENDING' },
  { label: '维修中', value: 'PROCESSING' },
  { label: '已维修', value: 'FIXED' },
];

// Page Style
const { currentStyle, loadMenuConfig } = usePageStyle();

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: false,
  showQuickJumper: false,
  showTotal: (total: number) => `共 ${Number(total || 0).toLocaleString('zh-CN')} 项`,
});

const baseColumns = [
  { title: '序号', dataIndex: 'id', key: 'id', width: 80 },
  { title: '报修描述', dataIndex: 'description', key: 'description' },
  { title: '状态', dataIndex: 'status', key: 'status' }, 
  { title: '报修时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '操作', key: 'action', width: 150 },
];
const tableFields = computed(() => resolveTableTarget(runtimeConfig.value)?.fields || []);
const columns = computed(() => buildConfiguredColumns(baseColumns, tableFields.value));

const loadRuntimeConfig = async () => {
  const manifest = findFormConfigManifestPage('/repair');
  const fallback: FormPageConfig = {
    pageKey: manifest?.pageKey || 'repair',
    routePath: '/repair',
    pageTitle: manifest?.pageTitle || '报修工单管理',
    enabled: true,
    quickSearchFields: [],
    defaultFilterLogic: 'AND',
    fields: [],
    targets: [],
  };

  try {
    const config = await fetchRuntimeFormConfig('/repair');
    runtimeConfig.value = normalizePageConfig(config, manifest);
  } catch (error) {
    runtimeConfig.value = normalizePageConfig(fallback, manifest);
  }
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await request.get('/repair/page', {
      params: {
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        status: statusFilter.value,
      },
    });
    if (res.code === 200) {
      const page = res.data as PageResult<Repair>;
      tableData.value = page.records || [];
      pagination.total = page.total || 0;
      return;
    }
    message.error(res.msg || '获取工单失败');
  } finally {
    loading.value = false;
  }
};

const handleTableChange = (pag: any) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  void loadData();
};

const applyStatusFilter = (status: string) => {
  statusFilter.value = status;
  pagination.current = 1;
  void loadData();
};

const nextRepairAction = (status: Repair['status']) => {
  if (status === 'PENDING') {
    return { label: '开始处理', status: 'PROCESSING' as Repair['status'] };
  }
  if (status === 'PROCESSING') {
    return { label: '完成维修', status: 'FIXED' as Repair['status'] };
  }
  return null;
};

const handleStatusUpdate = async (record: Repair, status: Repair['status']) => {
  const res = await request.put(`/repair/${record.id}/status`, { status });
  if (res.code === 200) {
    message.success(status === 'PROCESSING' ? '工单已转为维修中' : '维修已完成');
    await loadData();
    return;
  }
  message.error(res.msg || '更新工单失败');
};

const repairStatusLabel = (status: string) => {
  if (status === 'FIXED') return '已维修';
  if (status === 'PROCESSING') return '维修中';
  return '待维修';
};

const repairStatusTone = (status: string) => {
  if (status === 'FIXED') return 'status-pill--strong';
  if (status === 'PROCESSING') return 'status-pill--muted';
  return 'status-pill--soft';
};

onMounted(() => {
  loadMenuConfig();
  void loadRuntimeConfig();
  loadData();
});
</script>

<style scoped>
.repair-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.repair-status-filters {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.repair-action-btn {
  min-width: 0;
}

.repair-complete-text {
  color: rgba(100, 116, 139, 0.7);
}
</style>
