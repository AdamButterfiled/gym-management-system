<template>
  <a-config-provider :locale="locale" :theme="antdTheme">
    <router-view></router-view> <!-- 使用router-view代替直接组件引用 -->
  </a-config-provider>
</template>

<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import { computed, watch } from 'vue';
import { useStore } from 'vuex';

const locale = zhCN;
const store = useStore();

const themeColor = computed(() => store.state.themeSettings?.themeColor || '#f7b500');
const isDark = computed(() => store.state.themeSettings?.isDark || false);

// Ant Design theme configuration
const antdTheme = computed(() => ({
  token: {
    colorPrimary: themeColor.value,
  },
}));

// Watch for dark mode changes and apply to <html>
watch(isDark, (dark) => {
  if (dark) {
    document.documentElement.classList.add('dark');
    document.documentElement.setAttribute('data-theme', 'dark');
  } else {
    document.documentElement.classList.remove('dark');
    document.documentElement.setAttribute('data-theme', 'light');
  }
}, { immediate: true });

// Watch for style mode
const isTraditional = computed(() => store.state.themeSettings?.styleMode === 'traditional');
watch(isTraditional, (trad) => {
  if (trad) {
    document.documentElement.classList.add('theme-traditional-global');
    document.documentElement.classList.remove('theme-glass-global');
  } else {
    document.documentElement.classList.remove('theme-traditional-global');
    document.documentElement.classList.add('theme-glass-global');
  }
}, { immediate: true });

// Watch for border radius
const hasBorderRadius = computed(() => store.state.themeSettings?.borderRadius !== false);
watch(hasBorderRadius, (has) => {
  if (has) {
    document.documentElement.classList.remove('theme-no-radius-global');
  } else {
    document.documentElement.classList.add('theme-no-radius-global');
  }
}, { immediate: true });

const ignoreErrors = ["ResizeObserver loop completed with undelivered notifications", "ResizeObserver loop limit exceeded"]

window.addEventListener('error', e => {
let errorMsg = e.message;
ignoreErrors.forEach(m => {
if (errorMsg.startsWith(m)) {

const resizeObserverErrDiv = document.getElementById(
'webpack-dev-server-client-overlay-div'
);
const resizeObserverErr = document.getElementById(
'webpack-dev-server-client-overlay'
);
if (resizeObserverErr) {
resizeObserverErr.setAttribute('style', 'display: none');
}
if (resizeObserverErrDiv) {
resizeObserverErrDiv.setAttribute('style', 'display: none');
}
}
})
});

</script>

<style>
#app {
  min-height: 100vh;
  width: 100%;
  background-color: #f5f5f5;
  transition: background-color 0.3s;
}

html.dark #app {
  background-color: #141414;
}
</style>

<style>
/* =====================================================================================
   GLASS MODE TABLE STYLES — only active for glass mode (default)
   ===================================================================================== */

/* Default Yellow Style */
.default-yellow-mode .ant-table-thead > tr > th {
    background-color: #F8F6EF !important; 
    color: #867E7E !important;            
    font-size: 13.6px !important;
    text-align: center !important;        
    border-radius: 0px; 
    border: 0.05px solid #F4EFEE !important;
}

.default-yellow-mode .ant-table-tbody > tr > td {
    text-align: center !important;        
    border: 0.05px solid #F4EFEE !important;
}

/* Fix: Remove header column separators in default mode */
.default-yellow-mode .ant-table-thead > tr > th::before {
    display: none !important;
}

.default-yellow-mode .ant-table-tbody {
    background-color: #FDFCF9 !important;
}

.default-yellow-mode .ant-table-tbody > tr:nth-child(even) > td {
    background-color: #FDFCF9 !important;
}

.default-yellow-mode .ant-table-tbody > tr:nth-child(odd) > td {
    background-color: #FFFFFF !important;
}

.default-yellow-mode .ant-table-tbody > tr:hover > td {
    background-color: #faf7f0 !important;
}

.default-yellow-mode .ant-table-container {
    border: 0.8px solid #F4EFEE !important;
}

/* Transparent Glass Style */
.transparent-glass-mode .ant-table {
    background: transparent !important;
}

.transparent-glass-mode .ant-table-container {
    background: transparent !important;
    border: none !important;
    border-radius: 0;
}

.transparent-glass-mode .ant-table-thead > tr > th {
    background: transparent !important;
    color: #666 !important;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06) !important;
}

.transparent-glass-mode .ant-table-thead > tr > th::before {
    display: none !important;
}

.transparent-glass-mode .ant-table-tbody > tr > td {
    background: transparent !important;
    color: #333 !important;
    border-bottom: 1px solid rgba(0, 0, 0, 0.04) !important;
}

.transparent-glass-mode .ant-table-tbody > tr:hover > td {
    background: rgba(0, 0, 0, 0.02) !important;
}

.transparent-glass-mode .ant-table-cell,
.transparent-glass-mode .ant-table-thead > tr > th,
.transparent-glass-mode .ant-table-tbody > tr > td {
    border-left: none !important;
    border-right: none !important;
}

.transparent-glass-mode .ant-table-cell-fix-left,
.transparent-glass-mode .ant-table-cell-fix-right {
    background: rgba(255, 255, 255, 0.14) !important;
    box-shadow: none !important;
}

.transparent-glass-mode .ant-table-ping-right .ant-table-cell-fix-right-first::after,
.transparent-glass-mode .ant-table-ping-left .ant-table-cell-fix-left-last::after {
    box-shadow: none !important;
}

.transparent-glass-mode .ant-table-row-expand-icon {
    border-radius: 999px !important;
    border-color: rgba(15, 23, 42, 0.1) !important;
    background: rgba(255, 255, 255, 0.32) !important;
    color: #475569 !important;
}

.transparent-glass-mode .ant-table-row-expand-icon:hover {
    background: rgba(255, 255, 255, 0.5) !important;
    border-color: rgba(15, 23, 42, 0.16) !important;
}

.transparent-glass-mode .ant-table-placeholder,
.transparent-glass-mode .ant-empty-normal {
    background: transparent !important;
}

.transparent-glass-mode .ant-table-pagination.ant-pagination {
    margin: 18px 0 0 !important;
}

.transparent-glass-mode .ant-pagination-item,
.transparent-glass-mode .ant-pagination-prev .ant-pagination-item-link,
.transparent-glass-mode .ant-pagination-next .ant-pagination-item-link {
    border-radius: 12px !important;
    border-color: rgba(15, 23, 42, 0.08) !important;
    background: rgba(255, 255, 255, 0.36) !important;
    box-shadow: none !important;
}

.transparent-glass-mode .ant-pagination-item a,
.transparent-glass-mode .ant-pagination-prev .ant-pagination-item-link,
.transparent-glass-mode .ant-pagination-next .ant-pagination-item-link {
    color: #4b5563 !important;
}

.transparent-glass-mode .ant-pagination-item-active {
    border-color: rgba(247, 181, 0, 0.32) !important;
    background: rgba(247, 181, 0, 0.12) !important;
}

.transparent-glass-mode .ant-pagination-item-active a {
    color: #b7791f !important;
}

/* =====================================================================================
   GLASS MODE MODAL STYLES — The beautiful glass modal effect
   Disabled when html has .theme-traditional-global
   ===================================================================================== */

/* 1. Hide actual mask color */
html:not(.theme-traditional-global) .ant-modal-mask {
   z-index: 100001 !important;
   background-color: transparent !important; 
   backdrop-filter: none !important;
   -webkit-backdrop-filter: none !important;
}
html:not(.theme-traditional-global) .ant-modal-wrap {
   z-index: 100001 !important;
}

/* 2. Modal Content - Glass effect */
html:not(.theme-traditional-global) .ant-modal-content {
   background-color: rgba(255, 255, 255, 0.55) !important;
   backdrop-filter: blur(16px) !important; 
   -webkit-backdrop-filter: blur(16px) !important;
   border: 1px solid rgba(255, 255, 255, 0.8) !important;
   border-radius: 20px !important;
   box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.1), 
               0 0 0 9999px rgba(0, 0, 0, 0.45) !important;
}

/* 3. Shared modal internal styles — apply to BOTH glass and traditional */
.ant-modal-header, 
.ant-modal-body,
.ant-modal-footer {
   background: transparent !important;
   background-color: transparent !important;
   border-bottom: 0px solid transparent !important;
   border-top: 0px solid transparent !important;
}

/* 4. Title */
.ant-modal-header {
   padding: 16px 24px 0 24px !important; 
   margin-bottom: 0px !important;
   text-align: left !important;
   border-bottom: none !important;
}

.ant-modal-title {
    font-weight: 700 !important;
    color: #333 !important;
    font-size: 18px !important;
    background: transparent !important;
}

.ant-modal-body {
    padding: 24px !important; 
}

.ant-modal-close-x {
    background-color: transparent !important;
    color: #666 !important;
}

/* 5. Glass mode: Semi-Transparent inputs in Modals */
html:not(.theme-traditional-global) .ant-modal .ant-input,
html:not(.theme-traditional-global) .ant-modal .ant-input-number-input {
   background-color: rgba(255, 255, 255, 0.4) !important; 
   border: 1px solid rgba(255, 255, 255, 0.2) !important;
   border-radius: 8px !important;
   height: 40px !important;
   box-shadow: none !important;
}
html:not(.theme-traditional-global) .ant-modal .ant-select-selector {
   background-color: rgba(255, 255, 255, 0.4) !important; 
   border: 1px solid rgba(255, 255, 255, 0.2) !important;
   border-radius: 8px !important;
   min-height: 40px !important;
   height: 40px !important;
   display: flex !important;
   align-items: center !important;
   box-shadow: none !important;
}

/* Glass mode: Dropdowns */
html:not(.theme-traditional-global) .ant-select-dropdown,
html:not(.theme-traditional-global) .ant-picker-dropdown .ant-picker-panel-container,
html:not(.theme-traditional-global) .ant-cascader-menus,
html:not(.theme-traditional-global) .ant-tree-select-dropdown {
   background-color: rgba(255, 255, 255, 0.35) !important;
   backdrop-filter: blur(10px) !important; 
   -webkit-backdrop-filter: blur(10px) !important;
   border: 1px solid rgba(255, 255, 255, 0.4) !important;
   box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.1) !important;
   border-radius: 12px !important;
}

/* =====================================================================================
   TRADITIONAL MODE MODAL STYLES — Standard opaque modal
   ===================================================================================== */

html.theme-traditional-global .ant-modal-mask {
   z-index: 100001 !important;
   background-color: rgba(0, 0, 0, 0.45) !important;
   backdrop-filter: none !important;
}
html.theme-traditional-global .ant-modal-wrap {
   z-index: 100001 !important;
}

html.theme-traditional-global .ant-modal-content {
   background-color: #fff !important;
   backdrop-filter: none !important;
   -webkit-backdrop-filter: none !important;
   border: 1px solid #e8e8e8 !important;
   border-radius: 8px !important;
   box-shadow: 0 6px 16px 0 rgba(0, 0, 0, 0.08),
               0 3px 6px -4px rgba(0, 0, 0, 0.12),
               0 9px 28px 8px rgba(0, 0, 0, 0.05) !important;
}

/* Traditional mode: Solid inputs in modals */
html.theme-traditional-global .ant-modal .ant-input,
html.theme-traditional-global .ant-modal .ant-input-number-input {
   background-color: #fff !important;
   border: 1px solid #d9d9d9 !important;
   border-radius: 6px !important;
   height: 40px !important;
   box-shadow: none !important;
}
html.theme-traditional-global .ant-modal .ant-select-selector {
   background-color: #fff !important;
   border: 1px solid #d9d9d9 !important;
   border-radius: 6px !important;
   min-height: 40px !important;
   height: 40px !important;
   display: flex !important;
   align-items: center !important;
   box-shadow: none !important;
}

/* Traditional mode dropdowns */
html.theme-traditional-global .ant-select-dropdown,
html.theme-traditional-global .ant-picker-dropdown .ant-picker-panel-container,
html.theme-traditional-global .ant-cascader-menus,
html.theme-traditional-global .ant-tree-select-dropdown {
   background-color: #fff !important;
   backdrop-filter: none !important;
   -webkit-backdrop-filter: none !important;
   border: 1px solid #e8e8e8 !important;
   box-shadow: 0 6px 16px 0 rgba(0, 0, 0, 0.08) !important;
   border-radius: 8px !important;
}

/* Traditional mode: ALL inputs and selects use solid borders (page-level, not just modal) */
html.theme-traditional-global .ant-input {
   background-color: #fff !important;
   border: 1px solid #d9d9d9 !important;
   box-shadow: none !important;
}
html.theme-traditional-global .ant-input-affix-wrapper {
   background-color: #fff !important;
   border: 1px solid #d9d9d9 !important;
   box-shadow: none !important;
}
html.theme-traditional-global .ant-select:not(.ant-select-customize-input) .ant-select-selector {
   box-shadow: none !important;
}

/* =====================================================================================
   SHARED MODAL + DROPDOWN STYLES (Apply to both modes)
   ===================================================================================== */

/* z-index for dropdowns above modal */
.ant-select-dropdown,
.ant-picker-dropdown, 
.ant-cascader-menus, 
.ant-tree-select-dropdown, 
.ant-message,
.ant-notification {
    z-index: 100005 !important;
}

/* Select text vertically centered */
.ant-modal .ant-select-selection-item,
.ant-modal .ant-select-selection-placeholder {
   line-height: 40px !important;
   display: flex !important;
   align-items: center !important;
}

/* UNIFIED MODAL INPUT CLASS */
.modal-input-unified {
    width: 320px !important;
    border-radius: 8px !important;
    border: none !important;
    height: 40px !important;
}

.modal-input-unified .ant-input,
.modal-input-unified.ant-input,
.modal-input-unified .ant-select-selector,
.modal-input-unified .ant-select,
.modal-input-unified.ant-input-number,
.modal-input-unified .ant-input-number-input,
.modal-input-unified textarea,
textarea.modal-input-unified {
    border: none !important;
}

html:not(.theme-traditional-global) .modal-input-unified,
html:not(.theme-traditional-global) .modal-input-unified .ant-input,
html:not(.theme-traditional-global) .modal-input-unified.ant-input {
    background-color: #F7F5F5 !important;
}

html.theme-traditional-global .modal-input-unified,
html.theme-traditional-global .modal-input-unified .ant-input,
html.theme-traditional-global .modal-input-unified.ant-input {
    background-color: #fff !important;
    border: 1px solid #d9d9d9 !important;
}

/* Allow textareas to have dynamic height */
.modal-input-unified textarea,
textarea.modal-input-unified,
.modal-input-unified.ant-input-affix-wrapper {
    height: auto !important;
}

/* Fix Form Item Alignment */
.ant-modal .ant-form-item .ant-row {
    align-items: center !important;
}

.ant-modal .ant-input:focus,
.ant-modal .ant-input-focused,
.ant-modal .ant-select-selector:focus {
    background-color: #F0EEEE !important;
    border: none !important;
    box-shadow: none !important;
}

.ant-modal .ant-form-item-label > label {
    color: #666 !important; 
}

/* Footer Button Styles */
.ant-modal-footer {
    padding-bottom: 24px !important;
    padding-right: 24px !important;
}
.ant-modal-footer .ant-btn {
    border-radius: 24px !important;
    height: 38px !important;
    padding: 0 25px !important;
    font-weight: 500;
    box-shadow: none !important; 
}
/* Cancel button */
.ant-modal-footer .ant-btn-default {
    background-color: rgba(255, 255, 255, 0.5) !important;
    border: none !important;
    color: #666 !important;
}
.ant-modal-footer .ant-btn-default:hover {
    color: #333 !important;
    background-color: rgba(255, 255, 255, 0.8) !important;
}

/* Spacing for Hints */
.ant-form-item-extra {
    margin-top: 12px !important;
    color: #999 !important;
    font-size: 12px;
}

/* Remove default white backgrounds for dropdowns */
.ant-select-item,
.ant-select-item-option,
.ant-cascader-menu-item {
   background: transparent !important; 
   color: #333 !important;
}

/* FIX: Tree Select Backgrounds */
.ant-select-tree,
.ant-select-tree-list,
.ant-select-tree-list-holder-inner {
    background: transparent !important;
    background-color: transparent !important;
}

/* Hover/Active states */
.ant-select-item-option-active,
.ant-select-item-option-selected,
.ant-cascader-menu-item-active {
   font-weight: 500 !important;
}

/* Fix TreeSelect nodes */
.ant-select-tree .ant-select-tree-node-content-wrapper:hover {
   background-color: rgba(0, 0, 0, 0.04) !important; 
}

/* =====================================================================================
   DARK MODE — GLOBAL OVERRIDES
   ===================================================================================== */

/* Dark + Glass Modal */
html.dark:not(.theme-traditional-global) .ant-modal-content {
   background-color: rgba(40, 40, 40, 0.75) !important;
   border: 1px solid rgba(255, 255, 255, 0.1) !important;
   box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.3), 
               0 0 0 9999px rgba(0, 0, 0, 0.6) !important;
}

html.dark:not(.theme-traditional-global) .ant-modal-title {
   color: #ffffffd9 !important;
}

html.dark:not(.theme-traditional-global) .ant-modal .ant-form-item-label > label {
   color: #ffffffa6 !important;
}

html.dark:not(.theme-traditional-global) .ant-modal .ant-input,
html.dark:not(.theme-traditional-global) .ant-modal .ant-input-number-input {
   background-color: rgba(255, 255, 255, 0.08) !important;
   border: 1px solid rgba(255, 255, 255, 0.12) !important;
   color: #ffffffd9 !important;
}

html.dark:not(.theme-traditional-global) .ant-modal .ant-select-selector {
   background-color: rgba(255, 255, 255, 0.08) !important;
   border: 1px solid rgba(255, 255, 255, 0.12) !important;
   color: #ffffffd9 !important;
}

html.dark:not(.theme-traditional-global) .ant-modal .ant-input:focus,
html.dark:not(.theme-traditional-global) .ant-modal .ant-input-focused {
   background-color: rgba(255, 255, 255, 0.12) !important;
}

html.dark:not(.theme-traditional-global) .ant-modal-close-x {
   color: #ffffffa6 !important;
}

html.dark:not(.theme-traditional-global) .ant-modal-footer .ant-btn-default {
   background-color: rgba(255, 255, 255, 0.08) !important;
   color: #ffffffa6 !important;
   border: 1px solid rgba(255, 255, 255, 0.15) !important;
}

/* Dark + Glass Dropdowns */
html.dark:not(.theme-traditional-global) .ant-select-dropdown,
html.dark:not(.theme-traditional-global) .ant-tree-select-dropdown {
   background-color: rgba(40, 40, 40, 0.85) !important;
   border: 1px solid rgba(255, 255, 255, 0.1) !important;
}

html.dark:not(.theme-traditional-global) .ant-select-item,
html.dark:not(.theme-traditional-global) .ant-select-item-option {
   color: #ffffffd9 !important;
}

/* Dark + Traditional Modal */
html.dark.theme-traditional-global .ant-modal-content {
   background-color: #1f1f1f !important;
   border: 1px solid #303030 !important;
   box-shadow: 0 6px 16px 0 rgba(0, 0, 0, 0.32) !important;
}

html.dark.theme-traditional-global .ant-modal-title {
   color: #ffffffd9 !important;
}

html.dark.theme-traditional-global .ant-modal .ant-form-item-label > label {
   color: #ffffffa6 !important;
}

html.dark.theme-traditional-global .ant-modal .ant-input,
html.dark.theme-traditional-global .ant-modal .ant-input-number-input {
   background-color: #141414 !important;
   border: 1px solid #434343 !important;
   color: #ffffffd9 !important;
}

html.dark.theme-traditional-global .ant-modal .ant-select-selector {
   background-color: #141414 !important;
   border: 1px solid #434343 !important;
   color: #ffffffd9 !important;
}

html.dark.theme-traditional-global .ant-modal-close-x {
   color: #ffffffa6 !important;
}

html.dark.theme-traditional-global .ant-modal-footer .ant-btn-default {
   background-color: #303030 !important;
   color: #ffffffa6 !important;
   border: 1px solid #434343 !important;
}

/* Dark Traditional Dropdown */
html.dark.theme-traditional-global .ant-select-dropdown,
html.dark.theme-traditional-global .ant-tree-select-dropdown {
   background-color: #1f1f1f !important;
   border: 1px solid #303030 !important;
}

html.dark.theme-traditional-global .ant-select-item,
html.dark.theme-traditional-global .ant-select-item-option {
   color: #ffffffd9 !important;
}

/* Dark Table Overrides */
html.dark .default-yellow-mode .ant-table-thead > tr > th {
   background-color: #1f1f1f !important;
   color: #ffffffa6 !important;
   border: 1px solid #303030 !important;
}

html.dark .default-yellow-mode .ant-table-tbody > tr > td {
   background-color: #141414 !important;
   color: #ffffffd9 !important;
   border: 1px solid #303030 !important;
}

html.dark .default-yellow-mode .ant-table-tbody {
   background-color: #141414 !important;
}

html.dark .default-yellow-mode .ant-table-tbody > tr:nth-child(even) > td {
   background-color: #1a1a1a !important;
}

html.dark .default-yellow-mode .ant-table-tbody > tr:hover > td {
   background-color: #262626 !important;
}

html.dark .default-yellow-mode .ant-table-container {
   border: 1px solid #303030 !important;
}

/* Dark Glass Table */
html.dark .transparent-glass-mode .ant-table {
   background: transparent !important;
}

html.dark .transparent-glass-mode .ant-table-container {
   background: transparent !important;
   border: none !important;
   backdrop-filter: none !important;
   -webkit-backdrop-filter: none !important;
}

html.dark .transparent-glass-mode .ant-table-thead > tr > th {
   background: transparent !important;
   color: #ffffffa6 !important;
   border-bottom: 1px solid rgba(255, 255, 255, 0.05) !important; /* Make header line very subtle or transparent if they want */
   border-right: none !important;
}

/* Remove all row borders in dark glass mode if requested */
html.dark .transparent-glass-mode .ant-table-tbody > tr > td {
   background: transparent !important;
   color: #ffffffd9 !important;
   border-bottom: 1px solid transparent !important; /* FORCE TRANSPARENT */
   border-top: none !important;
   border-right: none !important;
   border-left: none !important;
}

html.dark .transparent-glass-mode .ant-table-tbody > tr:hover > td {
   background: rgba(255, 255, 255, 0.06) !important;
}

html.dark .transparent-glass-mode .ant-table-cell-fix-left,
html.dark .transparent-glass-mode .ant-table-cell-fix-right {
   background: rgba(255, 255, 255, 0.04) !important;
}

html.dark .transparent-glass-mode .ant-table-ping-right .ant-table-cell-fix-right-first::after,
html.dark .transparent-glass-mode .ant-table-ping-left .ant-table-cell-fix-left-last::after {
   box-shadow: none !important;
}

html.dark .transparent-glass-mode .ant-table-row-expand-icon {
   background: rgba(255, 255, 255, 0.06) !important;
   border-color: rgba(255, 255, 255, 0.12) !important;
   color: #ffffffd9 !important;
}

html.dark .transparent-glass-mode .ant-pagination-item,
html.dark .transparent-glass-mode .ant-pagination-prev .ant-pagination-item-link,
html.dark .transparent-glass-mode .ant-pagination-next .ant-pagination-item-link {
   background: rgba(255, 255, 255, 0.04) !important;
   border-color: rgba(255, 255, 255, 0.1) !important;
}

html.dark .transparent-glass-mode .ant-pagination-item a,
html.dark .transparent-glass-mode .ant-pagination-prev .ant-pagination-item-link,
html.dark .transparent-glass-mode .ant-pagination-next .ant-pagination-item-link {
   color: #ffffffa6 !important;
}

html.dark .transparent-glass-mode .ant-pagination-item-active {
   background: rgba(247, 181, 0, 0.18) !important;
   border-color: rgba(247, 181, 0, 0.3) !important;
}

html.dark .transparent-glass-mode .ant-pagination-item-active a {
   color: #ffd666 !important;
}

/* =====================================================================================
   TRADITIONAL MODE — Remove glass from GlassCard component
   ===================================================================================== */
html.theme-traditional-global .glass-container {
   background-color: #fff !important;
   backdrop-filter: none !important;
   -webkit-backdrop-filter: none !important;
   border: 1px solid #e8e8e8 !important;
   border-radius: 4px !important;
   box-shadow: 0 1px 2px rgba(0,0,0,0.03) !important;
}

html.theme-traditional-global .glass-panel {
   background-color: #fff !important;
   backdrop-filter: none !important;
   -webkit-backdrop-filter: none !important;
   border: 1px solid #e8e8e8 !important;
   border-radius: 4px !important;
   box-shadow: 0 1px 2px rgba(0,0,0,0.03) !important;
}

/* Dark Traditional GlassCard/Panel */
html.dark.theme-traditional-global .glass-container,
html.dark.theme-traditional-global .glass-panel {
   background-color: #1f1f1f !important;
   border: 1px solid #303030 !important;
}

/* Dark Glass Mode GlassCard/Panel */
html.dark:not(.theme-traditional-global) .glass-container {
   background-color: rgba(40, 40, 40, 0.7) !important;
   border: 1px solid rgba(255,255,255,0.08) !important;
}

html.dark:not(.theme-traditional-global) .glass-panel {
   background-color: rgba(40, 40, 40, 0.7) !important;
   border: 1px solid rgba(255,255,255,0.08) !important;
}

/* =====================================================================================
   NO BORDER RADIUS — Global override
   ===================================================================================== */
html.theme-no-radius-global .glass-container,
html.theme-no-radius-global .glass-panel,
html.theme-no-radius-global .ant-modal-content,
html.theme-no-radius-global .ant-table-container,
html.theme-no-radius-global .ant-btn,
html.theme-no-radius-global .ant-input,
html.theme-no-radius-global .ant-select-selector,
html.theme-no-radius-global .ant-tag,
html.theme-no-radius-global .ant-select-dropdown,
html.theme-no-radius-global .ant-card {
    border-radius: 2px !important;
}

/* =====================================================================================
   SCROLLBAR STYLES
   ===================================================================================== */
::-webkit-scrollbar {
    width: 6px;
    height: 6px;
}
::-webkit-scrollbar-track {
    background: transparent;
}
::-webkit-scrollbar-thumb {
    background: transparent;
    border-radius: 3px;
    transition: background-color 0.3s;
}
::-webkit-scrollbar:hover ::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.2);
}
::-webkit-scrollbar-thumb:hover {
    background: rgba(0, 0, 0, 0.4) !important;
}

/* Firefox */
* {
    scrollbar-width: thin;
    scrollbar-color: transparent transparent;
}

/* Dark Scrollbar */
html.dark ::-webkit-scrollbar-thumb:hover {
    background: rgba(255, 255, 255, 0.3) !important;
}

/* =====================================================================================
   DARK MODE — General text overrides for Dashboard and other pages
   ===================================================================================== */
html.dark .stat-label,
html.dark .nav-item div:last-child {
    color: #ffffffa6 !important;
}

html.dark .stat-number {
    color: #ffffffd9 !important;
}

html.dark .stat-trend {
    color: #ffffff73 !important;
}

html.dark h1,
html.dark h2,
html.dark h3 {
    color: #ffffffd9 !important;
}

/* Fix Global White Edges in layout */
html.dark, html.dark body, html.dark #app, html.dark .ant-layout {
    background-color: #000 !important;
}

html.dark p {
    color: #ffffffa6 !important;
}

/* Pagination Adjustments */
.ant-pagination-item,
.ant-pagination-prev,
.ant-pagination-next,
.ant-pagination-jump-prev,
.ant-pagination-jump-next {
    margin-right: 8px !important; /* Increase spacing */
}

/* Dark Mode Pagination Input (Quick Jumper) */
html.dark .ant-pagination-options-quick-jumper input {
    background-color: transparent !important;
    border: 1px solid rgba(255, 255, 255, 0.2) !important;
    color: #ffffffd9 !important;
}

html.dark .ant-pagination-item-active {
    background: rgba(255, 255, 255, 0.1) !important;
    border-color: transparent !important;
}

html.dark .ant-pagination-item-active a {
    color: #f7b500 !important;
}

/* =====================================================================================
   GLASS MODE — Drawer: glass background (same aesthetic as glass modal)
   ===================================================================================== */
html:not(.theme-traditional-global):not(.dark) .ant-drawer-content {
    background-color: rgba(255, 255, 255, 0.55) !important;
    backdrop-filter: blur(16px) !important;
    -webkit-backdrop-filter: blur(16px) !important;
    border-left: 1px solid rgba(0, 0, 0, 0.06) !important;
    box-shadow: none !important;
}

html:not(.theme-traditional-global):not(.dark) .ant-drawer-header {
    background: transparent !important;
    border-bottom: 1px solid rgba(255, 255, 255, 0.3) !important;
}

html:not(.theme-traditional-global):not(.dark) .ant-drawer-body {
    background: transparent !important;
}

/* =====================================================================================
   TRADITIONAL MODE — Drawer: solid white, no glass
   ===================================================================================== */
html.theme-traditional-global:not(.dark) .ant-drawer-content {
    background-color: #fff !important;
    backdrop-filter: none !important;
    -webkit-backdrop-filter: none !important;
}

html.theme-traditional-global:not(.dark) .ant-drawer-header {
    background: #fff !important;
    border-bottom: 1px solid #f0f0f0 !important;
}

/* Ant Design drawer dark support */
/* Dark Glass Mode Drawer */
html.dark:not(.theme-traditional-global) .ant-drawer-content {
    background-color: rgba(30, 30, 30, 0.85) !important;
    backdrop-filter: blur(20px) !important;
    -webkit-backdrop-filter: blur(20px) !important;
    border-left: 1px solid rgba(255, 255, 255, 0.06) !important;
    box-shadow: none !important;
}

html.dark:not(.theme-traditional-global) .ant-drawer-header {
    background: transparent !important;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08) !important;
}

html.dark:not(.theme-traditional-global) .ant-drawer-body {
    background: transparent !important;
}

/* Fallback for Traditional Dark Mode (Solid) */
html.dark.theme-traditional-global .ant-drawer-content {
    background: #1f1f1f !important;
    backdrop-filter: none !important;
    -webkit-backdrop-filter: none !important;
}

/* =====================================================================================
   GLOBAL TABLE & INPUT REFINEMENTS (Based on User Feedback)
   ===================================================================================== */

/* 1. Remove Vertical Lines in Table Headers */
.ant-table-thead > tr > th::before {
    display: none !important; 
    background-color: transparent !important;
}

/* 2. Remove Vertical Borders in Table Cells (Dark Mode) */
html.dark .ant-table-cell {
    border-right: none !important;
}

/* 3. Input Focus State - Prevent White Background in Dark Mode */
html.dark .ant-input:focus,
html.dark .ant-input-focused,
html.dark .ant-input-affix-wrapper-focused,
html.dark .ant-input-affix-wrapper:focus,
html.dark .ant-input-affix-wrapper:focus-within {
    background-color: rgba(255, 255, 255, 0.08) !important;
    border-color: var(--ant-primary-color) !important;
    box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2) !important;
}

/* Prevent input from turning white on blur in dark mode */
html.dark .ant-input-affix-wrapper {
    transition: background-color 0.3s, border-color 0.3s !important;
}

html.dark .ant-input-affix-wrapper:not(:focus-within):not(.ant-input-affix-wrapper-focused) {
    background-color: rgba(255, 255, 255, 0.05) !important;
    border-color: rgba(255, 255, 255, 0.1) !important;
    box-shadow: none !important;
}

/* 4. Glass Effect for Inputs (Global Dark Glass Mode) */
html.dark:not(.theme-traditional-global) .ant-input,
html.dark:not(.theme-traditional-global) .ant-input-affix-wrapper {
    background-color: rgba(255, 255, 255, 0.05) !important;
    border: 1px solid rgba(255, 255, 255, 0.1) !important;
    color: #fff !important;
    backdrop-filter: blur(4px);
}

/* Fix Dictionary Search Input in Dark Mode specific override */
html.dark .ant-input-affix-wrapper.variant-glass {
    background: rgba(255, 255, 255, 0.05) !important;
    border: 1px solid rgba(255, 255, 255, 0.1) !important;
    color: #fff !important;
    box-shadow: none !important;
}
html.dark .ant-input-affix-wrapper.variant-glass input.ant-input {
    background: transparent !important;
    color: #fff !important;
}

/* 5. Tag Styles - Glassy */
html.dark .ant-tag {
    border: 1px solid rgba(255, 255, 255, 0.1);
    background: rgba(255, 255, 255, 0.05); 
}

/* 6. Fix "Black Selection Box" in general interactions (Checkbox/Radio) */
html.dark .ant-checkbox-inner,
html.dark .ant-radio-inner {
    background-color: transparent;
    border-color: rgba(255,255,255,0.3);
}
html.dark .ant-checkbox-checked .ant-checkbox-inner,
html.dark .ant-radio-checked .ant-radio-inner {
    background-color: #1890ff; /* Theme color ideally */
    border-color: #1890ff;
}

html.dark.theme-traditional-global .ant-drawer-header {
    background: #1f1f1f !important;
    border-bottom: 1px solid #303030 !important;
}

html.dark .ant-drawer-title {
    color: #ffffffd9 !important;
}

html.dark .ant-drawer-close {
    color: #ffffffa6 !important;
}

html.dark .ant-divider {
    border-color: #303030 !important;
}

html.dark .section-title {
    color: #ffffffd9 !important;
}

html.dark .section-desc {
    color: #ffffff73 !important;
}

html.dark .style-card {
    border-color: rgba(255,255,255,0.1) !important;
}

html.dark .style-card:hover {
    border-color: rgba(255,255,255,0.2) !important;
}

html.dark .style-label {
    color: #ffffffa6 !important;
}

html.dark .dark-mode-card {
    border-color: rgba(255,255,255,0.1) !important;
}

html.dark .dark-mode-card:hover {
    border-color: rgba(255,255,255,0.2) !important;
}

html.dark .dark-mode-card span {
    color: #ffffffa6 !important;
}

html.dark .custom-color-picker {
    background: #303030 !important;
}

html.dark .color-hex-label {
    color: #ffffffa6 !important;
}

html.dark .style-note {
    background: #2a2000 !important;
    border-color: #594800 !important;
    color: #d4a800 !important;
}

/* Dark mode pagination */
html.dark .ant-pagination {
    gap: 4px;
}

html.dark .ant-pagination-item,
html.dark .ant-pagination-item a {
    color: #ffffffa6 !important;
    border-color: #434343 !important;
    background: transparent !important;
    margin: 0 2px !important;
}

html.dark .ant-pagination-item:hover {
    border-color: #666 !important;
}

html.dark .ant-pagination-item-active {
    background: rgba(255, 255, 255, 0.1) !important;
    border-color: var(--ant-primary-color) !important;
}

html.dark .ant-pagination-item-active a {
    color: #ffffffd9 !important;
}

/* Dark mode pagination: prev/next buttons */
html.dark .ant-pagination-prev .ant-pagination-item-link,
html.dark .ant-pagination-next .ant-pagination-item-link {
    background: transparent !important;
    border-color: #434343 !important;
    color: #ffffffa6 !important;
    margin: 0 2px !important;
}

html.dark .ant-pagination-prev:hover .ant-pagination-item-link,
html.dark .ant-pagination-next:hover .ant-pagination-item-link {
    border-color: #666 !important;
    color: #ffffffd9 !important;
}

html.dark .ant-pagination-disabled .ant-pagination-item-link {
    background: transparent !important;
    border-color: #303030 !important;
    color: #ffffff45 !important;
}

/* Dark mode pagination: total text */
html.dark .ant-pagination-total-text {
    color: #ffffffa6 !important;
}

/* Dark mode pagination: jumper */
html.dark .ant-pagination-options .ant-select-selector {
    background: transparent !important;
    border-color: #434343 !important;
    color: #ffffffa6 !important;
}

html.dark .ant-pagination-options-quick-jumper {
    color: #ffffffa6 !important;
}

html.dark .ant-pagination-options-quick-jumper input {
    background: transparent !important;
    border-color: #434343 !important;
    color: #ffffffd9 !important;
}

/* Dark mode tags — only override UNCOLORED tags; colored tags keep their original colors */
html.dark .ant-tag:not(.ant-tag-green):not(.ant-tag-blue):not(.ant-tag-red):not(.ant-tag-orange):not(.ant-tag-cyan):not(.ant-tag-magenta):not(.ant-tag-purple):not(.ant-tag-volcano):not(.ant-tag-lime):not(.ant-tag-gold):not(.ant-tag-geekblue):not([class*="ant-tag-"]) {
    background: rgba(255,255,255,0.08) !important;
    border-color: #434343 !important;
    color: #ffffffa6 !important;
}

/* Ensure colored tags have proper text contrast in dark mode */
html.dark .ant-tag-green,
html.dark .ant-tag-blue,
html.dark .ant-tag-red,
html.dark .ant-tag-orange,
html.dark .ant-tag-cyan,
html.dark .ant-tag-magenta,
html.dark .ant-tag-purple,
html.dark .ant-tag-volcano,
html.dark .ant-tag-gold,
html.dark .ant-tag-geekblue {
    opacity: 0.9;
}

/* Dashboard-specific dark fixes */
html.dark .dashboard-container {
    background-color: #141414 !important;
}

html.dark .announcement-item {
    border-bottom-color: #303030 !important;
}

html.dark .nav-icon-circle {
    background: #303030 !important;
    border-color: #434343 !important;
    color: #ffffffa6 !important;
}

html.dark .nav-item:hover .nav-icon-circle {
    background: #3a3a3a !important;
}

html.dark .weather-widget div {
    color: #ffffffa6 !important;
}

/* =====================================================================================
   DARK MODE — COMPREHENSIVE TEXT & COMPONENT OVERRIDES
   ===================================================================================== */

/* --- Dark: General text & labels --- */
html.dark .ant-form-item-label > label {
    color: #ffffffa6 !important;
}
html.dark .ant-form-item-extra {
    color: #ffffff73 !important;
}
html.dark .ant-empty-description {
    color: #ffffff73 !important;
}
html.dark .ant-empty-image svg {
    fill: #ffffff30 !important;
}
/* Dark span/div text colors are handled selectively below */

/* --- Dark: Buttons (StandardButton) --- */
html.dark .search-button {
    background-color: #f7b500 !important;
    color: #000 !important;
}
html.dark .reset-button {
    background-color: #303030 !important;
    color: #ffffffd9 !important;
}
html.dark .add-button,
html.dark .ant-btn-primary {
    background-color: #f7b500 !important; /* Force Theme Yellow or use var(--ant-primary-color) */
    border-color: #f7b500 !important;
    color: #fff !important;
}
html.dark .delete-button {
    background-color: #ff4d4f !important;
    color: #fff !important;
}

/* --- Dark: Ant Button defaults --- */
html.dark .ant-btn-default {
    background-color: #303030 !important;
    color: #ffffffd9 !important;
    border-color: #434343 !important;
}
html.dark .ant-btn-default:hover {
    background-color: #3a3a3a !important;
    border-color: #555 !important;
}
html.dark .ant-btn-text {
    color: #ffffffa6 !important;
}
html.dark .ant-btn-text:hover {
    color: #ffffffd9 !important;
    background: rgba(255,255,255,0.08) !important;
}
html.dark .ant-btn-link {
    color: #ffffffa6 !important;
}

/* --- Dark: StandardInput --- */
html.dark .std-input.variant-default {
    background-color: #1f1f1f !important;
    border-color: #434343 !important;
    color: #ffffffd9 !important;
}
html.dark .std-input.variant-grey {
    background-color: #262626 !important;
    border-color: transparent !important;
    color: #ffffffd9 !important;
}
html.dark .std-input.variant-grey:hover {
    background-color: #303030 !important;
}
html.dark .std-input.variant-glass {
    background-color: rgba(255,255,255,0.06) !important;
    border-color: rgba(255,255,255,0.1) !important;
    color: #ffffffd9 !important;
}
html.dark .std-input.variant-glass:hover {
    background-color: rgba(255,255,255,0.1) !important;
}
html.dark .ant-input-prefix {
    color: #ffffff73 !important;
}
html.dark .ant-input::placeholder {
    color: #ffffff45 !important;
}
html.dark .ant-input-affix-wrapper {
    background-color: rgba(255, 255, 255, 0.05) !important;
    border-color: rgba(255, 255, 255, 0.1) !important;
}
html.dark .ant-input-affix-wrapper > input.ant-input {
    color: #ffffffd9 !important;
}

/* --- Dark: General Ant Input/Select outside modals --- */
html.dark .ant-input {
    background-color: #1f1f1f !important;
    border-color: #434343 !important;
    color: #ffffffd9 !important;
}
html.dark .ant-select:not(.ant-select-customize-input) .ant-select-selector {
    background-color: #1f1f1f !important;
    border-color: #434343 !important;
    color: #ffffffd9 !important;
}
html.dark .ant-select-arrow {
    color: #ffffffa6 !important;
}
html.dark .ant-input-number {
    background-color: #1f1f1f !important;
    border-color: #434343 !important;
    color: #ffffffd9 !important;
}
html.dark .ant-input-number-input {
    color: #ffffffd9 !important;
}

/* --- Dark: Table generic (for all modes) --- */
html.dark .ant-table {
    background: transparent !important;
    color: #ffffffd9 !important;
}
html.dark .ant-table-thead > tr > th {
    color: #ffffffa6 !important;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06) !important;
}
html.dark .ant-table-tbody > tr > td {
    color: #ffffffd9 !important;
    border-bottom: none !important;
}
html.dark .ant-table-cell-fix-right,
html.dark .ant-table-cell-fix-left {
    background: inherit !important;
}
html.dark .ant-table-placeholder {
    background: transparent !important;
}
html.dark .ant-table-tbody > tr.ant-table-placeholder:hover > td {
    background: transparent !important;
}

/* --- Dark: Tree expand/collapse arrow icons --- */
html.dark .ant-table-row-expand-icon {
    background: #303030 !important;
    border-color: #434343 !important;
    color: #ffffffd9 !important;
}
html.dark .ant-table-row-expand-icon::before,
html.dark .ant-table-row-expand-icon::after {
    background: #ffffffd9 !important;
}

/* --- Dark: Popconfirm --- */
html.dark .ant-popover-inner {
    background: #1f1f1f !important;
    box-shadow: 0 4px 12px rgba(0,0,0,0.4) !important;
}
html.dark .ant-popconfirm-message-title {
    color: #ffffffd9 !important;
}
html.dark .ant-popover-arrow::before {
    background: #1f1f1f !important;
}
html.dark .ant-popconfirm-message-icon .anticon {
    color: #faad14 !important;
}

/* --- Dark: Tooltip --- */
html.dark .ant-tooltip-inner {
    background: #303030 !important;
    color: #ffffffd9 !important;
}
html.dark .ant-tooltip-arrow::before {
    background: #303030 !important;
}

/* --- Dark: Checkbox & Radio --- */
html.dark .ant-checkbox-inner {
    background-color: #1f1f1f !important;
    border-color: #434343 !important;
}
html.dark .ant-checkbox + span {
    color: #ffffffd9 !important;
}
html.dark .ant-radio-inner {
    background-color: #1f1f1f !important;
    border-color: #434343 !important;
}
html.dark .ant-radio-wrapper {
    color: #ffffffd9 !important;
}
html.dark .ant-radio-button-wrapper {
    background: #1f1f1f !important;
    border-color: #434343 !important;
    color: #ffffffd9 !important;
}
html.dark .ant-radio-button-wrapper:hover {
    color: #ffffffd9 !important;
}
html.dark .ant-select-selection-placeholder {
    color: #ffffff45 !important;
}

/* --- Dark: Tabs --- */
html.dark .ant-tabs-tab {
    color: #ffffffa6 !important;
}
html.dark .ant-tabs-tab-active .ant-tabs-tab-btn {
    color: #ffffffd9 !important;
}
html.dark .ant-tabs-ink-bar {
    background: #ffffffd9 !important;
}

/* --- Dark: Menu expand/collapse icons in sidebar --- */
html.dark .ant-menu-submenu-arrow::before,
html.dark .ant-menu-submenu-arrow::after {
    background: rgba(255,255,255,0.45) !important;
}

/* --- Dark: DictList left pane specific --- */
html.dark .dict-list-page .title {
    color: #ffffffd9 !important;
}
html.dark .dict-list-page .type-item {
    color: #ffffffd9 !important;
}
html.dark .dict-list-page .type-item:hover {
    background-color: #262626 !important;
}
html.dark .dict-list-page .type-item.active {
    background-color: #2a2500 !important;
    color: #f7b500 !important;
}
html.dark .dict-list-page .type-item.light-yellow-bg {
    background-color: #2a2500 !important;
}
html.dark .dict-list-page .empty-text {
    color: #ffffff73 !important;
}
html.dark .dict-list-page .pagination-footer {
    border-top-color: rgba(255,255,255,0.06) !important;
}
/* Dict left pane "+" add button: keep theme color in dark mode */
html.dark .dict-list-page .header-actions .ant-btn-link {
    color: #f7b500 !important;
}
html.dark .dict-list-page .header-actions .ant-btn-link:hover {
    color: #ffc929 !important;
}

/* --- Dark: GlassCard text colors --- */
html.dark .glass-container .pane-header .title {
    color: #ffffffd9 !important;
}
html.dark .glass-container {
    color: #ffffffd9 !important;
}

/* --- Dark: Delete button icon --- */
html.dark .trash-btn {
    color: #ffffffa6 !important;
}
html.dark .trash-btn:hover {
    color: #ffffffd9 !important;
}
html.dark .cancel-delete-btn {
    color: #ffffff73 !important;
}

/* --- Dark: Search label text --- */
html.dark .ant-form-item-label > label,
html.dark .ant-form label {
    color: #ffffffa6 !important;
}

/* --- Dark: Form extra text and section labels --- */
html.dark div[style*="color:#867E7E"],
html.dark div[style*="color: #867E7E"] {
    color: #ffffffa6 !important;
}

/* --- Dark: Breadcrumb & other misc --- */
html.dark .ant-breadcrumb-link,
html.dark .ant-breadcrumb-separator {
    color: #ffffffa6 !important;
}

/* --- Dark: message & notification adapt --- */
html.dark .ant-message-custom-content span {
    color: #ffffffd9 !important;
}

/* --- Dark: Menu Form Item labels --- */
html.dark .ant-form-item .ant-form-item-label > label {
    color: #ffffffa6 !important;
}

html.dark .ant-form-item-extra {
    color: #ffffff73 !important;
}

/* =====================================================================================
   TRADITIONAL MODE — Compact Spacing (Snowy-like)
   ===================================================================================== */

/* Tighter card margins in traditional mode */
html.theme-traditional-global .glass-container {
    margin-top: 12px !important;
    border-radius: 4px !important;
    padding: 16px !important;
    box-shadow: 0 1px 2px rgba(0,0,0,0.06) !important;
}
/* Search card variant: even tighter */
html.theme-traditional-global .glass-container[style*="paddingTop"] {
    padding-top: 12px !important;
}

/* Dark traditional GlassCard */
html.dark.theme-traditional-global .glass-container {
    background-color: #1f1f1f !important;
    border-color: #303030 !important;
}

/* =====================================================================================
   DARK MODE — Drawer top offset (shouldn't overlap header)
   ===================================================================================== */
/* Drawer top offset is handled via component props */

/* =====================================================================================
   LOGIN PAGE — Theme color dynamic support
   ===================================================================================== */
/* These are handled in LoginForm.vue via Vuex store */

</style>
