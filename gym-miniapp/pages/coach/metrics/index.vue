<template>
  <view class="page-shell">
    <view class="card">
      <view class="section-title">新增体测</view>
      <view class="field">
        <text class="field-label">会员用户ID</text>
        <input class="input" v-model="form.userId" placeholder="例如 2" />
      </view>
      <view class="field"><text class="field-label">身高(cm)</text><input class="input" v-model="form.height" /></view>
      <view class="field"><text class="field-label">体重(kg)</text><input class="input" v-model="form.weight" /></view>
      <view class="field"><text class="field-label">体脂率</text><input class="input" v-model="form.bodyFat" /></view>
      <view class="field"><text class="field-label">备注</text><input class="input" v-model="form.remark" /></view>
      <button class="btn-primary" @click="save">保存体测</button>
    </view>

    <view class="card">
      <view class="section-title">最近记录</view>
      <view v-for="item in metrics" :key="item.id" class="list-item">
        <view>会员 {{ item.userId }} · BMI {{ item.bmi }}</view>
        <view class="muted">{{ item.measuredAt }}</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { onShow, reactive, ref } from 'vue'
import { request } from '../../../utils/request'

const metrics = ref([])
const form = reactive({
  userId: '',
  height: '',
  weight: '',
  bodyFat: '',
  remark: ''
})

const loadData = async () => {
  const res = await request({ url: '/coach/body-metrics' })
  metrics.value = res.data
}

const save = async () => {
  await request({
    url: '/coach/body-metrics',
    method: 'POST',
    data: {
      userId: Number(form.userId),
      height: Number(form.height),
      weight: Number(form.weight),
      bodyFat: Number(form.bodyFat),
      remark: form.remark
    }
  })
  uni.showToast({ title: '体测已保存', icon: 'none' })
  Object.assign(form, { userId: '', height: '', weight: '', bodyFat: '', remark: '' })
  loadData()
}

onShow(loadData)
</script>

<style scoped>
.list-item {
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0e6cf;
}
</style>
