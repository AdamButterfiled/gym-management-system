<template>
  <WorkspacePage class="dashboard-shell" title="工作台">
    <template #actions>
      <div class="header-summary">
        <div class="revenue-brief">
          <div class="revenue-brief-label">今日营收</div>
          <div class="revenue-brief-value dashboard-art-number">{{ formatMoney(overview.todayRevenue, 2) }}</div>
          <div class="revenue-brief-sub">总营收 <span
              class="dashboard-art-number">{{ formatMoney(overview.totalRevenue, 2) }}</span></div>
        </div>
      </div>
    </template>

    <section class="hero-panel">
      <div class="hero-main">
        <h2 class="hero-title dashboard-art-text">今日经营总览</h2>

        <div class="hero-pill-row">
          <div v-for="pill in heroPills" :key="pill.label" class="hero-pill">
            <span class="dashboard-art-text">{{ pill.label }}</span>
            <strong class="dashboard-art-number">{{ pill.value }}</strong>
          </div>
        </div>
      </div>

      <div class="hero-side">
        <div class="health-meter" :style="{ '--health-progress': `${businessHealth}%` }">
          <div class="health-meter-inner">
            <small class="dashboard-art-text">运营温度</small>
            <strong class="dashboard-art-number">{{ businessHealth }}</strong>
            <span class="dashboard-art-text">{{ businessHealthLabel }}</span>
          </div>
        </div>

        <div class="hero-side-grid">
          <div class="hero-side-card">
            <span class="dashboard-art-text">今日预约</span>
            <strong class="dashboard-art-number">{{ formatCount(overview.todayBookings) }}</strong>
          </div>
          <div class="hero-side-card">
            <span class="dashboard-art-text">待审批私教</span>
            <strong class="dashboard-art-number">{{ formatCount(overview.pendingCoachApprovals) }}</strong>
          </div>
          <div class="hero-side-card">
            <span class="dashboard-art-text">峰值日</span>
            <strong class="dashboard-art-number">{{ peakDayLabel }}</strong>
          </div>
          <div class="hero-side-card">
            <span class="dashboard-art-text">主力教练</span>
            <strong class="dashboard-art-text">{{ strongestCoach?.coachName || '--' }}</strong>
          </div>
        </div>
      </div>
    </section>

    <section class="workspace-subsection">
      <div class="kpi-grid">
        <article v-for="card in statCards" :key="card.title" class="kpi-card">
          <div class="kpi-icon" :class="`tone-${card.tone}`">
            <component :is="card.icon"/>
          </div>
          <div class="kpi-body">
            <div class="kpi-title dashboard-art-text">{{ card.title }}</div>
            <div class="kpi-value dashboard-art-number">{{ card.value }}</div>
          </div>
        </article>
      </div>
    </section>

    <section class="workspace-subsection">
      <div class="dashboard-main-grid">
        <div class="dashboard-insights-grid">
          <div class="panel panel--workload">
            <div class="workspace-subsection-head">
              <h2 class="workspace-section-title dashboard-art-text">教练负载分布</h2>
            </div>

            <div class="workload-legend">
              <span><i class="legend-dot legend-dot--private"></i>私教</span>
              <span><i class="legend-dot legend-dot--group"></i>团课</span>
            </div>

            <div v-if="analytics.coachWorkload.length" class="workload-list">
              <div v-for="item in analytics.coachWorkload" :key="item.coachName" class="workload-item">
                <div class="workload-head">
                  <div class="workload-name-group">
                    <strong class="dashboard-art-text">{{ item.coachName }}</strong>
                    <span>{{ workloadStatus(item) }}</span>
                  </div>
                  <div class="workload-count"><span class="dashboard-art-number">{{ item.privateLessons }}</span> 私教 /
                    <span class="dashboard-art-number">{{ item.groupLessons }}</span> 团课
                  </div>
                </div>

                <div class="workload-track">
                  <div class="workload-composite" :style="{ width: workloadTotalWidth(item) }">
                    <div class="workload-segment workload-segment--private"
                         :style="{ width: workloadPrivateRatio(item) }"></div>
                    <div class="workload-segment workload-segment--group"
                         :style="{ width: workloadGroupRatio(item) }"></div>
                  </div>
                </div>
              </div>
            </div>

            <div v-else class="workload-empty">
              暂无教练任务。生成私教排班或团课排期后，这里会显示负载分布与高峰人员。
            </div>
          </div>

          <div class="panel panel--signals">
            <div class="workspace-subsection-head">
              <h2 class="workspace-section-title dashboard-art-text">经营信号</h2>
            </div>

            <div class="signal-grid">
              <div v-for="item in signalCards" :key="item.label" class="signal-card">
                <span class="dashboard-art-text">{{ item.label }}</span>
                <strong class="dashboard-art-number">{{ item.value }}</strong>
              </div>
            </div>
          </div>
        </div>

        <div class="panel panel--chart">
          <div class="workspace-subsection-head">
            <h2 class="workspace-section-title dashboard-art-text">营收与预约趋势</h2>
          </div>

          <RevenueBookingChart
              :days="analytics.days"
              :revenues="analytics.revenues"
              :bookings="analytics.bookings"
          />
        </div>
      </div>
    </section>

    <section class="workspace-subsection">
      <div class="dashboard-bottom-grid">
        <div class="panel panel--quick">
          <div class="workspace-subsection-head">
            <h2 class="workspace-section-title dashboard-art-text">快捷入口</h2>
          </div>

          <div class="quick-links">
            <StandardButton
                v-for="link in quickLinks"
                :key="link.path"
                type="ghost"
                :wrap-label="false"
                class="quick-link"
                @click="goTo(link.path)"
            >
              <div class="quick-link-icon">
                <component :is="link.icon"/>
              </div>
              <div class="quick-link-copy">
                <span class="dashboard-art-text">{{ link.title }}</span>
              </div>
              <div class="quick-link-arrow">
                <RightOutlined/>
              </div>
            </StandardButton>
          </div>
        </div>

        <div class="panel panel--focus">
          <div class="workspace-subsection-head">
            <h2 class="workspace-section-title dashboard-art-text">重点数据</h2>
          </div>

          <div class="focus-metrics">
            <div v-for="item in spotlightCards" :key="item.label" class="focus-metric-card">
              <span class="dashboard-art-text">{{ item.label }}</span>
              <strong class="dashboard-art-number">{{ item.value }}</strong>
            </div>
          </div>
        </div>
      </div>
    </section>
  </WorkspacePage>
</template>

<script setup lang="ts">
import {
  BarChartOutlined,
  CalendarOutlined,
  EnvironmentOutlined,
  RightOutlined,
  RiseOutlined,
  TeamOutlined,
  ThunderboltOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons-vue';
import {computed, onMounted, reactive, type Component} from 'vue';
import {useRouter} from 'vue-router';
import request from '@/request';
import {AdminOverview} from '@/types';
import RevenueBookingChart from '@/components/common/RevenueBookingChart.vue';
import StandardButton from '@/components/common/StandardButton.vue';
import WorkspacePage from '@/components/common/WorkspacePage.vue';

type ToneName = 'amber' | 'ink' | 'olive' | 'coral';

interface DashboardCard {
  title: string;
  value: string;
  icon: Component;
  tone: ToneName;
}

interface SignalCard {
  label: string;
  value: string;
}

interface QuickLink {
  title: string;
  path: string;
  icon: Component;
}

interface SpotlightCard {
  label: string;
  value: string;
}

const router = useRouter();

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

const integerFormatter = new Intl.NumberFormat('zh-CN', {maximumFractionDigits: 0});

const formatCount = (value: number) => integerFormatter.format(Number.isFinite(value) ? value : 0);

const formatMoney = (value: number, digits = 0) => {
  const safeValue = Number.isFinite(value) ? value : 0;
  const formatter = new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  });
  return `¥ ${formatter.format(safeValue)}`;
};

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value));

const weeklyRevenue = computed(() => analytics.revenues.reduce((sum, value) => sum + Number(value || 0), 0));
const weeklyBookings = computed(() => analytics.bookings.reduce((sum, value) => sum + Number(value || 0), 0));
const avgOrderValue = computed(() => (weeklyBookings.value ? weeklyRevenue.value / weeklyBookings.value : 0));
const activeCoachCount = computed(() => analytics.coachWorkload.filter(item => item.privateLessons + item.groupLessons > 0).length);

const peakRevenueIndex = computed(() => {
  if (!analytics.revenues.length) return -1;
  const maxRevenue = Math.max(...analytics.revenues, 0);
  if (maxRevenue <= 0) return -1;
  return analytics.revenues.findIndex(item => Number(item || 0) === maxRevenue);
});

const peakDayLabel = computed(() => {
  if (peakRevenueIndex.value < 0) return '--';
  const day = analytics.days[peakRevenueIndex.value];
  return day ? day.slice(5) : '--';
});

const strongestCoach = computed(() => {
  if (!analytics.coachWorkload.length) return null;

  return analytics.coachWorkload
      .map(item => ({...item, total: item.privateLessons + item.groupLessons}))
      .sort((left, right) => right.total - left.total)[0] || null;
});

const businessHealth = computed(() => {
  const base = (overview.venueUsageRate + overview.courseAttendanceRate + overview.checkinRate) / 3;
  const bookingBoost = Math.min(18, weeklyBookings.value * 2.5);
  const approvalPenalty = Math.min(18, overview.pendingCoachApprovals * 6);
  return clamp(Math.round(base * 0.78 + bookingBoost - approvalPenalty + 18), 0, 100);
});

const businessHealthLabel = computed(() => {
  if (businessHealth.value >= 78) return '节奏良好';
  if (businessHealth.value >= 55) return '可继续优化';
  return '需要提升转化';
});

const heroPills = computed(() => [
  {label: '活跃会员', value: formatCount(overview.memberCount)},
  {label: '在岗教练', value: formatCount(overview.coachCount)},
  {label: '平均客单价', value: formatMoney(avgOrderValue.value)},
  {label: '场馆利用率', value: `${overview.venueUsageRate}%`}
]);

const statCards = computed<DashboardCard[]>(() => [
  {
    title: '会员总数',
    value: formatCount(overview.memberCount),
    icon: TeamOutlined,
    tone: 'amber'
  },
  {
    title: '教练人数',
    value: formatCount(overview.coachCount),
    icon: UserSwitchOutlined,
    tone: 'ink'
  },
  {
    title: '今日预约',
    value: formatCount(overview.todayBookings),
    icon: CalendarOutlined,
    tone: 'olive'
  },
  {
    title: '待审批私教',
    value: formatCount(overview.pendingCoachApprovals),
    icon: ThunderboltOutlined,
    tone: 'coral'
  }
]);

const signalCards = computed<SignalCard[]>(() => [
  {
    label: '场馆利用率',
    value: `${overview.venueUsageRate}%`
  },
  {
    label: '团课上座率',
    value: `${overview.courseAttendanceRate}%`
  },
  {
    label: '签到转化率',
    value: `${overview.checkinRate}%`
  },
  {
    label: '平均客单价',
    value: formatMoney(avgOrderValue.value)
  },
  {
    label: '峰值日',
    value: peakDayLabel.value
  },
  {
    label: '活跃教练',
    value: formatCount(activeCoachCount.value)
  }
]);

const quickLinks = computed<QuickLink[]>(() => [
  {title: '场馆资源', path: '/venue', icon: EnvironmentOutlined},
  {title: '团课排期', path: '/course', icon: CalendarOutlined},
  {title: '私教排班', path: '/private-schedule', icon: UserSwitchOutlined},
  {title: '预约订单', path: '/reservation', icon: RiseOutlined},
  {title: '会员资产', path: '/member-assets', icon: TeamOutlined},
  {title: '经营分析', path: '/analytics', icon: BarChartOutlined},
]);

const spotlightCards = computed<SpotlightCard[]>(() => [
  {label: '近 7 日营收', value: formatMoney(weeklyRevenue.value)},
  {label: '平均客单价', value: formatMoney(avgOrderValue.value)},
  {label: '峰值日', value: peakDayLabel.value},
  {label: '活跃教练', value: formatCount(activeCoachCount.value)}
]);

const totalTasks = (item: { privateLessons: number; groupLessons: number }) => item.privateLessons + item.groupLessons;

const workloadMax = computed(() => Math.max(...analytics.coachWorkload.map(item => totalTasks(item)), 1));

const workloadTotalWidth = (item: { privateLessons: number; groupLessons: number }) => {
  const total = totalTasks(item);
  return `${Math.max((total / workloadMax.value) * 100, total > 0 ? 18 : 0)}%`;
};

const workloadPrivateRatio = (item: { privateLessons: number; groupLessons: number }) => {
  const total = totalTasks(item);
  return `${total ? (item.privateLessons / total) * 100 : 0}%`;
};

const workloadGroupRatio = (item: { privateLessons: number; groupLessons: number }) => {
  const total = totalTasks(item);
  return `${total ? (item.groupLessons / total) * 100 : 0}%`;
};

const workloadStatus = (item: { privateLessons: number; groupLessons: number }) => {
  const total = totalTasks(item);

  if (total === 0) return '暂无任务';
  if (total >= workloadMax.value * 0.72) return '高负载';
  if (total >= workloadMax.value * 0.42) return '负载均衡';
  return '可继续承接';
};

const goTo = (path: string) => {
  void router.push(path);
};

const normalizeNumberList = (values: unknown) => (
    Array.isArray(values)
        ? values.map(value => {
          const normalized = Number(value ?? 0);
          return Number.isFinite(normalized) ? normalized : 0;
        })
        : []
);

const loadData = async () => {
  const [overviewRes, analyticsRes] = await Promise.allSettled([
    request.get('/admin/overview'),
    request.get('/admin/analytics')
  ]);

  if (overviewRes.status === 'fulfilled' && overviewRes.value.code === 200) {
    Object.assign(overview, overviewRes.value.data);
  }

  if (analyticsRes.status === 'fulfilled' && analyticsRes.value.code === 200) {
    const nextAnalytics = analyticsRes.value.data || {};
    Object.assign(analytics, {
      ...nextAnalytics,
      days: Array.isArray(nextAnalytics.days) ? nextAnalytics.days : [],
      revenues: normalizeNumberList(nextAnalytics.revenues),
      bookings: normalizeNumberList(nextAnalytics.bookings),
      coachWorkload: Array.isArray(nextAnalytics.coachWorkload) ? nextAnalytics.coachWorkload : []
    });
  }
};

onMounted(() => {
  void loadData();
});
</script>

<style scoped>
.dashboard-shell {
  position: relative;
  padding: 24px;
  border: 1px solid rgba(17, 17, 17, 0.08);
  border-radius: var(--mono-radius-xl);
  background: radial-gradient(circle at top left, rgba(184, 135, 73, 0.18), transparent 28%),
  radial-gradient(circle at top right, rgba(17, 17, 17, 0.06), transparent 24%),
  linear-gradient(180deg, #fcfbf7 0%, #f4f1e9 100%);
  box-shadow: 0 24px 52px rgba(35, 23, 10, 0.08);
  overflow: hidden;
}

.dashboard-shell::before {
  content: '';
  position: absolute;
  inset: auto -120px -180px auto;
  width: 340px;
  height: 340px;
  border-radius: var(--mono-radius-pill);
  background: radial-gradient(circle, rgba(184, 135, 73, 0.12), transparent 66%);
  pointer-events: none;
}

.dashboard-art-text,
.dashboard-art-number {
  font-synthesis: none;
  text-rendering: geometricPrecision;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.dashboard-art-number {
  font-variant-numeric: tabular-nums lining-nums;
  font-feature-settings: 'tnum' 1, 'lnum' 1;
}

.header-summary {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.revenue-brief {
  min-width: 176px;
  padding: 12px 16px;
  border: 1px solid rgba(17, 17, 17, 0.08);
  border-radius: var(--mono-radius-lg);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
  text-align: right;
  backdrop-filter: blur(12px);
}

.revenue-brief-label {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.45;
  font-weight: 500;
}

.revenue-brief-value {
  margin-top: 6px;
  color: #111111;
  font-size: 30px;
  line-height: 1;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.revenue-brief-sub {
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.45;
  font-weight: 500;
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.9fr);
  gap: 20px;
  padding: 24px;
  border-radius: var(--mono-radius-xl);
  background: radial-gradient(circle at 92% 8%, rgba(243, 212, 168, 0.14), transparent 22%),
  linear-gradient(135deg, #161310 0%, #2a2119 52%, #6b482c 118%);
  color: #ffffff;
  box-shadow: 0 28px 56px rgba(31, 22, 14, 0.22);
  animation: fade-lift 0.7s ease both;
}

.hero-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-title {
  max-width: 720px;
  margin: 0;
  font-size: 34px;
  line-height: 1.1;
  font-weight: 700;
  letter-spacing: -0.05em;
}

.hero-pill-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.hero-pill {
  padding: 14px 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: var(--mono-radius-lg);
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(14px);
}

.hero-pill span,
.hero-side-card span {
  display: block;
  color: rgba(255, 255, 255, 0.76);
  font-size: 13px;
  line-height: 1.45;
  font-weight: 500;
}

.hero-pill strong,
.hero-side-card strong {
  display: block;
  margin-top: 8px;
  color: #ffffff;
  font-size: 26px;
  line-height: 1;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.hero-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.health-meter {
  --health-progress: 0%;
  position: relative;
  display: grid;
  place-items: center;
  min-height: 204px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: var(--mono-radius-lg);
  background: radial-gradient(circle at center, rgba(255, 255, 255, 0.12), transparent 58%),
  rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(14px);
}

.health-meter::before {
  content: '';
  width: 170px;
  height: 170px;
  border-radius: var(--mono-radius-pill);
  background: conic-gradient(from 210deg, #f3d4a8 0 var(--health-progress), rgba(255, 255, 255, 0.08) var(--health-progress) 100%);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.06);
}

.health-meter::after {
  content: '';
  position: absolute;
  width: 126px;
  height: 126px;
  border-radius: var(--mono-radius-pill);
  background: linear-gradient(180deg, rgba(18, 18, 18, 0.96), rgba(42, 31, 23, 0.92));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.health-meter-inner {
  position: absolute;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.health-meter-inner small {
  color: rgba(255, 255, 255, 0.74);
  font-size: 13px;
  line-height: 1.45;
  font-weight: 500;
}

.health-meter-inner strong {
  font-size: 40px;
  line-height: 1;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.health-meter-inner span {
  color: rgba(255, 255, 255, 0.84);
  font-size: 13px;
  line-height: 1.45;
  font-weight: 500;
}

.hero-side-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.hero-panel + .workspace-subsection {
  margin-top: 28px;
}

.hero-side-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 102px;
  padding: 14px 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: var(--mono-radius-lg);
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(12px);
}

.workspace-subsection + .workspace-subsection {
  margin-top: 24px;
}

.workspace-subsection-head {
  margin-bottom: 12px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.kpi-card,
.panel {
  position: relative;
  overflow: hidden;
}

.kpi-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border: 1px solid rgba(17, 17, 17, 0.08);
  border-radius: var(--mono-radius-lg);
  background: radial-gradient(circle at top right, rgba(184, 135, 73, 0.12), transparent 30%),
  rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(14px);
  box-shadow: 0 16px 36px rgba(35, 23, 10, 0.05);
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
  animation: fade-lift 0.78s ease both;
}

.kpi-card:nth-child(2) {
  animation-delay: 0.05s;
}

.kpi-card:nth-child(3) {
  animation-delay: 0.1s;
}

.kpi-card:nth-child(4) {
  animation-delay: 0.15s;
}

.kpi-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 42px rgba(35, 23, 10, 0.08);
  border-color: rgba(17, 17, 17, 0.12);
}

.kpi-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: var(--mono-radius-md);
  font-size: 18px;
  flex-shrink: 0;
}

.tone-amber {
  background: rgba(173, 117, 58, 0.12);
  color: #9b6029;
}

.tone-ink {
  background: rgba(17, 17, 17, 0.08);
  color: #111111;
}

.tone-olive {
  background: rgba(113, 123, 72, 0.12);
  color: #66723c;
}

.tone-coral {
  background: rgba(180, 88, 61, 0.12);
  color: #9b5036;
}

.kpi-body {
  min-width: 0;
  flex: 1;
}

.kpi-title,
.signal-card span,
.focus-metric-card span {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.45;
  font-weight: 500;
}

.kpi-value {
  margin-top: 10px;
  color: #111111;
  font-size: 34px;
  line-height: 1;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.dashboard-main-grid,
.dashboard-bottom-grid {
  display: grid;
  gap: 20px;
}

.dashboard-main-grid {
  grid-template-columns: minmax(0, 1fr);
}

.dashboard-bottom-grid {
  grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
}

.dashboard-insights-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(320px, 0.92fr);
  gap: 20px;
  align-items: stretch;
}

.panel {
  padding: 20px;
  border: 1px solid rgba(17, 17, 17, 0.08);
  border-radius: var(--mono-radius-xl);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(248, 245, 239, 0.86)),
  radial-gradient(circle at top right, rgba(184, 135, 73, 0.1), transparent 28%);
  box-shadow: 0 18px 40px rgba(35, 23, 10, 0.06);
  animation: fade-lift 0.82s ease both;
}

.panel::after,
.kpi-card::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  border: 1px solid rgba(255, 255, 255, 0.4);
  pointer-events: none;
}

.panel--signals,
.panel--workload,
.panel--focus {
  background: radial-gradient(circle at top right, rgba(184, 135, 73, 0.11), transparent 32%),
  rgba(255, 255, 255, 0.82);
}

.panel--quick {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(255, 255, 255, 0.86)),
  radial-gradient(circle at top left, rgba(255, 255, 255, 0.9), transparent 38%);
}

.signal-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.signal-card,
.focus-metric-card {
  padding: 14px 16px;
  border: 1px solid rgba(17, 17, 17, 0.06);
  border-radius: var(--mono-radius-lg);
  background: rgba(255, 255, 255, 0.62);
}

.signal-card strong,
.focus-metric-card strong {
  display: block;
  margin-top: 8px;
  color: #111111;
  font-size: 28px;
  line-height: 1;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.workload-legend {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 12px;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.45;
  font-weight: 500;
}

.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  margin-right: 8px;
  border-radius: var(--mono-radius-pill);
}

.legend-dot--private {
  background: linear-gradient(90deg, #1a1a1a, #6a5a49);
}

.legend-dot--group {
  background: linear-gradient(90deg, #d5b183, #b77c44);
}

.workload-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.workload-item {
  padding: 16px 0 0;
  border-top: 1px solid rgba(17, 17, 17, 0.08);
}

.workload-item:first-child {
  padding-top: 0;
  border-top: none;
}

.workload-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.workload-name-group strong {
  display: block;
  color: #111111;
  font-size: 15px;
  font-weight: 600;
}

.workload-name-group span,
.workload-count {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.45;
  font-weight: 500;
}

.workload-track {
  height: 12px;
  border-radius: var(--mono-radius-pill);
  background: rgba(17, 17, 17, 0.08);
  overflow: hidden;
}

.workload-composite {
  display: flex;
  height: 100%;
  min-width: 0;
  border-radius: inherit;
  overflow: hidden;
}

.workload-segment {
  height: 100%;
}

.workload-segment--private {
  background: linear-gradient(90deg, #111111, #4e4032);
}

.workload-segment--group {
  background: linear-gradient(90deg, #d6b486, #b7773c);
}

.workload-empty {
  padding: 28px 18px;
  border: 1px dashed rgba(17, 17, 17, 0.08);
  border-radius: var(--mono-radius-lg);
  background: rgba(255, 255, 255, 0.5);
  color: #5f6368;
  font-size: 13px;
  line-height: 1.8;
  text-align: center;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.quick-link {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  min-height: 88px;
  padding: 14px 16px;
  border: 1px solid rgba(17, 17, 17, 0.07);
  border-radius: var(--mono-radius-lg);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.76));
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.05),
  inset 0 1px 0 rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  cursor: pointer;
  text-align: left;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.quick-link:hover {
  transform: translateY(-2px);
  border-color: rgba(17, 17, 17, 0.1);
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.07),
  inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.quick-link-icon,
.quick-link-arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.quick-link-icon {
  width: 52px;
  height: 52px;
  border: 1px solid rgba(17, 17, 17, 0.06);
  border-radius: var(--mono-radius-lg);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
  color: #111111;
  font-size: 20px;
  backdrop-filter: blur(18px);
}

.quick-link-copy {
  min-width: 0;
  flex: 1;
}

.quick-link-copy span {
  display: block;
  color: #111111;
  font-size: 16px;
  font-weight: 450;
}

.quick-link-arrow {
  width: 38px;
  height: 38px;
  border: 1px solid rgba(17, 17, 17, 0.06);
  border-radius: var(--mono-radius-pill);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
  color: #5f6368;
  backdrop-filter: blur(18px);
}

.focus-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

@keyframes fade-lift {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1380px) {
  .kpi-grid,
  .hero-pill-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-insights-grid,
  .dashboard-main-grid,
  .dashboard-bottom-grid,
  .hero-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .dashboard-shell {
    padding: 18px;
    border-radius: var(--mono-radius-xl);
  }

  .header-summary,
  .hero-side-grid,
  .signal-grid,
  .focus-metrics,
  .quick-links,
  .kpi-grid,
  .hero-pill-row {
    grid-template-columns: 1fr;
  }

  .header-summary {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .revenue-brief {
    width: 100%;
    text-align: left;
  }

  .hero-panel,
  .panel,
  .kpi-card {
    padding: 20px;
  }

  .hero-title {
    font-size: 30px;
  }

  .workload-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .quick-link {
    align-items: flex-start;
  }
}
</style>

<style>
html.theme-glass-global .dashboard-shell {
  border: none;
  background: transparent;
  box-shadow: none;
  overflow: visible;
}

html.theme-glass-global .dashboard-shell::before {
  opacity: 0;
  width: 0;
  height: 0;
  background: none;
}

html.theme-glass-global .dashboard-shell .workspace-header {
  align-items: flex-start;
}

html.theme-glass-global:not(.dark) .dashboard-shell .revenue-brief {
  min-width: 0;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
}

html.theme-glass-global:not(.dark) .dashboard-shell .revenue-brief-label,
html.theme-glass-global:not(.dark) .dashboard-shell .revenue-brief-sub,
html.theme-glass-global:not(.dark) .dashboard-shell .kpi-title,
html.theme-glass-global:not(.dark) .dashboard-shell .signal-card span,
html.theme-glass-global:not(.dark) .dashboard-shell .focus-metric-card span,
html.theme-glass-global:not(.dark) .dashboard-shell .workload-name-group span,
html.theme-glass-global:not(.dark) .dashboard-shell .workload-count,
html.theme-glass-global:not(.dark) .dashboard-shell .workload-legend,
html.theme-glass-global:not(.dark) .dashboard-shell .health-meter-inner small,
html.theme-glass-global:not(.dark) .dashboard-shell .health-meter-inner span,
html.theme-glass-global:not(.dark) .dashboard-shell .quick-link-arrow {
  color: #6b7280;
}

html.theme-glass-global:not(.dark) .dashboard-shell .hero-panel {
  color: #111111;
  border: 1px solid var(--mono-line);
  background: var(--mono-surface-bg-elevated, #fff);
  box-shadow: none;
}

html.theme-glass-global:not(.dark) .dashboard-shell .hero-pill,
html.theme-glass-global:not(.dark) .dashboard-shell .hero-side-card,
html.theme-glass-global:not(.dark) .dashboard-shell .signal-card,
html.theme-glass-global:not(.dark) .dashboard-shell .focus-metric-card {
  background: var(--mono-surface-bg-elevated, #fff);
  border-color: var(--mono-line);
  box-shadow: none;
  backdrop-filter: none;
}

html.theme-glass-global:not(.dark) .dashboard-shell .hero-pill span,
html.theme-glass-global:not(.dark) .dashboard-shell .hero-side-card span {
  color: #6b7280;
}

html.theme-glass-global:not(.dark) .dashboard-shell .hero-pill strong,
html.theme-glass-global:not(.dark) .dashboard-shell .hero-side-card strong,
html.theme-glass-global:not(.dark) .dashboard-shell .health-meter-inner strong,
html.theme-glass-global:not(.dark) .dashboard-shell .signal-card strong,
html.theme-glass-global:not(.dark) .dashboard-shell .focus-metric-card strong,
html.theme-glass-global:not(.dark) .dashboard-shell .workload-name-group strong,
html.theme-glass-global:not(.dark) .dashboard-shell .quick-link-copy span {
  color: #111111;
}

html.theme-glass-global:not(.dark) .dashboard-shell .health-meter {
  border-color: var(--mono-line);
  background: var(--mono-surface-bg-elevated, #fff);
  backdrop-filter: none;
}

html.theme-glass-global:not(.dark) .dashboard-shell .health-meter::before {
  background: conic-gradient(from 210deg, #111111 0 var(--health-progress), rgba(17, 17, 17, 0.08) var(--health-progress) 100%);
  box-shadow: none;
}

html.theme-glass-global:not(.dark) .dashboard-shell .health-meter::after {
  background: #ffffff;
  box-shadow: inset 0 0 0 1px var(--mono-line);
}

html.theme-glass-global:not(.dark) .dashboard-shell .kpi-card,
html.theme-glass-global:not(.dark) .dashboard-shell .panel,
html.theme-glass-global:not(.dark) .dashboard-shell .workload-empty,
html.theme-glass-global:not(.dark) .dashboard-shell .quick-link {
  background: var(--mono-surface-bg-elevated, #fff);
  border-color: var(--mono-line);
  box-shadow: none;
  backdrop-filter: none;
}

html.theme-glass-global:not(.dark) .dashboard-shell .kpi-card:hover,
html.theme-glass-global:not(.dark) .dashboard-shell .quick-link:hover {
  border-color: var(--mono-line);
  box-shadow: none;
}

html.theme-glass-global:not(.dark) .dashboard-shell .panel::after,
html.theme-glass-global:not(.dark) .dashboard-shell .kpi-card::after {
  display: none;
}

html.theme-glass-global:not(.dark) .dashboard-shell .panel--signals,
html.theme-glass-global:not(.dark) .dashboard-shell .panel--workload,
html.theme-glass-global:not(.dark) .dashboard-shell .panel--focus {
  background: #ffffff;
}

html.theme-glass-global:not(.dark) .dashboard-shell .tone-amber,
html.theme-glass-global:not(.dark) .dashboard-shell .tone-ink,
html.theme-glass-global:not(.dark) .dashboard-shell .tone-olive,
html.theme-glass-global:not(.dark) .dashboard-shell .tone-coral {
  background: #f7f7f7;
  color: #111111;
}

html.theme-glass-global:not(.dark) .dashboard-shell .legend-dot--private,
html.theme-glass-global:not(.dark) .dashboard-shell .workload-segment--private {
  background: #111111;
}

html.theme-glass-global:not(.dark) .dashboard-shell .legend-dot--group,
html.theme-glass-global:not(.dark) .dashboard-shell .workload-segment--group {
  background: #d1d5db;
}

html.theme-glass-global:not(.dark) .dashboard-shell .workload-track {
  background: #f3f4f6;
}

html.theme-glass-global:not(.dark) .dashboard-shell .workload-item {
  border-top-color: rgba(17, 17, 17, 0.08);
}

html.theme-glass-global:not(.dark) .dashboard-shell .workload-empty {
  color: #6b7280;
  border-style: solid;
  background: var(--mono-surface-bg-elevated, #fff);
}

html.theme-glass-global:not(.dark) .dashboard-shell .panel--quick {
  background: var(--mono-surface-bg-elevated, #fff);
}

html.theme-glass-global:not(.dark) .dashboard-shell .quick-link {
  background: var(--mono-surface-bg-elevated, #fff);
  box-shadow: none;
  backdrop-filter: none;
}

html.theme-glass-global:not(.dark) .dashboard-shell .quick-link-icon {
  border-color: var(--mono-line);
  background: var(--mono-surface-bg-elevated, #fff);
  color: #111111;
  box-shadow: none;
  backdrop-filter: none;
}

html.theme-glass-global:not(.dark) .dashboard-shell .quick-link-arrow {
  border-color: var(--mono-line);
  background: var(--mono-surface-bg-elevated, #fff);
  box-shadow: none;
  backdrop-filter: none;
}

html.theme-glass-global:not(.dark) .dashboard-shell .chart-shell .summary-card,
html.theme-glass-global:not(.dark) .dashboard-shell .chart-shell .chart-canvas,
html.theme-glass-global:not(.dark) .dashboard-shell .chart-shell .empty-state {
  border-color: var(--mono-line);
  background: var(--mono-surface-bg-elevated, #fff);
  box-shadow: none;
  backdrop-filter: none;
}
</style>
