<template>
  <view class="page-shell">
    <view class="card">
      <view class="section-title">教练工作台</view>
      <view class="muted">今日课程与预约审批概览</view>
    </view>
    <view style="display:flex; gap:20rpx; margin-bottom:24rpx;">
      <StatCard label="今日课程" :value="dashboard.todayLessons || 0" sub="团课 + 私教" style="flex:1;" />
      <StatCard label="待审批" :value="dashboard.pendingApprovals || 0" sub="私教申请" style="flex:1;" />
    </view>
    <view style="display:flex; gap:20rpx; margin-bottom:24rpx;">
      <StatCard label="学员数" :value="dashboard.totalStudents || 0" sub="累计服务学员" style="flex:1;" />
      <StatCard label="今日签到" :value="dashboard.todayCheckins || 0" sub="已到馆" style="flex:1;" />
    </view>
    <view class="card">
      <view class="section-title">快捷入口</view>
      <view class="nav-grid">
        <button class="btn-secondary nav-btn" @click="go('/pages/coach/approvals/index')">预约审批</button>
        <button class="btn-secondary nav-btn" @click="go('/pages/coach/metrics/index')">体测记录</button>
        <button class="btn-secondary nav-btn" @click="go('/pages/coach/logs/index')">训练日志</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { onShow, reactive } from 'vue'
import StatCard from '../../../components/StatCard.vue'
import { request } from '../../../utils/request'

const dashboard = reactive({
  todayLessons: 0,
  pendingApprovals: 0,
  totalStudents: 0,
  todayCheckins: 0
})

const loadData = async () => {
  const res = await request({ url: '/coach/home' })
  Object.assign(dashboard, res.data)
}

const go = (url) => uni.navigateTo({ url })

onShow(loadData)
</script>

<style scoped>
.nav-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
}

.nav-btn {
  width: 100%;
}
</style>
