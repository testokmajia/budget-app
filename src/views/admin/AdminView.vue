<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getUsers, createUser, updateUser, toggleUser, resetPassword, getRoles, getCategories, createCategory, updateCategory, deleteCategory } from '@/api/admin'

const activeTab = ref('users')

// === 用户管理 ===
const users = ref([])
const roles = ref([])

// 新增用户
const createDialogVisible = ref(false)
const createForm = reactive({ username: '', password: '', name: '', department: '', position: '', phone: '' })
const createRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 3, max: 50, message: '3-50个字符', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '至少6位', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
}
const createFormRef = ref(null)

// 编辑用户
const editDialogVisible = ref(false)
const editForm = reactive({ name: '', department: '', position: '', phone: '', enabled: true, roleIds: [] })
const editUserId = ref(null)
const editRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
}

// 重置密码
const pwdDialogVisible = ref(false)
const pwdTargetUser = ref(null)
const newPassword = ref('')

async function fetchUsers() {
  const res = await getUsers()
  users.value = res.data || []
}
async function fetchRoles() {
  const res = await getRoles()
  roles.value = res.data || []
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

onMounted(() => {
  fetchUsers()
  fetchRoles()
  fetchCategories()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>系统管理</h2></div>

    <el-tabs v-model="activeTab">
      <!-- 用户管理 -->
      <el-tab-pane label="用户管理" name="users">
        <div style="margin-bottom: 16px">
          <el-button type="primary" :icon="Plus" @click="handleAddUser">新增用户</el-button>
        </div>
        <el-table :data="users" stripe border>
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="department" label="部门" width="140" />
          <el-table-column prop="position" label="职位" width="100" />
          <el-table-column prop="phone" label="电话" width="130" />
          <el-table-column label="角色" min-width="180">
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
          <el-input v-model="createForm.department" maxlength="100" />
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
          <el-input v-model="editForm.department" maxlength="100" />
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
  </div>
</template>
