<template>
    <div class="login-container">
      <!-- 左侧登录表单 -->
      <div class="login-form">
        <div class="avatar">
          <img src="avatarImg" alt="Avatar" />
        </div>
        <h2>用户登录</h2>
        <p>欢迎回来！请输入您的信息</p>
        <a-form @submit.prevent="handleLogin" class="form-group">
          <!-- 账号输入框 -->
          <a-form-item name="account">
            <a-input
              v-model:value="account"
              placeholder="请输入账号"
              clearable
              style="width: 320px; height: 40px;"
            />
          </a-form-item>
          <!-- 密码输入框 -->
          <a-form-item name="password">
            <a-input-password
              v-model:value="password"
              placeholder="请输入密码"
              clearable
              @focus="onPasswordFocus"
              @blur="onPasswordBlur"
              style="width: 320px; height: 40px; top: 5px;"
            >
              <template #iconRender="v">
                <EyeTwoTone v-if="v"></EyeTwoTone>
                <EyeInvisibleOutlined v-else></EyeInvisibleOutlined>
              </template>
            </a-input-password>
          </a-form-item>
          <!-- 记住密码和忘记密码 -->
          <div class="form-remember">
            <a-checkbox v-model:checked="rememberMe">记住账号密码</a-checkbox>
            <a href="#" class="forgot-password">忘记密码?</a>
          </div>
          <!-- 登录按钮 -->
          <a-form-item class="login-button-container">
            <a-button
              type="primary"
              style="width: 250px; height: 38px; top: 15px; border-radius: 20px;"
              @click="handleLogin"
              @mouseenter="handleLoginHover"
              @mouseleave="handleLoginLeave"
              block
            >
              登录
            </a-button>
          </a-form-item><div class="text-container">
            <a-typography.Text class="register-link">
              未注册账号？
              <a href="#">立即注册</a>
            </a-typography.Text>
          </div>
        </a-form>
      </div>
      <!-- 右侧蓝色圆形 -->
      <div class="login-visual">
        <div class="blue-circle">
          <!-- 左眼 -->
          <div class="eye left-eye" :class="{ 'blink': isBlinking }">
            <div class="pupil" :style="pupilStyle.left"></div>
          </div>
          <!-- 右眼 -->
          <div class="eye right-eye" :class="{ 'blink': isBlinking }">
            <div class="pupil" :style="pupilStyle.right"></div>
          </div>
          <!-- 小嘴巴 -->
          <div
            class="mouth"
            :class="{ smile: isSmiling, sad: isSad }"></div>
        </div>
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref, reactive, onMounted, onUnmounted } from "vue";
  import { EyeTwoTone, EyeInvisibleOutlined } from '@ant-design/icons-vue';
  import avatarImg from "/Users/adambutterfield/vue-project/my-project/src/assets/u_3261039029_3175779615_fm_253_gp_0-removebg-preview.png";
  
  // 表单数据
  const email = ref("");
  const password = ref("");
  const rememberMe = ref(false);
  const isSmiling = ref(false);
  const isSad = ref(false);
  const isBlinking = ref(false);
  
  // 控制眼睛瞳孔
  const pupilStyle = reactive({
    left: { transform: "translate(0px, 0px)" },
    right: { transform: "translate(0px, 0px)" },
  });
  
  const isAvoiding = ref(false);
  let blinkInterval;
  let peekTimeout;
  
  // 登录事件
  const handleLogin = () => {
    console.log("登录信息：", {
      email: email.value,
      password: password.value,
      rememberMe: rememberMe.value,
    });
    alert("登录成功！");
  };
  
  // 鼠标移动事件
  const onMouseMove = (event) => {
    if (isAvoiding.value) return;
    const circle = document.querySelector(".blue-circle");
    const rect = circle.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    const deltaX = event.clientX - centerX;
    const deltaY = event.clientY - centerY;
    const angle = Math.atan2(deltaY, deltaX);
    const maxOffset = 4;
    const offsetX = Math.cos(angle) * maxOffset;
    const offsetY = Math.sin(angle) * maxOffset;
    pupilStyle.left = { transform: `translate(${offsetX}px, ${offsetY}px)` };
    pupilStyle.right = { transform: `translate(${offsetX}px, ${offsetY}px)` };
  };
  
  // 密码框获得焦点时
const onPasswordFocus = () => {
  isAvoiding.value = true;
  isSad.value = true;
  pupilStyle.left = { transform: "translate(4px, -2px)" };
  pupilStyle.right = { transform: "translate(4px, -2px)" };

  // 无限循环偷看
  peekTimeout = setInterval(() => {
    isAvoiding.value = false;
    pupilStyle.left = { transform: "translate(-2px, 2px)" };
    pupilStyle.right = { transform: "translate(-2px, 2px)" };

    // 偷看后重新躲开
    setTimeout(() => {
      isAvoiding.value = true;
      pupilStyle.left = { transform: "translate(4px, -2px)" };
      pupilStyle.right = { transform: "translate(4px, -2px)" };
    }, 1300);
  }, 4200); // 每隔 3 秒触发偷看
};

// 密码框失去焦点时
const onPasswordBlur = () => {
  isAvoiding.value = false;
  isSad.value = false;
  clearInterval(peekTimeout); // 清除循环定时器
};

  
  // 登录按钮悬停效果
  const handleLoginHover = () => {
    isSmiling.value = true;
    isSad.value = false;
  };
  
  const handleLoginLeave = () => {
    isSmiling.value = false;
  };
  
  // 全局鼠标监听和眨眼动画
  onMounted(() => {
    window.addEventListener("mousemove", onMouseMove);
    blinkInterval = setInterval(() => {
      isBlinking.value = true;
      setTimeout(() => {
        isBlinking.value = false;
      }, 100);
    }, 2800);
  });
  
  onUnmounted(() => {
    window.removeEventListener("mousemove", onMouseMove);
    clearInterval(blinkInterval);
    clearTimeout(peekTimeout);
  });
  </script>
  
  <style scoped>
  /* 登录容器 */
  .login-container {
    display: flex;
    flex-wrap: wrap;
    background-color: white;
    width: 80%;
    max-width: 1000px;
    border-radius: 30px;
    box-shadow: 0px 4px 20px rgba(0, 0, 0, 0.1);
    overflow: hidden;
    margin: auto;
  }
  
  /* 登录表单 */
  .login-form {
    flex: 1;
    padding: 50px 100px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 24px;
  }
  
  /* 输入框 */
  a-input,
  a-input-password {
    width: 100%;
    max-width: 320px;
    height: 36px;
  }
  
  h2 {
    margin: 0;
    font-size: 28px;
    font-weight: bold;
    text-align: center;
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    margin-top: -20px;
  }
  
  p {
    margin: 0;
    color: #888;
    font-size: 14px;
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    margin-top: -6px;
    margin-bottom: 12px;
  }
  
  /* 登录按钮 */
  .login-button-container {
    width: 100%;
    display: flex;
    justify-content: center;
  }
  
  a-button {
    width: 240px;
    height: 36px;
  }
  
  /* 记住密码和忘记密码 */
  .form-remember {
    display: flex;
    justify-content: space-between;
    width: 100%;
    max-width: 400px;
    align-items: center;
    margin-bottom: 10px;
    margin-top: 2px;
  }
  
  .forgot-password {
    color: #007bff;
    text-decoration: none;
    font-size: 14px;
    display: inline-block;
    margin-left: 10px;
  }
  
  .text-container {
    display: flex;
    justify-content: center;
    align-items: center;
    position: relative;
  }
  
  .register-link {
    position: relative;
    top: 30px;
  }
  
  /* 右侧蓝色圆形 */
  .login-visual {
    flex: 1;
    background-color: #f0f8ff;
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 40px;
  }
  
  .blue-circle {
    width: 220px;
    height: 220px;
    background-color: #007bff;
    border-radius: 50%;
    position: relative;
  }
  
  /*眼睛 */
  .eye {
    width: 22px;
    height: 22px;
    background-color: white;
    border-radius: 50%;
    position: absolute;
    display: flex;
    justify-content: center;
    align-items: center;
    overflow: hidden;
    transition: all 0.1s ease;
  }
  
  .eye.blink {
    height: 2px;
    margin-top: 11px;
  }
  
  .left-eye {
    left: 60px;
    top: 50px;
  }
  
  .right-eye {
    right: 60px;
    top: 50px;
  }
  
  /* 瞳孔 */
  .pupil {
    width: 10px;
    height: 10px;
    background-color: black;
    border-radius: 50%;
    transition: transform 0.1s ease-out;
  }
  
  /* 嘴巴 */
  .mouth {
    position: absolute;
    bottom: 100px;
    left: 50%;
    width: 28px;
    height: 11px;
    background-color: black;
    border-radius: 60%;
    transform: translateX(-50%);
    overflow: hidden;
    transition: all 0.3s ease;
  }
  
  .mouth.smile {
    border-radius: 0 0 100px 100px;
    height: 18px;
    bottom: 100px;
  }
  
  .mouth.sad {
    border-radius: 100px 100px 0 0;
    height: 18px;
    bottom: 100px;
  }
  
  .avatar {
    display: flex;
    justify-content: center;
    align-items: center;
    margin-bottom: 15px;
    max-width: 82px;
    max-height: 80px;
  }
  
  .avatar img {
    width: auto;
    height: auto;
    max-width: 100%;
    max-height: 100%;
  }
  </style>
  