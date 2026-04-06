<template>
  <div class="user-list-page">
    <!-- 搜索栏容器 -->
    <GlassCard variant="search">
      <a-form layout="inline" style="padding-top: 0px;">
        <div style="margin-left: 14px;">
           <div style="color:#867E7E;">用户昵称</div>
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

    <!-- 表格容器 -->
    <GlassCard variant="table">
       <div style="margin-bottom: 20px; display: flex; align-items: center; justify-content: space-between;">
         <a-space warp style="margin-bottom: 3px;">
            <StandardButton type="add" icon="plus" @click="handleAdd" style="width:112px;">新增用户</StandardButton>
         </a-space>
         
         <!-- New Batch Delete Component -->
         <BatchDeleteButton 
            v-model:active="isDeleteMode" 
            :count="selectedRowKeys.length" 
            @delete="handleBatchDelete" 
         />
       </div>

        <StandardTable :configStyle="currentStyle" 
           
            :dataSource="tableData" 
            :columns="columns" 
            :pagination="pagination"
            rowKey="id"
            :row-selection="rowSelection"
            @change="handleTableChange"
            :scroll="{ x: 1000 }"
        >
        <template #bodyCell="{ column, record }: { column: any, record: User }">
            <template v-if="column.key === 'role'">
                <a-tag :color="record.role === 'ADMIN' ? 'red' : (record.role === 'COACH' ? 'orange' : 'blue')">
                    {{ record.role === 'ADMIN' ? '管理员' : (record.role === 'COACH' ? '教练' : '会员') }}
                </a-tag>
            </template>
        </template>
        </StandardTable>
     </GlassCard>

    <!-- 编辑/新增 弹窗 (Glassy Modal) -->
     <StandardModal
      v-model:visible="modalVisible"
      :title="modalTitle"
      @ok="handleModalOk"
    >
      <a-form :model="formState" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="用户名">
          <StandardInput v-model:value="formState.username" variant="grey" class="modal-input-unified" />
        </a-form-item>
        <a-form-item label="昵称">
          <StandardInput v-model:value="formState.nickname" variant="grey" class="modal-input-unified" />
        </a-form-item>
        <a-form-item label="手机号">
            <StandardInput v-model:value="formState.phone" variant="grey" class="modal-input-unified" />
        </a-form-item>
        <a-form-item label="邮箱">
            <StandardInput v-model:value="formState.email" variant="grey" class="modal-input-unified" />
        </a-form-item>
        <a-form-item label="余额">
          <a-input-number v-model:value="formState.balance" :min="0" class="modal-input-unified" />
        </a-form-item>
        <a-form-item label="角色">
             <a-select v-model:value="formState.role" class="modal-input-unified" placeholder="请选择角色">
                <a-select-option value="MEMBER">会员</a-select-option>
                <a-select-option value="COACH">教练</a-select-option>
                <a-select-option value="STAFF">员工</a-select-option>
                <a-select-option value="ADMIN">管理员</a-select-option>
             </a-select>
        </a-form-item>
      </a-form>
    </StandardModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue';
import { message } from 'ant-design-vue';
import request from '@/request';
import { User, PageResult } from '@/types';
import { usePageStyle } from '@/hooks/usePageStyle';

// Shared Components
import GlassCard from '@/components/common/GlassCard.vue';
import StandardInput from '@/components/common/StandardInput.vue';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardTable from '@/components/common/StandardTable.vue';
import BatchDeleteButton from '@/components/common/BatchDeleteButton.vue';
import StandardModal from '@/components/common/StandardModal.vue';

const searchText = ref('');
const tableData = ref<User[]>([]);
const selectedRowKeys = ref<number[]>([]);
const isDeleteMode = ref(false);

// Page Style
const { currentStyle, loadMenuConfig } = usePageStyle();

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});
const modalVisible = ref(false);
const modalTitle = ref('新增用户');
const formState = reactive<User>({
    id: 0,
    username: '',
    realName: '',
    nickname: '',
    phone: '',
    email: '',
    role: 'MEMBER',
    balance: 0
});

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
  { title: '角色', dataIndex: 'role', key: 'role' },
  { title: '手机号', dataIndex: 'phone', key: 'phone' },
  { title: '余额', dataIndex: 'balance', key: 'balance' },
];

const rowSelection = computed(() => {
    if (!isDeleteMode.value) return null; // Only show checkboxes in delete mode
    return {
        selectedRowKeys: selectedRowKeys.value,
        onChange: (keys: number[]) => {
            selectedRowKeys.value = keys;
        }
    };
});

// Clear selection when mode is toggled off
watch(isDeleteMode, (newVal) => {
    if (!newVal) {
        selectedRowKeys.value = [];
    }
});

const loadData = () => {
    request.get("/user/page", {
        params: {
            pageNum: pagination.current,
            pageSize: pagination.pageSize,
            nickname: searchText.value
        }
    }).then((res: any) => {
        if(res.code === 200) {
            const data = res.data as PageResult<User>;
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
  modalTitle.value = '新增用户';
  formState.id = 0;
  formState.username = '';
  formState.realName = '';
  formState.nickname = '';
  formState.phone = '';
  formState.email = '';
  formState.role = undefined as any;
  formState.balance = undefined as any;
  modalVisible.value = true;
};

const handleBatchDelete = () => {
    if(selectedRowKeys.value.length === 0) return;
    request.delete("/user/batch", { data: selectedRowKeys.value }).then((res: any) => {
        if(res.code === 200) {
            message.success("删除成功");
            selectedRowKeys.value = [];
            isDeleteMode.value = false; // Turn off mode after successful delete
            loadData();
        } else {
            message.error(res.msg);
        }
    });
};

const handleModalOk = () => {
    request.post("/user", formState).then((res: any) => {
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
 :deep(.ant-input) { width: 260px; }
 :deep(.ant-input-number-input) { height: 40px; }
 :deep(.ant-select-selector) { background-color: #F7F5F5 !important; height: 40px !important; display: flex !important; align-items: center !important; border: none !important; border-radius: 8px !important; }
 :deep(.ant-select-selection-item), :deep(.ant-select-selection-placeholder) { line-height: 40px !important; display: flex !important; align-items: center !important; }
  
  /* Modal Alignment */
 :deep(.ant-form-item .ant-row) {
     align-items: center;
 }
 :deep(.ant-form-item-control-input) {
     min-height: 40px;
 }
</style>
