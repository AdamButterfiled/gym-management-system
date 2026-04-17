<template>
  <div class="login-wrapper">
    <div class="login-shell">
      <section class="login-form-pane">
        <div class="brand-row">
          <div class="brand-lockup">
            <img class="brand-logo" :src="lightLogo" alt="GMS logo" />
            <div class="brand-copy">
              <span class="brand-kicker">Gym Venue Reservation System</span>
              <strong>健身房场馆预约系统</strong>
            </div>
          </div>
        </div>

        <div class="login-panel">
          <div class="profile-chip">
            <div class="avatar-shell">
              <a-avatar :src="avatarImg" style="width: 70px; height: 70px" />
            </div>
            <div class="profile-copy">
              <span class="profile-role">{{ profileRoleLabel }}</span>
              <strong>Adam</strong>
            </div>
          </div>

          <div class="panel-heading">
            <h1>欢迎回来</h1>
          </div>

          <div class="typewriter">
            <span v-if="showWelcomeText" class="typewriter-text">
              {{ typedGreetingText }}
            </span>

            <Transition name="hello-chip">
              <WindowsHelloIndicator
                v-if="showHelloIndicator"
                class="typewriter-indicator"
                :reduced-motion="reducedMotion"
              />
            </Transition>
          </div>

          <a-tabs v-model:activeKey="activeKey" centered class="custom-tabs">
            <a-tab-pane key="1" tab="密码登录">
              <a-form class="form-group" @submit.prevent="handleLogin">
                <a-form-item name="account" @click="onAccountFocus">
                  <a-input
                    v-model:value="account"
                    allow-clear
                    placeholder="请输入账号"
                    @mousedown="onAccountFocus"
                    @focus="onAccountFocus"
                  />
                </a-form-item>

                <a-form-item name="password">
                  <a-input-password
                    v-model:value="password"
                    allow-clear
                    placeholder="请输入密码"
                    @mousedown="onPasswordPointerDown"
                    @focus="onPasswordFocus"
                    @blur="onPasswordBlur"
                  >
                    <template #iconRender="visible">
                      <EyeTwoTone v-if="visible" />
                      <EyeInvisibleOutlined v-else />
                    </template>
                  </a-input-password>
                </a-form-item>

                <div class="form-remember">
                  <a-checkbox v-model:checked="rememberMe">记住账号密码</a-checkbox>
                  <a href="#" class="forgot-password">忘记密码?</a>
                </div>

                <a-form-item class="login-button-container">
                  <a-button
                    block
                    class="login-submit"
                    html-type="submit"
                    type="primary"
                  >
                    登录
                  </a-button>
                </a-form-item>
              </a-form>
            </a-tab-pane>

            <a-tab-pane key="2" tab="扫码登录">
              <div class="qr-login-panel">
                <div v-if="!isLoggedIn" class="qr-content">
                  <div class="qr-wrapper">
                    <div v-if="qrUrl" class="qr-code-box">
                      <img :src="qrUrl" alt="QR Code" />
                    </div>
                    <div v-else class="loading-state">
                      <a-spin />
                      <p>{{ qrStatus === '请扫码' ? '正在获取二维码...' : qrStatus }}</p>
                    </div>
                  </div>
                  <p class="scan-tip">使用手机 APP 进行扫码</p>
                </div>

                <div v-else class="success-state">
                  <div class="success-icon">✦</div>
                  <h3>登录成功</h3>
                  <p>小熊正在带你进入系统...</p>
                </div>
              </div>
            </a-tab-pane>
          </a-tabs>

        </div>
      </section>

      <section
        class="login-visual"
        @mouseleave="clearMascotPointer"
        @mousemove="updateMascotPointer"
      >
        <div class="visual-copy">
          <h2>{{ dailyVisualQuote }}</h2>
        </div>

        <div class="visual-orbit visual-orbit-a" aria-hidden="true"></div>
        <div class="visual-orbit visual-orbit-b" aria-hidden="true"></div>
        <div class="visual-watermark" aria-hidden="true">GMS</div>

        <div class="mascot-stage">
          <PolarBearLoginMascot
            :mode="mascotMode"
            :idle-interact-tick="accountInteractTick"
            :interrupt-tick="mascotInterruptTick"
            :pointer="mascotPointer"
            :reduced-motion="reducedMotion"
            @double-click="handleMascotDoubleClick"
            @intro-complete="handleMascotIntroComplete"
            @error-cycle-complete="handleMascotErrorCycleComplete"
            @success-cycle-complete="handleMascotSuccessCycleComplete"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { EyeInvisibleOutlined, EyeTwoTone } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { useRoute, useRouter } from 'vue-router';
import QRCode from 'qrcode';
import PolarBearLoginMascot from '@/components/PolarBearLoginMascot.vue';
import WindowsHelloIndicator from '@/components/WindowsHelloIndicator.vue';
import avatarImg from '@/assets/images-_2_.jpg';
import lightLogo from '@/assets/Light_logo.svg';
import request from '@/request';

const router = useRouter();
const route = useRoute();

const RESTING_POINTER = { x: -0.4, y: -0.24 };
const VALID_MASCOT_MODES = new Set(['intro', 'idle', 'password', 'error', 'success']);
const ROLE_LABELS = {
  ADMIN: '管理员',
  STAFF: '员工',
  COACH: '教练',
  MEMBER: '会员'
};
const DAILY_VISUAL_QUOTES = [
  '愿你胸中有丘壑，眼底有星河，脚下有生风的路。',
  '请把今天走成一首诗，让平凡也生出金色回响。',
  '纵有山高水长，心中有火的人，终会踏月而归。',
  '愿你以清醒为灯，以热望为舟，穿越所有漫长夜色。',
  '把晨光别在衣襟上，去做那个与世界温柔交锋的人。',
  '当你仍肯仰望天光，命运就会为你让出一条路。',
  '请把沉默的日子也酿成酒，敬未来辽阔，敬此刻勇敢。',
  '真正的光，不在远方喧哗处，而在你未曾熄灭的心里。',
  '愿你一路披霜戴月，仍能在黎明之前守住炽热与高贵。',
  '当群山沉默，愿你成为自己的风，吹开新的清晨。',
  '把理想写进骨骼，把温柔留给人间，把锋芒交给远方。',
  '愿你在尘世奔赴时，仍保有抬头看云的从容与辽阔。'
];
const DAILY_QUOTE_STORAGE_KEY = 'login_visual_quote_history_v1';
const QUOTE_REJECT_PATTERNS = [
  /绝望/,
  /死亡/,
  /仇恨/,
  /痛苦/,
  /悲伤/,
  /害怕/,
  /受伤/,
  /孤独/,
  /坠落/,
  /黑暗/,
  /失败/,
  /腐朽/,
  /哭泣/,
  /眼泪/,
  /哀伤/,
  /荒芜/,
  /迷失/
];

const activeKey = ref('1');
const showWelcomeText = ref(false);
const showHelloIndicator = ref(false);
const greeting = ref('');
const typedGreetingText = ref('');
const account = ref('');
const password = ref('');
const rememberMe = ref(false);
const rememberedRole = ref('');
const accountInteractTick = ref(0);
const mascotInterruptTick = ref(0);
const dailyVisualQuote = ref(DAILY_VISUAL_QUOTES[0]);

const mascotMode = ref('intro');
const mascotPointer = ref({ ...RESTING_POINTER });
const reducedMotion = ref(false);
const mascotLocked = ref(false);
const isPasswordFocused = ref(false);
const pendingRedirect = ref('');
const introCompleted = ref(false);

const uuid = ref('');
const qrUrl = ref('');
const qrStatus = ref('请扫码');
const isLoggedIn = ref(false);

let pollTimer = 0;
let welcomeTimer = 0;
let typewriterTimer = 0;
let helloIndicatorTimer = 0;
let motionQuery = null;
let motionListener = null;
let quoteAbortController = null;

const forcedMascotMode = computed(() => {
  const queryValue = Array.isArray(route.query.bearMode)
    ? route.query.bearMode[0]
    : route.query.bearMode;

  return typeof queryValue === 'string' && VALID_MASCOT_MODES.has(queryValue)
    ? queryValue
    : '';
});

const profileRoleLabel = computed(() => {
  const role = inferRoleFromAccount(account.value) || rememberedRole.value;
  return ROLE_LABELS[role] || '用户';
});
const greetingMessage = computed(() => `${greeting.value}，Adam。今天继续保持节奏。`);

function clearTimer(timerId) {
  if (timerId) {
    window.clearTimeout(timerId);
  }
}

function getPostLoginPath() {
  const redirect = Array.isArray(route.query.redirect)
    ? route.query.redirect[0]
    : route.query.redirect;

  if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('/login')) {
    return redirect;
  }

  return '/dashboard';
}

function getGreeting() {
  const hour = new Date().getHours();
  if (hour < 9) return '早上好';
  if (hour < 11) return '上午好';
  if (hour < 13) return '中午好';
  if (hour < 18) return '下午好';
  return '晚上好';
}

function getLocalDateKey(date = new Date()) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function hashDateKey(dateKey) {
  return Array.from(dateKey).reduce((total, char) => total + char.charCodeAt(0), 0);
}

function sanitizeQuote(text) {
  return typeof text === 'string' ? text.replace(/\s+/g, ' ').trim() : '';
}

function readDailyQuoteHistory() {
  try {
    const history = JSON.parse(localStorage.getItem(DAILY_QUOTE_STORAGE_KEY) || '{}');
    return history && typeof history === 'object' ? history : {};
  } catch (error) {
    return {};
  }
}

function writeDailyQuoteHistory(history) {
  const entries = Object.entries(history).sort(([left], [right]) => right.localeCompare(left));
  const trimmed = Object.fromEntries(entries.slice(0, 45));
  localStorage.setItem(DAILY_QUOTE_STORAGE_KEY, JSON.stringify(trimmed));
}

function persistDailyVisualQuote(quote) {
  const dateKey = getLocalDateKey();
  const history = readDailyQuoteHistory();
  history[dateKey] = quote;
  writeDailyQuoteHistory(history);
}

function isPreferredQuote(quote) {
  return Boolean(
    quote
      && quote.length >= 8
      && quote.length <= 32
      && QUOTE_REJECT_PATTERNS.every((pattern) => !pattern.test(quote))
  );
}

function pickFallbackQuote(dateKey, history) {
  const usedQuotes = new Set(Object.values(history));
  const startIndex = hashDateKey(dateKey) % DAILY_VISUAL_QUOTES.length;

  for (let offset = 0; offset < DAILY_VISUAL_QUOTES.length; offset += 1) {
    const candidate = DAILY_VISUAL_QUOTES[(startIndex + offset) % DAILY_VISUAL_QUOTES.length];
    if (!usedQuotes.has(candidate)) {
      return candidate;
    }
  }

  return DAILY_VISUAL_QUOTES[startIndex];
}

async function loadDailyVisualQuote() {
  const dateKey = getLocalDateKey();
  const quoteHistory = readDailyQuoteHistory();

  const cachedQuote = sanitizeQuote(quoteHistory[dateKey]);
  if (isPreferredQuote(cachedQuote)) {
    dailyVisualQuote.value = cachedQuote;
    return;
  }

  delete quoteHistory[dateKey];

  let timeoutId = 0;

  try {
    if (quoteAbortController) {
      quoteAbortController.abort();
    }

    quoteAbortController = new AbortController();
    timeoutId = window.setTimeout(() => quoteAbortController.abort(), 3200);

    const response = await fetch(
      'https://v1.hitokoto.cn/?c=d&c=i&c=k&max_length=28&encode=json',
      { signal: quoteAbortController.signal }
    );

    if (!response.ok) {
      throw new Error('quote request failed');
    }

    const data = await response.json();
    const networkQuote = sanitizeQuote(data.hitokoto);
    const quoteAlreadyUsed = Object.entries(quoteHistory)
      .some(([day, quote]) => day !== dateKey && quote === networkQuote);

    if (isPreferredQuote(networkQuote) && !quoteAlreadyUsed) {
      dailyVisualQuote.value = networkQuote;
      persistDailyVisualQuote(networkQuote);
      return;
    }
  } catch (error) {
    // Fallback handled below.
  } finally {
    clearTimer(timeoutId);
    quoteAbortController = null;
  }

  const fallbackQuote = pickFallbackQuote(dateKey, quoteHistory);
  dailyVisualQuote.value = fallbackQuote;
  persistDailyVisualQuote(fallbackQuote);
}

function rotateDailyVisualQuote() {
  const alternatives = DAILY_VISUAL_QUOTES.filter((quote) => quote !== dailyVisualQuote.value);
  if (!alternatives.length) {
    return;
  }

  const currentIndex = DAILY_VISUAL_QUOTES.indexOf(dailyVisualQuote.value);
  const nextIndex = currentIndex >= 0
    ? (currentIndex + 1) % DAILY_VISUAL_QUOTES.length
    : hashDateKey(getLocalDateKey()) % alternatives.length;
  const nextQuote = DAILY_VISUAL_QUOTES[nextIndex] === dailyVisualQuote.value
    ? alternatives[0]
    : DAILY_VISUAL_QUOTES[nextIndex];

  dailyVisualQuote.value = nextQuote;
  persistDailyVisualQuote(nextQuote);
}

function normalizeRole(role) {
  return typeof role === 'string' ? role.trim().toUpperCase() : '';
}

function inferRoleFromAccount(value) {
  const normalized = typeof value === 'string' ? value.trim().toLowerCase() : '';
  if (!normalized) return '';
  if (normalized.includes('admin') || normalized.includes('管理员')) return 'ADMIN';
  if (normalized.includes('coach') || normalized.includes('教练')) return 'COACH';
  if (normalized.includes('staff') || normalized.includes('员工')) return 'STAFF';
  if (normalized.includes('member') || normalized.includes('会员')) return 'MEMBER';
  return '';
}

function getStoredUserRole() {
  try {
    const storedUser = JSON.parse(localStorage.getItem('user') || '{}');
    return normalizeRole(storedUser.role);
  } catch (error) {
    return '';
  }
}

function rememberUserRole(user) {
  const role = normalizeRole(user && user.role);
  if (role) {
    rememberedRole.value = role;
  }
}

function getAmbientMascotMode() {
  if (forcedMascotMode.value) {
    return forcedMascotMode.value;
  }

  if (activeKey.value !== '1') {
    return 'idle';
  }
  return isPasswordFocused.value ? 'password' : 'idle';
}

function interruptMascotSequence() {
  mascotInterruptTick.value += 1;
}

function interruptMascotIntro(nextMode = 'idle') {
  if (forcedMascotMode.value) {
    return;
  }

  interruptMascotSequence();

  if (!introCompleted.value || mascotMode.value === 'intro') {
    introCompleted.value = true;
    mascotLocked.value = false;
    mascotMode.value = nextMode;
  }
}

function queueIdleInteract() {
  nextTick(() => {
    accountInteractTick.value += 1;
  });
}

function playMascotSmileInteract() {
  if (forcedMascotMode.value) {
    return;
  }

  interruptMascotIntro('idle');
  mascotLocked.value = false;
  mascotMode.value = 'idle';
  queueIdleInteract();
}

function syncMascotToAmbient() {
  if (forcedMascotMode.value) {
    mascotLocked.value = false;
    mascotMode.value = forcedMascotMode.value;
    return;
  }

  if (!introCompleted.value) {
    return;
  }

  if (!mascotLocked.value && mascotMode.value !== 'intro') {
    mascotMode.value = getAmbientMascotMode();
  }
}

function triggerMascotError() {
  if (forcedMascotMode.value) {
    mascotMode.value = forcedMascotMode.value;
    return;
  }

  introCompleted.value = true;
  mascotLocked.value = true;
  mascotMode.value = 'error';
}

function triggerMascotSuccess() {
  if (forcedMascotMode.value) {
    mascotMode.value = forcedMascotMode.value;
    return;
  }

  introCompleted.value = true;
  mascotLocked.value = true;
  mascotMode.value = 'success';
}

function restoreRememberedCredentials() {
  rememberedRole.value = getStoredUserRole();

  const remembered = localStorage.getItem('is_remember') === 'true';
  rememberMe.value = remembered;

  if (!remembered) {
    return;
  }

  account.value = localStorage.getItem('remember_account') || '';
  const encodedPassword = localStorage.getItem('remember_password');

  if (!encodedPassword) {
    return;
  }

  try {
    password.value = window.atob(encodedPassword);
  } catch (error) {
    password.value = '';
  }
}

function startGreetingTypewriter() {
  clearTimer(welcomeTimer);
  clearTimer(typewriterTimer);
  clearTimer(helloIndicatorTimer);

  typedGreetingText.value = '';
  showWelcomeText.value = false;
  showHelloIndicator.value = false;

  const message = greetingMessage.value;
  let currentIndex = 0;

  const typeNextCharacter = () => {
    typedGreetingText.value = message.slice(0, currentIndex + 1);
    currentIndex += 1;

    if (currentIndex >= message.length) {
      helloIndicatorTimer = window.setTimeout(() => {
        showHelloIndicator.value = true;
      }, 180);
      return;
    }

    const currentChar = message[currentIndex - 1];
    const delay = /[，。！？]/.test(currentChar) ? 150 : 48;
    typewriterTimer = window.setTimeout(typeNextCharacter, delay);
  };

  welcomeTimer = window.setTimeout(() => {
    showWelcomeText.value = true;
    typeNextCharacter();
  }, 240);
}

async function handleLogin() {
  try {
    const res = await request.post('/user/login', {
      username: account.value,
      password: password.value
    });

    if (res.code === 200) {
      message.success('登录成功！');
      rememberUserRole(res.data);
      localStorage.setItem('user', JSON.stringify(res.data));
      localStorage.setItem('isLoggedIn', 'true');

      if (rememberMe.value) {
        localStorage.setItem('remember_account', account.value);
        localStorage.setItem('remember_password', window.btoa(password.value));
        localStorage.setItem('is_remember', 'true');
      } else {
        localStorage.removeItem('remember_account');
        localStorage.removeItem('remember_password');
        localStorage.removeItem('is_remember');
      }

      pendingRedirect.value = getPostLoginPath();
      triggerMascotSuccess();
      return;
    }

    message.error(res.msg || '账号或密码错误！');
    triggerMascotError();
  } catch (error) {
    message.error(`Login failed: ${error}`);
    triggerMascotError();
  }
}

async function initQrCode() {
  try {
    qrStatus.value = '请扫码';
    const res = await request.get('/api/qrcode');
    let uuidValue = '';

    if (res.data && res.data.uuid) {
      uuidValue = res.data.uuid;
    } else if (res.uuid) {
      uuidValue = res.uuid;
    }

    if (!uuidValue) {
      qrStatus.value = '获取失败';
      return;
    }

    uuid.value = uuidValue;

    const host = window.location.hostname;
    const port = window.location.port;
    const mobileUrl = `http://${host}:${port}/mobile/${uuid.value}`;

    qrUrl.value = await QRCode.toDataURL(mobileUrl, {
      width: 200,
      margin: 0,
      color: {
        dark: '#000000',
        light: '#ffffff00'
      }
    });

    startPolling();
  } catch (error) {
    qrStatus.value = '无法连接服务器';
  }
}

function stopPolling() {
  clearTimer(pollTimer);
  pollTimer = 0;
}

function startPolling() {
  stopPolling();

  const poll = async () => {
    if (activeKey.value !== '2' || isLoggedIn.value) {
      return;
    }

    try {
      const res = await request.get(`/api/check-status?uuid=${uuid.value}`);
      const data = res.data || res;

      if (data.status === 'SUCCESS') {
        isLoggedIn.value = true;
        message.success('扫码登录成功!');

        if (data.user) {
          rememberUserRole(data.user);
          localStorage.setItem('user', JSON.stringify(data.user));
          localStorage.setItem('isLoggedIn', 'true');
        }

        pendingRedirect.value = getPostLoginPath();
        triggerMascotSuccess();
        return;
      }

      pollTimer = window.setTimeout(poll, 2000);
    } catch (error) {
      pollTimer = window.setTimeout(poll, 3000);
    }
  };

  poll();
}

function onAccountFocus() {
  if (forcedMascotMode.value || activeKey.value !== '1') {
    return;
  }

  isPasswordFocused.value = false;
  playMascotSmileInteract();
}

function onPasswordFocus() {
  if (forcedMascotMode.value || activeKey.value !== '1') {
    return;
  }

  isPasswordFocused.value = true;
  mascotLocked.value = false;

  if (!introCompleted.value || mascotMode.value === 'intro') {
    interruptMascotIntro('password');
    return;
  }

  mascotMode.value = 'password';
}

function onPasswordPointerDown() {
  if (forcedMascotMode.value || activeKey.value !== '1') {
    return;
  }

  isPasswordFocused.value = true;

  if (!introCompleted.value || mascotMode.value === 'intro') {
    interruptMascotIntro('password');
    mascotLocked.value = false;
    mascotMode.value = 'password';
  }
}

function onPasswordBlur() {
  if (forcedMascotMode.value) {
    return;
  }

  isPasswordFocused.value = false;
  syncMascotToAmbient();
}

function updateMascotPointer(event) {
  if (mascotMode.value === 'password') {
    return;
  }

  const rect = event.currentTarget.getBoundingClientRect();
  const normalizedX = ((event.clientX - rect.left) / rect.width - 0.5) * 2;
  const normalizedY = ((event.clientY - rect.top) / rect.height - 0.42) * 2;

  mascotPointer.value = {
    x: Math.max(-1, Math.min(1, normalizedX)),
    y: Math.max(-1, Math.min(1, normalizedY))
  };
}

function clearMascotPointer() {
  mascotPointer.value = { ...RESTING_POINTER };
}

function handleMascotIntroComplete() {
  if (forcedMascotMode.value) {
    mascotMode.value = forcedMascotMode.value;
    return;
  }

  introCompleted.value = true;
  if (!mascotLocked.value && mascotMode.value === 'intro') {
    mascotMode.value = getAmbientMascotMode();
  }
}

function handleMascotErrorCycleComplete() {
  if (forcedMascotMode.value) {
    return;
  }

  mascotLocked.value = false;
  mascotMode.value = getAmbientMascotMode();
}

function handleMascotSuccessCycleComplete() {
  if (forcedMascotMode.value) {
    return;
  }

  if (pendingRedirect.value) {
    router.replace(pendingRedirect.value);
  }
}

function handleMascotDoubleClick() {
  rotateDailyVisualQuote();
}

watch(
  forcedMascotMode,
  (mode) => {
    if (mode) {
      introCompleted.value = true;
      mascotLocked.value = false;
      isPasswordFocused.value = mode === 'password';
      mascotMode.value = mode;
      return;
    }

    if (!introCompleted.value) {
      mascotMode.value = 'intro';
      return;
    }

    syncMascotToAmbient();
  },
  { immediate: true }
);

watch(activeKey, (newValue) => {
  if (newValue === '2') {
    isPasswordFocused.value = false;
    if (!uuid.value) {
      initQrCode();
    } else {
      startPolling();
    }

    playMascotSmileInteract();
  } else {
    stopPolling();
  }

  syncMascotToAmbient();
});

onMounted(() => {
  greeting.value = getGreeting();
  restoreRememberedCredentials();
  clearMascotPointer();
  startGreetingTypewriter();
  loadDailyVisualQuote();

  if (typeof window.matchMedia === 'function') {
    motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    reducedMotion.value = motionQuery.matches;
    motionListener = (event) => {
      reducedMotion.value = event.matches;
    };

    if (typeof motionQuery.addEventListener === 'function') {
      motionQuery.addEventListener('change', motionListener);
    } else if (typeof motionQuery.addListener === 'function') {
      motionQuery.addListener(motionListener);
    }
  }
});

onUnmounted(() => {
  clearTimer(welcomeTimer);
  clearTimer(typewriterTimer);
  clearTimer(helloIndicatorTimer);
  stopPolling();

  if (quoteAbortController) {
    quoteAbortController.abort();
  }

  if (motionQuery && motionListener) {
    if (typeof motionQuery.removeEventListener === 'function') {
      motionQuery.removeEventListener('change', motionListener);
    } else if (typeof motionQuery.removeListener === 'function') {
      motionQuery.removeListener(motionListener);
    }
  }
});
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  padding: 0;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.94), transparent 24%),
    radial-gradient(circle at bottom right, rgba(17, 17, 17, 0.06), transparent 30%),
    linear-gradient(135deg, #f7f7f5 0%, #fcfcfb 46%, #f0f0ee 100%);
}

.login-wrapper::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(17, 17, 17, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(17, 17, 17, 0.03) 1px, transparent 1px);
  background-size: 42px 42px;
  opacity: 0.46;
  pointer-events: none;
}

.login-wrapper::after {
  content: '';
  position: absolute;
  top: -160px;
  right: -120px;
  width: 420px;
  height: 420px;
  border-radius: var(--mono-radius-pill);
  background: rgba(255, 255, 255, 0.8);
  filter: blur(40px);
  pointer-events: none;
}

.login-shell {
  position: relative;
  z-index: 1;
  display: flex;
  min-height: 100vh;
  width: 100%;
  overflow: visible;
  background: transparent;
}

.login-form-pane {
  flex: 0 0 42.5%;
  min-width: 420px;
  padding: clamp(30px, 4vw, 52px);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 36px;
  border-right: 1px solid rgba(17, 17, 17, 0.06);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.68) 0%, rgba(255, 255, 255, 0.42) 100%);
}

.brand-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand-lockup {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.brand-logo {
  width: 76px;
  height: auto;
  display: block;
  object-fit: contain;
  filter: grayscale(1) contrast(1.08);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.brand-copy strong {
  color: var(--mono-text);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.brand-kicker {
  color: #59636f;
  font-size: 11px;
  font-weight: 400;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.profile-role {
  color: #59636f;
  font-size: 12px;
  font-weight: 400;
  letter-spacing: 0.08em;
}

.login-panel {
  position: relative;
  width: min(100%, 430px);
  min-height: clamp(620px, 78vh, 824px);
  min-height: clamp(620px, 78dvh, 824px);
  padding: 34px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  border: 1px solid transparent;
  border-radius: var(--mono-radius-xl);
  background: transparent;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  box-shadow: none;
}

.login-panel::before {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: var(--mono-radius-xl);
  background: transparent;
  pointer-events: none;
}

.profile-chip,
.panel-heading,
.typewriter,
.custom-tabs {
  position: relative;
  z-index: 1;
}

.profile-chip {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 30px;
}

.avatar-shell {
  position: relative;
  width: 78px;
  height: 78px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--mono-radius-pill);
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: rgba(255, 255, 255, 0.58);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}

.profile-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.profile-copy strong {
  color: var(--mono-text);
  font-size: 24px;
  font-weight: 650;
  letter-spacing: -0.03em;
}

.panel-heading {
  margin-bottom: 14px;
}

.panel-heading h1 {
  margin: 0;
  color: var(--mono-text);
  font-family: var(--app-art-font);
  font-size: clamp(42px, 4vw, 56px);
  line-height: 0.96;
  font-weight: 600;
  letter-spacing: -0.05em;
}

.typewriter {
  min-height: 44px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.typewriter-text {
  color: #5f6874;
  font-size: 15px;
  font-weight: 400;
  letter-spacing: 0;
  line-height: 1.5;
}

.typewriter-indicator {
  margin-left: 2px;
}

.hello-chip-enter-active,
.hello-chip-leave-active {
  transition:
    opacity 0.28s ease,
    transform 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}

.hello-chip-enter-from,
.hello-chip-leave-to {
  opacity: 0;
  transform: translateY(4px) scale(0.9);
}

.form-group {
  width: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.ant-form-item) {
  width: 100%;
  margin-bottom: 22px;
}

:deep(.ant-form-item-control-input-content) {
  width: 100%;
}

:deep(.ant-input):not(.ant-input-affix-wrapper > input) {
  width: 100% !important;
  height: 54px !important;
  padding-left: 20px !important;
  color: var(--mono-control-text) !important;
  text-align: left;
  border: 1px solid var(--mono-control-border) !important;
  border-radius: var(--mono-radius-lg) !important;
  background: rgba(255, 255, 255, 0.5) !important;
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  box-shadow: none !important;
  transition: border-color 0.25s ease, background-color 0.25s ease, box-shadow 0.25s ease !important;
}

:deep(.ant-input-affix-wrapper) {
  width: 100% !important;
  height: 54px !important;
  padding-left: 20px !important;
  padding-right: 15px !important;
  border: 1px solid var(--mono-control-border) !important;
  border-radius: var(--mono-radius-lg) !important;
  background: rgba(255, 255, 255, 0.5) !important;
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  box-shadow: none !important;
  transition: border-color 0.25s ease, background-color 0.25s ease, box-shadow 0.25s ease !important;
}

:deep(.ant-input-affix-wrapper > input.ant-input) {
  height: 100% !important;
  padding: 0 !important;
  border: none !important;
  border-radius: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
}

:deep(.ant-input::placeholder),
:deep(.ant-input-affix-wrapper > input.ant-input::placeholder) {
  color: var(--mono-control-text-muted) !important;
}

:deep(.ant-input:focus):not(.ant-input-affix-wrapper > input),
:deep(.ant-input:hover):not(.ant-input-affix-wrapper > input),
:deep(.ant-input-affix-wrapper-focused),
:deep(.ant-input-affix-wrapper:hover) {
  border-color: var(--mono-control-border-strong) !important;
  box-shadow: var(--mono-focus-ring) !important;
  background: rgba(255, 255, 255, 0.74) !important;
}

.form-remember {
  width: 100%;
  padding: 0 2px;
  margin-bottom: 28px;
  display: flex;
  justify-content: space-between;
  color: #5f6874;
  font-size: 14px;
  font-weight: 400;
}

.forgot-password {
  color: #5f6874;
  font-weight: 400;
  transition: color 0.2s ease;
}

.forgot-password:hover {
  color: var(--mono-text);
}

:deep(.ant-checkbox + span) {
  color: #5f6874;
  font-weight: 400;
}

:deep(.ant-checkbox-checked .ant-checkbox-inner) {
  background: var(--mono-primary-bg);
  border-color: var(--mono-primary-bg);
}

.login-submit {
  width: 100%;
  height: 52px !important;
  margin-top: 18px;
  border: none !important;
  border-radius: var(--mono-radius-lg) !important;
  background: linear-gradient(135deg, #0f0f10 0%, #252527 100%) !important;
  box-shadow: none !important;
  font-size: 17px;
  font-weight: 400;
  letter-spacing: 0.08em;
}

:deep(.login-submit:hover) {
  transform: translateY(-1px);
  box-shadow: none !important;
}

:deep(.login-submit:focus),
:deep(.login-submit:active) {
  box-shadow: none !important;
}

.custom-tabs {
  width: 100%;
  margin-top: clamp(28px, 5vh, 52px);
  margin-top: clamp(28px, 5dvh, 52px);
}

:deep(.custom-tabs .ant-tabs-nav) {
  margin-bottom: 30px !important;
}

:deep(.custom-tabs .ant-tabs-nav::before) {
  display: none;
}

:deep(.custom-tabs .ant-tabs-nav-wrap) {
  padding: 0;
}

:deep(.custom-tabs .ant-tabs-ink-bar) {
  display: none !important;
}

:deep(.custom-tabs .ant-tabs-nav-list) {
  width: 100%;
  padding: 4px;
  gap: 4px;
  border: 1px solid var(--mono-line);
  border-radius: var(--mono-radius-pill);
  background: rgba(255, 255, 255, 0.45);
}

:deep(.custom-tabs .ant-tabs-tab) {
  flex: 1 1 0;
  justify-content: center;
  margin: 0 !important;
  padding: 10px 16px !important;
  border-radius: var(--mono-radius-pill);
  color: #66717c !important;
  font-size: 13px;
  font-weight: 400 !important;
  transition: background-color 0.2s ease, color 0.2s ease;
}

:deep(.custom-tabs .ant-tabs-tab-active) {
  background: var(--mono-primary-bg);
}

:deep(.custom-tabs .ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: var(--mono-primary-text) !important;
  text-shadow: none !important;
}

:deep(.custom-tabs .ant-tabs-tab:hover) {
  color: var(--mono-text) !important;
}

.qr-login-panel {
  width: 100%;
  min-height: 310px;
  margin-top: -4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  animation: slide-up 0.4s ease;
}

.qr-content {
  width: 100%;
}

.qr-wrapper {
  width: 100%;
  min-height: 230px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 0;
  background: transparent;
}

.qr-code-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.qr-code-box img {
  display: block;
  width: 204px;
  height: 204px;
  padding: 14px;
  border: 1px solid var(--mono-line);
  border-radius: var(--mono-radius-xl);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
}

.scan-tip,
.debug-link,
.qr-status,
.loading-state p {
  color: var(--mono-text-secondary);
  font-size: 13px;
  text-align: center;
}

.scan-tip {
  margin: 24px 0 0;
}

.debug-link {
  text-align: center;
}

.debug-link :deep(a) {
  color: var(--mono-text-secondary);
}

.loading-state {
  display: grid;
  gap: 16px;
  place-items: center;
}

.success-state {
  color: var(--mono-text);
  text-align: center;
  animation: fade-in 0.4s ease;
}

.success-icon {
  margin-bottom: 12px;
  font-size: 28px;
}

.success-state h3 {
  margin-bottom: 8px;
  font-size: 26px;
}

.success-state p {
  color: var(--mono-text-secondary);
}

.login-visual {
  flex: 1;
  min-width: 0;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  padding: clamp(32px, 4vw, 52px);
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.9), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.02) 100%);
}

.login-visual::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(17, 17, 17, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(17, 17, 17, 0.04) 1px, transparent 1px);
  background-size: 42px 42px;
  opacity: 0.34;
  pointer-events: none;
}

.login-visual::after {
  content: '';
  position: absolute;
  right: clamp(24px, 7vw, 84px);
  bottom: clamp(96px, 14vh, 180px);
  width: clamp(340px, 32vw, 520px);
  height: clamp(340px, 32vw, 520px);
  border-radius: var(--mono-radius-pill);
  background: radial-gradient(circle, rgba(255, 255, 255, 0.74) 0%, rgba(255, 255, 255, 0.2) 48%, rgba(255, 255, 255, 0) 72%);
  filter: blur(12px);
  pointer-events: none;
}

.visual-copy,
.mascot-stage,
.visual-watermark {
  position: relative;
  z-index: 1;
}

.visual-copy {
  align-self: flex-start;
  width: min(368px, 100%);
  margin-top: clamp(42px, 6.2vh, 72px);
  margin-top: clamp(42px, 6.2dvh, 72px);
}

.visual-copy h2 {
  margin: 14px 0 12px;
  color: var(--mono-text);
  font-family: var(--app-art-font);
  font-size: clamp(28px, 2.8vw, 42px);
  line-height: 1.08;
  font-weight: 600;
  letter-spacing: -0.05em;
}

.visual-orbit {
  position: absolute;
  border: 1px solid rgba(17, 17, 17, 0.08);
  border-radius: var(--mono-radius-pill);
  pointer-events: none;
}

.visual-orbit-a {
  right: 7%;
  top: 14%;
  width: clamp(260px, 28vw, 420px);
  height: clamp(90px, 10vw, 130px);
  transform: rotate(-12deg);
}

.visual-orbit-b {
  right: 12%;
  bottom: 21%;
  width: clamp(220px, 22vw, 360px);
  height: clamp(74px, 7vw, 110px);
  transform: rotate(18deg);
}

.mascot-stage {
  width: 100%;
  height: min(860px, calc(100% - 20px));
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  padding-right: clamp(0px, 2vw, 24px);
  transform: translate(22px, 34px);
}

.mascot-stage :deep(.polar-bear-mascot) {
  width: min(760px, 100%);
  height: min(760px, 100%);
}

.visual-watermark {
  position: absolute;
  left: clamp(26px, 5vw, 56px);
  bottom: clamp(22px, 4vh, 44px);
  color: rgba(17, 17, 17, 0.05);
  font-size: clamp(86px, 15vw, 180px);
  font-weight: 700;
  letter-spacing: -0.08em;
  line-height: 0.8;
  user-select: none;
}

:global(html.dark) .login-wrapper {
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.06), transparent 24%),
    radial-gradient(circle at bottom right, rgba(255, 255, 255, 0.08), transparent 30%),
    linear-gradient(135deg, #101011 0%, #141415 46%, #0d0d0e 100%);
}

:global(html.dark) .login-wrapper::before,
:global(html.dark) .login-visual::before {
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
}

:global(html.dark) .login-wrapper::after {
  background: rgba(255, 255, 255, 0.08);
}

:global(html.dark) .login-shell {
  background: transparent;
}

:global(html.dark) .login-form-pane {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.03) 0%, rgba(255, 255, 255, 0.01) 100%);
  border-right-color: rgba(255, 255, 255, 0.08);
}

:global(html.dark) .avatar-shell {
  background: rgba(255, 255, 255, 0.05);
}

:global(html.dark) .qr-wrapper {
  background: transparent;
}

:global(html.dark) .login-panel {
  background: transparent;
  box-shadow: none;
}

:global(html.dark) .login-panel::before {
  background: transparent;
}

:global(html.dark) :deep(.ant-input):not(.ant-input-affix-wrapper > input),
:global(html.dark) :deep(.ant-input-affix-wrapper) {
  background: rgba(255, 255, 255, 0.05) !important;
}

:global(html.dark) :deep(.ant-input:focus):not(.ant-input-affix-wrapper > input),
:global(html.dark) :deep(.ant-input:hover):not(.ant-input-affix-wrapper > input),
:global(html.dark) :deep(.ant-input-affix-wrapper-focused),
:global(html.dark) :deep(.ant-input-affix-wrapper:hover) {
  background: rgba(255, 255, 255, 0.07) !important;
}

:global(html.dark) :deep(.custom-tabs .ant-tabs-nav-list) {
  background: rgba(255, 255, 255, 0.04);
}

:global(html.dark) :deep(.custom-tabs .ant-tabs-tab-active) {
  background: rgba(255, 255, 255, 0.94);
}

:global(html.dark) :deep(.custom-tabs .ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: #111111 !important;
}

@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slide-up {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1120px) {
  .login-wrapper {
    padding: 0;
  }

  .login-wrapper::before {
    inset: 0;
  }

  .login-shell {
    flex-direction: column-reverse;
    min-height: 100vh;
  }

  .login-form-pane {
    flex: none;
    min-width: 0;
    padding: 28px 22px 34px;
    gap: 28px;
    border-right: none;
    border-top: 1px solid rgba(17, 17, 17, 0.06);
  }

  .login-visual {
    min-height: 52vh;
    padding: 28px 22px 18px;
  }

  .mascot-stage {
    min-height: 420px;
    padding-right: 0;
    transform: translate(0, 12px);
  }

  .visual-copy {
    width: min(520px, 100%);
  }

  .visual-watermark {
    left: 20px;
    bottom: 12px;
  }

  .login-form-pane {
    border-top-color: rgba(17, 17, 17, 0.06);
  }

  :global(html.dark) .login-form-pane {
    border-top-color: rgba(255, 255, 255, 0.08);
  }
}

@media (max-width: 640px) {
  .login-wrapper {
    padding: 0;
  }

  .login-wrapper::before,
  .login-wrapper::after {
    display: none;
  }

  .login-shell {
    min-height: 100vh;
    border-radius: 0;
  }

  .login-form-pane {
    padding: 22px 16px 28px;
  }

  .brand-row,
  .profile-chip {
    flex-wrap: wrap;
  }

  .login-panel {
    width: 100%;
    padding: 24px 18px;
    border-radius: var(--mono-radius-xl);
  }

  .panel-heading h1 {
    font-size: 38px;
  }

  .visual-copy h2 {
    font-size: 30px;
  }

  :deep(.ant-input):not(.ant-input-affix-wrapper > input),
  :deep(.ant-input-affix-wrapper),
  .form-remember,
  .custom-tabs {
    width: 100% !important;
    max-width: none;
  }

  .form-remember {
    flex-wrap: wrap;
    gap: 10px;
  }

  .login-visual {
    min-height: 46vh;
    padding: 22px 16px 12px;
  }

  .mascot-stage {
    min-height: 320px;
    transform: translate(6px, 12px);
  }

  .mascot-stage :deep(.polar-bear-mascot) {
    width: min(640px, 100%);
    height: min(640px, 100%);
  }
}
</style>
