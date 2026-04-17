<template>
  <view class="page-shell">
    <view class="card" v-for="item in bookings" :key="item.id">
      <view class="row-between">
        <view>
          <view class="section-title" style="font-size: 30rpx;">{{ item.resourceName }}</view>
          <view class="muted">{{ item.startTime }}</view>
        </view>
        <view class="tag">{{ item.status }}</view>
      </view>
      <button class="btn-primary" v-if="item.status === 'CONFIRMED'" @click="createToken(item.id)">生成签到码</button>
      <view v-if="tokenMap[item.id]" class="card" style="margin-top: 16rpx; background:#fbf7ee;">
        <view class="field-label">动态签到码</view>
        <view style="word-break: break-all;">{{ tokenMap[item.id].token }}</view>
        <view class="muted">有效期至 {{ tokenMap[item.id].expireTime }}</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { onShow, ref } from 'vue'
import { request } from '../../../utils/request'

const bookings = ref([])
const tokenMap = ref({})

const loadData = async () => {
  const res = await request({ url: '/member/bookings' })
  bookings.value = res.data.filter(item => ['CONFIRMED', 'CHECKED_IN'].includes(item.status))
}

const createToken = async (bookingId) => {
  const res = await request({ url: `/member/checkin/${bookingId}` })
  tokenMap.value = {
    ...tokenMap.value,
    [bookingId]: res.data
  }
}

onShow(loadData)
</script>
