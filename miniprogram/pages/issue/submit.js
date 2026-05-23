const http = require('../../utils/request')

Page({
  data: {
    deptNames: [], occNames: [], typeNames: [],
    departments: [], occasions: [], types: [],
    deptIdx: 0, occIdx: 0, typeIdx: 0,
    title: '', description: '', rootCause: '', permanentSolution: '', permanentDeadline: ''
  },

  onLoad() {
    Promise.all([
      http.get('/admin/departments'),
      http.get('/admin/occasions'),
      http.get('/admin/categories', { type: 'ISSUE' })
    ]).then(([deps, occs, typs]) => {
      const departments = deps.data || []
      const occasions = (occs.data || []).filter(o => o.enabled)
      const types = typs.data || []
      this.setData({
        departments, occasions, types,
        deptNames: departments.map(d => d.name),
        occNames: occasions.map(o => o.name),
        typeNames: types.map(t => t.name)
      })
    }).catch(() => {})
  },

  onDept(e) { this.setData({ deptIdx: e.detail.value }) },
  onOcc(e) { this.setData({ occIdx: e.detail.value }) },
  onType(e) { this.setData({ typeIdx: e.detail.value }) },
  onField(e) {
    const { field } = e.currentTarget.dataset
    this.setData({ [field]: e.detail.value })
  },
  onDate(e) { this.setData({ permanentDeadline: e.detail.value }) },

  async submit() {
    if (!this.data.title) {
      wx.showToast({ title: '请输入标题', icon: 'none' })
      return
    }
    try {
      await http.post('/issues', {
        title: this.data.title,
        description: this.data.description,
        submitterDepartment: this.data.departments[this.data.deptIdx]?.name || '',
        occasionId: this.data.occasions[this.data.occIdx]?.id || null,
        issueType: this.data.types[this.data.typeIdx]?.name || '',
        rootCause: this.data.rootCause,
        permanentSolution: this.data.permanentSolution,
        permanentDeadline: this.data.permanentDeadline || null
      })
      wx.showToast({ title: '提交成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 1000)
    } catch (e) {}
  }
})
