const http = require('../../utils/request')

const typeOptions = [
  { label: '全部类型', value: '' },
  { label: '奖励', value: 'REWARD' },
  { label: '处罚', value: 'PUNISHMENT' }
]

Page({
  data: {
    typeLabels: typeOptions.map(t => t.label),
    list: [], typeIdx: 0, keyword: ''
  },
  onShow() { this.load() },
  onPullDownRefresh() { this.load().then(() => wx.stopPullDownRefresh()) },
  onKeyword(e) { this.setData({ keyword: e.detail.value }) },
  onTypeChange(e) {
    this.setData({ typeIdx: e.detail.value })
    this.load()
  },
  async load() {
    try {
      const typeVal = typeOptions[this.data.typeIdx].value || ''
      const res = await http.get('/rewards', {
        page: 0, size: 50,
        type: typeVal || undefined,
        keyword: this.data.keyword || undefined
      })
      this.setData({ list: res.data?.content || [] })
    } catch (e) {}
  }
})
