<template>
  <div class="mobile-container">
    <div class="glass-card mobile-card">
        <div class="pc-icon-wrapper">
            <!-- 极简电脑图标 SVG -->
            <svg class="pc-icon-svg" viewBox="0 0 24 24" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
                <path d="M20 4H4C2.89543 4 2 4.89543 2 6V15C2 16.1046 2.89543 17 4 17H20C21.1046 17 22 16.1046 22 15V6C22 4.89543 21.1046 4 20 4ZM4 6H20V15H4V6ZM2 19H22V21H2V19Z" />
            </svg>
        </div>
        <h1 class="title">登录 PC 端</h1>
        <p class="desc">即将登录网页版，请确认是本人操作</p>
        
        <!-- 移除丑陋的 session ID 显示 -->
        
        <div class="actions">
            <button v-if="!isConfirmed" @click="confirmLogin" class="confirm-btn">
                {{ status === '正在确认...' ? '处理中...' : '确认登录' }}
            </button>
            <div v-else class="success-msg">
                <div class="check-icon">✓</div>
                <p style="font-size: 18px; color: #4caf50; font-weight: bold;">已允许登录</p>
            </div>
        </div>
        
        <!-- 移除底部状态文字 -->
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';

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
            status.value = 'PC端登录确认成功!';
        } else {
            status.value = '失败: ' + res.data.message;
        }
    } catch (e) {
        status.value = '网络错误';
    }
};
</script>

<style scoped>
.mobile-container {
    position: fixed; /* 强制覆盖全屏，跳出 #app 的 flex 限制 */
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #fcf9da 0%, #f7f1e3 100%); /* 确保背景色正确 */
    z-index: 999;
}

.mobile-card {
    width: 92%; /* 稍微加宽 */
    max-width: 450px; /* 放宽限制 */
    min-height: 500px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    text-align: center;
    
    /* Windows 7 Aero 风格极致透明毛玻璃 */
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.4), rgba(255, 255, 255, 0.1));
    backdrop-filter: blur(10px); /* 适度模糊，保证通透 */
    -webkit-backdrop-filter: blur(10px);
    border-radius: 28px;
    padding: 40px;
    
    /* 强质感边框和光泽 */
    border: 1px solid rgba(255, 255, 255, 0.5);
    border-top: 1px solid rgba(255, 255, 255, 0.8); /* 顶部高光 */
    border-left: 1px solid rgba(255, 255, 255, 0.8);
    
    box-shadow: 
        0 8px 32px 0 rgba(31, 38, 135, 0.1),
        inset 0 0 0 1px rgba(255, 255, 255, 0.2); /* 内部微光 */
}

@media screen and (min-width: 768px) {
    .mobile-card {
        max-width: 400px;
    }
}

/* 顶部大图标 */
.pc-icon-wrapper {
    margin-bottom: 30px; 
    display: flex;
    justify-content: center;
}

.pc-icon-svg {
    width: 60px; /* 缩小图标，更加精致 */
    height: 60px;
    color: #444; 
    opacity: 0.9;
}

.title {
    font-size: 24px;
    font-weight: 800;
    color: #333;
    margin-bottom: 10px;
}

.desc {
    margin-bottom: 40px;
    color: #666;
    font-size: 15px;
}

/* 确认按钮样式 - 完全复刻 PC 端登录按钮 */
.confirm-btn {
    width: 100%;
    background: linear-gradient(135deg, #ffcb51 0%, #ffb347 100%);
    border: none;
    padding: 16px;
    font-size: 18px;
    font-weight: 500; /* 不加粗，优雅一点 */
    color: #222; /* 纯黑太硬，用深灰 */
    border-radius: 22.5px; /* 完全圆角 */
    cursor: pointer;
    box-shadow: 0 4px 15px rgba(255, 203, 81, 0.4);
    transition: all 0.2s;
    letter-spacing: 1px;
}

.confirm-btn:active {
    transform: scale(0.96);
    box-shadow: 0 2px 8px rgba(255, 203, 81, 0.3);
}

.check-icon {
    font-size: 5em;
    color: #4caf50;
    margin-bottom: 20px;
    animation: popIn 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

@keyframes popIn {
    0% { transform: scale(0); opacity: 0; }
    80% { transform: scale(1.2); }
    100% { transform: scale(1); opacity: 1; }
}

.status-text {
    display: none; /* 隐藏底部状态文字，太丑 */
}
</style>
