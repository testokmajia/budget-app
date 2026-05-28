<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, provide } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getStats } from '@/api/dashboard'
import { changePassword } from '@/api/auth'
import { ElMessage } from 'element-plus'
import {
  HomeFilled,
  Clock,
  List,
  Trophy,
  Warning,
  Setting,
  SwitchButton,
  Menu,
  Lock,
  Document,
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const drawerVisible = ref(false)
const pendingTasks = ref([])

const pageTitle = computed(() => {
  const map = {
    '/dashboard': '首页',
    '/checklist': '清单管理',
    '/reward': '奖惩记录',
    '/issue': '科技问题管理',
    '/weekly': '工作周报',
    '/pending': '问题解决实施',
    '/admin': '系统管理',
  }
  return map[route.path] || ''
})

function parseQuery(raw) {
  const query = {}
  if (raw) {
    raw.split('&').forEach(p => {
      const [k, v] = p.split('=')
      if (k && v) query[k] = v
    })
  }
  return query
}

async function fetchPendingTasks() {
  try {
    const res = await getStats()
    pendingTasks.value = res.data?.pendingTasks || []
  } catch { pendingTasks.value = [] }
}

// Resizable sidebar
const STORAGE_KEY = 'sidebar_width'
const MIN_WIDTH = 140
const MAX_WIDTH = 320
const DEFAULT_WIDTH = 170

function loadSidebarWidth() {
  try {
    const v = localStorage.getItem(STORAGE_KEY)
    const n = parseInt(v, 10)
    if (!isNaN(n) && n >= MIN_WIDTH && n <= MAX_WIDTH) return n
  } catch { /* ignore */ }
  return DEFAULT_WIDTH
}

const sidebarWidth = ref(loadSidebarWidth())
const resizing = ref(false)

function onResizeStart(e) {
  resizing.value = true
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  document.addEventListener('mousemove', onResizeMove)
  document.addEventListener('mouseup', onResizeEnd)
  e.preventDefault()
}

function onResizeMove(e) {
  if (!resizing.value) return
  const w = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, e.clientX))
  sidebarWidth.value = w
}

function onResizeEnd() {
  resizing.value = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  document.removeEventListener('mousemove', onResizeMove)
  document.removeEventListener('mouseup', onResizeEnd)
  try { localStorage.setItem(STORAGE_KEY, String(sidebarWidth.value)) } catch { /* ignore */ }
}

provide('refreshBadges', fetchPendingTasks)

onMounted(() => {
  fetchPendingTasks()
})

onUnmounted(() => {
  document.removeEventListener('mousemove', onResizeMove)
  document.removeEventListener('mouseup', onResizeEnd)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
})

const menuItems = [
  { path: '/dashboard', title: '首页', icon: HomeFilled },
  { path: '/checklist', title: '清单管理', icon: List },
  { path: '/reward', title: '奖惩记录', icon: Trophy },
  { path: '/issue', title: '科技问题管理', icon: Warning },
  { path: '/weekly', title: '工作周报', icon: Document },
]

const adminMenu = [
  { path: '/admin', title: '系统管理', icon: Setting },
]

const showPending = computed(() => userStore.user?.department === '信息科技部')

function navigate(path) {
  drawerVisible.value = false
  router.push(path)
}

// 修改密码
const passwordVisible = ref(false)
const passwordFormRef = ref(null)
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const passwordSubmitting = ref(false)

const passwordRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '新密码至少8位', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)/, message: '新密码必须包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, cb) => {
        if (value !== passwordForm.newPassword) cb(new Error('两次输入的新密码不一致'))
        else cb()
      },
      trigger: 'blur',
    },
  ],
}

async function handlePasswordSubmit() {
  const valid = await passwordFormRef.value.validate().catch(() => null)
  if (!valid) return
  passwordSubmitting.value = true
  try {
    await changePassword({ ...passwordForm })
    ElMessage.success('密码修改成功，请重新登录')
    passwordVisible.value = false
    userStore.logout()
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.response?.data?.error || '修改失败')
  } finally {
    passwordSubmitting.value = false
  }
}

function handleLogout() {
  drawerVisible.value = false
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <!-- Desktop sidebar -->
    <el-aside class="desktop-sidebar" :width="sidebarWidth + 'px'">
      <div class="logo">科技管理平台</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        :collapse="sidebarWidth < 160"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
        <el-menu-item v-if="showPending" index="/pending">
          <el-icon><Clock /></el-icon>
          <span>问题解决实施</span>
        </el-menu-item>
        <el-menu-item
          v-for="item in adminMenu"
          :key="item.path"
          :index="item.path"
          v-if="userStore.hasRole('ROLE_ADMIN')"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
      <div class="resize-handle" @mousedown="onResizeStart" />
    </el-aside>

    <!-- Mobile drawer -->
    <el-drawer
      v-model="drawerVisible"
      direction="ltr"
      size="220px"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="logo">科技管理平台</div>
      <el-menu
        :default-active="route.path"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path" @click="navigate(item.path)">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
        <el-menu-item v-if="showPending" index="/pending" @click="navigate('/pending')">
          <el-icon><Clock /></el-icon>
          <span>问题解决实施</span>
        </el-menu-item>
        <el-menu-item
          v-for="item in adminMenu"
          :key="item.path"
          :index="item.path"
          v-if="userStore.hasRole('ROLE_ADMIN')"
          @click="navigate(item.path)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
      <div class="drawer-footer">
        <el-button type="danger" @click="handleLogout">退出登录</el-button>
      </div>
    </el-drawer>

    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <el-button class="menu-toggle" :icon="Menu" @click="drawerVisible = true" />
          <span class="mobile-logo">科技管理平台</span>
          <span class="page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <span
            v-for="t in pendingTasks"
            :key="t.title"
            class="header-badge"
            @click="router.push({ name: t.routeName, query: parseQuery(t.routeQuery) })"
          >
            {{ t.title }} <strong>{{ t.count }}</strong>
          </span>
          <span class="user-info">
            {{ userStore.user?.name }}
            <span class="roles">({{ userStore.user?.roles?.join('、') }})</span>
          </span>
          <el-button class="logout-btn" type="primary" text @click="passwordVisible = true">
            <el-icon><Lock /></el-icon>
            <span class="logout-text">修改密码</span>
          </el-button>
          <el-button class="logout-btn" type="danger" text @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span class="logout-text">退出登录</span>
          </el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>

    <!-- 修改密码 -->
    <el-dialog v-model="passwordVisible" title="修改密码" width="420px">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input v-model="passwordForm.currentPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="至少8位，含字母和数字" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSubmitting" @click="handlePasswordSubmit">确认修改</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
}

/* === Desktop sidebar === */
.desktop-sidebar {
  background-color: #304156;
  overflow: hidden;
  position: relative;
  transition: none;
}
.resize-handle {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 5px;
  cursor: col-resize;
  background: transparent;
  z-index: 10;
  transition: background 0.15s;
}
.resize-handle:hover {
  background: rgba(64, 158, 255, 0.4);
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

/* === Header === */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 16px;
}
.header-left, .header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.menu-toggle {
  display: none;
}
.mobile-logo {
  display: none;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.header-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 10px;
  border-radius: 12px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
}
.header-badge:hover {
  background: #d9ecff;
}
.header-badge strong {
  font-size: 13px;
}
.user-info {
  font-size: 14px;
  color: #606266;
}
.roles {
  color: #909399;
  font-size: 12px;
}

/* === Mobile drawer === */
.mobile-drawer :deep(.el-drawer__body) {
  padding: 0;
  background-color: #304156;
}
.mobile-drawer .el-menu {
  border-right: none;
}
.drawer-footer {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  text-align: center;
}

/* === Responsive === */
@media (max-width: 768px) {
  .desktop-sidebar {
    display: none;
  }
  .menu-toggle {
    display: inline-flex;
  }
  .mobile-logo {
    display: inline;
  }
  .logout-text {
    display: none;
  }
  .roles {
    display: none;
  }
}
</style>
