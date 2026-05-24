<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Upload, ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getList, create, update, remove, uploadFile } from '@/api/reward'
import { getDepartments, getUsers } from '@/api/admin'

const userStore = useUserStore()
const loading = ref(false)
const tableData = ref([])
const departments = ref([])
const allUsers = ref([])

const canEdit = computed(() => userStore.hasRole('ROLE_CLERK') || userStore.hasRole('ROLE_ADMIN'))

const searchForm = reactive({
  type: '',
  dateRange: [],
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const uploading = ref(false)
const form = reactive({
  type: '奖励',
  title: '',
  description: '',
  involvedPersonNames: [],
  department: userStore.user?.department || '',
  decisionDate: '',
  attachmentUrl: '',
})
const rules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入内容描述', trigger: 'blur' }],
  department: [{ required: true, message: '请选择部门', trigger: 'change' }],
  decisionDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
}

const selectedInvolvedNames = computed(() => {
  return form.involvedPersonNames.join(', ')
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getList({ type: searchForm.type || undefined })
    let data = res.data || []
    if (searchForm.dateRange?.length === 2) {
      const [start, end] = searchForm.dateRange
      data = data.filter(r => r.decisionDate >= start && r.decisionDate <= end)
    }
    tableData.value = data
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, {
    type: '奖励',
    title: '',
    description: '',
    involvedPersonNames: [],
    department: userStore.user?.department || '',
    decisionDate: '',
    attachmentUrl: '',
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  editId.value = row.id
  const names = row.involvedPerson ? row.involvedPerson.split(',').map(s => s.trim()).filter(Boolean) : []
  Object.assign(form, {
    type: row.type,
    title: row.title,
    description: row.description,
    involvedPersonNames: names,
    department: row.department,
    decisionDate: row.decisionDate,
    attachmentUrl: row.attachmentUrl || '',
  })
  dialogVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该记录吗？', '提示', { type: 'warning' })
  await remove(row.id)
  ElMessage.success('已删除')
  fetchData()
}

async function handleUpload(options) {
  uploading.value = true
  try {
    const res = await uploadFile(options.file)
    form.attachmentUrl = res.data.filePath
    ElMessage.success('上传成功')
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

function handleRemoveAttachment() {
  form.attachmentUrl = ''
}

async function handleSubmit() {
  if (!form.involvedPersonNames.length) {
    ElMessage.warning('请选择涉及人员')
    return
  }
  const data = {
    ...form,
    involvedPerson: form.involvedPersonNames.join(','),
    decisionDate: form.decisionDate || null,
  }
  delete data.involvedPersonNames
  if (isEdit.value) {
    await update(editId.value, data)
    ElMessage.success('已更新')
  } else {
    await create(data)
    ElMessage.success('已创建')
  }
  dialogVisible.value = false
  fetchData()
}

function getTypeTag(type) {
  return type === '奖励' ? 'success' : 'danger'
}

async function loadDepartments() {
  try {
    const res = await getDepartments()
    departments.value = res.data || []
  } catch { /* ignore */ }
}

async function loadUsers() {
  try {
    const res = await getUsers({ size: 9999, enabled: true })
    allUsers.value = res.data?.content || []
  } catch { /* ignore */ }
}

onMounted(() => {
  fetchData()
  loadDepartments()
  loadUsers()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>奖惩记录</h2>
      <el-button v-if="canEdit" type="primary" :icon="Plus" @click="handleAdd">新增记录</el-button>
    </div>

    <div class="search-bar">
      <el-select v-model="searchForm.type" placeholder="类型" clearable style="width: 120px" @change="fetchData">
        <el-option label="奖励" value="奖励" />
        <el-option label="惩罚" value="惩罚" />
      </el-select>
      <el-date-picker
        v-model="searchForm.dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 260px"
        @change="fetchData"
      />
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="type" label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="getTypeTag(row.type)">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
      <el-table-column prop="involvedPerson" label="涉及人员" width="140" />
      <el-table-column prop="department" label="部门" width="140" />
      <el-table-column prop="decisionDate" label="决定日期" width="110" />
      <el-table-column label="附件" width="80">
        <template #default="{ row }">
          <a v-if="row.attachmentUrl" :href="row.attachmentUrl" target="_blank">查看</a>
          <span v-else style="color: #c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="creatorName" label="录入人" width="90" />
      <el-table-column v-if="canEdit" label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑记录' : '新增记录'" width="580px">
      <el-form :model="form" :rules="rules" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类型" prop="type">
              <el-select v-model="form.type" style="width: 100%">
                <el-option label="奖励" value="奖励" />
                <el-option label="惩罚" value="惩罚" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="决定日期" prop="decisionDate">
              <el-date-picker v-model="form.decisionDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="200" />
        </el-form-item>
        <el-form-item label="内容描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="涉及人员" required>
              <el-select
                v-model="form.involvedPersonNames"
                style="width: 100%"
                multiple
                filterable
                placeholder="请选择涉及人员"
                collapse-tags
                collapse-tags-tooltip
              >
                <el-option v-for="u in allUsers" :key="u.id" :label="u.name" :value="u.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门" prop="department">
              <el-select v-model="form.department" style="width: 100%" clearable filterable placeholder="请选择部门">
                <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.name" :disabled="!d.enabled" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="附件">
          <div v-if="form.attachmentUrl" class="attachment-preview">
            <span class="attachment-path">{{ form.attachmentUrl }}</span>
            <el-button type="danger" link size="small" @click="handleRemoveAttachment">删除</el-button>
          </div>
          <el-upload
            v-else
            :auto-upload="false"
            :show-file-list="false"
            :http-request="handleUpload"
            accept="*"
          >
            <el-button :loading="uploading" :icon="Upload">选择文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
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
  align-items: center;
}

.attachment-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #f5f7fa;
  border-radius: 4px;
}
.attachment-path {
  font-size: 13px;
  color: #409eff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
</style>
