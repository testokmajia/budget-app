<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getStats } from '@/api/dashboard'
import { useUserStore } from '@/stores/user'
import { nameEquals } from '@/utils/compare'
import { DataBoard, Clock, WarningFilled, CircleCheckFilled } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const stats = ref(null)
const loading = ref(true)
const activeTab = ref('overview')

const isItDept = computed(() => nameEquals(userStore.user?.department, '信息科技部'))

const statusColors = {
  '待分派': '#909399',
  '待员工处理': '#e6a23c',
  '待组长审核': '#409eff',
  '待管理员审核': '#409eff',
  '解决中': '#1890ff',
  '待确认': '#e6a23c',
  '已完成': '#67c23a',
  '已驳回': '#f56c6c',
  '已关闭': '#909399',
}

const totalIssues = computed(() => {
  if (!stats.value) return 0
  return stats.value.statusCounts.reduce((sum, s) => sum + s.count, 0)
})

const activeIssues = computed(() => {
  if (!stats.value) return 0
  return stats.value.statusCounts
    .filter(s => !['已完成', '已关闭'].includes(s.status))
    .reduce((sum, s) => sum + s.count, 0)
})

const completedIssues = computed(() => {
  if (!stats.value) return 0
  const c = stats.value.statusCounts.find(s => s.status === '已完成')
  return c ? c.count : 0
})

const maxStatusCount = computed(() => {
  if (!stats.value) return 1
  return Math.max(1, ...stats.value.statusCounts.map(s => s.count))
})

const maxTeamCount = computed(() => {
  if (!stats.value || !stats.value.teamDistribution.length) return 1
  return Math.max(1, ...stats.value.teamDistribution.map(t => t.count))
})

const deptTotal = computed(() => {
  if (!stats.value) return 0
  return stats.value.personnelDistribution.reduce((sum, p) => sum + p.memberCount, 0)
})

const reqSegments = computed(() => {
  const rs = stats.value?.requirementStats
  if (!rs || rs.total === 0) return []
  return [
    { label: '审批中', count: rs.inApproval, color: '#fa8c16' },
    { label: '已确认', count: rs.confirmed, color: '#2f54eb' },
    { label: '实施中', count: rs.inProgress, color: '#1890ff' },
    { label: '测试通过', count: rs.testPassed, color: '#52c41a' },
    { label: '已投产', count: rs.production, color: '#13c2c2' },
    { label: '已关闭', count: rs.closed, color: '#8c8c8c' },
    { label: '已驳回', count: rs.rejected, color: '#f5222d' },
  ].filter(s => s.count > 0)
})

// 待办事项分类
const taskCategories = computed(() => {
  const tasks = stats.value?.pendingTasks || []
  const cats = [
    { key: 'RequirementList', icon: '📝', label: '需求相关', color: '#2f54eb', bg: '#f0f5ff' },
    { key: 'TestReportList', icon: '🧪', label: '测试报告', color: '#52c41a', bg: '#f6ffed' },
    { key: 'Issue', icon: '⚠️', label: '问题相关', color: '#fa8c16', bg: '#fff7e6' },
    { key: 'Weekly', icon: '📄', label: '周报相关', color: '#722ed1', bg: '#f9f0ff' },
  ]
  return cats.map(cat => ({
    ...cat,
    tasks: tasks.filter(t => t.routeName === cat.key),
    count: tasks.filter(t => t.routeName === cat.key).reduce((s, t) => s + t.count, 0),
  })).filter(cat => cat.tasks.length > 0)
})

const totalPending = computed(() => {
  return (stats.value?.pendingTasks || []).reduce((s, t) => s + t.count, 0)
})

function parseQuery(raw) {
  const query = {}
  if (raw) {
    raw.split('&').forEach(p => {
      const [k, v] = p.split('=')
      if (k && v) query[k] = v
    })
  }
  return query
}

function goTask(task) {
  router.push({ name: task.routeName, query: parseQuery(task.routeQuery) })
}

onMounted(async () => {
  try {
    const res = await getStats()
    stats.value = res.data
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-container" v-loading="loading">
    <!-- Tab 切换 -->
    <div class="dash-tabs">
      <div
        class="dash-tab"
        :class="{ active: activeTab === 'overview' }"
        @click="activeTab = 'overview'"
      >📊 数据概览</div>
      <div
        class="dash-tab"
        :class="{ active: activeTab === 'pending' }"
        @click="activeTab = 'pending'"
      >
        📋 待办事项
        <span v-if="totalPending" class="tab-badge">{{ totalPending }}</span>
      </div>
    </div>

    <!-- ==================== 待办事项 Tab ==================== -->
    <div v-if="activeTab === 'pending'">
      <!-- 空状态 -->
      <div v-if="totalPending === 0" class="empty-pending">
        <div class="empty-icon">🎉</div>
        <div class="empty-text">太棒了，没有待办事项！</div>
      </div>

      <!-- 按分类展示 -->
      <div v-for="cat in taskCategories" :key="cat.key" class="pending-section">
        <div class="pending-section-title">
          <span>{{ cat.icon }}</span>
          <span>{{ cat.label }}</span>
          <span class="pending-section-count">{{ cat.count }} 项</span>
        </div>
        <div class="pending-list">
          <div
            v-for="t in cat.tasks"
            :key="t.title"
            class="pending-card"
            @click="goTask(t)"
          >
            <div class="pending-icon" :style="{ background: cat.bg, color: cat.color }">
              <span v-if="cat.key === 'RequirementList'">📋</span>
              <span v-else-if="cat.key === 'TestReportList'">✅</span>
              <span v-else-if="cat.key === 'Issue'">🔧</span>
              <span v-else>📄</span>
            </div>
            <div class="pending-info">
              <div class="pending-title">{{ t.title }}</div>
              <div class="pending-desc">{{ t.description }}</div>
            </div>
            <div class="pending-count">{{ t.count }}</div>
            <div class="pending-arrow">→</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 数据概览 Tab ==================== -->
    <template v-if="activeTab === 'overview' && stats">
      <!-- 需求概览 -->
      <div class="req-overview" v-if="reqSegments.length">
        <div class="req-header">
          <span class="req-dot"></span>
          <span class="req-title">需求概览</span>
          <span class="req-total">共 {{ stats.requirementStats.total }} 项</span>
        </div>
        <div class="req-bar-wrap">
          <div class="req-bar">
            <div
              v-for="seg in reqSegments" :key="seg.label"
              class="req-seg"
              :style="{ flex: seg.count, background: seg.color }"
            >
              <span class="req-seg-label" v-if="seg.count >= Math.max(...reqSegments.map(s=>s.count)) * 0.3">{{ seg.label }}</span>
            </div>
          </div>
          <div class="req-legend">
            <span v-for="seg in reqSegments" :key="seg.label" class="req-lg-item">
              <i class="req-lg-dot" :style="{ background: seg.color }"></i>
              {{ seg.label }} <b>{{ seg.count }}</b>
            </span>
          </div>
        </div>
      </div>

      <el-row :gutter="20" class="charts-row">
        <!-- Status distribution -->
        <el-col :span="14">
          <div class="chart-card">
            <h3 class="chart-title">问题状态分布</h3>
            <div class="bar-chart">
              <div class="bar-row" v-for="s in stats.statusCounts" :key="s.status">
                <div class="bar-label">{{ s.status }}</div>
                <div class="bar-track">
                  <div
                    class="bar-fill"
                    :style="{
                      width: maxStatusCount ? (s.count / maxStatusCount * 100) + '%' : '0%',
                      backgroundColor: statusColors[s.status] || '#909399'
                    }"
                  />
                </div>
                <div class="bar-value">{{ s.count }}</div>
              </div>
            </div>
          </div>
        </el-col>

        <!-- Overdue details -->
        <el-col :span="10">
          <div class="chart-card">
            <h3 class="chart-title">超期情况</h3>
            <div class="overdue-cards">
              <div class="overdue-item temp">
                <div class="overdue-num">{{ stats.overdue.temporaryOverdue }}</div>
                <div class="overdue-desc">临时整改超期</div>
                <div class="overdue-hint">临时方案未在期限内完成</div>
              </div>
              <div class="overdue-item perm">
                <div class="overdue-num">{{ stats.overdue.permanentOverdue }}</div>
                <div class="overdue-desc">永久解决超期</div>
                <div class="overdue-hint">永久方案未在期限内完成</div>
              </div>
            </div>

            <!-- Completion rate -->
            <div class="completion-rate" v-if="totalIssues > 0">
              <div class="rate-header">
                <span>完成率</span>
                <span class="rate-pct">{{ Math.round(completedIssues / totalIssues * 100) }}%</span>
              </div>
              <el-progress
                :percentage="Math.round(completedIssues / totalIssues * 100)"
                :stroke-width="10"
                :color="completedIssues / totalIssues > 0.6 ? '#67c23a' : completedIssues / totalIssues > 0.3 ? '#e6a23c' : '#f56c6c'"
              />
            </div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="charts-row">
        <!-- Team distribution -->
        <el-col :span="14">
          <div class="chart-card">
            <h3 class="chart-title">问题团队分布</h3>
            <div class="bar-chart" v-if="stats.teamDistribution.length > 0">
              <div class="bar-row" v-for="t in stats.teamDistribution" :key="t.team">
                <div class="bar-label team-label" :title="t.team">{{ t.team }}</div>
                <div class="bar-track">
                  <div
                    class="bar-fill team-fill"
                    :style="{ width: (t.count / maxTeamCount * 100) + '%' }"
                  />
                </div>
                <div class="bar-value">{{ t.count }}</div>
              </div>
            </div>
            <div v-else class="empty-hint">暂无团队分配数据</div>
          </div>
        </el-col>

        <!-- Personnel distribution -->
        <el-col :span="10">
          <div class="chart-card">
            <h3 class="chart-title">
              信息科技部团队人员分布
              <span class="title-totals">
                <span class="title-tag dept">部门总计 {{ deptTotal }}人</span>
              </span>
            </h3>
            <div class="personnel-list" v-if="stats.personnelDistribution.length > 0">
              <div class="personnel-row" v-for="p in stats.personnelDistribution" :key="p.teamName">
                <div class="personnel-team">{{ p.teamName }}</div>
                <div class="personnel-count">
                  <span class="personnel-num">{{ p.memberCount }}</span>
                  <span class="personnel-unit">人</span>
                </div>
              </div>
            </div>
            <div v-else class="empty-hint">暂无团队人员数据</div>
          </div>
        </el-col>
      </el-row>

      <!-- Reward ranking -->
      <el-row :gutter="20" class="charts-row" v-if="isItDept">
        <el-col :span="24">
          <div class="chart-card">
            <h3 class="chart-title">人员奖励排名 <span class="title-sub">(按次数)</span></h3>
            <div v-if="stats.rewardRanking && stats.rewardRanking.length > 0" class="ranking-grid">
              <div
                v-for="(r, idx) in stats.rewardRanking"
                :key="r.personName"
                class="ranking-item"
                :class="{ 'top-1': idx === 0, 'top-2': idx === 1, 'top-3': idx === 2 }"
              >
                <div class="ranking-pos">
                  <span v-if="idx === 0" class="medal">🥇</span>
                  <span v-else-if="idx === 1" class="medal">🥈</span>
                  <span v-else-if="idx === 2" class="medal">🥉</span>
                  <span v-else class="pos-num">{{ idx + 1 }}</span>
                </div>
                <div class="ranking-info">
                  <div class="ranking-name">{{ r.personName }}</div>
                </div>
                <div class="ranking-score reward">{{ r.count }}次</div>
              </div>
            </div>
            <div v-else class="empty-hint">暂无奖励记录</div>
          </div>
        </el-col>
      </el-row>

      <!-- Punishment ranking -->
      <el-row :gutter="20" class="charts-row" v-if="isItDept">
        <el-col :span="24">
          <div class="chart-card">
            <h3 class="chart-title">人员惩罚排名 <span class="title-sub">(按次数)</span></h3>
            <div v-if="stats.punishmentRanking && stats.punishmentRanking.length > 0" class="ranking-grid">
              <div
                v-for="(r, idx) in stats.punishmentRanking"
                :key="r.personName"
                class="ranking-item"
                :class="{ 'top-1': idx === 0, 'top-2': idx === 1, 'top-3': idx === 2 }"
              >
                <div class="ranking-pos">
                  <span v-if="idx === 0" class="medal">🥇</span>
                  <span v-else-if="idx === 1" class="medal">🥈</span>
                  <span v-else-if="idx === 2" class="medal">🥉</span>
                  <span v-else class="pos-num">{{ idx + 1 }}</span>
                </div>
                <div class="ranking-info">
                  <div class="ranking-name">{{ r.personName }}</div>
                </div>
                <div class="ranking-score punishment">{{ r.count }}次</div>
              </div>
            </div>
            <div v-else class="empty-hint">暂无惩罚记录</div>
          </div>
        </el-col>
      </el-row>
    </template>
    <!-- end overview tab -->
  </div>
</template>

<style scoped>
/* ======== Tab 切换 ======== */
.dash-tabs {
  display: flex; gap: 0; margin-bottom: 20px;
  background: #fff; border-radius: 8px; padding: 4px;
  box-shadow: var(--card-shadow);
}
.dash-tab {
  flex: 1; text-align: center; padding: 10px 0; font-size: 14px; font-weight: 500;
  border-radius: 6px; cursor: pointer; color: #606266; transition: all .2s;
  display: flex; align-items: center; justify-content: center; gap: 6px;
}
.dash-tab.active { background: #006eff; color: #fff; }
.dash-tab:hover:not(.active) { color: #006eff; background: #ecf5ff; }
.tab-badge {
  display: inline-block; min-width: 20px; height: 20px; line-height: 20px;
  border-radius: 10px; font-size: 12px; background: #f56c6c; color: #fff;
  text-align: center; padding: 0 6px;
}
.dash-tab.active .tab-badge { background: rgba(255,255,255,.3); }

/* ======== 待办事项 ======== */
.empty-pending { text-align: center; padding: 80px 0; }
.empty-icon { font-size: 56px; margin-bottom: 16px; }
.empty-text { font-size: 15px; color: #909399; }

.pending-section { margin-bottom: 24px; }
.pending-section-title {
  font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 12px;
  display: flex; align-items: center; gap: 8px;
}
.pending-section-count { font-size: 12px; color: #909399; font-weight: 400; margin-left: 4px; }

.pending-list { display: flex; flex-direction: column; gap: 10px; }
.pending-card {
  background: #fff; border-radius: 8px; padding: 16px 20px;
  box-shadow: var(--card-shadow);
  display: flex; align-items: center; gap: 14px;
  transition: all .2s; cursor: pointer; border: 1px solid transparent;
}
.pending-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,0,0,.08);
  border-color: #006eff;
}
.pending-icon {
  width: 40px; height: 40px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; flex-shrink: 0;
}
.pending-info { flex: 1; min-width: 0; }
.pending-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 3px; }
.pending-desc { font-size: 12px; color: #909399; }
.pending-count {
  flex-shrink: 0; min-width: 24px; height: 24px; border-radius: 12px;
  font-size: 12px; font-weight: 600; display: flex; align-items: center; justify-content: center;
  background: #f56c6c; color: #fff; padding: 0 8px;
}
.pending-arrow {
  flex-shrink: 0; width: 28px; height: 28px; border-radius: 50%;
  background: #ecf5ff; color: #006eff;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; transition: all .2s;
}
.pending-card:hover .pending-arrow { background: #006eff; color: #fff; }

/* 需求概览 */
.req-overview {
  background: #faf5ff; border-radius: 12px; padding: 16px 24px; margin-bottom: 20px;
}
.req-header { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; }
.req-dot { width: 8px; height: 8px; border-radius: 50%; background: #722ed1; flex-shrink: 0; }
.req-title { font-size: 14px; font-weight: 600; color: #333; }
.req-total { font-size: 12px; color: #999; margin-left: auto; }
.req-bar-wrap { }
.req-bar { display: flex; height: 32px; border-radius: 8px; overflow: hidden; margin-bottom: 10px; }
.req-seg {
  display: flex; align-items: center; justify-content: center;
  min-width: 4px; transition: opacity .15s; cursor: default;
}
.req-seg:hover { opacity: .85; }
.req-seg-label { font-size: 11px; color: #fff; font-weight: 600; white-space: nowrap; text-shadow: 0 1px 2px rgba(0,0,0,.3); }
.req-legend { display: flex; flex-wrap: wrap; gap: 4px 16px; }
.req-lg-item { font-size: 12px; color: #555; display: inline-flex; align-items: center; gap: 5px; }
.req-lg-item b { color: #333; }
.req-lg-dot { display: inline-block; width: 10px; height: 10px; border-radius: 3px; flex-shrink: 0; }

/* Chart cards */
.charts-row {
  margin-bottom: 20px;
}
.chart-card {
  background: #fff;
  border-radius: 6px;
  padding: 20px 24px;
  box-shadow: var(--card-shadow);
  height: 100%;
}
.chart-title {
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 500;
  color: rgba(0,0,0,0.85);
}

/* Bar chart */
.bar-chart {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.bar-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.bar-label {
  width: 100px;
  font-size: 13px;
  color: rgba(0,0,0,0.65);
  text-align: right;
  flex-shrink: 0;
  white-space: nowrap;
}
.team-label {
  width: 130px;
}
.bar-track {
  flex: 1;
  height: 20px;
  background: #f5f5f5;
  border-radius: 10px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 10px;
  transition: width 0.6s ease;
  min-width: 4px;
}
.team-fill {
  background: #1890ff;
}
.bar-value {
  width: 36px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(0,0,0,0.85);
  text-align: left;
  flex-shrink: 0;
}

/* Overdue */
.overdue-cards {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.overdue-item {
  flex: 1;
  padding: 16px;
  border-radius: 6px;
  text-align: center;
}
.overdue-item.temp {
  background: #fff7e6;
}
.overdue-item.perm {
  background: #fff1f0;
}
.overdue-num {
  font-size: 32px;
  font-weight: 600;
  line-height: 1.2;
}
.overdue-item.temp .overdue-num { color: #fa8c16; }
.overdue-item.perm .overdue-num { color: #f5222d; }
.overdue-desc {
  font-size: 13px;
  font-weight: 500;
  color: rgba(0,0,0,0.85);
  margin-top: 4px;
}
.overdue-hint {
  font-size: 11px;
  color: rgba(0,0,0,0.45);
  margin-top: 2px;
}
.rate-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
  color: rgba(0,0,0,0.65);
}
.rate-pct {
  font-weight: 600;
  color: rgba(0,0,0,0.85);
}

/* Personnel list */
.personnel-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.personnel-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #fafafa;
  border-radius: 6px;
  transition: background 0.15s;
}
.personnel-row:hover {
  background: #e6f7ff;
}
.personnel-team {
  font-size: 14px;
  color: rgba(0,0,0,0.85);
  font-weight: 500;
}
.personnel-count {
  display: flex;
  align-items: baseline;
  gap: 3px;
}
.personnel-num {
  font-size: 20px;
  font-weight: 600;
  color: #1890ff;
}
.personnel-unit {
  font-size: 12px;
  color: rgba(0,0,0,0.45);
}
.title-totals {
  font-weight: 400;
  font-size: 13px;
  margin-left: 8px;
  display: inline-flex;
  gap: 6px;
  align-items: center;
}
.title-tag {
  background: #e6f7ff;
  color: #1890ff;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}
.title-tag.dept {
  background: #f5f5f5;
  color: rgba(0,0,0,0.65);
}
.empty-hint {
  text-align: center;
  color: rgba(0,0,0,0.25);
  padding: 32px 0;
  font-size: 14px;
}

/* Ranking */
.ranking-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
}
.ranking-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 6px;
  transition: all 0.15s;
}
.ranking-item:hover {
  background: #e6f7ff;
  transform: translateY(-1px);
  box-shadow: var(--card-shadow);
}
.ranking-item.top-1 {
  background: linear-gradient(135deg, #fffbe6 0%, #fff1b8 100%);
  border: 1px solid #ffe58f;
}
.ranking-item.top-2 {
  background: linear-gradient(135deg, #fafafa 0%, #f0f0f0 100%);
  border: 1px solid #d9d9d9;
}
.ranking-item.top-3 {
  background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%);
  border: 1px solid #ffd591;
}
.ranking-pos {
  width: 28px;
  text-align: center;
  flex-shrink: 0;
}
.medal {
  font-size: 20px;
}
.pos-num {
  font-size: 13px;
  font-weight: 600;
  color: rgba(0,0,0,0.45);
}
.ranking-info {
  flex: 1;
  min-width: 0;
}
.ranking-name {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0,0,0,0.85);
}
.ranking-score {
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}
.ranking-score.reward { color: #52c41a; }
.ranking-score.punishment { color: #f5222d; }
.title-sub {
  font-weight: 400;
  font-size: 13px;
  color: #86909c;
}

/* Responsive */
@media (max-width: 768px) {
  .req-overview { padding: 12px 16px; }
  .ranking-grid { grid-template-columns: repeat(2, 1fr); }
  .charts-row .el-col {
    flex: 0 0 100%;
    max-width: 100%;
    margin-bottom: 16px;
  }
}
.ranking-score.reward { color: #52c41a; }
.ranking-score.punishment { color: #f5222d; }
.title-sub {
  font-weight: 400;
  font-size: 13px;
  color: #86909c;
}
@media (max-width: 480px) {
  .req-legend { gap: 2px 8px; }
  .req-lg-item { font-size: 10px; }
  .overdue-cards {
    flex-direction: column;
  }
}
</style>
