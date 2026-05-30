<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getSubmittedList, listMyTeamSummaries, getMyTeams, getTeamStats, remindMember, approveReport, rejectReport } from '@/api/weekly'
import { parseItems } from '@/utils/workItemParser'
import WorkItemCard from './WorkItemCard.vue'
import SummaryChip from './SummaryChip.vue'
import LeaderIdentityBar from './LeaderIdentityBar.vue'
import AiMergeView from './AiMergeView.vue'

const userStore = useUserStore()
const reports = ref([]); const summaries = ref([]); const loading = ref(false); const teamStats = ref(null)
const selectedReport = ref(null); const selectedMember = ref(null); const selectedTeam = ref('')
const showMerge = ref(false); const mergeTeam = ref('')

onMounted(refresh)
async function refresh() {
  loading.value = true
  try {
    const [rr, sr, tr] = await Promise.all([getSubmittedList(), listMyTeamSummaries().catch(()=>({data:[]})), getTeamStats().catch(()=>({data:null}))])
    reports.value = rr.data || []; summaries.value = sr.data || []; teamStats.value = tr.data
  } finally { loading.value = false }
}

const grouped = computed(() => { const g = {}; for (const r of reports.value) { const t = r.teamName || r.submitterDepartment || '未分组'; (g[t] = g[t] || []).push(r) } return g })
const groupNames = computed(() => Object.keys(grouped.value))

function selectMember(r) {
  selectedReport.value = r; showMerge.value = false
  selectedMember.value = { id: r.id, name: r.userName || r.submitterName, dept: r.teamName || r.submitterDepartment,
    version: r.version || 1, status: r.status, submittedAt: r.submittedAt,
    doneItems: parseItems(r.doneWork), planItems: parseItems(r.planWork),
    problems: r.problems || '', supportNeeded: r.supportNeeded || '', reviewComment: r.reviewComment }
  selectedTeam.value = r.teamName || r.submitterDepartment || ''
}

function openMerge(teamName) { mergeTeam.value = teamName; showMerge.value = true; selectedMember.value = null }

async function handleApprove() { await approveReport(selectedReport.value.id); ElMessage.success('已通过'); refresh(); selectedMember.value = null }
async function handleReject() {
  const { value: comment } = await ElMessageBox.prompt('请填写驳回原因', '驳回周报', { confirmButtonText: '确认驳回', inputType: 'textarea', inputValidator: v => v?.trim() ? true : '驳回原因不能为空', inputErrorMessage: '驳回原因不能为空' }).catch(() => null)
  if (!comment) return
  await rejectReport(selectedReport.value.id, { comment }); ElMessage.success('已驳回'); refresh(); selectedMember.value = null
}
async function handleRemind(id) { await remindMember(id); ElMessage.success('已催办') }

function isOverdue(r) { if (r.status === 'SUBMITTED' || r.status === 'APPROVED') return false; const now = new Date(); const d = now.getDay() || 7; const fri = new Date(now); fri.setDate(now.getDate() + (5 - d)); return now > fri }

const totalMembers = computed(() => reports.value.length)
const submittedCount = computed(() => reports.value.filter(r => r.status === 'SUBMITTED' || r.status === 'APPROVED').length)
const overdueCount = computed(() => reports.value.filter(r => isOverdue(r)).length)
const submissionRate = computed(() => totalMembers.value > 0 ? submittedCount.value / totalMembers.value : 0)
const ringOffset = computed(() => 214 * (1 - submissionRate.value))
</script>

<template>
  <div v-loading="loading">
    <LeaderIdentityBar :leader-name="userStore.user?.name || ''" :team-names="groupNames" />

    <!-- AiMergeView when active -->
    <AiMergeView v-if="showMerge" :team-name="mergeTeam" :team-names="groupNames" :reports="reports" :visible="showMerge" @update:visible="showMerge = $event" @merged="showMerge = false; refresh()" />

    <div class="leader-layout" v-if="!showMerge">
      <div class="leader-sidebar">
        <div class="sidebar-stats">
          <div class="ring-container" style="width:80px;height:80px"><svg class="ring-svg" width="80" height="80" viewBox="0 0 80 80"><circle class="ring-bg" cx="40" cy="40" r="34" stroke-width="8"/><circle class="ring-fg" cx="40" cy="40" r="34" stroke-width="8" :stroke-dasharray="214" :stroke-dashoffset="ringOffset"/></svg><div class="ring-center"><div class="ring-number" style="font-size:20px">{{ submittedCount }}<span style="font-size:11px">/{{ totalMembers }}</span></div></div></div>
          <div style="text-align:center;font-size:12px;color:var(--gray-500)"><strong style="color:var(--brand)">{{ Math.round(submissionRate * 100) }}%</strong> 已提交<br><span v-if="overdueCount" style="color:var(--red);font-size:11px">{{ overdueCount }}人逾期</span></div>
        </div>
        <div class="sidebar-actions">
          <button class="btn btn-ghost" style="width:100%;font-size:11px;padding:6px" @click="refresh">🔄 刷新</button>
        </div>
        <div class="sidebar-member-list" v-if="reports.length">
          <div v-for="tn in groupNames" :key="tn" class="sidebar-group">
            <div class="sidebar-group-header">
              <div class="sidebar-group-title" style="font-size:12px;font-weight:700;color:var(--gray-900);margin:0;padding:0;text-transform:none;letter-spacing:0">{{ tn }}</div>
              <div class="sidebar-group-meta">{{ grouped[tn].length }}人</div>
              <button class="btn btn-primary" style="font-size:10px;padding:3px 8px" @click="openMerge(tn)">🤖 AI汇总</button>
            </div>
            <div v-for="r in grouped[tn]" :key="r.id" class="sidebar-member" :class="{ active: selectedReport?.id === r.id, overdue: isOverdue(r) }" @click="selectMember(r)">
              <div class="sidebar-member-avatar" :style="{ background: isOverdue(r) ? '#e08800' : '#006eff' }">{{ (r.userName || '?').charAt(0) }}</div>
              <div class="sidebar-member-info"><div class="sidebar-member-name">{{ r.userName }}</div><div class="sidebar-member-meta" :style="{ color: isOverdue(r) ? 'var(--red)' : '' }">V{{ r.version || 1 }} · {{ parseItems(r.doneWork).filter(i=>i.item).length }}项<span v-if="isOverdue(r)" style="color:var(--red)"> ⚠ 逾期</span></div></div>
              <button v-if="isOverdue(r)" class="urge-btn" @click.stop="handleRemind(r.id)">催办</button>
              <span v-else class="sidebar-member-status done">✅</span>
            </div>
          </div>
        </div>
        <div v-else class="sidebar-member-list" style="padding:16px;text-align:center;color:var(--gray-400);font-size:13px">暂无组员提交周报</div>
      </div>

      <div class="leader-main">
        <div class="team-context-bar" v-if="selectedMember"><span class="team-tag active" style="display:inline-block;padding:2px 10px;border-radius:2px;font-size:11px;font-weight:600;background:var(--brand);color:#fff">{{ selectedTeam }}</span><span style="font-size:11px;color:var(--gray-400)">▸ {{ selectedMember.name }}的周报</span></div>
        <template v-if="selectedMember">
          <div class="detail-topbar">
            <div class="detail-user"><div class="detail-avatar" style="background:#006eff">{{ selectedMember.name.charAt(0) }}</div><div><div class="detail-name">{{ selectedMember.name }} <span class="detail-dept">{{ selectedMember.dept }}</span></div><div class="detail-meta"><span class="tag tag-submitted" style="font-size:10px">已提交</span><span v-if="selectedMember.submittedAt">提交于 {{ selectedMember.submittedAt }}</span><span class="version-badge">V{{ selectedMember.version }}</span></div></div></div>
            <div class="detail-topbar-actions">
              <button class="btn btn-ghost" style="font-size:11px;padding:5px 12px" @click="handleReject">↩ 驳回</button>
              <button class="btn btn-success" style="font-size:11px;padding:5px 12px" @click="handleApprove">✓ 通过</button>
            </div>
          </div>
          <div class="detail-body">
            <div class="detail-summary-row">
              <SummaryChip label="✅ 完成" :count="selectedMember.doneItems.filter(i=>i.item&&i.status!=='blocked').length" variant="done" />
              <SummaryChip label="📋 计划" :count="selectedMember.planItems.filter(i=>i.item).length" variant="plan" />
              <SummaryChip label="⚠️ 问题" :count="selectedMember.doneItems.filter(i=>i.status==='blocked').length" variant="problem" />
            </div>
            <div class="detail-section"><h4>本周完成工作</h4><WorkItemCard v-for="(item,i) in selectedMember.doneItems.filter(x=>x.item)" :key="'d'+i" :item="item.item" :progress="item.progress" :status="item.status" :editable="false" :draggable="false" /></div>
            <div class="detail-section"><h4>下周工作计划</h4><WorkItemCard v-for="(item,i) in selectedMember.planItems.filter(x=>x.item)" :key="'p'+i" :item="item.item" :progress="item.progress" :status="item.status" :editable="false" :draggable="false" /></div>
            <div class="detail-section" v-if="selectedMember.problems"><h4>⚠️ 遇到的问题</h4><div class="extra-card"><p style="margin:0;font-size:13px;color:var(--gray-700)">{{ selectedMember.problems }}</p></div></div>
            <div class="detail-section" v-if="selectedMember.supportNeeded"><h4>🤝 需要支持</h4><div class="extra-card"><p style="margin:0;font-size:13px;color:var(--gray-700)">{{ selectedMember.supportNeeded }}</p></div></div>
            <div v-if="selectedMember.reviewComment" style="margin-top:8px;font-size:13px;color:var(--red);background:var(--red-bg);padding:6px 10px;border-radius:6px">驳回原因：{{ selectedMember.reviewComment }}</div>
          </div>
        </template>
        <div v-else class="detail-body" style="display:flex;align-items:center;justify-content:center;color:var(--gray-400);font-size:14px">选择左侧成员查看周报详情</div>
      </div>
    </div>

    <!-- Historical team summaries -->
    <div class="merge-section-title" v-if="summaries.length && !showMerge">历史组内汇总</div>
    <div v-if="!showMerge" style="display:flex;gap:12px;flex-wrap:wrap">
      <div v-for="s in summaries" :key="s.id" class="team-summary-chip">
        <span class="ts-team">{{ s.teamName }}</span>
        <span class="ts-leader">组长: {{ userStore.user?.name }}</span>
        <span class="ts-status"><span class="tag" :class="s.status==='SUBMITTED'?'tag-submitted':'tag-draft'" style="font-size:10px">{{ s.status==='SUBMITTED'?'已提交':'待提交' }}</span></span>
      </div>
    </div>
  </div>
</template>
