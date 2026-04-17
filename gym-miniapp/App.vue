<template>
  <slot />
</template>

<style>
page {
  background: #f7f2e8;
  color: #2f2412;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", sans-serif;
}

.page-shell {
  min-height: 100vh;
  padding: 24rpx;
}

.card {
  background: rgba(255, 255, 255, 0.95);
  border: 1rpx solid rgba(229, 216, 183, 0.9);
  border-radius: 28rpx;
  padding: 28rpx;
  box-shadow: 0 20rpx 40rpx rgba(132, 110, 63, 0.08);
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 34rpx;
  font-weight: 700;
  margin-bottom: 16rpx;
}

.muted {
  color: #8b7f67;
}

.btn-primary,
.btn-secondary,
.btn-danger {
  border-radius: 999rpx;
  padding: 18rpx 28rpx;
  font-size: 28rpx;
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, #f6bf2f, #ef8e19);
  color: #2f2412;
}

.btn-secondary {
  background: #f2eee3;
  color: #2f2412;
}

.btn-danger {
  background: #ff6b6b;
  color: #fff;
}

.field {
  margin-bottom: 20rpx;
}

.field-label {
  display: block;
  margin-bottom: 12rpx;
  color: #7f7259;
  font-size: 26rpx;
}

.input {
  width: 100%;
  box-sizing: border-box;
  padding: 18rpx 20rpx;
  border-radius: 18rpx;
  background: #f8f4ea;
  border: 1rpx solid #e8dcc1;
}

.row-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tag {
  display: inline-block;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(247, 183, 24, 0.15);
  color: #a76d00;
  font-size: 24rpx;
}
</style>
