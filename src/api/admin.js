import request from './request'

// 用户管理
export function getUsers() {
  return request.get('/admin/users')
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
