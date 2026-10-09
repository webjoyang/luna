<template>
  <div class="qr-slot flex flex-col items-center justify-center">
    <div v-if="text" class="qr-slot-code">
      <!-- 打印场景强制使用 tag="img"（data URL），确保 iframe innerHTML 复制时保留像素图像 -->
      <Qrcode
        :text="text"
        :width="width"
        tag="img"
        :options="{ margin: margin, ...options }"
      />
    </div>
    <div v-else class="qr-slot-placeholder text-gray-400 text-center flex items-center justify-center border border-dashed border-gray-300" :style="{ width: `${width}px`, height: `${width}px` }">
      <span class="text-11px">暂无二维码</span>
    </div>
    <div v-if="label || $slots.label" class="qr-slot-label text-center mt-1 text-10px text-gray-600">
      <slot name="label">{{ label }}</slot>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { PropType } from 'vue'
import { Qrcode } from '@/components/Qrcode'
import type { QRCodeRenderersOptions } from 'qrcode'

defineOptions({ name: 'QrSlot' })

defineProps({
  /** 二维码内容/URL */
  text: {
    type: String,
    default: ''
  },
  /** 宽度/高度 (px) */
  width: {
    type: Number,
    default: 80
  },
  /** 二维码边距 */
  margin: {
    type: Number,
    default: 1
  },
  /** 底部标签文字 */
  label: {
    type: String,
    default: ''
  },
  /** 额外 qrcode 配置项 */
  options: {
    type: Object as PropType<QRCodeRenderersOptions>,
    default: () => ({})
  }
})
</script>

<style scoped>
.qr-slot-code :deep(img) {
  display: block;
  margin: 0 auto;
  image-rendering: -webkit-optimize-contrast;
  image-rendering: crisp-edges;
}
</style>
