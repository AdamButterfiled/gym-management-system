<template>
  <div class="not-found-page">
    <div class="not-found-card mono-surface">
      <div class="not-found-code">404</div>
      <h1>页面不存在</h1>
      <p>地址 {{ missingPath }} 没有对应页面，系统也没有找到合适的纠错目标。</p>
      <StandardButton type="primary" @click="backToDashboard">返回工作台</StandardButton>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import StandardButton from '@/components/common/StandardButton.vue';

const route = useRoute();
const router = useRouter();

const missingPath = computed(() => {
  const from = route.query.from;
  return typeof from === 'string' ? from : route.path;
});

const backToDashboard = () => {
  router.push('/dashboard');
};
</script>

<style scoped>
.not-found-page {
  min-height: calc(100vh - 120px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.not-found-card {
  width: min(520px, 100%);
  padding: 40px;
  text-align: center;
}

.not-found-code {
  font-size: 64px;
  font-weight: 700;
  letter-spacing: -0.06em;
  color: var(--mono-text);
}

.not-found-card h1 {
  margin: 8px 0 0;
  font-size: 28px;
  color: var(--mono-text);
}

.not-found-card p {
  margin: 14px 0 24px;
  color: var(--mono-text-secondary);
}
</style>
