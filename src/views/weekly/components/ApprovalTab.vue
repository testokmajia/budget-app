<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingList, approveReport, rejectReport } from '@/api/weekly'
import { parseItems } from '@/utils/workItemParser'
import SummaryChip from './SummaryChip.vue'
import WorkPills from './WorkPills.vue'

const loading = ref(false)
const reports = ref([])

onMounted(refresh)

async function refresh() {
  loading.value = true
  try {
    const res = await getPendingList()
    reports.value = (res.data || []).filter(r => r.status === 'SUBMITTED')
  } finally {
    loading.value = false
  }
}

async function handleApprove(id) {
  await approveReport(id)
  ElMessage.success('已通过')
  refresh()
}

async function handleReject(id) {
  const { value: comment } = await ElMessageBox.prompt('请填写驳回原因', '驳回周报', {
    confirmButtonText: '确认驳回',
    inputType: 'textarea',
    inputValidator: v => v?.trim() ? true : '驳回原因不能为空',
    inputErrorMessage: '驳回原因不能为空'
  }).catch(() => null)
  if (!comment) return
  await rejectReport(id, { comment })
  ElMessage.success('已驳回')
  refresh()
}
</script>

<template>
  <div v-loading="loading">
    <div v-if="reports.length" style="display:flex;flex-direction:column;gap:12px;">
      <div v-for="r in reports" :key="r.id" class="approval-card">
        <div class="approval-card-header">
          <div style="display:flex;align-items:center;gap:8px;">
            <div class="approval-avatar" :style="{ background: '#006eff' }">
              {{ (r.userName || '?').charAt(0) }}
            </div>
            <strong style="font-size:13px;">{{ r.userName }}</strong>
            <span style="font-size:11px;color:var(--gray-400);">{{ r.teamName || r.userDepartment }}</span>
            <span style="font-size:11px;color:var(--gray-300);" v-if="r.submittedAt">提交于 {{ r.submittedAt }}</span>
            <el-tag type="warning" size="small">待审批</el-tag>
          </div>
          <div style="display:flex;gap:6px;">
            <el-button size="small" @click="handleReject(r.id)">驳回</el-button>
            <el-button size="small" type="success" @click="handleApprove(r.id)">通过</el-button>
          </div>
        </div>
        <div class="approval-card-body">
          <div class="approval-summary">
            <SummaryChip label="已完成" :count="parseItems(r.doneWork).filter(i=>i.item&&i.status==='done').length" variant="done" />
            <SummaryChip label="进行中" :count="parseItems(r.doneWork).filter(i=>i.item&&i.status==='in-progress').length" variant="plan" />
            <SummaryChip label="受阻" :count="parseItems(r.doneWork).filter(i=>i.status==='blocked').length" variant="problem" />
            <SummaryChip label="计划" :count="parseItems(r.planWork).filter(i=>i.item).length" variant="plan" />
          </div>
          <WorkPills :items="parseItems(r.doneWork).filter(i => i.item)" max="5" />
        </div>
      </div>
    </div>
    <div v-else style="text-align:center;padding:48px;color:var(--gray-400);">
      <div style="font-size:48px;margin-bottom:8px;">📋</div>
      <div>暂无待审批的周报</div>
    </div>
  </div>
</template>

<style scoped>
.approval-card {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
}

.approval-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: var(--gray-50);
  border-bottom: 1px solid var(--border);
}

.approval-avatar {
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

.approval-card-body {
  padding: 12px 16px;
}

.approval-summary {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
