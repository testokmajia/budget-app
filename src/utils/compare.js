/**
 * 名称比较工具函数
 * 所有名称比较前都会先 trim 去除首尾空格，防止空格导致的匹配失败
 */

/** 安全的 trim，null/undefined 安全 */
export function sTrim(value) {
  if (value == null) return ''
  return String(value).trim()
}

/** 名称相等比较（trim 后比较） */
export function nameEquals(a, b) {
  return sTrim(a) === sTrim(b)
}

/** 名称是否在数组中（trim 后逐一比较，支持字符串数组和含 name 属性的对象数组） */
export function nameInArray(arr, name) {
  const target = sTrim(name)
  if (!arr) return false
  return arr.some(item => {
    if (typeof item === 'string') {
      return item.trim() === target
    }
    if (item && typeof item === 'object' && 'name' in item) {
      return sTrim(item.name) === target
    }
    return false
  })
}

/** 名称是否在逗号/顿号分隔的字符串列表中（trim 后比较） */
export function nameInList(nameList, name) {
  const target = sTrim(name)
  if (!nameList) return false
  return String(nameList).split(/[、,，]/).some(n => n.trim() === target)
}

/** 数组中查找名称匹配的项（trim 后比较，支持字符串数组和含 name 属性的对象数组） */
export function findByName(arr, name) {
  const target = sTrim(name)
  if (!arr) return undefined
  return arr.find(item => {
    if (typeof item === 'string') {
      return item.trim() === target
    }
    if (item && typeof item === 'object' && 'name' in item) {
      return sTrim(item.name) === target
    }
    return false
  })
}

/** 数组中过滤名称匹配的项 */
export function filterByName(arr, name) {
  const target = sTrim(name)
  if (!arr) return []
  return arr.filter(item => {
    if (typeof item === 'string') {
      return item.trim() === target
    }
    if (item && typeof item === 'object' && 'name' in item) {
      return sTrim(item.name) === target
    }
    return false
  })
}
