<script setup>
import { ref, onMounted, onUnmounted, computed, inject, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getSubmittedList, getTeamStats, remindMember,
  mergeTeamSummary, updateTeamSummary, submitTeamSummary,
  listMyTeamSummaries, getDeptFinalizedStatus
} from '@/api/weekly'
import { parseItems } from '@/utils/workItemParser'
import ReportDetail from './ReportDetail.vue'

const userStore = useUserStore()
const loading = ref(false)

// Permission: leader can operate, clerk/admin can only view
const canOperate = computed(() => userStore.hasRole('ROLE_LEADER') || userStore.hasRole('ROLE_ADMIN'))

// 将团队名称传递给父组件标题栏
const headerTeams = inject('headerTeams', null)

const teamStats = ref(null)
// 注入父组件提供的 headerStats，写入数据供标题栏条形图使用
const headerStats = inject('headerStats', null)
const pendingReports = ref([])
const summaries = ref([])
const deptFinalized = ref(false)
const selectedMember = ref(null)
const selectedReportId = ref(null)
const selectedTeamName = ref('')
// Track whether we're viewing a team summary or a member report
const viewingSummary = ref(false)
const viewingSummaryId = ref(null)

// Merge state
const mergingTeam = ref('')
const editing = ref(false)
const editFields = ref({ overview: '', keyProgress: '', commonIssues: '', nextWeekPlans: '', coordinationItems: '' })

function getWeekDates() {
  const now = new Date()
  const day = now.getDay() || 7
  const monday = new Date(now)
  monday.setDate(now.getDate() - day + 1)
  const friday = new Date(monday)
  friday.setDate(monday.getDate() + 4)
  return {
    weekStartDate: monday.toISOString().slice(0, 10),
    weekEndDate: friday.toISOString().slice(0, 10)
  }
}

onMounted(refresh)

async function refresh() {
  loading.value = true
  try {
    const [sr, tr, su, fs] = await Promise.all([
      getSubmittedList().catch(() => ({ data: [] })),
      getTeamStats().catch(() => ({ data: null })),
      listMyTeamSummaries().catch(() => ({ data: [] })),
      getDeptFinalizedStatus().catch(() => ({ data: { finalized: false } }))
    ])
    pendingReports.value = sr.data || []
    teamStats.value = tr.data
    if (headerStats) headerStats.value = tr.data
    summaries.value = su.data || []
    deptFinalized.value = fs.data?.finalized === true
  } finally {
    loading.value = false
  }
}

const teams = computed(() => teamStats.value?.teams || [])

// 当前周部门周报是否已审定（FINALIZED）
const isDeptReportFinalized = computed(() => deptFinalized.value)

// 将团队名称同步到父组件标题栏（依赖 teams，必须放在 teams 定义之后）
watch(teams, (t) => {
  if (headerTeams) {
    headerTeams.value = t.map(t => t.teamName)
  }
}, { immediate: true })
onUnmounted(() => {
  if (headerTeams) headerTeams.value = []
})

// 当前查看的汇总是否可编辑（仅草稿状态可编辑）
const isCurrentSummaryEditable = computed(() => {
  if (!viewingSummary.value || !viewingSummaryId.value) return false
  const s = summaries.value.find(s => s.id === viewingSummaryId.value)
  return s && s.status !== 'SUBMITTED' && s.status !== 'APPROVED'
})

function memberAvatarColor(name) {
  if (!name) return '#006eff'
  const colors = ['#006eff', '#0abf5b', '#ff9c00', '#e54545', '#722ed1']
  return colors[name.charCodeAt(0) % colors.length]
}

// 获取指定团队的组内汇总（优先本周，兜底取最近一份）
function getTeamSummary(teamName) {
  const { weekStartDate } = getWeekDates()
  // 优先精确匹配本周
  const exact = summaries.value.find(s => s.teamName === teamName && s.weekStartDate === weekStartDate)
  if (exact) return exact
  // 兜底：取该团队最近一份汇总（处理后端因回退逻辑导致周日期不一致的情况）
  return summaries.value.find(s => s.teamName === teamName) || null
}

// 判断 AI 生成按钮是否应禁用，返回禁用原因（空字符串表示不禁用）
function teamAiDisabledReason(teamName) {
  if (mergingTeam.value === teamName) return 'loading'
  if (isDeptReportFinalized.value) return '周报已审批，不允许再次修改'
  const summary = getTeamSummary(teamName)
  if (summary?.status === 'SUBMITTED') return '组内汇总已提交，无法再次生成'
  if (summary?.status === 'APPROVED') return '组内汇总已审定，无法再次生成'
  return ''
}

async function selectMember(teamName, member) {
  selectedTeamName.value = teamName
  viewingSummary.value = false
  viewingSummaryId.value = null
  if (member.reportId) {
    const report = pendingReports.value.find(r => r.id === member.reportId)
    if (report) {
      selectedReportId.value = report.id
      selectedMember.value = {
        id: report.id,
        name: report.userName || member.name,
        dept: report.teamName || report.userDepartment || teamName,
        version: report.version || 1,
        status: report.status,
        submittedAt: report.submittedAt,
        doneItems: parseItems(report.doneWork),
        planItems: parseItems(report.planWork),
        problems: report.problems || '',
        supportNeeded: report.supportNeeded || '',
        reviewComment: report.reviewComment || ''
      }
      return
    }
  }
  selectedReportId.value = null
  selectedMember.value = {
    id: null,
    name: member.name,
    dept: teamName,
    version: 0,
    status: 'NOT_SUBMITTED',
    submittedAt: null,
    doneItems: [],
    planItems: [],
    problems: '',
    supportNeeded: '',
    reviewComment: ''
  }
}

function selectSummary(teamName, summary) {
  selectedTeamName.value = teamName
  selectedMember.value = null
  selectedReportId.value = null
  viewingSummary.value = true
  viewingSummaryId.value = summary.id
  editFields.value = parseContent(summary.editedContent || summary.mergedContent)
}

async function handleRemind(reportId) {
  await remindMember(reportId)
  ElMessage.success('已催办')
}

// ---- AI merge: direct merge, no popup ----
async function handleMerge(teamName) {
  mergingTeam.value = teamName
  try {
    const { weekStartDate, weekEndDate } = getWeekDates()
    const res = await mergeTeamSummary({ teamName, weekStartDate, weekEndDate })
    ElMessage.success('AI 汇总完成')
    const newSummary = res.data || res
    if (newSummary && newSummary.id) {
      // 加入列表并选中
      summaries.value = [newSummary, ...summaries.value.filter(s => s.id !== newSummary.id)]
      selectSummary(teamName, newSummary)
    }
    // 只刷新成员和统计，不覆盖 summaries
    // 如果刷新失败，保留原有数据不清空
    const [sr, tr] = await Promise.all([
      getSubmittedList().catch(() => ({ data: [] })),
      getTeamStats().catch(() => ({ data: null }))
    ])
    if (sr.data !== undefined) pendingReports.value = sr.data || []
    if (tr.data) {
      teamStats.value = tr.data
      if (headerStats) headerStats.value = tr.data
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.error || 'AI 汇总失败')
  } finally {
    mergingTeam.value = ''
  }
}

function parseContent(c) {
  try {
    const o = JSON.parse(c)
    return {
      overview: o.overview || '',
      keyProgress: o.keyProgress || '',
      commonIssues: o.commonIssues || '',
      nextWeekPlans: o.nextWeekPlans || '',
      coordinationItems: o.coordinationItems || ''
    }
  } catch {
    return { overview: c || '', keyProgress: '', commonIssues: '', nextWeekPlans: '', coordinationItems: '' }
  }
}

async function handleSaveMerge() {
  await updateTeamSummary(viewingSummaryId.value, { editedContent: JSON.stringify(editFields.value) })
  ElMessage.success('已保存')
  editing.value = false
}

async function handleSubmitMerge() {
  await submitTeamSummary(viewingSummaryId.value)
  ElMessage.success('已提交至部门文书（组员周报已自动审批通过）')
  viewingSummary.value = false
  viewingSummaryId.value = null
  editing.value = false
  refresh()
}

// ---- ReportDetail 数据转换 ----
// 成员详情
const memberTitle = computed(() => '个人周报')
const memberSubtitle = computed(() => {
  const m = selectedMember.value
  if (!m) return ''
  const parts = [m.name]
  if (m.dept) parts.push(m.dept)
  if (m.submittedAt) parts.push('提交于 ' + m.submittedAt)
  if (m.version > 0) parts.push('V' + m.version)
  return parts.join(' · ')
})
// 成员报告状态（考虑部门审定覆盖）
const memberReportStatus = computed(() => {
  if (!selectedMember.value) return ''
  if (isDeptReportFinalized.value) return 'FINALIZED'
  return selectedMember.value.status
})

const memberStats = computed(() => {
  const m = selectedMember.value
  if (!m) return null
  const doneItems = m.doneItems || []
  return {
    done: doneItems.filter(i => i.item && i.status === 'done').length,
    inProgress: doneItems.filter(i => i.item && i.status === 'in-progress').length,
    blocked: doneItems.filter(i => i.status === 'blocked').length,
    plan: (m.planItems || []).filter(i => i.item).length,
  }
})
const memberSections = computed(() => {
  const m = selectedMember.value
  if (!m) return []
  const secs = []
  const doneItems = (m.doneItems || []).filter(i => i.item)
  if (doneItems.length) {
    secs.push({ type: 'work-items', title: '本周完成工作', colorBar: 'green', count: doneItems.length, items: doneItems })
  }
  const planItems = (m.planItems || []).filter(i => i.item)
  if (planItems.length) {
    secs.push({ type: 'simple-list', title: '下周工作计划', colorBar: 'blue', count: planItems.length, items: planItems })
  }
  if (m.problems) {
    secs.push({ type: 'problems', title: '遇到的问题', colorBar: 'red', content: m.problems })
  }
  if (m.supportNeeded) {
    secs.push({ type: 'support', title: '需要支持', colorBar: 'amber', content: m.supportNeeded })
  }
  return secs
})

// 组内汇总详情
const summaryTitle = computed(() => '组内汇总')
const summarySubtitle = computed(() => {
  return selectedTeamName.value || ''
})
const summarySections = computed(() => {
  const secs = []
  if (editFields.value.overview) {
    secs.push({ type: 'prose', title: '本周工作概览', colorBar: 'green', content: editFields.value.overview })
  }
  if (editFields.value.keyProgress) {
    secs.push({ type: 'prose', title: '重点工作进展', colorBar: 'blue', content: editFields.value.keyProgress })
  }
  if (editFields.value.commonIssues) {
    secs.push({ type: 'problems', title: '共性问题与风险', colorBar: 'red', content: editFields.value.commonIssues })
  }
  if (editFields.value.nextWeekPlans) {
    secs.push({ type: 'prose', title: '下周重点计划', colorBar: 'blue', content: editFields.value.nextWeekPlans })
  }
  if (editFields.value.coordinationItems) {
    secs.push({ type: 'support', title: '需要协调的事项', colorBar: 'amber', content: editFields.value.coordinationItems })
  }
  return secs
})

function summaryStatus() {
  if (isDeptReportFinalized.value) return 'FINALIZED'
  const s = getTeamSummary(selectedTeamName.value)
  return s ? s.status : 'NOT_GENERATED'
}

// 状态中文映射（统一：未生成 / 草稿 / 已提交 / 已审定）
function summaryStatusLabel(status) {
  if (status === 'DRAFT') return '草稿'
  if (status === 'SUBMITTED') return '已提交'
  if (status === 'APPROVED' || status === 'FINALIZED') return '已审定'
  if (status === 'NOT_GENERATED') return '未生成'
  return status || '未生成'
}

// 状态样式映射
function summaryStatusClass(status) {
  if (status === 'DRAFT') return 'draft'
  if (status === 'SUBMITTED' || status === 'APPROVED') return 'ok'
  return 'draft'
}
</script>

<template>
  <div v-loading="loading">
    <!-- Dashboard view -->
    <div class="dashboard-layout">
      <!-- Left sidebar -->
      <div class="dashboard-sidebar">
        <div class="sidebar-teams" v-if="teams.length">
          <div v-for="t in teams" :key="t.teamName" class="sidebar-team-group">
            <div class="sidebar-team-header">
              <span class="sidebar-team-title">{{ t.teamName }}</span>
              <span class="sidebar-team-meta">{{ t.submittedCount }}/{{ t.totalMembers }}人</span>
            </div>

            <!-- Members -->
            <div
              v-for="m in t.members" :key="m.name"
              class="sidebar-member"
              :class="{ active: !viewingSummary && selectedMember?.name === m.name && selectedTeamName === t.teamName, overdue: !m.submitted }"
              @click="selectMember(t.teamName, m)"
            >
              <div class="member-avatar" :style="{ background: memberAvatarColor(m.name) }">{{ m.name.charAt(0) }}</div>
              <div class="member-info">
                <div class="member-name">{{ m.name }}</div>
              </div>
              <span class="member-pill" :class="(m.status === 'APPROVED' || m.submitted) ? 'ok' : 'no'">{{ (m.submitted && isDeptReportFinalized) || m.status === 'APPROVED' ? '已审定' : (m.submitted ? '已提交' : '未生成') }}</span>
            </div>

            <!-- AI merge button -->
            <el-tooltip
              v-if="canOperate"
              :content="teamAiDisabledReason(t.teamName) === 'loading' ? '' : teamAiDisabledReason(t.teamName)"
              :disabled="!teamAiDisabledReason(t.teamName) || teamAiDisabledReason(t.teamName) === 'loading'"
              placement="top"
            >
              <span style="display:block;">
                <button
                  class="ai-merge-btn"
                  :disabled="!!teamAiDisabledReason(t.teamName)"
                  @click="handleMerge(t.teamName)"
                >
                  <span v-if="mergingTeam === t.teamName">⏳</span>
                  <span v-else>✨</span> AI 生成小组周报
                </button>
              </span>
            </el-tooltip>

            <!-- Team summary below AI button (if exists) -->
            <div
              v-if="getTeamSummary(t.teamName)"
              class="sidebar-member summary-item"
              :class="{ active: viewingSummary && viewingSummaryId === getTeamSummary(t.teamName).id }"
              @click="selectSummary(t.teamName, getTeamSummary(t.teamName))"
            >
              <div class="member-avatar" style="background: #0abf5b; font-size:11px;">📋</div>
              <div class="member-info">
                <div class="member-name">本周组内汇总</div>
              </div>
              <span class="member-pill" :class="isDeptReportFinalized ? 'ok' : summaryStatusClass(getTeamSummary(t.teamName).status)">{{ isDeptReportFinalized ? '已审定' : summaryStatusLabel(getTeamSummary(t.teamName).status) }}</span>
            </div>
          </div>
        </div>
        <div v-else style="padding:16px;text-align:center;color:var(--gray-400);font-size:13px;">
          暂无团队数据
        </div>
      </div>

      <!-- Right side: detail panel -->
      <div class="dashboard-main">
        <!-- 组内汇总（查看模式） -->
        <ReportDetail
          v-if="viewingSummary && viewingSummaryId && !editing"
          :title="summaryTitle"
          :subtitle="summarySubtitle"
          :status="summaryStatus()"
          :sections="summarySections"
          aiSource="🤖 AI 合并自各组周报"
        >
          <template v-if="canOperate && isCurrentSummaryEditable" #actions>
            <el-button size="small" @click="editing = true">📝 编辑润色</el-button>
            <el-button size="small" type="primary" @click="handleSubmitMerge">📤 提交至部门文书</el-button>
          </template>
        </ReportDetail>

        <!-- 组内汇总（编辑模式） -->
        <template v-if="viewingSummary && viewingSummaryId && editing">
          <div class="detail-topbar">
            <div style="font-size:14px;font-weight:700;">📝 编辑组内汇总</div>
            <div style="display:flex;gap:8px;">
              <el-button size="small" type="primary" @click="handleSaveMerge">💾 保存</el-button>
              <el-button size="small" @click="editing = false">取消</el-button>
            </div>
          </div>
          <div class="detail-body">
            <div style="margin-bottom:10px;"><label style="font-size:12px;font-weight:600;display:block;margin-bottom:4px;">本周工作概览</label>
              <el-input v-model="editFields.overview" type="textarea" :rows="2" /></div>
            <div style="margin-bottom:10px;"><label style="font-size:12px;font-weight:600;display:block;margin-bottom:4px;">重点工作进展</label>
              <el-input v-model="editFields.keyProgress" type="textarea" :rows="4" /></div>
            <div style="margin-bottom:10px;"><label style="font-size:12px;font-weight:600;color:var(--red);display:block;margin-bottom:4px;">共性问题与风险</label>
              <el-input v-model="editFields.commonIssues" type="textarea" :rows="2" /></div>
            <div style="margin-bottom:10px;"><label style="font-size:12px;font-weight:600;display:block;margin-bottom:4px;">下周重点计划</label>
              <el-input v-model="editFields.nextWeekPlans" type="textarea" :rows="2" /></div>
          </div>
        </template>

        <!-- 成员详情 -->
        <ReportDetail
          v-else-if="selectedMember && selectedMember.status !== 'NOT_SUBMITTED'"
          :title="memberTitle"
          :subtitle="memberSubtitle"
          :status="memberReportStatus"
          :stats="memberStats"
          :sections="memberSections"
          :reviewComment="selectedMember.reviewComment"
        />

        <!-- 成员未提交 -->
        <div v-else-if="selectedMember" class="detail-body" style="display:flex;flex-direction:column;align-items:center;justify-content:center;color:var(--gray-400);gap:12px;">
          <span>📝 该成员尚未提交本周报</span>
          <el-button v-if="selectedMember.id" size="small" type="danger" @click="handleRemind(selectedMember.id)">催办</el-button>
        </div>

        <!-- 空状态 -->
        <div v-else class="detail-body" style="display:flex;align-items:center;justify-content:center;color:var(--gray-400);">
          选择左侧成员查看周报详情，或点击"AI 汇总本组"生成组内汇总
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-layout {
  display: flex; min-height: 500px;
  border: 1px solid #eee; border-radius: 8px;
  overflow: hidden; background: #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
}

.dashboard-sidebar {
  width: 240px; flex-shrink: 0;
  background: #fafbfc; border-right: 1px solid #eee;
  display: flex; flex-direction: column;
}

.sidebar-teams { flex: 1; overflow-y: auto; }
.sidebar-team-group { padding: 6px 8px; }
.sidebar-team-group + .sidebar-team-group { border-top: 1px solid #f0f0f0; }
.sidebar-team-header { display: flex; justify-content: space-between; align-items: center; padding: 4px 6px 5px; }
.sidebar-team-title { font-size: 12px; font-weight: 700; color: var(--gray-400); text-transform: uppercase; letter-spacing: 0.3px; }
.sidebar-team-meta { font-size: 11px; color: var(--gray-300); }

.sidebar-member {
  display: flex; align-items: center; gap: 10px; padding: 9px 10px;
  border-radius: 6px; cursor: pointer; transition: all 0.12s;
  border: 1px solid transparent; margin-bottom: 1px;
}
.sidebar-member:hover { background: #f0f4ff; }
.sidebar-member.active { background: #fff; border-color: #dbeafe; box-shadow: 0 1px 2px rgba(0,0,0,.03); }
.sidebar-member.overdue { border-left: 2px solid #e54545; }
.sidebar-member.summary-item { border-left: 2px solid #0abf5b; }

.member-avatar {
  width: 30px; height: 30px; border-radius: 6px; color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 12px; flex-shrink: 0;
}
.member-info { flex: 1; min-width: 0; }
.member-name { font-size: 13px; font-weight: 600; color: var(--gray-700); }
.member-meta { font-size: 11px; color: var(--gray-300); margin-top: 2px; }

.member-pill {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}
.member-pill.ok { background: #ecfdf5; color: #059669; }
.member-pill.no { background: #fef2f2; color: #dc2626; }
.member-pill.draft { background: #f3f4f6; color: #6b7280; }

.ai-merge-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 9px;
  margin: 6px 0;
  border: 1px solid #bfdbfe;
  background: #f0f5ff;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.ai-merge-btn:hover { background: #dbeafe; border-color: #93c5fd; }
.ai-merge-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.dashboard-main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.detail-topbar { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid #eee; }
.detail-body { flex: 1; overflow-y: auto; padding: 16px; }
</style>
