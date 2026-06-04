<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getList, getStats, getById, deptReview, getComments, confirm, reject } from '@/api/testReport'
import { getFilterOptions } from '@/api/requirement'
import { nameEquals, nameInArray, filterByName } from '@/utils/compare'
import TestReportForm from './components/TestReportForm.vue'

const userStore = useUserStore()

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
const comments = ref([])
const actionLoading = ref(false)
const confirmedDate = ref('')
const userOptions = ref([])
const showAdvFilter = ref(false)

const statusOptions = ['待确认', '待部门审核', '已确认']

function statusTagType(s) {
  if (s === '已确认') return 'success'
  if (s === '待部门审核') return 'warning'
  if (s === '待确认') return 'info'
  return 'info'
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
    confirmedDate.value = new Date().toISOString().substring(0, 10)
    detailVisible.value = true
    loadComments(row.id)
  } catch (e) { ElMessage.error('加载详情失败') }
}

async function loadComments(reportId) {
  try {
    const res = await getComments(reportId)
    comments.value = res.data || []
  } catch { comments.value = [] }
}

function parseReviewers(json) {
  if (!json) return []
  try { return typeof json === 'string' ? JSON.parse(json) : json } catch { return [] }
}

function parseSystems(json) {
  if (!json) return []
  try { return typeof json === 'string' ? JSON.parse(json) : json } catch { return [] }
}

// 当前用户是否是部门审核人（待部门审核状态）
const isDeptReviewer = computed(() => {
  const name = userStore.user?.name
  if (!name || !detailData.value) return false
  if (detailData.value.status !== '待部门审核') return false
  try {
    const reviewers = typeof detailData.value.deptReviewers === 'string'
      ? JSON.parse(detailData.value.deptReviewers) : (detailData.value.deptReviewers || [])
    // 修复：对字符串数组使用 nameInArray 进行 trim 后比较
    return nameInArray(reviewers, name)
  } catch { return false }
})

// 当前用户是否是待确认的确认人员（待确认状态 + 在列表中 + 未确认）
const isReviewer = computed(() => {
  const name = userStore.user?.name
  if (!name || !detailData.value) return false
  if (detailData.value.status !== '待确认') return false
  const persons = parseReviewers(detailData.value.reviewPersons)
  return persons.some(p => {
    const pName = (p.name || '').trim()
    return (pName === name || pName === name.trim()) && !p.confirmed
  })
})

// 部门审核是否已完成
const myDeptApproved = computed(() => {
  if (!detailData.value?.deptReviewStatus) return false
  try {
    const status = typeof detailData.value.deptReviewStatus === 'string'
      ? JSON.parse(detailData.value.deptReviewStatus) : detailData.value.deptReviewStatus
    return Object.values(status).some(v => v === 'approved')
  } catch { return false }
})

async function doConfirm() {
  actionLoading.value = true
  try {
    await confirm(detailData.value.id, { comment: '确认', confirmedDate: confirmedDate.value })
    ElMessage.success('已确认')
    detailVisible.value = false
    loadData(); loadStats()
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { actionLoading.value = false }
}

async function doReject() {
  actionLoading.value = true
  try {
    await reject(detailData.value.id, { comment: '驳回' })
    ElMessage.success('已驳回，重置所有确认状态')
    detailVisible.value = false
    loadData()
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { actionLoading.value = false }
}

async function doDeptApprove() {
  actionLoading.value = true
  try {
    await deptReview(detailData.value.id, { comment: '审核通过' })
    ElMessage.success('审核通过')
    detailVisible.value = false
    loadData(); loadStats()
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { actionLoading.value = false }
}

async function doDeptReject() {
  actionLoading.value = true
  try {
    await deptReview(detailData.value.id, { comment: 'reject' })
    ElMessage.success('已驳回，重置所有确认状态')
    detailVisible.value = false
    loadData()
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { actionLoading.value = false }
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
        v-for="s in ['全部','待确认','待部门审核','已确认']" :key="s"
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
        <el-table-column prop="reportCode" label="报告编号" width="130" />
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
    <el-dialog v-model="detailVisible" title="测试报告详情" width="1000px" destroy-on-close @closed="loadData(); loadStats()">
      <div v-if="detailData" class="tr-detail-layout">
        <!-- 左栏：基本信息 -->
        <div class="tr-detail-left">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="报告编号">{{ detailData.reportCode }}</el-descriptions-item>
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
              <template v-if="detailData.testReportPath">
                <el-link type="primary" :href="`/api/files/download?path=${encodeURIComponent(detailData.testReportPath)}&name=${encodeURIComponent(detailData.testReportName || detailData.testReportPath.split('/').pop())}`" target="_blank">
                  📄 {{ detailData.testReportName || detailData.testReportPath.split('/').pop() }}
                </el-link>
              </template>
              <span v-else style="color: #a8abb2">未上传</span>
            </el-descriptions-item>
          </el-descriptions>

          <!-- 确认人员操作（仅确认人员可见，状态=待确认） -->
          <div v-if="detailData.status === '待确认' && isReviewer" class="detail-actions">
            <h4 class="section-title">确认操作</h4>
            <div class="confirm-date-row">
              <span class="confirm-date-label">确认日期：</span>
              <el-date-picker v-model="confirmedDate" type="date" placeholder="选择确认日期" value-format="YYYY-MM-DD" style="width:160px" size="small" />
            </div>
            <div class="confirm-btns">
              <el-button type="danger" :loading="actionLoading" @click="doReject">驳回</el-button>
              <el-button type="primary" :loading="actionLoading" @click="doConfirm">确认通过</el-button>
            </div>
          </div>
          <!-- 非确认人员查看确认进度 -->
          <div v-else-if="detailData.status === '待确认'" class="detail-actions">
            <p v-if="parseReviewers(detailData.reviewPersons).some(p => (p.name||'').trim() === (userStore.user?.name||'').trim() && p.confirmed)" class="confirmed-tip">✅ 您已确认</p>
            <p v-else class="confirmed-tip">⏳ 等待确认人确认中…</p>
          </div>

          <!-- 确认人员进度列表（所有状态都显示，只要有人） -->
          <div v-if="parseReviewers(detailData.reviewPersons).length" class="confirm-progress-bar" :style="{ marginTop: detailData.status === '待确认' ? '0' : '16px' }">
            <div class="progress-text">
              确认进度：{{ parseReviewers(detailData.reviewPersons).filter(r => r.confirmed).length }}/{{ parseReviewers(detailData.reviewPersons).length }}
            </div>
            <div class="reviewer-progress-list">
              <div
                v-for="(rv, i) in parseReviewers(detailData.reviewPersons)"
                :key="i"
                class="reviewer-progress-item"
                :class="{ confirmed: rv.confirmed }"
              >
                <span class="rpi-dot">●</span>
                <span class="rpi-name">{{ rv.name }}</span>
                <span class="rpi-role">{{ rv.role }}</span>
                <span v-if="rv.system" class="rpi-system">{{ rv.system }}</span>
                <span v-if="rv.confirmed" class="rpi-status done">✓ {{ rv.confirmedAt?.substring(0, 10) || '已确认' }}</span>
                <span v-else class="rpi-status pending">待确认</span>
              </div>
            </div>
          </div>

          <!-- 操作按钮（仅确认人员可见） -->
          <!-- 部门审核操作（仅部门审核人可见，状态=待部门审核） -->
          <div v-if="detailData.status === '待部门审核' && isDeptReviewer" class="detail-actions">
            <h4 class="section-title">部门审核</h4>
            <p class="action-tip">需要您进行部门审核：</p>
            <div class="confirm-btns">
              <el-button type="danger" :loading="actionLoading" @click="doDeptReject">驳回</el-button>
              <el-button type="primary" :loading="actionLoading" @click="doDeptApprove">审核通过</el-button>
            </div>
          </div>
          <!-- 非审核人查看部门审核状态 -->
          <div v-else-if="detailData.status === '待部门审核'" class="detail-actions">
            <p class="confirmed-tip">⏳ 等待部门负责人审核中…</p>
          </div>
        </div>

        <!-- 右栏：审核记录 -->
        <div class="tr-detail-right">
          <div class="approval-panel" v-if="comments.length">
            <h4 class="section-title">审核记录</h4>
            <div class="approval-feed">
              <div
                v-for="(c, i) in comments"
                :key="c.id"
                class="feed-node"
                :class="{ passed: true }"
              >
                <div class="feed-indicator">
                  <div class="feed-dot"></div>
                  <div v-if="i < comments.length - 1" class="feed-line"></div>
                </div>
                <div class="feed-content">
                  <div class="feed-node-header">
                    <span class="feed-node-name">{{ c.author }}</span>
                    <span class="feed-node-role">{{ c.role }}</span>
                    <el-tag
                      :type="c.action.includes('驳回') ? 'danger' : c.action.includes('通过') ? 'success' : 'primary'"
                      size="small"
                    >{{ c.action }}</el-tag>
                  </div>
                  <div class="feed-node-comment">{{ c.content }}</div>
                  <div class="feed-node-time" v-if="c.createdAt">{{ c.createdAt?.substring(0, 16) }}</div>
                </div>
              </div>
            </div>
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
.tr-detail-right { width: 300px; flex-shrink: 0; }
.tr-detail-right .approval-panel {
  position: sticky; top: 0; max-height: 60vh; overflow-y: auto;
  background: #fafbfc; border: 1px solid #ebeef5; border-radius: 4px; padding: 12px;
}
.tr-detail-right .section-title { margin-top: 0; }

.section-title { font-size: 13px; font-weight: 600; color: #303133; margin-bottom: 10px;
  padding-bottom: 8px; border-bottom: 1px solid #ebeef5; }

.detail-actions { margin-top: 16px; }
.confirm-date-row { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.confirm-date-label { font-size: 13px; color: #606266; white-space: nowrap; }
.date-warning { font-size: 12px; color: #e6a23c; }
.confirm-btns { display: flex; gap: 8px; justify-content: flex-end; }
.confirmed-tip { font-size: 14px; color: #0abf5b; font-weight: 500; }

/* 确认进度条 */
.confirm-progress-bar { margin-top: 12px; padding-top: 10px; border-top: 1px solid #ebeef5; }
.progress-text { font-size: 12px; color: #606266; margin-bottom: 6px; }

/* ======== 审批时间线（参照需求详情） ======== */
.approval-feed { padding: 4px 0; }
.feed-node { display: flex; gap: 10px; position: relative; }
.feed-node:last-child .feed-content { padding-bottom: 0; }

.feed-indicator { display: flex; flex-direction: column; align-items: center; width: 20px; flex-shrink: 0; padding-top: 4px; }
.feed-dot {
  width: 8px; height: 8px; border-radius: 50%;
  border: 2px solid #d6dbe3; background: #fff; z-index: 1; flex-shrink: 0;
}
.feed-node.passed .feed-dot { background: #0abf5b; border-color: #0abf5b; }
.feed-node.current .feed-dot {
  background: #fff; border-color: #006eff;
  box-shadow: 0 0 0 3px rgba(0,110,255,0.18);
}
.feed-line { width: 2px; flex: 1; min-height: 16px; background: #ebeef5; margin-top: 4px; }
.feed-node.passed .feed-line { background: #0abf5b; }

.feed-content { flex: 1; padding-bottom: 10px; min-width: 0; }
.feed-node-header { display: flex; align-items: center; gap: 6px; margin-bottom: 2px; flex-wrap: wrap; }
.feed-node-name { font-size: 13px; font-weight: 500; color: #303133; }
.feed-node-role { font-size: 11px; color: #a8abb2; }
.feed-node.passed .feed-node-name { color: #606266; }
.feed-node-sys { font-size: 11px; color: #909399; margin-bottom: 2px; }
.feed-node-status { display: flex; align-items: center; gap: 6px; margin-top: 3px; }
.feed-node-time { font-size: 11px; color: #a8abb2; }
.feed-node-comment { font-size: 12px; color: #606266; margin-top: 3px; line-height: 1.5; }

/* 确认人进度列表 */
.reviewer-progress-list { display: flex; flex-direction: column; gap: 4px; margin-top: 8px; }
.reviewer-progress-item {
  display: flex; align-items: center; gap: 8px; padding: 6px 10px;
  background: #f5f7fa; border-radius: 4px; font-size: 13px;
}
.reviewer-progress-item.confirmed { background: #f0fdf4; }
.rpi-dot { font-size: 10px; }
.reviewer-progress-item.confirmed .rpi-dot { color: #0abf5b; }
.reviewer-progress-item:not(.confirmed) .rpi-dot { color: #c0c4cc; }
.rpi-name { font-weight: 500; color: #303133; min-width: 50px; }
.rpi-role { color: #909399; font-size: 12px; }
.rpi-system { color: #a8abb2; font-size: 12px; }
.rpi-status { margin-left: auto; font-size: 12px; }
.rpi-status.done { color: #0abf5b; }
.rpi-status.pending { color: #e6a23c; }
</style>
