<template>
  <div v-loading="loading" class="profile-container">
    <template v-if="userInfo">
      <el-card class="profile-card custom-card" shadow="never">
        <div class="profile-header">
          <el-avatar :size="104" class="avatar"
            :style="{ backgroundColor: 'var(--app-primary-soft)', color: 'var(--app-primary-strong)', fontSize: '32px', fontWeight: '600' }">
            {{ avatarText }}
          </el-avatar>
          <div class="user-info">
            <div class="name-row">
              <span class="username">{{ userInfo.username }}</span>
              <el-tag :type="userInfo.status === 'ENABLED' ? 'success' : 'info'" effect="light" round
                class="status-tag">
                {{ userInfo.status === 'ENABLED' ? '启用' : '停用' }}
              </el-tag>
            </div>
            <div class="role-row">
              <span class="role">{{ userInfo.role === 'ADMIN' ? '管理员' : '普通用户' }}</span>
            </div>
          </div>
        </div>

        <el-divider border-style="dashed" class="custom-divider" />

        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-label">总调用次数</div>
            <div class="stat-value highlight">{{ userInfo.totalCalls }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">账户余额</div>
            <div class="stat-value highlight">￥{{ Number(userInfo.balance).toFixed(2) }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">注册时间</div>
            <div class="stat-value time">{{ formatDate(userInfo.createdAt) }}</div>
          </div>
        </div>
      </el-card>

      <div class="contact-card custom-card">
        <div class="contact-icon-wrapper">
          <el-icon :size="32" class="contact-icon">
            <ChatDotRound />
          </el-icon>
        </div>
        <div class="contact-info">
          <div class="contact-title">联系客服</div>
          <div class="contact-number">2938374296</div>
        </div>
      </div>

      <el-card class="password-card custom-card" shadow="never">
        <template #header>
          <div class="card-title">修改密码</div>
        </template>

        <el-form ref="passwordFormRef" :model="pwdForm" :rules="passwordRules" label-position="top"
          class="password-form">
          <el-form-item label="当前密码" prop="currentPassword">
            <el-input v-model="pwdForm.currentPassword" type="password" show-password placeholder="请输入当前密码"
              class="custom-input" />
          </el-form-item>

          <el-form-item label="新密码" prop="newPassword" class="new-pwd-item">
            <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 8 个字符"
              class="custom-input" />
            <!-- <div class="form-hint"></div> -->
          </el-form-item>

          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="确认您的新密码"
              class="custom-input" />
          </el-form-item>

          <div class="form-actions">
            <el-button type="primary" class="btn-submit" :loading="submitting" @click="handleUpdatePassword">
              修改密码
            </el-button>
          </div>
        </el-form>
      </el-card>
    </template>
    <el-empty v-else-if="!loading" description="个人资料加载失败" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ChatDotRound } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { userAccountApi, type UserProfile } from '@/utils/api'

interface PasswordForm {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

const userInfo = ref<UserProfile | null>(null)
const loading = ref(false)
const submitting = ref(false)
const passwordFormRef = ref<FormInstance>()
const avatarText = computed(() => userInfo.value?.username.slice(0, 2).toUpperCase() ?? '')
const pwdForm = reactive<PasswordForm>({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const passwordRules: FormRules<PasswordForm> = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 72, message: '新密码需要为 8-72 个字符', trigger: 'blur' },
  ],
  confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }],
}

async function loadProfile() {
  loading.value = true
  try {
    userInfo.value = await userAccountApi.profile()
  } catch {
    userInfo.value = null
  } finally {
    loading.value = false
  }
}

async function handleUpdatePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid || submitting.value) {
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }

  submitting.value = true
  try {
    await userAccountApi.changePassword({
      currentPassword: pwdForm.currentPassword,
      newPassword: pwdForm.newPassword,
    })
    ElMessage.success('密码修改成功')
    passwordFormRef.value?.resetFields()
  } finally {
    submitting.value = false
  }
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(new Date(value))
}

onMounted(() => void loadProfile())
</script>

<style lang="scss" scoped>
$theme-green: var(--app-primary);
$text-main: var(--app-text);
$text-regular: var(--app-text-regular);
$text-secondary: var(--app-text-muted);

.profile-container {
  padding: 30px;
  max-width: 990px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 30px;

  .custom-card {
    background-color: var(--app-surface);
    border: 1px solid var(--app-border);
    border-radius: 12px;
    box-sizing: border-box;
    width: 100%;

    :deep(.el-card__header) {
      border-bottom: 1px solid var(--app-border-muted);
      padding: 24px 32px;
    }

    :deep(.el-card__body) {
      padding: 32px;
    }
  }

  .profile-card {
    .profile-header {
      display: flex;
      align-items: center;
      gap: 32px;

      .user-info {
        display: flex;
        flex-direction: column;
        gap: 12px;

        .name-row {
          display: flex;
          align-items: center;
          gap: 16px;

          .username {
            font-size: 28px;
            font-weight: 600;
            color: $text-main;
          }

          .status-tag {
            --el-tag-text-color: #11a983;
            --el-tag-bg-color: #e6f6f1;
            border: none;
            height: 28px;
            padding: 0 14px;
            font-size: 14px;
          }
        }

        .role-row {
          .role {
            font-size: 18px;
            color: $text-regular;
            background: var(--app-surface-subtle);
            padding: 6px 16px;
            border-radius: 6px;
          }
        }
      }
    }

    .custom-divider {
      margin: 32px 0;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 30px;

      .stat-item {
        display: flex;
        flex-direction: column;
        gap: 10px;

        .stat-label {
          font-size: 18px;
          color: $text-secondary;
        }

        .stat-value {
          font-size: 20px;
          color: $text-main;
          font-weight: 500;

          &.highlight {
            font-size: 28px;
            color: $theme-green;
            font-weight: 600;
          }

          &.time {
            font-size: 28px;
            font-weight: 600;
          }
        }
      }
    }
  }

  .contact-card {
    padding: 32px;
    display: flex;
    align-items: center;
    gap: 24px;

    .contact-icon-wrapper {
      width: 70px;
      height: 70px;
      background-color: var(--app-primary-soft);
      border-radius: 18px;
      display: flex;
      align-items: center;
      justify-content: center;

      .contact-icon {
        color: var(--app-primary-strong);
      }
    }

    .contact-info {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .contact-title {
        font-size: 24px;
        font-weight: bold;
        color: var(--app-primary-strong);
        letter-spacing: 1px;
      }

      .contact-number {
        font-size: 20px;
        color: var(--app-text);
      }
    }
  }

  .password-card {
    .card-title {
      font-size: 24px;
      font-weight: 600;
      color: $text-main;
    }

    .password-form {
      :deep(.el-form-item__label) {
        font-weight: 500;
        color: $text-regular;
        padding-bottom: 12px;
        font-size: 18px;
      }

      .custom-input {
        :deep(.el-input__wrapper) {
          padding: 10px 16px;
          box-shadow: 0 0 0 1px var(--app-border) inset;
          border-radius: 8px;

          &:hover,
          &.is-focus {
            box-shadow: 0 0 0 1px $theme-green inset;
          }
        }

        :deep(.el-input__inner) {
          font-size: 16px;
        }
      }

      .new-pwd-item {
        margin-bottom: 18px;

        .form-hint {
          font-size: 16px;
          color: $text-secondary;
          margin-top: 10px;
          line-height: 1;
        }
      }

      .form-actions {
        display: flex;
        justify-content: flex-end;
        margin-top: 24px;

        .btn-submit {
          background-color: $theme-green;
          border-color: $theme-green;
          padding: 24px 40px;
          font-size: 18px;
          border-radius: 8px;
          font-weight: 500;

          &:hover {
            opacity: 0.9;
          }
        }
      }
    }
  }
}

@media screen and (max-width: 768px) {
  .profile-container {
    padding: 16px;
    gap: 16px;

    .profile-card {
      .stats-grid {
        grid-template-columns: 1fr;
        gap: 16px;
      }

      .profile-header {
        flex-direction: column;
        align-items: flex-start;
      }
    }

    .password-card {
      .form-actions {
        justify-content: center !important;

        .btn-submit {
          width: 100%;
        }
      }
    }
  }
}
</style>
