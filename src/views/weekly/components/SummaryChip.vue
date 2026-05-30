<template>
  <span class="summary-chip" :class="`${variant}-chip`">
    <slot>{{ label }}</slot>
    <span v-if="count !== undefined" class="chip-num">{{ count }}</span>
  </span>
</template>

<script setup>
defineProps({
  label: { type: String, default: '' },
  count: { type: Number, default: undefined },
  variant: { type: String, default: 'done' }, // done | plan | problem
})
</script>

<style scoped>
/* Exactly matches demo .summary-chip */
.summary-chip {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 12px;
  background: var(--gray-50);
  border-radius: 2px;
  font-size: 12px;
  color: var(--gray-700);
}
.summary-chip .chip-num {
  font-weight: 700; font-size: 16px;
}
.done-chip .chip-num { color: var(--green); }
.plan-chip .chip-num { color: var(--brand-main); }
.problem-chip .chip-num { color: var(--red); }
</style>
