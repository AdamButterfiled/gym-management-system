<template>
  <div class="venue-list-page">
    <!-- Search Container -->
    <GlassCard variant="search">
      <a-form layout="inline" style="padding-top: 0px;">
        <div style="margin-left: 14px;">
           <div style="color:#867E7E;">场馆名称</div>
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
         <StandardButton type="add" icon="plus" @click="handleAdd" style="width:112px;">新增场馆</StandardButton>
       </a-space>

        <StandardTable :configStyle="currentStyle" 
           
            :dataSource="tableData" 
            :columns="columns" 
            :pagination="pagination"
            rowKey="id"
            @change="handleTableChange"
            :scroll="{ x: 1000 }"
        >
        <template #bodyCell="{ column, record }: { column: any, record: any }">
            <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 1 ? 'green' : 'red'">
                    {{ record.status === 1 ? '开放中' : '已关闭' }}
                </a-tag>
            </template>
            <template v-if="column.key === 'action'">
            <a-space>
                <a-button type="link" @click="handleEdit(record)" style="color: #F4B53F">编辑</a-button>
                <a-popconfirm title="确定删除吗?" @confirm="handleDelete(record.id)">
                    <a-button type="link" danger>删除</a-button>
                </a-popconfirm>
            </a-space>
            </template>
        </template>
        </StandardTable>
     </GlassCard>

    <!-- Edit Modal -->
    <StandardModal
      v-model:visible="modalVisible"
      :title="modalTitle"
      @ok="handleModalOk"
    >
      <a-form :model="formState" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="场馆名称">
          <StandardInput v-model:value="formState.name" variant="grey" class="modal-input-unified" />
        </a-form-item>
        <a-form-item label="位置">
            <StandardInput v-model:value="formState.location" variant="grey" class="modal-input-unified" />
          </a-form-item>
        <a-form-item label="容量">
          <a-input-number v-model:value="formState.capacity" class="modal-input-unified" />
        </a-form-item>
        <a-form-item label="状态">
             <a-select v-model:value="formState.status" class="modal-input-unified" placeholder="请选择状态">
                <a-select-option value="OPEN">开放</a-select-option>
                <a-select-option value="CLOSED">关闭</a-select-option>
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
import { Venue, PageResult } from '@/types';
import { usePageStyle } from '@/hooks/usePageStyle';

// Shared Components
import GlassCard from '@/components/common/GlassCard.vue';
import StandardInput from '@/components/common/StandardInput.vue';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardTable from '@/components/common/StandardTable.vue';
import StandardModal from '@/components/common/StandardModal.vue';

const searchText = ref('');
const tableData = ref<Venue[]>([]);

// Page Style
const { currentStyle, loadMenuConfig } = usePageStyle();

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});
const modalVisible = ref(false);
const modalTitle = ref('新增场馆');
const formState = reactive<Venue>({
    id: 0,
    name: '',
    location: '',
    capacity: 0,
    status: 1
});

const columns = [
  { title: '序号', dataIndex: 'id', key: 'id', width: 80 },
  { title: '场馆名称', dataIndex: 'name', key: 'name' },
  { title: '位置', dataIndex: 'location', key: 'location' },
  { title: '容量 (人)', dataIndex: 'capacity', key: 'capacity' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action', fixed: 'right' as const, width: 200 },
];

const loadData = () => {
    request.get("/venue/page", {
        params: {
            pageNum: pagination.current,
            pageSize: pagination.pageSize,
            name: searchText.value
        }
    }).then((res: any) => {
        if(res.code === 200) {
            const data = res.data as PageResult<Venue>;
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

const handleAdd = () => {
  modalTitle.value = '新增场馆';
  formState.id = 0;
  formState.name = '';
  formState.location = '';
  formState.capacity = undefined as any;
  formState.status = undefined as any;
  modalVisible.value = true;
};

const handleEdit = (record: Venue) => {
  modalTitle.value = '编辑场馆';
  Object.assign(formState, record);
  modalVisible.value = true;
};

const handleDelete = (id: number) => {
    request.delete("/venue/" + id).then((res: any) => {
        if(res.code === 200) {
            message.success("删除成功");
            loadData();
        } else {
            message.error(res.msg);
        }
    });
};

const handleModalOk = () => {
    request.post("/venue", formState).then((res: any) => {
        if(res.code === 200){
            message.success("保存成功");
            modalVisible.value = false;
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
 /* Modal Button & Title Adjustments - Keep these if needed, but remove conflicting input styles */
 :deep(.ant-modal-close) {
     top: 5px !important;
     right: 5px !important;
 }
 :deep(.ant-modal-header) {
     padding: 10px 20px !important;
 }
</style>
