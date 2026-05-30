import request from './request'

export function getCurrentWeekReport() { return request.get('/weekly-reports/current') }
export function saveReport(data) { return request.post('/weekly-reports', data) }
export function submitReport(data) { return request.post('/weekly-reports/submit', data) }
export function getMyReports(params) { return request.get('/weekly-reports/my', { params }) }
export function getSmartDraft() { return request.get('/weekly-reports/smart-draft') }

export function getSubmittedList() { return request.get('/weekly-reports/pending') }
export function getSubmittedCount() { return request.get('/weekly-reports/pending/count') }

// Aliases used by PendingApprovalTab
export { getSubmittedList as getPendingList }
export function approveReport(id, data) { return request.post(`/weekly-reports/${id}/approve`, data) }
export function rejectReport(id, data) { return request.post(`/weekly-reports/${id}/reject`, data) }

// New: team stats, remind, compare
export function getTeamStats(weekStartDate) { return request.get('/weekly-reports/team-stats', { params: { weekStartDate } }) }
export function remindMember(id) { return request.post(`/weekly-reports/${id}/remind`) }
export function compareReports(from, to) { return request.get('/weekly-reports/compare', { params: { from, to } }) }

// Team summaries
export function mergeTeamSummary(data) { return request.post('/team-summaries/merge', data) }
export function updateTeamSummary(id, data) { return request.put(`/team-summaries/${id}`, data) }
export function submitTeamSummary(id) { return request.post(`/team-summaries/${id}/submit`) }
export function getTeamSummary(id) { return request.get(`/team-summaries/${id}`) }
export function listMyTeamSummaries() { return request.get('/team-summaries/my') }
export function listTeamSummariesByWeek(weekStartDate) { return request.get('/team-summaries/by-week', { params: { weekStartDate } }) }
export function getMyTeams() { return request.get('/team-summaries/my/teams') }
export function getTeamHistory(params) { return request.get('/weekly-reports/team', { params }) }

export function getAllHistory(params) { return request.get('/weekly-reports/all', { params }) }

// Department team status
export function getDeptTeamStatus() { return request.get('/weekly-reports/dept-team-status') }

// Department reports
export function getDeptFinalizedStatus() { return request.get('/department-reports/current-finalized') }
export function mergeReports(data) { return request.post('/department-reports/merge', data) }
export function getDeptReports() { return request.get('/department-reports') }
export function getDeptReport(id) { return request.get(`/department-reports/${id}`) }
export function updateDeptReport(id, data) { return request.put(`/department-reports/${id}`, data) }
export function submitDeptReport(id) { return request.post(`/department-reports/${id}/submit`) }
export function finalizeDeptReport(id) { return request.post(`/department-reports/${id}/finalize`) }
export function exportWord(id) { return request.get(`/department-reports/${id}/export/word`, { responseType: 'blob' }) }
export function exportHtml(id) { return request.get(`/department-reports/${id}/export/html`) }
