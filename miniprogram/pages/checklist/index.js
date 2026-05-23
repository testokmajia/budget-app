const http = require('../../utils/request')

Page({
  data: { list: [] },
  onShow() { this.load() },
  onPullDownRefresh() { this.load().then(() => wx.stopPullDownRefresh()) },
  async load() {
    try {
      const res = await http.get('/checklists', { page: 0, size: 50 })
      this.setData({ list: res.data?.content || [] })
    } catch (e) {}
  },
  goDetail(e) {
    wx.navigateTo({ url: '/pages/checklist/detail?id=' + e.currentTarget.dataset.id })
  }
})
