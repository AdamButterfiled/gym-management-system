<template>
  <view class="page-shell">
    <view class="card" v-for="item in courses" :key="item.id">
      <view class="row-between">
        <view>
          <view class="section-title" style="font-size: 32rpx;">{{ item.name }}</view>
          <view class="muted">{{ item.coachName }} · {{ item.venueName }}</view>
        </view>
        <view class="tag" v-if="item.flashSale === 1">秒杀</view>
      </view>
      <view class="muted" style="margin: 10rpx 0;">{{ item.startTime }} - {{ item.endTime }}</view>
      <view class="muted">剩余名额 {{ item.availableCount }} / {{ item.capacity }}</view>
      <view class="muted" style="margin: 12rpx 0;">常规价 ¥{{ item.price }}，秒杀价 ¥{{ item.flashSalePrice || item.price }}</view>
      <button class="btn-primary" @click="bookCourse(item)">立即预约</button>
    </view>
  </view>
</template>

<script setup>
import { onShow, ref } from 'vue'
import { request } from '../../../utils/request'

const courses = ref([])

const loadData = async () => {
  const res = await request({ url: '/member/group-courses' })
  courses.value = res.data
}

const bookCourse = async (item) => {
  const response = await request({
    url: '/member/bookings',
    method: 'POST',
    data: {
      resourceType: 'GROUP_COURSE',
      resourceId: item.id,
      source: 'MINIAPP',
      idempotentKey: `course-${item.id}-${Date.now()}`
    }
  })
  uni.showModal({
    title: '预约结果',
    content: response.data.paymentOrder ? '团课名额已锁定，请去钱包完成支付。' : '团课预约已确认。',
    showCancel: false
  })
}

onShow(loadData)
</script>
