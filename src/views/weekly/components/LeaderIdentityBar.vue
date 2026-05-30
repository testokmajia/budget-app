<template>
  <div class="leader-identity-bar">
    <div class="leader-avatar">{{ leaderName?.charAt(0) }}</div>
    <div style="flex:1">
      <div class="leader-identity-name">{{ leaderName }}</div>
      <div class="leader-identity-teams">
        管理团队：
        <span v-for="t in teamNames" :key="t" class="team-tag">{{ t }}</span>
        <span v-if="teamNames.length > 1" class="team-tag-badge">兼任 {{ teamNames.length }} 组</span>
      </div>
    </div>
    <slot name="actions" />
  </div>
</template>

<script setup>
defineProps({
  leaderName: { type: String, required: true },
  teamNames: { type: Array, default: () => [] },
})
</script>

<style scoped>
/* ── Demo: .leader-identity-bar ── */
.leader-identity-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 18px;
  background: #e8f2ff; border: 1px solid #b3d4ff;
  border-radius: var(--radius); margin-bottom: 16px;
}
.leader-avatar {
  width: 40px; height: 40px; border-radius: 50%;
  background: #006eff; color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 16px; flex-shrink: 0;
}
.leader-identity-name { font-size: 14px; font-weight: 700; color: var(--gray-900); }
.leader-identity-teams { font-size: 12px; color: var(--gray-500); margin-top: 2px; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.team-tag { display: inline-block; padding: 2px 10px; border-radius: 2px; font-size: 11px; font-weight: 600; background: #fff; border: 1px solid #b3d4ff; color: #0052d9; }
.team-tag-badge { display: inline-block; padding: 2px 8px; border-radius: 2px; font-size: 10px; font-weight: 600; background: #fff8ed; color: #e08800; }
</style>
