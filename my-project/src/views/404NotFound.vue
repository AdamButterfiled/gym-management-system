<template>
  <a-result 
    status="404" 
    title="页面不存在" 
    sub-title="Sorry,功能开发中..." 
    style="margin-top: 70px; margin-left: -30px;"
  >
    <template #extra>
      <a-button type="primary" @click="backToDashboard">返回</a-button>
    </template>
  </a-result>
</template>

<script>
export default {
  methods: {
    backToDashboard() {
      this.$router.push('/dashboard')
    }
  }
}
</script>
