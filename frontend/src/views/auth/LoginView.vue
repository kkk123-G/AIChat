<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  email: '',
  password: '',
})

function goToRegister() {
  void router.push({ name: 'register' })
}

function submit() {
  loading.value = true
  // 登录接口接入后，在此处保存 access token 并跳转到 redirect 或聊天页。
  loading.value = false
}
</script>

<template>
  <main class="auth-page">
    <el-card class="auth-card" shadow="never">
      <h1>登录 AI Chat</h1>
      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" autocomplete="email" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            show-password
          />
        </el-form-item>
        <el-button class="submit-button" type="primary" native-type="submit" :loading="loading">
          登录
        </el-button>
      </el-form>
      <p class="auth-footer">还没有账号？<el-button text type="primary" @click="goToRegister">注册</el-button></p>
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
