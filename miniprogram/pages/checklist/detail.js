const http = require('../../utils/request')

Page({
  data: { detail: null, id: '' },
  onLoad(opts) {
    this.setData({ id: opts.id })
    this.load(opts.id)
  },
  async load(id) {
    try {
      const res = await http.get('/checklists/' + id)
      this.setData({ detail: res.data })
    } catch (e) {}
  },
  async toggle(e) {
    const itemId = e.currentTarget.dataset.id
    try {
      await http.put('/checklists/' + this.data.id + '/items/' + itemId + '/toggle')
      const items = this.data.detail.items.map(item => {
        if (item.id === itemId) item.completed = !item.completed
        return item
      })
      this.setData({ 'detail.items': items })
    } catch (e) {}
  }
})
