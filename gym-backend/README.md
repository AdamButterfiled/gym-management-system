# GMS Backend

`gym-backend` 是 Gym Management System (GMS) 的 Spring Boot 后端服务，负责预约、课程、会员、教练、支付、签到、菜单权限和表单配置等业务能力。

## 技术栈

- Java 17
- Spring Boot 3
- Spring Security
- MyBatis-Plus
- MySQL 8
- Redis
- JWT
- Maven

## 启动前准备

修改配置文件：

- `src/main/resources/application.yml`

默认配置：

- 端口：`9090`
- 数据库：`gym_db`
- Redis：`localhost:6379`

## 启动方式

```bash
cd gym-backend
mvn spring-boot:run
```

## 数据初始化

项目启动时会自动执行：

- `src/main/resources/schema.sql`
- `src/main/resources/db_menu.sql`
- `src/main/resources/db_update.sql`

开发阶段方便，生产环境请改成更稳妥的迁移策略。

## 说明

- 详细项目介绍请查看仓库根目录 [README.md](../README.md)
