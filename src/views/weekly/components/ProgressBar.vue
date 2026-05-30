<template>
  <div class="progress-bar-wrapper" :class="{ compact }">
    <div class="progress-track">
      <div class="progress-fill" :class="`fill--${status}`" :style="{ width: percent + '%' }"></div>
    </div>
    <span v-if="!compact && progress" class="progress-text">{{ progress }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  progress: { type: String, default: '' },
  status: { type: String, default: 'in-progress' },
  compact: { type: Boolean, default: false },
})

const percent = computed(() => {
  if (props.status === 'done') return 100
  if (props.status === 'blocked') return 25
  return 50
})
</script>

<style scoped>
.progress-bar-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}
.progress-track {
  flex: 1;
  height: 4px;
  background: var(--gray-100);
  border-radius: 2px;
  overflow: hidden;
}
.compact .progress-track {
  height: 3px;
}
.progress-fill {
  height: 100%;
  border-radius: 2px;
  transition: width .3s ease;
}
.fill--done { background: var(--green); }
.fill--in-progress { background: var(--brand-main); }
.fill--blocked { background: var(--amber); }
.progress-text {
  font-size: 12px;
  color: var(--gray-500);
  white-space: nowrap;
}
</style>
