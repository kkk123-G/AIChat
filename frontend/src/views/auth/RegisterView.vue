<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  email: '',
  password: '',
  confirmPassword: '',
})

function goToLogin() {
  void router.push({ name: 'login' })
}

function submit() {
  loading.value = true
  // 注册接口接入后，在此处完成账号创建并按产品策略跳转。
  loading.value = false
}
</script>

<template>
  <main class="auth-page">
    <el-card class="auth-card" shadow="never">
      <h1>创建账号</h1>
      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" autocomplete="email" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            autocomplete="new-password"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            autocomplete="new-password"
            show-password
          />
        </el-form-item>
        <el-button class="submit-button" type="primary" native-type="submit" :loading="loading">
          注册
        </el-button>
      </el-form>
      <p class="auth-footer">已有账号？<el-button text type="primary" @click="goToLogin">登录</el-button></p>
    </el-card>
  </main>
</template>

<style scoped>
.auth-page {
  display: grid;
  min-height: 100vh;
  padding: 24px;
  background: #f5f7fa;
  place-items: center;
}

.auth-card {
  width: min(100%, 400px);
}

h1 {
  margin: 0 0 24px;
  font-size: 24px;
  text-align: center;
}

.submit-button {
  width: 100%;
}

.auth-footer {
  margin: 20px 0 0;
  color: #606266;
  text-align: center;
}
</style>
