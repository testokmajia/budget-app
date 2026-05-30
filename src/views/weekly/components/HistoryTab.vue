<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getMyReports, getTeamHistory, getAllHistory } from '@/api/weekly'
import { parseItems } from '@/utils/workItemParser'
import ReportDetail from './ReportDetail.vue'

const userStore = useUserStore()
const loading = ref(false)
const activeCategory = ref('personal')
const selectedReport = ref(null)
const showExport = ref(false)
const activeExportFormat = ref('word')

// Data per category
const personalData = ref([])
const teamData = ref([])
const deptData = ref([])

// Role checks
const isLeader = computed(() => userStore.hasRole('ROLE_LEADER'))
const isClerkOrAdmin = computed(() => userStore.hasRole('ROLE_CLERK') || userStore.hasRole('ROLE_ADMIN'))

// Categories visible to this user
const categories = computed(() => {
  const cats = [{ key: 'personal', label: '个人周报' }]
  if (isLeader.value || isClerkOrAdmin.value) cats.push({ key: 'team', label: '小组周报' })
  if (isClerkOrAdmin.value) cats.push({ key: 'dept', label: '部门周报' })
  return cats
})

// Current table data based on active category
const tableData = computed(() => {
  if (activeCategory.value === 'team') return teamData.value
  if (activeCategory.value === 'dept') return deptData.value
  return personalData.value
})

// 同一周只保留最新版本（按id降序，每个唯一键只取第一条）
function dedupByWeek(arr, keyFn) {
  if (!arr || !arr.length) return arr
  const sorted = [...arr].sort((a, b) => b.id - a.id)
  const seen = new Set()
  return sorted.filter(r => {
    const key = keyFn(r)
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

onMounted(refresh)

async function refresh() {
  loading.value = true
  try {
    // Always load personal — 同一人同一周只显示最新
    const personal = await getMyReports({}).catch(() => ({ data: [] }))
    personalData.value = dedupByWeek(
      (personal.data || []).map(r => ({
        ...r, doneItems: parseItems(r.doneWork).filter(i => i.item), planItems: parseItems(r.planWork).filter(i => i.item)
      })),
      r => `${r.weekStartDate}_${r.weekEndDate}_${r.userId || r.userName}`
    )

    // Load team if leader or clerk/admin — 同一组同一周只显示最新
    if (isLeader.value || isClerkOrAdmin.value) {
      const team = await getTeamHistory({}).catch(() => ({ data: [] }))
      teamData.value = dedupByWeek(
        (team.data || []).map(r => ({
          ...r, doneItems: parseItems(r.doneWork).filter(i => i.item), planItems: parseItems(r.planWork).filter(i => i.item)
        })),
        r => `${r.weekStartDate}_${r.weekEndDate}_${r.teamName || ''}`
      )
    }

    // Load all if clerk/admin — 同一周只显示最新
    if (isClerkOrAdmin.value) {
      const all = await getAllHistory({}).catch(() => ({ data: [] }))
      deptData.value = dedupByWeek(
        (all.data || []).map(r => ({
          ...r, doneItems: parseItems(r.doneWork).filter(i => i.item), planItems: parseItems(r.planWork).filter(i => i.item)
        })),
        r => `${r.weekStartDate}_${r.weekEndDate}`
      )
    }

    // Set default category
    if (!categories.value.find(c => c.key === activeCategory.value)) {
      activeCategory.value = categories.value[0]?.key || 'personal'
    }
  } finally {
    loading.value = false
  }
}

function selectReport(r) {
  selectedReport.value = r
  showExport.value = false
}

function getTag(s) {
  return s === 'APPROVED' ? 'tag-approved' : s === 'REJECTED' ? 'tag-rejected'
    : s === 'SUBMITTED' ? 'tag-submitted' : 'tag-draft'
}
function getText(s) {
  return s === 'APPROVED' || s === 'FINALIZED' ? '已审定'
    : s === 'REJECTED' ? '已驳回'
    : s === 'SUBMITTED' || s === 'PENDING_REVIEW' ? '已提交'
    : s === 'DRAFT' ? '草稿'
    : '未生成'
}

// Handle category change
function onCategoryChange() {
  selectedReport.value = null
  showExport.value = false
}

// ---- ReportDetail 数据转换 ----
const reportTitle = computed(() => {
  if (activeCategory.value === 'team') return '小组周报'
  if (activeCategory.value === 'dept') return '部门周报'
  return '个人周报'
})

const reportSubtitle = computed(() => {
  const r = selectedReport.value
  if (!r) return ''
  const parts = []
  if (r.userName) parts.push(r.userName)
  if (r.teamName) parts.push(r.teamName)
  parts.push(`${r.weekStartDate} ~ ${r.weekEndDate}`)
  if (r.version > 0) parts.push(`V${r.version}`)
  return parts.join(' · ')
})

const reportStats = computed(() => {
  const r = selectedReport.value
  if (!r) return null
  const doneItems = r.doneItems || []
  return {
    done: doneItems.filter(i => i.status === 'done').length,
    inProgress: doneItems.filter(i => i.status === 'in-progress').length,
    blocked: doneItems.filter(i => i.status === 'blocked').length,
    plan: (r.planItems || []).filter(i => i.item).length,
  }
})

const reportSections = computed(() => {
  const r = selectedReport.value
  if (!r) return []
  const secs = []
  const doneItems = (r.doneItems || []).filter(i => i.item)

  if (doneItems.length) {
    secs.push({ type: 'work-items', title: '本周完成工作', colorBar: 'green', count: doneItems.length, items: doneItems })
  }
  const planItems = (r.planItems || []).filter(i => i.item)
  if (planItems.length) {
    secs.push({ type: 'simple-list', title: '下周工作计划', colorBar: 'blue', count: planItems.length, items: planItems })
  }
  if (r.problems) {
    secs.push({ type: 'problems', title: '遇到的问题', colorBar: 'red', content: r.problems })
  }
  if (r.supportNeeded) {
    secs.push({ type: 'support', title: '需要支持', colorBar: 'amber', content: r.supportNeeded })
  }
  return secs
})

// ---- Export functions ----
function generateReportText(r) {
  const lines = []
  const title = activeCategory.value === 'personal' ? '个人周报' : activeCategory.value === 'team' ? '小组周报' : '部门周报'
  lines.push(title)
  lines.push(`${r.weekStartDate} ~ ${r.weekEndDate}`)
  if (r.userName) lines.push(`姓名：${r.userName}`)
  if (r.teamName) lines.push(`团队：${r.teamName}`)
  lines.push('')

  if (r.doneItems && r.doneItems.length) {
    lines.push('一、本周完成工作')
    r.doneItems.forEach((item, i) => {
      const status = item.status === 'done' ? '✓' : item.status === 'blocked' ? '⚠' : '◉'
      lines.push(`  ${status} ${item.item}${item.progress ? '（' + item.progress + '）' : ''}`)
    })
    lines.push('')
  }

  if (r.planItems && r.planItems.length) {
    lines.push('二、下周工作计划')
    r.planItems.forEach((item, i) => {
      lines.push(`  · ${item.item}`)
    })
    lines.push('')
  }

  if (r.problems) {
    lines.push('三、遇到的问题')
    lines.push(`  ${r.problems}`)
    lines.push('')
  }

  if (r.supportNeeded) {
    lines.push('四、需要支持')
    lines.push(`  ${r.supportNeeded}`)
    lines.push('')
  }

  if (r.reviewComment) {
    lines.push(`批注：${r.reviewComment}`)
  }

  return lines.join('\n')
}

function generateReportHtml(r) {
  const title = activeCategory.value === 'personal' ? '个人周报' : activeCategory.value === 'team' ? '小组周报' : '部门周报'
  let html = `<!DOCTYPE html><html><head><meta charset='UTF-8'><title>${title}</title>
<style>body{font-family:'PingFang SC','Microsoft YaHei',sans-serif;line-height:1.8;max-width:800px;margin:0 auto;padding:20px;color:#333;}
h1{text-align:center;border-bottom:2px solid #1a1a1a;padding-bottom:10px;margin-bottom:20px;}
h3{color:#006eff;margin-top:20px;}
.period{text-align:center;color:#999;font-size:14px;margin-bottom:20px;}
.item-list{margin:0;padding-left:18px;}
.item-list li{padding:4px 0;}
.problems{background:#fff3f3;padding:12px;border-radius:4px;color:#c41230;}
.support{background:#f0f7ff;padding:12px;border-radius:4px;color:#006eff;}
.comment{margin-top:20px;padding:12px;background:#f5f5f5;border-radius:4px;}
</style></head><body>
<h1>${title}</h1>
<p class="period">${r.weekStartDate} ~ ${r.weekEndDate}</p>
${r.userName ? `<p>姓名：${r.userName}</p>` : ''}
${r.teamName ? `<p>团队：${r.teamName}</p>` : ''}`

  if (r.doneItems && r.doneItems.length) {
    html += '<h3>一、本周完成工作</h3><ul class="item-list">'
    r.doneItems.forEach(item => {
      const statusIcons = { 'done': '✅', 'in-progress': '🔄', 'blocked': '⚠️' }
      html += `<li>${statusIcons[item.status] || ''} ${item.item}${item.progress ? '（' + item.progress + '）' : ''}</li>`
    })
    html += '</ul>'
  }

  if (r.planItems && r.planItems.length) {
    html += '<h3>二、下周工作计划</h3><ul class="item-list">'
    r.planItems.forEach(item => {
      html += `<li>${item.item}</li>`
    })
    html += '</ul>'
  }

  if (r.problems) {
    html += `<h3>三、遇到的问题</h3><div class="problems">${r.problems}</div>`
  }

  if (r.supportNeeded) {
    html += `<h3>四、需要支持</h3><div class="support">${r.supportNeeded}</div>`
  }

  if (r.reviewComment) {
    html += `<div class="comment"><strong>批注：</strong>${r.reviewComment}</div>`
  }

  html += '</body></html>'
  return html
}

// 预览用：只返回 body 内部内容，不包含完整 HTML 文档结构（避免 v-html 注入非法嵌套 DOM）
function generatePreviewHtml(r) {
  const title = activeCategory.value === 'personal' ? '个人周报' : activeCategory.value === 'team' ? '小组周报' : '部门周报'
  let html = '<div class="preview-doc">'
  html += '<h1>' + title + '</h1>'
  html += '<p class="period">' + r.weekStartDate + ' ~ ' + r.weekEndDate + '</p>'
  if (r.userName) html += '<p>姓名：' + r.userName + '</p>'
  if (r.teamName) html += '<p>团队：' + r.teamName + '</p>'

  if (r.doneItems && r.doneItems.length) {
    html += '<h3>一、本周完成工作</h3><ul class="item-list">'
    r.doneItems.forEach(item => {
      const icons = { 'done': '✅', 'in-progress': '🔄', 'blocked': '⚠️' }
      html += '<li>' + (icons[item.status] || '') + ' ' + item.item + (item.progress ? '（' + item.progress + '）' : '') + '</li>'
    })
    html += '</ul>'
  }

  if (r.planItems && r.planItems.length) {
    html += '<h3>二、下周工作计划</h3><ul class="item-list">'
    r.planItems.forEach(item => {
      html += '<li>' + item.item + '</li>'
    })
    html += '</ul>'
  }

  if (r.problems) {
    html += '<h3>三、遇到的问题</h3><div class="problems">' + r.problems + '</div>'
  }

  if (r.supportNeeded) {
    html += '<h3>四、需要支持</h3><div class="support">' + r.supportNeeded + '</div>'
  }

  if (r.reviewComment) {
    html += '<div class="comment"><strong>批注：</strong>' + r.reviewComment + '</div>'
  }

  html += '</div>'
  return html
}

function openExport() {
  showExport.value = true
  activeExportFormat.value = 'word'
}

function handleDownloadWord() {
  if (!selectedReport.value) return
  const html = generateReportHtml(selectedReport.value)
  const blob = new Blob([html], { type: 'application/msword' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  const dateStr = (selectedReport.value.weekStartDate || '').replace(/-/g, '')
  a.download = `周报_${dateStr}.doc`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('Word 下载成功')
}

function handleDownloadHtml() {
  if (!selectedReport.value) return
  const html = generateReportHtml(selectedReport.value)
  const blob = new Blob([html], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  const dateStr = (selectedReport.value.weekStartDate || '').replace(/-/g, '')
  a.download = `周报_${dateStr}.html`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('HTML 下载成功')
}

function handleCopy() {
  if (!selectedReport.value) return
  const text = generateReportText(selectedReport.value)
  navigator.clipboard.writeText(text).then(() => ElMessage.success('已复制到剪贴板'))
}
</script>

<template>
  <div v-loading="loading">
    <!-- Category tabs -->
    <el-radio-group v-model="activeCategory" size="small" style="margin-bottom:16px;" @change="onCategoryChange">
      <el-radio-button v-for="cat in categories" :key="cat.key" :value="cat.key">
        {{ cat.label }}
      </el-radio-button>
    </el-radio-group>

    <div class="history-layout" style="min-height:480px;">
      <!-- Left sidebar: report list -->
      <div class="history-sidebar">
        <div style="font-size:11px;font-weight:700;color:var(--gray-400);padding:6px 8px 4px;">
          {{ activeCategory === 'personal' ? '个人周报' : activeCategory === 'team' ? '小组周报' : '部门周报' }}
          <span style="font-weight:400;">· {{ tableData.length }}条</span>
        </div>

        <div v-if="tableData.length">
          <div
            v-for="r in tableData" :key="r.id"
            class="sidebar-member"
            :class="{ active: selectedReport?.id === r.id }"
            @click="selectReport(r)"
          >
            <div class="member-info">
              <div class="member-name" style="font-size:12px;">{{ r.weekStartDate }} ~ {{ r.weekEndDate }}</div>
              <div class="member-meta">
                <span v-if="r.userName">{{ r.userName }}</span>
                <span v-if="r.teamName"> · {{ r.teamName }}</span>
              </div>
            </div>
            <span class="h-pill" :class="r.status === 'APPROVED' || r.status === 'FINALIZED' || r.status === 'SUBMITTED' || r.status === 'PENDING_REVIEW' ? 'ok' : 'no'">{{ getText(r.status) }}</span>
          </div>
        </div>
        <div v-else style="padding:16px;text-align:center;font-size:13px;color:var(--gray-400);">
          暂无记录
        </div>
      </div>

      <!-- Right: detail panel -->
      <div class="history-main">
        <!-- 详情视图（使用统一组件） -->
        <ReportDetail
          v-if="selectedReport && !showExport"
          :title="reportTitle"
          :subtitle="reportSubtitle"
          :status="selectedReport.status"
          :stats="reportStats"
          :sections="reportSections"
          :reviewComment="selectedReport.reviewComment"
          :reviewLabel="selectedReport.status === 'REJECTED' ? '驳回原因' : '批注'"
          :isRejected="selectedReport.status === 'REJECTED'"
        >
          <template #actions>
            <el-button size="small" @click="openExport">📮 导出</el-button>
          </template>
        </ReportDetail>

        <!-- Export view -->
        <template v-if="selectedReport && showExport">
          <div class="detail-topbar">
            <div style="font-size:14px;font-weight:700;">📮 导出周报</div>
            <el-button size="small" @click="showExport = false">← 返回详情</el-button>
          </div>
          <div class="detail-body">
            <!-- Format cards -->
            <div style="display:flex;gap:10px;margin-bottom:16px;">
              <div class="export-card" :class="{ active: activeExportFormat === 'word' }" @click="activeExportFormat = 'word'">
                <div style="font-size:24px;margin-bottom:4px;">📄</div>
                <div style="font-size:12px;font-weight:600;">Word 文档</div>
                <div style="font-size:10px;color:var(--gray-400);">标准公文格式</div>
              </div>
              <div class="export-card" :class="{ active: activeExportFormat === 'html' }" @click="activeExportFormat = 'html'">
                <div style="font-size:24px;margin-bottom:4px;">🌐</div>
                <div style="font-size:12px;font-weight:600;">HTML 网页</div>
                <div style="font-size:10px;color:var(--gray-400);">企微/邮件分享</div>
              </div>
              <div class="export-card" :class="{ active: activeExportFormat === 'text' }" @click="activeExportFormat = 'text'">
                <div style="font-size:24px;margin-bottom:4px;">📋</div>
                <div style="font-size:12px;font-weight:600;">纯文本</div>
                <div style="font-size:10px;color:var(--gray-400);">直接复制到聊天</div>
              </div>
            </div>

            <!-- Preview -->
            <div class="export-preview" v-if="activeExportFormat !== 'text'">
              <div class="export-preview-header">
                {{ activeExportFormat === 'word' ? '📄 Word 预览' : '🌐 HTML 预览' }}
              </div>
              <div class="export-preview-body word-preview" v-html="generatePreviewHtml(selectedReport)">
              </div>
            </div>
            <div class="export-preview" v-else>
              <div class="export-preview-header">📋 纯文本预览</div>
              <div class="export-preview-body">
                <pre style="font-family:var(--font-sans);white-space:pre-wrap;font-size:13px;line-height:1.8;margin:0;">{{ generateReportText(selectedReport) }}</pre>
              </div>
            </div>

            <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:12px;">
              <el-button size="small" @click="handleCopy">📋 复制纯文本</el-button>
              <el-button size="small" @click="handleDownloadHtml">🌐 下载 HTML</el-button>
              <el-button size="small" type="primary" @click="handleDownloadWord">📥 下载 Word</el-button>
            </div>
          </div>
        </template>

        <div v-if="!selectedReport" class="detail-body" style="display:flex;align-items:center;justify-content:center;color:var(--gray-400);font-size:14px;">
          选择左侧一条周报记录查看详情
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.h-pill {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}
.h-pill.ok { background: #ecfdf5; color: #059669; }
.h-pill.no { background: #fef2f2; color: #dc2626; }

.history-layout {
  display: flex;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
}

.history-sidebar {
  width: 290px;
  flex-shrink: 0;
  background: #fafbfc;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.history-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.sidebar-member {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.12s;
  border: 1px solid transparent;
  margin: 0 4px 1px;
}
.sidebar-member:hover { background: #f0f4ff; }
.sidebar-member.active { background: #fff; border-color: #dbeafe; box-shadow: 0 1px 2px rgba(0,0,0,.03); }
.member-info { min-width: 0; flex: 1; }
.member-name { font-size: 13px; font-weight: 600; color: var(--gray-700); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.member-meta { font-size: 11px; color: var(--gray-300); margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.detail-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid #eee;
}
.detail-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
}

/* Export styles */
.export-card {
  flex: 1; padding: 10px; border: 1px solid #e5e7eb; border-radius: 4px;
  cursor: pointer; text-align: center; transition: all 0.12s; background: #fff;
}
.export-card:hover { border-color: #b3d4ff; }
.export-card.active { border-color: var(--brand); background: #f0f5ff; }

.export-preview {
  border: 1px solid #e5e7eb; border-radius: 4px; overflow: hidden; background: #fff;
}
.export-preview-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 12px; background: var(--gray-50); border-bottom: 1px solid #e5e7eb;
  font-size: 11px; font-weight: 600; color: var(--gray-400);
}
.export-preview-body {
  padding: 16px 20px; background: #fff; max-height: 280px; overflow-y: auto;
}
.word-preview {
  font-family: 'Songti SC', 'SimSun', 'Noto Serif CJK SC', serif;
  line-height: 1.8; color: #333;
}
.word-preview :deep(h1) { text-align: center; border-bottom: 2px solid #1a1a1a; padding-bottom: 10px; margin-bottom: 16px; font-size: 18px; }
.word-preview :deep(.period) { text-align: center; color: #999; font-size: 12px; margin-bottom: 16px; }
.word-preview :deep(h3) { color: #006eff; margin-top: 20px; font-size: 14px; }
.word-preview :deep(.problems) { background: #fff3f3; padding: 12px; border-radius: 4px; color: #c41230; }
.word-preview :deep(.support) { background: #f0f7ff; padding: 12px; border-radius: 4px; color: #006eff; }
.word-preview :deep(.comment) { margin-top: 20px; padding: 12px; background: #f5f5f5; border-radius: 4px; }
</style>
