<template>
  <div class="coach-list-page">
    <!-- 搜索栏容器 (Glass Style) -->
    <GlassCard variant="search">
      <a-form layout="inline" style="padding-top: 0px;">
        <div style="margin-left: 14px;">
           <div style="color:#867E7E;">教练名称</div>
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

    <!-- 表格容器 (Glass Style) -->
    <GlassCard variant="table">
       <a-space wrap style="margin-bottom: 20px;">
         <StandardButton type="add" icon="plus" @click="handleAdd" style="width:112px;">新增教练</StandardButton>
       </a-space>

        <StandardTable :configStyle="currentStyle" 
           
            :dataSource="tableData" 
            :columns="columns" 
            :pagination="pagination"
            rowKey="id"
            @change="handleTableChange"
            :scroll="{ x: 1000 }"
        >
        <template #bodyCell="{ column, record }: { column: any, record: Coach }">
            
            <template v-if="column.key === 'gender'">
               <a-tag :color="record.gender === 1 ? 'blue' : 'magenta'">
                   {{ getDictLabel(record.gender, genderDict) }}
               </a-tag>
            </template>
            
            <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 1 ? 'green' : 'red'">
                    {{ getDictLabel(record.status, statusDict) }}
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

    <!-- 编辑/新增 弹窗 -->
    <!-- 编辑/新增 弹窗 -->
    <StandardModal
      v-model:visible="modalVisible"
      :title="modalTitle"
      @ok="handleModalOk"
    >
       <a-form :model="formState" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
         <a-form-item label="姓名">
          <StandardInput v-model:value="formState.name" variant="grey" class="modal-input-unified" />
        </a-form-item>
        
        <a-form-item label="性别">
            <a-select v-model:value="formState.gender" class="modal-input-unified" placeholder="请选择性别">
               <a-select-option 
                 v-for="item in genderDict" 
                 :key="item.dictValue" 
                 :value="Number(item.dictValue)"
               >
                 {{ item.dictLabel }}
               </a-select-option>
            </a-select>
        </a-form-item>
        
        <a-form-item label="年龄">
          <a-input-number v-model:value="formState.age" class="modal-input-unified" />
        </a-form-item>
        
        <a-form-item label="电话">
            <StandardInput v-model:value="formState.phone" variant="grey" class="modal-input-unified" />
        </a-form-item>
        
        <a-form-item label="专长">
            <StandardInput v-model:value="formState.specialization" placeholder="例如: 瑜伽, 增肌" variant="grey" class="modal-input-unified" />
        </a-form-item>
        
        <a-form-item label="简介">
            <a-textarea v-model:value="formState.intro" class="modal-input-unified" style="min-height: 80px;" />
        </a-form-item>
        
        <a-form-item label="状态">
             <a-select v-model:value="formState.status" class="modal-input-unified" placeholder="请选择状态">
                <a-select-option 
                  v-for="item in statusDict" 
                  :key="item.dictValue" 
                  :value="Number(item.dictValue)"
                >
                  {{ item.dictLabel }}
                </a-select-option>
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
import { PageResult } from '@/types';
import { usePageStyle } from '@/hooks/usePageStyle';

// Shared Components
import GlassCard from '@/components/common/GlassCard.vue';
import StandardInput from '@/components/common/StandardInput.vue';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardTable from '@/components/common/StandardTable.vue';
import StandardModal from '@/components/common/StandardModal.vue';

interface Coach {
  id: number;            // ID
  name: string;          // 姓名
  gender: number;        // 性别: 1男 0女
  age: number;           // 年龄
  phone: string;         // 电话
  specialization: string;// 专长
  intro: string;         // 简介
  status: number;        // 状态: 1在职 0离职
}

interface SysDict {
    dictLabel: string;   // 字典标签 (如: 男)
    dictValue: string;   // 字典值 (如: 1)
}

const searchText = ref('');
const tableData = ref<Coach[]>([]);

const genderDict = ref<SysDict[]>([]);
const statusDict = ref<SysDict[]>([]);

// Page Style
const { currentStyle, loadMenuConfig } = usePageStyle();

const pagination = reactive({
  current: 1,    // 当前页
  pageSize: 10,  // 每页条数
  total: 0,      // 总条数
});

const modalVisible = ref(false);
const modalTitle = ref('新增教练');

const formState = reactive<Coach>({
    id: 0,
    name: '',
    gender: 1,      // 默认为 1 (男)
    age: 25,        // 默认年龄
    phone: '',
    specialization: '',
    intro: '',
    status: 1       // 默认为 1 (在职)
});

const columns = [
  { title: '序号', dataIndex: 'id', key: 'id', width: 80 },
  { title: '教练姓名', dataIndex: 'name', key: 'name' },
  { title: '性别', dataIndex: 'gender', key: 'gender', width: 80 },
  { title: '年龄', dataIndex: 'age', key: 'age', width: 80 },
  { title: '电话', dataIndex: 'phone', key: 'phone' },
  { title: '专长', dataIndex: 'specialization', key: 'specialization' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action', fixed: 'right' as const, width: 200 },
];

const loadDictData = (type: string, targetRef: any) => {
    request.get('/dict/type', {
        params: { type: type }
    }).then((res: any) => {
        if(res.code === 200) {
            targetRef.value = res.data;
        }
    });
}

const getDictLabel = (val: number, dictList: SysDict[]) => {
    const item = dictList.find(d => Number(d.dictValue) === val);
    return item ? item.dictLabel : val;
}

const loadData = () => {
    request.get("/coach/page", {
        params: {
            pageNum: pagination.current,   // 当前页码
            pageSize: pagination.pageSize, // 每页大小
            name: searchText.value         // 搜索关键词
        }
    }).then((res: any) => {
        if(res.code === 200) {
            const data = res.data as PageResult<Coach>;
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
  modalTitle.value = '新增教练';
  formState.id = 0;
  formState.name = '';
  formState.gender = undefined as any;
  formState.age = undefined as any;
  formState.phone = '';
  formState.specialization = '';
  formState.intro = '';
  formState.status = undefined as any;
  modalVisible.value = true;
};

const handleEdit = (record: Coach) => {
  modalTitle.value = '编辑教练';
  Object.assign(formState, record);
  modalVisible.value = true;
};

const handleDelete = (id: number) => {
    request.delete("/coach/" + id).then((res: any) => {
        if(res.code === 200) {
            message.success("删除成功");
            loadData();
        } else {
            message.error(res.msg);
        }
    });
};

const handleModalOk = () => {
    request.post("/coach", formState).then((res: any) => {
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
  loadDictData('gender', genderDict);
  loadDictData('coach_status', statusDict);
});
</script>

<style scoped>
 :deep(.ant-input) { width: 260px; }
 :deep(.ant-input-number-input) { height: 40px; }
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
