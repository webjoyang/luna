<template>
  <el-dialog
    v-model="dialogVisible"
    :title="isPresetSectionMode ? '新建反馈缸（' + pinnedColorTitle + '）' : '新建反馈缸'"
    width="640px"
    destroy-on-close
    append-to-body
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" v-loading="loading">
      <!-- 染色单与颜色行选择（仅非锚定模式下展示） -->
      <template v-if="!isPresetSectionMode">
        <el-form-item label="染色单" prop="dyeingOrderId">
          <el-select
            v-model="form.dyeingOrderId"
            placeholder="搜索/选择染色单（仅 ACTIVE/COMPLETED）"
            filterable
            remote
            :remote-method="searchOrders"
            :loading="orderLoading"
            style="width: 100%"
            @change="handleOrderChange"
          >
            <el-option
              v-for="item in orderOptions"
              :key="item.id"
              :label="`#${item.id} ${item.contractNo ? ' - ' + item.contractNo : ''} (${item.dyeingFactoryName || '无染厂'})`"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="颜色明细行" prop="dyeingColorId">
          <el-select
            v-model="form.dyeingColorId"
            placeholder="请先选择染色单"
            :disabled="!form.dyeingOrderId"
            style="width: 100%"
            @change="handleColorChange"
          >
            <el-option
              v-for="color in colorSectionOptions"
              :key="color.dyeingColorId"
              :label="`#${color.lineNo ?? 1} ${color.colorName || '未命名'} ${color.customerColorNo ? '(' + color.customerColorNo + ')' : ''} - 需求: ${color.colorQuantity} ${color.unit || 'KG'}`"
              :value="color.dyeingColorId"
            />
          </el-select>
        </el-form-item>
      </template>

      <!-- 选中颜色行信息展示面板（零快照实时计算） -->
      <div v-if="currentColorSection" class="color-info-card">
        <div class="info-row">
          <span class="info-label">销售合同:</span>
          <span class="info-val">{{ currentColorSection.contractNo || '未绑定合同' }}</span>
          <span class="info-sep">|</span>
          <span class="info-label">承染厂:</span>
          <span class="info-val">{{ currentColorSection.dyeingFactoryName || '-' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">客户色号/颜色:</span>
          <span class="info-val highlight">
            {{ currentColorSection.colorName || '-' }}
            <template v-if="currentColorSection.customerColorNo">
              ({{ currentColorSection.customerColorNo }})
            </template>
          </span>
          <span class="info-sep">|</span>
          <span class="info-label">需求数量:</span>
          <span class="info-val bold">
            {{ currentColorSection.colorQuantity }} {{ currentUnit }}
            <template v-if="currentColorSection.colorPieces != null">
              ({{ currentColorSection.colorPieces }} 匹)
            </template>
          </span>
        </div>
        <div class="info-row status-summary">
          <span
            >当前已建缸合计:
            <strong>{{ currentColorSection.totalVatQuantity }} {{ currentUnit }}</strong></span
          >
          <span class="info-sep">|</span>
          <span
            >当前排产覆盖率: <strong>{{ currentColorSection.coveragePercent }}%</strong></span
          >
        </div>
      </div>

      <!-- 缸号 -->
      <el-form-item label="缸号" prop="vatNo">
        <el-input
          v-model="form.vatNo"
          placeholder="请输入缸号（同一颜色行下唯一，如 V01）"
          maxlength="50"
          clearable
        />
      </el-form-item>

      <!-- 本缸数量 -->
      <el-form-item label="本缸数量" prop="quantity">
        <el-input-number
          v-model="form.quantity"
          :min="0.01"
          :precision="2"
          :step="50"
          style="width: 220px"
          placeholder="必须大于0"
        />
        <span class="unit-append">{{ currentUnit }}</span>
      </el-form-item>

      <!-- 匹数 -->
      <el-form-item label="匹数" prop="pieces">
        <el-input-number
          v-model="form.pieces"
          :min="1"
          :step="1"
          :precision="0"
          style="width: 220px"
          placeholder="可空"
        />
        <span class="unit-append">匹</span>
      </el-form-item>

      <!-- 排缸日期 -->
      <el-form-item label="排缸日期" prop="scheduledDate">
        <el-date-picker
          v-model="form.scheduledDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="缺省为当天"
          style="width: 220px"
        />
      </el-form-item>

      <!-- 备注 -->
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
          maxlength="500"
          show-word-limit
          placeholder="选填备注信息"
        />
      </el-form-item>

      <!-- 超排软提示警告条 -->
      <div v-if="isOverScheduledWarning" class="over-warning-box">
        <el-alert type="error" :closable="false" show-icon class="over-alert">
          <template #title>
            <div class="warning-title">超排提示（允许保存）</div>
          </template>
          <div class="warning-desc">
            本缸数量（{{ form.quantity }} {{ currentUnit }}）保存后，总建缸量将达到
            <strong>{{ previewTotalQty }} {{ currentUnit }}</strong
            >， 预计覆盖率为 <strong>{{ previewCoveragePercent }}%</strong> （超出需求
            <strong>{{ overAmount }} {{ currentUnit }}</strong
            >）。
          </div>
        </el-alert>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button
        v-hasPermi="['lunaerp:production:progress-feedback:create']"
        type="primary"
        :loading="submitLoading"
        @click="handleConfirmCreate"
      >
        确定建缸
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useMessage } from '@/hooks/web/useMessage'
import * as DyeingOrderApi from '@/api/lunaerp-elmui/production/dyeing-order'
import type { DyeingOrderVO } from '@/api/lunaerp-elmui/production/dyeing-order'
import {
  createProgressFeedback,
  getProgressFeedbackListByOrder,
  type ProgressFeedbackColorSectionVO,
  type ProgressFeedbackCreateVO
} from '@/api/lunaerp-elmui/production/progress-feedback'

const emit = defineEmits(['success'])
const message = useMessage()

const dialogVisible = ref(false)
const loading = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const isPresetSectionMode = ref(false)
const pinnedSection = ref<ProgressFeedbackColorSectionVO | null>(null)

// 独立菜单建缸时的染色单与颜色明细选项
const orderLoading = ref(false)
const orderOptions = ref<DyeingOrderVO[]>([])
const colorSectionOptions = ref<ProgressFeedbackColorSectionVO[]>([])
const selectedSection = ref<ProgressFeedbackColorSectionVO | null>(null)

const getTodayDateStr = () => {
  const d = new Date()
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const form = reactive<ProgressFeedbackCreateVO>({
  dyeingOrderId: undefined as any,
  dyeingColorId: undefined as any,
  vatNo: '',
  quantity: undefined as any,
  pieces: undefined,
  scheduledDate: getTodayDateStr(),
  remark: ''
})

const rules = reactive<FormRules>({
  dyeingOrderId: [{ required: true, message: '请选择染色单', trigger: 'change' }],
  dyeingColorId: [{ required: true, message: '请选择颜色明细行', trigger: 'change' }],
  vatNo: [
    { required: true, message: '请输入缸号', trigger: 'blur' },
    { max: 50, message: '缸号不能超过50个字符', trigger: 'blur' }
  ],
  quantity: [
    { required: true, message: '请输入本缸数量', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value == null || value <= 0) {
          callback(new Error('本缸数量必须大于0'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

// 当前关联的颜色行骨架节
const currentColorSection = computed(() => {
  return isPresetSectionMode.value ? pinnedSection.value : selectedSection.value
})

const currentUnit = computed(() => {
  return currentColorSection.value?.unit || 'KG'
})

const pinnedColorTitle = computed(() => {
  if (!pinnedSection.value) return ''
  const sec = pinnedSection.value
  return `${sec.colorName || '未命名'} #${sec.lineNo ?? 1}`
})

// 预计总排产数量
const previewTotalQty = computed(() => {
  const currentTotal = currentColorSection.value?.totalVatQuantity || 0
  const inputQty = form.quantity || 0
  return Number((currentTotal + inputQty).toFixed(2))
})

// 预计排产覆盖率
const previewCoveragePercent = computed(() => {
  const demandQty = currentColorSection.value?.colorQuantity || 0
  if (demandQty <= 0) return 0
  return Number(((previewTotalQty.value / demandQty) * 100).toFixed(2))
})

// 是否触发超排软警告
const isOverScheduledWarning = computed(() => {
  const demandQty = currentColorSection.value?.colorQuantity || 0
  if (demandQty <= 0) return false
  return previewTotalQty.value > demandQty
})

// 超排超出数量
const overAmount = computed(() => {
  const demandQty = currentColorSection.value?.colorQuantity || 0
  return Number((previewTotalQty.value - demandQty).toFixed(2))
})

// 搜索仅 ACTIVE/COMPLETED 染色单（后端同口径拉取，避免被 DRAFT/CANCELLED 截断）
const searchOrders = async (query: string) => {
  orderLoading.value = true
  try {
    const [activeRes, completedRes] = await Promise.all([
      DyeingOrderApi.getDyeingOrderPage({
        pageNo: 1,
        pageSize: 20,
        status: 'ACTIVE',
        keyword: query || undefined
      }),
      DyeingOrderApi.getDyeingOrderPage({
        pageNo: 1,
        pageSize: 20,
        status: 'COMPLETED',
        keyword: query || undefined
      })
    ])
    const activeList: DyeingOrderVO[] = activeRes.list || []
    const completedList: DyeingOrderVO[] = completedRes.list || []
    orderOptions.value = [...activeList, ...completedList]
  } catch (error) {
    console.error('搜索染色单失败', error)
  } finally {
    orderLoading.value = false
  }
}

// 染色单切换联动加载颜色行节
const handleOrderChange = async (orderId: number) => {
  form.dyeingColorId = undefined as any
  selectedSection.value = null
  colorSectionOptions.value = []
  if (!orderId) return

  loading.value = true
  try {
    const sections = await getProgressFeedbackListByOrder(orderId)
    colorSectionOptions.value = sections || []
    if (sections && sections.length === 1) {
      form.dyeingColorId = sections[0].dyeingColorId
      selectedSection.value = sections[0]
    }
  } catch (error) {
    console.error('加载染色单颜色行失败', error)
  } finally {
    loading.value = false
  }
}

// 颜色明细行切换
const handleColorChange = (colorId: number) => {
  selectedSection.value = colorSectionOptions.value.find((c) => c.dyeingColorId === colorId) || null
}

// 打开弹窗（支持传入 presetSection 进行直接锚定建缸）
const open = (presetSection?: ProgressFeedbackColorSectionVO) => {
  dialogVisible.value = true
  form.vatNo = ''
  form.quantity = undefined as any
  form.pieces = undefined
  form.scheduledDate = getTodayDateStr()
  form.remark = ''

  if (presetSection) {
    isPresetSectionMode.value = true
    pinnedSection.value = presetSection
    form.dyeingOrderId = presetSection.dyeingOrderId
    form.dyeingColorId = presetSection.dyeingColorId
  } else {
    isPresetSectionMode.value = false
    pinnedSection.value = null
    form.dyeingOrderId = undefined as any
    form.dyeingColorId = undefined as any
    selectedSection.value = null
    colorSectionOptions.value = []
    searchOrders('')
  }
}

// 提交建缸
const handleConfirmCreate = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  submitLoading.value = true
  try {
    await createProgressFeedback({
      dyeingOrderId: form.dyeingOrderId,
      dyeingColorId: form.dyeingColorId,
      vatNo: form.vatNo.trim(),
      quantity: form.quantity,
      pieces: form.pieces || undefined,
      scheduledDate: form.scheduledDate || undefined,
      remark: form.remark ? form.remark.trim() : undefined
    })

    message.success('建缸成功')
    dialogVisible.value = false
    emit('success')
  } catch (error) {
    console.error('建缸失败', error)
  } finally {
    submitLoading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.color-info-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 10px 14px;
  margin-bottom: 18px;
  font-size: 13px;
  color: #475569;

  .info-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    line-height: 22px;

    &.status-summary {
      margin-top: 6px;
      padding-top: 6px;
      border-top: 1px dashed #e2e8f0;
      color: #334155;
    }
  }

  .info-label {
    color: #64748b;
    margin-right: 4px;
  }

  .info-val {
    color: #1e293b;
    margin-right: 8px;

    &.highlight {
      font-weight: 600;
      color: #0284c7;
    }

    &.bold {
      font-weight: 600;
    }
  }

  .info-sep {
    color: #cbd5e1;
    margin: 0 8px;
  }
}

.unit-append {
  margin-left: 10px;
  color: #64748b;
  font-weight: 500;
}

.over-warning-box {
  margin-top: 10px;

  .over-alert {
    background: #fef2f2;
    border: 1px solid #fecaca;
  }

  .warning-title {
    font-weight: 600;
    color: #b91c1c;
  }

  .warning-desc {
    margin-top: 4px;
    font-size: 13px;
    color: #991b1b;
    line-height: 18px;
  }
}
</style>
