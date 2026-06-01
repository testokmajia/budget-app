# 修复：团队周报和部门周报跨周状态污染

## 问题描述

新的一周开始后，团队周报和部门周报界面错误地显示"已审定"，无法生成新一周的周报。

## 根因

前端在两个地方使用了"兜底回退"逻辑：当查询不到当前周的记录时，回退使用历史周（上周）的记录。上一周已审定的状态被错误地带入当前周，导致操作被禁用。

### 涉及文件

| 文件 | 位置 | 问题 |
|------|------|------|
| `src/views/weekly/components/TeamWeeklyTab.vue` | `getTeamSummary()` L103-110 | 找不到本周记录时，回退到同团队任意历史记录 |
| `src/views/weekly/components/DeptReportTab.vue` | `currentWeekReport` computed L288-298 | 找不到本周记录时，回退到最近一份历史记录 |

### Bug 流程（以团队周报为例）

1. 上周一：组长生成组内汇总 → 提交 → 部门审定 → 组内汇总状态变为 `APPROVED`
2. 本周一（今天）：组长打开团队周报页面
3. `listMyTeamSummaries()` 返回所有历史记录（含上周 `APPROVED` 记录）
4. `getTeamSummary("某团队")` 找不到本周记录（`weekStartDate === 本周一` 无匹配）
5. **兜底逻辑触发** → 返回上周的 `APPROVED` 记录
6. `teamAiDisabledReason` 检查到 `status === 'APPROVED'` → 返回"组内汇总已审定，无法再次生成"
7. AI 生成按钮被禁用 ❌

## 修复方案

### 原则

**当当前周没有记录时，应返回 null（表示"未生成"），允许用户操作。不应使用历史周数据来判断当前周状态。**

### 具体修改

#### 1. TeamWeeklyTab.vue — `getTeamSummary()` 函数

**修改前（L103-110）：**
```js
function getTeamSummary(teamName) {
  const { weekStartDate } = getWeekDates()
  const exact = summaries.value.find(s => s.teamName === teamName && s.weekStartDate === weekStartDate)
  if (exact) return exact
  // 兜底：取该团队最近一份汇总
  return summaries.value.find(s => s.teamName === teamName) || null
}
```

**修改后：**
```js
function getTeamSummary(teamName) {
  const { weekStartDate } = getWeekDates()
  // 只精确匹配本周，不使用历史周数据兜底
  return summaries.value.find(s => s.teamName === teamName && s.weekStartDate === weekStartDate) || null
}
```

#### 2. DeptReportTab.vue — `currentWeekReport` computed

**修改前（L288-298）：**
```js
const currentWeekReport = computed(() => {
  const thisMonday = getWeekDates().weekStartDate
  const exact = reports.value.find(r => r.weekStartDate === thisMonday)
  if (exact) return exact
  // 兜底：取最近一份
  if (reports.value.length === 0) return null
  return reports.value.reduce((a, b) =>
    (a.weekStartDate || '') > (b.weekStartDate || '') ? a : b
  )
})
```

**修改后：**
```js
const currentWeekReport = computed(() => {
  const thisMonday = getWeekDates().weekStartDate
  // 只精确匹配本周，不使用历史周数据兜底
  return reports.value.find(r => r.weekStartDate === thisMonday) || null
})
```

### 影响分析

- 修复后，新一周开始时两个页面都会正确显示"未生成"状态
- AI 生成按钮正常可用
- 不影响已有数据——数据库中每周的记录都有独立的 `weekStartDate`
- 后端查询逻辑本身是正确的（按周过滤），无需修改

## 测试验证

1. 确保上周有已审定的团队周报和部门周报
2. 系统时间设为下周一（或自然等待到周一）
3. 打开团队周报页面 → AI 生成按钮应为可用状态
4. 打开部门周报页面 → AI 生成按钮应为可用状态
5. 点击生成 → 应创建新一周的记录（而非覆盖上周）
