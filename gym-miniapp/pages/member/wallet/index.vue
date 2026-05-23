<template>
  <view class="page-shell">
    <view class="card">
      <view class="section-title">钱包与资产</view>
      <view class="muted">余额 {{ home.balance || 0 }} 元</view>
      <view class="field" style="margin-top: 20rpx;">
        <text class="field-label">充值金额</text>
        <input class="input" v-model="rechargeAmount" placeholder="例如 300" />
      </view>
      <button class="btn-primary" @click="recharge">模拟充值</button>
    </view>

    <view class="card">
      <view class="section-title">会籍套餐</view>
      <view v-for="item in membershipPackages" :key="item.id" class="purchase-item">
        <view>
          <view>{{ item.name }}</view>
          <view class="muted">¥{{ item.price }} / {{ item.days }} 天</view>
        </view>
        <button class="btn-secondary" @click="purchase('MEMBERSHIP', item.id)">购买</button>
      </view>
    </view>

    <view class="card">
      <view class="section-title">私教课包</view>
      <view v-for="item in privatePackages" :key="item.id" class="purchase-item">
        <view>
          <view>{{ item.name }}</view>
          <view class="muted">¥{{ item.price }} / {{ item.totalSessions }} 节</view>
        </view>
        <button class="btn-secondary" @click="purchase('PRIVATE_PACKAGE', item.id)">购买</button>
      </view>
    </view>

    <view class="card">
      <view class="section-title">最近支付单</view>
      <view v-for="item in payments" :key="item.id" class="purchase-item">
        <view>
          <view>{{ paymentTypeText(item.paymentType) }}</view>
          <view class="muted">¥{{ item.amount }} · {{ paymentStatusText(item.status) }}</view>
        </view>
        <button
          v-if="item.status === 'UNPAID'"
          class="btn-secondary pay-btn"
          :disabled="payingPaymentNo === item.paymentNo"
          @click="payPayment(item)"
        >
          {{ payingPaymentNo === item.paymentNo ? '支付中' : '支付' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { onShow, reactive, ref } from 'vue'
import { request } from '../../../utils/request'

const home = reactive({
  balance: 0
})
const membershipPackages = ref([])
const privatePackages = ref([])
const payments = ref([])
const rechargeAmount = ref('300')
const payingPaymentNo = ref('')

const paymentTypeText = (type) => ({
  RECHARGE: '余额充值',
  MEMBERSHIP: '会籍套餐',
  PRIVATE_PACKAGE: '私教课包',
  BOOKING: '预约支付'
}[type] || type)

const paymentStatusText = (status) => ({
  UNPAID: '待支付',
  PAID: '已支付',
  CLOSED: '已关闭',
  REFUNDED: '已退款'
}[status] || status)

const loadData = async () => {
  const [homeRes, membershipRes, privateRes, paymentRes] = await Promise.all([
    request({ url: '/member/home' }),
    request({ url: '/member/membership-packages' }),
    request({ url: '/member/private-packages' }),
    request({ url: '/member/payments' })
  ])
  Object.assign(home, homeRes.data)
  membershipPackages.value = membershipRes.data
  privatePackages.value = privateRes.data
  payments.value = paymentRes.data
}

const recharge = async () => {
  const createRes = await request({
    url: '/member/payments',
    method: 'POST',
    data: {
      paymentType: 'RECHARGE',
      amount: Number(rechargeAmount.value)
    }
  })
  await request({ url: `/member/payments/${createRes.data.paymentNo}/pay`, method: 'POST' })
  uni.showToast({ title: '充值成功', icon: 'none' })
  loadData()
}

const purchase = async (paymentType, targetId) => {
  const createRes = await request({
    url: '/member/payments',
    method: 'POST',
    data: { paymentType, targetId }
  })
  await request({ url: `/member/payments/${createRes.data.paymentNo}/pay`, method: 'POST' })
  uni.showToast({ title: '支付成功', icon: 'none' })
  loadData()
}

const payPayment = async (item) => {
  if (!item?.paymentNo || item.status !== 'UNPAID' || payingPaymentNo.value) return
  payingPaymentNo.value = item.paymentNo
  try {
    await request({ url: `/member/payments/${item.paymentNo}/pay`, method: 'POST' })
    uni.showToast({ title: '支付成功', icon: 'none' })
    await loadData()
  } finally {
    payingPaymentNo.value = ''
  }
}

onShow(loadData)
</script>

<style scoped>
.purchase-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #f0e6cf;
}

.pay-btn {
  flex-shrink: 0;
  min-width: 132rpx;
  margin-left: 20rpx;
  margin-right: 0;
}
</style>
