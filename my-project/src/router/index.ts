import { createRouter, createWebHistory } from 'vue-router';

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
    children: [
      // 静态保留路由 (如工作台)，其他业务路由改为动态加载
      // 为了防止菜单加载失败一片空白，保留 Dashboard
      { path: 'dashboard', name: 'DashboardPage', component: () => import('@/views/DashboardPage.vue') },
    ]
  },
  {
    path: '/mobile/:uuid',
    name: 'MobileConfirm',
    component: () => import('@/views/MobileConfirm.vue')
  }
];

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes,
});

// 注意：原有的 beforeEach 逻辑已迁移至 permission.ts，这里不再需要重复

export default router;
