<template>
  <view class="page-shell">
    <view class="card">
      <view class="section-title">健身房预约系统</view>
      <view class="muted" style="margin-bottom: 24rpx;">登录后自动按角色进入会员端或教练端。</view>

      <view class="field">
        <text class="field-label">账号</text>
        <input class="input" v-model="username" placeholder="请输入账号" />
      </view>
      <view class="field">
        <text class="field-label">密码</text>
        <input class="input" password v-model="password" placeholder="请输入密码" />
      </view>

      <button class="btn-primary" @click="handleLogin">登录</button>

      <view class="card" style="margin-top: 24rpx; background: #fbf7ee;">
        <view class="field-label">演示账号</view>
        <view class="muted">会员：member01 / 123456</view>
        <view class="muted">教练：coach01 / 123456</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { request, relaunchByRole, setUser } from '../../utils/request'

const username = ref('member01')
const password = ref('123456')

const handleLogin = async () => {
  const res = await request({
    url: '/user/login',
    method: 'POST',
    data: {
      username: username.value,
      password: password.value
    }
  })
  setUser(res.data)
  relaunchByRole(res.data.role)
}
</script>
