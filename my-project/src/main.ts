import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import Antd, { message } from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
import './permission'; // 引入路由权限控制

message.config({
    top: '52px',
});

createApp(App).use(store).use(router).use(Antd).mount('#app')
