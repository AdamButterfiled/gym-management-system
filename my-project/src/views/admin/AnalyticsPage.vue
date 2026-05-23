<template>
  <WorkspacePage title="经营分析中心">
    <template #meta>
      <span class="page-meta">围绕收入、预约、签到与教练工作量的综合看板。</span>
    </template>

    <template #actions>
      <StandardButton type="search" icon="reload" @click="loadData">刷新数据</StandardButton>
    </template>

    <section class="workspace-subsection">
      <div class="analytics-grid">
        <section class="analytics-panel">
          <div class="workspace-subsection-head analytics-panel-head">
            <div>
              <h2 class="workspace-section-title">近 7 日收入趋势</h2>
              <div class="workspace-section-sub">收入走势按日聚合展示。</div>
            </div>
          </div>
          <div class="line-chart">
            <svg viewBox="0 0 640 260" preserveAspectRatio="none">
              <polyline
                :points="polylinePoints"
                fill="none"
                :stroke="chartStroke"
                stroke-width="4"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <circle
                v-for="(point, index) in pointList"
                :key="index"
                :cx="point.x"
                :cy="point.y"
                r="5"
                :fill="chartPointFill"
                :stroke="chartPointStroke"
                stroke-width="2"
              />
            </svg>
          </div>
          <div class="legend-row">
            <div v-for="(day, index) in analytics.days" :key="day" class="legend-item">
              <strong>{{ day.slice(5) }}</strong>
              <span>¥ {{ Number(analytics.revenues[index] || 0).toFixed(2) }}</span>
            </div>
          </div>
        </section>

        <section class="analytics-panel">
          <div class="workspace-subsection-head analytics-panel-head">
            <div>
              <h2 class="workspace-section-title">关键转化指标</h2>
              <div class="workspace-section-sub">核心经营指标的当前快照。</div>
            </div>
          </div>
          <div class="metric-list">
            <div class="metric-item">
              <span>总营收</span>
              <strong>¥ {{ overview.totalRevenue.toFixed(2) }}</strong>
            </div>
            <div class="metric-item">
              <span>今日营收</span>
              <strong>¥ {{ overview.todayRevenue.toFixed(2) }}</strong>
            </div>
            <div class="metric-item">
              <span>签到率</span>
              <strong>{{ overview.checkinRate }}%</strong>
            </div>
            <div class="metric-item">
              <span>场馆利用率</span>
              <strong>{{ overview.venueUsageRate }}%</strong>
            </div>
          </div>
        </section>
      </div>
    </section>

    <section class="workspace-subsection">
      <div class="workspace-subsection-head">
        <div>
          <h2 class="workspace-section-title">教练工作量拆分</h2>
          <div class="workspace-section-sub">拆分私教预约数与团课排期数。</div>
        </div>
      </div>
      <StandardTable :dataSource="analytics.coachWorkload" :columns="columns" :pagination="false" rowKey="coachName" />
    </section>
  </WorkspacePage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useStore } from 'vuex';
import request from '@/request';
import { AdminOverview } from '@/types';
import { fetchRuntimeFormConfig } from '@/api/formConfig';
import StandardButton from '@/components/common/StandardButton.vue';
import StandardTable from '@/components/common/StandardTable.vue';
import WorkspacePage from '@/components/common/WorkspacePage.vue';
import type { FormPageConfig } from '@/types/formConfig';
import { buildConfiguredColumns } from '@/utils/formConfig';
import { findFormConfigManifestPage } from '@/utils/formConfigManifest';
import { normalizePageConfig, resolveTableTarget } from '@/utils/formConfigDesigner';

const store = useStore();
const overview = reactive<AdminOverview>({
  memberCount: 0,
  coachCount: 0,
  todayBookings: 0,
  pendingCoachApprovals: 0,
  totalRevenue: 0,
  todayRevenue: 0,
  venueUsageRate: 0,
  courseAttendanceRate: 0,
  checkinRate: 0
});

const analytics = reactive<{
  days: string[];
  revenues: number[];
  bookings: number[];
  coachWorkload: Array<{ coachName: string; privateLessons: number; groupLessons: number }>;
}>({
  days: [],
  revenues: [],
  bookings: [],
  coachWorkload: []
});
const runtimeConfig = ref<FormPageConfig | null>(null);

const maxRevenue = computed(() => Math.max(...analytics.revenues, 1));
const isDark = computed(() => store.state.themeSettings?.isDark ?? false);
const chartStroke = computed(() => (isDark.value ? 'rgba(255, 255, 255, 0.92)' : '#111111'));
const chartPointFill = computed(() => (isDark.value ? '#111111' : '#ffffff'));
const chartPointStroke = computed(() => chartStroke.value);
const pointList = computed(() => analytics.revenues.map((value, index) => ({
  x: 40 + index * 90,
  y: 220 - (Number(value || 0) / maxRevenue.value) * 180
})));
const polylinePoints = computed(() => pointList.value.map(item => `${item.x},${item.y}`).join(' '));
const tableFields = computed(() => resolveTableTarget(runtimeConfig.value)?.fields || []);

const baseColumns = [
  { title: '教练', dataIndex: 'coachName', key: 'coachName' },
  { title: '私教预约数', dataIndex: 'privateLessons', key: 'privateLessons' },
  { title: '团课排期数', dataIndex: 'groupLessons', key: 'groupLessons' }
];
const columns = computed(() => buildConfiguredColumns(baseColumns, tableFields.value));

const loadRuntimeConfig = async () => {
  const manifest = findFormConfigManifestPage('/analytics');
  const fallback: FormPageConfig = {
    pageKey: manifest?.pageKey || 'analytics',
    routePath: manifest?.routePath || '/analytics',
    pageTitle: manifest?.pageTitle || '经营分析中心',
    enabled: true,
    quickSearchFields: [],
    defaultFilterLogic: 'AND',
    fields: [],
    targets: [],
  };

  try {
    const config = await fetchRuntimeFormConfig('/analytics');
    runtimeConfig.value = normalizePageConfig(config, manifest);
  } catch (error) {
    runtimeConfig.value = normalizePageConfig(fallback, manifest);
  }
};

const loadData = async () => {
  const [overviewRes, analyticsRes] = await Promise.allSettled([
    request.get('/admin/overview'),
    request.get('/admin/analytics')
  ]);
  if (overviewRes.status === 'fulfilled' && overviewRes.value.code === 200) Object.assign(overview, overviewRes.value.data);
  if (analyticsRes.status === 'fulfilled' && analyticsRes.value.code === 200) Object.assign(analytics, analyticsRes.value.data);
};

onMounted(() => {
  void loadRuntimeConfig();
  void loadData();
});
</script>

<style scoped>
.analytics-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.85fr);
  gap: 28px;
}

.analytics-panel {
  min-width: 0;
}

.analytics-panel-head {
  margin-bottom: 4px;
}

.line-chart {
  height: 280px;
  margin-top: 12px;
}

.line-chart svg {
  width: 100%;
  height: 100%;
}

.legend-row {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.legend-item {
  padding-top: 12px;
  border-top: 1px solid var(--mono-line);
}

.legend-item strong {
  display: block;
  color: var(--mono-text);
}

.legend-item span {
  color: var(--mono-text-secondary);
  font-size: 13px;
}

.metric-list {
  display: grid;
  gap: 14px;
  margin-top: 12px;
}

.metric-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid var(--mono-line);
}

.metric-item span {
  color: var(--mono-text-secondary);
}

.metric-item strong {
  font-size: 28px;
  color: var(--mono-text);
}

@media (max-width: 1100px) {
  .analytics-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .legend-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
