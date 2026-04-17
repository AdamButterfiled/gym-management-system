<template>
  <view class="page-shell">
    <view class="card">
      <view class="section-title">会员工作台</view>
      <view class="muted">余额 {{ home.balance || 0 }} 元，当前会籍 {{ home.activeMembershipName || '未开通' }}</view>
    </view>

    <view style="display:flex; gap: 20rpx; margin-bottom: 24rpx;">
      <StatCard label="剩余私教课时" :value="home.remainingPrivateSessions || 0" sub="可用于私教预约" style="flex:1;" />
      <StatCard label="待参加预约" :value="home.upcomingBookings?.length || 0" sub="未来预约数量" style="flex:1;" />
    </view>

    <view class="card">
      <view class="section-title">快捷入口</view>
      <view class="nav-grid">
        <button class="btn-secondary nav-btn" @click="go('/pages/member/venues/index')">场馆预约</button>
        <button class="btn-secondary nav-btn" @click="go('/pages/member/courses/index')">团课秒杀</button>
        <button class="btn-secondary nav-btn" @click="go('/pages/member/coaches/index')">私教预约</button>
        <button class="btn-secondary nav-btn" @click="go('/pages/member/bookings/index')">我的预约</button>
        <button class="btn-secondary nav-btn" @click="go('/pages/member/wallet/index')">钱包资产</button>
        <button class="btn-secondary nav-btn" @click="go('/pages/member/checkin/index')">签到核销</button>
      </view>
    </view>

    <view class="card">
      <view class="section-title">最近预约</view>
      <view v-for="item in home.upcomingBookings || []" :key="item.id" class="list-item">
        <view>{{ item.resourceName }}</view>
        <view class="muted">{{ item.startTime }}</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { onShow, reactive } from 'vue'
import StatCard from '../../../components/StatCard.vue'
import { request } from '../../../utils/request'

const home = reactive({
  balance: 0,
  activeMembershipName: '',
  remainingPrivateSessions: 0,
  upcomingBookings: []
})

const loadData = async () => {
  const res = await request({ url: '/member/home' })
  Object.assign(home, res.data)
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

.list-item {
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0e6cf;
}
</style>
