import request from './request'

// 需求列表
export function getList(params) {
  return request.get('/requirements', { params })
}

// 需求详情
export function getById(id) {
  return request.get(`/requirements/${id}`)
}

// 统计
export function getStats() {
  return request.get('/requirements/stats')
}

// 获取系统列表（架构管理岗选择系统时使用）
export function getSystems() {
  return request.get('/requirements/systems')
}

// 获取筛选选项
export function getFilterOptions() {
  return request.get('/requirements/filter-options')
}

// 创建需求
export function create(data) {
  return request.post('/requirements', data)
}

// 审批通过
export function approve(id, data) {
  return request.post(`/requirements/${id}/approve`, data)
}

// 驳回
export function reject(id, data) {
  return request.post(`/requirements/${id}/reject`, data)
}

// 架构管理岗提交评估
export function evaluate(id, data) {
  return request.post(`/requirements/${id}/evaluate`, data)
}

// 团队组长指派项目经理和产品经理（按系统，一步完成，systemItems带pm和pd字段）
export function teamLeaderAssign(id, data) {
  return request.post(`/requirements/${id}/assign-team`, data)
}

// 产品经理上传需求说明书
export function uploadSpec(id, data) {
  return request.post(`/requirements/${id}/upload-spec`, data)
}

// 多方确认需求说明书
export function confirmSpec(id, data) {
  return request.post(`/requirements/${id}/confirm-spec`, data)
}

// 上传需求说明书附件
export function uploadRequestFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/upload/request', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// 获取默认确认人员
export function getDefaultReviewers(id) {
  return request.get(`/requirements/${id}/default-reviewers`)
}

// 重新提交
export function resubmit(id, data) {
  return request.post(`/requirements/${id}/resubmit`, data)
}

// PM启动实施
export function startImplementation(id, data) {
  return request.post(`/requirements/${id}/start-implementation`, data)
}

// PM/PD填写正式投产日期
export function setProduction(id, data) {
  return request.post(`/requirements/${id}/set-production`, data)
}

// 业务提出人确认关闭
export function closeRequirement(id) {
  return request.post(`/requirements/${id}/close`)
}
