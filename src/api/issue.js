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
export function reviewByLeader(id, data) {
  return request.put(`/issues/${id}/review-leader`, data)
}
export function reviewByAdmin(id, data) {
  return request.put(`/issues/${id}/review-admin`, data)
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
export function updateIssue(id, data) {
  return request.put(`/issues/${id}`, data)
}
export function undoIssue(id) {
  return request.put(`/issues/${id}/undo`)
}
export function exportIssues(params) {
  return request.get('/issues/export', { params, responseType: 'blob' })
}

// Change proposals
export function submitChangeProposal(id, data) {
  return request.post(`/issues/${id}/change-proposals`, data)
}
export function getPendingProposals() {
  return request.get('/change-proposals/pending')
}
export function reviewProposal(id, data) {
  return request.put(`/change-proposals/${id}/review`, data)
}
export function getIssueProposals(id) {
  return request.get(`/issues/${id}/change-proposals`)
}

// Attachments
export function uploadAttachments(id, files) {
  const formData = new FormData()
  files.forEach(f => formData.append('files', f))
  return request.post(`/issues/${id}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
export function getAttachments(id) {
  return request.get(`/issues/${id}/attachments`)
}
export function deleteAttachment(issueId, attId) {
  return request.delete(`/issues/${issueId}/attachments/${attId}`)
}

// Pending issues & system assignments
export function getPendingIssues(params) {
  return request.get('/issues/pending', { params })
}
export function completeAssignment(issueId, assignmentId, data) {
  return request.put(`/issues/${issueId}/systems/${assignmentId}/complete`, data)
}
export function getSystemAssignments(issueId) {
  return request.get(`/issues/${issueId}/systems`)
}
export function feedbackToSubmitter(id) {
  return request.put(`/issues/${id}/feedback`)
}
