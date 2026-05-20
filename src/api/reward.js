import request from './request'

export function getList(params) {
  return request.get('/rewards', { params })
}
export function getById(id) {
  return request.get(`/rewards/${id}`)
}
export function create(data) {
  return request.post('/rewards', data)
}
export function update(id, data) {
  return request.put(`/rewards/${id}`, data)
}
export function remove(id) {
  return request.delete(`/rewards/${id}`)
}
