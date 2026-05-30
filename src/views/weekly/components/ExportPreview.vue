<script setup>
import { ref, watch, computed } from 'vue'
import { exportWord, exportHtml, getDeptReport } from '@/api/weekly'
import { ElMessage } from 'element-plus'

const props = defineProps({
  reportId: { type: Number, default: null },
  visible: { type: Boolean, default: false },
})
const emit = defineEmits(['update:visible'])

const activeFormat = ref('word')
const reportData = ref(null)
const loading = ref(false)

watch(() => props.visible, async (v) => {
  if (v && props.reportId) {
    loading.value = true
    try {
      const res = await getDeptReport(props.reportId)
      reportData.value = res.data
    } finally { loading.value = false }
  }
})

function parseContent(content) {
  try {
    const obj = JSON.parse(content)
    return {
      overview: obj.overview || '',
      keyProgress: obj.keyProgress || '',
      commonIssues: obj.commonIssues || '',
      nextWeekPlans: obj.nextWeekPlans || '',
      coordinationItems: obj.coordinationItems || '',
    }
  } catch { return { overview: content || '' } }
}

// Safe content sections rendered via template interpolation (auto-escaped, no XSS)
const sections = computed(() => {
  if (!reportData.value) return { overview: '' }
  return parseContent(reportData.value.editedContent || reportData.value.mergedContent)
})

const plainTextPreview = computed(() => {
  const c = sections.value
  return [c.overview, c.keyProgress, c.commonIssues, c.nextWeekPlans, c.coordinationItems]
    .filter(Boolean).join('\n\n')
})

function switchFormat(fmt) {
  activeFormat.value = fmt
}

async function handleDownloadWord() {
  try {
    const res = await exportWord(props.reportId)
    const url = URL.createObjectURL(new Blob([res], { type: 'application/octet-stream' }))
    const a = document.createElement('a'); a.href = url; a.download = '部门周报.doc'; a.click()
    URL.revokeObjectURL(url)
  } catch(e) { console.error('Word download failed', e); ElMessage.error('Word 下载失败') }
}

async function handleDownloadHtml() {
  try {
    const res = await exportHtml(props.reportId)
    const blob = new Blob([typeof res === 'string' ? res : res.data || ''], { type: 'text/html;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url; a.download = '部门周报.html'; a.click()
    URL.revokeObjectURL(url)
  } catch(e) { console.error('HTML download failed', e); ElMessage.error('HTML 下载失败') }
}

function handleCopy() {
  navigator.clipboard.writeText(plainTextPreview.value).then(() => ElMessage.success('已复制'))
}
</script>

<template>
  <el-dialog :model-value="visible" @update:model-value="emit('update:visible', $event)" title="📮 导出与分发" width="700px" top="5vh">
    <div v-loading="loading">
      <!-- Format selector -->
      <div style="display:flex;gap:12px;margin-bottom:16px">
        <div class="export-format-card" :class="{ active: activeFormat === 'word' }" @click="switchFormat('word')">
          <div class="export-format-icon">📄</div>
          <div class="export-format-name">Word 文档</div>
          <div class="export-format-desc">标准公文格式，含页眉页脚</div>
        </div>
        <div class="export-format-card" :class="{ active: activeFormat === 'html' }" @click="switchFormat('html')">
          <div class="export-format-icon">🌐</div>
          <div class="export-format-name">HTML 网页</div>
          <div class="export-format-desc">企业微信/邮件分享</div>
        </div>
        <div class="export-format-card" :class="{ active: activeFormat === 'text' }" @click="switchFormat('text')">
          <div class="export-format-icon">📋</div>
          <div class="export-format-name">纯文本</div>
          <div class="export-format-desc">直接复制到聊天</div>
        </div>
      </div>

      <!-- Preview — safe template rendering (no v-html, Vue auto-escapes all {{ }}) -->
      <div class="export-preview">
        <div class="export-preview-header">
          <span>{{ activeFormat === 'word' ? '📄 Word 预览' : activeFormat === 'html' ? '🌐 HTML 预览' : '📋 纯文本预览' }}</span>
        </div>

        <!-- Word/HTML format preview -->
        <div v-if="activeFormat !== 'text'" class="export-preview-body word-preview">
          <div class="preview-title-row">
            <div class="preview-doc-title">研发部工作周报</div>
            <div class="preview-date" v-if="reportData">{{ reportData.weekStartDate }} — {{ reportData.weekEndDate }}</div>
          </div>
          <div class="preview-section">
            <div class="preview-section-title">一、本周工作概览</div>
            <p class="preview-p">{{ sections.overview }}</p>
          </div>
          <div class="preview-section" v-if="sections.keyProgress">
            <div class="preview-section-title">二、重点工作进展</div>
            <p class="preview-p pre-wrap">{{ sections.keyProgress }}</p>
          </div>
          <div class="preview-section" v-if="sections.commonIssues">
            <div class="preview-section-title">三、共性问题与风险</div>
            <p class="preview-p pre-wrap">{{ sections.commonIssues }}</p>
          </div>
          <div class="preview-section" v-if="sections.nextWeekPlans">
            <div class="preview-section-title">四、下周重点计划</div>
            <p class="preview-p pre-wrap">{{ sections.nextWeekPlans }}</p>
          </div>
          <div class="preview-section" v-if="sections.coordinationItems">
            <div class="preview-section-title">五、需要协调的事项</div>
            <p class="preview-p pre-wrap">{{ sections.coordinationItems }}</p>
          </div>
          <div class="preview-footer" v-if="reportData">
            审定人：{{ reportData.finalizedByName || '-' }} · {{ reportData.finalizedAt || '' }}
          </div>
        </div>

        <!-- Plain text preview -->
        <div v-else class="export-preview-body">
          <pre class="text-preview">{{ plainTextPreview }}</pre>
        </div>
      </div>

      <!-- Actions -->
      <div style="display:flex;align-items:center;justify-content:space-between;margin-top:12px">
        <div style="display:flex;align-items:center;gap:8px;font-size:12px;color:var(--gray-500)" v-if="reportData">
          <span v-if="reportData.finalizedAt">✅ 已审定 · {{ reportData.finalizedByName || '-' }} · {{ reportData.finalizedAt }}</span>
        </div>
        <div style="display:flex;gap:8px;margin-left:auto">
          <button class="btn btn-ghost" style="font-size:11px;padding:5px 14px" @click="handleCopy">📋 复制纯文本</button>
          <button class="btn btn-ghost" style="font-size:11px;padding:5px 14px" @click="handleDownloadHtml">🌐 下载 HTML</button>
          <button class="btn btn-primary" style="font-size:11px;padding:5px 14px" @click="handleDownloadWord">📥 下载 Word</button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.export-format-card {
  flex: 1; padding: 14px; border: 2px solid var(--gray-200);
  border-radius: var(--radius); cursor: pointer; text-align: center;
  transition: all .2s; background: #fff;
}
.export-format-card:hover { border-color: #b3d4ff; }
.export-format-card.active { border-color: var(--brand-main); background: var(--brand-light); }
.export-format-icon { font-size: 28px; margin-bottom: 6px; }
.export-format-name { font-size: 13px; font-weight: 700; color: var(--gray-900); }
.export-format-desc { font-size: 11px; color: var(--gray-500); margin-top: 2px; }

.export-preview { border: 1px solid var(--gray-200); border-radius: var(--radius); overflow: hidden; background: #fff; }
.export-preview-header { display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; background: var(--gray-50); border-bottom: 1px solid var(--gray-200); font-size: 13px; font-weight: 600; color: var(--gray-700); }
.export-preview-body { padding: 20px 24px; background: #fff; max-height: 360px; overflow-y: auto; font-family: 'Songti SC', 'SimSun', 'Noto Serif CJK SC', serif; }

/* Word-style preview */
.word-preview { line-height: 1.8; color: #333; }
.preview-title-row { text-align: center; padding: 8px 0; border-bottom: 2px solid #1a1a1a; margin-bottom: 16px; }
.preview-doc-title { font-size: 18px; font-weight: 700; }
.preview-date { font-size: 12px; color: #999; margin-top: 4px; }
.preview-section { margin-bottom: 4px; }
.preview-section-title { font-weight: 700; margin-top: 12px; }
.preview-p { text-indent: 2em; margin: 0; }
.preview-p.pre-wrap { white-space: pre-wrap; }
.preview-footer { text-align: right; margin-top: 20px; font-size: 11px; color: #999; }

/* Plain-text preview */
.text-preview { font-family: var(--font-sans); white-space: pre-wrap; font-size: 13px; line-height: 1.8; margin: 0; }

.btn { padding: 8px 18px; border-radius: 2px; font-size: 13px; font-weight: 500; cursor: pointer; border: 1px solid transparent; transition: all .15s; display: inline-flex; align-items: center; gap: 5px; line-height: 1.4; }
.btn-ghost { background: #fff; color: var(--gray-700); border-color: var(--gray-200); }
.btn-ghost:hover { border-color: var(--brand-main); color: var(--brand-main); }
.btn-primary { background: var(--brand-main); color: #fff; border-color: var(--brand-main); }
.btn-primary:hover { background: var(--brand-hover); border-color: var(--brand-hover); }
</style>
