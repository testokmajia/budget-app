<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSubmittedList, mergeTeamSummary, updateTeamSummary, submitTeamSummary, listMyTeamSummaries, getMyTeams } from '@/api/weekly'

const emit = defineEmits(['submitted'])

const loading = ref(false)
const reports = ref([])
const summaries = ref([])
const teams = ref([])
const selectedReport = ref(null)
const selectedReportId = ref(null)

const merging = ref(false)
const mergeResult = ref(null)
const editing = ref(false)
const editingId = ref(null)
const editSaving = ref(false)
const submitLoading = ref(false)

const editOverview = ref('')
const editKeyProgress = ref('')
const editCommonIssues = ref('')
const editNextWeekPlans = ref('')
const editCoordinationItems = ref('')

// Group reports by team. Returns array of { teamName, members: [...] }
// If no teams data, all reports go into a single default group.
const groupedReports = computed(() => {
  if (teams.value.length === 0) {
    return reports.value.length > 0 ? [{ teamName: '', members: reports.value }] : []
  }
  const memberSet = new Map()
  teams.value.forEach(t => {
    t.members.forEach(name => memberSet.set(name, t.name))
  })
  const groups = new Map()
  teams.value.forEach(t => groups.set(t.name, []))
  groups.set('', []) // unmatched
  reports.value.forEach(r => {
    const team = memberSet.get(r.userName) || ''
    if (groups.has(team)) {
      groups.get(team).push(r)
    } else {
      groups.get('').push(r)
    }
  })
  const result = []
  teams.value.forEach(t => {
    const list = groups.get(t.name) || []
    if (list.length > 0) result.push({ teamName: t.name, members: list })
  })
  const unmatched = groups.get('') || []
  if (unmatched.length > 0) result.push({ teamName: '其他', members: unmatched })
  return result
})

async function fetchReports() {
  loading.value = true
  try {
    const res = await getSubmittedList()
    reports.value = res.data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '加载失败')
  } finally {
    loading.value = false
  }
}

async function fetchSummaries() {
  try {
    const res = await listMyTeamSummaries()
    summaries.value = res.data || []
  } catch { summaries.value = [] }
}

async function fetchTeams() {
  try {
    const res = await getMyTeams()
    teams.value = res.data || []
  } catch { teams.value = [] }
}

function formatDate(d) {
  if (!d) return '-'
  return typeof d === 'string' ? d.split('T')[0] : d
}

function formatDateTime(d) {
  if (!d) return '-'
  return typeof d === 'string' ? d.replace('T', ' ') : d
}

function parseContentToFields(text) {
  if (!text) return
  try {
    const obj = JSON.parse(text)
    if (typeof obj === 'object') {
      editOverview.value = obj.overview || ''
      editKeyProgress.value = obj.keyProgress || ''
      editCommonIssues.value = obj.commonIssues || ''
      editNextWeekPlans.value = obj.nextWeekPlans || ''
      editCoordinationItems.value = obj.coordinationItems || ''
      return
    }
  } catch { /* not JSON */ }
  editOverview.value = text
  editKeyProgress.value = ''
  editCommonIssues.value = ''
  editNextWeekPlans.value = ''
  editCoordinationItems.value = ''
}

function buildContentFromFields() {
  return JSON.stringify({
    overview: editOverview.value,
    keyProgress: editKeyProgress.value,
    commonIssues: editCommonIssues.value,
    nextWeekPlans: editNextWeekPlans.value,
    coordinationItems: editCoordinationItems.value,
  })
}

function formatContent(text) {
  if (!text) return ''
  try {
    const obj = JSON.parse(text)
    if (typeof obj === 'object') {
      let html = ''
      if (obj.overview) html += '<h3>本周工作概览</h3><p>' + obj.overview + '</p>'
      if (obj.keyProgress) html += '<h3>重点工作进展</h3><div>' + obj.keyProgress.replace(/\n/g, '<br>') + '</div>'
      if (obj.commonIssues) html += '<h3>共性问题与风险</h3><div>' + obj.commonIssues.replace(/\n/g, '<br>') + '</div>'
      if (obj.nextWeekPlans) html += '<h3>下周重点计划</h3><div>' + obj.nextWeekPlans.replace(/\n/g, '<br>') + '</div>'
      if (obj.coordinationItems) html += '<h3>需要协调的事项</h3><div>' + obj.coordinationItems.replace(/\n/g, '<br>') + '</div>'
      return html
    }
  } catch { /* not JSON */ }
  return text.replace(/\n/g, '<br>')
}

function parseLine(line) {
  if (!line) return { item: '', progress: '' }
  const idx = line.indexOf('|')
  if (idx === -1) return { item: line, progress: '' }
  return { item: line.substring(0, idx), progress: line.substring(idx + 1) }
}

function renderItems(text) {
  if (!text) return []
  return text.split('\n').filter(s => s.trim()).map(parseLine)
}

function getWeekMonday() {
  const now = new Date()
  const day = now.getDay()
  const diff = day === 0 ? -6 : 1 - day
  const monday = new Date(now.getFullYear(), now.getMonth(), now.getDate() + diff)
  return monday.toISOString().split('T')[0]
}

function getWeekFriday(monday) {
  const d = new Date(monday)
  d.setDate(d.getDate() + 4)
  return d.toISOString().split('T')[0]
}

function selectReport(row) {
  selectedReport.value = row
  selectedReportId.value = row.id
  mergeResult.value = null
}

async function handleMerge() {
  if (reports.value.length === 0) {
    ElMessage.warning('暂无已提交的组员周报')
    return
  }
  const monday = getWeekMonday()
  merging.value = true
  try {
    const res = await mergeTeamSummary({
      weekStartDate: monday,
      weekEndDate: getWeekFriday(monday)
    })
    mergeResult.value = res.data
    editingId.value = res.data?.id
    editing.value = false
    selectedReport.value = null
    selectedReportId.value = null
    parseContentToFields(res.data?.mergedContent || '')
    ElMessage.success('AI 汇总完成')
    fetchSummaries()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || 'AI 汇总失败')
  } finally {
    merging.value = false
  }
}

async function handleEditSave() {
  if (!editingId.value) return
  editSaving.value = true
  try {
    const res = await updateTeamSummary(editingId.value, { editedContent: buildContentFromFields() })
    mergeResult.value = res.data
    editing.value = false
    ElMessage.success('已保存')
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '保存失败')
  } finally {
    editSaving.value = false
  }
}

async function handleSubmit() {
  if (!editingId.value) return
  submitLoading.value = true
  try {
    await submitTeamSummary(editingId.value)
    ElMessage.success('已提交至部门文书')
    mergeResult.value = null
    editingId.value = null
    editing.value = false
    emit('submitted')
    fetchSummaries()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '提交失败')
  } finally {
    submitLoading.value = false
  }
}

function startEdit() {
  editing.value = true
}

function viewSummary(row) {
  mergeResult.value = row
  editingId.value = row.id
  editing.value = false
  selectedReport.value = null
  selectedReportId.value = null
  parseContentToFields(row.editedContent || row.mergedContent || '')
}

function statusText(s) {
  if (s === 'DRAFT') return '草稿'
  if (s === 'SUBMITTED') return '已提交'
  return s || '-'
}

onMounted(() => {
  fetchTeams()
  fetchReports()
  fetchSummaries()
})
</script>

<template>
  <div v-loading="loading || merging" class="team-layout">
    <!-- Left sidebar -->
    <div class="team-side">
      <div class="team-side-header">
        <el-button type="primary" size="small" :disabled="reports.length === 0" @click="handleMerge" :loading="merging">
          AI 汇总组内周报
        </el-button>
      </div>

      <div v-if="reports.length === 0 && !loading" class="empty-small">暂无已提交周报</div>

      <!-- Reports grouped by team -->
      <div v-for="group in groupedReports" :key="group.teamName" class="team-group">
        <div class="team-group-header" v-if="group.teamName">
          <span class="team-group-name">{{ group.teamName }}</span>
          <span class="team-group-count">{{ group.members.length }}人</span>
        </div>
        <div
          v-for="r in group.members"
          :key="r.id"
          class="team-item"
          :class="{ active: selectedReportId === r.id }"
          @click="selectReport(r)"
        >
          <div class="team-item-name">{{ r.userName }}</div>
          <div class="team-item-dept">{{ r.userDepartment }}</div>
          <div class="team-item-week">{{ formatDate(r.weekStartDate) }} ~ {{ formatDate(r.weekEndDate) }}</div>
          <el-tag size="small" type="success">已提交</el-tag>
        </div>
      </div>

      <!-- History summaries -->
      <div v-if="summaries.length > 0" class="history-section">
        <h4 class="history-title">历史汇总</h4>
        <div
          v-for="s in summaries"
          :key="'s' + s.id"
          class="team-item"
          :class="{ active: mergeResult && mergeResult.id === s.id }"
          @click="viewSummary(s)"
        >
          <div class="team-item-name">{{ s.teamName }}</div>
          <div class="team-item-week">{{ formatDate(s.weekStartDate) }} ~ {{ formatDate(s.weekEndDate) }}</div>
          <el-tag :type="s.status === 'SUBMITTED' ? 'success' : 'info'" size="small">{{ statusText(s.status) }}</el-tag>
        </div>
      </div>
    </div>

    <!-- Right main area -->
    <div class="team-main">
      <!-- Member report detail -->
      <div v-if="selectedReport && !mergeResult">
        <div class="detail-header">
          <h3>{{ selectedReport.userName }} 的周报</h3>
          <div class="detail-header-meta">
            <span>{{ selectedReport.userDepartment }}</span>
            <span>{{ formatDate(selectedReport.weekStartDate) }} ~ {{ formatDate(selectedReport.weekEndDate) }}</span>
            <el-tag size="small" type="success">已提交</el-tag>
            <span v-if="selectedReport.version > 1" class="version-badge">V{{ selectedReport.version }}</span>
          </div>
        </div>

        <div class="report-card">
          <div class="card-work">
            <div v-if="selectedReport.doneWork" class="work-section">
              <h4>本周完成工作：</h4>
              <table class="work-table" v-if="renderItems(selectedReport.doneWork).length">
                <thead><tr><th style="width:40px">序号</th><th style="width:180px">事项</th><th>进展情况</th></tr></thead>
                <tbody>
                  <tr v-for="(it, i) in renderItems(selectedReport.doneWork)" :key="i">
                    <td style="text-align:center">{{ i + 1 }}</td>
                    <td>{{ it.item }}</td>
                    <td>{{ it.progress || '-' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-if="selectedReport.planWork" class="work-section">
              <h4>下周工作计划：</h4>
              <table class="work-table" v-if="renderItems(selectedReport.planWork).length">
                <thead><tr><th style="width:40px">序号</th><th style="width:180px">事项</th><th>进展情况</th></tr></thead>
                <tbody>
                  <tr v-for="(it, i) in renderItems(selectedReport.planWork)" :key="i">
                    <td style="text-align:center">{{ i + 1 }}</td>
                    <td>{{ it.item }}</td>
                    <td>{{ it.progress || '-' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-if="selectedReport.problems" class="work-section">
              <h4>遇到的问题：</h4>
              <p>{{ selectedReport.problems }}</p>
            </div>
            <div v-if="selectedReport.supportNeeded" class="work-section">
              <h4>需要支持：</h4>
              <p>{{ selectedReport.supportNeeded }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Merge result view -->
      <div v-else-if="mergeResult">
        <div class="merge-header">
          <span class="merge-title">
            {{ mergeResult.teamName }} · {{ formatDate(mergeResult.weekStartDate) }} ~ {{ formatDate(mergeResult.weekEndDate) }}
          </span>
          <el-tag v-if="mergeResult.status === 'SUBMITTED'" type="success" size="small">已提交</el-tag>
          <el-tag v-else type="info" size="small">草稿</el-tag>
        </div>

        <div class="merge-body">
          <div class="merge-actions">
            <h4>汇总内容</h4>
            <div v-if="mergeResult.status !== 'SUBMITTED'" class="merge-btn-group">
              <template v-if="!editing">
                <el-button size="small" @click="startEdit">编辑润色</el-button>
                <el-button type="primary" size="small" :loading="submitLoading" @click="handleSubmit">提交至部门文书</el-button>
              </template>
              <template v-else>
                <el-button size="small" :loading="editSaving" @click="handleEditSave">保存修改</el-button>
                <el-button size="small" @click="editing = false">取消</el-button>
              </template>
            </div>
          </div>

          <div v-if="editing" class="edit-body">
            <div class="edit-section">
              <h3>本周工作概览</h3>
              <el-input v-model="editOverview" type="textarea" :rows="3" placeholder="2-3句话概述组内本周整体工作情况" />
            </div>
            <div class="edit-section">
              <h3>重点工作进展</h3>
              <el-input v-model="editKeyProgress" type="textarea" :rows="8" placeholder="按项目/方向归类的关键进展" />
            </div>
            <div class="edit-section">
              <h3>共性问题与风险</h3>
              <el-input v-model="editCommonIssues" type="textarea" :rows="5" placeholder="跨团队高频问题和潜在风险" />
            </div>
            <div class="edit-section">
              <h3>下周重点计划</h3>
              <el-input v-model="editNextWeekPlans" type="textarea" :rows="5" placeholder="下周重点工作方向" />
            </div>
            <div class="edit-section">
              <h3>需要协调的事项</h3>
              <el-input v-model="editCoordinationItems" type="textarea" :rows="4" placeholder="需要跨组协调的事项列表" />
            </div>
          </div>

          <div v-else class="summary-html" v-html="formatContent(mergeResult.editedContent || mergeResult.mergedContent)"></div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else class="empty-state">
        请选择左侧组员周报查看详情，或点击"AI 汇总组内周报"生成本周汇总
      </div>
    </div>
  </div>
</template>

<style scoped>
.team-layout {
  display: flex;
  gap: 20px;
  min-height: 400px;
}

.team-side {
  width: 250px;
  flex-shrink: 0;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.team-side-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.team-group {
  margin-bottom: 12px;
}

.team-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 6px 4px 0;
  font-size: 12px;
}

.team-group-name {
  font-weight: 600;
  color: #303133;
  font-size: 13px;
}

.team-group-count {
  color: #909399;
  font-size: 11px;
}

.team-item {
  padding: 6px 10px;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.15s;
  border: 1px solid transparent;
}

.team-item:hover {
  background: #f0f2f5;
}

.team-item.active {
  background: #ecf5ff;
  border-color: #b3d8ff;
}

.team-item-name {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}

.team-item-dept {
  font-size: 11px;
  color: #909399;
  margin-bottom: 2px;
}

.team-item-week {
  font-size: 11px;
  color: #909399;
  margin-bottom: 4px;
}

.empty-small {
  text-align: center;
  color: #c0c4cc;
  padding: 20px 0;
  font-size: 13px;
}

.history-section {
  margin-top: 16px;
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}

.history-title {
  font-size: 13px;
  color: #606266;
  margin: 0 0 8px 0;
}

.team-main {
  flex: 1;
  min-width: 0;
}

.detail-header {
  margin-bottom: 16px;
}

.detail-header h3 {
  margin: 0 0 6px 0;
  font-size: 16px;
}

.detail-header-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.report-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 14px 16px;
  background: #fff;
}

.card-work {
  background: #fafafa;
  border-radius: 4px;
  padding: 10px 14px;
}

.work-section h4 {
  font-size: 13px;
  margin: 0 0 4px 0;
}

.work-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.work-table th,
.work-table td {
  border: 1px solid #e4e7ed;
  padding: 4px 8px;
  text-align: left;
  vertical-align: top;
  word-break: break-all;
}

.work-table th {
  background: #f5f7fa;
  font-weight: 600;
  color: #606266;
}

.work-section p {
  margin: 0;
  font-size: 13px;
  color: #303133;
  line-height: 1.6;
}

.version-badge {
  font-size: 11px;
  color: #909399;
  background: #f0f0f0;
  padding: 1px 6px;
  border-radius: 3px;
}

.merge-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.merge-title {
  font-weight: 600;
  font-size: 15px;
}

.merge-body {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
}

.merge-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.merge-actions h4 {
  margin: 0;
  font-size: 14px;
}

.merge-btn-group {
  display: flex;
  gap: 8px;
}

.edit-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edit-section h3 {
  margin: 0 0 6px 0;
  font-size: 14px;
  color: #303133;
}

.summary-html :deep(h3) {
  font-size: 15px;
  color: #303133;
  margin: 16px 0 8px 0;
  padding-bottom: 4px;
  border-bottom: 1px solid #ebeef5;
}

.summary-html :deep(h3:first-child) {
  margin-top: 0;
}

.summary-html :deep(p) {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #303133;
  line-height: 1.7;
}

.summary-html :deep(div) {
  font-size: 14px;
  color: #303133;
  line-height: 1.7;
  margin-bottom: 8px;
}

.empty-state {
  text-align: center;
  color: #909399;
  padding: 80px 0;
  font-size: 15px;
}
</style>
