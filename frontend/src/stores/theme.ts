import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'

export type ThemeMode = 'light' | 'dark'

const THEME_STORAGE_KEY = 'ai-chat-theme'

function readStoredTheme(): ThemeMode {
  if (typeof window === 'undefined') return 'light'
  return window.localStorage.getItem(THEME_STORAGE_KEY) === 'dark' ? 'dark' : 'light'
}

function applyTheme(mode: ThemeMode) {
  if (typeof document === 'undefined') return
  document.documentElement.classList.toggle('dark', mode === 'dark')
  document.documentElement.style.colorScheme = mode
  window.dispatchEvent(new CustomEvent('themechange', { detail: mode }))
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>(readStoredTheme())
  const isDark = computed(() => mode.value === 'dark')

  function setTheme(nextMode: ThemeMode) {
    mode.value = nextMode
  }

  function toggleTheme() {
    setTheme(isDark.value ? 'light' : 'dark')
  }

  watch(mode, (nextMode) => {
    applyTheme(nextMode)
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(THEME_STORAGE_KEY, nextMode)
    }
  }, { immediate: true })

  return { mode, isDark, setTheme, toggleTheme }
})
