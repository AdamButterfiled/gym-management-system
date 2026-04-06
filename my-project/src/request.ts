import axios from 'axios';
import { message } from 'ant-design-vue';
import router from '@/router';

const request = axios.create({
    baseURL: 'http://localhost:9090',
    timeout: 5000
});

request.interceptors.request.use(config => {
    config.headers['Content-Type'] = 'application/json;charset=utf-8';

    // 1. 从浏览器缓存 (localStorage) 中取出用户信息
    const userJson = localStorage.getItem("user");
    if (userJson) {
        const user = JSON.parse(userJson);
        // 2. 如果用户已登录由 Token
        if (user.token) {
            // 3. 关键步骤：把 Token 放入请求头的 Authorization 字段
            // 格式必须是 "Bearer " + token，这是 JWT 的标准规范
            config.headers['Authorization'] = 'Bearer ' + user.token;
        }
    }

    return config;
}, error => {
    return Promise.reject(error);
});

// 防止重复弹出多个提示
let isLoggingOut = false;

request.interceptors.response.use(response => {
    let res = response.data;
    // If response is a file/blob, return directly
    if (response.config.responseType === 'blob') {
        return res;
    }
    // If string, try query parsing (for compatibility)
    if (typeof res === 'string') {
        res = res ? JSON.parse(res) : res;
    }
    // 业务层 401：后端返回的 code === 401 (token 过期)
    if (res.code === 401) {
        handleTokenExpired();
        return Promise.reject(new Error('登录已过期'));
    }
    return res;
}, error => {
    // HTTP 层 401：后端直接返回 401 状态码
    if (error.response && error.response.status === 401) {
        handleTokenExpired();
    } else {
        message.error('网络请求失败，请稍后重试');
    }
    return Promise.reject(error);
});

function handleTokenExpired() {
    if (isLoggingOut) return; // 防止多次触发
    isLoggingOut = true;

    message.warning('登录已过期，请重新登录');
    localStorage.removeItem('user');

    // 延迟跳转，让用户看到提示
    setTimeout(() => {
        router.push('/login');
        isLoggingOut = false;
    }, 1500);
}

export default request;
