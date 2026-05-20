<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Edit, Delete, Check } from '@element-plus/icons-vue'
import { getList, create, update, complete, remove } from '@/api/checklist'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const editId = ref(null)

const searchForm = reactive({
  status: '',
  keyword: '',
  responsiblePerson: '',
  startDate: '',
  endDate: '',
})

const sourceOptions = [
  { label: '领导交办', value: '领导交办' },
  { label: '会议部署', value: '会议部署' },
  { label: '自行安排', value: '自行安排' },
  { label: '其他', value: '其他' },
]

const statusOptions = [
  { label: '待办', value: '待办' },
  { label: '进行中', value: '进行中' },
  { label: '已完成', value: '已完成' },
]

const form = reactive({
  source: '',
  sourceDetail: '',
  description: '',
  status: '待办',
  responsiblePerson: '',
  plannedDate: '',
  actualDate: '',
  remark: '',
})

const rules = {
  source: [{ required: true, message: '请选择事件来源', trigger: 'change' }],
  description: [{ required: true, message: '请输入事项描述', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

// 来源详情占位提示
const sourceDetailPlaceholder = computed(() => {
  const map = {
    '领导交办': '请输入领导姓名',
    '会议部署': '请输入会议名称',
    '自行安排': '',
    '其他': '请输入来源说明',
  }
  return map[form.source] || ''
})
const showSourceDetail = computed(() => {
  return form.source && form.source !== '自行安排'
})

async function fetchData() {
  loading.value = true
  try {
    const params = {}
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.responsiblePerson) params.responsiblePerson = searchForm.responsiblePerson
    if (searchForm.startDate) params.startDate = searchForm.startDate
    if (searchForm.endDate) params.endDate = searchForm.endDate
    const res = await getList(params)
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    source: '',
    sourceDetail: '',
    description: '',
    status: '待办',
    responsiblePerson: '',
    plannedDate: '',
    actualDate: '',
    remark: '',
  })
}

function handleAdd() {
  dialogTitle.value = '新建清单'
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑清单'
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    source: row.source,
    sourceDetail: row.sourceDetail || '',
    description: row.description,
    status: row.status,
    responsiblePerson: row.responsiblePerson || '',
    plannedDate: row.plannedDate || '',
    actualDate: row.actualDate || '',
    remark: row.remark || '',
  })
  dialogVisible.value = true
}

async function handleComplete(row) {
  await ElMessageBox.confirm(
    `确定将「${row.description.substring(0, 30)}」标记为已完成吗？`,
    '提示',
    { type: 'warning' }
  )
  await complete(row.id)
  ElMessage.success('已标记为完成')
  fetchData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该清单吗？', '提示', { type: 'warning' })
  await remove(row.id)
  ElMessage.success('已删除')
  fetchData()
}

async function handleSubmit() {
  const data = {
    ...form,
    sourceDetail: showSourceDetail.value ? form.sourceDetail : '',
    plannedDate: form.plannedDate || null,
    actualDate: form.status === '已完成' ? form.actualDate || new Date().toISOString().split('T')[0] : null,
  }
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

function getStatusType(status) {
  return { '待办': '', '进行中': 'warning', '已完成': 'success' }[status] || ''
}

onMounted(fetchData)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>清单管理</h2>
      <el-button type="primary" :icon="Plus" @click="handleAdd">新建清单</el-button>
    </div>

    <div class="search-bar">
      <el-select v-model="searchForm.status" placeholder="事项状态" clearable style="width: 120px" @change="fetchData">
        <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-input v-model="searchForm.responsiblePerson" placeholder="责任人" clearable style="width: 120px" @input="fetchData" />
      <el-date-picker v-model="searchForm.startDate" type="date" placeholder="完成日期起始" value-format="YYYY-MM-DD" style="width: 150px" @change="fetchData" />
      <el-date-picker v-model="searchForm.endDate" type="date" placeholder="完成日期截止" value-format="YYYY-MM-DD" style="width: 150px" @change="fetchData" />
      <el-input v-model="searchForm.keyword" placeholder="搜索关键词" clearable style="width: 200px" @input="fetchData">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border>
      <el-table-column prop="source" label="事件来源" width="100" />
      <el-table-column label="来源详情" width="120" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.sourceDetail">{{ row.sourceDetail }}</span>
          <span v-else style="color: #c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="事项描述" min-width="180" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="responsiblePerson" label="责任人" width="90" />
      <el-table-column prop="plannedDate" label="计划完成" width="110" />
      <el-table-column prop="actualDate" label="实际完成" width="110" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status !== '已完成'" type="success" link :icon="Check" @click="handleComplete(row)">完成</el-button>
          <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" @keyup.enter="handleSubmit">
      <el-form :model="form" :rules="rules" label-width="100px">
        <el-form-item label="事件来源" prop="source">
          <el-select v-model="form.source" style="width: 100%">
            <el-option v-for="o in sourceOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="showSourceDetail" label="来源详情">
          <el-input v-model="form.sourceDetail" :placeholder="sourceDetailPlaceholder" maxlength="200" />
        </el-form-item>
        <el-form-item label="事项描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="事项状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任人">
          <el-input v-model="form.responsiblePerson" />
        </el-form-item>
        <el-form-item label="计划完成时间">
          <el-date-picker v-model="form.plannedDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="实际完成时间">
          <el-date-picker v-model="form.actualDate" type="date" placeholder="状态为已完成时填写" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
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
  flex-wrap: wrap;
  align-items: center;
}
</style>
