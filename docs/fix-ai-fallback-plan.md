# 修复：无成员周报时，AI 应基于上周计划生成团队/部门周报

## 问题描述

当前行为：
- **团队周报**：如果组员都没提交个人周报 → 报错"组内暂无已提交的周报"，无法生成
- **部门周报**：如果各组都没提交团队周报 → 报错"本周暂无已提交或已审批的周报"，无法生成

期望行为：
- **团队周报**：基于**上周团队周报的下周计划（nextWeekPlans）** AI 生成本周周报，本周的 nextWeekPlans 留空
- **部门周报**：基于**上周部门周报的下周计划** AI 生成本周周报，下周计划留空
- **兜底**：如果上周也没有记录 → 生成空模板（DRAFT 状态），允许组长/文书手动编辑

## 涉及文件

| 文件 | 修改类型 |
|------|---------|
| `backend/.../service/impl/TeamSummaryServiceImpl.java` | 修改 `mergeAi()` 方法 |
| `backend/.../service/impl/DepartmentReportServiceImpl.java` | 修改 `mergeAi()` 方法 |

前端无需修改（上次已修复兜底逻辑）。

## 详细设计

### 1. TeamSummaryServiceImpl.mergeAi()

**修改前流程：**
```
获取成员报告 → allSubmitted 为空？→ 抛异常"组内暂无已提交的周报"
            → reports 为空（目标周无数据）？→ 抛异常"本周组内暂无已提交的周报"
            → AI 合并
```

**修改后流程：**
```
获取成员报告
  ├─ 有报告 → 现有逻辑不变，AI 合并
  └─ 无报告（allSubmitted 为空 或 目标周 reports 为空）
       ├─ 查找上周 (targetWeek - 7天) 该团队的 TeamSummary
       │    ├─ 存在且有 nextWeekPlans → 构造合成报告 → AI 合并 → 清空 nextWeekPlans
       │    └─ 不存在或无 nextWeekPlans → 生成空模板
       └─ 保存记录
```

**合成报告构造方式：**
- 从上周 TeamSummary 的 mergedContent/editedContent（JSON）解析出 `nextWeekPlans`
- 构造一个 `WeeklyReportResponse`：
  - `userName` = "上周计划"
  - `doneWork` = 解析出的 nextWeekPlans 内容
  - `planWork` = ""
  - `problems` = ""
  - `supportNeeded` = ""
- 调用现有 `aiService.mergeReports(List.of(syntheticReport))`
- AI 返回后，解析 JSON，将 `nextWeekPlans` 字段清空

**空模板：**
```json
{
  "overview": "",
  "keyProgress": "",
  "commonIssues": "",
  "nextWeekPlans": "",
  "coordinationItems": ""
}
```

### 2. DepartmentReportServiceImpl.mergeAi()

**修改前流程：**
```
获取团队周报 → 有 → AI 合并
            → 无 → 获取个人周报 → 有 → AI 合并
                                → 无 → 查找最新有数据周 → 有 → 重试
                                                         → 无 → 抛异常
```

**修改后流程：**
```
获取团队周报
  ├─ 有 → 现有逻辑不变，AI 合并
  └─ 无 → 获取个人周报
           ├─ 有 → 现有逻辑不变，AI 合并
           └─ 无 → 查找上周 (targetWeek - 7天) 的 DepartmentReport
                    ├─ 存在且有 nextWeekPlans → 构造合成报告 → AI 合并 → 清空 nextWeekPlans
                    └─ 不存在或无 nextWeekPlans → 生成空模板
```

**合成报告构造方式（与团队周报同理）：**
- 从上周 DepartmentReport 的 mergedContent/editedContent JSON 解析 `nextWeekPlans`
- 构造 `WeeklyReportResponse`，doneWork = 上周计划
- 调用 `aiService.mergeReports(List.of(syntheticReport))`
- 清空 AI 返回的 `nextWeekPlans`

## 关键技术细节

### 上周周报查询

```java
// 团队周报
LocalDate lastMonday = targetWeek.minusDays(7);
var lastSummary = teamSummaryRepository
    .findByTeamNameAndWeekStartDate(teamName, lastMonday);

// 部门周报
var lastReport = deptReportRepository
    .findByDepartmentAndWeekStartDate("信息科技部", lastMonday);
```

### JSON 解析 nextWeekPlans

```java
private String extractNextWeekPlans(String jsonContent) {
    if (jsonContent == null || jsonContent.isBlank()) return null;
    try {
        JsonNode node = objectMapper.readTree(jsonContent);
        String plans = node.path("nextWeekPlans").asText(null);
        return (plans != null && !plans.isBlank()) ? plans : null;
    } catch (Exception e) {
        return null; // 解析失败，走空模板
    }
}
```

### 清空 AI 返回的 nextWeekPlans

```java
// AI 返回的 JSON 中，将 nextWeekPlans 置空
JsonNode aiResult = objectMapper.readTree(aiContent);
var obj = (ObjectNode) aiResult;
obj.put("nextWeekPlans", "");
String finalContent = objectMapper.writeValueAsString(obj);
```

### 空模板 JSON

当上周也无记录或解析失败时，不调用 AI，直接构造空 JSON：
```java
String emptyTemplate = """
    {
      "overview": "",
      "keyProgress": "",
      "commonIssues": "",
      "nextWeekPlans": "",
      "coordinationItems": ""
    }""";
```

## 数据流示意

```
新一周开始，无成员周报
  │
  ▼
查询上周团队周报
  │
  ├─ 找到上周周报，JSON中有 nextWeekPlans: "1. 完成XX系统对接\n2. 推进YY项目二期"
  │   │
  │   ▼
  │ 构造合成 WeeklyReportResponse:
  │   doneWork = "1. 完成XX系统对接\n2. 推进YY项目二期"
  │   │
  │   ▼
  │ AI prompt:"以下是本周各员工的工作汇报：\n--- 上周计划 ---\n本周完成工作：\n1. 完成XX系统对接\n2. 推进YY项目二期\n..."
  │   │
  │   ▼
  │ AI 返回: { overview:"本周持续推进...", keyProgress:"...", nextWeekPlans:"1. 继续..." }
  │   │
  │   ▼
  │ 清空 nextWeekPlans → 保存到数据库
  │
  └─ 没找到上周周报 或 nextWeekPlans 为空
      │
      ▼
     生成空模板 → 保存到数据库（状态 DRAFT）
```

## 测试验证

1. 确保上周有已审定的团队周报（含下周计划）和部门周报（含下周计划）
2. 系统时间设为下周一
3. 不提交任何个人周报
4. 点击"AI 生成小组周报" → 应生成基于上周计划的团队周报，下周计划为空
5. 点击"AI 生成部门周报" → 应生成基于上周计划的部门周报，下周计划为空
6. 验证生成的记录 weekStartDate 为本周一（新的一周），而非上周
7. 删除上周团队周报，再次生成 → 应生成空模板
