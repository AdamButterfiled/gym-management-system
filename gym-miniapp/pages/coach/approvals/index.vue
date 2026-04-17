<template>
  <view class="page-shell">
    <view class="card" v-for="item in bookings" :key="item.id">
      <view class="section-title" style="font-size: 30rpx;">{{ item.resourceName }}</view>
      <view class="muted">会员 {{ item.userId }} · {{ item.startTime }}</view>
      <view class="muted" style="margin: 10rpx 0;">{{ item.remark }}</view>
      <view style="display:flex; gap: 16rpx;">
        <button class="btn-primary" @click="review(item.id, true)">通过</button>
        <button class="btn-danger" @click="review(item.id, false)">拒绝</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { onShow, ref } from 'vue'
import { request } from '../../../utils/request'

const bookings = ref([])

const loadData = async () => {
  const res = await request({ url: '/coach/bookings/pending' })
  bookings.value = res.data
}

const review = async (id, approved) => {
  await request({ url: `/coach/bookings/${id}/${approved ? 'approve' : 'reject'}`, method: 'POST' })
  uni.showToast({ title: approved ? '已通过' : '已拒绝', icon: 'none' })
  loadData()
}

onShow(loadData)
</script>
