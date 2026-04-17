<template>
  <a-drawer
    :open="visible"
    title="系统风格设置"
    placement="right"
    :width="320"
    :closable="true"
    @close="emit('update:visible', false)"
    class="theme-settings-drawer"
    :wrapClassName="styleMode === 'glass' ? 'glass-drawer-wrapper' : 'traditional-drawer-wrapper'"
    :rootClassName="styleMode === 'glass' ? 'glass-drawer-wrapper' : 'traditional-drawer-wrapper'"
    :zIndex="1999"
    :rootStyle="{ top: '56px', height: 'calc(100vh - 56px)' }"
    :mask="false"
  >
    <!-- 风格模式 -->
    <div class="settings-section first-section">
      <div class="section-title">
        <appstore-outlined /> 界面风格
      </div>
      <div class="style-mode-cards">
        <div 
          class="style-card" 
          :class="{ active: styleMode === 'glass' }"
          @click="setStyleMode('glass')"
        >
          <div class="style-preview glass-preview">
            <div class="glass-window">
              <div class="win-header">
                <div class="win-dot" style="background:#ff5f56"></div>
                <div class="win-dot" style="background:#ffbd2e"></div>
                <div class="win-dot" style="background:#27c93f"></div>
              </div>
            </div>
          </div>
          <span class="style-label">透明极简白</span>
        </div>
        <div 
          class="style-card"
          :class="{ active: styleMode === 'traditional' }"
          @click="setStyleMode('traditional')"
        >
          <div class="style-preview traditional-preview">
            <div class="preview-sidebar">
              <div class="sidebar-line"></div>
              <div class="sidebar-line" style="width:40%"></div>
              <div class="sidebar-line" style="width:50%"></div>
            </div>
            <div class="preview-right">
              <div class="preview-header"></div>
              <div class="preview-content"></div>
            </div>
          </div>
          <span class="style-label">经典风格</span>
        </div>
      </div>
    </div>

    <!-- 主题色 -->
    <div class="settings-section">
      <div class="section-title">
        <bg-colors-outlined /> 主题色
      </div>
      <div class="color-presets">
        <div
          v-for="color in presetColors"
          :key="color.value"
          class="color-swatch"
          :style="{ backgroundColor: color.value }"
          :class="{ active: themeColor === color.value }"
          @click="setThemeColor(color.value)"
          :title="color.name"
        >
          <check-outlined v-if="themeColor === color.value" class="check-icon" />
        </div>
        <!-- 自定义颜色按键 -->
        <div class="color-swatch custom-color-btn" @click="showCustomColorPicker = !showCustomColorPicker" title="自定义颜色">
          <plus-outlined v-if="!showCustomColorPicker" />
          <close-outlined v-else />
        </div>
      </div>
      <!-- 自定义调色盘 -->
      <transition name="fade">
        <div v-if="showCustomColorPicker" class="custom-color-picker">
          <input
            type="color"
            :value="themeColor"
            @input="handleCustomColor"
            class="color-picker-input"
          />
          <span class="color-hex-label">{{ themeColor }}</span>
        </div>
      </transition>
    </div>

    <!-- 主题模式 -->
    <div class="settings-section">
      <div class="section-title">
        <bulb-outlined /> 主题模式
      </div>
      <div class="dark-mode-grid">
        <!-- Manual Switch Card (Day/Night) -->
        <div 
          class="scene-card" 
          :class="{ 'active': darkMode !== 'auto', 'is-dark': darkMode === 'dark' }"
          @click="toggleDayNight"
        >
          <div class="sky-scene">
            <!-- Stars -->
            <div class="stars">
               <div class="star s1"></div>
               <div class="star s2"></div>
               <div class="star s3"></div>
               <div class="star s4"></div>
            </div>
            
            <!-- Sun -->
            <div class="celestial-body sun">
               <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="12" cy="12" r="5" fill="#FFCC33" stroke="#F5A623" stroke-width="1.5"/>
                  <g class="sun-rays">
                    <line x1="12" y1="1" x2="12" y2="4" stroke="#F5A623" stroke-width="2" stroke-linecap="round"/>
                    <line x1="12" y1="20" x2="12" y2="23" stroke="#F5A623" stroke-width="2" stroke-linecap="round"/>
                    <line x1="4.22" y1="4.22" x2="6.34" y2="6.34" stroke="#F5A623" stroke-width="2" stroke-linecap="round"/>
                    <line x1="17.66" y1="17.66" x2="19.78" y2="19.78" stroke="#F5A623" stroke-width="2" stroke-linecap="round"/>
                    <line x1="1" y1="12" x2="4" y2="12" stroke="#F5A623" stroke-width="2" stroke-linecap="round"/>
                    <line x1="20" y1="12" x2="23" y2="12" stroke="#F5A623" stroke-width="2" stroke-linecap="round"/>
                    <line x1="4.22" y1="19.78" x2="6.34" y2="17.66" stroke="#F5A623" stroke-width="2" stroke-linecap="round"/>
                    <line x1="17.66" y1="6.34" x2="19.78" y2="4.22" stroke="#F5A623" stroke-width="2" stroke-linecap="round"/>
                  </g>
               </svg>
            </div>

            <!-- Moon -->
            <div class="celestial-body moon">
               <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" fill="#F5A623" stroke="#E8960C" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M16 8l1 1" stroke="#E8960C" stroke-width="1" stroke-linecap="round"/>
                  <path d="M14 14l1 1" stroke="#E8960C" stroke-width="1" stroke-linecap="round"/>
               </svg>
            </div>
          </div>
          <div class="card-label" style="text-align: center;">
            <span>{{ darkMode === 'dark' ? '黑夜' : '白天' }}</span>
          </div>
        </div>

        <!-- Follow System Card -->
        <div
          class="system-card"
          :class="{ active: darkMode === 'auto' }"
          @click="setDarkMode('auto')"
        >
          <div class="system-icon-wrap">
             <desktop-outlined style="font-size: 24px;" />
          </div>
          <div class="card-label">跟随系统</div>
        </div>
      </div>
    </div>

    <!-- 圆角 -->
    <div class="settings-section">
      <div class="section-title-row">
        <span class="section-title" style="margin-bottom: 0;">
          <border-outer-outlined /> 圆角
        </span>
        <a-switch
          :checked="borderRadius"
          @change="setBorderRadius"
          size="small"
        />
      </div>
      <div class="section-desc">开启后界面元素使用圆角展示</div>
    </div>

    <div class="settings-section">
      <div class="section-title-row">
        <span class="section-title" style="margin-bottom: 0;">
          <tags-outlined /> 彩签
        </span>
        <a-switch
          :checked="colorfulTags"
          @change="setColorfulTags"
          size="small"
        />
      </div>
      <div class="section-desc">开启后系统标签保留彩色语义，关闭后统一使用适配黑白风格的标签颜色</div>
    </div>

    <div class="settings-section">
      <div class="section-title-row">
        <span class="section-title" style="margin-bottom: 0;">
          <highlight-outlined /> 标题艺术字
        </span>
        <a-switch
          :checked="artisticTitles"
          @change="setArtisticTitles"
          size="small"
        />
      </div>
      <div class="section-desc">开启后页面标题与工作台里的数字展示一起使用艺术字，关闭后恢复常规字体</div>
    </div>

    <div class="settings-section">
      <div class="section-title-row">
        <span class="section-title" style="margin-bottom: 0;">
          <font-colors-outlined /> 全局艺术字
        </span>
        <a-switch
          :checked="globalArtFont"
          @change="setGlobalArtFont"
          size="small"
        />
      </div>
      <div class="section-desc">开启后整个系统页面统一切换为艺术字字体，登录页不受影响</div>
    </div>

    <div class="settings-section last-section">
      <div class="section-title-row">
        <span class="section-title" style="margin-bottom: 0;">
          <appstore-outlined /> 记账本风格
        </span>
        <a-switch
          :checked="ledgerShell"
          @change="setLedgerShell"
          size="small"
        />
      </div>
      <div class="section-desc">开启后恢复内页圆角卡片页壳，关闭则使用当前平铺样式</div>
    </div>

    <!-- Style mode note -->
    <div v-if="styleMode === 'traditional'" class="style-note">
      <info-circle-outlined style="margin-right: 6px;" />
      经典风格会启用更实的层次与暖白页面氛围，菜单配置中的表格样式选项将禁用。
    </div>

  </a-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useStore } from 'vuex';
import { 
  CheckOutlined, 
  BgColorsOutlined, 
  InfoCircleOutlined,
  AppstoreOutlined,
  BulbOutlined,
  DesktopOutlined,
  BorderOuterOutlined,
  TagsOutlined,
  HighlightOutlined,
  FontColorsOutlined,
  PlusOutlined,
  CloseOutlined
} from '@ant-design/icons-vue';
import type { StyleMode, DarkMode } from '@/store/themeSettings';

defineProps({
  visible: {
    type: Boolean,
    default: false,
  }
});

const emit = defineEmits(['update:visible']);
const store = useStore();

const showCustomColorPicker = ref(false);

// Getters
const styleMode = computed(() => store.state.themeSettings.styleMode);
const themeColor = computed(() => store.state.themeSettings.themeColor);
const artisticTitles = computed(() => store.state.themeSettings.artisticTitles !== false);
const globalArtFont = computed(() => store.state.themeSettings.globalArtFont === true);
const borderRadius = computed(() => store.state.themeSettings.borderRadius);
const colorfulTags = computed(() => store.state.themeSettings.colorfulTags);
const ledgerShell = computed(() => store.state.themeSettings.ledgerShell);
const darkMode = computed(() => store.state.themeSettings.darkMode);

// Setters
const setStyleMode = (mode: StyleMode) => {
  store.commit('themeSettings/SET_STYLE_MODE', mode);
};

const setThemeColor = (color: string) => {
  store.commit('themeSettings/SET_THEME_COLOR', color);
};

const setArtisticTitles = (enabled: boolean) => {
  store.commit('themeSettings/SET_ARTISTIC_TITLES', enabled);
};

const setGlobalArtFont = (enabled: boolean) => {
  store.commit('themeSettings/SET_GLOBAL_ART_FONT', enabled);
};

const setBorderRadius = (enabled: boolean) => {
  store.commit('themeSettings/SET_BORDER_RADIUS', enabled);
};

const setColorfulTags = (enabled: boolean) => {
  store.commit('themeSettings/SET_COLORFUL_TAGS', enabled);
};

const setLedgerShell = (enabled: boolean) => {
  store.commit('themeSettings/SET_LEDGER_SHELL', enabled);
};

const setDarkMode = (mode: DarkMode) => {
  store.commit('themeSettings/SET_DARK_MODE', mode);
};

const toggleDayNight = () => {
    if (darkMode.value === 'dark') {
      setDarkMode('light');
    } else {
      setDarkMode('dark');
    }
};

const handleCustomColor = (e: Event) => {
  const input = e.target as HTMLInputElement;
  setThemeColor(input.value);
};

// Preset colors
const presetColors = [
  { name: '墨黑', value: '#111111' },
  { name: '拂晓蓝', value: '#1890ff' },
  { name: '薄暮红', value: '#f5222d' },
  { name: '火山橙', value: '#fa541c' },
  { name: '日暮黄', value: '#f7b500' },
  { name: '极光绿', value: '#52c41a' },
  { name: '明青', value: '#13c2c2' },
  { name: '极客蓝', value: '#2f54eb' },
  { name: '酱紫', value: '#722ed1' },
];
</script>

<style scoped>
/* Remove Shadow from Drawer Root Wrapper - Nuclear Option */
/* Remove Shadow from Drawer Root Wrapper - Nuclear Option */
:global(.theme-settings-drawer .ant-drawer-content-wrapper),
:global(.theme-settings-drawer .ant-drawer-content) {
    box-shadow: none !important;
    filter: none !important;
}

/* Glass Mode Drawer Shadow / 透明玻璃模式下的抽屉阴影 */
:global(.glass-drawer-wrapper .ant-drawer-content-wrapper),
:global(.glass-drawer-wrapper .ant-drawer-content),
:global(.glass-drawer-wrapper .ant-drawer-wrapper-body) {
    /* <--- [Adjust] Glass Mode Shadow / 此处调整玻璃模式阴影 */
    box-shadow: -2px 0 8px rgba(0, 0, 0, 0.05) !important;
    filter: none !important;
}

/* Traditional Mode Drawer Shadow / 经典模式下的抽屉阴影 */
:global(.traditional-drawer-wrapper .ant-drawer-content-wrapper),
:global(.traditional-drawer-wrapper .ant-drawer-content),
:global(.traditional-drawer-wrapper .ant-drawer-wrapper-body) {
    /* 
       [Adjust] Traditional Mode Shadow
       Format: x-offset y-offset blur-radius color
       -1px: Shadow moves left by 1px (向左偏移1px)
       0:    No vertical offset (垂直不偏移)
       8px:  Blur radius (模糊半径/扩散程度)
       rgba(0,0,0,0.05): Black color with 5% opacity (黑色，5%透明度)
    */
    box-shadow: -1.5px 0 5px rgba(0, 0, 0, 0.04) !important;
    filter: none !important;
}

/* Use padding/margin for spacing instead of Dividers */
.settings-section {
  margin-bottom: 32px; /* Increased internal spacing */
  padding-bottom: 24px;
}
/* ... */

/* Add active state for manual mode */
.scene-card.active {
  border-color: #d9d9d9;
  /* color: inherit; Kept default */
  box-shadow: none;
}

/* Dark Mode: Blue Overlay Effect (Whole frame covered) / 暗黑模式: 蓝色覆盖效果 */
:global(.dark) .scene-card.active {
  background-color: rgba(23, 125, 220, 0.2); /* Blue overlay / 蓝色蒙层 */
  border-color: rgba(23, 125, 220, 0.3);      /* Subtle blue border / 淡淡的蓝边框 */
  color: #177ddc;                             /* Blue text / 蓝色文字 */
  box-shadow: none;
}

.first-section {
  padding-top: 12px;
}

.last-section {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

/* Dark mode border adjustment - removed bottom border for cleaner look, just spacing */
.settings-section {
    border-bottom: none; 
}
:global(.dark) .settings-section {
    border-bottom: none;
}

.section-title {
  font-size: 15px; /* Slightly larger */
  font-weight: 600;
  color: #333;
  margin-bottom: 20px; /* More space below title */
  display: flex;
  align-items: center;
  gap: 8px;
}

:global(.dark) .section-title {
  color: rgba(255,255,255,0.9);
}

.section-title .anticon {
  font-size: 16px;
  opacity: 0.8;
  position: relative;
  top: 1px;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.section-desc {
  font-size: 12px;
  color: #999;
  margin-top: 6px;
  /* margin-left: 24px; Removed strictly aligning with text, better flow */
}

:global(.dark) .section-desc {
  color: rgba(255,255,255,0.45);
}

/* Style Mode Cards */
.style-mode-cards {
  display: flex;
  gap: 20px; /* Increased gap */
}

.style-card {
  flex: 1;
  cursor: pointer;
  text-align: center;
  border: 1.5px solid transparent;
  border-radius: var(--mono-radius-md);
  padding: 16px 12px;
  background: #fff;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

:global(.dark) .style-card {
  background: rgba(255,255,255,0.05);
  border-color: rgba(255,255,255,0.1);
}

.style-card:hover {
  background: #f5f5f5;
}

:global(.dark) .style-card:hover {
  background: rgba(255,255,255,0.1);
}

/* Active State - Light gray background, no glass border */
.style-card.active {
  background: #f0f0f0;
  border-color: #d0d0d0;
  box-shadow: none;
}

:global(.dark) .style-card.active {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.2);
  box-shadow: none;
}

.style-preview {
  height: 70px; /* Taller preview */
  border-radius: var(--mono-radius-sm);
  display: flex;
  overflow: hidden;
  margin-bottom: 16px;
  position: relative;
  /* Shadow removed or made very subtle */
  box-shadow: 0 4px 12px rgba(0,0,0,0.05); 
}

:global(.dark) .style-preview {
  box-shadow: none;
}

/* --- Glass Preview - Gray Transparent --- */
.glass-preview {
  background: #ffffff;
  position: relative;
  overflow: hidden;
}
:global(.dark) .glass-preview {
  background: #262626;
}

/* Shine Animation */
/* Shine Animation Element - Moved to .glass-window to clip to the glass shape */
/* Active state added: Plays animation when selected / 选中状态也会播放动画 */
.style-card:hover .glass-window::after,
.style-card.active .glass-window::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 21%; /* <--- [Adjust] Width of the shine / 光效宽度 */
  height: 100%;
  /* <--- [Adjust] Opacity of the shine (0.3) / 光效透明度 */
  background: linear-gradient(to right, rgba(255,255,255,0) 0%, rgba(255,255,255,0.3) 50%, rgba(255,255,255,0) 100%);
  transform: skewX(-25deg); /* <--- [Adjust] Angle / 倾斜角度 */
  animation: shine 1.75s; /* <--- [Adjust] Speed: Larger number = Slower / 速度: 数值越大越慢 */
}

/* Only loop when active/selected / 仅在选中时无限循环 */
.style-card.active .glass-window::after {
  animation-iteration-count: infinite;
}

@keyframes shine {
  100% {
    left: 160%; /* <--- [Adjust] End position: Increase this to scan longer distance / 结束位置: 调大此数值能扫过更远距离(完整卡片) */
  }
}

.glass-preview .glass-window {
  width: 100%;
  height: 100%;
  background: #ffffff;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  border-radius: var(--mono-radius-xs);
  border: 1px solid rgba(17, 17, 17, 0.08);
  box-shadow: 0 10px 22px rgba(15,23,42,0.04);
  display: flex;
  flex-direction: column;
  position: relative; /* Ensure it can contain absolute children */
  overflow: hidden; /* <--- Clip content (shine) to border radius / 裁剪超出圆角的内容(光效) */
}

:global(.dark) .glass-preview .glass-window {
  background: rgba(0, 0, 0, 0.3); /* Dark glass */
  border: 1px solid rgba(255, 255, 255, 0.1); /* <--- [Adjust] Dark Mode Border / 暗黑模式边框 */
  box-shadow: none;
}

.glass-preview .win-header {
  height: 12px;
  border-bottom: 1px solid rgba(17,17,17,0.06);
  display: flex;
  align-items: center;
  padding-left: 6px;
  gap: 3px;
}
:global(.dark) .glass-preview .win-header {
  border-bottom: 1px solid rgba(255,255,255,0.05);
}

.glass-preview .win-dot {
  width: 5px;
  height: 5px;
  border-radius: var(--mono-radius-pill);
  background: rgba(255,255,255,0.8);
}

/* --- Traditional Preview Tweaks --- */
.traditional-preview {
  background: linear-gradient(180deg, #f7f7f5 0%, #f2f2ef 100%);
  border: 1px solid #eceae4;
  flex-direction: row;
}
:global(.dark) .traditional-preview {
  background: #141414;
  border: 1px solid #303030;
}

/* ... (keep internal structure styles mostly same, just tweaks) ... */
.preview-sidebar {
  width: 25%;
  background: linear-gradient(180deg, rgba(255,255,255,0.75) 0%, rgba(247,247,245,0.92) 100%);
  border-right: 1px solid rgba(17,17,17,0.06);
  height: 100%;
  display: flex;
  flex-direction: column;
  padding-top: 6px;
  gap: 4px;
  align-items: center;
}

.sidebar-line {
  width: 60%;
  height: 3px;
  background: rgba(17,17,17,0.16);
  border-radius: var(--mono-radius-pill);
}

.preview-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.preview-header {
  height: 14px;
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(17,17,17,0.05);
}
:global(.dark) .preview-header {
  background: #1f1f1f;
  border-bottom: 1px solid #303030;
}
.preview-content {
  flex: 1;
  background: #fff;
  margin: 6px;
  border-radius: var(--mono-radius-sm) 0 0 var(--mono-radius-sm);
  border: 1px solid rgba(17,17,17,0.05);
  box-shadow: 0 8px 18px rgba(15,23,42,0.04);
}
:global(.dark) .preview-content {
  background: #141414;
  border: 1px solid #303030;
}

.style-label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

:global(.dark) .style-label {
  color: rgba(255,255,255,0.65);
}

.style-card.active .style-label {
  color: v-bind(themeColor);
  font-weight: 600;
}

/* Color Presets */
.color-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 16px; /* Increase gap */
  margin-top: 12px;
}

.color-swatch {
  width: 24px;
  height: 24px;
  border-radius: var(--mono-radius-pill);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s;
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);
  border: 2px solid transparent; 
  position: relative;
}

.color-swatch:after {
  content: '';
  position: absolute;
  top: -4px;
  left: -4px;
  right: -4px;
  bottom: -4px;
  border-radius: var(--mono-radius-pill);
  border: 2px solid transparent;
  transition: border-color 0.2s;
}

.color-swatch.active:after {
  border-color: rgba(0, 0, 0, 0.25);
}

:global(.dark) .color-swatch.active:after {
  border-color: rgba(255, 255, 255, 0.35);
}

.color-swatch:hover {
  transform: scale(1.1);
}

.check-icon {
  color: #fff;
  font-size: 12px;
  font-weight: bold;
  text-shadow: 0 1px 2px rgba(0,0,0,0.3);
}

.custom-color-btn {
  background: conic-gradient(from 90deg at 50% 50%, #f00, #ff0, #0f0, #0ff, #00f, #f0f, #f00);
  color: #fff;
}

/* Custom Color Picker */
.custom-color-picker {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
  padding: 10px 14px;
  background: rgba(0,0,0,0.03);
  border-radius: var(--mono-radius-sm);
  border: 1px solid rgba(0,0,0,0.05);
}

:global(.dark) .custom-color-picker {
  background: rgba(255,255,255,0.05);
  border-color: rgba(255,255,255,0.1);
}

.color-picker-input {
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  border-radius: var(--mono-radius-pill);
  cursor: pointer;
  background: transparent;
  overflow: hidden;
}

.color-hex-label {
  font-size: 13px;
  font-family: monospace;
  font-weight: 500;
  color: #666;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

:global(.dark) .color-hex-label {
  color: rgba(255,255,255,0.85);
}

/* Dark Mode Grid Layout */
.dark-mode-grid {
  display: flex;
  gap: 16px;
  margin-top: 10px;
}

/* Ensure animations don't overflow */
.style-card {
  overflow: hidden !important;
}

/* Scene Card (Day/Night) */
.scene-card {
  flex: 1;
  border-radius: var(--mono-radius-sm);
  border: 1.5px solid transparent; /* Transparent by default */
  cursor: pointer;
  background: #fff;
  padding: 8px;
  display: flex;
  flex-direction: column;
  align-items: center; /* Center content horizontally */
  justify-content: center;
  overflow: hidden;
  transition: all 0.3s;
}



/*.scene-card dark mode moved to global block below*/
.scene-card:hover {
  border-color: #d9d9d9;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.sky-scene {
  width: 100%;
  height: 60px;
  background: linear-gradient(to bottom, #87CEEB 0%, #E0F7FA 100%); /* Day Sky */
  border-radius: var(--mono-radius-sm);
  position: relative;
  overflow: hidden;
  transition: background 0.8s ease;
  margin-bottom: 8px;
}

/* Night State Sky */
.scene-card.is-dark .sky-scene {
  background: linear-gradient(to bottom, #0f2027 0%, #203a43 100%); /* Night Sky */
}

/* Celestial Bodies */
.celestial-body {
  width: 28px;
  height: 28px;
  position: absolute;
  left: 50%;
  margin-left: -14px; /* Center horizontally */
  transition: all 0.8s cubic-bezier(0.68, -0.55, 0.27, 1.55); /* Bouncy move */
}

.sun {
  top: 16px;
  transform: translateY(0);
}
.moon {
  top: 16px;
  transform: translateY(60px); /* Initially hidden below */
  opacity: 0;
}

.sun-rays {
  animation: sun-spin 10s linear infinite;
  transform-origin: 12px 12px;
}

@keyframes sun-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Transitions for Night Mode */
.scene-card.is-dark .sun {
  transform: translateY(60px); /* Sun sets */
  opacity: 0;
}
.scene-card.is-dark .moon {
  transform: translateY(0); /* Moon rises */
  opacity: 1;
}

/* Stars */
.star {
  position: absolute;
  width: 2px;
  height: 2px;
  background: white;
  border-radius: var(--mono-radius-pill);
  opacity: 0;
  transition: opacity 0.5s;
}
.star.s1 { top: 10px; left: 20%; }
.star.s2 { top: 20px; left: 80%; }
.star.s3 { top: 40px; left: 15%; }
.star.s4 { top: 15px; left: 60%; }

.scene-card.is-dark .star {
  opacity: 0.8;
  animation: twinkle 1.5s infinite alternate;
}

@keyframes twinkle {
  0% { opacity: 0.3; transform: scale(0.8); }
  100% { opacity: 1; transform: scale(1.2); }
}

/* System Card */
.system-card {
  flex: 1;
  border-radius: var(--mono-radius-sm);
  border: 1.5px solid transparent; /* Transparent by default */
  cursor: pointer;
  background: #fff;
  padding: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

/*.system-card dark mode moved to global block below*/
/* System Card Active State */
.system-card.active {
  border-color: #d9d9d9;
  background: #f0f0f0;
  box-shadow: none;
}
:global(.dark) .system-card.active {
  background-color: rgba(23, 125, 220, 0.2); /* Blue overlay */
  border-color: rgba(23, 125, 220, 0.3);
  color: #177ddc;
}

.system-icon-wrap {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
  color: #666;
}
:global(.dark) .system-icon-wrap { color: #fff; }

.card-label {
  font-size: 13px;
  color: #666;
}
:global(.dark) .card-label { color: rgba(255,255,255,0.85); }

/* END Styles */

/* Style Note */
.style-note {
  margin-top: 24px;
  padding: 12px 16px;
  background: rgba(255, 160, 0, 0.1);
  border-radius: var(--mono-radius-sm);
  font-size: 12px;
  color: #ad6800;
  line-height: 1.6;
  display: flex;
  align-items: flex-start;
}
:global(.dark) .style-note {
  background: rgba(255, 160, 0, 0.15);
  color: #ffc107;
}

/* Transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

<style>
/* Non-scoped styles to ensure Dark Mode overrides work correctly */
.dark .theme-settings-drawer .settings-section {
    border-bottom: none;
}

.dark .theme-settings-drawer .section-title {
  color: rgba(255,255,255,0.95);
}

.dark .theme-settings-drawer .section-desc {
  color: rgba(255,255,255,0.45);
}

/* Style Cards (Glass/Traditional) */
.dark .theme-settings-drawer .style-card {
  background: rgba(255,255,255,0.05);
  border: 1.5px solid rgba(255,255,255,0.1);
  box-shadow: none;
}

.dark .theme-settings-drawer .style-card:hover {
  background: rgba(255,255,255,0.1);
}

.dark .theme-settings-drawer .style-card.active {
  background: rgba(24, 144, 255, 0.1);
  border: 1.5px solid rgba(255, 255, 255, 0.25);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.1);
}

/* Fix "White Block" / "Black Frame" issue - transparency is key */

.dark .theme-settings-drawer .style-preview {
  box-shadow: none;
}

.dark .theme-settings-drawer .glass-preview {
  background: linear-gradient(135deg, #1f1f1f 0%, #2c2c2c 100%);
}

.dark .theme-settings-drawer .glass-preview .glass-window {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: none;
}

.dark .theme-settings-drawer .glass-preview .win-header {
  border-bottom: 1px solid rgba(255,255,255,0.05);
}

.dark .theme-settings-drawer .traditional-preview {
  background: #000;
  border: 1px solid #333;
}

.dark .theme-settings-drawer .preview-header {
  background: #1f1f1f;
  border-bottom: 1px solid #333;
}

.dark .theme-settings-drawer .preview-content {
  background: #141414;
  border: 1px solid #333;
}

.dark .theme-settings-drawer .style-label {
  color: rgba(255,255,255,0.85);
}

/* Custom Color Picker */
.dark .theme-settings-drawer .custom-color-picker {
  background: rgba(255,255,255,0.05);
  border-color: rgba(255,255,255,0.1);
}

.dark .theme-settings-drawer .color-hex-label {
  color: rgba(255,255,255,0.85);
}

/* Dark Mode Toggles */
.dark .theme-settings-drawer .dark-mode-options {
  background: transparent;
  gap: 12px;
}

.dark .theme-settings-drawer .dark-mode-card {
  color: rgba(255,255,255,0.5);
  background: rgba(255,255,255,0.05);
  flex-direction: column;
  gap: 6px;
  padding: 10px 8px;
  height: auto;
  border: 1.5px solid rgba(255,255,255,0.1);
}

.dark .theme-settings-drawer .dark-mode-card .mode-icon {
  font-size: 18px;
  margin-bottom: 2px;
  color: rgba(255,255,255,0.5);
}

.dark .theme-settings-drawer .dark-mode-card .mode-icon-svg {
  color: rgba(255,255,255,0.5);
}

.dark .theme-settings-drawer .dark-mode-card:hover {
  background: rgba(255,255,255,0.1);
  color: #fff;
}
.dark .theme-settings-drawer .dark-mode-card:hover .mode-icon,
.dark .theme-settings-drawer .dark-mode-card:hover .mode-icon-svg {
  color: #fff;
}

.dark .theme-settings-drawer .dark-mode-card.active {
  background: rgba(24, 144, 255, 0.1); 
  color: #1890ff; 
  border: 1.5px solid rgba(255, 255, 255, 0.25);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.1);
}

.dark .theme-settings-drawer .dark-mode-card.active .mode-icon,
.dark .theme-settings-drawer .dark-mode-card.active .mode-icon-svg {
  color: #1890ff;
}

/* Style Note */
.dark .theme-settings-drawer .style-note {
  background: rgba(255, 160, 0, 0.15);
  color: #ffc107;
}

/* Drawer Shadow Adjustment */
.dark .ant-drawer-content-wrapper {
    box-shadow: -4px 0 24px rgba(0, 0, 0, 0.4) !important; /* Lighter/Softer shadow */
}
</style>

<style>
/* Global Dark Mode Overrides for Drawer Cards to ensure they apply */
html.dark .scene-card,
html.dark .system-card {
  background: rgba(255,255,255,0.05) !important;
  border-color: rgba(255,255,255,0.1) !important;
}
html.dark .scene-card:hover,
html.dark .system-card:hover {
  border-color: rgba(255,255,255,0.2) !important;
}
/* Ensure text color is correct too */
html.dark .card-label {
    color: rgba(255,255,255,0.85) !important;
}
html.dark .system-icon-wrap {
    color: #fff !important;
}
</style>
