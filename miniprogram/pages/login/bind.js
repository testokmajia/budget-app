const http = require('../../utils/request')

Page({
  data: { username: '', password: '', loading: false, errorMsg: '', sid: '' },

  onLoad(opts) {
    this.setData({ sid: opts.sid || '' })
  },

  onField(e) {
    const { field } = e.currentTarget.dataset
    this.setData({ [field]: e.detail.value })
  },

  async handleBind() {
    if (!this.data.username || !this.data.password) {
      this.setData({ errorMsg: '请输入用户名和密码' })
      return
    }
    this.setData({ loading: true, errorMsg: '' })
    try {
      await http.post('/auth/qr/bind', {
        sessionId: this.data.sid,
        username: this.data.username,
        password: this.data.password
      })
      wx.showToast({ title: '绑定成功', icon: 'success' })
      setTimeout(() => wx.navigateBack({ delta: 2 }), 1000)
    } catch (e) {
      this.setData({ loading: false, errorMsg: e.message || '绑定失败，请检查账号密码' })
    }
  }
})
