App({
  onLaunch() {
    // 检查登录状态
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
      const user = wx.getStorageSync('user')
      if (user) this.globalData.user = user
    }
  },

  globalData: {
    token: '',
    user: null,
    baseUrl: 'http://82.156.10.177/api'
  }
})
