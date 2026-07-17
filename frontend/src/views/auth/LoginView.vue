<template>
  <div class="login-container">
    <div class="logo-section">
      <div class="logo-icon">
        <img class="logo-image" :src="logoUrl" alt="玩转AI" />
      </div>
      <h1 class="logo-title">玩转AI</h1>
      <p class="logo-subtitle">更适合你的AI</p>
    </div>

    <div class="login-card">
      <h2 class="card-title">欢迎回来</h2>
      <p class="card-subtitle">登录即可体验极速AI</p>

      <el-form :model="loginForm" :rules="rules" ref="formRef" label-position="top" class="login-form">
        <el-form-item label="账号" prop="account">
          <el-input v-model="loginForm.account" placeholder="请输入账号" clearable />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>

        <el-form-item class="submit-item">
          <el-button type="primary" :loading="loading" @click="handleLogin(formRef)" class="submit-btn">
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="register-hint">
        还没有账户？ <span class="register-link" @click="goToRegister">注册</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, reactive } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '@/utils/api'
import { defaultConsoleRouteName } from '@/utils/authorization'
import { clearAccessToken, setAccessToken } from '@/utils/auth'
import logoUrl from '@/assets/logo.png'

const loginForm = reactive({
  account: '',
  password: ''
})

const loading = ref(false)
const formRef = ref<FormInstance>()
const router = useRouter()
const route = useRoute()
const DEVICE_ID_STORAGE_KEY = 'ai-chat-browser-device-id'

onMounted(() => {
  if (typeof route.query.username === 'string') {
    loginForm.account = route.query.username
  }
})

const rules = reactive<FormRules>({
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 32, message: '账号长度为 4 到 32 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 72, message: '密码长度为 8 到 72 个字符', trigger: 'blur' }
  ]
})

const handleLogin = async (formEl: FormInstance | undefined) => {
  if (!formEl) return
  try {
    await formEl.validate()
  } catch {
    ElMessage.error('请完善登录信息')
    return
  }

  loading.value = true
  try {
    const tokenData = await authApi.login({
      username: loginForm.account,
      password: loginForm.password,
      deviceId: getBrowserDeviceId(),
    })
    setAccessToken(tokenData.accessToken)
    let currentUser
    try {
      currentUser = await authApi.currentUser()
    } catch (error) {
      clearAccessToken()
      throw error
    }
    ElMessage.success('登录成功')
    await router.replace({ name: defaultConsoleRouteName(currentUser.menuCodes) })
  } finally {
    loading.value = false
  }
}

const goToRegister = () => {
  void router.push({ name: 'register' })
}

function getBrowserDeviceId() {
  try {
    const existingDeviceId = localStorage.getItem(DEVICE_ID_STORAGE_KEY)
    if (existingDeviceId) return existingDeviceId

    const deviceId = createDeviceId()
    localStorage.setItem(DEVICE_ID_STORAGE_KEY, deviceId)
    return deviceId
  } catch {
    return createDeviceId()
  }
}

function createDeviceId() {
  const value = globalThis.crypto?.randomUUID?.() ?? `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`
  return `browser-${value}`
}
</script>

<style scoped lang="scss">
.login-container {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  width: 100vw;
  box-sizing: border-box;
  overflow-x: hidden;

  background-color: var(--app-bg);
  background-image:
    linear-gradient(to right, rgba(0, 0, 0, 0.03) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(0, 0, 0, 0.03) 1px, transparent 1px);
  background-size: 24px 24px;
}

.login-container::before,
.login-container::after {
  content: '';
  position: absolute;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  z-index: 0;
}

.login-container::before {
  bottom: -50px;
  left: -50px;
  background: radial-gradient(circle, rgba(20, 184, 166, 0.3) 0%, transparent 70%);
}

.login-container::after {
  top: -50px;
  right: -50px;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.3) 0%, transparent 70%);
}

.login-container .logo-section,
.login-container .login-card,
.login-container {
  position: relative;
  z-index: 1;
}

.login-container .logo-section {
  text-align: center;
  margin-bottom: 24px;
}

.login-container .logo-section .logo-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 12px;
}

.login-container .logo-section .logo-image {
  display: block;
  width: 48px;
  height: 48px;
  object-fit: contain;
}

.login-container .logo-section .logo-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--app-text);
  margin: 0 0 6px;
  letter-spacing: 1px;
}

.login-container .logo-section .logo-subtitle {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
}

.login-container .login-card {
  width: 420px;
  box-sizing: border-box;
  padding: 40px 36px;
  text-align: center;
  background: var(--app-surface);
  border: 1px solid var(--app-border);
  border-radius: 16px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.02), 0 8px 10px -6px rgba(0, 0, 0, 0.02);
}

.login-container .login-card .card-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: var(--app-text);
}

.login-container .login-card .card-subtitle {
  margin: 0 0 32px;
  font-size: 13px;
  color: #9ca3af;
}

.login-container .login-form :deep(.el-form-item__label) {
  padding: 0;
  margin-bottom: 6px;
  font-weight: 600;
  color: var(--app-text-regular);
}

.login-container .login-form :deep(.el-input__wrapper) {
  padding: 8px 12px;
  background-color: var(--app-surface-muted);
  border: 1px solid transparent;
  border-radius: 8px;
  box-shadow: none !important;
  transition: all 0.2s ease;
}

.login-container .login-form :deep(.el-input__wrapper.is-focus) {
  background-color: var(--app-surface);
  border-color: var(--app-primary);
}

.login-container .login-form :deep(.el-input__inner) {
  height: 24px;
  color: var(--app-text);
}

.login-container .login-form .submit-item {
  margin-top: 32px;
  margin-bottom: 16px;
}

.login-container .login-form .submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 2px;
  background-color: var(--app-primary);
  border-color: var(--app-primary);
  border-radius: 8px;
}

.login-container .login-form .submit-btn:hover,
.login-container .login-form .submit-btn:focus {
  background-color: var(--app-primary-strong);
  border-color: var(--app-primary-strong);
}

.login-container .login-card .register-hint {
  margin-top: 16px;
  font-size: 13px;
  color: #9ca3af;
}

.login-container .login-card .register-link {
  margin-left: 4px;
  font-weight: 500;
  color: var(--app-primary-strong);
  cursor: pointer;
}

.login-container .login-card .register-link:hover {
  text-decoration: underline;
}

@media (max-width: 576px) {
  .login-container {
    padding: 20px;
    justify-content: flex-start;
    padding-top: 12vh;
  }

  .login-container .login-card {
    width: 92%;
    padding: 32px 24px;
    background: var(--app-surface);
    box-shadow: none;
  }

  .login-container {
    padding-bottom: 24px;
  }
}
</style>
