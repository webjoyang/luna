<template>
  <div class="release-production-tab" v-loading="loading">
    <!-- 顶部状态栏 -->
    <el-card shadow="never" class="mb-16px info-card">
      <div class="flex items-center justify-between flex-wrap gap-4">
        <div class="flex items-center gap-6 flex-wrap">
          <div class="info-item">
            <span class="label">销售合同：</span>
            <span class="value font-bold font-mono">{{ contractInfo.contractNo || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="label">客户：</span>
            <span class="value">{{ contractInfo.clientName || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="label">交货日期：</span>
            <span class="value">{{ contractInfo.deliveryDate || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="label">合同状态：</span>
            <DictTag
              v-if="contractInfo.status"
              :type="DICT_TYPE.LUNAERP_SALES_CONTRACT_STATUS"
              :value="contractInfo.status"
            />
            <span v-else class="text-gray-400">—</span>
          </div>
        </div>
        <div>
          <el-button @click="loadData" :loading="loading">
            <Icon icon="ep:refresh" class="mr-2px" /> 刷新数据
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- CANCELLED 合同红色警示条 -->
    <el-alert
      v-if="isCancelled"
      title="当前合同已取消，无法下达生产"
      type="error"
      show-icon
      :closable="false"
      class="mb-16px"
    />

    <!-- 工厂选择区 -->
    <el-card shadow="never" class="mb-16px factory-card">
      <template #header>
        <div class="card-header font-bold text-15px">
          <span class="section-badge">1</span> 工厂配置（必填）
        </div>
      </template>
      <el-form label-width="120px" :disabled="isCancelled">
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="承织坯布厂" required>
              <el-select
                v-model="factoryConfig.greigeFactoryId"
                placeholder="搜索并选择织造加工厂（必填）"
                filterable
                remote
                reserve-keyword
                :remote-method="searchGreigeFactories"
                :loading="greigeFactoryLoading"
                class="!w-full"
              >
                <el-option
                  v-for="item in greigeFactoryOptions"
                  :key="item.id"
                  :label="item.companyName"
                  :value="item.id!"
                >
                  <span>{{ item.companyName }}</span>
                  <span v-if="item.shortName" class="text-gray-400 ml-8px">({{ item.shortName }})</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="承染方" required>
              <el-select
                v-model="factoryConfig.dyeingFactoryId"
                placeholder="搜索并选择染厂（必填）"
                filterable
                remote
                reserve-keyword
                :remote-method="searchDyeingFactories"
                :loading="dyeingFactoryLoading"
                class="!w-full"
              >
                <el-option
                  v-for="item in dyeingFactoryOptions"
                  :key="item.id"
                  :label="item.companyName"
                  :value="item.id!"
                >
                  <span>{{ item.companyName }}</span>
                  <span v-if="item.shortName" class="text-gray-400 ml-8px">({{ item.shortName }})</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 下达区：产品分组卡片与明细表格 -->
    <div class="release-groups mb-20px">
      <div v-if="groups.length === 0 && !loading" class="text-center py-40px text-gray-400">
        该合同暂无明细行可下达
      </div>

      <el-card
        v-for="(group, gIdx) in groups"
        :key="group.productCode || gIdx"
        shadow="never"
        class="mb-16px group-card"
      >
        <template #header>
          <div class="flex items-center justify-between flex-wrap gap-3">
            <div class="flex items-center gap-3">
              <span class="product-badge">{{ group.productCode }}</span>
              <span class="font-bold text-15px text-gray-800">{{ group.productName }}</span>
              <span v-if="group.composition || group.width || group.gsm" class="text-gray-500 text-13px">
                ({{ [group.composition, group.width, group.gsm].filter(Boolean).join(' · ') }})
              </span>
              <el-tag type="info" size="small" effect="plain">
                同产品合并生成 1 张胚布单与 1 张染色单
              </el-tag>
            </div>

            <!-- 打色单模式选择 -->
            <div class="flex items-center gap-2">
              <span class="text-13px text-gray-600 font-medium">打色单：</span>
              <el-select
                v-model="group.colorInstructionMode"
                size="default"
                class="!w-190px"
                :disabled="isCancelled"
              >
                <el-option label="不关联打色单" value="NONE" />
                <el-option label="引用已有已确认单" value="EXISTING" />
                <el-option label="新建打色指示单" value="NEW" />
              </el-select>

              <!-- EXISTING 模式选择已有打色单 -->
              <template v-if="group.colorInstructionMode === 'EXISTING'">
                <el-select
                  v-model="group.existingColorInstructionId"
                  placeholder="选择已有打色单（须已确认）"
                  size="default"
                  class="!w-260px"
                  :disabled="isCancelled"
                >
                  <el-option
                    v-for="inst in getMatchingColorInstructions(group.productCode)"
                    :key="inst.id"
                    :label="inst.instructionNo"
                    :value="inst.id"
                  >
                    <div class="flex items-center justify-between">
                      <span class="font-mono">{{ inst.instructionNo }}</span>
                      <DictTag
                        :type="DICT_TYPE.LUNAERP_COLOR_INSTRUCTION_STATUS"
                        :value="inst.status"
                        class="ml-8px"
                      />
                    </div>
                  </el-option>
                </el-select>
              </template>
            </div>
          </div>
        </template>

        <!-- 明细表格 -->
        <el-table :data="group.lines" border size="small" class="w-full">
          <el-table-column label="行号" prop="lineNo" width="60" align="center" />
          <el-table-column label="产品编号 / 名称" min-width="160">
            <template #default="{ row }">
              <div class="font-medium text-gray-800">{{ row.productCode }}</div>
              <div class="text-12px text-gray-500">{{ row.productName }}</div>
            </template>
          </el-table-column>
          <el-table-column label="客户色号" prop="customerColor" min-width="120" />
          <el-table-column label="合同数量" prop="contractQty" width="110" align="right">
            <template #default="{ row }">{{ formatQty(row.contractQty) }} KG</template>
          </el-table-column>
          <el-table-column label="已下达量" prop="releasedQty" width="110" align="right">
            <template #default="{ row }">
              <span :class="{ 'text-primary font-medium': row.releasedQty > 0 }">
                {{ formatQty(row.releasedQty) }} KG
              </span>
            </template>
          </el-table-column>
          <el-table-column label="剩余可下达" prop="remainingQty" width="120" align="right">
            <template #default="{ row }">
              <span class="font-bold text-gray-700">{{ formatQty(row.remainingQty) }} KG</span>
            </template>
          </el-table-column>
          <el-table-column label="本次下达量 (KG)" width="180" align="center">
            <template #default="{ row }">
              <div class="flex flex-col items-center">
                <el-input-number
                  v-model="row.releaseQty"
                  :min="0"
                  :precision="2"
                  :step="100"
                  size="small"
                  :disabled="isCancelled"
                  :class="{ 'qty-over-limit': row.releaseQty > row.remainingQty }"
                  class="!w-140px"
                />
                <span
                  v-if="row.releaseQty > row.remainingQty"
                  class="text-12px text-red-500 mt-2px"
                >
                  超额 (余 {{ formatQty(row.remainingQty) }})
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="损耗率 (%)" width="130" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.lossRatePercent"
                :min="0"
                :max="100"
                :precision="1"
                :step="1"
                size="small"
                :disabled="isCancelled"
                class="!w-100px"
              />
            </template>
          </el-table-column>
        </el-table>

        <!-- 打色单配置区（仅当选 NEW 时展开） -->
        <div v-if="group.colorInstructionMode === 'NEW'" class="new-color-instruction-box mt-16px">
          <div class="box-title flex items-center justify-between mb-12px">
            <div class="flex items-center gap-2">
              <Icon icon="ep:magic-stick" class="text-primary" />
              <span class="font-bold text-14px text-gray-800">新建打色指示单配置</span>
              <span class="text-12px text-gray-500">（下达时将自动创建待打色状态单据）</span>
            </div>
            <div class="flex items-center gap-2">
              <span class="text-13px text-gray-600">打色承接方：</span>
              <el-select
                v-if="group.newColorInstruction"
                v-model="group.newColorInstruction.labFactoryId"
                placeholder="留空为本厂打色房"
                clearable
                filterable
                remote
                reserve-keyword
                :remote-method="searchLabFactories"
                :loading="labFactoryLoading"
                size="small"
                class="!w-200px"
                :disabled="isCancelled"
              >
                <el-option
                  v-for="item in labFactoryOptions"
                  :key="item.id"
                  :label="item.companyName"
                  :value="item.id!"
                />
              </el-select>
            </div>
          </div>

          <el-table
            v-if="group.newColorInstruction?.colors"
            :data="group.newColorInstruction.colors"
            border
            size="small"
            class="w-full bg-gray-50"
          >
            <el-table-column label="客户色号" prop="customerColorNo" width="120" />
            <el-table-column label="颜色名称" width="140">
              <template #default="{ row }">
                <el-input v-model="row.colorName" size="small" :disabled="isCancelled" />
              </template>
            </el-table-column>
            <el-table-column label="主灯源 (必填)" width="160">
              <template #default="{ row }">
                <el-select
                  v-model="row.lightPrimary"
                  size="small"
                  placeholder="选择主灯源"
                  :disabled="isCancelled"
                  :class="{ 'light-missing': !row.lightPrimary }"
                >
                  <el-option
                    v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_COLOR_LIGHT_SOURCE)"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="辅灯源" width="150">
              <template #default="{ row }">
                <el-select
                  v-model="row.lightSecondary"
                  size="small"
                  clearable
                  placeholder="可空"
                  :disabled="isCancelled"
                >
                  <el-option
                    v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_COLOR_LIGHT_SOURCE)"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="厂内色号" width="130">
              <template #default="{ row }">
                <el-input v-model="row.factoryColorNo" size="small" placeholder="可空" :disabled="isCancelled" />
              </template>
            </el-table-column>
            <el-table-column label="色卡参考" min-width="140">
              <template #default="{ row }">
                <el-input v-model="row.colorCardRef" size="small" placeholder="可空" :disabled="isCancelled" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>
    </div>

    <!-- 底部操作与汇总栏 -->
    <div class="summary-footer-bar">
      <div class="summary-details flex items-center gap-6 flex-wrap">
        <div class="summary-item">
          <span class="label">本次下达胚布量：</span>
          <span class="val font-mono font-bold">{{ formatQty(summary.totalGreigeQty) }}</span>
          <span class="unit">KG</span>
        </div>
        <div class="summary-item">
          <span class="label">本次下达染色量：</span>
          <span class="val font-mono font-bold">{{ formatQty(summary.totalDyeingQty) }}</span>
          <span class="unit">KG</span>
        </div>
        <div class="summary-item text-gray-600">
          <span class="label">生成单据预估：</span>
          <span class="tag-count">胚布单 {{ summary.greigeOrderCount }} 张</span>
          <span class="tag-count">染色单 {{ summary.dyeingOrderCount }} 张</span>
          <span class="tag-count">胚布检验单 {{ summary.inspectionOrderCount }} 张</span>
          <span v-if="summary.colorInstructionCount > 0" class="tag-count text-primary font-medium">
            打色单 {{ summary.colorInstructionCount }} 张
          </span>
        </div>
      </div>

      <div class="actions">
        <el-button
          type="primary"
          size="large"
          :loading="submitting"
          :disabled="isCancelled || summary.totalGreigeQty <= 0 || hasErrors"
          @click="handleReleaseSubmit"
          v-hasPermi="['lunaerp:production:release-production:release']"
        >
          <Icon icon="ep:promotion" class="mr-4px" /> 下达生产
        </el-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useMessage } from '@/hooks/web/useMessage'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as ReleaseProductionApi from '@/api/lunaerp-elmui/production/release-production'
import type {
  ReleaseFromContractRespVO,
  ExistingColorInstructionVO
} from '@/api/lunaerp-elmui/production/release-production'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SalesContractVO } from '@/api/lunaerp-elmui/business/sales-contract'
import {
  initReleaseWorkbenchState,
  calculateReleaseSummary,
  validateReleaseForm,
  buildReleaseProductionPayload,
  type ReleaseWorkbenchGroup,
  type ReleaseWorkbenchLine
} from './releaseProductionHelper'

defineOptions({ name: 'ReleaseProductionTab' })

const props = defineProps<{
  contractId: number
  contract?: SalesContractVO
}>()

const emit = defineEmits<{
  (e: 'released', data: ReleaseProductionApi.ReleaseProductionRespVO): void
  (e: 'refresh'): void
}>()

const message = useMessage()

const loading = ref(false)
const submitting = ref(false)

// 基础信息
const contractInfo = reactive({
  contractNo: '',
  clientName: '',
  deliveryDate: '',
  status: ''
})

// 工厂配置
const factoryConfig = reactive<{
  greigeFactoryId?: number
  dyeingFactoryId?: number
}>({
  greigeFactoryId: undefined,
  dyeingFactoryId: undefined
})

const greigeFactoryLoading = ref(false)
const greigeFactoryOptions = ref<SupplierVO[]>([])

const dyeingFactoryLoading = ref(false)
const dyeingFactoryOptions = ref<SupplierVO[]>([])

const labFactoryLoading = ref(false)
const labFactoryOptions = ref<SupplierVO[]>([])

// 产品分组与已有打色单
interface ExtendedWorkbenchLine extends ReleaseWorkbenchLine {
  lossRatePercent: number
}

interface ExtendedWorkbenchGroup extends ReleaseWorkbenchGroup {
  lines: ExtendedWorkbenchLine[]
}

const groups = ref<ExtendedWorkbenchGroup[]>([])
const existingColorInstructions = ref<ExistingColorInstructionVO[]>([])

// 合同是否已取消
const isCancelled = computed(() => {
  return contractInfo.status === 'CANCELLED' || props.contract?.status === 'CANCELLED'
})

// 汇总数据
const summary = computed(() => {
  return calculateReleaseSummary(groups.value)
})

// 校验是否有错误（包括超限）
const hasErrors = computed(() => {
  for (const g of groups.value) {
    for (const l of g.lines) {
      if (Number(l.releaseQty || 0) > Number(l.remainingQty || 0)) {
        return true
      }
    }
  }
  return false
})

/** 数量格式化 */
const formatQty = (qty?: number | string) => {
  return Number(qty ?? 0).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

/** 筛选匹配产品的已有打色单（须 CONFIRMED 或 IN_PRODUCTION） */
const getMatchingColorInstructions = (productCode: string) => {
  const allowedStatuses = ['CONFIRMED', 'IN_PRODUCTION']
  return existingColorInstructions.value.filter((inst) => {
    const productMatch = !inst.productCode || inst.productCode === productCode
    const statusMatch = allowedStatuses.includes(inst.status)
    return productMatch && statusMatch
  })
}

/** 通用供应商远程搜索 */
const searchSuppliers = async (
  query: string,
  loadingRef: { value: boolean },
  optionsRef: { value: SupplierVO[] }
) => {
  loadingRef.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 20,
      search: query || '',
      status: 'ACTIVE'
    })
    optionsRef.value = res.list || []
  } finally {
    loadingRef.value = false
  }
}

const searchGreigeFactories = (query: string) =>
  searchSuppliers(query, greigeFactoryLoading, greigeFactoryOptions)
const searchDyeingFactories = (query: string) =>
  searchSuppliers(query, dyeingFactoryLoading, dyeingFactoryOptions)
const searchLabFactories = (query: string) =>
  searchSuppliers(query, labFactoryLoading, labFactoryOptions)

/** 加载数据 */
const loadData = async (clearReleaseQty = false) => {
  if (!props.contractId) {
    return
  }
  loading.value = true
  try {
    const data: ReleaseFromContractRespVO = await ReleaseProductionApi.getReleaseFromContract(
      props.contractId
    )

    contractInfo.contractNo = data.contractNo || props.contract?.contractNo || ''
    contractInfo.clientName = data.clientName || props.contract?.clientName || ''
    contractInfo.deliveryDate = props.contract?.deliveryDate || ''
    contractInfo.status = props.contract?.status || ''

    existingColorInstructions.value = data.existingColorInstructions || []

    // 初始化产品分组（支持刷新后清空本次下达量）
    const initialGroups = initReleaseWorkbenchState(data.lines || [], clearReleaseQty)
    groups.value = initialGroups.map((g) => ({
      ...g,
      lines: g.lines.map((l) => ({
        ...l,
        lossRatePercent: Number((Number(l.lossRate || 0) * 100).toFixed(1))
      }))
    }))
  } finally {
    loading.value = false
  }
}

/** 提交下达生产 */
const handleReleaseSubmit = async () => {
  // 同步百分比到 lossRate 小数
  for (const g of groups.value) {
    for (const l of g.lines) {
      l.lossRate = Number(((l.lossRatePercent || 0) / 100).toFixed(4))
    }
  }

  // 表单校验
  const validation = validateReleaseForm({
    contractStatus: contractInfo.status,
    greigeFactoryId: factoryConfig.greigeFactoryId,
    dyeingFactoryId: factoryConfig.dyeingFactoryId,
    groups: groups.value
  })

  if (!validation.valid) {
    message.error(validation.errors[0])
    return
  }

  // 二次确认单据清单
  const s = summary.value
  const confirmLines = [
    `确定要下达生产吗？本次将生成：`,
    `· 胚布单：${s.greigeOrderCount} 张`,
    `· 染色单：${s.dyeingOrderCount} 张`,
    `· 胚布检验单：${s.inspectionOrderCount} 张`,
    s.colorInstructionCount > 0 ? `· 打色指示单：${s.colorInstructionCount} 张` : '',
    `合计下达量：${formatQty(s.totalGreigeQty)} KG`
  ].filter(Boolean)

  try {
    await ElMessageBox.confirm(confirmLines.join('\n'), '下达生产确认', {
      confirmButtonText: '确定下达',
      cancelButtonText: '取消',
      type: 'warning'
    })

    submitting.value = true
    const payload = buildReleaseProductionPayload({
      contractId: props.contractId,
      greigeFactoryId: factoryConfig.greigeFactoryId!,
      dyeingFactoryId: factoryConfig.dyeingFactoryId!,
      groups: groups.value
    })

    const res = await ReleaseProductionApi.releaseProduction(payload)
    message.success('下达生产成功！')

    emit('released', res)
    emit('refresh')

    // 重新拉取预填接口刷新已下达量并清空本次下达量
    await loadData(true)
  } catch (e: any) {
    if (e !== 'cancel') {
      // API error handled by request interceptor or message
    }
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.contract,
  (val) => {
    if (val) {
      if (val.contractNo) contractInfo.contractNo = val.contractNo
      if (val.clientName) contractInfo.clientName = val.clientName
      if (val.deliveryDate) contractInfo.deliveryDate = val.deliveryDate
      if (val.status) contractInfo.status = val.status
    }
  },
  { immediate: true, deep: true }
)

watch(
  () => props.contractId,
  (newId) => {
    if (newId) {
      loadData()
    }
  }
)

onMounted(() => {
  loadData()
  searchGreigeFactories('')
  searchDyeingFactories('')
  searchLabFactories('')
})
</script>

<style lang="scss" scoped>
.release-production-tab {
  padding-bottom: 70px;

  .info-card {
    background-color: var(--el-fill-color-light);
    border-radius: 6px;

    .info-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;

      .label {
        color: var(--el-text-color-secondary);
      }
      .value {
        color: var(--el-text-color-primary);
      }
    }
  }

  .section-badge {
    display: inline-block;
    width: 20px;
    height: 20px;
    line-height: 20px;
    text-align: center;
    border-radius: 50%;
    background-color: var(--el-color-primary);
    color: #fff;
    font-size: 12px;
    margin-right: 6px;
  }

  .group-card {
    border-radius: 8px;
    border-color: var(--el-border-color-lighter);

    .product-badge {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 4px;
      background: var(--el-color-primary-light-9);
      color: var(--el-color-primary);
      font-weight: 600;
      font-size: 13px;
    }
  }

  .qty-over-limit {
    :deep(.el-input__wrapper) {
      border: 1px solid var(--el-color-danger) !important;
      box-shadow: 0 0 0 1px var(--el-color-danger) inset !important;
    }
  }

  .light-missing {
    :deep(.el-input__wrapper) {
      border: 1px solid var(--el-color-danger) !important;
    }
  }

  .new-color-instruction-box {
    padding: 12px 16px;
    border-radius: 6px;
    background-color: var(--el-fill-color-blank);
    border: 1px dashed var(--el-border-color);
  }

  .summary-footer-bar {
    position: sticky;
    bottom: 0;
    z-index: 10;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 20px;
    background: #ffffff;
    box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.06);
    border-radius: 6px;
    border: 1px solid var(--el-border-color-lighter);

    .summary-item {
      font-size: 14px;
      display: flex;
      align-items: baseline;
      gap: 4px;

      .label {
        color: var(--el-text-color-secondary);
      }
      .val {
        font-size: 18px;
        color: var(--el-color-primary);
      }
      .unit {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    .tag-count {
      display: inline-block;
      padding: 2px 6px;
      background: var(--el-fill-color);
      border-radius: 4px;
      font-size: 12px;
      margin-left: 6px;
    }
  }
}
</style>
