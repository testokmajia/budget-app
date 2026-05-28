<script setup>
import { ref, watch, onMounted, inject } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getCurrentWeekReport, saveReport, submitReport, getSmartDraft, getMyReports } from '@/api/weekly'

const userStore = useUserStore()
const refreshBadges = inject('refreshBadges', null)

const loading = ref(false)
const submittedStatus = ref('')
const reportId = ref(null)
const reportVersion = ref(1)
const weekLabel = ref('')

const doneItems = ref([{ item: '', progress: '' }])
const planItems = ref([{ item: '', progress: '' }])
const problems = ref('')
const supportNeeded = ref('')

const LS_KEY = `weekly_draft_${userStore.user?.id}`

function parseItems(text) {
  if (!text) return [{ item: '', progress: '' }]
  const items = text.split('\n').filter(s => s.trim()).map(line => {
    const idx = line.indexOf('|')
    if (idx === -1) return { item: line, progress: '' }
    return { item: line.substring(0, idx).trim(), progress: line.substring(idx + 1).trim() }
  })
  return items.length ? items : [{ item: '', progress: '' }]
}

function serializeItems(items) {
  return items.filter(s => s.item.trim()).map(s => `${s.item.trim()}|${s.progress.trim()}`).join('\n')
}

function loadFromLocalStorage() {
  try {
    const raw = localStorage.getItem(LS_KEY)
    if (raw) {
      const data = JSON.parse(raw)
      if (data.doneItems) doneItems.value = data.doneItems.length ? data.doneItems : [{ item: '', progress: '' }]
      if (data.planItems) planItems.value = data.planItems.length ? data.planItems : [{ item: '', progress: '' }]
      if (data.problems) problems.value = data.problems
      if (data.supportNeeded) supportNeeded.value = data.supportNeeded
    }
  } catch { /* ignore */ }
}

function saveToLocalStorage() {
  try {
    localStorage.setItem(LS_KEY, JSON.stringify({
      doneItems: doneItems.value,
      planItems: planItems.value,
      problems: problems.value,
      supportNeeded: supportNeeded.value,
    }))
  } catch { /* ignore */ }
}

let saveTimer = null
watch([doneItems, planItems, problems, supportNeeded], () => {
  if (submittedStatus.value && submittedStatus.value !== 'DRAFT' && submittedStatus.value !== 'REJECTED') return
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(saveToLocalStorage, 2000)
}, { deep: true })

function addItem(list) {
  list.push({ item: '', progress: '' })
}
function removeItem(list, index) {
  if (list.length > 1) list.splice(index, 1)
}

function onDragStart(e, index) {
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', String(index))
}
function onDragOver(e) {
  e.preventDefault()
  e.dataTransfer.dropEffect = 'move'
}
function onDrop(e, list) {
  e.preventDefault()
  const from = parseInt(e.dataTransfer.getData('text/plain'), 10)
  const toEl = e.target.closest('.drag-row')
  if (!toEl) return
  const to = parseInt(toEl.dataset.index, 10)
  if (from !== to && to >= 0 && to < list.length) {
    const item = list.splice(from, 1)[0]
    list.splice(to, 0, item)
  }
}

function buildPayload() {
  const monday = getMonday()
  return {
    weekStartDate: monday,
    weekEndDate: getFriday(monday),
    doneWork: serializeItems(doneItems.value),
    planWork: serializeItems(planItems.value),
    problems: problems.value,
    supportNeeded: supportNeeded.value,
  }
}

async function handleSave() {
  loading.value = true
  try {
    const res = await saveReport(buildPayload())
    submittedStatus.value = res.data?.status || 'DRAFT'
    reportId.value = res.data?.id
    reportVersion.value = res.data?.version || 1
    localStorage.removeItem(LS_KEY)
    ElMessage.success('草稿已保存')
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '保存失败')
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  const confirmMsg = isDraft() ? '提交后组长将汇总组内周报，确认提交？' : '将生成新的周报版本并重新提交，确认？'
  await ElMessageBox.confirm(confirmMsg, '提示', { type: 'warning' })
  const payload = buildPayload()
  if (!payload.doneWork && !payload.planWork) {
    ElMessage.warning('请至少填写本周完成工作或下周工作计划')
    return
  }
  loading.value = true
  try {
    const res = await submitReport(payload)
    submittedStatus.value = res.data?.status || 'SUBMITTED'
    reportId.value = res.data?.id
    reportVersion.value = res.data?.version || 1
    localStorage.removeItem(LS_KEY)
    refreshBadges?.()
    ElMessage.success('已提交')
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '提交失败')
  } finally {
    loading.value = false
  }
}

async function handleSmartDraft() {
  try {
    const res = await getSmartDraft()
    const data = res.data
    if (data && data.hasLastWeek && data.suggestedDoneWork) {
      const items = parseItems(data.suggestedDoneWork)
      if (items.length && items[0].item) {
        doneItems.value = items
        ElMessage.success('已从上周计划导入初稿，请确认调整后提交')
        return
      }
    }
    ElMessage.info('暂无上周计划数据')
  } catch { ElMessage.info('暂无上周计划数据') }
}

function getMonday() {
  const d = new Date()
  const day = d.getDay() || 7
  d.setDate(d.getDate() - day + 1)
  return d.toISOString().split('T')[0]
}
function getFriday(monday) {
  const d = new Date(monday)
  d.setDate(d.getDate() + 4)
  return d.toISOString().split('T')[0]
}
function formatDateRange() {
  const m = getMonday()
  const f = getFriday(m)
  weekLabel.value = `${m} (周一) ~ ${f} (周五)`
}

const statusLabels = {
  'DRAFT': '草稿',
  'SUBMITTED': '已提交',
  'APPROVED': '已通过',
  'REJECTED': '已驳回',
}
const statusTypes = {
  'DRAFT': 'info',
  'SUBMITTED': 'warning',
  'APPROVED': 'success',
  'REJECTED': 'danger',
}

async function init() {
  formatDateRange()
  try {
    const res = await getCurrentWeekReport()
    const data = res.data
    if (data) {
      reportId.value = data.id
      submittedStatus.value = data.status || ''
      reportVersion.value = data.version || 1
      if (data.doneWork) {
        doneItems.value = parseItems(data.doneWork)
      }
      if (data.planWork) {
        planItems.value = parseItems(data.planWork)
      }
      if (data.problems) problems.value = data.problems
      if (data.supportNeeded) supportNeeded.value = data.supportNeeded
      if (data.weekStartDate) {
        weekLabel.value = `${data.weekStartDate} (周一) ~ ${data.weekEndDate} (周五)`
      }
      if (data.status && data.status !== 'DRAFT' && data.status !== 'REJECTED') {
        return
      }
    }
  } catch { /* ignore */ }
  loadFromLocalStorage()
}

const isDraft = () => !submittedStatus.value || submittedStatus.value === 'DRAFT' || submittedStatus.value === 'REJECTED'
const showVersion = () => reportVersion.value > 1

// Personal history
const historyLoading = ref(false)
const historyData = ref([])

function formatHistoryDate(d) {
  if (!d) return '-'
  return typeof d === 'string' ? d.split('T')[0] : d
}

function formatHistoryDateTime(d) {
  if (!d) return '-'
  return typeof d === 'string' ? d.replace('T', ' ') : d
}

function historyParseLine(line) {
  if (!line) return { item: '', progress: '' }
  const idx = line.indexOf('|')
  if (idx === -1) return { item: line, progress: '' }
  return { item: line.substring(0, idx), progress: line.substring(idx + 1) }
}

function historyRenderItems(text) {
  if (!text) return []
  return text.split('\n').filter(s => s.trim()).map(historyParseLine)
}

async function fetchHistory() {
  historyLoading.value = true
  try {
    const res = await getMyReports({})
    historyData.value = res.data || []
  } catch { historyData.value = [] }
  finally { historyLoading.value = false }
}

const historyStatusLabels = { 'DRAFT': '草稿', 'SUBMITTED': '已提交', 'APPROVED': '已通过', 'REJECTED': '已驳回' }
const historyStatusTypes = { 'DRAFT': 'info', 'SUBMITTED': 'warning', 'APPROVED': 'success', 'REJECTED': 'danger' }

onMounted(() => {
  init()
  fetchHistory()
})
</script>

<template>
  <div v-loading="loading">
    <div class="week-label">{{ weekLabel }}</div>

    <div v-if="submittedStatus" class="status-bar">
      <el-tag :type="statusTypes[submittedStatus] || 'info'" size="default">
        {{ statusLabels[submittedStatus] || submittedStatus }}
      </el-tag>
      <span v-if="showVersion()" class="version-badge">V{{ reportVersion }}</span>
    </div>

    <div class="section">
      <div class="section-header">
        <h3>本周完成工作</h3>
        <el-button :icon="Plus" size="small" @click="addItem(doneItems)">添加</el-button>
      </div>
      <div class="work-table">
        <div class="work-header">
          <span class="col-seq">序号</span>
          <span class="col-item">事项</span>
          <span class="col-progress">进展情况</span>
          <span class="col-action"></span>
        </div>
        <div
          class="work-body"
          @dragover="onDragOver"
          @drop="e => onDrop(e, doneItems)"
        >
          <div
            v-for="(row, i) in doneItems"
            :key="i"
            class="drag-row"
            :data-index="i"
            draggable="true"
            @dragstart="e => onDragStart(e, i)"
          >
            <span class="col-seq">
              <span class="drag-handle">⠿</span>
              <span>{{ i + 1 }}</span>
            </span>
            <el-input
              v-model="row.item"
              class="col-item"
                            placeholder="事项（30字以内）"
              maxlength="30"
            />
            <el-input
              v-model="row.progress"
              class="col-progress"
                            placeholder="进展情况"
            />
            <span class="col-action">
              <el-button
                v-if="doneItems.length > 1"
                :icon="Delete"
                size="small"
                type="danger"
                text
                @click="removeItem(doneItems, i)"
              />
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="section">
      <div class="section-header">
        <h3>下周工作计划</h3>
        <el-button :icon="Plus" size="small" @click="addItem(planItems)">添加</el-button>
      </div>
      <div class="work-table">
        <div class="work-header">
          <span class="col-seq">序号</span>
          <span class="col-item">事项</span>
          <span class="col-progress">进展情况</span>
          <span class="col-action"></span>
        </div>
        <div
          class="work-body"
          @dragover="onDragOver"
          @drop="e => onDrop(e, planItems)"
        >
          <div
            v-for="(row, i) in planItems"
            :key="i"
            class="drag-row"
            :data-index="i"
            draggable="true"
            @dragstart="e => onDragStart(e, i)"
          >
            <span class="col-seq">
              <span class="drag-handle">⠿</span>
              <span>{{ i + 1 }}</span>
            </span>
            <el-input
              v-model="row.item"
              class="col-item"
                            placeholder="事项（30字以内）"
              maxlength="30"
            />
            <el-input
              v-model="row.progress"
              class="col-progress"
                            placeholder="进展情况"
            />
            <span class="col-action">
              <el-button
                v-if="planItems.length > 1"
                :icon="Delete"
                size="small"
                type="danger"
                text
                @click="removeItem(planItems, i)"
              />
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="section">
      <h3>遇到的问题</h3>
      <el-input
        v-model="problems"
        type="textarea"
        :rows="3"
                placeholder="需要协调的事项或遇到的困难（选填）"
        maxlength="500"
        show-word-limit
      />
    </div>

    <div class="section">
      <h3>需要支持</h3>
      <el-input
        v-model="supportNeeded"
        type="textarea"
        :rows="3"
                placeholder="需要上级或跨组协调的事项（选填）"
        maxlength="500"
        show-word-limit
      />
    </div>

    <div class="actions">
      <el-button @click="handleSave" :loading="loading">保存草稿</el-button>
      <el-button type="primary" @click="handleSmartDraft">智能初稿</el-button>
      <el-button type="success" @click="handleSubmit" :loading="loading">{{ isDraft() ? '提交周报' : '重新提交' }}</el-button>
    </div>

    <!-- Personal history -->
    <div class="history-section" v-if="historyData.length > 0">
      <h3 class="history-heading">我的历史周报</h3>
      <el-table :data="historyData" v-loading="historyLoading" size="small" stripe>
        <el-table-column label="周期" width="200">
          <template #default="{ row }">
            {{ formatHistoryDate(row.weekStartDate) }} ~ {{ formatHistoryDate(row.weekEndDate) }}
          </template>
        </el-table-column>
        <el-table-column label="版本" width="60" align="center">
          <template #default="{ row }">V{{ row.version || 1 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="historyStatusTypes[row.status] || 'info'" size="small">
              {{ historyStatusLabels[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ formatHistoryDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="history-expand">
              <div v-if="row.doneWork" class="expand-section">
                <h4>本周完成工作：</h4>
                <table class="work-table" v-if="historyRenderItems(row.doneWork).length">
                  <thead><tr><th style="width:40px">序号</th><th style="width:200px">事项</th><th>进展情况</th></tr></thead>
                  <tbody>
                    <tr v-for="(it, i) in historyRenderItems(row.doneWork)" :key="i">
                      <td style="text-align:center">{{ i + 1 }}</td>
                      <td>{{ it.item }}</td>
                      <td>{{ it.progress || '-' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div v-if="row.planWork" class="expand-section">
                <h4>下周工作计划：</h4>
                <table class="work-table" v-if="historyRenderItems(row.planWork).length">
                  <thead><tr><th style="width:40px">序号</th><th style="width:200px">事项</th><th>进展情况</th></tr></thead>
                  <tbody>
                    <tr v-for="(it, i) in historyRenderItems(row.planWork)" :key="i">
                      <td style="text-align:center">{{ i + 1 }}</td>
                      <td>{{ it.item }}</td>
                      <td>{{ it.progress || '-' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div v-if="row.problems" class="expand-section">
                <h4>遇到的问题：</h4>
                <p>{{ row.problems }}</p>
              </div>
              <div v-if="row.supportNeeded" class="expand-section">
                <h4>需要支持：</h4>
                <p>{{ row.supportNeeded }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.week-label {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding: 10px 16px;
  background: #f0f2f5;
  border-radius: 6px;
}
.status-bar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.version-badge {
  font-size: 12px;
  color: #909399;
  background: #f0f2f5;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}
.section {
  margin-bottom: 20px;
}
.section h3 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #303133;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.section-header h3 {
  margin: 0;
}

.work-table {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}
.work-header {
  display: flex;
  align-items: center;
  background: #f5f7fa;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  border-bottom: 1px solid #e4e7ed;
}
.work-body {
  display: flex;
  flex-direction: column;
}
.drag-row {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  gap: 8px;
  border-bottom: 1px solid #ebeef5;
  transition: background 0.15s;
}
.drag-row:last-child {
  border-bottom: none;
}
.drag-row:hover {
  background: #fafafa;
}

.col-seq {
  width: 60px;
  flex-shrink: 0;
  text-align: center;
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.col-item {
  width: 200px;
  flex-shrink: 0;
}
.col-progress {
  flex: 1;
  min-width: 0;
}
.col-action {
  width: 40px;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
}

.drag-handle {
  cursor: grab;
  color: #c0c4cc;
  font-size: 14px;
  user-select: none;
  line-height: 1;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.history-section {
  margin-top: 32px;
  border-top: 2px solid #e4e7ed;
  padding-top: 20px;
}

.history-heading {
  font-size: 15px;
  color: #303133;
  margin: 0 0 12px 0;
}

.history-expand {
  padding: 8px 20px;
  background: #f7f8fa;
  font-size: 13px;
}

.expand-section h4 {
  margin: 8px 0 4px 0;
  font-size: 13px;
}

.expand-section:first-child h4 {
  margin-top: 0;
}

.expand-section p {
  margin: 0;
  line-height: 1.6;
}

.expand-section .work-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 4px;
}

.expand-section .work-table th,
.expand-section .work-table td {
  border: 1px solid #e4e7ed;
  padding: 6px 10px;
  text-align: left;
  vertical-align: top;
}

.expand-section .work-table th {
  background: #f5f7fa;
  font-weight: 600;
  color: #606266;
}
</style>
