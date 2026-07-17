<template>
  <div class="app-layout">
    <div class="sidebar-overlay" :class="{ 'is-visible': isMobileSidebarOpen }" @click="isMobileSidebarOpen = false" />

    <aside class="sidebar" :class="{ 'is-mobile-open': isMobileSidebarOpen }">
      <div class="sidebar-header">
        <div class="sidebar-actions">
          <el-button class="new-chat-btn" plain :disabled="isGenerating" @click="handleNewChat">
            <el-icon>
              <Plus />
            </el-icon>
            新对话
          </el-button>
          <el-tooltip content="搜索聊天记录" placement="bottom">
            <el-button class="search-trigger" text circle aria-label="搜索聊天记录" @click="openSearchDialog">
              <el-icon>
                <Search />
              </el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <nav v-loading="loadingConversations" class="session-list">
        <div v-for="session in sessions" :key="session.id" class="session-item"
          :class="{ 'is-active': session.id === activeSessionId }">
          <button class="session-select" :disabled="isGenerating" @click="selectSession(session.id)">
            <span class="session-title">{{ session.title }}</span>
          </button>
          <el-tooltip content="删除会话" placement="right">
            <el-button class="session-delete" text circle aria-label="删除会话" :loading="deletingSessionId === session.id"
              :disabled="isGenerating || deletingSessionId !== null" @click.stop="deleteSession(session.id)">
              <el-icon>
                <Delete />
              </el-icon>
            </el-button>
          </el-tooltip>
        </div>
        <div v-if="!loadingConversations && sessions.length === 0" class="empty-session-state">
          <el-empty description="暂无历史对话">
            <template #image>
              <el-icon class="empty-session-icon">
                <ChatDotRound />
              </el-icon>
            </template>
          </el-empty>
        </div>
      </nav>
    </aside>

    <el-dialog v-model="searchDialogVisible" title="搜索聊天记录" width="560px" class="search-dialog" @closed="resetSearch">
      <el-input ref="searchInputRef" v-model="searchKeyword" placeholder="搜索会话标题或消息内容" clearable
        class="search-dialog-input" @input="scheduleSearch" @keyup.enter="searchConversations">
        <template #append>
          <el-button :loading="searching" aria-label="搜索" @click="searchConversations">
            <el-icon>
              <Search />
            </el-icon>
          </el-button>
        </template>
      </el-input>

      <div v-loading="searching" class="search-result-list">
        <button v-for="conversation in searchResults" :key="conversation.id" class="search-result-item"
          :disabled="isGenerating" @click="selectSearchResult(conversation)">
          <span class="search-result-title">{{ conversation.title }}</span>
          <span class="search-result-time">{{ formatConversationTime(conversation.lastMessageAt ||
            conversation.createdAt)
            }}</span>
        </button>
        <el-empty v-if="searchKeyword.trim() && !searching && searchResults.length === 0" :image-size="72"
          description="未找到匹配的聊天记录" />
      </div>
    </el-dialog>

    <section class="chat-container" :class="{ 'is-draft': isDraftConversation }">
      <header class="chat-header">
        <el-button class="menu-toggle-btn" text circle aria-label="打开聊天列表" @click="isMobileSidebarOpen = true">
          <el-icon>
            <Expand />
          </el-icon>
        </el-button>
        <h1 class="title">{{ activeSession?.title || '新对话' }}</h1>
      </header>

      <p v-if="isDraftConversation" class="new-chat-placeholder">开始一段新的对话</p>

      <main ref="chatMainRef" v-loading="loadingMessages" class="chat-main">
        <el-empty v-if="activeSession && !loadingMessages && displayedMessages.length === 0" description="暂无聊天消息" />
        <article v-for="message in displayedMessages" :key="message.id" class="message-row"
          :class="`is-${message.role}`">
          <el-avatar class="avatar" :size="34">{{ message.role === 'user' ? '我' : 'AI' }}</el-avatar>
          <div class="message-bubble">
            <p v-if="message.role === 'user'" class="content">{{ message.content }}</p>
            <div v-else class="markdown-content" v-html="renderMarkdown(message.content)" />
          </div>
        </article>
        <div ref="scrollAnchor" />
      </main>

      <footer class="chat-footer" :class="{ 'is-draft': isDraftConversation }">
        <div class="input-wrapper">
          <el-input ref="chatInputRef" v-model="inputText" type="textarea" :autosize="{ minRows: 1, maxRows: 6 }"
            resize="none" placeholder="有问题，尽管问" class="chat-input" :disabled="isGenerating"
            @keydown.enter.exact.prevent="handleSend" />
          <el-button v-if="!isGenerating" type="primary" circle class="send-btn" aria-label="发送消息"
            :disabled="!inputText.trim()" @click="handleSend">
            <el-icon>
              <Promotion />
            </el-icon>
          </el-button>
          <el-button v-else type="danger" circle plain class="stop-btn" aria-label="停止生成" @click="stopGeneration">
            <el-icon>
              <VideoPause />
            </el-icon>
          </el-button>
        </div>
      </footer>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Delete, Expand, Plus, Promotion, Search, VideoPause } from '@element-plus/icons-vue'
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import { chatApi, type ChatMessage, type Conversation } from '@/utils/api'
import { useAccountStore } from '@/stores/account'

interface ChatSession extends Conversation {
  messages: ChatMessage[]
}

const inputText = ref('')
const sessions = ref<ChatSession[]>([])
const draftMessages = ref<ChatMessage[]>([])
const activeSessionId = ref<string | null>(null)
const isGenerating = ref(false)
const isMobileSidebarOpen = ref(false)
const loadingConversations = ref(false)
const loadingMessages = ref(false)
const deletingSessionId = ref<string | null>(null)
const searchDialogVisible = ref(false)
const searchKeyword = ref('')
const searchResults = ref<Conversation[]>([])
const searching = ref(false)
const chatMainRef = ref<HTMLElement | null>(null)
const chatInputRef = ref<{ focus: () => void } | null>(null)
const searchInputRef = ref<{ focus: () => void } | null>(null)
const scrollAnchor = ref<HTMLElement | null>(null)
const accountStore = useAccountStore()

let searchTimer: ReturnType<typeof window.setTimeout> | undefined
let latestSearchRequestId = 0
let streamAbortController: AbortController | undefined
let titleRefreshTimers: ReturnType<typeof window.setTimeout>[] = []

const activeSession = computed(() => sessions.value.find((session) => session.id === activeSessionId.value))
const displayedMessages = computed(() => activeSession.value?.messages ?? draftMessages.value)
const isDraftConversation = computed(() => !activeSession.value && displayedMessages.value.length === 0)

async function loadConversations(selectFirst = false) {
  loadingConversations.value = true
  try {
    const conversations = await chatApi.listConversations()
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
  draftMessages.value = []
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
  if (isGenerating.value) return
  activeSessionId.value = null
  draftMessages.value = []
  isMobileSidebarOpen.value = false
  await scrollToBottom(false)
  await focusChatInput()
}

async function openSearchDialog() {
  searchDialogVisible.value = true
  await nextTick()
  searchInputRef.value?.focus()
}

function scheduleSearch() {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => void searchConversations(), 250)
}

async function searchConversations() {
  const keyword = searchKeyword.value.trim()
  const requestId = ++latestSearchRequestId
  if (!keyword) {
    searchResults.value = []
    searching.value = false
    return
  }

  searching.value = true
  try {
    const results = await chatApi.searchConversations(keyword)
    if (requestId === latestSearchRequestId) {
      searchResults.value = results
    }
  } finally {
    if (requestId === latestSearchRequestId) {
      searching.value = false
    }
  }
}

async function selectSearchResult(conversation: Conversation) {
  if (isGenerating.value) return
  if (!sessions.value.some((session) => session.id === conversation.id)) {
    sessions.value.unshift({ ...conversation, messages: [] })
  }
  searchDialogVisible.value = false
  await selectSession(conversation.id)
}

function resetSearch() {
  if (searchTimer) window.clearTimeout(searchTimer)
  latestSearchRequestId += 1
  searchKeyword.value = ''
  searchResults.value = []
  searching.value = false
}

function formatConversationTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
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
      } else {
        await handleNewChat()
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
  if (!content || isGenerating.value) return

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
  isGenerating.value = true
  const controller = new AbortController()
  streamAbortController = controller
  let createdConversation: Conversation | null = null
  let messageList = session?.messages ?? draftMessages.value
  let messagesAdded = false

  try {
    if (!session) {
      createdConversation = await chatApi.createConversation()
    }
    const conversationId = createdConversation?.id ?? session?.id
    if (!conversationId) throw new Error('创建会话失败')

    messageList.push(userMessage, assistantMessage)
    messagesAdded = true
    inputText.value = ''
    await scrollToBottom()

    await chatApi.streamMessage(conversationId, content, controller.signal, (event) => {
      if (session && activeSessionId.value !== conversationId) return
      if (event.type === 'delta' && event.delta) {
        assistantMessage.content += event.delta
        if (createdConversation) {
          promoteCreatedConversation(createdConversation, messageList)
        }
        void scrollToBottom()
      }
      if (event.type === 'done') {
        if (event.message) {
          assistantMessage.id = event.message.id
          assistantMessage.content = event.message.content
          assistantMessage.createdAt = event.message.createdAt
          if (createdConversation) {
            promoteCreatedConversation(createdConversation, messageList)
          }
          scheduleTitleRefresh()
        }
        finishGeneration(controller)
      }
      if (event.type === 'error') {
        throw new Error(event.error || 'AI 回复失败')
      }
    })
  } catch (error) {
    if (!(error instanceof DOMException && error.name === 'AbortError')) {
      const message = error instanceof Error ? error.message : 'AI 回复失败'
      ElMessage.error(message)
    }
    if (messagesAdded && !assistantMessage.content) {
      messageList = messageList.filter((message) => message !== userMessage && message !== assistantMessage)
      if (session) {
        session.messages = messageList
      } else {
        draftMessages.value = messageList
      }
      if (createdConversation && activeSessionId.value !== createdConversation.id) {
        await chatApi.deleteConversation(createdConversation.id)
      }
    }
  } finally {
    if (streamAbortController === controller) {
      isGenerating.value = false
      streamAbortController = undefined
    }
    void accountStore.refreshBalance()
  }
}

function promoteCreatedConversation(conversation: Conversation, messages: ChatMessage[]) {
  if (activeSessionId.value === conversation.id) return
  sessions.value.unshift({ ...conversation, messages })
  activeSessionId.value = conversation.id
  draftMessages.value = []
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

async function focusChatInput() {
  await nextTick()
  chatInputRef.value?.focus()
}

onMounted(() => {
  void initializeChat()
})

async function initializeChat() {
  await loadConversations(true)
  if (sessions.value.length === 0 && !activeSessionId.value) {
    await handleNewChat()
  }
}

onBeforeUnmount(() => {
  if (searchTimer) window.clearTimeout(searchTimer)
  titleRefreshTimers.forEach((timer) => window.clearTimeout(timer))
  streamAbortController?.abort()
})
</script>

<style scoped lang="scss">
$bg-color: var(--app-bg);
$surface-color: var(--app-surface);
$border-color: var(--app-border);
$text-primary: var(--app-text);
$text-regular: var(--app-text-regular);
$text-secondary: var(--app-text-muted);
$accent: var(--app-primary);

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
  background: var(--app-surface);
  z-index: 2;
}

.sidebar-header {
  padding: 16px;
}

.sidebar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.new-chat-btn {
  display: inline-flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  height: 38px;
  border-radius: 12px;
  border-color: #99f6e4;
  color: var(--app-primary-strong);
}

.search-trigger {
  width: 38px;
  height: 38px;
  border: 1px solid $border-color;
  color: $text-regular;
}

.session-list {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  overflow-y: auto;
  padding: 8px;
}

.empty-session-state {
  display: flex;
  min-height: 0;
  flex: 1;
  align-items: center;
  justify-content: center;
}

.empty-session-icon {
  color: $text-secondary;
  font-size: 44px;
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
    background: var(--app-surface-subtle);

    .session-delete:not(:disabled) {
      opacity: 1;
    }
  }

  &.is-active {
    background: var(--app-primary-soft);
    color: var(--app-primary-strong);
    font-weight: 600;

    .session-select {
      color: var(--app-primary-strong);
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

.search-result-list {
  min-height: 160px;
  max-height: 360px;
  overflow-y: auto;
  padding-top: 12px;
}

:global(.search-dialog) {
  max-width: calc(100% - 32px);
}

.search-result-item {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 4px;
  border: 0;
  border-bottom: 1px solid #f1f5f9;
  padding: 12px 4px;
  background: transparent;
  color: $text-primary;
  cursor: pointer;
  text-align: left;

  &:hover:not(:disabled) {
    background: var(--app-surface-muted);
  }

  &:disabled {
    cursor: wait;
  }
}

.search-result-title {
  overflow: hidden;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-result-time {
  color: $text-secondary;
  font-size: 12px;
}

.chat-container {
  display: flex;
  position: relative;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  background: $surface-color;
}

.chat-header {
  display: flex;
  min-height: 70px;
  align-items: center;
  gap: 8px;
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
  -webkit-tap-highlight-color: transparent;

  &:focus,
  &:focus-visible {
    outline: none;
    background: transparent !important;
    box-shadow: none;
  }
}

.chat-main {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 28px clamp(16px, 5vw, 72px);
  background: $surface-color;
}

.new-chat-placeholder {
  position: absolute;
  top: calc(54% - 44px);
  right: 0;
  left: 0;
  margin: 0;
  color: var(--app-text);
  font-size: 22px;
  font-weight: 700;
  line-height: 24px;
  text-align: center;
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
      background: var(--app-user-bubble);
      color: var(--app-user-bubble-text);
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
    background: var(--app-code-bg);
    color: var(--app-code-text);
  }

  :deep(code) {
    border-radius: 3px;
    padding: 2px 4px;
    background: var(--app-surface-subtle);
    font-family: Consolas, monospace;
  }

  :deep(pre code) {
    padding: 0;
    background: transparent;
  }

  :deep(a) {
    color: var(--app-primary-strong);
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
  background: $surface-color;
}

.chat-footer.is-draft {
  position: absolute;
  top: 54%;
  right: 0;
  left: 0;
  padding: 0 clamp(16px, 5vw, 72px);
  background: transparent;
}

.chat-footer.is-draft .input-wrapper {
  max-width: 760px;
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
