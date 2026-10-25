import { ref, type Ref } from 'vue'
import { showNotify, showToast, showSuccessToast, showFailToast } from 'vant'

/**
 * LunaERP 车间 H5 弱网交互与提交守卫工具（ADR-0045, Ticket 08）
 * 规约：
 * 1. 提交按钮提交中禁用防连点；
 * 2. 业务错误与网络错误分色提示；
 * 3. 网络错误文案："网络异常，单据未提交，可点重试"，不暗示已成功；
 * 4. 提交成功后触发草稿清理。
 */

export const NETWORK_ERROR_MESSAGE = '网络异常，单据未提交，可点重试'

export interface SubmitErrorInfo {
  isNetwork: boolean
  message: string
  type: 'warning' | 'danger'
  code?: number | string
  rawError: unknown
}

/**
 * 判断异常是否属于弱网/断网/请求超时异常（区别于服务端返回的业务守卫码）
 */
export function isNetworkError(error: unknown): boolean {
  if (typeof navigator !== 'undefined' && navigator.onLine === false) {
    return true
  }

  if (!error) return false

  const err = error as any
  const message = String(err.message || '')

  if (message === 'Network Error' || message === 'Failed to fetch') {
    return true
  }

  if (err.code === 'ECONNABORTED' || err.name === 'TimeoutError') {
    return true
  }

  if (message.toLowerCase().includes('timeout') || message.includes('网络连接异常')) {
    return true
  }

  // Axios 请求发出但未收到任何服务端 HTTP Response（如 DNS 解析失败、断网连接中断）
  if (err.isAxiosError && !err.response) {
    return true
  }

  return false
}

/**
 * 格式化提交异常，提供分色提示类型与规约文案
 */
export function formatSubmitError(error: unknown): SubmitErrorInfo {
  const isNetwork = isNetworkError(error)
  const err = (error || {}) as any

  if (isNetwork) {
    return {
      isNetwork: true,
      message: NETWORK_ERROR_MESSAGE,
      type: 'warning',
      code: err.code,
      rawError: error
    }
  }

  const message =
    err.msg || err.message || (typeof error === 'string' ? error : '单据提交失败，请重试')

  return {
    isNetwork: false,
    message,
    type: 'danger',
    code: err.code,
    rawError: error
  }
}

export interface UseH5SubmitOptions<TResult = any> {
  successMessage?: string
  networkErrorMessage?: string
  showFeedback?: boolean
  feedbackMode?: 'notify' | 'toast'
  onSuccess?: (result: TResult) => void
  onError?: (error: unknown, info: SubmitErrorInfo) => void
  onClearDraft?: () => void
}

export interface UseH5SubmitReturn<TArg = any, TResult = any> {
  submitting: Ref<boolean>
  submit: (arg?: TArg) => Promise<TResult | undefined>
  lastError: Ref<SubmitErrorInfo | null>
}

/**
 * 车间 H5 防连点与弱网/业务分色提交 Composable
 */
export function useH5Submit<TArg = any, TResult = any>(
  submitFn: (arg?: TArg) => Promise<TResult>,
  options: UseH5SubmitOptions<TResult> = {}
): UseH5SubmitReturn<TArg, TResult> {
  const {
    successMessage,
    networkErrorMessage = NETWORK_ERROR_MESSAGE,
    showFeedback = true,
    feedbackMode = 'notify',
    onSuccess,
    onError,
    onClearDraft
  } = options

  const submitting = ref(false)
  const lastError = ref<SubmitErrorInfo | null>(null)

  const showFeedbackMessage = (info: {
    type: 'success' | 'warning' | 'danger'
    message: string
  }) => {
    if (typeof window === 'undefined') return

    if (feedbackMode === 'notify') {
      showNotify({
        type: info.type,
        message: info.message,
        duration: info.type === 'warning' ? 3500 : 2500
      })
    } else {
      if (info.type === 'success') {
        showSuccessToast(info.message)
      } else if (info.type === 'warning') {
        showToast({
          type: 'fail',
          message: info.message,
          icon: 'warning-o',
          duration: 3500
        })
      } else {
        showFailToast(info.message)
      }
    }
  }

  const submit = async (arg?: TArg): Promise<TResult | undefined> => {
    // 防连点：提交中重复点击直接静默忽略
    if (submitting.value) {
      return undefined
    }

    submitting.value = true
    lastError.value = null

    try {
      const result = await submitFn(arg)

      // 提交成功清除草稿
      if (onClearDraft) {
        try {
          onClearDraft()
        } catch (e) {
          console.warn('[h5-submit] 清理草稿回调执行异常:', e)
        }
      }

      if (showFeedback && successMessage) {
        showFeedbackMessage({ type: 'success', message: successMessage })
      }

      if (onSuccess) {
        onSuccess(result)
      }

      return result
    } catch (err: unknown) {
      const errorInfo = formatSubmitError(err)
      if (errorInfo.isNetwork && networkErrorMessage) {
        errorInfo.message = networkErrorMessage
      }
      lastError.value = errorInfo

      if (showFeedback) {
        showFeedbackMessage({
          type: errorInfo.type,
          message: errorInfo.message
        })
      }

      if (onError) {
        onError(err, errorInfo)
      }

      throw err
    } finally {
      submitting.value = false
    }
  }

  return {
    submitting,
    submit,
    lastError
  }
}
