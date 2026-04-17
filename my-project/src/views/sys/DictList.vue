<template>
  <WorkspacePage title="数据字典管理">
    <a-row :gutter="gutterSize" class="dict-layout">
        <!-- Left Pane: Dictionary Types -->
        <a-col :span="6">
            <section class="left-pane-card">
                <div class="pane-header">
                    <div class="title">字典类型</div>
                    <div class="header-actions">
                         <a-tooltip title="新增类型" v-if="!isTypeDeleteMode">
                             <StandardButton type="icon" size="icon-sm" icon="plus" @click="handleAddType" />
                        </a-tooltip>
                        
                         <!-- Left Trash Action Group -> Refactored to use Component -->
                         <BatchDeleteButton 
                            v-model:active="isTypeDeleteMode"
                            :count="selectedTypeKeys.length"
                            popconfirmTitle="确定删除选中的类型吗? 将清空其下所有数据!"
                            @delete="handleBatchDeleteTypes"
                         />
                    </div>
                </div>
                
                <!-- Search Input -> Variant Glass (White) as requested -->
                <div class="search-wrap">
                    <StandardInput 
                        v-model:value="typeSearchText" 
                        placeholder="搜索类型" 
                        allow-clear 
                        variant="glass"
                        @pressEnter="loadTypes"
                        @change="onSearchChange"
                    >
                        <template #prefix><SearchOutlined class="search-icon"/></template>
                    </StandardInput>
                </div>
                
                <div class="type-list">
                     <div 
                         v-for="type in paginatedTypes" 
                         :key="type"
                         class="type-item"
                         :class="{ 'active': currentType === type, 'light-yellow-bg': selectedTypeKeys.includes(type) }"
                         @click="handleTypeSelect(type)"
                     >
                         <transition name="slide-fade">
                             <div v-if="isTypeDeleteMode" class="checkbox-wrapper" @click.stop>
                                 <a-checkbox :value="type" :checked="selectedTypeKeys.includes(type)" @change="(e) => onTypeCheck(type, e.target.checked)" />
                             </div>
                         </transition>

                         <div class="type-content">
                             <FolderOutlined style="margin-right: 8px; font-size: 16px;"/>
                             <span class="type-text">{{ type }}</span>
                         </div>
                     </div>
                     <div v-if="filteredTypes.length === 0" class="empty-text">暂无数据</div>
                </div>

                <!-- Pagination Footer -->
                <div class="pagination-footer" v-if="filteredTypes.length > 0">
                    <a-pagination 
                        v-model:current="pagination.current" 
                        :total="pagination.total" 
                        :page-size="pagination.pageSize" 
                        :show-total="showTotal" 
                        show-quick-jumper
                        @change="handlePageChange"
                    />
                </div>
            </section>
        </a-col>

        <!-- Right Pane: Dictionary Data -->
        <a-col :span="18">
            <div class="filter-row dict-filter-row">
                <TableSearchToolbar
                    v-model="keyword"
                    :placeholder="quickSearchPlaceholder"
                    :loading="loading"
                    :filter-count="activeFilterCount"
                    :show-keyword="quickSearchEnabled"
                    @search="handleSearch"
                    @open-filter="filterModalVisible = true"
                    @reset="handleReset"
                />
                <StandardButton type="add" icon="plus" @click="handleAddData">新增数据</StandardButton>
            </div>

            <section class="workspace-subsection">
                 <div class="dict-toolbar">
                    <!-- Right Trash Action Group -> Refactored to use Component -->
                    <BatchDeleteButton 
                        v-model:active="isDataDeleteMode"
                        :count="selectedRowKeys.length"
                        @delete="handleBatchDelete"
                    />
                 </div>

                <StandardTable :configStyle="currentStyle" 
                    :dataSource="tableData" 
                    :columns="columns" 
                    :pagination="false"
                    rowKey="id"
                    :scroll="{ x: 800 }"
                    :row-selection="rowSelection" 
                    class="dict-table"
                >
                    <template #bodyCell="{ column, record }">
                        <template v-if="column.key === 'status'">
                            <span>{{ record.status === 0 ? '停用' : '正常' }}</span>
                        </template>
                        <template v-if="column.key === 'action'">
                            <a-space>
                                <StandardButton type="link" size="sm" class="table-action-link" @click="handleEdit(record)">编辑</StandardButton>
                                <a-popconfirm title="确定删除吗?" @confirm="handleDelete(record.id)" okText="删除" cancelText="取消" :okButtonProps="{ shape: 'round', size: 'small', danger: true }" :cancelButtonProps="{ shape: 'round', size: 'small' }">
                                    <StandardButton type="link" size="sm" danger class="table-action-link">删除</StandardButton>
                                </a-popconfirm>
                            </a-space>
                        </template>
                    </template>
                </StandardTable>
            </section>
        </a-col>
    </a-row>

    <AdvancedFilterModal
      v-model:visible="filterModalVisible"
      :fields="dictFilterableFields"
      :logic="filterLogic"
      :rules="filterRules"
      :text-suggestions="textSuggestions"
      @apply="applyAdvancedFilters"
    />

    <!-- Modal: Type Add/Edit -->
    <!-- Modal: Type Add/Edit -->
    <StandardModal
      v-model:visible="typeModalVisible"
      :title="typeModalTitle"
      @ok="handleTypeModalOk"
    >
      <a-form
        :model="typeForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
        class="workspace-modal-form"
      >
        <ConfiguredFormLayout :fields="typeFormFields">
          <template #field-dictName>
            <a-form-item label="字典名称">
              <StandardInput v-model:value="typeForm.dictName" placeholder="如: 性别" variant="grey" class="modal-input-unified" />
            </a-form-item>
          </template>
          <template #field-dictType>
            <a-form-item label="字典类型">
              <StandardInput v-model:value="typeForm.dictType" placeholder="如: sys_user_sex" variant="grey" class="modal-input-unified" />
            </a-form-item>
          </template>
          <template #field-status>
            <a-form-item label="状态">
              <a-select v-model:value="typeForm.status" class="modal-input-unified">
                <a-select-option :value="1">正常</a-select-option>
                <a-select-option :value="0">停用</a-select-option>
              </a-select>
            </a-form-item>
          </template>
        </ConfiguredFormLayout>
      </a-form>
    </StandardModal>

    <!-- Modal: Data Add/Edit -->
    <!-- Modal: Data Add/Edit -->
    <StandardModal
      v-model:visible="dataModalVisible"
      :title="dataModalTitle"
      @ok="handleDataModalOk"
    >
      <a-form
        :model="dataForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
        class="workspace-modal-form"
      >
        <ConfiguredFormLayout :fields="dataFormFields">
          <template #field-dictLabel>
            <a-form-item label="字典标签">
              <StandardInput v-model:value="dataForm.dictLabel" placeholder="如: 男" variant="grey" class="modal-input-unified" />
            </a-form-item>
          </template>
          <template #field-dictValue>
            <a-form-item label="字典键值">
              <StandardInput v-model:value="dataForm.dictValue" placeholder="如: 1" variant="grey" class="modal-input-unified" />
            </a-form-item>
          </template>
          <template #field-sort>
            <a-form-item label="排序">
              <a-input-number v-model:value="dataForm.dictSort" class="modal-input-unified" />
            </a-form-item>
          </template>
          <template #field-status>
            <a-form-item label="状态">
              <div class="checkbox-wrapper" style="height: 40px;">
                <a-radio-group v-model:value="dataForm.status">
                  <a-radio :value="1">正常</a-radio>
                  <a-radio :value="0">停用</a-radio>
                </a-radio-group>
              </div>
            </a-form-item>
          </template>
          <template #field-remark>
            <a-form-item label="备注">
              <a-textarea v-model:value="dataForm.remark" class="modal-input-unified" style="min-height: 80px;" />
            </a-form-item>
          </template>
        </ConfiguredFormLayout>
      </a-form>
    </StandardModal>
  </WorkspacePage>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue';
import { message } from 'ant-design-vue';
import { 
    SearchOutlined,
    FolderOutlined
} from '@ant-design/icons-vue';
import request from '@/request';
import { usePageStyle } from '@/hooks/usePageStyle';
import { useConfiguredTablePage } from '@/composables/useConfiguredTablePage';
import TableSearchToolbar from '@/components/common/TableSearchToolbar.vue';
import AdvancedFilterModal from '@/components/common/AdvancedFilterModal.vue';
import type { FormFilterRule } from '@/types/formConfig';
import ConfiguredFormLayout from '@/components/common/ConfiguredFormLayout.vue';

// Standard Components
import StandardTable from '@/components/common/StandardTable.vue';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardInput from '@/components/common/StandardInput.vue';
import BatchDeleteButton from '@/components/common/BatchDeleteButton.vue';
import StandardModal from '@/components/common/StandardModal.vue';
import WorkspacePage from '@/components/common/WorkspacePage.vue';
import { useStore } from 'vuex';

interface SysDict {
  id?: number;
  dictName?: string;
  dictType: string;
  dictLabel: string;
  dictValue: string;
  sort: number;
  status?: number;
  remark?: string;
}

const typeSearchText = ref('');
const typeList = ref<string[]>([]);
const currentType = ref<string>('');
const selectedTypeKeys = ref<string[]>([]); // For Left Pane
const selectedRowKeys = ref<number[]>([]); // For Right Pane
const fixedRules = computed<FormFilterRule[]>(() =>
    currentType.value
        ? [{
            id: 'dict-type-fixed',
            fieldKey: 'dictType',
            queryKey: 'dictType',
            controlType: 'text',
            operator: 'equals',
            matchMode: 'exact',
            value: currentType.value,
        }]
        : []
);
const {
    filterableFields,
    quickSearchEnabled,
    quickSearchPlaceholder,
    keyword,
    filterLogic,
    filterRules,
    filterModalVisible,
    activeFilterCount,
    tableData,
    loading,
    textSuggestions,
    ensureConfig,
    loadData,
    handleSearch,
    handleReset,
    applyAdvancedFilters,
    buildColumns,
    getTargetFields,
} = useConfiguredTablePage<SysDict>({
    routePath: '/sys/dict',
    pageSize: 500,
    fixedRules,
});

// Modal State
const typeModalVisible = ref(false);
const typeModalTitle = ref('');
const dataModalVisible = ref(false);
const dataModalTitle = ref('');

const isTypeDeleteMode = ref(false); // Left Pane Mode
const isDataDeleteMode = ref(false); // Right Pane Mode

const store = useStore();
const isTraditional = computed(() => store.state.themeSettings.styleMode === 'traditional');

// Dynamic gutter: Traditional mode has tighter spacing (cards closer together)
const gutterSize = computed(() => isTraditional.value ? 12 : 24); 
const dictFilterableFields = computed(() =>
    filterableFields.value.filter((field) => field.fieldKey !== 'dictType')
);

// Use Hook for Page Style
const { currentStyle, loadMenuConfig } = usePageStyle();

// Pagination for Types
const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0
});

// Show total logic
const showTotal = (total: number) => {
    const pages = Math.ceil(total / pagination.pageSize) || 1;
    return `共 ${total} 条, 共 ${pages} 页`;
};

const filteredTypes = computed(() => {
    let list = typeList.value;
    if(typeSearchText.value) {
        list = list.filter(t => t.toLowerCase().includes(typeSearchText.value.toLowerCase()));
    }
    return list;
});

// Update pagination total on filter
watch(filteredTypes, (newVal) => {
    pagination.total = newVal.length;
    const maxPage = Math.ceil(newVal.length / pagination.pageSize) || 1;
    if (pagination.current > maxPage) {
        pagination.current = 1;
    }
}, { immediate: true });

const paginatedTypes = computed(() => {
    const start = (pagination.current - 1) * pagination.pageSize;
    const end = start + pagination.pageSize;
    return filteredTypes.value.slice(start, end);
});

const handlePageChange = (page: number) => {
    pagination.current = page;
};

const onSearchChange = () => {
    pagination.current = 1;
};

const baseColumns = [
  { title: '字典类型', dataIndex: 'dictType', key: 'dictType', width: 180 },
  { title: '字典标签', dataIndex: 'dictLabel', key: 'dictLabel' },
  { title: '字典键值', dataIndex: 'dictValue', key: 'dictValue' },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 220 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' },
];
const columns = computed(() => buildColumns(baseColumns));
const typeFormFields = computed(() => getTargetFields('type-form'));
const dataFormFields = computed(() => getTargetFields('data-form'));

const typeForm = reactive({
    id: undefined,
    dictName: '',
    dictType: '',
    status: 1
});

const dataForm = reactive({
    id: undefined,
    dictType: '',
    dictLabel: '',
    dictValue: '',
    dictSort: 0,
    status: 1,
    remark: ''
});

// Load distinct types
const loadTypes = () => {
    request.get("/dict/types").then((res: any) => {
        if(res.code === 200) {
            typeList.value = res.data;
            if(!currentType.value && typeList.value.length > 0) {
                handleTypeSelect(typeList.value[0]);
            }
        }
    });
};

const handleTypeSelect = (type: string) => {
    currentType.value = type;
    // Reset Data Delete Mode on switch
    isDataDeleteMode.value = false;
    selectedRowKeys.value = [];
    loadData();
};

const onTypeCheck = (type: string, checked: boolean) => {
    if (checked) {
        selectedTypeKeys.value.push(type);
    } else {
        selectedTypeKeys.value = selectedTypeKeys.value.filter(k => k !== type);
    }
};

// --- Left Pane Trash Logic ---
// Handled by Component, but we need watchers or logic if mode changes externally
// Actually component v-models the mode, so we just react to selection.

const rowSelection = computed(() => {
    if(!isDataDeleteMode.value) return null;
    return {
        selectedRowKeys: selectedRowKeys.value,
        onChange: (keys: number[]) => { selectedRowKeys.value = keys; }
    };
});

const handleBatchDeleteTypes = () => {
    if(selectedTypeKeys.value.length === 0) return;
    request.delete("/dict/type/batch", { data: selectedTypeKeys.value }).then((res: any) => {
        if(res.code === 200) {
             message.success("类型批量删除成功");
             selectedTypeKeys.value = [];
             currentType.value = '';
             tableData.value = [];
             isTypeDeleteMode.value = false;
             loadTypes();
        } else {
            message.error(res.msg);
        }
    });
};

const handleBatchDelete = () => {
    if (selectedRowKeys.value.length === 0) return;
    request.delete("/dict/batch", { data: selectedRowKeys.value }).then((res: any) => {
        if(res.code === 200) {
             message.success("批量删除成功");
             selectedRowKeys.value = [];
             isDataDeleteMode.value = false; // Reset mode on success
             loadData();
        } else {
            message.error(res.msg);
        }
    });
};

 // Type Handlers
const handleAddType = () => {
    typeModalTitle.value = '新增字典类型';
    typeForm.id = undefined;
    typeForm.dictName = '';
    typeForm.dictType = '';
    typeForm.status = 1;
    typeModalVisible.value = true;
};

const handleTypeModalOk = () => {
    request.post("/dict/type", typeForm).then((res: any) => {
         if(res.code === 200) {
            message.success("保存成功");
            typeModalVisible.value = false;
            loadTypes();
         } else {
             message.error(res.msg);
         }
    });
};

// Data Handlers
const handleAddData = () => {
    if(!currentType.value) {
        message.warning("请先选择一个字典类型");
        return;
    }
    dataModalTitle.value = `新增数据 [${currentType.value}]`;
    dataForm.id = undefined;
    dataForm.dictType = currentType.value;
    dataForm.dictLabel = '';
    dataForm.dictValue = '';
    dataForm.dictSort = 0;
    dataForm.status = 1;
    dataForm.remark = '';
    dataModalVisible.value = true;
};

const handleEdit = (record: any) => {
    dataModalTitle.value = '编辑字典数据';
    // Map record to dataForm
    dataForm.id = record.id;
    dataForm.dictType = record.dictType;
    dataForm.dictLabel = record.dictLabel;
    dataForm.dictValue = record.dictValue;
    dataForm.dictSort = record.sort || 0; // Backend map 'sort' to 'dictSort' if needed? 
    // Wait, typical backend for DictData has fields: dictSort, dictLabel, dictValue... 
    // Assuming table record matches. 
    // If table record has 'sort', we map it.
    dataForm.status = record.status !== undefined ? record.status : 1;
    dataForm.remark = record.remark || '';
    
    dataModalVisible.value = true;
};

const handleDataModalOk = () => {
    // Map form to backend expected payload
    const payload = {
        ...dataForm,
        sort: dataForm.dictSort // Ensure sort is mapped if backend expects 'sort'
    };
    
    request.post("/dict", payload).then((res: any) => {
        if(res.code === 200) {
            message.success("保存成功");
            dataModalVisible.value = false;
            loadData();
        } else {
             message.error(res.msg);
        }
    });
};

const handleDelete = (id: number) => {
     request.delete("/dict/" + id).then((res: any) => {
        if(res.code === 200) {
            message.success("删除成功");
            loadData();
        } else {
            message.error(res.msg);
        }
    });
};

onMounted(async () => {
    loadMenuConfig();
    await ensureConfig();
    loadTypes();
});
</script>

<style scoped>
 /* Only page specific styles left, common animations moved to BatchDeleteButton but keeping slide-fade for left menu items checkboxes */

 .left-pane-card {
     height: 600px;
     display: flex;
     flex-direction: column;
     overflow: hidden;
     padding-right: 15px;
 }

 .pane-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; height: 32px;}
 .title { font-weight: 600; font-size: 16px; color: #111111; }
 .header-actions { display: flex; align-items: center; justify-content: flex-end; }
 .form-label { color: #5f6368; }
 .dict-filter-row { padding-top: 0; }
 .dict-toolbar { margin-bottom: 20px; display: flex; align-items: center; justify-content: flex-end; gap: 12px; }
 
 .search-wrap { margin-bottom: 15px; }

 .type-list { flex: 1; overflow-y: auto; padding-right: 4px; margin-top: 10px; }
 
 .type-item { 
     display: flex; 
     align-items: center; 
     padding: 10px 12px; 
     border-radius: var(--mono-radius-sm); 
     cursor: pointer; 
     transition: all 0.2s;
     margin-bottom: 4px;
 }
 .type-item:hover { background-color: #f7f7f5; }
 .type-item.active { background-color: #f0f0ee; color: #111111; font-weight: 600; }
 .type-item.light-yellow-bg { background-color: #f7f7f5; }
 .type-item .type-text { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
 .empty-text { color: #8a8f98; text-align: center; margin-top: 20px; }
 .pagination-footer { padding-top: 15px; display: flex; justify-content: center; /* Removed border-top */ }

 /* Fade and Slide Transitions */
 .slide-fade-enter-active, .slide-fade-leave-active { transition: all 0.3s ease; }
 .slide-fade-enter-from, .slide-fade-leave-to { transform: translateX(-10px); opacity: 0; margin-right: -24px; }
 .fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
 .fade-enter-from, .fade-leave-to { opacity: 0; }
 
 .checkbox-wrapper { margin-right: 8px; display: flex; align-items: center; }
 /* ===== DARK MODE ===== */
 :global(.dark) .title {
     color: #ffffffd9;
 }

 :global(.dark) .type-item:hover {
     background-color: rgba(255, 255, 255, 0.08);
 }

 :global(.dark) .type-item.active {
     background-color: rgba(255, 255, 255, 0.12);
     color: #ffffffd9;
 }

 :global(.dark) .type-item.light-yellow-bg {
     background-color: rgba(255, 255, 255, 0.08);
 }

 :global(.dark) .empty-text {
     color: rgba(255, 255, 255, 0.45);
 }



 :global(.dark) .type-text {
     color: #ffffffd9;
 }

 :global(.dark) .form-label {
     color: rgba(255, 255, 255, 0.55);
 }
</style>

<style>
:global(.search-icon) { color: rgba(0,0,0,0.25); }
:global(.dark) :global(.search-icon) { color: rgba(255,255,255,0.45); }

/* Force input transparency in dark mode for this component */
:global(.dark) .search-wrap .standard-input-wrapper .std-input {
    background-color: transparent !important;
    border-color: rgba(255,255,255,0.1) !important;
}
:global(.dark) .search-wrap .standard-input-wrapper .std-input-control {
    background-color: transparent !important; 
}
</style>
