<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { create, uploadRequestFile, getFilterOptions } from '@/api/requirement'

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['update:visible', 'success'])

const userStore = useUserStore()
const formRef = ref(null)
const submitting = ref(false)
const userOptions = ref([])

const form = reactive({
  title: '',
  content: '',
  priority: '普通',
  expectedDate: '',
  specDocumentPath: '',
  specDocumentName: '',
  submitterId: null,
  dept: '',
})

const rules = {
  title: [{ required: true, message: '请输入需求标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入需求内容', trigger: 'blur' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
}

async function loadUsers() {
  try {
    const res = await getFilterOptions()
    userOptions.value = res.data?.submitters || []
  } catch (e) { /* ignore */ }
}

function onSubmitterChange(userId) {
  const user = userOptions.value.find(u => u.id === userId)
  form.dept = user?.department || ''
}

async function handleUpload(options) {
  try {
    const res = await uploadRequestFile(options.file)
    form.specDocumentPath = res.data.filePath
    form.specDocumentName = res.data.fileName
    ElMessage.success('上传成功')
  } catch (e) { ElMessage.error(e.response?.data?.error || '上传失败') }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await create({
      title: form.title,
      content: form.content,
      priority: form.priority,
      expectedDate: form.expectedDate || undefined,
      specDocumentPath: form.specDocumentPath || undefined,
      specDocumentName: form.specDocumentName || undefined,
      submitterId: form.submitterId || undefined,
    })
    ElMessage.success('需求已提交')
    emit('success')
    resetForm()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '提交失败')
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  form.title = ''
  form.content = ''
  form.priority = '普通'
  form.expectedDate = ''
  form.specDocumentPath = ''
  form.specDocumentName = ''
  form.submitterId = null
  form.dept = ''
  formRef.value?.resetFields()
}

function handleClose() {
  emit('update:visible', false)
  resetForm()
}

watch(() => props.visible, (val) => {
  if (val) {
    loadUsers()
    form.submitterId = userStore.user?.id || null
    form.dept = userStore.user?.department || ''
  }
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="新建需求"
    width="600px"
    :close-on-click-modal="false"
    @update:model-value="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-row :gutter="20">
        <el-col :span="14">
          <el-form-item label="需求标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入需求标题" maxlength="200" show-word-limit />
          </el-form-item>
        </el-col>
        <el-col :span="10">
          <el-form-item label="优先级" prop="priority">
            <el-select v-model="form.priority" style="width: 100%">
              <el-option label="普通" value="普通" />
              <el-option label="高" value="高" />
              <el-option label="紧急" value="紧急" />
              <el-option label="低" value="低" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="需求内容" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="5"
          placeholder="请详细描述需求内容…"
          maxlength="5000"
          show-word-limit
        />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="提出人">
            <el-select v-model="form.submitterId" filterable placeholder="选择提出人" style="width:100%" @change="onSubmitterChange">
              <el-option v-for="u in userOptions" :key="u.id" :label="u.name" :value="u.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="提出部门">
            <el-input :model-value="form.dept" disabled placeholder="选择提出人后自动带出" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="期望完成">
        <el-date-picker
          v-model="form.expectedDate"
          type="date"
          placeholder="选择期望完成时间"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="需求说明书">
        <div>
          <el-upload :http-request="handleUpload" :limit="1" :on-exceed="() => ElMessage.warning('仅支持上传一个文件')">
            <el-button type="primary" plain>选择文件</el-button>
            <template #tip>
              <div style="font-size: 12px; color: #909399; margin-top: 6px">支持 PDF/Word/图片，单文件≤20MB</div>
            </template>
          </el-upload>
          <span v-if="form.specDocumentPath" style="font-size:12px;color:#006eff">{{ form.specDocumentName || form.specDocumentPath }}</span>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">提交需求</el-button>
    </template>
  </el-dialog>
</template>
