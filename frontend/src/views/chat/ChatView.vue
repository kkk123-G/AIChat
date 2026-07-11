<script setup lang="ts">
import { ref } from 'vue'

const prompt = ref('')
</script>

<template>
  <main class="chat-page">
    <header class="chat-header">
      <strong>AI Chat</strong>
    </header>
    <section class="message-list" aria-label="对话消息">
      <el-empty description="开始一段新的对话" />
    </section>
    <footer class="chat-composer">
      <el-input v-model="prompt" type="textarea" :rows="3" placeholder="输入消息，按 Enter 发送" />
      <el-button type="primary" :disabled="!prompt.trim()">发送</el-button>
    </footer>
  </main>
</template>

<style scoped>
.chat-page {
  display: grid;
  grid-template-rows: auto 1fr auto;
  height: 100vh;
  background: #fff;
}

.chat-header,
.chat-composer {
  padding: 16px 24px;
  border-color: #ebeef5;
  border-style: solid;
}

.chat-header {
  border-width: 0 0 1px;
}

.message-list {
  display: grid;
  place-items: center;
}

.chat-composer {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  border-width: 1px 0 0;
}
</style>
