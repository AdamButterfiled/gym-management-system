<template>
  <div class="dashboard-container" style="padding: 0px; background-color: transparent; margin-top:-4px;">
    <!-- Welcome Section -->
    <div class="glass-panel" style="margin-bottom: 24px; padding: 32px;">
      <div style="display: flex; align-items: center; justify-content: space-between;">
        <div style="display: flex; align-items: center;">
             <a-avatar :size="72" :src="userAvatarImg" style="border: 2px solid #fff; box-shadow: 0 4px 10px rgba(0,0,0,0.1);" />
             <div style="margin-left: 24px;">
                <h1 style="font-size: 28px; font-weight: 300; margin-bottom: 8px; color: #2c3e50;">欢迎回来，管理员</h1>
                <p style="margin: 0; color: #7f8c8d; font-size: 16px; font-weight: 300;">今日场馆运营状况良好，一切正常。</p>
             </div>
        </div>
        <div class="weather-widget" style="text-align: right;">
            <div style="font-size: 28px; font-weight: 300; color: #2c3e50;">26°C</div>
            <div style="color: #95a5a6;">晴朗舒适</div>
        </div>
      </div>
    </div>

    <!-- Stats Cards -->
    <a-row :gutter="24">
      <a-col :span="6">
        <div class="glass-panel stat-card">
          <div class="stat-content">
            <div class="stat-label">今日预约</div>
            <div class="stat-number">42</div>
            <div class="stat-trend increasing">
               <arrow-up-outlined /> +12%
            </div>
          </div>
          <div class="stat-icon-subtle">
            <schedule-outlined />
          </div>
        </div>
      </a-col>
      <a-col :span="6">
         <div class="glass-panel stat-card">
          <div class="stat-content">
            <div class="stat-label">当前在馆</div>
            <div class="stat-number">15</div>
            <div class="stat-trend stable">
               <minus-outlined /> 平稳
            </div>
          </div>
          <div class="stat-icon-subtle">
            <user-outlined />
          </div>
        </div>
      </a-col>
      <a-col :span="6">
         <div class="glass-panel stat-card">
          <div class="stat-content">
            <div class="stat-label">空闲器材</div>
            <div class="stat-number">88</div>
             <div class="stat-trend">
               使用率: 25%
            </div>
          </div>
          <div class="stat-icon-subtle">
            <thunderbolt-outlined />
          </div>
        </div>
      </a-col>
      <a-col :span="6">
         <div class="glass-panel stat-card">
          <div class="stat-content">
            <div class="stat-label">待处理报修</div>
            <div class="stat-number attention">3</div>
            <div class="stat-trend decreasing">
               需要处理
            </div>
          </div>
          <div class="stat-icon-subtle">
            <tool-outlined />
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- Quick Nav -->
    <div class="glass-panel" style="margin-top: 24px; margin-bottom: 24px;">
      <h3 style="margin-bottom: 20px; color: #555; font-weight: 500;">快捷导航</h3>
      <div style="display: flex; justify-content: space-around; text-align: center;">
             <div class="nav-item" @click="$router.push('/venue')">
                <div class="nav-icon-circle">
                    <EnvironmentOutlined />
                </div>
                <div>场馆管理</div>
             </div>
             <div class="nav-item" @click="$router.push('/equipment')">
                <div class="nav-icon-circle">
                    <ToolOutlined />
                </div>
                 <div>器材管理</div>
             </div>
             <div class="nav-item" @click="$router.push('/course')">
                <div class="nav-icon-circle">
                     <TeamOutlined />
                </div>
                 <div>团课排期</div>
             </div>
             <div class="nav-item" @click="$router.push('/gym-monitor')">
                <div class="nav-icon-circle">
                    <EyeOutlined />
                </div>
                 <div>实时监控</div>
             </div>
      </div>
    </div>

    <!-- New Bottom Section: Charts & Tables -->
    <a-row :gutter="24">
        <a-col :span="16">
            <div class="glass-panel" style="height: 400px; padding: 24px; display: flex; flex-direction: column;">
                <h3 style="margin: 0 0 40px 0; color: #555; font-weight: 500; font-size: 16px;">周预约/流量趋势 (Weekly Trend)</h3>
                <div style="flex: 1; display: flex; align-items: flex-end; justify-content: space-around; padding: 0 40px 10px 40px;">
                    <!-- CSS Bar Chart Simulation -->
                    <div v-for="i in 7" :key="i" class="chart-bar-track">
                         <div :style="{height: Math.random() * 60 + 20 + '%', background: '#f7b500', borderRadius: '8px', position: 'absolute', bottom: 0, width: '100%'}"></div>
                         <div class="chart-bar-label">{{i}}日</div>
                    </div>
                </div>
            </div>
        </a-col>
        <a-col :span="8">
            <div class="glass-panel" style="height: 400px; padding: 24px; display: flex; flex-direction: column;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                    <h3 style="margin: 0; color: #555; font-weight: 500; font-size: 16px;">系统公告 & 待办</h3>
                    <a style="font-size: 12px; color: #f7b500;" @click="$router.push('/notifications')">查看更多 <right-outlined /></a>
                </div>
                <div style="flex: 1; overflow: hidden;">
                    <a-list item-layout="horizontal" :data-source="announcements.slice(0, 4)">
                        <template #renderItem="{ item }">
                        <a-list-item 
                            class="announcement-item" 
                            style="padding: 12px 10px; border-radius: 8px; transition: background 0.3s; cursor: pointer; margin-bottom: 8px; border-bottom: 1px solid #f0f0f0;"
                            @click="$router.push('/notifications')"
                        >
                            <a-list-item-meta>
                                <template #description>
                                    <span style="font-size: 12px; color: #aaa;">{{ item.date }}</span>
                                </template>
                                <template #title>
                                    <div style="color: #333; font-weight: 400; font-size: 14px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                                        {{ item.title }}
                                    </div>
                                </template>
                            </a-list-item-meta>
                        </a-list-item>
                        </template>
                    </a-list>
                </div>
            </div>
        </a-col>
    </a-row>

  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { 
    UserOutlined, ScheduleOutlined, ThunderboltOutlined, ToolOutlined, 
    EnvironmentOutlined, TeamOutlined, EyeOutlined,
    ArrowUpOutlined, MinusOutlined, RightOutlined
} from '@ant-design/icons-vue';
import userAvatarImg from "../assets/user_avatar.jpg";

const announcements = ref([
  { title: '春节期间营业时间通知', date: '02-01' },
  { title: '新增瑜伽课程体验活动', date: '01-28' },
  { title: '器材维护公告', date: '01-25' },
  { title: '会员系统升级通知', date: '01-20' },
  { title: '失物招领: 黑色运动鞋', date: '01-15' },
]);
</script>

<style scoped>
/* Minimalist Glass Theme - White-Grey Glass (Sidebar Style) */
/* Minimalist Glass Theme - White-Grey Glass (Exact Match to VenueList) */
.glass-panel {
     background-color: #FDFDFD;
     backdrop-filter: blur(28px);
     -webkit-backdrop-filter: blur(8px);
     border: 0.1px solid #d9d9d9;
     border-radius: 19px;
     padding: 24px;
     box-shadow: 0px 0.5px 2.0px rgba(0, 0, 0, 0.1);
     transition: all 0.3s ease;
}

.glass-panel:hover {
    background: rgba(255, 255, 255, 0.95);
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.06);
}

.stat-card {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    position: relative;
    overflow: hidden;
    height: 140px;
}

.stat-content {
    z-index: 1;
}

.stat-label {
    font-size: 11px;
    letter-spacing: 1px;
    color: #95a5a6;
    margin-bottom: 8px;
    font-weight: 600;
    text-transform: uppercase;
}

.stat-number {
    font-size: 36px;
    font-weight: 300;
    color: #2c3e50;
    line-height: 1.2;
}

.stat-number.attention {
    color: #e74c3c;
}

.stat-trend {
    font-size: 12px;
    margin-top: 8px;
    color: #bdc3c7;
}

.stat-trend.increasing { color: #27ae60; }
.stat-trend.decreasing { color: #e74c3c; }

.stat-icon-subtle {
    font-size: 48px;
    color: rgba(0, 0, 0, 0.03);
    position: absolute;
    right: 20px;
    bottom: 20px;
    transform: rotate(-15deg);
}

/* Nav Items */
.nav-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    cursor: pointer;
    transition: transform 0.2s;
}

.nav-item:hover {
    transform: scale(1.05);
}

.nav-icon-circle {
    width: 60px;
    height: 60px;
    border-radius: 50%;
    background: #fff;
    border: 1px solid #eee;
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 24px;
    color: #555;
    margin-bottom: 12px;
    box-shadow: 0 4px 10px rgba(0,0,0,0.02);
    transition: all 0.3s;
}

.nav-item:hover .nav-icon-circle {
    box-shadow: 0 8px 15px rgba(0,0,0,0.05);
    background: #fafafa;
    color: #f7b500;
    border-color: #f7b500;
}

.nav-item div:last-child {
    font-size: 13px;
    color: #7f8c8d;
}

/* ============================================================
   DARK MODE OVERRIDES
   ============================================================ */

/* Dark: container bg */
html.dark .dashboard-container {
    background-color: #141414 !important;
}

/* Dark: glass panels */
html.dark .glass-panel {
    background-color: #1f1f1f !important;
    border-color: #303030 !important;
    box-shadow: 0 1px 4px rgba(0,0,0,0.3) !important;
}

html.dark .glass-panel:hover {
    background: #252525 !important;
    box-shadow: 0 4px 16px rgba(0,0,0,0.4) !important;
}

/* Dark: stat text */
html.dark .stat-label {
    color: rgba(255,255,255,0.45) !important;
}

html.dark .stat-number {
    color: #ffffffd9 !important;
}

html.dark .stat-number.attention {
    color: #ff4d4f !important;
}

html.dark .stat-trend {
    color: rgba(255,255,255,0.35) !important;
}

html.dark .stat-trend.increasing {
    color: #52c41a !important;
}

html.dark .stat-trend.decreasing {
    color: #ff4d4f !important;
}

/* Dark: stat icon (make visible) */
html.dark .stat-icon-subtle {
    color: rgba(255,255,255,0.06) !important;
}

/* Dark: nav icons */
html.dark .nav-icon-circle {
    background: #2a2a2a !important;
    border-color: #3a3a3a !important;
    color: rgba(255,255,255,0.65) !important;
}

html.dark .nav-item:hover .nav-icon-circle {
    background: #303030 !important;
    border-color: #f7b500 !important;
    color: #f7b500 !important;
}

html.dark .nav-item div:last-child {
    color: rgba(255,255,255,0.45) !important;
}

/* Dark: headings */
html.dark .dashboard-container h1 {
    color: #ffffffd9 !important;
}

html.dark .dashboard-container h3 {
    color: rgba(255,255,255,0.65) !important;
}

html.dark .dashboard-container p {
    color: rgba(255,255,255,0.45) !important;
}

/* Dark: welcome text */
html.dark .weather-widget div {
    color: rgba(255,255,255,0.45) !important;
}

/* Dark: chart bar track */
html.dark .chart-bar-track {
    background: #2a2a2a !important;
}

/* Dark: chart date labels */
html.dark .chart-bar-label {
    color: rgba(255,255,255,0.35) !important;
}

/* Dark: announcement items */
html.dark .announcement-item {
    border-bottom-color: #303030 !important;
}

html.dark .dashboard-container .ant-list-item {
    border-bottom-color: #303030 !important;
}

html.dark .dashboard-container .ant-list-item-meta-title div {
    color: #ffffffd9 !important;
}

html.dark .dashboard-container .ant-list-item-meta-description span {
    color: rgba(255,255,255,0.35) !important;
}

/* Dark: view more link */
html.dark .dashboard-container a[style*="color: #f7b500"] {
    color: #f7b500 !important;
}

/* ============================================================
   CHART BAR STYLES
   ============================================================ */
.chart-bar-track {
    width: 8%;
    background: #f9f9f9;
    border-radius: 8px;
    position: relative;
    height: 100%;
}

.chart-bar-label {
    position: absolute;
    bottom: -23px;
    width: 100%;
    text-align: center;
    color: #999;
    font-size: 12px;
}
</style>
