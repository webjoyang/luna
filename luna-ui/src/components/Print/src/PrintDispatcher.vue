<template>
  <div class="print-dispatcher-inline inline-block">
    <!-- 默认插槽 / 触发按钮 -->
    <slot :open="open">
      <el-button
        v-if="showButton"
        :type="buttonType"
        :size="buttonSize"
        :plain="buttonPlain"
        :link="buttonLink"
        :disabled="buttonDisabled"
        @click="handleTriggerClick"
      >
        <Icon v-if="buttonIcon" :icon="buttonIcon" class="mr-1" />
        {{ buttonText }}
      </el-button>
    </slot>

    <!-- 打印预览与版式选择弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :width="dialogWidth"
      append-to-body
      destroy-on-close
      class="print-preview-dialog"
      :before-close="handleBeforeClose"
    >
      <!-- 多模板切换条（仅在同 bizType 注册了多个版式时展示） -->
      <div v-if="availableTemplates.length > 1" class="template-selector mb-3 flex items-center gap-2">
        <span class="text-13px text-gray-600 font-medium">选择打印版式：</span>
        <el-radio-group v-model="selectedTemplateCode" size="small" @change="handleTemplateChange">
          <el-radio-button
            v-for="tpl in availableTemplates"
            :key="tpl.templateCode"
            :label="tpl.templateCode"
          >
            {{ tpl.name }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- 纸张预览与打印外壳 -->
      <PrintPaper
        :paper="currentTemplate?.paper || 'A4'"
        :title="currentTemplate?.name || '打印预览'"
        :loading="dataLoading"
        :printing="printing"
        :page-count="printItemCount"
        @print="handleExecutePrint"
      >
        <!-- 动态挂载对应的模板组件 -->
        <component
          :is="currentTemplate?.component"
          v-if="currentTemplate && hasData"
          :data="singlePrintData"
          :list="batchPrintDataList"
        />
        <div v-else-if="!dataLoading" class="text-center py-8 text-gray-400">
          暂无打印数据
        </div>
      </PrintPaper>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import type { PropType } from 'vue'
import { ElMessage } from 'element-plus'
import PrintPaper from './PrintPaper.vue'
import { usePrintHtml } from '../composables/usePrintHtml'
import { getTemplatesByBizType } from '../registry'
import type { PrintTemplateReg } from '../registry'

defineOptions({ name: 'PrintDispatcher' })

const props = defineProps({
  /** 业务对象类型，如 WEAVING_TICKET / SHIPMENT */
  bizType: {
    type: String,
    required: true
  },
  /** 单个业务ID */
  bizId: {
    type: [String, Number],
    default: null
  },
  /** 批量业务ID集合 */
  bizIds: {
    type: Array as PropType<(string | number)[]>,
    default: () => []
  },
  /** 指定模板编码（可选） */
  templateCode: {
    type: String,
    default: ''
  },
  /** 是否渲染默认触发按钮 */
  showButton: {
    type: Boolean,
    default: false
  },
  /** 按钮文案 */
  buttonText: {
    type: String,
    default: '打印'
  },
  buttonType: {
    type: String as PropType<'primary' | 'success' | 'warning' | 'danger' | 'info' | 'default'>,
    default: 'primary'
  },
  buttonSize: {
    type: String as PropType<'large' | 'default' | 'small'>,
    default: 'default'
  },
  buttonPlain: {
    type: Boolean,
    default: false
  },
  buttonLink: {
    type: Boolean,
    default: false
  },
  buttonDisabled: {
    type: Boolean,
    default: false
  },
  buttonIcon: {
    type: String,
    default: 'ep:printer'
  }
})

const emit = defineEmits(['printed', 'close'])

const { printElement } = usePrintHtml()

const dialogVisible = ref(false)
const dataLoading = ref(false)
const printing = ref(false)

const activeBizId = ref<string | number | null>(null)
const activeBizIds = ref<(string | number)[]>([])
const selectedTemplateCode = ref<string>('')

const singlePrintData = ref<any>(null)
const batchPrintDataList = ref<any[]>([])

const availableTemplates = computed<PrintTemplateReg[]>(() => {
  return getTemplatesByBizType(props.bizType)
})

const currentTemplate = computed<PrintTemplateReg | undefined>(() => {
  if (selectedTemplateCode.value) {
    return availableTemplates.value.find((t) => t.templateCode === selectedTemplateCode.value)
  }
  return availableTemplates.value[0]
})

const printItemCount = computed(() => {
  if (batchPrintDataList.value.length > 0) {
    return batchPrintDataList.value.length
  }
  return singlePrintData.value ? 1 : 0
})

const hasData = computed(() => {
  return singlePrintData.value != null || batchPrintDataList.value.length > 0
})

const dialogTitle = computed(() => {
  const docNo = singlePrintData.value?.shipmentNo || singlePrintData.value?.code || ''
  const suffix = docNo ? ` - ${docNo}` : ''
  return `${currentTemplate.value?.name || '统一打印'}${suffix} - 打印预览`
})

const dialogWidth = computed(() => {
  return currentTemplate.value?.paper === 'A4' ? '920px' : '820px'
})

// 加载打印数据
const loadPrintData = async () => {
  if (!currentTemplate.value) {
    ElMessage.warning(`未找到业务类型【${props.bizType}】对应的打印模板`)
    return
  }

  dataLoading.value = true
  singlePrintData.value = null
  batchPrintDataList.value = []

  try {
    const idsToFetch = activeBizIds.value.length > 0
      ? activeBizIds.value
      : (activeBizId.value != null ? [activeBizId.value] : [])

    if (idsToFetch.length === 0) {
      ElMessage.warning('未指定打印对象ID')
      dataLoading.value = false
      return
    }

    if (idsToFetch.length === 1) {
      const data = await currentTemplate.value.fetchApi(idsToFetch[0])
      singlePrintData.value = data
    } else {
      const list = await Promise.all(
        idsToFetch.map((id) => currentTemplate.value!.fetchApi(id))
      )
      batchPrintDataList.value = list
    }
  } catch (err: any) {
    console.error('[PrintDispatcher] 取数失败:', err)
  } finally {
    dataLoading.value = false
  }
}

const handleTemplateChange = () => {
  loadPrintData()
}

/** 触发独立 iframe 打印 */
const handleExecutePrint = async (printAreaEl: HTMLElement | null) => {
  if (!printAreaEl) {
    ElMessage.warning('打印区域内容未就绪')
    return
  }
  printing.value = true
  try {
    const paper = currentTemplate.value?.paper || 'A4'
    const pageStyle = paper === 'LABEL-100x60'
      ? '@page { size: 100mm 60mm; margin: 0; }'
      : '@page { size: portrait; margin: 10mm; }'

    await printElement(printAreaEl, {
      title: currentTemplate.value?.name || '打印',
      pageStyle,
      customCss: currentTemplate.value?.customCss || ''
    })
    emit('printed')
  } catch (err) {
    console.error('[PrintDispatcher] 打印调用失败:', err)
  } finally {
    printing.value = false
  }
}

/** 打开弹窗 */
const open = (options?: { bizId?: string | number; bizIds?: (string | number)[]; templateCode?: string }) => {
  if (options?.bizIds && options.bizIds.length > 0) {
    activeBizIds.value = options.bizIds
    activeBizId.value = null
  } else if (options?.bizId != null) {
    activeBizId.value = options.bizId
    activeBizIds.value = []
  } else if (props.bizIds && props.bizIds.length > 0) {
    activeBizIds.value = props.bizIds
    activeBizId.value = null
  } else if (props.bizId != null) {
    activeBizId.value = props.bizId
    activeBizIds.value = []
  }

  const initialCode = options?.templateCode || props.templateCode || availableTemplates.value[0]?.templateCode || ''
  selectedTemplateCode.value = initialCode

  dialogVisible.value = true
  loadPrintData()
}

const handleTriggerClick = () => {
  open()
}

const handleBeforeClose = (done: () => void) => {
  emit('close')
  done()
}

defineExpose({
  open
})
</script>

<style scoped>
:deep(.el-dialog__body) {
  padding: 12px 20px 20px;
}
</style>
