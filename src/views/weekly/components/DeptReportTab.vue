<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  mergeReports, getDeptReports, getDeptReport, updateDeptReport,
  submitDeptReport, finalizeDeptReport, exportWord, exportHtml,
  listTeamSummariesByWeek, getDeptTeamStatus, getTeamSummary
} from '@/api/weekly'
import ReportDetail from './ReportDetail.vue'

const userStore = useUserStore()
const reports = ref([])
const teamSummaries = ref([])
const deptTeamStatus = ref(null)
const selectedType = ref('')
const selectedTeamSummary = ref(null)
const selectedDeptReport = ref(null)
const editing = ref(false)
const saving = ref(false)
const merging = ref(false)
const submitLoading = ref(false)

const isHead = computed(() => (userStore.user?.roles || []).includes('ROLE_ADMIN'))

const editOverview = ref('')
const editKeyProgress = ref('')
const editCommonIssues = ref('')
const editNextWeekPlans = ref('')
const editCoordinationItems = ref('')
const exportVisible = ref(false)
const activeFormat = ref('word')

// Calculate current week Monday/Friday
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
  try {
    const thisMonday = getWeekDates().weekStartDate
    const [a, b, c] = await Promise.all([
      getDeptReports().catch(() => ({ data: [] })),
      listTeamSummariesByWeek(thisMonday).catch(() => ({ data: [] })),
      getDeptTeamStatus().catch(() => ({ data: null }))
    ])
    reports.value = a.data || []
    teamSummaries.value = b.data || []
    deptTeamStatus.value = c.data
  } catch {
    /* ignore */
  }
}

function sl(s) {
  return s === 'DRAFT' ? '草稿' : s === 'PENDING_REVIEW' ? '已提交' : s === 'FINALIZED' ? '已审定' : '未生成'
}

function stc(s) {
  return s === 'DRAFT' ? 'tag-draft' : s === 'PENDING_REVIEW' ? 'tag-submitted' : s === 'FINALIZED' ? 'tag-approved' : 'tag-draft'
}

function tsStatusIcon(s) {
  return s === 'SUBMITTED' ? '✅' : '⏳'
}

async function selectTS(ts) {
  selectedType.value = 'team'
  selectedTeamSummary.value = ts
  selectedDeptReport.value = null
  exportVisible.value = false
}

async function selectDR(r) {
  selectedType.value = 'dept'
  selectedDeptReport.value = r
  selectedTeamSummary.value = null
  exportVisible.value = false
  try {
    const res = await getDeptReport(r.id)
    pf(res.data.editedContent || res.data.mergedContent)
  } catch {
    /* ignore */
  }
}

function pf(c) {
  if (!c) { cf(); return }
  try {
    const o = JSON.parse(c)
    editOverview.value = o.overview || ''
    editKeyProgress.value = o.keyProgress || ''
    editCommonIssues.value = o.commonIssues || ''
    editNextWeekPlans.value = o.nextWeekPlans || ''
    editCoordinationItems.value = o.coordinationItems || ''
  } catch {
    cf()
    editOverview.value = c
  }
}

function cf() {
  editOverview.value = editKeyProgress.value = editCommonIssues.value = editNextWeekPlans.value = editCoordinationItems.value = ''
}

function bc() {
  return JSON.stringify({
    overview: editOverview.value,
    keyProgress: editKeyProgress.value,
    commonIssues: editCommonIssues.value,
    nextWeekPlans: editNextWeekPlans.value,
    coordinationItems: editCoordinationItems.value
  })
}

function parseContentFields(content) {
  if (!content) return { overview: '', keyProgress: '', commonIssues: '', nextWeekPlans: '', coordinationItems: '' }
  try {
    const o = JSON.parse(content)
    return {
      overview: o.overview || '',
      keyProgress: o.keyProgress || '',
      commonIssues: o.commonIssues || '',
      nextWeekPlans: o.nextWeekPlans || '',
      coordinationItems: o.coordinationItems || ''
    }
  } catch {
    return { overview: content, keyProgress: '', commonIssues: '', nextWeekPlans: '', coordinationItems: '' }
  }
}

async function handleMerge() {
  merging.value = true
  try {
    const { weekStartDate, weekEndDate } = getWeekDates()
    const res = await mergeReports({ weekStartDate, weekEndDate })
    ElMessage.success('AI合并完成')
    // 直接用响应数据加入列表，防止 refresh 覆盖
    if (res.data) {
      reports.value = [res.data, ...reports.value.filter(r => r.id !== res.data.id)]
      selectDR(res.data)
    }
    const keepId = res.data?.id
    await refresh()
    if (keepId && !reports.value.find(r => r.id === keepId) && res.data) {
      reports.value = [res.data, ...reports.value]
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.error || 'AI合并失败')
  } finally {
    merging.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateDeptReport(selectedDeptReport.value.id, { editedContent: bc() })
    ElMessage.success('已保存')
    editing.value = false
  } finally {
    saving.value = false
  }
}

async function handleSubmit() {
  submitLoading.value = true
  try {
    await submitDeptReport(selectedDeptReport.value.id)
    ElMessage.success('已提交给负责人')
    refresh()
  } finally {
    submitLoading.value = false
  }
}

async function handleFinalize() {
  await finalizeDeptReport(selectedDeptReport.value.id)
  ElMessage.success('已审定')
  refresh()
}

function openExport() {
  exportVisible.value = true
  activeFormat.value = 'word'
  const c = selectedDeptReport.value?.editedContent || selectedDeptReport.value?.mergedContent || ''
  pf(c)
}

async function handleDownloadWord() {
  try {
    const r = await exportWord(selectedDeptReport.value.id)
    const u = URL.createObjectURL(new Blob([r], { type: 'application/octet-stream' }))
    const a = document.createElement('a')
    a.href = u
    const dateStr = (selectedDeptReport.value?.weekStartDate || '').replace(/-/g, '')
    a.download = `部门周报_${dateStr}.doc`
    a.click()
    URL.revokeObjectURL(u)
  } catch {
    ElMessage.error('下载失败')
  }
}

async function handleDownHtml() {
  try {
    const r = await exportHtml(selectedDeptReport.value.id)
    const b = new Blob([typeof r === 'string' ? r : r.data || ''], { type: 'text/html;charset=utf-8' })
    const u = URL.createObjectURL(b)
    const a = document.createElement('a')
    a.href = u
    const dateStr = (selectedDeptReport.value?.weekStartDate || '').replace(/-/g, '')
    a.download = `部门周报_${dateStr}.html`
    a.click()
    URL.revokeObjectURL(u)
  } catch {
    ElMessage.error('下载失败')
  }
}

function handleCopy() {
  const c = selectedDeptReport.value?.editedContent || selectedDeptReport.value?.mergedContent || ''
  pf(c)
  const t = [editOverview.value, editKeyProgress.value, editCommonIssues.value, editNextWeekPlans.value, editCoordinationItems.value]
    .filter(Boolean).join('\n\n')
  navigator.clipboard.writeText(t).then(() => ElMessage.success('已复制'))
}

// All teams for the department (from deptTeamStatus)
const deptTeams = computed(() => {
  return deptTeamStatus.value?.teams || []
})

function teamSummaryForTeam(teamName) {
  return teamSummaries.value.find(s => s.teamName === teamName)
}

// 处理团队点击：优先使用已加载的汇总，兜底按ID获取
async function handleTeamClick(t) {
  // 没有组内汇总（后端已精确判断）
  if (!t.hasSummary) {
    selectedType.value = ''
    selectedTeamSummary.value = null
    selectedDeptReport.value = null
    ElMessage.info('该组本周暂无组内汇总，请等待组长提交')
    return
  }

  // 已加载的汇总列表中匹配
  const local = teamSummaryForTeam(t.teamName)
  if (local) {
    selectTS(local)
    return
  }

  // 有汇总但日期不一致，按ID单独获取
  if (t.summaryId) {
    try {
      const res = await getTeamSummary(t.summaryId)
      const data = res.data || res
      if (data && data.id) {
        // 补入列表并选中
        teamSummaries.value = [data, ...teamSummaries.value]
        selectTS(data)
        return
      }
    } catch {
      // 获取失败，走兜底
    }
  }

  // 最终兜底
  ElMessage.info('组内汇总数据加载失败，请刷新页面重试')
}

// 本周的部门汇总（优先精确匹配，兜底取最近一份）
const currentWeekReport = computed(() => {
  const thisMonday = getWeekDates().weekStartDate
  // 优先精确匹配本周
  const exact = reports.value.find(r => r.weekStartDate === thisMonday)
  if (exact) return exact
  // 兜底：取最近一份（处理后端回退逻辑导致周日期不一致的情况）
  if (reports.value.length === 0) return null
  return reports.value.reduce((a, b) =>
    (a.weekStartDate || '') > (b.weekStartDate || '') ? a : b
  )
})

// 部门周报是否已审定
const isDeptReportFinalized = computed(() => currentWeekReport.value?.status === 'FINALIZED')

// ---- ReportDetail 数据转换 ----
// 部门汇总详情
const deptReportTitle = computed(() => '部门周报')
const deptReportSubtitle = computed(() => {
  const r = selectedDeptReport.value
  if (!r) return ''
  return `${r.weekStartDate} ~ ${r.weekEndDate} · 基于各组周报 AI 合并`
})
const deptReportSections = computed(() => {
  const secs = []
  if (editOverview.value) {
    secs.push({ type: 'prose', title: '本周工作概览', colorBar: 'green', content: editOverview.value })
  }
  if (editKeyProgress.value) {
    secs.push({ type: 'prose', title: '重点工作进展', colorBar: 'blue', content: editKeyProgress.value })
  }
  if (editCommonIssues.value) {
    secs.push({ type: 'problems', title: '共性问题与风险', colorBar: 'red', content: editCommonIssues.value })
  }
  if (editNextWeekPlans.value) {
    secs.push({ type: 'prose', title: '下周重点计划', colorBar: 'blue', content: editNextWeekPlans.value })
  }
  if (editCoordinationItems.value) {
    secs.push({ type: 'support', title: '需要协调的事项', colorBar: 'amber', content: editCoordinationItems.value })
  }
  return secs
})

// 组内汇总详情（在部门汇总页面查看）
const tsSubtitle = computed(() => {
  const ts = selectedTeamSummary.value
  if (!ts) return ''
  return `${ts.teamName} · 组长: ${ts.leaderName || '-'} · ${ts.weekStartDate} ~ ${ts.weekEndDate}`
})
const tsSections = computed(() => {
  const ts = selectedTeamSummary.value
  if (!ts) return []
  const f = parseContentFields(ts.editedContent || ts.mergedContent)
  const secs = []
  if (f.overview) secs.push({ type: 'prose', title: '本周工作概览', colorBar: 'green', content: f.overview })
  if (f.keyProgress) secs.push({ type: 'prose', title: '重点工作进展', colorBar: 'blue', content: f.keyProgress })
  if (f.commonIssues) secs.push({ type: 'problems', title: '共性问题与风险', colorBar: 'red', content: f.commonIssues })
  if (f.nextWeekPlans) secs.push({ type: 'prose', title: '下周重点计划', colorBar: 'blue', content: f.nextWeekPlans })
  if (f.coordinationItems) secs.push({ type: 'support', title: '需要协调的事项', colorBar: 'amber', content: f.coordinationItems })
  return secs
})
</script>

<template>
  <div>
    <div class="dashboard-layout" style="min-height:560px;">
      <!-- Left sidebar -->
      <div class="dashboard-sidebar" style="width:240px;">
        <!-- 各组周报列表 -->
        <div style="padding:8px;" v-if="deptTeams.length">
          <div style="font-size:11px;font-weight:700;color:var(--gray-400);padding:4px 8px 6px;">
            各组周报 ({{ deptTeams.length }}组)
          </div>
          <div
            v-for="t in deptTeams" :key="t.teamName"
            class="sidebar-member"
            :class="{ active: selectedType === 'team' && selectedTeamSummary?.teamName === t.teamName }"
            style="align-items:flex-start;padding:8px;"
            :style="{ cursor: t.hasSummary ? 'pointer' : 'default' }"
            @click="handleTeamClick(t)"
          >
            <div class="member-info" style="flex:1;">
              <div class="member-name" style="font-size:12px;">{{ t.teamName }}</div>
              <div class="member-meta">组长: {{ t.leaderName || '-' }} · {{ t.submittedCount }}/{{ t.memberCount }}人提交</div>
            </div>
            <span class="sb-pill" :class="t.hasSummary ? (t.summaryStatus === 'DRAFT' && !isDeptReportFinalized ? 'draft' : 'ok') : 'no'">{{ t.hasSummary ? (isDeptReportFinalized ? '已审定' : (t.summaryStatus === 'DRAFT' ? '草稿' : '已提交')) : '未生成' }}</span>
          </div>
        </div>
        <div v-else style="padding:8px 16px;font-size:12px;color:var(--gray-400);">
          暂无团队数据
        </div>

        <!-- AI 汇总按钮（各组周报下方） -->
        <div style="padding:8px;border-top:1px solid #eee;">
          <el-tooltip
            :content="isDeptReportFinalized ? '周报已审批，不允许再次修改' : ''"
            :disabled="!isDeptReportFinalized"
            placement="top"
          >
            <span style="display:block;">
              <button class="ai-merge-btn" :disabled="merging || isDeptReportFinalized" @click="handleMerge">
                <span v-if="merging">⏳</span><span v-else>✨</span> AI 生成本周部门汇总
              </button>
            </span>
          </el-tooltip>
        </div>

        <!-- 本周部门汇总（AI 按钮下方，已生成时显示） -->
        <div v-if="currentWeekReport" style="padding:0 8px 8px;border-top:1px solid var(--border-light);">
          <div
            class="sidebar-member"
            :class="{ active: selectedType === 'dept' && selectedDeptReport?.id === currentWeekReport.id }"
            style="align-items:flex-start;border-left:2px solid var(--green);"
            @click="selectDR(currentWeekReport)"
          >
            <div class="member-info">
              <div class="member-name" style="font-size:12px;">📋 本周部门汇总</div>
              <div class="member-meta">{{ sl(currentWeekReport.status) }}</div>
            </div>
            <span class="sb-pill" :class="currentWeekReport.status === 'DRAFT' ? 'draft' : 'ok'">{{ currentWeekReport.status === 'DRAFT' ? '草稿' : (currentWeekReport.status === 'FINALIZED' ? '已审定' : '已提交') }}</span>
          </div>
        </div>
      </div>

      <!-- Right: Editor -->
      <div class="dashboard-main" style="flex:1;">
        <!-- 部门汇总（查看模式） -->
        <ReportDetail
          v-if="selectedType === 'dept' && selectedDeptReport && !editing && !exportVisible"
          :title="deptReportTitle"
          :subtitle="deptReportSubtitle"
          :status="selectedDeptReport.status"
          :sections="deptReportSections"
          :aiSource="'🤖 AI 合并来源：' + teamSummaries.map(t => t.teamName + '（' + (t.sourceReportIds?.split(',').length || 0) + '人）').join(' · ')"
        >
          <template #actions>
            <el-button v-if="selectedDeptReport.status === 'DRAFT'" size="small" @click="editing = true">📝 编辑</el-button>
            <el-button v-if="selectedDeptReport.status === 'DRAFT'" size="small" type="primary" :loading="submitLoading" @click="handleSubmit">📤 提交负责人</el-button>
            <el-button v-if="selectedDeptReport.status === 'PENDING_REVIEW' && isHead" size="small" type="success" @click="handleFinalize">✓ 审定通过</el-button>
            <el-button v-if="selectedDeptReport.status === 'FINALIZED'" size="small" type="primary" @click="openExport">📮 导出分发</el-button>
          </template>
        </ReportDetail>

        <!-- 部门汇总（编辑模式） -->
        <template v-if="selectedType === 'dept' && selectedDeptReport && editing && !exportVisible">
          <div class="detail-topbar">
            <div style="font-size:14px;font-weight:700;">📝 编辑部门周报</div>
            <div style="display:flex;gap:8px;">
              <el-button size="small" type="primary" :loading="saving" @click="handleSave">💾 保存</el-button>
              <el-button size="small" @click="editing = false">取消</el-button>
            </div>
          </div>
          <div class="detail-body">
            <div style="margin-bottom:12px;"><label style="font-size:12px;font-weight:600;display:block;margin-bottom:4px;">本周工作概览</label><el-input v-model="editOverview" type="textarea" :rows="2" /></div>
            <div style="margin-bottom:12px;"><label style="font-size:12px;font-weight:600;display:block;margin-bottom:4px;">重点工作进展</label><el-input v-model="editKeyProgress" type="textarea" :rows="5" /></div>
            <div style="margin-bottom:12px;"><label style="font-size:12px;font-weight:600;color:var(--red);display:block;margin-bottom:4px;">共性问题与风险</label><el-input v-model="editCommonIssues" type="textarea" :rows="3" /></div>
            <div style="margin-bottom:12px;"><label style="font-size:12px;font-weight:600;display:block;margin-bottom:4px;">下周重点计划</label><el-input v-model="editNextWeekPlans" type="textarea" :rows="3" /></div>
            <div style="margin-bottom:12px;"><label style="font-size:12px;font-weight:600;color:var(--brand);display:block;margin-bottom:4px;">需要协调的事项</label><el-input v-model="editCoordinationItems" type="textarea" :rows="2" /></div>
          </div>
        </template>

        <!-- Export view -->
        <template v-if="exportVisible">
          <div class="detail-topbar">
            <div style="font-size:15px;font-weight:700;">📮 导出与分发</div>
          </div>
          <div class="detail-body">
            <!-- Format cards -->
            <div style="display:flex;gap:10px;margin-bottom:12px;">
              <div class="export-card" :class="{ active: activeFormat === 'word' }" @click="activeFormat = 'word'">
                <div style="font-size:24px;margin-bottom:4px;">📄</div>
                <div style="font-size:12px;font-weight:600;">Word 文档</div>
                <div style="font-size:10px;color:var(--gray-400);">标准公文格式</div>
              </div>
              <div class="export-card" :class="{ active: activeFormat === 'html' }" @click="activeFormat = 'html'">
                <div style="font-size:24px;margin-bottom:4px;">🌐</div>
                <div style="font-size:12px;font-weight:600;">HTML 网页</div>
                <div style="font-size:10px;color:var(--gray-400);">企微/邮件分享</div>
              </div>
              <div class="export-card" :class="{ active: activeFormat === 'text' }" @click="activeFormat = 'text'">
                <div style="font-size:24px;margin-bottom:4px;">📋</div>
                <div style="font-size:12px;font-weight:600;">纯文本</div>
                <div style="font-size:10px;color:var(--gray-400);">直接复制到聊天</div>
              </div>
            </div>

            <!-- Preview -->
            <div class="export-preview" v-if="activeFormat === 'word'">
              <div class="export-preview-header">
                📄 Word 预览
                <span style="font-size:10px;color:var(--gray-300);margin-left:auto;">导出后将自动归档至部门文档库</span>
              </div>
              <div class="export-preview-body">
                <div style="text-align:center;padding:8px 0;border-bottom:2px solid var(--gray-900);margin-bottom:14px;">
                  <div style="font-size:18px;font-weight:700;">研发部工作周报</div>
                  <div style="font-size:12px;color:var(--gray-400);margin-top:4px;">
                    {{ selectedDeptReport?.weekStartDate }} — {{ selectedDeptReport?.weekEndDate }}
                  </div>
                </div>
                <p style="font-weight:700;">一、本周工作概览</p>
                <p style="text-indent:2em;">{{ editOverview }}</p>
                <p style="font-weight:700;margin-top:10px;">二、重点工作进展</p>
                <p style="white-space:pre-wrap;">{{ editKeyProgress }}</p>
                <p style="font-weight:700;margin-top:10px;">三、共性问题与风险</p>
                <p style="white-space:pre-wrap;">{{ editCommonIssues }}</p>
                <p style="font-weight:700;margin-top:10px;">四、下周重点计划</p>
                <p style="white-space:pre-wrap;">{{ editNextWeekPlans }}</p>
                <p style="font-weight:700;margin-top:10px;">五、需要协调的事项</p>
                <p style="white-space:pre-wrap;">{{ editCoordinationItems }}</p>
                <div style="text-align:right;margin-top:18px;font-size:11px;color:var(--gray-500);">
                  审定人：{{ selectedDeptReport?.finalizedByName || '-' }}
                </div>
              </div>
            </div>
            <div class="export-preview" v-else>
              <div class="export-preview-header">
                {{ activeFormat === 'html' ? '🌐 HTML 预览' : '📋 纯文本预览' }}
              </div>
              <div class="export-preview-body">
                <pre style="font-family:var(--font-sans);white-space:pre-wrap;font-size:13px;line-height:1.8;">{{ [editOverview, editKeyProgress, editCommonIssues, editNextWeekPlans, editCoordinationItems].filter(Boolean).join('\n\n') }}</pre>
              </div>
            </div>

            <div style="display:flex;justify-content:space-between;align-items:center;margin-top:12px;">
              <div style="font-size:12px;color:var(--gray-500);" v-if="selectedDeptReport?.finalizedAt">
                ✅ 已审定 · {{ selectedDeptReport?.finalizedByName || '-' }}
              </div>
              <div style="display:flex;gap:8px;margin-left:auto;">
                <el-button size="small" @click="handleCopy">📋 复制纯文本</el-button>
                <el-button size="small" @click="handleDownHtml">🌐 下载 HTML</el-button>
                <el-button size="small" type="primary" @click="handleDownloadWord">📥 下载 Word</el-button>
              </div>
            </div>
            <el-button size="small" style="margin-top:12px;" @click="exportVisible = false">← 返回编辑</el-button>
          </div>
        </template>

        <!-- 组内汇总详情 -->
        <ReportDetail
          v-if="selectedType === 'team' && selectedTeamSummary"
          title="组内汇总"
          :subtitle="tsSubtitle"
          :status="isDeptReportFinalized ? 'FINALIZED' : selectedTeamSummary.status"
          :sections="tsSections"
        />

        <div v-if="!selectedType" class="detail-body" style="display:flex;align-items:center;justify-content:center;color:var(--gray-400);font-size:14px;">
          选择左侧一组周报或部门汇总记录查看详情
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sb-pill {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}
.sb-pill.ok { background: #ecfdf5; color: #059669; }
.sb-pill.no { background: #fef2f2; color: #dc2626; }
.sb-pill.draft { background: #f3f4f6; color: #6b7280; }
.sb-pill.warn { background: #fffbeb; color: #d97706; }

.ai-merge-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 9px;
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

.dashboard-layout { display: flex; border: 1px solid #eee; border-radius: 8px; overflow: hidden; background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,.04); }
.dashboard-sidebar { background: #fafbfc; border-right: 1px solid #eee; display: flex; flex-direction: column; }
.dashboard-main { display: flex; flex-direction: column; }

.sidebar-member {
  display: flex; align-items: center; gap: 10px; padding: 9px 10px;
  border-radius: 6px; cursor: pointer;
  transition: all 0.12s; border: 1px solid transparent; margin-bottom: 1px;
}
.sidebar-member:hover { background: #f0f4ff; }
.sidebar-member.active { background: #fff; border-color: #dbeafe; box-shadow: 0 1px 2px rgba(0,0,0,.03); }
.member-info { min-width: 0; flex:1; }
.member-name { font-size: 13px; font-weight: 600; color: var(--gray-700); }
.member-meta { font-size: 11px; color: var(--gray-300); margin-top: 2px; }

.detail-topbar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 16px; border-bottom: 1px solid #eee;
}
.detail-body { flex: 1; overflow-y: auto; padding: 16px; }

.export-card {
  flex: 1; padding: 10px; border: 1px solid #e5e7eb; border-radius: 4px;
  cursor: pointer; text-align: center; transition: all 0.12s; background: #fff;
}
.export-card:hover { border-color: #b3d4ff; }
.export-card.active { border-color: var(--brand); background: #f0f5ff; }

.export-preview {
  border: 1px solid #e5e7eb; border-radius: 4px; overflow: hidden; background: #fff;
}
.export-preview-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 12px; background: var(--gray-50); border-bottom: 1px solid #e5e7eb;
  font-size: 11px; font-weight: 600; color: var(--gray-400);
}
.export-preview-body {
  padding: 16px 20px; background: #fff; max-height: 280px; overflow-y: auto; font-size: 13px; line-height: 1.8;
}
</style>
