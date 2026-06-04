<script setup>
import { reactive, ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { create, uploadTestDocFile, getDefaultReviewers } from '@/api/testReport'
import { getList, getFilterOptions, getSystems } from '@/api/requirement'

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['update:visible', 'success'])

const submitting = ref(false)
const formRef = ref(null)
const reqOptions = ref([])
const reqOptionsLoading = ref(false)
const allUsers = ref([])
const systemOptions = ref([])
const uploadLoading = ref(false)
const reviewersLoading = ref(false)

const roleOptions = ['项目经理', '产品经理', '业务提出人', '系统负责人', '部门负责人', '开发负责人', '测试负责人']

const form = reactive({
  title: '',
  reqIds: [],
  systemNames: [],
  plannedTestDate: '',
  plannedProductionDate: '',
  testReportPath: '',
  testReportName: '',
  reviewPersons: [],
})

// 远程搜索需求
async function searchRequirements(keyword) {
  reqOptionsLoading.value = true
  try {
    const params = { size: 100 }
    if (keyword) params.keyword = keyword
    else params.status = '需求已确认'
    const res = await getList(params)
    const confirmed = res.data?.content || []
    const res2 = await getList({ status: '实施中', size: 100 })
    const inProgress = res2.data?.content || []
    const res3 = await getList({ status: '测试通过', size: 100 })
    const passed = res3.data?.content || []
    const all = [...confirmed, ...inProgress, ...passed]
    const seen = new Set()
    reqOptions.value = all.filter(r => {
      if (seen.has(r.id)) return false
      seen.add(r.id)
      return true
    })
  } catch (e) { /* ignore */ } finally { reqOptionsLoading.value = false }
}

// 加载用户列表和系统列表
async function loadUsers() {
  try {
    const res = await getFilterOptions()
    allUsers.value = res.data?.submitters || []
  } catch (e) { /* ignore */ }
}
async function loadSystems() {
  try {
    const res = await getSystems()
    systemOptions.value = res.data || []
  } catch (e) { /* ignore */ }
}

const userNames = computed(() => allUsers.value.map(u => u.name).filter(Boolean))

// 确认人员操作
function addReviewer() {
  form.reviewPersons.push({ name: '', role: '项目经理', system: '', confirmed: false, confirmedAt: null })
}

function removeReviewer(index) {
  form.reviewPersons.splice(index, 1)
}

// 选择需求后自动带出涉及系统、计划时间、标题、确认人员
async function onReqsChange(ids) {
  if (!ids || ids.length === 0) {
    form.systemNames = []
    form.plannedTestDate = ''
    form.plannedProductionDate = ''
    form.title = ''
    form.reviewPersons = []
    return
  }
  const systems = new Set()
  let firstReq = null
  ids.forEach(id => {
    const req = reqOptions.value.find(r => r.id === id)
    if (req) {
      if (!firstReq) firstReq = req
      if (req.systemItems) {
        req.systemItems.forEach(s => s.name && systems.add(s.name))
      }
    }
  })
  form.systemNames = [...systems]
  if (firstReq) {
    form.plannedTestDate = firstReq.plannedTestDate || ''
    form.plannedProductionDate = firstReq.plannedProductionDate || ''
    form.title = firstReq.title + '测试报告'
  }

  // 自动获取确认人员
  await fetchDefaultReviewers(ids)
}

// 调用后端接口获取默认确认人员
async function fetchDefaultReviewers(ids) {
  reviewersLoading.value = true
  try {
    const res = await getDefaultReviewers(JSON.stringify(ids))
    const list = res.data || []
    if (list.length > 0) {
      form.reviewPersons = list.map(r => ({
        name: r.name || '',
        role: r.role || '项目经理',
        system: r.system || '',
        confirmed: false,
        confirmedAt: null,
      }))
    } else {
      form.reviewPersons = []
    }
  } catch (e) {
    form.reviewPersons = []
  } finally {
    reviewersLoading.value = false
  }
}

async function handleTestDocUpload(options) {
  uploadLoading.value = true
  try {
    const res = await uploadTestDocFile(options.file)
    form.testReportPath = res.data.filePath
    form.testReportName = res.data.fileName
    if (!form.title.trim()) {
      form.title = res.data.fileName.replace(/\.[^.]+$/, '') + '测试报告'
    }
    ElMessage.success('上传成功')
  } catch (e) { ElMessage.error(e.response?.data?.error || '上传失败') } finally { uploadLoading.value = false }
}

async function handleSubmit() {
  if (!form.title.trim()) { ElMessage.warning('请输入测试报告标题'); return }
  if (!form.reqIds.length) { ElMessage.warning('请选择关联需求'); return }

  submitting.value = true
  try {
    const selectedReqs = reqOptions.value.filter(r => form.reqIds.includes(r.id))
    // 过滤掉姓名为空的确认人员
    const validReviewers = form.reviewPersons.filter(r => r.name && r.name.trim())
    await create({
      title: form.title,
      requirementIds: JSON.stringify(form.reqIds),
      requirementCodes: selectedReqs.map(r => r.requirementCode).join('、'),
      systemNames: JSON.stringify(form.systemNames),
      reviewPersons: JSON.stringify(validReviewers),
      testReportPath: form.testReportPath || undefined,
      testReportName: form.testReportName || undefined,
      plannedTestDate: form.plannedTestDate || undefined,
      plannedProductionDate: form.plannedProductionDate || undefined,
    })
    ElMessage.success('测试报告已创建')
    emit('success')
    resetForm()
  } catch (e) { ElMessage.error(e.response?.data?.error || '创建失败') } finally { submitting.value = false }
}

function resetForm() {
  form.title = ''; form.reqIds = []; form.systemNames = []; form.reviewPersons = []
  form.plannedTestDate = ''; form.plannedProductionDate = ''; form.testReportPath = ''; form.testReportName = ''
}

function handleClose() { emit('update:visible', false); resetForm() }

watch(() => props.visible, (val) => { if (val) { searchRequirements(''); loadUsers(); loadSystems() } })
</script>

<template>
  <el-dialog
    :model-value="visible" title="新建测试报告" width="750px"
    :close-on-click-modal="false" destroy-on-close
    @update:model-value="handleClose"
  >
    <el-form ref="formRef" :model="form" label-width="100px">
      <!-- 选择需求 -->
      <el-form-item label="关联需求" required>
        <el-select
          v-model="form.reqIds" multiple filterable remote
          :remote-method="searchRequirements"
          :loading="reqOptionsLoading"
          placeholder="输入关键词搜索需求（可多选）"
          style="width:100%"
          @change="onReqsChange"
        >
          <el-option v-for="r in reqOptions" :key="r.id"
            :label="r.requirementCode + ' ' + r.title" :value="r.id" />
        </el-select>
      </el-form-item>

      <el-row :gutter="20">
        <el-col :span="14">
          <el-form-item label="报告标题" required>
            <el-input v-model="form.title" placeholder="选择需求后自动生成，可修改" maxlength="200" />
          </el-form-item>
        </el-col>
        <el-col :span="10">
          <el-form-item label="计划测试时间">
            <el-date-picker v-model="form.plannedTestDate" type="date" placeholder="自动获取" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="计划投产时间">
            <el-date-picker v-model="form.plannedProductionDate" type="date" placeholder="自动获取" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="信息系统">
            <el-select v-model="form.systemNames" multiple filterable
              placeholder="选择后自动带出" style="width:100%"
            >
              <el-option v-for="s in systemOptions" :key="s.code" :label="s.name" :value="s.name" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 确认人员 -->
      <el-form-item label="确认人员">
        <div style="width:100%">
          <div v-if="reviewersLoading" style="padding:8px;color:#909399;font-size:13px">正在根据需求自动生成确认人员…</div>
          <div v-else-if="form.reviewPersons.length === 0" style="padding:8px;color:#909399;font-size:13px">
            暂无确认人员，选择需求后自动生成，也可手动添加
          </div>
          <div v-else class="reviewer-list">
            <div v-for="(r, index) in form.reviewPersons" :key="index" class="reviewer-row-edit">
              <el-autocomplete
                v-model="r.name"
                :fetch-suggestions="(kw, cb) => cb(userNames.filter(n => n.includes(kw)).map(n => ({ value: n })))"
                placeholder="姓名"
                style="width:100px"
                size="small"
              />
              <el-select v-model="r.role" placeholder="角色" style="width:110px" size="small">
                <el-option v-for="ro in roleOptions" :key="ro" :label="ro" :value="ro" />
              </el-select>
              <el-input v-model="r.system" placeholder="所属系统" style="width:120px" size="small" />
              <el-button type="danger" :icon="'Delete'" circle size="small" @click="removeReviewer(index)" />
            </div>
          </div>
          <el-button type="primary" link size="small" style="margin-top:6px" @click="addReviewer">
            + 添加确认人员
          </el-button>
        </div>
      </el-form-item>

      <!-- 附件 -->
      <el-form-item label="测试报告附件">
        <div>
          <el-upload :http-request="handleTestDocUpload" :limit="5" :on-exceed="() => ElMessage.warning('最多上传5个文件')">
            <el-button type="primary" plain :loading="uploadLoading">选择文件</el-button>
            <template #tip>
              <div style="font-size: 12px; color: #909399; margin-top: 6px">支持 PDF/Word/图片，单文件≤20MB</div>
            </template>
          </el-upload>
          <span v-if="form.testReportName" style="font-size:12px;color:#006eff;margin-left:8px">{{ form.testReportName }}</span>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">创建测试报告</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.reviewer-list { padding: 8px; background: #fafafa; border-radius: 4px; border: 1px solid #ebeef5; }
.reviewer-row-edit { display: flex; gap: 6px; align-items: center; margin-bottom: 6px; }
.reviewer-row-edit:last-child { margin-bottom: 0; }
</style>
