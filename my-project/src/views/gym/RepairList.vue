<template>
  <WorkspacePage title="报修工单管理">
    <template #meta>
      <span class="page-meta">当前为演示数据</span>
    </template>

    <template #actions>
      <StandardButton type="search" icon="reload" @click="loadData">刷新记录</StandardButton>
    </template>

    <section class="workspace-subsection">
      <StandardTable
        :configStyle="currentStyle"
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
                <StandardButton v-if="record.status !== 'FIXED'" type="link" size="sm" class="repair-action-btn" @click="handleComplete(record)">完成维修</StandardButton>
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
// import request from '@/request'; // Backend missing for Repair
import { Repair } from '@/types';
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

// Page Style
const { currentStyle, loadMenuConfig } = usePageStyle();

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 2,
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

// Mock Data for UI Demonstration (Since Backend API is missing)
const loadData = () => {
    // request.get("/repair/page"...).then(...)
    tableData.value = [
        { id: 1, description: '跑步机显示屏故障', status: 'PENDING', createdAt: '2023-10-20 10:00:00', venueId: 1 },
        { id: 2, description: '哑铃架松动', status: 'FIXED', createdAt: '2023-10-18 14:30:00', venueId: 1 }
    ] as any;
};

const handleTableChange = (pag: any) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  // loadData();
};

const handleComplete = (record: Repair) => {
    record.status = 'FIXED';
    message.success("维修已完成");
    // request.post("/repair", record)...
}

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
 .repair-action-btn {
     min-width: 0;
 }

 .repair-complete-text {
     color: rgba(100, 116, 139, 0.7);
 }
</style>
