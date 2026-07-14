<template>
  <div class="app-layout">
    <div class="sidebar-overlay" :class="{ 'is-visible': isMobileSidebarOpen }" @click="isMobileSidebarOpen = false" />

    <aside class="sidebar" :class="{ 'is-mobile-open': isMobileSidebarOpen }">
      <div class="sidebar-header">
        <el-button class="new-chat-btn" plain :loading="creatingConversation" @click="handleNewChat">
          <el-icon><Plus /></el-icon>
          新聊天
        </el-button>
        <el-input v-model="searchQuery" placeholder="搜索聊天记录" clearable class="search-input">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <nav v-loading="loadingConversations" class="session-list">
        <div
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ 'is-active': session.id === activeSessionId }"
        >
          <button class="session-select" :disabled="isGenerating" @click="selectSession(session.id)">
            <span class="session-title">{{ session.title }}</span>
          </button>
          <el-tooltip content="删除会话" placement="right">
            <el-button
              class="session-delete"
              text
              circle
              aria-label="删除会话"
              :loading="deletingSessionId === session.id"
              :disabled="isGenerating || deletingSessionId !== null"
              @click.stop="deleteSession(session.id)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
        <el-empty v-if="!loadingConversations && sessions.length === 0" :image-size="56" description="暂无聊天记录" />
      </nav>
    </aside>

    <section class="chat-container">
      <header class="chat-header">
        <el-button class="menu-toggle-btn" text circle aria-label="打开聊天列表" @click="isMobileSidebarOpen = true">
          <el-icon><Expand /></el-icon>
        </el-button>
        <h1 class="title">{{ activeSession?.title || '智能对话' }}</h1>
      </header>

      <main ref="chatMainRef" v-loading="loadingMessages" class="chat-main">
        <el-empty v-if="activeSession && !loadingMessages && activeSession.messages.length === 0" description="开始一段新的对话" />
        <article
          v-for="message in activeSession?.messages"
          :key="message.id"
          class="message-row"
          :class="`is-${message.role}`"
        >
          <el-avatar class="avatar" :size="34">{{ message.role === 'user' ? '我' : 'AI' }}</el-avatar>
          <div class="message-bubble">
            <p v-if="message.role === 'user'" class="content">{{ message.content }}</p>
            <div v-else class="markdown-content" v-html="renderMarkdown(message.content)" />
          </div>
        </article>
        <div ref="scrollAnchor" />
      </main>

      <footer class="chat-footer">
        <div class="input-wrapper">
          <el-input
            v-model="inputText"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 6 }"
            resize="none"
            placeholder="输入消息，Enter 发送，Shift + Enter 换行"
            class="chat-input"
            :disabled="!activeSession || isGenerating"
            @keydown.enter.exact.prevent="handleSend"
          />
          <el-button
            v-if="!isGenerating"
            type="primary"
            circle
            class="send-btn"
            aria-label="发送消息"
            :disabled="!inputText.trim() || !activeSession"
            @click="handleSend"
          >
            <el-icon><Promotion /></el-icon>
          </el-button>
          <el-button v-else type="danger" circle plain class="stop-btn" aria-label="停止生成" @click="stopGeneration">
            <el-icon><VideoPause /></el-icon>
          </el-button>
        </div>
      </footer>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Expand, Plus, Promotion, Search, VideoPause } from '@element-plus/icons-vue'
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import { chatApi, type ChatMessage, type Conversation } from '@/utils/api'
import { useAccountStore } from '@/stores/account'

interface ChatSession extends Conversation {
  messages: ChatMessage[]
}

const inputText = ref('')
const searchQuery = ref('')
const sessions = ref<ChatSession[]>([])
const activeSessionId = ref<string | null>(null)
const isGenerating = ref(false)
const isMobileSidebarOpen = ref(false)
const loadingConversations = ref(false)
const loadingMessages = ref(false)
const creatingConversation = ref(false)
const deletingSessionId = ref<string | null>(null)
const chatMainRef = ref<HTMLElement | null>(null)
const scrollAnchor = ref<HTMLElement | null>(null)
const accountStore = useAccountStore()

let searchTimer: ReturnType<typeof window.setTimeout> | undefined
let streamAbortController: AbortController | undefined
let titleRefreshTimers: ReturnType<typeof window.setTimeout>[] = []

const activeSession = computed(() => sessions.value.find((session) => session.id === activeSessionId.value))

watch(searchQuery, () => {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => void loadConversations(), 250)
})

async function loadConversations(selectFirst = false) {
  loadingConversations.value = true
  try {
    const conversations = await chatApi.listConversations(searchQuery.value.trim() || undefined)
    const existingMessages = new Map(sessions.value.map((session) => [session.id, session.messages]))
    sessions.value = conversations.map((conversation) => ({
      ...conversation,
      messages: existingMessages.get(conversation.id) || [],
    }))
    const firstSession = sessions.value[0]
    if (selectFirst && firstSession && !activeSessionId.value) {
      await selectSession(firstSession.id)
    }
  } finally {
    loadingConversations.value = false
  }
}

async function selectSession(id: string) {
  if (id === activeSessionId.value || isGenerating.value) return
  activeSessionId.value = id
  isMobileSidebarOpen.value = false
  const session = sessions.value.find((item) => item.id === id)
  if (!session) return

  loadingMessages.value = true
  try {
    session.messages = await chatApi.listMessages(id)
    await scrollToBottom(false)
  } finally {
    loadingMessages.value = false
  }
}

async function handleNewChat() {
  if (isGenerating.value || creatingConversation.value) return
  creatingConversation.value = true
  try {
    const conversation = await chatApi.createConversation()
    sessions.value.unshift({ ...conversation, messages: [] })
    activeSessionId.value = conversation.id
    isMobileSidebarOpen.value = false
    await scrollToBottom(false)
  } finally {
    creatingConversation.value = false
  }
}

async function deleteSession(id: string) {
  if (isGenerating.value || deletingSessionId.value) return
  try {
    await ElMessageBox.confirm('删除后无法恢复该会话及其聊天记录。', '删除会话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  const index = sessions.value.findIndex((session) => session.id === id)
  if (index < 0) return
  deletingSessionId.value = id
  try {
    await chatApi.deleteConversation(id)
    const wasActive = activeSessionId.value === id
    sessions.value = sessions.value.filter((session) => session.id !== id)
    if (wasActive) {
      activeSessionId.value = null
      const nextSession = sessions.value[index] || sessions.value[index - 1]
      if (nextSession) {
        await selectSession(nextSession.id)
      }
    }
    ElMessage.success('会话已删除')
  } finally {
    deletingSessionId.value = null
  }
}

async function handleSend() {
  const content = inputText.value.trim()
  const session = activeSession.value
  if (!content || !session || isGenerating.value) return

  const userMessage: ChatMessage = {
    id: `temporary-user-${Date.now()}`,
    role: 'user',
    content,
    createdAt: new Date().toISOString(),
  }
  const assistantMessage: ChatMessage = {
    id: `temporary-assistant-${Date.now()}`,
    role: 'assistant',
    content: '',
    createdAt: new Date().toISOString(),
  }
  const conversationId = session.id
  session.messages.push(userMessage, assistantMessage)
  inputText.value = ''
  isGenerating.value = true
  const controller = new AbortController()
  streamAbortController = controller
  await scrollToBottom()

  try {
    await chatApi.streamMessage(conversationId, content, controller.signal, (event) => {
      if (activeSessionId.value !== conversationId) return
      if (event.type === 'delta' && event.delta) {
        assistantMessage.content += event.delta
        void scrollToBottom()
      }
      if (event.type === 'done') {
        if (event.message) {
          assistantMessage.id = event.message.id
          assistantMessage.content = event.message.content
          assistantMessage.createdAt = event.message.createdAt
          scheduleTitleRefresh()
        }
        finishGeneration(controller)
      }
      if (event.type === 'error') {
        throw new Error(event.error || 'AI response failed')
      }
    })
  } catch (error) {
    if (!(error instanceof DOMException && error.name === 'AbortError')) {
      const message = error instanceof Error ? error.message : 'AI response failed'
      ElMessage.error(message)
    }
    if (!assistantMessage.content) {
      session.messages = session.messages.filter((message) => message !== assistantMessage)
    }
  } finally {
    if (streamAbortController === controller) {
      isGenerating.value = false
      streamAbortController = undefined
    }
    void accountStore.refreshBalance()
  }
}

function scheduleTitleRefresh() {
  titleRefreshTimers.forEach((timer) => window.clearTimeout(timer))
  titleRefreshTimers = [1_000, 3_500].map((delay) => window.setTimeout(() => void loadConversations(), delay))
}

function finishGeneration(controller: AbortController) {
  if (streamAbortController === controller) {
    isGenerating.value = false
    streamAbortController = undefined
  }
}

function stopGeneration() {
  streamAbortController?.abort()
  ElMessage.info('已停止生成')
}

function renderMarkdown(content: string) {
  return DOMPurify.sanitize(marked.parse(content || '...') as string)
}

async function scrollToBottom(smooth = true) {
  await nextTick()
  scrollAnchor.value?.scrollIntoView({ behavior: smooth ? 'smooth' : 'auto', block: 'end' })
}

onMounted(() => {
  void loadConversations(true)
})

onBeforeUnmount(() => {
  if (searchTimer) window.clearTimeout(searchTimer)
  titleRefreshTimers.forEach((timer) => window.clearTimeout(timer))
  streamAbortController?.abort()
})
</script>

<style scoped lang="scss">
$bg-color: #f8fafc;
$surface-color: #ffffff;
$border-color: #e5e7eb;
$text-primary: #1f2937;
$text-regular: #4b5563;
$text-secondary: #9ca3af;
$accent: #0d9488;

.app-layout {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: $bg-color;
}

.sidebar {
  display: flex;
  width: 280px;
  flex: 0 0 280px;
  flex-direction: column;
  border-right: 1px solid $border-color;
  background: $surface-color;
  z-index: 2;
}

.sidebar-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid $border-color;
}

.new-chat-btn {
  justify-content: flex-start;
  height: 38px;
  border-color: #99f6e4;
  color: #0f766e;
}

.search-input :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px $border-color inset;
}

.session-list {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  display: flex;
  width: 100%;
  align-items: center;
  margin-bottom: 3px;
  overflow: hidden;
  border-radius: 6px;
  background: transparent;
  color: $text-regular;

  &:hover {
    background: #f1f5f9;

    .session-delete:not(:disabled) {
      opacity: 1;
    }
  }

  &.is-active {
    background: #ccfbf1;
    color: #0f766e;
    font-weight: 600;

    .session-select {
      color: #0f766e;
      font-weight: 600;
    }
  }
}

.session-select {
  min-width: 0;
  flex: 1;
  border: 0;
  padding: 11px 4px 11px 12px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;

  &:disabled {
    cursor: wait;
  }
}

.session-delete {
  width: 30px;
  height: 30px;
  margin-right: 4px;
  color: $text-secondary;
  opacity: 0;

  &:hover:not(:disabled) {
    color: #dc2626;
  }
}

@media (max-width: 768px) {
  .session-delete {
    opacity: 1;
  }
}

.session-title {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-container {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.chat-header {
  display: flex;
  min-height: 60px;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid $border-color;
  padding: 0 24px;
  background: $surface-color;
}

.title {
  margin: 0;
  overflow: hidden;
  color: $text-primary;
  font-size: 16px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-toggle-btn {
  display: none;
}

.chat-main {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 28px clamp(16px, 5vw, 72px);
}

.message-row {
  display: flex;
  max-width: 920px;
  gap: 12px;
  margin: 0 auto 24px;
  align-items: flex-start;

  &.is-user {
    flex-direction: row-reverse;

    .avatar {
      background: $accent;
    }

    .message-bubble {
      background: #ccfbf1;
    }
  }

  &.is-assistant .avatar {
    border: 1px solid $border-color;
    background: $surface-color;
    color: $accent;
  }
}

.message-bubble {
  max-width: min(82%, 760px);
  border: 1px solid $border-color;
  border-radius: 8px;
  padding: 12px 16px;
  background: $surface-color;
  color: $text-primary;
  line-height: 1.7;
  overflow-wrap: anywhere;
}

.content {
  margin: 0;
  white-space: pre-wrap;
}

.markdown-content {
  :deep(*) {
    max-width: 100%;
  }

  :deep(p:first-child),
  :deep(h1:first-child),
  :deep(h2:first-child),
  :deep(h3:first-child) {
    margin-top: 0;
  }

  :deep(p:last-child) {
    margin-bottom: 0;
  }

  :deep(pre) {
    overflow-x: auto;
    border-radius: 6px;
    padding: 12px;
    background: #0f172a;
    color: #e2e8f0;
  }

  :deep(code) {
    border-radius: 3px;
    padding: 2px 4px;
    background: #f1f5f9;
    font-family: Consolas, monospace;
  }

  :deep(pre code) {
    padding: 0;
    background: transparent;
  }

  :deep(a) {
    color: #0f766e;
  }

  :deep(table) {
    border-collapse: collapse;
  }

  :deep(th),
  :deep(td) {
    border: 1px solid $border-color;
    padding: 6px 8px;
  }
}

.chat-footer {
  padding: 16px clamp(16px, 5vw, 72px) 24px;
  background: $bg-color;
}

.input-wrapper {
  display: flex;
  max-width: 920px;
  margin: 0 auto;
  align-items: flex-end;
  gap: 8px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 8px;
  background: $surface-color;

  &:focus-within {
    border-color: $accent;
    box-shadow: 0 0 0 2px rgba(13, 148, 136, 0.12);
  }
}

.chat-input {
  flex: 1;

  :deep(.el-textarea__inner) {
    box-shadow: none;
  }
}

.send-btn {
  border-color: $accent;
  background: $accent;
}

.sidebar-overlay {
  display: none;
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    top: 0;
    bottom: 0;
    left: -280px;
    transition: transform 0.2s ease;

    &.is-mobile-open {
      transform: translateX(280px);
    }
  }

  .sidebar-overlay.is-visible {
    position: fixed;
    z-index: 1;
    display: block;
    inset: 0;
    background: rgba(15, 23, 42, 0.28);
  }

  .menu-toggle-btn {
    display: inline-flex;
  }

  .chat-header {
    padding: 0 12px;
  }

  .chat-main {
    padding: 16px;
  }

  .message-bubble {
    max-width: 84%;
  }

  .chat-footer {
    padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  }
}
</style>
