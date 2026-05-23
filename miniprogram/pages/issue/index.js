const http = require('../../utils/request')

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待分派', value: '待分派' },
  { label: '已分派', value: '已分派' },
  { label: '整改中', value: '整改中' },
  { label: '待确认', value: '待确认' },
  { label: '已完成', value: '已完成' },
  { label: '已驳回', value: '已驳回' }
]

Page({
  data: {
    statusLabels: statusOptions.map(s => s.label),
    list: [], statusIdx: 0, keyword: '', page: 0, hasMore: true
  },

  onShow() { this.load(true) },
  onPullDownRefresh() { this.load(true).then(() => wx.stopPullDownRefresh()) },

  onKeyword(e) { this.setData({ keyword: e.detail.value }) },
  search() { this.load(true) },

  onStatusChange(e) {
    this.setData({ statusIdx: e.detail.value })
    this.load(true)
  },

  async load(reset) {
    if (reset) this.setData({ page: 0, hasMore: true })
    try {
      const statusVal = statusOptions[this.data.statusIdx].value || ''
      const res = await http.get('/issues', {
        page: reset ? 0 : this.data.page,
        size: 20,
        sortBy: 'createdAt',
        sortDir: 'desc',
        status: statusVal || undefined,
        title: this.data.keyword || undefined
      })
      const content = res.data?.content || []
      const list = reset ? content : this.data.list.concat(content)
      this.setData({ list, hasMore: content.length >= 20 })
    } catch (e) {}
  },

  loadMore() {
    this.setData({ page: this.data.page + 1 })
    this.load(false)
  },

  statusColor(status) {
    const map = {
      '待分派':'#e6a23c','已分派':'#409eff','整改中':'#e6a23c',
      '待确认':'#409eff','已完成':'#67c23a','已驳回':'#f56c6c','已关闭':'#909399'
    }
    return map[status] || '#909399'
  },

  goDetail(e) { wx.navigateTo({ url: '/pages/issue/detail?id=' + e.currentTarget.dataset.id }) },
  goSubmit() { wx.navigateTo({ url: '/pages/issue/submit' }) }
})
