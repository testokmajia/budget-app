<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { getList, getStats, confirm, reject, getById } from '@/api/testReport'
import { getFilterOptions } from '@/api/requirement'
import TestReportForm from './components/TestReportForm.vue'

const loading = ref(false)
const tableData = ref([])
const stats = ref({})
const currentPage = ref(1)
const pageSize = ref(20)
const totalItems = ref(0)
const filters = reactive({
  keyword: '',
  status: [],
  submitterId: null,
  systemName: '',
  requirementTitle: '',
  createdAtFrom: '',
  createdAtTo: '',
  confirmedAtFrom: '',
  confirmedAtTo: '',
})

const formVisible = ref(false)
const detailVisible = ref(false)
const detailData = ref(null)
const actionLoading = ref(false)
const confirmDate = ref('')
const userOptions = ref([])
const showAdvFilter = ref(false)

const statusOptions = ['草稿', '已确认']

const dateWarning = computed(() => {
  if (!confirmDate.value || !detailData.value?.plannedProductionDate) return ''
  if (confirmDate.value >= detailData.value.plannedProductionDate) {
    return '注意：确认日期应早于计划投产日期（' + detailData.value.plannedProductionDate + '）'
  }
  return ''
})

function disabledConfirmDate(time) {
  if (detailData.value?.plannedProductionDate) {
    return time.getTime() >= new Date(detailData.value.plannedProductionDate).getTime()
  }
  return false
}

function statusTagType(s) {
  return s === '已确认' ? 'success' : 'info'
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1, size: pageSize.value,
      keyword: filters.keyword || undefined,
      status: filters.status.length ? filters.status.join(',') : undefined,
      submitterId: filters.submitterId || undefined,
      systemName: filters.systemName || undefined,
      requirementTitle: filters.requirementTitle || undefined,
      createdAtFrom: filters.createdAtFrom || undefined,
      createdAtTo: filters.createdAtTo || undefined,
      confirmedAtFrom: filters.confirmedAtFrom || undefined,
      confirmedAtTo: filters.confirmedAtTo || undefined,
    }
    const res = await getList(params)
    tableData.value = res.data?.content || []
    totalItems.value = res.data?.totalElements || 0
  } catch (e) { ElMessage.error('加载失败') } finally { loading.value = false }
}

async function loadStats() { try { const r = await getStats(); stats.value = r.data } catch {} }

async function loadUsers() {
  try {
    const res = await getFilterOptions()
    userOptions.value = res.data?.submitters || []
  } catch (e) { /* ignore */ }
}

function handleSearch() { currentPage.value = 1; loadData() }

function clearFilters() {
  filters.keyword = ''; filters.status = []; filters.submitterId = null
  filters.systemName = ''; filters.requirementTitle = ''
  filters.createdAtFrom = ''; filters.createdAtTo = ''
  filters.confirmedAtFrom = ''; filters.confirmedAtTo = ''
  handleSearch()
}

function filterByStatus(status) {
  if (filters.status.includes(status)) {
    filters.status = filters.status.filter(s => s !== status)
  } else {
    filters.status = [...filters.status, status]
  }
  handleSearch()
}

async function viewDetail(row) {
  try {
    const res = await getById(row.id)
    detailData.value = res.data
    confirmDate.value = new Date().toISOString().substring(0, 10)
    detailVisible.value = true
  } catch (e) { ElMessage.error('加载详情失败') }
}

async function doConfirm() {
  if (!confirmDate.value) { ElMessage.warning('请选择确认日期'); return }
  actionLoading.value = true
  try {
    await confirm(detailData.value.id, { confirmedDate: confirmDate.value })
    ElMessage.success('确认成功')
    detailVisible.value = false
    loadData(); loadStats()
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { actionLoading.value = false }
}

async function doReject() {
  actionLoading.value = true
  try {
    await reject(detailData.value.id, {})
    ElMessage.success('已驳回（重置所有确认状态）')
    detailVisible.value = false
    loadData()
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { actionLoading.value = false }
}

function parseReviewers(json) {
  if (!json) return []
  try { return typeof json === 'string' ? JSON.parse(json) : json } catch { return [] }
}

function parseSystems(json) {
  if (!json) return []
  try { return typeof json === 'string' ? JSON.parse(json) : json } catch { return [] }
}

function onFormSuccess() { formVisible.value = false; handleSearch(); loadStats() }

function handlePageChange(page) { currentPage.value = page; loadData() }
function handleSizeChange(size) { pageSize.value = size; currentPage.value = 1; loadData() }

onMounted(() => { loadData(); loadStats(); loadUsers() })
</script>

<template>
  <div class="tr-view">
    <!-- 统计 -->
    <div class="stats-row">
      <div
        v-for="s in ['全部','草稿','已确认']" :key="s"
        class="stat-card"
        :class="{ active: s !== '全部' && filters.status.includes(s) }"
        @click="filterByStatus(s)"
      >
        <div class="stat-label">{{ s }}</div>
        <div class="stat-value">{{ stats[s] || 0 }}</div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-top">
        <div class="toolbar-left">
          <el-input v-model="filters.keyword" placeholder="搜索标题/需求编号…" clearable style="width:200px"
            :prefix-icon="Search" @keyup.enter="handleSearch" @clear="handleSearch" />
          <el-select v-model="filters.status" placeholder="状态（多选）" multiple clearable style="width:160px" @change="handleSearch">
            <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
          </el-select>
          <el-button text @click="showAdvFilter = !showAdvFilter">
            {{ showAdvFilter ? '🔼 收起' : '🔽 更多筛选' }}
          </el-button>
        </div>
        <el-button type="primary" :icon="Plus" @click="formVisible = true">新建测试报告</el-button>
      </div>
      <div v-if="showAdvFilter" class="toolbar-adv">
        <el-select v-model="filters.submitterId" placeholder="上传人" clearable filterable style="width:140px" @change="handleSearch">
          <el-option v-for="u in userOptions" :key="u.id" :label="u.name" :value="u.id" />
        </el-select>
        <el-input v-model="filters.systemName" placeholder="信息系统" clearable style="width:150px" @keyup.enter="handleSearch" @clear="handleSearch" />
        <el-input v-model="filters.requirementTitle" placeholder="需求标题" clearable style="width:150px" @keyup.enter="handleSearch" @clear="handleSearch" />
        <span class="filter-label">创建时间</span>
        <el-date-picker v-model="filters.createdAtFrom" type="date" placeholder="起" value-format="YYYY-MM-DD" style="width:130px" @change="handleSearch" />
        <span style="color:#999">—</span>
        <el-date-picker v-model="filters.createdAtTo" type="date" placeholder="止" value-format="YYYY-MM-DD" style="width:130px" @change="handleSearch" />
        <span class="filter-label">确认时间</span>
        <el-date-picker v-model="filters.confirmedAtFrom" type="date" placeholder="起" value-format="YYYY-MM-DD" style="width:130px" @change="handleSearch" />
        <span style="color:#999">—</span>
        <el-date-picker v-model="filters.confirmedAtTo" type="date" placeholder="止" value-format="YYYY-MM-DD" style="width:130px" @change="handleSearch" />
        <el-button text @click="clearFilters">清除筛选</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-wrap">
      <el-table :data="tableData" stripe border v-loading="loading" @row-click="viewDetail" style="cursor: pointer">
        <el-table-column prop="title" label="测试报告标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="requirementCodes" label="关联需求" width="240" show-overflow-tooltip />
        <el-table-column label="涉及系统" width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ parseSystems(row.systemNames).join('、') }}</template>
        </el-table-column>
        <el-table-column prop="submitterName" label="上传人" width="90" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="plannedTestDate" label="计划测试" width="110" />
        <el-table-column prop="createdAt" label="创建时间" width="110">
          <template #default="{ row }">{{ row.createdAt?.substring(0, 10) }}</template>
        </el-table-column>
        <el-table-column label="确认时间" width="110">
          <template #default="{ row }">{{ row.confirmedAt?.substring(0, 10) || '—' }}</template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage" v-model:page-size="pageSize" :total="totalItems"
          :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next,jumper"
          @current-change="handlePageChange" @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="测试报告详情" width="1000px" destroy-on-close>
      <div v-if="detailData" class="tr-detail-layout">
        <!-- 左栏：基本信息 -->
        <div class="tr-detail-left">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="标题" :span="2">{{ detailData.title }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTagType(detailData.status)" size="small">{{ detailData.status }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="计划测试">{{ detailData.plannedTestDate || '—' }}</el-descriptions-item>
            <el-descriptions-item label="计划投产">{{ detailData.plannedProductionDate || '—' }}</el-descriptions-item>
            <el-descriptions-item label="上传人">{{ detailData.submitterName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="关联需求" :span="2">{{ detailData.requirementCodes }}</el-descriptions-item>
            <el-descriptions-item label="涉及系统" :span="2">{{ parseSystems(detailData.systemNames).join('、') }}</el-descriptions-item>
            <el-descriptions-item label="测试报告附件" :span="2">
              <span v-if="detailData.testReportPath">{{ detailData.testReportPath }}</span>
              <span v-else style="color: #a8abb2">未上传</span>
            </el-descriptions-item>
          </el-descriptions>

          <!-- 操作按钮 -->
          <div v-if="detailData.status === '草稿'" class="detail-actions">
            <div class="confirm-date-row">
              <span class="confirm-date-label">确认日期：</span>
              <el-date-picker
                v-model="confirmDate"
                type="date"
                placeholder="选择确认日期"
                value-format="YYYY-MM-DD"
                style="width: 180px"
                :disabled-date="disabledConfirmDate"
              />
              <span v-if="dateWarning" class="date-warning">{{ dateWarning }}</span>
            </div>
            <div class="confirm-btns">
              <el-button type="danger" :loading="actionLoading" @click="doReject">驳回（重置所有确认）</el-button>
              <el-button type="primary" :loading="actionLoading" @click="doConfirm">确认</el-button>
            </div>
          </div>
        </div>

        <!-- 右栏：确认人员状态 -->
        <div class="tr-detail-right">
          <div class="approval-panel" v-if="parseReviewers(detailData.reviewPersons).length">
            <h4 class="section-title">确认人员</h4>
            <div class="reviewer-grid">
              <div
                v-for="(r, i) in parseReviewers(detailData.reviewPersons)"
                :key="i"
                class="reviewer-card"
                :class="{ confirmed: r.confirmed }"
              >
                <span class="rc-name">{{ r.name }}</span>
                <span class="rc-role">{{ r.role }}</span>
                <span class="rc-system">{{ r.system }}</span>
                <el-tag v-if="r.confirmed" type="success" size="small">已确认</el-tag>
                <el-tag v-else type="info" size="small">待确认</el-tag>
                <span v-if="r.confirmed && r.confirmedAt" class="rc-time">{{ r.confirmedAt?.substring(0, 16) }}</span>
                <span v-if="r.confirmed && r.confirmedDate" class="rc-date">确认日: {{ r.confirmedDate }}</span>
              </div>
            </div>
            <p class="confirm-progress">
              确认进度：{{ parseReviewers(detailData.reviewPersons).filter(r=>r.confirmed).length }}/{{ parseReviewers(detailData.reviewPersons).length }}
            </p>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 新建弹窗 -->
    <TestReportForm v-model:visible="formVisible" @success="onFormSuccess" />
  </div>
</template>

<style scoped>
.tr-view { padding: 0; }
.stats-row { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-card { flex: 1; background: #fff; border-radius: 6px; padding: 14px 16px; cursor: pointer;
  box-shadow: 0 1px 2px rgba(0,0,0,.03), 0 1px 6px -1px rgba(0,0,0,.02); border-left: 4px solid #006eff;
}
.stat-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,.06); }
.stat-card.active { background: #f0f5ff; }
.stat-label { font-size: 12px; color: #606266; margin-bottom: 4px; }
.stat-value { font-size: 22px; font-weight: 600; color: #006eff; }
.toolbar { background: #fff; padding: 12px 16px; border-radius: 2px; margin-bottom: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,.04); }
.toolbar-top { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.toolbar-left { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.toolbar-adv { display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  padding-top: 10px; border-top: 1px solid #ebeef5; margin-top: 10px; }
.filter-label { font-size: 12px; color: #a8abb2; white-space: nowrap; }
.table-wrap { background: #fff; border: 1px solid #e8e8e8; border-radius: 2px; }
.pagination-wrap { display: flex; justify-content: flex-end; padding: 12px 16px; }

/* 详情两栏布局 */
.tr-detail-layout { display: flex; gap: 20px; }
.tr-detail-left { flex: 1; min-width: 0; }
.tr-detail-right { width: 280px; flex-shrink: 0; }
.tr-detail-right .approval-panel {
  position: sticky; top: 0; max-height: 60vh; overflow-y: auto;
  background: #fafbfc; border: 1px solid #ebeef5; border-radius: 4px; padding: 12px;
}
.tr-detail-right .section-title { margin-top: 0; }

.section-title { font-size: 13px; font-weight: 600; color: #303133; margin-bottom: 10px;
  padding-bottom: 8px; border-bottom: 1px solid #ebeef5; }
.reviewer-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.reviewer-card { padding: 8px 12px; background: #fafafa; border-radius: 4px; border: 1px solid #ebeef5;
  display: flex; align-items: center; gap: 8px; font-size: 13px; }
.reviewer-card.confirmed { background: #e8f8ee; border-color: #c6f0d5; }
.rc-name { font-weight: 500; } .rc-role { color: #909399; font-size: 12px; }
.rc-system { color: #a8abb2; font-size: 12px; }
.confirm-progress { font-size: 12px; color: #a8abb2; margin-top: 8px; }
.detail-actions { margin-top: 16px; }
.confirm-date-row { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.confirm-date-label { font-size: 13px; color: #606266; white-space: nowrap; }
.date-warning { font-size: 12px; color: #e6a23c; }
.confirm-btns { display: flex; gap: 8px; justify-content: flex-end; }
.rc-time { font-size: 11px; color: #909399; width: 100%; margin-top: 2px; }
.rc-date { font-size: 11px; color: #606266; width: 100%; margin-top: 1px; }
.reviewer-card { flex-direction: column; align-items: flex-start; }
</style>
