# 企业科技管理平台

面向企业内部的科技管理 Web 应用，提供清单管理、奖惩记录、科技问题反馈等业务功能。

## 技术栈

- **前端**: Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios
- **后端**: Spring Boot 3.3.7 + JPA + Spring Security + JWT
- **数据库**: MySQL 8.0

## 项目结构

```
tech-manage/
├── src/                    # Vue 前端源代码
│   ├── api/                # API 请求封装
│   ├── router/             # 路由配置
│   ├── stores/             # Pinia 状态管理
│   ├── views/              # 页面组件
│   ├── layouts/            # 布局组件
│   ├── components/         # 可复用组件
│   └── utils/              # 工具函数
├── backend/                # Spring Boot 后端
│   └── src/main/java/com/techmanage/
│       ├── controller/     # REST 控制器
│       ├── service/        # 业务逻辑
│       ├── repository/     # 数据访问
│       ├── entity/         # JPA 实体
│       ├── dto/            # 数据传输对象
│       ├── config/         # 配置类
│       ├── security/       # JWT 安全
│       └── common/         # 通用类
└── docs/                   # 文档
```

## 快速开始

### 前端

```bash
npm install
npm run dev
```

### 后端

```bash
cd backend
./mvnw spring-boot:run
```

### 数据库

确保 MySQL 已启动，创建数据库 `tech_manage`（JPA 会自动建表）。
