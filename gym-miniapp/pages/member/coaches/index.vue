<template>
  <view class="page-shell">
    <view class="card" v-for="coach in coaches" :key="coach.id">
      <view class="row-between">
        <view>
          <view class="section-title" style="font-size: 32rpx;">{{ coach.name }}</view>
          <view class="muted">{{ coach.specialization }}</view>
        </view>
        <view class="tag">¥ {{ coach.hourlyPrice }}/h</view>
      </view>
      <view class="muted" style="margin: 10rpx 0;">评分 {{ coach.rating }} · {{ coach.intro }}</view>
      <view class="field-label">课包</view>
      <view v-for="pkg in coach.packages || []" :key="pkg.id" class="muted">- {{ pkg.name }} / ¥{{ pkg.price }}</view>
      <view class="field-label" style="margin-top: 16rpx;">未来可约时段</view>
      <view v-for="slot in coach.schedules || []" :key="slot.id" class="schedule-item">
        <view>
          <view>{{ slot.startTime }}</view>
          <view class="muted">{{ slot.endTime }}</view>
        </view>
        <button class="btn-primary small-btn" @click="bookPrivate(slot.id)">预约</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { onShow, ref } from 'vue'
import { request } from '../../../utils/request'

const coaches = ref([])

const loadData = async () => {
  const res = await request({ url: '/member/coaches' })
  coaches.value = res.data
}

const bookPrivate = async (scheduleId) => {
  const response = await request({
    url: '/member/bookings',
    method: 'POST',
    data: {
      resourceType: 'PRIVATE_COACH',
      resourceId: scheduleId,
      source: 'MINIAPP',
      idempotentKey: `private-${scheduleId}-${Date.now()}`
    }
  })
  uni.showModal({
    title: '预约结果',
    content: response.data.message || '已提交私教预约，等待教练确认。',
    showCancel: false
  })
}

onShow(loadData)
</script>

<style scoped>
.schedule-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0e6cf;
}

.small-btn {
  padding: 12rpx 20rpx;
  font-size: 24rpx;
}
</style>
