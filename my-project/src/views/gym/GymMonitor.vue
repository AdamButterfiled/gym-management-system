<template>
  <WorkspacePage title="场馆实时监控中心">
    <template #meta>
      <span class="page-meta">每 30 秒自动刷新一次状态数据。</span>
    </template>

    <template #actions>
      <div class="monitor-legend">
        <span class="legend-item"><span class="dot available"></span> 空闲开放</span>
        <span class="legend-item"><span class="dot busy"></span> 使用中</span>
        <span class="legend-item"><span class="dot closed"></span> 关闭/维护</span>
      </div>
      <StandardButton type="search" icon="reload" @click="loadData">刷新状态</StandardButton>
    </template>

    <section class="workspace-subsection">
      <div class="workspace-subsection-head">
        <div>
          <h2 class="workspace-section-title">场馆状态</h2>
          <div class="workspace-section-sub">监控场馆开放情况与容量信息。</div>
        </div>
      </div>
      <StandardTable :dataSource="venues" :columns="venueColumns" :pagination="false" rowKey="id">
        <template #bodyCell="{ column, record }: { column: any; record: Venue }">
          <template v-if="column.key === 'status'">
            <span :class="['status-pill', venueStatusTone(record.status)]">{{ venueStatusLabel(record.status) }}</span>
          </template>
          <template v-if="column.key === 'capacity'">
            {{ record.capacity }} 人
          </template>
        </template>
      </StandardTable>
    </section>

    <section class="workspace-subsection">
      <div class="workspace-subsection-head">
        <div>
          <h2 class="workspace-section-title">器材状态</h2>
          <div class="workspace-section-sub">监控当前器材可用、使用中与维护状态。</div>
        </div>
      </div>
      <StandardTable :dataSource="equipments" :columns="equipmentColumns" :pagination="false" rowKey="id">
        <template #bodyCell="{ column, record }: { column: any; record: Equipment }">
          <template v-if="column.key === 'venueId'">
            {{ record.venueId ? `${record.venueId} 号厅` : '-' }}
          </template>
          <template v-if="column.key === 'status'">
            <span :class="['status-pill', equipmentStatusTone(record.status)]">{{ equipmentStatusLabel(record.status) }}</span>
          </template>
        </template>
      </StandardTable>
    </section>
  </WorkspacePage>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import request from '@/request';
import { Venue, Equipment, PageResult } from '@/types';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardTable from '@/components/common/StandardTable.vue';
import WorkspacePage from '@/components/common/WorkspacePage.vue';

const venues = ref<Venue[]>([]);
const equipments = ref<Equipment[]>([]);
let refreshTimer: number | undefined;

const venueColumns = [
  { title: '场馆名称', dataIndex: 'name', key: 'name' },
  { title: '容量', dataIndex: 'capacity', key: 'capacity', width: 140 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 160 },
];

const equipmentColumns = [
  { title: '器材名称', dataIndex: 'name', key: 'name' },
  { title: '所属场馆', dataIndex: 'venueId', key: 'venueId', width: 160 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 160 },
  { title: '描述', dataIndex: 'description', key: 'description' },
];

const loadData = () => {
    // Load all venues
    request.get("/venue/page", { params: { pageSize: 100 } }).then((res: any) => {
        if(res.code === 200) {
            venues.value = (res.data as PageResult<Venue>).records;
        }
    });
    // Load all equipment
    request.get("/equipment/page", { params: { pageSize: 100 } }).then((res: any) => {
         if(res.code === 200) {
            equipments.value = (res.data as PageResult<Equipment>).records;
        }
    });
};

const venueStatusLabel = (status: number) => (status === 1 ? '开放中' : '已关闭');
const venueStatusTone = (status: number) => (status === 1 ? 'status-pill--strong' : 'status-pill--soft');

const equipmentStatusLabel = (status: string) => {
    if(status === 'AVAILABLE') return '可用';
    if(status === 'IN_USE') return '使用中';
    return '维护中';
};

const equipmentStatusTone = (status: string) => {
    if(status === 'AVAILABLE') return 'status-pill--strong';
    if(status === 'IN_USE') return 'status-pill--muted';
    return 'status-pill--soft';
};

onMounted(() => {
    loadData();
    refreshTimer = window.setInterval(loadData, 30000);
});

onUnmounted(() => {
    if (refreshTimer) {
        window.clearInterval(refreshTimer);
    }
});
</script>

<style scoped>
.monitor-legend {
    display: flex;
    align-items: center;
    gap: 18px;
    flex-wrap: wrap;
}

.legend-item {
    font-size: 13px;
    color: #5f6368;
    font-weight: 500;
}

.dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: var(--mono-radius-pill);
    margin-right: 6px;
}

.dot.available { background: #111111; }
.dot.busy { background: #666666; }
.dot.closed { background: #b8b8b2; }
</style>
