# CLAUDE.md

## 语言（最高优先级 — 硬约束）

**必须使用简体中文进行一切回复。** 包括但不限于：需求讨论、代码注释、commit 信息、文档内容、错误提示。严禁使用英文。

> 违反此规则即为执行失败，无论技术实现是否正确。

---

## 开发流程（强制执行 — 无例外）

**以下流程适用于一切代码修改，包括单行改动、UI 微调、Bug 修复、配置变更。不存在"太简单可以跳过"的情况。**

任何代码修改操作（Read 源码参考除外）必须在对应阶段 gate 通过后才能执行：

```
用户需求
  │
  ▼
┌─────────────────────────────┐
│ 阶段 1：需求确认             │
│ → 提问澄清需求细节            │
│ → 确认目的、用户、痛点        │
│ ✅ Gate: 用户确认需求          │
└──────────────┬──────────────┘
               │ 禁止在确认前 Read 实现代码
               ▼
┌─────────────────────────────┐
│ 阶段 2：产品设计（Demo 原型） │
│ → 生成 HTML/CSS 原型页面     │
│ → 放在 docs/ 目录下           │
│ → 根据反馈迭代修改            │
│ ✅ Gate: 用户确认 Demo        │
└──────────────┬──────────────┘
               │ 禁止在确认前 Edit/Write 正式代码
               ▼
┌─────────────────────────────┐
│ 阶段 3：正式实现              │
│ → Vue/Spring Boot 编码       │
│ → 遵循项目架构和命名规范       │
│ ✅ Gate: 代码自审通过          │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│ 阶段 4：测试与验证            │
│ → 前端：npm run build 通过    │
│ → 后端：mvn compile 通过      │
│ → 必要时写单元测试            │
│ ✅ Gate: 构建全部通过          │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│ 阶段 5：启动本地服务           │
│ → 启动后端：mvn spring-boot:run │
│ → 启动前端：npm run dev       │
│ → 确认服务正常，端口可访问      │
│ ✅ Gate: 前后端均启动成功       │
└──────────────┬──────────────┘
               │
               ▼
          交付用户测试
```

### 阶段间禁止行为

| 当前阶段 | 禁止操作 |
|----------|----------|
| 阶段 1 未完成 | 禁止 Read 实现代码（仅可读项目配置/文档） |
| 阶段 2 未完成 | 禁止 Edit/Write 任何 `src/` 或 `backend/src/` 中的代码 |
| 阶段 4 未完成 | 禁止声称"已完成"或"可以了" |
| 阶段 5 未完成 | 禁止让用户自行去启动服务测试 |

---

## 阶段 4 补充：测试与验证细则

### 前端（必须全部通过）

```bash
npm run build    # 生产构建，必须零错误
```

- 构建失败 = 阶段 4 未通过，修复后重新验证
- chunk 大小警告可以忽略，但需说明
- 如有新增组件，确认组件已在路由/父组件中正确引用

### 后端（必须全部通过）

```bash
cd backend && mvn compile    # 至少编译通过
```

- 编译失败 = 阶段 4 未通过，修复后重新验证
- 如涉及关键业务逻辑变更，应补充单元测试

---

## 阶段 5 补充：本地服务启动与验证

### 启动后端

```bash
cd backend && mvn spring-boot:run
```

验证标准：
- 日志中出现 `Started TechManageApplication in X seconds`
- 端口 8080 监听确认：日志中无 `Port already in use` 错误
- 数据库连接成功：日志中无 `CommunicationsException` 或 `Access denied`

### 启动前端

```bash
npm run dev
```

验证标准：
- Vite 输出 `Local: http://localhost:5173/`
- 无编译错误（黄色警告可以接受）

### 验证完成后

两个服务都启动成功后，告知用户：
- 前端地址：`http://localhost:5173`
- 后端地址：`http://localhost:8080`
- API 代理已配置（`/api` → `localhost:8080`）

---

## Project

企业科技管理平台（Enterprise IT Technology Management）。

Monorepo: 前端在根目录，后端在 `backend/`。

## Architecture

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + `<script setup>` Composition API + Vite |
| UI 库 | Element Plus（中文 locale） |
| 状态管理 | Pinia |
| 路由 | Vue Router 4 |
| HTTP | Axios（`src/api/request.js`，baseURL `/api`） |
| 后端 | Spring Boot 3.3.7 + Java 21 + JPA |
| 数据库 | MySQL `tech_manage`，localhost:3306，root / 空密码 |
| 认证 | Spring Security + JWT |
| 入口 | 前端 `src/main.js` → `src/App.vue`；后端 `TechManageApplication.java` |

## Commands

```bash
# 前端
npm run dev                  # 启动开发服务器（端口 5173，HMR）
npm run build                # 生产构建
npm run preview              # 本地预览生产构建

# 后端
cd backend && ./mvnw spring-boot:run   # 启动后端（端口 8080）
cd backend && ./mvnw compile           # 仅编译
cd backend && ./mvnw clean package -DskipTests   # 打包 JAR
```

## Conventions

- 前端组件使用 `<script setup>` + Composition API
- CSS 变量在 `src/style.css :root` 中定义，前缀 `--`
- 文件组织：按功能/视图划分，组件放 `components/` 子目录
- 后端：Controller → Service → Repository 标准分层
- DTO 使用 Java record，校验注解使用中文 message
- API 响应统一使用 `ApiResponse<T>` 信封格式
- 不可变性：前端用扩展运算符，后端用 record / 防御性拷贝
