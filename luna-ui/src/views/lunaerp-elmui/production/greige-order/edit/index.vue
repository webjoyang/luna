<template>
  <ContentWrap>
    <div class="edit-header">
      <el-page-header @back="handleBack">
        <template #content>
          <span class="header-title">{{ pageTitle }}</span>
        </template>
      </el-page-header>
    </div>

    <!-- 整页表单：新建 / 编辑 / 查看（查看模式全部控件 disabled、隐藏保存） -->
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      :disabled="isViewMode"
      label-width="110px"
      class="greige-form"
      v-loading="loading"
    >
      <div class="section-title">基本信息</div>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="胚布单号" prop="orderNo">
            <el-input
              v-model="formData.orderNo"
              maxlength="32"
              placeholder="留空自动生成（如 PB0919001），也可手填"
            >
              <template v-if="isCreateMode" #append>
                <el-button :loading="nextNoLoading" @click="handleGetNextNo">获取编号</el-button>
              </template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="生产模式" prop="productionMode">
            <el-radio-group
              v-model="formData.productionMode"
              :disabled="isViewMode"
              @change="handleProductionModeChange"
            >
              <el-radio-button
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_GREIGE_PRODUCTION_MODE)"
                :key="dict.value"
                :label="dict.value"
              >
                {{ dict.label }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="销售合同" prop="salesContractId">
            <el-select
              v-model="formData.salesContractId"
              placeholder="输入关键词搜索合同（可空）"
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="searchContracts"
              :loading="contractLoading"
              class="!w-full"
              @change="handleContractChange"
            >
              <el-option
                v-for="item in contractOptions"
                :key="item.id"
                :label="item.contractNo"
                :value="item.id!"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单据日期" prop="orderDate">
            <el-date-picker
              v-model="formData.orderDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择单据日期"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="交货日期" prop="deliveryDate">
            <el-date-picker
              v-model="formData.deliveryDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择交货日期（可空）"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="产品档案" prop="productId">
            <el-select
              v-model="formData.productId"
              placeholder="输入关键词搜索产品档案（可空）"
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="searchProducts"
              :loading="productLoading"
              class="!w-full"
              @change="handleProductChange"
            >
              <el-option
                v-for="product in productOptions"
                :key="product.id"
                :label="productOptionLabel(product)"
                :value="product.id!"
              >
                <span>{{ product.code }}</span>
                <span class="product-option-name">{{ product.name }}</span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <div class="section-title">坯布厂与染厂</div>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="坯布厂" prop="greigeFactoryId">
            <el-select
              v-model="formData.greigeFactoryId"
              :placeholder="
                formData.productionMode === 'SELF'
                  ? '自织模式坯布厂可为空'
                  : '输入关键词搜索供应商（必选）'
              "
              :clearable="formData.productionMode === 'SELF'"
              filterable
              remote
              reserve-keyword
              :remote-method="(query: string) => searchSuppliers(query, 'greige')"
              :loading="greigeSupplierLoading"
              class="!w-full"
              @change="(val?: number) => handleSupplierChange(val, 'greige')"
            >
              <el-option
                v-for="item in greigeSupplierOptions"
                :key="item.id"
                :label="supplierOptionLabel(item)"
                :value="item.id!"
              >
                <span>{{ item.supplierNo }} {{ item.companyName }}</span>
                <span class="option-type">{{ typeLabel(item.supplierType) }}</span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="指定染厂" prop="dyeingFactoryId">
            <el-select
              v-model="formData.dyeingFactoryId"
              placeholder="输入关键词搜索供应商（可空）"
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="(query: string) => searchSuppliers(query, 'dyeing')"
              :loading="dyeingSupplierLoading"
              class="!w-full"
              @change="(val?: number) => handleSupplierChange(val, 'dyeing')"
            >
              <el-option
                v-for="item in dyeingSupplierOptions"
                :key="item.id"
                :label="supplierOptionLabel(item)"
                :value="item.id!"
              >
                <span>{{ item.supplierNo }} {{ item.companyName }}</span>
                <span class="option-type">{{ typeLabel(item.supplierType) }}</span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <div class="section-title">品种规格</div>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="产品编号" prop="productCode">
            <el-input
              v-model="formData.productCode"
              maxlength="64"
              :disabled="isViewMode || !!formData.productId"
              :placeholder="formData.productId ? '已关联产品档案（自动带出）' : '可手填产品编号'"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="产品名称" prop="productName">
            <el-input
              v-model="formData.productName"
              maxlength="128"
              :disabled="isViewMode || !!formData.productId"
              :placeholder="formData.productId ? '已关联产品档案（自动带出）' : '可手填产品名称'"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="成分" prop="composition">
            <el-input v-model="formData.composition" maxlength="128" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="幅宽" prop="width">
            <el-input v-model="formData.width" maxlength="64" placeholder="如 160cm 有效" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="克重" prop="gsm">
            <el-input v-model="formData.gsm" maxlength="64" placeholder="如 180g" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="缩水率" prop="shrinkage">
            <el-input v-model="formData.shrinkage" maxlength="64" placeholder="如 ≤5%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="匹重(KG)" prop="pieceWeight">
            <el-input-number
              v-model="formData.pieceWeight"
              :precision="2"
              :controls="false"
              :min="0"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <div class="section-title">生产计划</div>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="计划投胚量" prop="plannedQty">
            <el-input-number
              v-model="formData.plannedQty"
              :precision="2"
              :controls="false"
              :min="0"
              class="!w-full"
              placeholder="KG"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="匹数" prop="pieceCount">
            <el-input-number
              v-model="formData.pieceCount"
              :precision="0"
              :controls="false"
              :min="0"
              class="!w-full"
              placeholder="按投胚量÷匹重自动算出，可手改"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="成品数量" prop="finishedQty">
            <el-input-number
              v-model="formData.finishedQty"
              :precision="2"
              :controls="false"
              :min="0"
              class="!w-full"
              placeholder="下单成品数量 KG（可空）"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="预设损耗" prop="lossPercent">
            <el-input-number
              v-model="lossPercent"
              :precision="2"
              :controls="false"
              :min="0"
              :max="100"
              class="!w-full"
            >
              <template #append>%</template>
            </el-input-number>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="加工费" prop="processingFee">
            <el-input-number
              v-model="formData.processingFee"
              :precision="2"
              :controls="false"
              :min="0"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="胚布品种" prop="greigeTypeId">
            <el-select
              v-model="formData.greigeTypeId"
              placeholder="请选择胚布品种（可空）"
              clearable
              filterable
              class="!w-full"
            >
              <el-option
                v-for="item in greigeTypeOptions"
                :key="item.id"
                :label="`${item.code} ${item.name}`"
                :value="String(item.id)"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 织造执行进度（自织模式且非新建状态展示，只读聚合视图） -->
      <template v-if="formData.productionMode === 'SELF' && !isCreateMode">
        <div class="section-title section-title-flex">
          <span>织造执行进度</span>
          <el-button
            type="primary"
            size="small"
            plain
            :disabled="false"
            @click="handleCreateWeavingOrder"
          >
            生成织造单
          </el-button>
        </div>
        <div class="weaving-progress-wrap">
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="stat-card">
                <div class="stat-title">织造计划匹数</div>
                <div class="stat-value">{{ weavingPlannedPieces }} 匹</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-card">
                <div class="stat-title">织造已完成匹数</div>
                <div class="stat-value">{{ weavingCompletedPieces }} 匹</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-card">
                <div class="stat-title">完成进度</div>
                <div class="stat-value">
                  <el-progress
                    :percentage="weavingProgressPercentage"
                    :stroke-width="14"
                    :format="() => `${weavingCompletedPieces} / ${weavingPlannedPieces} 匹 (${weavingProgressPercentage}%)`"
                  />
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </template>

      <div class="section-title">颜色投胚分配</div>
      <el-table
        :data="colorRows"
        :row-key="getRowKey"
        border
        size="small"
        class="color-table"
        empty-text="暂无颜色行，可点击下方按钮添加（零颜色单据也可保存）"
      >
        <el-table-column label="行号" type="index" width="50" align="center" />
        <el-table-column label="颜色" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.color" :disabled="isViewMode" maxlength="64" placeholder="如 大红" />
          </template>
        </el-table-column>
        <el-table-column label="成品下单量(KG)" width="150">
          <template #default="{ row }">
            <el-input-number
              v-model="row.finishedQty"
              :disabled="isViewMode"
              :precision="2"
              :controls="false"
              :min="0"
              class="!w-full"
            />
          </template>
        </el-table-column>
        <el-table-column label="损耗率(%)" width="130">
          <template #default="{ row }">
            <el-input-number
              v-model="row.lossPercent"
              :disabled="isViewMode"
              :precision="2"
              :controls="false"
              :min="0"
              :max="99.99"
              class="!w-full"
              placeholder="留空取主单预设损耗"
            />
          </template>
        </el-table-column>
        <el-table-column label="投胚需求(KG)" width="130" align="right">
          <template #default="{ row }">
            <span class="row-demand">{{ formatQty(rowDemand(row)) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.remark" :disabled="isViewMode" />
          </template>
        </el-table-column>
        <el-table-column v-if="!isViewMode" label="操作" width="70" align="center" fixed="right">
          <template #default="{ $index }">
            <el-button type="danger" link @click="removeColor($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!isViewMode" class="items-actions">
        <el-button type="primary" plain @click="addColor">添加颜色行</el-button>
      </div>
      <div class="items-summary">
        合计：成品下单量 <b>{{ formatQty(totalFinishedQty) }}</b>
        ／ 投胚需求 <b>{{ formatQty(totalDemandQty) }}</b>
        <span class="summary-tip">（仅供参考，不与计划投胚量做一致校验；保存后以服务端重算为准）</span>
      </div>

      <div class="section-title">备注</div>
      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="生产工艺备注" prop="productionRemark">
            <el-input
              v-model="formData.productionRemark"
              type="textarea"
              :rows="3"
              maxlength="1000"
              placeholder="请输入织造指令等生产工艺备注"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="formData.remark" type="textarea" :rows="3" maxlength="1000" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-select v-model="formData.status" class="!w-full">
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_GREIGE_ORDER_STATUS)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <div class="form-footer">
        <!-- 只读模式仅隐藏保存按钮（规格要求），返回保留 -->
        <el-button
          v-if="!isViewMode"
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
          v-hasPermi="
            isCreateMode
              ? ['lunaerp:production:greige-order:create']
              : ['lunaerp:production:greige-order:update']
          "
        >
          保存
        </el-button>
        <el-button @click="handleBack">返回</el-button>
      </div>
    </el-form>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { DICT_TYPE, getStrDictOptions, getDictLabel } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as GreigeOrderApi from '@/api/lunaerp-elmui/production/greige-order'
import type { GreigeOrderVO, GreigeOrderColorVO } from '@/api/lunaerp-elmui/production/greige-order'
import * as SalesContractApi from '@/api/lunaerp-elmui/business/sales-contract'
import type { SalesContractVO } from '@/api/lunaerp-elmui/business/sales-contract'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import * as ProductApi from '@/api/lunaerp/baseinfo/product'
import type { ProductVO } from '@/api/lunaerp/baseinfo/product'
import * as GreigeTypeApi from '@/api/lunaerp/baseinfo/greige-type'
import type { GreigeTypeSimpleVO } from '@/api/lunaerp/baseinfo/greige-type'

defineOptions({ name: 'LunaerpElmuiProductionGreigeOrderEdit' })

const route = useRoute()
const router = useRouter()
const message = useMessage()

const formRef = ref()
const loading = ref(false)
const submitting = ref(false)

const formData = reactive<GreigeOrderVO>({
  productionMode: 'OUTSOURCE',
  orderNo: '',
  salesContractId: undefined,
  contractNo: '',
  productId: undefined,
  greigeFactoryId: undefined as unknown as number,
  greigeFactoryName: '',
  dyeingFactoryId: undefined,
  dyeingFactoryName: '',
  orderDate: formatDate(new Date(), 'YYYY-MM-DD'),
  deliveryDate: undefined,
  productCode: '',
  productName: '',
  composition: '',
  width: '',
  gsm: '',
  shrinkage: '',
  pieceWeight: 25,
  pieceCount: undefined,
  plannedQty: undefined,
  finishedQty: undefined,
  processingFee: undefined,
  greigeTypeId: undefined,
  productionRemark: '',
  remark: '',
  status: 'ACTIVE'
})

// ==================== 颜色投胚分配（聚合保存，ADR-0003 & 差异保存 ADR-0032） ====================

/**
 * 颜色行前端展示类型：损耗率以百分比（lossPercent）录入，
 * 提交时换算回小数 lossRate；demandQty 由前端实时预览、服务端权威重算。
 * _clientKey 作为新增行前端稳定 row-key（存量行以 id 为 key）。
 */
type ColorRow = GreigeOrderColorVO & {
  lossPercent?: number
  _clientKey?: string
}

let tempKeyCounter = 0
/** 颜色明细表格以行 id 为稳定 key；新增行未落库前以临时 key 保证行唯一性（ADR-0032） */
const getRowKey = (row: ColorRow) => {
  if (row.id != null) {
    return row.id
  }
  if (!row._clientKey) {
    row._clientKey = `temp_${++tempKeyCounter}`
  }
  return row._clientKey
}

/** 颜色行独立于 formData 维护（避开 Object.assign 回填覆盖），保存时并入载荷 */
const colorRows = ref<ColorRow[]>([])

/** 新增一行空颜色行 */
const addColor = () => {
  colorRows.value.push({
    _clientKey: `temp_${++tempKeyCounter}`,
    id: undefined,
    color: '',
    finishedQty: 0,
    lossPercent: undefined,
    remark: ''
  })
}

/** 删除指定颜色行 */
const removeColor = (index: number) => {
  colorRows.value.splice(index, 1)
}

/** null/undefined 安全转数字 */
const toNumber = (val: number | undefined | null): number => (val == null ? 0 : Number(val))

/**
 * 投胚需求实时预览：成品量 ÷ (1 − 损耗率)，与后端口径一致；
 * 行损耗留空取主单预设损耗（lossPercent 状态源），损耗 ≥ 100% 无放大意义按不放大显示。
 */
const rowDemand = (row: ColorRow): number => {
  const finishedQty = toNumber(row.finishedQty)
  const lossRate =
    row.lossPercent != null ? toNumber(row.lossPercent) / 100 : toNumber(formData.suggestedLoss)
  const divisor = 1 - lossRate
  if (divisor <= 0) return finishedQty
  return finishedQty / divisor
}

/** 合计成品下单量（仅供参考） */
const totalFinishedQty = computed(() =>
  colorRows.value.reduce((sum, row) => sum + toNumber(row.finishedQty), 0)
)

/** 合计投胚需求（仅供参考，不与计划投胚量做一致校验） */
const totalDemandQty = computed(() =>
  colorRows.value.reduce((sum, row) => sum + rowDemand(row), 0)
)

/** 数量格式化（两位小数） */
const formatQty = (val: number): string => val.toFixed(2)

const handleProductionModeChange = (mode: string) => {
  if (mode === 'SELF') {
    formRef.value?.clearValidate('greigeFactoryId')
  } else {
    formRef.value?.validateField('greigeFactoryId').catch(() => {})
  }
}

const rules = computed(() => ({
  greigeFactoryId: [
    {
      required: formData.productionMode !== 'SELF',
      message: '坯布厂不能为空',
      trigger: 'change'
    }
  ],
  orderDate: [{ required: true, message: '单据日期不能为空', trigger: 'change' }],
  plannedQty: [{ required: true, message: '计划投胚量不能为空', trigger: 'blur' }]
}))

const weavingPlannedPieces = computed(() => {
  return formData.weavingProgress?.plannedPieces ?? formData.weavingPlannedPieces ?? 0
})

const weavingCompletedPieces = computed(() => {
  return formData.weavingProgress?.completedPieces ?? formData.weavingCompletedPieces ?? 0
})

const weavingProgressPercentage = computed(() => {
  const planned = weavingPlannedPieces.value
  const completed = weavingCompletedPieces.value
  if (!planned || planned <= 0) return 0
  const pct = Math.round((completed / planned) * 100)
  return Math.min(100, Math.max(0, pct))
})

const handleCreateWeavingOrder = () => {
  router.push({
    path: '/production/weaving-order/workspace',
    query: { greigeOrderId: String(orderId.value) }
  })
}

const orderId = computed(() => (route.params.id as string) || null)
const isCreateMode = computed(() => !orderId.value)
/** 查看模式：列表"查看"以同一页只读打开（?mode=view），全部控件 disabled、隐藏保存 */
const isViewMode = computed(() => route.query.mode === 'view')

const pageTitle = computed(() => {
  if (isCreateMode.value) return '新建胚布单'
  return isViewMode.value ? '查看胚布单' : '编辑胚布单'
})

// ==================== 获取编号（只读预览、不占号） ====================

const nextNoLoading = ref(false)

const handleGetNextNo = async () => {
  // 用户已手工输入（含去空格）时不自动覆盖
  if (formData.orderNo && formData.orderNo.trim()) return
  nextNoLoading.value = true
  try {
    const no = await GreigeOrderApi.getNextGreigeOrderNo()
    // 请求返回后再次判空，避免与手工输入竞态
    if (!formData.orderNo || !formData.orderNo.trim()) {
      formData.orderNo = no
    }
  } finally {
    nextNoLoading.value = false
  }
}

// ==================== 供应商远程搜索（坯布厂/染厂，ADR-0007 不做类型强过滤） ====================

/**
 * 两个选择器共用同一数据源（全部 ACTIVE 供应商，不按 supplierType 过滤），
 * 仅候选列表与加载态独立，选项显示"供应商编号 公司名（类型标签）"辅助辨认。
 */
const greigeSupplierOptions = ref<SupplierVO[]>([])
const dyeingSupplierOptions = ref<SupplierVO[]>([])
const greigeSupplierLoading = ref(false)
const dyeingSupplierLoading = ref(false)

const typeLabel = (supplierType?: string): string =>
  supplierType ? getDictLabel(DICT_TYPE.LUNAERP_SUPPLIER_TYPE, supplierType) : ''

/**
 * 供应商选项 label：编辑/查看回填时用快照预置选项（无档案编号），
 * 仅显示公司名；正常远程搜索选项显示"编号 公司名"。
 */
const supplierOptionLabel = (item: SupplierVO): string =>
  item.supplierNo ? `${item.supplierNo} ${item.companyName}` : item.companyName || ''

const searchSuppliers = async (query: string, role: 'greige' | 'dyeing') => {
  const setLoading = role === 'greige' ? greigeSupplierLoading : dyeingSupplierLoading
  const setOptions = role === 'greige' ? greigeSupplierOptions : dyeingSupplierOptions
  setLoading.value = true
  try {
    // 附加 status=ACTIVE：停用供应商不进新单选项；不传 supplierType（ADR-0007）
    const data = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 20,
      search: query || '',
      status: 'ACTIVE'
    })
    setOptions.value = data.list
  } finally {
    setLoading.value = false
  }
}

/** 选中供应商：保存公司名快照（建单时名称，档案日后改名不回写） */
const handleSupplierChange = (supplierId: number | undefined, role: 'greige' | 'dyeing') => {
  const options = role === 'greige' ? greigeSupplierOptions : dyeingSupplierOptions
  const supplier = options.value.find((item) => item.id === supplierId)
  if (role === 'greige') {
    formData.greigeFactoryName = supplier?.companyName || ''
  } else {
    formData.dyeingFactoryName = supplier?.companyName || ''
  }
}

// ==================== 销售合同远程搜索（可空引用） ====================

const contractLoading = ref(false)
const contractOptions = ref<SalesContractVO[]>([])

const searchContracts = async (query: string) => {
  contractLoading.value = true
  try {
    const data = await SalesContractApi.getSalesContractPage({
      pageNo: 1,
      pageSize: 20,
      keyword: query || ''
    })
    contractOptions.value = data.list
  } finally {
    contractLoading.value = false
  }
}

/** 选中合同：保存合同号快照（可手改） */
const handleContractChange = (contractId: number | undefined) => {
  const contract = contractOptions.value.find((item) => item.id === contractId)
  formData.contractNo = contract?.contractNo || ''
}

// ==================== 产品档案远程搜索（回填品种规格后再改） ====================

const productLoading = ref(false)
const productOptions = ref<ProductVO[]>([])

const productOptionLabel = (item: ProductVO): string =>
  [item.code, item.name].filter(Boolean).join(' ')

const searchProducts = async (query: string) => {
  productLoading.value = true
  try {
    // 产品分页 search 对编号/品名等分词 AND 匹配；不传 status 默认查 ACTIVE
    const data = await ProductApi.getProductPage({ pageNo: 1, pageSize: 20, search: query || '' })
    productOptions.value = data.list || []
  } finally {
    productLoading.value = false
  }
}

/** 选中产品后回填编号/品名/成分/幅宽/克重，全部字段仍可手改 */
const handleProductChange = (productId?: number) => {
  const product = productOptions.value.find((p) => p.id === productId)
  if (!product) {
    formData.productId = undefined
    return
  }
  formData.productId = product.id
  formData.productCode = product.code || ''
  formData.productName = product.name || ''
  formData.composition = product.composition || ''
  formData.width = product.widthCm != null ? String(product.widthCm) : ''
  formData.gsm = product.weightGsm != null ? String(product.weightGsm) : ''
}

// ==================== 胚布品种（手选、不自动识别） ====================

const greigeTypeOptions = ref<GreigeTypeSimpleVO[]>([])

const loadGreigeTypes = async () => {
  try {
    greigeTypeOptions.value = (await GreigeTypeApi.getSimpleGreigeTypeList()) || []
  } catch {
    // 加载失败不阻断表单，品种字段可留空
    greigeTypeOptions.value = []
  }
}

// ==================== 计划投胚量 → 匹数自动计算（可手改） ====================

/**
 * 计划投胚量或匹重变化时按 ceil(投胚量÷匹重) 重算匹数；
 * 用户手改匹数后，再次修改投胚量/匹重仍会重算（以最新输入为准）。
 * 编辑/查看回填期间跳过（restoring），保证匹数人工值原样回显。
 */
const restoring = ref(false)

watch([() => formData.plannedQty, () => formData.pieceWeight], ([plannedQty, pieceWeight]) => {
  if (restoring.value) return
  const qty = Number(plannedQty ?? 0)
  const weight = Number(pieceWeight ?? 0)
  if (qty > 0 && weight > 0) {
    formData.pieceCount = Math.ceil(qty / weight)
  }
})

// ==================== 预设损耗：百分比输入、小数传输 ====================

/** 预设损耗唯一状态源（百分比），提交时统一换算为小数（0.05 = 5%） */
const lossPercent = ref(0)

const toSuggestedLoss = (): number => Number((lossPercent.value ?? 0) / 100)

// ==================== 编辑/查看：详情回填 ====================

/**
 * 加载胚布单（编辑/查看模式）：主表全部字段回填；
 * 远程下拉（坯布厂/染厂/合同）用快照预置当前选项，保证选中态有 label。
 */
const loadGreigeOrder = async () => {
  if (!orderId.value) return
  loading.value = true
  restoring.value = true
  try {
    const data = await GreigeOrderApi.getGreigeOrder(Number(orderId.value))
    Object.assign(formData, data)
    formData.productionMode = data.productionMode || 'OUTSOURCE'
    // 预设损耗小数 → 百分比展示
    lossPercent.value = Number(((data.suggestedLoss ?? 0) * 100).toFixed(2))
    // 颜色行回填：损耗率小数 → 百分比展示（后端保存时已把空值解析为主单损耗/0）
    colorRows.value = (data.colors || []).map((color) => ({
      ...color,
      lossPercent:
        color.lossRate != null ? Number((Number(color.lossRate) * 100).toFixed(2)) : undefined
    }))
    if (data.greigeFactoryId) {
      greigeSupplierOptions.value = [
        { id: data.greigeFactoryId, companyName: data.greigeFactoryName || '' } as SupplierVO
      ]
    }
    if (data.dyeingFactoryId) {
      dyeingSupplierOptions.value = [
        { id: data.dyeingFactoryId, companyName: data.dyeingFactoryName || '' } as SupplierVO
      ]
    }
    if (data.salesContractId) {
      contractOptions.value = [
        { id: data.salesContractId, contractNo: data.contractNo || '' } as SalesContractVO
      ]
    }
    if (data.productId) {
      productOptions.value = [
        {
          id: data.productId,
          code: data.productCode || '',
          name: data.productName || ''
        } as ProductVO
      ]
    }
  } finally {
    restoring.value = false
    loading.value = false
  }
}

// ==================== 保存 ====================

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload: GreigeOrderVO = {
      ...formData,
      productionMode: formData.productionMode || 'OUTSOURCE',
      orderNo: formData.orderNo?.trim() || undefined, // 留空走后端自动号
      contractNo: formData.contractNo?.trim() || undefined,
      greigeFactoryId:
        formData.productionMode === 'SELF'
          ? (formData.greigeFactoryId ?? (null as unknown as number))
          : formData.greigeFactoryId,
      greigeFactoryName:
        formData.productionMode === 'SELF'
          ? (formData.greigeFactoryName?.trim() || (null as unknown as string))
          : (formData.greigeFactoryName?.trim() || undefined),
      dyeingFactoryName: formData.dyeingFactoryName?.trim() || undefined,
      suggestedLoss: toSuggestedLoss(),
      productId: formData.productId ?? undefined,
      productCode: formData.productCode?.trim() || undefined,
      productName: formData.productName?.trim() || undefined,
      greigeTypeId: formData.greigeTypeId || undefined,
      // 颜色行并入载荷差异保存：带 id 走 update、无 id 走 insert；损耗率百分比 → 小数；投胚需求传前端预览值（服务端权威重算覆盖）
      colors: colorRows.value.map((row) => ({
        id: row.id != null ? row.id : undefined,
        color: row.color,
        finishedQty: row.finishedQty,
        lossRate:
          row.lossPercent != null ? Number((row.lossPercent / 100).toFixed(4)) : undefined,
        demandQty: Number(rowDemand(row).toFixed(2)),
        remark: row.remark
      }))
    }
    if (isCreateMode.value) {
      await GreigeOrderApi.createGreigeOrder(payload)
    } else {
      // 编辑整体保存：携带 id，编号/引用校验规则与新建一致（后端权威）
      await GreigeOrderApi.updateGreigeOrder({ ...payload, id: Number(orderId.value) })
    }
    message.success('保存成功')
    router.push('/production/greige-order')
  } finally {
    submitting.value = false
  }
}

const handleBack = () => {
  router.push('/production/greige-order')
}

/** 初始化 */
onMounted(() => {
  loadGreigeTypes()
  if (isCreateMode.value) {
    // 新建：预取一批供应商/合同/产品候选，保证下拉一打开就有内容可看
    searchSuppliers('', 'greige')
    searchSuppliers('', 'dyeing')
    searchContracts('')
    searchProducts('')
  } else {
    // 编辑/查看：按 id 回填全部字段（远程下拉用快照预置选中态）
    loadGreigeOrder()
  }
})
</script>

<style lang="scss" scoped>
.edit-header {
  margin-bottom: 16px;

  .header-title {
    font-size: 16px;
    font-weight: 600;
  }
}

.section-title {
  margin-bottom: 12px;
  padding-left: 8px;
  font-weight: 600;
  border-left: 3px solid var(--el-color-primary);
}

.section-title-flex {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
}

.weaving-progress-wrap {
  margin-bottom: 16px;
  padding: 16px;
  background: var(--el-fill-color-light);
  border-radius: 6px;

  .stat-card {
    background: var(--el-bg-color);
    padding: 12px 16px;
    border-radius: 4px;
    border: 1px solid var(--el-border-color-lighter);

    .stat-title {
      font-size: 13px;
      color: var(--el-text-color-secondary);
      margin-bottom: 6px;
    }

    .stat-value {
      font-size: 15px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }
  }
}

.greige-form {
  max-width: 1100px;
  padding: 4px 8px;
}

.color-table {
  margin-bottom: 8px;
}

.items-actions {
  margin-bottom: 8px;
}

.items-summary {
  padding: 8px 12px;
  margin-bottom: 4px;
  text-align: right;
  font-size: 14px;
  background: var(--el-fill-color-light);
  border-radius: 4px;

  b {
    color: var(--el-color-primary);
    margin: 0 4px;
  }

  .summary-tip {
    margin-left: 8px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.row-demand {
  font-weight: 600;
  color: var(--el-color-primary);
}

.option-type {
  float: right;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.product-option-name {
  float: right;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.form-footer {
  margin-top: 12px;
  padding-left: 110px;
}
</style>
