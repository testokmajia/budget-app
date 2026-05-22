import request from './request'

// 用户管理
export function getUsers(params) {
  return request.get('/admin/users', { params })
}
export function createUser(data) {
  return request.post('/admin/users', data)
}
export function toggleUser(id) {
  return request.put(`/admin/users/${id}/toggle`)
}
export function resetPassword(id, password) {
  return request.put(`/admin/users/${id}/reset-password`, { password })
}
export function updateUser(id, data) {
  return request.put(`/admin/users/${id}`, data)
}
export function getRoles() {
  return request.get('/admin/roles')
}

// 问题分类
export function getCategories() {
  return request.get('/admin/categories')
}
export function createCategory(data) {
  return request.post('/admin/categories', data)
}
export function updateCategory(id, data) {
  return request.put(`/admin/categories/${id}`, data)
}
export function deleteCategory(id) {
  return request.delete(`/admin/categories/${id}`)
}

// 部门管理
export function getDepartments() {
  return request.get('/admin/departments')
}
export function createDepartment(data) {
  return request.post('/admin/departments', data)
}
export function updateDepartment(id, data) {
  return request.put(`/admin/departments/${id}`, data)
}
export function deleteDepartment(id) {
  return request.delete(`/admin/departments/${id}`)
}

// 所属系统管理
export function getSystems() {
  return request.get('/admin/systems')
}
export function createSystem(data) {
  return request.post('/admin/systems', data)
}
export function updateSystem(id, data) {
  return request.put(`/admin/systems/${id}`, data)
}
export function deleteSystem(id) {
  return request.delete(`/admin/systems/${id}`)
}

// 团队管理
export function getTeams() {
  return request.get('/admin/teams')
}
export function createTeam(data) {
  return request.post('/admin/teams', data)
}
export function updateTeam(id, data) {
  return request.put(`/admin/teams/${id}`, data)
}
export function deleteTeam(id) {
  return request.delete(`/admin/teams/${id}`)
}
