# 修复：历史记录页看不到团队周报和部门周报

## 问题描述

在"工作周报 → 历史记录"页面，有三个子标签页：个人周报、小组周报、部门周报。但今天生成的团队周报和部门周报不显示。

## 根因

历史记录页的三个标签页都只从 `weekly_reports`（个人周报）表加载数据：

| 标签页 | 当前 API | 当前数据源 | 缺失数据源 |
|--------|---------|-----------|-----------|
| 小组周报 | `/weekly-reports/team` | 个人周报（按团队筛选） | `team_summaries` 表 |
| 部门周报 | `/weekly-reports/all` | 全部个人周报 | `department_reports` 表 |

团队周报和部门周报存储在独立表中，通过不同 API 访问：
- 团队周报：`/team-summaries/my`（组长）或 `/team-summaries/by-week`（文员/管理员）
- 部门周报：`/department-reports`

## 修复方案

### HistoryTab.vue

1. 新增导入 `listMyTeamSummaries` 和 `getDeptReports`
2. 在"小组周报"标签页：同时加载 `getTeamHistory()` 和 `listMyTeamSummaries()`，合并数据
3. 在"部门周报"标签页：同时加载 `getAllHistory()` 和 `getDeptReports()`，合并数据
4. 将 TeamSummary 和 DepartmentReport 转换为与现有列表兼容的格式

### 数据格式转换

TeamSummary/DepartmentReport 的 JSON 内容拆解为现有格式的字段：
- `doneWork` ← overview + keyProgress（文本拼接）
- `planWork` ← nextWeekPlans
- `problems` ← commonIssues
- `supportNeeded` ← coordinationItems

## 涉及文件

| 文件 | 修改类型 |
|------|---------|
| `src/views/weekly/components/HistoryTab.vue` | 修改数据加载逻辑 |

前端构建验证即可，无需修改后端。
