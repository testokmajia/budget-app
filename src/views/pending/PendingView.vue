<script setup>
import { ref, reactive, onMounted, computed, inject } from 'vue'
import { ElMessage } from 'element-plus'
import { getPendingIssues, completeAssignment } from '@/api/issue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const refreshBadges = inject('refreshBadges', null)

const loading = ref(false)
const tableData = ref([])

// 筛选
const filterStatus = ref('pending')
const filterOwner = ref('mine') // 'mine' | 'all'

// 完成备注弹窗
const noteVisible = ref(false)
const noteForm = reactive({ assignmentId: null, issueId: null, completionNote: '' })
const noteSubmitting = ref(false)
const noteFormRef = ref(null)
const currentRow = ref(null)

const noteRules = {
  completionNote: [
    { max: 300, message: '不能超过300字', trigger: 'blur' }
  ]
}

function formatDate(d) {
  if (!d) return '-'
  if (typeof d === 'string') return d.split('T')[0]
  return d
}
function formatDeadline(d) {
  if (!d) return '-'
  const s = typeof d === 'string' ? d : String(d)
  if (s.includes('2099-12-31')) return '长期工作'
  return s.split('T')[0]
}

const queryParams = computed(() => {
  const p = { status: filterStatus.value }
  if (filterOwner.value === 'mine') {
    p.ownerId = userStore.user?.id
  } else {
    p.ownerId = -1
  }
  return p
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getPendingIssues(queryParams.value)
    tableData.value = res.data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '加载失败')
  } finally {
    loading.value = false
  }
}

function openNote(row) {
  currentRow.value = row
  noteForm.assignmentId = row.assignmentId
  noteForm.issueId = row.issueId
  noteForm.completionNote = row.completionNote || ''
  noteVisible.value = true
}

async function submitNote() {
  noteSubmitting.value = true
  try {
    await completeAssignment(noteForm.issueId, noteForm.assignmentId, {
      completionNote: noteForm.completionNote
    })
    ElMessage.success('已标记完成')
    noteVisible.value = false
    refreshBadges?.()
    fetchData()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '操作失败')
  } finally {
    noteSubmitting.value = false
  }
}

function handleFilterChange() {
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>系统问题实施</h2>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-left">
        <span class="filter-label">状态：</span>
        <el-radio-group v-model="filterStatus" @change="handleFilterChange" size="small">
          <el-radio-button value="pending">未完成</el-radio-button>
          <el-radio-button value="completed">已完成</el-radio-button>
          <el-radio-button value="all">全部</el-radio-button>
        </el-radio-group>
        <span class="filter-label" style="margin-left: 16px;">负责人：</span>
        <el-radio-group v-model="filterOwner" @change="handleFilterChange" size="small">
          <el-radio-button value="mine">仅看自己</el-radio-button>
          <el-radio-button value="all">查看全部</el-radio-button>
        </el-radio-group>
      </div>
      <div class="filter-right">
        <el-button size="small" @click="fetchData">刷新</el-button>
      </div>
    </div>

    <div class="table-wrap">
    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="issueCode" label="问题编号" min-width="140" />
      <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
      <el-table-column prop="submitterName" label="提出人" min-width="80" />
      <el-table-column prop="systemName" label="涉及系统" min-width="110" />
      <el-table-column prop="systemOwnerName" label="系统负责人" min-width="90" />
      <el-table-column prop="permanentDeadline" label="永久解决时限" min-width="110">
        <template #default="{ row }">{{ formatDeadline(row.permanentDeadline) }}</template>
      </el-table-column>
      <el-table-column label="完成状态" min-width="100">
        <template #default="{ row }">
          <el-tag :type="row.completed ? 'success' : 'info'" size="small">
            {{ row.completed ? '已完成' : '未完成' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.canComplete"
            type="success" size="small"
            @click="openNote(row)"
          >确认已完成</el-button>
          <el-tag v-else-if="row.completed && row.systemOwnerId === userStore.user?.id"
            type="success" size="small">已完成</el-tag>
          <span v-else style="color: #c0c4cc; font-size: 13px;">-</span>
        </template>
      </el-table-column>
    </el-table>
    </div>

    <div v-if="tableData.length === 0 && !loading" style="text-align: center; color: #909399; margin-top: 40px;">
      暂无数据
    </div>

    <!-- 完成情况备注弹窗 -->
    <el-dialog v-model="noteVisible" title="确认已完成" width="640px" :close-on-click-modal="false">
      <div v-if="currentRow" class="note-issue-info">
        <div class="ni-row"><span class="ni-label">问题编号：</span>{{ currentRow.issueCode }}</div>
        <div class="ni-row"><span class="ni-label">标题：</span>{{ currentRow.title }}</div>
        <div class="ni-row"><span class="ni-label">问题描述：</span>{{ currentRow.description }}</div>
        <div class="ni-row"><span class="ni-label">涉及系统：</span>{{ currentRow.systemName }}</div>
        <div v-if="currentRow.temporarySolution" class="ni-row">
          <span class="ni-label">临时整改方案：</span>{{ currentRow.temporarySolution }}
        </div>
        <div v-if="currentRow.temporaryDeadline" class="ni-row">
          <span class="ni-label">临时整改时限：</span>{{ formatDate(currentRow.temporaryDeadline) }}
        </div>
        <div v-if="currentRow.permanentSolution" class="ni-row">
          <span class="ni-label">永久解决方案：</span>{{ currentRow.permanentSolution }}
        </div>
        <div v-if="currentRow.permanentDeadline" class="ni-row">
          <span class="ni-label">永久解决时限：</span>{{ formatDeadline(currentRow.permanentDeadline) }}
        </div>
        <div v-if="currentRow.rootCause" class="ni-row">
          <span class="ni-label">产生原因：</span>{{ currentRow.rootCause }}
        </div>
      </div>
      <el-form ref="noteFormRef" :model="noteForm" :rules="noteRules" label-width="0" style="margin-top: 16px;">
        <el-form-item prop="completionNote">
          <el-input
            v-model="noteForm.completionNote"
            type="textarea"
            :rows="5"
            maxlength="300"
            show-word-limit
            placeholder="请填写完成情况（选填，最多300字）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noteVisible = false">取消</el-button>
        <el-button type="primary" :loading="noteSubmitting" @click="submitNote">确认完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header h2 {
  margin: 0;
}
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 16px 0;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 6px;
}
.filter-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.filter-label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}
.table-wrap {
  overflow-x: auto;
}
.note-issue-info {
  background: #f7f8fa;
  border-radius: 6px;
  padding: 12px 16px;
  font-size: 13px;
  line-height: 1.8;
}
.ni-row {
  color: #303133;
}
.ni-label {
  color: #909399;
  font-weight: 500;
}
</style>
