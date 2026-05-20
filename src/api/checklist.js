import request from './request'

export function getList(params) {
  return request.get('/checklists', { params })
}
export function getById(id) {
  return request.get(`/checklists/${id}`)
}
export function create(data) {
  return request.post('/checklists', data)
}
export function update(id, data) {
  return request.put(`/checklists/${id}`, data)
}
export function complete(id) {
  return request.put(`/checklists/${id}/complete`)
}
export function remove(id) {
  return request.delete(`/checklists/${id}`)
}
