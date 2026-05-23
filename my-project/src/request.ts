import axios, { AxiosError } from 'axios';
import { message } from 'ant-design-vue';
import { clearAuthStorage, getLoginRedirectPath, getStoredUser } from '@/utils/auth';

const normalizeBaseUrl = (url?: string) => (url ? url.replace(/\/+$/, '') : '');

const resolveApiBaseUrl = () => {
    const configuredBaseUrl = normalizeBaseUrl(
        import.meta.env.VITE_API_BASE_URL || import.meta.env.VUE_APP_API_BASE_URL
    );
    if (configuredBaseUrl) {
        return configuredBaseUrl;
    }

    if (typeof window === 'undefined') {
        return '';
    }

    if (import.meta.env.DEV) {
        return '';
    }

    return '';
};

const request = axios.create({
    baseURL: resolveApiBaseUrl(),
    timeout: 5000
});

request.interceptors.request.use(config => {
    config.headers = config.headers || {};
    config.headers['Content-Type'] = 'application/json;charset=utf-8';

    const user = getStoredUser();
    if (user && typeof user.token === 'string' && user.token.trim()) {
        config.headers['Authorization'] = 'Bearer ' + user.token;
    }

    return config;
}, error => {
    return Promise.reject(error);
});

// 防止重复弹出多个提示
let authRedirectTimer: ReturnType<typeof setTimeout> | null = null;
let networkErrorMessageTimer: ReturnType<typeof setTimeout> | null = null;

interface ApiResult {
    code?: number;
    msg?: string;
    data?: unknown;
}

interface ApiErrorPayload {
    msg?: string;
    message?: string;
}

request.interceptors.response.use(response => {
    let res = response.data;
    // If response is a file/blob, return directly
    if (response.config.responseType === 'blob') {
        return res;
    }
    // If string, try query parsing (for compatibility)
    if (typeof res === 'string') {
        try {
            res = res ? JSON.parse(res) : res;
        } catch {
            return res;
        }
    }
    const result = typeof res === 'object' && res !== null ? res as ApiResult : null;
    if (result?.code === 401 || result?.code === 403) {
        handleAuthExpired(result.msg);
        return Promise.reject(new Error(result.msg || '登录状态已失效'));
    }
    return res;
}, error => {
    const responseStatus = error.response?.status;
    if (responseStatus === 401 || responseStatus === 403) {
        handleAuthExpired();
    } else {
        showRequestError(error);
    }
    return Promise.reject(error);
});

function handleAuthExpired(customMessage?: string) {
    clearAuthStorage();

    if (authRedirectTimer) {
        return;
    }

    message.warning(customMessage || '登录状态已失效，请重新登录');

    if (typeof window === 'undefined' || window.location.pathname === '/login') {
        return;
    }

    authRedirectTimer = setTimeout(() => {
        authRedirectTimer = null;
        window.location.replace(getLoginRedirectPath());
    }, 1200);
}

function showRequestError(error: AxiosError<ApiErrorPayload>) {
    if (networkErrorMessageTimer) return;

    const responseData = error.response?.data;
    const responseMessage =
        responseData && typeof responseData === 'object'
            ? responseData.msg || responseData.message
            : '';
    const fallbackMessage = error.response ? '请求失败，请稍后重试' : '网络请求失败，请稍后重试';

    message.error(responseMessage || fallbackMessage);
    networkErrorMessageTimer = setTimeout(() => {
        networkErrorMessageTimer = null;
    }, 1500);
}

export default request;
