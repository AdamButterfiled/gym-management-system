import { createApp, nextTick } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import { message } from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
import './styles/globals.css';
import './theme.css';
import './permission'; // 引入路由权限控制
import { markAppBootReady, reportAppBootFailure, updateAppBootMessage } from './utils/appBoot';

const DYNAMIC_IMPORT_RETRY_STORAGE_KEY = 'gms_dynamic_import_retry_v1';
const DYNAMIC_IMPORT_ERROR_PATTERNS = [
    /Failed to fetch dynamically imported module/i,
    /error loading dynamically imported module/i,
    /Importing a module script failed/i,
    /Failed to fetch module script/i,
    /Unable to preload CSS/i,
];

message.config({
    top: '52px',
});

const getRouteKey = (target?: string) => {
    if (typeof window === 'undefined') {
        return target || '/';
    }

    try {
        const url = new URL(target || window.location.href, window.location.origin);
        url.searchParams.delete('__reload');
        return `${url.pathname}${url.search}${url.hash}` || '/';
    } catch {
        return target || `${window.location.pathname}${window.location.search}${window.location.hash}` || '/';
    }
};

const readDynamicImportRetryState = () => {
    if (typeof window === 'undefined') {
        return {} as Record<string, number>;
    }

    try {
        const rawValue = window.sessionStorage.getItem(DYNAMIC_IMPORT_RETRY_STORAGE_KEY);
        const parsedValue = rawValue ? JSON.parse(rawValue) : {};
        return parsedValue && typeof parsedValue === 'object'
            ? parsedValue as Record<string, number>
            : {};
    } catch {
        return {};
    }
};

const writeDynamicImportRetryState = (state: Record<string, number>) => {
    if (typeof window === 'undefined') {
        return;
    }

    window.sessionStorage.setItem(DYNAMIC_IMPORT_RETRY_STORAGE_KEY, JSON.stringify(state));
};

const clearDynamicImportRetryState = (target?: string) => {
    if (typeof window === 'undefined') {
        return;
    }

    const routeKey = getRouteKey(target);
    const retryState = readDynamicImportRetryState();
    if (!(routeKey in retryState)) {
        return;
    }

    delete retryState[routeKey];
    writeDynamicImportRetryState(retryState);
};

const stripRuntimeReloadQuery = () => {
    if (typeof window === 'undefined') {
        return;
    }

    const url = new URL(window.location.href);
    if (!url.searchParams.has('__reload')) {
        return;
    }

    url.searchParams.delete('__reload');
    window.history.replaceState(window.history.state, '', `${url.pathname}${url.search}${url.hash}`);
};

const buildRecoveryUrl = (target?: string) => {
    if (typeof window === 'undefined') {
        return target || '/';
    }

    const url = new URL(target || window.location.href, window.location.origin);
    url.searchParams.set('__reload', String(Date.now()));
    return `${url.pathname}${url.search}${url.hash}`;
};

const isDynamicImportFailure = (error: unknown) => {
    let normalizedError = '';

    if (error instanceof Error) {
        normalizedError = `${error.message}\n${error.stack || ''}`;
    } else if (typeof error === 'string') {
        normalizedError = error;
    } else {
        try {
            normalizedError = JSON.stringify(error);
        } catch {
            normalizedError = String(error);
        }
    }

    return DYNAMIC_IMPORT_ERROR_PATTERNS.some((pattern) => pattern.test(normalizedError));
};

const recoverDynamicImportFailure = (error: unknown, target?: string) => {
    if (typeof window === 'undefined') {
        return false;
    }

    const routeKey = getRouteKey(target);
    const retryState = readDynamicImportRetryState();
    const attempts = Number(retryState[routeKey] || 0);

    if (attempts < 1) {
        retryState[routeKey] = attempts + 1;
        writeDynamicImportRetryState(retryState);
        updateAppBootMessage('页面资源已更新，正在自动刷新...');
        window.location.replace(buildRecoveryUrl(routeKey));
        return true;
    }

    reportAppBootFailure(
        error,
        '页面资源加载失败',
        '系统已经自动重试过一次，但页面模块仍未加载成功。请重新加载；如果还不行，执行清除登录状态。'
    );
    return false;
};

if (typeof window !== 'undefined') {
    window.addEventListener('vite:preloadError', (event: Event) => {
        const preloadEvent = event as Event & {
            payload?: unknown;
            preventDefault?: () => void;
        };

        preloadEvent.preventDefault?.();
        recoverDynamicImportFailure(preloadEvent.payload ?? preloadEvent);
    });
}

router.onError((error, to) => {
    if (isDynamicImportFailure(error)) {
        if (recoverDynamicImportFailure(error, to?.fullPath)) {
            return;
        }
    } else {
        reportAppBootFailure(
            error,
            '路由切换失败',
            '页面跳转过程中发生异常，请重新加载页面。'
        );
    }
});

router.afterEach((to) => {
    clearDynamicImportRetryState(to.fullPath);
    stripRuntimeReloadQuery();
});

const bootstrap = async () => {
    updateAppBootMessage('正在装载应用与路由守卫...');

    const app = createApp(App);
    app.config.errorHandler = (error, _instance, info) => {
        reportAppBootFailure(
            error,
            '页面渲染失败',
            `组件渲染过程中发生异常：${info}`
        );
    };

    app.use(store);
    app.use(router);

    updateAppBootMessage('正在等待路由完成首次解析...');
    await router.isReady();

    app.mount('#app');
    await nextTick();
    stripRuntimeReloadQuery();
    markAppBootReady();
};

void bootstrap().catch((error) => {
    reportAppBootFailure(
        error,
        '页面启动失败',
        '应用在挂载前发生异常。可以直接重试；如果仍失败，先清除登录状态再进入。'
    );
});
