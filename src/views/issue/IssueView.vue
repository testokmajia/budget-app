<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, watch, nextTick, inject } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Download, ArrowDown, Edit, Share, UserFilled, DocumentAdd, Select, CircleCheck, CircleClose, CloseBold, Switch, MoreFilled, RefreshLeft } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getList, getById, create, assign, submitSolution, reviewByLeader, reviewByAdmin, confirm, reject, closeIssue, updateIssue, exportIssues, submitChangeProposal, getPendingProposals, reviewProposal, undoIssue, uploadAttachments, getAttachments, deleteAttachment, getSystemAssignments, feedbackToSubmitter } from '@/api/issue'
import { getUsers, getCategories, getDepartments, getTeams, getOccasions, getSystems } from '@/api/admin'

const userStore = useUserStore()
const route = useRoute()
const refreshBadges = inject('refreshBadges', null)

// === Table state ===
const loading = ref(false)
const tableData = ref([])
const totalElements = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const sortBy = ref('createdAt')
const sortDir = ref('desc')

// === Filter state ===
const filters = reactive({
  statuses: [],
  submitterIds: [],
  submitterDepartments: [],
  occasionId: null,
  issueType: '',
  responsibleTeam: '',
  responsiblePersonId: null,
  dateFrom: '',
  dateTo: '',
})

const statusOptions = [
  { label: '待分派', value: '待分派' },
  { label: '待员工处理', value: '待员工处理' },
  { label: '待组长审核', value: '待组长审核' },
  { label: '待管理员审核', value: '待管理员审核' },
  { label: '解决中', value: '解决中' },
  { label: '待确认', value: '待确认' },
  { label: '已完成', value: '已完成' },
  { label: '已驳回', value: '已驳回' },
  { label: '已关闭', value: '已关闭' },
]

// === Reference data ===
const allUsers = ref([])
const categories = ref([])
const departments = ref([])
const teams = ref([])
const occasions = ref([])
const systems = ref([])

// === Create dialog ===
const createVisible = ref(false)
const createFormRef = ref(null)
const createForm = reactive({
  submitterDepartment: '',
  submitterId: null,
  occasionId: null,
  meetingDepartment: '',
  meetingDate: '',
  title: '',
  description: '',
  issueType: '',
})
const createRules = {
  title: [{ required: true, message: '请输入问题标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入问题详情', trigger: 'blur' }],
  submitterDepartment: [{ required: true, message: '请选择提出部门', trigger: 'change' }],
  submitterId: [{ required: true, message: '请选择提出人', trigger: 'change' }],
}

const selectedOccasion = computed(() => {
  if (!createForm.occasionId) return null
  return occasions.value.find(o => o.id === createForm.occasionId)
})
const showMeetingFields = computed(() => {
  return selectedOccasion.value && selectedOccasion.value.name === '业务协调会'
})
// 需求6: 提出人联动 - 根据部门过滤用户
const createFormSubmitterOptions = computed(() => {
  if (!createForm.submitterDepartment) return allUsers.value
  return allUsers.value.filter(u => u.department === createForm.submitterDepartment)
})
const editFormSubmitterOptions = computed(() => {
  if (!editForm.submitterDepartment) return allUsers.value
  return allUsers.value.filter(u => u.department === editForm.submitterDepartment)
})

// === Assign dialog ===
const assignVisible = ref(false)
const assignId = ref(null)
const assignFormRef = ref(null)
const assignForm = reactive({ responsibleTeam: '', responsiblePersonId: null, systems: [] })
const assignRules = {
  responsibleTeam: [{ required: true, message: '请选择责任团队', trigger: 'change' }],
  responsiblePersonId: [{ required: true, message: '请选择责任人', trigger: 'change' }],
}
const selectedTeam = computed(() => {
  if (!assignForm.responsibleTeam) return null
  return teams.value.find(t => t.name === assignForm.responsibleTeam)
})
const teamMembers = computed(() => {
  if (!selectedTeam.value || !selectedTeam.value.members) {
    if (selectedTeam.value && selectedTeam.value.leader) {
      return allUsers.value.filter(u => u.name === selectedTeam.value.leader)
    }
    return []
  }
  const names = new Set(selectedTeam.value.members.split(',').map(s => s.trim()).filter(Boolean))
  if (selectedTeam.value.leader) names.add(selectedTeam.value.leader)
  return allUsers.value.filter(u => names.has(u.name))
})
const teamSystems = computed(() => {
  if (!assignForm.responsibleTeam) return []
  return systems.value.filter(s => s.team === assignForm.responsibleTeam && s.enabled)
})
const editTeamSystems = computed(() => {
  if (!editForm.responsibleTeam) return []
  return systems.value.filter(s => s.team === editForm.responsibleTeam && s.enabled)
})
// 需求8: 系统多选时，自动选第一个系统的负责人
watch(() => assignForm.systems, (sysNames) => {
  if (!sysNames || sysNames.length === 0) return
  const firstSys = sysNames[0]
  const sys = systems.value.find(s => s.name === firstSys)
  if (sys && sys.leader) {
    const owner = allUsers.value.find(u => u.name === sys.leader)
    if (owner) assignForm.responsiblePersonId = owner.id
  }
})

// === Solution dialog ===
const solutionVisible = ref(false)
const solutionId = ref(null)
const solutionFormRef = ref(null)
const solutionForm = reactive({
  temporarySolution: '',
  temporaryDeadline: '',
  rootCause: '',
  permanentSolution: '',
  permanentDeadline: '',
  permanentLongTerm: false,
})
const solutionIssue = ref(null)
const solutionRules = {
  rootCause: [{ required: true, message: '请输入产生原因', trigger: 'blur' }],
  permanentSolution: [{ required: true, message: '请输入永久解决方案', trigger: 'blur' }],
  permanentDeadline: [{ required: true, message: '请选择永久解决时限', trigger: 'change' }],
}

// === Reject dialog ===
const rejectVisible = ref(false)
const rejectId = ref(null)
const rejectFormRef = ref(null)
const rejectForm = reactive({ reason: '' })
const rejectRules = { reason: [{ required: true, message: '请输入驳回原因', trigger: 'blur' }] }

// === Close dialog ===
const closeVisible = ref(false)
const closeId = ref(null)
const closeFormRef = ref(null)
const closeForm = reactive({ reason: '' })

// === Admin edit dialog ===
const editVisible = ref(false)
const editId = ref(null)
const editFormRef = ref(null)
const editForm = reactive({
  title: '',
  description: '',
  submitterDepartment: '',
  occasionId: null,
  meetingDepartment: '',
  meetingDate: '',
  issueType: '',
  responsibleTeam: '',
  responsiblePersonId: null,
  temporarySolution: '',
  temporaryDeadline: '',
  rootCause: '',
  permanentSolution: '',
  permanentDeadline: '',
  permanentLongTerm: false,
  status: '',
  system: '',
  submitterId: null,
})
const editRules = {
  title: [{ required: true, message: '请输入问题标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入问题详情', trigger: 'blur' }],
  submitterDepartment: [{ required: true, message: '请选择提出部门', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  submitterId: [{ required: true, message: '请选择提出人', trigger: 'change' }],
  occasionId: [{ required: true, message: '请选择提出场合', trigger: 'change' }],
  issueType: [{ required: true, message: '请选择问题类型', trigger: 'change' }],
}

// === Workflow review dialog (leader/admin) ===
const workflowReviewVisible = ref(false)
const workflowReviewId = ref(null)
const workflowReviewType = ref('leader') // 'leader' or 'admin'
const workflowReviewComment = ref('')
const workflowReviewing = ref(false)
const workflowReviewRow = ref(null)

// === Change proposal ===
const changeProposalVisible = ref(false)
const changeProposalId = ref(null)
const changeProposalFormRef = ref(null)
const changeProposalForm = reactive({
  temporarySolution: '',
  temporaryDeadline: '',
  rootCause: '',
  permanentSolution: '',
  permanentDeadline: '',
  permanentLongTerm: false,
})
const changeProposalRules = {
  temporarySolution: [{ required: true, message: '请输入临时整改方案', trigger: 'blur' }],
  temporaryDeadline: [{ required: true, message: '请选择临时整改时限', trigger: 'change' }],
}
const submittingProposal = ref(false)

// === Pending approvals (matched to issue rows) ===
const pendingProposals = ref([])

// === Review proposal ===
const reviewVisible = ref(false)
const reviewingProposal = ref(null)
const reviewComment = ref('')
const reviewing = ref(false)

// === Shared action dropdown (single instance, no per-row Popper) ===
const dropdownVisible = ref(false)
const dropdownRow = ref(null)
const dropdownPos = ref({ top: 0, left: 0 })
function toggleDropdown(row, event) {
  if (dropdownRow.value === row && dropdownVisible.value) {
    dropdownVisible.value = false
    return
  }
  dropdownRow.value = row
  const rect = event.currentTarget.getBoundingClientRect()
  dropdownPos.value = { top: rect.bottom + 4, left: rect.left }
  nextTick(() => { dropdownVisible.value = true })
}
function closeDropdown() {
  dropdownVisible.value = false
}
function onDocClick(e) {
  if (!e.target.closest('.action-dropdown-trigger') && !e.target.closest('.action-dropdown-menu')) {
    closeDropdown()
  }
}

// === Detail drawer ===
const detailVisible = ref(false)
const detail = ref(null)

// === Inline edit solution in detail ===
const editingSolution = ref(false)
const editSolutionForm = reactive({
  temporarySolution: '',
  temporaryDeadline: '',
  rootCause: '',
  permanentSolution: '',
  permanentDeadline: '',
  permanentLongTerm: false,
})
const savingSolution = ref(false)

function canEditSolution(row) {
  if (!isResponsiblePerson(row)) return false
  if (!isItEmployee.value) return false
  if (row.status === '待确认') return false
  if (row.status === '待分派') return false
  return true
}
function startEditSolution() {
  const d = detail.value
  if (d.status === '待员工处理' || d.status === '已驳回') {
    editSolutionForm.temporarySolution = d.temporarySolution || ''
    editSolutionForm.temporaryDeadline = d.temporaryDeadline || ''
    editSolutionForm.rootCause = d.rootCause || ''
    editSolutionForm.permanentSolution = d.permanentSolution || ''
    editSolutionForm.permanentDeadline = d.permanentDeadline && d.permanentDeadline !== '2099-12-31' ? d.permanentDeadline : ''
    editSolutionForm.permanentLongTerm = d.permanentDeadline === '2099-12-31'
    editingSolution.value = true
    return
  }
  // Later statuses → change proposal
  changeProposalId.value = d.id
  changeProposalForm.temporarySolution = d.temporarySolution || ''
  changeProposalForm.temporaryDeadline = d.temporaryDeadline || ''
  changeProposalForm.rootCause = d.rootCause || ''
  changeProposalForm.permanentSolution = d.permanentSolution || ''
  changeProposalForm.permanentDeadline = d.permanentDeadline && d.permanentDeadline !== '2099-12-31' ? d.permanentDeadline : ''
  changeProposalForm.permanentLongTerm = d.permanentDeadline === '2099-12-31'
  changeProposalVisible.value = true
}
function cancelEditSolution() {
  editingSolution.value = false
}
async function saveEditSolution() {
  if (!editSolutionForm.temporarySolution) {
    ElMessage.warning('请输入临时整改方案')
    return
  }
  if (!editSolutionForm.temporaryDeadline) {
    ElMessage.warning('请选择临时整改时限')
    return
  }
  savingSolution.value = true
  try {
    const data = { ...editSolutionForm, permanentDeadline: editSolutionForm.permanentLongTerm ? '2099-12-31' : editSolutionForm.permanentDeadline }
    delete data.permanentLongTerm
    await submitSolution(detail.value.id, data)
    ElMessage.success('方案已提交，等待组长审核')
    editingSolution.value = false
    const res = await getById(detail.value.id)
    detail.value = res.data
  } finally {
    savingSolution.value = false
  }
}

// === Change proposal handlers ===
async function handleSubmitChangeProposal() {
  const valid = await changeProposalFormRef.value.validate().catch(() => null)
  if (!valid) return
  submittingProposal.value = true
  try {
    const data = { ...changeProposalForm, permanentDeadline: changeProposalForm.permanentLongTerm ? '2099-12-31' : changeProposalForm.permanentDeadline }
    delete data.permanentLongTerm
    await submitChangeProposal(changeProposalId.value, data)
    ElMessage.success('变更申请已提交，请等待审批')
    changeProposalVisible.value = false
  } finally {
    submittingProposal.value = false
  }
}
async function loadPendingProposals() {
  try {
    const res = await getPendingProposals()
    pendingProposals.value = res.data || []
  } catch { pendingProposals.value = [] }
}
function openProposalReview(p) {
  reviewingProposal.value = p
  reviewComment.value = ''
  reviewVisible.value = true
}
async function handleReview(approved) {
  reviewing.value = true
  try {
    await reviewProposal(reviewingProposal.value.id, { approved, comment: reviewComment.value })
    ElMessage.success(approved ? '已批准' : '已拒绝')
    reviewVisible.value = false
    loadPendingProposals()
    fetchData()
  } finally {
    reviewing.value = false
  }
}
function hasFieldChanged(oldVal, newVal) {
  const a = oldVal || ''
  const b = newVal || ''
  return a !== b
}

// === Permissions ===
const isAdmin = computed(() => userStore.hasRole('ROLE_ADMIN'))
const isIssueAdmin = computed(() => userStore.hasRole('ROLE_ISSUE_ADMIN'))
const canManage = computed(() => isAdmin.value || isIssueAdmin.value)
const isItEmployee = computed(() => userStore.user?.department === '信息科技部')
const isTeamLeader = computed(() => {
  if (!isItEmployee.value) return false
  return teams.value.some(t => t.leader === userStore.user?.name)
})
const ledTeamMembers = computed(() => {
  if (!isTeamLeader.value) return []
  const memberNames = new Set()
  teams.value.filter(t => t.leader === userStore.user?.name).forEach(t => {
    if (t.members) t.members.split(',').map(s => s.trim()).filter(Boolean).forEach(n => memberNames.add(n))
  })
  return allUsers.value.filter(u => memberNames.has(u.name))
})
const hasActiveFilters = computed(() => {
  return filters.submitterIds.length > 0 ||
    filters.submitterDepartments.length > 0 ||
    filters.occasionId ||
    filters.issueType ||
    filters.responsibleTeam ||
    filters.responsiblePersonId ||
    filters.dateFrom ||
    filters.dateTo
})

// === Data fetching ===
async function fetchData() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      sortBy: sortBy.value,
      sortDir: sortDir.value,
    }
    if (filters.statuses.length) params.statuses = filters.statuses.join(',')
    if (filters.submitterIds.length) params.submitterIds = filters.submitterIds.join(',')
    if (filters.submitterDepartments.length) params.submitterDepartments = filters.submitterDepartments.join(',')
    if (filters.occasionId) params.occasionId = filters.occasionId
    if (filters.issueType) params.issueType = filters.issueType
    if (filters.responsibleTeam) params.responsibleTeam = filters.responsibleTeam
    if (filters.responsiblePersonId) params.responsiblePersonId = filters.responsiblePersonId
    if (filters.dateFrom) params.dateFrom = filters.dateFrom
    if (filters.dateTo) params.dateTo = filters.dateTo
    params.myScope = !hasActiveFilters.value
    const res = await getList(params)
    tableData.value = res.data?.content || []
    // pre-compute row actions
    tableData.value.forEach(row => {
      const actions = getRowActions(row)
      row._primary = actions.length > 0 ? actions[0] : null
      row._secondary = actions.length > 1 ? actions.slice(1) : []
    })
    totalElements.value = res.data?.totalElements || 0
    // match pending proposals to rows for approval actions
    await loadPendingProposals()
    const proposalMap = {}
    pendingProposals.value.forEach(p => { proposalMap[p.issueId] = p })
    tableData.value.forEach(row => {
      row._pendingProposal = proposalMap[row.id] || null
    })
  } finally {
    loading.value = false
  }
}

async function loadRefData() {
  try {
    const [uRes, cRes, dRes, tRes, oRes, sRes] = await Promise.all([
      getUsers({ size: 9999, enabled: true }).catch(() => ({})),
      getCategories().catch(() => ({})),
      getDepartments().catch(() => ({})),
      getTeams().catch(() => ({})),
      getOccasions().catch(() => ({})),
      getSystems().catch(() => ({})),
    ])
    allUsers.value = uRes.data?.content || []
    categories.value = cRes.data || []
    departments.value = dRes.data || []
    teams.value = tRes.data || []
    occasions.value = oRes.data || []
    systems.value = sRes.data || []
  } catch { /* ignore */ }
}

// === Filter handlers ===
function handleFilter() {
  currentPage.value = 1
  fetchData()
}
const defaultStatuses = computed(() => statusOptions.filter(s => s.value !== '已完成' && s.value !== '已关闭').map(s => s.value))

function handleResetFilters() {
  Object.assign(filters, {
    statuses: [...defaultStatuses.value], submitterIds: [], submitterDepartments: [],
    occasionId: null, issueType: '', responsibleTeam: '',
    responsiblePersonId: null, dateFrom: '', dateTo: '',
  })
  currentPage.value = 1
  fetchData()
}
function handleSortChange({ prop, order }) {
  sortBy.value = prop || 'createdAt'
  sortDir.value = order === 'ascending' ? 'asc' : 'desc'
  fetchData()
}
function handleSizeChange() {
  currentPage.value = 1
  fetchData()
}

// === Create ===
const uploadFiles = ref([])
function handleCreate() {
  createForm.submitterDepartment = userStore.user?.department || ''
  createForm.submitterId = userStore.user?.id || null
  createForm.occasionId = null
  createForm.meetingDepartment = ''
  createForm.meetingDate = ''
  createForm.title = ''
  createForm.description = ''
  createForm.issueType = ''
  uploadFiles.value = []
  createVisible.value = true
}
async function handleCreateSubmit() {
  const valid = await createFormRef.value.validate().catch(() => null)
  if (!valid) return
  const data = { ...createForm }
  if (!showMeetingFields.value) {
    data.meetingDepartment = ''
    data.meetingDate = ''
  }
  const res = await create(data)
  ElMessage.success('问题已提交')
  createVisible.value = false
  // 上传附件
  const issueId = res.data.id
  if (uploadFiles.value.length > 0) {
    try { await uploadAttachments(issueId, uploadFiles.value) } catch { /* ignore */ }
  }
  fetchData()
}

// === Assign ===
function handleAssign(row) {
  assignId.value = row.id
  assignForm.responsibleTeam = row.responsibleTeam || ''
  assignForm.responsiblePersonId = row.responsiblePersonId || null
  assignForm.systems = row.system ? row.system.split(',').map(s => s.trim()).filter(Boolean) : []
  assignVisible.value = true
}
async function handleAssignSubmit() {
  const valid = await assignFormRef.value.validate().catch(() => null)
  if (!valid) return
  await assign(assignId.value, { ...assignForm })
  ElMessage.success('已分派')
  assignVisible.value = false
  refreshBadges?.()
  fetchData()
}

// === Solution ===
function handleSolution(row) {
  solutionId.value = row.id
  solutionIssue.value = row
  solutionForm.temporarySolution = row.temporarySolution || ''
  solutionForm.temporaryDeadline = row.temporaryDeadline || ''
  solutionForm.rootCause = row.rootCause || ''
  solutionForm.permanentSolution = row.permanentSolution || ''
  solutionForm.permanentDeadline = row.permanentDeadline && row.permanentDeadline !== '2099-12-31' ? row.permanentDeadline : ''
  solutionForm.permanentLongTerm = row.permanentDeadline === '2099-12-31'
  solutionVisible.value = true
}
async function handleSolutionSubmit() {
  const valid = await solutionFormRef.value.validate().catch(() => null)
  if (!valid) return
  const data = { ...solutionForm, permanentDeadline: solutionForm.permanentLongTerm ? '2099-12-31' : solutionForm.permanentDeadline }
  delete data.permanentLongTerm
  await submitSolution(solutionId.value, data)
  ElMessage.success('方案已提交，等待组长审核')
  solutionVisible.value = false
  fetchData()
  if (detailVisible.value && detail.value?.id === solutionId.value) {
    const res = await getById(solutionId.value)
    detail.value = res.data
  }
}

// === Workflow review (leader/admin) ===
function openLeaderReview(row) {
  workflowReviewId.value = row.id
  workflowReviewType.value = 'leader'
  workflowReviewComment.value = ''
  workflowReviewRow.value = row
  workflowReviewVisible.value = true
}
function openAdminReview(row) {
  workflowReviewId.value = row.id
  workflowReviewType.value = 'admin'
  workflowReviewComment.value = ''
  workflowReviewRow.value = row
  workflowReviewVisible.value = true
}
async function handleWorkflowReview(approved) {
  workflowReviewing.value = true
  try {
    const api = workflowReviewType.value === 'leader' ? reviewByLeader : reviewByAdmin
    await api(workflowReviewId.value, { approved, comment: workflowReviewComment.value })
    ElMessage.success(approved ? '已通过' : '已退回')
    workflowReviewVisible.value = false
    fetchData()
  } finally {
    workflowReviewing.value = false
  }
}

// === Confirm ===
async function handleConfirm(row, satisfied) {
  const title = satisfied ? '确认完成' : '退回重改'
  if (satisfied) {
    await ElMessageBox.confirm('确定该问题已整改完成吗？', title, { type: 'warning' })
    await confirm(row.id, { satisfied: true, remark: '' })
    ElMessage.success('问题已完成')
  } else {
    const { value } = await ElMessageBox.prompt('请输入退回原因', title, {
      confirmButtonText: '确定退回',
      cancelButtonText: '取消',
      inputType: 'textarea',
    }).catch(() => null)
    if (!value) return
    await confirm(row.id, { satisfied: false, remark: value })
    ElMessage.success('已退回整改')
  }
  fetchData()
}

// === Reject ===
const rejectRow = ref(null)
function handleReject(row) {
  rejectId.value = row.id
  rejectForm.reason = ''
  rejectRow.value = row
  rejectVisible.value = true
}
async function handleRejectSubmit() {
  const valid = await rejectFormRef.value.validate().catch(() => null)
  if (!valid) return
  await reject(rejectId.value, { reason: rejectForm.reason })
  ElMessage.success('已驳回')
  rejectVisible.value = false
  fetchData()
}

// === Close ===
function handleClose(row) {
  closeId.value = row.id
  closeForm.reason = ''
  closeVisible.value = true
}
async function handleCloseSubmit() {
  await closeIssue(closeId.value, { reason: closeForm.reason })
  ElMessage.success('问题已关闭')
  closeVisible.value = false
  fetchData()
}

// === Admin edit ===
function handleEdit(row) {
  editId.value = row.id
  editForm.title = row.title || ''
  editForm.description = row.description || ''
  editForm.submitterDepartment = row.submitterDepartment || ''
  editForm.occasionId = row.occasionId || null
  editForm.meetingDepartment = row.meetingDepartment || ''
  editForm.meetingDate = row.meetingDate || ''
  editForm.issueType = row.issueType || ''
  editForm.responsibleTeam = row.responsibleTeam || ''
  editForm.responsiblePersonId = row.responsiblePersonId || null
  editForm.temporarySolution = row.temporarySolution || ''
  editForm.temporaryDeadline = row.temporaryDeadline || ''
  editForm.rootCause = row.rootCause || ''
  editForm.permanentSolution = row.permanentSolution || ''
  editForm.permanentDeadline = row.permanentDeadline && row.permanentDeadline !== '2099-12-31' ? row.permanentDeadline : ''
  editForm.permanentLongTerm = row.permanentDeadline === '2099-12-31'
  editForm.status = row.status || ''
  editForm.system = row.system || ''
  editForm.submitterId = row.submitterId || null
  editVisible.value = true
}
async function handleEditSubmit() {
  const valid = await editFormRef.value.validate().catch(() => null)
  if (!valid) return
  const data = { ...editForm, permanentDeadline: editForm.permanentLongTerm ? '2099-12-31' : editForm.permanentDeadline }
  delete data.permanentLongTerm
  await updateIssue(editId.value, data)
  ElMessage.success('问题已更新')
  editVisible.value = false
  fetchData()
}

// === Export ===
async function handleExport() {
  const params = {}
  if (filters.statuses.length) params.statuses = filters.statuses.join(',')
  if (filters.submitterIds.length) params.submitterIds = filters.submitterIds.join(',')
  if (filters.submitterDepartments.length) params.submitterDepartments = filters.submitterDepartments.join(',')
  if (filters.occasionId) params.occasionId = filters.occasionId
  if (filters.issueType) params.issueType = filters.issueType
  if (filters.responsibleTeam) params.responsibleTeam = filters.responsibleTeam
  if (filters.responsiblePersonId) params.responsiblePersonId = filters.responsiblePersonId
  if (filters.dateFrom) params.dateFrom = filters.dateFrom
  if (filters.dateTo) params.dateTo = filters.dateTo
  try {
    const res = await exportIssues(params)
    const url = window.URL.createObjectURL(new Blob([res]))
    const a = document.createElement('a')
    a.href = url
    a.download = '科技问题管理.xlsx'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch { ElMessage.warning('导出失败') }
}

// === Detail ===
const detailAttachments = ref([])
const detailSystemAssignments = ref([])
async function handleDetail(row) {
  try {
    const res = await getById(row.id)
    detail.value = res.data
    detailVisible.value = true
    // 加载附件
    getAttachments(row.id).then(r => { detailAttachments.value = r.data || [] }).catch(() => { detailAttachments.value = [] })
    // 加载系统分配状态
    getSystemAssignments(row.id).then(r => { detailSystemAssignments.value = r.data || [] }).catch(() => { detailSystemAssignments.value = [] })
  } catch { /* ignore */ }
}

// === Helpers ===
function getStatusType(status) {
  const map = { '待分派': 'info', '待员工处理': 'warning', '待组长审核': 'warning', '待管理员审核': '', '解决中': 'warning', '待确认': '', '已完成': 'success', '已驳回': 'danger', '已关闭': 'info' }
  return map[status] || ''
}
function formatDateTime(dt) {
  if (!dt) return '-'
  const d = new Date(dt)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
function formatDate(d) { if (!d) return '-'; return d.split('T')[0] }
function formatDeadline(d) { if (!d) return '-'; if (d === '2099-12-31' || String(d).includes('2099-12-31')) return '长期工作'; return String(d).split('T')[0] }
function isResponsiblePerson(row) { return row.responsiblePersonId === userStore.user?.id }
function isSubmitter(row) { return row.submitterId === userStore.user?.id }
function isTeamLeaderForIssue(row) {
  if (!isTeamLeader.value) return false
  if (row.status !== '待组长审核') return false
  return isResponsiblePerson(row) || ledTeamMembers.value.some(m => m.id === row.responsiblePersonId)
}

async function handleUndo(row) {
  try {
    await ElMessageBox.confirm('确定要撤回上一操作吗？', '撤回确认', { type: 'warning' })
    await undoIssue(row.id)
    ElMessage.success('已撤回')
    fetchData()
  } catch { /* canceled */ }
}

async function handleFeedbackToSubmitter(row) {
  try {
    await ElMessageBox.confirm('确认所有负责人已完成？将反馈给问题提出人。', '确认反馈', { type: 'warning' })
    await feedbackToSubmitter(row.id)
    ElMessage.success('已反馈给提出人')
    refreshBadges?.()
    fetchData()
    if (detailVisible.value && detail.value?.id === row.id) {
      const res = await getById(row.id)
      detail.value = res.data
    }
  } catch { /* canceled */ }
}

function getRowActions(row) {
  const actions = []
  // undo for last operator
  if (row.lastOperatorId && row.lastOperatorId === userStore.user?.id
      && !['已完成', '已关闭'].includes(row.status)) {
    actions.push({ label: '撤回', icon: RefreshLeft, type: 'warning', handler: () => handleUndo(row) })
  }
  // change proposal approval
  if (row._pendingProposal) {
    actions.push({ label: '审批变更', icon: Select, type: 'warning', handler: () => openProposalReview(row._pendingProposal) })
  }
  // workflow actions
  if (canManage.value && row.status === '待分派') {
    actions.push({ label: '分派', icon: Share, type: 'primary', handler: () => handleAssign(row) })
  }
  if (isItEmployee.value && isResponsiblePerson(row) && (row.status === '待员工处理' || row.status === '已驳回')) {
    actions.push({ label: '提交方案', icon: Edit, type: 'primary', handler: () => handleSolution(row) })
  }
  if (isTeamLeaderForIssue(row)) {
    actions.push({ label: '审核', icon: Select, type: 'primary', handler: () => openLeaderReview(row) })
  }
  if (canManage.value && row.status === '待管理员审核') {
    actions.push({ label: '审核', icon: Select, type: 'primary', handler: () => openAdminReview(row) })
  }
  if (canManage.value && (row.status === '解决中' || row.status === '待确认')) {
    actions.push({ label: '反馈给提出人', icon: CircleCheck, type: 'success', handler: () => handleFeedbackToSubmitter(row) })
  }
  if (row.status === '待确认' && isSubmitter(row)) {
    actions.push({ label: '确认', icon: CircleCheck, type: 'success', handler: () => handleConfirm(row, true) })
    actions.push({ label: '退回', icon: CircleClose, type: 'warning', handler: () => handleConfirm(row, false) })
  }
  // general
  if (canManage.value && row.status !== '待确认') {
    actions.push({ label: '编辑', icon: Edit, type: '', handler: () => handleEdit(row) })
  }
  // destructive
  if (canManage.value && !['已完成','已关闭','已驳回'].includes(row.status)) {
    actions.push({ label: '驳回', icon: CloseBold, type: 'danger', handler: () => handleReject(row) })
  }
  if (canManage.value && row.status !== '已关闭') {
    actions.push({ label: '关闭', icon: Switch, type: 'danger', handler: () => handleClose(row) })
  }
  return actions
}

watch(() => createForm.occasionId, () => {
  if (!showMeetingFields.value) {
    createForm.meetingDepartment = ''
    createForm.meetingDate = ''
  }
})
// 需求6: 部门变更时清空提出人（同步执行，确保 handleCreate 先清再赋值）
watch(() => createForm.submitterDepartment, () => {
  createForm.submitterId = null
}, { flush: 'sync' })
watch(() => editForm.submitterDepartment, () => {
  editForm.submitterId = null
}, { flush: 'sync' })

onMounted(() => {
  document.addEventListener('click', onDocClick)
  if (route.query.statuses) {
    filters.statuses = route.query.statuses.split(',')
  } else {
    filters.statuses = [...defaultStatuses.value]
  }
  fetchData()
  loadRefData()
})
onUnmounted(() => {
  document.removeEventListener('click', onDocClick)
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div style="display: flex; gap: 8px">
        <el-button :icon="Download" @click="handleExport">导出</el-button>
        <el-button type="primary" :icon="Plus" @click="handleCreate">提交问题</el-button>
      </div>
    </div>

    <!-- Filter bar -->
    <div class="search-bar">
      <el-select v-model="filters.statuses" placeholder="问题状态" clearable multiple collapse-tags style="width: 180px" @change="handleFilter">
        <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-select v-model="filters.submitterIds" placeholder="提出人" clearable multiple collapse-tags filterable style="width: 180px" @change="handleFilter">
        <el-option v-for="u in allUsers" :key="u.id" :label="u.name" :value="u.id" />
      </el-select>
      <el-select v-model="filters.submitterDepartments" placeholder="提出部门" clearable multiple collapse-tags style="width: 180px" @change="handleFilter">
        <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.name" />
      </el-select>
      <el-select v-model="filters.occasionId" placeholder="提出场合" clearable style="width: 200px" @change="handleFilter">
        <el-option v-for="o in occasions" :key="o.id" :label="o.name" :value="o.id" />
      </el-select>
      <el-select v-model="filters.issueType" placeholder="问题类型" clearable style="width: 130px" @change="handleFilter">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.name" :disabled="!c.enabled" />
      </el-select>
      <el-select v-model="filters.responsibleTeam" placeholder="责任团队" clearable style="width: 140px" @change="handleFilter">
        <el-option v-for="t in teams" :key="t.id" :label="t.name" :value="t.name" />
      </el-select>
      <el-select v-model="filters.responsiblePersonId" placeholder="责任人" clearable filterable style="width: 130px" @change="handleFilter">
        <el-option v-for="u in allUsers" :key="u.id" :label="u.name" :value="u.id" />
      </el-select>
      <el-date-picker v-model="filters.dateFrom" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" style="width: 140px" @change="handleFilter" />
      <el-date-picker v-model="filters.dateTo" type="date" placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 140px" @change="handleFilter" />
      <el-button @click="handleResetFilters">重置</el-button>
    </div>

    <!-- Table -->
    <div class="table-wrap">
    <el-table :data="tableData" v-loading="loading" stripe border @sort-change="handleSortChange"
              :default-sort="{ prop: 'createdAt', order: 'descending' }">
      <el-table-column prop="issueCode" label="问题编号" min-width="153" sortable="custom" />
      <el-table-column prop="title" label="标题" min-width="290" show-overflow-tooltip sortable="custom">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleDetail(row)">{{ row.title }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="submitterDepartment" label="提出部门" min-width="132" sortable="custom" />
      <el-table-column prop="submitterName" label="提出人" min-width="110" sortable="custom" />
      <el-table-column prop="occasionName" label="提出场合" min-width="130" show-overflow-tooltip />
      <el-table-column prop="responsibleTeam" label="责任团队" min-width="110" show-overflow-tooltip />
      <el-table-column prop="responsiblePersonName" label="责任人" min-width="90">
        <template #default="{ row }">
          <span v-if="row.responsiblePersonName">{{ row.responsiblePersonName }}</span>
          <span v-else style="color: #c0c4cc">未分配</span>
        </template>
      </el-table-column>
      <el-table-column prop="permanentDeadline" label="永久时限" min-width="110" sortable="custom">
        <template #default="{ row }">{{ formatDeadline(row.permanentDeadline) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" min-width="90" sortable="custom">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="提出日期" min-width="120" sortable="custom">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <div class="action-btns">
            <el-button v-if="row._primary" :type="row._primary.type" size="small" @click="row._primary.handler">
              {{ row._primary.label }}
            </el-button>
            <el-button v-if="row._secondary.length" size="small" class="action-dropdown-trigger" @click="toggleDropdown(row, $event)">
              更多<el-icon style="margin-left:2px;font-size:12px"><ArrowDown /></el-icon>
            </el-button>
            <span v-if="!row._primary && !row._secondary.length" style="color: #c0c4cc">-</span>
          </div>
        </template>
      </el-table-column>
    </el-table>
    </div>

    <!-- Pagination -->
    <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="totalElements"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="fetchData"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- Create dialog -->
    <el-dialog v-model="createVisible" title="提交问题" width="600px" @keyup.enter="handleCreateSubmit">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="110px">
        <el-form-item label="提出部门" prop="submitterDepartment">
          <el-select v-model="createForm.submitterDepartment" style="width: 100%" clearable filterable placeholder="请选择部门">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.name" :disabled="!d.enabled" />
          </el-select>
        </el-form-item>
        <el-form-item label="提出人" prop="submitterId">
          <el-select v-model="createForm.submitterId" style="width: 100%" clearable filterable placeholder="请选择提出人">
            <el-option v-for="u in createFormSubmitterOptions" :key="u.id" :label="`${u.name} (${u.department || '-'})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="提出场合">
          <el-select v-model="createForm.occasionId" style="width: 100%" clearable placeholder="请选择提出场合">
            <el-option-group label="会议">
              <el-option v-for="o in occasions.filter(x => x.type === 'MEETING' && x.enabled)" :key="o.id" :label="o.name" :value="o.id" />
            </el-option-group>
            <el-option-group label="通用">
              <el-option v-for="o in occasions.filter(x => x.type === 'GENERAL' && x.enabled)" :key="o.id" :label="o.name" :value="o.id" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <template v-if="showMeetingFields">
          <el-form-item label="会议参与部门">
            <el-input v-model="createForm.meetingDepartment" placeholder="参与部门" />
          </el-form-item>
          <el-form-item label="会议日期">
            <el-date-picker v-model="createForm.meetingDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
          </el-form-item>
        </template>
        <el-form-item label="问题标题" prop="title">
          <el-input v-model="createForm.title" maxlength="20" show-word-limit placeholder="不超过20个字" />
        </el-form-item>
        <el-form-item label="问题详情" prop="description">
          <el-input v-model="createForm.description" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="不超过1000个字" />
        </el-form-item>
        <el-form-item label="问题类型">
          <el-select v-model="createForm.issueType" style="width: 100%" clearable placeholder="请选择问题类型">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.name" :disabled="!c.enabled" />
          </el-select>
        </el-form-item>
        <el-form-item label="附件">
          <el-upload
            v-model:file-list="uploadFiles"
            :auto-upload="false"
            multiple
            :limit="10"
            :before-upload="(file) => { if (file.size > 20*1024*1024) { ElMessage.warning('文件 ' + file.name + ' 超过20MB限制'); return false } return true }"
          >
            <el-button type="primary" plain>选择文件</el-button>
            <template #tip>
              <div style="font-size: 12px; color: #909399; margin-top: 6px">支持图片、PDF、Word、Excel，单文件≤20MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateSubmit">提交</el-button>
      </template>
    </el-dialog>

    <!-- Assign dialog -->
    <el-dialog v-model="assignVisible" title="分配团队、系统和责任人" width="480px">
      <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="100px">
        <el-form-item label="责任团队" prop="responsibleTeam">
          <el-select v-model="assignForm.responsibleTeam" style="width: 100%" placeholder="请选择责任团队" @change="assignForm.systems = []; assignForm.responsiblePersonId = null">
            <el-option v-for="t in teams" :key="t.id" :label="t.name" :value="t.name" :disabled="!t.enabled" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任人" prop="responsiblePersonId">
          <el-select v-model="assignForm.responsiblePersonId" style="width: 100%" placeholder="请选择责任人" clearable filterable>
            <el-option v-for="u in teamMembers" :key="u.id" :label="`${u.name} (${u.department || '-'})`" :value="u.id" />
          </el-select>
          <span v-if="assignForm.responsibleTeam && teamMembers.length === 0" style="color: #f56c6c; font-size: 12px;">
            该团队暂无成员
          </span>
        </el-form-item>
        <el-form-item label="涉及系统" v-if="assignForm.responsibleTeam">
          <el-select v-model="assignForm.systems" style="width: 100%" placeholder="请选择涉及系统" clearable filterable multiple collapse-tags>
            <el-option v-for="s in systems" :key="s.id" :label="s.name" :value="s.name" />
          </el-select>
          <span v-if="systems.length === 0" style="color: #909399; font-size: 12px;">
            该团队未配置信息系统，请前往「系统管理 → 信息系统」配置
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Solution dialog -->
    <el-dialog v-model="solutionVisible" title="整改方案" width="750px">
      <!-- 问题详情 -->
      <div v-if="solutionIssue" class="solution-issue-info">
        <div class="si-row"><span class="si-label">编号：</span>{{ solutionIssue.issueCode }}</div>
        <div class="si-row"><span class="si-label">标题：</span>{{ solutionIssue.title }}</div>
        <div class="si-row"><span class="si-label">描述：</span>{{ solutionIssue.description }}</div>
        <div class="si-row"><span class="si-label">提出人：</span>{{ solutionIssue.submitterName }} / {{ solutionIssue.submitterDepartment }}</div>
        <div class="si-row"><span class="si-label">状态：</span><el-tag size="small">{{ solutionIssue.status }}</el-tag></div>
        <div class="si-row"><span class="si-label">涉及系统：</span>{{ solutionIssue.system || '未指定' }}</div>
        <div class="si-row"><span class="si-label">系统负责人：</span>{{ solutionIssue.responsiblePersonName || '未分配' }}</div>
      </div>
      <el-form ref="solutionFormRef" :model="solutionForm" :rules="solutionRules" label-width="110px">
        <el-form-item label="产生原因" prop="rootCause" label-position="top">
          <el-input v-model="solutionForm.rootCause" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <div style="display: flex; gap: 20px; margin-bottom: 18px;">
          <div style="flex: 1; min-width: 0;">
            <el-form-item label="临时整改方案" label-position="top">
              <el-input v-model="solutionForm.temporarySolution" type="textarea" :rows="8" maxlength="2000" show-word-limit />
            </el-form-item>
            <el-form-item label="临时整改时限" label-position="top">
              <el-date-picker v-model="solutionForm.temporaryDeadline" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </div>
          <div style="flex: 1; min-width: 0;">
            <el-form-item label="永久解决方案" prop="permanentSolution" label-position="top">
              <el-input v-model="solutionForm.permanentSolution" type="textarea" :rows="8" maxlength="1000" show-word-limit />
            </el-form-item>
            <el-form-item label="永久解决时限" prop="permanentDeadline" label-position="top">
              <div style="display: flex; align-items: center; gap: 8px">
                <el-date-picker v-model="solutionForm.permanentDeadline" type="date" placeholder="选择日期" style="flex: 1" value-format="YYYY-MM-DD" :disabled="solutionForm.permanentLongTerm" />
                <el-checkbox v-model="solutionForm.permanentLongTerm">长期工作</el-checkbox>
              </div>
            </el-form-item>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="solutionVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSolutionSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Reject dialog -->
    <el-dialog v-model="rejectVisible" title="驳回问题" width="650px">
      <template v-if="rejectRow">
        <div class="review-issue-info">
          <div class="review-issue-header">
            <span class="review-issue-code">{{ rejectRow.issueCode }}</span>
            <el-tag :type="getStatusType(rejectRow.status)" size="small">{{ rejectRow.status }}</el-tag>
          </div>
          <h4 class="review-issue-title">{{ rejectRow.title }}</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="提出人">{{ rejectRow.submitterName }}</el-descriptions-item>
            <el-descriptions-item label="提出部门">{{ rejectRow.submitterDepartment }}</el-descriptions-item>
            <el-descriptions-item label="责任团队">{{ rejectRow.responsibleTeam || '未分配' }}</el-descriptions-item>
            <el-descriptions-item label="责任人">{{ rejectRow.responsiblePersonName || '未分配' }}</el-descriptions-item>
            <el-descriptions-item label="涉及系统">{{ rejectRow.system || '未指定' }}</el-descriptions-item>
            <el-descriptions-item label="问题类型">{{ rejectRow.issueType || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="rejectRow.description" label="问题详情" :span="2">
              <div class="long-text">{{ rejectRow.description }}</div>
            </el-descriptions-item>
            <el-descriptions-item v-if="rejectRow.temporarySolution" label="临时整改方案" :span="2">
              <div class="long-text">{{ rejectRow.temporarySolution }}</div>
            </el-descriptions-item>
            <el-descriptions-item v-if="rejectRow.permanentSolution" label="永久解决方案" :span="2">
              <div class="long-text">{{ rejectRow.permanentSolution }}</div>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <el-divider />
      </template>
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="100px">
        <el-form-item label="驳回原因" prop="reason">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="handleRejectSubmit">驳回</el-button>
      </template>
    </el-dialog>

    <!-- Close dialog -->
    <el-dialog v-model="closeVisible" title="关闭问题" width="480px">
      <el-form ref="closeFormRef" :model="closeForm" label-width="100px">
        <el-form-item label="关闭原因">
          <el-input v-model="closeForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="可选填写关闭原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeVisible = false">取消</el-button>
        <el-button type="danger" @click="handleCloseSubmit">确认关闭</el-button>
      </template>
    </el-dialog>

    <!-- Workflow review dialog -->
    <el-dialog v-model="workflowReviewVisible" :title="workflowReviewType === 'leader' ? '负责人审核' : '管理员审核'" width="700px">
      <template v-if="workflowReviewRow">
        <div class="review-issue-info">
          <div class="review-issue-header">
            <span class="review-issue-code">{{ workflowReviewRow.issueCode }}</span>
            <el-tag :type="getStatusType(workflowReviewRow.status)" size="small">{{ workflowReviewRow.status }}</el-tag>
          </div>
          <h4 class="review-issue-title">{{ workflowReviewRow.title }}</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="提出人">{{ workflowReviewRow.submitterName }}</el-descriptions-item>
            <el-descriptions-item label="提出部门">{{ workflowReviewRow.submitterDepartment }}</el-descriptions-item>
            <el-descriptions-item label="责任团队">{{ workflowReviewRow.responsibleTeam || '未分配' }}</el-descriptions-item>
            <el-descriptions-item label="责任人">{{ workflowReviewRow.responsiblePersonName || '未分配' }}</el-descriptions-item>
            <el-descriptions-item label="涉及系统">{{ workflowReviewRow.system || '未指定' }}</el-descriptions-item>
            <el-descriptions-item label="问题类型">{{ workflowReviewRow.issueType || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="workflowReviewRow.description" label="问题详情" :span="2">
              <div class="long-text">{{ workflowReviewRow.description }}</div>
            </el-descriptions-item>
            <el-descriptions-item v-if="workflowReviewRow.temporarySolution" label="临时整改方案" :span="2">
              <div class="long-text">{{ workflowReviewRow.temporarySolution }}</div>
            </el-descriptions-item>
            <el-descriptions-item v-if="workflowReviewRow.permanentSolution" label="永久解决方案" :span="2">
              <div class="long-text">{{ workflowReviewRow.permanentSolution }}</div>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <el-divider />
      </template>
      <el-form label-width="80px">
        <el-form-item label="审核意见">
          <el-input v-model="workflowReviewComment" type="textarea" :rows="3" placeholder="可选填写审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="workflowReviewVisible = false">取消</el-button>
        <el-button type="danger" :loading="workflowReviewing" @click="handleWorkflowReview(false)">退回</el-button>
        <el-button type="primary" :loading="workflowReviewing" @click="handleWorkflowReview(true)">通过</el-button>
      </template>
    </el-dialog>

    <!-- Admin edit dialog -->
    <el-dialog v-model="editVisible" title="编辑问题" width="700px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="问题标题" prop="title" required>
              <el-input v-model="editForm.title" maxlength="20" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="问题状态" prop="status" required>
              <el-select v-model="editForm.status" style="width: 100%">
                <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="问题详情" prop="description" required>
          <el-input v-model="editForm.description" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="提出部门" prop="submitterDepartment" required>
              <el-select v-model="editForm.submitterDepartment" style="width: 100%" clearable filterable>
                <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.name" :disabled="!d.enabled" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="提出人" prop="submitterId" required>
              <el-select v-model="editForm.submitterId" style="width: 100%" clearable filterable>
                <el-option v-for="u in editFormSubmitterOptions" :key="u.id" :label="`${u.name} (${u.department || '-'})`" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="提出场合" prop="occasionId" required>
              <el-select v-model="editForm.occasionId" style="width: 100%" clearable>
                <el-option-group label="会议">
                  <el-option v-for="o in occasions.filter(x => x.type === 'MEETING' && x.enabled)" :key="o.id" :label="o.name" :value="o.id" />
                </el-option-group>
                <el-option-group label="通用">
                  <el-option v-for="o in occasions.filter(x => x.type === 'GENERAL' && x.enabled)" :key="o.id" :label="o.name" :value="o.id" />
                </el-option-group>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="问题类型" prop="issueType" required>
              <el-select v-model="editForm.issueType" style="width: 100%" clearable>
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.name" :disabled="!c.enabled" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="会议参与部门">
              <el-input v-model="editForm.meetingDepartment" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="会议日期">
              <el-date-picker v-model="editForm.meetingDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="责任团队">
              <el-select v-model="editForm.responsibleTeam" style="width: 100%" clearable @change="editForm.system = ''; editForm.responsiblePersonId = null">
                <el-option v-for="t in teams" :key="t.id" :label="t.name" :value="t.name" :disabled="!t.enabled" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="责任人">
              <el-select v-model="editForm.responsiblePersonId" style="width: 100%" clearable filterable>
                <el-option v-for="u in allUsers" :key="u.id" :label="`${u.name} (${u.department || '-'})`" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="涉及系统">
              <el-select v-model="editForm.system" style="width: 100%" placeholder="请选择涉及系统" clearable filterable :disabled="!editForm.responsibleTeam">
                <el-option v-for="s in editTeamSystems" :key="s.id" :label="s.name" :value="s.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="产生原因">
          <el-input v-model="editForm.rootCause" type="textarea" :rows="2" maxlength="2000" />
        </el-form-item>
        <div style="display: flex; gap: 20px; margin-bottom: 18px;">
          <div style="flex: 1; min-width: 0;">
            <div style="font-weight: 600; margin-bottom: 12px; color: #e6a23c; font-size: 14px;">临时方案</div>
            <el-form-item label="临时整改方案">
              <el-input v-model="editForm.temporarySolution" type="textarea" :rows="2" maxlength="2000" show-word-limit />
            </el-form-item>
            <el-form-item label="临时整改时限">
              <el-date-picker v-model="editForm.temporaryDeadline" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </div>
          <div style="flex: 1; min-width: 0;">
            <div style="font-weight: 600; margin-bottom: 12px; color: #409eff; font-size: 14px;">永久方案</div>
            <el-form-item label="永久解决方案">
              <el-input v-model="editForm.permanentSolution" type="textarea" :rows="2" maxlength="1000" show-word-limit />
            </el-form-item>
            <el-form-item label="永久解决时限">
              <div style="display: flex; align-items: center; gap: 8px">
                <el-date-picker v-model="editForm.permanentDeadline" type="date" placeholder="选择日期" style="flex: 1" value-format="YYYY-MM-DD" :disabled="editForm.permanentLongTerm" />
                <el-checkbox v-model="editForm.permanentLongTerm">长期工作</el-checkbox>
              </div>
            </el-form-item>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Change proposal dialog -->
    <el-dialog v-model="changeProposalVisible" title="提交变更申请" width="750px">
      <el-form ref="changeProposalFormRef" :model="changeProposalForm" :rules="changeProposalRules" label-width="110px">
        <el-form-item label="产生原因">
          <el-input v-model="changeProposalForm.rootCause" type="textarea" :rows="2" maxlength="2000" />
        </el-form-item>
        <div style="display: flex; gap: 20px; margin-bottom: 18px;">
          <div style="flex: 1; min-width: 0;">
            <div style="font-weight: 600; margin-bottom: 12px; color: #e6a23c; font-size: 14px;">临时方案</div>
            <el-form-item label="临时整改方案" prop="temporarySolution">
              <el-input v-model="changeProposalForm.temporarySolution" type="textarea" :rows="3" maxlength="2000" show-word-limit />
            </el-form-item>
            <el-form-item label="临时整改时限" prop="temporaryDeadline">
              <el-date-picker v-model="changeProposalForm.temporaryDeadline" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </div>
          <div style="flex: 1; min-width: 0;">
            <div style="font-weight: 600; margin-bottom: 12px; color: #409eff; font-size: 14px;">永久方案</div>
            <el-form-item label="永久解决方案">
              <el-input v-model="changeProposalForm.permanentSolution" type="textarea" :rows="3" maxlength="1000" show-word-limit />
            </el-form-item>
            <el-form-item label="永久解决时限">
              <div style="display: flex; align-items: center; gap: 8px">
                <el-date-picker v-model="changeProposalForm.permanentDeadline" type="date" placeholder="选择日期" style="flex: 1" value-format="YYYY-MM-DD" :disabled="changeProposalForm.permanentLongTerm" />
                <el-checkbox v-model="changeProposalForm.permanentLongTerm">长期工作</el-checkbox>
              </div>
            </el-form-item>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="changeProposalVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingProposal" @click="handleSubmitChangeProposal">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- Review proposal dialog -->
    <el-dialog v-model="reviewVisible" title="审核变更" width="900px">
      <template v-if="reviewingProposal">
        <div class="review-header">
          <div><strong>{{ reviewingProposal.issueCode }}</strong> — {{ reviewingProposal.issueTitle }}</div>
          <div style="color: #909399; font-size: 13px">
            申请人: {{ reviewingProposal.proposerName }} · {{ formatDateTime(reviewingProposal.createdAt) }}
          </div>
        </div>
        <el-divider />
        <div class="diff-list">
          <div class="diff-item" v-for="item in [
            { label: '临时整改方案', old: reviewingProposal.oldTemporarySolution, new: reviewingProposal.newTemporarySolution },
            { label: '临时整改时限', old: reviewingProposal.oldTemporaryDeadline, new: reviewingProposal.newTemporaryDeadline },
            { label: '产生原因', old: reviewingProposal.oldRootCause, new: reviewingProposal.newRootCause },
            { label: '永久解决方案', old: reviewingProposal.oldPermanentSolution, new: reviewingProposal.newPermanentSolution },
            { label: '永久解决时限', old: reviewingProposal.oldPermanentDeadline, new: reviewingProposal.newPermanentDeadline },
          ]" :key="item.label">
            <div class="diff-label">{{ item.label }}</div>
            <div class="diff-cols" :class="{ 'diff-changed': hasFieldChanged(item.old, item.new) }">
              <div class="diff-col diff-old">
                <div class="diff-col-label">旧值</div>
                <div class="diff-col-value">{{ item.old || '（空）' }}</div>
              </div>
              <div class="diff-arrow">→</div>
              <div class="diff-col diff-new">
                <div class="diff-col-label">新值</div>
                <div class="diff-col-value">{{ item.new || '（空）' }}</div>
              </div>
            </div>
          </div>
        </div>
        <el-divider />
        <el-form label-width="80px">
          <el-form-item label="审批备注">
            <el-input v-model="reviewComment" type="textarea" :rows="2" placeholder="可选填写审批意见" />
          </el-form-item>
        </el-form>
        <div v-if="reviewingProposal.status === 'PENDING_TEAM_LEADER' && reviewingProposal.teamLeaderReviewComment" style="margin-bottom: 8px; font-size: 13px; color: #909399">
          组长已审批: {{ reviewingProposal.teamLeaderReviewComment }}
        </div>
      </template>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="danger" :loading="reviewing" @click="handleReview(false)">拒绝</el-button>
        <el-button type="primary" :loading="reviewing" @click="handleReview(true)">批准</el-button>
      </template>
    </el-dialog>

    <!-- Detail drawer -->
    <el-drawer v-model="detailVisible" title="问题详情" size="900px">
      <template v-if="detail">
        <div class="detail-status-bar">
          <div class="detail-status-item">
            <span class="detail-status-label">当前状态</span>
            <el-tag :type="getStatusType(detail.status)" size="large">{{ detail.status }}</el-tag>
          </div>
          <div class="detail-status-item">
            <span class="detail-status-label">当前处理人</span>
            <span class="detail-status-value">{{ detail.responsiblePersonName || '待分派' }}</span>
            <span v-if="detail.responsibleTeam" style="color: #909399; font-size: 12px; margin-left: 4px">({{ detail.responsibleTeam }})</span>
          </div>
        </div>
        <div class="detail-layout">
          <!-- Left: info -->
          <div class="detail-left">
            <div class="detail-header-actions">
              <el-button
                v-if="detail.lastOperatorId === userStore.user?.id && !['已完成','已关闭'].includes(detail.status)"
                type="warning" size="small"
                @click="handleUndo(detail)"
              >撤回</el-button>
              <el-button
                v-if="isTeamLeaderForIssue(detail)"
                type="primary" size="small"
                @click="openLeaderReview(detail)"
              >审核</el-button>
              <el-button
                v-if="canEditSolution(detail)"
                type="primary" size="small"
                @click="startEditSolution"
              >编辑方案</el-button>
            </div>
            <el-descriptions :column="1" border label-width="110px" class="detail-descriptions">
              <el-descriptions-item label="问题编号">{{ detail.issueCode }}</el-descriptions-item>
              <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
              <el-descriptions-item label="详情">
                <div class="long-text">{{ detail.description || '-' }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="提出部门">{{ detail.submitterDepartment }}</el-descriptions-item>
              <el-descriptions-item label="提出人">{{ detail.submitterName }}</el-descriptions-item>
              <el-descriptions-item label="提出场合">{{ detail.occasionName || '-' }}</el-descriptions-item>
              <el-descriptions-item v-if="detail.meetingDepartment" label="会议参与部门">{{ detail.meetingDepartment }}</el-descriptions-item>
              <el-descriptions-item v-if="detail.meetingDate" label="会议日期">{{ formatDate(detail.meetingDate) }}</el-descriptions-item>
              <el-descriptions-item label="问题类型">{{ detail.issueType || '-' }}</el-descriptions-item>
              <el-descriptions-item label="责任团队">{{ detail.responsibleTeam || '未分配' }}</el-descriptions-item>
              <el-descriptions-item label="责任人">{{ detail.responsiblePersonName || '未分配' }}</el-descriptions-item>
              <el-descriptions-item label="涉及系统">{{ detail.system || '未指定' }}</el-descriptions-item>

              <template v-if="editingSolution">
                <el-descriptions-item label="产生原因">
                  <el-input v-model="editSolutionForm.rootCause" type="textarea" :rows="2" maxlength="2000" />
                </el-descriptions-item>
                <el-descriptions-item label="方案编辑">
                  <div style="display: flex; gap: 16px;">
                    <div style="flex: 1; min-width: 0;">
                      <div style="font-weight: 600; margin-bottom: 8px; color: #e6a23c; font-size: 13px;">临时方案</div>
                      <div style="margin-bottom: 8px;">
                        <div style="color: #606266; font-size: 13px; margin-bottom: 4px;">临时整改方案</div>
                        <el-input v-model="editSolutionForm.temporarySolution" type="textarea" :rows="3" maxlength="2000" />
                      </div>
                      <div>
                        <div style="color: #606266; font-size: 13px; margin-bottom: 4px;">临时整改时限</div>
                        <el-date-picker v-model="editSolutionForm.temporaryDeadline" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
                      </div>
                    </div>
                    <div style="flex: 1; min-width: 0;">
                      <div style="font-weight: 600; margin-bottom: 8px; color: #409eff; font-size: 13px;">永久方案</div>
                      <div style="margin-bottom: 8px;">
                        <div style="color: #606266; font-size: 13px; margin-bottom: 4px;">永久解决方案</div>
                        <el-input v-model="editSolutionForm.permanentSolution" type="textarea" :rows="3" maxlength="1000" />
                      </div>
                      <div>
                        <div style="color: #606266; font-size: 13px; margin-bottom: 4px;">永久解决时限</div>
                        <div style="display: flex; align-items: center; gap: 8px">
                          <el-date-picker v-model="editSolutionForm.permanentDeadline" type="date" placeholder="选择日期" style="flex: 1" value-format="YYYY-MM-DD" :disabled="editSolutionForm.permanentLongTerm" />
                          <el-checkbox v-model="editSolutionForm.permanentLongTerm">长期工作</el-checkbox>
                        </div>
                      </div>
                    </div>
                  </div>
                </el-descriptions-item>
                <el-descriptions-item label="">
                  <div style="display: flex; gap: 8px">
                    <el-button type="primary" size="small" :loading="savingSolution" @click="saveEditSolution">保存</el-button>
                    <el-button size="small" @click="cancelEditSolution">取消</el-button>
                  </div>
                </el-descriptions-item>
              </template>
              <template v-else>
                <el-descriptions-item label="临时整改方案">
                  <div class="long-text">{{ detail.temporarySolution || '-' }}</div>
                </el-descriptions-item>
                <el-descriptions-item label="临时整改时限">{{ formatDate(detail.temporaryDeadline) }}</el-descriptions-item>
                <el-descriptions-item label="产生原因">
                  <div class="long-text">{{ detail.rootCause || '-' }}</div>
                </el-descriptions-item>
                <el-descriptions-item label="永久解决方案">
                  <div class="long-text">{{ detail.permanentSolution || '-' }}</div>
                </el-descriptions-item>
                <el-descriptions-item label="永久解决时限">{{ formatDeadline(detail.permanentDeadline) }}</el-descriptions-item>
              </template>

              <el-descriptions-item label="状态">
                <el-tag :type="getStatusType(detail.status)">{{ detail.status }}</el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- Right: timeline + attachments + systems -->
          <div class="detail-right">
            <!-- System assignments (admin) -->
            <div v-if="detailSystemAssignments.length > 0 && canManage" style="margin-bottom: 16px;">
              <h4 style="margin: 0 0 8px">系统负责人完成状态</h4>
              <div v-for="sa in detailSystemAssignments" :key="sa.id"
                   style="display: flex; justify-content: space-between; align-items: center; padding: 6px 10px; background: #f7f8fa; border-radius: 6px; margin-bottom: 4px; font-size: 13px;">
                <div>
                  <span style="font-weight: 500;">{{ sa.systemOwnerName }}</span>
                  <span style="color: #909399; margin-left: 4px;">({{ sa.systemName }})</span>
                </div>
                <el-tag :type="sa.completed ? 'success' : 'info'" size="small">{{ sa.completed ? '已完成' : '未完成' }}</el-tag>
              </div>
              <el-button
                v-if="detail.status === '解决中' || detail.status === '待确认'"
                type="success" size="small" style="margin-top: 8px; width: 100%;"
                :disabled="detailSystemAssignments.some(sa => !sa.completed)"
                @click="handleFeedbackToSubmitter(detail)"
              >
                <span v-if="detailSystemAssignments.some(sa => !sa.completed)">
                  尚有 {{ detailSystemAssignments.filter(sa => !sa.completed).length }} 位负责人未完成
                </span>
                <span v-else>反馈给问题提出人</span>
              </el-button>
            </div>
            <!-- Attachments -->
            <div v-if="detailAttachments.length > 0" style="margin-bottom: 16px;">
              <h4 style="margin: 0 0 8px">附件 ({{ detailAttachments.length }})</h4>
              <div v-for="att in detailAttachments" :key="att.id"
                   style="display: flex; justify-content: space-between; align-items: center; padding: 6px 10px; background: #f7f8fa; border-radius: 6px; margin-bottom: 4px; font-size: 13px;">
                <a :href="att.filePath" target="_blank" style="color: #409eff; text-decoration: none; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 180px;">
                  {{ att.fileName }}
                </a>
                <span style="color: #909399; font-size: 12px; white-space: nowrap;">{{ (att.fileSize / 1024).toFixed(1) }} KB</span>
              </div>
            </div>
            <h4 style="margin: 0 0 12px">操作日志</h4>
            <el-timeline>
              <el-timeline-item
                v-for="log in detail.logs"
                :key="log.id"
                :timestamp="formatDateTime(log.createdAt)"
                placement="top"
              >
                <strong>{{ log.userName }}</strong> — {{ log.action }}
                <div v-if="log.remark" style="color: #909399; font-size: 13px">{{ log.remark }}</div>
              </el-timeline-item>
              <el-timeline-item v-if="!detail.logs || detail.logs.length === 0" placement="top" color="#c0c4cc">
                暂无操作记录
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </template>
    </el-drawer>

    <!-- Shared action dropdown menu (single instance, avoids per-row Popper overhead) -->
    <teleport to="body">
      <div
        v-if="dropdownVisible && dropdownRow"
        class="action-dropdown-menu"
        :style="{ top: dropdownPos.top + 'px', left: dropdownPos.left + 'px' }"
        @click.stop
      >
        <div
          v-for="act in dropdownRow._secondary"
          :key="act.label"
          class="action-dropdown-item"
          @click="act.handler(); closeDropdown()"
        >
          {{ act.label }}
        </div>
      </div>
    </teleport>
  </div>
</template>

<style scoped>
.action-btns {
  display: flex;
  gap: 4px;
  flex-wrap: nowrap;
  align-items: center;
}

.action-btns :deep(.el-button--small) {
  border-radius: 6px;
}
.action-dropdown-menu {
  position: fixed;
  z-index: 3000;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 4px 0;
  min-width: 120px;
}
.action-dropdown-item {
  padding: 8px 16px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  transition: background 0.15s;
}
.action-dropdown-item:hover {
  background: #f5f7fa;
}
.review-issue-info {
  margin-bottom: 8px;
}
.review-issue-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.review-issue-code {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
  font-family: monospace;
}
.review-issue-title {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #303133;
}
.long-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  max-height: 200px;
  overflow-y: auto;
}
.detail-status-bar {
  display: flex;
  gap: 32px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
  align-items: center;
}
.detail-status-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.detail-status-label {
  font-size: 13px;
  color: #909399;
}
.detail-status-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.detail-layout {
  display: flex;
  gap: 24px;
}
.detail-left {
  flex: 1;
  min-width: 0;
}
.detail-right {
  width: 260px;
  flex-shrink: 0;
  border-left: 1px solid #ebeef5;
  padding-left: 20px;
  overflow-y: auto;
}
.detail-header-actions {
  text-align: right;
  margin-bottom: 12px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
}
.table-wrap {
  overflow-x: auto;
}
.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: nowrap;
  align-items: center;
  overflow-x: auto;
}

/* === Change proposal === */
.proposal-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.proposal-card {
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: box-shadow 0.15s;
}
.proposal-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.proposal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.proposal-code {
  font-weight: 600;
  color: #1d2129;
}
.proposal-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 6px;
}
.proposal-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}
.review-header {
  margin-bottom: 4px;
}
.diff-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.diff-item {
  padding: 10px 14px;
  background: #f7f8fa;
  border-radius: 8px;
}
.diff-label {
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 8px;
  color: #303133;
}
.diff-cols {
  display: flex;
  gap: 12px;
  align-items: stretch;
}
.diff-cols.diff-changed {
  background: #ecf5ff;
  padding: 8px;
  border-radius: 6px;
  border-left: 3px solid #409eff;
  margin: 0 -8px;
}
.diff-col {
  flex: 1;
  min-width: 0;
}
.diff-col-label {
  font-size: 11px;
  color: #909399;
  margin-bottom: 2px;
}
.diff-col-value {
  font-size: 13px;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.4;
}
.diff-arrow {
  font-size: 18px;
  color: #909399;
  align-self: center;
  flex-shrink: 0;
}
.diff-changed .diff-new .diff-col-value {
  color: #409eff;
  font-weight: 500;
}

/* === Mobile === */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  .search-bar {
    flex-direction: column;
    align-items: stretch;
  }
  .search-bar .el-select,
  .search-bar .el-date-picker,
  .search-bar .el-button {
    width: 100% !important;
  }
  .detail-layout {
    flex-direction: column;
  }
  .detail-right {
    width: 100%;
    border-left: none;
    padding-left: 0;
    border-top: 1px solid #ebeef5;
    padding-top: 16px;
    margin-top: 16px;
  }
  :deep(.el-dialog) {
    width: 95% !important;
  }
  :deep(.el-drawer) {
    width: 100% !important;
  }
}

/* Solution dialog issue info */
.solution-issue-info {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 16px;
  font-size: 13px;
}
.solution-issue-info .si-row {
  margin-bottom: 4px;
  color: #606266;
}
.solution-issue-info .si-row:last-child {
  margin-bottom: 0;
}
.solution-issue-info .si-label {
  color: #909399;
}
</style>
