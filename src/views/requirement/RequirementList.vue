<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search, Edit } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getList, getStats, getFilterOptions } from '@/api/requirement'
import RequirementForm from './components/RequirementForm.vue'
import RequirementDetail from './components/RequirementDetail.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const tableData = ref([])
const stats = ref({})
const filterOptions = ref({})

// 分页
const currentPage = ref(1)
const pageSize = ref(20)
const totalItems = ref(0)

// 筛选
const filters = reactive({
  keyword: '',
  status: '',
  priority: '',
  dept: '',
  submitterId: null,
  sysOwner: '',
  dateFrom: '',
  dateTo: '',
  currentNode: '',
})
const showAdvFilter = ref(false)

// 弹窗
const formVisible = ref(false)
const detailVisible = ref(false)
const detailId = ref(null)

// 状态/优先级选项
const statusOptions = ['草稿', '审批中', '需求已确认', '实施中', '测试通过', '已投产', '已关闭', '已驳回']
const priorityOptions = ['紧急', '高', '普通', '低']

// 统计卡片配置
const statCards = [
  { key: '全部', label: '全部需求', color: '#006eff' },
  { key: '审批中', label: '审批中', color: '#ff9c00' },
  { key: '实施中', label: '实施中', color: '#006eff' },
  { key: '测试通过', label: '测试通过', color: '#0abf5b' },
  { key: '已关闭', label: '已关闭', color: '#909399' },
]

// 状态tag类型映射
function statusTagType(status) {
  const map = {
    '草稿': 'info', '审批中': 'primary', '需求已确认': 'success',
    '实施中': 'warning', '测试通过': 'success', '已投产': '', '已关闭': 'info', '已驳回': 'danger'
  }
  return map[status] || 'info'
}

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      priority: filters.priority || undefined,
      dept: filters.dept || undefined,
      submitterId: filters.submitterId || undefined,
      sysOwner: filters.sysOwner || undefined,
      dateFrom: filters.dateFrom || undefined,
      dateTo: filters.dateTo || undefined,
      currentNode: filters.currentNode || undefined,
    }
    const res = await getList(params)
    tableData.value = res.data?.content || []
    totalItems.value = res.data?.totalElements || 0
  } catch (e) {
    ElMessage.error('加载需求列表失败')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await getStats()
    stats.value = res.data
  } catch (e) { /* ignore */ }
}

async function loadFilterOptions() {
  try {
    const res = await getFilterOptions()
    filterOptions.value = res.data
  } catch (e) { /* ignore */ }
}

// 搜索
function handleSearch() {
  currentPage.value = 1
  loadData()
}

// 统计卡片点击筛选
function filterByStatus(status) {
  filters.status = filters.status === status ? '' : status
  handleSearch()
}

// 清除全部筛选（含URL参数带入的status和currentNode）
function clearAdvFilter() {
  filters.keyword = ''
  filters.status = ''
  filters.priority = ''
  filters.dept = ''
  filters.submitterId = null
  filters.sysOwner = ''
  filters.dateFrom = ''
  filters.dateTo = ''
  filters.currentNode = ''
  handleSearch()
}

// 新建
function openCreate() {
  formVisible.value = true
}

function onFormSuccess() {
  formVisible.value = false
  handleSearch()
  loadStats()
}

// 查看详情
function viewDetail(id) {
  detailId.value = id
  detailVisible.value = true
}

function onDetailClose() {
  detailVisible.value = false
  loadData()
  loadStats()
}

// 分页
function handlePageChange(page) {
  currentPage.value = page
  loadData()
}
function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  loadData()
}

onMounted(() => {
  // 支持从URL参数初始化筛选（来自首页待办跳转）
  if (route.query.status) filters.status = route.query.status
  if (route.query.currentNode) filters.currentNode = route.query.currentNode
  loadData()
  loadStats()
  loadFilterOptions()
})
</script>

<template>
  <div class="requirement-list">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div
        v-for="card in statCards" :key="card.key"
        class="stat-card"
        :class="{ active: filters.status === card.key }"
        :style="{ borderLeftColor: card.color }"
        @click="filterByStatus(card.key)"
      >
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value" :style="{ color: card.color }">{{ stats[card.key] || 0 }}</div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-top">
        <div class="toolbar-left">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索需求编号、标题…"
            :prefix-icon="Search"
            clearable
            style="width: 240px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 120px" @change="handleSearch">
            <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
          </el-select>
          <el-select v-model="filters.priority" placeholder="全部优先级" clearable style="width: 130px" @change="handleSearch">
            <el-option v-for="p in priorityOptions" :key="p" :label="p" :value="p" />
          </el-select>
          <el-button text @click="showAdvFilter = !showAdvFilter">
            {{ showAdvFilter ? '🔼 收起筛选' : '🔽 更多筛选' }}
          </el-button>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" :icon="Plus" @click="openCreate">新建需求</el-button>
        </div>
      </div>

      <!-- 高级筛选 -->
      <div v-if="showAdvFilter" class="toolbar-adv">
        <span class="filter-label">提出时间</span>
        <el-date-picker
          v-model="filters.dateFrom"
          type="date"
          placeholder="开始日期"
          value-format="YYYY-MM-DD"
          style="width: 140px"
          @change="handleSearch"
        />
        <span style="color: #999">—</span>
        <el-date-picker
          v-model="filters.dateTo"
          type="date"
          placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 140px"
          @change="handleSearch"
        />
        <el-select v-model="filters.dept" placeholder="提出部门" clearable style="width: 130px" @change="handleSearch">
          <el-option v-for="d in filterOptions.departments" :key="d.name" :label="d.name" :value="d.name" />
        </el-select>
        <el-select v-model="filters.submitterId" placeholder="提出人" clearable style="width: 120px" @change="handleSearch">
          <el-option v-for="u in filterOptions.submitters" :key="u.id" :label="u.name" :value="u.id" />
        </el-select>
        <el-select v-model="filters.sysOwner" placeholder="系统负责人" clearable style="width: 140px" @change="handleSearch">
          <el-option v-for="o in filterOptions.sysOwners" :key="o.name" :label="o.name" :value="o.name" />
        </el-select>
        <el-button text @click="clearAdvFilter">清除筛选</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-wrap">
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        @row-click="(row) => viewDetail(row.id)"
      >
        <el-table-column prop="requirementCode" label="需求编号" width="130" />
        <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click.stop="viewDetail(row.id)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="submitterName" label="提出人" width="90" />
        <el-table-column prop="dept" label="提出部门" width="110" />
        <el-table-column prop="priority" label="优先级" width="80">
          <template #default="{ row }">
            <span :class="'priority-' + row.priority">{{ row.priority }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentNode" label="当前节点" width="110" />
        <el-table-column label="涉及系统" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.systemItems?.map(s => s.name).join('、') || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="expectedDate" label="期望完成" width="110" />
        <el-table-column prop="createdAt" label="创建时间" width="110">
          <template #default="{ row }">
            {{ row.createdAt?.substring(0, 10) }}
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="totalItems"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 新建弹窗 -->
    <RequirementForm v-model:visible="formVisible" @success="onFormSuccess" />

    <!-- 详情弹窗 -->
    <RequirementDetail
      v-model:visible="detailVisible"
      :id="detailId"
      @close="onDetailClose"
    />
  </div>
</template>

<style scoped>
.requirement-list { padding: 0; }

/* 统计卡片 */
.stats-row { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-card {
  flex: 1; background: #fff; border-radius: 6px; padding: 14px 16px;
  box-shadow: 0 1px 2px rgba(0,0,0,.03), 0 1px 6px -1px rgba(0,0,0,.02);
  border-left: 4px solid transparent; cursor: pointer; transition: box-shadow .2s;
}
.stat-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,.06); }
.stat-card.active { background: #f0f5ff; }
.stat-label { font-size: 12px; color: #606266; margin-bottom: 4px; }
.stat-value { font-size: 22px; font-weight: 600; }

/* 工具栏 */
.toolbar {
  background: #fff; padding: 12px 16px; border-radius: 2px;
  margin-bottom: 12px; box-shadow: 0 1px 2px rgba(0,0,0,.04);
}
.toolbar-top { display: flex; align-items: center; justify-content: space-between; }
.toolbar-left { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.toolbar-right { display: flex; align-items: center; gap: 10px; }
.toolbar-adv {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  padding-top: 10px; border-top: 1px solid #ebeef5; margin-top: 10px;
}
.filter-label { font-size: 12px; color: #a8abb2; white-space: nowrap; }

/* 表格 */
.table-wrap { background: #fff; border: 1px solid #e8e8e8; border-radius: 2px; }
.pagination-wrap { display: flex; justify-content: flex-end; padding: 12px 16px; }

/* 优先级颜色 */
.priority-紧急 { color: #e54545; font-weight: 600; }
.priority-高 { color: #ff9c00; font-weight: 500; }
.priority-普通 { color: #606266; }
.priority-低 { color: #a8abb2; }
</style>
