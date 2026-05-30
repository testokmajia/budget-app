/**
 * Shared parser for weekly report work items.
 * Supports both legacy pipe-delimited format and new JSON array format.
 *
 * Legacy format:  "item1|progress1\nitem2|progress2"
 * New format:     '[{"item":"...","progress":"...","status":"done|in-progress|blocked"}]'
 */

const DEFAULT_STATUS = 'in-progress'

/**
 * Parse a stored work items string into an array of {item, progress, status}.
 * Auto-detects format: JSON array first, falls back to pipe-delimited.
 *
 * @param {string|null|undefined} text - Raw stored text
 * @returns {Array<{item: string, progress: string, status: string}>}
 */
export function parseItems(text) {
  if (!text) return [{ item: '', progress: '', status: DEFAULT_STATUS }]

  // Try JSON array format first
  try {
    const parsed = JSON.parse(text)
    if (Array.isArray(parsed) && parsed.length > 0) {
      return parsed.map(p => ({
        item: p.item || '',
        progress: p.progress || '',
        status: p.status || DEFAULT_STATUS,
      }))
    }
  } catch {
    // Not JSON, fall through to pipe-delimited parsing
  }

  // Legacy pipe-delimited format: "item|progress\nitem2|progress2"
  const items = text.split('\n')
    .filter(s => s.trim())
    .map(line => {
      const idx = line.indexOf('|')
      if (idx === -1) return { item: line.trim(), progress: '', status: DEFAULT_STATUS }
      return {
        item: line.substring(0, idx).trim(),
        progress: line.substring(idx + 1).trim(),
        status: DEFAULT_STATUS,
      }
    })
  return items.length > 0 ? items : [{ item: '', progress: '', status: DEFAULT_STATUS }]
}

/**
 * Serialize an array of work items to the new JSON array format for storage.
 *
 * @param {Array<{item: string, progress: string, status?: string}>} items
 * @returns {string} JSON string of the array
 */
export function serializeItems(items) {
  const nonEmpty = items
    .filter(s => s.item.trim())
    .map(s => ({
      item: s.item.trim(),
      progress: (s.progress || '').trim(),
      status: s.status || DEFAULT_STATUS,
    }))
  return JSON.stringify(nonEmpty)
}

/**
 * Check if a stored text is in the old pipe-delimited format.
 *
 * @param {string|null|undefined} text
 * @returns {boolean}
 */
export function isLegacyFormat(text) {
  if (!text) return false
  try {
    JSON.parse(text)
    return false
  } catch {
    return text.includes('|')
  }
}

/**
 * Count items by status.
 *
 * @param {Array<{status: string}>} items
 * @returns {{done: number, inProgress: number, blocked: number, total: number}}
 */
export function countByStatus(items) {
  const counts = { done: 0, inProgress: 0, blocked: 0, total: items.length }
  for (const item of items) {
    if (item.status === 'done') counts.done++
    else if (item.status === 'blocked') counts.blocked++
    else counts.inProgress++
  }
  return counts
}

/**
 * Get the display label for a status value.
 */
export function statusLabel(status) {
  const map = { done: '已完成', 'in-progress': '进行中', blocked: '受阻' }
  return map[status] || '进行中'
}
