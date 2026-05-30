<script setup>
import { ref, onMounted, computed } from 'vue'
import { getStats } from '@/api/dashboard'
import { useUserStore } from '@/stores/user'
import { DataBoard, Clock, WarningFilled, CircleCheckFilled } from '@element-plus/icons-vue'

const userStore = useUserStore()
const stats = ref(null)
const loading = ref(true)

const isItDept = computed(() => userStore.user?.department === '信息科技部')

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
        <div class="stat-card">
          <div class="stat-accent blue"></div>
          <div class="stat-icon-box blue">
            <el-icon :size="22"><DataBoard /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ totalIssues }}</div>
            <div class="stat-label">问题总数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-accent orange"></div>
          <div class="stat-icon-box orange">
            <el-icon :size="22"><Clock /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ activeIssues }}</div>
            <div class="stat-label">处理中</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-accent red"></div>
          <div class="stat-icon-box red">
            <el-icon :size="22"><WarningFilled /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.overdue.temporaryOverdue + stats.overdue.permanentOverdue }}</div>
            <div class="stat-label">已超期</div>
            <div class="stat-sub" v-if="stats.overdue.temporaryOverdue > 0 || stats.overdue.permanentOverdue > 0">
              临时 {{ stats.overdue.temporaryOverdue }} · 永久 {{ stats.overdue.permanentOverdue }}
            </div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-accent green"></div>
          <div class="stat-icon-box green">
            <el-icon :size="22"><CircleCheckFilled /></el-icon>
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
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  border-radius: 6px;
  background: #fff;
  box-shadow: var(--card-shadow);
  transition: box-shadow 0.2s;
  overflow: hidden;
}
.stat-card:hover {
  box-shadow: var(--card-shadow-hover);
}
.stat-accent {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
}
.stat-accent.blue  { background: #1890ff; }
.stat-accent.orange { background: #fa8c16; }
.stat-accent.red   { background: #f5222d; }
.stat-accent.green { background: #52c41a; }
.stat-icon-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  flex-shrink: 0;
}
.stat-icon-box.blue  { background: #e6f7ff; color: #1890ff; }
.stat-icon-box.orange { background: #fff7e6; color: #fa8c16; }
.stat-icon-box.red   { background: #fff1f0; color: #f5222d; }
.stat-icon-box.green { background: #f6ffed; color: #52c41a; }
.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: rgba(0,0,0,0.85);
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: rgba(0,0,0,0.45);
  margin-top: 2px;
}
.stat-sub {
  font-size: 11px;
  color: rgba(0,0,0,0.25);
  margin-top: 2px;
}

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
.ranking-score.reward { color: #52c41a; }
.ranking-score.punishment { color: #f5222d; }
.title-sub {
  font-weight: 400;
  font-size: 13px;
  color: #86909c;
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
