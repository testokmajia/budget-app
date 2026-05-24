<script setup>
import { ref, onMounted, computed } from 'vue'
import { getStats } from '@/api/dashboard'

const stats = ref(null)
const loading = ref(true)

const statusColors = {
  '待分派': '#909399',
  '待员工处理': '#e6a23c',
  '待组长审核': '#409eff',
  '待管理员审核': '#409eff',
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

const teamPersonnelTotal = computed(() => {
  if (!stats.value) return 0
  return stats.value.personnelDistribution.reduce((sum, p) => sum + p.memberCount, 0)
})

const deptTotal = computed(() => teamPersonnelTotal.value + 3)

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
    <template v-if="stats">
      <!-- Stat cards -->
      <div class="stat-cards">
        <div class="stat-card total">
          <div class="stat-icon">
            <span class="icon-circle">📋</span>
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ totalIssues }}</div>
            <div class="stat-label">问题总数</div>
          </div>
        </div>
        <div class="stat-card pending">
          <div class="stat-icon">
            <span class="icon-circle">⏳</span>
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ activeIssues }}</div>
            <div class="stat-label">处理中</div>
          </div>
        </div>
        <div class="stat-card overdue">
          <div class="stat-icon">
            <span class="icon-circle">⚠️</span>
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.overdue.temporaryOverdue + stats.overdue.permanentOverdue }}</div>
            <div class="stat-label">已超期</div>
            <div class="stat-sub" v-if="stats.overdue.temporaryOverdue > 0 || stats.overdue.permanentOverdue > 0">
              临时 {{ stats.overdue.temporaryOverdue }} · 永久 {{ stats.overdue.permanentOverdue }}
            </div>
          </div>
        </div>
        <div class="stat-card completed">
          <div class="stat-icon">
            <span class="icon-circle">✅</span>
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ completedIssues }}</div>
            <div class="stat-label">已完成</div>
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
                <div class="bar-label team-label">{{ t.team }}</div>
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
                <span class="title-tag">团队 {{ teamPersonnelTotal }}人</span>
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
      <el-row :gutter="20" class="charts-row" v-if="stats.rewardRanking && stats.rewardRanking.length > 0">
        <el-col :span="24">
          <div class="chart-card">
            <h3 class="chart-title">人员奖惩排名 <span style="font-weight:400;font-size:13px;color:#86909c">(分值合计)</span></h3>
            <div class="ranking-grid">
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
                  <div class="ranking-dept">{{ r.department }}</div>
                </div>
                <div class="ranking-score" :class="{ positive: r.totalScore > 0, negative: r.totalScore < 0 }">
                  {{ r.totalScore > 0 ? '+' : '' }}{{ r.totalScore }}
                </div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<style scoped>
/* Stat cards */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  transition: box-shadow 0.2s;
}
.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.icon-circle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  font-size: 22px;
}
.stat-card.total .icon-circle { background: #e8f3ff; }
.stat-card.pending .icon-circle { background: #fff7e6; }
.stat-card.overdue .icon-circle { background: #ffece8; }
.stat-card.completed .icon-circle { background: #e8f8ee; }
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1d2129;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #86909c;
  margin-top: 2px;
}
.stat-sub {
  font-size: 11px;
  color: #c9cdd4;
  margin-top: 2px;
}

/* Chart cards */
.charts-row {
  margin-bottom: 20px;
}
.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  height: 100%;
}
.chart-title {
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
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
  gap: 10px;
}
.bar-label {
  width: 65px;
  font-size: 13px;
  color: #4e5969;
  text-align: right;
  flex-shrink: 0;
}
.team-label {
  width: 80px;
}
.bar-track {
  flex: 1;
  height: 20px;
  background: #f2f3f5;
  border-radius: 4px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
  min-width: 4px;
}
.team-fill {
  background: #409eff;
}
.bar-value {
  width: 36px;
  font-size: 13px;
  font-weight: 600;
  color: #1d2129;
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
  border-radius: 8px;
  text-align: center;
}
.overdue-item.temp {
  background: #fff7e6;
}
.overdue-item.perm {
  background: #ffece8;
}
.overdue-num {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}
.overdue-item.temp .overdue-num { color: #e6a23c; }
.overdue-item.perm .overdue-num { color: #f56c6c; }
.overdue-desc {
  font-size: 13px;
  font-weight: 500;
  color: #1d2129;
  margin-top: 4px;
}
.overdue-hint {
  font-size: 11px;
  color: #86909c;
  margin-top: 2px;
}
.rate-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
  color: #4e5969;
}
.rate-pct {
  font-weight: 600;
  color: #1d2129;
}

/* Personnel list */
.personnel-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.personnel-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #f7f8fa;
  border-radius: 8px;
  transition: background 0.15s;
}
.personnel-row:hover {
  background: #e8f3ff;
}
.personnel-team {
  font-size: 14px;
  color: #1d2129;
  font-weight: 500;
}
.personnel-count {
  display: flex;
  align-items: baseline;
  gap: 3px;
}
.personnel-num {
  font-size: 20px;
  font-weight: 700;
  color: #409eff;
}
.personnel-unit {
  font-size: 12px;
  color: #86909c;
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
  background: #e8f3ff;
  color: #409eff;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}
.title-tag.dept {
  background: #f2f3f5;
  color: #303133;
}
.empty-hint {
  text-align: center;
  color: #c9cdd4;
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
  background: #f7f8fa;
  border-radius: 8px;
  transition: all 0.15s;
}
.ranking-item:hover {
  background: #e8f3ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.ranking-item.top-1 {
  background: linear-gradient(135deg, #fff9e6 0%, #fff3cc 100%);
  border: 1px solid #f0d060;
}
.ranking-item.top-2 {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  border: 1px solid #c0c4cc;
}
.ranking-item.top-3 {
  background: linear-gradient(135deg, #fdf2ec 0%, #f9e0cc 100%);
  border: 1px solid #e0b080;
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
  color: #909399;
}
.ranking-info {
  flex: 1;
  min-width: 0;
}
.ranking-name {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ranking-dept {
  font-size: 11px;
  color: #86909c;
  margin-top: 1px;
}
.ranking-score {
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
}
.ranking-score.positive { color: #67c23a; }
.ranking-score.negative { color: #f56c6c; }

/* Responsive */
@media (max-width: 768px) {
  .ranking-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  .charts-row .el-col {
    flex: 0 0 100%;
    max-width: 100%;
    margin-bottom: 16px;
  }
}
@media (max-width: 480px) {
  .stat-cards {
    grid-template-columns: 1fr;
  }
  .overdue-cards {
    flex-direction: column;
  }
}
</style>
