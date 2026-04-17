<template>
  <a-config-provider :locale="locale" :theme="antdTheme">
    <router-view />
  </a-config-provider>
</template>

<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import { theme as antTheme } from 'ant-design-vue';
import { computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useStore } from 'vuex';

const locale = zhCN;
const store = useStore();
const route = useRoute();
const BASE_FONT_FAMILY = '"Avenir Next", "SF Pro Text", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif';
const ART_FONT_FAMILY = '"STKaiti", "Kaiti SC", "KaiTi", "DFKai-SB", "STSong", "Songti SC", "Source Han Serif SC", "Noto Serif SC", "SimSun", serif';

const isDark = computed(() => store.state.themeSettings?.isDark || false);
const isTraditional = computed(() => store.state.themeSettings?.styleMode === 'traditional');
const isGlass = computed(() => store.state.themeSettings?.styleMode === 'glass');
const hasBorderRadius = computed(() => store.state.themeSettings?.borderRadius !== false);
const artisticTitles = computed(() => store.state.themeSettings?.artisticTitles !== false);
const globalArtFont = computed(() => store.state.themeSettings?.globalArtFont === true);
const colorfulTags = computed(() => store.state.themeSettings?.colorfulTags !== false);
const themeColor = computed(() => store.state.themeSettings?.themeColor || '#111111');
const isLoginRoute = computed(() => route.path === '/login');
const effectiveGlobalArtFont = computed(() => globalArtFont.value && !isLoginRoute.value);

const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max);

const hexToRgb = (hex: string) => {
  const normalized = hex.replace('#', '').trim();
  const expanded = normalized.length === 3
    ? normalized.split('').map(char => `${char}${char}`).join('')
    : normalized;

  if (expanded.length !== 6) {
    return { r: 17, g: 17, b: 17 };
  }

  const value = Number.parseInt(expanded, 16);
  return {
    r: (value >> 16) & 255,
    g: (value >> 8) & 255,
    b: value & 255,
  };
};

const rgbToHex = (r: number, g: number, b: number) => {
  const toHex = (channel: number) => clamp(Math.round(channel), 0, 255).toString(16).padStart(2, '0');
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
};

const mixHex = (hex: string, target: string, weight: number) => {
  const fromRgb = hexToRgb(hex);
  const toRgb = hexToRgb(target);
  const amount = clamp(weight, 0, 1);

  return rgbToHex(
    fromRgb.r + (toRgb.r - fromRgb.r) * amount,
    fromRgb.g + (toRgb.g - fromRgb.g) * amount,
    fromRgb.b + (toRgb.b - fromRgb.b) * amount,
  );
};

const relativeLuminance = (hex: string) => {
  const { r, g, b } = hexToRgb(hex);
  const normalize = (channel: number) => {
    const value = channel / 255;
    return value <= 0.03928 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
  };

  return 0.2126 * normalize(r) + 0.7152 * normalize(g) + 0.0722 * normalize(b);
};

const getReadableForeground = (hex: string) => (relativeLuminance(hex) > 0.58 ? '#111111' : '#ffffff');

const antdTheme = computed(() => {
  const dark = isDark.value;
  const radius = hasBorderRadius.value ? 18 : 4;
  const lightLayoutBg = isGlass.value ? '#ffffff' : '#f5f5f3';
  const lightContainerBg = isGlass.value ? '#ffffff' : '#ffffff';
  const lightElevatedBg = isGlass.value ? '#ffffff' : '#ffffff';
  const lightBorder = isGlass.value ? 'rgba(17, 17, 17, 0.08)' : 'rgba(17, 17, 17, 0.08)';
  const lightBorderSecondary = isGlass.value ? 'rgba(17, 17, 17, 0.05)' : 'rgba(17, 17, 17, 0.06)';
  const lightFillSecondary = isGlass.value ? '#f7f7f7' : '#f7f7f5';
  const lightFillTertiary = isGlass.value ? '#fafafa' : '#f7f7f5';
  const lightFillQuaternary = isGlass.value ? '#fcfcfc' : '#fbfbfa';
  const drawerBg = dark
    ? (isGlass.value ? 'rgba(24, 24, 24, 0.9)' : '#181818')
    : (isGlass.value ? 'rgba(255, 255, 255, 0.98)' : '#ffffff');

  return {
    algorithm: dark ? antTheme.darkAlgorithm : antTheme.defaultAlgorithm,
    token: {
      colorPrimary: themeColor.value,
      colorInfo: themeColor.value,
      colorLink: themeColor.value,
      colorText: dark ? 'rgba(255, 255, 255, 0.92)' : '#111111',
      colorTextSecondary: dark ? 'rgba(255, 255, 255, 0.68)' : (isGlass.value ? '#6b7280' : '#5f6368'),
      colorTextTertiary: dark ? 'rgba(255, 255, 255, 0.46)' : (isGlass.value ? '#9ca3af' : '#8a8f98'),
      colorBorder: dark ? 'rgba(255, 255, 255, 0.08)' : lightBorder,
      colorBorderSecondary: dark ? 'rgba(255, 255, 255, 0.06)' : lightBorderSecondary,
      colorBgBase: dark ? '#141414' : lightContainerBg,
      colorBgContainer: dark ? '#181818' : lightContainerBg,
      colorBgElevated: dark ? '#1c1c1c' : lightElevatedBg,
      colorBgLayout: dark ? '#141414' : lightLayoutBg,
      colorFillSecondary: dark ? 'rgba(255, 255, 255, 0.08)' : lightFillSecondary,
      colorFillTertiary: dark ? 'rgba(255, 255, 255, 0.06)' : lightFillTertiary,
      colorFillQuaternary: dark ? 'rgba(255, 255, 255, 0.04)' : lightFillQuaternary,
      fontFamily: effectiveGlobalArtFont.value ? ART_FONT_FAMILY : BASE_FONT_FAMILY,
      borderRadius: radius,
      boxShadowSecondary: dark
        ? '0 22px 54px rgba(0, 0, 0, 0.42)'
        : (isGlass.value ? '0 10px 30px rgba(15, 23, 42, 0.04)' : '0 18px 40px rgba(15, 23, 42, 0.08)'),
    },
    components: {
      Layout: {
        headerBg: 'transparent',
        siderBg: 'transparent',
        bodyBg: dark ? '#141414' : lightLayoutBg,
        triggerBg: 'transparent',
      },
      Menu: {
        itemBg: 'transparent',
        subMenuItemBg: 'transparent',
        darkItemBg: 'transparent',
        darkSubMenuItemBg: 'transparent',
        itemBorderRadius: hasBorderRadius.value ? 12 : 2,
        itemSelectedBg: isGlass.value ? 'rgba(17, 17, 17, 0.04)' : 'rgba(17, 17, 17, 0.06)',
        darkItemSelectedBg: 'rgba(255, 255, 255, 0.08)',
      },
      Drawer: {
        colorBgElevated: drawerBg,
      },
      Modal: {
        contentBg: drawerBg,
        headerBg: 'transparent',
      },
      Table: {
        headerBg: dark ? 'rgba(255, 255, 255, 0.02)' : (isGlass.value ? '#fafafa' : 'rgba(17, 17, 17, 0.02)'),
        headerColor: dark ? 'rgba(255, 255, 255, 0.68)' : (isGlass.value ? '#6b7280' : '#5f6368'),
        rowHoverBg: dark ? 'rgba(255, 255, 255, 0.03)' : (isGlass.value ? '#fafafa' : 'rgba(17, 17, 17, 0.02)'),
        borderColor: dark ? 'rgba(255, 255, 255, 0.08)' : lightBorder,
      },
    },
  };
});

watch(themeColor, color => {
  document.documentElement.style.setProperty('--theme-accent', color);
  document.documentElement.style.setProperty('--shad-primary-bg', color);
  document.documentElement.style.setProperty('--shad-primary-hover', mixHex(color, isDark.value ? '#ffffff' : '#000000', 0.12));
  document.documentElement.style.setProperty('--shad-primary-foreground', getReadableForeground(color));
  document.documentElement.style.setProperty('--shad-ring', mixHex(color, isDark.value ? '#ffffff' : '#111111', isDark.value ? 0.18 : 0.32));
}, { immediate: true });

watch(isDark, dark => {
  if (dark) {
    document.documentElement.classList.add('dark');
    document.documentElement.setAttribute('data-theme', 'dark');
  } else {
    document.documentElement.classList.remove('dark');
    document.documentElement.setAttribute('data-theme', 'light');
  }
}, { immediate: true });

watch(isLoginRoute, login => {
  document.documentElement.classList.toggle('route-login', login);
  document.documentElement.classList.toggle('route-app', !login);
}, { immediate: true });

watch(isTraditional, trad => {
  if (trad) {
    document.documentElement.classList.add('theme-traditional-global');
    document.documentElement.classList.remove('theme-glass-global');
  } else {
    document.documentElement.classList.remove('theme-traditional-global');
    document.documentElement.classList.add('theme-glass-global');
  }
}, { immediate: true });

watch(hasBorderRadius, has => {
  if (has) {
    document.documentElement.classList.remove('theme-no-radius-global');
  } else {
    document.documentElement.classList.add('theme-no-radius-global');
  }
}, { immediate: true });

watch(artisticTitles, enabled => {
  document.documentElement.classList.toggle('theme-art-titles-on', enabled);
  document.documentElement.classList.toggle('theme-art-titles-off', !enabled);
}, { immediate: true });

watch(effectiveGlobalArtFont, enabled => {
  document.documentElement.classList.toggle('theme-art-global-on', enabled);
  document.documentElement.classList.toggle('theme-art-global-off', !enabled);
}, { immediate: true });

watch(colorfulTags, enabled => {
  document.documentElement.classList.toggle('theme-color-tags-off', !enabled);
}, { immediate: true });

const ignoreErrors = [
  'ResizeObserver loop completed with undelivered notifications',
  'ResizeObserver loop limit exceeded',
];

window.addEventListener('error', e => {
  const errorMsg = e.message;

  ignoreErrors.forEach(message => {
    if (!errorMsg.startsWith(message)) {
      return;
    }

    const resizeObserverErrDiv = document.getElementById('webpack-dev-server-client-overlay-div');
    const resizeObserverErr = document.getElementById('webpack-dev-server-client-overlay');

    if (resizeObserverErr) {
      resizeObserverErr.setAttribute('style', 'display: none');
    }

    if (resizeObserverErrDiv) {
      resizeObserverErrDiv.setAttribute('style', 'display: none');
    }
  });
});
</script>

<style>
#app {
  min-height: 100vh;
  width: 100%;
  background-color: var(--mono-bg);
  color: var(--mono-text);
  transition: background-color 0.3s, color 0.3s;
}
</style>
