<script setup>
import { ref, shallowRef, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { mergeReports, getDeptReports, getDeptReport, updateDeptReport, submitDeptReport, finalizeDeptReport, exportWord, exportHtml } from '@/api/weekly'

const userStore = useUserStore()
const isHead = ref(userStore.hasRole('ROLE_ADMIN'))

const loading = ref(false)
const reports = ref([])
const selectedId = ref(null)
const currentReport = ref(null)

const editing = ref(false)
const editContent = ref('')
const saving = ref(false)
const merging = ref(false)

// structured edit fields
const editOverview = ref('')
const editKeyProgress = ref('')
const editCommonIssues = ref('')
const editNextWeekPlans = ref('')
const editCoordinationItems = ref('')

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

const todayMonday = getMonday()

const statusLabels = { 'DRAFT': 'AI初稿', 'PENDING_REVIEW': '文书已编辑', 'FINALIZED': '负责人已审定' }
const statusTypes = { 'DRAFT': 'info', 'PENDING_REVIEW': 'warning', 'FINALIZED': 'success' }

function formatDate(d) {
  if (!d) return '-'
  return typeof d === 'string' ? d.split('T')[0] : d
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

async function handleMerge() {
  await ElMessageBox.confirm('将调取AI对本周所有已审批周报进行智能合并，确认生成？', '提示', { type: 'warning' })
  merging.value = true
  try {
    const monday = getMonday()
    const res = await mergeReports({ weekStartDate: monday, weekEndDate: getFriday(monday) })
    ElMessage.success('AI合并完成')
    selectedId.value = res.data.id
    currentReport.value = res.data
    editContent.value = res.data.mergedContent || ''
    parseContentToFields(res.data.mergedContent || '')
    editing.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || 'AI合并失败')
  } finally {
    merging.value = false
  }
}

async function selectReport(row) {
  loading.value = true
  try {
    const res = await getDeptReport(row.id)
    currentReport.value = res.data
    selectedId.value = row.id
    const raw = res.data.editedContent || res.data.mergedContent || ''
    editContent.value = raw
    parseContentToFields(raw)
    editing.value = false
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '加载失败')
  } finally {
    loading.value = false
  }
}

function startEdit() {
  const raw = currentReport.value?.editedContent || currentReport.value?.mergedContent || ''
  editContent.value = raw
  parseContentToFields(raw)
  editing.value = true
}

async function handleSave() {
  saving.value = true
  try {
    await updateDeptReport(selectedId.value, { editedContent: buildContentFromFields() })
    ElMessage.success('已保存')
    editing.value = false
    const res = await getDeptReport(selectedId.value)
    currentReport.value = res.data
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
    await submitDeptReport(selectedId.value)
    ElMessage.success('已提交给负责人')
    const res = await getDeptReport(selectedId.value)
    currentReport.value = res.data
    editing.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '提交失败')
  }
}

async function handleFinalize() {
  await ElMessageBox.confirm('确认审定通过该部门周报？', '审定', { type: 'warning' })
  try {
    await finalizeDeptReport(selectedId.value)
    ElMessage.success('已审定')
    const res = await getDeptReport(selectedId.value)
    currentReport.value = res.data
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '操作失败')
  }
}

async function handleExport(type) {
  try {
    let blob
    if (type === 'word') {
      const res = await exportWord(selectedId.value)
      blob = res
    } else {
      const res = await exportHtml(selectedId.value)
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

function formatContent(text) {
  if (!text) return ''
  try {
    const obj = JSON.parse(text)
    if (typeof obj === 'object') {
      let html = ''
      if (obj.overview) html += `<h3>本周工作概览</h3><p>${obj.overview}</p>`
      if (obj.keyProgress) html += `<h3>重点工作进展</h3><div>${obj.keyProgress.replace(/\n/g, '<br>')}</div>`
      if (obj.commonIssues) html += `<h3>共性问题与风险</h3><div>${obj.commonIssues.replace(/\n/g, '<br>')}</div>`
      if (obj.nextWeekPlans) html += `<h3>下周重点计划</h3><div>${obj.nextWeekPlans.replace(/\n/g, '<br>')}</div>`
      if (obj.coordinationItems) html += `<h3>需要部门协调的事项</h3><div>${obj.coordinationItems.replace(/\n/g, '<br>')}</div>`
      return html || text
    }
  } catch { /* not JSON, use as-is */ }
  return text.replace(/\n/g, '<br>')
}

onMounted(fetchList)
</script>

<template>
  <div v-loading="loading || merging" class="dept-layout">
    <div class="dept-side">
      <div class="dept-side-header">
        <h4>部门周报列表</h4>
        <el-button type="primary" size="small" @click="handleMerge" :loading="merging">AI生成本周汇总</el-button>
      </div>
      <div v-if="reports.length === 0" class="empty-small">暂无记录</div>
      <div
        v-for="r in reports"
        :key="r.id"
        class="dept-item"
        :class="{ active: selectedId === r.id }"
        @click="selectReport(r)"
      >
        <div class="dept-item-week">{{ formatDate(r.weekStartDate) }} ~ {{ formatDate(r.weekEndDate) }}</div>
        <el-tag :type="statusTypes[r.status]" size="small">{{ statusLabels[r.status] || r.status }}</el-tag>
      </div>
    </div>

    <div class="dept-main" v-if="currentReport">
      <div class="report-header">
        <div>
          <h3>{{ formatDate(currentReport.weekStartDate) }} ~ {{ formatDate(currentReport.weekEndDate) }} 部门周报</h3>
          <el-tag :type="statusTypes[currentReport.status]">{{ statusLabels[currentReport.status] }}</el-tag>
        </div>
        <div class="report-actions">
          <template v-if="currentReport.status === 'DRAFT' && !editing">
            <el-button type="primary" size="small" @click="startEdit">编辑润色</el-button>
            <el-button type="success" size="small" @click="handleSubmit">提交负责人</el-button>
          </template>
          <template v-if="editing">
            <el-button type="primary" size="small" @click="handleSave" :loading="saving">保存编辑</el-button>
            <el-button size="small" @click="editing = false">取消</el-button>
          </template>
          <template v-if="currentReport.status === 'PENDING_REVIEW' && isHead">
            <el-button type="success" size="small" @click="handleFinalize">审定通过</el-button>
          </template>
          <template v-if="currentReport.status === 'FINALIZED'">
            <el-button size="small" @click="handleExport('word')">导出Word</el-button>
            <el-button size="small" @click="handleExport('html')">导出HTML</el-button>
          </template>
        </div>
      </div>

      <div class="report-body" v-if="editing">
        <div class="edit-section">
          <h3>本周工作概览</h3>
          <el-input v-model="editOverview" type="textarea" :rows="3" placeholder="2-3句话概述部门本周整体工作情况" />
        </div>
        <div class="edit-section">
          <h3>重点工作进展</h3>
          <el-input v-model="editKeyProgress" type="textarea" :rows="8" placeholder="按项目/方向归类的关键进展（支持换行）" />
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
          <h3>需要部门协调的事项</h3>
          <el-input v-model="editCoordinationItems" type="textarea" :rows="4" placeholder="需要跨组协调的事项列表" />
        </div>
      </div>
      <div class="report-body" v-else>
        <div v-html="formatContent(currentReport.editedContent || currentReport.mergedContent)" class="report-html"></div>
      </div>
    </div>

    <div class="dept-main" v-else>
      <div class="empty-state">请选择左侧的部门周报查看，或点击"AI生成本周汇总"创建新的部门周报</div>
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
}
.dept-item {
  padding: 8px 10px;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.15s;
}
.dept-item:hover { background: #f0f2f5; }
.dept-item.active { background: #ecf5ff; }
.dept-item-week { font-size: 13px; color: #303133; margin-bottom: 4px; }
.empty-small { text-align: center; color: #c0c4cc; padding: 20px 0; font-size: 13px; }

.dept-main {
  flex: 1;
  min-width: 0;
}
.report-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}
.report-header h3 { margin: 0 0 8px 0; }
.report-actions { display: flex; gap: 8px; flex-wrap: wrap; }

.report-body {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  min-height: 300px;
}
.report-html :deep(h3) {
  font-size: 15px;
  color: #303133;
  margin: 16px 0 8px 0;
  padding-bottom: 6px;
  border-bottom: 1px solid #ebeef5;
}
.report-html :deep(h3:first-child) { margin-top: 0; }
.report-html :deep(p) { line-height: 1.8; color: #606266; }
.report-html :deep(div) { line-height: 1.8; color: #606266; }
.empty-state { text-align: center; color: #909399; padding: 80px 0; }
.edit-section {
  margin-bottom: 20px;
}
.edit-section:last-child {
  margin-bottom: 0;
}
.edit-section h3 {
  font-size: 14px;
  color: #303133;
  margin: 0 0 8px 0;
  padding-bottom: 6px;
  border-bottom: 1px solid #ebeef5;
}
</style>
