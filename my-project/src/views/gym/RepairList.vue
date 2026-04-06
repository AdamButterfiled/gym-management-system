<template>
  <div class="repair-list-page">
    <!-- Search / Top Container -->
    <GlassCard variant="search">
      <a-form layout="inline" style="padding-top: 0px;">
        <div style="margin-left: 14px;">
           <div style="color:#867E7E;">报修筛选</div>
           <StandardInput disabled placeholder="暂无筛选条件" class="input-1" variant="grey" />
        </div>
        <a-form-item style="margin-left: 40px; margin-top: 30px;">
             <!-- Use Default/Search button style for Refresh but with different icon if needed or just handle semantics -->
             <StandardButton type="search" icon="reload" @click="loadData">刷新</StandardButton>
        </a-form-item>
      </a-form>
    </GlassCard>

    <!-- Table Container -->
    <GlassCard variant="table">
        <StandardTable :configStyle="currentStyle" 
           
            :dataSource="tableData" 
            :columns="columns" 
            :pagination="pagination"
            rowKey="id"
            @change="handleTableChange"
        >
        <template #bodyCell="{ column, record }: { column: any, record: any }">
            <template v-if="column.key === 'status'">
                <a-tag :class="['repair-status-pill', `repair-status-pill--${String(record.status || '').toLowerCase()}`]">
                    {{ record.status === 'FIXED' ? '已维修' : (record.status === 'PROCESSING' ? '维修中' : '待维修') }}
                </a-tag>
            </template>
            <template v-if="column.key === 'action'">
                <a-button v-if="record.status !== 'FIXED'" type="text" class="repair-action-btn" @click="handleComplete(record)">完成维修</a-button>
                <span v-else class="repair-complete-text">已完成</span>
            </template>
        </template>
        </StandardTable>
    </GlassCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
// import request from '@/request'; // Backend missing for Repair
import { Repair } from '@/types';
import { usePageStyle } from '@/hooks/usePageStyle';

// Shared Components
import GlassCard from '@/components/common/GlassCard.vue';
import StandardInput from '@/components/common/StandardInput.vue';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardTable from '@/components/common/StandardTable.vue';

const tableData = ref<Repair[]>([]);

// Page Style
const { currentStyle, loadMenuConfig } = usePageStyle();

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 2,
});

const columns = [
  { title: '序号', dataIndex: 'id', key: 'id', width: 80 },
  { title: '报修描述', dataIndex: 'description', key: 'description' },
  { title: '状态', dataIndex: 'status', key: 'status' }, 
  { title: '报修时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '操作', key: 'action', width: 150 },
];

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

onMounted(() => {
  loadMenuConfig();
  loadData();
});
</script>

<style scoped>
 :deep(.ant-input) { width: 260px; }

 .repair-status-pill {
     padding: 2px 10px;
     border-radius: 999px;
     border: 1px solid rgba(255, 255, 255, 0.45) !important;
     background: rgba(255, 255, 255, 0.34) !important;
     box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.22);
 }

 .repair-status-pill--pending {
     color: #b7791f !important;
     background: rgba(245, 158, 11, 0.12) !important;
     border-color: rgba(245, 158, 11, 0.2) !important;
 }

 .repair-status-pill--processing {
     color: #2563eb !important;
     background: rgba(59, 130, 246, 0.1) !important;
     border-color: rgba(59, 130, 246, 0.18) !important;
 }

 .repair-status-pill--fixed {
     color: #15803d !important;
     background: rgba(34, 197, 94, 0.1) !important;
     border-color: rgba(34, 197, 94, 0.18) !important;
 }

 .repair-action-btn {
     height: 30px;
     padding: 0 12px;
     border-radius: 999px;
     border: 1px solid rgba(245, 158, 11, 0.18) !important;
     background: rgba(245, 158, 11, 0.08) !important;
     color: #b7791f !important;
     box-shadow: none !important;
 }

 .repair-action-btn:hover {
     background: rgba(245, 158, 11, 0.16) !important;
     color: #9a6a12 !important;
 }

 .repair-complete-text {
     color: rgba(100, 116, 139, 0.7);
 }
</style>
