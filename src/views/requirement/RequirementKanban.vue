<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { getList, getStats } from '@/api/requirement'
import { nameEquals } from '@/utils/compare'
import RequirementForm from './components/RequirementForm.vue'
import RequirementDetail from './components/RequirementDetail.vue'

const loading = ref(false)
const detailLoading = ref(false)
const stats = ref({})
const detailCards = ref([])
const allCards = ref([])

const formVisible = ref(false)
const detailVisible = ref(false)
const detailId = ref(null)

// 当前选中的维度和筛选
const viewMode = ref('status') // status | system | team
const activeFilter = ref('')   // 当前展开的状态/系统/团队名称
const expanded = ref(false)

const statusOrder = ['草稿', '审批中', '需求已确认', '实施中', '测试通过', '已投产', '已关闭', '已驳回']
const statusIcons = {
  '草稿': '📝', '审批中': '⏳', '需求已确认': '✅', '实施中': '🚧',
  '测试通过': '🧪', '已投产': '🚀', '已关闭': '🔒', '已驳回': '↩'
}
const statusColors = {
  '草稿': '#909399', '审批中': '#ff9c00', '需求已确认': '#0abf5b', '实施中': '#006eff',
  '测试通过': '#0abf5b', '已投产': '#0abf5b', '已关闭': '#909399', '已驳回': '#e54545'
}

// 系统统计（从已加载数据计算）
const systemStats = computed(() => {
  const map = new Map()
  allCards.value.forEach(card => {
    const sysNames = new Set()
    if (card.systemItems?.length) {
      card.systemItems.forEach(s => { if (s.name) sysNames.add(s.name) })
    }
    if (sysNames.size === 0) sysNames.add('未指定系统')
    sysNames.forEach(name => {
      map.set(name, (map.get(name) || 0) + 1)
    })
  })
  return Array.from(map.entries()).sort((a, b) => b[1] - a[1])
})

// 团队统计
const teamStats = computed(() => {
  const map = new Map()
  allCards.value.forEach(card => {
    const teams = new Set()
    if (card.systemItems?.length) {
      card.systemItems.forEach(s => { if (s.team) teams.add(s.team) })
    }
    if (teams.size === 0) teams.add('未指定团队')
    teams.forEach(t => {
      map.set(t, (map.get(t) || 0) + 1)
    })
  })
  return Array.from(map.entries()).sort((a, b) => b[1] - a[1])
})

// 按状态统计的维度卡片
const statusCards = computed(() => {
  return statusOrder.map(st => ({
    key: st,
    label: statusIcons[st] + ' ' + st,
    count: stats.value[st] || 0,
    color: statusColors[st],
  }))
})

async function loadStats() {
  try {
    const res = await getStats()
    stats.value = res.data
  } catch (e) { /* ignore */ }
}

async function loadSummaryData() {
  // 轻量加载，仅用于系统/团队统计
  try {
    const res = await getList({ size: 500 })
    allCards.value = res.data?.content || []
  } catch (e) { /* ignore */ }
}

// 点击统计卡片 → 加载详情
async function toggleDetail(key, type) {
  if (expanded.value && activeFilter.value === key && viewMode.value === type) {
    expanded.value = false
    activeFilter.value = ''
    detailCards.value = []
    return
  }
  viewMode.value = type
  activeFilter.value = key
  expanded.value = true
  detailLoading.value = true
  try {
    let params = { size: 100 }
    if (type === 'status') {
      params.status = key
    } else if (type === 'system') {
      params.sysOwner = key !== '未指定系统' ? key : undefined
    }
    const res = await getList(params)
    let cards = res.data?.content || []
    // 客户端按系统/团队筛选
    if (type === 'system' && key !== '未指定系统') {
      cards = cards.filter(c => c.systemItems?.some(s => nameEquals(s.name, key)))
    }
    if (type === 'team') {
      cards = cards.filter(c => {
        if (key === '未指定团队') return !c.systemItems?.some(s => s.team)
        return c.systemItems?.some(s => nameEquals(s.team, key))
      })
    }
    detailCards.value = cards
  } catch (e) {
    ElMessage.error('加载需求详情失败')
  } finally {
    detailLoading.value = false
  }
}

function viewDetail(id) {
  detailId.value = id
  detailVisible.value = true
}

function onFormSuccess() {
  formVisible.value = false
  loadStats()
  loadSummaryData()
  if (expanded.value) toggleDetail(activeFilter.value, viewMode.value)
}

function onDetailClose() {
  detailVisible.value = false
  loadStats()
  loadSummaryData()
  if (expanded.value) toggleDetail(activeFilter.value, viewMode.value)
}

function switchMode(mode) {
  viewMode.value = mode
  expanded.value = false
  activeFilter.value = ''
  detailCards.value = []
}

function getTagType(status) {
  const map = { '已驳回': 'danger', '需求已确认': 'success', '实施中': 'warning', '测试通过': 'success', '已投产': '', '已关闭': 'info' }
  return map[status] || 'primary'
}

onMounted(() => {
  loadStats()
  loadSummaryData()
})
</script>

<template>
  <div class="kanban-view">
    <!-- 顶部工具栏 -->
    <div class="kanban-toolbar">
      <div class="dimension-tabs">
        <el-radio-group v-model="viewMode" size="small" @change="switchMode">
          <el-radio-button value="status">按状态</el-radio-button>
          <el-radio-button value="system">按系统</el-radio-button>
          <el-radio-button value="team">按团队</el-radio-button>
        </el-radio-group>
      </div>
      <el-button type="primary" size="small" :icon="Plus" @click="formVisible = true">新建需求</el-button>
    </div>

    <!-- 统计卡片网格 -->
    <div class="stats-grid" v-if="viewMode === 'status'">
      <div
        v-for="sc in statusCards" :key="sc.key"
        class="stat-card"
        :class="{ active: expanded && activeFilter === sc.key && viewMode === 'status' }"
        :style="{ borderTopColor: sc.color }"
        @click="toggleDetail(sc.key, 'status')"
      >
        <div class="sc-label">{{ sc.label }}</div>
        <div class="sc-count" :style="{ color: sc.color }">{{ sc.count }}</div>
        <div class="sc-hint">点击查看详情</div>
      </div>
    </div>

    <div class="stats-grid" v-else-if="viewMode === 'system'">
      <div
        v-for="[name, count] in systemStats" :key="name"
        class="stat-card"
        :class="{ active: expanded && activeFilter === name && viewMode === 'system' }"
        @click="toggleDetail(name, 'system')"
      >
        <div class="sc-label">🖥 {{ name }}</div>
        <div class="sc-count">{{ count }}</div>
        <div class="sc-hint">点击查看详情</div>
      </div>
      <div v-if="systemStats.length === 0" class="empty-hint">暂无数据</div>
    </div>

    <div class="stats-grid" v-else-if="viewMode === 'team'">
      <div
        v-for="[name, count] in teamStats" :key="name"
        class="stat-card"
        :class="{ active: expanded && activeFilter === name && viewMode === 'team' }"
        @click="toggleDetail(name, 'team')"
      >
        <div class="sc-label">👥 {{ name }}</div>
        <div class="sc-count">{{ count }}</div>
        <div class="sc-hint">点击查看详情</div>
      </div>
      <div v-if="teamStats.length === 0" class="empty-hint">暂无数据</div>
    </div>

    <!-- 展开的详情表格 -->
    <div v-if="expanded" class="detail-panel" v-loading="detailLoading">
      <div class="detail-header">
        <span class="detail-title">
          {{ viewMode === 'status' ? statusIcons[activeFilter] + ' ' + activeFilter : activeFilter }}
          <span class="detail-count">共 {{ detailCards.length }} 条</span>
        </span>
        <el-button text :icon="ArrowUp" @click="expanded = false; activeFilter = ''; detailCards = []">收起</el-button>
      </div>
      <el-table :data="detailCards" stripe border size="small" max-height="500" @row-click="viewDetail" style="cursor: pointer">
        <el-table-column prop="requirementCode" label="编号" width="130" />
        <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
        <el-table-column prop="priority" label="优先级" width="70">
          <template #default="{ row }">
            <span :class="'pri-' + row.priority">{{ row.priority }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitterName" label="提出人" width="80" />
        <el-table-column prop="currentNode" label="当前节点" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getTagType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="涉及系统" width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.systemItems?.map(s => s.name).join('、') || '—' }}</template>
        </el-table-column>
        <el-table-column prop="expectedDate" label="期望完成" width="100" />
      </el-table>
    </div>

    <!-- 弹窗 -->
    <RequirementForm v-model:visible="formVisible" @success="onFormSuccess" />
    <RequirementDetail v-model:visible="detailVisible" :id="detailId" @close="onDetailClose" />
  </div>
</template>

<style scoped>
.kanban-view { padding: 0; }

.kanban-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; padding: 10px 16px; border-radius: 2px;
  margin-bottom: 16px; box-shadow: 0 1px 2px rgba(0,0,0,.04);
}

/* 统计卡片网格 */
.stats-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; margin-bottom: 16px; }
.stat-card {
  background: #fff; border-radius: 6px; padding: 16px; cursor: pointer;
  box-shadow: 0 1px 2px rgba(0,0,0,.03), 0 1px 6px -1px rgba(0,0,0,.02);
  border-top: 4px solid #006eff; transition: all .2s;
}
.stat-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,.08); transform: translateY(-1px); }
.stat-card.active { background: #f0f5ff; }
.sc-label { font-size: 14px; color: #303133; margin-bottom: 8px; }
.sc-count { font-size: 28px; font-weight: 700; margin-bottom: 6px; }
.sc-hint { font-size: 11px; color: #a8abb2; }

.empty-hint { text-align: center; padding: 40px; color: #a8abb2; font-size: 14px; }

/* 详情面板 */
.detail-panel {
  background: #fff; border-radius: 2px; padding: 12px 16px;
  box-shadow: 0 1px 2px rgba(0,0,0,.04);
}
.detail-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #ebeef5;
}
.detail-title { font-size: 14px; font-weight: 600; color: #303133; }
.detail-count { font-weight: 400; font-size: 12px; color: #a8abb2; margin-left: 8px; }

.pri-紧急 { color: #e54545; font-weight: 600; }
.pri-高 { color: #ff9c00; font-weight: 500; }
.pri-普通 { color: #606266; }
.pri-低 { color: #a8abb2; }
</style>
