const http = require('../../utils/request')

Page({
  data: {
    loading: true, confirmed: false, needBind: false, errorMsg: '', sid: ''
  },

  onLoad(opts) {
    const sid = opts.sid || ''
    if (!sid) {
      this.setData({ loading: false, errorMsg: '无效的登录会话' })
      return
    }
    this.setData({ sid, loading: false })
  },

  async confirmLogin() {
    this.setData({ loading: true, errorMsg: '' })
    try {
      const loginRes = await wx.login()
      const res = await http.post('/auth/qr/wechat-login', {
        sessionId: this.data.sid,
        code: loginRes.code
      })
      if (res.data?.bound) {
        this.setData({ confirmed: true, loading: false })
      } else {
        this.setData({ needBind: true, loading: false })
      }
    } catch (e) {
      this.setData({ loading: false, errorMsg: e.message || '验证失败，请重试' })
    }
  },

  goBind() {
    wx.redirectTo({ url: '/pages/login/bind?sid=' + this.data.sid })
  },

  goBack() {
    wx.navigateBack()
  }
})
