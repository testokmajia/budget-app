import request from './request'

export function getList(params) {
  return request.get('/issues', { params })
}
export function getById(id) {
  return request.get(`/issues/${id}`)
}
export function create(data) {
  return request.post('/issues', data)
}
export function assign(id, data) {
  return request.put(`/issues/${id}/assign`, data)
}
export function submitSolution(id, data) {
  return request.put(`/issues/${id}/solution`, data)
}
export function complete(id) {
  return request.put(`/issues/${id}/complete`)
}
export function confirm(id, data) {
  return request.put(`/issues/${id}/confirm`, data)
}
export function reject(id, data) {
  return request.put(`/issues/${id}/reject`, data)
}
export function closeIssue(id, data) {
  return request.put(`/issues/${id}/close`, data)
}
export function exportIssues(params) {
  return request.get('/issues/export', { params, responseType: 'blob' })
}
