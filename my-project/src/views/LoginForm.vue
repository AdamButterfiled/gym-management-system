<template>
  <div class="login-wrapper">
    <div class="login-container">
    <!-- 左侧登录表单区域 -->
    <div class="login-form">
      <div class="avatar">
        <a-avatar :src="avatarImg" style="width: 70px; height: 70px ; margin-top: -8px;" />
      </div>
      <h2>Adam</h2>
      <div class="typewriter">
        <span class="typewriter-text" v-if="showWelcomeText">{{ greeting }}呀&nbsp;,&nbsp;Adam !&nbsp;&nbsp;&nbsp;&nbsp;欢迎回来 &nbsp; ☺️ &nbsp;</span>
      </div>

      <!-- TABS 切换 -->
      <a-tabs v-model:activeKey="activeKey" centered class="custom-tabs">
        <!-- 标签页 1: 账号密码登录 -->
        <a-tab-pane key="1" tab="密码登录">
          <a-form @submit.prevent="handleLogin" class="form-group">
            <!-- 账号 -->
            <a-form-item name="account">
              <a-input
                v-model:value="account"
                placeholder="请输入账号"
                clearable
                style="width: 320px; height: 44.5px;"
              />
            </a-form-item>
            <!-- 密码 -->
            <a-form-item name="password">
              <a-input-password
                v-model:value="password"
                placeholder="请输入密码"
                clearable
                @focus="onPasswordFocus"
                @blur="onPasswordBlur"
                style="width: 320px; height: 44.5px;"
              >
                <template #iconRender="v">
                  <EyeTwoTone v-if="v"></EyeTwoTone>
                  <EyeInvisibleOutlined v-else></EyeInvisibleOutlined>
                </template>
              </a-input-password>
            </a-form-item>
            
            <div class="form-remember">
              <a-checkbox v-model:checked="rememberMe">记住账号密码</a-checkbox>
              <a href="#" class="forgot-password">忘记密码?</a>
            </div>
            
            <a-form-item class="login-button-container">
              <a-button
                type="primary"
                style="width: 250px; height: 38px; margin-top: 10px; border-radius: 20px;"
                @click="handleLogin"
                @mouseenter="handleLoginHover"
                @mouseleave="handleLoginLeave"
                block
              >
                登录
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- 标签页 2: 扫码登录 -->
        <a-tab-pane key="2" tab="扫码登录">
          <div class="qr-login-panel">
            <div v-if="!isLoggedIn" class="qr-content">
              <div class="qr-wrapper glass-effect">
                <div v-if="qrUrl" class="qr-code-box">
                  <img :src="qrUrl" alt="QR Code" />
                  <div class="qr-status">{{ qrStatus }}</div>
                </div>
                <div v-else class="loading-state">
                  <a-spin />
                  <p>正在获取二维码...</p>
                </div>
              </div>
              <p class="scan-tip">请使用手机扫码登录</p>
              <!-- Debug link for testing without phone -->
              <div class="debug-link">
                 <a :href="`/mobile/${uuid}`" target="_blank" style="font-size: 12px; color: #ccc;">(调试) 打开手机模拟器</a>
              </div>
            </div>
            
            <div v-else class="success-state">
              <div class="success-icon">🎉</div>
              <h3>登录成功</h3>
              <p>正在跳转...</p>
            </div>
          </div>
        </a-tab-pane>
      </a-tabs>

    </div>
    
    <!-- 右侧蓝色圆形 (保持原样) -->
    <div class="login-visual">
      <div class="blue-circle" :class="{ 'roll-in': startRoll }">
        <div class="eye left-eye" :class="{ 'blink': isBlinking }">
          <div class="pupil" :style="pupilStyle.left"></div>
        </div>
        <div class="eye right-eye" :class="{ 'blink': isBlinking }">
          <div class="pupil" :style="pupilStyle.right"></div>
        </div>
        <div class="mouth" :class="{ smile: isSmiling || isLifting || isWaving, sad: isSad, 'o-mouth': isLifting }"></div>
        
        <!-- Limbs (Jointed Arms) -->
        <div class="limb left-limb" :class="{ 'cover': isCovering, 'lift': isLifting, 'hidden': !isCovering && !isLifting, 'peek': isPeeking }">
          <div class="upper-arm">
              <div class="forearm">
                  <div class="hand"></div>
              </div>
          </div>
        </div>
        <div class="limb right-limb" :class="{ 'cover': isCovering, 'lift': isLifting, 'wave': isWaving, 'hidden': !isCovering && !isLifting && !isWaving, 'peek': isPeeking }">
          <div class="upper-arm">
              <div class="forearm">
                  <div class="hand"></div>
              </div>
          </div>
        </div>
        
        <!-- Dumbbell (New) -->
        <div class="dumbbell" v-if="isLifting">
            <div class="weight left"></div>
            <div class="bar"></div>
            <div class="weight right"></div>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch } from "vue";
import { EyeTwoTone, EyeInvisibleOutlined } from '@ant-design/icons-vue';
import avatarImg from "@/assets/images-_2_.jpg";
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import request from "@/request"; // Use configured axios instance
import QRCode from 'qrcode';

// ... (keep other imports)

// ... (keep other imports)

const router = useRouter();
const activeKey = ref('1'); // 默认 tab 1
const startRoll = ref(false);
const isCovering = ref(false);
const isPeeking = ref(false); // 偷看状态
const isLifting = ref(false);
const isWaving = ref(false); // 新增挥手状态

const showWelcomeText = ref(false);
const account = ref("");
const greeting = ref('');
const password = ref("");
const rememberMe = ref(false);
const isSmiling = ref(false);
const isSad = ref(false);
const isBlinking = ref(false);
const pupilStyle = reactive({
  left: { transform: "translate(0px, 0px)" },
  right: { transform: "translate(0px, 0px)" },
});
const isAvoiding = ref(false);
let blinkInterval;
let peekInterval;
let peekResetTimer;

const getGreeting = () => {
    const hour = new Date().getHours();
    if(hour < 9) return '早上好';
    if(hour < 11) return '上午好';
    if(hour < 13) return '中午好';
    if(hour < 18) return '下午好';
    return '晚上好';
};

// --- QR Code Logic ---
const uuid = ref('');
const qrUrl = ref('');
const qrStatus = ref('请扫码');
const isLoggedIn = ref(false);
let pollTimer = null;

const handleLogin = () => {
  // 1. 发起 POST 请求调用后端登录接口 /user/login
  // 参数是用户名和密码
  request.post("/user/login", { 
      username: account.value, 
      password: password.value 
  }).then(res => {
      // 2. 判断后端返回的状态码
      // 注意：后端 Result.java 定义 code 为 Integer (200)，不是字符串
      if (res.code === 200) {
        message.success('登录成功！');
        
        // 打印调试日志，确认拿到的数据
        console.log("Login Success. User Data:", res.data);
        
        // 3. 登录成功后，把用户信息（包含 Token）存入浏览器缓存 (localStorage)
        // 这样 request.ts 拦截器才能拿到 Token 放入请求头
        localStorage.setItem("user", JSON.stringify(res.data)); 
        localStorage.setItem('isLoggedIn', 'true');
        
        // 4. 实现 "记住密码" 功能
        // 如果用户勾选了记住密码，把账号密码存入 localStorage
        if (rememberMe.value) {
            localStorage.setItem("remember_account", account.value);
            // 注意：实际生产中不建议明文存密码，这里仅做演示
            // 可以简单的 base64 一下稍微掩盖：btoa(password.value)
            localStorage.setItem("remember_password", window.btoa(password.value));
            localStorage.setItem("is_remember", "true");
        } else {
            // 如果没勾选，清除之前可能存的记录
            localStorage.removeItem("remember_account");
            localStorage.removeItem("remember_password");
            localStorage.removeItem("is_remember");
        }

        // 5. 触发一些 UI 动画效果
        isLifting.value = true; 
        isSmiling.value = true;
        
        // 6. 延迟跳转到主页 /dashboard
        setTimeout(() => router.push('/dashboard'), 2500); 
      } else {
        // 登录失败提示
        message.error(res.msg || '账号 or 密码错误！');
        isSad.value = true;
        setTimeout(() => isSad.value = false, 2000);
      }
  }).catch(err => {
       message.error('Login failed: ' + err);
  });
};

// ... QR Code Logic ...
// 扫码登录逻辑
const initQrCode = async () => {
    try {
        // 1. 请求后端生成一个 UUID
        const res = await request.get('/api/qrcode');
        // 后端 QrCodeController 返回的结构是 { code: 200, data: { uuid: '...' } }
        // 注意：request.ts 拦截器可能已经解包了 data，也可能没有。
        // 根据之前的代码，request.ts 如果没有特别处理解包，res 就是整个返回体 {code, msg, data}
        
        let uuidVal = '';
        if (res.data && res.data.uuid) {
             uuidVal = res.data.uuid;
        } else if (res.uuid) {
             uuidVal = res.uuid; // 拦截器可能解包过
        }
        
        if (!uuidVal) {
             console.error("无法获取 UUID", res);
             qrStatus.value = '获取失败';
             return;
        }
        
        uuid.value = uuidVal;
        
        // 2. 构造手机端访问的 URL (用于生成二维码)
        // 这里会自动判断当前是 localhost 还是局域网 IP
        let host = window.location.hostname;
        if (host === 'localhost' || host === '127.0.0.1') {
             // 简单处理：为了演示，写死一个假设的局域网 IP，
             // 或者让用户自己输，但在代码里我们尽量自动获取或给个提示
             // 这里保留原逻辑，或者提示用户"请用 IP 访问"
             // host = '192.168.1.xxx'; 
        }
        
        const port = window.location.port;
        // 这个 URL 是手机扫码后打开的网页地址
        const mobileUrl = `http://${host}:${port}/mobile/${uuid.value}`;
        
        // 3. 生成二维码图片 Data URL
        qrUrl.value = await QRCode.toDataURL(mobileUrl, { 
            width: 200, margin: 0, 
            color: { dark: '#000000', light: '#ffffff00' } 
        });
        
        // 4. 开始轮询后端，检查此 UUID 是否被确认
        startPolling();
    } catch (e) {
        qrStatus.value = '无法连接服务器';
    }
};

// ...

const startPolling = () => {
    if(pollTimer) clearTimeout(pollTimer);
    
    const poll = async () => {
        // 如果切换了 Tab 或已经登录，停止轮询
        if(activeKey.value !== '2' || isLoggedIn.value) return; 
        
        try {
            // 5. 轮询接口 /api/check-status
            const res = await request.get(`/api/check-status?uuid=${uuid.value}`);
            const data = res.data || res; 
            
            // 6. 如果后端返回 SUCCESS
            if (data.status === 'SUCCESS') {
                isLoggedIn.value = true;
                message.success("扫码登录成功!");
                
                // 存用户信息 (后端最好在 check-status 成功时返回用户信息)
                if (data.user) {
                     localStorage.setItem("user", JSON.stringify(data.user));
                     localStorage.setItem('isLoggedIn', 'true');
                }
                
                setTimeout(() => router.push('/dashboard'), 1500);
            } else {
                // 7. 否则继续轮询，每 2 秒一次
                pollTimer = setTimeout(poll, 2000); 
            }
        } catch(e) {
            pollTimer = setTimeout(poll, 3000); // 出错延迟重试
        }
    };
    poll();
};

// Watch tab switch
watch(activeKey, (newVal) => {
    if (newVal === '2') {
        if (!uuid.value) initQrCode();
        else startPolling();
    } else {
        if(pollTimer) clearTimeout(pollTimer);
    }
});


// --- Animation Events (Original) ---
const onMouseMove = (event) => {
  if (isAvoiding.value) return;
  
  // 如果正在偷看，且用户动了鼠标(被发现了)，立即捂回去
  if (isPeeking.value) {
      isPeeking.value = false;
      clearTimeout(peekResetTimer);
  }

  const circle = document.querySelector(".blue-circle");
  if(!circle) return;
  const rect = circle.getBoundingClientRect();
  const centerX = rect.left + rect.width / 2;
  const centerY = rect.top + rect.height / 2;
  const angle = Math.atan2(event.clientY - centerY, event.clientX - centerX);
  const maxOffset = 4;
  const offsetX = Math.cos(angle) * maxOffset;
  const offsetY = Math.sin(angle) * maxOffset;
  pupilStyle.left = { transform: `translate(${offsetX}px, ${offsetY}px)` };
  pupilStyle.right = { transform: `translate(${offsetX}px, ${offsetY}px)` };
};

const onPasswordFocus = () => {
    isCovering.value = true; // 捂眼睛
    
    // 启动偷看逻辑
    peekInterval = setInterval(() => {
        if (!isCovering.value) return;
        // 随机偷看
        if (Math.random() > 0.5) {
            isPeeking.value = true;
            // 偷看 0.4秒后捂回去
            peekResetTimer = setTimeout(() => {
                isPeeking.value = false;
            }, 400);
        }
    }, 2500);
};

const onPasswordBlur = () => {
    isCovering.value = false;
    isPeeking.value = false;
    clearInterval(peekInterval);
    clearTimeout(peekResetTimer);
};

const handleLoginHover = () => { isSmiling.value = true; isSad.value = false; };
const handleLoginLeave = () => { isSmiling.value = false; };

onMounted(() => {
  startRoll.value = true; // 触发滚入动画
  
  // 1. 初始眼神看向左上角 (浏览器的返回/刷新按钮方向)
  // 注意：不能偏移太大导致眼珠飞出眼眶 (Max 4-5px)
  // 1. 初始眼神看向左上角 (浏览器的返回/刷新按钮方向)
  pupilStyle.left = { transform: "translate(-6px, -6px)" };
  pupilStyle.right = { transform: "translate(-6px, -6px)" };
  
  greeting.value = getGreeting();
  window.addEventListener("mousemove", onMouseMove);
  blinkInterval = setInterval(() => {
    isBlinking.value = true;
    setTimeout(() => isBlinking.value = false, 100);
  }, 2800);
  setTimeout(() => showWelcomeText.value = true, 500);

  // 滚入后打招呼 (Wave)
  setTimeout(() => { isWaving.value = true; }, 1200);
  setTimeout(() => { isWaving.value = false; }, 2500); // 挥手稍久一点

  // 滚入后表现出歉意/害羞 (Apologize)
  // setTimeout(() => { isSad.value = true; }, 1000); 
  // setTimeout(() => { isSad.value = false; }, 2500);
});

onUnmounted(() => {
  window.removeEventListener("mousemove", onMouseMove);
  clearInterval(blinkInterval);
  clearInterval(peekInterval);
  clearTimeout(peekResetTimer);
  if(pollTimer) clearTimeout(pollTimer);
});
</script>

<style scoped>
.login-wrapper {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}

/* 登录容器 - 全屏铺满 */
.login-container {
  display: flex;
  flex-wrap: wrap;
  background-color: #ffffff;
  width: 100%;
  height: 100vh; /* 强制全屏高度 */
  max-width: none;
  border-radius: 0;
  box-shadow: none;
  overflow: hidden;
  margin: 0;
  padding: 0;
}

/* 登录表单区域 */
.login-form {
  flex: 0.4; /* 占比缩小到 40% */
  padding: 40px 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 15px; /* 增加间距 */
  position: relative;
  min-width: 450px; /* 防止过窄 */
}
/* 强制所有表单项居中 */
.form-group {
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
}

:deep(.ant-form-item) {
    width: 100%;
    display: flex;
    justify-content: center;
    margin-bottom: 24px;
}

:deep(.ant-form-item-control-input-content) {
    display: flex;
    justify-content: center;
    width: 100%;
}

/* 输入框样式重构：扁平化毛玻璃风格 (仿 Dashboard Card) */

/* 1. 普通输入框 (无图标) */
:deep(.ant-input):not(.ant-input-affix-wrapper > input) {
  width: 320px !important;
  height: 48px !important;
  border-radius: 19px !important; /* 卡片圆角 */
  background: #FDFDFD !important; /* 极简白，去灰，仿卡片背景 */
  backdrop-filter: blur(28px) !important;
  -webkit-backdrop-filter: blur(8px) !important;
  border: 0.1px solid #d9d9d9 !important; /* 细边框 */
  box-shadow: 0px 0.5px 2.0px rgba(0, 0, 0, 0.1) !important; /* 卡片投影，扁平化 */
  text-align: left;
  padding-left: 20px !important;
  color: #333 !important;
  transition: all 0.3s ease !important;
}

/* 2. 密码输入框外层容器 (Affix Wrapper) */
:deep(.ant-input-affix-wrapper) {
  width: 320px !important;
  height: 48px !important;
  border-radius: 19px !important;
  background: #FDFDFD !important;
  backdrop-filter: blur(28px) !important;
  -webkit-backdrop-filter: blur(8px) !important;
  border: 0.1px solid #d9d9d9 !important;
  box-shadow: 0px 0.5px 2.0px rgba(0, 0, 0, 0.1) !important;
  padding-left: 20px !important;
  padding-right: 15px !important;
  transition: all 0.3s ease !important;
}

/* 3. 密码输入框内部 (去除所有边框背景，防止重叠) */
:deep(.ant-input-affix-wrapper > input.ant-input) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  height: 100% !important;
  padding: 0 !important;
  border-radius: 0 !important;
}

/* 4. 聚焦状态 / 悬停状态 (稍微提升投影) */
:deep(.ant-input:focus):not(.ant-input-affix-wrapper > input),
:deep(.ant-input-affix-wrapper-focused),
:deep(.ant-input:hover):not(.ant-input-affix-wrapper > input),
:deep(.ant-input-affix-wrapper:hover) {
    background: rgba(255, 255, 255, 0.95) !important;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08) !important;
    border-color: #ffcb51 !important;
}

/* 标题区域 */
h2 {
  font-size: 32px;
  font-weight: 800;
  color: #333;
  margin-top: 5px;
  letter-spacing: 1px;
}

.avatar {
    margin-bottom: 15px;
    filter: drop-shadow(0 4px 6px rgba(0,0,0,0.1));
}

/* 打字机 */
.typewriter {
  height: 24px;
  margin-bottom: 25px;
}
.typewriter-text {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

/* 记住密码行 - 严格对齐输入框 */
.form-remember {
  display: flex;
  justify-content: space-between;
  width: 320px;
  margin-bottom: 24px;
  font-size: 14px;
  color: #666;
  padding: 0 5px; /* 微调边缘 */
}

/* 按钮样式优化 */
:deep(.ant-btn-primary) {
    background: linear-gradient(135deg, #ffcb51 0%, #ffb347 100%) !important;
    border: none !important;
    box-shadow: 0 4px 15px rgba(255, 203, 81, 0.4) !important;
    font-weight: 600;
    letter-spacing: 2px;
    height: 45px !important;
    border-radius: 22.5px !important;
    transition: all 0.3s ease;
}
:deep(.ant-btn-primary:hover) {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(255, 203, 81, 0.6) !important;
}

/* Tabs 样式 */
.custom-tabs {
    width: 100%;
    max-width: 350px;
}
/* 调整 Tab 标题与下方内容的间距 (拉开距离) */
:deep(.ant-tabs-nav) {
    margin-bottom: 28px !important;
}
:deep(.ant-tabs-nav-wrap) {
    padding-bottom: 5px;
}
:deep(.ant-tabs-ink-bar) {
    background: #ffcb51 !important;
    height: 3px !important;
    border-radius: 3px;
    bottom: 5px !important; /* 调整下划线位置 */
}
:deep(.ant-tabs-tab) {
    font-weight: 400 !important; /* 强制不加粗 */
    transition: all 0.2s;
}
:deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
    color: #333 !important;
    font-weight: 400 !important; /* 彻底不加粗 */
    text-shadow: none !important; /* 移除任何模拟加粗 */
}
:deep(.ant-tabs-tab:hover) {
    color: #ffcb51 !important;
}

/* 
   ----------------------------------------
   扫码登录区域 - 极致简化 
   ----------------------------------------
*/
.qr-login-panel {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 300px;
    width: 100%;
    margin-top: -40px; /* 往上提 */
}

.qr-wrapper {
    /* 移除之前的框框，直接显示二维码 */
    padding: 0;
    margin: 0;
    background: transparent;
    border: none;
    box-shadow: none;
    backdrop-filter: none;
    display: flex;
    justify-content: center; /* 再次确保绝对居中 */
    align-items: center;
}

.qr-code-box img {
    width: 200px;
    height: 200px;
    display: block;
    /* 二维码本身不带白边，如果生成的图带白边，那是库的问题，
       这里可以用 clip-path 或者父容器 overflow hidden 来切掉边，
       或者在生成时设置 margin:0 */
    border-radius: 0 !important; /* 直角 */
    box-shadow: 0 10px 25px rgba(0,0,0,0.1); 
}

/* 隐藏所有多余文字链接 */
.scan-tip, .debug-link, .qr-status {
    display: none !important;
}

/* 扫码成功后的动画容器 */
.success-state {
    text-align: center;
    color: #4caf50;
    animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}

/* 扫码切换动画 */
.qr-login-panel {
    animation: slideUp 0.4s ease;
}
@keyframes slideUp {
    from { opacity: 0; transform: translateY(20px); }
    to { opacity: 1; transform: translateY(0); }
}

/* 右侧视觉区域 (保持) */
.login-visual {
  flex: 0.6; /* 占比扩大到 60% */
  background-color: #fcf9da; /* 也可以改成渐变色显得更高级 */
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
}

/* 增加一点装饰圆把右侧变得更有趣 */
.login-visual::after {
    content: '';
    position: absolute;
    width: 400px;
    height: 400px;
    background: rgba(255,255,255,0.2);
    border-radius: 50%;
    top: -100px;
    right: -100px;
}

/* 动画 CSS */
@keyframes rollIn {
    from { transform: translateX(120%) rotate(360deg); opacity: 0; }
    to { transform: translateX(0) rotate(0deg); opacity: 1; }
}

@keyframes liftUp {
    0% { transform: translateX(-50%) translateY(0); }
    100% { transform: translateX(-50%) translateY(-20px); }
}

@keyframes handLift {
    0% { transform: translateY(0); }
    100% { transform: translateY(-20px); }
}

@keyframes wave {
    0% { transform: rotate(0deg); }
    25% { transform: rotate(-20deg); }
    50% { transform: rotate(0deg); }
    75% { transform: rotate(-20deg); }
    100% { transform: rotate(0deg); }
}

.blue-circle {
    width: 220px; height: 220px; background-color: #ffcb51; border-radius: 50%; position: relative; z-index: 2;
    box-shadow: 0 10px 30px rgba(255, 203, 81, 0.5);
    opacity: 0; /* 初始隐藏，等待 roll-in */
    transition: all 0.5s cubic-bezier(0.68, -0.55, 0.27, 1.55);
}

.blue-circle.roll-in {
    animation: rollIn 1.2s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}

.eye { width: 22px; height: 22px; background-color: white; border-radius: 50%; position: absolute; display: flex; justify-content: center; align-items: center; overflow: hidden; transition: all 0.1s ease; }
.eye.blink { height: 2px; margin-top: 11px; }
.left-eye { left: 60px; top: 50px; }
.right-eye { right: 60px; top: 50px; }
.pupil { width: 10px; height: 10px; background-color: black; border-radius: 50%; transition: transform 0.1s ease-out; }

.mouth { position: absolute; bottom: 100px; left: 50%; width: 28px; height: 11px; background-color: black; border-radius: 60%; transform: translateX(-50%); overflow: hidden; transition: all 0.3s ease; }
.mouth.smile { border-radius: 0 0 100px 100px; height: 18px; bottom: 100px; }
.mouth.sad { border-radius: 100px 100px 0 0; height: 18px; bottom: 100px; }
.mouth.o-mouth { height: 15px; width: 15px; border-radius: 50%; bottom: 90px; } /* 举重时的小嘴 */

@keyframes waveHand {
    0% { transform: rotate(-140deg); }
    50% { transform: rotate(-120deg); }
    100% { transform: rotate(-140deg); }
}

@keyframes waveHand {
    0% { transform: rotate(-10deg); }
    50% { transform: rotate(10deg); }
    100% { transform: rotate(-10deg); }
}

/* 关节手臂容器 */
.limb {
    position: absolute;
    top: 130px; 
    width: 4px;
    z-index: 10;
}
.left-limb { left: 10px; }
.right-limb { right: 10px; }

/* 大臂/小臂 通用 */
.upper-arm, .forearm {
    width: 4px;
    background: #333;
    border-radius: 2px;
    transform-origin: top center;
    transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    position: relative;
    height: 0; /* 默认收起 */
}

/* 小臂在大臂末端 */
.forearm {
    position: absolute;
    top: 100%; /* 接在大臂下面 */
    left: 0;
}

/* 小手在小臂末端 */
.hand {
    width: 25px; height: 25px; /* 缩小手的大小 */
    background: #333; 
    border: none;      
    border-radius: 50%;
    position: absolute;
    bottom: -11px; /* 调整挂载位置 */
    left: 50%;
    transform: translateX(-50%);
    box-shadow: none; 
    opacity: 0; 
    transition: opacity 0.2s;
}

/* --- 状态逻辑 --- */

/* 1. 显形 (默认举重长度 70, 挥手长度 60) */
.limb:not(.hidden) .upper-arm { height: 70px; } 
.limb:not(.hidden) .forearm { height: 70px; }
.limb:not(.hidden) .hand { opacity: 1; }

/* 2. 挥手 (右手) - 缩短一点 */
.right-limb.wave .upper-arm {
    height: 55px !important;
    transform: rotate(-140deg) !important; 
}
.right-limb.wave .forearm {
    height: 55px !important;
    transform: rotate(0deg); 
    animation: waveHand 1s infinite ease-in-out; 
}

/* 3. 捂眼睛 (宽肘部 \_/, 大臂横展，小臂直指眼睛) */
.left-limb.cover .upper-arm { 
    height: 35px; /* 短横臂 */
    transform: rotate(95deg); /* 指向左侧(略下) */
}
.left-limb.cover .forearm { 
    height: 114px; /* 长小臂够到眼睛 */
    transform: rotate(140deg); /* 折向内上 */
}

.right-limb.cover .upper-arm { 
    height: 35px;
    transform: rotate(-95deg); 
}
.right-limb.cover .forearm { 
    height: 114px;
    transform: rotate(-140deg); 
}

/* 偷看状态 (Peek) - 轻轻向上抬起露出眼睛 */
.left-limb.cover.peek .forearm { transform: rotate(90deg); }
.right-limb.cover.peek .forearm { transform: rotate(-90deg); }

/* 4. 举重 (双手直举, 伸长) */
.limb.lift .upper-arm { height: 70px; transform: rotate(180deg); }
.limb.lift .forearm { height: 70px; transform: rotate(0deg); }
.limb.lift {
    animation: handLift 0.6s infinite alternate;
}

.limb.hidden .hand { opacity: 0; }


/* 哑铃 CSS */
.dumbbell {
    position: absolute;
    top: -50px;
    left: 50%;
    transform: translateX(-50%);
    width: 140px;
    height: 30px;
    display: flex;
    align-items: center;
    justify-content: center;
    animation: liftUp 0.6s infinite alternate;
    z-index: 12;
}
.dumbbell .bar { width: 100px; height: 8px; background: #333; border-radius: 4px; }
.dumbbell .weight { width: 15px; height: 40px; background: #333; border-radius: 4px; position:absolute; }
.dumbbell .weight.left { left: 10px; } /* Adjust pos */
.dumbbell .weight.right { right: 10px; }
</style>
