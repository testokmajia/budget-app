<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { getSubmittedCount } from '@/api/weekly'
import WeeklyFillTab from './components/WeeklyFillTab.vue'
import TeamSummaryTab from './components/TeamSummaryTab.vue'
import DeptReportTab from './components/DeptReportTab.vue'

const userStore = useUserStore()

const activeTab = ref('fill')
const submittedCount = ref(0)

const isLeaderOrClerk = computed(() => {
  return userStore.hasRole('ROLE_ADMIN') || userStore.hasRole('ROLE_CLERK')
})

async function loadCounts() {
  try {
    const res = await getSubmittedCount()
    submittedCount.value = res.data || 0
  } catch { submittedCount.value = 0 }
}

function onTabChange(tab) {
  if (tab === 'summary') {
    loadCounts()
  }
}

loadCounts()
</script>

<template>
  <div class="weekly-container">
    <div class="weekly-header">
      <h2>工作周报</h2>
      <el-radio-group v-model="activeTab" size="default" @change="onTabChange">
        <el-radio-button value="fill">本周填报</el-radio-button>
        <el-radio-button value="summary">组内汇总</el-radio-button>
        <el-radio-button v-if="isLeaderOrClerk" value="dept">部门汇总</el-radio-button>
      </el-radio-group>
    </div>

    <div class="weekly-content">
      <WeeklyFillTab v-if="activeTab === 'fill'" />
      <TeamSummaryTab v-if="activeTab === 'summary'" @submitted="loadCounts" />
      <DeptReportTab v-if="activeTab === 'dept'" />
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
.tab-badge {
  margin-left: 4px;
}
.weekly-content {
  min-height: 400px;
}
</style>
