<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingList, approveReport, rejectReport } from '@/api/weekly'
import { parseItems } from '@/utils/workItemParser'
import SummaryChip from './SummaryChip.vue'
import WorkPills from './WorkPills.vue'

const tableData = ref([]); const loading = ref(false); const rejectVisible = ref(false); const rejectForm = ref({ id: null, comment: '' })

onMounted(refresh)
async function refresh() { loading.value = true; try { const r = await getPendingList(); tableData.value = (r.data||[]).map(r=>({...r, doneItems: parseItems(r.doneWork).filter(i=>i.item), planItems: parseItems(r.planWork).filter(i=>i.item)})) } finally { loading.value = false } }

async function handleApprove(row) { await approveReport(row.id); ElMessage.success(`已通过 ${row.userName||row.submitterName} 的周报`); refresh() }
function openReject(row) { rejectForm.value = { id: row.id, comment: '' }; rejectVisible.value = true }
async function handleReject() { if (!rejectForm.value.comment.trim()) { ElMessage.warning('驳回必须填写原因'); return }; await rejectReport(rejectForm.value.id, rejectForm.value); ElMessage.success('已驳回'); rejectVisible.value = false; refresh() }
</script>

<template>
  <div class="approval-list" v-loading="loading">
    <div v-if="!tableData.length" style="text-align:center;padding:40px;color:var(--gray-400);font-size:14px">暂无待审批周报</div>

    <div v-for="row in tableData" :key="row.id" class="approval-card">
      <div class="approval-card-header">
        <div class="approval-user">
          <strong>{{ row.userName || row.submitterName }}</strong>
          <span class="approval-dept">{{ row.teamName || row.submitterDepartment || '' }}</span>
          <span style="font-size:12px;color:var(--gray-400)">提交于 {{ row.submittedAt }}</span>
          <span class="tag tag-submitted">待审批</span>
        </div>
        <div class="approval-actions">
          <button class="btn btn-ghost" style="font-size:12px;padding:6px 14px" @click="openReject(row)">驳回</button>
          <button class="btn btn-success" style="font-size:12px;padding:6px 14px" @click="handleApprove(row)">通过</button>
        </div>
      </div>
      <div class="approval-body">
        <div class="approval-summary">
          <SummaryChip label="✅ 完成" :count="row.doneItems.filter(i=>i.status!=='blocked').length" variant="done" />
          <SummaryChip label="📋 计划" :count="row.planItems.length" variant="plan" />
          <SummaryChip label="⚠️ 问题" :count="row.doneItems.filter(i=>i.status==='blocked').length" variant="problem" />
        </div>
        <WorkPills :items="[...row.doneItems.map(i=>({item:i.item,_type:'done'})),...row.planItems.map(i=>({item:i.item,_type:'plan'}))]" :max="5" />
      </div>
    </div>

    <el-dialog v-model="rejectVisible" title="驳回周报" width="400px">
      <el-input v-model="rejectForm.comment" type="textarea" :rows="3" placeholder="请填写驳回原因（必填）" />
      <template #footer><el-button @click="rejectVisible = false">取消</el-button><el-button type="danger" @click="handleReject">确认驳回</el-button></template>
    </el-dialog>
  </div>
</template>
