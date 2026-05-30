<script setup>
import { ref, watch, onMounted, inject, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCurrentWeekReport, saveReport, submitReport, getSmartDraft, getDeptFinalizedStatus } from '@/api/weekly'
import { parseItems, serializeItems, countByStatus } from '@/utils/workItemParser'

const userStore = useUserStore()
const refreshBadges = inject('refreshBadges', null)
const loading = ref(false), submitting = ref(false)
const submittedStatus = ref(''), reportId = ref(null), reportVersion = ref(1), weekLabel = ref('')
const deptFinalized = ref(false)
const doneItems = ref([{ item: '', progress: '', status: 'in-progress' }])
const planItems = ref([{ item: '', progress: '', status: 'in-progress' }])
const problems = ref(''), supportNeeded = ref('')
const LS_KEY = `weekly_draft_v4_${userStore.user?.id}`
const dc = () => countByStatus(doneItems.value)
const pc = () => countByStatus(planItems.value)

// Week date editing
const weekStartInput = ref('')
const weekEndInput = ref('')

function getCurrentMonday() {
  const now = new Date()
  const day = now.getDay() || 7
  const m = new Date(now)
  m.setDate(now.getDate() - day + 1)
  return m.toISOString().slice(0, 10)
}
function getCurrentSunday() {
  const m = new Date(getCurrentMonday())
  m.setDate(m.getDate() + 6)
  return m.toISOString().slice(0, 10)
}

function isEditable() { return submittedStatus.value !== 'APPROVED' && !deptFinalized.value }

function loadLS() {
  try {
    const d = JSON.parse(localStorage.getItem(LS_KEY))
    if (d?.doneItems?.length) {
      doneItems.value = d.doneItems
      planItems.value = d.planItems || [{ item: '', progress: '', status: 'in-progress' }]
      problems.value = d.problems || ''
      supportNeeded.value = d.supportNeeded || ''
    }
  } catch {}
}

function saveLS() {
  try { localStorage.setItem(LS_KEY, JSON.stringify({ doneItems: doneItems.value, planItems: planItems.value, problems: problems.value, supportNeeded: supportNeeded.value })) } catch {}
}

let t = null
watch([doneItems, planItems, problems, supportNeeded], () => {
  if (submittedStatus.value === 'APPROVED' || deptFinalized.value) return
  clearTimeout(t); t = setTimeout(saveLS, 2000)
}, { deep: true })

async function init() {
  loading.value = true
  try {
    const [r, fs] = await Promise.all([
      getCurrentWeekReport().catch(() => ({ data: null })),
      getDeptFinalizedStatus().catch(() => ({ data: { finalized: false } }))
    ])
    reportId.value = r.data?.id; submittedStatus.value = r.data?.status || ''
    reportVersion.value = r.data?.version || 1; weekLabel.value = r.data?.weekLabel || ''
    weekStartInput.value = r.data?.weekStartDate || getCurrentMonday()
    weekEndInput.value = r.data?.weekEndDate || getCurrentSunday()
    deptFinalized.value = fs.data?.finalized === true
    if (r.data && (r.data.doneWork || r.data.planWork)) {
      doneItems.value = parseItems(r.data.doneWork); planItems.value = parseItems(r.data.planWork)
      problems.value = r.data.problems || ''; supportNeeded.value = r.data.supportNeeded || ''
    } else if (isEditable()) { loadLS() }
  } catch { loadLS() } finally { loading.value = false }
}
onMounted(init)

function addItem(a) { a.push({ item: '', progress: '', status: 'in-progress' }) }
function removeItem(a, i) { if (a.length <= 1) { a[0] = { item: '', progress: '', status: 'in-progress' }; return }; a.splice(i, 1) }

function payload() {
  return { doneWork: serializeItems(doneItems.value), planWork: serializeItems(planItems.value), problems: problems.value.trim(), supportNeeded: supportNeeded.value.trim() }
}

async function handleSave() {
  loading.value = true
  try {
    const r = await saveReport(payload()); reportId.value = r.data?.id
    submittedStatus.value = 'DRAFT'; reportVersion.value = r.data?.version || 1
    localStorage.removeItem(LS_KEY); ElMessage.success('草稿已保存')
  } finally { loading.value = false }
}

async function handleSubmit() {
  if (dc().total === 0 || (dc().total === 1 && !doneItems.value[0].item.trim())) { ElMessage.warning('请至少填写一项完成工作'); return }
  const label = (submittedStatus.value === 'SUBMITTED' || submittedStatus.value === 'REJECTED') ? '确认重新提交本周报吗？' : '确认提交本周报吗？'
  await ElMessageBox.confirm(label, '确认提交', { confirmButtonText: '确认提交', cancelButtonText: '再检查一下', type: 'warning' })
  submitting.value = true
  try {
    const r = await submitReport(payload()); reportId.value = r.data?.id
    submittedStatus.value = 'SUBMITTED'; reportVersion.value = r.data?.version || 1
    localStorage.removeItem(LS_KEY); ElMessage.success('周报已提交'); refreshBadges?.()
  } finally { submitting.value = false }
}

async function handleSmartDraft() {
  loading.value = true
  try {
    const r = await getSmartDraft()
    if (r.data?.hasLastWeek && r.data.suggestedDoneWork) { doneItems.value = parseItems(r.data.suggestedDoneWork); ElMessage.success('已根据上周计划生成初稿') }
    else { ElMessage.info('未找到上周周报') }
  } finally { loading.value = false }
}

function statusClass(s) {
  if (s === 'APPROVED') return 'ok'
  if (s === 'SUBMITTED') return 'warn'
  if (s === 'REJECTED') return 'bad'
  return 'draft'
}
function statusText(s) {
  const map = { APPROVED: '已审定', SUBMITTED: '已提交', REJECTED: '已驳回', DRAFT: '草稿', NOT_SUBMITTED: '未生成' }
  return map[s] || '未生成'
}
function setStatus(item, s) { item.status = s }
function dotClass(s) {
  if (s === 'done') return 'green'
  if (s === 'in-progress') return 'blue'
  return 'red'
}
</script>

<template>
  <div v-loading="loading" class="fill-root">
    <!-- ===== 头部 ===== -->
    <div class="fill-hd">
      <div class="fill-hd-l">
        <div class="fill-sub">
          <template v-if="isEditable()">
            <el-date-picker v-model="weekStartInput" type="date" placeholder="周一" value-format="YYYY-MM-DD" size="small" style="width:130px;" />
            <span style="color:var(--gray-300);">~</span>
            <el-date-picker v-model="weekEndInput" type="date" placeholder="周日" value-format="YYYY-MM-DD" size="small" style="width:130px;" />
          </template>
          <span v-else style="font-size:14px;font-weight:600;color:var(--gray-700);">{{ weekLabel }}</span>
        </div>
      </div>
      <div style="display:flex;align-items:center;gap:8px;">
        <span v-if="submittedStatus" class="fill-tag" :class="statusClass(submittedStatus)">
          <span class="fill-tag-dot"></span>{{ statusText(submittedStatus) }}
        </span>
        <el-tag v-if="reportVersion > 1" size="small" style="font-weight:600;">V{{ reportVersion }}</el-tag>
        <template v-if="isEditable()">
          <el-button size="small" @click="handleSave" :loading="loading">💾 保存草稿</el-button>
          <el-button size="small" @click="handleSmartDraft" :loading="loading">🤖 智能初稿</el-button>
          <el-button size="small" type="primary" @click="handleSubmit" :loading="loading || submitting">
            {{ (submittedStatus === 'SUBMITTED' || submittedStatus === 'REJECTED') ? '📤 重新提交' : '📤 提交周报' }}
          </el-button>
        </template>
      </div>
    </div>

    <!-- ===== 统计条 ===== -->
    <div v-if="submittedStatus" class="fill-statbar">
      <div class="fill-stat theme-green">
        <div class="fill-stat-val">{{ dc().done }}</div>
        <div class="fill-stat-lbl">已完成</div>
      </div>
      <div class="fill-stat theme-blue">
        <div class="fill-stat-val">{{ dc().inProgress }}</div>
        <div class="fill-stat-lbl">进行中</div>
      </div>
      <div class="fill-stat theme-red">
        <div class="fill-stat-val">{{ dc().blocked }}</div>
        <div class="fill-stat-lbl">受阻</div>
      </div>
      <div class="fill-stat theme-gray">
        <div class="fill-stat-val">{{ pc().total }}</div>
        <div class="fill-stat-lbl">计划</div>
      </div>
    </div>

    <!-- ===== 内容区 ===== -->
    <div class="fill-bd">
      <!-- 本周完成工作 -->
      <div class="fill-sec">
        <div class="fill-sec-hd">
          <div class="fill-sec-bar green"></div>
          <span class="fill-sec-title">本周完成工作</span>
          <span class="fill-sec-note">{{ dc().total }} 项</span>
        </div>

        <div v-if="doneItems.length" class="fill-wi-list">
          <div v-for="(item, i) in doneItems" :key="'d'+i" class="fill-wi-item" :class="{ 'is-empty': !item.item && isEditable() }">
            <div class="fill-wi-dot" :class="dotClass(item.status)"></div>
            <div class="fill-wi-body">
              <div class="fill-wi-row1">
                <input v-if="isEditable()" v-model="item.item" placeholder="输入事项描述…" maxlength="50" class="fill-input" />
                <span v-else class="fill-readonly">{{ item.item || '(空)' }}</span>
                <!-- 状态切换 -->
                <span v-if="isEditable()" class="fill-wi-actions">
                  <button class="fill-pill" :class="{ on: item.status === 'done' }" @click="setStatus(item, 'done')">已完成</button>
                  <button class="fill-pill" :class="{ on: item.status === 'in-progress' }" @click="setStatus(item, 'in-progress')">进行中</button>
                  <button class="fill-pill" :class="{ on: item.status === 'blocked' }" @click="setStatus(item, 'blocked')">受阻</button>
                  <button class="fill-del" @click="removeItem(doneItems, i)" title="删除">×</button>
                </span>
              </div>
              <input v-if="isEditable()" v-model="item.progress" placeholder="进展说明（选填）…" maxlength="100" class="fill-input fill-input-prog" />
              <span v-else-if="item.progress" class="fill-readonly-prog">{{ item.progress }}</span>
            </div>
          </div>
        </div>
        <button v-if="isEditable()" class="fill-add" @click="addItem(doneItems)">+ 添加工作项</button>
      </div>

      <!-- 下周工作计划 -->
      <div class="fill-sec">
        <div class="fill-sec-hd">
          <div class="fill-sec-bar blue"></div>
          <span class="fill-sec-title">下周工作计划</span>
          <span class="fill-sec-note">{{ pc().total }} 项</span>
        </div>

        <div v-if="planItems.length" class="fill-wi-list">
          <div v-for="(item, i) in planItems" :key="'p'+i" class="fill-wi-item" :class="{ 'is-empty': !item.item && isEditable() }">
            <div class="fill-wi-dot" :class="dotClass(item.status)"></div>
            <div class="fill-wi-body">
              <div class="fill-wi-row1">
                <input v-if="isEditable()" v-model="item.item" placeholder="输入计划事项…" maxlength="50" class="fill-input" />
                <span v-else class="fill-readonly">{{ item.item || '(空)' }}</span>
                <span v-if="isEditable()" class="fill-wi-actions">
                  <button class="fill-pill" :class="{ on: item.status === 'done' }" @click="setStatus(item, 'done')">已完成</button>
                  <button class="fill-pill" :class="{ on: item.status === 'in-progress' }" @click="setStatus(item, 'in-progress')">进行中</button>
                  <button class="fill-del" @click="removeItem(planItems, i)" title="删除">×</button>
                </span>
              </div>
              <input v-if="isEditable()" v-model="item.progress" placeholder="预期目标（选填）…" maxlength="100" class="fill-input fill-input-prog" />
              <span v-else-if="item.progress" class="fill-readonly-prog">{{ item.progress }}</span>
            </div>
          </div>
        </div>
        <button v-if="isEditable()" class="fill-add" @click="addItem(planItems)">+ 添加计划项</button>
      </div>

      <!-- 遇到的问题 -->
      <div class="fill-sec">
        <div class="fill-sec-hd">
          <div class="fill-sec-bar red"></div>
          <span class="fill-sec-title">遇到的问题</span>
          <span class="fill-sec-note">选填</span>
        </div>
        <textarea v-if="isEditable()" v-model="problems" class="fill-textarea" placeholder="需要协调的事项或遇到的困难…" maxlength="500" rows="3"></textarea>
        <div v-else class="fill-prose fill-prose-red" v-text="problems || '无'"></div>
      </div>

      <!-- 需要支持 -->
      <div class="fill-sec">
        <div class="fill-sec-hd">
          <div class="fill-sec-bar amber"></div>
          <span class="fill-sec-title">需要支持</span>
          <span class="fill-sec-note">选填</span>
        </div>
        <textarea v-if="isEditable()" v-model="supportNeeded" class="fill-textarea" placeholder="需要上级或跨组协调的事项…" maxlength="500" rows="3"></textarea>
        <div v-else class="fill-prose fill-prose-amber" v-text="supportNeeded || '无'"></div>
      </div>

      <!-- 底部提示 -->
      <div v-if="!isEditable()" style="text-align:center;padding:20px;color:var(--gray-300);font-size:13px;">
        {{ deptFinalized ? '🔒 本周部门周报已审定，不允许再修改' : '✅ 本周报已审批通过，不再支持修改' }}
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== 根容器 ===== */
.fill-root {
  max-width: 780px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,.06);
  overflow: hidden;
  border: 1px solid #eee;
}

/* ===== 头部 ===== */
.fill-hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}
.fill-sub {
  font-size: 13px;
  color: var(--gray-300);
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 状态标签 */
.fill-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 12px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 700;
}
.fill-tag-dot { width: 6px; height: 6px; border-radius: 50%; }
.fill-tag.ok { background: #ecfdf5; color: #059669; } .fill-tag.ok .fill-tag-dot { background: #0abf5b; }
.fill-tag.warn { background: #fffbeb; color: #d97706; } .fill-tag.warn .fill-tag-dot { background: #f59e0b; }
.fill-tag.draft { background: #f3f4f6; color: #6b7280; } .fill-tag.draft .fill-tag-dot { background: #9ca3af; }
.fill-tag.bad { background: #fef2f2; color: #dc2626; } .fill-tag.bad .fill-tag-dot { background: #e54545; }

/* ===== 统计条 ===== */
.fill-statbar {
  display: flex;
  border-bottom: 1px solid #eee;
}
.fill-stat {
  flex: 1;
  text-align: center;
  padding: 12px 8px;
  position: relative;
}
.fill-stat + .fill-stat::after {
  content: '';
  position: absolute;
  left: 0;
  top: 25%;
  height: 50%;
  width: 1px;
  background: #eee;
}
.fill-stat-val {
  font-size: 24px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.5px;
}
.fill-stat-lbl {
  font-size: 12px;
  font-weight: 600;
  margin-top: 3px;
}
.theme-green .fill-stat-val, .theme-green .fill-stat-lbl { color: #0abf5b; }
.theme-blue  .fill-stat-val, .theme-blue  .fill-stat-lbl { color: var(--brand); }
.theme-red   .fill-stat-val, .theme-red   .fill-stat-lbl { color: #e54545; }
.theme-gray  .fill-stat-val, .theme-gray  .fill-stat-lbl { color: var(--gray-300); }

/* ===== 内容区 ===== */
.fill-bd {
  padding: 16px 20px;
}

/* 区块 */
.fill-sec {
  margin-bottom: 20px;
}
.fill-sec:last-child { margin-bottom: 0; }
.fill-sec-hd {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.fill-sec-bar {
  width: 3px;
  height: 14px;
  border-radius: 2px;
  flex-shrink: 0;
}
.fill-sec-bar.green { background: #0abf5b; }
.fill-sec-bar.blue  { background: var(--brand); }
.fill-sec-bar.red   { background: #e54545; }
.fill-sec-bar.amber { background: #f59e0b; }
.fill-sec-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--gray-900);
}
.fill-sec-note {
  font-size: 12px;
  color: var(--gray-300);
  font-weight: 400;
}

/* 工作项列表 */
.fill-wi-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.fill-wi-item {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  background: var(--gray-50);
  border-radius: 4px;
  border: 1px solid transparent;
  transition: all 0.15s;
}
.fill-wi-item:hover { background: #fff; border-color: #e5e7eb; box-shadow: 0 1px 2px rgba(0,0,0,.03); }
.fill-wi-item.is-empty { border-color: #e5e7eb; background: #fff; }

.fill-wi-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 7px;
  flex-shrink: 0;
}
.fill-wi-dot.green { background: #0abf5b; }
.fill-wi-dot.blue  { background: var(--brand); }
.fill-wi-dot.red   { background: #e54545; }

.fill-wi-body { flex: 1; min-width: 0; }
.fill-wi-row1 {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 输入框 */
.fill-input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-500);
  padding: 2px 0;
  outline: none;
  font-family: inherit;
  line-height: 1.5;
}
.fill-input::placeholder { color: var(--gray-300); font-weight: 400; }
.fill-input:focus { color: var(--gray-900); }
.fill-input-prog {
  display: block;
  width: 100%;
  margin-top: 3px;
  font-size: 12px;
  font-weight: 400;
  color: var(--gray-300);
  padding-left: 1px;
}
.fill-input-prog::before {
  content: '';
  display: inline-block;
  width: 1px;
  height: 10px;
  background: var(--gray-200);
  margin-right: 6px;
  vertical-align: middle;
  position: relative;
  top: -1px;
}

.fill-readonly {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-500);
  padding: 2px 0;
}
.fill-readonly-prog {
  display: block;
  margin-top: 4px;
  font-size: 13px;
  color: var(--gray-400);
  line-height: 1.6;
  padding-left: 1px;
}
.fill-readonly-prog::before {
  content: '';
  display: inline-block;
  width: 1px;
  height: 10px;
  background: var(--gray-200);
  margin-right: 6px;
  vertical-align: middle;
  position: relative;
  top: -1px;
}

/* 状态切换药丸 */
.fill-wi-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.fill-pill {
  border: 1px solid var(--gray-200);
  background: #fff;
  color: var(--gray-300);
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
  white-space: nowrap;
}
.fill-pill:hover { border-color: #9ca3af; color: var(--gray-500); }
.fill-pill.on { color: #fff; border-color: transparent; }
.fill-pill.on:nth-child(1) { background: #0abf5b; border-color: #0abf5b; color: #fff; }
.fill-pill.on:nth-child(2) { background: var(--brand); border-color: var(--brand); color: #fff; }
.fill-pill.on:nth-child(3) { background: #e54545; border-color: #e54545; color: #fff; }

.fill-del {
  border: none;
  background: transparent;
  color: var(--gray-300);
  font-size: 16px;
  cursor: pointer;
  padding: 0 2px;
  line-height: 1;
  transition: color 0.15s;
}
.fill-del:hover { color: #e54545; }

/* 添加按钮 */
.fill-add {
  display: block;
  width: 100%;
  margin-top: 6px;
  padding: 8px;
  border: 1px dashed var(--gray-200);
  background: transparent;
  color: var(--gray-300);
  font-size: 13px;
  font-weight: 600;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}
.fill-add:hover { border-color: var(--brand); color: var(--brand); background: #f8faff; }

/* 文本框 */
.fill-textarea {
  display: block;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  font-size: 14px;
  color: var(--gray-500);
  resize: vertical;
  outline: none;
  font-family: inherit;
  line-height: 1.6;
  transition: border-color 0.15s;
  box-sizing: border-box;
}
.fill-textarea::placeholder { color: var(--gray-300); }
.fill-textarea:focus { border-color: var(--brand); box-shadow: 0 0 0 2px rgba(0,110,255,.1); }

/* 富文本只读 */
.fill-prose {
  padding: 14px 16px;
  background: var(--gray-50);
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--gray-500);
  white-space: pre-wrap;
}
.fill-prose-red {
  background: #fef2f2;
  color: #991b1b;
  border-left: 3px solid #e54545;
}
.fill-prose-amber {
  background: #fffbeb;
  color: #92400e;
  border-left: 3px solid #f59e0b;
}
</style>
