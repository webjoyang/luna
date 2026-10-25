/**
 * 车间移动 H5 - 扫码落点分流逻辑与存储辅助函数
 *
 * 业务规约：
 * 冻结二维码落点 /h5/scan/weaving-ticket?code=
 * 登录后按角色分流：
 * 1. 仅 report（报工）→ /h5/ticket?code=
 * 2. 仅 inspection（质检）→ /h5/inspection?code=（后续 14 号工单走 ticket-scan 进胚布录检）
 * 3. 两者都有（双角色/管理员）→ ActionSheet 选择并记住上次选择 localStorage，可在"我的"里重置
 * 4. 两者皆无 → /h5/403
 *
 * @author luna
 */

export const STORAGE_KEY_SCAN_LANDING = 'luna_h5_scan_landing_choice'

export type ScanLandingChoice = 'report' | 'inspection'

export interface ScanLandingOptions {
  code: string
  canReport: boolean
  canInspect: boolean
  savedChoice?: ScanLandingChoice | null
}

export type ScanLandingDecision =
  | { action: 'redirect'; target: string; query: { code: string } }
  | { action: 'choose' }
  | { action: 'forbidden'; target: string }

/**
 * 计算扫码分流决策（纯函数，易于单元测试）
 */
export function resolveScanLanding(options: ScanLandingOptions): ScanLandingDecision {
  const { code, canReport, canInspect, savedChoice } = options
  const cleanCode = code ? code.trim() : ''

  // 1. 无任何对应权限 -> 403
  if (!canReport && !canInspect) {
    return {
      action: 'forbidden',
      target: '/h5/403'
    }
  }

  // 2. 仅报工权限 -> 直接进入布票卡
  if (canReport && !canInspect) {
    return {
      action: 'redirect',
      target: '/h5/ticket',
      query: { code: cleanCode }
    }
  }

  // 3. 仅质检权限 -> 进入质检域
  if (!canReport && canInspect) {
    return {
      action: 'redirect',
      target: '/h5/inspection',
      query: { code: cleanCode }
    }
  }

  // 4. 双角色（同时拥有报工与质检）：检查是否有记忆偏好
  if (savedChoice === 'report') {
    return {
      action: 'redirect',
      target: '/h5/ticket',
      query: { code: cleanCode }
    }
  }
  if (savedChoice === 'inspection') {
    return {
      action: 'redirect',
      target: '/h5/inspection',
      query: { code: cleanCode }
    }
  }

  // 尚未记住选择 -> 提示 ActionSheet 选择
  return {
    action: 'choose'
  }
}

function getStorage(): Storage | null {
  try {
    if (typeof window !== 'undefined' && window.localStorage) {
      return window.localStorage
    }
    if (typeof globalThis !== 'undefined' && (globalThis as any).localStorage) {
      return (globalThis as any).localStorage
    }
  } catch {
    // 忽略异常
  }
  return null
}

/**
 * 获取本地存储的扫码落点上次选择
 */
export function getSavedScanLandingChoice(): ScanLandingChoice | null {
  try {
    const storage = getStorage()
    if (!storage) return null
    const val = storage.getItem(STORAGE_KEY_SCAN_LANDING)
    if (val === 'report' || val === 'inspection') {
      return val
    }
    return null
  } catch {
    return null
  }
}

/**
 * 记住扫码落点选择
 */
export function setSavedScanLandingChoice(choice: ScanLandingChoice): void {
  try {
    const storage = getStorage()
    if (!storage) return
    storage.setItem(STORAGE_KEY_SCAN_LANDING, choice)
  } catch {
    // 忽略异常
  }
}

/**
 * 重置扫码落点选择（在"我的"个人中心中重置）
 */
export function resetSavedScanLandingChoice(): void {
  try {
    const storage = getStorage()
    if (!storage) return
    storage.removeItem(STORAGE_KEY_SCAN_LANDING)
  } catch {
    // 忽略异常
  }
}
