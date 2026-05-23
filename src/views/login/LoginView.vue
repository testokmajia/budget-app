<script setup>
import { ref, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login, initQrLogin, pollQrStatus } from '@/api/auth'
import { Loading, FullScreen, Refresh, CircleCheck } from '@element-plus/icons-vue'
import QRCode from 'qrcode'

const router = useRouter()
const userStore = useUserStore()

const form = ref({ username: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')
const activeTab = ref('password')

async function handleLogin() {
  if (!form.value.username || !form.value.password) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    await userStore.login(form.value)
    router.push('/dashboard')
  } catch (e) {
    errorMsg.value = e.response?.data?.error || e.message || '登录失败'
  } finally {
    loading.value = false
  }
}

// QR login state
const qrCanvas = ref(null)
const qrState = ref('idle') // idle | loading | waiting | scanned | expired
const shortCode = ref('')
const qrMessage = ref('')
let pollTimer = null
let sessionId = ''

async function startQrLogin() {
  qrState.value = 'loading'
  try {
    const res = await initQrLogin()
    sessionId = res.data.sessionId
    shortCode.value = res.data.shortCode

    await nextTick()
    if (qrCanvas.value) {
      await QRCode.toCanvas(qrCanvas.value, sessionId, { width: 200, margin: 1 })
    }

    qrState.value = 'waiting'
    qrMessage.value = '请打开小程序扫码登录'
    startPolling()
  } catch (e) {
    qrMessage.value = '初始化失败，请重试'
    qrState.value = 'idle'
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      const res = await pollQrStatus(sessionId)
      const data = res.data
      if (data.status === 'SCANNED') {
        qrState.value = 'scanned'
        qrMessage.value = '已扫描，请在手机上确认登录'
      } else if (data.status === 'CONFIRMED') {
        stopPolling()
        userStore.token = data.token
        userStore.user = data.user
        localStorage.setItem('token', data.token)
        localStorage.setItem('user', JSON.stringify(data.user))
        router.push('/dashboard')
      } else if (data.status === 'EXPIRED') {
        qrState.value = 'expired'
        qrMessage.value = '二维码已过期，请刷新'
        stopPolling()
      }
    } catch {
      // continue polling
    }
  }, 2000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function refreshQr() {
  startQrLogin()
}

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>企业科技管理平台</h2>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="密码登录" name="password">
          <el-form @keyup.enter="handleLogin">
            <el-form-item>
              <el-input
                v-model="form.username"
                placeholder="用户名"
                prefix-icon="User"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="form.password"
                type="password"
                placeholder="密码"
                show-password
                prefix-icon="Lock"
              />
            </el-form-item>
            <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
            <el-form-item>
              <el-button
                type="primary"
                style="width: 100%"
                :loading="loading"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form-item>
            <p class="hint">默认管理员：admin / admin123456</p>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="扫码登录" name="qr">
          <div class="qr-section">
            <div class="qr-wrap" :class="{ 'qr-expired': qrState === 'expired' }">
              <!-- QR code canvas -->
              <canvas v-show="qrState === 'waiting' || qrState === 'scanned'" ref="qrCanvas" class="qr-canvas" />

              <!-- Loading -->
              <div v-if="qrState === 'loading'" class="qr-placeholder">
                <el-icon class="is-loading" :size="40"><Loading /></el-icon>
              </div>

              <!-- Idle/initial -->
              <div
                v-if="qrState === 'idle'"
                class="qr-placeholder qr-trigger"
                @click="startQrLogin"
              >
                <el-icon :size="40"><FullScreen /></el-icon>
                <span>点击获取二维码</span>
              </div>

              <!-- Expired overlay -->
              <div v-if="qrState === 'expired'" class="qr-overlay" @click="refreshQr">
                <el-icon :size="32"><Refresh /></el-icon>
                <span>已过期，点击刷新</span>
              </div>

              <!-- Scanned overlay -->
              <div v-if="qrState === 'scanned'" class="qr-overlay qr-scanned">
                <el-icon :size="32" color="#67c23a"><CircleCheck /></el-icon>
                <span>已扫描</span>
              </div>
            </div>

            <p class="qr-msg">{{ qrMessage }}</p>
            <p v-if="shortCode" class="qr-code-hint">
              小程序内可输入短码：<strong>{{ shortCode }}</strong>
            </p>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  max-width: 90vw;
}
.login-card h2 {
  text-align: center;
  margin-bottom: 16px;
  color: #303133;
}

:deep(.login-tabs .el-tabs__nav-wrap::after) {
  height: 1px;
}
:deep(.login-tabs .el-tabs__header) {
  margin-bottom: 12px;
}

.error-msg {
  color: #f56c6c;
  font-size: 13px;
  margin-bottom: 16px;
  text-align: center;
}
.hint {
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
}

/* QR section */
.qr-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0 16px;
}

.qr-wrap {
  position: relative;
  width: 200px;
  height: 200px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.qr-canvas {
  width: 190px;
  height: 190px;
}

.qr-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #909399;
  font-size: 14px;
  cursor: pointer;
  width: 100%;
  height: 100%;
  transition: background 0.2s;
  border-radius: 8px;
}
.qr-placeholder:hover {
  background: #f5f7fa;
}

.qr-expired .qr-canvas {
  filter: blur(4px) grayscale(100%);
  opacity: 0.3;
}

.qr-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  z-index: 2;
}

.qr-scanned {
  background: rgba(255, 255, 255, 0.9);
  cursor: default;
}

.qr-msg {
  font-size: 13px;
  color: #606266;
  text-align: center;
}

.qr-code-hint {
  font-size: 12px;
  color: #a8abb2;
  margin-top: 6px;
}
</style>
