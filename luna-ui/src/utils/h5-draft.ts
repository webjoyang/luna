import { ref, watch, type Ref, isRef, unref } from 'vue'

/**
 * LunaERP 车间 H5 本地表单草稿缓存工具（ADR-0045, Ticket 08）
 * 规约：
 * 1. 表单内容实时存 localStorage 草稿按 code/id 分键
 * 2. 提交成功后清除
 * 3. 隔离异常与存储不可用边界
 */

export const H5_DRAFT_PREFIX = 'luna:h5:draft'

export interface DraftPayload<T = any> {
  namespace: string
  key: string
  data: T
  updatedAt: number
}

/**
 * 生成规范化的草稿存储键名
 */
export function getDraftKey(namespace: string, key: string | number): string {
  const ns = String(namespace || '').trim()
  const k = String(key ?? '').trim()
  return `${H5_DRAFT_PREFIX}:${ns}:${k}`
}

function getStorage(): Storage | null {
  if (typeof window !== 'undefined' && window.localStorage) {
    return window.localStorage
  }
  if (typeof globalThis !== 'undefined' && (globalThis as any).localStorage) {
    return (globalThis as any).localStorage
  }
  return null
}

/**
 * 保存表单草稿至 localStorage
 */
export function saveDraft<T = any>(namespace: string, key: string | number, data: T): boolean {
  const ns = String(namespace || '').trim()
  const k = String(key ?? '').trim()
  const storage = getStorage()
  if (!ns || !k || !storage) {
    return false
  }

  const storageKey = getDraftKey(ns, k)
  const payload: DraftPayload<T> = {
    namespace: ns,
    key: k,
    data,
    updatedAt: Date.now()
  }

  try {
    storage.setItem(storageKey, JSON.stringify(payload))
    return true
  } catch (error) {
    console.warn(`[h5-draft] 保存草稿失败 (${storageKey}):`, error)
    return false
  }
}

/**
 * 获取完整草稿载荷（含时间戳）
 */
export function getDraftPayload<T = any>(
  namespace: string,
  key: string | number
): DraftPayload<T> | null {
  const ns = String(namespace || '').trim()
  const k = String(key ?? '').trim()
  const storage = getStorage()
  if (!ns || !k || !storage) {
    return null
  }

  const storageKey = getDraftKey(ns, k)
  try {
    const raw = storage.getItem(storageKey)
    if (!raw) return null

    const parsed = JSON.parse(raw) as DraftPayload<T>
    if (!parsed || typeof parsed !== 'object' || !('data' in parsed)) {
      storage.removeItem(storageKey)
      return null
    }
    return parsed
  } catch (error) {
    console.warn(`[h5-draft] 读取草稿损坏，自动清理 (${storageKey}):`, error)
    storage.removeItem(storageKey)
    return null
  }
}

/**
 * 获取草稿表单数据
 */
export function getDraft<T = any>(namespace: string, key: string | number): T | null {
  const payload = getDraftPayload<T>(namespace, key)
  return payload ? payload.data : null
}

/**
 * 判断指定草稿是否存在
 */
export function hasDraft(namespace: string, key: string | number): boolean {
  return getDraft(namespace, key) !== null
}

/**
 * 清除指定表单草稿（提交成功后调用）
 */
export function clearDraft(namespace: string, key: string | number): boolean {
  const ns = String(namespace || '').trim()
  const k = String(key ?? '').trim()
  const storage = getStorage()
  if (!ns || !k || !storage) {
    return false
  }

  const storageKey = getDraftKey(ns, k)
  try {
    storage.removeItem(storageKey)
    return true
  } catch (error) {
    console.warn(`[h5-draft] 清除草稿失败 (${storageKey}):`, error)
    return false
  }
}

export interface UseH5DraftOptions<T> {
  namespace: string
  key:
    | Ref<string | number | undefined>
    | (() => string | number | undefined)
    | string
    | number
    | undefined
  formData: Ref<T>
  autoSave?: boolean
  debounceMs?: number
}

/**
 * Vue Composable: 响应式草稿存取与监听
 */
export function useH5Draft<T extends Record<string, any>>(options: UseH5DraftOptions<T>) {
  const { namespace, key, formData, autoSave = true, debounceMs = 300 } = options

  const lastSavedAt = ref<number | null>(null)
  let timer: any = null

  const getCurrentKey = (): string | number | undefined => {
    if (typeof key === 'function') {
      return (key as () => string | number | undefined)()
    }
    return isRef(key) ? key.value : key
  }

  const save = () => {
    const k = getCurrentKey()
    if (!k) return false
    const success = saveDraft(namespace, k, unref(formData))
    if (success) {
      lastSavedAt.value = Date.now()
    }
    return success
  }

  const load = (): boolean => {
    const k = getCurrentKey()
    if (!k) return false
    const payload = getDraftPayload<T>(namespace, k)
    if (payload && payload.data) {
      Object.assign(unref(formData), payload.data)
      lastSavedAt.value = payload.updatedAt
      return true
    }
    return false
  }

  const clear = (): boolean => {
    const k = getCurrentKey()
    if (!k) return false
    const success = clearDraft(namespace, k)
    if (success) {
      lastSavedAt.value = null
    }
    return success
  }

  const has = (): boolean => {
    const k = getCurrentKey()
    if (!k) return false
    return hasDraft(namespace, k)
  }

  if (autoSave) {
    watch(
      () => unref(formData),
      () => {
        if (timer) clearTimeout(timer)
        timer = setTimeout(() => {
          save()
        }, debounceMs)
      },
      { deep: true }
    )
  }

  return {
    lastSavedAt,
    saveDraft: save,
    loadDraft: load,
    clearDraft: clear,
    hasDraft: has
  }
}
