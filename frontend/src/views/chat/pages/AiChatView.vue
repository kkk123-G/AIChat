<template>
  <div class="app-layout">
    <!-- H5端 侧边栏遮罩层 -->
    <div class="sidebar-overlay" :class="{ 'is-visible': isMobileSidebarOpen }" @click="isMobileSidebarOpen = false">
    </div>

    <!-- 左侧聊天记录区域 -->
    <aside class="sidebar" :class="{ 'is-mobile-open': isMobileSidebarOpen }">
      <!-- 顶部操作区：新聊天 & 搜索 -->
      <div class="sidebar-header">
        <el-button class="new-chat-btn" plain @click="handleNewChat">
          <span class="plus-icon">+</span> 新聊天
        </el-button>
        <div class="search-wrapper">
          <el-input v-model="searchQuery" placeholder="搜索..." clearable class="search-input">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </div>

      <!-- 历史列表区 -->
      <nav class="session-list">
        <div v-for="session in filteredSessions" :key="session.id"
          :class="['session-item', { 'is-active': session.id === activeSessionId }]" @click="selectSession(session.id)">
          <div class="session-title">{{ session.title }}</div>
        </div>
        <div v-if="filteredSessions.length === 0" class="empty-search">
          无相关记录
        </div>
      </nav>
    </aside>

    <!-- 右侧聊天主区域 -->
    <div class="chat-container">
      <!-- 顶部标题栏 -->
      <header class="chat-header">
        <!-- H5端 切换侧边栏按钮 -->
        <button class="menu-toggle-btn" @click="isMobileSidebarOpen = true">
          <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none">
            <line x1="3" y1="12" x2="21" y2="12"></line>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <line x1="3" y1="18" x2="21" y2="18"></line>
          </svg>
        </button>
        <div class="header-text">
          <h1 class="title">{{ currentSession?.title || '智能对话' }}</h1>
        </div>
      </header>

      <!-- 聊天内容区域 -->
      <main class="chat-main" ref="chatMainRef">
        <div v-for="msg in currentSession?.messages" :key="msg.id" :class="['message-row', `is-${msg.role}`]">
          <div class="avatar">
            {{ msg.role === 'user' ? 'Me' : 'AI' }}
          </div>
          <div class="message-bubble">
            <p class="content">{{ msg.content }}</p>
          </div>
        </div>

        <!-- 占位符，用于自动滚动到底部 -->
        <div ref="scrollAnchor"></div>
      </main>

      <!-- 底部输入区域 -->
      <footer class="chat-footer">
        <div class="input-wrapper">
          <el-input v-model="inputText" type="textarea" :rows="1" autosize placeholder="有问题，尽管问" class="chat-input"
            @keydown.enter.prevent="handleSend" />

          <div class="action-btn-group">
            <el-button v-if="!isGenerating" type="primary" class="send-btn" :disabled="!inputText.trim()"
              @click="handleSend">
              发送
            </el-button>
            <el-button v-else type="info" plain class="stop-btn" @click="stopGeneration">
              停止
            </el-button>
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

// --- 类型定义 ---
interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
}

interface ChatSession {
  id: string
  title: string
  messages: Message[]
}

// --- 状态管理 ---
const inputText = ref('')
const searchQuery = ref('')
const isGenerating = ref(false)
const isMobileSidebarOpen = ref(false) // H5侧边栏开关
const activeSessionId = ref('1')

const chatMainRef = ref<HTMLElement | null>(null)
const scrollAnchor = ref<HTMLElement | null>(null)

// 模拟多条聊天历史数据
const sessions = ref<ChatSession[]>([
  {
    id: '1',
    title: '关于极简主义的探讨',
    messages: [
      { id: '1-1', role: 'assistant', content: '你好，我是你的专属助手。请问今天有什么我可以帮你的吗？' },
      { id: '1-2', role: 'user', content: '我想了解一下极简主义设计的核心理念是什么？' },
      { id: '1-3', role: 'assistant', content: '极简主义设计的核心理念可以概括为“少即是多（Less is more）”。它强调去除一切不必要的装饰和冗余元素，保留事物最本质的功能和结构。' }
    ]
  },
  {
    id: '2',
    title: 'Vue3响应式原理',
    messages: [
      { id: '2-1', role: 'user', content: '简述一下 Vue3 的 Proxy 相比 Vue2 有什么优势？' },
      { id: '2-2', role: 'assistant', content: 'Vue3 改用 Proxy 替代了 Object.defineProperty。主要优势在于：1. 可以完美监听对象属性的添加与删除；2. 完美支持数组的索引与长度变化监听；3. 嵌套对象的收集是惰性触发的，性能更好。' }
    ]
  },
  {
    id: '3',
    title: 'TypeScript 泛型工具',
    messages: [
      { id: '3-1', role: 'user', content: 'Omit 和 Pick 有什么区别？' },
      { id: '3-2', role: 'assistant', content: 'Pick<T, K> 是从类型 T 中挑选出一组属性 K 来构建新类型；而 Omit<T, K> 则是从类型 T 中忽略一组属性 K，用剩余的属性构建新类型。两者逻辑恰好相反。' }
    ]
  }
])

// --- 计算属性 ---
const currentSession = computed(() => {
  return sessions.value.find(s => s.id === activeSessionId.value)
})

// 根据搜索关键词过滤历史记录
const filteredSessions = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  if (!query) return sessions.value
  return sessions.value.filter(s =>
    s.title.toLowerCase().includes(query) ||
    s.messages.some(m => m.content.toLowerCase().includes(query))
  )
})

// --- 方法 ---

// 切换会话
const selectSession = (id: string) => {
  activeSessionId.value = id
  isMobileSidebarOpen.value = false // 移动端选择后自动收起
  scrollToBottom()
}

// 新建聊天
const handleNewChat = () => {
  const newId = Date.now().toString()
  sessions.value.unshift({
    id: newId,
    title: `新对话 ${sessions.value.length + 1}`,
    messages: [
      { id: `${newId}-init`, role: 'assistant', content: '新对话已开启，有什么我可以帮你的？' }
    ]
  })
  activeSessionId.value = newId
  isMobileSidebarOpen.value = false
  scrollToBottom()
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (scrollAnchor.value) {
    scrollAnchor.value.scrollIntoView({ behavior: 'smooth' })
  }
}

// 发送消息
const handleSend = () => {
  const text = inputText.value.trim()
  if (!text || !currentSession.value) return

  // 1. 添加用户消息
  currentSession.value.messages.push({
    id: Date.now().toString(),
    role: 'user',
    content: text
  })
  inputText.value = ''
  scrollToBottom()

  // 2. 模拟触发 AI 生成
  isGenerating.value = true
  const assistantMsgId = (Date.now() + 1).toString()

  currentSession.value.messages.push({
    id: assistantMsgId,
    role: 'assistant',
    content: '正在思考中...'
  })
  scrollToBottom()

  simulateAIResponse(assistantMsgId)
}

// 停止生成
const stopGeneration = () => {
  isGenerating.value = false
  ElMessage.success('已停止生成')
}

// 模拟 AI 回复过程
const simulateAIResponse = (msgId: string) => {
  setTimeout(() => {
    if (!isGenerating.value || !currentSession.value) return
    const targetMsg = currentSession.value.messages.find(m => m.id === msgId)
    if (targetMsg) {
      targetMsg.content = '这就为您处理。在极简风格的前端架构中，我们更倾向于通过高内聚的排版和自然的组件边界来替代生硬的分割线，从而带来流畅的视觉体验。'
    }
    isGenerating.value = false
    scrollToBottom()
  }, 1200)
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped lang="scss">
// 极简风格颜色规范
$bg-color: #fafafa;
$surface-color: #ffffff;
$border-color: #f0f0f0;
$text-primary: #1a1a1a;
$text-regular: #5f5f5f;
$text-secondary: #999999;
$active-bg: #f5f5f5;
$user-bubble-bg: #f0f0f2;
$ai-bubble-bg: #ffffff;

.app-layout {
  display: flex;
  width: 100%;
  height: 100%;
  background-color: $bg-color;
  overflow: hidden;
  position: relative;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;

  @media (max-width: 768px) {
    height: 100dvh;
  }
}

/* ==========================================================================
   左侧侧边栏 (Sidebar) Style
   ========================================================================== */
.sidebar {
  width: 280px;
  height: 100%;
  background-color: $surface-color;
  border-right: 1px solid $border-color;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  z-index: 100;

  /* H5端自适应：变为抽屉式滑出 */
  @media (max-width: 768px) {
    position: fixed;
    left: -280px;
    top: 0;
    bottom: 0;
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 4px 0 16px rgba(0, 0, 0, 0.03);

    &.is-mobile-open {
      transform: translateX(280px);
    }
  }
}

.sidebar-header {
  padding: 24px 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;

  .new-chat-btn {
    width: 100%;
    height: 40px;
    border: 1px dashed #dcdfe6;
    border-radius: 6px;
    color: $text-regular;
    font-size: 14px;
    letter-spacing: 0.5px;

    &:hover {
      color: $text-primary;
      border-color: $text-primary;
      background-color: $bg-color;
    }

    .plus-icon {
      margin-right: 4px;
      font-size: 16px;
      font-weight: 300;
    }
  }

  .search-wrapper {
    :deep(.el-input__wrapper) {
      box-shadow: none !important;
      border: 1px solid $border-color;
      background-color: #ffffff;
      /* 修改为纯白背景 */
      border-radius: 6px;
      padding: 0 10px;
    }
  }
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: transparent;
    border-radius: 2px;
  }

  &:hover::-webkit-scrollbar-thumb {
    background: #e0e0e0;
  }

  .session-item {
    padding: 12px 14px;
    border-radius: 6px;
    cursor: pointer;
    margin: 0 6px 4px;
    transition: background-color 0.2s;

    &:hover {
      background-color: $bg-color;
    }

    &.is-active {
      background-color: $active-bg;

      .session-title {
        color: $text-primary;
        font-weight: 500;
      }
    }

    .session-title {
      font-size: 14px;
      color: $text-regular;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .empty-search {
    text-align: center;
    font-size: 12px;
    color: $text-secondary;
    margin-top: 24px;
  }
}

/* H5 遮罩层 */
.sidebar-overlay {
  display: none;

  @media (max-width: 768px) {
    &.is-visible {
      display: block;
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.15);
      backdrop-filter: blur(1px);
      z-index: 99;
    }
  }
}

/* ==========================================================================
   右侧聊天区域 (Chat Container) Style
   ========================================================================== */
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: $bg-color;
  position: relative;
}

.chat-header {
  flex-shrink: 0;
  padding: 18px 24px;
  background-color: $surface-color;
  border-bottom: 1px solid $border-color;
  display: flex;
  align-items: center;
  gap: 16px;

  .menu-toggle-btn {
    display: none;
    background: none;
    border: none;
    color: $text-regular;
    padding: 4px;
    cursor: pointer;

    @media (max-width: 768px) {
      display: block;
    }
  }

  .header-text {
    display: flex;
    align-items: baseline;
  }

  .title {
    margin: 0;
    font-size: 18px;
    font-weight: 500;
    color: $text-primary;
    letter-spacing: 0.5px;
  }
}

.chat-main {
  flex: 1;
  overflow-y: auto;
  padding: 32px 40px;
  scroll-behavior: smooth;

  @media (max-width: 768px) {
    padding: 16px;
  }

  .message-row {
    display: flex;
    margin-bottom: 28px;
    align-items: flex-start;
    gap: 16px;

    &.is-user {
      flex-direction: row-reverse;

      .avatar {
        background-color: $text-primary;
        color: #fff;
      }

      .message-bubble {
        background-color: $user-bubble-bg;
      }
    }

    &.is-assistant {
      .avatar {
        background-color: #fff;
        color: $text-primary;
        border: 1px solid #e0e0e0;
      }

      .message-bubble {
        background-color: $ai-bubble-bg;
        border: 1px solid $border-color;
      }
    }
  }

  .avatar {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 500;
    flex-shrink: 0;
  }

  .message-bubble {
    max-width: 80%;
    padding: 12px 18px;
    border-radius: 8px;
    line-height: 1.6;

    .content {
      margin: 0;
      color: $text-primary;
      font-size: 15px;
      white-space: pre-wrap;
      word-break: break-word;
    }

    @media (max-width: 768px) {
      max-width: 88%;
      padding: 10px 14px;

      .content {
        font-size: 14px;
      }
    }
  }
}

.chat-footer {
  flex-shrink: 0;
  padding: 24px 40px;
  background-color: transparent;

  @media (max-width: 768px) {
    padding: 12px 16px;
    padding-bottom: calc(12px + env(safe-area-inset-bottom));
    background-color: $surface-color;
    border-top: 1px solid $border-color;
  }

  .input-wrapper {
    display: flex;
    align-items: flex-end;
    gap: 12px;
    background-color: $surface-color;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    padding: 10px 12px;
    transition: border-color 0.2s;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.01);

    &:focus-within {
      border-color: $text-primary;
    }

    :deep(.el-textarea__inner) {
      box-shadow: none !important;
      border: none !important;
      padding: 4px 4px;
      background-color: transparent;
      font-family: inherit;
      resize: none;
      font-size: 15px;
      color: $text-primary;
    }
  }

  .action-btn-group {
    flex-shrink: 0;

    .send-btn,
    .stop-btn {
      height: 34px;
      border-radius: 6px;
      padding: 0 16px;
      font-weight: 500;
      font-size: 13px;
    }

    .send-btn {
      background-color: $text-primary;
      border-color: $text-primary;

      &:hover {
        background-color: #333333;
        border-color: #333333;
      }

      &:disabled {
        background-color: #e4e7ed;
        border-color: #e4e7ed;
        color: #a8abb2;
      }
    }
  }
}
</style>
