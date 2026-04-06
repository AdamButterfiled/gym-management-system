<template>
  <div class="equipment-list-page">
    <!-- Search Container -->
    <GlassCard variant="search">
      <a-form layout="inline" style="padding-top: 0px;">
        <div style="margin-left: 14px;">
           <div style="color:#867E7E;">器材名称</div>
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
         <StandardButton type="add" icon="plus" @click="handleAdd" style="width:112px;">新增器材</StandardButton>
       </a-space>

        <StandardTable :configStyle="currentStyle" 
           
            :dataSource="tableData" 
            :columns="columns" 
            :pagination="pagination"
            rowKey="id"
            @change="handleTableChange"
        >
        <template #bodyCell="{ column, record }: { column: any, record: Equipment }">
            <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 'AVAILABLE' ? 'green' : (record.status === 'IN_USE' ? 'blue' : 'red')">
                    {{ record.status === 'AVAILABLE' ? '可用' : (record.status === 'IN_USE' ? '使用中' : '维护中') }}
                </a-tag>
            </template>
            <template v-if="column.key === 'action'">
                <a-button type="link" @click="handleEdit(record)" style="color: #F4B53F">编辑</a-button>
                <a-popconfirm title="确定删除吗?" @confirm="handleDelete(record.id)">
                    <a-button type="link" danger>删除</a-button>
                </a-popconfirm>
            </template>
        </template>
        </StandardTable>
     </GlassCard>

    <!-- Modal -->
    <StandardModal
      v-model:visible="modalVisible"
      :title="modalTitle"
      @ok="handleModalOk"
    >
      <a-form :model="formState" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
         <a-form-item label="器材名称">
          <StandardInput v-model:value="formState.name" variant="grey" class="modal-input-unified" />
        </a-form-item>
        
        <a-form-item label="数量">
             <a-input-number v-model:value="formState.quantity" :min="0" class="modal-input-unified" />
        </a-form-item>
        
        <a-form-item label="状态">
             <a-select v-model:value="formState.status" class="modal-input-unified" placeholder="请选择状态">
                <a-select-option value="NORMAL">正常</a-select-option>
                <a-select-option value="DAMAGED">损坏</a-select-option>
                <a-select-option value="MAINTENANCE">维护中</a-select-option>
             </a-select>
        </a-form-item>
      </a-form>
    </StandardModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import request from '@/request';
import { Equipment, PageResult } from '@/types';
import { usePageStyle } from '@/hooks/usePageStyle';

// Shared Components
import GlassCard from '@/components/common/GlassCard.vue';
import StandardInput from '@/components/common/StandardInput.vue';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardTable from '@/components/common/StandardTable.vue';
import StandardModal from '@/components/common/StandardModal.vue';

const searchText = ref('');
const tableData = ref<Equipment[]>([]);

// Page Style
const { currentStyle, loadMenuConfig } = usePageStyle();

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});
const modalVisible = ref(false);
const modalTitle = ref('新增器材');
const formState = reactive<Equipment>({
    id: 0,
    name: '',
    description: '',
    status: 'AVAILABLE',
    venueId: 0
});

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '器材名称', dataIndex: 'name', key: 'name' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action', width: 150 },
];

const loadData = () => {
    request.get("/equipment/page", {
        params: {
            pageNum: pagination.current,
            pageSize: pagination.pageSize,
            name: searchText.value
        }
    }).then((res: any) => {
        if(res.code === 200) {
             const data = res.data as PageResult<Equipment>;
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
    modalTitle.value = '新增器材';
    formState.id = 0;
    formState.name = '';
    formState.description = '';
    formState.status = undefined as any;
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

const handleDelete = (id: number) => {
     request.delete("/equipment/" + id).then((res: any) => {
        if(res.code === 200) {
            message.success("删除成功");
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
 :deep(.ant-select-selector) { background-color: #F7F5F5 !important; height: 40px !important; display: flex !important; align-items: center !important; border: none !important; border-radius: 8px !important; }
 :deep(.ant-select-selection-item), :deep(.ant-select-selection-placeholder) { line-height: 40px !important; display: flex !important; align-items: center !important; }

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
