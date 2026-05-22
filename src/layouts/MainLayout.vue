<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  HomeFilled,
  List,
  Trophy,
  Warning,
  Setting,
  SwitchButton,
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const menuItems = [
  { path: '/dashboard', title: '首页', icon: HomeFilled },
  { path: '/checklist', title: '清单管理', icon: List },
  { path: '/reward', title: '奖惩记录', icon: Trophy },
  { path: '/issue', title: '科技问题管理', icon: Warning },
]

const adminMenu = [
  { path: '/admin', title: '系统管理', icon: Setting },
]

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px">
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
    <el-container>
      <el-header>
        <span class="user-info">
          {{ userStore.user?.name }}
          <span class="roles">({{ userStore.user?.roles?.join('、') }})</span>
        </span>
        <el-button type="danger" text @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-button>
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
.el-aside {
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
.el-header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  border-bottom: 1px solid #e6e6e6;
  gap: 12px;
}
.user-info {
  font-size: 14px;
  color: #606266;
}
.roles {
  color: #909399;
  font-size: 12px;
}
</style>
