<template>
  <view class="card stat-card">
    <text class="stat-label">{{ label }}</text>
    <text class="stat-value">{{ value }}</text>
    <text class="stat-sub">{{ sub }}</text>
  </view>
</template>

<script setup>
defineProps({
  label: String,
  value: [String, Number],
  sub: String
})
</script>

<style scoped>
.stat-card {
  margin-bottom: 0;
}

.stat-label {
  font-size: 24rpx;
  color: #8b7f67;
}

.stat-value {
  display: block;
  margin-top: 10rpx;
  font-size: 46rpx;
  font-weight: 700;
}

.stat-sub {
  display: block;
  margin-top: 8rpx;
  color: #8b7f67;
  font-size: 24rpx;
}
</style>
