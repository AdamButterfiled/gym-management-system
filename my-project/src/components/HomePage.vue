<template>
  <a-config-provider
    :theme="{
      token: {
        colorPrimary: themeColor,
      },
    }"
  >
  </a-config-provider>
  <a-layout 
    :style="{ minHeight: '100vh', '--shell-side-width': `${sidebarWidth}px` }"
    :class="rootClasses"
  >
    <!-- 固定头部 -->
    <a-layout-header class="header">
      <div class="header-left" :class="{ collapsed }">
        <div class="header-brand">
          <img :src="gmsLogoImg" class="header-brand-logo" :class="{ 'is-dark': isDark }" alt="GVS" />
          <span v-show="!collapsed" class="header-brand-title">健身房场馆预约系统</span>
        </div>
      </div>
      <div class="header-right">
        <!-- 主题设置图标 -->
        <div class="settings-trigger" @click="settingsVisible = true" style="margin-right: 4px;">
          <LayoutOutlined style="font-size: 18px;" />
        </div>
        <a-dropdown>
          <!-- 用户头像 -->
          <a-avatar :src="userAvatarImg" style="width: 34px; height: 34px; border: 2px solid #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.15); cursor: pointer;" />
          <!-- 下拉菜单内容 -->
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
      <!-- 侧边栏 -->
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
            theme="light" 
            style="background: transparent; border-right: none;"
        >
                  <!-- 动态渲染菜单 -->
          <!-- 1. 静态 Dashboard (如果需要固定在第一个) -->
          <a-menu-item key="dashboard" style="margin-top: 16px;">
            <router-link to="/dashboard">
              <AppstoreOutlined />
              <span>工作台</span>
            </router-link>
          </a-menu-item>

          <!-- 2. 动态菜单部分 -->
          <template v-for="item in menuTree" :key="item.id">
              
              <!-- Special Handling for System Root (ID 999): Flatten it visually -->
              <template v-if="String(item.id) === '999' && item.children">
                  <template v-for="child in item.children" :key="child.path">
                      <!-- Case A: Child has children (SubMenu) -->
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

                      <!-- Case B: Child has no children (MenuItem) -->
                      <a-menu-item v-else :key="child.path">
                          <router-link :to="child.path">
                              <component :is="icons[child.icon]" v-if="child.icon && icons[child.icon]" />
                              <FileOutlined v-else />
                              <span>{{ child.title }}</span>
                          </router-link>
                      </a-menu-item>
                  </template>
              </template>

              <!-- Standard Items (Not Root 999) -->
              <template v-else>
                  <!-- 情况A: 有子菜单 (SubMenu) -->
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
    
                  <!-- 情况B: 无子菜单 -->
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

      <!-- 内容区域 -->
      <a-layout :style="{ marginLeft: `${sidebarWidth}px`, marginTop: '0px' }">
        <a-layout-content class="content" :class="{ 
            'transparent-glass-mode': route.meta.style === 'glass',
            'default-yellow-mode': route.meta.style !== 'glass',
            'is-dashboard': route.path === '/dashboard' || route.name === 'DashboardPage'
        }">
          <router-view />
        </a-layout-content>
      </a-layout>
    </a-layout>
  </a-layout>

  <!-- Theme Settings Drawer -->
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
  FolderOutlined,
  FileOutlined,
  LayoutOutlined,
} from "@ant-design/icons-vue";

import ThemeSettingsDrawer from './common/ThemeSettingsDrawer.vue';

// Define an icon map for dynamic rendering
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
  FolderOutlined,
  LayoutOutlined,
  FileOutlined
};

import gmsLogoImg from "../assets/Light_logo.svg";
import userAvatarImg from "../assets/user_avatar.jpg";
import { ref, watch, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useStore } from "vuex";
import { message } from 'ant-design-vue';

const collapsed = ref(false);
const selectedKeys = ref<string[]>(["dashboard"]);
const openKeys = ref<string[]>([]);
const settingsVisible = ref(false);

const route = useRoute();
const router = useRouter();
const store = useStore();

// Theme settings
const themeColor = computed(() => store.state.themeSettings.themeColor);
const isTraditional = computed(() => store.state.themeSettings.styleMode === 'traditional');
const isDark = computed(() => store.state.themeSettings.isDark);
const hasBorderRadius = computed(() => store.state.themeSettings.borderRadius);
const sidebarWidth = computed(() => collapsed.value ? 80 : 259);

// Root CSS classes
const rootClasses = computed(() => ({
  'theme-traditional': isTraditional.value,
  'theme-glass': !isTraditional.value,
  'theme-dark': isDark.value,
  'theme-light': !isDark.value,
  'theme-no-radius': !hasBorderRadius.value,
}));

// Dynamic sider style: glass or traditional
const siderStyle = computed(() => {
  if (isDark.value) {
    return {
      top: '56px',
      position: 'fixed' as const,
      height: 'calc(100vh - 56px)',
      background: isTraditional.value ? '#1f1f1f' : 'rgba(31, 31, 31, 0.86)',
      backdropFilter: isTraditional.value ? 'none' : 'blur(14px)',
      WebkitBackdropFilter: isTraditional.value ? 'none' : 'blur(14px)',
      borderRight: 'none',
      zIndex: 1900,
      transition: 'all 0.2s',
    };
  }
  return {
    top: '56px',
    position: 'fixed' as const,
    height: 'calc(100vh - 56px)',
    background: isTraditional.value ? '#fff' : 'rgba(255, 255, 255, 0.78)',
    backdropFilter: isTraditional.value ? 'none' : 'blur(14px)',
    WebkitBackdropFilter: isTraditional.value ? 'none' : 'blur(14px)',
    borderRight: isTraditional.value ? 'none' : '1px solid rgba(15, 23, 42, 0.06)',
    zIndex: 1900,
    transition: 'all 0.2s',
  };
});

// 计算属性获取 Vuex 中的菜单树
const menuTree = computed(() => store.getters.getMenuTree);

// 退出登录逻辑
const handleLogout = () => {
    localStorage.removeItem("user");
    localStorage.removeItem("isLoggedIn");
    localStorage.removeItem("token");
    store.commit('SET_ROUTES_LOADED', false);
    store.commit('SET_MENU_TREE', []);
    
    message.success("退出成功");
    router.push("/login");
};

// 递归查找当前路由所属的父级菜单 keys
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

// 监听路由变化并更新 selectedKeys 和 openKeys
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

// Init system dark mode listener
onMounted(() => {
  store.dispatch('themeSettings/initSystemDarkListener');
});
</script>

<style scoped>
/* ============================================================
   BASE STYLES (Glass Mode / Light)
   ============================================================ */

/* Header */
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
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  transition: background 0.3s, border-color 0.3s, color 0.3s;
}

.header::after {
  content: "";
  position: absolute;
  left: var(--shell-side-width);
  right: 0;
  bottom: 0;
  height: 1px;
  background: rgba(0, 0, 0, 0.05);
  transition: left 0.2s ease, background 0.3s;
  pointer-events: none;
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
  color: #111;
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

/* Settings trigger button */
.settings-trigger {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  color: #666;
  background: transparent; /* Transparent by default */
}

.settings-trigger:hover {
  background: rgba(0,0,0,0.05); /* Gray on hover */
  color: #333;
  transform: scale(1.05);
}

/* Content */
.content {
  margin: 0;
  background: #fff;
  min-height: calc(100vh - 56px);
  padding: 24px;
  /* <--- [Adjust] Glass Mode Top Spacing (Light) / 玻璃浅色模式顶部间距 (Header is 56px, so 62px = 6px gap - move up as requested) */
  padding-top: 79px;
  border-radius: 0px;
  margin-top: 0px; 
  overflow: visible;
  transition: background 0.3s;
  box-sizing: border-box !important;
}

/* Sider trigger */
:deep(.ant-layout-sider-trigger) {
  background: rgba(255, 255, 255, 0.78) !important;
  backdrop-filter: blur(14px) !important;
  -webkit-backdrop-filter: blur(14px) !important;
  border: 1px solid rgba(15, 23, 42, 0.06) !important;
  transition: background 0.3s, border-color 0.3s;
}

:deep(.ant-layout-sider-trigger .anticon) {
  color: #2323235f;
}

:deep(.ant-layout-sider-trigger:hover) {
  background-color: rgba(255, 255, 255, 0.92) !important;
}

:deep(.ant-layout-sider-children) {
  height: calc(100vh - 56px) !important;
  overflow-y: auto !important;
  overflow-x: hidden !important;
  scroll-behavior: smooth;
}

/* ============================================================
   TRADITIONAL STYLE OVERRIDES (No glass effects)
   ============================================================ */
:deep(.theme-traditional) .header,
.theme-traditional .header {
  background: #ffffff !important;
  backdrop-filter: none !important;
  -webkit-backdrop-filter: none !important;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

:deep(.theme-traditional) .header::after,
.theme-traditional .header::after {
  background: #e8e8e8;
}

.theme-traditional .content {
  background: #f5f5f5 !important; /* <--- [Adjust] Background Color (Grey #f5f5f5 to distinguish from sidebar) */
  /* <--- [Adjust] Side Spacing */
  padding: 13px 13px !important;
  /* <--- [Adjust] Content Top Spacing (Default for all pages) */
  padding-top: 57px !important; 
}

/* Dashboard Specific Spacing in Traditional Mode */
.theme-traditional .content.is-dashboard {
  /* <--- [Adjust] Dashboard Top Spacing / 工作台顶部间距 (Extra buffer) */
  padding-top: 74px !important; 
}

/* Traditional Mode Dark Side Spacing */
.theme-traditional.theme-dark .content {
    /* <--- [Adjust] Traditional Dark Mode Side Spacing / 传统深色模式两侧间距 (Larger margins) */
    padding-left: 17px !important;
    padding-right: 17px !important;
}

.theme-traditional :deep(.ant-layout-sider-trigger) {
  background: #fff !important;
  backdrop-filter: none !important;
  -webkit-backdrop-filter: none !important;
  border: 1px solid #e8e8e8 !important;
}

/* Selected Menu Item Background in Traditional Mode */
.theme-traditional :deep(.ant-menu-item-selected) {
    /* <--- [Adjust] Selected Item Background / 选中菜单项背景 */
    background-color: #fafafa !important; 
}

/* ============================================================
   DARK MODE OVERRIDES
   ============================================================ */

/* --- Dark (shared for both glass and traditional) --- */
.theme-dark .header {
  background: #1f1f1f !important;
  backdrop-filter: none !important;
  -webkit-backdrop-filter: none !important;
  box-shadow: none;
}

.theme-dark .header::after {
  background: rgba(255,255,255,0.05);
}

.theme-dark .header-brand {
  color: inherit;
}

.theme-dark .header-brand-title {
  color: #fff;
}

.theme-dark .content {
  background: #141414 !important;
}

/* Glass Mode Dark Specific Spacing */
.theme-glass.theme-dark .content {
    /* <--- [Adjust] Glass Dark Mode Top Spacing / 玻璃深色模式顶部间距 (Move down to avoid squeezed look) */
    padding-top: 79px !important;
}

.theme-dark .settings-trigger {
  color: #ffffffa6;
  background: rgba(255,255,255,0.08);
}

.theme-dark .settings-trigger:hover {
  background: rgba(255,255,255,0.15);
  color: #ffffffd9;
}

.theme-dark :deep(.ant-layout-sider-trigger) {
  background: rgba(31, 31, 31, 0.88) !important;
  backdrop-filter: blur(14px) !important;
  -webkit-backdrop-filter: blur(14px) !important;
  border: 1px solid rgba(255,255,255,0.06) !important;
}

.theme-dark :deep(.ant-layout-sider-trigger .anticon) {
  color: #ffffffa6;
}

/* --- Dark mode: Menu --- */
.theme-dark :deep(.ant-menu) {
  background: transparent !important;
}

.theme-dark :deep(.ant-menu-item),
.theme-dark :deep(.ant-menu-item a),
.theme-dark :deep(.ant-menu-submenu-title),
.theme-dark :deep(.ant-menu-submenu-title span),
.theme-dark :deep(.ant-menu-submenu-title .ant-menu-title-content),
.theme-dark :deep(.ant-menu-item .ant-menu-title-content) {
  color: rgba(255,255,255,0.65) !important;
}

.theme-dark :deep(.ant-menu-item:hover),
.theme-dark :deep(.ant-menu-item:hover a),
.theme-dark :deep(.ant-menu-submenu-title:hover),
.theme-dark :deep(.ant-menu-submenu-title:hover span) {
  color: rgba(255,255,255,0.85) !important;
}

.theme-dark :deep(.ant-menu-item-selected),
.theme-dark :deep(.ant-menu-item-selected a),
.theme-dark :deep(.ant-menu-item-selected .ant-menu-title-content) {
  color: #fff !important;
  font-weight: 500;
}

.theme-dark :deep(.ant-menu-item-selected) {
  background-color: rgba(255,255,255,0.08) !important;
}

.theme-dark :deep(.ant-menu-item::after),
.theme-dark :deep(.ant-menu-submenu-title::after) {
  display: none !important;
  border: 0 !important;
}

.theme-dark :deep(.ant-menu-submenu-open > .ant-menu-submenu-title),
.theme-dark :deep(.ant-menu-submenu-open > .ant-menu-submenu-title span) {
  color: rgba(255,255,255,0.85) !important;
}

.theme-dark :deep(.ant-menu-submenu .ant-menu-sub) {
  background: transparent !important;
}

.theme-dark :deep(.ant-menu-submenu-arrow::before),
.theme-dark :deep(.ant-menu-submenu-arrow::after) {
  background: rgba(255,255,255,0.45) !important;
}

.theme-dark :deep(.ant-menu-item .anticon),
.theme-dark :deep(.ant-menu-submenu-title .anticon) {
  color: rgba(255,255,255,0.65) !important;
}

.theme-dark :deep(.ant-menu-item-selected .anticon) {
  color: #fff !important;
}

/* ============================================================
   NO BORDER RADIUS
   ============================================================ */
.theme-no-radius :deep(.glass-container),
.theme-no-radius :deep(.glass-panel),
.theme-no-radius :deep(.ant-modal-content),
.theme-no-radius :deep(.ant-table-container),
.theme-no-radius :deep(.ant-btn),
.theme-no-radius :deep(.ant-input),
.theme-no-radius :deep(.ant-select-selector),
.theme-no-radius :deep(.ant-tag) {
  border-radius: 2px !important;
}
</style>
