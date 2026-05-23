<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  HomeFilled,
  List,
  Trophy,
  Warning,
  Setting,
  SwitchButton,
  Menu,
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const drawerVisible = ref(false)

const menuItems = [
  { path: '/dashboard', title: '首页', icon: HomeFilled },
  { path: '/checklist', title: '清单管理', icon: List },
  { path: '/reward', title: '奖惩记录', icon: Trophy },
  { path: '/issue', title: '科技问题管理', icon: Warning },
]

const adminMenu = [
  { path: '/admin', title: '系统管理', icon: Setting },
]

function navigate(path) {
  drawerVisible.value = false
  router.push(path)
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
    <el-aside class="desktop-sidebar" width="220px">
      <div class="logo">科技管理平台</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
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
    </el-aside>

    <!-- Mobile drawer -->
    <el-drawer
      v-model="drawerVisible"
      direction="ltr"
      size="240px"
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
        </div>
        <div class="header-right">
          <span class="user-info">
            {{ userStore.user?.name }}
            <span class="roles">({{ userStore.user?.roles?.join('、') }})</span>
          </span>
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
