<template>
  <div class="work-pills">
    <span
      v-for="(w, i) in visibleItems"
      :key="i"
      class="work-pill"
      :class="(w.status === 'done' || w._type === 'done') ? 'done-pill' : 'plan-pill'"
    >{{ truncate(w.item) }}</span>
    <span v-if="overflow > 0" class="work-pill" style="background:var(--gray-50);color:var(--gray-400);border-color:var(--gray-100);">+{{ overflow }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: { type: Array, required: true },
  max: { type: Number, default: 5 },
})

const visibleItems = computed(() => (props.items || []).slice(0, props.max))
const overflow = computed(() => Math.max(0, (props.items || []).length - props.max))

function truncate(text) {
  if (!text) return ''
  return text.length > 20 ? text.slice(0, 20) + '…' : text
}
</script>

<style scoped>
/* Exactly matches demo .work-pills */
.work-pills { display: flex; gap: 6px; flex-wrap: wrap; }
.work-pill {
  padding: 4px 10px;
  border-radius: 2px;
  font-size: 12px;
  font-weight: 500;
  background: var(--gray-50);
  border: 1px solid var(--gray-200);
  color: var(--gray-700);
  max-width: 200px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.done-pill { background: var(--green-bg); border-color: #85e0b0; color: #006633; }
.plan-pill { background: var(--brand-light); border-color: #bfdbfe; color: #1e40af; }
</style>
