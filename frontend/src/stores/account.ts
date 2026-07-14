import { ref } from 'vue'
import { defineStore } from 'pinia'

import { userAccountApi } from '@/utils/api'

export const useAccountStore = defineStore('account', () => {
  const balance = ref<number | null>(null)
  const loading = ref(false)
  let latestRequestId = 0

  async function refreshBalance() {
    const requestId = ++latestRequestId
    loading.value = true
    try {
      const result = await userAccountApi.balance()
      if (requestId === latestRequestId) {
        balance.value = result.balance
      }
    } catch {
      // Keep the last known balance when a background refresh fails.
    } finally {
      if (requestId === latestRequestId) {
        loading.value = false
      }
    }
  }

  function clearBalance() {
    latestRequestId += 1
    balance.value = null
    loading.value = false
  }

  return { balance, loading, refreshBalance, clearBalance }
})
