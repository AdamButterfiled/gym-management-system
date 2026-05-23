<template>
  <view class="page-shell">
    <view class="card" v-for="item in bookings" :key="item.id">
      <view class="row-between">
        <view>
          <view class="section-title" style="font-size: 30rpx;">{{ item.resourceName }}</view>
          <view class="muted">{{ item.startTime }}</view>
        </view>
        <view class="tag">{{ bookingStatusLabel(item.status) }}</view>
      </view>
      <view class="muted">金额 ¥{{ item.amount }}，支付 {{ paymentStatusLabel(item.paymentStatus) }}</view>
      <view class="muted" style="margin: 12rpx 0;">{{ item.remark }}</view>
      <view class="action-row" v-if="canPay(item) || canCancel(item)">
        <button
          class="btn-secondary action-btn"
          v-if="canPay(item)"
          :loading="payingBookingId === item.id"
          @click="payBooking(item)"
        >
          支付
        </button>
        <button class="btn-danger action-btn" v-if="canCancel(item)" @click="cancel(item.id)">取消预约</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { onShow, ref } from 'vue'
import { request } from '../../../utils/request'

const bookings = ref([])
const payingBookingId = ref(null)

const bookingStatusMap = {
  CREATED: '待确认',
  PENDING_PAY: '待支付',
  CONFIRMED: '已确认',
  CHECKED_IN: '已签到',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDED: '已退款'
}

const paymentStatusMap = {
  UNPAID: '待支付',
  PAID: '已支付',
  CLOSED: '已关闭',
  REFUNDED: '已退款'
}

const loadData = async () => {
  const res = await request({ url: '/member/bookings' })
  bookings.value = res.data
}

const bookingStatusLabel = (status) => bookingStatusMap[status] || status || '-'

const paymentStatusLabel = (status) => paymentStatusMap[status] || status || '-'

const canCancel = (booking) => !['CANCELLED', 'REFUNDED', 'CHECKED_IN', 'COMPLETED'].includes(booking.status)

const canPay = (booking) => {
  return !['CANCELLED', 'REFUNDED', 'CHECKED_IN', 'COMPLETED'].includes(booking.status)
    && (booking.paymentStatus === 'UNPAID' || booking.status === 'PENDING_PAY')
}

const cancel = async (id) => {
  await request({ url: `/member/bookings/${id}/cancel`, method: 'POST' })
  uni.showToast({ title: '预约已取消', icon: 'none' })
  loadData()
}

const payBooking = async (booking) => {
  payingBookingId.value = booking.id
  try {
    const createRes = await request({
      url: '/member/payments',
      method: 'POST',
      data: {
        paymentType: 'BOOKING',
        targetId: booking.id
      }
    })
    await request({ url: `/member/payments/${createRes.data.paymentNo}/pay`, method: 'POST' })
    uni.showToast({ title: '支付成功', icon: 'none' })
    loadData()
  } finally {
    payingBookingId.value = null
  }
}

onShow(loadData)
</script>

<style scoped>
.action-row {
  display: flex;
  gap: 16rpx;
  margin-top: 16rpx;
}

.action-btn {
  flex: 1;
}
</style>
