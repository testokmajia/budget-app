<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { mergeReports, getDeptReports, getDeptReport, updateDeptReport, submitDeptReport, finalizeDeptReport, exportWord, exportHtml, listTeamSummariesByWeek } from '@/api/weekly'

const userStore = useUserStore()
const isHead = ref(userStore.hasRole('ROLE_ADMIN'))

const loading = ref(false)
const reports = ref([])
const teamSummaries = ref([])
const selectedType = ref('') // 'team' | 'dept'
const selectedTeamSummary = ref(null)
const selectedDeptReport = ref(null)

const editing = ref(false)
const saving = ref(false)
const merging = ref(false)
const submitLoading = ref(false)

const editOverview = ref('')
const editKeyProgress = ref('')
const editCommonIssues = ref('')
const editNextWeekPlans = ref('')
const editCoordinationItems = ref('')

const statusLabels = { 'DRAFT': 'AI初稿', 'PENDING_REVIEW': '文书已编辑', 'FINALIZED': '负责人已审定' }
const statusTypes = { 'DRAFT': 'info', 'PENDING_REVIEW': 'warning', 'FINALIZED': 'success' }

function formatDate(d) {
  if (!d) return '-'
  return typeof d === 'string' ? d.split('T')[0] : d
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

function getMonday() {
  const d = new Date()
  const day = d.getDay() || 7
  d.setDate(d.getDate() - day + 1)
  return d.toISOString().split('T')[0]
}
function getFriday(m) {
  const d = new Date(m)
  d.setDate(d.getDate() + 4)
  return d.toISOString().split('T')[0]
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getDeptReports()
    reports.value = res.data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '加载失败')
  } finally {
    loading.value = false
  }
}

async function fetchTeamSummaries() {
  try {
    const monday = getMonday()
    const res = await listTeamSummariesByWeek(monday)
    teamSummaries.value = res.data || []
  } catch { teamSummaries.value = [] }
}

function selectTeamSummary(ts) {
  selectedTeamSummary.value = ts
  selectedDeptReport.value = null
  selectedType.value = 'team'
  editing.value = false
}

async function selectDeptReport(row) {
  loading.value = true
  try {
    const res = await getDeptReport(row.id)
    selectedDeptReport.value = res.data
    selectedTeamSummary.value = null
    selectedType.value = 'dept'
    parseContentToFields(res.data.editedContent || res.data.mergedContent || '')
    editing.value = false
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleMerge() {
  await ElMessageBox.confirm('将调取AI对本周所有已审批周报进行智能合并，确认生成？', '提示', { type: 'warning' })
  merging.value = true
  try {
    const monday = getMonday()
    const res = await mergeReports({ weekStartDate: monday, weekEndDate: getFriday(monday) })
    ElMessage.success('AI合并完成')
    selectedDeptReport.value = res.data
    selectedTeamSummary.value = null
    selectedType.value = 'dept'
    parseContentToFields(res.data.mergedContent || '')
    editing.value = false
    fetchList()
    fetchTeamSummaries()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || 'AI合并失败')
  } finally {
    merging.value = false
  }
}

function startEdit() {
  const raw = selectedDeptReport.value?.editedContent || selectedDeptReport.value?.mergedContent || ''
  parseContentToFields(raw)
  editing.value = true
}

async function handleSave() {
  saving.value = true
  try {
    await updateDeptReport(selectedDeptReport.value.id, { editedContent: buildContentFromFields() })
    ElMessage.success('已保存')
    editing.value = false
    const res = await getDeptReport(selectedDeptReport.value.id)
    selectedDeptReport.value = res.data
    parseContentToFields(res.data.editedContent || res.data.mergedContent || '')
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSubmit() {
  await ElMessageBox.confirm('确认将编辑后的部门周报提交给负责人审定？', '提示', { type: 'warning' })
  try {
    if (editing.value) await handleSave()
    await submitDeptReport(selectedDeptReport.value.id)
    ElMessage.success('已提交给负责人')
    const res = await getDeptReport(selectedDeptReport.value.id)
    selectedDeptReport.value = res.data
    editing.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '提交失败')
  }
}

async function handleFinalize() {
  await ElMessageBox.confirm('确认审定通过该部门周报？', '审定', { type: 'warning' })
  submitLoading.value = true
  try {
    await finalizeDeptReport(selectedDeptReport.value.id)
    ElMessage.success('已审定')
    const res = await getDeptReport(selectedDeptReport.value.id)
    selectedDeptReport.value = res.data
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleExport(type) {
  try {
    let blob
    if (type === 'word') {
      const res = await exportWord(selectedDeptReport.value.id)
      blob = res
    } else {
      const res = await exportHtml(selectedDeptReport.value.id)
      blob = new Blob([res], { type: 'text/html' })
    }
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = type === 'word' ? '部门周报.doc' : '部门周报.html'
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('导出失败')
  }
}

function isTeamActive(id) { return selectedType.value === 'team' && selectedTeamSummary.value?.id === id }
function isDeptActive(id) { return selectedType.value === 'dept' && selectedDeptReport.value?.id === id }

onMounted(() => {
  fetchList()
  fetchTeamSummaries()
})
</script>

<template>
  <div v-loading="loading || merging" class="dept-layout">
    <!-- Left sidebar -->
    <div class="dept-side">
      <div class="dept-side-header">
        <h4>部门周报</h4>
        <el-button type="primary" size="small" @click="handleMerge" :loading="merging">AI 生成本周汇总</el-button>
      </div>

      <div v-if="reports.length === 0 && teamSummaries.length === 0 && !loading" class="empty-small">暂无记录</div>

      <!-- Team summaries for current week -->
      <div v-if="teamSummaries.length > 0" class="side-section">
        <h4 class="side-section-title">各组周报 ({{ teamSummaries.length }})</h4>
        <div
          v-for="ts in teamSummaries"
          :key="'ts' + ts.id"
          class="side-item"
          :class="{ active: isTeamActive(ts.id) }"
          @click="selectTeamSummary(ts)"
        >
          <div class="side-item-name">{{ ts.teamName }}</div>
          <div class="side-item-sub">{{ ts.leaderName }}</div>
          <el-tag :type="ts.status === 'SUBMITTED' ? 'success' : 'info'" size="small">{{ ts.status === 'SUBMITTED' ? '已提交' : '草稿' }}</el-tag>
        </div>
      </div>

      <!-- Department reports history -->
      <div v-if="reports.length > 0" class="side-section">
        <h4 class="side-section-title">部门汇总记录</h4>
        <div
          v-for="r in reports"
          :key="'dr' + r.id"
          class="side-item"
          :class="{ active: isDeptActive(r.id) }"
          @click="selectDeptReport(r)"
        >
          <div class="side-item-week">{{ formatDate(r.weekStartDate) }} ~ {{ formatDate(r.weekEndDate) }}</div>
          <el-tag :type="statusTypes[r.status] || 'info'" size="small">{{ statusLabels[r.status] || r.status }}</el-tag>
        </div>
      </div>
    </div>

    <!-- Right main area -->
    <div class="dept-main">
      <!-- Team summary detail -->
      <template v-if="selectedType === 'team' && selectedTeamSummary">
        <div class="merge-header">
          <span class="merge-title">{{ selectedTeamSummary.teamName }} · {{ formatDate(selectedTeamSummary.weekStartDate) }} ~ {{ formatDate(selectedTeamSummary.weekEndDate) }}</span>
          <el-tag :type="selectedTeamSummary.status === 'SUBMITTED' ? 'success' : 'info'" size="small">{{ selectedTeamSummary.status === 'SUBMITTED' ? '已提交' : '草稿' }}</el-tag>
        </div>
        <div class="merge-body">
          <h4 style="margin:0 0 12px 0;">组长：{{ selectedTeamSummary.leaderName }}</h4>
          <div class="summary-html" v-html="formatContent(selectedTeamSummary.editedContent || selectedTeamSummary.mergedContent)"></div>
        </div>
      </template>

      <!-- Department report detail -->
      <template v-else-if="selectedType === 'dept' && selectedDeptReport">
        <div class="merge-header">
          <span class="merge-title">
            {{ formatDate(selectedDeptReport.weekStartDate) }} ~ {{ formatDate(selectedDeptReport.weekEndDate) }} 部门周报
          </span>
          <el-tag :type="statusTypes[selectedDeptReport.status] || 'info'" size="small">{{ statusLabels[selectedDeptReport.status] || selectedDeptReport.status }}</el-tag>
        </div>

        <div class="merge-body">
          <div class="merge-actions">
            <h4>汇总内容</h4>
            <div class="merge-btn-group">
              <template v-if="selectedDeptReport.status === 'DRAFT' && !editing">
                <el-button size="small" @click="startEdit">编辑润色</el-button>
                <el-button type="primary" size="small" @click="handleSubmit">提交负责人</el-button>
              </template>
              <template v-if="editing">
                <el-button size="small" :loading="saving" @click="handleSave">保存修改</el-button>
                <el-button size="small" @click="editing = false">取消</el-button>
              </template>
              <template v-if="selectedDeptReport.status === 'PENDING_REVIEW' && isHead">
                <el-button type="success" size="small" :loading="submitLoading" @click="handleFinalize">审定通过</el-button>
              </template>
              <template v-if="selectedDeptReport.status === 'FINALIZED'">
                <el-button size="small" @click="handleExport('word')">导出Word</el-button>
                <el-button size="small" @click="handleExport('html')">导出HTML</el-button>
              </template>
            </div>
          </div>

          <div v-if="editing" class="edit-body">
            <div class="edit-section">
              <h3>本周工作概览</h3>
              <el-input v-model="editOverview" type="textarea" :rows="3" placeholder="2-3句话概述部门本周整体工作情况" />
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

          <div v-else class="summary-html" v-html="formatContent(selectedDeptReport.editedContent || selectedDeptReport.mergedContent)"></div>
        </div>
      </template>

      <!-- Empty state -->
      <div v-else class="empty-state">
        请选择左侧记录查看详情，或点击"AI 生成本周汇总"创建新的部门周报
      </div>
    </div>
  </div>
</template>

<style scoped>
.dept-layout {
  display: flex;
  gap: 20px;
  min-height: 400px;
}

.dept-side {
  width: 250px;
  flex-shrink: 0;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.dept-side-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.dept-side-header h4 {
  margin: 0;
  font-size: 14px;
  color: #303133;
}

.side-section {
  margin-bottom: 16px;
}

.side-section-title {
  font-size: 13px;
  color: #606266;
  margin: 0 0 6px 0;
  padding-bottom: 4px;
  border-bottom: 1px solid #ebeef5;
}

.side-item {
  padding: 6px 10px;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.15s;
  border: 1px solid transparent;
}

.side-item:hover {
  background: #f0f2f5;
}

.side-item.active {
  background: #ecf5ff;
  border-color: #b3d8ff;
}

.side-item-name {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}

.side-item-sub {
  font-size: 11px;
  color: #909399;
  margin-bottom: 4px;
}

.side-item-week {
  font-size: 13px;
  color: #303133;
  margin-bottom: 4px;
}

.empty-small {
  text-align: center;
  color: #c0c4cc;
  padding: 20px 0;
  font-size: 13px;
}

/* Right main area */
.dept-main {
  flex: 1;
  min-width: 0;
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
