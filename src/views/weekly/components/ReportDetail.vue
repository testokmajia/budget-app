<script setup>
defineProps({
  // 头部
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  status: { type: String, default: '' },
  version: { type: Number, default: 0 },

  // 统计条
  stats: { type: Object, default: null },

  // 内容区块数组
  sections: { type: Array, default: () => [] },

  // AI 来源
  aiSource: { type: String, default: '' },

  // 批注
  reviewLabel: { type: String, default: '批注' },
  reviewComment: { type: String, default: '' },
  isRejected: { type: Boolean, default: false },

  // 空状态
  emptyText: { type: String, default: '暂无内容' },
  isEmpty: { type: Boolean, default: false },
})

function statusClass(s) {
  if (s === 'APPROVED' || s === 'FINALIZED') return 'ok'
  if (s === 'SUBMITTED' || s === 'PENDING_REVIEW') return 'warn'
  if (s === 'REJECTED') return 'bad'
  if (s === 'NOT_SUBMITTED' || s === 'NOT_GENERATED') return 'draft'
  return 'draft'
}

function statusText(s) {
  const map = {
    APPROVED: '已审定', FINALIZED: '已审定',
    SUBMITTED: '已提交', PENDING_REVIEW: '已提交',
    REJECTED: '已驳回', NOT_SUBMITTED: '未生成',
    DRAFT: '草稿', NOT_GENERATED: '未生成',
  }
  return map[s] || '未生成'
}

function statusDot(s) {
  if (s === 'done') return 'green'
  if (s === 'in-progress') return 'blue'
  return 'red'
}

function itemTagClass(s) {
  if (s === 'done') return 'done'
  if (s === 'in-progress') return 'active'
  return 'blocked'
}

function itemTagText(s) {
  if (s === 'done') return '已完成'
  if (s === 'in-progress') return '进行中'
  return '受阻'
}
</script>

<template>
  <div class="rd-root">
    <!-- 空状态 -->
    <template v-if="isEmpty">
      <div class="rd-empty">{{ emptyText }}</div>
    </template>

    <template v-else>
      <!-- 头部 -->
      <div class="rd-hd">
        <div class="rd-hd-l">
          <h2 class="rd-title">{{ title }}</h2>
          <div v-if="subtitle" class="rd-sub">{{ subtitle }}</div>
        </div>
        <div class="rd-hd-r">
          <slot name="actions"></slot>
          <span v-if="status" class="rd-tag" :class="statusClass(status)">
            <span class="rd-tag-dot"></span>{{ statusText(status) }}
          </span>
        </div>
      </div>

      <!-- 统计条 -->
      <div v-if="stats" class="rd-statbar">
        <div class="rd-stat theme-green">
          <div class="rd-stat-val">{{ stats.done ?? 0 }}</div>
          <div class="rd-stat-lbl">已完成</div>
        </div>
        <div class="rd-stat theme-blue">
          <div class="rd-stat-val">{{ stats.inProgress ?? 0 }}</div>
          <div class="rd-stat-lbl">进行中</div>
        </div>
        <div class="rd-stat theme-red">
          <div class="rd-stat-val">{{ stats.blocked ?? 0 }}</div>
          <div class="rd-stat-lbl">受阻</div>
        </div>
        <div class="rd-stat theme-gray">
          <div class="rd-stat-val">{{ stats.plan ?? 0 }}</div>
          <div class="rd-stat-lbl">计划</div>
        </div>
      </div>

      <!-- 内容区 -->
      <div class="rd-bd">
        <!-- AI 来源 -->
        <div v-if="aiSource" class="rd-ai">{{ aiSource }}</div>

        <!-- 区块 -->
        <template v-for="(sec, i) in sections" :key="i">
          <div class="rd-sec">
            <div class="rd-sec-hd">
              <div class="rd-sec-bar" :class="sec.colorBar || 'blue'"></div>
              <span class="rd-sec-title">{{ sec.title }}</span>
              <span v-if="sec.count != null" class="rd-sec-note">{{ sec.count }} 项</span>
            </div>

            <!-- 工作项 -->
            <template v-if="sec.type === 'work-items'">
              <ul v-if="sec.items && sec.items.length" class="rd-wi-list">
                <li v-for="(wi, j) in sec.items" :key="j" class="rd-wi-item">
                  <div class="rd-wi-dot" :class="statusDot(wi.status)"></div>
                  <div class="rd-wi-body">
                    <div class="rd-wi-row1">
                      <span class="rd-wi-name">{{ wi.item }}</span>
                      <span class="rd-wi-tag" :class="itemTagClass(wi.status)">{{ itemTagText(wi.status) }}</span>
                    </div>
                    <div v-if="wi.progress" class="rd-wi-prog">{{ wi.progress }}</div>
                  </div>
                </li>
              </ul>
              <div v-else class="rd-prose" style="color:var(--gray-300);text-align:center;">暂无</div>
            </template>

            <!-- 简单列表 -->
            <template v-else-if="sec.type === 'simple-list'">
              <ul v-if="sec.items && sec.items.length" class="rd-sl">
                <li v-for="(si, j) in sec.items" :key="j">{{ si.item }}</li>
              </ul>
              <div v-else class="rd-prose" style="color:var(--gray-300);text-align:center;">暂无</div>
            </template>

            <!-- 富文本 -->
            <div v-else-if="sec.type === 'prose'" class="rd-prose">{{ sec.content || '暂无内容' }}</div>

            <!-- 问题 -->
            <div v-else-if="sec.type === 'problems'" class="rd-prose rd-prose-red">{{ sec.content || '暂无' }}</div>

            <!-- 需要支持 -->
            <div v-else-if="sec.type === 'support'" class="rd-prose rd-prose-amber">{{ sec.content || '暂无' }}</div>
          </div>
        </template>

        <!-- 批注 -->
        <div v-if="reviewComment" class="rd-comment" :class="{ rejected: isRejected }">
          <div class="rd-comment-lbl">{{ reviewLabel }}</div>
          <div class="rd-comment-txt">{{ reviewComment }}</div>
        </div>

        <!-- 默认插槽（按钮等） -->
        <slot></slot>
      </div>
    </template>
  </div>
</template>

<style scoped>
/* ===== 根容器 ===== */
.rd-root {
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.rd-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gray-400);
  font-size: 14px;
  padding: 40px;
}

/* ===== 头部 ===== */
.rd-hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}
.rd-hd-r {
  display: flex;
  align-items: center;
  gap: 8px;
}
.rd-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.3px;
  margin: 0;
}
.rd-sub {
  font-size: 13px;
  color: var(--gray-300);
  margin-top: 3px;
  display: flex;
  gap: 8px;
  align-items: center;
}

/* 状态标签 */
.rd-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 12px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}
.rd-tag-dot { width: 6px; height: 6px; border-radius: 50%; }
.rd-tag.ok { background: #ecfdf5; color: #059669; } .rd-tag.ok .rd-tag-dot { background: #0abf5b; }
.rd-tag.warn { background: #fffbeb; color: #d97706; } .rd-tag.warn .rd-tag-dot { background: #f59e0b; }
.rd-tag.draft { background: #f3f4f6; color: #6b7280; } .rd-tag.draft .rd-tag-dot { background: #9ca3af; }
.rd-tag.bad { background: #fef2f2; color: #dc2626; } .rd-tag.bad .rd-tag-dot { background: #e54545; }

/* ===== 统计条 ===== */
.rd-statbar {
  display: flex;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}
.rd-stat {
  flex: 1;
  text-align: center;
  padding: 12px 8px;
  position: relative;
}
.rd-stat + .rd-stat::after {
  content: '';
  position: absolute;
  left: 0;
  top: 25%;
  height: 50%;
  width: 1px;
  background: #eee;
}
.rd-stat-val {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.5px;
  line-height: 1;
}
.rd-stat-lbl {
  font-size: 12px;
  font-weight: 600;
  margin-top: 3px;
}
.theme-green .rd-stat-val, .theme-green .rd-stat-lbl { color: #0abf5b; }
.theme-blue  .rd-stat-val, .theme-blue  .rd-stat-lbl { color: var(--brand); }
.theme-red   .rd-stat-val, .theme-red   .rd-stat-lbl { color: #e54545; }
.theme-gray  .rd-stat-val, .theme-gray  .rd-stat-lbl { color: var(--gray-300); }

/* ===== 内容区 ===== */
.rd-bd {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

/* AI 来源 */
.rd-ai {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  margin-bottom: 14px;
  background: #f0f5ff;
  border-radius: 4px;
  font-size: 12px;
  color: #3b82f6;
  font-weight: 500;
}

/* 区块 */
.rd-sec { margin-bottom: 18px; }
.rd-sec:last-child { margin-bottom: 0; }
.rd-sec-hd {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.rd-sec-bar {
  width: 3px;
  height: 14px;
  border-radius: 2px;
  flex-shrink: 0;
}
.rd-sec-bar.green { background: #0abf5b; }
.rd-sec-bar.blue  { background: var(--brand); }
.rd-sec-bar.red   { background: #e54545; }
.rd-sec-bar.amber { background: #f59e0b; }
.rd-sec-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--gray-900);
}
.rd-sec-note {
  font-size: 12px;
  color: var(--gray-300);
  font-weight: 400;
}

/* 工作项 */
.rd-wi-list { list-style: none; }
.rd-wi-item {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 6px;
  background: var(--gray-50);
  border-radius: 4px;
  border: 1px solid transparent;
  transition: all 0.15s;
}
.rd-wi-item:hover { background: #fff; border-color: #e5e7eb; box-shadow: 0 1px 2px rgba(0,0,0,.03); }
.rd-wi-item:last-child { margin-bottom: 0; }

.rd-wi-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}
.rd-wi-dot.green { background: #0abf5b; }
.rd-wi-dot.blue  { background: var(--brand); }
.rd-wi-dot.red   { background: #e54545; }

.rd-wi-body { flex: 1; min-width: 0; }

.rd-wi-row1 {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.rd-wi-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-500);
  line-height: 1.5;
}
.rd-wi-tag {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 8px;
  white-space: nowrap;
}
.rd-wi-tag.done    { background: #ecfdf5; color: #059669; }
.rd-wi-tag.active  { background: #eff6ff; color: #2563eb; }
.rd-wi-tag.blocked { background: #fef2f2; color: #dc2626; }

.rd-wi-prog {
  font-size: 13px;
  color: var(--gray-500);
  margin-top: 4px;
  line-height: 1.6;
}
.rd-wi-prog::before {
  content: '';
  display: inline-block;
  width: 1px;
  height: 10px;
  background: var(--gray-200);
  margin-right: 6px;
  vertical-align: middle;
  position: relative;
  top: -1px;
}

/* 简单列表 */
.rd-sl { list-style: none; }
.rd-sl li {
  padding: 8px 12px 8px 20px;
  font-size: 14px;
  color: var(--gray-500);
  line-height: 1.6;
  position: relative;
}
.rd-sl li::before {
  content: '';
  position: absolute;
  left: 7px;
  top: 14px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--gray-200);
}

/* 富文本 */
.rd-prose {
  padding: 14px 16px;
  background: var(--gray-50);
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--gray-500);
  white-space: pre-wrap;
}
.rd-prose-red {
  background: #fef2f2;
  color: #991b1b;
  border-left: 3px solid #e54545;
}
.rd-prose-amber {
  background: #fffbeb;
  color: #92400e;
  border-left: 3px solid #f59e0b;
}

/* 批注 */
.rd-comment {
  margin-top: 18px;
  padding: 10px 14px;
  border-radius: 4px;
  background: var(--gray-50);
  border: 1px solid #eee;
}
.rd-comment.rejected {
  background: #fef2f2;
  border-color: #fecaca;
}
.rd-comment-lbl {
  font-size: 10px;
  font-weight: 700;
  color: var(--gray-300);
  text-transform: uppercase;
  letter-spacing: 0.3px;
  margin-bottom: 4px;
}
.rd-comment.rejected .rd-comment-lbl { color: #dc2626; }
.rd-comment-txt {
  font-size: 14px;
  color: var(--gray-500);
  line-height: 1.6;
}
</style>
