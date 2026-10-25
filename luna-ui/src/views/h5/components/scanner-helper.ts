import { Html5QrcodeSupportedFormats } from 'html5-qrcode'

export type ScanFormatMode = 'qr' | 'code128' | 'all'

export interface ScannerEnv {
  isSecureContext?: boolean
  protocol?: string
  hostname?: string
  hasMediaDevices?: boolean
  hasGetUserMedia?: boolean
}

export interface CameraSupportResult {
  supported: boolean
  reason: 'supported' | 'insecure_http' | 'insecure_context' | 'no_mediadevices'
  message?: string
}

export interface FormattedScanError {
  code: string
  message: string
  isPermissionDenied: boolean
  isDeviceNotFound: boolean
  rawError?: unknown
}

/**
 * 提取当前运行时浏览器/WebView 的媒体与安全上下文环境
 */
export function getRuntimeScannerEnv(): ScannerEnv {
  if (typeof window === 'undefined') {
    return {
      isSecureContext: false,
      protocol: '',
      hostname: '',
      hasMediaDevices: false,
      hasGetUserMedia: false
    }
  }

  const hostname = window.location?.hostname || ''
  const protocol = window.location?.protocol || ''
  const hasMediaDevices = Boolean(window.navigator?.mediaDevices)
  const hasGetUserMedia = Boolean(
    window.navigator?.mediaDevices &&
    typeof window.navigator.mediaDevices.getUserMedia === 'function'
  )

  return {
    isSecureContext: window.isSecureContext,
    protocol,
    hostname,
    hasMediaDevices,
    hasGetUserMedia
  }
}

/**
 * 校验当前环境是否允许使用摄像头扫码
 * 规则：
 * 1. 必须具备 navigator.mediaDevices.getUserMedia
 * 2. localhost / 127.0.0.1 视为安全上下文允许使用
 * 3. HTTP 局域网（如 192.168.*、10.*）或 isSecureContext=false 下自动隐藏摄像头入口
 * 4. HTTPS 下允许使用
 */
export function checkCameraSupported(
  env: ScannerEnv = getRuntimeScannerEnv()
): CameraSupportResult {
  if (!env.hasMediaDevices || !env.hasGetUserMedia) {
    return {
      supported: false,
      reason: 'no_mediadevices',
      message: '当前浏览器或 WebView 不支持摄像头媒体流'
    }
  }

  const hostname = env.hostname || ''
  const protocol = env.protocol || ''
  const isLocalhost = hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '::1'

  if (isLocalhost) {
    return {
      supported: true,
      reason: 'supported'
    }
  }

  // 局域网 HTTP 或任意非安全 HTTP
  if (protocol === 'http:') {
    return {
      supported: false,
      reason: 'insecure_http',
      message: '当前为局域网 HTTP 环境，系统安全限制已自动停用摄像头，请使用扫码枪或键盘输入'
    }
  }

  if (env.isSecureContext === false) {
    return {
      supported: false,
      reason: 'insecure_context',
      message: '当前运行环境非安全上下文 (HTTPS)，摄像头不可用，请使用扫码枪或键盘输入'
    }
  }

  return {
    supported: true,
    reason: 'supported'
  }
}

/**
 * 将扫码模式映射为 html5-qrcode 的支持码制列表
 */
export function resolveSupportedFormats(mode: ScanFormatMode): Html5QrcodeSupportedFormats[] {
  switch (mode) {
    case 'code128':
      return [Html5QrcodeSupportedFormats.CODE_128]
    case 'all':
      return [Html5QrcodeSupportedFormats.QR_CODE, Html5QrcodeSupportedFormats.CODE_128]
    case 'qr':
    default:
      return [Html5QrcodeSupportedFormats.QR_CODE]
  }
}

/**
 * 统一格式化摄像头启动与运行异常，输出降级友好提示
 */
export function formatScanError(err: unknown): FormattedScanError {
  const errorObj = err instanceof Error ? err : new Error(String(err))
  const name = errorObj.name || ''
  const msg = errorObj.message || ''

  const isPermissionDenied =
    name === 'NotAllowedError' ||
    name === 'PermissionDeniedError' ||
    /permission/i.test(msg) ||
    /denied/i.test(msg)

  const isDeviceNotFound =
    name === 'NotFoundError' || name === 'DevicesNotFoundError' || /not found/i.test(msg)

  if (isPermissionDenied) {
    return {
      code: 'PERMISSION_DENIED',
      isPermissionDenied: true,
      isDeviceNotFound: false,
      message: '摄像头权限未开启或被拒绝，请在浏览器中允许摄像头访问，或使用下方输入框/扫码枪录入',
      rawError: err
    }
  }

  if (isDeviceNotFound) {
    return {
      code: 'DEVICE_NOT_FOUND',
      isPermissionDenied: false,
      isDeviceNotFound: true,
      message: '未检测到可用摄像头设备，请检查设备连接或直接使用下方输入框录入',
      rawError: err
    }
  }

  return {
    code: 'CAMERA_START_FAILED',
    isPermissionDenied: false,
    isDeviceNotFound: false,
    message: '摄像头启动失败，请检查摄像头占用或直接使用下方输入框录入',
    rawError: err
  }
}

/**
 * 处理键盘楔入/扫码枪输入
 * 去除首尾空格、回车换行等冗余字符
 */
export function processScannedInput(input: string): string {
  if (!input) return ''
  return input.replace(/[\r\n]+/g, '').trim()
}
