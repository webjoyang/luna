<template>
  <div class="print-paper-wrapper" v-loading="loading">
    <!-- 顶部操作栏 -->
    <div class="print-toolbar flex items-center justify-between pb-3 mb-3 border-b border-gray-200">
      <div class="flex items-center gap-2">
        <span class="font-bold text-15px text-gray-800">{{ title }}</span>
        <el-tag size="small" type="info">
          {{ paperBadgeText }}
        </el-tag>
        <span v-if="pageCount && pageCount > 1" class="text-12px text-gray-500">
          共 {{ pageCount }} 页/标签
        </span>
      </div>
      <div class="flex items-center gap-2">
        <slot name="extra-actions"></slot>
        <el-button
          v-if="showPrintButton"
          type="primary"
          :loading="printing"
          @click="handlePrintClick"
        >
          <Icon icon="ep:printer" class="mr-1" /> 发送打印机
        </el-button>
      </div>
    </div>

    <!-- 纸张预览区域 -->
    <div class="print-paper-viewport bg-gray-100 p-4 rounded overflow-auto flex justify-center">
      <div
        ref="printAreaRef"
        class="print-paper-sheet bg-white text-black shadow-md"
        :class="paperClass"
      >
        <slot :print-area-ref="printAreaRef"></slot>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'

defineOptions({ name: 'PrintPaper' })

const props = defineProps({
  paper: {
    type: String as () => 'A4' | 'LABEL-100x60',
    default: 'A4'
  },
  title: {
    type: String,
    default: '打印预览'
  },
  loading: {
    type: Boolean,
    default: false
  },
  printing: {
    type: Boolean,
    default: false
  },
  showPrintButton: {
    type: Boolean,
    default: true
  },
  pageCount: {
    type: Number,
    default: 1
  }
})

const emit = defineEmits(['print'])

const printAreaRef = ref<HTMLElement | null>(null)

const paperBadgeText = computed(() => {
  if (props.paper === 'LABEL-100x60') return '100×60mm 不干胶'
  return 'A4 纸张'
})

const paperClass = computed(() => {
  return props.paper === 'LABEL-100x60' ? 'paper-label-100x60' : 'paper-a4'
})

const handlePrintClick = () => {
  emit('print', printAreaRef.value)
}

defineExpose({
  getPrintArea: () => printAreaRef.value
})
</script>

<style scoped>
.print-paper-viewport {
  max-height: 72vh;
}

.paper-a4 {
  width: 210mm;
  min-height: 297mm;
  padding: 10mm;
  margin: 0 auto;
  box-sizing: border-box;
}

.paper-label-100x60 {
  width: 100mm;
  min-height: 60mm;
  padding: 3mm 4mm;
  margin: 0 auto;
  box-sizing: border-box;
}

/* 屏幕预览自适应小屏幕 */
@media screen and (max-width: 900px) {
  .paper-a4 {
    width: 100%;
    min-height: auto;
    padding: 12px;
  }
}
</style>
