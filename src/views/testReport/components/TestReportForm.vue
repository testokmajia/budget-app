<script setup>
import { reactive, ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { create, getDefaultReviewers, uploadTestDocFile } from '@/api/testReport'
import { getList, getFilterOptions } from '@/api/requirement'

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['update:visible', 'success'])

const submitting = ref(false)
const formRef = ref(null)
const reqOptions = ref([])
const reqOptionsLoading = ref(false)
const allUsers = ref([])

const form = reactive({
  title: '',
  reqIds: [],
  systemNames: [],
  plannedTestDate: '',
  plannedProductionDate: '',
  testReportPath: '',
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
    // 也加载实施中和测试通过的需求
    const res2 = await getList({ status: '实施中', size: 100 })
    const inProgress = res2.data?.content || []
    const res3 = await getList({ status: '测试通过', size: 100 })
    const passed = res3.data?.content || []
    // 合并去重
    const all = [...confirmed, ...inProgress, ...passed]
    const seen = new Set()
    reqOptions.value = all.filter(r => {
      if (seen.has(r.id)) return false
      seen.add(r.id)
      return true
    })
  } catch (e) { /* ignore */ } finally { reqOptionsLoading.value = false }
}

// 加载用户列表
async function loadUsers() {
  try {
    const res = await getFilterOptions()
    allUsers.value = res.data?.submitters || []
  } catch (e) { /* ignore */ }
}

const userNames = computed(() => allUsers.value.map(u => u.name).filter(Boolean))

// 选择需求后自动带出涉及系统
function onReqsChange(ids) {
  if (!ids || ids.length === 0) { form.systemNames = []; return }
  const systems = new Set()
  ids.forEach(id => {
    const req = reqOptions.value.find(r => r.id === id)
    if (req?.systemItems) {
      req.systemItems.forEach(s => s.name && systems.add(s.name))
    }
  })
  form.systemNames = [...systems]
  loadDefaultReviewers(ids)
}

async function loadDefaultReviewers(ids) {
  if (!ids || ids.length === 0) return
  try {
    const res = await getDefaultReviewers(JSON.stringify(ids))
    form.reviewPersons = res.data || []
  } catch (e) { /* ignore */ }
}

function addReviewer() {
  form.reviewPersons.push({ name: '', role: '', system: '', confirmed: false, confirmedAt: null })
}
function removeReviewer(i) { form.reviewPersons.splice(i, 1) }

async function handleTestDocUpload(options) {
  try {
    const res = await uploadTestDocFile(options.file)
    const existing = form.testReportPath ? form.testReportPath.split('、') : []
    existing.push(res.data.filePath)
    form.testReportPath = existing.join('、')
    ElMessage.success('上传成功')
  } catch (e) { ElMessage.error(e.response?.data?.error || '上传失败') }
}

async function handleSubmit() {
  if (!form.title.trim()) { ElMessage.warning('请输入测试报告标题'); return }
  if (!form.reqIds.length) { ElMessage.warning('请选择关联需求'); return }
  if (!form.reviewPersons.length) { ElMessage.warning('请设置确认人员'); return }
  if (form.reviewPersons.some(r => !r.name)) { ElMessage.warning('请完善确认人员姓名'); return }

  submitting.value = true
  try {
    const selectedReqs = reqOptions.value.filter(r => form.reqIds.includes(r.id))
    await create({
      title: form.title,
      requirementIds: JSON.stringify(form.reqIds),
      requirementCodes: selectedReqs.map(r => r.requirementCode).join('、'),
      systemNames: JSON.stringify(form.systemNames),
      reviewPersons: JSON.stringify(form.reviewPersons),
      testReportPath: form.testReportPath || undefined,
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
  form.plannedTestDate = ''; form.plannedProductionDate = ''; form.testReportPath = ''
}

function handleClose() { emit('update:visible', false); resetForm() }

watch(() => props.visible, (val) => { if (val) { searchRequirements(''); loadUsers() } })
</script>

<template>
  <el-dialog
    :model-value="visible" title="新建测试报告" width="750px"
    :close-on-click-modal="false" destroy-on-close
    @update:model-value="handleClose"
  >
    <el-form ref="formRef" :model="form" label-width="100px">
      <el-row :gutter="20">
        <el-col :span="14">
          <el-form-item label="报告标题" required>
            <el-input v-model="form.title" placeholder="请输入测试报告标题" maxlength="200" />
          </el-form-item>
        </el-col>
        <el-col :span="10">
          <el-form-item label="测试报告附件">
            <div>
              <el-upload :http-request="handleTestDocUpload" :limit="5" :on-exceed="() => ElMessage.warning('最多上传5个文件')">
                <el-button type="primary" plain>选择文件</el-button>
                <template #tip>
                  <div style="font-size: 12px; color: #909399; margin-top: 6px">支持 PDF/Word/图片，单文件≤20MB</div>
                </template>
              </el-upload>
              <span v-if="form.testReportPath" style="font-size:12px;color:#909399">{{ form.testReportPath.split('/').pop() }}</span>
            </div>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="计划测试时间">
            <el-date-picker v-model="form.plannedTestDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="计划投产时间">
            <el-date-picker v-model="form.plannedProductionDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>

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

      <el-form-item label="信息系统">
        <el-select v-model="form.systemNames" multiple filterable allow-create default-first-option
          placeholder="选择后自动带出，可手动添加（可选）" style="width:100%"
        >
          <el-option v-for="s in form.systemNames" :key="s" :label="s" :value="s" />
        </el-select>
      </el-form-item>

      <el-form-item label="确认人员" required>
        <div class="reviewer-list">
          <div v-for="(r, i) in form.reviewPersons" :key="i" class="reviewer-row-edit">
            <el-select v-model="r.name" filterable placeholder="姓名" size="small" style="width:110px">
              <el-option v-for="n in userNames" :key="n" :label="n" :value="n" />
            </el-select>
            <el-select v-model="r.role" placeholder="角色" size="small" style="width:120px">
              <el-option label="项目经理" value="项目经理" />
              <el-option label="产品经理" value="产品经理" />
              <el-option label="团队负责人" value="团队负责人" />
              <el-option label="业务人员" value="业务人员" />
              <el-option label="部门负责人" value="部门负责人" />
              <el-option label="系统负责人" value="系统负责人" />
            </el-select>
            <el-select v-model="r.system" filterable allow-create default-first-option
              clearable placeholder="系统(可选)" size="small" style="width:130px">
              <el-option v-for="s in form.systemNames" :key="s" :label="s" :value="s" />
            </el-select>
            <el-button type="danger" size="small" circle @click="removeReviewer(i)">×</el-button>
          </div>
          <el-button size="small" style="margin-top:6px" @click="addReviewer">+ 手动增加确认人员</el-button>
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
