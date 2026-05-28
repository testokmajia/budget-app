<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getMyReports, getTeamHistory, getAllHistory } from '@/api/weekly'

const userStore = useUserStore()

const loading = ref(false)
const tableData = ref([])
const filters = reactive({ from: '', to: '' })

const isClerkOrHead = computed(() => userStore.hasRole('ROLE_CLERK') || userStore.hasRole('ROLE_ADMIN'))

function formatDate(d) {
  if (!d) return '-'
  return typeof d === 'string' ? d.split('T')[0] : d
}
function formatDateTime(d) {
  if (!d) return '-'
  return typeof d === 'string' ? d.replace('T', ' ') : d
}

const statusLabels = { 'DRAFT': '草稿', 'PENDING_REVIEW': '已提交', 'APPROVED': '已通过', 'REJECTED': '已驳回' }
const statusTypes = { 'DRAFT': 'info', 'PENDING_REVIEW': 'warning', 'APPROVED': 'success', 'REJECTED': 'danger' }

async function fetchData() {
  loading.value = true
  try {
    const params = {}
    if (filters.from) params.from = filters.from
    if (filters.to) params.to = filters.to

    let res
    if (isClerkOrHead.value) {
      res = await getAllHistory(params)
    } else {
      // Try team history first (for leaders), fall back to personal
      try {
        res = await getTeamHistory(params)
      } catch {
        res = await getMyReports(params)
      }
    }
    tableData.value = res.data || []
  } catch {
    // Fall back to personal history
    try {
      const params2 = {}
      if (filters.from) params2.from = filters.from
      if (filters.to) params2.to = filters.to
      const res = await getMyReports(params2)
      tableData.value = res.data || []
    } catch (e) {
      ElMessage.error(e.response?.data?.error || '加载失败')
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <div>
    <div class="search-bar">
      <el-date-picker v-model="filters.from" type="date" placeholder="起始日期" value-format="YYYY-MM-DD" style="width: 150px" @change="fetchData" />
      <el-date-picker v-model="filters.to" type="date" placeholder="截止日期" value-format="YYYY-MM-DD" style="width: 150px" @change="fetchData" />
      <el-button @click="fetchData">查询</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="userName" label="姓名" width="90" />
      <el-table-column label="周期" width="200">
        <template #default="{ row }">
          {{ formatDate(row.weekStartDate) }} ~ {{ formatDate(row.weekEndDate) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTypes[row.status] || 'info'" size="small">
            {{ statusLabels[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="submittedAt" label="提交时间" width="160">
        <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
      </el-table-column>
      <el-table-column label="审批信息" min-width="180">
        <template #default="{ row }">
          <template v-if="row.reviewerName">
            <div>审批人：{{ row.reviewerName }}</div>
            <div v-if="row.reviewComment" style="color: #909399; font-size: 12px;">批注：{{ row.reviewComment }}</div>
            <div style="font-size: 12px; color: #c0c4cc;">{{ formatDateTime(row.reviewedAt) }}</div>
          </template>
          <span v-else style="color: #c0c4cc;">-</span>
        </template>
      </el-table-column>
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="expand-content">
            <div v-if="row.doneWork" class="expand-section">
              <h4>本周完成工作：</h4>
              <table class="work-table" v-if="(row.doneWork || '').split('\n').filter(s => s.trim()).length">
                <thead><tr><th style="width:40px">序号</th><th style="width:200px">事项</th><th>进展情况</th></tr></thead>
                <tbody>
                  <tr v-for="(line, i) in (row.doneWork || '').split('\n').filter(s => s.trim())" :key="i">
                    <td style="text-align:center">{{ i + 1 }}</td>
                    <td>{{ line.includes('|') ? line.split('|')[0] : line }}</td>
                    <td>{{ line.includes('|') ? line.split('|').slice(1).join('|') : '-' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-if="row.planWork" class="expand-section">
              <h4>下周工作计划：</h4>
              <table class="work-table" v-if="(row.planWork || '').split('\n').filter(s => s.trim()).length">
                <thead><tr><th style="width:40px">序号</th><th style="width:200px">事项</th><th>进展情况</th></tr></thead>
                <tbody>
                  <tr v-for="(line, i) in (row.planWork || '').split('\n').filter(s => s.trim())" :key="i">
                    <td style="text-align:center">{{ i + 1 }}</td>
                    <td>{{ line.includes('|') ? line.split('|')[0] : line }}</td>
                    <td>{{ line.includes('|') ? line.split('|').slice(1).join('|') : '-' }}</td>
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

    <div v-if="tableData.length === 0 && !loading" class="empty-state">暂无历史记录</div>
  </div>
</template>

<style scoped>
.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}
.empty-state {
  text-align: center;
  color: #909399;
  padding: 60px 0;
  font-size: 15px;
}
.expand-content {
  padding: 8px 20px;
  background: #f7f8fa;
  font-size: 13px;
}
.expand-section h4 {
  margin: 8px 0 4px 0;
  font-size: 13px;
}
.expand-section h4:first-child {
  margin-top: 0;
}
.expand-section ul {
  margin: 0;
  padding-left: 18px;
  line-height: 1.8;
}
.expand-section p {
  margin: 0;
}
.work-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  margin-top: 4px;
}
.work-table th, .work-table td {
  border: 1px solid #e4e7ed;
  padding: 6px 10px;
  text-align: left;
  vertical-align: top;
}
.work-table th {
  background: #f5f7fa;
  font-weight: 600;
  color: #606266;
}
</style>
