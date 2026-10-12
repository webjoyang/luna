import { ref, onMounted, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPortalAuthInfoApi } from '@/api/portal/auth'

export function usePortalAuth() {
  const route = useRoute()
  const router = useRouter()
  const customerName = ref('')
  const customerId = ref<number | null>(null)
  const loading = ref(true)

  const bootstrapToken = async (): Promise<boolean> => {
    // 1. 如果带有 ?t= 令牌，优先存入会话并清理地址栏
    const queryToken = route.query?.t as string | undefined
    if (queryToken && typeof sessionStorage !== 'undefined') {
      sessionStorage.setItem('portal_token', queryToken)
      try {
        if (typeof window !== 'undefined' && window.location) {
          const url = new URL(window.location.href)
          url.searchParams.delete('t')
          window.history.replaceState({}, '', url.pathname + (url.search ? url.search : ''))
        }
      } catch {
        // 兼容测试环境无完整 window.location
      }
    }

    // 2. 检查会话中是否存在有效令牌
    const token = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('portal_token') : null
    if (!token) {
      router.replace('/portal/invalid')
      return false
    }

    // 3. 校验并获取客户认证信息
    try {
      const res = await getPortalAuthInfoApi()
      if (res) {
        customerId.value = res.customerId
        customerName.value = res.customerName || ''
      }
      return true
    } catch {
      // 拦截器已处理 401/403/异常并跳转 /portal/invalid
      return false
    } finally {
      loading.value = false
    }
  }

  if (getCurrentInstance()) {
    onMounted(() => {
      bootstrapToken()
    })
  }

  return {
    customerName,
    customerId,
    loading,
    bootstrapToken
  }
}
