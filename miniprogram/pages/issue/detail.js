const http = require('../../utils/request')

Page({
  data: { detail: null },
  onLoad(opts) { this.load(opts.id) },
  async load(id) {
    try {
      const res = await http.get('/issues/' + id)
      this.setData({ detail: res.data })
    } catch (e) {}
  },
  statusColor(status) {
    const map = {
      '待分派':'#e6a23c','已分派':'#409eff','整改中':'#e6a23c',
      '待确认':'#409eff','已完成':'#67c23a','已驳回':'#f56c6c','已关闭':'#909399'
    }
    return map[status] || '#909399'
  }
})
