import request from './request'

export function getList(params) { return request.get('/test-reports', { params }) }
export function getById(id) { return request.get(`/test-reports/${id}`) }
export function getStats() { return request.get('/test-reports/stats') }
export function getByRequirement(reqId) { return request.get(`/test-reports/by-requirement/${reqId}`) }
export function create(data) { return request.post('/test-reports', data) }
export function confirm(id, data) { return request.post(`/test-reports/${id}/confirm`, data) }
export function reject(id, data) { return request.post(`/test-reports/${id}/reject`, data) }
export function uploadTestDocFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/upload/testdoc', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
export function deptReview(id, data) { return request.post(`/test-reports/${id}/dept-review`, data) }
export function getComments(id) { return request.get(`/test-reports/${id}/comments`) }
export function getDefaultReviewers(requirementIds) {
  return request.post('/test-reports/default-reviewers', { requirementIds })
}
