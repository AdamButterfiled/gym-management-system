<template>
  <div class="reservation-list-page">
    <!-- Search / Filter Container -->
    <GlassCard variant="search">
      <a-form layout="inline" style="padding-top: 0px;">
        <div style="margin-left: 14px;">
           <div style="color:#867E7E;">预约筛选</div>
           <!-- Placeholder input if searching is needed, or just visual consistency -->
           <StandardInput disabled placeholder="暂无筛选条件" class="input-1" variant="grey" />
        </div>
        <a-form-item style="margin-left: 40px; margin-top: 30px;">
             <!-- Use generic reload as search action for now -->
             <StandardButton type="search" icon="reload" @click="loadData">刷新</StandardButton>
        </a-form-item>
      </a-form>
    </GlassCard>

    <!-- Table Container -->
    <GlassCard variant="table">
        <!-- No "Add" button for reservations usually, but we keep the structure consistent in spacing -->
        <div style="height: 20px;"></div>

        <StandardTable :configStyle="currentStyle" 
           
            :dataSource="tableData" 
            :columns="columns" 
            :pagination="pagination"
            rowKey="id"
            @change="handleTableChange" 
        >
        <template #bodyCell="{ column, record }: { column: any, record: Reservation }">
            <template v-if="column.key === 'targetType'">
                <a-tag :color="record.targetType === 'VENUE' ? 'orange' : 'purple'">{{ record.targetType === 'VENUE' ? '场馆' : '团课' }}</a-tag>
            </template>
            <template v-if="column.key === 'status'">
                 <a-badge :status="record.status === 'CONFIRMED' ? 'success' : 'default'" :text="record.status" />
            </template>
            <template v-if="column.key === 'action'">
            <a-popconfirm title="确定删除吗?" @confirm="handleDelete(record.id)">
                <a-button type="link" danger>删除</a-button>
            </a-popconfirm>
            </template>
        </template>
        </StandardTable>
    </GlassCard>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import request from '@/request';
import { Reservation, PageResult } from '@/types';
import { usePageStyle } from '@/hooks/usePageStyle';

// Shared Components
import GlassCard from '@/components/common/GlassCard.vue';
import StandardInput from '@/components/common/StandardInput.vue';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardTable from '@/components/common/StandardTable.vue';

const tableData = ref<Reservation[]>([]);

// Page Style
const { currentStyle, loadMenuConfig } = usePageStyle();

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId' },
  { title: '预约对象', dataIndex: 'targetId', key: 'targetId' },
  { title: '类型', key: 'targetType', dataIndex: 'targetType' },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime' },
  { title: '结束时间', dataIndex: 'endTime', key: 'endTime' },
  { title: '状态', key: 'status', dataIndex: 'status' },
  { title: '操作', key: 'action', width: 120 },
];

const loadData = () => {
    request.get("/reservation/page", {
        params: {
            pageNum: pagination.current,
            pageSize: pagination.pageSize
        }
    }).then((res: any) => {
        if(res.code === 200) {
            const data = res.data as PageResult<Reservation>;
            tableData.value = data.records;
            pagination.total = data.total;
        } else {
            message.error(res.msg);
        }
    })
};

const handleTableChange = (pag: any) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  loadData();
};

const handleDelete = (id: number) => {
    request.delete("/reservation/" + id).then((res: any) => {
        if(res.code === 200) {
            message.success("删除成功");
            loadData();
        } else {
            message.error(res.msg);
        }
    });
};

onMounted(() => {
  loadMenuConfig();
  loadData();
});
</script>

<style scoped>
 :deep(.ant-input) { width: 260px; }
</style>
