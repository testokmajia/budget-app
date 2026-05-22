<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getUsers, createUser, updateUser, toggleUser, resetPassword, getRoles, getCategories, createCategory, updateCategory, deleteCategory, getDepartments, createDepartment, updateDepartment, deleteDepartment, getSystems, createSystem, updateSystem, deleteSystem, getTeams, createTeam, updateTeam, deleteTeam } from '@/api/admin'

const activeTab = ref('users')

// === 用户管理 ===
const users = ref([])
const allUsers = ref([])
const userTotal = ref(0)
const userPage = ref(1)
const userSize = ref(20)
const roles = ref([])

const userFilters = reactive({
  username: '',
  name: '',
  department: '',
  enabled: null,
})

const createDialogVisible = ref(false)
const createForm = reactive({ username: '', password: '', name: '', department: '', position: '', phone: '' })
const createRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 3, max: 50, message: '3-50个字符', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '至少6位', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
}
const createFormRef = ref(null)

const editDialogVisible = ref(false)
const editForm = reactive({ name: '', department: '', position: '', phone: '', enabled: true, roleIds: [] })
const editUserId = ref(null)
const editRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
}

const pwdDialogVisible = ref(false)
const pwdTargetUser = ref(null)
const newPassword = ref('')

async function fetchUsers() {
  const params = {
    page: userPage.value - 1,
    size: userSize.value,
  }
  if (userFilters.username) params.username = userFilters.username
  if (userFilters.name) params.name = userFilters.name
  if (userFilters.department) params.department = userFilters.department
  if (userFilters.enabled !== null && userFilters.enabled !== '') params.enabled = userFilters.enabled
  const res = await getUsers(params)
  users.value = res.data?.content || []
  userTotal.value = res.data?.totalElements || 0
}

function handleUserFilter() {
  userPage.value = 1
  fetchUsers()
}

function handleUserPageChange(page) {
  userPage.value = page
  fetchUsers()
}

function handleUserSizeChange(size) {
  userSize.value = size
  userPage.value = 1
  fetchUsers()
}
async function fetchRoles() {
  const res = await getRoles()
  roles.value = res.data || []
}

async function fetchAllUsers() {
  const res = await getUsers({ page: 0, size: 9999, enabled: true })
  allUsers.value = res.data?.content || []
}

function handleAddUser() {
  Object.assign(createForm, { username: '', password: '', name: '', department: '', position: '', phone: '' })
  createDialogVisible.value = true
}
async function handleCreateSubmit() {
  await createFormRef.value.validate()
  await createUser(createForm)
  ElMessage.success('用户已创建')
  createDialogVisible.value = false
  fetchUsers()
}

function handleEdit(row) {
  editUserId.value = row.id
  Object.assign(editForm, {
    name: row.name,
    department: row.department || '',
    position: row.position || '',
    phone: row.phone || '',
    enabled: row.enabled,
    roleIds: row.roles.map(r => r.id),
  })
  editDialogVisible.value = true
}
async function handleEditSubmit() {
  await updateUser(editUserId.value, editForm)
  ElMessage.success('用户信息已更新')
  editDialogVisible.value = false
  fetchUsers()
}

async function handleToggle(user) {
  await ElMessageBox.confirm(
    `确定${user.enabled ? '禁用' : '启用'}用户「${user.name}」吗？`,
    '提示',
    { type: 'warning' }
  )
  await toggleUser(user.id)
  ElMessage.success('操作成功')
  fetchUsers()
}

function handleResetPwd(user) {
  pwdTargetUser.value = user
  newPassword.value = ''
  pwdDialogVisible.value = true
}
async function handlePwdSubmit() {
  if (!newPassword.value || newPassword.value.length < 6) {
    ElMessage.warning('密码至少6位')
    return
  }
  await resetPassword(pwdTargetUser.value.id, newPassword.value)
  ElMessage.success('密码已重置')
  pwdDialogVisible.value = false
}

// === 问题分类 ===
const categories = ref([])
const categoryDialogVisible = ref(false)
const categoryForm = reactive({ name: '', sortOrder: 0, enabled: true })
const isCategoryEdit = ref(false)
const editCategoryId = ref(null)

async function fetchCategories() {
  const res = await getCategories()
  categories.value = res.data || []
}
function handleAddCategory() {
  isCategoryEdit.value = false
  editCategoryId.value = null
  Object.assign(categoryForm, { name: '', sortOrder: 0, enabled: true })
  categoryDialogVisible.value = true
}
function handleEditCategory(row) {
  isCategoryEdit.value = true
  editCategoryId.value = row.id
  Object.assign(categoryForm, { name: row.name, sortOrder: row.sortOrder, enabled: row.enabled })
  categoryDialogVisible.value = true
}
async function handleDeleteCategory(row) {
  await ElMessageBox.confirm('确定删除该分类吗？', '提示', { type: 'warning' })
  await deleteCategory(row.id)
  ElMessage.success('已删除')
  fetchCategories()
}
async function handleCategorySubmit() {
  if (isCategoryEdit.value) {
    await updateCategory(editCategoryId.value, categoryForm)
    ElMessage.success('已更新')
  } else {
    await createCategory(categoryForm)
    ElMessage.success('已创建')
  }
  categoryDialogVisible.value = false
  fetchCategories()
}

// === 部门管理 ===
const departments = ref([])
const departmentDialogVisible = ref(false)
const departmentForm = reactive({ name: '', leader: '', enabled: true })
const isDeptEdit = ref(false)
const editDeptId = ref(null)

async function fetchDepartments() {
  const res = await getDepartments()
  departments.value = res.data || []
}
function handleAddDepartment() {
  isDeptEdit.value = false
  editDeptId.value = null
  Object.assign(departmentForm, { name: '', leader: '', enabled: true })
  departmentDialogVisible.value = true
}
function handleEditDepartment(row) {
  isDeptEdit.value = true
  editDeptId.value = row.id
  Object.assign(departmentForm, { name: row.name, leader: row.leader || '', enabled: row.enabled })
  departmentDialogVisible.value = true
}
async function handleDeleteDepartment(row) {
  await ElMessageBox.confirm('确定删除该部门吗？', '提示', { type: 'warning' })
  await deleteDepartment(row.id)
  ElMessage.success('已删除')
  fetchDepartments()
}
async function handleDepartmentSubmit() {
  if (isDeptEdit.value) {
    await updateDepartment(editDeptId.value, departmentForm)
    ElMessage.success('已更新')
  } else {
    await createDepartment(departmentForm)
    ElMessage.success('已创建')
  }
  departmentDialogVisible.value = false
  fetchDepartments()
}

// === 所属系统管理 ===
const systems = ref([])
const systemDialogVisible = ref(false)
const systemForm = reactive({ name: '', leader: '', team: '', enabled: true })
const isSysEdit = ref(false)
const editSysId = ref(null)

async function fetchSystems() {
  const res = await getSystems()
  systems.value = res.data || []
}
function handleAddSystem() {
  isSysEdit.value = false
  editSysId.value = null
  Object.assign(systemForm, { name: '', leader: '', team: '', enabled: true })
  systemDialogVisible.value = true
}
function handleEditSystem(row) {
  isSysEdit.value = true
  editSysId.value = row.id
  Object.assign(systemForm, { name: row.name, leader: row.leader || '', team: row.team || '', enabled: row.enabled })
  systemDialogVisible.value = true
}
async function handleDeleteSystem(row) {
  await ElMessageBox.confirm('确定删除该系统吗？', '提示', { type: 'warning' })
  await deleteSystem(row.id)
  ElMessage.success('已删除')
  fetchSystems()
}
async function handleSystemSubmit() {
  if (isSysEdit.value) {
    await updateSystem(editSysId.value, systemForm)
    ElMessage.success('已更新')
  } else {
    await createSystem(systemForm)
    ElMessage.success('已创建')
  }
  systemDialogVisible.value = false
  fetchSystems()
}

// === 团队管理 ===
const teams = ref([])
const teamDialogVisible = ref(false)
const teamForm = reactive({ name: '', department: '', leader: '', members: [], enabled: true })

const filteredMembers = computed(() => {
  if (!teamForm.department) return allUsers.value
  return allUsers.value.filter(u => u.department === teamForm.department)
})
const isTeamEdit = ref(false)
const editTeamId = ref(null)

async function fetchTeams() {
  const res = await getTeams()
  teams.value = res.data || []
}
function handleAddTeam() {
  isTeamEdit.value = false
  editTeamId.value = null
  Object.assign(teamForm, { name: '', department: '', leader: '', members: [], enabled: true })
  teamDialogVisible.value = true
}
function handleEditTeam(row) {
  isTeamEdit.value = true
  editTeamId.value = row.id
  const memberList = row.members ? row.members.split(',').map(s => s.trim()).filter(Boolean) : []
  Object.assign(teamForm, { name: row.name, department: row.department || '', leader: row.leader || '', members: memberList, enabled: row.enabled })
  teamDialogVisible.value = true
}
async function handleDeleteTeam(row) {
  await ElMessageBox.confirm('确定删除该团队吗？', '提示', { type: 'warning' })
  await deleteTeam(row.id)
  ElMessage.success('已删除')
  fetchTeams()
}
async function handleTeamSubmit() {
  const payload = { ...teamForm, members: teamForm.members.join(',') }
  if (isTeamEdit.value) {
    await updateTeam(editTeamId.value, payload)
    ElMessage.success('已更新')
  } else {
    await createTeam(payload)
    ElMessage.success('已创建')
  }
  teamDialogVisible.value = false
  fetchTeams()
}

onMounted(() => {
  fetchUsers()
  fetchAllUsers()
  fetchRoles()
  fetchCategories()
  fetchDepartments()
  fetchSystems()
  fetchTeams()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>系统管理</h2></div>

    <el-tabs v-model="activeTab">
      <!-- 用户管理 -->
      <el-tab-pane label="用户管理" name="users">
        <div class="user-toolbar">
          <el-button type="primary" :icon="Plus" @click="handleAddUser">新增用户</el-button>
          <div class="user-filters">
            <el-input v-model="userFilters.username" placeholder="用户名" clearable style="width: 140px" @keyup.enter="handleUserFilter" @clear="handleUserFilter" />
            <el-input v-model="userFilters.name" placeholder="姓名" clearable style="width: 120px" @keyup.enter="handleUserFilter" @clear="handleUserFilter" />
            <el-select v-model="userFilters.department" clearable filterable placeholder="部门" style="width: 180px" @change="handleUserFilter" @clear="handleUserFilter">
              <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.name" />
            </el-select>
            <el-select v-model="userFilters.enabled" clearable placeholder="状态" style="width: 100px" @change="handleUserFilter" @clear="handleUserFilter">
              <el-option label="启用" :value="true" />
              <el-option label="禁用" :value="false" />
            </el-select>
          </div>
        </div>
        <el-table :data="users" stripe border style="width: 100%">
          <el-table-column prop="username" label="用户名" min-width="140" />
          <el-table-column prop="name" label="姓名" min-width="100" />
          <el-table-column prop="department" label="部门" min-width="160" />

          <el-table-column prop="phone" label="电话" min-width="130" />
          <el-table-column label="角色" min-width="160">
            <template #default="{ row }">
              <el-tag v-for="r in row.roles" :key="r.id" size="small" style="margin-right: 4px">
                {{ r.name }}
              </el-tag>
              <span v-if="!row.roles.length" style="color: #c0c4cc">未分配</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
                {{ row.enabled ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
              <el-button type="primary" link @click="handleResetPwd(row)">重置密码</el-button>
              <el-button
                :type="row.enabled ? 'warning' : 'success'"
                link
                @click="handleToggle(row)"
              >
                {{ row.enabled ? '禁用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="user-pagination">
          <el-pagination
            v-model:current-page="userPage"
            v-model:page-size="userSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="userTotal"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="handleUserPageChange"
            @size-change="handleUserSizeChange"
          />
        </div>
      </el-tab-pane>

      <!-- 问题分类 -->
      <el-tab-pane label="问题分类" name="categories">
        <div style="margin-bottom: 16px">
          <el-button type="primary" :icon="Plus" @click="handleAddCategory">新增分类</el-button>
        </div>
        <el-table :data="categories" stripe border>
          <el-table-column prop="name" label="分类名称" width="200" />
          <el-table-column prop="sortOrder" label="排序" width="80" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                {{ row.enabled ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button type="primary" link :icon="Edit" @click="handleEditCategory(row)">编辑</el-button>
              <el-button type="danger" link :icon="Delete" @click="handleDeleteCategory(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 部门管理 -->
      <el-tab-pane label="部门管理" name="departments">
        <div style="margin-bottom: 16px">
          <el-button type="primary" :icon="Plus" @click="handleAddDepartment">新增部门</el-button>
        </div>
        <el-table :data="departments" stripe border>
          <el-table-column prop="name" label="部门名称" width="200" />
          <el-table-column prop="leader" label="部门负责人" width="120" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                {{ row.enabled ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button type="primary" link :icon="Edit" @click="handleEditDepartment(row)">编辑</el-button>
              <el-button type="danger" link :icon="Delete" @click="handleDeleteDepartment(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 信息系统管理 -->
      <el-tab-pane label="信息系统" name="systems">
        <div style="margin-bottom: 16px">
          <el-button type="primary" :icon="Plus" @click="handleAddSystem">新增系统</el-button>
        </div>
        <el-table :data="systems" stripe border>
          <el-table-column prop="name" label="系统名称" width="180" />
          <el-table-column prop="leader" label="系统负责人" width="120" />
          <el-table-column prop="team" label="所属团队" width="120" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                {{ row.enabled ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button type="primary" link :icon="Edit" @click="handleEditSystem(row)">编辑</el-button>
              <el-button type="danger" link :icon="Delete" @click="handleDeleteSystem(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 团队管理 -->
      <el-tab-pane label="团队管理" name="teams">
        <div style="margin-bottom: 16px">
          <el-button type="primary" :icon="Plus" @click="handleAddTeam">新增团队</el-button>
        </div>
        <el-table :data="teams" stripe border>
          <el-table-column prop="name" label="团队名称" width="160" />
          <el-table-column prop="department" label="所属部门" width="140" />
          <el-table-column prop="leader" label="团队负责人" width="120" />
          <el-table-column prop="members" label="团队成员" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                {{ row.enabled ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button type="primary" link :icon="Edit" @click="handleEditTeam(row)">编辑</el-button>
              <el-button type="danger" link :icon="Delete" @click="handleDeleteTeam(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增用户弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新增用户" width="480px" @keyup.enter="handleCreateSubmit">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" maxlength="50" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password maxlength="100" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="createForm.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="createForm.department" style="width: 100%" clearable filterable placeholder="请选择部门">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.name" :disabled="!d.enabled" />
          </el-select>
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="createForm.position" maxlength="50" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="createForm.phone" maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑用户弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑用户" width="520px" @keyup.enter="handleEditSubmit">
      <el-form :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="editForm.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="editForm.department" style="width: 100%" clearable filterable placeholder="请选择部门">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.name" :disabled="!d.enabled" />
          </el-select>
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="editForm.position" maxlength="50" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="editForm.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="editForm.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="角色">
          <el-checkbox-group v-model="editForm.roleIds">
            <el-checkbox v-for="r in roles" :key="r.id" :label="r.id" :value="r.id">
              {{ r.name }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="420px" @keyup.enter="handlePwdSubmit">
      <p style="margin-bottom: 12px">用户：{{ pwdTargetUser?.name }}（{{ pwdTargetUser?.username }}）</p>
      <el-input v-model="newPassword" type="password" show-password placeholder="输入新密码（至少6位）" maxlength="100" />
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePwdSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分类弹窗 -->
    <el-dialog v-model="categoryDialogVisible" :title="isCategoryEdit ? '编辑分类' : '新增分类'" width="480px">
      <el-form :model="categoryForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="categoryForm.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="categoryForm.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCategorySubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 部门弹窗 -->
    <el-dialog v-model="departmentDialogVisible" :title="isDeptEdit ? '编辑部门' : '新增部门'" width="480px">
      <el-form :model="departmentForm" label-width="100px">
        <el-form-item label="部门名称">
          <el-input v-model="departmentForm.name" />
        </el-form-item>
        <el-form-item label="部门负责人">
          <el-select v-model="departmentForm.leader" style="width: 100%" clearable filterable placeholder="请选择负责人">
            <el-option v-for="u in allUsers" :key="u.id" :label="u.name" :value="u.name" :disabled="!u.enabled" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="departmentForm.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="departmentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDepartmentSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 系统弹窗 -->
    <el-dialog v-model="systemDialogVisible" :title="isSysEdit ? '编辑系统' : '新增系统'" width="480px">
      <el-form :model="systemForm" label-width="100px">
        <el-form-item label="系统名称">
          <el-input v-model="systemForm.name" />
        </el-form-item>
        <el-form-item label="系统负责人">
          <el-select v-model="systemForm.leader" style="width: 100%" clearable filterable placeholder="请选择负责人">
            <el-option v-for="u in allUsers" :key="u.id" :label="u.name" :value="u.name" :disabled="!u.enabled" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属团队">
          <el-select v-model="systemForm.team" style="width: 100%" clearable filterable placeholder="请选择团队">
            <el-option v-for="t in teams" :key="t.id" :label="t.name" :value="t.name" :disabled="!t.enabled" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="systemForm.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="systemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSystemSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 团队弹窗 -->
    <el-dialog v-model="teamDialogVisible" :title="isTeamEdit ? '编辑团队' : '新增团队'" width="520px">
      <el-form :model="teamForm" label-width="100px">
        <el-form-item label="团队名称">
          <el-input v-model="teamForm.name" />
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="teamForm.department" style="width: 100%" clearable filterable placeholder="请选择所属部门" @change="teamForm.members = []">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.name" :disabled="!d.enabled" />
          </el-select>
        </el-form-item>
        <el-form-item label="团队负责人">
          <el-select v-model="teamForm.leader" style="width: 100%" clearable filterable placeholder="请选择负责人">
            <el-option v-for="u in filteredMembers" :key="u.id" :label="u.name" :value="u.name" :disabled="!u.enabled" />
          </el-select>
        </el-form-item>
        <el-form-item label="团队成员">
          <el-select v-model="teamForm.members" style="width: 100%" multiple filterable placeholder="请选择团队成员">
            <el-option v-for="u in filteredMembers" :key="u.id" :label="u.name" :value="u.name" :disabled="!u.enabled" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="teamForm.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="teamDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTeamSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.user-filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.user-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
