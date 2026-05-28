<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { getPendingCount } from '@/api/weekly'
import WeeklyFillTab from './components/WeeklyFillTab.vue'
import PendingApprovalTab from './components/PendingApprovalTab.vue'
import HistoryTab from './components/HistoryTab.vue'
import DeptReportTab from './components/DeptReportTab.vue'

const userStore = useUserStore()

const activeTab = ref('fill')
const pendingCount = ref(0)

const isClerkOrHead = computed(() => {
  return userStore.hasRole('ROLE_CLERK') || userStore.hasRole('ROLE_ADMIN')
})

async function loadPendingCount() {
  try {
    const res = await getPendingCount()
    pendingCount.value = res.data || 0
  } catch { pendingCount.value = 0 }
}

function onTabChange(tab) {
  if (tab === 'approval') {
    loadPendingCount()
  }
}

loadPendingCount()
</script>

<template>
  <div class="weekly-container">
    <div class="weekly-header">
      <h2>工作周报</h2>
      <el-radio-group v-model="activeTab" size="default" @change="onTabChange">
        <el-radio-button value="fill">本周填报</el-radio-button>
        <el-radio-button value="approval">
          待审批
          <el-badge v-if="pendingCount > 0" :value="pendingCount" class="tab-badge" />
        </el-radio-button>
        <el-radio-button value="history">历史记录</el-radio-button>
        <el-radio-button v-if="isClerkOrHead" value="dept">部门汇总</el-radio-button>
      </el-radio-group>
    </div>

    <div class="weekly-content">
      <WeeklyFillTab v-if="activeTab === 'fill'" />
      <PendingApprovalTab v-if="activeTab === 'approval'" @approved="loadPendingCount" />
      <HistoryTab v-if="activeTab === 'history'" />
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
