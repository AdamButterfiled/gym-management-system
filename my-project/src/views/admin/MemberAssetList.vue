<template>
  <WorkspacePage class="member-assets-page" title="会员资产中心">
    <template #meta>
      <span class="page-meta asset-page-meta">统一查看会员钱包余额、会籍状态和私教课包配置，减少来回切换。</span>
    </template>

    <template #actions>
      <div class="asset-page-indicators">
        <div class="asset-page-indicator">
          <span>会员记录</span>
          <strong>{{ pagination.total }}</strong>
        </div>
        <div class="asset-page-indicator">
          <span>会籍套餐</span>
          <strong>{{ membershipPackages.length }}</strong>
        </div>
        <div class="asset-page-indicator">
          <span>私教课包</span>
          <strong>{{ privatePackages.length }}</strong>
        </div>
      </div>
    </template>

    <template #filters>
      <div class="asset-filter-bar">
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
        <span class="asset-filter-hint">支持按用户名或真实姓名快速定位会员资产。</span>
      </div>
    </template>

    <section class="workspace-subsection asset-stage">
      <div class="workspace-subsection-head">
        <div>
          <h2 class="workspace-section-title">会员资产概览</h2>
          <div class="workspace-section-sub">钱包余额、会籍与剩余私教课时统一查看。</div>
        </div>
        <div class="asset-panel-tag">实时快照</div>
      </div>
      <div class="asset-stat-strip">
        <article v-for="item in summaryCards" :key="item.label" class="asset-stat-slot">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.sub }}</small>
        </article>
      </div>
      <StandardTable
        class="asset-table asset-table--overview"
        :dataSource="assetRows"
        :columns="assetColumns"
        :pagination="pagination"
        :scroll="{ x: 1180 }"
        rowKey="userId"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }: { column: any; record: MemberAssetRow }">
          <template v-if="column.key === 'balance'">
            <span class="asset-strong-value">{{ formatMoney(record.balance) }}</span>
          </template>
          <template v-else-if="column.key === 'membership'">
            <span :class="['asset-chip', hasMembership(record.membership) ? 'asset-chip--active' : 'asset-chip--muted']">
              {{ hasMembership(record.membership) ? record.membership : '未开通' }}
            </span>
          </template>
          <template v-else-if="column.key === 'membershipEndDate'">
            {{ record.membershipEndDate || '未设置' }}
          </template>
          <template v-else-if="column.key === 'remainingPrivateSessions'">
            <span class="asset-strong-value">{{ record.remainingPrivateSessions || 0 }} 节</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <div class="asset-row-actions">
              <StandardButton type="text" size="sm" @click="openBalanceRecharge(record)">充值</StandardButton>
              <StandardButton type="text" size="sm" @click="openMembershipGrant(record)">开会籍</StandardButton>
              <StandardButton type="text" size="sm" @click="openPrivateGrant(record)">发课包</StandardButton>
            </div>
          </template>
        </template>
      </StandardTable>
    </section>

    <section class="workspace-subsection asset-catalog">
      <div class="asset-catalog-column">
        <div class="workspace-subsection-head asset-catalog-head">
          <div>
            <h2 class="workspace-section-title">会籍套餐</h2>
            <div class="workspace-section-sub">{{ membershipSummary }}</div>
          </div>
          <StandardButton type="add" icon="plus" @click="membershipVisible = true">新增套餐</StandardButton>
        </div>

        <StandardTable
          class="asset-table asset-table--package"
          :configStyle="currentStyle"
          :dataSource="membershipPackages"
          :columns="membershipColumns"
          rowKey="id"
          :pagination="false"
        >
          <template #bodyCell="{ column, record }: { column: any; record: MembershipPackage }">
            <template v-if="column.key === 'price'">
              <span class="asset-strong-value">{{ formatMoney(record.price) }}</span>
            </template>
            <template v-else-if="column.key === 'days'">
              {{ record.days }} 天
            </template>
            <template v-else-if="column.key === 'description'">
              <span class="asset-description-cell">{{ record.description || '暂无说明' }}</span>
            </template>
          </template>
        </StandardTable>
      </div>

      <div class="asset-catalog-column">
        <div class="workspace-subsection-head asset-catalog-head">
          <div>
            <h2 class="workspace-section-title">私教课包</h2>
            <div class="workspace-section-sub">{{ privateSummary }}</div>
          </div>
          <StandardButton type="add" icon="plus" @click="privateVisible = true">新增课包</StandardButton>
        </div>

        <StandardTable
          class="asset-table asset-table--package"
          :configStyle="currentStyle"
          :dataSource="privatePackages"
          :columns="privateColumns"
          rowKey="id"
          :pagination="false"
        >
          <template #bodyCell="{ column, record }: { column: any; record: PrivatePackage }">
            <template v-if="column.key === 'coachId'">
              {{ coachNameMap[record.coachId || 0] || '未绑定' }}
            </template>
            <template v-else-if="column.key === 'price'">
              <span class="asset-strong-value">{{ formatMoney(record.price) }}</span>
            </template>
            <template v-else-if="column.key === 'totalSessions'">
              {{ record.totalSessions }} 节
            </template>
            <template v-else-if="column.key === 'description'">
              <span class="asset-description-cell">{{ record.description || '暂无说明' }}</span>
            </template>
          </template>
        </StandardTable>
      </div>
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

  <StandardModal v-model:visible="membershipVisible" title="新增会籍套餐" @ok="saveMembershipPackage">
    <a-form
      :model="membershipForm"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 18 }"
      class="workspace-modal-form"
    >
      <ConfiguredFormLayout :fields="membershipFormFields">
        <template #field-membershipPackageName>
          <a-form-item label="套餐名称">
            <a-input v-model:value="membershipForm.name" />
          </a-form-item>
        </template>
        <template #field-membershipPackagePrice>
          <a-form-item label="价格">
            <a-input-number v-model:value="membershipForm.price" :min="0" style="width: 100%" />
          </a-form-item>
        </template>
        <template #field-membershipPackageDays>
          <a-form-item label="时长(天)">
            <a-input-number v-model:value="membershipForm.days" :min="1" style="width: 100%" />
          </a-form-item>
        </template>
        <template #field-membershipPackageDescription>
          <a-form-item label="说明">
            <a-textarea v-model:value="membershipForm.description" :rows="3" />
          </a-form-item>
        </template>
      </ConfiguredFormLayout>
    </a-form>
  </StandardModal>

  <StandardModal v-model:visible="privateVisible" title="新增私教课包" @ok="savePrivatePackage">
    <a-form
      :model="privateForm"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 18 }"
      class="workspace-modal-form"
    >
      <ConfiguredFormLayout :fields="privateFormFields">
        <template #field-privatePackageCoachId>
          <a-form-item label="绑定教练">
            <a-select v-model:value="privateForm.coachId">
              <a-select-option v-for="item in coachOptions" :key="item.id" :value="item.id">{{ item.name }}</a-select-option>
            </a-select>
          </a-form-item>
        </template>
        <template #field-privatePackageName>
          <a-form-item label="课包名称">
            <a-input v-model:value="privateForm.name" />
          </a-form-item>
        </template>
        <template #field-privatePackagePrice>
          <a-form-item label="价格">
            <a-input-number v-model:value="privateForm.price" :min="0" style="width: 100%" />
          </a-form-item>
        </template>
        <template #field-privatePackageTotalSessions>
          <a-form-item label="总课时">
            <a-input-number v-model:value="privateForm.totalSessions" :min="1" style="width: 100%" />
          </a-form-item>
        </template>
        <template #field-privatePackageDescription>
          <a-form-item label="说明">
            <a-textarea v-model:value="privateForm.description" :rows="3" />
          </a-form-item>
        </template>
      </ConfiguredFormLayout>
    </a-form>
  </StandardModal>

  <StandardModal v-model:visible="balanceVisible" :title="`会员充值 - ${selectedMemberLabel}`" @ok="saveBalanceRecharge">
    <a-form :model="balanceForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }" class="workspace-modal-form">
      <a-form-item label="充值金额">
        <a-input-number v-model:value="balanceForm.amount" :min="1" style="width: 100%" />
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea v-model:value="balanceForm.remark" :rows="3" />
      </a-form-item>
    </a-form>
  </StandardModal>

  <StandardModal v-model:visible="membershipGrantVisible" :title="`开通会籍 - ${selectedMemberLabel}`" @ok="saveMembershipGrant">
    <a-form :model="membershipGrantForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }" class="workspace-modal-form">
      <a-form-item label="会籍套餐">
        <a-select v-model:value="membershipGrantForm.packageId">
          <a-select-option v-for="item in membershipPackages" :key="item.id" :value="item.id">
            {{ item.name }} · {{ formatMoney(item.price) }} / {{ item.days }} 天
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea v-model:value="membershipGrantForm.remark" :rows="3" />
      </a-form-item>
    </a-form>
  </StandardModal>

  <StandardModal v-model:visible="privateGrantVisible" :title="`发放课包 - ${selectedMemberLabel}`" @ok="savePrivateGrant">
    <a-form :model="privateGrantForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }" class="workspace-modal-form">
      <a-form-item label="私教课包">
        <a-select v-model:value="privateGrantForm.packageId">
          <a-select-option v-for="item in privatePackages" :key="item.id" :value="item.id">
            {{ item.name }} · {{ coachNameMap[item.coachId || 0] || '未绑定' }} · {{ item.totalSessions }} 节
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea v-model:value="privateGrantForm.remark" :rows="3" />
      </a-form-item>
    </a-form>
  </StandardModal>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import request from '@/request';
import {
  CoachProfile,
  MemberAssetRow,
  MembershipPackage,
  PageResult,
  PrivatePackage
} from '@/types';
import { usePageStyle } from '@/hooks/usePageStyle';
import { useConfiguredTablePage } from '@/composables/useConfiguredTablePage';
import ConfiguredFormLayout from '@/components/common/ConfiguredFormLayout.vue';
import TableSearchToolbar from '@/components/common/TableSearchToolbar.vue';
import AdvancedFilterModal from '@/components/common/AdvancedFilterModal.vue';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardModal from '@/components/common/StandardModal.vue';
import StandardTable from '@/components/common/StandardTable.vue';
import WorkspacePage from '@/components/common/WorkspacePage.vue';
import { buildConfiguredColumns } from '@/utils/formConfig';

const { currentStyle, loadMenuConfig } = usePageStyle();
const membershipPackages = ref<MembershipPackage[]>([]);
const privatePackages = ref<PrivatePackage[]>([]);
const coachOptions = ref<CoachProfile[]>([]);
const membershipVisible = ref(false);
const privateVisible = ref(false);
const balanceVisible = ref(false);
const membershipGrantVisible = ref(false);
const privateGrantVisible = ref(false);
const selectedMember = ref<MemberAssetRow | null>(null);
const {
  filterableFields,
  quickSearchEnabled,
  quickSearchPlaceholder,
  keyword,
  filterLogic,
  filterRules,
  filterModalVisible,
  activeFilterCount,
  tableData: assetRows,
  pagination,
  loading,
  textSuggestions,
  ensureConfig,
  loadData,
  handleSearch,
  handleReset,
  handleTableChange,
  applyAdvancedFilters,
  buildColumns,
  getTargetFields,
} = useConfiguredTablePage<MemberAssetRow>({
  routePath: '/member-assets',
});

const membershipForm = reactive<MembershipPackage>({
  name: '',
  price: 199,
  days: 30,
  dailyLimit: 1,
  description: ''
});

const privateForm = reactive<PrivatePackage>({
  coachId: undefined,
  name: '',
  price: 999,
  totalSessions: 5,
  description: ''
});

const balanceForm = reactive({
  amount: 100,
  remark: ''
});

const membershipGrantForm = reactive({
  packageId: undefined as number | undefined,
  remark: ''
});

const privateGrantForm = reactive({
  packageId: undefined as number | undefined,
  remark: ''
});

const coachNameMap = computed<Record<number, string>>(() => {
  const map: Record<number, string> = {};
  coachOptions.value.forEach(item => {
    if (item.id) map[item.id] = item.name;
  });
  return map;
});

const selectedMemberLabel = computed(() => {
  if (!selectedMember.value) return '未选择会员';
  return selectedMember.value.realName || selectedMember.value.username;
});

const totalBalance = computed(() =>
  assetRows.value.reduce((sum, item) => sum + Number(item.balance || 0), 0)
);

const activeMembershipCount = computed(() =>
  assetRows.value.filter(item => hasMembership(item.membership)).length
);

const expiringSoonCount = computed(() => {
  const now = new Date();
  const maxWindow = 1000 * 60 * 60 * 24 * 30;

  return assetRows.value.filter(item => {
    if (!item.membershipEndDate) return false;
    const expiry = new Date(item.membershipEndDate);
    const diff = expiry.getTime() - now.getTime();
    return diff >= 0 && diff <= maxWindow;
  }).length;
});

const remainingSessionsTotal = computed(() =>
  assetRows.value.reduce((sum, item) => sum + Number(item.remainingPrivateSessions || 0), 0)
);

const summaryCards = computed(() => [
  {
    label: '当前页会员',
    value: `${assetRows.value.length}`,
    sub: pagination.total ? `共 ${pagination.total} 条资产记录` : '等待会员资产数据'
  },
  {
    label: '钱包余额',
    value: formatMoney(totalBalance.value),
    sub: assetRows.value.length
      ? `人均 ${formatMoney(totalBalance.value / assetRows.value.length)}`
      : '暂无可统计余额'
  },
  {
    label: '有效会籍',
    value: `${activeMembershipCount.value}`,
    sub: expiringSoonCount.value ? `${expiringSoonCount.value} 个会籍 30 天内到期` : '近期暂无到期会籍'
  },
  {
    label: '剩余私教课时',
    value: `${remainingSessionsTotal.value} 节`,
    sub: privatePackages.value.length ? `${privatePackages.value.length} 种课包可售` : '尚未配置私教课包'
  }
]);

const membershipSummary = computed(() => {
  if (!membershipPackages.value.length) return '会员购买时会从钱包余额扣款。';
  const minPrice = Math.min(...membershipPackages.value.map(item => Number(item.price || 0)));
  return `共 ${membershipPackages.value.length} 个套餐，最低 ${formatMoney(minPrice)}。`;
});

const privateSummary = computed(() => {
  if (!privatePackages.value.length) return '可绑定教练并发放剩余课时。';
  const totalSessions = privatePackages.value.reduce((sum, item) => sum + Number(item.totalSessions || 0), 0);
  return `共 ${privatePackages.value.length} 个课包，累计配置 ${totalSessions} 节课时。`;
});

const baseAssetColumns = [
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 100 },
  { title: '用户名', dataIndex: 'username', key: 'username', width: 140 },
  { title: '姓名', dataIndex: 'realName', key: 'realName', width: 140 },
  { title: '钱包余额', dataIndex: 'balance', key: 'balance', width: 120 },
  { title: '当前会籍', dataIndex: 'membership', key: 'membership', width: 140 },
  { title: '会籍到期', dataIndex: 'membershipEndDate', key: 'membershipEndDate', width: 140 },
  { title: '剩余私教课时', dataIndex: 'remainingPrivateSessions', key: 'remainingPrivateSessions', width: 140 },
  { title: '操作', key: 'actions', width: 220, fixed: 'right' }
];
const assetColumns = computed(() => buildColumns(baseAssetColumns));
const membershipTableFields = computed(() => getTargetFields('membership-table'));
const membershipFormFields = computed(() => getTargetFields('membership-form'));

const baseMembershipColumns = [
  { title: '名称', dataIndex: 'name', key: 'name', configKey: 'membershipPackageName' },
  { title: '价格', dataIndex: 'price', key: 'price', width: 120, configKey: 'membershipPackagePrice' },
  { title: '时长(天)', dataIndex: 'days', key: 'days', width: 120, configKey: 'membershipPackageDays' },
  { title: '说明', dataIndex: 'description', key: 'description', configKey: 'membershipPackageDescription' }
];
const membershipColumns = computed(() => buildConfiguredColumns(baseMembershipColumns, membershipTableFields.value));
const privateTableFields = computed(() => getTargetFields('private-table'));
const privateFormFields = computed(() => getTargetFields('private-form'));

const basePrivateColumns = [
  { title: '名称', dataIndex: 'name', key: 'name', configKey: 'privatePackageName' },
  { title: '教练', dataIndex: 'coachId', key: 'coachId', width: 120, configKey: 'privatePackageCoachId' },
  { title: '价格', dataIndex: 'price', key: 'price', width: 120, configKey: 'privatePackagePrice' },
  { title: '课时', dataIndex: 'totalSessions', key: 'totalSessions', width: 120, configKey: 'privatePackageTotalSessions' },
  { title: '说明', dataIndex: 'description', key: 'description', width: 220, configKey: 'privatePackageDescription' }
];
const privateColumns = computed(() => buildConfiguredColumns(basePrivateColumns, privateTableFields.value));

const formatMoney = (value: number) => {
  const amount = Number(value || 0);
  return `¥ ${amount.toLocaleString('zh-CN', {
    minimumFractionDigits: Number.isInteger(amount) ? 0 : 2,
    maximumFractionDigits: 2
  })}`;
};

const hasMembership = (value?: string) => Boolean(value && !['无', '未开通', '-', 'NONE'].includes(value));

const loadPackages = async () => {
  const [membershipRes, privateRes, coachRes] = (await Promise.all([
    request.get('/admin/membership-packages'),
    request.get('/admin/private-packages'),
    request.get('/admin/coaches/page', { params: { pageNum: 1, pageSize: 200 } })
  ])) as any[];

  if (membershipRes.code === 200) membershipPackages.value = membershipRes.data;
  if (privateRes.code === 200) privatePackages.value = privateRes.data;
  if (coachRes.code === 200) coachOptions.value = (coachRes.data as PageResult<CoachProfile>).records;
};

const openBalanceRecharge = (record: MemberAssetRow) => {
  selectedMember.value = record;
  Object.assign(balanceForm, { amount: 100, remark: '' });
  balanceVisible.value = true;
};

const openMembershipGrant = (record: MemberAssetRow) => {
  selectedMember.value = record;
  Object.assign(membershipGrantForm, {
    packageId: membershipPackages.value[0]?.id,
    remark: ''
  });
  membershipGrantVisible.value = true;
};

const openPrivateGrant = (record: MemberAssetRow) => {
  selectedMember.value = record;
  Object.assign(privateGrantForm, {
    packageId: privatePackages.value[0]?.id,
    remark: ''
  });
  privateGrantVisible.value = true;
};

const showBusinessError = (res: any, fallback: string) => {
  message.error(res?.msg || fallback);
};

const saveMembershipPackage = async () => {
  const res = (await request.post('/admin/membership-packages', membershipForm)) as any;
  if (res.code === 200) {
    message.success('会籍套餐已保存');
    membershipVisible.value = false;
    Object.assign(membershipForm, { name: '', price: 199, days: 30, dailyLimit: 1, description: '' });
    loadPackages();
  } else {
    showBusinessError(res, '会籍套餐保存失败');
  }
};

const savePrivatePackage = async () => {
  const res = (await request.post('/admin/private-packages', privateForm)) as any;
  if (res.code === 200) {
    message.success('私教课包已保存');
    privateVisible.value = false;
    Object.assign(privateForm, { coachId: undefined, name: '', price: 999, totalSessions: 5, description: '' });
    loadPackages();
  } else {
    showBusinessError(res, '私教课包保存失败');
  }
};

const saveBalanceRecharge = async () => {
  if (!selectedMember.value) return;
  if (!balanceForm.amount || Number(balanceForm.amount) <= 0) {
    message.warning('请输入有效充值金额');
    return;
  }

  const res = (await request.post(
    `/admin/member-assets/${selectedMember.value.userId}/balance-recharge`,
    balanceForm
  )) as any;
  if (res.code === 200) {
    message.success('余额已充值');
    balanceVisible.value = false;
    await loadData();
  } else {
    showBusinessError(res, '余额充值失败');
  }
};

const saveMembershipGrant = async () => {
  if (!selectedMember.value) return;
  if (!membershipGrantForm.packageId) {
    message.warning('请选择会籍套餐');
    return;
  }

  const res = (await request.post(
    `/admin/member-assets/${selectedMember.value.userId}/membership`,
    membershipGrantForm
  )) as any;
  if (res.code === 200) {
    message.success('会籍已开通');
    membershipGrantVisible.value = false;
    await loadData();
  } else {
    showBusinessError(res, '会籍开通失败');
  }
};

const savePrivateGrant = async () => {
  if (!selectedMember.value) return;
  if (!privateGrantForm.packageId) {
    message.warning('请选择私教课包');
    return;
  }

  const res = (await request.post(
    `/admin/member-assets/${selectedMember.value.userId}/private-package`,
    privateGrantForm
  )) as any;
  if (res.code === 200) {
    message.success('私教课包已发放');
    privateGrantVisible.value = false;
    await loadData();
  } else {
    showBusinessError(res, '私教课包发放失败');
  }
};

onMounted(async () => {
  loadMenuConfig();
  await ensureConfig();
  await Promise.all([loadData(), loadPackages()]);
});
</script>

<style scoped>
.member-assets-page {
  --asset-line: rgba(17, 17, 17, 0.08);
  --asset-line-strong: rgba(17, 17, 17, 0.14);
  --asset-paper: rgba(255, 255, 255, 0.94);
  --asset-paper-soft: rgba(247, 245, 239, 0.9);
  --asset-shadow: 0 16px 38px rgba(15, 23, 42, 0.035);
  --asset-indicator-bg: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(246, 244, 238, 0.88));
  --asset-stage-bg:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 246, 241, 0.94)),
    radial-gradient(circle at top right, rgba(17, 17, 17, 0.035), transparent 32%);
  --asset-tag-bg: rgba(255, 255, 255, 0.72);
  --asset-tag-text: #4f545c;
  --asset-strip-bg: linear-gradient(90deg, rgba(17, 17, 17, 0.015), transparent 18%, transparent 82%, rgba(17, 17, 17, 0.02));
  --asset-catalog-bg: linear-gradient(180deg, var(--asset-paper), var(--asset-paper-soft));
  --asset-catalog-accent: linear-gradient(180deg, rgba(17, 17, 17, 0.02), transparent 22%);
  --asset-chip-border: rgba(17, 17, 17, 0.06);
  --asset-chip-active-bg: rgba(17, 17, 17, 0.06);
  --asset-chip-active-text: #111111;
  --asset-chip-muted-bg: rgba(255, 255, 255, 0.6);
  --asset-chip-muted-text: #70757f;
}

html.theme-glass-global:not(.dark) .member-assets-page {
  --asset-line: rgba(17, 17, 17, 0.08);
  --asset-line-strong: rgba(17, 17, 17, 0.12);
  --asset-paper: #ffffff;
  --asset-paper-soft: #ffffff;
  --asset-shadow: 0 12px 30px rgba(15, 23, 42, 0.04);
  --asset-indicator-bg: #ffffff;
  --asset-stage-bg: #ffffff;
  --asset-tag-bg: #f7f7f7;
  --asset-tag-text: #6b7280;
  --asset-strip-bg: linear-gradient(90deg, rgba(17, 17, 17, 0.015), transparent 18%, transparent 82%, rgba(17, 17, 17, 0.02));
  --asset-catalog-bg: #ffffff;
  --asset-catalog-accent: linear-gradient(180deg, rgba(17, 17, 17, 0.02), transparent 22%);
  --asset-chip-border: rgba(17, 17, 17, 0.08);
  --asset-chip-active-bg: rgba(17, 17, 17, 0.04);
  --asset-chip-active-text: #111111;
  --asset-chip-muted-bg: #f7f7f7;
  --asset-chip-muted-text: #6b7280;
}

html.dark .member-assets-page {
  --asset-line: rgba(255, 255, 255, 0.08);
  --asset-line-strong: rgba(255, 255, 255, 0.16);
  --asset-paper: #1f1f1f;
  --asset-paper-soft: #181818;
  --asset-shadow: 0 22px 48px rgba(0, 0, 0, 0.32);
  --asset-indicator-bg: #1f1f1f;
  --asset-stage-bg: #111111;
  --asset-tag-bg: rgba(255, 255, 255, 0.08);
  --asset-tag-text: rgba(255, 255, 255, 0.72);
  --asset-strip-bg: linear-gradient(90deg, rgba(255, 255, 255, 0.04), transparent 18%, transparent 82%, rgba(255, 255, 255, 0.06));
  --asset-catalog-bg: #1f1f1f;
  --asset-catalog-accent: linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 22%);
  --asset-chip-border: rgba(255, 255, 255, 0.08);
  --asset-chip-active-bg: rgba(255, 255, 255, 0.12);
  --asset-chip-active-text: rgba(255, 255, 255, 0.94);
  --asset-chip-muted-bg: rgba(255, 255, 255, 0.06);
  --asset-chip-muted-text: rgba(255, 255, 255, 0.62);
}

.asset-page-meta {
  max-width: 520px;
  white-space: normal;
  line-height: 1.6;
}

.asset-page-indicators {
  display: inline-flex;
  align-items: stretch;
  flex-wrap: wrap;
  gap: 0;
  padding: 6px 4px;
  border: 1px solid var(--asset-line);
  border-radius: var(--mono-radius-xl);
  background: var(--asset-indicator-bg);
}

html.theme-glass-global:not(.dark) .member-assets-page .asset-page-indicators,
html.theme-glass-global:not(.dark) .member-assets-page .asset-stage,
html.theme-glass-global:not(.dark) .member-assets-page .asset-catalog,
html.theme-glass-global:not(.dark) .member-assets-page .asset-panel-tag {
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

.asset-page-indicator {
  min-width: 112px;
  padding: 10px 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.asset-page-indicator + .asset-page-indicator {
  border-left: 1px solid var(--asset-line);
}

.asset-page-indicator span {
  color: var(--mono-text-tertiary);
  font-size: 12px;
  line-height: 1;
  letter-spacing: 0.04em;
}

.asset-page-indicator strong {
  color: var(--mono-text);
  font-size: 24px;
  line-height: 1;
  font-weight: 700;
  letter-spacing: -0.04em;
}

.asset-filter-bar {
  width: 100%;
  padding: 6px 0 16px;
  border-bottom: 1px solid var(--asset-line);
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.asset-filter-hint {
  margin-left: auto;
  color: var(--mono-text-tertiary);
  font-size: 13px;
  line-height: 1.6;
}

.asset-stage {
  padding: 28px 28px 24px;
  border: 1px solid var(--asset-line);
  border-radius: var(--mono-radius-xl);
  background: var(--asset-stage-bg);
  box-shadow: var(--asset-shadow);
}

.asset-panel-tag {
  padding: 8px 12px;
  border: 1px solid var(--asset-line);
  border-radius: var(--mono-radius-pill);
  background: var(--asset-tag-bg);
  color: var(--asset-tag-text);
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}

.asset-stat-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  margin: 20px 0 18px;
  border-top: 1px solid var(--asset-line);
  border-bottom: 1px solid var(--asset-line);
  background: var(--asset-strip-bg);
}

.asset-stat-slot {
  min-height: 120px;
  padding: 18px 20px 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
}

.asset-stat-slot + .asset-stat-slot {
  border-left: 1px solid var(--asset-line);
}

.asset-stat-slot span {
  color: var(--mono-text-tertiary);
  font-size: 12px;
  line-height: 1;
  letter-spacing: 0.04em;
}

.asset-stat-slot strong {
  color: var(--mono-text);
  font-size: 28px;
  line-height: 1;
  font-weight: 700;
  letter-spacing: -0.05em;
}

.asset-stat-slot small {
  color: var(--mono-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.asset-catalog {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(0, 0.92fr);
  gap: 0;
  border: 1px solid var(--asset-line);
  border-radius: var(--mono-radius-xl);
  background: var(--asset-catalog-bg);
  box-shadow: var(--asset-shadow);
  overflow: hidden;
}

.asset-catalog-column {
  min-width: 0;
  padding: 26px 28px 24px;
}

.asset-catalog-column + .asset-catalog-column {
  border-left: 1px solid var(--asset-line);
  background: var(--asset-catalog-accent);
}

.asset-catalog-head {
  margin-bottom: 18px;
}

.asset-table {
  margin-top: 4px;
}

.asset-strong-value {
  font-weight: 600;
  letter-spacing: -0.02em;
}

.asset-row-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.asset-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: var(--mono-radius-pill);
  border: 1px solid var(--asset-chip-border);
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

.asset-chip--active {
  background: var(--asset-chip-active-bg);
  color: var(--asset-chip-active-text);
}

.asset-chip--muted {
  background: var(--asset-chip-muted-bg);
  color: var(--asset-chip-muted-text);
}

.asset-description-cell {
  display: block;
  color: var(--mono-text-secondary);
  line-height: 1.7;
  text-align: left;
}

.asset-table--overview :deep(.ant-table-tbody > tr > td) {
  padding-top: 18px !important;
  padding-bottom: 18px !important;
}

.asset-table--package :deep(.ant-table-tbody > tr > td) {
  padding-top: 16px !important;
  padding-bottom: 16px !important;
}

.asset-table--package :deep(.ant-table-thead > tr > th),
.asset-table--package :deep(.ant-table-tbody > tr > td) {
  font-size: 13px !important;
}

.asset-catalog :deep(.ant-table-tbody > tr:last-child > td),
.asset-stage :deep(.ant-table-tbody > tr:last-child > td) {
  border-bottom-color: transparent !important;
}

@media (max-width: 1280px) {
  .asset-stat-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .asset-stat-slot {
    border-left: 1px solid var(--asset-line);
  }

  .asset-stat-slot:nth-child(2n + 1) {
    border-left: none;
  }

  .asset-stat-slot:nth-child(n + 3) {
    border-top: 1px solid var(--asset-line);
  }
}

@media (max-width: 960px) {
  .asset-filter-bar,
  .asset-page-indicators {
    align-items: stretch;
  }

  .asset-page-indicators {
    display: flex;
    width: 100%;
  }

  .asset-page-indicator {
    flex: 1 1 0;
  }

  .asset-filter-hint {
    margin-left: 0;
  }

  .asset-catalog,
  .asset-stat-strip {
    grid-template-columns: minmax(0, 1fr);
  }

  .asset-stat-slot,
  .asset-catalog-column + .asset-catalog-column {
    border-left: none;
  }

  .asset-stat-slot + .asset-stat-slot,
  .asset-catalog-column + .asset-catalog-column {
    border-top: 1px solid var(--asset-line);
  }

  .asset-stage,
  .asset-catalog-column {
    padding: 20px 18px;
  }
}
</style>
