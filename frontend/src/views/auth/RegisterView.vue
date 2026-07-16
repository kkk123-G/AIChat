<template>
  <div class="register-container">
    <div class="register-content">
      <div class="logo-area">
        <div class="logo-icon">
          <img class="logo-image" :src="logoUrl" alt="玩转AI" />
        </div>
        <h1 class="brand-title">玩转AI</h1>
        <p class="brand-subtitle">更懂你的 AI</p>
      </div>

      <el-card class="register-card" shadow="never">
        <h2 class="form-title">创建账户</h2>
        <p class="form-subtitle">注册以开始使用 玩转AI</p>

        <el-form ref="formRef" :model="formData" :rules="formRules" :validate-on-rule-change="false"
          label-position="top" status-icon class="register-form">
          <el-form-item label="账号" prop="account">
            <el-input v-model="formData.account" placeholder="请输入账号" clearable />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input v-model="formData.password" type="password" placeholder="至少 8 个字符" show-password />
          </el-form-item>

          <el-form-item class="submit-item">
            <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister(formRef)">
              创建账户
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <div class="footer-link">
        已有账户？<el-link type="primary" :underline="false" @click="goToLogin">登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { authApi } from '@/utils/api'
import logoUrl from '@/assets/logo.png'

const formData = reactive({
  account: '',
  password: ''
})

const loading = ref(false)
const formRef = ref<FormInstance>()
const router = useRouter()

const formRules = reactive<FormRules>({
  account: [
    { required: true, message: '请输入账号', trigger: 'submit' },
    { min: 4, max: 32, message: '账号长度为 4 到 32 个字符', trigger: 'submit' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'submit' },
    { min: 8, max: 72, message: '密码长度为 8 到 72 个字符', trigger: 'submit' }
  ]
})

const handleRegister = async (formEl: FormInstance | undefined) => {
  if (!formEl) return

  try {
    await formEl.validate()
  } catch {
    ElMessage.error('请完善注册信息')
    return
  }

  loading.value = true
  try {
    await authApi.register({
      username: formData.account,
      password: formData.password,
    })
    ElMessage.success('注册成功，请登录')
    await router.replace({ name: 'login', query: { username: formData.account } })
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push({ name: 'login' })
}
</script>

<style scoped lang="scss">
.register-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  width: 100vw;
  background-color: var(--app-bg);
  background-image:
    linear-gradient(to right, rgba(0, 0, 0, 0.03) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(0, 0, 0, 0.03) 1px, transparent 1px);
  background-size: 24px 24px;
  box-sizing: border-box;
  padding: 20px;
}

.register-container::before,
.register-container::after {
  content: '';
  position: absolute;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  z-index: 0;
}

.register-container::before {
  bottom: -50px;
  left: -50px;
  background: radial-gradient(circle, rgba(20, 184, 166, 0.3) 0%, transparent 70%);
}

.register-container::after {
  top: -50px;
  right: -50px;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.3) 0%, transparent 70%);
}

.register-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 420px;
}

.logo-area {
  text-align: center;
  margin-bottom: 24px;

  .logo-icon {
    display: inline-flex;
    justify-content: center;
    align-items: center;
    margin-bottom: 12px;
  }

  .logo-image {
    display: block;
    width: 48px;
    height: 48px;
    object-fit: contain;
  }

  .brand-title {
    font-size: 24px;
    font-weight: 700;
    color: var(--app-text);
    margin: 0 0 6px;
    letter-spacing: 1px;
  }

  .brand-subtitle {
    font-size: 13px;
    color: #9ca3af;
    margin: 0;
  }
}

.register-card {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid rgba(0, 0, 0, 0.04);
  border-radius: 16px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.02), 0 8px 10px -6px rgba(0, 0, 0, 0.02) !important;
  background-color: var(--app-surface);

  :deep(.el-card__body) {
    padding: 40px 36px;
  }

  .form-title {
    font-size: 22px;
    font-weight: 700;
    color: var(--app-text);
    text-align: center;
    margin: 0 0 8px;
  }

  .form-subtitle {
    font-size: 13px;
    color: #9ca3af;
    text-align: center;
    margin: 0 0 32px;
  }
}

.register-form :deep(.el-form-item__label) {
  padding: 0;
  margin-bottom: 6px;
  font-weight: 600;
  color: var(--app-text-regular);
}

.register-form :deep(.el-input__wrapper) {
  padding: 8px 12px;
  background-color: var(--app-surface-muted);
  border: 1px solid transparent;
  border-radius: 8px;
  box-shadow: none !important;
  transition: all 0.2s ease;
}

.register-form :deep(.el-input__wrapper.is-focus) {
  background-color: var(--app-surface);
  border-color: var(--app-primary);
}

.register-form :deep(.el-input__inner) {
  height: 24px;
  color: var(--app-text);
}

.submit-item {
  margin-top: 32px;
  margin-bottom: 16px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  background-color: var(--app-primary) !important;
  border-color: var(--app-primary) !important;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 2px;

  &:hover,
  &:focus {
    background-color: var(--app-primary-strong) !important;
    border-color: var(--app-primary-strong) !important;
  }
}

.footer-link {
  margin-top: 16px;
  font-size: 13px;
  color: #9ca3af;
  display: flex;
  align-items: center;
  gap: 4px;

  .el-link {
    font-size: 13px;
    color: var(--app-primary-strong);
    font-weight: 500;

    &:hover {
      color: var(--app-primary-strong);
      text-decoration: underline;
    }
  }
}

@media (max-width: 576px) {
  .register-container {
    justify-content: flex-start;
    padding: 20px;
    padding-top: 12vh;
    align-items: flex-start;
  }

  .register-content {
    max-width: 100%;
  }

  .register-card {
    width: 92%;
    border: none;
    background: var(--app-surface);
    box-shadow: none !important;

    :deep(.el-card__body) {
      padding: 32px 24px;
    }
  }

  .register-container {
    padding-bottom: 24px;
  }
}
</style>
