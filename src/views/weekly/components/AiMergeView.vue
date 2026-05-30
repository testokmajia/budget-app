<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { mergeTeamSummary, updateTeamSummary, submitTeamSummary, getTeamStats, getSubmittedList } from '@/api/weekly'

const loading = ref(false)
const merging = ref(false)
const teamStats = ref(null)
const reports = ref([])
const selectedTeam = ref('')
const mergeResult = ref(null)
const editing = ref(false)
const editFields = ref({ overview: '', keyProgress: '', commonIssues: '', nextWeekPlans: '', coordinationItems: '' })

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

async function refresh() {
  loading.value = true
  try {
    const [ts, sr] = await Promise.all([
      getTeamStats().catch(() => ({ data: null })),
      getSubmittedList().catch(() => ({ data: [] }))
    ])
    teamStats.value = ts.data
    reports.value = sr.data || []
    if (teamStats.value?.teams?.length && !selectedTeam.value) {
      selectedTeam.value = teamStats.value.teams[0].teamName
    }
  } finally {
    loading.value = false
  }
}
refresh()

const teams = computed(() => teamStats.value?.teams || [])
const teamNames = computed(() => teams.value.map(t => t.teamName))

const teamReports = computed(() => {
  if (!selectedTeam.value) return []
  return reports.value.filter(r => (r.teamName || r.userDepartment) === selectedTeam.value)
})

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

async function handleMerge() {
  merging.value = true
  mergeResult.value = null
  try {
    const { weekStartDate, weekEndDate } = getWeekDates()
    const res = await mergeTeamSummary({
      teamName: selectedTeam.value,
      weekStartDate,
      weekEndDate
    })
    mergeResult.value = res.data
    const c = parseContent(res.data?.editedContent || res.data?.mergedContent)
    editFields.value = c
    ElMessage.success('AI 合并完成')
  } catch (e) {
    ElMessage.error(e.response?.data?.error || 'AI 合并失败')
  } finally {
    merging.value = false
  }
}

async function handleSave() {
  if (!mergeResult.value) return
  await updateTeamSummary(mergeResult.value.id, { editedContent: JSON.stringify(editFields.value) })
  ElMessage.success('已保存')
  editing.value = false
}

async function handleSubmit() {
  if (!mergeResult.value) return
  await submitTeamSummary(mergeResult.value.id)
  ElMessage.success('已提交至部门文书')
  mergeResult.value = null
  editing.value = false
  refresh()
}
</script>

<template>
  <div v-loading="loading">
    <!-- Team selector -->
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:16px;" v-if="teamNames.length">
      <span style="font-size:12px;font-weight:600;color:var(--gray-500);">当前合并团队：</span>
      <el-tag
        v-for="t in teamNames" :key="t"
        :type="t === selectedTeam ? '' : 'info'"
        size="large"
        style="cursor:pointer;"
        @click="selectedTeam = t"
      >
        {{ t }}
      </el-tag>
      <span style="font-size:11px;color:var(--gray-300);">每组独立生成汇总，互不混淆</span>
    </div>

    <!-- Flow steps -->
    <div class="merge-steps-bar">
      <span class="merge-step done">① 组员提交</span>
      <span class="merge-step-arrow">▸</span>
      <span class="merge-step" :class="merging ? 'active' : mergeResult ? 'done' : 'pending'">② AI 智能合并</span>
      <span class="merge-step-arrow">▸</span>
      <span class="merge-step" :class="editing ? 'active' : mergeResult ? 'done' : 'pending'">③ 组长编辑</span>
      <span class="merge-step-arrow">▸</span>
      <span class="merge-step pending">④ 提交部门</span>
    </div>

    <!-- Merge flow: Source → AI → Result -->
    <div class="merge-flow-grid">
      <!-- Left: Source reports -->
      <div class="merge-panel">
        <div class="merge-panel-header">
          源数据 · {{ selectedTeam }}
          <el-tag size="small" type="warning" style="margin-left:auto;">共 {{ teamReports.length }} 份</el-tag>
        </div>
        <div class="merge-panel-body">
          <div v-for="r in teamReports" :key="r.id" class="merge-source-item">
            <div class="src-name">{{ r.userName }} <span class="src-dept">{{ r.teamName || r.userDepartment }}</span></div>
            <div class="src-summary">
              <span v-if="r.doneWork">完成 {{ (r.doneWork || '').split('\n').filter(l => l.trim()).length }}项</span>
              <span v-if="r.planWork">计划 {{ (r.planWork || '').split('\n').filter(l => l.trim()).length }}项</span>
            </div>
          </div>
          <div v-if="!teamReports.length" style="padding:16px;text-align:center;color:var(--gray-400);font-size:12px;">
            暂无已提交的周报
          </div>
        </div>
      </div>

      <!-- Center: AI icon -->
      <div class="merge-center">
        <div class="merge-ai-icon">🤖</div>
        <div class="merge-center-text">AI 智能合并</div>
        <div class="merge-center-count">{{ teamReports.length }} 份 → 1 份汇总</div>
        <div v-if="merging" class="ai-processing">
          <span style="font-size:11px;color:var(--gray-500);">合并中</span>
          <span class="ai-dot"></span><span class="ai-dot"></span><span class="ai-dot"></span>
        </div>
        <el-button
          v-else
          type="primary"
          style="margin-top:12px;"
          @click="handleMerge"
          :disabled="teamReports.length === 0"
          :loading="merging"
        >
          开始合并
        </el-button>
      </div>

      <!-- Right: Result -->
      <div class="merge-panel result" v-if="mergeResult">
        <div class="merge-panel-header result-header">
          AI 合并结果 · {{ selectedTeam }}
          <el-tag size="small" style="margin-left:auto;">AI 初稿</el-tag>
        </div>
        <div class="merge-panel-body-scroll">
          <template v-if="!editing">
            <h4>📊 本周工作概览</h4>
            <p>{{ editFields.overview }}</p>
            <h4>🔑 重点工作进展</h4>
            <p style="white-space:pre-wrap">{{ editFields.keyProgress }}</p>
            <h4 style="color:var(--red);">⚠️ 共性问题与风险</h4>
            <p style="white-space:pre-wrap">{{ editFields.commonIssues }}</p>
            <h4>📋 下周重点计划</h4>
            <p style="white-space:pre-wrap">{{ editFields.nextWeekPlans }}</p>
            <h4 style="color:var(--brand);">🤝 需要协调的事项</h4>
            <p style="white-space:pre-wrap">{{ editFields.coordinationItems }}</p>
          </template>
          <template v-else>
            <div class="merge-edit-field">
              <label>📊 本周工作概览</label>
              <el-input v-model="editFields.overview" type="textarea" :rows="2" />
            </div>
            <div class="merge-edit-field">
              <label>🔑 重点工作进展</label>
              <el-input v-model="editFields.keyProgress" type="textarea" :rows="5" />
            </div>
            <div class="merge-edit-field">
              <label>⚠️ 共性问题与风险</label>
              <el-input v-model="editFields.commonIssues" type="textarea" :rows="3" />
            </div>
            <div class="merge-edit-field">
              <label>📋 下周重点计划</label>
              <el-input v-model="editFields.nextWeekPlans" type="textarea" :rows="3" />
            </div>
            <div class="merge-edit-field">
              <label>🤝 需要协调的事项</label>
              <el-input v-model="editFields.coordinationItems" type="textarea" :rows="2" />
            </div>
          </template>
        </div>
        <div class="merge-result-actions">
          <el-button v-if="!editing" size="small" @click="editing = true">📝 编辑润色</el-button>
          <el-button v-else size="small" @click="handleSave">💾 保存</el-button>
          <el-button size="small" type="primary" @click="handleSubmit">📤 提交至部门文书</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.merge-steps-bar {
  display: flex;
  align-items: center;
  gap: 0;
  margin-bottom: 16px;
  padding: 8px 16px;
  background: var(--gray-50);
  border-radius: var(--radius-sm);
  font-size: 12px;
  overflow-x: auto;
  border: 1px solid var(--border);
}

.merge-step {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-weight: 600;
  white-space: nowrap;
}

.merge-step.active {
  background: var(--brand);
  color: #fff;
}

.merge-step.done {
  background: var(--green-bg);
  color: var(--green-text);
}

.merge-step.pending {
  color: var(--gray-300);
}

.merge-step-arrow {
  color: var(--gray-300);
  margin: 0 4px;
  font-size: 14px;
}

.merge-flow-grid {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 0;
  align-items: start;
}

.merge-panel {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
  background: #fff;
}

.merge-panel.result {
  border-color: #b3d4ff;
  border-width: 2px;
}

.merge-panel-header {
  padding: 10px 16px;
  background: var(--gray-50);
  border-bottom: 1px solid var(--border);
  font-size: 12px;
  font-weight: 600;
  color: var(--gray-700);
  display: flex;
  align-items: center;
  gap: 6px;
}

.result-header {
  background: var(--blue-bg);
  color: var(--brand);
}

.merge-panel-body {
  padding: 8px;
  max-height: 400px;
  overflow-y: auto;
}

.merge-panel-body-scroll {
  padding: 12px 16px;
  max-height: 360px;
  overflow-y: auto;
}

.merge-panel-body-scroll h4 {
  font-size: 12px;
  font-weight: 700;
  color: var(--brand);
  margin: 12px 0 4px 0;
}

.merge-panel-body-scroll p {
  font-size: 12px;
  color: var(--gray-700);
  line-height: 1.7;
  margin: 0;
}

.merge-source-item {
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  margin-bottom: 4px;
  border: 1px solid transparent;
  font-size: 12px;
}

.merge-source-item:hover {
  background: var(--bg-hover);
}

.src-name {
  font-weight: 600;
  color: var(--gray-900);
}

.src-dept {
  font-size: 11px;
  color: var(--gray-400);
  margin-left: 4px;
}

.src-summary {
  font-size: 11px;
  color: var(--gray-400);
  margin-top: 2px;
  display: flex;
  gap: 8px;
}

.merge-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  text-align: center;
}

.merge-ai-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--brand);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  animation: aiPulse 2s infinite;
  box-shadow: 0 0 0 6px rgba(0,110,255,.08);
}

@keyframes aiPulse {
  0%, 100% { box-shadow: 0 0 0 4px rgba(0,110,255,.08); }
  50% { box-shadow: 0 0 0 10px rgba(0,110,255,.03); }
}

.merge-center-text {
  font-size: 11px;
  color: var(--gray-400);
  margin-top: 6px;
  font-weight: 600;
}

.merge-center-count {
  font-size: 12px;
  color: var(--brand);
  font-weight: 700;
  margin-top: 2px;
}

.ai-processing {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 10px;
  justify-content: center;
}

.ai-dot {
  width: 5px; height: 5px;
  border-radius: 50%;
  background: var(--brand);
  animation: aiDot 1.4s infinite;
}

.ai-dot:nth-child(2) { animation-delay: .2s; }
.ai-dot:nth-child(3) { animation-delay: .4s; }

@keyframes aiDot {
  0%, 80%, 100% { opacity: .2; transform: scale(.8); }
  40% { opacity: 1; transform: scale(1.15); }
}

.merge-result-actions {
  padding: 8px 16px;
  border-top: 1px solid var(--border);
  background: var(--gray-50);
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.merge-edit-field {
  margin-bottom: 12px;
}

.merge-edit-field label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--gray-500);
  margin-bottom: 4px;
}
</style>
