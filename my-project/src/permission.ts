import router from './router'
import store from '@/store'
import { message } from 'ant-design-vue';
import { RouteLocationNormalized, NavigationGuardNext } from 'vue-router';
import { clearAuthStorage, getStoredUser, hasValidToken } from '@/utils/auth';
import { collectRouteCandidates, getSmartRedirectTarget, isRouteFallback, normalizeRoutePath } from './router/routeCorrection';

const viewModules: Record<string, () => Promise<unknown>> = import.meta.glob('./views/**/*.vue');

// 白名单路由 (无需登录即可访问)：必须精确匹配，避免 /login1212 这类错误路径被放行成空白页。
const isPublicRoute = (path: string) => {
    const normalizedPath = normalizeRoutePath(path);
    return normalizedPath === '/login'
        || normalizedPath === '/404'
        || /^\/mobile\/[^/]+$/.test(normalizedPath);
};

const normalizeMenuComponentPath = (componentPath?: string | null) => String(componentPath || '')
        .replace(/^\/+/, '')
        .replace(/\.vue$/i, '');

const isPlaceholderMenuComponent = (componentPath?: string | null) => {
    const normalizedComponentPath = normalizeMenuComponentPath(componentPath).toLowerCase();
    return !normalizedComponentPath || normalizedComponentPath === 'layout';
};

const resolveMenuComponent = (componentPath: string) => {
    const normalizedComponentPath = normalizeMenuComponentPath(componentPath);
    const modulePath = `./views/${normalizedComponentPath}.vue`;
    const loader = viewModules[modulePath];

    if (!loader) {
        throw new Error(`Component not found: ${modulePath}`);
    }

    return loader;
};

const resetRouteState = () => {
    store.commit('SET_ROUTES_LOADED', false);
    store.commit('SET_MENU_TREE', []);
};

const hasRegisteredRoute = (path: string, name?: string) => {
    const normalizedPath = normalizeRoutePath(path);
    return router.getRoutes().some(route => {
        const sameName = typeof route.name === 'string' && !!name && route.name === name;
        const samePath = normalizeRoutePath(route.path) === normalizedPath;
        return sameName || samePath;
    });
};

const collectMenuPaths = (menus: Array<{ path?: string; component?: string | null; children?: any[] }> = []) => {
    const pathSet = new Set<string>();

    const walk = (items: Array<{ path?: string; component?: string | null; children?: any[] }>) => {
        items.forEach(item => {
            if (item.path && !isPlaceholderMenuComponent(item.component)) {
                pathSet.add(normalizeRoutePath(item.path));
            }

            if (item.children && item.children.length > 0) {
                walk(item.children);
            }
        });
    };

    walk(menus);
    return Array.from(pathSet);
};

const redirectToFirstAccessibleMenu = (
    next: NavigationGuardNext,
    to: RouteLocationNormalized,
    accessiblePaths: string[],
    currentPath: string
) => {
    const firstAccessiblePath = accessiblePaths[0];
    if (firstAccessiblePath) {
        redirectToResolvedPath(next, to, firstAccessiblePath);
        return;
    }

    redirectToNotFound(next, currentPath);
};

const registerDynamicRoutes = (menuList: any[]) => {
    menuList.forEach((menu: any) => {
        if (isPlaceholderMenuComponent(menu.component)) {
            return;
        }
        if (menu.component && menu.path) {
            try {
                if (hasRegisteredRoute(menu.path, menu.name || menu.title)) {
                    return;
                }

                let effectiveStyle = menu.componentStyle;
                if (!effectiveStyle && menu.parentId) {
                    let currentParentId = menu.parentId;
                    while (currentParentId) {
                        const parent = menuList.find((item: any) => item.id === currentParentId);
                        if (parent) {
                            if (parent.componentStyle) {
                                effectiveStyle = parent.componentStyle;
                                break;
                            }
                            currentParentId = parent.parentId;
                        } else {
                            break;
                        }
                    }
                }

                if (!effectiveStyle) {
                    effectiveStyle = 'default';
                }

                router.addRoute('HomePage', {
                    path: menu.path.replace(/^\//, ''),
                    name: (menu.name || menu.title) as string,
                    component: resolveMenuComponent(menu.component),
                    meta: {
                        title: menu.title,
                        style: effectiveStyle
                    }
                });
            } catch (error) {
                console.error(`Component load failed: ${menu.component}`, error);
            }
        }
    });
};

const redirectToResolvedPath = (
    next: NavigationGuardNext,
    to: RouteLocationNormalized,
    path: string
) => {
    next({
        path,
        query: to.query,
        hash: to.hash,
        replace: true
    });
};

const redirectToNotFound = (next: NavigationGuardNext, path: string) => {
    next({
        path: '/404',
        query: { from: path },
        replace: true
    });
};

const handleSmartFallback = (
    to: RouteLocationNormalized,
    next: NavigationGuardNext,
    candidates: string[]
) => {
    const normalizedPath = normalizeRoutePath(to.path);

    if (candidates.includes(normalizedPath)) {
        redirectToResolvedPath(next, to, normalizedPath);
        return true;
    }

    const smartTarget = getSmartRedirectTarget(normalizedPath, candidates);
    if (smartTarget) {
        redirectToResolvedPath(next, to, smartTarget);
        return true;
    }

    redirectToNotFound(next, normalizedPath);
    return true;
};

router.beforeEach(async (to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
    const user = getStoredUser();
    const tokenReady = hasValidToken(user);
    const normalizedPath = normalizeRoutePath(to.path);

    if (isRouteFallback(to)) {
        const publicTarget = getSmartRedirectTarget(normalizedPath, collectRouteCandidates(router));
        if (publicTarget && isPublicRoute(publicTarget)) {
            redirectToResolvedPath(next, to, publicTarget);
            return;
        }
    }

    // 1. 判断是否在白名单中
    if (isPublicRoute(normalizedPath)) {
        // 如果是去登录页且已登录，跳转到首页
        if (user && !tokenReady) {
            clearAuthStorage();
            resetRouteState();
        }

        if (normalizedPath === '/login' && tokenReady) {
            next({ path: '/' });
        } else {
            next();
        }
        return;
    }

    // 2. 判断是否有Token (已登录)
    if (tokenReady) {
        // 3. 判断路由是否已动态加载
        if (store.getters.areRoutesLoaded) {
            const accessiblePaths = collectMenuPaths(store.getters.getMenuTree);
            if (normalizedPath !== '/' && !accessiblePaths.includes(normalizedPath)) {
                if (isRouteFallback(to)) {
                    handleSmartFallback(to, next, accessiblePaths.length ? accessiblePaths : collectRouteCandidates(router));
                } else {
                    redirectToFirstAccessibleMenu(next, to, accessiblePaths, normalizedPath);
                }
                return;
            }

            if (isRouteFallback(to)) {
                handleSmartFallback(to, next, accessiblePaths.length ? accessiblePaths : collectRouteCandidates(router));
                return;
            }
            next();
        } else {
            try {
                const menuList = await store.dispatch('fetchUserMenus');
                registerDynamicRoutes(menuList);
                store.commit('SET_ROUTES_LOADED', true);

                const candidates = collectRouteCandidates(router, menuList);
                const accessiblePaths = collectMenuPaths(menuList);
                if (normalizedPath !== '/' && !accessiblePaths.includes(normalizedPath)) {
                    const resolvedRoute = router.resolve(normalizedPath);
                    if (isRouteFallback(resolvedRoute)) {
                        handleSmartFallback(to, next, accessiblePaths.length ? accessiblePaths : candidates);
                    } else {
                        redirectToFirstAccessibleMenu(next, to, accessiblePaths, normalizedPath);
                    }
                    return;
                }

                if (candidates.includes(normalizedPath)) {
                    next({ ...to, replace: true });
                    return;
                }

                const resolvedRoute = router.resolve(normalizedPath);
                if (isRouteFallback(resolvedRoute)) {
                    handleSmartFallback(to, next, accessiblePaths.length ? accessiblePaths : candidates);
                    return;
                }

                next({ ...to, replace: true });

            } catch (error) {
                console.error("Dynamic route loading error", error);
                clearAuthStorage();
                resetRouteState();
                next('/login');
            }
        }
    } else {
        if (user) {
            clearAuthStorage();
            resetRouteState();
            message.warning('登录状态已失效，请重新登录');
        }

        if (normalizedPath !== '/login') {
            next(`/login?redirect=${encodeURIComponent(to.fullPath)}`);
        } else {
            next();
        }
    }
});
