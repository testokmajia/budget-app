<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getSubmittedList, getTeamStats, remindMember, approveReport, rejectReport } from '@/api/weekly'
import { parseItems } from '@/utils/workItemParser'
import WorkItemCard from './WorkItemCard.vue'
import SummaryChip from './SummaryChip.vue'

const userStore = useUserStore()
const loading = ref(false)
const teamStats = ref(null)
const pendingReports = ref([])
const selectedMember = ref(null)
const selectedReportId = ref(null)
const selectedTeamName = ref('')

const leaderName = computed(() => userStore.user?.name || '')

onMounted(refresh)

async function refresh() {
  loading.value = true
  try {
    const [sr, tr] = await Promise.all([
      getSubmittedList().catch(() => ({ data: [] })),
      getTeamStats().catch(() => ({ data: null }))
    ])
    pendingReports.value = sr.data || []
    teamStats.value = tr.data
  } finally {
    loading.value = false
  }
}

const teams = computed(() => teamStats.value?.teams || [])
const overallRate = computed(() => teamStats.value?.overallRate || 0)
const totalMembers = computed(() => teamStats.value?.totalMembers || 0)
const totalSubmitted = computed(() => teamStats.value?.totalSubmitted || 0)

const ringOffset = computed(() => {
  const circumference = 2 * Math.PI * 31
  return circumference * (1 - overallRate.value)
})

function isOverdue(r) {
  if (r?.status === 'SUBMITTED' || r?.status === 'APPROVED') return false
  const now = new Date()
  const d = now.getDay() || 7
  const fri = new Date(now)
  fri.setDate(now.getDate() + (5 - d))
  return now > fri
}

async function selectMember(teamName, member) {
  selectedTeamName.value = teamName
  if (member.reportId) {
    // Find the full report from pending reports
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
  // Not submitted yet
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

async function handleApprove() {
  if (!selectedReportId.value) return
  await approveReport(selectedReportId.value)
  ElMessage.success('已通过')
  selectedMember.value = null
  refresh()
}

async function handleReject() {
  if (!selectedReportId.value) return
  const { value: comment } = await ElMessageBox.prompt('请填写驳回原因', '驳回周报', {
    confirmButtonText: '确认驳回',
    inputType: 'textarea',
    inputValidator: v => v?.trim() ? true : '驳回原因不能为空',
    inputErrorMessage: '驳回原因不能为空'
  }).catch(() => null)
  if (!comment) return
  await rejectReport(selectedReportId.value, { comment })
  ElMessage.success('已驳回')
  selectedMember.value = null
  refresh()
}

async function handleRemind(reportId) {
  await remindMember(reportId)
  ElMessage.success('已催办')
}

function ringCircumference() {
  return 2 * Math.PI * 31
}

function memberAvatarColor(name) {
  if (!name) return '#006eff'
  const colors = ['#006eff', '#0abf5b', '#ff9c00', '#e54545', '#722ed1']
  return colors[name.charCodeAt(0) % colors.length]
}
</script>

<template>
  <div v-loading="loading">
    <!-- Leader identity bar -->
    <el-alert type="info" :closable="false" style="margin-bottom:16px;">
      <template #title>
        <div style="display:flex;align-items:center;gap:12px;">
          <div class="avatar-circle" :style="{ background: '#006eff', width: '36px', height: '36px', fontSize: '14px' }">
            {{ leaderName.charAt(0) }}
          </div>
          <div>
            <div style="font-weight:700;font-size:13px;">{{ leaderName }}</div>
            <div style="font-size:11px;color:var(--gray-500);display:flex;align-items:center;gap:6px;flex-wrap:wrap;">
              管理团队：
              <el-tag
                v-for="t in teams" :key="t.teamName"
                size="small"
                :type="t.teamName === selectedTeamName ? '' : 'info'"
              >
                {{ t.teamName }}
              </el-tag>
              <el-tag v-if="teams.length > 1" type="warning" size="small">兼任 {{ teams.length }} 组</el-tag>
            </div>
          </div>
        </div>
      </template>
    </el-alert>

    <!-- Left-right layout -->
    <div class="dashboard-layout">
      <!-- Left sidebar -->
      <div class="dashboard-sidebar">
        <!-- Ring chart stats -->
        <div class="sidebar-stats">
          <div class="ring-wrap">
            <svg class="ring-svg" width="80" height="80" viewBox="0 0 80 80">
              <circle cx="40" cy="40" r="31" fill="none" stroke="var(--gray-100)" stroke-width="7" />
              <circle cx="40" cy="40" r="31" fill="none" stroke="var(--brand)" stroke-width="7"
                :stroke-dasharray="ringCircumference()"
                :stroke-dashoffset="ringOffset"
                stroke-linecap="round" />
            </svg>
            <div class="ring-center">
              <div style="font-size:20px;font-weight:800;color:var(--brand);line-height:1;">
                {{ totalSubmitted }}<span style="font-size:11px">/{{ totalMembers }}</span>
              </div>
            </div>
          </div>
          <div style="font-size:11px;color:var(--gray-500);text-align:center;">
            <strong style="color:var(--brand);font-size:14px;">{{ Math.round(overallRate * 100) }}%</strong> 已提交
          </div>
        </div>

        <!-- Team member lists -->
        <div class="sidebar-teams" v-if="teams.length">
          <div v-for="t in teams" :key="t.teamName" class="sidebar-team-group">
            <div class="sidebar-team-header">
              <span class="sidebar-team-title">{{ t.teamName }}</span>
              <span class="sidebar-team-meta">{{ t.submittedCount }}/{{ t.totalMembers }}人</span>
            </div>
            <div
              v-for="m in t.members"
              :key="m.name"
              class="sidebar-member"
              :class="{
                active: selectedMember?.name === m.name && selectedTeamName === t.teamName,
                overdue: !m.submitted
              }"
              @click="selectMember(t.teamName, m)"
            >
              <div class="member-avatar" :style="{ background: memberAvatarColor(m.name) }">
                {{ m.name.charAt(0) }}
              </div>
              <div class="member-info">
                <div class="member-name">{{ m.name }}</div>
                <div class="member-meta" :style="{ color: m.submitted ? '' : 'var(--red)' }">
                  {{ m.submitted ? '已提交' : '未提交' }}
                </div>
              </div>
              <span v-if="m.submitted" class="member-status ok">✅</span>
              <span v-else class="member-status warn">⏳</span>
            </div>
          </div>
        </div>
        <div v-else style="padding:16px;text-align:center;color:var(--gray-400);font-size:13px;">
          暂无团队数据
        </div>
      </div>

      <!-- Right main content -->
      <div class="dashboard-main">
        <template v-if="selectedMember">
          <!-- Context bar -->
          <div class="context-bar">
            <el-tag size="small">{{ selectedTeamName }}</el-tag>
            <span style="font-size:12px;">▸ {{ selectedMember.name }}的周报</span>
          </div>

          <!-- Detail topbar -->
          <div class="detail-topbar">
            <div style="display:flex;align-items:center;gap:10px;">
              <div class="member-avatar" :style="{ background: memberAvatarColor(selectedMember.name), width: '36px', height: '36px', fontSize: '15px' }">
                {{ selectedMember.name.charAt(0) }}
              </div>
              <div>
                <div style="font-size:13px;font-weight:700;">
                  {{ selectedMember.name }}
                  <span style="font-weight:400;color:var(--gray-400);font-size:11px;margin-left:4px;">{{ selectedMember.dept }}</span>
                </div>
                <div style="font-size:11px;color:var(--gray-400);display:flex;align-items:center;gap:8px;margin-top:2px;">
                  <el-tag v-if="selectedMember.status === 'SUBMITTED'" type="warning" size="small">已提交</el-tag>
                  <el-tag v-else-if="selectedMember.status === 'APPROVED'" type="success" size="small">已通过</el-tag>
                  <el-tag v-else type="danger" size="small">未提交</el-tag>
                  <span v-if="selectedMember.submittedAt">提交于 {{ selectedMember.submittedAt }}</span>
                  <el-tag v-if="selectedMember.version > 0" size="small">V{{ selectedMember.version }}</el-tag>
                </div>
              </div>
            </div>
            <div v-if="selectedMember.status === 'SUBMITTED'" style="display:flex;gap:8px;">
              <el-button size="small" @click="handleReject">↩ 驳回</el-button>
              <el-button size="small" type="success" @click="handleApprove">✓ 通过</el-button>
            </div>
          </div>

          <!-- Detail body -->
          <div class="detail-body">
            <!-- Summary chips -->
            <div class="summary-chips" v-if="selectedMember.status !== 'NOT_SUBMITTED'">
              <SummaryChip label="已完成" :count="selectedMember.doneItems.filter(i => i.item && i.status === 'done').length" variant="done" />
              <SummaryChip label="进行中" :count="selectedMember.doneItems.filter(i => i.item && i.status === 'in-progress').length" variant="plan" />
              <SummaryChip label="受阻" :count="selectedMember.doneItems.filter(i => i.status === 'blocked').length" variant="problem" />
              <SummaryChip label="计划" :count="selectedMember.planItems.filter(i => i.item).length" variant="plan" />
            </div>

            <template v-if="selectedMember.status !== 'NOT_SUBMITTED'">
              <div class="detail-section" v-if="selectedMember.doneItems.filter(i => i.item).length">
                <h4>本周完成工作</h4>
                <WorkItemCard
                  v-for="(item, i) in selectedMember.doneItems.filter(x => x.item)"
                  :key="'d'+i"
                  :item="item.item"
                  :progress="item.progress"
                  :status="item.status"
                  :editable="false"
                  :draggable="false"
                />
              </div>
              <div class="detail-section" v-if="selectedMember.planItems.filter(i => i.item).length">
                <h4>下周工作计划</h4>
                <WorkItemCard
                  v-for="(item, i) in selectedMember.planItems.filter(x => x.item)"
                  :key="'p'+i"
                  :item="item.item"
                  :progress="item.progress"
                  :status="item.status"
                  :editable="false"
                  :draggable="false"
                />
              </div>
              <div class="detail-section" v-if="selectedMember.problems">
                <h4 style="color:var(--red);">⚠️ 遇到的问题</h4>
                <div class="detail-block red-bg">
                  {{ selectedMember.problems }}
                </div>
              </div>
              <div class="detail-section" v-if="selectedMember.supportNeeded">
                <h4 style="color:var(--brand);">🤝 需要支持</h4>
                <div class="detail-block blue-bg">
                  {{ selectedMember.supportNeeded }}
                </div>
              </div>
              <div v-if="selectedMember.reviewComment" class="reject-comment">
                驳回原因：{{ selectedMember.reviewComment }}
              </div>
            </template>
            <div v-else class="empty-state">
              <div style="font-size:48px;margin-bottom:8px;">📝</div>
              <div>该成员尚未提交本周报</div>
              <el-button size="small" type="primary" style="margin-top:8px;" @click="handleRemind(selectedMember.id || 0)" v-if="selectedMember.id">
                催办
              </el-button>
            </div>
          </div>
        </template>
        <div v-else class="detail-body empty-state">
          选择左侧成员查看周报详情
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.avatar-circle {
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.dashboard-layout {
  display: flex;
  min-height: 500px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
  background: #fff;
}

.dashboard-sidebar {
  width: 260px;
  flex-shrink: 0;
  background: var(--gray-50);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
}

.sidebar-stats {
  padding: 16px;
  border-bottom: 1px solid var(--border);
  background: #fff;
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: center;
}

.ring-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.ring-svg {
  transform: rotate(-90deg);
}

.ring-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.sidebar-teams {
  flex: 1;
  overflow-y: auto;
}

.sidebar-team-group {
  padding: 8px;
  border-bottom: 1px solid var(--border-light);
}

.sidebar-team-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 4px 6px 4px;
}

.sidebar-team-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--gray-900);
}

.sidebar-team-meta {
  font-size: 10px;
  color: var(--gray-400);
}

.sidebar-member {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.15s;
  border: 1px solid transparent;
  margin-bottom: 2px;
}

.sidebar-member:hover {
  background: var(--bg-hover);
  border-color: var(--border);
}

.sidebar-member.active {
  background: #fff;
  border-color: #b3d4ff;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.sidebar-member.overdue {
  border-left: 2px solid var(--red);
}

.member-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 12px;
  flex-shrink: 0;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--gray-900);
}

.member-meta {
  font-size: 11px;
  color: var(--gray-400);
  margin-top: 1px;
}

.member-status {
  font-size: 14px;
  flex-shrink: 0;
}

.member-status.ok { color: var(--green); }
.member-status.warn { color: var(--amber); }

.dashboard-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.context-bar {
  padding: 8px 16px;
  background: var(--gray-50);
  border-bottom: 1px solid var(--border);
  font-size: 12px;
  color: var(--gray-500);
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
}

.detail-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.detail-section {
  margin-bottom: 16px;
}

.detail-section h4 {
  font-size: 12px;
  font-weight: 700;
  color: var(--gray-500);
  margin-bottom: 8px;
}

.detail-block {
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--gray-700);
}

.detail-block.red-bg {
  background: var(--red-bg);
}

.detail-block.blue-bg {
  background: var(--blue-bg);
}

.reject-comment {
  margin-top: 8px;
  font-size: 12px;
  color: var(--red);
  background: var(--red-bg);
  padding: 6px 10px;
  border-radius: var(--radius);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--gray-400);
  font-size: 14px;
}

.summary-chips {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
</style>
