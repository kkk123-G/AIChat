import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './styles/theme.scss'

import App from './App.vue'
import router from './router'
import { authApi } from './utils/api'
import { clearAccessToken, hasAccessToken, setAccessToken } from './utils/auth'
import { useThemeStore } from './stores/theme'

async function bootstrap() {
  if (!hasAccessToken()) {
    try {
      const tokenData = await authApi.refresh()
      setAccessToken(tokenData.accessToken)
    } catch {
      clearAccessToken()
    }
  }

  const app = createApp(App)
  const pinia = createPinia()
  useThemeStore(pinia)
  app.use(pinia)
  app.use(router)
  app.use(ElementPlus)
  app.mount('#app')
}

void bootstrap()
