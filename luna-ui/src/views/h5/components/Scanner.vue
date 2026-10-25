<template>
  <div class="h5-scanner-component w-full">
    <!-- 1. 码制切换（二维码 / Code128 一维码 / 全部） -->
    <div
      v-if="showModeSwitch"
      class="scanner-mode-switch flex items-center justify-between mb-2 px-1"
    >
      <span class="text-12px text-gray-500 font-medium">码制类型：</span>
      <div class="mode-buttons flex space-x-1">
        <button
          type="button"
          class="px-2 py-0.5 text-12px rounded transition-colors"
          :class="
            currentMode === 'qr' ? 'bg-primary text-white font-medium' : 'bg-gray-100 text-gray-600'
          "
          @click="handleModeChange('qr')"
        >
          二维码(布票)
        </button>
        <button
          type="button"
          class="px-2 py-0.5 text-12px rounded transition-colors"
          :class="
            currentMode === 'code128'
              ? 'bg-primary text-white font-medium'
              : 'bg-gray-100 text-gray-600'
          "
          @click="handleModeChange('code128')"
        >
          一维码(Code128)
        </button>
        <button
          type="button"
          class="px-2 py-0.5 text-12px rounded transition-colors"
          :class="
            currentMode === 'all'
              ? 'bg-primary text-white font-medium'
              : 'bg-gray-100 text-gray-600'
          "
          @click="handleModeChange('all')"
        >
          全部
        </button>
      </div>
    </div>

    <!-- 2. 摄像头取景与控制区 -->
    <!-- 安全上下文（HTTPS 或 localhost）下提供摄像头扫码能力 -->
    <div v-if="cameraSupport.supported" class="camera-control-box mb-3">
      <!-- 摄像头未启动状态：提供大扫码激活入口 -->
      <div v-if="!isCameraRunning" class="camera-launcher">
        <van-button
          round
          block
          type="primary"
          plain
          icon="scan"
          size="normal"
          :disabled="disabled"
          :loading="isStartingCamera"
          loading-text="正在启动摄像头..."
          @click="startCamera"
        >
          开启摄像头扫码
        </van-button>
      </div>

      <!-- 摄像头运行中：取景器容器与悬浮控制条 -->
      <div
        v-show="isCameraRunning"
        class="camera-viewport-card bg-black rounded-lg overflow-hidden relative shadow-md"
      >
        <div :id="scannerElementId" class="w-full min-h-[240px]"></div>
        <div
          class="px-3 py-2 bg-gray-900 bg-opacity-90 flex justify-between items-center text-white text-12px"
        >
          <span class="text-gray-300 flex items-center space-x-1">
            <span class="inline-block w-2 h-2 rounded-full bg-green-500 animate-pulse"></span>
            <span>正在扫描{{ currentModeLabel }}...</span>
          </span>
          <van-button size="mini" round type="danger" @click="stopCamera"> 关闭摄像头 </van-button>
        </div>
      </div>

      <!-- 摄像头启动失败或权限降级提示 -->
      <div
        v-if="cameraError"
        class="mt-2 p-2 bg-orange-50 border border-orange-200 rounded text-12px text-orange-800 leading-relaxed"
      >
        <div class="flex items-center space-x-1 font-semibold text-orange-900">
          <van-icon name="warning-o" />
          <span>{{ cameraError.isPermissionDenied ? '摄像头权限受限' : '摄像头无法开启' }}</span>
        </div>
        <p class="mt-0.5">{{ cameraError.message }}</p>
      </div>
    </div>

    <!-- 局域网 HTTP 或非安全上下文环境：自动隐藏摄像头入口，提示切换楔入模式 -->
    <div
      v-else
      class="http-insecure-banner mb-3 p-2.5 bg-gray-50 border border-gray-200 rounded text-12px text-gray-600 flex items-start space-x-2"
    >
      <van-icon name="info-o" class="text-blue-500 mt-0.5 text-14px flex-shrink-0" />
      <div>
        <span class="font-medium text-gray-700">局域网环境（摄像头已停用）</span>
        <p class="text-11px text-gray-500 mt-0.5">
          {{
            cameraSupport.message || '受浏览器安全策略限制，局域网 HTTP 仅支持扫码枪/键盘楔入输入。'
          }}
        </p>
      </div>
    </div>

    <!-- 3. 常驻自动聚焦输入框 + 软回车按钮（PDA 头扫/USB 扫码枪键盘楔入一等公民） -->
    <div class="wedge-input-section bg-white rounded-lg p-2.5 border border-gray-200 shadow-sm">
      <div class="flex items-center space-x-2">
        <div class="flex-1">
          <van-field
            ref="wedgeFieldRef"
            v-model="inputValue"
            :placeholder="placeholder"
            :disabled="disabled"
            clearable
            :autofocus="autofocus"
            @keyup.enter="handleEnterScan"
            autocomplete="off"
            class="!p-1.5 border border-gray-200 rounded text-14px font-mono"
          >
            <template #left-icon>
              <van-icon name="search" class="text-gray-400" />
            </template>
          </van-field>
        </div>
        <van-button
          type="primary"
          size="small"
          :disabled="disabled || !inputValue.trim()"
          @click="handleEnterScan"
          class="px-4 font-medium"
        >
          录入
        </van-button>
      </div>
      <div class="flex justify-between items-center text-11px text-gray-400 px-1 mt-1.5">
        <span>PDA 头扫或 USB 枪扫码自动触发回车</span>
        <span v-if="lastScannedCode" class="text-primary truncate max-w-[160px]">
          已录: {{ lastScannedCode }}
        </span>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Html5Qrcode } from 'html5-qrcode'
import {
  checkCameraSupported,
  resolveSupportedFormats,
  formatScanError,
  processScannedInput,
  type ScanFormatMode,
  type CameraSupportResult,
  type FormattedScanError
} from './scanner-helper'

defineOptions({ name: 'H5Scanner' })

const props = withDefaults(
  defineProps<{
    modelValue?: string
    placeholder?: string
    defaultMode?: ScanFormatMode
    showModeSwitch?: boolean
    autoStartCamera?: boolean
    clearOnScan?: boolean
    autofocus?: boolean
    disabled?: boolean
    qrboxSize?: number
    fps?: number
  }>(),
  {
    modelValue: '',
    placeholder: '请扫码或输入编号后回车',
    defaultMode: 'qr',
    showModeSwitch: true,
    autoStartCamera: false,
    clearOnScan: true,
    autofocus: true,
    disabled: false,
    qrboxSize: 250,
    fps: 10
  }
)

const emit = defineEmits<{
  (e: 'scan', code: string): void
  (e: 'update:modelValue', val: string): void
  (e: 'mode-change', mode: ScanFormatMode): void
  (e: 'camera-state-change', isOpen: boolean): void
  (e: 'error', error: FormattedScanError): void
}>()

// 状态管理
const currentMode = ref<ScanFormatMode>(props.defaultMode)
const inputValue = ref(props.modelValue || '')

watch(
  () => props.modelValue,
  (val) => {
    if (val !== inputValue.value) {
      inputValue.value = val || ''
    }
  }
)
const lastScannedCode = ref('')
const isCameraRunning = ref(false)
const isStartingCamera = ref(false)
const cameraError = ref<FormattedScanError | null>(null)
const cameraSupport = ref<CameraSupportResult>({ supported: false, reason: 'insecure_context' })

// DOM 与实例
const wedgeFieldRef = ref<{ focus?: () => void } | null>(null)
const scannerElementId = `h5-scanner-reader-${Math.random().toString(36).substring(2, 9)}`
let html5QrCodeInstance: Html5Qrcode | null = null

const currentModeLabel = computed(() => {
  switch (currentMode.value) {
    case 'code128':
      return '一维条码 (Code128)'
    case 'all':
      return '条形码 / 二维码'
    case 'qr':
    default:
      return '布票二维码'
  }
})

/**
 * 聚焦输入框，便于 PDA 连续头扫
 */
function focusInput() {
  if (props.autofocus) {
    nextTick(() => {
      wedgeFieldRef.value?.focus?.()
    })
  }
}

/**
 * 切换条码/二维码模式
 */
async function handleModeChange(mode: ScanFormatMode) {
  if (currentMode.value === mode) return
  currentMode.value = mode
  emit('mode-change', mode)

  // 若摄像头正在运行，重启以生效新码制配置
  if (isCameraRunning.value) {
    await stopCamera()
    await startCamera()
  }
}

/**
 * 启动摄像头扫码
 */
async function startCamera() {
  if (!cameraSupport.value.supported || isCameraRunning.value || isStartingCamera.value) {
    return
  }

  isStartingCamera.value = true
  cameraError.value = null

  try {
    // 确保 DOM 容器已挂载
    await nextTick()

    if (!html5QrCodeInstance) {
      html5QrCodeInstance = new Html5Qrcode(scannerElementId, {
        formatsToSupport: resolveSupportedFormats(currentMode.value),
        verbose: false
      })
    }

    const config = {
      fps: props.fps,
      qrbox: { width: props.qrboxSize, height: props.qrboxSize }
    }

    await html5QrCodeInstance.start(
      { facingMode: 'environment' },
      config,
      (decodedText: string) => {
        onScanSuccess(decodedText)
      },
      () => {
        // html5-qrcode 默认每帧均触发 error 回调，常规未识读帧直接忽略
      }
    )

    isCameraRunning.value = true
    emit('camera-state-change', true)
  } catch (err) {
    const formatted = formatScanError(err)
    cameraError.value = formatted
    emit('error', formatted)
    // 发生异常时清理实例
    try {
      if (html5QrCodeInstance) {
        await html5QrCodeInstance.stop().catch(() => {})
        html5QrCodeInstance.clear()
        html5QrCodeInstance = null
      }
    } catch {
      // 忽略清理异常
    }
  } finally {
    isStartingCamera.value = false
  }
}

/**
 * 停止摄像头扫码并释放硬件占用
 */
async function stopCamera() {
  if (!html5QrCodeInstance || !isCameraRunning.value) {
    isCameraRunning.value = false
    return
  }

  try {
    await html5QrCodeInstance.stop()
    html5QrCodeInstance.clear()
    html5QrCodeInstance = null
  } catch (err) {
    // 忽略停止过程中的非阻塞错误
  } finally {
    isCameraRunning.value = false
    emit('camera-state-change', false)
  }
}

/**
 * 摄像头识别成功处理
 */
function onScanSuccess(rawText: string) {
  const code = processScannedInput(rawText)
  if (!code) return

  // 手机端微震动反馈
  if (
    typeof window !== 'undefined' &&
    window.navigator &&
    typeof window.navigator.vibrate === 'function'
  ) {
    try {
      window.navigator.vibrate(100)
    } catch {
      // 忽略非关键震动异常
    }
  }

  lastScannedCode.value = code
  emit('scan', code)
  emit('update:modelValue', code)

  // 默认识别成功后关闭摄像头，以便聚焦单据卡片
  stopCamera()
  focusInput()
}

/**
 * 键盘楔入 / 软回车录入处理（PDA 与 USB 枪）
 */
function handleEnterScan() {
  const code = processScannedInput(inputValue.value)
  if (!code) return

  lastScannedCode.value = code
  emit('scan', code)
  emit('update:modelValue', code)

  if (props.clearOnScan) {
    inputValue.value = ''
  }

  focusInput()
}

onMounted(() => {
  // 检查安全上下文与摄像头可用性
  cameraSupport.value = checkCameraSupported()

  if (cameraSupport.value.supported && props.autoStartCamera) {
    startCamera()
  }

  focusInput()
})

onBeforeUnmount(() => {
  stopCamera()
})

defineExpose({
  startCamera,
  stopCamera,
  focusInput,
  currentMode
})
</script>

<style scoped>
.camera-viewport-card :deep(video) {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
  border-radius: 8px;
}
</style>
