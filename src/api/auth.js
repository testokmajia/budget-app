import request from './request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function initQrLogin() {
  return request.get('/auth/qr/init')
}

export function pollQrStatus(sid) {
  return request.get('/auth/qr/status', { params: { sid } })
}

export function wechatLogin(data) {
  return request.post('/auth/qr/wechat-login', data)
}

export function bindWechat(data) {
  return request.post('/auth/qr/bind', data)
}

export function changePassword(data) {
  return request.put('/auth/password', data)
}

export function sendResetCode(data) {
  return request.post('/auth/send-reset-code', data)
}

export function verifyResetCode(data) {
  return request.post('/auth/verify-reset-code', data)
}

export function resetPassword(data) {
  return request.post('/auth/reset-password', data)
}
