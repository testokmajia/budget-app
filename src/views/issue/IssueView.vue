<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, User, Document, CircleCheck, CircleClose, View } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getList, getById, create, assign, submitSolution, complete, confirm, reject } from '@/api/issue'
import { getUsers } from '@/api/admin'
import { getCategories } from '@/api/admin'

const userStore = useUserStore()
const loading = ref(false)
const tableData = ref([])

const statusFilter = ref('')

const statusOptions = [
  { label: '待分派', value: '待分派' },
  { label: '整改中', value: '整改中' },
  { label: '待确认', value: '待确认' },
  { label: '已完成', value: '已完成' },
  { label: '已驳回', value: '已驳回' },
]

const urgencyOptions = [
  { label: '紧急', value: '紧急' },
  { label: '普通', value: '普通' },
  { label: '低', value: '低' },
]

// === 新建问题 ===
const createVisible = ref(false)
const createFormRef = ref(null)
const createForm = reactive({
  title: '',
  description: '',
  department: '',
  category: '',
  urgency: '普通',
  system: '',
})
const createRules = {
  title: [{ required: true, message: '请输入问题标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入问题描述', trigger: 'blur' }],
  department: [{ required: true, message: '请输入所属部门', trigger: 'blur' }],
  urgency: [{ required: true, message: '请选择紧急程度', trigger: 'change' }],
}

// === 分派 ===
const assignVisible = ref(false)
const assignId = ref(null)
const assignFormRef = ref(null)
const assignForm = reactive({ category: '', assigneeId: null })
const assignRules = { assigneeId: [{ required: true, message: '请选择责任人', trigger: 'change' }] }
const users = ref([])
const categories = ref([])

// === 解决方案 ===
const solutionVisible = ref(false)
const solutionId = ref(null)
const solutionFormRef = ref(null)
const solutionForm = reactive({ solution: '', deadline: '', progress: '' })
const solutionRules = {
  solution: [{ required: true, message: '请输入解决方案', trigger: 'blur' }],
  deadline: [{ required: true, message: '请选择解决时限', trigger: 'change' }],
}

// === 驳回 ===
const rejectVisible = ref(false)
const rejectId = ref(null)
const rejectFormRef = ref(null)
const rejectForm = reactive({ reason: '' })
const rejectRules = { reason: [{ required: true, message: '请输入驳回原因', trigger: 'blur' }] }

// === 详情 ===
const detailVisible = ref(false)
const detail = ref(null)

// === 权限判断 ===
const isAdmin = computed(() => userStore.hasRole('ROLE_ADMIN'))
const isIssueAdmin = computed(() => userStore.hasRole('ROLE_ISSUE_ADMIN'))
const canManage = computed(() => isAdmin.value || isIssueAdmin.value)

// === 数据加载 ===
async function fetchData() {
  loading.value = true
  try {
    const params = {}
    if (statusFilter.value) params.status = statusFilter.value
    const res = await getList(params)
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  try {
    const res = await getUsers()
    users.value = res.data || []
  } catch { /* ignore */ }
}

async function loadCategories() {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch { /* ignore */ }
}

// === 新建 ===
function handleCreate() {
  createForm.title = ''
  createForm.description = ''
  createForm.department = userStore.user?.department || ''
  createForm.category = ''
  createForm.urgency = '普通'
  createForm.system = ''
  createVisible.value = true
}

async function handleCreateSubmit() {
  const valid = await createFormRef.value.validate().catch(() => null)
  if (!valid) return
  await create({ ...createForm })
  ElMessage.success('问题已提交')
  createVisible.value = false
  fetchData()
}

// === 分派 ===
function handleAssign(row) {
  assignId.value = row.id
  assignForm.category = row.category || ''
  assignForm.assigneeId = null
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

// === 方案 ===
function handleSolution(row) {
  solutionId.value = row.id
  solutionForm.solution = row.solution || ''
  solutionForm.deadline = row.deadline || ''
  solutionForm.progress = row.progress || ''
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

// === 完成 ===
async function handleComplete(row) {
  await ElMessageBox.confirm('确定提交完成吗？', '提示', { type: 'warning' })
  await complete(row.id)
  ElMessage.success('已提交完成，等待确认')
  fetchData()
}

// === 确认 ===
async function handleConfirm(row, satisfied) {
  const title = satisfied ? '确认完成' : '退回重改'
  const text = satisfied ? '确定该问题已整改完成吗？' : '请输入退回原因：'
  if (satisfied) {
    await ElMessageBox.confirm(text, title, { type: 'warning' })
    await confirm(row.id, { satisfied: true, remark: '' })
    ElMessage.success('问题已关闭')
  } else {
    const { value } = await ElMessageBox.prompt(text, title, {
      confirmButtonText: '确定退回',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValidator: (v) => v && v.trim() ? true : '请输入退回原因',
      inputErrorMessage: '退回原因不能为空',
    }).catch(() => null)
    if (!value) return
    await confirm(row.id, { satisfied: false, remark: value })
    ElMessage.success('已退回整改')
  }
  fetchData()
}

// === 驳回 ===
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

// === 详情 ===
async function handleDetail(row) {
  try {
    const res = await getById(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch { /* ignore */ }
}

// === 公用 ===
function getStatusType(status) {
  const map = { '待分派': 'info', '整改中': 'warning', '待确认': '', '已完成': 'success', '已驳回': 'danger' }
  return map[status] || ''
}

function getUrgencyType(urgency) {
  const map = { '紧急': 'danger', '普通': 'warning', '低': 'info' }
  return map[urgency] || 'info'
}

function formatDateTime(dt) {
  if (!dt) return '-'
  const d = new Date(dt)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatDate(d) {
  return d || '-'
}

// 当前用户是否是该问题的责任人
function isAssignee(row) {
  return row.assigneeId === userStore.user?.id
}

// 当前用户是否是该问题的提出人
function isSubmitter(row) {
  return row.submitterId === userStore.user?.id
}

onMounted(() => {
  fetchData()
  loadUsers()
  loadCategories()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>科技问题反馈</h2>
      <el-button type="primary" :icon="Plus" @click="handleCreate">提交问题</el-button>
    </div>

    <div class="search-bar">
      <el-select v-model="statusFilter" placeholder="问题状态" clearable style="width: 130px" @change="fetchData">
        <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="id" label="编号" width="70" />
      <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
      <el-table-column prop="department" label="部门" width="100" />
      <el-table-column prop="category" label="分类" width="100" />
      <el-table-column prop="urgency" label="紧急程度" width="100">
        <template #default="{ row }">
          <el-tag :type="getUrgencyType(row.urgency)" size="small">{{ row.urgency }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="system" label="所属系统" width="120" show-overflow-tooltip />
      <el-table-column prop="submitterName" label="提出人" width="90" />
      <el-table-column prop="assigneeName" label="责任人" width="90">
        <template #default="{ row }">
          <span v-if="row.assigneeName">{{ row.assigneeName }}</span>
          <span v-else style="color: #c0c4cc">未分配</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="deadline" label="解决时限" width="110">
        <template #default="{ row }">{{ formatDate(row.deadline) }}</template>
      </el-table-column>
      <el-table-column prop="progress" label="进度" width="120" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="提出日期" width="160">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <!-- 查看详情 -->
          <el-button type="info" link :icon="View" @click="handleDetail(row)">详情</el-button>

          <!-- 分派：待分派 or 整改中，管理员可见 -->
          <el-button
            v-if="canManage && (row.status === '待分派' || row.status === '整改中')"
            type="primary" link :icon="User" @click="handleAssign(row)"
          >分派</el-button>

          <!-- 驳回：待分派，管理员可见 -->
          <el-button
            v-if="canManage && row.status === '待分派'"
            type="danger" link :icon="CircleClose" @click="handleReject(row)"
          >驳回</el-button>

          <!-- 填写方案：整改中，责任人可见 -->
          <el-button
            v-if="row.status === '整改中' && isAssignee(row)"
            type="primary" link :icon="Document" @click="handleSolution(row)"
          >方案</el-button>

          <!-- 提交完成：整改中，责任人可见 -->
          <el-button
            v-if="row.status === '整改中' && isAssignee(row)"
            type="success" link :icon="CircleCheck" @click="handleComplete(row)"
          >完成</el-button>

          <!-- 确认/退回：待确认，提出人可见 -->
          <template v-if="row.status === '待确认' && isSubmitter(row)">
            <el-button type="success" link :icon="CircleCheck" @click="handleConfirm(row, true)">确认</el-button>
            <el-button type="warning" link :icon="CircleClose" @click="handleConfirm(row, false)">退回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建对话框 -->
    <el-dialog v-model="createVisible" title="提交问题" width="560px" @keyup.enter="handleCreateSubmit">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="问题标题" prop="title">
          <el-input v-model="createForm.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="问题描述" prop="description">
          <el-input v-model="createForm.description" type="textarea" :rows="4" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="所属部门" prop="department">
          <el-input v-model="createForm.department" maxlength="100" />
        </el-form-item>
        <el-form-item label="问题分类">
          <el-select v-model="createForm.category" style="width: 100%" clearable placeholder="请选择分类">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="紧急程度" prop="urgency">
          <el-select v-model="createForm.urgency" style="width: 100%">
            <el-option v-for="o in urgencyOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属系统">
          <el-input v-model="createForm.system" maxlength="100" placeholder="如：ERP、OA等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateSubmit">提交</el-button>
      </template>
    </el-dialog>

    <!-- 分派对话框 -->
    <el-dialog v-model="assignVisible" title="分派责任人" width="480px">
      <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="100px">
        <el-form-item label="问题分类">
          <el-select v-model="assignForm.category" style="width: 100%" clearable placeholder="可修改分类">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任人" prop="assigneeId">
          <el-select v-model="assignForm.assigneeId" style="width: 100%" placeholder="请选择责任人">
            <el-option v-for="u in users" :key="u.id" :label="`${u.name} (${u.department || '-'})`" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 方案对话框 -->
    <el-dialog v-model="solutionVisible" title="整改方案" width="560px">
      <el-form ref="solutionFormRef" :model="solutionForm" :rules="solutionRules" label-width="100px">
        <el-form-item label="解决方案" prop="solution">
          <el-input v-model="solutionForm.solution" type="textarea" :rows="4" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="解决时限" prop="deadline">
          <el-date-picker v-model="solutionForm.deadline" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="当前进度">
          <el-input v-model="solutionForm.progress" maxlength="500" show-word-limit placeholder="如：已定位问题，正在修复" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="solutionVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSolutionSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 驳回对话框 -->
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

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="问题详情" size="500px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="编号">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ detail.description }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ detail.department }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ detail.category || '-' }}</el-descriptions-item>
          <el-descriptions-item label="紧急程度">
            <el-tag :type="getUrgencyType(detail.urgency)" size="small">{{ detail.urgency }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="所属系统">{{ detail.system || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提出人">{{ detail.submitterName }}</el-descriptions-item>
          <el-descriptions-item label="责任人">{{ detail.assigneeName || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(detail.status)">{{ detail.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="解决时限">{{ formatDate(detail.deadline) }}</el-descriptions-item>
          <el-descriptions-item label="进度">{{ detail.progress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="解决方案">{{ detail.solution || '-' }}</el-descriptions-item>
          <el-descriptions-item label="实际完成时间">{{ formatDateTime(detail.actualCompletionTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.rejectReason" label="驳回原因">{{ detail.rejectReason }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin: 20px 0 12px">操作日志</h4>
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
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
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
