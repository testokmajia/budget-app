<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { sendResetCode, verifyResetCode, resetPassword } from '@/api/auth'

const router = useRouter()
const route = useRoute()

// 步骤状态：1=填写邮箱，2=验证身份，3=重置密码，4=成功
const currentStep = ref(1)
// 从登录页传来的邮箱参数自动填充
const email = ref(route.query.email || '')
const code = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const resetToken = ref('')
const loading = ref(false)
const errorMsg = ref('')
const infoMsg = ref('')
const resendCountdown = ref(0)
let countdownTimer = null

const resendDisabled = computed(() => resendCountdown.value > 0)
const resendBtnText = computed(() =>
  resendCountdown.value > 0 ? `${resendCountdown.value}s 后重发` : '重新发送'
)

// 步骤标签
const stepLabels = ['填写公司邮箱', '输入验证码', '设置新密码', '']

// ========== 步骤1：发送验证码 ==========
async function handleSendCode() {
  if (!email.value || !email.value.includes('@')) {
    errorMsg.value = '请输入有效的邮箱地址'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    await sendResetCode({ email: email.value })
    infoMsg.value = `验证码已发送至 ${email.value}，10 分钟内有效`
    currentStep.value = 2
    startResendCountdown()
  } catch (e) {
    errorMsg.value = e.response?.data?.error || e.message || '发送失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// ========== 步骤2：重发倒计时 ==========
function startResendCountdown() {
  stopCountdown()
  resendCountdown.value = 60
  countdownTimer = setInterval(() => {
    resendCountdown.value--
    if (resendCountdown.value <= 0) {
      stopCountdown()
    }
  }, 1000)
}

function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

async function handleResendCode() {
  if (resendDisabled.value) return
  loading.value = true
  errorMsg.value = ''
  try {
    await sendResetCode({ email: email.value })
    infoMsg.value = `验证码已重新发送至 ${email.value}`
    startResendCountdown()
  } catch (e) {
    errorMsg.value = e.response?.data?.error || e.message || '重发失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// ========== 步骤3：校验验证码 ==========
async function handleVerifyCode() {
  const rawCode = code.value.replace(/\D/g, '')
  if (rawCode.length !== 6) {
    errorMsg.value = '请输入6位数字验证码'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await verifyResetCode({ email: email.value, code: rawCode })
    resetToken.value = res.data
    newPassword.value = ''
    confirmPassword.value = ''
    currentStep.value = 3
  } catch (e) {
    errorMsg.value = e.response?.data?.error || e.message || '验证失败'
    code.value = ''
  } finally {
    loading.value = false
  }
}

// ========== 步骤4：重置密码 ==========
async function handleResetPassword() {
  if (newPassword.value.length < 8) {
    errorMsg.value = '密码至少8位'
    return
  }
  if (!/[a-zA-Z]/.test(newPassword.value) || !/\d/.test(newPassword.value)) {
    errorMsg.value = '密码必须包含字母和数字'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    await resetPassword({
      email: email.value,
      resetToken: resetToken.value,
      newPassword: newPassword.value
    })
    currentStep.value = 4
  } catch (e) {
    errorMsg.value = e.response?.data?.error || e.message || '重置失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// ========== 切换步骤 ==========
function goToStep(step) {
  errorMsg.value = ''
  infoMsg.value = step === 2 ? `验证码已发送至 ${email.value}，10 分钟内有效` : ''
  currentStep.value = step
}

// ========== 返回登录 ==========
function goBackToLogin() {
  router.push('/login')
}

onUnmounted(() => {
  stopCountdown()
})
</script>

<template>
  <div class="forgot-container">
    <el-card class="forgot-card">
      <h2>企业科技管理平台</h2>
      <p class="forgot-desc">{{ stepLabels[currentStep - 1] || '重置密码' }}</p>

      <!-- 步骤条 -->
      <el-steps :active="currentStep - 1" align-center class="forgot-steps" v-if="currentStep <= 3">
        <el-step title="填写邮箱" />
        <el-step title="验证身份" />
        <el-step title="重置密码" />
      </el-steps>

      <!-- ===== 步骤1：填写邮箱 ===== -->
      <div v-if="currentStep === 1">
        <el-alert
          v-if="infoMsg"
          :title="infoMsg"
          type="info"
          show-icon
          :closable="false"
          class="forgot-alert"
        />
        <el-alert
          v-if="errorMsg"
          :title="errorMsg"
          type="error"
          show-icon
          :closable="false"
          class="forgot-alert"
        />
        <el-form @keyup.enter="handleSendCode">
          <el-form-item>
            <el-input
              v-model="email"
              placeholder="请输入您的公司邮箱地址"
              prefix-icon="Message"
              autocomplete="email"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              style="width: 100%"
              :loading="loading"
              @click="handleSendCode"
            >
              发送验证码
            </el-button>
          </el-form-item>
        </el-form>
        <p class="forgot-footer">
          <a @click="goBackToLogin">&larr; 返回登录</a>
        </p>
      </div>

      <!-- ===== 步骤2：输入验证码 ===== -->
      <div v-if="currentStep === 2">
        <el-alert
          v-if="infoMsg"
          :title="infoMsg"
          type="info"
          show-icon
          :closable="false"
          class="forgot-alert"
        />
        <el-alert
          v-if="errorMsg"
          :title="errorMsg"
          type="error"
          show-icon
          :closable="false"
          class="forgot-alert"
        />
        <el-form @keyup.enter="handleVerifyCode">
          <el-form-item>
            <el-input
              v-model="code"
              placeholder="000000"
              maxlength="6"
              class="code-input"
              autocomplete="one-time-code"
              inputmode="numeric"
              @input="code = $event.replace(/\D/g, '')"
            />
          </el-form-item>
          <el-form-item>
            <div class="code-actions">
              <el-button
                type="primary"
                style="flex: 1"
                :loading="loading"
                @click="handleVerifyCode"
              >
                下一步
              </el-button>
              <el-button
                :disabled="resendDisabled"
                @click="handleResendCode"
              >
                {{ resendBtnText }}
              </el-button>
            </div>
          </el-form-item>
        </el-form>
        <p class="forgot-footer">
          <a @click="goToStep(1)">&larr; 更换邮箱</a>
        </p>
      </div>

      <!-- ===== 步骤3：重置密码 ===== -->
      <div v-if="currentStep === 3">
        <el-alert
          v-if="infoMsg"
          :title="infoMsg"
          type="info"
          show-icon
          :closable="false"
          class="forgot-alert"
        />
        <el-alert
          v-if="errorMsg"
          :title="errorMsg"
          type="error"
          show-icon
          :closable="false"
          class="forgot-alert"
        />
        <el-form @keyup.enter="handleResetPassword">
          <el-form-item>
            <el-input
              v-model="newPassword"
              type="password"
              placeholder="请输入新密码（至少8位，包含字母和数字）"
              show-password
              prefix-icon="Lock"
              autocomplete="new-password"
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password
              prefix-icon="Lock"
              autocomplete="new-password"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              style="width: 100%"
              :loading="loading"
              @click="handleResetPassword"
            >
              重置密码
            </el-button>
          </el-form-item>
        </el-form>
        <p class="forgot-footer">
          <a @click="goToStep(2)">&larr; 重新验证</a>
        </p>
      </div>

      <!-- ===== 步骤4：完成 ===== -->
      <div v-if="currentStep === 4" class="forgot-success">
        <el-result
          icon="success"
          title="密码重置成功"
          sub-title="请使用新密码重新登录"
        >
          <template #extra>
            <el-button type="primary" @click="goBackToLogin">返回登录</el-button>
          </template>
        </el-result>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.forgot-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: #f0f2f5;
  background-image: radial-gradient(ellipse at 50% 50%, rgba(24,144,255,0.06) 0%, transparent 70%);
}

.forgot-card {
  width: 420px;
  max-width: 90vw;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08), 0 4px 16px rgba(0,0,0,0.04);
}

.forgot-card h2 {
  text-align: center;
  margin-bottom: 4px;
  color: rgba(0,0,0,0.85);
  font-weight: 500;
  font-size: 20px;
}

.forgot-desc {
  text-align: center;
  font-size: 14px;
  color: #909399;
  margin-bottom: 24px;
}

.forgot-steps {
  margin-bottom: 28px;
}

.forgot-alert {
  margin-bottom: 16px;
}

/* 验证码输入框 — 大字居中 */
.code-input :deep(.el-input__inner) {
  text-align: center;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 8px;
}

.code-actions {
  display: flex;
  gap: 12px;
  width: 100%;
}

.forgot-footer {
  text-align: center;
  font-size: 14px;
  margin-top: -8px;
}

.forgot-footer a {
  color: var(--brand, #006eff);
  cursor: pointer;
  text-decoration: none;
}

.forgot-footer a:hover {
  text-decoration: underline;
}

.forgot-success {
  padding: 8px 0 16px;
}
</style>
