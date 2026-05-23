const http = require('../../utils/request')

Page({
  data: {
    stats: { totalIssues: 0, pendingIssues: 0, completedIssues: 0, checklistProgress: 0 },
    recentIssues: []
  },

  onShow() {
    this.load()
  },

  onPullDownRefresh() {
    this.load().then(() => wx.stopPullDownRefresh())
  },

  async load() {
    try {
      const issues = await http.get('/issues', {
        page: 0, size: 5, sortBy: 'createdAt', sortDir: 'desc'
      })
      const content = issues.data?.content || []
      this.setData({
        stats: {
          totalIssues: issues.data?.totalElements || content.length,
          pendingIssues: content.filter(i => i.status !== '已完成' && i.status !== '已关闭').length,
          completedIssues: content.filter(i => i.status === '已完成').length,
          checklistProgress: 0
        },
        recentIssues: content
      })
    } catch (e) {
      // ignore
    }
  },

  statusColor(status) {
    const map = {
      '待分派': '#e6a23c', '已分派': '#409eff', '整改中': '#e6a23c',
      '待确认': '#409eff', '已完成': '#67c23a', '已驳回': '#f56c6c', '已关闭': '#909399'
    }
    return map[status] || '#909399'
  },

  goIssue(e) {
    wx.navigateTo({ url: '/pages/issue/detail?id=' + e.currentTarget.dataset.id })
  },

  handleScan() {
    wx.scanCode({
      scanType: ['qrCode'],
      success(res) {
        const sid = res.result
        if (sid) {
          wx.navigateTo({ url: '/pages/login/index?sid=' + encodeURIComponent(sid) })
        }
      },
      fail() {
        wx.showToast({ title: '扫码取消', icon: 'none' })
      }
    })
  }
})
