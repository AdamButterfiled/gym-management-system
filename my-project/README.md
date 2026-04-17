# GMS Admin

`my-project` 是 Gym Management System (GMS) 的 Vue 3 管理后台，面向运营人员、管理员和内部管理角色，提供场馆、课程、会员、预约、支付、签到和系统配置等页面。

## 技术栈

- Vue 3
- Vite
- TypeScript
- Vue Router
- Vuex
- Ant Design Vue
- ECharts / Unovis
- Axios

## 安装依赖

```bash
cd my-project
npm install
```

## 本地开发

```bash
npm run dev
```

默认需要后端服务先运行在：

- `http://localhost:9090`

因为开发环境下会通过 Vite 代理把接口请求转发给后端。

## 生产构建

```bash
npm run build
```

## 本地预览

```bash
npm run preview
```

## 代码检查

```bash
npm run lint
```

## 说明

- 详细项目说明请查看仓库根目录 [README.md](../README.md)
- Vite 配置参考：<https://vite.dev/config/>
