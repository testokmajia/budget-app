<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingList, approveReport, rejectReport } from '@/api/weekly'

const emit = defineEmits(['approved'])

const loading = ref(false)
const tableData = ref([])

const rejectVisible = ref(false)
const rejectForm = reactive({ id: null, comment: '' })
const rejectSubmitting = ref(false)
const rejectFormRef = ref(null)

const rejectRules = {
  comment: [{ required: true, message: '请填写驳回原因', trigger: 'blur' }],
}

const approveVisible = ref(false)
const approveForm = reactive({ id: null, comment: '' })
const approveSubmitting = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const res = await getPendingList()
    tableData.value = res.data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '加载失败')
  } finally {
    loading.value = false
  }
}

function formatDate(d) {
  if (!d) return '-'
  return typeof d === 'string' ? d.split('T')[0] : d
}
function formatDateTime(d) {
  if (!d) return '-'
  return typeof d === 'string' ? d.replace('T', ' ') : d
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

async function handleApprove(row) {
  approveForm.id = row.id
  approveForm.comment = ''
  approveVisible.value = true
}

async function doApprove() {
  approveSubmitting.value = true
  try {
    await approveReport(approveForm.id, { comment: approveForm.comment || null })
    ElMessage.success('已通过')
    approveVisible.value = false
    emit('approved')
    fetchData()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '操作失败')
  } finally {
    approveSubmitting.value = false
  }
}

function openReject(row) {
  rejectForm.id = row.id
  rejectForm.comment = ''
  rejectVisible.value = true
}

async function doReject() {
  const valid = await rejectFormRef.value.validate().catch(() => null)
  if (!valid) return
  rejectSubmitting.value = true
  try {
    await rejectReport(rejectForm.id, { comment: rejectForm.comment })
    ElMessage.success('已驳回')
    rejectVisible.value = false
    emit('approved')
    fetchData()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '操作失败')
  } finally {
    rejectSubmitting.value = false
  }
}

async function handleBatchApprove() {
  await ElMessageBox.confirm('确定全部通过所有待审批周报吗？', '批量审批', { type: 'warning' })
  loading.value = true
  let count = 0
  for (const row of tableData.value) {
    try {
      await approveReport(row.id, { comment: null })
      count++
    } catch { /* skip */ }
  }
  loading.value = false
  ElMessage.success(`已通过 ${count} 份周报`)
  emit('approved')
  fetchData()
}

onMounted(fetchData)
</script>

<template>
  <div v-loading="loading">
    <div v-if="tableData.length === 0 && !loading" class="empty-state">
      暂无待审批周报
    </div>

    <div v-else class="approval-list">
      <div class="approval-header">
        <span>共 {{ tableData.length }} 份待审批</span>
        <el-button type="success" size="small" @click="handleBatchApprove">全部通过</el-button>
      </div>

      <div v-for="row in tableData" :key="row.id" class="approval-card">
        <div class="card-header">
          <div class="card-user">
            <strong>{{ row.userName }}</strong>
            <span class="card-dept">{{ row.userDepartment }}</span>
          </div>
          <div class="card-actions">
            <el-button type="success" size="small" @click="handleApprove(row)">通过</el-button>
            <el-button type="danger" size="small" @click="openReject(row)">驳回</el-button>
          </div>
        </div>
        <div class="card-meta">
          提交时间：{{ formatDateTime(row.submittedAt) }} | 周期：{{ formatDate(row.weekStartDate) }} ~ {{ formatDate(row.weekEndDate) }}
        </div>

        <div class="card-work">
          <div v-if="row.doneWork" class="work-section">
            <h4>本周完成工作：</h4>
            <table class="work-table" v-if="renderItems(row.doneWork).length">
              <thead><tr><th style="width:40px">序号</th><th style="width:180px">事项</th><th>进展情况</th></tr></thead>
              <tbody>
                <tr v-for="(it, i) in renderItems(row.doneWork)" :key="i">
                  <td style="text-align:center">{{ i + 1 }}</td>
                  <td>{{ it.item }}</td>
                  <td>{{ it.progress || '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-if="row.planWork" class="work-section">
            <h4>下周工作计划：</h4>
            <table class="work-table" v-if="renderItems(row.planWork).length">
              <thead><tr><th style="width:40px">序号</th><th style="width:180px">事项</th><th>进展情况</th></tr></thead>
              <tbody>
                <tr v-for="(it, i) in renderItems(row.planWork)" :key="i">
                  <td style="text-align:center">{{ i + 1 }}</td>
                  <td>{{ it.item }}</td>
                  <td>{{ it.progress || '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-if="row.problems" class="work-section">
            <h4>遇到的问题：</h4>
            <p>{{ row.problems }}</p>
          </div>
          <div v-if="row.supportNeeded" class="work-section">
            <h4>需要支持：</h4>
            <p>{{ row.supportNeeded }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Approve dialog -->
    <el-dialog v-model="approveVisible" title="审批通过" width="420px">
      <el-form label-width="0">
        <el-form-item>
          <el-input v-model="approveForm.comment" type="textarea" :rows="3" placeholder="批注（选填）" maxlength="300" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="success" :loading="approveSubmitting" @click="doApprove">确认通过</el-button>
      </template>
    </el-dialog>

    <!-- Reject dialog -->
    <el-dialog v-model="rejectVisible" title="驳回周报" width="420px">
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="0">
        <el-form-item prop="comment">
          <el-input v-model="rejectForm.comment" type="textarea" :rows="3" placeholder="请填写驳回原因（必填）" maxlength="300" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectSubmitting" @click="doReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.empty-state {
  text-align: center;
  color: #909399;
  padding: 60px 0;
  font-size: 15px;
}
.approval-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.approval-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #606266;
}
.approval-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card-user strong {
  font-size: 15px;
}
.card-dept {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}
.card-actions {
  display: flex;
  gap: 8px;
}
.card-meta {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}
.card-work {
  background: #fafafa;
  border-radius: 4px;
  padding: 10px 14px;
}
.work-section h4 {
  font-size: 13px;
  margin: 8px 0 4px 0;
}
.work-section h4:first-child {
  margin-top: 0;
}
.work-section ul {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: #303133;
  line-height: 1.8;
}
.work-section p {
  margin: 0;
  font-size: 13px;
  color: #303133;
}
.work-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  margin-top: 4px;
}
.work-table th, .work-table td {
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
</style>
