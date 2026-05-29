<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Upload } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getList, create, update, remove, uploadFile } from '@/api/reward'
import { getDepartments, getUsers } from '@/api/admin'

const userStore = useUserStore()
const loading = ref(false)
const tableData = ref([])
const departments = ref([])
const allUsers = ref([])

const canEdit = computed(() => userStore.hasRole('ROLE_CLERK') || userStore.hasRole('ROLE_ADMIN'))

const detailVisible = ref(false)
const detailRow = ref(null)

function showDetail(row) {
  detailRow.value = row
  detailVisible.value = true
}

const searchForm = reactive({
  type: '',
  department: '',
  keyword: '',
  dateRange: [],
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const uploading = ref(false)
const form = reactive({
  type: '奖励',
  involvedPersonNames: [],
  title: '',
  description: '',
  department: userStore.user?.department || '',
  decisionDate: '',
  attachmentUrl: '',
  attachmentFileName: '',
})
const rules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  involvedPersonNames: [{ type: 'array', required: true, message: '请选择涉及人员', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入内容描述', trigger: 'blur' }],
  department: [{ required: true, message: '请选择部门', trigger: 'change' }],
  decisionDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
}

async function fetchData() {
  loading.value = true
  try {
    const params = {}
    if (searchForm.type) params.type = searchForm.type
    if (searchForm.department) params.department = searchForm.department
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.dateRange?.length === 2) {
      params.dateFrom = searchForm.dateRange[0]
      params.dateTo = searchForm.dateRange[1]
    }
    const res = await getList(params)
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, {
    type: '奖励',
    involvedPersonNames: [],
    title: '',
    description: '',
    department: userStore.user?.department || '',
    decisionDate: '',
    attachmentUrl: '',
    attachmentFileName: '',
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    type: row.type,
    involvedPersonNames: row.involvedPerson ? [row.involvedPerson] : [],
    title: row.title,
    description: row.description,
    department: row.department,
    decisionDate: row.decisionDate,
    attachmentUrl: row.attachmentUrl || '',
    attachmentFileName: row.attachmentFileName || '',
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
    form.attachmentFileName = res.data.fileName
    ElMessage.success('上传成功')
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

function handleRemoveAttachment() {
  form.attachmentUrl = ''
  form.attachmentFileName = ''
}

async function handleSubmit() {
  if (!form.involvedPersonNames.length) {
    ElMessage.warning('请选择涉及人员')
    return
  }
  const data = { ...form, decisionDate: form.decisionDate || null }
  if (isEdit.value) {
    data.involvedPersonNames = [form.involvedPersonNames[0] || form.involvedPersonNames]
    await update(editId.value, data)
    ElMessage.success('已更新')
  } else {
    await create(data)
    ElMessage.success(`已创建 ${data.involvedPersonNames.length} 条记录`)
  }
  dialogVisible.value = false
  fetchData()
}

function getTypeTag(type) {
  return type === '奖励' ? 'success' : 'danger'
}

function truncateFileName(name) {
  if (!name) return '查看'
  return name.length > 20 ? name.slice(0, 20) + '...' : name
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
      <el-button v-if="canEdit" type="primary" :icon="Plus" @click="handleAdd">新增记录</el-button>
    </div>

    <div class="search-bar">
      <el-select v-model="searchForm.type" placeholder="类型" clearable style="width: 100px" @change="fetchData">
        <el-option label="奖励" value="奖励" />
        <el-option label="惩罚" value="惩罚" />
      </el-select>
      <el-select v-model="searchForm.department" placeholder="部门" clearable style="width: 140px" @change="fetchData">
        <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.name" />
      </el-select>
      <el-input v-model="searchForm.keyword" placeholder="搜索标题/内容" clearable style="width: 180px" @input="fetchData" />
      <el-date-picker
        v-model="searchForm.dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 240px"
        @change="fetchData"
      />
      <el-button @click="Object.assign(searchForm, { type: '', department: '', keyword: '', dateRange: [] }); fetchData()">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="involvedPerson" label="涉及人员" width="100" />
      <el-table-column prop="type" label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="getTypeTag(row.type)">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标题" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <a class="title-link" @click.prevent="showDetail(row)">{{ row.title }}</a>
        </template>
      </el-table-column>
      <el-table-column prop="department" label="部门" width="120" />
      <el-table-column prop="decisionDate" label="决定日期" width="110" />
      <el-table-column label="附件" width="180">
        <template #default="{ row }">
          <a v-if="row.attachmentUrl" :href="row.attachmentUrl" target="_blank" class="attachment-link" :title="row.attachmentFileName || ''">{{ truncateFileName(row.attachmentFileName) }}</a>
          <span v-else style="color: #c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="creatorName" label="录入人" width="90" />
      <el-table-column v-if="canEdit" label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <div class="action-btns">
            <el-button type="primary" size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑记录' : '新增记录'" width="580px">
      <el-form :model="form" :rules="rules" label-width="80px">
        <el-form-item label="涉及人员" prop="involvedPersonNames" required>
          <el-select
            v-model="form.involvedPersonNames"
            style="width: 100%"
            :multiple="!isEdit"
            filterable
            placeholder="请选择涉及人员"
            collapse-tags
            collapse-tags-tooltip
          >
            <el-option v-for="u in allUsers" :key="u.id" :label="u.name" :value="u.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="奖励" value="奖励" />
            <el-option label="惩罚" value="惩罚" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="200" />
        </el-form-item>
        <el-form-item label="内容描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="部门" prop="department">
              <el-select v-model="form.department" style="width: 100%" clearable filterable placeholder="请选择部门">
                <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.name" :disabled="!d.enabled" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="决定日期" prop="decisionDate">
              <el-date-picker v-model="form.decisionDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="附件">
          <div v-if="form.attachmentUrl" class="attachment-preview">
            <a :href="form.attachmentUrl" target="_blank" class="attachment-name">{{ form.attachmentFileName || form.attachmentUrl }}</a>
            <el-button type="danger" link size="small" @click="handleRemoveAttachment">删除</el-button>
          </div>
          <el-upload
            v-else
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

    <!-- Detail dialog -->
    <el-dialog v-model="detailVisible" title="奖惩详情" width="580px">
      <template v-if="detailRow">
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">涉及人员</span>
            <span class="detail-value">{{ detailRow.involvedPerson }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">类型</span>
            <el-tag :type="getTypeTag(detailRow.type)" size="small">{{ detailRow.type }}</el-tag>
          </div>
          <div class="detail-item">
            <span class="detail-label">部门</span>
            <span class="detail-value">{{ detailRow.department }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">决定日期</span>
            <span class="detail-value">{{ detailRow.decisionDate }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">录入人</span>
            <span class="detail-value">{{ detailRow.creatorName }}</span>
          </div>
          <div class="detail-item" v-if="detailRow.createdAt">
            <span class="detail-label">录入时间</span>
            <span class="detail-value">{{ detailRow.createdAt?.substring(0, 16)?.replace('T', ' ') }}</span>
          </div>
        </div>
        <div class="detail-block">
          <div class="detail-label">标题</div>
          <div class="detail-value" style="font-weight: 600; margin-top: 4px;">{{ detailRow.title }}</div>
        </div>
        <div class="detail-block">
          <div class="detail-label">内容描述</div>
          <div class="detail-value detail-desc">{{ detailRow.description }}</div>
        </div>
        <div class="detail-block" v-if="detailRow.attachmentUrl">
          <div class="detail-label">附件</div>
          <a :href="detailRow.attachmentUrl" target="_blank" class="attachment-link">{{ detailRow.attachmentFileName || detailRow.attachmentUrl }}</a>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.action-btns {
  display: flex;
  gap: 4px;
  flex-wrap: nowrap;
  align-items: center;
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

.attachment-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #f5f7fa;
  border-radius: 4px;
}
.attachment-name {
  font-size: 13px;
  color: #409eff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.attachment-link {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
  max-width: 100%;
}

.title-link {
  color: #303133;
  cursor: pointer;
  text-decoration: none;
}
.title-link:hover {
  color: #1890ff;
  text-decoration: underline;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 24px;
  margin-bottom: 16px;
}
.detail-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.detail-label {
  font-size: 13px;
  color: rgba(0,0,0,0.45);
  flex-shrink: 0;
}
.detail-value {
  font-size: 14px;
  color: rgba(0,0,0,0.85);
}
.detail-block {
  margin-bottom: 16px;
}
.detail-desc {
  margin-top: 4px;
  padding: 12px;
  background: #fafafa;
  border-radius: 4px;
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>
