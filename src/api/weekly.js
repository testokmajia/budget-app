import request from './request'

export function getCurrentWeekReport() { return request.get('/weekly-reports/current') }
export function saveReport(data) { return request.post('/weekly-reports', data) }
export function submitReport(data) { return request.post('/weekly-reports/submit', data) }
export function getMyReports(params) { return request.get('/weekly-reports/my', { params }) }
export function getSmartDraft() { return request.get('/weekly-reports/smart-draft') }

export function getSubmittedList() { return request.get('/weekly-reports/pending') }
export function getSubmittedCount() { return request.get('/weekly-reports/pending/count') }

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

// Department reports
export function mergeReports(data) { return request.post('/department-reports/merge', data) }
export function getDeptReports() { return request.get('/department-reports') }
export function getDeptReport(id) { return request.get(`/department-reports/${id}`) }
export function updateDeptReport(id, data) { return request.put(`/department-reports/${id}`, data) }
export function submitDeptReport(id) { return request.post(`/department-reports/${id}/submit`) }
export function finalizeDeptReport(id) { return request.post(`/department-reports/${id}/finalize`) }
export function exportWord(id) { return request.get(`/department-reports/${id}/export/word`, { responseType: 'blob' }) }
export function exportHtml(id) { return request.get(`/department-reports/${id}/export/html`) }
