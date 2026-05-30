const BASE_URL = 'https://www.techmaodian.online/api'

function request(options) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token') || ''
    const header = { 'Content-Type': 'application/json' }
    if (token) header.Authorization = 'Bearer ' + token

    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header,
      success(res) {
        if (res.statusCode === 401) {
          wx.removeStorageSync('token')
          wx.removeStorageSync('user')
          reject(new Error('登录已过期'))
          return
        }
        // 优先使用后端返回的中文错误消息
        if (res.statusCode >= 400) {
          const errMsg = res.data?.error || '请求失败'
          wx.showToast({ title: errMsg, icon: 'none' })
          reject(new Error(errMsg))
          return
        }
        if (res.data.success === false) {
          wx.showToast({ title: res.data.error || '请求失败', icon: 'none' })
          reject(new Error(res.data.error))
          return
        }
        resolve(res.data)
      },
      fail(err) {
        wx.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = {
  get(url, params) {
    const query = params
      ? '?' + Object.keys(params)
          .filter(k => params[k] !== undefined && params[k] !== null && params[k] !== '')
          .map(k => k + '=' + encodeURIComponent(params[k]))
          .join('&')
      : ''
    return request({ url: url + query, method: 'GET' })
  },
  post(url, data) {
    return request({ url, method: 'POST', data })
  },
  put(url, data) {
    return request({ url, method: 'PUT', data })
  },
  del(url) {
    return request({ url, method: 'DELETE' })
  }
}
