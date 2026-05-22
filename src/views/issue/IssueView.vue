<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, View, Download } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getList, getById, create, assign, submitSolution, complete, confirm, reject, closeIssue, exportIssues } from '@/api/issue'
import { getUsers, getCategories, getDepartments, getTeams, getOccasions } from '@/api/admin'

const userStore = useUserStore()

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
  submitterDepartment: '',
  occasionId: null,
  issueType: '',
  responsibleTeam: '',
  responsiblePersonId: null,
  dateFrom: '',
  dateTo: '',
})

const statusOptions = [
  { label: '待分派', value: '待分派' },
  { label: '已分派', value: '已分派' },
  { label: '整改中', value: '整改中' },
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

// === Create dialog ===
const createVisible = ref(false)
const createFormRef = ref(null)
const createForm = reactive({
  submitterDepartment: '',
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
}

const selectedOccasion = computed(() => {
  if (!createForm.occasionId) return null
  return occasions.value.find(o => o.id === createForm.occasionId)
})
const showMeetingFields = computed(() => {
  return selectedOccasion.value && selectedOccasion.value.name === '业务协调会'
})

// === Assign dialog ===
const assignVisible = ref(false)
const assignId = ref(null)
const assignFormRef = ref(null)
const assignForm = reactive({ responsibleTeam: '', responsiblePersonId: null })
const assignRules = {
  responsibleTeam: [{ required: true, message: '请选择责任团队', trigger: 'change' }],
  responsiblePersonId: [{ required: true, message: '请选择责任人', trigger: 'change' }],
}
const selectedTeam = computed(() => {
  if (!assignForm.responsibleTeam) return null
  return teams.value.find(t => t.name === assignForm.responsibleTeam)
})
const teamMembers = computed(() => {
  if (!selectedTeam.value || !selectedTeam.value.members) return []
  const names = selectedTeam.value.members.split(',').map(s => s.trim()).filter(Boolean)
  return allUsers.value.filter(u => names.includes(u.name))
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
})
const solutionRules = {
  temporarySolution: [{ required: true, message: '请输入临时整改方案', trigger: 'blur' }],
  temporaryDeadline: [{ required: true, message: '请选择临时整改时限', trigger: 'change' }],
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
})
const savingSolution = ref(false)

function canEditSolution(row) {
  return isResponsiblePerson(row) && (row.status === '已分派' || row.status === '整改中')
}
function startEditSolution() {
  const d = detail.value
  editSolutionForm.temporarySolution = d.temporarySolution || ''
  editSolutionForm.temporaryDeadline = d.temporaryDeadline || ''
  editSolutionForm.rootCause = d.rootCause || ''
  editSolutionForm.permanentSolution = d.permanentSolution || ''
  editSolutionForm.permanentDeadline = d.permanentDeadline || ''
  editingSolution.value = true
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
    await submitSolution(detail.value.id, { ...editSolutionForm })
    ElMessage.success('方案已更新')
    editingSolution.value = false
    const res = await getById(detail.value.id)
    detail.value = res.data
  } finally {
    savingSolution.value = false
  }
}

// === Permissions ===
const isAdmin = computed(() => userStore.hasRole('ROLE_ADMIN'))
const isIssueAdmin = computed(() => userStore.hasRole('ROLE_ISSUE_ADMIN'))
const canManage = computed(() => isAdmin.value || isIssueAdmin.value)

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
    if (filters.submitterDepartment) params.submitterDepartment = filters.submitterDepartment
    if (filters.occasionId) params.occasionId = filters.occasionId
    if (filters.issueType) params.issueType = filters.issueType
    if (filters.responsibleTeam) params.responsibleTeam = filters.responsibleTeam
    if (filters.responsiblePersonId) params.responsiblePersonId = filters.responsiblePersonId
    if (filters.dateFrom) params.dateFrom = filters.dateFrom
    if (filters.dateTo) params.dateTo = filters.dateTo
    const res = await getList(params)
    tableData.value = res.data?.content || []
    totalElements.value = res.data?.totalElements || 0
  } finally {
    loading.value = false
  }
}

async function loadRefData() {
  try {
    const [uRes, cRes, dRes, tRes, oRes] = await Promise.all([
      getUsers({ size: 9999, enabled: true }).catch(() => ({})),
      getCategories().catch(() => ({})),
      getDepartments().catch(() => ({})),
      getTeams().catch(() => ({})),
      getOccasions().catch(() => ({})),
    ])
    allUsers.value = uRes.data?.content || []
    categories.value = cRes.data || []
    departments.value = dRes.data || []
    teams.value = tRes.data || []
    occasions.value = oRes.data || []
  } catch { /* ignore */ }
}

// === Filter handlers ===
function handleFilter() {
  currentPage.value = 1
  fetchData()
}
const defaultStatuses = computed(() => statusOptions.filter(s => s.value !== '已关闭').map(s => s.value))

function handleResetFilters() {
  Object.assign(filters, {
    statuses: [...defaultStatuses.value], submitterIds: [], submitterDepartment: '',
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
function handleCreate() {
  createForm.submitterDepartment = userStore.user?.department || ''
  createForm.occasionId = null
  createForm.meetingDepartment = ''
  createForm.meetingDate = ''
  createForm.title = ''
  createForm.description = ''
  createForm.issueType = ''
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
  await create(data)
  ElMessage.success('问题已提交')
  createVisible.value = false
  fetchData()
}

// === Assign ===
function handleAssign(row) {
  assignId.value = row.id
  assignForm.responsibleTeam = row.responsibleTeam || ''
  assignForm.responsiblePersonId = null
  assignVisible.value = true
}
async function handleAssignSubmit() {
  const valid = await assignFormRef.value.validate().catch(() => null)
  if (!valid) return
  await assign(assignId.value, { ...assignForm })
  ElMessage.success('已分派')
  assignVisible.value = false
  fetchData()
}

// === Solution ===
function handleSolution(row) {
  solutionId.value = row.id
  solutionForm.temporarySolution = row.temporarySolution || ''
  solutionForm.temporaryDeadline = row.temporaryDeadline || ''
  solutionForm.rootCause = row.rootCause || ''
  solutionForm.permanentSolution = row.permanentSolution || ''
  solutionForm.permanentDeadline = row.permanentDeadline || ''
  solutionVisible.value = true
}
async function handleSolutionSubmit() {
  const valid = await solutionFormRef.value.validate().catch(() => null)
  if (!valid) return
  await submitSolution(solutionId.value, { ...solutionForm })
  ElMessage.success('方案已提交')
  solutionVisible.value = false
  fetchData()
}

// === Complete ===
async function handleComplete(row) {
  await ElMessageBox.confirm('确定提交完成吗？', '提示', { type: 'warning' })
  await complete(row.id)
  ElMessage.success('已提交完成，等待确认')
  fetchData()
}

// === Confirm ===
async function handleConfirm(row, satisfied) {
  const title = satisfied ? '确认完成' : '退回重改'
  if (satisfied) {
    await ElMessageBox.confirm('确定该问题已整改完成吗？', title, { type: 'warning' })
    await confirm(row.id, { satisfied: true, remark: '' })
    ElMessage.success('问题已关闭')
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
function handleReject(row) {
  rejectId.value = row.id
  rejectForm.reason = ''
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

// === Export ===
async function handleExport() {
  const params = {}
  if (filters.statuses.length) params.statuses = filters.statuses.join(',')
  if (filters.submitterIds.length) params.submitterIds = filters.submitterIds.join(',')
  if (filters.submitterDepartment) params.submitterDepartment = filters.submitterDepartment
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
async function handleDetail(row) {
  try {
    const res = await getById(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch { /* ignore */ }
}

// === Helpers ===
function getStatusType(status) {
  const map = { '待分派': 'info', '已分派': 'warning', '整改中': 'warning', '待确认': '', '已完成': 'success', '已驳回': 'danger', '已关闭': 'info' }
  return map[status] || ''
}
function formatDateTime(dt) {
  if (!dt) return '-'
  const d = new Date(dt)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
function formatDate(d) { if (!d) return '-'; return d.split('T')[0] }
function isResponsiblePerson(row) { return row.responsiblePersonId === userStore.user?.id }
function isSubmitter(row) { return row.submitterId === userStore.user?.id }

watch(() => createForm.occasionId, () => {
  if (!showMeetingFields.value) {
    createForm.meetingDepartment = ''
    createForm.meetingDate = ''
  }
})

onMounted(() => {
  filters.statuses = [...defaultStatuses.value]
  fetchData()
  loadRefData()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>科技问题管理</h2>
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
      <el-select v-model="filters.submitterDepartment" placeholder="提出部门" clearable style="width: 140px" @change="handleFilter">
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
    <el-table :data="tableData" v-loading="loading" stripe border @sort-change="handleSortChange"
              :default-sort="{ prop: 'createdAt', order: 'descending' }">
      <el-table-column prop="issueCode" label="问题编号" min-width="170" sortable="custom" />
      <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip sortable="custom">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleDetail(row)">{{ row.title }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="submitterDepartment" label="提出部门" min-width="110" sortable="custom" />
      <el-table-column prop="submitterName" label="提出人" min-width="110" sortable="custom" />
      <el-table-column prop="occasionName" label="提出场合" min-width="130" show-overflow-tooltip />
      <el-table-column prop="issueType" label="问题类型" min-width="120" sortable="custom" />
      <el-table-column prop="responsibleTeam" label="责任团队" min-width="110" show-overflow-tooltip />
      <el-table-column prop="responsiblePersonName" label="责任人" min-width="90">
        <template #default="{ row }">
          <span v-if="row.responsiblePersonName">{{ row.responsiblePersonName }}</span>
          <span v-else style="color: #c0c4cc">未分配</span>
        </template>
      </el-table-column>
      <el-table-column prop="temporaryDeadline" label="临时时限" min-width="110" sortable="custom">
        <template #default="{ row }">{{ formatDate(row.temporaryDeadline) }}</template>
      </el-table-column>
      <el-table-column prop="permanentDeadline" label="永久时限" min-width="110" sortable="custom">
        <template #default="{ row }">{{ formatDate(row.permanentDeadline) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" min-width="90" sortable="custom">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="提出日期" min-width="120" sortable="custom">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" min-width="200" fixed="right" class-name="actions-col">
        <template #default="{ row }">
          <el-button type="info" link :icon="View" @click="handleDetail(row)">详情</el-button>

          <el-button
            v-if="canManage && (row.status === '待分派' || row.status === '已分派' || row.status === '整改中')"
            type="primary" link @click="handleAssign(row)"
          >分派</el-button>

          <el-button
            v-if="canManage && row.status === '待分派'"
            type="danger" link @click="handleReject(row)"
          >驳回</el-button>

          <el-button
            v-if="(row.status === '已分派' || row.status === '整改中') && isResponsiblePerson(row)"
            type="primary" link @click="handleSolution(row)"
          >方案</el-button>

          <el-button
            v-if="row.status === '整改中' && isResponsiblePerson(row)"
            type="success" link @click="handleComplete(row)"
          >完成</el-button>

          <template v-if="row.status === '待确认' && isSubmitter(row)">
            <el-button type="success" link @click="handleConfirm(row, true)">确认</el-button>
            <el-button type="warning" link @click="handleConfirm(row, false)">退回</el-button>
          </template>

          <el-button
            v-if="canManage && row.status !== '已关闭'"
            type="danger" link @click="handleClose(row)"
          >关闭</el-button>
        </template>
      </el-table-column>
    </el-table>

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
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateSubmit">提交</el-button>
      </template>
    </el-dialog>

    <!-- Assign dialog -->
    <el-dialog v-model="assignVisible" title="分配团队和责任人" width="480px">
      <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="100px">
        <el-form-item label="责任团队" prop="responsibleTeam">
          <el-select v-model="assignForm.responsibleTeam" style="width: 100%" placeholder="请选择责任团队">
            <el-option v-for="t in teams" :key="t.id" :label="t.name" :value="t.name" :disabled="!t.enabled" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任人" prop="responsiblePersonId">
          <el-select v-model="assignForm.responsiblePersonId" style="width: 100%" placeholder="请选择责任人">
            <el-option v-for="u in teamMembers" :key="u.id" :label="`${u.name} (${u.department || '-'})`" :value="u.id" />
          </el-select>
          <span v-if="assignForm.responsibleTeam && teamMembers.length === 0" style="color: #f56c6c; font-size: 12px;">
            该团队暂无成员，请先为团队添加成员
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Solution dialog -->
    <el-dialog v-model="solutionVisible" title="整改方案" width="600px">
      <el-form ref="solutionFormRef" :model="solutionForm" :rules="solutionRules" label-width="110px">
        <el-form-item label="临时整改方案" prop="temporarySolution">
          <el-input v-model="solutionForm.temporarySolution" type="textarea" :rows="4" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="临时整改时限" prop="temporaryDeadline">
          <el-date-picker v-model="solutionForm.temporaryDeadline" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="产生原因">
          <el-input v-model="solutionForm.rootCause" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="永久解决方案">
          <el-input v-model="solutionForm.permanentSolution" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="永久解决时限">
          <el-date-picker v-model="solutionForm.permanentDeadline" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="solutionVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSolutionSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Reject dialog -->
    <el-dialog v-model="rejectVisible" title="驳回问题" width="480px">
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

    <!-- Detail drawer -->
    <el-drawer v-model="detailVisible" title="问题详情" size="900px">
      <template v-if="detail">
        <div class="detail-layout">
          <!-- Left: info -->
          <div class="detail-left">
            <div class="detail-header-actions">
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

              <template v-if="editingSolution">
                <el-descriptions-item label="临时整改方案">
                  <el-input v-model="editSolutionForm.temporarySolution" type="textarea" :rows="3" maxlength="2000" />
                </el-descriptions-item>
                <el-descriptions-item label="临时整改时限">
                  <el-date-picker v-model="editSolutionForm.temporaryDeadline" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
                </el-descriptions-item>
                <el-descriptions-item label="产生原因">
                  <el-input v-model="editSolutionForm.rootCause" type="textarea" :rows="2" maxlength="2000" />
                </el-descriptions-item>
                <el-descriptions-item label="永久解决方案">
                  <el-input v-model="editSolutionForm.permanentSolution" type="textarea" :rows="2" maxlength="1000" />
                </el-descriptions-item>
                <el-descriptions-item label="永久解决时限">
                  <el-date-picker v-model="editSolutionForm.permanentDeadline" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
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
                <el-descriptions-item label="永久解决时限">{{ formatDate(detail.permanentDeadline) }}</el-descriptions-item>
              </template>

              <el-descriptions-item label="状态">
                <el-tag :type="getStatusType(detail.status)">{{ detail.status }}</el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- Right: timeline -->
          <div class="detail-right">
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
  </div>
</template>

<style scoped>
:deep(.actions-col .cell) {
  white-space: nowrap;
}
.long-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  max-height: 200px;
  overflow-y: auto;
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
.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}
</style>
