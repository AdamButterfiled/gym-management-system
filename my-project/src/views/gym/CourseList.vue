<template>
  <div class="course-list-page">
    <!-- Search Container -->
    <GlassCard variant="search">
      <a-form layout="inline" style="padding-top: 0px;">
        <div style="margin-left: 14px;">
           <div style="color:#867E7E;">课程名称</div>
           <StandardInput 
              v-model:value="searchText" 
              placeholder="请输入" 
              class="input-1" 
              variant="grey"
              @pressEnter="loadData"
           />
        </div>
        <a-form-item style="margin-left: 40px; margin-top: 30px;">
             <StandardButton type="search" icon="search" @click="loadData">搜索</StandardButton>
             <StandardButton type="reset" icon="reload" @click="searchText = ''; loadData()">重置</StandardButton>
        </a-form-item>
      </a-form>
    </GlassCard>

    <!-- Table Container -->
    <GlassCard variant="table">
        <a-space warp style="margin-bottom: 20px;">
         <StandardButton type="add" icon="plus" @click="handleAdd" style="width:112px;">发布团课</StandardButton>
        </a-space>

        <StandardTable :configStyle="currentStyle" 
           
            :dataSource="tableData" 
            :columns="columns" 
            :pagination="pagination"
            rowKey="id"
            @change="handleTableChange" 
        >
        <template #bodyCell="{ column, record }: { column: any, record: Course }">
            <template v-if="column.key === 'participants'">
                {{ record.currentParticipants }} / {{ record.maxParticipants }}
            </template>
            <template v-if="column.key === 'action'">
                <a-button type="link" danger @click="handleDelete(record.id)">取消</a-button>
            </template>
        </template>
        </StandardTable>
    </GlassCard>

     <!-- Edit Modal -->
     <StandardModal
      v-model:visible="modalVisible"
      title="发布团课"
      @ok="handleModalOk"
    >
      <a-form :model="formState" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="课程名称">
          <StandardInput v-model:value="formState.name" variant="grey" class="modal-input-unified" />
        </a-form-item>
        <a-form-item label="最大人数">
          <a-input-number v-model:value="formState.maxParticipants" class="modal-input-unified" />
        </a-form-item>
      </a-form>
    </StandardModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import request from '@/request';
import { Course, PageResult } from '@/types';
import { usePageStyle } from '@/hooks/usePageStyle';

// Shared Components
import GlassCard from '@/components/common/GlassCard.vue';
import StandardInput from '@/components/common/StandardInput.vue';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardTable from '@/components/common/StandardTable.vue';
import StandardModal from '@/components/common/StandardModal.vue';

const searchText = ref('');
const tableData = ref<Course[]>([]);

// Page Style
const { currentStyle, loadMenuConfig } = usePageStyle();

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});
const modalVisible = ref(false);
const formState = reactive<Course>({
    id: 0,
    name: '',
    startTime: '',
    endTime: '',
    maxParticipants: 20,
    currentParticipants: 0
});

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '课程名称', dataIndex: 'name', key: 'name' },
  { title: '参与人数', key: 'participants' },
  { title: '操作', key: 'action', width: 150 },
];

const loadData = () => {
    request.get("/course/page", {
        params: {
            pageNum: pagination.current,
            pageSize: pagination.pageSize,
        }
    }).then((res: any) => {
        if(res.code === 200) {
             const data = res.data as PageResult<Course>;
            tableData.value = data.records;
            pagination.total = data.total;
        }
    });
};

const handleTableChange = (pag: any) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  loadData();
};

const handleAdd = () => {
    formState.name = '';
    formState.maxParticipants = undefined as any;
    modalVisible.value = true;
};

const handleModalOk = () => {
    request.post("/course", formState).then((res: any) => {
        if(res.code === 200) {
            message.success("发布成功");
            modalVisible.value = false;
            loadData();
        }
    })
};

const handleDelete = (id: number) => {
     request.delete("/course/" + id).then((res: any) => {
        if(res.code === 200) {
            message.success("已取消");
            loadData();
        }
    });
}

onMounted(() => {
  loadMenuConfig();
  loadData();
});
</script>

<style scoped>
 :deep(.ant-input) { width: 260px; }
 :deep(.ant-input-number-input) { height: 40px; }


 
 /* Fix Vertical Alignment for Form Items */
 :deep(.ant-form-item .ant-row) {
     align-items: center;
 }
 :deep(.ant-form-item-control-input) {
     min-height: 40px;
 }
 /* Modal Button & Title Adjustments */
 :deep(.ant-modal-close) {
     top: 5px !important;
     right: 5px !important;
 }
 :deep(.ant-modal-header) {
     padding: 10px 20px !important;
 }
</style>
