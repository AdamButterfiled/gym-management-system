<template>
  <div class="mobile-page">
    <div class="mobile-card mono-surface">
      <div class="mobile-icon">
        <svg viewBox="0 0 24 24" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
          <path d="M20 4H4C2.89543 4 2 4.89543 2 6V15C2 16.1046 2.89543 17 4 17H20C21.1046 17 22 16.1046 22 15V6C22 4.89543 21.1046 4 20 4ZM4 6H20V15H4V6ZM2 19H22V21H2V19Z" />
        </svg>
      </div>

      <h1>确认登录 PC 端</h1>
      <p class="mobile-desc">请确认这是你本人发起的登录操作，确认后网页端会直接进入后台。</p>

      <div class="mobile-actions">
        <StandardButton v-if="!isConfirmed" type="primary" class="confirm-btn" @click="confirmLogin">
          {{ status === '正在确认...' ? '处理中...' : '确认登录' }}
        </StandardButton>

        <div v-else class="success-block">
          <div class="success-mark">✓</div>
          <div class="success-title">已允许登录</div>
          <div class="success-desc">网页端正在继续登录流程</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';
import StandardButton from '@/components/common/StandardButton.vue';

const route = useRoute();
const uuid = route.params.uuid;
const status = ref('准备登录');
const isConfirmed = ref(false);

const confirmLogin = async () => {
  status.value = '正在确认...';

  try {
    const res = await axios.post('/api/login', { uuid });
    if (res.data.success) {
      isConfirmed.value = true;
      status.value = 'PC端登录确认成功';
    } else {
      status.value = `失败: ${res.data.message}`;
    }
  } catch (error) {
    status.value = '网络错误';
  }
};
</script>

<style scoped>
.mobile-page {
  min-height: 100vh;
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mobile-card {
  width: min(460px, 100%);
  padding: 32px;
  text-align: center;
}

.mobile-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  border-radius: var(--mono-radius-lg);
  border: 1px solid var(--mono-line);
  background: var(--mono-surface-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--mono-text);
}

.mobile-icon svg {
  width: 28px;
  height: 28px;
}

.mobile-card h1 {
  margin: 0;
  font-size: 28px;
  color: var(--mono-text);
  letter-spacing: -0.03em;
}

.mobile-desc {
  margin: 14px 0 0;
  color: var(--mono-text-secondary);
  line-height: 1.7;
}

.mobile-actions {
  margin-top: 28px;
}

.confirm-btn {
  width: 100%;
  height: 48px;
  border-radius: var(--mono-radius-pill);
  background: var(--shad-primary-bg) !important;
  color: var(--shad-primary-foreground) !important;
  font-size: 16px;
  font-weight: 600;
}

.success-block {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.success-mark {
  width: 60px;
  height: 60px;
  border-radius: var(--mono-radius-pill);
  border: 1px solid var(--mono-line);
  background: var(--mono-surface-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: var(--mono-text);
}

.success-title {
  margin-top: 18px;
  font-size: 20px;
  font-weight: 600;
  color: var(--mono-text);
}

.success-desc {
  margin-top: 6px;
  color: var(--mono-text-secondary);
  font-size: 14px;
}
</style>
