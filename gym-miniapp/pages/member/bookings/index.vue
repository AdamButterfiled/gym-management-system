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
      <view class="muted">金额 ¥{{ item.amount }}，支付 {{ item.paymentStatus }}</view>
      <view class="muted" style="margin: 12rpx 0;">{{ item.remark }}</view>
      <button class="btn-danger" v-if="item.status !== 'CANCELLED' && item.status !== 'REFUNDED'" @click="cancel(item.id)">取消预约</button>
    </view>
  </view>
</template>

<script setup>
import { onShow, ref } from 'vue'
import { request } from '../../../utils/request'

const bookings = ref([])

const loadData = async () => {
  const res = await request({ url: '/member/bookings' })
  bookings.value = res.data
}

const cancel = async (id) => {
  await request({ url: `/member/bookings/${id}/cancel`, method: 'POST' })
  uni.showToast({ title: '预约已取消', icon: 'none' })
  loadData()
}

onShow(loadData)
</script>
