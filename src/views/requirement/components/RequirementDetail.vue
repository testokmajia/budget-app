<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getById, approve, deptApprove, reject, evaluate, teamLeaderAssign,
  uploadSpec, confirmSpec, resubmit, getSystems, getDefaultReviewers,
  startImplementation, setProduction, closeRequirement, uploadRequestFile
} from '@/api/requirement'
import { getByRequirement } from '@/api/testReport'
import { nameEquals, nameInList, findByName } from '@/utils/compare'

const props = defineProps({ visible: Boolean, id: Number })
const emit = defineEmits(['update:visible', 'close'])

const userStore = useUserStore()
const req = ref(null)
const loading = ref(false)
const actionLoading = ref(false)
const commentText = ref('')
const systemOptions = ref([])
const allUsers = ref([])
const myTeamName = ref('')
const myTeamNames = ref([])
const myDeptName = ref('')
const isDeptLeader = ref(false)

// 架构管理岗
const evalSystems = ref([{ name: '', team: '', owner: '', modification: '' }])
const primarySystem = ref('')

// 产品经理 - 需求说明书
const specReviewers = ref([])
const specDocPath = ref('')
const specDocName = ref('')
const specFileList = ref([])

// 架构管理岗 - 架构评审报告
const reviewReportPath = ref('')
const reviewReportName = ref('')
const reviewFileList = ref([])

// 确认后操作
const implVisible = ref(false)
const implForm = reactive({ plannedTestDate: '', plannedProductionDate: '' })
const productionVisible = ref(false)
const productionDate = ref('')

// 关联测试报告
const relatedReports = ref([])

// 审批节点（7节点）
const nodes = [
  { name: '提出人', role: '提交需求' },
  { name: '部门负责人', role: '部门审批' },
  { name: '架构管理岗', role: '评估涉及系统' },
  { name: '团队组长', role: '指派PM和PD' },
  { name: '产品经理', role: '上传需求说明书' },
  { name: '多方确认', role: '确认需求说明书' },
  { name: '项目经理', role: '启动实施' },
]

const statusTagMap = {
  '草稿': 'info', '审批中': 'primary', '已驳回': 'danger', '需求已确认': 'success',
  '实施中': 'warning', '测试通过': 'success', '已投产': '', '已关闭': 'info'
}

const systemItemsData = ref([])

// 用户姓名列表（用于PM/PD下拉）
const userOptions = computed(() => {
  return allUsers.value.map(u => u.name).filter(Boolean)
})

// 审批记录按节点分组
const commentsByNode = computed(() => {
  if (!req.value?.comments) return {}
  const map = {}
  req.value.comments.forEach(c => {
    const nodeKey = findByName(nodes, c.role)?.name || c.role
    if (!map[nodeKey]) map[nodeKey] = []
    map[nodeKey].push(c)
  })
  return map
})

// 当前用户可编辑的系统（团队组长节点时只显示自己团队的系统）
const editableSystems = computed(() => {
  if (req.value?.currentNode !== '团队组长') return systemItemsData.value
  return systemItemsData.value.filter(s => isMyTeam(s.team))
})

// 判断系统团队是否属于当前用户负责的团队之一
function isMyTeam(sysTeam) {
  if (!sysTeam) return false
  if (myTeamNames.value.length > 0) {
    return myTeamNames.value.some(t => nameEquals(sysTeam, t))
  }
  // 兼容单团队字段
  if (myTeamName.value) return nameEquals(sysTeam, myTeamName.value)
  return true // 未加载完成时默认显示
}

// 权限判断：当前用户是否有权操作当前节点
const isAdmin = computed(() => userStore.hasRole('ROLE_ADMIN'))
const isArchitect = computed(() => userStore.hasRole('ROLE_ARCHITECT'))
const canActAsDeptLeader = computed(() => {
  if (!req.value || req.value.currentNode !== '部门负责人') return false
  return isAdmin.value || (isDeptLeader.value && req.value.dept === myDeptName.value)
})
const canActAsArchitect = computed(() => {
  if (!req.value || req.value.currentNode !== '架构管理岗') return false
  return isArchitect.value || isAdmin.value
})
const canActAsTeamLeader = computed(() => {
  if (!req.value || req.value.currentNode !== '团队组长') return false
  return editableSystems.value.length > 0
})

function showActions() {
  if (!req.value) return false
  const noActionStatuses = ['草稿', '需求已确认', '实施中', '测试通过', '已投产', '已关闭']
  if (noActionStatuses.includes(req.value.status)) return false
  return true
}

// ==================== 系统选择 ====================
function onSysSelect(index, name) {
  const sys = findByName(systemOptions.value, name)
  if (sys) {
    evalSystems.value[index].team = sys.team || ''
    evalSystems.value[index].owner = sys.leader || ''
  } else {
    evalSystems.value[index].team = ''
    evalSystems.value[index].owner = ''
  }
}
function addEvalSystem() { evalSystems.value.push({ name: '', team: '', owner: '', modification: '' }) }
function removeEvalSystem(i) { evalSystems.value.splice(i, 1) }

// ==================== 操作 ====================

async function doApprove() {
  actionLoading.value = true
  try {
    await approve(props.id, { comment: commentText.value })
    ElMessage.success('审批通过')
    setTimeout(() => emit('update:visible', false), 800)
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { actionLoading.value = false; commentText.value = '' }
}

async function doDeptApprove() {
  actionLoading.value = true
  try {
    await deptApprove(props.id, { comment: commentText.value })
    ElMessage.success('部门审批通过')
    setTimeout(() => emit('update:visible', false), 800)
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { actionLoading.value = false; commentText.value = '' }
}

async function doReject() {
  if (!commentText.value.trim()) { ElMessage.warning('请填写驳回意见'); return }
  actionLoading.value = true
  try {
    await reject(props.id, { comment: commentText.value })
    ElMessage.success('已驳回')
    setTimeout(() => emit('update:visible', false), 800)
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { actionLoading.value = false; commentText.value = '' }
}

async function doEvaluate() {
  const sys = evalSystems.value.filter(s => s.name)
  if (sys.length === 0) { ElMessage.warning('请至少添加一个涉及系统'); return }
  if (!primarySystem.value) { ElMessage.warning('请指定主责系统'); return }
  actionLoading.value = true
  try {
    await evaluate(props.id, {
      systemItems: JSON.stringify(sys),
      primarySystemName: primarySystem.value,
      comment: commentText.value,
      reviewReportPath: reviewReportPath.value || undefined,
      reviewReportName: reviewReportName.value || undefined,
    })
    ElMessage.success('评估已提交')
    setTimeout(() => emit('update:visible', false), 800)
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { actionLoading.value = false; commentText.value = '' }
}

async function doTeamAssign() {
  const unassignedPM = editableSystems.value.filter(s => !s.pm)
  const unassignedPD = editableSystems.value.filter(s => !s.pd)
  if (unassignedPM.length > 0) { ElMessage.warning('请为所有涉及系统指派项目经理（默认已选系统负责人）'); return }
  if (unassignedPD.length > 0) { ElMessage.warning('请为所有涉及系统指派产品经理'); return }
  actionLoading.value = true
  try {
    await teamLeaderAssign(props.id, {
      systemItems: JSON.stringify(systemItemsData.value),
      comment: commentText.value,
    })
    ElMessage.success('指派完成')
    emit('update:visible', false)
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { actionLoading.value = false }
}

// 上传附件（产品经理用）
const uploadLoading = ref(false)
async function handleSpecUpload(options) {
  uploadLoading.value = true
  try {
    const res = await uploadRequestFile(options.file)
    specDocPath.value = res.data.filePath
    specDocName.value = res.data.fileName
    ElMessage.success('上传成功')
  } catch (e) { ElMessage.error(e.response?.data?.error || '上传失败') } finally { uploadLoading.value = false }
}

// 上传架构评审报告（架构管理岗用）
const reviewUploadLoading = ref(false)
async function handleReviewUpload(options) {
  reviewUploadLoading.value = true
  try {
    const res = await uploadRequestFile(options.file)
    reviewReportPath.value = res.data.filePath
    reviewReportName.value = res.data.fileName
    ElMessage.success('架构评审报告上传成功')
  } catch (e) { ElMessage.error(e.response?.data?.error || '上传失败') } finally { reviewUploadLoading.value = false }
}

async function doUploadSpec() {
  actionLoading.value = true
  try {
    // 如果上传了文件，使用上传后的路径；否则保留已有路径
    const docPath = specDocPath.value || req.value?.specDocumentPath || ''
    if (!docPath) { ElMessage.warning('请上传需求说明书'); return }
    await uploadSpec(props.id, {
      specDocumentPath: docPath,
      specDocumentName: specDocName.value || req.value?.specDocumentName || '',
      specReviewers: JSON.stringify(specReviewers.value),
      comment: commentText.value,
    })
    ElMessage.success('需求说明书已提交')
    await reload()
    // 自动关闭弹窗
    setTimeout(() => emit('update:visible', false), 800)
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { actionLoading.value = false; commentText.value = '' }
}

async function doConfirmSpec() {
  actionLoading.value = true
  try {
    await confirmSpec(props.id, { comment: commentText.value || '确认需求说明书' })
    ElMessage.success('确认完成')
    setTimeout(() => emit('update:visible', false), 800)
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { actionLoading.value = false; commentText.value = '' }
}

// ==================== 确认后操作 ====================

function openImplDialog() {
  implForm.plannedTestDate = req.value?.plannedTestDate || ''
  implForm.plannedProductionDate = req.value?.plannedProductionDate || ''
  implVisible.value = true
}

async function doStartImpl() {
  if (!implForm.plannedTestDate || !implForm.plannedProductionDate) {
    ElMessage.warning('请填写计划测试时间和计划投产时间'); return
  }
  actionLoading.value = true
  try {
    await startImplementation(props.id, {
      plannedTestDate: implForm.plannedTestDate,
      plannedProductionDate: implForm.plannedProductionDate,
    })
    ElMessage.success('实施已启动')
    implVisible.value = false
    setTimeout(() => emit('update:visible', false), 800)
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { actionLoading.value = false }
}

function openProductionDialog() {
  productionDate.value = req.value?.productionDate || ''
  productionVisible.value = true
}

async function doSetProduction() {
  if (!productionDate.value) { ElMessage.warning('请填写正式投产日期'); return }
  actionLoading.value = true
  try {
    await setProduction(props.id, { productionDate: productionDate.value })
    ElMessage.success('投产日期已填写')
    productionVisible.value = false
    setTimeout(() => emit('update:visible', false), 800)
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { actionLoading.value = false }
}

async function doCloseReq() {
  actionLoading.value = true
  try {
    await closeRequirement(props.id)
    ElMessage.success('需求已关闭')
    setTimeout(() => emit('update:visible', false), 800)
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { actionLoading.value = false }
}

async function loadRelatedReports() {
  if (!props.id) return
  try {
    const res = await getByRequirement(props.id)
    relatedReports.value = res.data || []
  } catch (e) { /* ignore */ }
}

async function doResubmit() {
  if (!commentText.value.trim()) { ElMessage.warning('请说明修改内容'); return }
  actionLoading.value = true
  try {
    await resubmit(props.id, { comment: commentText.value })
    ElMessage.success('已重新提交')
    await reload()
  } catch (e) { ElMessage.error(e.response?.data?.error || '操作失败') } finally { actionLoading.value = false; commentText.value = '' }
}

async function loadReviewers() {
  if (!props.id) return
  try {
    const res = await getDefaultReviewers(props.id)
    specReviewers.value = res.data || []
  } catch (e) { /* ignore */ }
}
function addReviewer() { specReviewers.value.push({ name: '', role: '', system: '' }) }
function removeReviewer(i) { specReviewers.value.splice(i, 1) }

// ==================== 加载数据 ====================

async function loadData() {
  if (!props.id) return
  loading.value = true
  try {
    // 重置状态，防止上一个需求的旧数据残留
    systemItemsData.value = []
    specReviewers.value = []
    primarySystem.value = ''
    evalSystems.value = [{ name: '', team: '', owner: '', modification: '' }]
    specDocPath.value = ''
    specDocName.value = ''
    reviewReportPath.value = ''
    reviewReportName.value = ''

    const res = await getById(props.id)
    req.value = res.data
    if (req.value) {
      // 初始化系统项，团队组长节点时PM默认为系统负责人
      if (req.value.systemItems?.length) {
        systemItemsData.value = req.value.systemItems.map(s => ({
          name: s.name, team: s.team, owner: s.owner, modification: s.modification,
          pm: s.pm || (req.value.currentNode === '团队组长' ? s.owner : ''),
          pd: s.pd || ''
        }))
      }
      primarySystem.value = req.value.primarySystemName || ''
      if (req.value.specReviewers) {
        try { specReviewers.value = JSON.parse(req.value.specReviewers) } catch { specReviewers.value = [] }
      }
      if (req.value.reviewReportPath) reviewReportPath.value = req.value.reviewReportPath
      if (req.value.reviewReportName) reviewReportName.value = req.value.reviewReportName
      if (req.value.currentNode === '产品经理' && specReviewers.value.length === 0) {
        await loadReviewers()
      }
    }
  } catch (e) { ElMessage.error('加载需求详情失败') } finally { loading.value = false }
}

async function loadSystems() {
  try { const res = await getSystems(); systemOptions.value = res.data } catch (e) { /* ignore */ }
}

async function loadUsers() {
  try {
    const { getFilterOptions } = await import('@/api/requirement')
    const res = await getFilterOptions()
    allUsers.value = res.data?.submitters || []
    myTeamName.value = res.data?.myTeamName || ''
    myTeamNames.value = res.data?.myTeamNames || []
    myDeptName.value = res.data?.myDeptName || ''
    isDeptLeader.value = res.data?.isDeptLeader || false
  } catch (e) { /* ignore */ }
}

async function reload() {
  await loadData()
  loadRelatedReports()
  loadUsers()
  commentText.value = ''
}

watch(() => props.id, (val) => {
  if (val) { loadData(); loadSystems(); loadRelatedReports(); loadUsers() }
})
// 每次打开弹窗时强制重新加载数据，防止缓存旧数据
watch(() => props.visible, (val) => {
  if (val && props.id) { loadData(); loadSystems(); loadRelatedReports(); loadUsers() }
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="req ? req.requirementCode + ' · ' + req.title : '需求详情'"
    width="1200px" top="3vh" :close-on-click-modal="false" destroy-on-close
    @update:model-value="emit('update:visible', $event)"
    @closed="emit('close')"
  >
    <div v-if="req" class="detail-layout" v-loading="loading">
      <!-- 左栏：主内容 -->
      <div class="detail-left">
        <!-- 基本信息 -->
        <el-descriptions title="基本信息" :column="3" border size="small">
          <el-descriptions-item label="需求标题" :span="2">{{ req.title }}</el-descriptions-item>
          <el-descriptions-item label="优先级">
            <span :class="'pri-' + req.priority">{{ req.priority }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="需求内容" :span="3">
            <div class="content-text">{{ req.content }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="提出人">{{ req.submitterName }}</el-descriptions-item>
          <el-descriptions-item label="提出部门">{{ req.dept }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagMap[req.status] || 'info'" size="small">{{ req.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="期望完成">{{ req.expectedDate || '—' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ req.createdAt?.substring(0, 10) }}</el-descriptions-item>
          <el-descriptions-item label="当前节点">{{ req.currentNode }}</el-descriptions-item>
          <el-descriptions-item v-if="req.plannedTestDate" label="计划测试">{{ req.plannedTestDate }}</el-descriptions-item>
          <el-descriptions-item v-if="req.plannedProductionDate" label="计划投产">{{ req.plannedProductionDate }}</el-descriptions-item>
          <el-descriptions-item v-if="req.productionDate" label="正式投产">{{ req.productionDate }}</el-descriptions-item>
        </el-descriptions>

        <!-- 附件区（需求说明书、架构评审报告、测试报告附件集中展示） -->
        <div v-if="req.specDocumentPath || req.reviewReportPath || req.attachmentPath || relatedReports.length" class="section-block">
          <h4 class="section-title">📎 需求附件</h4>
          <div class="attach-group">
            <!-- 需求说明书 -->
            <div v-if="req.specDocumentPath" class="attach-item">
              <span class="attach-icon">📄</span>
              <span class="attach-label">需求说明书：</span>
              <el-link type="primary" :href="`/api/files/download?path=${encodeURIComponent(req.specDocumentPath)}&name=${encodeURIComponent(req.specDocumentName || req.specDocumentPath.split('/').pop())}`" target="_blank">
                {{ req.specDocumentName || (req.specDocumentPath || '').split('/').pop() }}
              </el-link>
              <el-tag v-if="req.status === '需求已确认' || req.status === '实施中' || req.status === '测试通过' || req.status === '已投产' || req.status === '已关闭'" type="success" size="small" class="attach-status">已确认</el-tag>
              <el-tag v-else-if="req.currentNode === '多方确认'" type="primary" size="small" class="attach-status">确认中</el-tag>
              <el-tag v-else-if="req.currentNode === '产品经理'" type="warning" size="small" class="attach-status">待提交</el-tag>
              <el-tag v-else type="info" size="small" class="attach-status">待确认</el-tag>
            </div>
            <!-- 架构评审报告 -->
            <div v-if="req.reviewReportPath" class="attach-item">
              <span class="attach-icon">📎</span>
              <span class="attach-label">架构评审报告：</span>
              <el-link type="primary" :href="`/api/files/download?path=${encodeURIComponent(req.reviewReportPath)}&name=${encodeURIComponent(req.reviewReportName || req.reviewReportPath.split('/').pop())}`" target="_blank">
                {{ req.reviewReportName || (req.reviewReportPath || '').split('/').pop() }}
              </el-link>
              <el-tag type="success" size="small" class="attach-status">已上传</el-tag>
            </div>
            <!-- 通用附件 -->
            <div v-if="req.attachmentPath" class="attach-item">
              <span class="attach-icon">📎</span>
              <span class="attach-label">附件：</span>
              <el-link type="primary" :href="`/api/files/download?path=${encodeURIComponent(req.attachmentPath)}`" target="_blank">
                {{ (req.attachmentPath || '').split('/').pop() }}
              </el-link>
              <el-tag type="success" size="small" class="attach-status">已上传</el-tag>
            </div>
            <!-- 关联测试报告附件 -->
            <template v-for="tr in relatedReports" :key="'tra-' + tr.id">
              <div v-if="tr.testReportPath" class="attach-item">
                <span class="attach-icon">🧪</span>
                <span class="attach-label">测试报告 {{ tr.reportCode || '' }}（{{ tr.title }}）：</span>
                <el-link type="primary" :href="`/api/files/download?path=${encodeURIComponent(tr.testReportPath)}&name=${encodeURIComponent(tr.testReportName || tr.testReportPath.split('/').pop())}`" target="_blank">
                  {{ tr.testReportName || (tr.testReportPath || '').split('/').pop() }}
                </el-link>
                <el-tag :type="tr.status === '已确认' ? 'success' : 'info'" size="small" class="attach-status">{{ tr.status }}</el-tag>
              </div>
            </template>
          </div>
        </div>

        <!-- 涉及系统 + PM/PD 合并展示 -->
        <div v-if="systemItemsData.length && req.nodeIndex >= 2" class="section-block">
          <h4 class="section-title">涉及系统与指派</h4>
          <div class="sys-table">
            <div class="sys-table-header">
              <span class="st-col-name">系统</span>
              <span class="st-col-team">团队/负责人</span>
              <span class="st-col-mod">改造内容</span>
              <span class="st-col-pm">项目经理</span>
              <span class="st-col-pd">产品经理</span>
            </div>
            <div
              v-for="(sys, i) in systemItemsData"
              :key="i"
              class="sys-table-row"
              :class="{
                'is-primary': nameEquals(sys.name, req.primarySystemName),
                'is-other-team': req.currentNode === '团队组长' && myTeamNames.length > 0 && !isMyTeam(sys.team)
              }"
            >
              <span class="st-col-name">
                {{ sys.name }}
                <el-tag v-if="nameEquals(sys.name, req.primarySystemName)" type="danger" size="small" effect="dark" class="primary-tag">主责</el-tag>
              </span>
              <span class="st-col-team">{{ sys.team }} · {{ sys.owner }}</span>
              <span class="st-col-mod">{{ sys.modification }}</span>
              <span class="st-col-pm">
                <!-- 团队组长节点：同时可编辑PM和PD -->
                <template v-if="req.currentNode === '团队组长' && req.status === '审批中' && isMyTeam(sys.team)">
                  <el-select v-model="sys.pm" placeholder="选PM" size="small" style="width: 100%" filterable>
                    <el-option v-for="u in userOptions" :key="u" :label="u" :value="u" />
                  </el-select>
                </template>
                <template v-else>
                  <span :class="{ 'text-muted': !sys.pm }">{{ sys.pm || '未指派' }}</span>
                </template>
              </span>
              <span class="st-col-pd">
                <template v-if="req.currentNode === '团队组长' && req.status === '审批中' && isMyTeam(sys.team)">
                  <el-select v-model="sys.pd" placeholder="选PD" size="small" style="width: 100%" filterable>
                    <el-option v-for="u in userOptions" :key="u" :label="u" :value="u" />
                  </el-select>
                </template>
                <template v-else>
                  <span :class="{ 'text-muted': !sys.pd }">{{ sys.pd || '未指派' }}</span>
                </template>
              </span>
            </div>
          </div>
        </div>

        <!-- 确认详细信息（始终显示，不依赖当前节点） -->
        <div v-if="specReviewers.length" class="section-block">
          <h4 class="section-title">确认详细信息</h4>
          <div class="reviewer-table">
            <div class="reviewer-header">
              <span class="r-col-name">姓名</span>
              <span class="r-col-role">角色</span>
              <span class="r-col-sys">所属系统</span>
              <span class="r-col-status">确认状态</span>
              <span class="r-col-time">确认时间</span>
            </div>
            <div v-for="(r, i) in specReviewers" :key="i" class="reviewer-row">
              <span class="r-col-name">{{ r.name }}</span>
              <span class="r-col-role">{{ r.role }}</span>
              <span class="r-col-sys">{{ r.system || '—' }}</span>
              <span class="r-col-status">
                <el-tag v-if="r.confirmed" type="success" size="small">已确认</el-tag>
                <el-tag v-else type="info" size="small">待确认</el-tag>
              </span>
              <span class="r-col-time">{{ r.confirmedAt ? r.confirmedAt.substring(0, 16) : '—' }}</span>
            </div>
          </div>
          <p class="confirm-progress">
            确认进度：{{ specReviewers.filter(r => r.confirmed).length }}/{{ specReviewers.length }}
          </p>
        </div>

        <!-- 关联测试报告 -->
        <div v-if="relatedReports.length" class="section-block">
          <h4 class="section-title">关联测试报告</h4>
          <div v-for="tr in relatedReports" :key="tr.id" class="related-report-card">
            <div class="rr-header">
              <span class="rr-title">{{ tr.reportCode ? tr.reportCode + ' · ' : '' }}{{ tr.title }}</span>
              <el-tag :type="tr.status === '已确认' ? 'success' : 'info'" size="small">{{ tr.status }}</el-tag>
            </div>
            <div class="rr-meta">关联需求：{{ tr.requirementCodes }} | 计划测试：{{ tr.plannedTestDate || '—' }}</div>
            <div v-if="tr.testReportPath" class="rr-attach">
              <el-link type="primary" :href="`/api/files/download?path=${encodeURIComponent(tr.testReportPath)}&name=${encodeURIComponent(tr.testReportName || tr.testReportPath.split('/').pop())}`" target="_blank">
                📄 下载测试报告：{{ tr.testReportName || (tr.testReportPath || '').split('/').pop() }}
              </el-link>
            </div>
          </div>
        </div>

        <!-- 确认后操作 -->
        <div v-if="req.status === '需求已确认' || req.status === '实施中' || req.status === '测试通过' || req.status === '已投产'" class="section-block">
          <h4 class="section-title">操作</h4>
          <div class="post-confirm-actions">
            <div v-if="req.status === '需求已确认'">
              <el-button v-if="userStore.user?.name && nameInList(req.assignedPm, userStore.user.name)" type="primary" @click="openImplDialog">启动实施</el-button>
              <p v-else class="action-tip">等待项目经理（{{ req.assignedPm || '未指派' }}）启动实施</p>
            </div>
            <div v-else-if="req.status === '实施中'">
              <p class="action-tip">实施中，等待测试报告确认…</p>
            </div>
            <div v-else-if="req.status === '测试通过'">
              <el-button v-if="userStore.user?.name && nameInList(req.assignedPm, userStore.user.name)" type="primary" @click="openProductionDialog">填写正式投产日期</el-button>
              <p v-else class="action-tip">等待项目经理（{{ req.assignedPm || '未指派' }}）填写投产日期</p>
            </div>
            <div v-else-if="req.status === '已投产'">
              <el-button v-if="userStore.user?.name && nameEquals(userStore.user.name, req.submitterName)" type="success" @click="doCloseReq">确认关闭</el-button>
              <p v-else class="action-tip">等待提出人（{{ req.submitterName || '未知' }}）确认关闭</p>
            </div>
          </div>
        </div>

        <!-- 当前节点操作区 -->
        <div v-if="showActions() && req.currentNode !== '团队组长'" class="section-block">
          <h4 class="section-title">当前节点操作</h4>

          <!-- 部门负责人 -->
          <div v-if="canActAsDeptLeader" class="action-area">
            <p class="action-tip">作为部门负责人，审批该需求：</p>
            <el-input v-model="commentText" type="textarea" :rows="2" placeholder="审批意见（可选）" style="margin-bottom: 10px" />
            <div class="action-btns">
              <el-button type="danger" @click="doReject">驳回至提出人</el-button>
              <el-button type="primary" :loading="actionLoading" @click="doDeptApprove">审批通过</el-button>
            </div>
          </div>

          <!-- 架构管理岗 -->
          <div v-if="canActAsArchitect" class="action-area">
            <!-- 需求说明书展示 -->
            <div v-if="req.specDocumentPath" class="attach-view">
              <span class="attach-label">📄 需求说明书：</span>
              <el-link type="primary" :href="`/api/files/download?path=${encodeURIComponent(req.specDocumentPath)}&name=${encodeURIComponent(req.specDocumentName || req.specDocumentPath.split('/').pop())}`" target="_blank">
                {{ req.specDocumentName || req.specDocumentPath.split('/').pop() }}
              </el-link>
            </div>
            <p class="action-tip">选择涉及系统并指定主责系统：</p>
            <div v-for="(sys, i) in evalSystems" :key="i" class="eval-row-wrap">
              <div class="eval-row">
                <el-select v-model="sys.name" filterable allow-create default-first-option
                  placeholder="选择或输入系统名" style="width: 200px"
                  @change="onSysSelect(i, sys.name)">
                  <el-option v-for="s in systemOptions" :key="s.code" :label="s.name" :value="s.name" />
                </el-select>
                <el-radio-group v-model="primarySystem" style="margin-left: 8px">
                  <el-radio :value="sys.name" v-if="sys.name">主责</el-radio>
                </el-radio-group>
                <span class="eval-info">{{ sys.team ? sys.team + ' · ' + sys.owner : '← 选择后自动带出' }}</span>
                <el-button v-if="evalSystems.length > 1" type="danger" size="small" circle @click="removeEvalSystem(i)">×</el-button>
              </div>
              <el-input v-model="sys.modification" type="textarea" :rows="3" placeholder="改造内容描述…" style="margin-top: 6px" />
            </div>
            <el-button size="small" style="margin-top: 8px" @click="addEvalSystem">+ 新增涉及系统</el-button>
            <!-- 架构评审报告上传 -->
            <div style="margin-top: 12px">
              <span class="field-label">架构评审报告：</span>
              <el-upload :http-request="handleReviewUpload" :limit="1" :on-exceed="() => ElMessage.warning('仅支持上传一个文件')" :file-list="reviewFileList">
                <el-button type="primary" plain size="small" :loading="reviewUploadLoading">选择文件上传</el-button>
              </el-upload>
              <span v-if="reviewReportName" style="font-size:12px;color:#006eff;margin-left:8px">已选择：{{ reviewReportName }}</span>
              <span v-else style="font-size:12px;color:#a8abb2;margin-left:8px">文件名自动命名为"需求标题+架构预审报告"</span>
            </div>
            <el-input v-model="commentText" type="textarea" :rows="2" placeholder="评估意见（可选）" style="margin-top: 10px" />
            <div class="action-btns">
              <el-button type="danger" @click="doReject">驳回至提出人</el-button>
              <el-button type="primary" :loading="actionLoading" @click="doEvaluate">提交评估</el-button>
            </div>
          </div>

          <!-- 产品经理 -->
          <div v-else-if="req.currentNode === '产品经理' && userStore.user?.name && nameInList(req.assignedPd, userStore.user.name)" class="action-area">
            <p class="action-tip">上传/更新需求说明书并设置确认人员：</p>
            <!-- 已有附件展示 -->
            <div v-if="req.specDocumentPath || req.reviewReportPath" class="attach-view" style="margin-bottom:12px">
              <template v-if="req.specDocumentPath">
                📄 <el-link type="primary" :href="`/api/files/download?path=${encodeURIComponent(req.specDocumentPath)}&name=${encodeURIComponent(req.specDocumentName || req.specDocumentPath.split('/').pop())}`" target="_blank">需求说明书</el-link>
              </template>
              <template v-if="req.reviewReportPath">
                <span v-if="req.specDocumentPath" style="margin: 0 8px">|</span>
                📎 <el-link type="primary" :href="`/api/files/download?path=${encodeURIComponent(req.reviewReportPath)}&name=${encodeURIComponent(req.reviewReportName || req.reviewReportPath.split('/').pop())}`" target="_blank">架构评审报告</el-link>
              </template>
            </div>
            <div style="margin-bottom: 12px">
              <span class="field-label">更新需求说明书：</span>
              <el-upload :http-request="handleSpecUpload" :limit="1" :on-exceed="() => ElMessage.warning('仅支持上传一个文件')" :file-list="specFileList">
                <el-button type="primary" plain size="small" :loading="uploadLoading">选择文件上传</el-button>
              </el-upload>
              <span v-if="specDocName" style="font-size:12px;color:#006eff;margin-left:8px">新上传：{{ specDocName }}</span>
            </div>
            <p class="field-label">确认人员：</p>
            <div class="reviewer-table">
              <div class="reviewer-header">
                <span class="r-col-name">姓名</span><span class="r-col-role">角色</span><span class="r-col-sys">所属系统</span><span class="r-col-act">操作</span>
              </div>
              <div v-for="(r, i) in specReviewers" :key="i" class="reviewer-row">
                <el-select v-model="r.name" filterable placeholder="姓名" size="small" class="r-col-name">
                  <el-option v-for="u in userOptions" :key="u" :label="u" :value="u" />
                </el-select>
                <el-select v-model="r.role" placeholder="角色" size="small" class="r-col-role">
                  <el-option label="系统负责人" value="系统负责人" />
                  <el-option label="产品经理" value="产品经理" />
                  <el-option label="业务人员" value="业务人员" />
                  <el-option label="项目经理" value="项目经理" />
                  <el-option label="团队负责人" value="团队负责人" />
                  <el-option label="部门负责人" value="部门负责人" />
                </el-select>
                <el-select v-model="r.system" filterable allow-create default-first-option
                  placeholder="系统（可选）" size="small" class="r-col-sys"
                  :clearable="true">
                  <el-option v-for="s in systemItemsData" :key="s.name" :label="s.name" :value="s.name" />
                </el-select>
                <el-button type="danger" size="small" circle class="r-col-act" @click="removeReviewer(i)">×</el-button>
              </div>
            </div>
            <el-button size="small" style="margin-top: 8px" @click="addReviewer">+ 新增确认人员</el-button>
            <el-input v-model="commentText" type="textarea" :rows="2" placeholder="备注（可选）" style="margin-top: 10px" />
            <div class="action-btns">
              <el-button type="primary" :loading="actionLoading" @click="doUploadSpec">提交评审</el-button>
            </div>
          </div>

          <!-- 多方确认 -->
          <div v-else-if="req.currentNode === '多方确认' && specReviewers.some(r => nameEquals(r.name, userStore.user?.name))" class="action-area">
            <!-- 附件展示 -->
            <div v-if="req.specDocumentPath || req.reviewReportPath" class="attach-view">
              <template v-if="req.specDocumentPath">
                📄 <el-link type="primary" :href="`/api/files/download?path=${encodeURIComponent(req.specDocumentPath)}&name=${encodeURIComponent(req.specDocumentName || req.specDocumentPath.split('/').pop())}`" target="_blank">需求说明书</el-link>
              </template>
              <template v-if="req.reviewReportPath">
                <span v-if="req.specDocumentPath" style="margin: 0 8px">|</span>
                📎 <el-link type="primary" :href="`/api/files/download?path=${encodeURIComponent(req.reviewReportPath)}&name=${encodeURIComponent(req.reviewReportName || req.reviewReportPath.split('/').pop())}`" target="_blank">架构评审报告</el-link>
              </template>
            </div>
            <p class="action-tip">以下人员需逐一确认需求说明书，全部确认后状态变更为「需求已确认」：</p>
            <div class="reviewer-table">
              <div class="reviewer-header">
                <span class="r-col-name">姓名</span><span class="r-col-role">角色</span><span class="r-col-sys">所属系统</span><span class="r-col-status">确认状态</span>
              </div>
              <div v-for="(r, i) in specReviewers" :key="i" class="reviewer-row">
                <span class="r-col-name">{{ r.name }}</span>
                <span class="r-col-role">{{ r.role }}</span>
                <span class="r-col-sys">{{ r.system }}</span>
                <span class="r-col-status">
                  <el-tag v-if="r.confirmed" type="success" size="small">已确认</el-tag>
                  <el-tag v-else type="info" size="small">待确认</el-tag>
                </span>
              </div>
            </div>
            <p class="confirm-progress">
              确认进度：{{ specReviewers.filter(r => r.confirmed).length }}/{{ specReviewers.length }}
            </p>
            <el-input v-model="commentText" type="textarea" :rows="2" placeholder="确认意见（可选）" style="margin-top:10px" />
            <div class="action-btns">
              <el-button type="danger" @click="doReject">驳回至产品经理</el-button>
              <el-button type="primary" :loading="actionLoading" @click="doConfirmSpec">确认需求说明书</el-button>
            </div>
          </div>

          <!-- 已驳回 -->
          <div v-else-if="req.status === '已驳回' && nameEquals(userStore.user?.name, req.submitterName)" class="action-area">
            <p class="action-tip">需求已被驳回，修改后重新提交。</p>
            <el-input v-model="commentText" type="textarea" :rows="2" placeholder="请说明修改内容…" />
            <div class="action-btns">
              <el-button type="primary" :loading="actionLoading" @click="doResubmit">修改后重新提交</el-button>
            </div>
          </div>
        </div>

        <!-- 团队组长操作区 -->
        <div v-if="canActAsTeamLeader && req.status === '审批中'" class="section-block">
          <h4 class="section-title">当前节点操作 · 团队组长</h4>
          <!-- 附件展示 -->
          <div v-if="req.specDocumentPath || req.reviewReportPath" class="attach-view">
            <template v-if="req.specDocumentPath">
              📄 <el-link type="primary" :href="`/api/files/download?path=${encodeURIComponent(req.specDocumentPath)}&name=${encodeURIComponent(req.specDocumentName || req.specDocumentPath.split('/').pop())}`" target="_blank">需求说明书</el-link>
            </template>
            <template v-if="req.reviewReportPath">
              <span v-if="req.specDocumentPath" style="margin: 0 8px">|</span>
              📎 <el-link type="primary" :href="`/api/files/download?path=${encodeURIComponent(req.reviewReportPath)}&name=${encodeURIComponent(req.reviewReportName || req.reviewReportPath.split('/').pop())}`" target="_blank">架构评审报告</el-link>
            </template>
          </div>
          <p class="action-tip">
            为所负责系统的指派项目经理和产品经理（在上方系统表中同时编辑PM和PD列）。
            <template v-if="myTeamNames.length > 0">
              您负责团队：<b>{{ myTeamNames.join('、') }}</b>
              <span v-if="systemItemsData.some(s => !isMyTeam(s.team))" style="color: #ff9c00">
                （其他团队的系统由对应团队负责人指派）
              </span>
            </template>
          </p>
          <el-input v-model="commentText" type="textarea" :rows="2" placeholder="审批意见（可选）" style="margin-top: 10px" />
          <div class="action-btns">
            <el-button type="danger" @click="doReject">驳回至提出人</el-button>
            <el-button type="primary" :loading="actionLoading" @click="doTeamAssign">确认指派</el-button>
          </div>
        </div>
      </div>

      <!-- 右栏：审批流程时间线 -->
      <div class="detail-right">
        <div class="approval-panel">
          <h4 class="section-title">审批流程与记录</h4>
          <div class="approval-feed">
            <div
              v-for="(node, i) in nodes"
              :key="i"
              class="feed-node"
              :class="{
                passed: i < req.nodeIndex || req.status === '需求已确认' || (req.status === '实施中' || req.status === '测试通过' || req.status === '已投产' || req.status === '已关闭'),
                current: i === req.nodeIndex && req.status === '审批中',
                pending: i > req.nodeIndex && req.status === '审批中',
              }"
            >
              <div class="feed-indicator">
                <div class="feed-dot"></div>
                <div v-if="i < nodes.length - 1" class="feed-line"></div>
              </div>
              <div class="feed-content">
                <div class="feed-node-header">
                  <span class="feed-node-name">{{ node.name }}</span>
                  <span class="feed-node-role">{{ node.role }}</span>
                  <span v-if="req.status === '已驳回' && i === 0" class="feed-rejected-badge">已驳回至此</span>
                </div>

                <!-- 团队组长节点：显示各系统指派状态（仅已到达或超过该节点时显示） -->
                <div v-if="node.name === '团队组长' && i <= req.nodeIndex && systemItemsData.length" class="feed-sub-status">
                  <div v-for="sys in systemItemsData" :key="'ts-' + sys.name" class="sub-status-row">
                    <span class="ss-dot" :class="{ done: sys.pm && sys.pd }"></span>
                    <span class="ss-name">{{ sys.name }}</span>
                    <span class="ss-pm">PM: {{ sys.pm || '—' }}</span>
                    <span class="ss-pd">PD: {{ sys.pd || '—' }}</span>
                  </div>
                </div>

                <!-- 多方确认节点：显示每个确认人员状态（仅已到达或超过该节点时显示） -->
                <div v-if="node.name === '多方确认' && i <= req.nodeIndex && specReviewers.length" class="feed-sub-status">
                  <div v-for="r in specReviewers" :key="'cr-' + r.name" class="sub-status-row">
                    <span class="ss-dot" :class="{ done: r.confirmed }"></span>
                    <span class="ss-name">{{ r.name }}</span>
                    <span class="ss-role">{{ r.role }}</span>
                    <el-tag v-if="r.confirmed" type="success" size="small">已确认</el-tag>
                    <el-tag v-else type="info" size="small">待确认</el-tag>
                  </div>
                </div>

                <!-- 该节点的审批评论 -->
                <div v-if="commentsByNode[node.name]?.length" class="feed-comments">
                  <div v-for="c in commentsByNode[node.name]" :key="c.time" class="feed-comment-item">
                    <span class="fci-author">{{ c.author }}</span>
                    <span class="fci-action" :class="'act-' + c.action">{{ c.action }}</span>
                    <span class="fci-time">{{ c.time?.substring(0, 16) }}</span>
                    <div class="fci-text">{{ c.content }}</div>
                  </div>
                </div>

                <div v-if="i === req.nodeIndex && req.status === '审批中'" class="feed-current-hint">
                  等待处理…
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-else-if="!loading" class="empty-state">暂无可显示的需求</div>

    <!-- 启动实施弹窗 -->
    <el-dialog v-model="implVisible" title="启动实施" width="450px" append-to-body destroy-on-close>
      <el-form :model="implForm" label-width="120px">
        <el-form-item label="计划测试时间" required>
          <el-date-picker v-model="implForm.plannedTestDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="计划投产时间" required>
          <el-date-picker v-model="implForm.plannedProductionDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="implVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="doStartImpl">确认启动</el-button>
      </template>
    </el-dialog>

    <!-- 填写投产日期弹窗 -->
    <el-dialog v-model="productionVisible" title="填写正式投产日期" width="450px" append-to-body destroy-on-close>
      <el-form label-width="120px">
        <el-form-item label="正式投产日期" required>
          <el-date-picker v-model="productionDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productionVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="doSetProduction">确认</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<style scoped>
/* ==================== 左右两栏布局 ==================== */
.detail-layout {
  display: flex;
  gap: 20px;
  min-height: 300px;
}
.detail-left { flex: 1; min-width: 0; }
.detail-right { width: 300px; flex-shrink: 0; }
.approval-panel {
  position: sticky;
  top: 0;
  height: calc(100vh - 180px);
  max-height: none;
  overflow-y: auto;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
}

.empty-state { text-align: center; padding: 40px; color: #999; }

.section-block { margin-top: 16px; }
.section-title {
  font-size: 13px; font-weight: 600; color: #303133;
  margin-bottom: 10px; padding-bottom: 8px; border-bottom: 1px solid #ebeef5;
}

.content-text { white-space: pre-wrap; line-height: 1.6; }

/* ==================== 系统+PM/PD 合并表 ==================== */
.sys-table { border: 1px solid #ebeef5; border-radius: 2px; overflow: hidden; }
.sys-table-header, .sys-table-row {
  display: flex; gap: 6px; padding: 8px 10px; align-items: flex-start;
  font-size: 13px;
}
.sys-table-header { background: #fafafa; font-weight: 500; color: #606266; border-bottom: 1px solid #ebeef5; }
.sys-table-row { border-bottom: 1px solid #ebeef5; }
.sys-table-row:last-child { border-bottom: none; }
.sys-table-row.is-primary { background: #fef0f0; }
.sys-table-row.is-other-team { opacity: 0.6; background: #f9f9f9; }
.st-col-name { width: 160px; flex-shrink: 0; }
.st-col-team { width: 130px; flex-shrink: 0; color: #909399; font-size: 12px; }
.st-col-mod { flex: 1; min-width: 150px; }
.st-col-pm { width: 120px; flex-shrink: 0; }
.st-col-pd { width: 120px; flex-shrink: 0; }
.primary-tag { margin-left: 4px; }
.text-muted { color: #a8abb2; font-size: 12px; }

/* ==================== 操作区 ==================== */
.action-area { margin-top: 8px; }
.action-tip { font-size: 13px; color: #606266; margin-bottom: 8px; }
.action-btns { display: flex; gap: 8px; margin-top: 12px; justify-content: flex-end; }
.field-label { font-size: 13px; color: #606266; margin-right: 8px; margin-bottom: 8px; display: block; }
.confirm-progress { font-size: 12px; color: #a8abb2; margin-top: 8px; }

.eval-row-wrap { margin-bottom: 10px; padding: 10px; background: #fafafa; border-radius: 2px; }
.eval-row { display: flex; align-items: center; gap: 8px; }
.eval-info { font-size: 12px; color: #a8abb2; white-space: nowrap; flex: 1; }

/* 确认人员表 */
.reviewer-table { border: 1px solid #ebeef5; border-radius: 2px; overflow: hidden; }
.reviewer-header, .reviewer-row { display: flex; gap: 6px; padding: 6px 8px; align-items: center; }
.reviewer-header { background: #fafafa; font-size: 12px; font-weight: 500; color: #606266; border-bottom: 1px solid #ebeef5; }
.reviewer-row { border-bottom: 1px solid #ebeef5; }
.reviewer-row:last-child { border-bottom: none; }
.r-col-name { width: 110px; flex-shrink: 0; }
.r-col-role { width: 120px; flex-shrink: 0; }
.r-col-sys { flex: 1; }
.r-col-act { width: 32px; flex-shrink: 0; }
.r-col-status { width: 80px; flex-shrink: 0; }
.r-col-time { width: 130px; flex-shrink: 0; font-size: 12px; color: #909399; }

/* ==================== 审批流程时间线（右侧面板） ==================== */
.approval-panel .section-title { margin-top: 0; }
.approval-feed { padding: 4px 0; }
.feed-node { display: flex; gap: 10px; position: relative; }
.feed-node:last-child .feed-content { padding-bottom: 0; }

.feed-indicator { display: flex; flex-direction: column; align-items: center; width: 20px; flex-shrink: 0; padding-top: 4px; }
.feed-dot {
  width: 8px; height: 8px; border-radius: 50%;
  border: 2px solid #d6dbe3; background: #fff; z-index: 1; flex-shrink: 0;
}
.feed-node.passed .feed-dot { background: #0abf5b; border-color: #0abf5b; }
.feed-node.current .feed-dot {
  background: #fff; border-color: #006eff;
  box-shadow: 0 0 0 3px rgba(0,110,255,0.18);
}
.feed-line { width: 2px; flex: 1; min-height: 20px; background: #ebeef5; margin-top: 4px; }
.feed-node.passed .feed-line { background: #0abf5b; }

.feed-content { flex: 1; padding-bottom: 12px; min-width: 0; }
.feed-node-header { display: flex; align-items: center; gap: 4px; margin-bottom: 2px; flex-wrap: wrap; }
.feed-node-name { font-size: 12px; font-weight: 500; color: #303133; }
.feed-node-role { font-size: 11px; color: #a8abb2; }
.feed-node.pending .feed-node-name { color: #a8abb2; }
.feed-rejected-badge { font-size: 11px; color: #e54545; background: #fdecec; padding: 1px 6px; border-radius: 10px; }

/* 子状态行（系统指派/确认人员状态） */
.feed-sub-status { margin-top: 4px; margin-bottom: 4px; }
.sub-status-row { display: flex; align-items: center; gap: 4px; font-size: 11px; padding: 2px 0; color: #606266; }
.ss-dot { width: 6px; height: 6px; border-radius: 50%; background: #d6dbe3; flex-shrink: 0; }
.ss-dot.done { background: #0abf5b; }
.ss-name { font-weight: 500; }
.ss-pm, .ss-pd, .ss-role { color: #909399; }

/* 审批评论 */
.feed-comments { margin-top: 4px; }
.feed-comment-item {
  padding: 6px 8px; background: #fafafa; border-radius: 3px; margin-bottom: 3px;
  border-left: 2px solid #e8e8e8; font-size: 11px;
}
.feed-node.current .feed-comment-item { border-left-color: #006eff; }
.fci-author { font-size: 11px; font-weight: 500; color: #303133; }
.fci-action { font-size: 10px; padding: 1px 5px; border-radius: 2px; margin-left: 4px; }
.act-提交 { background: #e8f2ff; color: #006eff; } .act-通过 { background: #e8f8ee; color: #0abf5b; }
.act-驳回 { background: #fdecec; color: #e54545; } .act-指派 { background: #fff8ed; color: #ff9c00; }
.act-评估 { background: #e8f2ff; color: #006eff; } .act-上传 { background: #e8f2ff; color: #006eff; }
.act-确认 { background: #e8f8ee; color: #0abf5b; } .act-完成 { background: #e8f8ee; color: #0abf5b; }
.act-转交 { background: #fff8ed; color: #ff9c00; }
.fci-time { font-size: 10px; color: #a8abb2; margin-left: auto; float: right; }
.fci-text { font-size: 11px; color: #606266; margin-top: 2px; line-height: 1.4; }
.feed-current-hint { font-size: 11px; color: #006eff; margin-top: 2px; font-style: italic; }

/* 优先级 */
.pri-紧急 { color: #e54545; font-weight: 600; } .pri-高 { color: #ff9c00; font-weight: 500; }
.pri-普通 { color: #606266; } .pri-低 { color: #a8abb2; }

/* 关联测试报告 */
.related-report-card { padding: 10px 12px; background: #fafafa; border-radius: 4px; border: 1px solid #ebeef5; margin-bottom: 6px; }
.rr-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.rr-title { font-size: 13px; font-weight: 500; }
.rr-meta { font-size: 12px; color: #a8abb2; }
.rr-attach { margin-top: 6px; padding-top: 6px; border-top: 1px solid #ebeef5; font-size: 12px; }

.post-confirm-actions { display: flex; gap: 8px; align-items: center; }

.attach-view { margin-bottom: 8px; padding: 8px 12px; background: #f5f7fa; border-radius: 4px; display: flex; align-items: center; gap: 8px; }
.attach-label { font-size: 13px; color: #606266; white-space: nowrap; }

/* 附件集中展示区 */
.attach-group { padding: 8px 0; }
.attach-item { display: flex; align-items: center; gap: 6px; padding: 6px 10px; margin-bottom: 4px; background: #fafbfc; border-radius: 4px; border: 1px solid #ebeef5; font-size: 13px; }
.attach-item:last-child { margin-bottom: 0; }
.attach-icon { font-size: 14px; flex-shrink: 0; }
.attach-status { margin-left: auto; flex-shrink: 0; }
</style>
