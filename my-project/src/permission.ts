import router from './router'
import store from '@/store'
import { RouteLocationNormalized, NavigationGuardNext } from 'vue-router';

// 白名单路由 (无需登录即可访问)
const whiteList = ['/login', '/mobile', '/404'];

router.beforeEach(async (to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
    // 1. 判断是否在白名单中
    // 注意：/mobile/:uuid 这种带参数的需要用正则或 startsWith 判断
    if (whiteList.some(path => to.path.startsWith(path))) {
        // 如果是去登录页且已登录，跳转到首页
        const userJson = localStorage.getItem('user');
        if (to.path === '/login' && userJson) {
            next({ path: '/' });
        } else {
            next();
        }
        return;
    }

    // 2. 判断是否有Token (已登录)
    const userJson = localStorage.getItem('user');
    if (userJson) {
        // 3. 判断路由是否已动态加载
        if (store.getters.areRoutesLoaded) {
            next();
        } else {
            try {
                // 4. 获取菜单数据 (返回扁平列表)
                const menuList = await store.dispatch('fetchUserMenus');

                // 5. 动态添加路由
                // 我们所有的动态页面都是 HomePage 的子路由，所以 parentName='HomePage'
                // 如果是顶级路由(Layout)，则 parentName 为空

                // 引入所有 views 下的组件
                // 注意: require.context 是 Webpack 的，Vite 用 import.meta.glob
                // 这里项目是 Vue CLI (Webpack)，使用 require.context 或 import()
                // 但 import() 必须写死一部分路径，所以最好建立映射与后端约定
                // 简单起见，我们只能假设组件都在 src/views 下

                menuList.forEach((menu: any) => {
                    if (menu.component && menu.path) {
                        try {
                            // 计算有效样式 (简单的继承逻辑：如果当前没有，查找父级)
                            let effectiveStyle = menu.componentStyle;
                            if (!effectiveStyle && menu.parentId) {
                                let currentParentId = menu.parentId;
                                // 查找父级菜单配置
                                while (currentParentId) {
                                    // 注意: menuList 是扁平数组
                                    const parent = menuList.find((m: any) => m.id === currentParentId);
                                    if (parent) {
                                        if (parent.componentStyle) {
                                            effectiveStyle = parent.componentStyle;
                                            break; // 找到最近配置即停止
                                        }
                                        currentParentId = parent.parentId;
                                    } else {
                                        break;
                                    }
                                }
                            }

                            // 默认样式处理：如果最终没有找到样式，或者显式为 default，统一标记为 default
                            // 这样 HomePage 可以统一处理
                            if (!effectiveStyle) {
                                effectiveStyle = 'default';
                            }



                            router.addRoute('HomePage', {
                                path: menu.path.replace(/^\//, ''), // 去掉开头斜杠作为子路由path(相对路径)
                                name: (menu.name || menu.title) as string,      // 路由名称
                                // 核心：根据后端 component 字符串加载组件
                                // 必须是相对路径，例如 'gym/VenueList' -> import('@/views/gym/VenueList.vue')
                                component: () => import(`@/views/${menu.component}.vue`),
                                meta: {
                                    title: menu.title,
                                    style: effectiveStyle // 将计算后的样式存入 meta，供 App.vue 或组件读取
                                }
                            });
                        } catch (e) {
                            console.error(`Component load failed: ${menu.component}`, e);
                        }
                    }
                });

                // 6. 添加 404 路由 (最后添加，防止匹配不到动态路由)
                router.addRoute({ path: '/:pathMatch(.*)*', redirect: '/dashboard' });

                // 7. 标记路由已加载
                store.commit('SET_ROUTES_LOADED', true);

                // 8. 重新进入路由 (replace: true 确保路由加载完成)
                next({ ...to, replace: true });

            } catch (error) {
                console.error("Dynamic route loading error", error);
                // 出错时清除登录状态并重定向回登录页
                localStorage.removeItem('user');
                next('/login');
            }
        }
    } else {
        // 未登录，跳转到登录页
        // 避免无限重定向
        if (to.path !== '/login') {
            next(`/login?redirect=${to.path}`);
        } else {
            next();
        }
    }
});
