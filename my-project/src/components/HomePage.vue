<template>
  <a-layout :style="layoutStyle" :class="rootClasses">
    <a-layout-header class="header">
      <div class="header-left" :class="{ collapsed }">
        <div class="header-brand">
          <img :src="gmsLogoImg" class="header-brand-logo" :class="{ 'is-dark': isDark }" alt="GMS" />
          <span v-show="!collapsed" class="header-brand-title">Gym Management System</span>
        </div>
      </div>
      <div class="header-right">
        <div class="settings-trigger" @click="settingsVisible = true" style="margin-right: 4px;">
          <LayoutOutlined style="font-size: 18px;" />
        </div>
        <a-dropdown>
          <img :src="userAvatarImg" alt="用户头像" class="plain-avatar plain-avatar--header" />
          <template #overlay>
            <a-menu>
              <a-menu-item key="1">
                <UserOutlined /> 个人中心
              </a-menu-item>
              <a-menu-item key="2">
                <SettingOutlined /> 设置
              </a-menu-item>
              <a-menu-divider />
              <a-menu-item key="3" @click="handleLogout">
                <LogoutOutlined /> 退出登录
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </a-layout-header>

    <a-layout>
      <a-layout-sider
        v-model:collapsed="collapsed"
        collapsible
        :style="siderStyle"
        :width="sidebarWidth"
      >
        <a-menu
          v-model:selectedKeys="selectedKeys"
          v-model:openKeys="openKeys"
          mode="inline"
          :theme="isDark ? 'dark' : 'light'"
          style="background: transparent; border-right: none;"
        >
          <a-menu-item key="dashboard" style="margin-top: 16px;">
            <router-link to="/dashboard">
              <AppstoreOutlined />
              <span>工作台</span>
            </router-link>
          </a-menu-item>

          <template v-for="item in menuTree" :key="item.id">
            <template v-if="String(item.id) === '999' && item.children">
              <template v-for="child in item.children" :key="child.path">
                <a-sub-menu v-if="child.children && child.children.length > 0" :key="String(child.id)">
                  <template #icon>
                    <component :is="icons[child.icon]" v-if="child.icon && icons[child.icon]" />
                    <FolderOutlined v-else />
                  </template>
                  <template #title>{{ child.title }}</template>

                  <a-menu-item v-for="grandchild in child.children" :key="grandchild.path">
                    <router-link :to="grandchild.path">
                      <component :is="icons[grandchild.icon]" v-if="grandchild.icon && icons[grandchild.icon]" />
                      <span>{{ grandchild.title }}</span>
                    </router-link>
                  </a-menu-item>
                </a-sub-menu>

                <a-menu-item v-else :key="child.path">
                  <router-link :to="child.path">
                    <component :is="icons[child.icon]" v-if="child.icon && icons[child.icon]" />
                    <FileOutlined v-else />
                    <span>{{ child.title }}</span>
                  </router-link>
                </a-menu-item>
              </template>
            </template>

            <template v-else>
              <a-sub-menu v-if="item.children && item.children.length > 0" :key="String(item.id)">
                <template #icon>
                  <component :is="icons[item.icon]" v-if="item.icon && icons[item.icon]" />
                  <FolderOutlined v-else />
                </template>
                <template #title>{{ item.title }}</template>

                <a-menu-item v-for="child in item.children" :key="child.path">
                  <router-link :to="child.path">
                    <component :is="icons[child.icon]" v-if="child.icon && icons[child.icon]" />
                    <span>{{ child.title }}</span>
                  </router-link>
                </a-menu-item>
              </a-sub-menu>

              <a-menu-item v-else-if="item.path !== '/dashboard'" :key="item.path">
                <router-link :to="item.path">
                  <component :is="icons[item.icon]" v-if="item.icon && icons[item.icon]" />
                  <FileOutlined v-else />
                  <span>{{ item.title }}</span>
                </router-link>
              </a-menu-item>
            </template>
          </template>
        </a-menu>
        <span style="margin-top: 20px;"></span>
      </a-layout-sider>

      <a-layout :style="{ marginLeft: `${sidebarWidth}px`, marginTop: '0px' }">
        <a-layout-content
          class="content"
          :class="{
            'transparent-glass-mode': route.meta.style === 'glass',
            'default-yellow-mode': route.meta.style !== 'glass',
            'is-dashboard': route.path === '/dashboard' || route.name === 'DashboardPage'
          }"
        >
          <div class="content-shell">
            <router-view />
          </div>
        </a-layout-content>
      </a-layout>
    </a-layout>
  </a-layout>

  <ThemeSettingsDrawer v-model:visible="settingsVisible" />
</template>

<script setup lang="ts">
import {
  AppstoreOutlined,
  HomeOutlined,
  EnvironmentOutlined,
  ToolOutlined,
  CalendarOutlined,
  TeamOutlined,
  ScheduleOutlined,
  UserOutlined,
  UserSwitchOutlined,
  WarningOutlined,
  SettingOutlined,
  LogoutOutlined,
  WalletOutlined,
  QrcodeOutlined,
  BarChartOutlined,
  FolderOutlined,
  FileOutlined,
  LayoutOutlined,
  FormOutlined,
} from '@ant-design/icons-vue';
import ThemeSettingsDrawer from './common/ThemeSettingsDrawer.vue';
import gmsLogoImg from '../assets/Light_logo.svg';
import userAvatarImg from '../assets/user_avatar.jpg';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { message } from 'ant-design-vue';
import { clearAuthStorage } from '@/utils/auth';

const icons: Record<string, any> = {
  AppstoreOutlined,
  HomeOutlined,
  EnvironmentOutlined,
  ToolOutlined,
  CalendarOutlined,
  TeamOutlined,
  ScheduleOutlined,
  UserOutlined,
  UserSwitchOutlined,
  WarningOutlined,
  SettingOutlined,
  LogoutOutlined,
  WalletOutlined,
  QrcodeOutlined,
  BarChartOutlined,
  FolderOutlined,
  LayoutOutlined,
  FileOutlined,
  FormOutlined,
};

const collapsed = ref(false);
const selectedKeys = ref<string[]>(['dashboard']);
const openKeys = ref<string[]>([]);
const settingsVisible = ref(false);

const route = useRoute();
const router = useRouter();
const store = useStore();

const isTraditional = computed(() => store.state.themeSettings.styleMode === 'traditional');
const isDark = computed(() => store.state.themeSettings.isDark);
const hasBorderRadius = computed(() => store.state.themeSettings.borderRadius);
const ledgerShell = computed(() => store.state.themeSettings.ledgerShell);
const sidebarWidth = computed(() => (collapsed.value ? 80 : 259));
const shellSurfaceVars = computed(() => {
  if (isTraditional.value && isDark.value) {
    return {
      '--shell-surface-bg': 'rgba(24, 24, 24, 0.88)',
      '--shell-surface-blur': 'blur(12px)',
      '--shell-header-bg': 'rgba(24, 24, 24, 0.92)',
      '--shell-header-blur': 'blur(12px)',
      '--shell-surface-divider': 'rgba(255,255,255,0.08)',
      '--shell-trigger-border': 'rgba(255,255,255,0.06)',
      '--shell-trigger-icon': 'rgba(255,255,255,0.72)',
      '--shell-trigger-hover-bg': 'rgba(255,255,255,0.08)',
      '--shell-trigger-hover-text': '#ffffff',
      '--shell-brand-text': 'rgba(255,255,255,0.92)',
      '--shell-content-bg': 'rgba(24, 24, 24, 0.98)',
      '--shell-menu-text': 'rgba(255,255,255,0.68)',
      '--shell-menu-text-hover': 'rgba(255,255,255,0.92)',
      '--shell-menu-selected-text': '#ffffff',
      '--shell-menu-selected-bg': 'rgba(255,255,255,0.08)',
      '--shell-menu-icon': 'rgba(255,255,255,0.65)',
      '--shell-menu-arrow': 'rgba(255,255,255,0.45)',
    };
  }

  if (isTraditional.value) {
    return {
      '--shell-surface-bg': 'rgba(255, 255, 255, 0.6)',
      '--shell-surface-blur': 'blur(12px)',
      '--shell-header-bg': 'rgba(247, 247, 245, 0.78)',
      '--shell-header-blur': 'blur(12px)',
      '--shell-surface-divider': 'rgba(0, 0, 0, 0.05)',
      '--shell-trigger-border': 'rgba(15, 23, 42, 0.06)',
      '--shell-trigger-icon': 'rgba(17,17,17,0.56)',
      '--shell-trigger-hover-bg': 'rgba(17,17,17,0.05)',
      '--shell-trigger-hover-text': '#111111',
      '--shell-brand-text': '#111111',
      '--shell-content-bg': 'var(--mono-bg-elevated)',
      '--shell-menu-text': '#666666',
      '--shell-menu-text-hover': '#111111',
      '--shell-menu-selected-text': '#111111',
      '--shell-menu-selected-bg': 'rgba(17,17,17,0.06)',
      '--shell-menu-icon': '#777777',
      '--shell-menu-arrow': 'rgba(17,17,17,0.45)',
    };
  }

  if (isDark.value) {
    return {
      '--shell-surface-bg': 'rgba(20, 24, 31, 0.78)',
      '--shell-surface-blur': 'blur(18px)',
      '--shell-header-bg': 'rgba(20, 24, 31, 0.86)',
      '--shell-header-blur': 'blur(18px)',
      '--shell-surface-divider': 'rgba(255,255,255,0.05)',
      '--shell-trigger-border': 'rgba(255,255,255,0.06)',
      '--shell-trigger-icon': 'rgba(255,255,255,0.72)',
      '--shell-trigger-hover-bg': 'rgba(255,255,255,0.15)',
      '--shell-trigger-hover-text': '#ffffff',
      '--shell-brand-text': 'rgba(255,255,255,0.92)',
      '--shell-content-bg': 'rgba(20, 24, 31, 0.72)',
      '--shell-menu-text': 'rgba(255,255,255,0.66)',
      '--shell-menu-text-hover': 'rgba(255,255,255,0.92)',
      '--shell-menu-selected-text': '#ffffff',
      '--shell-menu-selected-bg': 'rgba(255,255,255,0.08)',
      '--shell-menu-icon': 'rgba(255,255,255,0.6)',
      '--shell-menu-arrow': 'rgba(255,255,255,0.45)',
    };
  }

  return {
    '--shell-surface-bg': 'rgba(255, 255, 255, 0.68)',
    '--shell-surface-blur': 'blur(18px)',
    '--shell-header-bg': 'rgba(255, 255, 255, 0.68)',
    '--shell-header-blur': 'blur(18px)',
    '--shell-surface-divider': 'rgba(17, 17, 17, 0.06)',
    '--shell-trigger-border': 'rgba(17, 17, 17, 0.08)',
    '--shell-trigger-icon': 'rgba(17,17,17,0.56)',
    '--shell-trigger-hover-bg': 'rgba(17,17,17,0.04)',
    '--shell-trigger-hover-text': '#111111',
    '--shell-brand-text': '#111111',
    '--shell-content-bg': '#ffffff',
    '--shell-menu-text': '#6b7280',
    '--shell-menu-text-hover': '#111111',
    '--shell-menu-selected-text': '#111111',
    '--shell-menu-selected-bg': 'rgba(17,17,17,0.04)',
    '--shell-menu-icon': '#9ca3af',
    '--shell-menu-arrow': 'rgba(17, 17, 17, 0.35)',
  };
});
const layoutStyle = computed(() => ({
  minHeight: '100vh',
  '--shell-side-width': `${sidebarWidth.value}px`,
  ...shellSurfaceVars.value,
}));

const rootClasses = computed(() => ({
  'theme-traditional': isTraditional.value,
  'theme-glass': !isTraditional.value,
  'theme-dark': isDark.value,
  'theme-light': !isDark.value,
  'theme-ledger-shell': ledgerShell.value,
  'theme-no-radius': !hasBorderRadius.value,
}));

const siderStyle = computed(() => ({
  top: '56px',
  position: 'fixed' as const,
  height: 'calc(100vh - 56px)',
  background: 'var(--shell-header-bg)',
  backdropFilter: 'var(--shell-header-blur)',
  WebkitBackdropFilter: 'var(--shell-header-blur)',
  borderRight: 'none',
  zIndex: 1900,
  transition: 'all 0.2s',
}));

const menuTree = computed(() => store.getters.getMenuTree);

const handleLogout = () => {
  clearAuthStorage();
  store.commit('SET_ROUTES_LOADED', false);
  store.commit('SET_MENU_TREE', []);

  message.success('退出成功');
  router.push('/login');
};

const findParentKeys = (items: any[], targetPath: string, parents: string[] = []): string[] | null => {
  for (const item of items) {
    if (item.path === targetPath) {
      return parents;
    }
    if (item.children && item.children.length > 0) {
      const result = findParentKeys(item.children, targetPath, [...parents, String(item.id)]);
      if (result) return result;
    }
  }
  return null;
};

watch([() => route.path, menuTree], ([newPath, tree]) => {
  selectedKeys.value = [newPath];

  if (tree && tree.length > 0) {
    const parentKeys = findParentKeys(tree, newPath);
    if (parentKeys && parentKeys.length > 0) {
      parentKeys.forEach(key => {
        if (!openKeys.value.includes(key)) {
          openKeys.value.push(key);
        }
      });
    }
  }
}, { immediate: true });

onMounted(() => {
  store.dispatch('themeSettings/initSystemDarkListener');
});
</script>

<style scoped>
.header {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 2200;
  padding: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 56px;
  background: var(--shell-header-bg);
  backdrop-filter: var(--shell-header-blur);
  -webkit-backdrop-filter: var(--shell-header-blur);
  transition: background 0.3s, border-color 0.3s, color 0.3s;
}

.header-left {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  flex: 0 0 var(--shell-side-width);
  width: var(--shell-side-width);
  height: 100%;
  padding: 0 16px 0 18px;
  transition: width 0.2s ease, padding 0.2s ease;
}

.header-left.collapsed {
  justify-content: center;
  padding: 0;
}

.header-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  height: 100%;
  padding: 0;
}

.header-brand-logo {
  height: 22px;
  width: auto;
  object-fit: contain;
  display: block;
}

.header-brand-logo.is-dark {
  filter: brightness(0) invert(1);
}

.header-left.collapsed .header-brand {
  gap: 0;
}

.header-left.collapsed .header-brand-logo {
  height: 20px;
}

.header-brand-title {
  color: var(--shell-brand-text);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.header-right {
  display: flex;
  flex: 1;
  height: 100%;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  padding: 0 24px;
}

.settings-trigger {
  width: 36px;
  height: 36px;
  border-radius: var(--mono-radius-pill);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  color: var(--shell-trigger-icon);
  background: transparent;
}

.settings-trigger:hover {
  background: var(--shell-trigger-hover-bg);
  color: var(--shell-trigger-hover-text);
  transform: scale(1.05);
}

.plain-avatar--header {
  width: 34px;
  height: 34px;
  cursor: pointer;
}

.content {
  margin: 0;
  min-height: calc(100vh - 56px);
  padding: 56px 0 0;
  margin-top: 0px;
  overflow: visible;
  transition: background 0.3s;
  box-sizing: border-box !important;
}

.content.is-dashboard {
  padding-top: 72px;
}

.content-shell {
  min-height: calc(100vh - 56px);
  padding: 24px;
  border-radius: 0;
  background: transparent;
  box-sizing: border-box;
  transition: background 0.3s, border-color 0.3s, box-shadow 0.3s, backdrop-filter 0.3s;
}

.theme-glass .content-shell {
  border: none;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  box-shadow: none;
}

.theme-traditional .content-shell {
  border: none;
  box-shadow: none;
}

.theme-dark.theme-glass .content-shell {
  border-color: transparent;
  box-shadow: none;
}

.theme-dark.theme-traditional .content-shell {
  border-color: transparent;
  box-shadow: none;
}

.theme-ledger-shell .content-shell {
  min-height: calc(100vh - 72px);
  margin-top: 16px;
  border-radius: var(--mono-radius-xl) 0 0 0;
  background: var(--shell-content-bg);
}

.theme-ledger-shell .content {
  padding-top: 56px !important;
}

.theme-ledger-shell .content.is-dashboard {
  padding-top: 72px !important;
}

.theme-ledger-shell .content.is-dashboard .content-shell {
  min-height: calc(100vh - 88px);
}

.theme-ledger-shell.theme-glass .content-shell {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  box-shadow: none;
}

.theme-ledger-shell.theme-traditional .content-shell {
  border: 1px solid rgba(15, 23, 42, 0.04);
  box-shadow: none;
}

.theme-ledger-shell.theme-dark.theme-glass .content-shell {
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow: none;
}

.theme-ledger-shell.theme-dark.theme-traditional .content-shell {
  border-color: rgba(255, 255, 255, 0.05);
  box-shadow: none;
}

:deep(.ant-layout-sider-trigger) {
  background: var(--shell-header-bg) !important;
  backdrop-filter: var(--shell-header-blur) !important;
  -webkit-backdrop-filter: var(--shell-header-blur) !important;
  border: 1px solid var(--shell-trigger-border) !important;
  transition: background 0.3s, border-color 0.3s;
}

:deep(.ant-layout-sider-trigger .anticon) {
  color: var(--shell-trigger-icon);
}

:deep(.ant-layout-sider-trigger:hover) {
  background-color: var(--shell-trigger-hover-bg) !important;
}

:deep(.ant-layout-sider-children) {
  height: calc(100vh - 56px) !important;
  overflow-y: auto !important;
  overflow-x: hidden !important;
  scroll-behavior: smooth;
}

:deep(.ant-menu) {
  background: transparent !important;
}

:deep(.ant-menu-item),
:deep(.ant-menu-submenu-title) {
  border-radius: var(--mono-radius-sm);
  transition: background-color 0.2s ease, color 0.2s ease;
}

:deep(.ant-menu-item),
:deep(.ant-menu-item a),
:deep(.ant-menu-submenu-title),
:deep(.ant-menu-submenu-title span),
:deep(.ant-menu-submenu-title .ant-menu-title-content),
:deep(.ant-menu-item .ant-menu-title-content) {
  color: var(--shell-menu-text) !important;
}

:deep(.ant-menu-item:hover),
:deep(.ant-menu-item:hover a),
:deep(.ant-menu-submenu-title:hover),
:deep(.ant-menu-submenu-title:hover span),
:deep(.ant-menu-submenu-title:hover .ant-menu-title-content) {
  color: var(--shell-menu-text-hover) !important;
}

:deep(.ant-menu-item-selected),
:deep(.ant-menu-item-selected a),
:deep(.ant-menu-item-selected .ant-menu-title-content) {
  color: var(--shell-menu-selected-text) !important;
  font-weight: 600;
}

:deep(.ant-menu-item-selected) {
  background-color: var(--shell-menu-selected-bg) !important;
}

:deep(.ant-menu-submenu-selected > .ant-menu-submenu-title),
:deep(.ant-menu-submenu-selected > .ant-menu-submenu-title span),
:deep(.ant-menu-submenu-selected > .ant-menu-submenu-title .ant-menu-title-content),
:deep(.ant-menu-submenu-open > .ant-menu-submenu-title),
:deep(.ant-menu-submenu-open > .ant-menu-submenu-title span),
:deep(.ant-menu-submenu-open > .ant-menu-submenu-title .ant-menu-title-content) {
  color: var(--shell-menu-text-hover) !important;
}

:deep(.ant-menu-item::after),
:deep(.ant-menu-submenu-title::after) {
  display: none !important;
  border: 0 !important;
}

:deep(.ant-menu-item .anticon),
:deep(.ant-menu-submenu-title .anticon) {
  color: var(--shell-menu-icon) !important;
}

:deep(.ant-menu-item-selected .anticon),
:deep(.ant-menu-submenu-selected > .ant-menu-submenu-title .anticon),
:deep(.ant-menu-submenu-open > .ant-menu-submenu-title .anticon) {
  color: var(--shell-menu-selected-text) !important;
}

:deep(.ant-menu-submenu .ant-menu-sub) {
  background: transparent !important;
}

:deep(.ant-menu-submenu-arrow::before),
:deep(.ant-menu-submenu-arrow::after) {
  background: var(--shell-menu-arrow) !important;
}

:deep(.theme-traditional) .header,
.theme-traditional .header {
  background: var(--shell-header-bg) !important;
  backdrop-filter: var(--shell-header-blur) !important;
  -webkit-backdrop-filter: var(--shell-header-blur) !important;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.theme-traditional .content {
  padding: 56px 0 0 !important;
}

.theme-traditional .content.is-dashboard {
  padding-top: 72px !important;
}

.theme-traditional.theme-dark .content {
  padding-right: 0 !important;
}

.theme-traditional :deep(.ant-layout-sider-trigger) {
  background: var(--shell-header-bg) !important;
  backdrop-filter: var(--shell-header-blur) !important;
  -webkit-backdrop-filter: var(--shell-header-blur) !important;
  border: 1px solid var(--shell-trigger-border) !important;
}

.theme-traditional :deep(.ant-menu-item-selected) {
  background-color: var(--shell-menu-selected-bg) !important;
}

.theme-no-radius :deep(.glass-container),
.theme-no-radius :deep(.glass-panel),
.theme-no-radius :deep(.ant-modal-content),
.theme-no-radius :deep(.ant-table-container),
.theme-no-radius :deep(.ant-btn),
.theme-no-radius :deep(.ant-input),
.theme-no-radius :deep(.ant-select-selector),
.theme-no-radius :deep(.ant-tag) {
  border-radius: var(--mono-radius-xs) !important;
}

.theme-no-radius .content-shell {
  border-radius: var(--mono-radius-xs) !important;
}
</style>
