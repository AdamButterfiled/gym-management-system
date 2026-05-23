<template>
  <div class="chart-shell">
    <div class="chart-summary">
      <div class="summary-card">
        <span>7日营收</span>
        <strong>{{ formatMoney(totalRevenue) }}</strong>
        <small>日均 {{ formatMoney(avgRevenue) }}</small>
      </div>
      <div class="summary-card">
        <span>预约总量</span>
        <strong>{{ totalBookings }} 单</strong>
        <small>日均 {{ avgBookings.toFixed(1) }} 单</small>
      </div>
      <div class="summary-card">
        <span>峰值日</span>
        <strong>{{ peakLabel }}</strong>
        <small>{{ peakRevenue > 0 ? formatMoney(peakRevenue) : '等待业务数据' }}</small>
      </div>
    </div>

    <div ref="chartRef" class="chart-canvas" :class="{ 'chart-canvas--empty': !hasData }"></div>

    <div v-if="!hasData" class="empty-state">
      <div class="empty-state-title">近 7 日还没有有效营收或预约数据</div>
      <div class="empty-state-sub">产生支付单或预约单后，这里会自动呈现趋势与峰值。</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import * as echarts from 'echarts/core';
import { BarChart, LineChart } from 'echarts/charts';
import {
  GridComponent,
  LegendComponent,
  MarkPointComponent,
  TooltipComponent,
  type GridComponentOption,
  type LegendComponentOption,
  type MarkPointComponentOption,
  type TooltipComponentOption
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { BarSeriesOption, LineSeriesOption } from 'echarts/charts';
import type { ComposeOption, ECharts } from 'echarts/core';
import { useStore } from 'vuex';

echarts.use([
  BarChart,
  LineChart,
  GridComponent,
  LegendComponent,
  MarkPointComponent,
  TooltipComponent,
  CanvasRenderer
]);

type RevenueBookingChartOption = ComposeOption<
  | BarSeriesOption
  | LineSeriesOption
  | GridComponentOption
  | LegendComponentOption
  | MarkPointComponentOption
  | TooltipComponentOption
>;

const props = defineProps<{
  days: string[];
  revenues: Array<number | string | null | undefined>;
  bookings: Array<number | string | null | undefined>;
}>();

const chartRef = ref<HTMLDivElement>();

let chart: ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;
const store = useStore();

const toNumber = (value: number | string | null | undefined) => {
  const normalized = Number(value ?? 0);
  return Number.isFinite(normalized) ? normalized : 0;
};

const labels = computed(() => props.days.map(day => (day ? day.slice(5) : '--')));
const revenueValues = computed(() => props.revenues.map(toNumber));
const bookingValues = computed(() => props.bookings.map(toNumber));

const totalRevenue = computed(() => revenueValues.value.reduce((sum, value) => sum + value, 0));
const totalBookings = computed(() => bookingValues.value.reduce((sum, value) => sum + value, 0));
const avgRevenue = computed(() => (revenueValues.value.length ? totalRevenue.value / revenueValues.value.length : 0));
const avgBookings = computed(() => (bookingValues.value.length ? totalBookings.value / bookingValues.value.length : 0));
const hasData = computed(() => totalRevenue.value > 0 || totalBookings.value > 0);

const peakIndex = computed(() => {
  if (!revenueValues.value.length) return -1;
  const maxValue = Math.max(...revenueValues.value, 0);
  if (maxValue <= 0) return -1;
  return revenueValues.value.findIndex(value => value === maxValue);
});

const peakLabel = computed(() => (peakIndex.value >= 0 ? labels.value[peakIndex.value] : '--'));
const peakRevenue = computed(() => (peakIndex.value >= 0 ? revenueValues.value[peakIndex.value] : 0));
const isDark = computed(() => store.state.themeSettings?.isDark ?? false);
const hasBorderRadius = computed(() => store.state.themeSettings?.borderRadius !== false);
const chartBarRadius = computed<[number, number, number, number]>(() => (
  hasBorderRadius.value ? [10, 10, 4, 4] : [4, 4, 2, 2]
));
const chartPillRadius = computed(() => (hasBorderRadius.value ? 999 : 8));

const formatMoney = (value: number) => {
  if (value >= 10000) {
    return `¥ ${(value / 10000).toFixed(value >= 100000 ? 0 : 1)}万`;
  }
  return `¥ ${value.toFixed(0)}`;
};

const buildOption = (): RevenueBookingChartOption => {
  const maxRevenue = Math.max(...revenueValues.value, 0);
  const dark = isDark.value;
  const legendColor = dark ? 'rgba(255, 255, 255, 0.68)' : '#5f6368';
  const axisColor = dark ? 'rgba(255, 255, 255, 0.46)' : '#8a8f98';
  const splitLineColor = dark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(17, 17, 17, 0.08)';
  const tooltipBg = dark ? 'rgba(12, 12, 12, 0.96)' : 'rgba(17, 17, 17, 0.92)';
  const tooltipShadow = dark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(17, 17, 17, 0.05)';
  const bookingLineColor = dark ? '#f3d4a8' : '#1f2329';
  const bookingAreaTop = dark ? 'rgba(243, 212, 168, 0.24)' : 'rgba(31, 35, 41, 0.18)';
  const bookingAreaBottom = dark ? 'rgba(243, 212, 168, 0.04)' : 'rgba(31, 35, 41, 0.02)';
  const markPointBg = dark ? '#111111' : '#111111';
  const markPointText = dark ? 'rgba(255, 255, 255, 0.92)' : '#ffffff';
  const markerBorder = dark ? '#111111' : '#ffffff';

  return {
    animationDuration: 700,
    animationDurationUpdate: 400,
    animationEasing: 'cubicOut',
    color: ['#a96c31', bookingLineColor],
    grid: {
      top: 58,
      right: 20,
      bottom: 12,
      left: 18,
      containLabel: true
    },
    legend: {
      top: 0,
      itemWidth: 12,
      itemHeight: 12,
      icon: 'roundRect',
      textStyle: {
        color: legendColor,
        fontSize: 12
      }
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: tooltipBg,
      borderWidth: 0,
      padding: [10, 12],
      textStyle: {
        color: '#ffffff',
        fontSize: 12
      },
      axisPointer: {
        type: 'shadow',
        shadowStyle: {
          color: tooltipShadow
        }
      },
      formatter: (params) => {
        const seriesItems = Array.isArray(params) ? params : [params];
        const index = (seriesItems[0] as { dataIndex?: number } | undefined)?.dataIndex ?? 0;
        const revenue = revenueValues.value[index] ?? 0;
        const bookings = bookingValues.value[index] ?? 0;
        const dayLabel = labels.value[index] ?? '--';

        return [
          `<div style="margin-bottom:6px;color:rgba(255,255,255,0.7);">${dayLabel}</div>`,
          `<div style="display:flex;justify-content:space-between;gap:24px;"><span>营收</span><strong>¥ ${revenue.toFixed(2)}</strong></div>`,
          `<div style="display:flex;justify-content:space-between;gap:24px;margin-top:4px;"><span>预约</span><strong>${bookings} 单</strong></div>`
        ].join('');
      }
    },
    xAxis: {
      type: 'category',
      data: labels.value,
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: axisColor,
        margin: 14,
        fontSize: 12
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '营收',
        minInterval: 1,
        splitNumber: 4,
        nameTextStyle: {
          color: axisColor,
          padding: [0, 0, 8, 0]
        },
        axisLabel: {
          color: axisColor,
          formatter: (value: number) => (value >= 10000 ? `${(value / 10000).toFixed(1)}万` : `${Math.round(value)}`)
        },
        splitLine: {
          lineStyle: {
            type: 'dashed',
            color: splitLineColor
          }
        }
      },
      {
        type: 'value',
        name: '预约',
        minInterval: 1,
        splitLine: {
          show: false
        },
        axisLine: {
          show: false
        },
        axisTick: {
          show: false
        },
        nameTextStyle: {
          color: axisColor,
          padding: [0, 0, 8, 0]
        },
        axisLabel: {
          color: axisColor,
          formatter: (value: number) => `${Math.round(value)}`
        }
      }
    ],
    series: [
      {
        name: '营收',
        type: 'bar',
        data: revenueValues.value,
        barWidth: 22,
        z: 2,
        itemStyle: {
          borderRadius: chartBarRadius.value,
          shadowColor: 'rgba(169, 108, 49, 0.18)',
          shadowBlur: 12,
          shadowOffsetY: 8,
          color: (params) => {
            const dataIndex = (params as { dataIndex?: number } | undefined)?.dataIndex;
            const isPeak = dataIndex === peakIndex.value && maxRevenue > 0;
            return isPeak
              ? new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: '#171717' },
                  { offset: 1, color: '#8d5c2d' }
                ])
              : new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: '#dcc19a' },
                  { offset: 1, color: '#a96c31' }
                ]);
          }
        },
        emphasis: {
          scale: true,
          itemStyle: {
            opacity: 0.92
          }
        },
        markPoint: maxRevenue > 0
          ? {
              symbol: 'roundRect',
              symbolSize: [52, 26],
              symbolOffset: [0, -18],
              data: [{ type: 'max' }],
              itemStyle: {
                color: markPointBg,
                borderRadius: chartPillRadius.value
              },
              label: {
                color: markPointText,
                fontWeight: 600,
                formatter: '峰值'
              }
            }
          : undefined
      },
      {
        name: '预约',
        type: 'line',
        yAxisIndex: 1,
        data: bookingValues.value,
        smooth: 0.35,
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 8,
        z: 3,
        lineStyle: {
          width: 3,
          color: bookingLineColor
        },
        itemStyle: {
          color: bookingLineColor,
          borderColor: markerBorder,
          borderWidth: 2
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: bookingAreaTop },
            { offset: 1, color: bookingAreaBottom }
          ])
        }
      }
    ]
  };
};

const resizeChart = () => {
  chart?.resize();
};

const renderChart = async () => {
  await nextTick();
  if (!chartRef.value) return;

  if (!chart) {
    chart = echarts.init(chartRef.value);
  }

  if (!hasData.value) {
    chart.clear();
    return;
  }

  chart.setOption(buildOption(), true);
  resizeChart();
};

watch([labels, revenueValues, bookingValues, isDark, hasBorderRadius], () => {
  void renderChart();
}, { deep: true });

onMounted(() => {
  if (chartRef.value) {
    resizeObserver = new ResizeObserver(() => {
      resizeChart();
    });
    resizeObserver.observe(chartRef.value);
  }

  window.addEventListener('resize', resizeChart);
  void renderChart();
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart);
  resizeObserver?.disconnect();
  chart?.dispose();
  chart = null;
});
</script>

<style scoped>
.chart-shell {
  position: relative;
}

.chart-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.summary-card {
  padding: 14px 16px;
  border: 1px solid var(--mono-line);
  border-radius: var(--mono-radius-lg);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(246, 245, 241, 0.96)),
    radial-gradient(circle at top right, rgba(220, 193, 154, 0.22), transparent 52%);
  box-shadow: inset 0 1px 0 var(--mono-surface-overlay);
}

.summary-card span,
.summary-card small {
  display: block;
  color: var(--mono-text-tertiary);
  line-height: 1.45;
  font-weight: 500;
  text-rendering: geometricPrecision;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  font-synthesis: none;
}

.summary-card span {
  font-size: 13px;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  color: var(--mono-text);
  font-size: 26px;
  line-height: 1;
  font-weight: 600;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums lining-nums;
  font-feature-settings: 'tnum' 1, 'lnum' 1;
  text-rendering: geometricPrecision;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  font-synthesis: none;
}

.summary-card small {
  margin-top: 8px;
  font-size: 13px;
  color: var(--mono-text-secondary);
}

.chart-canvas {
  height: 320px;
  border-radius: var(--mono-radius-xl);
  background:
    radial-gradient(circle at top left, rgba(220, 193, 154, 0.16), transparent 38%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(248, 248, 246, 0.96));
}

.chart-canvas--empty {
  opacity: 0.45;
}

.empty-state {
  position: absolute;
  inset: 112px 24px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: var(--mono-radius-lg);
  background: rgba(255, 255, 255, 0.58);
  backdrop-filter: blur(8px);
  text-align: center;
}

.empty-state-title {
  color: var(--mono-text);
  font-size: 15px;
  font-weight: 600;
  text-rendering: geometricPrecision;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  font-synthesis: none;
}

.empty-state-sub {
  max-width: 360px;
  color: var(--mono-text-secondary);
  font-size: 13px;
  line-height: 1.6;
  font-weight: 500;
  text-rendering: geometricPrecision;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  font-synthesis: none;
}

html.dark .chart-shell .summary-card {
  background: #1f1f1f;
}

html.dark .chart-shell .chart-canvas {
  background: #111111;
}

html.dark .chart-shell .empty-state {
  background: #111111;
}

@media (max-width: 960px) {
  .chart-summary {
    grid-template-columns: 1fr;
  }

  .chart-canvas {
    height: 300px;
  }

  .empty-state {
    inset: 208px 18px 18px;
  }
}
</style>
