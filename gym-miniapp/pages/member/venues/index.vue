<template>
  <view class="page-shell">
    <view class="card" v-for="item in venues" :key="item.id">
      <view class="row-between">
        <view>
          <view class="section-title" style="font-size: 32rpx;">{{ item.name }}</view>
          <view class="muted">{{ item.location }} · 容量 {{ item.capacity }}</view>
        </view>
        <view class="tag">¥ {{ item.pricePerHour || 0 }}/小时</view>
      </view>
      <view class="muted" style="margin: 12rpx 0;">{{ item.description }}</view>
      <view class="field">
        <text class="field-label">开始时间</text>
        <input class="input" v-model="bookingStart[item.id]" placeholder="YYYY-MM-DD HH:mm:ss" />
      </view>
      <view class="field">
        <text class="field-label">结束时间</text>
        <input class="input" v-model="bookingEnd[item.id]" placeholder="YYYY-MM-DD HH:mm:ss" />
      </view>
      <button class="btn-primary" @click="bookVenue(item.id)">预约该场馆</button>
      <view class="muted" style="margin-top: 16rpx;">布局 JSON：{{ item.layoutJson || '未配置' }}</view>
    </view>
  </view>
</template>

<script setup>
import { onShow, ref } from 'vue'
import { request } from '../../../utils/request'

const venues = ref([])
const bookingStart = ref({})
const bookingEnd = ref({})

const loadData = async () => {
  const res = await request({ url: '/member/venues' })
  venues.value = res.data
}

const bookVenue = async (id) => {
  const response = await request({
    url: '/member/bookings',
    method: 'POST',
    data: {
      resourceType: 'VENUE',
      resourceId: id,
      source: 'MINIAPP',
      idempotentKey: `venue-${id}-${Date.now()}`,
      startTime: bookingStart.value[id],
      endTime: bookingEnd.value[id]
    }
  })
  uni.showModal({
    title: '预约结果',
    content: response.data.paymentOrder ? '预约成功，请前往钱包页面支付。' : '场馆预约已确认。',
    showCancel: false
  })
}

onShow(loadData)
</script>
