import { createRouter, createWebHistory } from 'vue-router';

const coreAppRoutes = [
  {
    path: 'dashboard',
    name: 'DashboardPage',
    component: () => import('@/views/DashboardPage.vue'),
    meta: { title: '工作台', style: 'glass' }
  },
  {
    path: 'venue',
    name: 'VenueList',
    component: () => import('@/views/gym/VenueList.vue'),
    meta: { title: '场馆资源管理', style: 'default' }
  },
  {
    path: 'course',
    name: 'CourseList',
    component: () => import('@/views/gym/CourseList.vue'),
    meta: { title: '团课排期管理', style: 'default' }
  },
  {
    path: 'private-schedule',
    name: 'PrivateScheduleList',
    component: () => import('@/views/admin/PrivateScheduleList.vue'),
    meta: { title: '私教排班管理', style: 'default' }
  },
  {
    path: 'reservation',
    name: 'ReservationList',
    component: () => import('@/views/gym/ReservationList.vue'),
    meta: { title: '预约订单管理', style: 'default' }
  },
  {
    path: 'member-assets',
    name: 'MemberAssetList',
    component: () => import('@/views/admin/MemberAssetList.vue'),
    meta: { title: '会员资产中心', style: 'default' }
  },
  {
    path: 'payment-orders',
    name: 'PaymentOrderList',
    component: () => import('@/views/admin/PaymentOrderList.vue'),
    meta: { title: '支付订单管理', style: 'default' }
  },
  {
    path: 'checkin-records',
    name: 'CheckinList',
    component: () => import('@/views/admin/CheckinList.vue'),
    meta: { title: '签到核销记录', style: 'default' }
  },
  {
    path: 'coach',
    name: 'CoachList',
    component: () => import('@/views/gym/CoachList.vue'),
    meta: { title: '教练信息管理', style: 'default' }
  },
  {
    path: 'equipment',
    name: 'EquipmentList',
    component: () => import('@/views/gym/EquipmentList.vue'),
    meta: { title: '器材管理', style: 'default' }
  },
  {
    path: 'repair',
    name: 'RepairList',
    component: () => import('@/views/gym/RepairList.vue'),
    meta: { title: '报修工单管理', style: 'default' }
  },
  {
    path: 'schedule-conflicts',
    name: 'ConflictList',
    component: () => import('@/views/admin/ConflictList.vue'),
    meta: { title: '排期冲突日志', style: 'default' }
  },
  {
    path: 'analytics',
    name: 'AnalyticsPage',
    component: () => import('@/views/admin/AnalyticsPage.vue'),
    meta: { title: '经营数据分析', style: 'glass' }
  },
  {
    path: 'user',
    name: 'UserList',
    component: () => import('@/views/gym/UserList.vue'),
    meta: { title: '用户信息管理', style: 'default' }
  },
  {
    path: 'sys/menu',
    name: 'MenuList',
    component: () => import('@/views/sys/MenuList.vue'),
    meta: { title: '菜单权限管理', style: 'glass' }
  },
  {
    path: 'sys/dict',
    name: 'DictList',
    component: () => import('@/views/sys/DictList.vue'),
    meta: { title: '数据字典管理', style: 'glass' }
  },
  {
    path: 'sys/form-config',
    name: 'FormConfigList',
    component: () => import('@/views/sys/FormConfigList.vue'),
    meta: { title: '表单管理', style: 'glass' }
  },
  {
    path: 'sys/permissions',
    name: 'PermissionList',
    component: () => import('@/views/sys/PermissionList.vue'),
    meta: { title: '角色权限管理', style: 'glass' }
  }
];

const routes = [
  {
    path: '/login',
    name: 'LoginPage',
    component: () => import('@/views/LoginForm.vue'),
  },
  {
    path: '/',
    redirect: '/dashboard', // 默认跳转
  },
  {
    path: '/',
    name: 'HomePage',
    component: () => import('@/components/HomePage.vue'),
    meta: { requiresAuth: true },
    children: coreAppRoutes
  },
  {
    path: '/mobile/:uuid',
    name: 'MobileConfirm',
    component: () => import('@/views/MobileConfirm.vue')
  },
  {
    path: '/404',
    name: 'NotFoundPage',
    component: () => import('@/views/404NotFound.vue')
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'RouteFallback',
    component: () => import('@/views/404NotFound.vue')
  }
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

// 注意：原有的 beforeEach 逻辑已迁移至 permission.ts，这里不再需要重复

export default router;
