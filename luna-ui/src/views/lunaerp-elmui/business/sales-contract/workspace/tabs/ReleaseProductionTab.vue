<template>
  <div class="release-production-tab" v-loading="loading">
    <!-- 顶部状态提示：合同已取消时红色报警 -->
    <el-alert
      v-if="isCancelled"
      title="合同已取消，无法下达生产！"
      type="error"
      description="该销售合同当前状态为已取消 (CANCELLED)，所有生产下达操作已被禁用。"
      show-icon
      :closable="false"
      class="mb-16px"
    />

    <!-- 顶部摘要卡片 -->
    <el-card shadow="never" class="mb-16px header-card">
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="合同编号">
          <span class="font-bold text-primary">{{ contractInfo.contractNo || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="客户名称">
          <span>{{ contractInfo.clientName || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="交货日期">
          <span>{{ contractInfo.deliveryDate || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="合同状态">
          <DictTag
            v-if="contractInfo.status"
            :type="DICT_TYPE.LUNAERP_SALES_CONTRACT_STATUS"
            :value="contractInfo.status"
          />
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 工厂配置区（必填） -->
    <el-card shadow="never" class="mb-16px factory-card">
      <template #header>
        <div class="card-header-title">
          <span class="section-title">工厂配置</span>
          <span class="header-tip">（下达生产将为各产品生成胚布单与染色单，请指定承接工厂）</span>
        </div>
      </template>
      <el-form :model="factoryForm" label-width="110px" class="factory-form">
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="承织坯布厂" required>
              <el-select
                v-model="factoryForm.greigeFactoryId"
                placeholder="输入关键词搜索坯布厂"
                filterable
                remote
                reserve-keyword
                :remote-method="searchGreigeFactories"
                :loading="greigeLoading"
                :disabled="isCancelled"
                class="!w-full"
              >
                <el-option
                  v-for="item in greigeFactoryOptions"
                  :key="item.id"
                  :label="item.companyName"
                  :value="item.id!"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="承染方" required>
              <el-select
                v-model="factoryForm.dyeingFactoryId"
                placeholder="输入关键词搜索染厂"
                filterable
                remote
                reserve-keyword
                :remote-method="searchDyeingFactories"
                :loading="dyeingLoading"
                :disabled="isCancelled"
                class="!w-full"
              >
                <el-option
                  v-for="item in dyeingFactoryOptions"
                  :key="item.id"
                  :label="item.companyName"
                  :value="item.id!"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 可编辑下达明细表格 -->
    <el-card shadow="never" class="mb-16px table-card">
      <template #header>
        <div class="card-header-title">
          <span class="section-title">下达明细</span>
          <span class="header-tip">（同产品编号合同行将合并生成同一张胚布单与染色单；本次下达量缺省为剩余可下达量）</span>
        </div>
      </template>

      <el-table
        :data="tableRows"
        border
        size="small"
        :row-class-name="getRowClassName"
        class="release-table"
      >
        <el-table-column label="行号" prop="lineNo" width="60" align="center" />
        <el-table-column label="产品编号 / 品名" min-width="180">
          <template #default="{ row }">
            <div class="product-cell">
              <span class="product-code font-bold">{{ row.productCode || '-' }}</span>
              <el-tag
                v-if="productGroupCountMap[row.productCode] > 1"
                type="info"
                size="small"
                effect="plain"
                class="ml-6px"
              >
                同品合并({{ productGroupCountMap[row.productCode] }}行)
              </el-tag>
              <div class="product-name text-gray-500 text-xs">{{ row.productName || '-' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="客户色号" prop="customerColor" min-width="100">
          <template #default="{ row }">
            <span>{{ row.customerColor || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="合同数量" prop="contractQty" width="110" align="right">
          <template #default="{ row }">
            <span>{{ formatQty(row.contractQty) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="已下达量" prop="releasedQty" width="110" align="right">
          <template #default="{ row }">
            <span>{{ formatQty(row.releasedQty) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="剩余量" prop="remainingQty" width="110" align="right">
          <template #default="{ row }">
            <span :class="{ 'text-gray-400': row.remainingQty <= 0 }">
              {{ formatQty(row.remainingQty) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="本次下达量" width="150" align="center">
          <template #default="{ row }">
            <div :class="{ 'qty-error-wrapper': isQtyExceeded(row) }">
              <el-input-number
                v-model="row.releaseQty"
                :min="0"
                :precision="2"
                :controls="false"
                :disabled="isCancelled"
                placeholder="下达数量"
                class="!w-full"
                :class="{ 'is-error-input': isQtyExceeded(row) }"
              />
              <div v-if="isQtyExceeded(row)" class="error-tip">
                超出剩余量({{ formatQty(row.remainingQty) }})
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="损耗率(%)" width="120" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.lossRatePercent"
              :min="0"
              :max="100"
              :precision="2"
              :controls="false"
              :disabled="isCancelled"
              placeholder="0.00"
              class="!w-full"
            />
          </template>
        </el-table-column>
        <el-table-column label="打色单" min-width="200">
          <template #default="{ row }">
            <div class="color-mode-cell">
              <el-select
                v-model="row.colorInstructionMode"
                size="small"
                :disabled="isCancelled"
                class="!w-full"
                @change="(val) => handleModeChange(row, val)"
              >
                <el-option label="不关联" value="NONE" />
                <el-option label="选已有 (CONFIRMED)" value="EXISTING" />
                <el-option label="新建 (PENDING)" value="NEW" />
              </el-select>

              <el-select
                v-if="row.colorInstructionMode === 'EXISTING'"
                v-model="row.existingColorInstructionId"
                size="small"
                placeholder="请选择已有打色单"
                :disabled="isCancelled"
                class="!w-full mt-4px"
                @change="(val) => handleExistingInstructionChange(row, val)"
              >
                <el-option
                  v-for="item in getProductExistingInstructions(row.productCode)"
                  :key="item.id"
                  :label="`${item.instructionNo} (${item.status})`"
                  :value="item.id"
                />
              </el-select>
              <div
                v-if="
                  row.colorInstructionMode === 'EXISTING' &&
                  getProductExistingInstructions(row.productCode).length === 0
                "
                class="text-xs text-orange-500 mt-2px"
              >
                暂无此产品可用打色单
              </div>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无合同明细数据" :image-size="60" />
        </template>
      </el-table>
    </el-card>

    <!-- 打色单配置区（当任一产品组选择「新建」时展开） -->
    <el-card v-if="hasNewColorInstructionGroups" shadow="never" class="mb-16px new-color-card">
      <template #header>
        <div class="card-header-title">
          <span class="section-title">新建打色单配置</span>
          <span class="header-tip">（选择「新建」的产品组将生成 PENDING 状态打色单，主灯源为必填项）</span>
        </div>
      </template>

      <div
        v-for="group in newColorGroups"
        :key="group.productCode"
        class="new-color-group-box mb-16px"
      >
        <div class="group-title">
          <span class="font-bold">产品：{{ group.productCode }}</span>
          <span v-if="group.productName" class="ml-8px text-gray-500">({{ group.productName }})</span>
        </div>

        <el-form label-width="110px" class="mt-8px mb-12px">
          <el-form-item label="打色承接方">
            <el-select
              v-model="groupConfigMap[group.productCode].labFactoryId"
              placeholder="留空为本厂打色房，或搜索外部打色厂"
              filterable
              remote
              clearable
              reserve-keyword
              :remote-method="searchLabSuppliers"
              :loading="labLoading"
              :disabled="isCancelled"
              style="width: 320px"
            >
              <el-option
                v-for="item in labSupplierOptions"
                :key="item.id"
                :label="item.companyName"
                :value="item.id!"
              />
            </el-select>
            <span class="text-xs text-gray-400 ml-8px">（可空，留空默认为本厂打色房）</span>
          </el-form-item>
        </el-form>

        <el-table
          :data="group.lines"
          border
          size="small"
          class="color-lines-table"
        >
          <el-table-column label="客户色号" prop="customerColor" min-width="120">
            <template #default="{ row }">
              <span>{{ row.customerColor || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="主灯源" min-width="140">
            <template #header>
              <span class="text-red-500 mr-2px">*</span>
              <span>主灯源</span>
            </template>
            <template #default="{ row }">
              <el-select
                v-model="getColorConfig(group.productCode, row.contractLineId).lightPrimary"
                placeholder="请选择主灯源"
                size="small"
                :disabled="isCancelled"
                class="!w-full"
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
          <el-table-column label="工厂色号" min-width="130">
            <template #default="{ row }">
              <el-input
                v-model="getColorConfig(group.productCode, row.contractLineId).factoryColorNo"
                placeholder="选填工厂色号"
                size="small"
                :disabled="isCancelled"
              />
            </template>
          </el-table-column>
          <el-table-column label="色卡参考" min-width="140">
            <template #default="{ row }">
              <el-input
                v-model="getColorConfig(group.productCode, row.contractLineId).colorCardRef"
                placeholder="选填色卡参考"
                size="small"
                :disabled="isCancelled"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 底部汇总及提交按钮 -->
    <div class="bottom-bar">
      <div class="summary-info">
        本次下达汇总：
        胚布量 <b>{{ formatQty(totalGreigeQty) }}</b> KG ／
        染色量 <b>{{ formatQty(totalDyeingQty) }}</b> KG ／
        生成检验单 <b>{{ inspectionOrderCount }}</b> 张
        <span v-if="newColorInstructionCount > 0">
          ／ 新建打色单 <b>{{ newColorInstructionCount }}</b> 张
        </span>
      </div>
      <div class="actions">
        <el-button
          type="primary"
          size="default"
          :loading="submitting"
          :disabled="isCancelled || totalGreigeQty <= 0 || hasExceededQty"
          v-hasPermi="['lunaerp:production:release-production:release']"
          @click="handleSubmit"
        >
          下达生产
        </el-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as ReleaseProductionApi from '@/api/lunaerp-elmui/production/release-production'
import type {
  ReleaseContractLineVO,
  ReleaseExistingColorInstructionVO,
  ReleaseFromContractRespVO,
  ReleaseGroupLineVO,
  ReleaseGroupVO,
  ReleaseNewColorInstructionVO,
  ReleaseProductionReqVO
} from '@/api/lunaerp-elmui/production/release-production'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'

defineOptions({ name: 'LunaerpElmuiSalesContractReleaseProductionTab' })

const props = defineProps<{
  contractId: number | string
}>()

const message = useMessage()

const loading = ref(false)
const submitting = ref(false)

/** 合同头信息快照 */
const contractInfo = reactive<{
  contractId?: number
  contractNo?: string
  clientName?: string
  deliveryDate?: string
  status?: string
}>({
  contractId: undefined,
  contractNo: '',
  clientName: '',
  deliveryDate: '',
  status: ''
})

/** 已有打色单列表 */
const existingColorInstructions = ref<ReleaseExistingColorInstructionVO[]>([])

/** 工厂配置表单 */
const factoryForm = reactive<{
  greigeFactoryId?: number
  dyeingFactoryId?: number
}>({
  greigeFactoryId: undefined,
  dyeingFactoryId: undefined
})

/** 供应商远程候选列表与加载状态 */
const greigeLoading = ref(false)
const dyeingLoading = ref(false)
const labLoading = ref(false)
const greigeFactoryOptions = ref<SupplierVO[]>([])
const dyeingFactoryOptions = ref<SupplierVO[]>([])
const labSupplierOptions = ref<SupplierVO[]>([])

/** 合同行前端可编辑状态结构 */
export interface TableRowItem extends ReleaseContractLineVO {
  releaseQty: number
  lossRatePercent: number
  colorInstructionMode: 'NONE' | 'EXISTING' | 'NEW'
  existingColorInstructionId?: number | null
}

const tableRows = ref<TableRowItem[]>([])

/** 单个颜色行的打色单配置项 */
interface ColorConfig {
  lightPrimary: string
  factoryColorNo: string
  colorCardRef: string
}

/** 分组级配置项：打色模式、已有打色单ID、打色承接方、逐颜色行配置 */
interface GroupConfig {
  colorInstructionMode: 'NONE' | 'EXISTING' | 'NEW'
  existingColorInstructionId?: number | null
  labFactoryId?: number | null
  colorConfigs: Record<number, ColorConfig>
}

const groupConfigMap = reactive<Record<string, GroupConfig>>({})

/** 合同是否已取消 */
const isCancelled = computed(() => contractInfo.status?.toUpperCase() === 'CANCELLED')

/** 产品行计数映射（用于同品合并标识） */
const productGroupCountMap = computed(() => {
  const map: Record<string, number> = {}
  for (const row of tableRows.value) {
    const key = row.productCode || 'UNKNOWN'
    map[key] = (map[key] || 0) + 1
  }
  return map
})

/** 获取各产品可用的已有打色单 */
const getProductExistingInstructions = (productCode?: string) => {
  if (!productCode) return existingColorInstructions.value
  return existingColorInstructions.value.filter(
    (item) => !item.productCode || item.productCode === productCode
  )
}

/** 获取新建打色单颜色行配置 */
const getColorConfig = (productCode: string, contractLineId: number): ColorConfig => {
  const grp = groupConfigMap[productCode]
  if (!grp) return { lightPrimary: 'D65', factoryColorNo: '', colorCardRef: '' }
  if (!grp.colorConfigs[contractLineId]) {
    grp.colorConfigs[contractLineId] = {
      lightPrimary: 'D65',
      factoryColorNo: '',
      colorCardRef: ''
    }
  }
  return grp.colorConfigs[contractLineId]
}

/** 本次下达量是否超出剩余量 */
const isQtyExceeded = (row: TableRowItem): boolean => {
  const release = Number(row.releaseQty || 0)
  const remaining = Number(row.remainingQty || 0)
  return release > remaining
}

/** 是否存在任何超出剩余量的行 */
const hasExceededQty = computed(() => {
  return tableRows.value.some((row) => isQtyExceeded(row))
})

/** 表格行样式：同组视觉区分 */
const getRowClassName = ({ row }: { row: TableRowItem }) => {
  const count = productGroupCountMap.value[row.productCode || 'UNKNOWN'] || 1
  return count > 1 ? 'grouped-row' : ''
}

/** 模式切换：同步组内所有行的 colorInstructionMode */
const handleModeChange = (row: TableRowItem, mode: 'NONE' | 'EXISTING' | 'NEW') => {
  const code = row.productCode || 'UNKNOWN'
  if (groupConfigMap[code]) {
    groupConfigMap[code].colorInstructionMode = mode
  }
  // 同步同品行
  for (const item of tableRows.value) {
    if ((item.productCode || 'UNKNOWN') === code) {
      item.colorInstructionMode = mode
    }
  }
}

/** 已有打色单切换：同步组内所有行 */
const handleExistingInstructionChange = (row: TableRowItem, id: number | null) => {
  const code = row.productCode || 'UNKNOWN'
  if (groupConfigMap[code]) {
    groupConfigMap[code].existingColorInstructionId = id
  }
  for (const item of tableRows.value) {
    if ((item.productCode || 'UNKNOWN') === code) {
      item.existingColorInstructionId = id
    }
  }
}

/** 按产品分组聚合数据 */
const groupedData = computed(() => {
  const map = new Map<string, { productCode: string; productName: string; lines: TableRowItem[] }>()
  for (const row of tableRows.value) {
    const code = row.productCode || 'UNKNOWN'
    if (!map.has(code)) {
      map.set(code, {
        productCode: code,
        productName: row.productName || '',
        lines: []
      })
    }
    map.get(code)!.lines.push(row)
  }
  return Array.from(map.values())
})

/** 选择「新建」打色单的产品组 */
const newColorGroups = computed(() => {
  return groupedData.value.filter((g) => {
    const mode = groupConfigMap[g.productCode]?.colorInstructionMode
    return mode === 'NEW'
  })
})

const hasNewColorInstructionGroups = computed(() => newColorGroups.value.length > 0)

/** 底部汇总数据计算 */
const totalGreigeQty = computed(() => {
  return tableRows.value.reduce((sum, row) => sum + Number(row.releaseQty || 0), 0)
})

const totalDyeingQty = computed(() => {
  return totalGreigeQty.value
})

/** 有下达量的产品组数 = 胚布单数 = 染色单数 = 检验单数 */
const activeProductGroups = computed(() => {
  return groupedData.value.filter((g) =>
    g.lines.some((line) => Number(line.releaseQty || 0) > 0)
  )
})

const inspectionOrderCount = computed(() => activeProductGroups.value.length)

const newColorInstructionCount = computed(() => {
  return activeProductGroups.value.filter((g) => {
    return groupConfigMap[g.productCode]?.colorInstructionMode === 'NEW'
  }).length
})

/** 数量格式化 */
const formatQty = (val?: number | string): string => {
  if (val == null) return '0.00'
  return Number(val).toFixed(2)
}

/** 供应商远程搜索 */
const searchSuppliersRemote = async (query: string): Promise<SupplierVO[]> => {
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 20,
      search: query || ''
    })
    return res.list || []
  } catch (e) {
    console.error('搜索供应商失败', e)
    return []
  }
}

const searchGreigeFactories = async (query: string) => {
  greigeLoading.value = true
  try {
    greigeFactoryOptions.value = await searchSuppliersRemote(query)
  } finally {
    greigeLoading.value = false
  }
}

const searchDyeingFactories = async (query: string) => {
  dyeingLoading.value = true
  try {
    dyeingFactoryOptions.value = await searchSuppliersRemote(query)
  } finally {
    dyeingLoading.value = false
  }
}

const searchLabSuppliers = async (query: string) => {
  labLoading.value = true
  try {
    labSupplierOptions.value = await searchSuppliersRemote(query)
  } finally {
    labLoading.value = false
  }
}

/** 初始化供应商列表 */
const initSuppliers = async () => {
  const list = await searchSuppliersRemote('')
  greigeFactoryOptions.value = [...list]
  dyeingFactoryOptions.value = [...list]
  labSupplierOptions.value = [...list]
}

/** 加载销售合同下达生产预填数据 */
const loadData = async (isAfterSuccess = false) => {
  if (!props.contractId) return
  loading.value = true
  try {
    const data: ReleaseFromContractRespVO = await ReleaseProductionApi.getReleaseFromContract(
      Number(props.contractId)
    )

    contractInfo.contractId = data.contractId
    contractInfo.contractNo = data.contractNo
    contractInfo.clientName = data.clientName
    contractInfo.deliveryDate = data.deliveryDate
    contractInfo.status = data.status

    existingColorInstructions.value = data.existingColorInstructions || []

    // 初始化合同行
    const rows: TableRowItem[] = (data.lines || []).map((line) => {
      const code = line.productCode || 'UNKNOWN'

      // 若没有该组配置则初始化
      if (!groupConfigMap[code]) {
        groupConfigMap[code] = {
          colorInstructionMode: 'NONE',
          existingColorInstructionId: null,
          labFactoryId: null,
          colorConfigs: {}
        }
      }

      // 颜色行默认主灯源 D65
      if (!groupConfigMap[code].colorConfigs[line.contractLineId]) {
        groupConfigMap[code].colorConfigs[line.contractLineId] = {
          lightPrimary: 'D65',
          factoryColorNo: '',
          colorCardRef: ''
        }
      }

      // 下达成功后清空本次下达量（0），初次进入缺省为剩余量
      const defaultRelease = isAfterSuccess
        ? 0
        : Math.max(0, Number(line.remainingQty ?? 0))

      return {
        ...line,
        releaseQty: defaultRelease,
        lossRatePercent: line.defaultLossRate != null ? Number(line.defaultLossRate) : 0,
        colorInstructionMode: groupConfigMap[code].colorInstructionMode,
        existingColorInstructionId: groupConfigMap[code].existingColorInstructionId
      }
    })

    tableRows.value = rows
  } catch (e: any) {
    console.error('加载下达生产预填失败', e)
  } finally {
    loading.value = false
  }
}

/** 提交下达生产 */
const handleSubmit = async () => {
  // 1. 合同取消校验
  if (isCancelled.value) {
    message.error('合同已取消，无法下达生产')
    return
  }

  // 2. 工厂必填校验
  if (!factoryForm.greigeFactoryId) {
    message.error('请选择承织坯布厂')
    return
  }
  if (!factoryForm.dyeingFactoryId) {
    message.error('请选择承染方')
    return
  }

  // 3. 下达量有效性校验
  if (totalGreigeQty.value <= 0) {
    message.error('本次下达量必须大于 0')
    return
  }

  for (const row of tableRows.value) {
    if (isQtyExceeded(row)) {
      message.error(
        `合同行【行号 ${row.lineNo ?? row.contractLineId}】本次下达量超出剩余可下达量`
      )
      return
    }
  }

  // 4. 打色单校验
  for (const grp of activeProductGroups.value) {
    const config = groupConfigMap[grp.productCode]
    if (config?.colorInstructionMode === 'EXISTING') {
      if (!config.existingColorInstructionId) {
        message.error(`请为产品【${grp.productCode}】选择已有打色单`)
        return
      }
    } else if (config?.colorInstructionMode === 'NEW') {
      // 检验主灯源必填
      const activeLines = grp.lines.filter((l) => Number(l.releaseQty || 0) > 0)
      for (const line of activeLines) {
        const colorCfg = config.colorConfigs[line.contractLineId]
        if (!colorCfg || !colorCfg.lightPrimary || !colorCfg.lightPrimary.trim()) {
          message.error(
            `产品【${grp.productCode}】客户色号【${line.customerColor || ''}】打色单主灯源不能为空`
          )
          return
        }
      }
    }
  }

  // 5. 提交前二次确认
  const activeGrpCount = activeProductGroups.value.length
  const newInstCount = newColorInstructionCount.value
  const confirmDetails = [
    `• 承织坯布厂 / 承染方已指定`,
    `• 胚布单：${activeGrpCount} 张，数量合计 ${formatQty(totalGreigeQty.value)} KG`,
    `• 染色单：${activeGrpCount} 张，数量合计 ${formatQty(totalDyeingQty.value)} KG`,
    `• 胚布检验单：${activeGrpCount} 张（GREIGE 待检草稿）`,
    newInstCount > 0 ? `• 打色指示单：${newInstCount} 张（PENDING 待确认）` : ''
  ]
    .filter(Boolean)
    .join('\n')

  try {
    await message.confirm(
      `即将为合同【${contractInfo.contractNo || props.contractId}】下达生产：\n${confirmDetails}\n\n是否确认提交？`,
      '确认下达生产'
    )
  } catch {
    return
  }

  // 6. 构造请求 Payload
  const groupsPayload: ReleaseGroupVO[] = []

  for (const grp of activeProductGroups.value) {
    const config = groupConfigMap[grp.productCode]
    const linesPayload: ReleaseGroupLineVO[] = grp.lines
      .filter((line) => Number(line.releaseQty || 0) > 0)
      .map((line) => ({
        contractLineId: line.contractLineId,
        customerColor: line.customerColor,
        releaseQty: Number(line.releaseQty),
        lossRate: Number(((line.lossRatePercent || 0) / 100).toFixed(4))
      }))

    let newColorInstructionPayload: ReleaseNewColorInstructionVO | null = null
    if (config?.colorInstructionMode === 'NEW') {
      newColorInstructionPayload = {
        labFactoryId: config.labFactoryId || null,
        colors: grp.lines
          .filter((line) => Number(line.releaseQty || 0) > 0)
          .map((line) => {
            const colorCfg = config.colorConfigs[line.contractLineId] || {
              lightPrimary: 'D65',
              factoryColorNo: '',
              colorCardRef: ''
            }
            return {
              contractLineId: line.contractLineId,
              customerColorNo: line.customerColor || '',
              colorName: line.customerColor || '',
              lightPrimary: colorCfg.lightPrimary,
              factoryColorNo: colorCfg.factoryColorNo || '',
              colorCardRef: colorCfg.colorCardRef || ''
            }
          })
      }
    }

    const firstLine = grp.lines[0]
    groupsPayload.push({
      productId: firstLine?.productId,
      productCode: grp.productCode,
      productName: grp.productName,
      composition: firstLine?.composition,
      width: firstLine?.width,
      gsm: firstLine?.gsm,
      colorInstructionMode: config?.colorInstructionMode || 'NONE',
      existingColorInstructionId:
        config?.colorInstructionMode === 'EXISTING' ? config.existingColorInstructionId : null,
      newColorInstruction: newColorInstructionPayload,
      lines: linesPayload
    })
  }

  const reqVO: ReleaseProductionReqVO = {
    contractId: Number(props.contractId),
    greigeFactoryId: factoryForm.greigeFactoryId!,
    dyeingFactoryId: factoryForm.dyeingFactoryId!,
    groups: groupsPayload
  }

  submitting.value = true
  try {
    await ReleaseProductionApi.releaseProduction(reqVO)
    message.success('下达生产成功')
    // 成功后重新加载刷新已下达量，并清空本次下达量
    await loadData(true)
  } catch (e: any) {
    console.error('下达生产失败', e)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  initSuppliers()
  loadData(false)
})
</script>

<style lang="scss" scoped>
.release-production-tab {
  padding: 8px 0;
}

.section-title {
  padding-left: 8px;
  font-weight: 600;
  border-left: 3px solid var(--el-color-primary);
}

.card-header-title {
  display: flex;
  align-items: center;

  .header-tip {
    margin-left: 8px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.product-cell {
  line-height: 1.4;

  .product-code {
    color: var(--el-text-color-primary);
  }
}

.qty-error-wrapper {
  .error-tip {
    font-size: 11px;
    color: var(--el-color-danger);
    margin-top: 2px;
    white-space: nowrap;
  }
}

.is-error-input :deep(.el-input__wrapper) {
  border: 1px solid var(--el-color-danger) !important;
  box-shadow: 0 0 0 1px var(--el-color-danger) inset !important;
}

.new-color-group-box {
  padding: 12px 16px;
  background-color: var(--el-fill-color-light);
  border-radius: 4px;
  border: 1px dashed var(--el-border-color);

  .group-title {
    font-size: 14px;
    color: var(--el-text-color-primary);
  }
}

.bottom-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding: 12px 20px;
  background-color: var(--el-fill-color-light);
  border-radius: 4px;

  .summary-info {
    font-size: 14px;

    b {
      color: var(--el-color-primary);
      margin: 0 4px;
    }
  }
}

:deep(.grouped-row) {
  background-color: var(--el-fill-color-lighter);
}
</style>
