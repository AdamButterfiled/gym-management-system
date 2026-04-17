<template>
  <div class="user-list-page">
    <WorkspacePage title="用户信息管理" variant="menu-list">
      <template #actions>
        <StandardButton type="add" icon="plus" class="add-user-button" @click="handleAdd">
          新增用户
        </StandardButton>

        <div class="delete-shell">
          <BatchDeleteButton
            v-model:active="isDeleteMode"
            :count="selectedRowKeys.length"
            @delete="handleBatchDelete"
          />
        </div>
      </template>

      <template #filters>
        <TableSearchToolbar
          v-model="keyword"
          variant="menu-list"
          :placeholder="quickSearchPlaceholder"
          :loading="loading"
          :filter-count="activeFilterCount"
          :show-keyword="quickSearchEnabled"
          @search="handleSearch"
          @open-filter="filterModalVisible = true"
          @reset="handleReset"
        />
      </template>

      <section class="workspace-subsection">
        <StandardTable
          class="user-list-table"
          :configStyle="currentStyle"
          surface="menu-list"
          :dataSource="tableData"
          :columns="columns"
          :pagination="pagination"
          :locale="tableLocale"
          rowKey="id"
          :row-selection="rowSelection"
          @change="handleTableChange"
          :scroll="{ x: 1000 }"
        >
        <template #bodyCell="{ column, record }: { column: any, record: User }">
            <template v-if="column.key === 'id'">
                <span class="mono-id">#{{ String(record.id).padStart(2, '0') }}</span>
            </template>

            <template v-else-if="column.key === 'username'">
                <div class="user-identity">
                    <span class="user-avatar">{{ getUserInitial(record.username) }}</span>
                    <span class="user-text">
                        <span class="user-name">{{ record.username }}</span>
                        <span class="user-subcopy">{{ record.email || '系统账户' }}</span>
                    </span>
                </div>
            </template>

            <template v-else-if="column.key === 'nickname'">
                <span :class="['soft-text', { 'soft-text--muted': !record.nickname }]">
                    {{ record.nickname || '未设置昵称' }}
                </span>
            </template>

            <template v-else-if="column.key === 'role'">
                <span :class="['status-pill', `status-pill--${getRoleTone(record.role)}`]">
                    {{ getRoleLabel(record.role) }}
                </span>
            </template>

            <template v-else-if="column.key === 'phone'">
                <span :class="['soft-text', { 'soft-text--muted': !record.phone }]">
                    {{ record.phone || '未绑定手机号' }}
                </span>
            </template>

            <template v-else-if="column.key === 'email'">
                <span :class="['soft-text', { 'soft-text--muted': !record.email }]">
                    {{ record.email || '未设置邮箱' }}
                </span>
            </template>

            <template v-else-if="column.key === 'status'">
                <span :class="['soft-text', { 'soft-text--muted': !record.status }]">
                    {{ record.status || '未设置状态' }}
                </span>
            </template>

            <template v-else-if="column.key === 'createdAt'">
                <span :class="['soft-text', { 'soft-text--muted': !record.createdAt }]">
                    {{ record.createdAt || '未记录' }}
                </span>
            </template>

            <template v-else-if="column.key === 'balance'">
                <span class="balance-cell">¥ {{ formatBalance(record.balance) }}</span>
            </template>
        </template>
        </StandardTable>
      </section>
    </WorkspacePage>

    <AdvancedFilterModal
      v-model:visible="filterModalVisible"
      :fields="filterableFields"
      :logic="filterLogic"
      :rules="filterRules"
      :text-suggestions="textSuggestions"
      @apply="applyAdvancedFilters"
    />

    <!-- 编辑/新增弹窗 -->
    <StandardModal
       v-model:visible="modalVisible"
       :title="modalTitle"
       width="720px"
       @ok="handleModalOk"
     >
      <a-form
        :model="formState"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
        class="workspace-modal-form user-modal-form"
      >
        <ConfiguredFormLayout :fields="primaryFormFields">
          <template #field-username>
            <a-form-item label="用户名">
              <StandardInput
                v-model:value="formState.username"
                :noMargin="true"
                placeholder="请输入用户名"
                class="modal-input-unified"
              />
            </a-form-item>
          </template>
          <template #field-realName>
            <a-form-item label="真实姓名">
              <StandardInput
                v-model:value="formState.realName"
                :noMargin="true"
                placeholder="请输入真实姓名"
                class="modal-input-unified"
              />
            </a-form-item>
          </template>
          <template #field-nickname>
            <a-form-item label="昵称">
              <StandardInput
                v-model:value="formState.nickname"
                :noMargin="true"
                placeholder="请输入昵称"
                class="modal-input-unified"
              />
            </a-form-item>
          </template>
          <template #field-phone>
            <a-form-item label="手机号">
              <StandardInput
                v-model:value="formState.phone"
                :noMargin="true"
                placeholder="请输入手机号"
                class="modal-input-unified"
              />
            </a-form-item>
          </template>
          <template #field-email>
            <a-form-item label="邮箱">
              <StandardInput
                v-model:value="formState.email"
                :noMargin="true"
                placeholder="请输入邮箱"
                class="modal-input-unified"
              />
            </a-form-item>
          </template>
          <template #field-balance>
            <a-form-item label="初始余额">
              <a-input-number
                v-model:value="formState.balance"
                :min="0"
                :controls="false"
                placeholder="0"
                class="modal-input-unified modal-input-unified--number"
              />
            </a-form-item>
          </template>
          <template #field-role>
            <a-form-item label="角色">
              <a-select v-model:value="formState.role" class="modal-input-unified" placeholder="请选择角色">
                <a-select-option value="MEMBER">会员</a-select-option>
                <a-select-option value="COACH">教练</a-select-option>
                <a-select-option value="STAFF">员工</a-select-option>
                <a-select-option value="ADMIN">管理员</a-select-option>
              </a-select>
            </a-form-item>
          </template>
        </ConfiguredFormLayout>
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
import { useConfiguredTablePage } from '@/composables/useConfiguredTablePage';
import TableSearchToolbar from '@/components/common/TableSearchToolbar.vue';
import AdvancedFilterModal from '@/components/common/AdvancedFilterModal.vue';
import ConfiguredFormLayout from '@/components/common/ConfiguredFormLayout.vue';

// Shared Components
import StandardInput from '@/components/common/StandardInput.vue';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardTable from '@/components/common/StandardTable.vue';
import BatchDeleteButton from '@/components/common/BatchDeleteButton.vue';
import StandardModal from '@/components/common/StandardModal.vue';
import WorkspacePage from '@/components/common/WorkspacePage.vue';

const { currentStyle, loadMenuConfig } = usePageStyle();
const selectedRowKeys = ref<number[]>([]);
const isDeleteMode = ref(false);

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
  pagination,
  loading,
  textSuggestions,
  primaryFormFields,
  ensureConfig,
  loadData,
  handleSearch,
  handleReset,
  handleTableChange,
  applyAdvancedFilters,
  buildColumns,
  isFieldVisible,
} = useConfiguredTablePage<User>({
  routePath: '/user',
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

const tableLocale = {
  emptyText: '暂无符合条件的用户'
};

const roleMeta: Record<User['role'], { label: string; tone: string }> = {
  ADMIN: { label: '管理员', tone: 'admin' },
  COACH: { label: '教练', tone: 'coach' },
  MEMBER: { label: '会员', tone: 'member' },
  STAFF: { label: '员工', tone: 'staff' },
};

const baseColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 92 },
  {
    title: '用户名',
    dataIndex: 'username',
    key: 'username',
    width: 260,
    customHeaderCell: () => ({ class: 'column-username-head' }),
    customCell: () => ({ class: 'column-username-cell' }),
  },
  { title: '真实姓名', dataIndex: 'realName', key: 'realName', width: 160 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 220 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 220 },
  { title: '角色', dataIndex: 'role', key: 'role', width: 140 },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 190 },
  { title: '余额', dataIndex: 'balance', key: 'balance', width: 140 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
];
const columns = computed(() => buildColumns(baseColumns));

const rowSelection = computed(() => {
    if (!isDeleteMode.value) return null; // Only show checkboxes in delete mode
    return {
        selectedRowKeys: selectedRowKeys.value,
        onChange: (keys: number[]) => {
            selectedRowKeys.value = keys;
        }
    };
});

const getRoleLabel = (role: User['role']) => roleMeta[role]?.label || '会员';
const getRoleTone = (role: User['role']) => roleMeta[role]?.tone || 'member';
const getUserInitial = (username: string) => (username || 'U').slice(0, 1).toUpperCase();
const formatBalance = (value?: number) => Number(value || 0).toLocaleString('zh-CN', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
});

// Clear selection when mode is toggled off
watch(isDeleteMode, (newVal) => {
    if (!newVal) {
        selectedRowKeys.value = [];
    }
});

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
  ensureConfig();
  loadData();
});
</script>

<style scoped>
.user-list-page {
  --line: rgba(15, 23, 42, 0.08);
  --line-strong: rgba(15, 23, 42, 0.12);
  --text-primary: #111111;
  --text-secondary: #6b7280;
  --text-tertiary: #9ca3af;
  min-width: 0;
}

.delete-shell {
  display: flex;
  align-items: center;
}

.user-list-table :deep(.column-username-head) {
  text-align: center !important;
  padding-left: 14px !important;
  padding-right: 14px !important;
}

.user-list-table :deep(.column-username-cell) {
  text-align: center !important;
  padding-left: 14px !important;
  padding-right: 14px !important;
}

.user-list-table :deep(.ant-checkbox-inner) {
  border-radius: var(--mono-radius-xs) !important;
}

.mono-id {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.user-identity {
  display: grid;
  grid-template-columns: 24px 32px minmax(0, 1fr);
  align-items: center;
  column-gap: 14px;
  justify-content: flex-start;
  width: 180px;
  max-width: 100%;
  margin: 0 auto;
}

.user-avatar {
  grid-column: 2;
  width: 32px;
  height: 32px;
  border-radius: var(--mono-radius-sm);
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: #f7f7f5;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-transform: uppercase;
  flex-shrink: 0;
}

.user-text {
  grid-column: 3;
  display: flex;
  flex-direction: column;
  gap: 2px;
  align-items: flex-start;
  text-align: left;
  min-width: 0;
}

.user-name {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
}

.user-subcopy,
.soft-text {
  color: var(--text-secondary);
  font-size: 12px;
}

.soft-text--muted {
  color: var(--text-tertiary);
}

.balance-cell {
  display: flex;
  justify-content: center;
  width: 100%;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.delete-shell :deep(.active-jitter .anticon) {
  animation: none !important;
}

.delete-shell :deep(.trash-btn),
.delete-shell :deep(.cancel-delete-btn) {
  height: 36px;
  border: 1px solid var(--line);
  border-radius: var(--mono-radius-pill);
  background: #ffffff;
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.delete-shell :deep(.trash-btn) {
  width: 38px;
}

.delete-shell :deep(.trash-btn:hover),
.delete-shell :deep(.cancel-delete-btn:hover) {
  border-color: var(--line-strong);
  background: #ffffff;
  color: var(--text-primary);
}

.delete-shell :deep(.cancel-delete-btn) {
  margin-right: 8px;
  padding: 0 14px;
}

.delete-shell :deep(.delete-count) {
  margin-left: 10px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.user-modal-form {
  padding-top: 4px;
}

.user-modal-form :deep(.ant-form-item) {
  margin-bottom: 22px;
}

.user-modal-form :deep(.ant-form-item-control-input) {
  min-height: 44px;
}

.user-modal-form :deep(.modal-input-unified) {
  width: 100%;
}

.user-modal-form :deep(.modal-input-unified--number .ant-input-number-handler-wrap) {
  display: none;
}

@media (max-width: 768px) {
  .user-modal-form :deep(.ant-form-item) {
    margin-bottom: 18px;
  }
}

</style>

<style>
html.dark .user-list-page {
  --line: rgba(255, 255, 255, 0.08);
  --line-strong: rgba(255, 255, 255, 0.14);
  --text-primary: rgba(255, 255, 255, 0.94);
  --text-secondary: rgba(255, 255, 255, 0.68);
  --text-tertiary: rgba(255, 255, 255, 0.42);
  background: #000000;
}

html.dark .user-list-page .page-meta,
html.dark .user-list-page .mono-id,
html.dark .user-list-page .soft-text,
html.dark .user-list-page .soft-text--muted,
html.dark .user-list-page .user-subcopy {
  color: var(--text-secondary);
}

html.dark .user-list-page .user-name,
html.dark .user-list-page .balance-cell {
  color: var(--text-primary);
}

html.dark .user-list-page .delete-shell .trash-btn,
html.dark .user-list-page .delete-shell .cancel-delete-btn {
  background: rgba(255, 255, 255, 0.05) !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
  color: rgba(255, 255, 255, 0.72) !important;
}

html.dark .user-list-page .delete-shell .trash-btn:hover,
html.dark .user-list-page .delete-shell .cancel-delete-btn:hover {
  background: rgba(255, 255, 255, 0.08) !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
  color: rgba(255, 255, 255, 0.96) !important;
}

html.dark .user-list-page .delete-shell .confirm-delete-btn {
  background: rgba(255, 255, 255, 0.92) !important;
  border-color: rgba(255, 255, 255, 0.92) !important;
  color: #111111 !important;
}

html.dark .user-list-page .delete-shell .confirm-delete-btn:hover {
  background: #ffffff !important;
  border-color: #ffffff !important;
  color: #111111 !important;
}

html.dark .user-list-page .delete-shell .confirm-delete-btn[disabled] {
  background: rgba(255, 255, 255, 0.2) !important;
  border-color: rgba(255, 255, 255, 0.14) !important;
  color: rgba(255, 255, 255, 0.7) !important;
}

html.dark .user-list-page .user-avatar {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.92);
}

</style>
