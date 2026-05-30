<template>
  <div class="work-card" :class="{ dragging: isDragging }" :draggable="draggable && editable" @dragstart="onDragStart" @dragover.prevent @drop.prevent="onDrop">
    <div class="work-card-row">
      <span v-if="draggable && editable" class="work-card-drag" title="拖拽排序">⠿</span>
      <div class="work-card-body">
        <input v-if="editable" class="work-card-title" :value="item" :placeholder="itemPlaceholder" maxlength="30" @input="$emit('update:item', $event.target.value)" />
        <div v-else class="work-card-title" style="font-size:13px">{{ item || '(空)' }}</div>
        <div class="work-card-meta" :style="{ fontSize: editable ? '12px' : '11px' }">
          <div v-if="editable" class="work-card-progress">
            <span style="font-size:12px;color:var(--gray-400)">{{ progressLabel }}</span>
            <input :value="progress" placeholder="进展情况" @input="$emit('update:progress', $event.target.value)" />
          </div>
          <span v-else-if="progress" :style="{ color: 'var(--gray-500)' }">{{ progressLabel }}：{{ progress }}</span>
          <div v-if="editable" class="work-card-status">
            <span class="status-dot done" :class="{ active: status === 'done' }" title="已完成" @click="$emit('update:status', 'done')">✓</span>
            <span class="status-dot progress" :class="{ active: status === 'in-progress' }" title="进行中" @click="$emit('update:status', 'in-progress')">◉</span>
            <span class="status-dot blocked" :class="{ active: status === 'blocked' }" title="受阻" @click="$emit('update:status', 'blocked')">!</span>
          </div>
        </div>
        <div v-if="editable" class="progress-bar-wrap">
          <div class="progress-bar-fill" :class="progressClass" :style="{ width: percent + '%' }"></div>
        </div>
      </div>
      <button v-if="editable" class="work-card-delete" title="删除" @click="$emit('delete')">✕</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
const props = defineProps({
  item: { type: String, required: true }, progress: { type: String, default: '' },
  status: { type: String, default: 'in-progress' }, editable: { type: Boolean, default: true },
  draggable: { type: Boolean, default: true }, itemPlaceholder: { type: String, default: '事项（30字以内）' },
  progressLabel: { type: String, default: '进展' },
})
const emit = defineEmits(['update:item', 'update:progress', 'update:status', 'delete', 'dragstart', 'drop'])
const isDragging = ref(false)
const progressClass = computed(() => props.status === 'done' ? 'done' : props.status === 'blocked' ? 'started' : 'in-progress')
const percent = computed(() => props.status === 'done' ? 100 : props.status === 'blocked' ? 30 : 60)
function onDragStart(e) { isDragging.value = true; e.dataTransfer.effectAllowed = 'move'; e.dataTransfer.setData('text/plain', ''); emit('dragstart', e) }
function onDrop(e) { isDragging.value = false; emit('drop', e) }
</script>

<style scoped>
.work-card { background: #fff; border: 1px solid var(--gray-200); border-radius: var(--radius); padding: 14px 16px; transition: all .2s ease; cursor: grab; position: relative; }
.work-card:hover { border-color: #b3d4ff; box-shadow: 0 2px 4px rgba(0,0,0,.06); transform: translateY(-1px); }
.work-card.dragging { opacity: .5; box-shadow: var(--shadow-lg); }
.work-card-row { display: flex; align-items: flex-start; gap: 12px; }
.work-card-drag { color: var(--gray-400); cursor: grab; font-size: 16px; margin-top: 2px; user-select: none; }
.work-card-body { flex: 1; min-width: 0; }
.work-card-title { font-size: 14px; font-weight: 600; color: var(--gray-900); margin-bottom: 6px; border: none; background: transparent; width: 100%; outline: none; }
.work-card-title::placeholder { color: var(--gray-400); }
.work-card-meta { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.work-card-progress { flex: 1; min-width: 100px; display: flex; align-items: center; gap: 8px; }
.work-card-progress input { border: 1px solid var(--gray-200); border-radius: 2px; padding: 4px 8px; font-size: 12px; width: 100%; outline: none; transition: border-color .2s; }
.work-card-progress input:focus { border-color: var(--brand); }
.work-card-status { display: flex; gap: 6px; }
.status-dot { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; cursor: pointer; border: 2px solid transparent; transition: all .15s; }
.status-dot:hover { transform: scale(1.1); }
.status-dot.done { background: var(--green-bg); color: var(--green); }
.status-dot.progress { background: var(--blue-bg); color: var(--blue); }
.status-dot.blocked { background: var(--red-bg); color: var(--red); }
.status-dot.active { border-color: currentColor; }
.work-card-delete { background: none; border: none; color: var(--gray-400); cursor: pointer; font-size: 16px; padding: 2px; transition: color .2s; }
.work-card-delete:hover { color: var(--red); }
.progress-bar-wrap { height: 4px; background: var(--gray-100); border-radius: 2px; margin-top: 8px; overflow: hidden; }
.progress-bar-fill { height: 100%; border-radius: 2px; transition: width .4s ease; }
.progress-bar-fill.done { background: var(--green); width: 100%; }
.progress-bar-fill.in-progress { background: var(--blue); }
.progress-bar-fill.started { background: var(--amber); }
</style>
