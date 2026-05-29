/**
 * 将单段文本（可能含 markdown 基础语法）转为 HTML。
 * 处理：**bold**、- 无序列表、1. 有序列表、空行分隔段落、\n 换行。
 */
function renderBlock(text) {
  if (!text) return ''
  const lines = text.split('\n')
  const result = []
  let i = 0

  while (i < lines.length) {
    const line = lines[i]

    // 空行 → 段落分隔，跳过
    if (line.trim() === '') {
      i++
      continue
    }

    // 有序列表项：1. xxx / 2. xxx
    const olMatch = line.match(/^(\d+)\.\s+(.*)/)
    if (olMatch) {
      result.push('<ol>')
      while (i < lines.length) {
        const olLine = lines[i].match(/^(\d+)\.\s+(.*)/)
        if (!olLine) break
        result.push('<li>' + formatInline(olLine[2]) + '</li>')
        i++
      }
      result.push('</ol>')
      continue
    }

    // 无序列表项：- xxx / * xxx
    const ulMatch = line.match(/^[-*]\s+(.*)/)
    if (ulMatch) {
      result.push('<ul>')
      while (i < lines.length) {
        const ulLine = lines[i].match(/^[-*]\s+(.*)/)
        if (!ulLine) break
        result.push('<li>' + formatInline(ulLine[1]) + '</li>')
        i++
      }
      result.push('</ul>')
      continue
    }

    // 普通段落：收集连续非空行
    const paragraphLines = []
    while (i < lines.length && lines[i].trim() !== '' && !lines[i].match(/^(\d+\.|[-*])\s/)) {
      paragraphLines.push(lines[i])
      i++
    }
    if (paragraphLines.length > 0) {
      result.push('<p>' + formatInline(paragraphLines.join('\n')) + '</p>')
    }
  }

  return result.join('')
}

/**
 * 行内格式化：**bold** → <strong>bold</strong>，\n → <br>
 */
function formatInline(text) {
  if (!text) return ''
  return text
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

/**
 * 将后端存储的 JSON 汇总内容转为 HTML 显示。
 * JSON 结构：{ overview, keyProgress, commonIssues, nextWeekPlans, coordinationItems }
 * 非 JSON 文本则直接渲染。
 */
export function formatSummaryContent(text) {
  if (!text) return ''

  try {
    const obj = JSON.parse(text)
    if (typeof obj === 'object') {
      const sections = [
        { key: 'overview', title: '本周工作概览' },
        { key: 'keyProgress', title: '重点工作进展' },
        { key: 'commonIssues', title: '共性问题与风险' },
        { key: 'nextWeekPlans', title: '下周重点计划' },
        { key: 'coordinationItems', title: '需要协调的事项' },
      ]
      let html = ''
      for (const s of sections) {
        if (obj[s.key]) {
          html += '<h3>' + s.title + '</h3>' + renderBlock(obj[s.key])
        }
      }
      return html
    }
  } catch {
    // 非 JSON，直接渲染
  }

  return renderBlock(text)
}
