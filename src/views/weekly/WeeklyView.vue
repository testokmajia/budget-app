<script setup>
import { computed, ref, provide, inject } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const headerTeams = ref([])
provide('headerTeams', headerTeams)

const headerStats = ref(null)
provide('headerStats', headerStats)

function barColor(rate) {
  if (rate >= 0.66) return 'high'
  if (rate >= 0.33) return 'medium'
  return 'low'
}

const pageTitle = computed(() => {
  const map = {
    '/weekly/fill': '填写周报',
    '/weekly/team': '团队周报',
    '/weekly/history': '历史记录',
    '/weekly/dept': '部门汇总',
  }
  return map[route.path] || '工作周报'
})
</script>

<template>
  <div class="weekly-container">
    <div class="weekly-header">
      <h2>
        {{ pageTitle }}
        <template v-if="headerTeams.length">
          <el-tag v-for="t in headerTeams" :key="t" size="small" type="info" style="margin-left:6px;font-weight:400;">{{ t }}</el-tag>
        </template>
      </h2>

      <!-- 标题行右侧：分组提交率条形图 -->
      <div v-if="headerStats && headerStats.teams && headerStats.teams.length" class="header-stats">
        <div v-for="t in headerStats.teams" :key="t.teamName" class="bar-chart-group">
          <span class="bar-chart-label">{{ t.teamName }}</span>
          <div class="bar-chart-track">
            <div
              class="bar-chart-fill"
              :class="barColor(t.totalMembers ? t.submittedCount / t.totalMembers : 0)"
              :style="{ width: (t.totalMembers ? (t.submittedCount / t.totalMembers * 100) : 0) + '%' }"
            ></div>
          </div>
          <span class="bar-chart-value">{{ t.submittedCount }}/{{ t.totalMembers }}</span>
          <span class="bar-chart-pct" :class="barColor(t.totalMembers ? t.submittedCount / t.totalMembers : 0)">
            {{ t.totalMembers ? Math.round(t.submittedCount / t.totalMembers * 100) : 0 }}%
          </span>
        </div>
        <span class="stats-divider"></span>
        <span class="stats-summary">
          合计&nbsp;<strong>{{ headerStats.totalSubmitted }}</strong><span style="font-size:11px;">/{{ headerStats.totalMembers }}</span>&nbsp;·&nbsp;<strong>{{ Math.round(headerStats.overallRate * 100) }}%</strong>
        </span>
      </div>
    </div>
    <div class="weekly-content">
      <router-view />
    </div>
  </div>
</template>

<style scoped>
.weekly-container {
  max-width: 1100px;
  margin: 0 auto;
}
.weekly-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}
.weekly-header h2 {
  margin: 0;
}
.weekly-content {
  min-height: 400px;
}

/* 标题行条形图 */
.header-stats {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}
.bar-chart-group {
  display: flex;
  align-items: center;
  gap: 6px;
}
.bar-chart-label {
  font-size: 11px;
  color: var(--gray-500);
  white-space: nowrap;
}
.bar-chart-track {
  width: 100px;
  height: 12px;
  background: var(--gray-100);
  border-radius: 6px;
  overflow: hidden;
}
.bar-chart-fill {
  height: 100%;
  border-radius: 6px;
  transition: width 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}
.bar-chart-fill.high { background: #0abf5b; }
.bar-chart-fill.medium { background: var(--brand); }
.bar-chart-fill.low { background: #e54545; }
.bar-chart-value {
  font-size: 12px;
  font-weight: 700;
  color: var(--gray-900);
  white-space: nowrap;
}
.bar-chart-pct {
  font-size: 11px;
  font-weight: 600;
  min-width: 32px;
}
.bar-chart-pct.high { color: #0abf5b; }
.bar-chart-pct.medium { color: var(--brand); }
.bar-chart-pct.low { color: #e54545; }
.stats-divider {
  width: 1px;
  height: 18px;
  background: var(--border);
}
.stats-summary {
  font-size: 12px;
  color: var(--gray-500);
  white-space: nowrap;
}
.stats-summary strong {
  color: var(--brand);
  font-size: 15px;
}
</style>
